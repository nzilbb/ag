//
// Copyright 2015 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of LaBB-CAT.
//
//    LaBB-CAT is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    LaBB-CAT is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with LaBB-CAT; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package nzilbb.configure;

import java.util.LinkedHashMap;

/**
 * Set of parameters, being a map to Parameters keyed on parameter name.  Uses LinkedHashMap so that iteration order can be controlled, as it's insertion-order.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class ParameterSet
   extends LinkedHashMap<String,Parameter>
{   
   // Methods:
   
   /**
    * Default constructor
    */
   public ParameterSet()
   {
   } // end of constructor

   
   /**
    * Adds a parameter to the set.
    * @param parameter
    */
   public void addParameter(Parameter parameter)
   {
      put(parameter.getName(), parameter);
   } // end of addParameter()

} // end of class ParameterSet
