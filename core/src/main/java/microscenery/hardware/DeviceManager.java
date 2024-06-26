package microscenery.hardware;

/*
import org.micromanager.utils.ReportingUtils;
import spim.gui.util.Layout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;

public class DeviceManager extends JPanel implements ItemListener, EventListener {
	private SPIMSetup setup;
	private JFrame display;
	public DeviceManager(SPIMSetup setup) {
		this.setup = setup;

		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		JPanel stages = Layout.titled( "Stages", new JPanel( new GridLayout( 4, 2, 0, 2 ) ) );
		Layout.addAll( stages, labelCombo( SPIMSetup.SPIMDevice.STAGE_X ) );
		Layout.addAll( stages, labelCombo( SPIMSetup.SPIMDevice.STAGE_Y ) );
		Layout.addAll( stages, labelCombo( SPIMSetup.SPIMDevice.STAGE_Z ) );
		Layout.addAll( stages, labelCombo( SPIMSetup.SPIMDevice.STAGE_THETA ) );
		add(stages);

		JPanel illum = Layout.titled( "Illumination/Detection", new JPanel( new GridLayout( 5, 2, 0, 2 ) ) );
		Layout.addAll( illum, labelCombo( SPIMSetup.SPIMDevice.LASER1 ) );
		Layout.addAll( illum, labelCombo( SPIMSetup.SPIMDevice.LASER2 ) );
		Layout.addAll( illum, labelCombo( SPIMSetup.SPIMDevice.CAMERA1 ) );
		Layout.addAll( illum, labelCombo( SPIMSetup.SPIMDevice.CAMERA2 ) );
		Layout.addAll( illum, labelCombo( SPIMSetup.SPIMDevice.SYNCHRONIZER ) );
		add(illum);
	}

	public void setVisible(boolean show) {
		if (display == null) {
			display = new JFrame("Device Manager");
			display.add(this);
			display.pack();
		}

		display.setVisible(show);
	}

	private JComponent[] labelCombo(SPIMDevice type) {
		Vector<String> devices = new Vector<String>(Arrays.asList(setup.getCore().getLoadedDevices().toArray()));
		Set<String> names = Device.getKnownDeviceNames(type);

		Iterator<String> iter = devices.iterator();
		while (iter.hasNext())
			try {
				if (!names.contains(setup.getCore().getDeviceName(iter.next())))
					iter.remove();
			} catch (Throwable t) {
				ReportingUtils.Companion.logError(t);
			}

		try {
			String defaultDevice = setup.getDefaultDeviceLabel(type);

			if(defaultDevice != null && !defaultDevice.isEmpty() && !devices.contains(defaultDevice))
				devices.add(defaultDevice);
		} catch (Throwable t) {
			ReportingUtils.Companion.logError(t);
		}

		devices.add("(none)");

		JComboBox combo = new JComboBox(devices);
		combo.setName(type.toString());
		combo.setSelectedItem(setup.getDevice(type) != null ? setup.getDevice(type).getLabel() : "(none)");
		combo.addItemListener(this);

		return new JComponent[] { new JLabel(type.getText()), combo };
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (!(ie.getSource() instanceof JComboBox))
			return; // what

		JComboBox src = (JComboBox) ie.getSource();
		SPIMDevice type = SPIMDevice.valueOf( src.getName() );
		String selectedLabel = src.getSelectedItem().toString();
		
		if(selectedLabel.equals("(none)"))
			selectedLabel = null;

		setup.setDevice(type, selectedLabel);
	}
}*/
