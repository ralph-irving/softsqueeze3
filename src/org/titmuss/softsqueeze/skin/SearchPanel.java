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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.titmuss.softsqueeze.music.SongList;
import org.titmuss.softsqueeze.music.SongListTable;
import org.w3c.dom.Element;


/**
 * @author Richard Titmuss
 */
public class SearchPanel extends SkinComponent {
	
	protected Color fgcolor;

	protected Color bgcolor;

	protected Font font;

	private SongList songList;
	
	public SearchPanel(Skin skin, Element e) {
		super(skin, e);
		
		fgcolor = parseColorAttribute(e, "fgcolor", null);
		bgcolor = parseColorAttribute(e, "bgcolor", null);
		font = parseFontAttribute(e, "font", "fontsize", null);
	}
		
	public JComponent createComponent()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBackground(bgcolor);
		panel.setForeground(fgcolor);
		panel.setFont(font);
		
		JTextField searchField = new JTextField();
		searchField.setFont(font);
		
		SearchFieldListener listener = new SearchFieldListener();		
        searchField.getDocument().addDocumentListener(listener);        
        searchField.addActionListener(listener);

		
		songList = new SongList();		
		SongListTable table = new SongListTable(songList);
		
	    // FIXME dynamic columns from configuration, need to think how the
	    // table settings are stored in the configuration ...
        table.setAllColumnsVisible(false);
        table.setColumnVisible("Title", true);
        table.setColumnVisible("Album", true);
        table.setColumnVisible("Artist", true);
        table.setColumnVisible("Genre", true);
        table.setColumnVisible("Duration", true);
        table.setColumnVisible("play", true);
        table.setColumnVisible("add", true);

		
		panel.add(searchField, BorderLayout.NORTH);
		panel.add(table, BorderLayout.CENTER);		
		
		return panel;
	}
	
	private class SearchFieldListener implements ActionListener, DocumentListener {

        /* (non-Javadoc)
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        public void actionPerformed(ActionEvent e) {
            JTextField tf = (JTextField) e.getSource();
            songList.searchSongs(squeeze.getCLI(), "search:" + tf.getText());
        }

        /* (non-Javadoc)
         * @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
         */
        public void insertUpdate(DocumentEvent e) {
            try {
                Document d = e.getDocument();
                dynamicSearch(d.getText(0, d.getLength()));
            } catch (BadLocationException e1) {
            }            
        }

        /* (non-Javadoc)
         * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
         */
        public void removeUpdate(DocumentEvent e) {
            try {
                Document d = e.getDocument();
                dynamicSearch(d.getText(0, d.getLength()));
            } catch (BadLocationException e1) {
            }            
        }

        /* (non-Javadoc)
         * @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
         */
        public void changedUpdate(DocumentEvent e) {
            try {
                Document d = e.getDocument();
                dynamicSearch(d.getText(0, d.getLength()));
            } catch (BadLocationException e1) {
            }            
        }

        private void dynamicSearch(String str) {
            if (str.length() == 0)
                songList.clear();
            else if (str.length() > 2)
                songList.searchSongs(squeeze.getCLI(), "search:"+str);                
        }
	}	
}
