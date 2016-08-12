//
// Copyright 2016 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.util;

import java.lang.annotation.*;

/**
 * Annotation for a {@link CommandLineProgram} or {@link GuiProgram} - used to inform command-line and applet parameter interpretation, and to product usage information.
 * @author Robert Fromont robert@fromont.net.nz
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProgramDescription
{
   /**
    * 'Usage' description of the application.
    * @return 'Usage' description of the application.
    */
   String value();
   /**
    * Arguments to show in usage information.
    * @return Arguments to show in usage information.
    */
   String arguments() default "";
} // end of class Switch
