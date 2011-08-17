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
package com.leafdigital.ui.api;

import java.lang.annotation.*;

/**
 * Annotation used to indicate that an object supports callbacks associated
 * with a particular UI XML file. This is used for compile-time checking that
 * all the required action callback methods actually exist, and at runtime
 * to throw an error if your supplied callback doesn't have the annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface UIHandler
{
	/**
	 * @return Name of xml files that contain the UI definition, not including
	 *   the '.xml'; files are assumed to be in same folder as Java source
	 */
	String[] value();
}
