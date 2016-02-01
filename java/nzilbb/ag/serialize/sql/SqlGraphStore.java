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
package nzilbb.ag.serialize.sql;

import java.sql.*;
import java.util.Vector;
import nzilbb.ag.*;
import nzilbb.ag.serialize.*;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;

/**
 * A serializer/deserializer that accesses graphs stored in a relational database.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class SqlGraphStore
   implements IStoreDeserializer
{
   // Attributes:
   
   /**
    * Database connection.
    * @see #getDb()
    * @see #setDb(Connection)
    */
   protected Connection db;
   /**
    * Getter for {@link #db}: Database connection.
    * @return Database connection.
    */
   public Connection getDb() { return db; }
   /**
    * Setter for {@link #db}: Database connection.
    * @param newDb Database connection.
    */
   public void setDb(Connection newDb) { db = newDb; }
   
   /**
    * Whether to disconnect the connection when garbage collected.
    * @see #getDisconnectWhenFinished()
    * @see #setDisconnectWhenFinished(boolean)
    */
   protected boolean disconnectWhenFinished = false;
   /**
    * Getter for {@link #disconnectWhenFinished}: Whether to disconnect the connection when garbage collected.
    * @return Whether to disconnect the connection when garbage collected.
    */
   public boolean getDisconnectWhenFinished() { return disconnectWhenFinished; }
   /**
    * Setter for {@link #disconnectWhenFinished}: Whether to disconnect the connection when garbage collected.
    * @param newDisconnectWhenFinished Whether to disconnect the connection when garbage collected.
    */
   public void setDisconnectWhenFinished(boolean newDisconnectWhenFinished) { disconnectWhenFinished = newDisconnectWhenFinished; }

   
   // Methods:
   
   /**
    * Default constructor.
    */
   public SqlGraphStore()
   {
   } // end of constructor
   
   /**
    * Returns the deserializer's descriptor
    * @return The deserializer's descriptor
    */
   public SerializationDescriptor getDescriptor()
   {
      return new SerializationDescriptor("SQL store", "20150705.1749");
   }
   
   /**
    * Sets parameters for deserializer as a whole.  This might include database connection parameters, locations of supporting files, etc.
    * @param configuration Configuration that includes either "connection", a connected database connection, or strings "dbConnectString", "dbUser", and "dbPassword".
    * @throws DeserializerNotConfiguredException If a database connection could not be established.
    */
   public void configure(ParameterSet configuration)
      throws DeserializerNotConfiguredException
   {
      // create a connection to the database
      if (configuration.containsKey("connection"))
      {
	 try
	 {
	    setDb((Connection)configuration.get("connection"));
	    setDisconnectWhenFinished(false);
	 }
	 catch(ClassCastException exception)
	 {
	    throw new DeserializerNotConfiguredException("\"connection\" parameter is not a Connection object");
	 }
      }
      else if (configuration.containsKey("dbConnectString")
	       && configuration.containsKey("dbUser")
	       && configuration.containsKey("dbPassword"))
      {
	 try
	 {
	    setDb(DriverManager.getConnection (
		     (String)configuration.get("dbConnectString").getValue(), 
		     (String)configuration.get("dbUser").getValue(), 
		     (String)configuration.get("dbPassword").getValue()));
	 }
	 catch(SQLException sql)
	 {
	    throw new DeserializerNotConfiguredException(sql);
	 }
	 catch(ClassCastException cc)
	 {
	    throw new DeserializerNotConfiguredException("Database parameters were not all Strings");
	 }	 
      }
      else
      {
	 throw new DeserializerNotConfiguredException();
      }
   }
   
   // IStoreDeserializer methods
   
   /**
    * Loads the serialized form of the graph, using the given identifier.
    * @param graphId Identifier for the graph to serialize.
    * @return A list of parameters that require setting before {@link IDeserializer#deserialize()} can be invoked. This may be an empty list, and may include parameters with the value already set to a workable default. If there are parameters, and user interaction is possible, then the user may be presented with an interface for setting/confirming these parameters, before they are then passed to {@link IDeserializer#setParameters(ParameterSet)}.
    * @throws Exception If the graph could not be loaded.
    */
   public ParameterSet load(String graphId)
      throws Exception
   {
      // TODO check the graph exists
      return new ParameterSet(); // TODO ask what layers to load
   }
   
   /**
    * Sets parameters for a given deserialization operation, after loading the serialized form of the graph. This might include mappings from format-specific objects like tiers to graph layers, etc.
    * @param parameters The parameters for a given deserialization operation.
    * @throws DeserializationParametersMissingException If not all required parameters are set.
    */
   public void setParameters(ParameterSet parameters) 
    throws DeserializationParametersMissingException
   {
      // TODO make a note of the layers that are required
   }
   
   /**
    * Deserializes the serialized data, generating one or more {@link Graph}s.
    * <p>Many data formats will only yield one graph (e.g. Transcriber
    * transcript or Praat textgrid), however there are formats that
    * are capable of storing multiple transcripts in the same file
    * (e.g. AGTK, Transana XML export), which is why this method
    * returns a list.
    * @return A list of valid (if incomplete) {@link Graph}s. 
    * @throws DeserializerNotConfiguredException if the object has not been configured.
    * @throws DeserializationParametersMissingException if the parameters for this particular graph have not been set.
    * @throws DeserializationException if errors occur during deserialization.
    */
   public Vector<Graph> deserialize() 
      throws DeserializerNotConfiguredException, DeserializationParametersMissingException, DeserializationException
   {
      return new Vector<Graph>(); // TODO load the graph from the database
   }

   /**
    * Returns any warnings that may have arisen during the last execution of {@link #deserialize()}.
    * @return A possibly empty list of warnings.
    */
   public Vector<String> getWarnings()
   {
      return new Vector<String>(); // no warnings
   }

} // end of class SqlGraphStore
