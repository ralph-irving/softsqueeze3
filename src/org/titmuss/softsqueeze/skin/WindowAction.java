/*
 * Created on 04-Nov-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.titmuss.softsqueeze.skin;

import java.awt.Container;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.titmuss.softsqueeze.platform.Platform;
import org.w3c.dom.Element;

/*
 * Allow the application window to be moved around the screen
 */
class WindowAction extends Action {
	private Point orig = null;

	private boolean dragGroup = false;

	private HashMap windowpos = null;

	private int dx, dy;

	private final int SNAP = 15;

	
	protected WindowAction(Skin skin, Element e) {
	    super(skin, e);
	}

	public void mouseClicked(MouseEvent e) {
	    Container window = getWindowForEvent(e);

	    window.requestFocusInWindow();
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
	    Container window = getWindowForEvent(e);

	    if (Platform.JRE_1_5_PLUS) {
			orig = MouseInfo.getPointerInfo().getLocation();
		} else {
			orig = e.getPoint();
			dx = dy = 0;
		}

		dragGroup = !e.isControlDown() && !e.isMetaDown();

		windowpos = new HashMap();
		for (Iterator i = skin.containerIterator(); i.hasNext();) {
			Map.Entry map = (Map.Entry) i.next();
			Window frame = (Window) map.getValue();

			if (!dragGroup && frame != window)
				continue;

			windowpos.put(frame, frame.getLocation());
		}
	}

	public void mouseReleased(MouseEvent e) {
		orig = null;

	    Container window = getWindowForEvent(e);
		
		/* store window location */
		for (Iterator i = skin.containerIterator(); i.hasNext();) {
			Map.Entry map = (Map.Entry) i.next();
			String id = (String) map.getKey();
			Window frame = (Window) map.getValue();

			if (!dragGroup && frame != window)
				continue;

			skin.getSkinWindow(id).setPosition(frame.getX(), frame.getY());
		}
		windowpos = null;
	}

	public void mouseDragged(MouseEvent e) {
		if (orig == null)
			return;

	    Container window = getWindowForEvent(e);

	    if (SkinWindow.isFullscreen(window)) {
	    	return;	    	
	    }
	    
	    if (Platform.JRE_1_5_PLUS) {
			Point p = MouseInfo.getPointerInfo().getLocation();
			dx = (int) (p.getX() - orig.getX());
			dy = (int) (p.getY() - orig.getY());
		} else {
			Point p = e.getPoint();
			dx += (int) (p.getX() - orig.getX());
			dy += (int) (p.getY() - orig.getY());
		}

		/*
		 * first snap any window in the drag to the screen edges
		 */
		for (Iterator i = skin.containerIterator(); i.hasNext();) {
			Map.Entry map = (Map.Entry) i.next();
			Window frame = (Window) map.getValue();

			if (!dragGroup && frame != window)
				continue;

			Point fp = (Point) windowpos.get(frame);
			Point xy = new Point(fp);
			xy.translate(dx, dy);
			snapScreen(xy, frame);
			dx = (int) (xy.getX() - fp.getX());
			dy = (int) (xy.getY() - fp.getY());
		}

		/*
		 * then drag the windows
		 */
		for (Iterator i = skin.containerIterator(); i.hasNext();) {
			Map.Entry map = (Map.Entry) i.next();
			Window frame = (Window) map.getValue();

			if (!dragGroup && frame != window)
				continue;

			Point xy = new Point((Point) windowpos.get(frame));
			xy.translate(dx, dy);
			if (!dragGroup)
				snapWindow(xy, window);

			frame.setLocation(xy);
		}
	}

	public void mouseMoved(MouseEvent e) {
	}

	private void snapWindow(Point xy, Container frame) {
		int top = (int) xy.getY();
		int left = (int) xy.getX();
		int bottom = top + frame.getHeight();
		int right = left + frame.getWidth();

		/* snap to other windows */
		for (Iterator i = skin.containerIterator(); i.hasNext();) {
			Map.Entry map = (Map.Entry) i.next();
			Window f = (Window) map.getValue();

			if (f == frame || !(f.isShowing()))
				continue;

			/* top -> bottom */
			Point fxy = f.getLocation();
			fxy.translate(0, f.getHeight());
			if (Math.abs(top - fxy.getY()) < SNAP)
				xy.setLocation(xy.getX(), fxy.getY());

			/* top -> top */
			fxy = f.getLocation();
			if (Math.abs(top - fxy.getY()) < SNAP)
				xy.setLocation(xy.getX(), fxy.getY());

			/* bottom -> top */
			fxy = f.getLocation();
			if (Math.abs(bottom - fxy.getY()) < SNAP)
				xy.setLocation(xy.getX(), fxy.getY() - frame.getHeight());

			/* bottom -> bottom */
			fxy = f.getLocation();
			fxy.translate(0, f.getHeight());
			if (Math.abs(bottom - fxy.getY()) < SNAP)
				xy.setLocation(xy.getX(), fxy.getY() - frame.getHeight());

			/* left -> right */
			fxy = f.getLocation();
			fxy.translate(f.getWidth(), 0);
			if (Math.abs(left - fxy.getX()) < SNAP)
				xy.setLocation(fxy.getX(), xy.getY());

			/* left -> left */
			fxy = f.getLocation();
			if (Math.abs(left - fxy.getX()) < SNAP)
				xy.setLocation(fxy.getX(), xy.getY());

			/* right -> left */
			fxy = f.getLocation();
			fxy.translate(0, 0);
			if (Math.abs(right - fxy.getX()) < SNAP)
				xy.setLocation(fxy.getX() - frame.getWidth(), xy.getY());

			/* right -> right */
			fxy = f.getLocation();
			fxy.translate(f.getWidth(), 0);
			if (Math.abs(right - fxy.getX()) < SNAP)
				xy.setLocation(fxy.getX() - frame.getWidth(), xy.getY());

			/* horiz centers */
			int hmid = left + frame.getWidth() / 2;
			fxy = f.getLocation();
			fxy.translate(f.getWidth() / 2, 0);
			if (Math.abs(hmid - fxy.getX()) < SNAP)
				xy
						.setLocation(fxy.getX() - frame.getWidth() / 2, xy
								.getY());

			/* vert centers */
			int vmid = top + frame.getHeight() / 2;
			fxy = f.getLocation();
			fxy.translate(0, f.getHeight() / 2);
			if (Math.abs(vmid - fxy.getY()) < SNAP)
				xy.setLocation(xy.getX(), fxy.getY() - frame.getHeight()
						/ 2);
		}
	}

	private void snapScreen(Point xy, Window frame) {
		int top = (int) xy.getY();
		int left = (int) xy.getX();
		int bottom = top + frame.getHeight();
		int right = left + frame.getWidth();

		/* snap to screen(s) */
		GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		for (int i = 0; i < gs.length; i++) {
			Rectangle bounds = gs[i].getDefaultConfiguration().getBounds();

			logger.debug("screen " + i + ": " + bounds);

			/* top -> top */
			if (Math.abs(top - bounds.getY()) < SNAP)
				xy.setLocation(xy.getX(), bounds.getY());

			/* bottom -> bottom */
			if (Math.abs(bottom - bounds.getY() - bounds.getHeight()) < SNAP)
				xy.setLocation(xy.getX(), bounds.getY()
						+ bounds.getHeight() - frame.getHeight());

			/* left -> left */
			if (Math.abs(left - bounds.getX()) < SNAP)
				xy.setLocation(bounds.getX(), xy.getY());

			/* right -> right */
			if (Math.abs(right - (bounds.getX() + bounds.getWidth())) < SNAP)
				xy.setLocation(bounds.getX() + bounds.getWidth()
						- frame.getWidth(), xy.getY());
		}
	}
}

