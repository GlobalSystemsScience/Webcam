package webcam;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.io.IOException;
import java.util.Vector;

import javax.media.CannotRealizeException;
import javax.media.CaptureDeviceInfo;
import javax.media.Codec;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Format;
import javax.media.Manager;
import javax.media.NoDataSourceException;
import javax.media.NoProcessorException;
import javax.media.NotConfiguredError;
import javax.media.Player;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.UnsupportedPlugInException;
import javax.media.cdm.CaptureDeviceManager;
import javax.media.control.TrackControl;
import javax.media.format.VideoFormat;
import javax.media.pim.PlugInManager;
import javax.media.protocol.DataSource;
import javax.swing.JFrame;

public class Main implements ControllerListener {

	final private static int graphWidth = 500;
	private CaptureDeviceInfo videoCDI;
	private Player dualPlayer;
	private JFrame myFrame;
	private Grapher myGrapher;

	public Main() throws IOException, NoProcessorException,
			CannotRealizeException, NoDataSourceException,
			InstantiationException, IllegalAccessException {
		
		PlugInManager.addPlugIn(ColorSum.class.getName(),
				ColorSum.supportedFormats, ColorSum.supportedFormats,
				PlugInManager.CODEC);
	}

	public void registerDevices() {
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
			if (videoDevices.size() > 1) {
				System.out.println("Probable a problem");
			}
			if (videoDevices.size() != 0)
//				System.out.println(videoDevices.elementAt(0).toString());
				videoCDI = videoDevices.elementAt(0);
		} // end if devices!=null && devices.size>0
		else {
			// no devices found or something bad happened.
		}
	}

	@SuppressWarnings("unchecked")
	public void capture() {
		if (videoCDI == null) {
			registerDevices();
		}

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
						myGrapher = new Grapher();
						colorSum.setGrapher(myGrapher);
//						if (frame2 == null) {
//							frame2 = new JFrame();
//							frame2.setSize(graphWidth, graphWidth);
//							frame2.getContentPane().add(myGrapher.p);
//							frame2.setVisible(true);
//						}
						break;
					}
				}
				System.out.println(colorSum);
				ds = addPlayer(ds, codec, colorSum);
				dualPlayer = Manager.createPlayer(ds);
				dualPlayer.addControllerListener(this);
				dualPlayer.start();
			} else {
				System.out.println("CDI not found.");
			}
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	synchronized DataSource addPlayer(DataSource old, Codec... codec)
			throws NoProcessorException, IOException, InterruptedException {
		Processor player1 = Manager.createProcessor(old);
		player1.configure();
		player1.addControllerListener(new ControllerListener() {

			@Override
			public void controllerUpdate(ControllerEvent event) {
				synchronized (Main.this) {
					Main.this.notify();
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

	public static void main(String[] args) throws IOException,
			NoProcessorException, CannotRealizeException,
			NoDataSourceException, InstantiationException,
			IllegalAccessException {
		Main m = new Main();
		m.capture();
	}

	Image getImage() throws IOException {
		return null;
	}

	@Override
	public synchronized void controllerUpdate(ControllerEvent event) {
		System.out.println(event.toString());

		if (event instanceof RealizeCompleteEvent) {
			Component comp;
			if (myFrame == null) {
				myFrame = new JFrame();
				myFrame.setLayout(new BorderLayout());
			}
			System.out.println("Adding visual component");
			if ((comp = dualPlayer.getVisualComponent()) != null) {
				myFrame.getContentPane().add(comp, BorderLayout.CENTER);
			}
			System.out.println("Adding control panel");
			if ((comp = dualPlayer.getControlPanelComponent()) != null) {
				myFrame.getContentPane().add(comp, BorderLayout.SOUTH);
			}
		}
	}

	public Poly getGraphingComponent() {
		return myGrapher.p;
	}
	public static int getGraphWidth() {
		return graphWidth;
	}
}
