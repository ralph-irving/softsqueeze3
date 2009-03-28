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

package org.titmuss.softsqueeze.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Util {

	/**
	 * Splits a string using the given delimiters. Returns an array of strings.
	 * This is equivalent to String.split("\s*[delim]\s*") in jdk1.4.
	 */
	public static String[] split(String str, String delim) {
		StringTokenizer tok = new StringTokenizer(str, delim);

		int n = tok.countTokens();
		String split[] = new String[n];

		for (int i = 0; tok.hasMoreTokens(); i++)
			split[i] = tok.nextToken().trim();

		return split;
	}

	/**
	 * Oh, for a decent xml library ;). This method returns a list of nodes
	 * matching very simple xml xpath expressions.
	 */
	public static List xmlSelectNodes(Node node, String xpathExpression) {
		List list = new LinkedList();

		String path[] = split(xpathExpression, "/");
		xmlSelectNodes0(list, node, path, 0);

		return list;
	}

	private static void xmlSelectNodes0(List list, Node node, String path[],
			int n) {
		if (n == path.length) {
			list.add(node);
			return;
		}

		if (path[n].equals("..")) {
			xmlSelectNodes0(list, node.getParentNode(), path, n + 1);
			return;
		}
		
		NodeList kids = node.getChildNodes();
		for (int i = 0; i < kids.getLength(); i++) {
			Node kid = kids.item(i);

			if (kid.getNodeName().equals(path[n]))
				xmlSelectNodes0(list, kid, path, n + 1);
		}
	}

	/**
	 * Select the first node found that matches the very simple xpath
	 * expression.
	 */
	public static Node xmlSelectSingleNode(Node node, String xpathExpression) {
		List list = xmlSelectNodes(node, xpathExpression);
		if (list.isEmpty())
			return null;
		return (Node) list.get(0);
	}

	/**
	 * Returns the text of the node.
	 */
	public static String xmlGetText(Node node) {
		NodeList kids = node.getChildNodes();
		for (int i = 0; i < kids.getLength(); i++) {
			Node kid = kids.item(i);

			if (kid.getNodeName().equals("#text"))
				return kid.getNodeValue();
			else if (kid.getNodeName().equals("#cdata-section"))
				return kid.getNodeValue();
		}
		return null;
	}

	/**
	 * Returns the textual representation after evaluating the xpathExpression.
	 */
	public static String xmlValueOf(Node node, String xpathExpression) {
		List nodes = xmlSelectNodes(node, xpathExpression);
		StringBuffer buf = new StringBuffer();
		for (Iterator i = nodes.iterator(); i.hasNext();) {
			Node n = (Node) i.next();
			String str = xmlGetText(n);
			if (str != null)
				buf.append(str);
		}
		return buf.toString();
	}
}