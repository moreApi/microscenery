package microscenery.hardware;

import mmcorej.CMMCore;

import java.util.HashMap;

import static microscenery.hardware.Device.installFactory;

public class InitFactories {
    public static void init() {

        for (SPIMSetup.SPIMDevice type : SPIMSetup.SPIMDevice.values())
            Device.factoryMap.put(type, new HashMap<String, Device.Factory>());

        installFactory(new Device.Factory() {
            @Override
            public Device manufacture(CMMCore core, String label) {
                return new Camera(core, label);
            }
        }, "*", SPIMSetup.SPIMDevice.CAMERA1, SPIMSetup.SPIMDevice.CAMERA2);


        installFactory(new Device.Factory() {
            @Override
            public Device manufacture(CMMCore core, String label) {
                return new Cobolt(core, label);
            }
        }, "Cobolt", SPIMSetup.SPIMDevice.LASER1, SPIMSetup.SPIMDevice.LASER2);

        installFactory(new Device.Factory() {
            @Override
            public Device manufacture(CMMCore core, String label) {
                return new CoherentCube(core, label);
            }
        }, "CoherentCube", SPIMSetup.SPIMDevice.LASER1, SPIMSetup.SPIMDevice.LASER2);

        installFactory(new Device.Factory() {
            @Override
            public Device manufacture(CMMCore core, String label) {
                return new CoherentObis(core, label);
            }
        }, "CoherentObis", SPIMSetup.SPIMDevice.LASER1, SPIMSetup.SPIMDevice.LASER2);

        installFactory(new Device.Factory() {
            @Override
            public Device manufacture(CMMCore core, String label) {
                return new GenericRotator(core, label);
            }
        }, "*", SPIMSetup.SPIMDevice.STAGE_THETA);

        Device.Factory factX = new Device.Factory() {
            @Override
            public Device manufacture(CMMCore core, String label) {
                return PicardXYStage.getStage(core, label, true);
            }
        };

        Device.Factory factY = new Device.Factory() {
            @Override
            public Device manufacture(CMMCore core, String label) {
                return PicardXYStage.getStage(core, label, false);
            }
        };

        installFactory(factX, "*", SPIMSetup.SPIMDevice.STAGE_X);
        installFactory(factY, "*", SPIMSetup.SPIMDevice.STAGE_Y);

        installFactory(new Device.Factory() {
            @Override
            public Device manufacture(CMMCore core, String label) {
                return new Laser(core, label);
            }
        }, "*", SPIMSetup.SPIMDevice.LASER1, SPIMSetup.SPIMDevice.LASER2);
        /*
         * Every new device implementation needs to define a Factory to create it.
         * This lets you implement new devices with only the addition of a single
         * file. This is done using Device.installFactory, and should be inside a
         * static initializer block like below.
         *
         * The 'manufacture' method is where you make a new instance of your device.
         * If the class needs any special setup beyond the constructor, be sure to
         * do that before returning. Here, we only need to make a new PicardStage.
         *
         * The string following the Factory is the device name (*not* label!) that
         * this factory can create. After that comes the SPIMDevices that this class
         * can control. You can list as many as you want; as seen here, the X, Y,
         * and Z stages could each be a linear stage.
         */

        installFactory(new Device.Factory() {
            public Device manufacture(CMMCore core, String label) {
                return new PicardStage(core, label);
            }
        }, "Picard Z Stage", SPIMSetup.SPIMDevice.STAGE_X, SPIMSetup.SPIMDevice.STAGE_Y, SPIMSetup.SPIMDevice.STAGE_Z);

        installFactory(new Device.Factory() {
            @Override
            public Device manufacture(CMMCore core, String label) {
                return new PicardTwister(core, label);
            }
        }, "Picard Twister", SPIMSetup.SPIMDevice.STAGE_THETA);

        Device.Factory factPX = new Device.Factory() {
            @Override
            public Device manufacture(CMMCore core, String label) {
                return PicardXYStage.getStage(core, label, true);
            }
        };

        Device.Factory factPY = new Device.Factory() {
            @Override
            public Device manufacture(CMMCore core, String label) {
                return PicardXYStage.getStage(core, label, false);
            }
        };

        installFactory(factPX, "Picard XY Stage", SPIMSetup.SPIMDevice.STAGE_X);
        installFactory(factPY, "Picard XY Stage", SPIMSetup.SPIMDevice.STAGE_Y);

        installFactory(new Device.Factory() {
            public Device manufacture(CMMCore core, String label) {
                return new Stage(core, label);
            }
        }, "*", SPIMSetup.SPIMDevice.STAGE_Z);
    }
}

