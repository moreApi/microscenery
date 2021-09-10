package microscenery.hardware;

import mmcorej.CMMCore;

public class CoherentObis extends Laser {


    public CoherentObis(CMMCore core, String label) {
        super(core, label);
    }

    @Override
    public void setPower(double power) { setProperty("PowerSetpoint", power * 1000); }

    @Override
    public double getPower() { return getPropertyDouble("PowerSetpoint") / 1000.0; }

    @Override
    public double getMinPower() {
        return getPropertyDouble("Minimum Laser Power");
    }

    @Override
    public double getMaxPower() {
        return getPropertyDouble("Maximum Laser Power");
    }

}
