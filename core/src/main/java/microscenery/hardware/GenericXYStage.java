package microscenery.hardware;

import mmcorej.CMMCore;
import mmcorej.DeviceType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GenericXYStage {


    protected double destX, destY;
    protected SubStage stageX, stageY;

    public GenericXYStage() {
        stageX = stageY = null;
        destX = destY = -1;
    }

    public class SubStage extends Stage {
        protected boolean iAmX;

        public SubStage(CMMCore core, String label, boolean isX) {
            super(core, label);

            iAmX = isX;
        }

        @Override
        public void setPosition(double pos) {
            try {
                if (iAmX) {
                    if (destY < 0) destY = core.getYPosition(label);
                    core.setXYPosition(label, pos, GenericXYStage.this.destY);
                    GenericXYStage.this.destX = pos;
                } else {
                    if (destX < 0) destX = core.getXPosition(label);
                    core.setXYPosition(label, GenericXYStage.this.destX, pos);
                    GenericXYStage.this.destY = pos;
                }
            } catch (Exception e) {
                ReportingUtils.Companion.logError(e, "Couldn't set " + (iAmX ? "X" : "Y") + " position on " + label);
            }
        }

        @Override
        public double getPosition() {
            try {
                if (iAmX)
                    return core.getXPosition(label);
                else
                    return core.getYPosition(label);
            } catch (Exception e) {
                ReportingUtils.Companion.logError(e, "Couldn't get " + (iAmX ? "X" : "Y") + " position on " + label);
                return 0;
            }
        }

        public double getStepSize() {
            if (hasProperty("StepSize")) {
                return getPropertyDouble("StepSize");
            } else {
                return 1.0;
            }
        }

        public double getMinPosition() {
            if (hasProperty("Min"))
                return getPropertyDouble("Min") * getStepSize();
            else
                return 0.0;
        }

        /**
         * Get the maximum possible position in um.
         *
         * @return max position in um
         */
        public double getMaxPosition() {
            if (hasProperty("Max")) {
                return getPropertyDouble("Max") * getStepSize();
            } else
                return 9000.0; // *** this is why you should implement your own stages.
        }

        @Override
        public boolean hasProperty(String name) {
            return super.hasProperty((iAmX ? "X-" : "Y-") + name);
        }

        @Override
        public String getProperty(String name) {
            return super.getProperty((iAmX ? "X-" : "Y-") + name);
        }

        @Override
        public void setProperty(String name, String value) {
            super.setProperty((iAmX ? "X-" : "Y-") + name, value);
        }

        @Override
        public Collection<String> getPropertyAllowedValues(String name) {
            return super.getPropertyAllowedValues((iAmX ? "X-" : "Y-") + name);
        }

        @Override
        public DeviceType getMMType() {
            return DeviceType.XYStageDevice;
        }
    }

    private static final Map<String, GenericXYStage> labelToInstanceMap = new HashMap<String, GenericXYStage>();

    public static Device getStage(CMMCore core, String label, boolean X) {
        GenericXYStage instance = labelToInstanceMap.get(label);

        if (instance == null) {
            instance = new GenericXYStage();
            labelToInstanceMap.put(label, instance);
        }

        if (X && instance.stageX == null)
            instance.stageX = instance.new SubStage(core, label, true);
        else if (!X && instance.stageY == null)
            instance.stageY = instance.new SubStage(core, label, false);

        return X ? instance.stageX : instance.stageY;
    }
}
