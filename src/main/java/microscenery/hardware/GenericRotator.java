package microscenery.hardware;

import mmcorej.CMMCore;

import java.util.Arrays;
import java.util.Collection;

public class GenericRotator extends Stage {


	public GenericRotator(CMMCore core, String label) {
		super(core, label);
	}

	@Override
	public double getVelocity() {
		return 1.0D;
	}

	@Override
	public void setVelocity(double velocity) throws IllegalArgumentException {
		if (velocity != 1.0D)
			throw new IllegalArgumentException("Invalid rotator velocity " + velocity);

		// nop
	}

	@Override
	public Collection<Double> getAllowedVelocities() {
		return Arrays.asList(1.0D);
	}

	@Override
	public double getMinPosition() {
		return -180.0D;
	}

	@Override
	public double getMaxPosition() {
		return +180.0D;
	}
}
