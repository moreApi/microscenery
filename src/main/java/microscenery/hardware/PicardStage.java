package microscenery.hardware;

import mmcorej.CMMCore;

public class PicardStage extends Stage {


	public PicardStage(CMMCore core, String label) {
		super(core, label);
	}

	@Override
	public void home() {
		// TODO: This is a workaround, since the MM API lacks a home() call for single-axis stages.
		setProperty("GoHome", 1);
		waitFor();
	}
}
