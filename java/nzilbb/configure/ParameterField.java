//
// Copyright 2019 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of nzilbb.ag.
//
//    nzilbb.ag is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 3 of the License, or
//    (at your option) any later version.
//
//    nzilbb.ag is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with nzilbb.ag; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package nzilbb.configure;

import java.lang.annotation.*;

/**
 * Annotation for a field that can be configured as a {@link Parameter}.
 * @author Robert Fromont robert@fromont.net.nz
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParameterField
{
   /**
    * A hint to display for the field.
    * @return A hint to display for the field.
    */
   String value() default "";
   /**
    * A short label for the field.
    * @return A short label for the field.
    */
   String label() default "";
   /**
    * Whether or not the switch is compulsory.
    * @return true if the switch is compulsory, false otherwise.
    */
   boolean required() default true;

} // end of class ParameterField
