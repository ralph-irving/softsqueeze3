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

package org.titmuss.softsqueeze.music;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;


public class SongListTable extends JPanel implements TableModelListener, ListSelectionListener {
    private SongList songList;
    
    private JTable table;
    
    private JLabel line;
    
    private JLabel buttonAdd; 

    private JLabel buttonDelete; 
    
    private JLabel buttonDown; 
    
    private JLabel buttonPlay; 

    private JLabel buttonUp; 
    

    private VisibilityColumnModel columnModel; 

    public SongListTable(SongList songList) {
        super(new BorderLayout());
        this.songList = songList;
        
        songList.addTableModelListener(this);
        
        buttonAdd = _makeButton("/skin/slim/images/b_add.gif");
        buttonDelete = _makeButton("/skin/slim/images/b_delete.gif");
        buttonDown = _makeButton("/skin/slim/images/b_down.gif");
        buttonPlay = _makeButton("/skin/slim/images/b_play.gif");
        buttonUp = _makeButton("/skin/slim/images/b_up.gif");

        columnModel = new VisibilityColumnModel();        
        table = new JTable(songList, columnModel);
	    table.setPreferredScrollableViewportSize(new Dimension(500, 70));
	    table.createDefaultColumnsFromModel();

        table.setDefaultRenderer(SongList.SongListButton.class, new SongListButtonRenderer());
        table.addMouseListener(new PopupListener());
        table.addMouseListener(new JTableButtonMouseListener());
        table.getSelectionModel().addListSelectionListener(this);

        line = new JLabel();
        
        add(line, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }
    
    // FIXME move into skin
    private JLabel _makeButton(String name) {
        URL url = getClass().getResource(name);
        return new JLabel(new ImageIcon(url));        
    }

    
    public void setColumnVisible(Object identifier, boolean visible) {
        TableColumn aColumn = columnModel.getAllColumn(identifier);
        columnModel.setColumnVisible(aColumn, visible);
    }
    
    public void setAllColumnsVisible(boolean visible) {
        columnModel.setAllColumnsVisible(visible);
    }
    
    /* (non-Javadoc)
     * @see javax.swing.event.TableModelListener#tableChanged(javax.swing.event.TableModelEvent)
     */
    public void tableChanged(TableModelEvent e) {
        line.setText("All titles (" + songList.getRowCount() + ")");
    }

    /* (non-Javadoc)
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {
        line.setText("Selected titles (" + table.getSelectedRowCount() + ")");
    }

    
    class SongListButtonRenderer implements TableCellRenderer {
        public SongListButtonRenderer() {
        }
        
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            
            if (value == SongList.PLAY_BUTTON)
                return (Component) buttonPlay;

            if (value == SongList.ADD_BUTTON)
                return (Component) buttonAdd;

            if (value == SongList.DOWN_BUTTON)
                return (Component) buttonDown;

            if (value == SongList.UP_BUTTON)
                return (Component) buttonUp;

            if (value == SongList.DELETE_BUTTON)
                return (Component) buttonDelete;

            return null;
        }
    }
    
    class JTableButtonMouseListener implements MouseListener {
        
        public JTableButtonMouseListener() {
        }
        
        public void mouseClicked(MouseEvent e) {
            forwardEvent(e);
        }
        
        public void mouseEntered(MouseEvent e) {
        }
        
        public void mouseExited(MouseEvent e) {
        }
        
        public void mousePressed(MouseEvent e) {
        }
        
        public void mouseReleased(MouseEvent e) {
        }
        
        private void forwardEvent(MouseEvent e) {
            TableColumnModel columnModel = table.getColumnModel();
            int column = columnModel.getColumnIndexAtX(e.getX());
            int row    = e.getY() / table.getRowHeight();
            Object value;
            
            if(row >= table.getRowCount() || row < 0 ||
                    column >= table.getColumnCount() || column < 0)
                return;

            SongList songList = (SongList) table.getModel();
            songList.actionPerformed(row, column);
        }
    }
    
    class PopupListener extends MouseAdapter implements ItemListener {
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                JPopupMenu popup = new JPopupMenu();
                for (Iterator i=columnModel.getAllColumns(); i.hasNext(); ) {
                    TableColumn aColumn = (TableColumn) i.next();
                    boolean isVisible = columnModel.isVisible(aColumn);
                    JMenuItem item = new JCheckBoxMenuItem(aColumn.getIdentifier().toString(), isVisible); 
                    item.addItemListener(this);
                    popup.add(item);
                }

                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }

        /* (non-Javadoc)
         * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
         */
        public void itemStateChanged(ItemEvent e) {
            JMenuItem item = (JMenuItem) e.getItem();
            boolean visible = e.getStateChange() == ItemEvent.SELECTED;
            
            TableColumn aColumn = columnModel.getAllColumn(item.getText());
            columnModel.setColumnVisible(aColumn, visible);            
        }
    }

}
