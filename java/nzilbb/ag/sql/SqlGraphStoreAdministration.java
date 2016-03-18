//
// (c) 2015, Robert Fromont - robert@fromont.net.nz
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
package nzilbb.ag.sql;

import java.util.HashMap;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import nzilbb.ag.*;
import nzilbb.ag.serialize.*;

/**
 * Graph store administration that uses a relational database as its back end.
 * <p>TODO: implement deserializer registration in the database instead of in-memory.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class SqlGraphStoreAdministration
   extends SqlGraphStore
   implements IGraphStoreAdministration
{
   // Attributes:
   
   /**
    * Registered deserializers, keyed by MIME type.
    * @see #getDeserializersByMimeType()
    * @see #setDeserializersByMimeType(HashMap)
    */
   protected HashMap<String,IDeserializer> deserializersByMimeType = new HashMap<String,IDeserializer>();
   /**
    * Getter for {@link #deserializersByMimeType}: Registered deserializers, keyed by MIME type.
    * @return Registered deserializers, keyed by MIME type.
    */
   public HashMap<String,IDeserializer> getDeserializersByMimeType() { return deserializersByMimeType; }
   /**
    * Setter for {@link #deserializersByMimeType}: Registered deserializers, keyed by MIME type.
    * @param newDeserializersByMimeType Registered deserializers, keyed by MIME type.
    */
   public void setDeserializersByMimeType(HashMap<String,IDeserializer> newDeserializersByMimeType) { deserializersByMimeType = newDeserializersByMimeType; }

   
   /**
    * Registered deserializers, keyed by file suffix (extension).
    * @see #getDeserializersBySuffix()
    * @see #setDeserializersBySuffix(HashMap)
    */
   protected HashMap<String,IDeserializer> deserializersBySuffix = new HashMap<String,IDeserializer>();
   /**
    * Getter for {@link #deserializersBySuffix}: Registered deserializers, keyed by file suffix (extension).
    * @return Registered deserializers, keyed by file suffix (extension).
    */
   public HashMap<String,IDeserializer> getDeserializersBySuffix() { return deserializersBySuffix; }
   /**
    * Setter for {@link #deserializersBySuffix}: Registered deserializers, keyed by file suffix (extension).
    * @param newDeserializersBySuffix Registered deserializers, keyed by file suffix (extension).
    */
   public void setDeserializersBySuffix(HashMap<String,IDeserializer> newDeserializersBySuffix) { deserializersBySuffix = newDeserializersBySuffix; }

   
   // Methods:
   
   /**
    * Default constructor.
    */
   public SqlGraphStoreAdministration()
   {
   } // end of constructor

   /**
    * Constructor with connection.
    * @param baseUrl URL prefix for file access.
    * @param files Root directory for file structure.
    * @param connection An opened database connection.
    */
   public SqlGraphStoreAdministration(String baseUrl, File files, Connection connection)
   {
      super(baseUrl, files, connection);
   } // end of constructor

   /**
    * Constructor with connection parameters.
    * @param baseUrl URL prefix for file access.
    * @param files Root directory for file structure.
    * @param connectString The database connection string.
    * @param user The database username.
    * @param password The databa password.
    * @throws SQLException If an error occurs during connection.
    */
   public SqlGraphStoreAdministration(String baseUrl, File files, String connectString, String user, String password)
      throws SQLException
   {
      super(baseUrl, files, connectString, user, password);
   } // end of constructor

   /**
    * Registers a graph deserializer.
    * @param deserializer
    * @throws StoreException If an error prevents the graph from being saved.
    * @throws PermissionException If saving the graph is not permitted.
    */
   public void registerDeserializer(IDeserializer deserializer)
       throws StoreException, PermissionException
   {
      SerializationDescriptor descriptor = deserializer.getDescriptor();
      deserializersByMimeType.put(descriptor.getMimeType(), deserializer);
      for (String suffix : descriptor.getFileSuffixes())
      {
	 deserializersBySuffix.put(suffix, deserializer);
      } // next suffix
   }

   /**
    * De-registers a graph deserializer.
    * @param deserializer
    * @throws StoreException If an error prevents the graph from being saved.
    * @throws PermissionException If saving the graph is not permitted.
    */
   public void deregisterDeserializer(IDeserializer deserializer)
       throws StoreException, PermissionException
   {
      SerializationDescriptor descriptor = deserializer.getDescriptor();
      deserializersByMimeType.remove(descriptor.getMimeType());
      for (String suffix : descriptor.getFileSuffixes())
      {
	 deserializersBySuffix.remove(suffix);
      } // next suffix
   }

   /**
    * Lists the descriptors of all registered deserializers.
    * @return A list of the descriptors of all registered deserializers.
    * @throws StoreException If an error prevents the graph from being saved.
    * @throws PermissionException If saving the graph is not permitted.
    */
   public SerializationDescriptor[] getDeserializerDescriptors()
       throws StoreException, PermissionException
   {
      SerializationDescriptor[] descriptors = new SerializationDescriptor[deserializersByMimeType.size()];
      int i = 0;
      for (IDeserializer deserializer : deserializersByMimeType.values())
      {
	 descriptors[i++] = deserializer.getDescriptor();
      }
      return descriptors;
   }
   
   /**
    * Gets the deserializer for the given MIME type.
    * @param mimeType
    * @return The deserializer for the given MIME type, or null if none is registered.
    * @throws StoreException If an error prevents the graph from being saved.
    * @throws PermissionException If saving the graph is not permitted.
    */
   public IDeserializer deserializerForMimeType(String mimeType)
       throws StoreException, PermissionException
   {
      return deserializersByMimeType.get(mimeType);
   }

   /**
    * Gets the deserializer for the given file suffix (extension).
    * @param suffix
    * @return The deserializer for the given suffix, or null if none is registered.
    * @throws StoreException If an error prevents the graph from being saved.
    * @throws PermissionException If saving the graph is not permitted.
    */
   public IDeserializer deserializerForFilesSuffix(String suffix) throws StoreException, PermissionException
   {
      return deserializersBySuffix.get(suffix);
   }

} // end of class SqlGraphStoreAdministration
