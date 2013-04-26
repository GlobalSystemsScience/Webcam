package webcam;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.JComponent;

public final class DrawOn extends JComponent implements MouseListener,
		MouseMotionListener {
	public static final int PEN_DRAW = 0;

	public static final int LINE_DRAW = 1;

	private static final long serialVersionUID = 1L;

	private LinkedList<Poly> polies = new LinkedList<Poly>();

	private Poly thisPoly;

	private Drawable dr;

	// private Rectangle drawBounds = null;

	private Color drawColor;

	private int drawType = PEN_DRAW;

	private boolean lineDown = false;

	private Point mouseCoord;

	private Point lastPoint;

	private int pressButton = MouseEvent.NOBUTTON;

	public DrawOn(Drawable dr, Color c) {
		this.dr = dr;
		dr.getComponent().addMouseListener(this);
		dr.getComponent().addMouseMotionListener(this);
		this.drawColor = c;
		setLayout(new GridLayout(1, 1));
		add(dr.getComponent());
	}

	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		dr.getComponent().paint(g);
		g2.setStroke(new BasicStroke(2));
		g2.setColor(drawColor);
		Iterator<Poly> iter = polies.iterator();
		Poly poly;
		while (iter.hasNext()) {
			poly = iter.next();
			poly.scale(dr.getDrawRectangle());
			poly.drawPoints(g);
		}
		if (thisPoly != null) {
			thisPoly.scale(dr.getDrawRectangle());
			if (drawType == LINE_DRAW && lineDown)
				thisPoly.drawPoints(g, mouseCoord);
			else
				thisPoly.drawPoints(g);
		}
	}

	public JComponent getComponent() {
		return dr.getComponent();
	}

	private void setPoly() {
		ListIterator<Poly> li = polies.listIterator();
		boolean precedes = false;
		Poly poly;
		while (li.hasNext()) {
			poly = li.next();
			if (precedes) {
				if (poly.lastX() < thisPoly.lastX()) {
					li.remove();
				} else {
					if (poly.firstX() < thisPoly.lastX())
						poly.removeBelow(thisPoly.lastX());
					li.previous();
					break;
				}
			} else {
				if (poly.firstX() > thisPoly.firstX()) {
					precedes = true;
					if (poly.firstX() > thisPoly.lastX()) {
						li.previous();
						break;
					} else if (poly.lastX() > thisPoly.lastX()) {
						poly.removeBelow(thisPoly.lastX());
						li.previous();
						break;
					} else
						li.remove();
				} else if (poly.lastX() > thisPoly.firstX()) {
					Poly[] splitUp = poly.split(thisPoly.firstX());
					li.remove();
					if (!splitUp[0].isEmpty())
						li.add(splitUp[0]);
					if (!splitUp[1].isEmpty())
						li.add(splitUp[1]);
					li.previous();
					precedes = true;
				}
			}
		}
		li.add(thisPoly);
		thisPoly = null;
	}

	public void mouseDragged(MouseEvent me) {
		if (isEnabled()) {
			if (thisPoly != null) {
				if (pressButton == MouseEvent.BUTTON1)
					if (drawType == PEN_DRAW) {
						Point p = me.getPoint();
						thisPoly.addPoint(p);
						if (Math.abs(p.x - lastPoint.x) > 1) {
							int dir = p.x > lastPoint.x ? 1 : -1;
							for (int x = lastPoint.x + 1; x != p.x; x += dir) {
								thisPoly.addPoint(new Point(x,
										(x - lastPoint.x) * (p.y - lastPoint.y)
												/ (p.x - lastPoint.x)
												+ lastPoint.y));
							}
						}
						lastPoint = p;
					} else if (drawType == LINE_DRAW) {
						mouseMoved(me);
					}
			}
			repaint();
		}
	}

	public void mouseMoved(MouseEvent me) {
		if (isEnabled()) {
			mouseCoord = me.getPoint();
			if (thisPoly != null)
				repaint();
		}
	}

	public void mouseClicked(MouseEvent me) {
	}

	public void mouseEntered(MouseEvent arg0) {
	}

	public void mouseExited(MouseEvent arg0) {
	}

	public void mousePressed(MouseEvent me) {
		if (isEnabled()) {
			pressButton = me.getButton();
			if (pressButton == MouseEvent.BUTTON1)
				if (drawType == PEN_DRAW) {
					if (thisPoly == null) {
						Point mousePoint = me.getPoint();
						Rectangle drawRectangle = dr.getDrawRectangle();
						if (drawRectangle.contains(mousePoint)) {
							thisPoly = new Poly(drawRectangle);
							lastPoint = mousePoint;
							thisPoly.addPoint(mousePoint);
						}
					}
					repaint();
				} else if (drawType == LINE_DRAW) {
					if (lineDown)
						if (me.getClickCount() > 1) {
							lineDown = false;
							setPoly();
						} else
							thisPoly.addPoint(me.getPoint());
					else {
						Rectangle drawRectangle = dr.getDrawRectangle();
						Point mousePoint = me.getPoint();
						if (drawRectangle.contains(mousePoint)) {
							thisPoly = new Poly(drawRectangle);
							lineDown = true;
							thisPoly.addPoint(mousePoint);
						}
					}
					repaint();
				}
		}
	}

	public void mouseReleased(MouseEvent me) {
		if (isEnabled()) {
			if (pressButton == MouseEvent.BUTTON1 && drawType == LINE_DRAW
					&& lineDown) {
				thisPoly.addPoint(me.getPoint());
				repaint();
			} else if (thisPoly != null && drawType == PEN_DRAW) {
				setPoly();
				repaint();
			}
			pressButton = MouseEvent.NOBUTTON;
		}
	}

	public void setDrawType(int drawType) {
		this.drawType = drawType;
		if (thisPoly != null)
			setPoly();
	}

	public void eraseAll() {
		thisPoly = null;
		polies.clear();
		lineDown = false;
		repaint();
	}
}
