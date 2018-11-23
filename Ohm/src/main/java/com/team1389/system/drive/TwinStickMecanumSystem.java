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
	PercentIn leftStickY, leftStickX;
	PercentIn rightStickX;
	FourDriveOut<Percent> drive;

	private void setPower(double leftStickX, double leftStickY, double rightStickX, FourDriveOut<Percent> drive) {
		double r = Math.hypot(leftStickX, leftStickY);
		double robotAngle = Math.atan2(leftStickY, leftStickX) - Math.PI / 4;
		final double leftFront = r * Math.cos(robotAngle) + rightStickX;
		final double rightFront = r * Math.sin(robotAngle) - rightStickX;
		final double leftRear = r * Math.sin(robotAngle) + rightStickX;
		final double rightRear = r * Math.cos(robotAngle) - rightStickX;
		drive.set(new FourWheelSignal(leftFront, rightFront, leftRear, rightRear));
	}

	public TwinStickMecanumSystem(PercentIn leftStickY, PercentIn leftStickX, PercentIn rightStickX,
			FourDriveOut<Percent> drive) {
		this.leftStickY = leftStickY;
		this.leftStickX = leftStickX;
		this.rightStickX = rightStickX;
		this.drive = drive;
	}

	@Override
	public AddList<Watchable> getSubWatchables(AddList<Watchable> stem) {
		return stem.put(leftStickY.getWatchable("left stick y"), leftStickX.getWatchable("left stick x"),
				rightStickX.getWatchable("rightStickX"), drive);
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
		setPower(leftStickX.get(), leftStickY.get(), rightStickX.get(), drive);
	}

}
