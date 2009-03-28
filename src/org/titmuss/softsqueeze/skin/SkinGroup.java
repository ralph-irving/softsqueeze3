/*
 *   SoftSqueeze Copyright (c) 2004 Richard Titmuss
 *
 *   This file is part of SoftSqueeze.
 *
 *   SoftSqueeze is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   SoftSqueeze is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with SoftSqueeze; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package org.titmuss.softsqueeze.skin;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class SkinGroup extends SkinComponent {
	private Color bgColor;

	private ArrayList children = new ArrayList();

	private BufferedImage background;

	private BufferedImage backlight;

	private BufferedImage mask;

	private BufferedImage roll;

	private BufferedImage down;

	private GraphicsConfiguration gc;
	
	
	public SkinGroup(Skin skin, Element e) {
		super(skin, e);

		bgColor = parseColorAttribute(e, "bgcolor", null);

		background = parseBufferedImageAttribute(e, "background", null);
		backlight = parseBufferedImageAttribute(e, "backlight", null);
		mask = parseBufferedImageAttribute(e, "mask", null);
		roll = parseBufferedImageAttribute(e, "roll", null);
		down = parseBufferedImageAttribute(e, "down", null);

	    // Create an image for drawing
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	    GraphicsDevice gs = ge.getDefaultScreenDevice();
	    gc = gs.getDefaultConfiguration();	    

		NodeList parts = e.getElementsByTagName("*");
		for (int j = 0; j < parts.getLength(); j++) {
			Element e2 = (Element) parts.item(j);
			children.add(e2);
		}
	}
	
	public void dispose() {
		if (mask != null) {
			mask.flush();
			mask = null;			
		}
		if (roll != null) {
			roll.flush();
			roll = null;			
		}
		if (down != null) {
			down.flush();
			down = null;			
		}	
	}

	public JComponent createComponent() {
		BackgroundPanel panel = new BackgroundPanel();

		panel.setLayout(new AbsoluteLayout());
		panel.setPreferredSize(new Dimension(width, height));
		if (bgColor != null)
			panel.setBackground(bgColor);
		if (background != null)
			panel.setBackgroundImage(background);

		if (mask != null) {
		    ActionDispatcher dispatcher = new ActionDispatcher(panel);
		    panel.addMouseMotionListener(dispatcher);
		    panel.addMouseWheelListener(dispatcher);
		    panel.addMouseListener(dispatcher);
		}

		for (Iterator i = children.iterator(); i.hasNext();) {
			Element e2 = (Element) i.next();

			String id = e2.getAttribute("id");
			SkinComponent comp = skin.getSkinComponent(id);
			if (comp == null) {
				logger.error("Unknown component: " + id);
				continue;
			}
			comp.addToPanel(panel, e2);
		}

		return panel;
	}

	/**
	 * Panel that includes a background image. I thought Swing would be able to
	 * do this, oh well!
	 */
	private class BackgroundPanel extends JComponent {
		private static final long serialVersionUID = 272225765159584276L;

		private Image image;
		private Image overlay;
		private Rectangle clip;
		
		public BackgroundPanel() {
			setOpaque(true);
			setFocusable(false);
			setBorder(new EmptyBorder(0, 0, 0, 0));
		}

		public void setBackground(Color background) {
			super.setBackground(background);
			setOpaque(true);
		}

		public void setBackgroundImage(Image image) {
			this.image = image;
			repaint();
		}
		
		public void setOverlayImage(Image newOverlay, Rectangle newClip) {
			if (newOverlay == null && overlay == null) {				
			}
			else if (newOverlay == null) {
				overlay = null;				
				repaint(clip);
				clip = null;
			}
			else if (overlay == null) {
				overlay = newOverlay;				
				repaint(newClip);
				clip = newClip;
			}
			else {
				overlay = newOverlay;
				Rectangle union = new Rectangle(clip);
				union = union.union(newClip);				
				repaint(union);
				clip = newClip;
			}
		}
		
		protected void paintComponent(Graphics g) {
			if (isOpaque()) {
				g.setColor(getBackground());
				g.fillRect(0, 0, getWidth(), getHeight());
			}

			logger.debug("gx clip " + g.getClip() + " ov clip " + clip);
			if (image != null) {
				g.drawImage(image, 0, 0, this);
			}
			if (overlay != null) {
				g.drawImage(overlay, clip.x,clip.y,clip.x+clip.width,clip.y+clip.height, 0,0,clip.width,clip.height, null);					
			}
		}
	}

	/*
	 * Layout manger that allows the components to stay just where they have
	 * been put. This is only needed to set the size of the panel.
	 */
	private class AbsoluteLayout implements LayoutManager {
		public void addLayoutComponent(String name, Component comp) {
		}

		public void removeLayoutComponent(Component comp) {
		}

		public void layoutContainer(Container parent) {
			Insets insets = parent.getInsets();
			Component clist[] = parent.getComponents();
			for (int i = 0; i < clist.length; i++) {
				Component c = clist[i];
				Point pt = c.getLocation();
				Dimension size = c.getPreferredSize();
				c.setBounds(pt.x + insets.left, pt.y + insets.top, size.width,
						size.height);
			}
		}

		public Dimension minimumLayoutSize(Container parent) {
			return preferredLayoutSize(parent);
		}

		public Dimension preferredLayoutSize(Container parent) {
			layoutContainer(parent);

			if (parent instanceof JComponent
					&& ((JComponent) parent).isPreferredSizeSet()) {
				Dimension size = parent.getPreferredSize();
				logger.debug("using preferred size " + size);

				Insets insets = parent.getInsets();
				return new Dimension(insets.left + (int) size.getWidth()
						+ insets.right, insets.top + (int) size.getHeight()
						+ insets.bottom);
			}

			int x = 0;
			int y = 0;

			Component clist[] = parent.getComponents();
			for (int i = 0; i < clist.length; i++) {
				Component c = clist[i];
				Rectangle bounds = c.getBounds();
				x = Math.max(x, (int) bounds.getMaxX());
				y = Math.max(y, (int) bounds.getMaxY());
			}

			Insets insets = parent.getInsets();
			return new Dimension(insets.left + x + insets.right, insets.top + y
					+ insets.bottom);
		}
	}

	HashMap downCache = new HashMap();
	HashMap rollCache = new HashMap();
	
	private class ImageTile {
		Image img;
		Rectangle area;
		
		private ImageTile(int rgb, BufferedImage source) {			
            rgb = 0xFF000000 | rgb;
            int minx = background.getWidth(null);
            int miny = background.getHeight(null);
            int maxx = 0;
            int maxy = 0;
            
    	    BufferedImage target = gc.createCompatibleImage(minx, miny, Transparency.BITMASK);

            for (int x=0; x<background.getWidth(); x++) {
                for (int y=0; y<background.getHeight(); y++) {
                	if (mask.getRGB(x,y) != rgb) {
                		target.setRGB(x, y, background.getRGB(x, y));
                		continue;
                	}
                	
            		target.setRGB(x, y, source.getRGB(x, y));

                	if (x < minx) minx = x;
                	else if (x > maxx) maxx = x;
                	if (y < miny) miny = y;
                	else if (y > maxy) maxy = y;
                }
            }
            
        	area = new Rectangle(minx, miny, maxx-minx, maxy-miny);
            logger.debug("rgb " + rgb + " source " + source + " area " + area);

    	    img = gc.createCompatibleImage(area.width, area.height, Transparency.BITMASK);
            Graphics g = img.getGraphics();
            g.drawImage(target, 0,0,area.width,area.height, area.x,area.y,area.x+area.width,area.y+area.height, null);
		}
	}
	
    private class ActionDispatcher implements MouseMotionListener, MouseWheelListener, MouseListener, ActionListener {
        private BackgroundPanel panel;
        
        private Action mouseOver = null;

        private Action mouseDrag = null;

        private Timer backlightTimer = null;
        
        ActionDispatcher(BackgroundPanel panel) {
            this.panel = panel;
        }
        
        private void updateImage(boolean isClicked) {
            if (mouseOver == null) {
            	panel.setOverlayImage(null, null);
                return;
            }

            if (backlight != null && isClicked) {
            	logger.debug("backlight on");            	
            	if (backlightTimer == null) {
            		backlightTimer = new Timer(10000, this);
            		panel.setBackgroundImage(backlight);
            	}
            	else {
                	backlightTimer.restart();            		
            	}
            }
            
            logger.debug("before overlay processing");
            if (isClicked && down != null) {
            	ImageTile tile = (ImageTile)downCache.get(mouseOver);
            	
            	if (tile == null) {
            		tile = new ImageTile(mouseOver.getMaskRGB(), down);
            		downCache.put(mouseOver, tile);
            	}
            	
            	panel.setOverlayImage(tile.img, tile.area);
            }
            else if (!isClicked && roll != null) {
            	ImageTile tile = (ImageTile)rollCache.get(mouseOver);
            	
            	if (tile == null) {
            		tile = new ImageTile(mouseOver.getMaskRGB(), roll);
            		rollCache.put(mouseOver, tile);
            	}
            	
            	panel.setOverlayImage(tile.img, tile.area);            	
            }
            else {
            	panel.setOverlayImage(null, null);
            	return;
            }
            logger.debug("after overlay processing");            
        }
        
        private Action getAction(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            
            if (x < 0 || x >= mask.getWidth() || y < 0 || y >= mask.getHeight())
                return null;
            
            int rgb = mask.getRGB(x, y) & 0xFFFFFF;
            return (Action) skin.getActionRGB(rgb);            
        }
        
        
        /* (non-Javadoc)
         * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
         */
        public void mouseDragged(MouseEvent e) {
            if (mouseDrag != null) {
                mouseDrag.mouseDragged(e);
                return;
            }
            
            Action action = getAction(e);
            if (action != null)
                action.mouseDragged(e);
            
            mouseDrag = action;
        }

        /* (non-Javadoc)
         * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
         */
        public void mouseMoved(MouseEvent e) {
            Action action = getAction(e);
            if (mouseOver == action)
                return;

            if (mouseOver != null)
                mouseOver.mouseExited(e);
            if (action != null)
                action.mouseEntered(e);
            
            mouseOver = action;
            updateImage(false);
        }


        /* (non-Javadoc)
         * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
         */
        public void mouseClicked(MouseEvent e) {
            switch (e.getButton()) {
            case MouseEvent.BUTTON1:
                Action action1 = getAction(e);
            	if (action1 != null)
            	    action1.mouseClicked(e);
            	break;
            	
            case MouseEvent.BUTTON2:
            case MouseEvent.BUTTON3:
                Action action2 = skin.getKnobAction();
            	if (action2 != null)
            	    action2.mouseClicked(e);
            	break;
            }            
        }


        /* (non-Javadoc)
         * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
         */
        public void mousePressed(MouseEvent e) {
            switch (e.getButton()) {
            case MouseEvent.BUTTON1:
                Action action = getAction(e);
            	if (action != null) {
            	    action.mousePressed(e);            
            	    updateImage(true);
            	}
            	break;
            	
            case MouseEvent.BUTTON2:
            case MouseEvent.BUTTON3:
                Action action2 = skin.getKnobAction();
            	if (action2 != null)
            	    action2.mousePressed(e);
            	break;
            }
        }


        /* (non-Javadoc)
         * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
         */
        public void mouseReleased(MouseEvent e) {
            mouseDrag = null;
            
            switch (e.getButton()) {
            case MouseEvent.BUTTON1:
                Action action = getAction(e);
            	if (action != null) {
            	    action.mouseReleased(e);            
            	    updateImage(false);
            	}
        	
            case MouseEvent.BUTTON2:
            case MouseEvent.BUTTON3:
                Action action2 = skin.getKnobAction();
            	if (action2 != null)
            	    action2.mouseReleased(e);
            	break;
            }
        }


        /* (non-Javadoc)
         * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
         */
        public void mouseEntered(MouseEvent e) {
            Action action = getAction(e);
            if (action != null)
                action.mouseEntered(e);

            mouseOver = action;
            updateImage(false);
        }


        /* (non-Javadoc)
         * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
         */
        public void mouseExited(MouseEvent e) {
           if (mouseOver != null)
                mouseOver.mouseExited(e);
            
            mouseOver = null;
            updateImage(false);
        }

        /* (non-Javadoc)
         * @see java.awt.event.MouseWheelListener#mouseWheelMoved(java.awt.event.MouseWheelEvent)
         */
        public void mouseWheelMoved(MouseWheelEvent e) {
            Action action2 = skin.getKnobAction();
        	if (action2 != null)
        	    action2.mouseWheelMoved(e);
        }

		public void actionPerformed(ActionEvent arg0) {
        	if (backlight != null) {
        		logger.debug("backlight off");
        		backlightTimer.stop();
        		backlightTimer = null;
        		panel.setBackgroundImage(background);
        	}
		}
    }
    
}

