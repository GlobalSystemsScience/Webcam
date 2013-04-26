package webcam;

import java.awt.Choice;
import java.awt.event.*;
import java.util.Vector;
import javax.media.*;
import javax.media.format.*;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class CaptureDeviceDialog extends JDialog implements ActionListener,
		ItemListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	boolean configurationChanged = false;
	Vector devices;
	Vector videoDevices;
	Vector videoFormats;
	Choice videoDeviceCombo;
	Choice videoFormatCombo;

	public CaptureDeviceDialog(JFrame parent, String title, boolean mode) {
		super(parent, title, mode);
		init();
	}

	private void init() {
		setSize(450, 180);
		JPanel p = new JPanel();
		p.setLayout(null);

		JLabel l2 = new JLabel("Video Device(s)");
		JLabel l4 = new JLabel("Video Format(s)");
		videoDeviceCombo = new Choice();
		videoFormatCombo = new Choice();

		JButton OKbutton = new JButton("OK");
		JButton cancelButton = new JButton("Cancel");

		p.add(l2);
		l2.setBounds(5, 55, 100, 20);
		p.add(videoDeviceCombo);
		videoDeviceCombo.setBounds(115, 55, 300, 20);
		p.add(l4);
		l4.setBounds(5, 80, 100, 20);
		p.add(videoFormatCombo);
		videoFormatCombo.setBounds(115, 80, 300, 20);
		p.add(OKbutton);
		OKbutton.setBounds(280, 115, 60, 25);
		p.add(cancelButton);
		cancelButton.setBounds(355, 115, 60, 25);

		getContentPane().add(p);
		videoDeviceCombo.addItemListener(this);
		OKbutton.addActionListener(this);
		cancelButton.addActionListener(this);

		// get all the capture devices
		devices = CaptureDeviceManager.getDeviceList(null);
		CaptureDeviceInfo cdi;
		if (devices != null && devices.size() > 0) {
			int deviceCount = devices.size();
			videoDevices = new Vector();

			Format[] formats;
			for (int i = 0; i < deviceCount; i++) {
				cdi = (CaptureDeviceInfo) devices.elementAt(i);
				formats = cdi.getFormats();
				for (int j = 0; j < formats.length; j++) {
					if (formats[j] instanceof VideoFormat) {
						videoDevices.addElement(cdi);
						break;
					}
				}
			}

			// populate the choices for video
			for (int i = 0; i < videoDevices.size(); i++) {
				cdi = (CaptureDeviceInfo) videoDevices.elementAt(i);
				videoDeviceCombo.addItem(cdi.getName());
			}

			displayVideoFormats();

		} // end if devices!=null && devices.size>0
		else {
			// no devices found or something bad happened.
		}
	}

	void displayVideoFormats() {
		// get audio formats of the selected audio device and repopulate the
		// audio format combo
		CaptureDeviceInfo cdi;
		videoFormatCombo.removeAll();

		int i = videoDeviceCombo.getSelectedIndex();
		// i = -1 --> no selected index

		if (i != -1) {
			cdi = (CaptureDeviceInfo) videoDevices.elementAt(i);
			if (cdi != null) {
				Format[] formats = cdi.getFormats();
				videoFormats = new Vector();
				for (int j = 0; j < formats.length; j++) {
					videoFormatCombo.add(formats[j].toString());
					videoFormats.addElement(formats[j]);
				}
			}
		}
	}

	public CaptureDeviceInfo getVideoDevice() {
		CaptureDeviceInfo cdi = null;
		if (videoDeviceCombo != null) {
			int i = videoDeviceCombo.getSelectedIndex();
			cdi = (CaptureDeviceInfo) videoDevices.elementAt(i);
		}
		return cdi;
	}

	public Format getVideoFormat() {
		Format format = null;
		if (videoFormatCombo != null) {
			int i = videoFormatCombo.getSelectedIndex();
			format = (Format) videoFormats.elementAt(i);
		}
		return format;
	}

	public boolean hasConfigurationChanged() {
		return configurationChanged;
	}

	public void actionPerformed(ActionEvent ae) {
		String command = ae.getActionCommand().toString();
		if (command.equals("OK")) {
			configurationChanged = true;
		}
		dispose();
	}

	public void itemStateChanged(ItemEvent ie) {
		System.out.println(ie.getSource().toString());
		displayVideoFormats();
	}
}