package webcam;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.swing.JComponent;

final class Poly extends JComponent {

	private static final long serialVersionUID = 6145481308875958051L;

	private LinkedList<Point> points;

	private ListIterator<Point> iter;

	private final Rectangle initialBounds;

	private Rectangle nowBounds;

	Poly(Rectangle bounds) {
		this(new LinkedList<Point>(), bounds, bounds);
	}

	private Poly(LinkedList<Point> points, Rectangle initialBounds,
			Rectangle nowBounds) {
		this.points = points;
		this.initialBounds = initialBounds;
		this.nowBounds = nowBounds;
	}

	void drawPoints(Graphics g) {
		if (points.size() > 1) {
			Iterator<Point> iter = points.iterator();
			Point lastPoint = iter.next();
			Point point;
			while (iter.hasNext()) {
				point = iter.next();
				g.drawLine(scaleOutX(lastPoint.x), scaleOutY(lastPoint.y),
						scaleOutX(point.x), scaleOutY(point.y));
				lastPoint = point;
			}
		}
	}

	@SuppressWarnings("unchecked")
	Poly[] split(int x) {
		Poly[] p = {
				new Poly((LinkedList<Point>) points.clone(), initialBounds,
						nowBounds),
				new Poly((LinkedList<Point>) points.clone(), initialBounds,
						nowBounds) };
		p[0].removeAbove(x);
		p[1].removeBelow(x);
		return p;
	}

	void removeBelow(int x) {
		removeBelowUnscaled(scaleInX(x));
	}

	private void removeAbove(int x) {
		removeAboveUnscaled(scaleInX(x));
	}

	private void removeBelowUnscaled(int x) {
		Point p = null;
		while (points.size() > 0 && firstUnscaledX() < x)
			p = points.removeFirst();
		iter = points.listIterator();
		if (p != null && p.x != x) {
			Point firstPoint = points.getFirst();
			addUnscaledPoint(new Point(x, (x - firstPoint.x)
					* (p.y - firstPoint.y) / (p.x - firstPoint.x)
					+ firstPoint.y));
		}
	}

	private void removeAboveUnscaled(int x) {
		Point p = null;
		while (points.size() > 0 && lastUnscaledX() > x)
			p = points.removeLast();
		if (points.size() == 0)
			return;
		iter = points.listIterator(points.size());
		if (p != null && p.x != x) {
			Point lastPoint = points.getLast();
			addUnscaledPoint(new Point(x, (x - lastPoint.x)
					* (p.y - lastPoint.y) / (p.x - lastPoint.x) + lastPoint.y));
		}
	}

	Point addUnscaledPoint(Point p) {
		if (points.size() == 0) {
			points.add(p);
			iter = points.listIterator(1);
			iter.previous();
		} else if (p.x > lastUnscaledX()) {
			points.addLast(p);
			iter = points.listIterator(points.size());
			iter.previous();
		} else if (p.x < firstUnscaledX()) {
			points.addFirst(p);
			iter = points.listIterator(1);
			iter.previous();
		} else {
			Point point;
			if (iter.hasNext()) {
				point = iter.next();
			} else {
				iter.previous();
				point = iter.next();
			}
			if (point.x < p.x) {
				while (iter.hasNext()) {
					point = iter.next();
					if (point.x > p.x) {
						iter.previous();
						iter.add(p);
						iter.previous();
						return null;
					} else if (point.x == p.x) {
						iter.remove();
						iter.add(p);
						iter.previous();
						return point;
					}
				}
			} else if (point.x > p.x) {
				iter.previous();
				while (iter.hasPrevious()) {
					point = iter.previous();
					if (point.x < p.x) {
						iter.next();
						iter.add(p);
						iter.previous();
						return null;
					} else if (point.x == p.x) {
						iter.remove();
						iter.add(p);
						iter.previous();
						return point;
					}
				}
			} else {
				iter.remove();
				iter.add(p);
				iter.previous();
				return point;
			}
		}
		return null;
	}

	private int lastUnscaledX() {
		return points.getLast().x;
	}

	int lastX() {
		return scaleOutX(lastUnscaledX());
	}

	private int firstUnscaledX() {
		return points.getFirst().x;
	}

	int firstX() {
		return scaleOutX(firstUnscaledX());
	}

	void drawPoints(Graphics g, Point mouseCoord) {
		Point removed = addUnscaledPoint(scaleInPoint(mouseCoord));
		drawPoints(g);
		iter.remove();
		if (removed != null)
			addUnscaledPoint(removed);
	}

	void addPoint(Point point) {
		if (nowBounds.contains(point))
			addUnscaledPoint(scaleInPoint(point));
	}

	private Point scaleInPoint(Point point) {
		return new Point(scaleInX(point.x), scaleInY(point.y));
	}

	private int scaleOutX(int x) {
		if (nowBounds.equals(initialBounds))
			return x;
		return (x - initialBounds.x) * nowBounds.width / initialBounds.width
				+ nowBounds.x;
	}

	private int scaleOutY(int y) {
		if (nowBounds.equals(initialBounds))
			return y;
		return (y - initialBounds.y) * nowBounds.height / initialBounds.height
				+ nowBounds.y;
	}

	private int scaleInX(int x) {
		if (nowBounds.equals(initialBounds))
			return x;
		return (x - nowBounds.x) * initialBounds.width / nowBounds.width
				+ initialBounds.x;
	}

	private int scaleInY(int y) {
		if (nowBounds.equals(initialBounds))
			return y;
		return (y - nowBounds.y) * initialBounds.height / nowBounds.height
				+ initialBounds.y;
	}

	void scale(Rectangle newBounds) {
		nowBounds = newBounds;
	}

	public boolean isEmpty() {
		return points.size() == 0;
	}

	@Override
	public void paintComponent(Graphics g) {
		Dimension d = getSize();
		if (d.width != nowBounds.width || d.height != nowBounds.height) {
			scale(new Rectangle(d.width, d.height));
		}
		Point lastPoint = null;
		for (Point p : points) {
			if (lastPoint != null) {
				g.drawLine(scaleOutX(lastPoint.x), scaleOutY(lastPoint.y),
						scaleOutX(p.x), scaleOutY(p.y));
			}
			lastPoint = p;
		}
	}
}
