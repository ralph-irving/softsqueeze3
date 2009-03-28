/*
 * Created on 04-Nov-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.titmuss.softsqueeze.skin;

import java.awt.AWTEvent;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import org.apache.log4j.Logger;
import org.titmuss.softsqueeze.util.Util;
import org.w3c.dom.Element;

/**
 * @author richard
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class Action extends SkinObject implements MouseMotionListener, MouseWheelListener, MouseListener, KeyListener {
    protected static final Logger logger = Logger.getLogger(Action.class);
    
    private int maskRGB;
    
    protected String script;
    
    protected String arg[];
    
    private String label;
    
    private String keybinding;
    
        
    protected Action(Skin skin, Element e) {
        super(skin, e);
        
	    maskRGB = parseHexAttribute(e, "maskrgb", 0);
	    label = parseStringAttribute(e, "label", null);
	    keybinding = parseStringAttribute(e, "keybinding", null);

	    script = parseStringAttribute(e, "script", null);
	    arg = Util.split(script, "(,)");	    
    }
    
    
    /**
     * @return Returns the maskRGB.
     */
    public int getMaskRGB() {
        return maskRGB;
    }
    /**
     * @return Returns the tooltip.
     */
    public String getLabel() {
        return label;
    }
    /**
     * @return Returns the keybinding.
     */
    public String getKeybinding() {
        return keybinding;
    }
    
    /**
     * @return true if this is a knob action
     */
    public boolean isKnobAction() {
        return false;
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent e) {
    }


    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e) {
    }


    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent e) {
    }


    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent e) {
    }


    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent e) {
    }


    /* (non-Javadoc)
     * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
     */
    public void mouseDragged(MouseEvent e) {
    }


    /* (non-Javadoc)
     * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
     */
    public void mouseMoved(MouseEvent e) {
    }


    /* (non-Javadoc)
     * @see java.awt.event.MouseWheelListener#mouseWheelMoved(java.awt.event.MouseWheelEvent)
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
    }    

    /* (non-Javadoc)
     * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
     */
    public void keyTyped(KeyEvent e) {
    }


    /* (non-Javadoc)
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    public void keyPressed(KeyEvent e) {
    }


    /* (non-Javadoc)
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    public void keyReleased(KeyEvent e) {
    }


    /**
     * Returns the window specified in arg[1] of the script, or the window
     * for AWTEvent e.
     *  
     * @param e
     * @return
     */
    protected Container getWindowForEvent(AWTEvent e) {
        if (arg.length > 1)
            return skin.getContainer(arg[1]);
        
        Container c = (Container)e.getSource();
        while (! (c instanceof java.awt.Window) && c.getParent() != null)
            c = c.getParent();
        
        return c;
    }    
}
