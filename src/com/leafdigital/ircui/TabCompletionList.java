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
package com.leafdigital.ircui;

import java.util.*;

/**
 * Stores a tab completion list for a window (list of candidates). Built up
 * when user presses tab.
 */
class TabCompletionList
{
	private String partial;
	private boolean atStart;

	private Set<String> gotAlready = new HashSet<String>();
	private List<String> result = new LinkedList<String>();

	TabCompletionList(String partial,boolean atStart)
	{
		this.partial=partial;
		this.atStart=atStart;
	}

	void add(String option, boolean includeSuffix)
	{
		if(gotAlready.contains(option)) return;
		if(!option.toLowerCase().startsWith(partial.toLowerCase())) return;
		if(includeSuffix && atStart) option+=": ";
		gotAlready.add(option);
		result.add(option);
	}

	String[] getResult()
	{
		return result.toArray(new String[result.size()]);
	}
}
