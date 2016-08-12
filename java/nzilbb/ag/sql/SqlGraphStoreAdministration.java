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
package nzilbb.ag.sql;

import java.util.HashMap;
import java.util.jar.JarFile;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.sql.*;

import nzilbb.util.IO;
import nzilbb.ag.*;
import nzilbb.ag.serialize.*;
import nzilbb.ag.serialize.util.IconHelper;

/**
 * Graph store administration that uses a relational database as its back end.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class SqlGraphStoreAdministration
   extends SqlGraphStore
   implements IGraphStoreAdministration
{
   // Attributes:

   
   /**
    * Root directory for serializers.
    * @see #getSerializersDirectory()
    * @see #setSerializersDirectory(File)
    */
   protected File serializersDirectory;
   /**
    * Getter for {@link #serializersDirectory}: Root directory for serializers.
    * @return Root directory for serializers.
    */
   public File getSerializersDirectory() 
   { 
      if (serializersDirectory == null)
      {
	 if (files != null)
	 {
	    try
	    {
	       setSerializersDirectory(new File(files.getParentFile(), "converters"));
	    }
	    catch(Exception exception)
	    {}
	 }
      }
      return serializersDirectory; 
   }
   /**
    * Setter for {@link #serializersDirectory}: Root directory for serializers.
    * @param newSerializersDirectory Root directory for serializers.
    */
   public void setSerializersDirectory(File newSerializersDirectory) { serializersDirectory = newSerializersDirectory; }

   
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
    * Constructor with connection.
    * @param baseUrl URL prefix for file access.
    * @param files Root directory for file structure.
    * @param connection An opened database connection.
    * @throws SQLException If an error occurs during connection or loading of configuraion.
    */
   public SqlGraphStoreAdministration(String baseUrl, File files, Connection connection)
      throws SQLException
   {
      super(baseUrl, files, connection);
      loadSerializers();
   } // end of constructor

   /**
    * Constructor with connection parameters.
    * @param baseUrl URL prefix for file access.
    * @param files Root directory for file structure.
    * @param connectString The database connection string.
    * @param user The database username.
    * @param password The databa password.
    * @throws SQLException If an error occurs during connection or loading of configuraion.
    */
   public SqlGraphStoreAdministration(String baseUrl, File files, String connectString, String user, String password)
      throws SQLException
   {
      super(baseUrl, files, connectString, user, password);
      loadSerializers();
   }

   /**
    * Loads the registered serializers/deserializers.
    * @throws SQLException On SQL error.
    */
   protected void loadSerializers()
    throws SQLException
   {
      PreparedStatement sqlRegisteredConverter = getConnection().prepareStatement(
	 "SELECT class, jar FROM converter WHERE type = 'Deserializer' ORDER BY mimetype");
      ResultSet rs = sqlRegisteredConverter.executeQuery();
      while (rs.next())
      {
	 // get the jar file
	 File file = new File(getSerializersDirectory(), rs.getString("jar"));
	 try
	 {
	    JarFile jar = new JarFile(file);
	    
	    // get an instance of the class
	    URL[] url = new URL[] { file.toURI().toURL() };
	    URLClassLoader classLoader = URLClassLoader.newInstance(url, getClass().getClassLoader());
	    IDeserializer deserializer = (IDeserializer)classLoader.loadClass(rs.getString("class")).newInstance();
	    
	    // register it in memory
	    SerializationDescriptor descriptor = deserializer.getDescriptor();
	    deserializersByMimeType.put(descriptor.getMimeType(), deserializer);
	    for (String suffix : descriptor.getFileSuffixes())
	    {
	       deserializersBySuffix.put(suffix, deserializer);
	    } // next suffix
	    File iconFile = IconHelper.EnsureIconFileExists(descriptor, getSerializersDirectory());
	    descriptor.setIcon(
	       new URL(getBaseUrl()+"/"+getSerializersDirectory().getName()+"/"+iconFile.getName()));
	 }
	 catch(ClassNotFoundException x) { System.err.println(rs.getString("class") + ": " + x); }
	 catch(InstantiationException x) { System.err.println(rs.getString("class") + ": " + x); }
	 catch(IllegalAccessException x) { System.err.println(rs.getString("class") + ": " + x); }
	 catch(MalformedURLException x) { System.err.println(rs.getString("class") + ": " + x); }
	 catch(IOException x) { System.err.println(rs.getString("class") + ": " + x); }
      }
      rs.close();
      sqlRegisteredConverter.close();
   } // end of loadSerializers()

   /**
    * Registers a graph deserializer.
    * @param deserializer The deserializer to register.
    * @throws StoreException If an error prevents the graph from being saved.
    * @throws PermissionException If saving the graph is not permitted.
    */
   public void registerDeserializer(IDeserializer deserializer)
       throws StoreException, PermissionException
   {
      deregisterDeserializer(deserializer);

      try
      {
	 SerializationDescriptor descriptor = deserializer.getDescriptor();
	 PreparedStatement sqlRegister = connection.prepareStatement(
	    "INSERT INTO converter"
	    +" (mimetype, type, class, version, name, jar)"
	    +" VALUES (?,'Deserializer',?,?,?,?)");
	 sqlRegister.setString(1, descriptor.getMimeType());
	 sqlRegister.setString(2, deserializer.getClass().getName());
	 sqlRegister.setString(3, descriptor.getVersion());
	 sqlRegister.setString(4, descriptor.getName());
	 sqlRegister.setString(5, IO.JarFileOfClass(deserializer.getClass()).getName());
	 sqlRegister.executeUpdate();
	 sqlRegister.close();
	 
	 deserializersByMimeType.put(descriptor.getMimeType(), deserializer);
	 for (String suffix : descriptor.getFileSuffixes())
	 {
	    deserializersBySuffix.put(suffix, deserializer);
	 } // next suffix

	 try
	 {
	    File iconFile = IconHelper.EnsureIconFileExists(descriptor, getSerializersDirectory());
	    descriptor.setIcon(
	       new URL(getBaseUrl()+"/"+getSerializersDirectory().getName()+"/"+iconFile.getName()));
	 }
	 catch(MalformedURLException exception) {}
	 catch(IOException exception) {}
      }
      catch(SQLException exception)
      {
	 throw new StoreException(exception);
      }
   }

   /**
    * De-registers a graph deserializer.
    * @param deserializer The deserializer to de-register.
    * @throws StoreException If an error prevents the graph from being saved.
    * @throws PermissionException If saving the graph is not permitted.
    */
   public void deregisterDeserializer(IDeserializer deserializer)
       throws StoreException, PermissionException
   {
      try
      {
	 SerializationDescriptor descriptor = deserializer.getDescriptor();
	 deserializersByMimeType.remove(descriptor.getMimeType());
	 for (String suffix : descriptor.getFileSuffixes())
	 {
	    deserializersBySuffix.remove(suffix);
	 } // next suffix

	 PreparedStatement sqlDeregister = connection.prepareStatement(
	    "DELETE FROM converter WHERE mimetype = ? AND type = 'Deserializer'");
	 sqlDeregister.setString(1, descriptor.getMimeType());
	 sqlDeregister.executeUpdate();
	 sqlDeregister.close();
      }
      catch(SQLException exception)
      {
	 throw new StoreException(exception);
      }
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
	 SerializationDescriptor descriptor = deserializer.getDescriptor();
	 try
	 { // fix up the icon URL
	    File iconFile = IconHelper.EnsureIconFileExists(descriptor, getSerializersDirectory());
	    descriptor.setIcon(
	       new URL(getBaseUrl()+"/"+getSerializersDirectory().getName()+"/"+iconFile.getName()));
	 }
	 catch(MalformedURLException exception) {}
	 catch(IOException exception) {}
	 descriptors[i++] = descriptor;
      }
      return descriptors;
   }
   
   /**
    * Gets the deserializer for the given MIME type.
    * @param mimeType The MIME type.
    * @return The deserializer for the given MIME type, or null if none is registered.
    * @throws StoreException If an error prevents the graph from being saved.
    * @throws PermissionException If saving the graph is not permitted.
    */
   public IDeserializer deserializerForMimeType(String mimeType)
       throws StoreException, PermissionException
   {
      try
      {
	 return (IDeserializer)deserializersByMimeType.get(mimeType).getClass().newInstance();
      }
      catch(IllegalAccessException exception)
      {
	 return null;
      }
      catch(InstantiationException exception)
      {
	 return null;
      }
      catch(NullPointerException exception)
      {
	 return null;
      }
   }

   /**
    * Gets the deserializer for the given file suffix (extension).
    * @param suffix The file extension.
    * @return The deserializer for the given suffix, or null if none is registered.
    * @throws StoreException If an error prevents the graph from being saved.
    * @throws PermissionException If saving the graph is not permitted.
    */
   public IDeserializer deserializerForFilesSuffix(String suffix) throws StoreException, PermissionException
   {
      try
      {
	 return (IDeserializer)deserializersBySuffix.get(suffix).getClass().newInstance();
      }
      catch(IllegalAccessException exception)
      {
	 return null;
      }
      catch(InstantiationException exception)
      {
	 return null;
      }
      catch(NullPointerException exception)
      {
	 return null;
      }
   }

} // end of class SqlGraphStoreAdministration
