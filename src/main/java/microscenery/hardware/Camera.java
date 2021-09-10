package microscenery.hardware;

import mmcorej.CMMCore;
import mmcorej.DeviceType;
import mmcorej.TaggedImage;

public class Camera extends Device {


	public Camera(CMMCore core, String label) {
		super(core, label);
	}

	public TaggedImage snapImage() {
		try {
			core.snapImage();
			return core.getTaggedImage();
		} catch (Exception e) {
			ReportingUtils.logError(e);
			return null;
		}
	}

	/**
	 * Sets the exposure time in milliseconds.
	 * 
	 * @param exposureTime Time to open camera's physical shutter in milliseconds.
	 */
	public void setExposure(double exposureTime) {
		try {
			core.setExposure(exposureTime);
		} catch (Exception e) {
			ReportingUtils.Companion.logError(e);
		}
	}

	/**
	 * Gets the exposure time in milliseconds.
	 * 
	 * @return Camera's current exposure time, in ms.
	 */
	public double getExposure() {
		try {
			return core.getExposure();
		} catch (Exception e) {
			ReportingUtils.Companion.logError(e);
			return -1;
		}
	}

	@Override
	public DeviceType getMMType() {
		return DeviceType.CameraDevice;
	}

}
