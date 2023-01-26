package microscenery.hardware;

import mmcorej.CMMCore;

import java.util.HashMap;
import java.util.Map;

public class PicardXYStage extends GenericXYStage {


    public PicardXYStage() {
        super();
    }

    public class PicardSubStage extends SubStage {
        public PicardSubStage(CMMCore core, String label, boolean isX) {
            super(core, label, isX);
        }

        @Override
        public void setVelocity(double velocity) throws IllegalArgumentException {
            if (velocity < 1 || velocity > 10 || Math.round(velocity) != velocity)
                throw new IllegalArgumentException("Velocity is not in 1..10 or is not an integer.");

            super.setVelocity(velocity);
        }

        @Override
        public void home() {
            try {
                core.home(label);
                if (iAmX)
                    destX = -1;
                else
                    destY = -1;
            } catch (Exception e) {
                ReportingUtils.Companion.logError(e, "Could not home X/Y stage.");
            }
        }

    }

    private static final Map<String, PicardXYStage> labelToInstanceMap = new HashMap<String, PicardXYStage>();

    public static Device getStage(CMMCore core, String label, boolean X) {
        PicardXYStage instance = labelToInstanceMap.get(label);

        if (instance == null) {
            instance = new PicardXYStage();
            labelToInstanceMap.put(label, instance);
        }

        if (X && instance.stageX == null)
            instance.stageX = instance.new PicardSubStage(core, label, true);
        else if (!X && instance.stageY == null)
            instance.stageY = instance.new PicardSubStage(core, label, false);

        return X ? instance.stageX : instance.stageY;
    }
}
