package com.team1389.system.drive;

import com.team1389.hardware.inputs.software.AngleIn;
import com.team1389.hardware.inputs.software.DigitalIn;
import com.team1389.hardware.inputs.software.PercentIn;
import com.team1389.hardware.value_types.Percent;
import com.team1389.util.list.AddList;
import com.team1389.watch.Watchable;

public class CurvatureDriveStraightSystem extends CurvatureDriveSystem
{
	private DriveOut<Percent> drive;
	private PercentIn throttle;
	private PercentIn wheel;
	private DigitalIn quickTurnButton;
	public CurvatureDriveAlgorithm calc;
	public AngleIn angle;
	public double kP;
	public DigitalIn driveStraight;
	private DriveSignal mSignal = DriveSignal.NEUTRAL;

	/**
	 * 
	 * @param drive
	 *            percent of voltage going to left/right motors
	 * @param throttle
	 *            percent of desired speed(forward/back)
	 * @param wheel
	 *            percent used for turning(l/r)
	 * @param quickTurnButton
	 *            switching from curvature drive, to tank drive
	 */
	public CurvatureDriveStraightSystem(DriveOut<Percent> drive, PercentIn throttle, PercentIn wheel,
			DigitalIn quickTurnButton, AngleIn angle, double kP, DigitalIn driveStraight)
	{
		super(drive, throttle, wheel, quickTurnButton, 1.0, 1.0);
		this.angle = angle;
		this.kP = kP;
		this.driveStraight = driveStraight;
	}

	/**
	 * 
	 * @param drive
	 *            percent of voltage going to left/right motors
	 * @param throttle
	 *            percent of desired speed(forward/back)
	 * @param wheel
	 *            percent used for turning(l/r)
	 * @param quickTurnButton
	 *            switching from curvature drive, to tank drive
	 * @param turnSensitivity
	 *            severity of turn
	 */
	public CurvatureDriveStraightSystem(DriveOut<Percent> drive, PercentIn throttle, PercentIn wheel,
			DigitalIn quickTurnButton, double turnSensitivity, double spinSensitivity, AngleIn angle, double kP,
			DigitalIn driveStraight)
	{
		super(drive, throttle, wheel, quickTurnButton, turnSensitivity, spinSensitivity);
		this.kP = kP;
		this.angle = angle;
		this.driveStraight = driveStraight;

	}

	/**
	 * if throttle or wheel is changed, all commands are cleared
	 */
	@Override
	public void init()
	{
	}

	/**
	 * expects to be called every tick, updates values of throttle, wheel,
	 * quickTurnButton, and brakemode
	 */
	@Override
	public void update()
	{

		if (driveStraight.get())
		{
			drive.left().set(throttle.get() + (angle.get() * kP));
			drive.right().set(throttle.get() - (angle.get() * kP));
		} else
		{

			super.update();
		}

	}

	/**
	 * return key
	 */
	@Override
	public String getName()
	{
		return "Curvature Drive System";
	}

	/**
	 * add watchables for voltage being applied to the motors, and the quickturn
	 * button
	 */
	@Override
	public AddList<Watchable> getSubWatchables(AddList<Watchable> stem)
	{
		return stem.put(drive, quickTurnButton.getWatchable("quickTurnButton"));
	}

}
