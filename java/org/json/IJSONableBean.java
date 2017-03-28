//
// (c) 2017, Robert Fromont - robert@fromont.net.nz
//
//
//    This module is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    This module is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this module; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
package org.json;

/**
 * Interface supported by beans that explicitly specify which attributes are serialized to JSON.
 * @author Robert Fromont robert@fromont.net.nz
 */

public interface IJSONableBean
{
   /**
    * The names of fields to be obtained from the object, via getters called "get...".
    * @return The names of fields to be obtained from the object.
    */
   public String[] JSONAttributes();
}
