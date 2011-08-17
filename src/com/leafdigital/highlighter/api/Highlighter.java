/*
This file is part of leafdigital leafChat.

leafChat is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

leafChat is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with leafChat. If not, see <http://www.gnu.org/licenses/>.

Copyright 2011 Samuel Marshall.
*/
package com.leafdigital.highlighter.api;

import util.xml.XMLException;
import leafchat.core.api.Singleton;

/**
 * Highlighter singleton handles text highlights.
 */
public interface Highlighter extends Singleton
{
	/**
	 * Applies highlights to XML data.
	 * @param currentNickname Current nickname (may be highlighted); null if none
	 * @param xml Well-formed XML input (may not have outer tag)
	 * @return Well-formed XML output (may not have outer tag)
	 * @throws XMLException If XML is not well-formed
	 */
	public String highlight(String currentNickname, String xml) throws XMLException;
}
