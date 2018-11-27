package com.team1389.system.drive;

import com.team1389.hardware.inputs.software.PercentIn;
import com.team1389.hardware.value_types.Percent;
import com.team1389.system.Subsystem;
import com.team1389.util.list.AddList;
import com.team1389.watch.Watchable;

/**
 * Mecanum Drive System for controller with two sticks. Still Untested.
 */
public class TwinStickMecanumSystem extends Subsystem {
	PercentIn leftStickY;
	PercentIn rightStickX;
	FourDriveOut<Percent> drive;

	// new alg is untested
	private void setPower(double leftStickY, double rightStickX, FourDriveOut<Percent> drive) {
		double forward = -leftStickY; // push joystick forward to go forward
		double right = rightStickX; // push joystick to the right to strafe right
		// tentative hack to get twist. Not sure if forward should be negative.
		double clockwise = Math.hypot(forward, right);

		double frontLeft = forward + clockwise + right;
		double frontRight = forward - clockwise - right;
		double rearLeft = forward + clockwise - right;
		double rearRight = forward - clockwise + right;
		// Finally, normalize the wheel speed commands
		// so that no wheel speed command exceeds magnitude of 1:
		double max = Math.abs(frontLeft);
		if (Math.abs(frontRight) > max)
			max = Math.abs(frontRight);
		if (Math.abs(rearLeft) > max)
			max = Math.abs(rearLeft);
		if (Math.abs(rearRight) > max)
			max = Math.abs(rearRight);
		if (max > 1) {
			frontLeft /= max;
			frontRight /= max;
			rearLeft /= max;
			rearRight /= max;
		}
		drive.set(new FourWheelSignal(frontLeft, frontRight, rearLeft, rearRight));

	}

	public TwinStickMecanumSystem(PercentIn leftStickY, PercentIn rightStickX, FourDriveOut<Percent> drive) {
		this.leftStickY = leftStickY;
		this.rightStickX = rightStickX;
		this.drive = drive;
	}

	@Override
	public AddList<Watchable> getSubWatchables(AddList<Watchable> stem) {
		return stem.put(leftStickY.getWatchable("left stick y"), rightStickX.getWatchable("rightStickX"), drive);
	}

	@Override
	public String getName() {
		return "Twin Stick Mecanum Drive";
	}

	@Override
	public void init() {

	}

	@Override
	public void update() {
		setPower(leftStickY.get(), rightStickX.get(), drive);
	}

}
