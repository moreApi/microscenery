package microscenery.hardware;

// TODO fix imports
//import ij.IJ;

import mmcorej.CMMCore;
import mmcorej.DeviceType;
import mmcorej.StrVector;
import mmcorej.TaggedImage;
import org.joml.Vector3d;

import java.util.EnumMap;
import java.util.Map;

public class SPIMSetup {
    /**
     * Logger for this application, will be instantiated upon first use.
     */
    public enum SPIMDevice {
        STAGE_X("X Stage"),
        STAGE_Y("Y Stage"),
        STAGE_Z("Z Stage"),
        STAGE_THETA("Angle"),
        LASER1("Laser"),
        LASER2("Laser (2)"),
        CAMERA1("Camera"),
        CAMERA2("Camera (2)"),
        SYNCHRONIZER("Synchronizer");

        private final String text;

        SPIMDevice(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    private final Map<SPIMDevice, Device> deviceMap;
    private final CMMCore core;

    public SPIMSetup(CMMCore core) {
        this.core = core;

        deviceMap = new EnumMap<SPIMDevice, Device>(SPIMDevice.class);
    }
// TODO fix imports
//	public void debugLog() {
//		IJ.log("SPIM Setup " + this.toString() + ":");
//		for(Map.Entry<SPIMDevice, Device> entr : deviceMap.entrySet())
//			IJ.log(" " + entr.getKey() + " => " + (entr.getValue() != null ? "\"" + entr.getValue().getLabel() + "\" (" + entr.getValue().getDeviceName() + " / " + entr.getValue().toString() + ")" : "None"));
//	}

    /*
     * Some methods which analyze the setup.
     */
    public boolean isConnected(SPIMDevice type) {
        if (deviceMap.get(type) != null) {
            try {
                return strVecContains(core.getLoadedDevicesOfType(deviceMap.get(type).getMMType()), deviceMap.get(type).getLabel());
            } catch (Throwable t) {
                ReportingUtils.Companion.logError(t, "SPIMAcquisition checking connection");
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean hasZStage() {
        return isConnected(SPIMDevice.STAGE_Z);
    }

    public boolean hasXYStage() {
        return isConnected(SPIMDevice.STAGE_X) && isConnected(SPIMDevice.STAGE_Y);
    }

    public boolean has3DStage() {
        return hasZStage() && hasXYStage();
    }

    public boolean hasAngle() {
        return isConnected(SPIMDevice.STAGE_THETA);
    }

    public boolean has4DStage() {
        return has3DStage() && hasAngle();
    }

    public boolean isMinimalMicroscope() {
        return hasZStage() && isConnected(SPIMDevice.CAMERA1);
    }

    public boolean is3DMicroscope() {
        return has3DStage() && isConnected(SPIMDevice.CAMERA1);
    }

    public boolean isMinimalSPIM() {
        return is3DMicroscope() && hasAngle() && isConnected(SPIMDevice.LASER1);
    }

    /*
     * Some generic getters. The class might not stay backed by an EnumMap, so
     * these may be more important in the future.
     */
    public Device getDevice(SPIMDevice device) {
        return deviceMap.get(device);
    }

    public void setDevice(SPIMDevice type, String label) {
        try {
            if (label == null || label.length() <= 0)
                deviceMap.put(type, null);
            else
                deviceMap.put(type, Device.createDevice(core, type, label));
        } catch (Exception e) {
            ReportingUtils.Companion.logError(e, "Trying to exchange " + (getDevice(type) != null ? getDevice(type).getLabel() : "(null)") + " with " + label);
        }
    }

    public Stage getXStage() {
        return (Stage) deviceMap.get(SPIMDevice.STAGE_X);
    }

    public Stage getYStage() {
        return (Stage) deviceMap.get(SPIMDevice.STAGE_Y);
    }

    public Stage getZStage() {
        return (Stage) deviceMap.get(SPIMDevice.STAGE_Z);
    }

    public Stage getThetaStage() {
        return (Stage) deviceMap.get(SPIMDevice.STAGE_THETA);
    }

    public Laser getLaser() {
        return (Laser) deviceMap.get(SPIMDevice.LASER1);
    }

    public Camera getCamera() {
        return (Camera) deviceMap.get(SPIMDevice.CAMERA1);
    }

    public Device getSynchronizer() {
        return deviceMap.get(SPIMDevice.SYNCHRONIZER);
    }

    public TaggedImage snapImage() {
        if (!core.getAutoShutter() && getLaser() != null)
            getLaser().setPoweredOn(true);

        TaggedImage ret = getCamera().snapImage();

        if (!core.getAutoShutter() && getLaser() != null)
            getLaser().setPoweredOn(false);

        return ret;
    }

    /*
     * Some convenience methods for positioning the setup's 4D stage.
     */

    /**
     * Navigates the stage to the specified quadruplet. Null parameters mean no
     * change.
     *
     * @param x New position of the X stage
     * @param y New position of the Y stage
     * @param z New position of the Z stage
     * @param t New position of the theta stage
     */
    public void setPosition(Double x, Double y, Double z, Double t) {
        if (!has3DStage())
            return;

        if (x != null)
            getXStage().setPosition(x);

        if (y != null)
            getYStage().setPosition(y);

        if (z != null)
            getZStage().setPosition(z);

        if (t != null)
            getThetaStage().setPosition(t);
    }

    /**
     * Reposition the stage to the specified coordinates and angle.
     *
     * @param xyz New translational position of the stage
     * @param t   New position of the theta stage
     */
    public void setPosition(Vector3d xyz, Double t) {
        setPosition(xyz.x(), xyz.y(), xyz.z(), t);
    }

    /**
     * Reposition the stage to the specified coordinates.
     *
     * @param xyz New translational position of the stage
     */
    public void setPosition(Vector3d xyz) {
        setPosition(xyz, null);
    }

    /**
     * Gets the position of the stage as a vector.
     *
     * @return Current stage position
     */
    public Vector3d getPosition() {
        if (!has3DStage())
            return new Vector3d();

        return new Vector3d(getXStage().getPosition(), getYStage().getPosition(), getZStage().getPosition());
    }

    public double getAngle() {
        return getThetaStage().getPosition();
    }

    public CMMCore getCore() {
        return core;
    }

    public static SPIMSetup createDefaultSetup(CMMCore core) {
        InitFactories.init();
        SPIMSetup setup = new SPIMSetup(core);

        try {
            for (SPIMDevice dev : SPIMDevice.values())
                setup.deviceMap.put(dev, setup.constructIfValid(dev, setup.getDefaultDeviceLabel(dev)));
        } catch (Exception e) {
            ReportingUtils.Companion.logError(e, "Couldn't build default setup.");
            return null;
        }

        return setup;
    }

    public String getDefaultDeviceLabel(SPIMDevice dev) throws Exception {
        switch (dev) {
            case STAGE_X:
            case STAGE_Y:
                return core.getXYStageDevice();

            case STAGE_Z:
                return core.getFocusDevice();

            case STAGE_THETA:
                // TODO: In my ideal stage setup (three unique linear stages) this
                // wouldn't work.
                // The X and Y stages would also be StageDevices. I haven't thought
                // of a workaround yet. :(
                return labelOfSecondary(DeviceType.StageDevice, core.getFocusDevice());

            case LASER1:
                return core.getShutterDevice();

            case LASER2:
                // TODO: This might not be exact -- Arduino might end up showing up
                // as a shutter.
                return labelOfSecondary(DeviceType.ShutterDevice, core.getShutterDevice());

            case CAMERA1:
                return core.getCameraDevice();

            case CAMERA2:
                return labelOfSecondary(DeviceType.CameraDevice, core.getCameraDevice());

            case SYNCHRONIZER:
                // wot
                return null;

            default:
                return null;
        }
    }

    private Device constructIfValid(SPIMDevice type, String label) throws Exception {
        if (label != null && label.length() > 0)
            return Device.createDevice(core, type, label);
        else
            return null;
    }

    private String labelOfSecondary(DeviceType mmtype, String except) throws Exception {
        String other = null;

        for (String s : core.getLoadedDevicesOfType(mmtype)) {
            if (!s.equals(except)) {
                if (other == null)
                    other = s;
                else
                    return null;
            }
        }

        return other;
    }

    private static boolean strVecContains(StrVector vec, String check) {
        for (String str : vec)
            if (str.equals(check))
                return true;

        return false;
    }
}