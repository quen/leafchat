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
 * Annotation used to indicate that a method is used as a UI callback from an
 * XML file. This is used for compile-time checking that the XML file actually
 * links to this method.
 * <p>
 * Set the 'runtime' parameter if the callback is used dynamically from code
 * and not from an XML file.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface UIAction
{
	/**
	 * @return True if this method is only used at runtime via code calling a
	 *   setOnXx method, and is not referenced in an XML file
	 */
	boolean runtime() default false;
}
