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
 * Indicates that method has a single parameter which is the name of a
 * callback function in the target UI handler.
 * <p>
 * When set methods use this annotation, then when an XML file indicates the
 * use of this set method, it will be checked to make sure it actually exists
 * in the target object.
 * <p>
 * This annotation may only be added to a set method inside an interface. (The
 * annotation must not be added to a superinterface.)
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@Documented
public @interface UICallback
{
}
