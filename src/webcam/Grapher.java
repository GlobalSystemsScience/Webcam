package webcam;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import javax.swing.JFrame;
import javax.swing.JLabel;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.Point;
import java.awt.Rectangle;

public class Grapher {

	private static final float LENIENCY = 0.05F;
	public static final int GRAPH_WIDTH = 1000;
	
	private int captureNum = 0;
	private int[] importantBytes;
	private long total;
	private long startTime;
	private boolean masking;
	private long TIME_EXECUTION = 30000;
	private String state;
	private ExecutorService tf = Executors.newCachedThreadPool();
	Poly p = new Poly(new Rectangle(1000, 1000));

	private class Printer implements Runnable {

		public long time;
		public long sum;
		public double topScale = 100;
		public double bottomScale = 85;
		Point point = new Point();
		
		

		@Override
		public void run() {
			if (total == 0)
				return;
			if (((time - startTime) * 1000 / 30000) < (GRAPH_WIDTH-10)) {
				point.x = 75 + (int) ((time - startTime) * 1000 / TIME_EXECUTION);
//				point.wait(TIME_EXECUTION-time);
//				point.move(arg0, arg1);
			} else {
				point.x = Main.getGraphWidth();
//				point.move(arg0, arg1);
			}
			//This percent is the percent down on the graph that it will go - based on the scale the user can change
			double percent = (((double)sum / total)*100 - bottomScale) * (100 / (topScale-bottomScale));

			point.y = 700 - (int) ((percent)*6);
			System.out.println("it's working: " + point.y + " " + percent);
			p.addUnscaledPoint(point);
			p.repaint();
		}

	}

	private Pool<Printer> printerPool = new Pool<Printer>(
			new Factory<Printer>() {

				@Override
				public Printer newObject() {
					return new Printer();
				}

				@Override
				public void reset(Printer t) {
				}

			});

	public void graph(byte[] data) {
		if (captureNum == 40) {
			captureNum++;
			final byte[] dataCopy = new byte[data.length];
			System.arraycopy(data, 0, dataCopy, 0, data.length);
			setMask(dataCopy);
//			if (state.equals("exhibition")) {
//				TIME_EXECUTION = ?
//			} else {
//				TIME_EXECUTION = 30000;
//			}
		} else if (captureNum > 20 && !masking) {
			captureNum++;
			plot(getSum(data));
		} else
			captureNum++;
	}

	private void plot(long sum) {
		if (startTime == 0L)
			return;
		Printer p = printerPool.get();
		p.sum = sum;
		p.time = System.currentTimeMillis();
		if (p.time - startTime <= 30000)				//EDIT THIS CODE!!
			tf.execute(p);
	}
	/*
	 * Takes string of data and returns the total weighted brightness of the 
	 * brightest pixels
	 */
	private long getSum(byte[] data) {
		if (startTime == 0L)
			return 0L;
		long sum = 0L;
		for (int i : importantBytes) {
			sum += (data[i] & 255) * 30 + (data[i] & 255) * 59
					+ (data[i] & 255) * 11;
		}
		return sum;
	}

	public void setMask(byte[] data) {
		ArrayList<Integer> importantBytes = new ArrayList<Integer>(data.length);
		int max = 0;
		for (int i = 0; i < data.length; i += 3) {
			int val = (data[i] & 255) * 30 + (data[i + 1] & 255) * 59		//finding the brightest pixel
					+ (data[i + 2] & 255) * 11;
			if (val > max)
				max = val;
		}
		total = 0;
		int comp = (int) (max - LENIENCY * 25500);
		for (int i = 0; i < data.length; i += 3) {
			int val = (data[i] & 255) * 30 + (data[i + 1] & 255) * 59
					+ (data[i + 2] & 255) * 11;							//Getting enough pixels to average it out a little
			if (val >= comp) {
				importantBytes.add(i);
				total += 25500;
			}
		}
		this.importantBytes = new int[importantBytes.size()];
		for (int i = 0; i < importantBytes.size(); i++) {
			this.importantBytes[i] = importantBytes.get(i);
		}
		this.getGraph().setVisible(true);

		startTime = System.currentTimeMillis();
	}
	
	public JFrame getGraph() {
		JFrame jf = new JFrame("Grapher");
		jf.setSize(Main.getGraphWidth(), Main.getGraphWidth());
		jf.getContentPane().add(p);

		JLabel graphTitle = new JLabel ("Brightness as a funtion of Time");
		graphTitle.setVerticalAlignment(JLabel.NORTH);
		graphTitle.setHorizontalAlignment(JLabel.CENTER);
		
		jf.add(graphTitle, BorderLayout.NORTH);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		return jf;
	}
}