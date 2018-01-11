package com.team1389.hardware.outputs.hardware;

import java.util.Optional;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.team1389.configuration.PIDConstants;
import com.team1389.hardware.Hardware;
import com.team1389.hardware.inputs.software.DigitalIn;
import com.team1389.hardware.inputs.software.PercentIn;
import com.team1389.hardware.inputs.software.PositionEncoderIn;
import com.team1389.hardware.inputs.software.RangeIn;
import com.team1389.hardware.outputs.software.PercentOut;
import com.team1389.hardware.outputs.software.RangeOut;
import com.team1389.hardware.registry.Registry;
import com.team1389.hardware.registry.port_types.CAN;
import com.team1389.hardware.value_types.Position;
import com.team1389.hardware.value_types.Speed;
import com.team1389.hardware.value_types.Value;
import com.team1389.util.list.AddList;
import com.team1389.util.state.State;
import com.team1389.util.state.StateTracker;
import com.team1389.watch.Watchable;
import com.team1389.watch.info.NumberInfo;
import com.team1389.watch.info.StringInfo;

/**
 * This class offers input/output stream sources for a Talon SRX.
 * <p>
 * Furthermore, this class will ensure that the Talon has been given all
 * required configuration before it enters any control mode. <br>
 * TODO add limit switch support
 * 
 * @author amind
 *
 */
public class CANTalonHardware extends Hardware<CAN> {

	private StateTracker stateTracker;
	private Optional<WPI_TalonSRX> wpiTalon;
	private boolean outputInverted;
	private boolean inputInverted;
	private State voltageState, speedState, positionState, followingState;

	/**
	 * @param outInverted
	 *            whether the motor output should be inverted (used for both voltage
	 *            and position control modes)
	 * @param inpInverted
	 *            whether the sensor input should be inverted
	 * @param requestedPort
	 *            the port to attempt to initialize this hardware
	 * @param registry
	 *            the registry associated with the robot
	 * @see <a href=
	 *      "https://www.ctr-electronics.com/Talon%20SRX%20Software%20Reference%20Manual.pdf">Talon
	 *      SRX user manual</a> for more information on output/input inversion
	 */
	public CANTalonHardware(boolean outInverted, boolean inpInverted, CAN requestedPort, Registry registry) {
		this.outputInverted = outInverted;
		this.inputInverted = inpInverted;
		attachHardware(requestedPort, registry);
	}

	/**
	 * assumes input is not inverted
	 * 
	 * @param outInverted
	 *            whether the motor output should be inverted (used for both voltage
	 *            and position control modes)
	 * @param requestedPort
	 *            the port to attempt to initialize this hardware
	 * @param registry
	 *            the registry associated with the robot
	 * @see <a href=
	 *      "https://www.ctr-electronics.com/Talon%20SRX%20Software%20Reference%20Manual.pdf">Talon
	 *      SRX user manual</a> for more information on output/input inversion
	 */
	public CANTalonHardware(boolean outInverted, CAN requestedPort, Registry registry) {
		this(outInverted, false, requestedPort, registry);
	}

	/**
	 * @return a speed input stream indicating the current speed of the motor based
	 *         on the talon's preferred speed sensor <br>
	 *         uses a default range of [0,4096] specified by the CTRE mag encoder,
	 *         but may vary depending on the sensor
	 */
	public RangeIn<Speed> getSpeedInput() {
		// max ticks was 8192, changes for Ctre mag encoder
		return new RangeIn<Speed>(Speed.class, this::getSpeed, 0, 4096);
	}

	/**
	 * @return a position input stream indicating the current speed of the motor
	 *         based on the talon's preferred position sensor <br>
	 *         uses a default range of [0,4096] specified by the CTRE mag encoder,
	 *         but may vary depending on the sensor
	 */
	public PositionEncoderIn getPositionInput() {
		// max encoder ticks was at 8192, new version meant for ctre mag encoder
		return new PositionEncoderIn(this::getPosition, 4096);
	}

	/**
	 * @return a percent output stream that sets the voltage of the talon in
	 *         PercentVBus mode
	 */
	public PercentOut getVoltageOutput() {
		return new PercentOut(voltage -> {
			voltageState.init();
			wpiTalon.ifPresent(talon -> talon.set(voltage));
		});
	}

	public PercentIn getVoltageInput() {
		return new PercentIn(this::getVoltageOut);
	}

	/**
	 * @param config
	 *            the P,I, and D gains
	 * @param feedForward
	 *            the F gain
	 * @return a speed output stream
	 */
	public RangeOut<Speed> getSpeedOutput(PIDConstants config) {
		setupSpeedState(config);
		return new RangeOut<Speed>(speed -> {
			speedState.init();
			wpiTalon.ifPresent(talon -> talon.set(speed));
		}, 0, 8192);
	}

	public void setFeedbackDevice(FeedbackDevice device) {
		wpiTalon.ifPresent(talon -> talon.configSelectedFeedbackSensor(device, 0, 5000));
	}

	/**
	 * 
	 * @param config
	 *            the P, I, and D gains
	 * @return a position output stream that controls the position of the talon
	 */
	public RangeOut<Position> getPositionOutput(PIDConstants config) {
		setupPositionState(config);
		return new RangeOut<Position>(position -> {
			positionState.init();
			wpiTalon.ifPresent(talon -> talon.set(position));
		}, 0, 8192);
	}

	public DigitalIn getSensorTracker(FeedbackDevice device) {
		return new DigitalIn(() -> isSensorPresent(device));
	}

	/**
	 * @param toFollow
	 *            the master Talon to follow
	 * @return a follower object
	 */
	public CANTalonFollower getFollower(CANTalonHardware toFollow) {
		setupFollowingState(toFollow);
		return new CANTalonFollower() {
			@Override
			public void follow() {
				followingState.init();
			}
		};
	}

	/**
	 * use 40 amps for current limit sets talon current limit to specified val
	 * 
	 * @param currentLimit
	 *            max current in amps
	 */
	// may have to enable current limit in talon
	public void setCurrentLimit(int currentLimit) {
		wpiTalon.get().configCur;
	}

	@Override
	public AddList<Watchable> getSubWatchables(AddList<Watchable> stem) {
		stem = super.getSubWatchables(stem);
		stem.put(new StringInfo("mode", getControlMode()::name));
		switch (getControlMode()) {
		case Current:
			break;
		case Disabled:
			break;
		case Follower:
			break;
		case MotionProfile:
			break;
		case PercentVbus:
			stem.put(new NumberInfo("voltage", this::getVoltage), getPositionInput().getWatchable("position"));
			break;
		case Position:
			stem.put(getPositionInput().getWatchable("position"), new NumberInfo("setpoint", this::getSetpoint));
			break;
		case Speed:
			stem.put(getSpeedInput().getWatchable("speed"), new NumberInfo("setpoint", this::getSetpoint));
			break;
		case Voltage:
			stem.put(new NumberInfo("voltage", this::getVoltage), getPositionInput().getWatchable("position"));
			break;
		default:
			break;

		}
		return stem;
	}

	@Override
	protected String getHardwareIdentifier() {
		return "Talon";
	}

	/**
	 * represents a slave talon in a {@link CANTalonGroup}
	 * 
	 * @author amind
	 *
	 */
	public interface CANTalonFollower {
		/**
		 * 
		 */
		public void follow();
	}

	@Override
	public void init(CAN port) {
		stateTracker = new StateTracker();
		WPI_TalonSRX talon = new WPI_TalonSRX(port.index());
		talon.reverseSensor(inputInverted);
		voltageState = stateTracker.newState(() -> {
			talon.reverseSensor(inputInverted);
			talon.setInverted(outputInverted);
			talon.changeControlMode(TalonControlMode.PercentVbus);
		});
		talon.setPosition(0);
		talon.configNominalOutputVoltage(0.0f, 0.0f);
		talon.configPeakOutputVoltage(12.0f, -12.0f);
		wpiTalon = Optional.of(talon);
	}

	private void setupPositionState(PIDConstants config) {
		positionState = stateTracker.newState(() -> {
			if (wpiTalon.isPresent()) {
				WPI_TalonSRX talon = getWrappedTalon();
				talon.reverseOutput(outputInverted);
				talon.reverseSensor(inputInverted);
				talon.changeControlMode(TalonControlMode.Position);
				setPID(config);
			}
		});
	}

	private void setupSpeedState(PIDConstants config) {
		speedState = stateTracker.newState(() -> {
			if (wpiTalon.isPresent()) {
				WPI_TalonSRX talon = getWrappedTalon();
				talon.reverseOutput(outputInverted);
				talon.reverseSensor(inputInverted);
				talon.changeControlMode(TalonControlMode.Speed);
				setPID(config);
			}
		});
	}

	private void setupFollowingState(CANTalonHardware toFollow) {
		followingState = stateTracker.newState(() -> {
			if (wpiTalon.isPresent() && toFollow.wpiTalon.isPresent()) {
				WPI_TalonSRX talon = wpiTalon.get();
				talon.changeControlMode(TalonControlMode.Follower);
				talon.set(toFollow.getPort());
				talon.reverseOutput(toFollow.outputInverted ^ outputInverted);
			}
		});
	}

	private void setPID(PIDConstants pidConstants) {
		wpiTalon.ifPresent(talon -> {
			talon.setPID(pidConstants.p, pidConstants.i, pidConstants.d);
			talon.setF(pidConstants.f);
		});
	}

	/**
	 * 
	 * @return the setpoint of the talon
	 */
	public double getSetpoint() {
		return wpiTalon.map(talon -> talon.getSetpoint()).orElse(0.0);
	}

	/**
	 * 
	 * @return the current control mode of the talon
	 */
	public ControlMode getControlMode() {
		return wpiTalon.map(talon -> talon.getControlMode()).orElse(ControlMode.Disabled);
	}

	private double getPosition() {
		return wpiTalon.map(talon -> talon.getPosition()).orElse(0.0);
	}

	private double getVoltageOut() {
		return wpiTalon.map(talon -> talon.getOutputVoltage()).orElse(0.0);
	}

	private double getSpeed() {
		return wpiTalon.map(talon -> talon.getSpeed()).orElse(0.0);
	}

	private double getVoltage() {
		return wpiTalon.map(talon -> talon.getOutputVoltage()).orElse(0.0);
	}

	private double getAbsolutePosition() {
		wpiTalon.ifPresent(t -> t.setFeedbackDevice(FeedbackDevice.CtreMagEncoder_Absolute));
		return wpiTalon.map(talon -> talon.getPosition()).orElse(0.0);
	}

	private double getCurrent() {
		return wpiTalon.map(talon -> talon.getOutputCurrent()).orElse(0.0);
	}

	private boolean isSensorPresent(FeedbackDevice device) {
		return wpiTalon.map(talon -> talon.isSensorPresent(device) == FeedbackDeviceStatus.FeedbackStatusPresent)
				.orElse(false);
	}

	public PercentOut getCompensatedVoltageOut() {
		double maxvolts = 12.0;
		return new PercentOut(v -> {
			double inv = outputInverted ? -1.0 : 1.0;
			wpiTalon.ifPresent(t -> t.changeControlMode(TalonControlMode.Voltage));
			wpiTalon.ifPresent(t -> t.setVoltageCompensationRampRate(24.0));
			wpiTalon.get().set(v * maxvolts);
			wpiTalon.ifPresent(t -> t.set(inv * v * maxvolts));
		});
	}

	public RangeIn<Position> getAbsoluteIn() {
		return new RangeIn<Position>(Position.class, () -> (double) getAbsolutePosition(), 0, 1);
	}

	public RangeIn<Value> getCurrentIn() {
		return new RangeIn<Value>(Value.class, this::getCurrent, 0, 40);
	}

	/**
	 * @return the WPILib talon object
	 * @throws NullPointerException
	 *             if the talon failed to initialize due to port claiming failure
	 */
	public WPI_TalonSRX getWrappedTalon() {
		return wpiTalon.get();
	}

	@Override
	public void failInit() {
		wpiTalon = Optional.empty();
	}

}
