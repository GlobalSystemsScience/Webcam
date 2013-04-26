//"How JMF is like your stereo system"

package webcam;

import java.awt.Component;
import java.awt.event.*;
import java.io.IOException;
import java.util.Vector;

import javax.media.*;
import javax.media.control.TrackControl;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class JMF extends JFrame implements ActionListener, ItemListener,
		WindowListener, ControllerListener {
	private static final long serialVersionUID = 3488554892880622958L;
	CaptureDeviceInfo videoCDI = null;
	String videoDeviceName = null;
	Player videoPlayer;
	Format videoFormat;
	Player dualPlayer;

	DataSource dataSource; // of the capture devices

	public JMF(String title) {
		super(title);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				dispose();
			}
		});
		JMenuBar mb = new JMenuBar();
		setJMenuBar(mb);
		JMenu menuConfigure = new JMenu("Configure");
		mb.add(menuConfigure);

		JMenu menuAction = new JMenu("Action");
		mb.add(menuAction);

		/* menu items for Configure */
		JMenuItem menuItemSetting = new JMenuItem("Capture Device");
		menuItemSetting.addActionListener(this);
		menuConfigure.add(menuItemSetting);

		/* menu items for Action */

		JMenuItem a1 = new JMenuItem("Capture");
		a1.addActionListener(this);
		menuAction.add(a1);
		JMenuItem a3 = new JMenuItem("Stop");
		a3.addActionListener(this);
		menuAction.add(a3);
		PlugInManager.addPlugIn(ColorSum.class.getName(),
				ColorSum.supportedFormats, ColorSum.supportedFormats,
				PlugInManager.CODEC);
	}

	public void actionPerformed(ActionEvent ae) {
		String command = ae.getActionCommand().toString();
		if (command.equals("Capture Device")) {
			registerDevices();
		} else if (command.equals("Capture")) {
			capture();
		} else if (command.equals("Stop")) {
			stop();
		}
	}

	void registerDevices() {
		@SuppressWarnings("unchecked")
		Vector<CaptureDeviceInfo> devices = CaptureDeviceManager
				.getDeviceList(null);
		CaptureDeviceInfo cdi;
		if (devices != null && devices.size() > 0) {
			int deviceCount = devices.size();
			Vector<CaptureDeviceInfo> videoDevices = new Vector<CaptureDeviceInfo>();

			Format[] formats;
			for (int i = 0; i < deviceCount; i++) {
				cdi = devices.elementAt(i);
				formats = cdi.getFormats();
				for (int j = 0; j < formats.length; j++) {
					if (formats[j] instanceof VideoFormat) {
						videoDevices.addElement(cdi);
						break;
					}
				}
			}
			videoCDI = videoDevices.elementAt(0);
		} // end if devices!=null && devices.size>0
		else {
			// no devices found or something bad happened.
		}
	}

	@SuppressWarnings("unchecked")
	synchronized void capture() {
		if (videoCDI == null)
			registerDevices();

		try {
			if (!(videoCDI == null)) {

				/*
				 * This works, but now we end up having 2 players
				 * 
				 * videoPlayer = Manager.createPlayer(videoCDI.getLocator());
				 * audioPlayer = Manager.createPlayer(audioCDI.getLocator());
				 * videoPlayer.addControllerListener(this); videoPlayer.start();
				 * audioPlayer.start();
				 */
				System.out.println("Creating data sources.");
				VideoFormat rgb = new VideoFormat(VideoFormat.RGB);
				DataSource ds = Manager.createDataSource(videoCDI.getLocator());
				Vector<String> cString = PlugInManager.getPlugInList(videoCDI
						.getFormats()[0], rgb, PlugInManager.CODEC);
				System.out.println(cString);
				final Codec codec = Class.forName((String) cString.get(0))
						.asSubclass(Codec.class).newInstance();
				System.out.println(codec);
				cString = PlugInManager.getPlugInList(rgb, rgb,
						PlugInManager.CODEC);
				ColorSum colorSum = null;
				for (String s : cString) {
					Class<?> c = Class.forName(s);
					if (ColorSum.class.isAssignableFrom(c)) {
						colorSum = (ColorSum) c.newInstance();
						Grapher g = new Grapher();
						colorSum.setGrapher(g);
						break;
					}
				}
				System.out.println(colorSum);
				ds = addPlayer(ds, codec, colorSum);
				dualPlayer = Manager.createPlayer(ds);
				dualPlayer.addControllerListener(this);
				dualPlayer.start();
			} else
				System.out.println("CDI not found.");
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	DataSource addPlayer(DataSource old, Codec... codec)
			throws NoProcessorException, IOException, InterruptedException {
		Processor player1 = Manager.createProcessor(old);
		player1.configure();
		final JMF jm = this;
		player1.addControllerListener(new ControllerListener() {
			@Override
			public void controllerUpdate(ControllerEvent event) {
				synchronized (jm) {
					jm.notify();
				}
			}

		});
		while (!(player1.getState() == Processor.Configured)) {
			System.out.println("Still here");
			wait();
		}
		for (TrackControl tc : (player1).getTrackControls()) {
			if (tc.getFormat() instanceof VideoFormat) {
				System.out.println("Something here 1");
				try {
					tc.setCodecChain(codec);
				} catch (UnsupportedPlugInException e) {
					throw new Error(e);
				} catch (NotConfiguredError e) {
					throw new Error(e);
				}
				break;
			}
		}
		player1.realize();
		while (!(player1.getState() == Processor.Realized)) {
			System.out.println("Still here");
			wait();
		}
		player1.start();
		return player1.getDataOutput();
	}

	void stop() {
		if (dualPlayer != null) {
			dualPlayer.stop();
			dualPlayer.deallocate();
		}
	}

	public synchronized void controllerUpdate(ControllerEvent event) {
		System.out.println(event.toString());

		if (event instanceof RealizeCompleteEvent) {
			Component comp;

			System.out.println("Adding visual component");
			if ((comp = dualPlayer.getVisualComponent()) != null)
				add("Center", comp);
			System.out.println("Adding control panel");
			if ((comp = dualPlayer.getControlPanelComponent()) != null)
				add("South", comp);
			validate();
		}
	}

	public void itemStateChanged(ItemEvent ie) {
	}

	public void windowActivated(WindowEvent we) {
	}

	public void windowClosed(WindowEvent we) {
	}

	public void windowClosing(WindowEvent we) {
	}

	public void windowDeactivated(WindowEvent we) {
	}

	public void windowDeiconified(WindowEvent we) {
	}

	public void windowIconified(WindowEvent we) {
	}

	public void windowOpened(WindowEvent we) {
	}

	public static void main(String[] argv) {
		JMF myFrame = new JMF("Java Media Framework Project");
		myFrame.setSize(300, 300);
		myFrame.setVisible(true);
	}

}