//
// Copyright 2015-2016 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag;

import java.util.Vector;

/**
 * Interface for querying and updating an annotation graph store, a database of graphs.
 * <p>In order to easily support access via scripting in other languages, methods that return lists use arrays rather than collection classes.
 * @author Robert Fromont robert@fromont.net.nz
 */
public interface IGraphStore 
   extends IGraphStoreQuery
{
   
   /**
    * Saves the given graph. The graph can be partial e.g. include only some of the layers that the stored version of the graph contains.
    * <p>The graph deltas are assumed to be set correctly, so if this is a new graph, then {@link Graph#getChange()} should return Change.Operation.Create, if it's an update, Change.Operation.Update, and to delete, Change.Operation.Delete.  Correspondingly, all {@link Anchor}s and {@link Annotation}s should have their changes set also.  If {@link Graph#getChanges()} returns no changes, no action will be taken, and this method returns false.
    * <p>After this method has executed, {@link Graph#commit()} is <em>not</em> called - this must be done by the caller, if they want changes to be committed.
    * @param graph The graph to save.
    * @return true if changes were saved, false if there were no changes to save.
    * @throws StoreException If an error prevents the graph from being saved.
    * @throws PermissionException If saving the graph is not permitted.
    * @throws GraphNotFoundException If the graph doesn't exist.
    */
   public boolean saveGraph(Graph graph) throws StoreException, PermissionException, GraphNotFoundException;


} // end of interface IGraphStore
