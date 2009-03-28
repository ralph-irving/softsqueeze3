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

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;


public class VisibilityColumnModel extends DefaultTableColumnModel {
    ArrayList allColumns = new ArrayList();
    ArrayList hiddenColumns = new ArrayList();
    
    
    /* (non-Javadoc)
     * @see javax.swing.table.TableColumnModel#addColumn(javax.swing.table.TableColumn)
     */
    public void addColumn(TableColumn aColumn) {
        super.addColumn(aColumn);
        allColumns.add(aColumn);
    }

    public void removeColumn(TableColumn aColumn) {
        super.removeColumn(aColumn);
        allColumns.remove(aColumn);
    }

    public Iterator getAllColumns() {
        return allColumns.iterator();
    }
    
    public TableColumn getAllColumn(Object identifier) {
        for (Iterator i=allColumns.iterator(); i.hasNext(); ) {
            TableColumn aColumn = (TableColumn) i.next();
            if (identifier.equals(aColumn.getIdentifier()))
                return aColumn;
        }
        throw new IllegalArgumentException("Identifier not found");
    }
    
    public boolean isVisible(TableColumn aColumn) {
        return !hiddenColumns.contains(aColumn);
    }
    
    public void setColumnVisible(TableColumn aColumn, boolean visible) {
        if (visible) {
            super.addColumn(aColumn);
            hiddenColumns.remove(aColumn);
        }
        else {
            super.removeColumn(aColumn);
            hiddenColumns.add(aColumn);
        }
    }
    
    public void setAllColumnsVisible(boolean visible) {
        for (Iterator i=allColumns.iterator(); i.hasNext(); ) {
            setColumnVisible((TableColumn)i.next(), visible);
        }
    }
}
