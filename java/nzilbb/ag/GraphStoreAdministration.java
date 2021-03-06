//
// Copyright 2016-2020 New Zealand Institute of Language, Brain and Behaviour, 
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

import nzilbb.ag.serialize.*;

/**
 * Interface for administration of a graph store.
 * <p>This interface inherits the <em>read-write</em> operations of {@link GraphStore}
 * and adds some administration operations, including definition of layers,
 * registration of converters, etc.
 * @author Robert Fromont robert@fromont.net.nz
 */
public interface GraphStoreAdministration extends GraphStore {   
   /**
    * Registers a transcript deserializer.
    * @param deserializer The deserializer to register.
    * @throws StoreException If an error prevents the operation.
    * @throws PermissionException If the operation is not permitted.
    */
   public void registerDeserializer(GraphDeserializer deserializer)
      throws StoreException, PermissionException;

   /**
    * De-registers a transcript deserializer.
    * @param deserializer The deserializer to de-register.
    * @throws StoreException If an error prevents the operation.
    * @throws PermissionException If the operation is not permitted.
    */
   public void deregisterDeserializer(GraphDeserializer deserializer)
      throws StoreException, PermissionException;

   /**
    * Registers a transcript serializer.
    * @param serializer The serializer to register.
    * @throws StoreException If an error prevents the operation.
    * @throws PermissionException If the operation is not permitted.
    */
   public void registerSerializer(GraphSerializer serializer)
      throws StoreException, PermissionException;

   /**
    * De-registers a transcript serializer.
    * @param serializer The serializer to de-register.
    * @throws StoreException If an error prevents the operation.
    * @throws PermissionException If the operation is not permitted.
    */
   public void deregisterSerializer(GraphSerializer serializer)
      throws StoreException, PermissionException;

   /**
    * Adds a new layer to the scheam.
    * @param layer A new layer definition.
    * @return The resulting layer definition.
    * @throws StoreException If an error prevents the operation.
    * @throws PermissionException If the operation is not permitted.
    */
   public Layer newLayer(Layer layer) throws StoreException, PermissionException;
   
   /**
    * Saves changes to a layer.
    * @param layer A modified layer definition.
    * @return The resulting layer definition.
    * @throws StoreException If an error prevents the operation.
    * @throws PermissionException If the operation is not permitted.
    */
   public Layer saveLayer(Layer layer) throws StoreException, PermissionException;
   
   /**
    * Deletes the given layer, and all associated annotations.
    * @param id The ID layer to delete.
    * @throws StoreException If an error prevents the transcript from being saved.
    * @throws PermissionException If saving the transcript is not permitted.
    */
   public void deleteLayer(String id) throws StoreException, PermissionException;

} // end of class GraphStoreAdministration
