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
package org.titmuss.softsqueeze.net;

import java.util.ArrayList;
import java.util.Iterator;


public class CliParameterIterator implements Iterator {
    private Iterator iterator;
    private String next;
    
    CliParameterIterator(ArrayList parameters) {
        iterator = parameters.iterator();
    }
    
    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return iterator.hasNext();
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public Object next() {
        next = (String) iterator.next();
        return next;
    }

    /**
     * @return the next parameter.
     */
    public String nextParameter() {
        next = (String) iterator.next();
        return next;            
    }

    /**
     * @return the next tag.
     */
    public String nextTag() {
        next = (String) iterator.next();
        return next.substring(0, next.indexOf(':'));            
    }

    /**
     * @return the next value.
     */
    public String nextValue() {
        return next.substring(next.indexOf(':') + 1);            
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        iterator.remove();
    }        
}