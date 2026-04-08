//
// Copyright 2026 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.automation;

import java.lang.annotation.*;

/**
 * Annotation for exposing an {@link #Annotator} method as a possible
 * API endpoint for the 'ext' webapp, which specifies the role the
 * user must have in order to access the endpoint - "view" for any
 * user, "edit" for users with read/write privileges, and "admin" for
 * superusers.
 * <p> For example:
 * <pre> public class MyTagger extends Annotator {
 *   public void setTaskParameters(String p) throws InvalidConfigurationException { ... }
 *   public Graph transform(Graph g) throws TransformationException { ... }
 *
 *   // This method has no annotation and can't be invoked from the 'ext' webapp.
 *   public String getAPISecret() { ... }
 *
 *   // Any user of the 'ext' webapp can invoke this method.
 *   @ApiEndpoint("view") public String readWugs() { ... }
 *
 *   // Only users of the 'ext' webapp with 'edit' or 'admin' role can invoke this method.
 *   @ApiEndpoint("edit") public String hugWug(String wugId) { ... }
 *
 *   // Only users of the 'ext' webapp with 'admin' role can invoke this method.
 *   @ApiEndpoint("admin") public String hugWug(String wugId) { ... }
 * } </pre>
 * @author Robert Fromont robert@fromont.nz
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiEndpoint {
  /**
   * The the role the user must have in order to access the endpoint -
   * "view" for any user, "edit" for users with read/write privileges,
   * and "admin" for superusers.  
   * @return The minimum role the user must have, "view", "edit", or "admin"
   */
  String value();
} // end of class ApiEndpoint
