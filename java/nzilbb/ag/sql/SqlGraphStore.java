//
// Copyright 2015-2019 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.*;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.SortedSet;
import java.util.Vector;
import java.util.regex.Pattern;
import nzilbb.ag.*;
import nzilbb.ag.ql.AGQLException;
import nzilbb.ag.util.AnnotationsByAnchor;
import nzilbb.ag.util.LayerHierarchyTraversal;
import nzilbb.ag.util.Validator;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.media.IMediaCensor;
import nzilbb.media.IMediaConverter;
import nzilbb.media.MediaException;
import nzilbb.media.MediaThread;
import nzilbb.media.ffmpeg.FfmpegCensor;
import nzilbb.media.ffmpeg.FfmpegConverter;
import nzilbb.media.wav.FragmentExtractor;
import nzilbb.media.wav.Resampler;
import nzilbb.util.IO;
import nzilbb.util.MonitorableSeries;
import nzilbb.util.Timers;

/**
 * Graph store that uses a relational database as its back end.
 * TODO user permission checking
 * @author Robert Fromont robert@fromont.net.nz
 */

public class SqlGraphStore
  implements IGraphStore
{
  // Attributes:

  /** Format of annotation IDs, where {0} = scope, {1} = layer_id, and {2} = annotation_id */
  protected MessageFormat fmtAnnotationId = new MessageFormat("e{0}_{1,number,0}_{2,number,0}");

  /** Format of annotation IDs for 'meta' layers, where {0} = layer_id and {1} = the id of the entity (corpus_id, family_id, speaker_number, etc.) */
  protected MessageFormat fmtMetaAnnotationId = new MessageFormat("m_{0,number,0}_{1}");

  /** Format of annotation IDs for transcript attributes, where {0} = attribute and {1} = annotation_id */
  protected MessageFormat fmtTranscriptAttributeId = new MessageFormat("t|{0}|{1,number,0}");

  /** Format of annotation IDs for participant attributes, where {0} = attribute and {1} = annotation_id */
  protected MessageFormat fmtParticipantAttributeId = new MessageFormat("p|{0}|{1,number,0}");

  /** Format of anchor IDs, where {0} = anchor_id */
  protected MessageFormat fmtAnchorId = new MessageFormat("n_{0,number,0}");
   
  /**
   * URL prefix for file access.
   * @see #getBaseUrl()
   * @see #setBaseUrl(String)
   */
  protected String baseUrl;
  /**
   * Getter for {@link #baseUrl}: URL prefix for file access.
   * @return URL prefix for file access.
   */
  public String getBaseUrl() { return baseUrl; }
  /**
   * Setter for {@link #baseUrl}: URL prefix for file access.
   * @param newBaseUrl URL prefix for file access.
   */
  public SqlGraphStore setBaseUrl(String newBaseUrl) { baseUrl = newBaseUrl; return this; }

  /**
   * Root directory for file structure.
   * @see #getFiles()
   * @see #setFiles(File)
   */
  protected File files;
  /**
   * Getter for {@link #files}: Root directory for file structure.
   * @return Root directory for file structure.
   */
  public File getFiles() { return files; }
  /**
   * Setter for {@link #files}: Root directory for file structure.
   * @param newFiles Root directory for file structure.
   */
  public SqlGraphStore setFiles(File newFiles) { files = newFiles; return this; }

  /**
   * Database connection.
   * @see #getConnection()
   * @see #setConnection(Connection)
   */
  protected Connection connection;
  /**
   * Getter for {@link #connection}: Database connection.
   * @return Database connection.
   */
  public Connection getConnection() { return connection; }
  /**
   * Setter for {@link #connection}: Database connection.
   * @param newConnection Database connection.
   */
  public SqlGraphStore setConnection(Connection newConnection) { connection = newConnection; return this; }

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
  public SqlGraphStore setDisconnectWhenFinished(boolean newDisconnectWhenFinished) { disconnectWhenFinished = newDisconnectWhenFinished; return this; }
   
  /**
   * The store's ID.
   * @see #getId()
   * @see #setId(String)
   */
  protected String id;
  /**
   * IGraphStore method and getter for {@link #id}: The store's ID.
   * @return The store's ID.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String getId() throws StoreException, PermissionException { return id; }
  /**
   * Setter for {@link #id}: The store's ID.
   * @param newId The store's ID.
   */
  public SqlGraphStore setId(String newId) { id = newId; return this; }


  /**
   * ID of the user querying the store.
   * @see #getUser()
   * @see #setUser(String)
   */
  protected String user;
  /**
   * Getter for {@link #user}: ID of the user querying the store.
   * @return ID of the user querying the store.
   */
  public String getUser() { return user; }
  /**
   * Setter for {@link #user}: ID of the user querying the store.
   * @param newUser ID of the user querying the store.
   */
  public SqlGraphStore setUser(String newUser)
  {
    user = newUser;
    if (user != null)
    {
      try
      {
        PreparedStatement sqlUserGroups = getConnection().prepareStatement(
          "SELECT role_id FROM role WHERE user_id = ?");
        sqlUserGroups.setString(1, user);
        ResultSet rstUserGroups = sqlUserGroups.executeQuery();
        while (rstUserGroups.next())
        {
          userRoles.add(rstUserGroups.getString("role_id"));
        } // next group
        rstUserGroups.close();
        sqlUserGroups.close();
      }
      catch(Exception exception)
      {}
    }
    return this;
  }

  /**
   * Roles the user fulfills.
   * @see #getUserRoles()
   */
  protected HashSet<String> userRoles = new HashSet<String>();
  /**
   * Getter for {@link #userRoles}: Roles the user fulfills.
   * @return Roles the user fulfills.
   */
  public HashSet<String> getUserRoles() { return userRoles; }
   
   
  // Methods:
   
  /**
   * Default constructor.
   */
  public SqlGraphStore()
  {
  } // end of constructor

  /**
   * Constructor with connection.
   * @param baseUrl URL prefix for file access.
   * @param connection An opened database connection.
   * @param user ID of the user
   */
  public SqlGraphStore(String baseUrl, Connection connection, String user)
    throws SQLException
  {
    setId(baseUrl);
    setBaseUrl(baseUrl);
    setConnection(connection);
    setFiles(new File(getSystemAttribute("transcriptPath")));
    setUser(user);
  } // end of constructor

  /**
   * Constructor with connection.
   * @param baseUrl URL prefix for file access.
   * @param files Root directory for file structure.
   * @param connection An opened database connection.
   * @param user ID of the user
   */
  public SqlGraphStore(String baseUrl, File files, Connection connection, String user)
  {
    setId(baseUrl);
    setBaseUrl(baseUrl);
    setFiles(files);
    setConnection(connection);
    setUser(user);
  } // end of constructor

  /**
   * Constructor with connection parameters.
   * @param baseUrl URL prefix for file access.
   * @param files Root directory for file structure.
   * @param connectString The database connection string.
   * @param databaseUser The database username.
   * @param password The databa password.
   * @param storeUser ID of the user
   * @throws SQLException If an error occurs during connection.
   */
  public SqlGraphStore(String baseUrl, File files, String connectString, String databaseUser, String password, String storeUser)
    throws SQLException
  {
    setId(baseUrl);
    setBaseUrl(baseUrl);
    setFiles(files);
    setConnection(DriverManager.getConnection (connectString, user, password));
    setUser(storeUser);
  } // end of constructor

  /**
   * Called when the object is garbage-collected.
   */
  @SuppressWarnings("deprecation")
  public void finalize()
  {
    if (getDisconnectWhenFinished() && getConnection() != null)
    {
      try { getConnection().close(); } catch(Throwable t) {}
    }
  } // end of finalize()

  // IGraphStore methods

  /**
   * Gets a list of layer IDs (annotation 'types').
   * @return A list of layer IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String[] getLayerIds() throws StoreException, PermissionException
  {
    try
    {
      // temporal layers
      PreparedStatement sql = getConnection().prepareStatement(
        "SELECT short_description FROM layer ORDER BY short_description");
      ResultSet rs = sql.executeQuery();
      Vector<String> layerIds = new Vector<String>();
      layerIds.add("main_participant"); // transcript_speaker.main_speaker
      while (rs.next())
      {
        layerIds.add(rs.getString("short_description"));
      } // next layer
      rs.close();
      sql.close();

      // graph layers
      layerIds.add("transcript_type"); // special case that's not in attribute_definition
      sql = getConnection().prepareStatement(
        "SELECT attribute FROM attribute_definition"
        +" WHERE class_id = 'transcript' ORDER BY display_order, attribute");
      rs = sql.executeQuery();
      while (rs.next())
      {
        layerIds.add("transcript_"+rs.getString("attribute"));
      } // next layer
      rs.close();
      sql.close();

      // participant layers
      sql = getConnection().prepareStatement(
        "SELECT attribute FROM attribute_definition"
        +" WHERE class_id = 'speaker' ORDER BY display_order, attribute");
      rs = sql.executeQuery();
      while (rs.next())
      {
        layerIds.add("participant_"+rs.getString("attribute"));
      } // next layer
      rs.close();
      sql.close();

      return layerIds.toArray(new String[0]);
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
  }
   
  /**
   * Gets a list of layer definitions.
   * @return A list of layer definitions.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public Layer[] getLayers() throws StoreException, PermissionException
  {
    LinkedHashMap<String,Layer> layerLookup = new LinkedHashMap<String,Layer>();
    for (String layerId : getLayerIds())
    {
      Layer layer = getLayer(layerId);
      layerLookup.put(layerId, layer);
    }
    // set parents
    for (Layer layer : layerLookup.values()) 
    {
      layer.setParent(layerLookup.get(layer.getParentId()));
    }
    return layerLookup.values().toArray(new Layer[0]);
  }

  /** Schema is only retrieved/built once */
  protected Schema schema = null;
  
  /**
   * Gets the layer schema. For performance reasons, this implementation only retrieves/builds
   * the schema once, and always returns a clone of that original object.
   * @return A schema defining the layers and how they relate to each other.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public Schema getSchema() throws StoreException, PermissionException
  {
    if (schema == null)
    {
      schema = new Schema();
      for (String layerId : getLayerIds())
      {
        Layer layer = getLayer(layerId);
        schema.addLayer(layer);
        if (Integer.valueOf(11).equals(layer.get("@layer_id")))
        {
          schema.setTurnLayerId(layer.getId());
        }
        else if (Integer.valueOf(12).equals(layer.get("@layer_id")))
        {
          schema.setUtteranceLayerId(layer.getId());
        } 
        else if (Integer.valueOf(0).equals(layer.get("@layer_id")))
        {
          schema.setWordLayerId(layer.getId());
        } 
        else if (Integer.valueOf(-50).equals(layer.get("@layer_id")))
        {
          schema.setEpisodeLayerId(layer.getId());
        } 
        else if (Integer.valueOf(-100).equals(layer.get("@layer_id")))
        {
          schema.setCorpusLayerId(layer.getId());
        } 
      } // next layer
      schema.setParticipantLayerId("who");
    }

    Schema clone = (Schema)schema.clone();
    for (Layer layer : schema.getLayers().values())
    {
      Layer layerCopy = clone.getLayer(layer.getId());
      if (layerCopy == null) continue;
      // clone the temporary attributes too
      for (String key : layer.keySet())
      {
        if (key.startsWith("@") && layer.get(key) != null)
        {
          layerCopy.put(key, layer.get(key));
        }
      } // next attribute
    } // next layer

    return clone;
  }


  /**
   * Gets a layer definition.
   * @param id ID of the layer to get the definition for.
   * @return The definition of the given layer.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public Layer getLayer(String id) throws StoreException, PermissionException
  {
    try
    {
      if (id.equals("transcript_type"))
      { // special case that's not (yet) in the database
        Layer layer = new Layer(id, "Transcript type", Constants.ALIGNMENT_NONE, false, false, true, "graph", true);
        layer.setValidLabels(new LinkedHashMap<String,String>());
        PreparedStatement sql = getConnection().prepareStatement(
          "SELECT transcript_type FROM transcript_type ORDER BY type_id");
        ResultSet rs = sql.executeQuery();
        while (rs.next())
        {
          layer.getValidLabels().put(rs.getString("transcript_type"), rs.getString("transcript_type"));
        }	 
        rs.close();
        sql.close();
        return layer;
      }

      PreparedStatement sqlParentId = getConnection().prepareStatement(
        "SELECT short_description FROM layer WHERE layer_id = ?");
      PreparedStatement sql = getConnection().prepareStatement(
        "SELECT layer.*, project.project"
        +" FROM layer"
        +" LEFT OUTER JOIN project ON layer.project_id = project.project_id"
        +" WHERE short_description = ?");
      sql.setString(1, id);
      ResultSet rs = sql.executeQuery();
      if (rs.next())
      {
        Layer layer = new Layer();
        layer.setId(rs.getString("short_description"));
        layer.setDescription(rs.getString("description"));
        layer.setAlignment(rs.getInt("alignment"));
        sqlParentId.setInt(1, rs.getInt("parent_id"));
        ResultSet rsParentId = sqlParentId.executeQuery();
        if (rsParentId.next())
        {
          layer.setParentId(rsParentId.getString("short_description"));
        }
        rsParentId.close();
        layer.setParentIncludes(rs.getInt("parent_includes") == 1);
        layer.setPeers(rs.getInt("peers") == 1);
        layer.setPeersOverlap(rs.getInt("peers_overlap") == 1);
        layer.setSaturated(rs.getInt("saturated") == 1);

        if (rs.getString("type").equals("N")
            || rs.getString("type").equals("number")
            || rs.getString("type").equals("integer"))
        {
          layer.setType(Constants.TYPE_NUMBER);
        }
        else if (rs.getString("type").equals("D"))
        {
          layer.setType(Constants.TYPE_IPA);
        }
        else if (rs.getString("type").equals("boolean"))
        {
          layer.setType(Constants.TYPE_BOOLEAN);
        }
        else
        {
          layer.setType(Constants.TYPE_STRING);
        }
        if (rs.getString("project") != null)
        {
           layer.setCategory(rs.getString("project"));
        }

        // other attributes
        layer.put("@layer_id", Integer.valueOf(rs.getInt("layer_id")));
        layer.put("@type", rs.getString("type"));
        layer.put("@user_id", rs.getString("user_id"));
        layer.put("@layer_manager_id", rs.getString("layer_manager_id"));
        layer.put("@extra", rs.getString("extra"));
        layer.put("@scope", rs.getString("scope"));
        layer.put("@enabled", rs.getString("enabled"));
        layer.put("@notes", rs.getString("notes"));
        layer.put("@project_id", rs.getString("project_id"));
        System.out.println(rs.getString("layer_id") + " " + rs.getString("project"));
        layer.put("@data_mime_type", rs.getString("data_mime_type"));
        layer.put("@alignment", rs.getString("alignment"));
        layer.put("@style", rs.getString("style"));
        layer.put("@peers", rs.getString("peers"));
	    
        rs.close();
        sql.close();
        sqlParentId.close();

        if (layer.getId().equals("corpus"))
        {
          LinkedHashMap<String,String> corpora = new LinkedHashMap<String,String>();
          for (String corpus : getCorpusIds())
          {
            corpora.put(corpus, corpus);
          }
          layer.setValidLabels(corpora);
        }

        return layer;
      }
      else
      {
        rs.close();
        sql.close();
        sqlParentId.close();
	    
        // maybe a transcript attribute
        sql = getConnection().prepareStatement(
          "SELECT * FROM attribute_definition WHERE CONCAT('transcript_', attribute) = ?"
          +" AND class_id = 'transcript'");
        sql.setString(1, id);
        rs = sql.executeQuery();
        if (rs.next())
        {
          Layer layer = new Layer();
          layer.setId("transcript_" + rs.getString("attribute"));
          layer.setDescription(rs.getString("label"));
          layer.setAlignment(Constants.ALIGNMENT_NONE);
          layer.setParentId("graph");
          layer.setParentIncludes(true);
          layer.setPeers(rs.getInt("peers") == 1
                         || rs.getString("style").matches(".*multiple.*"));
          layer.setPeersOverlap(true);
          layer.setSaturated(true);
	       
          if (rs.getString("type").equals("N")
              || rs.getString("type").equals("number")
              || rs.getString("type").equals("integer"))
          {
            layer.setType(Constants.TYPE_NUMBER);
          }
          else if (rs.getString("type").equals("D"))
          {
            layer.setType(Constants.TYPE_IPA);
          }
          else if (rs.getString("type").equals("boolean"))
          {
            layer.setType(Constants.TYPE_BOOLEAN);
          }
          else
          {
            layer.setType(Constants.TYPE_STRING);
          }
          layer.setCategory(rs.getString("category"));

          // other attributes
          layer.put("@class_id", rs.getString("class_id"));
          layer.put("@attribute", rs.getString("attribute"));
          layer.put("@type", rs.getString("type"));
          layer.put("@style", rs.getString("style"));
          layer.put("@peers", rs.getString("peers"));
          layer.put("@label", rs.getString("label"));
          layer.put("@description", rs.getString("description"));
          layer.put("@display_order", rs.getInt("display_order"));
          layer.put("@searchable", rs.getString("searchable"));
          layer.put("@access", rs.getString("access"));
	    
          rs.close();
          sql.close();
          sqlParentId.close();

          if (layer.get("@type").equals("select"))
          {
            // possible values
            sql = getConnection().prepareStatement(
              "SELECT value, description FROM attribute_option"
              +" WHERE class_id = 'transcript' AND attribute = ?"
              +" ORDER BY description");
            sql.setString(1, layer.get("@attribute").toString());
            rs = sql.executeQuery();
            if (rs.next())
            {
              layer.setValidLabels(new LinkedHashMap<String,String>());
              do
              {
                layer.getValidLabels().put(rs.getString("value"), rs.getString("description"));
              }	 
              while (rs.next());
            }
            rs.close();
            sql.close();
          }
	       
          return layer;
        }
        else
        {
          rs.close();
          sql.close();
          sqlParentId.close();
	       
          // maybe a participant attribute
          sql = getConnection().prepareStatement(
            "SELECT * FROM attribute_definition WHERE CONCAT('participant_', attribute) = ?"
            +" AND class_id = 'speaker'");
          sql.setString(1, id);
          rs = sql.executeQuery();
          if (rs.next())
          {
            Layer layer = new Layer();
            layer.setId("participant_" + rs.getString("attribute"));
            layer.setDescription(rs.getString("label"));
            layer.setAlignment(Constants.ALIGNMENT_NONE);
            layer.setParentId("who");
            layer.setParentIncludes(true);
            layer.setPeers(rs.getInt("peers") == 1
                           || rs.getString("style").matches(".*multiple.*"));
            layer.setPeersOverlap(false);
            layer.setSaturated(true);
		  
            if (rs.getString("type").equals("N")
                || rs.getString("type").equals("number")
                || rs.getString("type").equals("integer"))
            {
              layer.setType(Constants.TYPE_NUMBER);
            }
            else if (rs.getString("type").equals("D"))
            {
              layer.setType(Constants.TYPE_IPA);
            }
            else if (rs.getString("type").equals("boolean"))
            {
              layer.setType(Constants.TYPE_BOOLEAN);
            }
            else
            {
              layer.setType(Constants.TYPE_STRING);
            }
            layer.setCategory(rs.getString("category"));

            // other attributes
            layer.put("@class_id", rs.getString("class_id"));
            layer.put("@attribute", rs.getString("attribute"));
            layer.put("@type", rs.getString("type"));
            layer.put("@style", rs.getString("style"));
            layer.put("@peers", rs.getString("peers"));
            layer.put("@label", rs.getString("label"));
            layer.put("@description", rs.getString("description"));
            layer.put("@display_order", rs.getInt("display_order"));
            layer.put("@searchable", rs.getString("searchable"));
            layer.put("@access", rs.getString("access"));
		  
            rs.close();
            sql.close();
            sqlParentId.close();

            if (layer.get("@type").equals("select"))
            {
              // possible values
              sql = getConnection().prepareStatement(
                "SELECT value, description FROM attribute_option"
                +" WHERE class_id = 'speaker' AND attribute = ?"
                +" ORDER BY description");
              sql.setString(1, layer.get("@attribute").toString());
              rs = sql.executeQuery();
              if (rs.next())
              {
                layer.setValidLabels(new LinkedHashMap<String,String>());
                do
                {
                  layer.getValidLabels().put(rs.getString("value"), rs.getString("description"));
                }	 
                while (rs.next());
              }
              rs.close();
              sql.close();
            }
		  
            return layer;
          }
          else
          {
            throw new StoreException("Layer not found: " + id);
          }
        } // not a participant attribute
      } // not a temporal layer
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
  }

  /**
   * Gets a list of corpus IDs.
   * @return A list of corpus IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String[] getCorpusIds() throws StoreException, PermissionException
  {
    try
    {
      PreparedStatement sql = getConnection().prepareStatement(
        "SELECT corpus_name FROM corpus ORDER BY corpus_name");
      if (getUser() != null && !getUserRoles().contains("admin"))
      {
        PreparedStatement sqlValuePattern = getConnection().prepareStatement(
          "SELECT value_pattern"
          + " FROM role"
          + " INNER JOIN role_permission ON role.role_id = role_permission.role_id" 
          + " AND role_permission.attribute_name = 'corpus'" 
          + " AND role_permission.entity REGEXP '.*t.*'" // transcript access
          + " WHERE user_id = '"+getUser().replaceAll("'","\\'")+"'");
        ResultSet rsValuePattern = sqlValuePattern.executeQuery();
        if (rsValuePattern.next())
        {
          sql.close();
          sql = getConnection().prepareStatement(
            "SELECT corpus_name FROM corpus WHERE corpus_name REGEXP ? ORDER BY corpus_name");
          sql.setString(1, rsValuePattern.getString(1));
        }
        rsValuePattern.close();
        sqlValuePattern.close();
      }

      ResultSet rs = sql.executeQuery();
      Vector<String> corpora = new Vector<String>();
      while (rs.next())
      {
        corpora.add(rs.getString("corpus_name"));
      } // next layer
      rs.close();
      sql.close();
      return corpora.toArray(new String[0]);
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
  }

  /**
   * Gets a list of participant IDs.
   * @return A list of participant IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String[] getParticipantIds() throws StoreException, PermissionException
  {
    try
    {
      PreparedStatement sql = getConnection().prepareStatement(
        "SELECT name FROM speaker WHERE COALESCE(name,'') <> ''  ORDER BY name");
      ResultSet rs = sql.executeQuery();
      Vector<String> participants = new Vector<String>();
      while (rs.next())
      {
        participants.add(rs.getString("name"));
      } // next layer
      rs.close();
      sql.close();
      return participants.toArray(new String[0]);
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
  }
   
  /**
   * Gets the participant record specified by the given identifier.
   * @param id The ID of the participant, which could be their name or their database annotation ID.
   * @return An annotation representing the participant, or null if the participant was not found.
   * @throws StoreException
   * @throws PermissionException
   */
  public Annotation getParticipant(String id)
    throws StoreException, PermissionException
  {
    try
    {
      String speakerNumber = null;
      String name = null;
      String userWhereClause = userWhereClauseParticipant(true);
      if (id.startsWith("m_-2_"))
      {
        speakerNumber = id.substring("m_-2_".length());
        PreparedStatement sqlParticipant = getConnection().prepareStatement(
          "SELECT name FROM speaker WHERE speaker_number = ?" + userWhereClause);
        sqlParticipant.setString(1, speakerNumber);
        ResultSet rsParticipant = sqlParticipant.executeQuery();
        if (rsParticipant.next())
        {
          name = rsParticipant.getString(1);
        }
        rsParticipant.close();
        sqlParticipant.close();
      }
      else
      {
        name = id;
        PreparedStatement sqlParticipant = getConnection().prepareStatement(
          "SELECT speaker_number FROM speaker WHERE name = ?" + userWhereClause);
        sqlParticipant.setString(1, name);
        ResultSet rsParticipant = sqlParticipant.executeQuery();
        if (rsParticipant.next())
        {
          speakerNumber = rsParticipant.getString(1);
        }
        rsParticipant.close();
        sqlParticipant.close();
      }

      if (name != null && speakerNumber != null)
      {
        Schema schema = getSchema();
	    
        // add participant annotation
        Annotation participant = new Annotation(
          "m_-2_" + speakerNumber, name, schema.getParticipantLayerId());
	    
        PreparedStatement sqlValue = getConnection().prepareStatement(
          "SELECT a.annotation_id, a.speaker_number, a.label, a.annotated_by, a.annotated_when"
          +" FROM annotation_participant a"
          +" WHERE a.layer = ? and a.speaker_number = ?");
        sqlValue.setString(2, speakerNumber);
	    
        // load participant attributes
        for (Layer attributeLayer : schema.getLayer(schema.getParticipantLayerId()).getChildren().values())
        {
          // participant tag layers
          if (attributeLayer.getAlignment() == Constants.ALIGNMENT_NONE)
          {
            if ("speaker".equals(attributeLayer.get("@class_id")))
            {
              sqlValue.setString(1, attributeLayer.get("@attribute").toString());
              ResultSet rsValue = sqlValue.executeQuery();
              int ordinal = 1;
              while (rsValue.next())
              {
                // add graph-tag annotation
                Object[] annotationIdParts = {
                  attributeLayer.get("@attribute"), Integer.valueOf(rsValue.getInt("annotation_id"))};
                Annotation attribute = new Annotation(
                  fmtParticipantAttributeId.format(annotationIdParts), 
                  rsValue.getString("label"), attributeLayer.getId());
                if (rsValue.getString("annotated_by") != null)
                {
                  attribute.setAnnotator(rsValue.getString("annotated_by"));
                }
                if (rsValue.getTimestamp("annotated_when") != null)
                {
                  attribute.setWhen(rsValue.getTimestamp("annotated_when"));
                }
                attribute.setParentId("m_-2_"+rsValue.getString("speaker_number"));
                attribute.setOrdinal(ordinal++);
                participant.addAnnotation(attribute);
                attribute.commit();
              } // next annotation
              rsValue.close();
            } // class_id set
          } // unaligned layer
        } // next child of "who" layer
        sqlValue.close();

        participant.commit();
	    
        return participant;
      } // participant found
      else
      {
        return null;
      }
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }

  } // end of getParticipant()
   
  /**
   * Saves a participant, and all its tags, to the database.  The participant is represented by an Annotation that isn't assumed to be part of a graph.
   * @param participant
   * @return true if changes were saved, false if there were no changes to save.
   * @throws StoreException If an error prevents the participant from being saved.
   * @throws PermissionException If saving the participant is not permitted.
   */
  public boolean saveParticipant(Annotation participant)
    throws StoreException, PermissionException
  {
    if (!"who".equals(participant.getLayerId()))
      throw new StoreException("Annotation is not on the participant layer.");
    if (participant.getChange() == Change.Operation.Destroy )
      throw new StoreException("Deleting participants is not supported.");
      
    boolean thereWereChanges = false;
    try
    {
      // save the participant record itself
      switch(participant.getChange())
      {
        case Update:
        {
          thereWereChanges = true;
          Object[] o = fmtMetaAnnotationId.parse(participant.getId());
          int speakerNumber = Integer.parseInt(o[1].toString().replace(",",""));
          // update the label (the only possible change)
          PreparedStatement sql = getConnection().prepareStatement(
            "UPDATE speaker SET name = ? WHERE speaker_number = ?");
          sql.setString(1, participant.getLabel());
          sql.setInt(2, speakerNumber);
          sql.executeUpdate();
          sql.close();
          break;
        }
        case Create:
        {
          thereWereChanges = true;
          // update the label (the only possible change)
          PreparedStatement sql = getConnection().prepareStatement(
            "INSERT INTO speaker (name) VALUES (?)");
          sql.setString(1, participant.getLabel());
          sql.executeUpdate();
          sql.close();
          sql = getConnection().prepareStatement("SELECT LAST_INSERT_ID()");
          ResultSet rsInsert = sql.executeQuery();
          rsInsert.next();
          int speakerNumber = rsInsert.getInt(1);
          rsInsert.close();
          sql.close();
          Object[] o = { Integer.valueOf(-2), Integer.valueOf(speakerNumber) };
          participant.setId(fmtMetaAnnotationId.format(o));
          break;
        }
      }

      // save the tags
      PreparedStatement sqlInsertParticipantAttribute = getConnection().prepareStatement(
        "REPLACE INTO annotation_participant (speaker_number, layer, label, annotated_by, annotated_when) VALUES (?,?,?,?,?)");
      PreparedStatement sqlUpdateParticipantAttribute = getConnection().prepareStatement(
        "UPDATE annotation_participant SET label = ?, annotated_by = ?, annotated_when = ? WHERE layer = ? AND annotation_id = ?");
      PreparedStatement sqlDeleteParticipantAttribute = getConnection().prepareStatement(
        "DELETE FROM annotation_participant WHERE layer = ? AND annotation_id = ?");
      PreparedStatement sqlDeleteAllParticipantAttributesOnLayer = getConnection().prepareStatement(
        "DELETE FROM annotation_participant WHERE speaker_number = ? AND layer = ?");
      try
      {
        for (String layerId : participant.getAnnotations().keySet())
        {
          for (Annotation attribute : participant.getAnnotations().get(layerId))
          {
            if (attribute.getChange() != Change.Operation.NoChange)
            {
              thereWereChanges = true;
              if (attribute.getParentId() == null) attribute.setParentId(participant.getId());
              saveParticipantAttributeChanges(attribute, sqlInsertParticipantAttribute, sqlUpdateParticipantAttribute, sqlDeleteParticipantAttribute, sqlDeleteAllParticipantAttributesOnLayer);
            }
          } // next child
        } // next child layer
      }
      finally
      {
        sqlInsertParticipantAttribute.close();
        sqlUpdateParticipantAttribute.close();
        sqlDeleteParticipantAttribute.close();
        sqlDeleteAllParticipantAttributesOnLayer.close();
      }
    }
    catch(ParseException exception)
    {
      System.err.println("Error parsing ID for participant: "+participant.getId());
      throw new StoreException("Error parsing ID for participant: "+participant.getId(), exception);
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
    return thereWereChanges;
  } // end of saveParticipant()

  /**
   * Returns an SQL WHERE clause for restricting access by user ID, if the user is set.
   * <p>Assumes that the <tt>transcript</tt> table (with no alias) is in the FROM clause
   * of the query into which the WHERE clause will be embedded.
   * @param prefixWithAnd Whether to prefix the clause with "AND" or "WHERE".
   * @param transcriptTableAlias The alias for the transcript table.
   * @return A WHERE clause if appropriate, or an empty string if not.
   */
  private String userWhereClauseGraph(boolean prefixWithAnd, String transcriptTableAlias)
  {
    if (getUser() != null && !getUserRoles().contains("admin"))
    {
      return (prefixWithAnd?" AND":" WHERE")
        +" (EXISTS (SELECT * FROM role"
        + " INNER JOIN role_permission ON role.role_id = role_permission.role_id" 
        + " INNER JOIN annotation_transcript access_attribute" 
        + " ON access_attribute.layer = role_permission.attribute_name" 
        + " AND access_attribute.label REGEXP role_permission.value_pattern"
        + " AND role_permission.entity REGEXP '.*t.*'" // transcript access
        + " WHERE user_id = '"+getUser().replaceAll("'","\\'")+"'"
        + " AND access_attribute.ag_id = "+transcriptTableAlias+".ag_id)"
        +" OR EXISTS (SELECT * FROM role"
        + " INNER JOIN role_permission ON role.role_id = role_permission.role_id" 
        + " AND role_permission.attribute_name = 'corpus'" 
        + " AND role_permission.entity REGEXP '.*t.*'" // transcript access
        + " WHERE "+transcriptTableAlias+".corpus_name REGEXP role_permission.value_pattern"
        + " AND user_id = '"+getUser().replaceAll("'","\\'")+"')"
        +" OR NOT EXISTS (SELECT * FROM role_permission)) "; // permissions are being used in general
    }
    return "";
  } // end of userWhereClauseGraph()


  /**
   * Returns an SQL WHERE clauser for restricting access by userID, if the user is set.
   * <p>Assumes that the <tt>speaker</tt> table (with no alias) is in the FROM clause
   * of the query into which the WHERE clause will be embedded.
   * @param prefixWithAnd Whether to prefix the clause with "AND" or "WHERE".
   * @return A WHERE clause if appropriate, or an empty string if not.
   */
  private String userWhereClauseParticipant(boolean prefixWithAnd)
  {
    String userWhereClauseSpeaker = "";
    String userWhereClause = userWhereClauseGraph(prefixWithAnd, "transcript");
    if (userWhereClause.length() > 0)
    {
      // insert links to speaker table
      userWhereClauseSpeaker = userWhereClause
        .replace(
          " WHERE user_id",
          " INNER JOIN transcript_speaker ON access_attribute.ag_id = transcript_speaker.ag_id"
          + " WHERE transcript_speaker.speaker_number = speaker.speaker_number"
          +" AND user_id")
        .replace("AND access_attribute.ag_id = transcript.ag_id", "")
        .replace(
          " WHERE transcript.corpus_name",
          " INNER JOIN transcript_speaker"
          +" INNER JOIN transcript ON transcript_speaker.ag_id = transcript.ag_id"
          + " WHERE transcript_speaker.speaker_number = speaker.speaker_number"
          + " AND transcript.corpus_name")
        .replace(
          "FROM role_permission)",
          "FROM role_permission)"
          // include those with no transcripts too
          + " OR NOT EXISTS (SELECT * FROM transcript_speaker"
          + " WHERE transcript_speaker.speaker_number = speaker.speaker_number)");
    }
    return userWhereClauseSpeaker;
  } // end of userWhereClauseParticipant()

  /**
   * Gets a list of graph IDs.
   * @return A list of graph IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String[] getGraphIds() throws StoreException, PermissionException
  {
    try
    {
      PreparedStatement sql = getConnection().prepareStatement(
        "SELECT transcript_id FROM transcript " + userWhereClauseGraph(false, "transcript")
        +" ORDER BY transcript_id");
      ResultSet rs = sql.executeQuery();
      Vector<String> graphs = new Vector<String>();
      while (rs.next())
      {
        graphs.add(rs.getString("transcript_id"));
      } // next layer
      rs.close();
      sql.close();
      return graphs.toArray(new String[0]);
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
  }

  /**
   * Gets a list of graph IDs in the given corpus.
   * @param id A corpus ID.
   * @return A list of graph IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String[] getGraphIdsInCorpus(String id) throws StoreException, PermissionException
  {
    return getMatchingGraphIds("my('corpus').label = '"+id.replaceAll("\\\\","\\\\\\\\").replaceAll("'","\\\\'")+"'");
  }


  /**
   * Gets a list of IDs of graphs that include the given participant.
   * @param id A participant ID.
   * @return A list of graph IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String[] getGraphIdsWithParticipant(String id) throws StoreException, PermissionException
  {
    return getMatchingGraphIds("'"+id.replaceAll("\\\\","\\\\\\\\").replaceAll("'","\\\\'")+"' IN labels('who')");
  }

   
  /**
   * Converts a participant-matching expression into a resultset (SELECT selectedClause FROM... WHERE... orderClause).
   * <p> The expression language is currently not well defined, but expressions such as the following can be used:
   * <ul>
   *  <li><code>id MATCHES 'Ada.+'</code></li>
   *  <li><code>'CC' IN labels('corpus')</code></li>
   *  <li><code>'en' IN labels('participant_languages')</code></li>
   *  <li><code>'en' IN labels('transcript_language')</code></li>
   *  <li><code>id NOT MATCHES 'Ada.+' AND my('corpus').label = 'CC'</code></li>
   *  <li><code>list('transcript_rating').length &gt; 2</code></li>
   *  <li><code>list('participant_rating').length = 0</code></li>
   *  <li><code>'labbcat' NOT IN annotators('transcript_rating')</code></li>
   *  <li><code>my('participant_gender').label = 'NA'</code></li>
   * </ul>
   * @param expression The graph-matching expression.
   * @param sqlSelectClause The expression that is to go between SELECT and FROM.
   * @param sqlOrderClause The expression that appended to the end of the SQL query.
   * @return A PreparedStatement for the given expression, with parameters already set.
   * @throws SQLException
   * @throws StoreException If the expression is invalid.
   */
  private PreparedStatement participantMatchSql(String expression, String sqlSelectClause, String sqlOrderClause)
    throws SQLException, StoreException, PermissionException
  {
    String userWhereClause = userWhereClauseParticipant(true).replaceAll("^ AND ","");
    ParticipantAgqlToSql transformer = new ParticipantAgqlToSql(getSchema());
    ParticipantAgqlToSql.Query q = transformer.sqlFor(
      expression, sqlSelectClause, userWhereClause, sqlOrderClause);
    System.out.println("QL: " + expression);
    System.out.println("SQL: " + q.sql);
    PreparedStatement sql = q.prepareStatement(getConnection());
    return sql;
  } // end of participantMatchSql()

  /**
   * Counts the number of participants that match a particular pattern.
   * @param expression An expression that determines which participants match.
   * <p> The expression language is currently not well defined, but expressions such as the
   * following can be used: 
   * <ul>
   *  <li><code>id MATCHES 'Ada.+'</code></li>
   *  <li><code>'CC' IN labels('corpus')</code></li>
   *  <li><code>'en' IN labels('participant_languages')</code></li>
   *  <li><code>'en' IN labels('transcript_language')</code></li>
   *  <li><code>id NOT MATCHES 'Ada.+' AND my('corpus').label = 'CC'</code></li>
   *  <li><code>list('transcript_rating').length &gt; 2</code></li>
   *  <li><code>list('participant_rating').length = 0</code></li>
   *  <li><code>'labbcat' NOT IN annotators('transcript_rating')</code></li>
   *  <li><code>my('participant_gender').label = 'NA'</code></li>
   * </ul>
   * @return The number of matching participants.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public int countMatchingParticipantIds(String expression)
    throws StoreException, PermissionException
  {
    try
    {
      PreparedStatement sql = participantMatchSql(expression, "COUNT(*)", "");
      ResultSet rs = sql.executeQuery();
      try
      {
        rs.next();
        return rs.getInt(1);
      }
      finally
      {
        rs.close();
        sql.close();
      }
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
  }
   
  /**
   * Gets a list of IDs of participants that match a particular pattern.
   * @param expression An expression that determines which participants match.
   * <p> The expression language is currently not well defined, but expressions such as the
   * following can be used: 
   * <ul>
   *  <li><code>id MATCHES 'Ada.+'</code></li>
   *  <li><code>'CC' IN labels('corpus')</code></li>
   *  <li><code>'en' IN labels('participant_languages')</code></li>
   *  <li><code>'en' IN labels('transcript_language')</code></li>
   *  <li><code>id NOT MATCHES 'Ada.+' AND my('corpus').label = 'CC'</code></li>
   *  <li><code>list('transcript_rating').length &gt; 2</code></li>
   *  <li><code>list('participant_rating').length = 0</code></li>
   *  <li><code>'labbcat' NOT IN annotators('transcript_rating')</code></li>
   *  <li><code>my('participant_gender').label = 'NA'</code></li>
   * </ul>
   * @return A list of participant IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String[] getMatchingParticipantIds(String expression)
    throws StoreException, PermissionException
  {
    return getMatchingParticipantIdsPage(expression, null, null);
  }

  /**
   * Gets a list of IDs of participants that match a particular pattern.
   * @param expression An expression that determines which participants match.
   * <p> The expression language is currently not well defined, but expressions such as the
   * following can be used: 
   * <ul>
   *  <li><code>id MATCHES 'Ada.+'</code></li>
   *  <li><code>'CC' IN labels('corpus')</code></li>
   *  <li><code>'en' IN labels('participant_languages')</code></li>
   *  <li><code>'en' IN labels('transcript_language')</code></li>
   *  <li><code>id NOT MATCHES 'Ada.+' AND my('corpus').label = 'CC'</code></li>
   *  <li><code>list('transcript_rating').length &gt; 2</code></li>
   *  <li><code>list('participant_rating').length = 0</code></li>
   *  <li><code>'labbcat' NOT IN annotators('transcript_rating')</code></li>
   *  <li><code>my('participant_gender').label = 'NA'</code></li>
   * </ul>
   * @param pageLength The maximum number of IDs to return, or null to return all.
   * @param pageNumber The page number to return, or null to return the first page.
   * @return A list of participant IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String[] getMatchingParticipantIdsPage(String expression, Integer pageLength, Integer pageNumber)
    throws StoreException, PermissionException
  {
    try
    {
      String limit = "";
      if (pageLength != null)
      {
        if (pageNumber == null) pageNumber = 0;
        limit = " LIMIT " + (pageNumber * pageLength) + "," + pageLength;
      }
      PreparedStatement sql = participantMatchSql(
        expression, "DISTINCT speaker.name", "ORDER BY speaker.name" + limit);
      ResultSet rs = sql.executeQuery();
      Vector<String> ids = new Vector<String>();
      while (rs.next())
      {
        ids.add(rs.getString("name"));
      } // next layer
      rs.close();
      sql.close();
      return ids.toArray(new String[0]);
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
  }

  /**
   * Converts a graph-matching expression into a resultset (SELECT selectedClause FROM... WHERE... orderClause).
   * <p> The expression language is currently not well defined, but expressions such as the following can be used:
   * <ul>
   *  <li><code>id MATCHES 'Ada.+'</code></li>
   *  <li><code>'Robert' IN labels('who')</code></li>
   *  <li><code>my('corpus').label IN ('CC', 'IA', 'MU')</code></li>
   *  <li><code>my('episode').label = 'Ada Aitcheson'</code></li>
   *  <li><code>my('transcript_scribe').label = 'Robert'</code></li>
   *  <li><code>my('participant_languages').label = 'en'</code></li>
   *  <li><code>my('noise').label = 'bell'</code></li>
   *  <li><code>'en' IN labels('transcript_languages')</code></li>
   *  <li><code>'en' IN labels('participant_languages')</code></li>
   *  <li><code>'bell' IN labels('noise')</code></li>
   *  <li><code>list('transcript_languages').length gt; 1</code></li>
   *  <li><code>list('participant_languages').length gt; 1</code></li>
   *  <li><code>list('transcript').length gt; 100</code></li>
   *  <li><code>'Robert' IN annotators('transcript_rating')</code></li>
   *  <li><code>id NOT MATCHES 'Ada.+' AND my('corpus').label = 'CC' AND 'Robert' IN labels('who')</code></li>
   * </ul>
   * @param expression The graph-matching expression.
   * @param selectClause The expression that is to go between SELECT and FROM.
   * @param order The expression that appended to the end of the SQL query.
   * @param limit SQL LIMIT clause, if any.
   * @return A PreparedStatement for the given expression, with parameters already set.
   * @throws SQLException
   * @throws StoreException If the expression is invalid.
   */
  private PreparedStatement graphMatchSql(String expression, String selectClause, String order, String limit)
    throws SQLException, StoreException, PermissionException
  {
    String userWhereClause = userWhereClauseGraph(true, "transcript").replaceAll("^ AND ","");
    GraphAgqlToSql transformer = new GraphAgqlToSql(getSchema());
    GraphAgqlToSql.Query q = transformer.sqlFor(
      expression, selectClause, userWhereClause, order, limit);
    System.out.println("QL: " + expression);
    System.out.println("SQL: " + q.sql);
    PreparedStatement sql = q.prepareStatement(getConnection());
    return sql;
  } // end of graphMatchSql()

  /**
   * Counts the number of graphs that match a particular pattern.
   * @param expression An expression that determines which graphs match.
   * <p> The expression language is currently not well defined, but expressions such as the following can be used:
   * <ul>
   *  <li><code>id MATCHES 'Ada.+'</code></li>
   *  <li><code>'Robert' IN labels('who')</code></li>
   *  <li><code>my('corpus').label IN ('CC', 'IA', 'MU')</code></li>
   *  <li><code>my('episode').label = 'Ada Aitcheson'</code></li>
   *  <li><code>my('transcript_scribe').label = 'Robert'</code></li>
   *  <li><code>my('participant_languages').label = 'en'</code></li>
   *  <li><code>my('noise').label = 'bell'</code></li>
   *  <li><code>'en' IN labels('transcript_languages')</code></li>
   *  <li><code>'en' IN labels('participant_languages')</code></li>
   *  <li><code>'bell' IN labels('noise')</code></li>
   *  <li><code>list('transcript_languages').length gt; 1</code></li>
   *  <li><code>list('participant_languages').length gt; 1</code></li>
   *  <li><code>list('transcript').length gt; 100</code></li>
   *  <li><code>'Robert' IN annotators('transcript_rating')</code></li>
   *  <li><code>id NOT MATCHES 'Ada.+' AND my('corpus').label = 'CC' AND 'Robert' IN labels('who')</code></li>
   * </ul>
   * @return The number of matching graphs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public int countMatchingGraphIds(String expression)
    throws StoreException, PermissionException
  {
    try
    {
      PreparedStatement sql = graphMatchSql(expression, "COUNT(*)", null, null);
      ResultSet rs = sql.executeQuery();
      try
      {
        rs.next();
        return rs.getInt(1);
      }
      finally
      {
        rs.close();
        sql.close();
      }
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
  }
   
  /**
   * Gets a list of IDs of graphs that match a particular pattern.
   * @param expression An expression that determines which graphs match.
   * <p> The expression language is currently not well defined, but expressions such as the following can be used:
   * <ul>
   *  <li><code>id MATCHES 'Ada.+'</code></li>
   *  <li><code>'Robert' IN labels('who')</code></li>
   *  <li><code>my('corpus').label IN ('CC', 'IA', 'MU')</code></li>
   *  <li><code>my('episode').label = 'Ada Aitcheson'</code></li>
   *  <li><code>my('transcript_scribe').label = 'Robert'</code></li>
   *  <li><code>my('participant_languages').label = 'en'</code></li>
   *  <li><code>my('noise').label = 'bell'</code></li>
   *  <li><code>'en' IN labels('transcript_languages')</code></li>
   *  <li><code>'en' IN labels('participant_languages')</code></li>
   *  <li><code>'bell' IN labels('noise')</code></li>
   *  <li><code>list('transcript_languages').length gt; 1</code></li>
   *  <li><code>list('participant_languages').length gt; 1</code></li>
   *  <li><code>list('transcript').length gt; 100</code></li>
   *  <li><code>'Robert' IN annotators('transcript_rating')</code></li>
   *  <li><code>id NOT MATCHES 'Ada.+' AND my('corpus').label = 'CC' AND 'Robert' IN labels('who')</code></li>
   * </ul>
   * @return A list of graph IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String[] getMatchingGraphIds(String expression)
    throws StoreException, PermissionException
  {
    return getMatchingGraphIdsPage(expression, null, null, null);
  }

  /**
   * Gets a list of IDs of graphs that match a particular pattern.
   * @param expression An expression that determines which graphs match.
   * <p> The expression language is currently not well defined, but expressions such as the following can be used:
   * <ul>
   *  <li><code>id MATCHES 'Ada.+'</code></li>
   *  <li><code>'Robert' IN labels('who')</code></li>
   *  <li><code>my('corpus').label IN ('CC', 'IA', 'MU')</code></li>
   *  <li><code>my('episode').label = 'Ada Aitcheson'</code></li>
   *  <li><code>my('transcript_scribe').label = 'Robert'</code></li>
   *  <li><code>my('participant_languages').label = 'en'</code></li>
   *  <li><code>my('noise').label = 'bell'</code></li>
   *  <li><code>'en' IN labels('transcript_languages')</code></li>
   *  <li><code>'en' IN labels('participant_languages')</code></li>
   *  <li><code>'bell' IN labels('noise')</code></li>
   *  <li><code>list('transcript_languages').length gt; 1</code></li>
   *  <li><code>list('participant_languages').length gt; 1</code></li>
   *  <li><code>list('transcript').length gt; 100</code></li>
   *  <li><code>'Robert' IN annotators('transcript_rating')</code></li>
   *  <li><code>id NOT MATCHES 'Ada.+' AND my('corpus').label = 'CC' AND 'Robert' IN labels('who')</code></li>
   * </ul>
   * @param pageLength The maximum number of IDs to return, or null to return all.
   * @param pageNumber The zero-based page number to return, or null to return the first page.
   * @return A list of graph IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String[] getMatchingGraphIdsPage(String expression, Integer pageLength, Integer pageNumber)
    throws StoreException, PermissionException
  {
    return getMatchingGraphIdsPage(expression, pageLength, pageNumber, null);
  }
  /**
   * Gets a list of IDs of graphs that match a particular pattern.
   * @param expression An expression that determines which graphs match.
   * <p> The expression language is currently not well defined, but expressions such as the following can be used:
   * <ul>
   *  <li><code>id MATCHES 'Ada.+'</code></li>
   *  <li><code>'Robert' IN labels('who')</code></li>
   *  <li><code>my('corpus').label IN ('CC', 'IA', 'MU')</code></li>
   *  <li><code>my('episode').label = 'Ada Aitcheson'</code></li>
   *  <li><code>my('transcript_scribe').label = 'Robert'</code></li>
   *  <li><code>my('participant_languages').label = 'en'</code></li>
   *  <li><code>my('noise').label = 'bell'</code></li>
   *  <li><code>'en' IN labels('transcript_languages')</code></li>
   *  <li><code>'en' IN labels('participant_languages')</code></li>
   *  <li><code>'bell' IN labels('noise')</code></li>
   *  <li><code>list('transcript_languages').length gt; 1</code></li>
   *  <li><code>list('participant_languages').length gt; 1</code></li>
   *  <li><code>list('transcript').length gt; 100</code></li>
   *  <li><code>'Robert' IN annotators('transcript_rating')</code></li>
   *  <li><code>id NOT MATCHES 'Ada.+' AND my('corpus').label = 'CC' AND 'Robert' IN labels('who')</code></li>
   * </ul>
   * @param pageLength The maximum number of IDs to return, or null to return all.
   * @param pageNumber The zero-based page number to return, or null to return the first page.
   * @param order The ordering for the list of IDs, a string containing a comma-separated list of epxressions, which may be appended by " ASC" or " DESC", or null for graph ID order.
   * @return A list of graph IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String[] getMatchingGraphIdsPage(String expression, Integer pageLength, Integer pageNumber, String order)
    throws StoreException, PermissionException
  {
    try
    {
      String limit = "";
      if (pageLength != null)
      {
        if (pageNumber == null) pageNumber = 0;
        limit = " LIMIT " + (pageNumber * pageLength) + "," + pageLength;
      }
      if (order == null) order = "id ASC";
      PreparedStatement sql = graphMatchSql(
        expression, "transcript_id", order, limit);
      ResultSet rs = sql.executeQuery();
      Vector<String> graphs = new Vector<String>();
      while (rs.next())
      {
        graphs.add(rs.getString("transcript_id"));
      } // next layer
      rs.close();
      sql.close();
      return graphs.toArray(new String[0]);
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
  }
   
  /**
   * Gets a graph given its ID.
   * @param id The given graph ID.
   * @return The identified graph.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the graph was not found in the store.
   */
  public Graph getGraph(String id) throws StoreException, PermissionException, GraphNotFoundException

  {
    return getGraph(id, getLayerIds());
  }

  /**
   * Gets a graph given its ID, containing only the given layers.
   * @param id The given graph ID.
   * @param layerIds The IDs of the layers to load, or null if only graph data is required.
   * @return The identified graph.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the graph was not found in the store.
   */
  public Graph getGraph(String id, String[] layerIds) throws StoreException, PermissionException, GraphNotFoundException
  {
    try
    {
      Graph graph = new Graph();
      graph.setMediaProvider(new StoreGraphMediaProvider(graph, this));
      Schema mainSchema = getSchema();
      graph.getSchema().setParticipantLayerId(mainSchema.getParticipantLayerId());
      graph.getSchema().setTurnLayerId(mainSchema.getTurnLayerId());
      graph.getSchema().setUtteranceLayerId(mainSchema.getUtteranceLayerId());
      graph.getSchema().setWordLayerId(mainSchema.getWordLayerId());
      graph.getSchema().setEpisodeLayerId(mainSchema.getEpisodeLayerId());
      graph.getSchema().setCorpusLayerId(mainSchema.getCorpusLayerId());

      PreparedStatement sql = getConnection().prepareStatement(
        "SELECT transcript.*, COALESCE(transcript_family.name,'') AS series,"
        +" COALESCE(transcript_type.transcript_type,'') AS transcript_type, divergent.label AS divergent"
        +" FROM transcript"
        +" LEFT OUTER JOIN transcript_family ON transcript.family_id = transcript_family.family_id"
        +" LEFT OUTER JOIN transcript_type ON transcript.type_id = transcript_type.type_id"
        +" LEFT OUTER JOIN annotation_transcript divergent ON transcript.ag_id = divergent.ag_id AND divergent.layer = 'divergent'"
        +" WHERE transcript.transcript_id = ?"+userWhereClauseGraph(true, "transcript"));
      sql.setString(1, id);
      ResultSet rs = sql.executeQuery();
      if (!rs.next())
      { // graph not found - maybe we've been given a name with a mismatching extension?
        rs.close();
        sql.close();
        sql = getConnection().prepareStatement(
          "SELECT transcript.*, COALESCE(transcript_family.name,'') AS series,"
          +" COALESCE(transcript_type.transcript_type,'') AS transcript_type, divergent.label AS divergent"
          +" FROM transcript"
          +" LEFT OUTER JOIN transcript_family ON transcript.family_id = transcript_family.family_id"
          +" LEFT OUTER JOIN transcript_type ON transcript.type_id = transcript_type.type_id"
          +" LEFT OUTER JOIN annotation_transcript divergent ON transcript.ag_id = divergent.ag_id AND divergent.layer = 'divergent'"
          +" WHERE transcript.transcript_id REGEXP ?"+userWhereClauseGraph(true, "transcript"));
        sql.setString(1, "^" + id.replaceAll("\\.[^.]+$","") + "\\.[^.]+$");
        rs = sql.executeQuery();
        if (!rs.next())
        { // graph not found - maybe we've been given an ag_id?
          try
          {
            rs.close();
            sql.close();
            int iAgId = Integer.parseInt(id);
            sql = getConnection().prepareStatement(
              "SELECT transcript.*, COALESCE(transcript_family.name,'') AS series,"
              +" COALESCE(transcript_type.transcript_type,'') AS transcript_type, divergent.label AS divergent"
              +" FROM transcript"
              +" LEFT OUTER JOIN transcript_family ON transcript.family_id = transcript_family.family_id"
              +" LEFT OUTER JOIN transcript_type ON transcript.type_id = transcript_type.type_id"
              +" LEFT OUTER JOIN annotation_transcript divergent ON transcript.ag_id = divergent.ag_id AND divergent.layer = 'divergent'"
              +" WHERE transcript.ag_id = ?"+userWhereClauseGraph(true, "transcript"));
            sql.setInt(1, iAgId);
            rs = sql.executeQuery();
            if (!rs.next()) throw new GraphNotFoundException(id);
		  
          }
          catch(NumberFormatException exception)
          {
            throw new GraphNotFoundException(id);
          }
        }
      }
	 
      graph.setId(rs.getString("transcript_id"));
      int iAgId = rs.getInt("ag_id");
      graph.put("@ag_id", Integer.valueOf(iAgId));
      graph.setCorpus(rs.getString("corpus_name"));
      graph.put("@transcript_type", rs.getString("transcript_type"));
      graph.put("@series", rs.getString("series"));
      graph.put("@family_id", rs.getInt("family_id"));
      graph.setOrdinal(rs.getInt("family_sequence"));
      graph.put("@offset_in_series", Double.valueOf(rs.getInt("family_offset")));
      if (rs.getString("divergent") != null) graph.put("@divergent", Boolean.TRUE);

      rs.close();

      Vector<String> setStartEndLayers = new Vector<String>();

      if (layerIds != null)
      {
        // for each layer specified, all ancestor layers must also be loaded, to
        // ensure the graph is well structured.
        LinkedHashSet<String> layersToLoad = new LinkedHashSet<String>();
        for (String layerId : layerIds)
        {
          Layer layer = mainSchema.getLayer(layerId);
          if (layer != null)
          {
            for (Layer ancestor : layer.getAncestors())
            {
              layersToLoad.add(ancestor.getId());
            }		  
            layersToLoad.add(layerId);
          } // layer exists
        } // next specified layer
	    
        // load annotations
        PreparedStatement sqlAnnotation = getConnection().prepareStatement(
          "SELECT layer.*,"
          +" start.offset AS start_offset, start.alignment_status AS start_alignment_status,"
          +" start.annotated_by AS start_annotated_by, start.annotated_when AS start_annotated_when,"
          +" end.offset AS end_offset, end.alignment_status AS end_alignment_status,"
          +" end.annotated_by AS end_annotated_by, end.annotated_when AS end_annotated_when"
          +" FROM annotation_layer_? layer"
          +" INNER JOIN anchor start ON layer.start_anchor_id = start.anchor_id"
          +" INNER JOIN anchor end ON layer.end_anchor_id = end.anchor_id"
          +" WHERE layer.ag_id = ? ORDER BY start.offset, end.offset DESC, annotation_id");
        sqlAnnotation.setInt(2, iAgId);
        for (String layerId : layersToLoad)
        {
          Layer layer = getLayer(layerId);
          if (layerId.equals("graph"))
          { // special case
            continue;
          }
          else if (layerId.equals("who"))
          {
            // create participant layer...
            // thereby creating a lookup list of participant names
            graph.addLayer(layer);
            Layer mainParticipantLayer = getLayer("main_participant");
            graph.addLayer(mainParticipantLayer);
            PreparedStatement sqlParticipant = getConnection().prepareStatement(
              "SELECT speaker.speaker_number, speaker.name, transcript_speaker.main_speaker"
              +" FROM speaker"
              +" INNER JOIN transcript_speaker ON transcript_speaker.speaker_number = speaker.speaker_number"
              +" WHERE ag_id = ? ORDER BY speaker.name");
            sqlParticipant.setInt(1, iAgId);
            ResultSet rsParticipant = sqlParticipant.executeQuery();
            while (rsParticipant.next())
            {
              // add graph-tag annotation
              Object[] annotationIdParts = {
                layer.get("@layer_id"), rsParticipant.getString("speaker_number")};
              Annotation participant = new Annotation(
                fmtMetaAnnotationId.format(annotationIdParts), 
                rsParticipant.getString("name"), layer.getId());
              participant.setParentId(graph.getId());		     
              graph.addAnnotation(participant);
		     
              // are they a main participant?
              if (rsParticipant.getInt("main_speaker") == 1)
              {
                Object[] annotationIdParts2 = {
                  mainParticipantLayer.get("@layer_id"), rsParticipant.getString("speaker_number")};
                Annotation mainParticipant = new Annotation(
                  fmtMetaAnnotationId.format(annotationIdParts2), 
                  rsParticipant.getString("name"), "main_participant");
                mainParticipant.setParentId(participant.getId());
                graph.addAnnotation(mainParticipant);
              }
            } // next participant
            rsParticipant.close();
            sqlParticipant.close();

            setStartEndLayers.add(layerId);
            continue;
          }
          else if (layerId.equals("main_participant"))
          { // loaded with "who"
            setStartEndLayers.add(layerId);
            continue;
          }
          else if (layerId.equals("episode"))
          {
            graph.addLayer(layer);
            PreparedStatement sqlEpisode = getConnection().prepareStatement(
              "SELECT t.family_id, e.name"
              +" FROM transcript t"
              +" INNER JOIN transcript_family e ON e.family_id = t.family_id"
              +" WHERE t.ag_id = ?");
            sqlEpisode.setInt(1, iAgId);
            ResultSet rsEpisode = sqlEpisode.executeQuery();
            if (rsEpisode.next())
            {
              // add episode annotation
              Object[] annotationIdParts = {
                layer.get("@layer_id"), rsEpisode.getString("family_id")};
              Annotation episode = new Annotation(
                fmtMetaAnnotationId.format(annotationIdParts), 
                rsEpisode.getString("name"), layer.getId());
              episode.setParentId(graph.getId());
              graph.addAnnotation(episode);		     
            }
            rsEpisode.close();
            sqlEpisode.close();
            setStartEndLayers.add(layerId);
            continue;
          }
          else if ("episode".equals(layer.getParentId()))
          { // episode tag layer
            graph.addLayer(layer);
            PreparedStatement sqlEpisode = getConnection().prepareStatement(
              "SELECT a.annotation_id, a.label, a.annotated_by, a.annotated_when, t.family_id, data"
              +" FROM `annotation_layer_" + layer.get("@layer_id") + "` a"
              +" INNER JOIN transcript t ON t.family_id = a.family_id"
              +" WHERE t.ag_id = ?");
            sqlEpisode.setInt(1, iAgId);
            ResultSet rsEpisode = sqlEpisode.executeQuery();
            while (rsEpisode.next())
            {
              // add graph-tag annotation
              Object[] annotationIdParts = {
                "e", layer.get("@layer_id"), rsEpisode.getInt("annotation_id")};
              Object[] parentAnnotationIdParts = {
                Integer.valueOf(-50), rsEpisode.getString("family_id")};
              Annotation episodeTag = new Annotation(
                fmtAnnotationId.format(annotationIdParts), 
                rsEpisode.getString("label"), layer.getId());
              episodeTag.setParentId(fmtMetaAnnotationId.format(parentAnnotationIdParts));
              if (rsEpisode.getString("annotated_by") != null)
              {
                episodeTag.setAnnotator(rsEpisode.getString("annotated_by"));
              }
              if (rsEpisode.getTimestamp("annotated_when") != null)
              {
                episodeTag.setWhen(rsEpisode.getTimestamp("annotated_when"));
              }
              byte[] data = rsEpisode.getBytes("data");
              try
              {
                if (data != null) episodeTag.put("data", data);
              }
              catch(Exception exception)
              {
                System.err.println("Could not get data for " + episodeTag.getId() + ": " + exception);
              }
              graph.addAnnotation(episodeTag);		     
            }
            rsEpisode.close();
            sqlEpisode.close();
            setStartEndLayers.add(layerId);
            continue;
          }
          else if ("E".equals(layer.get("@scope")))
		  
          { // episode tag tag layer
            graph.addLayer(layer);
            PreparedStatement sqlEpisode = getConnection().prepareStatement(
              "SELECT a.annotation_id, a.label, a.annotated_by, a.annotated_when, a.parent_id, t.family_id, data"
              +" FROM `annotation_layer_" + layer.get("@layer_id") + "` a"
              +" INNER JOIN transcript t ON t.family_id = a.family_id"
              +" WHERE t.ag_id = ?");
            sqlEpisode.setInt(1, iAgId);
            ResultSet rsEpisode = sqlEpisode.executeQuery();
            while (rsEpisode.next())
            {
              // add graph-tag annotation
              Object[] annotationIdParts = {
                "e", layer.get("@layer_id"), rsEpisode.getInt("annotation_id")};
              Object[] parentAnnotationIdParts = {
                "e", layer.getParent().get("@layer_id"), rsEpisode.getInt("parent_id")};
              Annotation episodeTag = new Annotation(
                fmtAnnotationId.format(annotationIdParts), 
                rsEpisode.getString("label"), layer.getId());
              episodeTag.setParentId(fmtAnnotationId.format(parentAnnotationIdParts));
              if (rsEpisode.getString("annotated_by") != null)
              {
                episodeTag.setAnnotator(rsEpisode.getString("annotated_by"));
              }
              if (rsEpisode.getTimestamp("annotated_when") != null)
              {
                episodeTag.setWhen(rsEpisode.getTimestamp("annotated_when"));
              }
              byte[] data = rsEpisode.getBytes("data");
              try
              {
                if (data != null) episodeTag.put("data", data);
              }
              catch(Exception exception)
              {
                System.err.println("Could not get data for " + episodeTag.getId() + ": " + exception);
              }
              graph.addAnnotation(episodeTag);		     
            }
            rsEpisode.close();
            sqlEpisode.close();
            setStartEndLayers.add(layerId);
            continue;
          }
          else if (layerId.equals("corpus"))
          {
            graph.addLayer(layer);
            PreparedStatement sqlCorpus = getConnection().prepareStatement(
              "SELECT t.corpus_name, COALESCE(c.corpus_id, t.corpus_name) AS corpus_id"
              +" FROM transcript t"
              +" LEFT OUTER JOIN corpus c ON c.corpus_name = t.corpus_name"
              +" WHERE t.ag_id = ?");
            sqlCorpus.setInt(1, iAgId);
            ResultSet rsCorpus = sqlCorpus.executeQuery();
            if (rsCorpus.next())
            {
              // add graph-tag annotation
              Object[] annotationIdParts = {
                layer.get("@layer_id"), rsCorpus.getString("corpus_id")};
              Annotation corpus = new Annotation(
                fmtMetaAnnotationId.format(annotationIdParts), 
                rsCorpus.getString("corpus_name"), layer.getId());
              corpus.setParentId(graph.getId());
              graph.addAnnotation(corpus);		     
            }
            rsCorpus.close();
            sqlCorpus.close();
            setStartEndLayers.add(layerId);
            continue;
          }
          else if (layerId.equals("transcript_type"))
          {
            graph.addLayer(layer);
            PreparedStatement sqlType = getConnection().prepareStatement(
              "SELECT transcript_type, t.type_id"
              +" FROM transcript t"
              +" INNER JOIN transcript_type tt ON tt.type_id = t.type_id"
              +" WHERE t.ag_id = ?");
            sqlType.setInt(1, iAgId);
            ResultSet rsType = sqlType.executeQuery();
            if (rsType.next())
            {
              // add graph-tag annotation
              Object[] annotationIdParts = {"type", Integer.valueOf(iAgId)};
              Annotation type = new Annotation(
                fmtTranscriptAttributeId.format(annotationIdParts), 
                rsType.getString("transcript_type"), layer.getId());
              type.setParentId(graph.getId());
              graph.addAnnotation(type);
            }
            rsType.close();
            sqlType.close();
            setStartEndLayers.add(layerId);
            continue;
          }
	       
          if (layerId.startsWith("transcript_"))
          { // probably a transcript attribute layer
            if ("transcript".equals(layer.get("@class_id")))
            { // definitedly a transcript attribute layer
              graph.addLayer(layer);
              PreparedStatement sqlValue = getConnection().prepareStatement(
                "SELECT annotation_id, label, annotated_by, annotated_when"
                +" FROM annotation_transcript"
                +" WHERE ag_id = ? AND layer = ?");
              sqlValue.setInt(1, iAgId);
              sqlValue.setString(2, layer.get("@attribute").toString());
              ResultSet rsValue = sqlValue.executeQuery();
              boolean attributeFound = false;
              while (rsValue.next())
              {
                attributeFound = true;
                // add graph-tag annotation
                Object[] annotationIdParts = {
                  layer.get("@attribute"), Integer.valueOf(rsValue.getInt("annotation_id"))};
                Annotation attribute = new Annotation(
                  fmtTranscriptAttributeId.format(annotationIdParts), 
                  rsValue.getString("label"), layer.getId());
                if (rsValue.getString("annotated_by") != null)
                {
                  attribute.setAnnotator(rsValue.getString("annotated_by"));
                }
                if (rsValue.getTimestamp("annotated_when") != null)
                {
                  attribute.setWhen(rsValue.getTimestamp("annotated_when"));
                }
                attribute.setParentId(graph.getId());
                graph.addAnnotation(attribute);
              } 
              if (!attributeFound && layer.getId().equals("transcript_language"))
              { // transcript_language can magically inherit from corpus
                PreparedStatement sqlCorpusLanguage = getConnection().prepareStatement(
                  "SELECT corpus_language FROM corpus"
                  +" INNER JOIN transcript ON transcript.corpus_name = corpus.corpus_name"
                  +" WHERE ag_id = ?");
                sqlCorpusLanguage.setInt(1, iAgId);
                ResultSet rsCorpusLanguage = sqlCorpusLanguage.executeQuery();
                if (rsCorpusLanguage.next())
                {
                  Object[] annotationIdParts = {
                    layer.get("@attribute"), Integer.valueOf(iAgId)};
                  Annotation attribute = new Annotation(
                    fmtTranscriptAttributeId.format(annotationIdParts),
                    rsCorpusLanguage.getString("corpus_language"), 
                    layer.getId());
                  attribute.setParentId(graph.getId());
                  graph.addAnnotation(attribute);
                }
                rsCorpusLanguage.close();
                sqlCorpusLanguage.close();
              }
              rsValue.close();
              sqlValue.close();
              setStartEndLayers.add(layerId);
              continue;
            } // definitely a transcript attribute layer
          } // probably a transcript attribute layer
	       
          if (layerId.startsWith("participant_"))
          { // probably a transcript attribute layer
            if ("speaker".equals(layer.get("@class_id")))
            { // definitedly a participant attribute layer
              graph.addLayer(layer);
              PreparedStatement sqlValue = getConnection().prepareStatement(
                "SELECT a.annotation_id, a.speaker_number, a.label, a.annotated_by, a.annotated_when"
                +" FROM annotation_participant a"
                +" INNER JOIN transcript_speaker ts ON ts.speaker_number = a.speaker_number"
                +" WHERE ts.ag_id = ? AND a.layer = ?");
              sqlValue.setInt(1, iAgId);
              sqlValue.setString(2, layer.get("@attribute").toString());
              ResultSet rsValue = sqlValue.executeQuery();
              int ordinal = 1;
              while (rsValue.next())
              {
                // add graph-tag annotation
                Object[] annotationIdParts = {
                  layer.get("@attribute"), Integer.valueOf(rsValue.getInt("annotation_id"))};
                Annotation attribute = new Annotation(
                  fmtParticipantAttributeId.format(annotationIdParts), 
                  rsValue.getString("label"), layer.getId());
                if (rsValue.getString("annotated_by") != null)
                {
                  attribute.setAnnotator(rsValue.getString("annotated_by"));
                }
                if (rsValue.getTimestamp("annotated_when") != null)
                {
                  attribute.setWhen(rsValue.getTimestamp("annotated_when"));
                }
                attribute.setParentId("m_-2_"+rsValue.getString("speaker_number"));
                attribute.setOrdinal(ordinal++);
                graph.addAnnotation(attribute);
              }
              rsValue.close();
              sqlValue.close();
              setStartEndLayers.add(layerId);
              continue;
            } // definitely a transcript attribute layer
          } // probably a transcript attribute layer

          graph.addLayer(layer);
          int iLayerId = ((Integer)layer.get("@layer_id")).intValue();
          String scope = (String)layer.get("@scope");
          sqlAnnotation.setInt(1, iLayerId);
          ResultSet rsAnnotation = sqlAnnotation.executeQuery();
          while (rsAnnotation.next())
          {
            Annotation annotation = annotationFromResult(rsAnnotation, layer, graph);

            // start anchor
            if (graph.getAnchor(annotation.getStartId()) == null)
            { // start anchor isn't in graph yet
              graph.addAnchor(anchorFromResult(rsAnnotation, "start_"));
            } // start anchor isn't in graph yet 		  

            // end anchor
            if (graph.getAnchor(annotation.getEndId()) == null)
            { // start anchor isn't in graph yet
              graph.addAnchor(anchorFromResult(rsAnnotation, "end_"));
            } // start anchor isn't in graph yet 

            graph.addAnnotation(annotation);

          } // next annotation
          rsAnnotation.close();
        } // next layerId
        sqlAnnotation.close();
      } // layerIds specified

      // set anchors for graph tag layers
      SortedSet<Anchor> anchors = graph.getSortedAnchors();
      if (anchors.size() > 0)
      {
        Anchor firstAnchor = anchors.first();
        Anchor lastAnchor = anchors.last();
        for (String layerId : setStartEndLayers)
        {
          for (Annotation a : graph.list(layerId))
          {
            a.setStartId(firstAnchor.getId());
            a.setEndId(lastAnchor.getId());
          } // next annotation
        } // next layer
      } // there are anchors

      graph.commit();
      return graph;
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
  }

  /**
   * Gets a fragment of a graph, given its ID and the ID of an annotation in it that defines the 
   * desired fragment.
   * <p>The given annotation defines both the start and end anchors of the fragment, and also 
   * which annotations on descendant layers will be included. 
   * So the resulting fragment will include:
   * <ul>
   *  <li>the given defining annotation</li>
   *  <li>its parent annotation, and all ancestors, but their anchors are included only if the 
   *      defining annotation t-includes the ancestor</li>
   *  <li>all descendants of the defining annotation or any of its ancestors, 
   *      which are t-included by the defining annotation (but not annotations
   *      that the defining annotation t-includes but which aren't directly related,
   *      so in the case of simultaneous speech, only the words of the speaker of the defining
   *      utterance will be included, not words spoken by other speakers.
   *      </li>
   * </ul>
   * @param graphId The ID of the graph.
   * @param annotationId The ID of an annotation that defines the bounds of the fragment.
   * @return The identified graph fragment.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the graph was not found in the store.
   */
  public Graph getFragment(String graphId, String annotationId) 
    throws StoreException, PermissionException, GraphNotFoundException
  {
    return getFragment(graphId, annotationId, getLayerIds());
  }

  /**
   * Converts a graph-matching expression into a resultset (SELECT selectedClause FROM... WHERE... orderClause).
   * <ul>
   *  <li><code>id = 'ew_0_456'</code></li>
   *  <li><code>layer.id = 'orthography' AND label NOT MATCHES 'th[aeiou].*'</code></li>
   *  <li><code>layer.id = 'orthography' AND my('who').label = 'Robert' AND my('utterances').start.offset = 12.345</code></li>
   *  <li><code>graph.id = 'AdaAicheson-01.trs' AND layer.id = 'orthography' AND start.offset &gt; 10.5</code></li>
   *  <li><code>layer.id = 'utterances' AND 'ew_0_456' IN list('transcript')</code></li>
   *  <li><code>previous.id = 'ew_0_456'</code></li>
   * </ul>
   * <p><em>NB</em> all expressions must match by either id or layer.id.
   * @param expression The annotation-matching expression.
   * @param limit SQL LIMIT clause, if any.
   * @return A PreparedStatement for the given expression, with parameters already set.
   * @throws SQLException
   * @throws StoreException If the expression is invalid.
   */
  private PreparedStatement annotationMatchSql(String expression, String limit)
    throws SQLException, StoreException, PermissionException
  {
    String[] subexpressions = expression.split(" AND ");
    // first figure out what layer we're looking at
    Layer layer = null;
    for (String subexpression : subexpressions)
    {
      subexpression = subexpression.trim();
      if (subexpression.length() == 0) continue;
      String operator = null;
      String[] operators = {" = "," <> "," <= "," >= "," < "," > ", " NOT MATCHES "," MATCHES ", " NOT IN ", " IN "};
      String[] operands = null;
      for (String op : operators)
      {
        operands = subexpression.split(op);
        if (operands.length == 2)
        {
          operator = op.trim();
          break;
        } // match
      } // next operator
      if (operator == null) throw new StoreException("Not valid operator found in: " + expression);
      if (operands == null) throw new StoreException("No operands found in: " + expression);
      if (operands.length != 2) throw new StoreException("Wrong number of operands ("+operands.length+") found in: " + expression);
	 
      // LHS = "id"?
      if (operands[0].trim().equals("id"))
      {
        // strip quotes
        // extract text between first pair of quotes: works for "id = 'xyz'" and "id IN ('xyy, 'abc')"
        int firstQuotePos = operands[1].indexOf('\'');
        int secondQuotePos = operands[1].indexOf('\'', firstQuotePos + 1);
        if (firstQuotePos >= 0 && secondQuotePos >= 0)
        {
          operands[1] = operands[1].substring(firstQuotePos + 1, secondQuotePos);
        }
        // parse ID
        try
        {
          Object[] o = fmtAnnotationId.parse(operands[1]);
          int layer_id = ((Long)o[1]).intValue();
          PreparedStatement sqlLayer = getConnection().prepareStatement(
            "SELECT short_description FROM layer WHERE layer_id = ?");
          sqlLayer.setInt(1, layer_id);
          ResultSet rsLayer = sqlLayer.executeQuery();
          try
          {
            if (!rsLayer.next()) throw new StoreException("Invalid layer_id for " + operands[1] + ": " + layer_id);
            layer = getSchema().getLayer(rsLayer.getString("short_description"));
          }
          finally
          {
            rsLayer.close();
            sqlLayer.close();
          }
        }
        catch(ParseException exception)
        {
          throw new StoreException("Invalid annotation ID: " + operands[1]);
        }
      }
      // LHS = "layer.id" or "layerId"?
      if (operands[0].trim().equals("layer.id") || operands[0].trim().equals("layerId"))
      {
        // strip quotes
        if (operands[1].startsWith("'")) operands[1] = operands[1].substring(1);
        if (operands[1].endsWith("'")) operands[1] = operands[1].substring(0,operands[1].length() - 1);
        // unescape any remaining quotes
        operands[1] = operands[1].replaceAll("\\\\'","'");
        layer = getSchema().getLayer(operands[1]);
        break;
      }
    } // next subexpression

    if (layer == null)
    {
      throw new StoreException("Could not determine principal layer from query: " + expression);
    }
    if (layer.get("@scope") == null)
    {
      throw new StoreException("Only temporal layers are supported: " + layer.getId());
    }

    boolean transcriptJoin = false;
    boolean anchorsJoin = false;
    // now rewrite the operands of the expression
    StringBuffer extraJoins = new StringBuffer();
    int xj = 0; // extra join count, for suffixing table names uniquely
    StringBuffer conditions = new StringBuffer();
    for (String subexpression : subexpressions)
    {
      subexpression = subexpression.trim();
      if (subexpression.length() == 0) continue;
      String operator = null;
      String[] operators = {" = "," <> "," <= "," >= "," < "," > ", " NOT MATCHES "," MATCHES ", " NOT IN ", " IN "};
      String[] operands = null;
      for (String op : operators)
      {
        operands = subexpression.split(op);
        if (operands.length == 2)
        {
          operator = op.trim();
          break;
        } // match
      } // next operator
      if (operator == null) throw new StoreException("Not valid operator found in: " + expression);
      if (operands == null) throw new StoreException("No operands found in: " + expression);

      String sqlLhs = null;
      String sqlOperator = operator.replace("MATCHES","REGEXP");
      String sqlRhs = null;
      for (String operand : operands)
      {
        operand = operand.trim();
        String sqlOperand = null;
        if (operand.equals("id"))
        {
          String scope = ((String)layer.get("@scope")).toLowerCase();
          if (scope.equals("f")) scope = "";
          sqlOperand = "CONCAT('e"+scope+"_"+layer.get("@layer_id")+"_', annotation.annotation_id)";
        }
        else if (operand.equals("layer.id") || operand.equals("layerId"))
        { // we've already matched the layer ID, but to ensure the SQL is valid...
          sqlOperand = "'"+layer.getId().replaceAll("'","\\'")+"'";
        }
        else if (operand.equals("parent.id") || operand.equals("parentId"))
        {
          Layer parentLayer = getSchema().getLayer(layer.getParentId());
          String parentScope = ((String)parentLayer.get("@scope")).toLowerCase();
          if (parentScope.equals("f")) parentScope = "";
          sqlOperand = "CONCAT('e"+parentScope+"_"+parentLayer.get("@layer_id")+"_', annotation.parent_id)";
        }
        else if (operand.equals("graph.id") || operand.equals("graph.label")
                 || operand.equals("my('graph').label") || operand.equals("my('graph').id"))
        {
          sqlOperand = "graph.transcript_id";
          transcriptJoin = true;
        }
        else if (operand.equals("my('corpus').label"))
        {
          sqlOperand = "graph.corpus_name";
          transcriptJoin = true;
        }
        else if (operand.equals("my('episode').label"))
        {
          sqlOperand = "(SELECT name"
            +" FROM transcript_family"
            +" WHERE transcript_family.family_id = graph.family_id)";
          transcriptJoin = true;
        }
        else if (operand.equals("start.id"))
        {
          sqlOperand = "CONCAT('n_',annotation.start_anchor_id)";
        }
        else if (operand.equals("end.id"))
        {
          sqlOperand = "CONCAT('n_',annotation.end_anchor_id)";
        }
        else if (operand.equals("start.offset"))
        {
          sqlOperand = "start.offset";
          anchorsJoin = true;
        }
        else if (operand.equals("end.offset"))
        {
          sqlOperand = "end.offset";
          anchorsJoin = true;
        }
        else if (operand.equals("annotator"))
        {
          sqlOperand = "annotation.annotated_by";
        }
        else if (operand.equals("when"))
        {
          sqlOperand = "annotation.annotated_when";
        }
        else if (operand.equals("labels('who')") || operand.equals("my('who').label"))
        {
          if (layer.get("@scope").equals("F"))
          { // turn based on anchor offsets
            sqlOperand = "(SELECT speaker.name"
              +" FROM speaker"
              +" INNER JOIN annotation_layer_11 turn ON speaker.speaker_number = turn.label"
              +" INNER JOIN anchor turn_start_anchor ON turn.start_anchor_id = turn_start_anchor.id"
              +" INNER JOIN anchor turn_end_anchor ON turn.end_anchor_id = turn_end_anchor.id"
              +" WHERE turn.ag_id = graph.ag_id"
              // any overlap with the turn
              +" AND turn_start_anchor.offset <= end.offset"
              +" AND start.offset <= turn_end_anchor.offset"
              +" ORDER BY turn_start_anchor.offset, turn_end_anchor.offset DESC"
              +(operand.equals("my('who')")?" LIMIT 1":"")
              +")";
            anchorsJoin = true;
          } // turn based on anchor offsets
          else
          { // turn based on turn_annotation_id
            // (no need to distinguish between my() and list() because there can be only one turn)
            sqlOperand = "(SELECT speaker.name"
              +" FROM speaker"
              +" INNER JOIN annotation_layer_11 turn ON speaker.speaker_number = turn.label"
              // match turn_annotation_id
              +" WHERE turn.annotation_id = annotation.turn_annotation_id"
              +")";
          } // turn based on turn_annotation_id
        }
        else if (operand.startsWith("my('") && (operand.endsWith("').label") || operand.endsWith("').id")))
        { // my
          String layerId = operand.replaceFirst("my\\('","").replaceFirst("'\\)\\.(label|id)$","");
          Layer operandLayer = getSchema().getLayer(layerId);
          if (operandLayer == null) throw new StoreException("Invalid layer: " + layerId);
          // join by the finest-grain compatible with both layers
          String scope = (String)layer.get("@scope");
          String operandScope = (String)operandLayer.get("@scope");
          if (operandScope == null) throw new StoreException("Cannot query by non-temporal annotation layer: " + layerId);
          String[] joinFields = {
            null,
            "turn_annotation_id",
            "word_annotation_id",
            "segment_annotation_id" };
          int joinFieldIndex = 3;
          xj++;
          String selectedValue = "otherLayer_"+xj+".label";
          if (scope.equals("W")) joinFieldIndex = Math.min(joinFieldIndex, 2);
          if (operandScope.equals("W") && joinFieldIndex > 2) joinFieldIndex = Math.min(joinFieldIndex, 2);
          if (scope.equals("M")) joinFieldIndex = Math.min(joinFieldIndex, 1);
          if (operandScope.equals("M") && joinFieldIndex > 2) joinFieldIndex = Math.min(joinFieldIndex, 1);
          if (scope.equals("F")) joinFieldIndex = Math.min(joinFieldIndex, 0);
          if (operandScope.equals("F") && joinFieldIndex > 2) joinFieldIndex = Math.min(joinFieldIndex, 0);
          if (operand.endsWith("').id")) // getting id, not label
          { // construct id from scope, layer_id, and annotation_id
            String scopePrefix = operandScope.toLowerCase();
            if (operandScope.equals("F")) scopePrefix = "";
            selectedValue = "CONCAT('e"+scopePrefix+"_"+operandLayer.get("@layer_id")+"_', otherLayer_"+xj+".annotation_id)";
          }
          // if both layers are the same scope, we don't need to compare anchors
          boolean temporalJoin = !scope.equals(operandScope);
          String order = "otherLayer_start_"+xj+".offset, otherLayer_end_"+xj+".offset DESC";
          if (!temporalJoin)
          {
            order = "otherLayer_"+xj+".ordinal";
          }
          
          String joinField = joinFields[joinFieldIndex];
          extraJoins.append(
            " INNER JOIN"
            +" annotation_layer_"+operandLayer.get("@layer_id")+" otherLayer_"+xj
            +" ON otherLayer_"+xj+".ag_id = annotation.ag_id"
            +(joinField==null?"":" AND otherLayer_"+xj+"."+joinField+" = annotation."+joinField)
            +(!temporalJoin?"":
              " INNER JOIN anchor otherLayer_start_"+xj
              +" ON otherLayer_"+xj+".start_anchor_id = otherLayer_start_"+xj+".anchor_id"
              +" AND otherLayer_start_"+xj+".offset <= end.offset"
              +" INNER JOIN anchor otherLayer_end_"+xj
              +" ON otherLayer_"+xj+".end_anchor_id = otherLayer_end_"+xj+".anchor_id"
              +" AND start.offset <= otherLayer_end_"+xj+".offset"));
          sqlOperand = selectedValue;
          if (temporalJoin) anchorsJoin = true;
        } // my
        else if (operand.startsWith("list('") && operand.endsWith("')"))
        { // list()
          String layerId = operand.replaceFirst("list\\('","").replaceFirst("'\\)$","");
          Layer operandLayer = getSchema().getLayer(layerId);
          if (operandLayer == null) throw new StoreException("Invalid layer: " + layerId);
          // join by the finest-grain compatible with both layers
          String scope = (String)layer.get("@scope");
          String operandScope = (String)operandLayer.get("@scope");
          if (operandScope == null) throw new StoreException("Cannot query by non-temporal annotation layer: " + layerId);
          String[] joinFields = {
            null,
            "turn_annotation_id",
            "word_annotation_id",
            "segment_annotation_id" };
          int joinFieldIndex = 3;
          if (scope.equals("W")) joinFieldIndex = Math.min(joinFieldIndex, 2);
          if (operandScope.equals("W") && joinFieldIndex > 2) Math.min(joinFieldIndex, 2);
          if (scope.equals("M")) joinFieldIndex = Math.min(joinFieldIndex, 1);
          if (operandScope.equals("M") && joinFieldIndex > 2) Math.min(joinFieldIndex, 1);
          if (scope.equals("F")) joinFieldIndex = Math.min(joinFieldIndex, 0);
          if (operandScope.equals("F") && joinFieldIndex > 2) Math.min(joinFieldIndex, 0);
          String joinField = joinFields[joinFieldIndex];
          if (operandScope.equals("F")) operandScope = "";
          sqlOperand = "(SELECT CONCAT('e"
            +operandScope.toLowerCase()+"_"
            +operandLayer.get("@layer_id")+"_', otherLayer.annotation_id)"
            +" FROM annotation_layer_"+operandLayer.get("@layer_id")+" otherLayer"
            +" INNER JOIN anchor otherLayer_start ON otherLayer.start_anchor_id = otherLayer_start.anchor_id"
            +" INNER JOIN anchor otherLayer_end ON otherLayer.end_anchor_id = otherLayer_end.anchor_id"
            +" WHERE otherLayer.ag_id = annotation.ag_id"
            +(joinField==null?"":" AND otherLayer."+joinField+" = annotation."+joinField)
            // any overlap
            +" AND otherLayer_start.offset <= end.offset"
            +" AND start.offset <= otherLayer_end.offset"
            +" ORDER BY otherLayer_start.offset, otherLayer_end.offset DESC"
            +")";
          anchorsJoin = true;
        } // list()
        else if (operand.startsWith("list('") && operand.endsWith("').length"))
        { // list().length
          String layerId = operand.replaceFirst("list\\('","").replaceFirst("'\\)\\.length$","");
          Layer operandLayer = getSchema().getLayer(layerId);
          if (operandLayer == null) throw new StoreException("Invalid layer: " + layerId);
          // join by the finest-grain compatible with both layers
          String scope = (String)layer.get("@scope");
          String operandScope = (String)operandLayer.get("@scope");
          if (operandScope == null) throw new StoreException("Cannot query by non-temporal annotation layer: " + layerId);
          String[] joinFields = {
            null,
            "turn_annotation_id",
            "word_annotation_id",
            "segment_annotation_id" };
          int joinFieldIndex = 3;
          if (scope.equals("W")) joinFieldIndex = Math.min(joinFieldIndex, 2);
          if (operandScope.equals("W") && joinFieldIndex > 2) Math.min(joinFieldIndex, 2);
          if (scope.equals("M")) joinFieldIndex = Math.min(joinFieldIndex, 1);
          if (operandScope.equals("M") && joinFieldIndex > 2) Math.min(joinFieldIndex, 1);
          if (scope.equals("F")) joinFieldIndex = Math.min(joinFieldIndex, 0);
          if (operandScope.equals("F") && joinFieldIndex > 2) Math.min(joinFieldIndex, 0);
          String joinField = joinFields[joinFieldIndex];
          sqlOperand = "(SELECT COUNT(*)"
            +" FROM annotation_layer_"+operandLayer.get("@layer_id")+" otherLayer"
            +" INNER JOIN anchor otherLayer_start ON otherLayer.start_anchor_id = otherLayer_start.anchor_id"
            +" INNER JOIN anchor otherLayer_end ON otherLayer.end_anchor_id = otherLayer_end.anchor_id"
            +" WHERE otherLayer.ag_id = annotation.ag_id"
            +(joinField==null?"":" AND otherLayer."+joinField+" = annotation."+joinField)
            // any overlap
            +" AND otherLayer_start.offset <= end.offset"
            +" AND start.offset <= otherLayer_end.offset"
            +" ORDER BY otherLayer_start.offset, otherLayer_end.offset DESC"
            +")";
          anchorsJoin = true;
        } // list().length
        else
        {
          sqlOperand = operand;
        }
        if (sqlLhs == null)
        {
          sqlLhs = sqlOperand;
        }
        else
        {
          sqlRhs = sqlOperand;
        }
      } // next operand
      conditions.append(conditions.length() == 0?" WHERE ":" AND ");
      conditions.append(sqlLhs);
      conditions.append(" ");
      conditions.append(sqlOperator);
      conditions.append(" ");
      conditions.append(sqlRhs);
    } // next subexpression
    if (limit == null) limit = "";
    String select = "annotation.*, ? AS layer, graph.transcript_id AS graph";
    if (limit.equals("COUNT(*)"))
    {
      select = "COUNT(*), ? AS layer";
      limit = "";
    }
    else
    {
      transcriptJoin = true;
    }
    String sSql = "SELECT DISTINCT " + select
      +(anchorsJoin?", start.offset, end.offset":"") // need to SELECT for ORDER BY
      +" FROM annotation_layer_? annotation"
      +(transcriptJoin?" INNER JOIN transcript graph ON annotation.ag_id = graph.ag_id":"")
      +(anchorsJoin?" INNER JOIN anchor start ON annotation.start_anchor_id = start.anchor_id"
        +" INNER JOIN anchor end ON annotation.end_anchor_id = end.anchor_id":"")
      + extraJoins.toString()
      + conditions.toString()
      + userWhereClauseGraph(conditions.length() > 0, "graph")
      +" ORDER BY "
      +(transcriptJoin?"graph.transcript_id":"ag_id") + ", "
      +(anchorsJoin?"start.offset, end.offset, ":"")
      +"parent_id, annotation_id"
      + " " + limit;
    System.out.println("QL: " + expression);
    System.out.println("SQL: " + sSql);
    PreparedStatement sql = getConnection().prepareStatement(sSql);
    sql.setString(1, layer.getId());
    sql.setInt(2, ((Integer)layer.get("@layer_id")).intValue());
    //System.err.println(sSql);
    return sql;
  } // end of annotationMatchSql()

  /**
   * Gets the number of annotations on the given layer of the given graph.
   * @param id The ID of the graph.
   * @param layerId The ID of the layer.
   * @return A (possibly empty) array of annotations.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the graph was not found in the store.
   */
  public long countAnnotations(String id, String layerId)
    throws StoreException, PermissionException, GraphNotFoundException
  {
    return countMatchingAnnotations("graph.id = '" + id.replaceAll("'","\\'") + "' AND layer.id = '" + layerId.replaceAll("'","\\'") + "'");
  }
  /**
   * Gets the annotations on the given layer of the given graph.
   * @param id The ID of the graph.
   * @param layerId The ID of the layer.
   * @return A (possibly empty) array of annotations.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the graph was not found in the store.
   */
  public Annotation[] getAnnotations(String id, String layerId)
    throws StoreException, PermissionException, GraphNotFoundException
  {
    return getAnnotations(id, layerId, null, null);
  }
  /**
   * Gets the annotations on the given layer of the given graph.
   * @param id The ID of the graph.
   * @param layerId The ID of the layer.
   * @param pageLength The maximum number of IDs to return, or null to return all.
   * @param pageNumber The page number to return, or null to return the first page.
   * @return A (possibly empty) array of annotations.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the graph was not found in the store.
   */
  public Annotation[] getAnnotations(String id, String layerId, Integer pageLength, Integer pageNumber)
    throws StoreException, PermissionException, GraphNotFoundException
  {
    return getMatchingAnnotationsPage("graph.id = '" + id.replaceAll("'","\\'") + "' AND layer.id = '" + layerId.replaceAll("'","\\'") + "'", pageLength, pageNumber);
  }
   
  /**
   * Counts the number of annotations that match a particular pattern.
   * @param expression An expression that determines which participants match.
   * @return The number of matching annotations.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public int countMatchingAnnotations(String expression)
    throws StoreException, PermissionException
  {
    try
    {
      PreparedStatement sql = annotationMatchSql(expression, "COUNT(*)");
      ResultSet rs = sql.executeQuery();
      try
      {
        rs.next();
        return rs.getInt(1);
      }
      finally
      {
        rs.close();
        sql.close();
      }
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
  }

  /**
   * Gets a list of annotations that match a particular pattern.
   * @param expression An expression that determines which graphs match.
   * @return A list of matching {@link Annotation}s.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public Annotation[] getMatchingAnnotations(String expression)
    throws StoreException, PermissionException
  {
    return getMatchingAnnotationsPage(expression, null, null);
  }

  /**
   * Gets a list of annotations that match a particular pattern.
   * @param expression An expression that determines which graphs match.
   * @param pageLength The maximum number of annotations to return, or null to return all.
   * @param pageNumber The page number to return, or null to return the first page.
   * @return A list of matching {@link Annotation}s.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public Annotation[] getMatchingAnnotationsPage(String expression, Integer pageLength, Integer pageNumber)
    throws StoreException, PermissionException
  {
    try
    {
      String limit = "";
      if (pageLength != null)
      {
        if (pageNumber == null) pageNumber = 0;
        limit = " LIMIT " + (pageNumber * pageLength) + "," + pageLength;
      }
      PreparedStatement sql = annotationMatchSql(expression, limit);

      ResultSet rsAnnotation = sql.executeQuery();
      Layer layer = null;
      Graph graph = null;
      Vector<Annotation> annotations = new Vector<Annotation>();
      while (rsAnnotation.next())
      {
        if (layer == null)
        { // they'll all be on the same layer
          layer = getSchema().getLayer(rsAnnotation.getString("layer"));
        }
        if ("F".equals(layer.get("@scope")) // freeform layer
            || Integer.valueOf(11).equals(layer.get("@layer_id")) // turn layer
            || Integer.valueOf(11).equals(layer.get("@layer_id"))) // utterance layer
        { // we need graph for annotationFromResult	       
          if (graph == null || !graph.getId().equals(rsAnnotation.getInt("graph")))
          { // graph can change
            try
            {
              graph = getGraph(rsAnnotation.getString("graph"), null);
            }
            catch(GraphNotFoundException exception)
            {
              System.out.println("getMatchingAnnotationsPage: " + expression + " : " + exception);
              continue;
            }
          } // need graph
        } // freeform/turn/utterance
        Annotation annotation = annotationFromResult(rsAnnotation, layer, graph);
        // annotation.setStart(anchorFromResult(rsAnnotation, "start_"));
        // annotation.setEnd(anchorFromResult(rsAnnotation, "end_"));
        if (layer.getId().equals(getSchema().getUtteranceLayerId())
            || layer.getId().equals(getSchema().getTurnLayerId()))
        {
          // label is speaker.speaker_number but should be speaker.name
          String id = "m_-2_" + annotation.getLabel();
          Annotation participant = getParticipant(id);
          if (participant != null) annotation.setLabel(participant.getLabel());
        }
        annotations.add(annotation);
      } // next annotation
      rsAnnotation.close();
      sql.close();
	 
      return annotations.toArray(new Annotation[0]);	 
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
  }

  /**
   * Gets the given anchors in the given graph.
   * @param id The ID of the graph.
   * @param anchorIds An array of anchor IDs.
   * @return A (possibly empty) array of anchors.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the graph was not found in the store.
   */
  public Anchor[] getAnchors(String id, String[] anchorIds)
    throws StoreException, PermissionException, GraphNotFoundException
  {
    try
    {
      Vector<Anchor> anchors = new Vector<Anchor>();
      PreparedStatement sqlAnchor = getConnection().prepareStatement(
        "SELECT anchor_id, offset, alignment_status, annotated_by, annotated_when"
        +" FROM anchor"
        +" WHERE anchor_id = ?");
      try
      {
        for (String anchorId : anchorIds)
        {
          try
          {
            Object[] o = fmtAnchorId.parse(anchorId);
            Long databaseId = (Long)o[0];
            sqlAnchor.setLong(1, databaseId);
            ResultSet rsAnchor = sqlAnchor.executeQuery();
            if (rsAnchor.next())
            {
              Anchor anchor = anchorFromResult(rsAnchor, "");
              anchors.add(anchor);
            }
            rsAnchor.close();
          }
          catch(ClassCastException castX)
          {
            throw new StoreException("Not a valid anchor ID: " + anchorId);
          }
          catch(ParseException parseX)
          {
            throw new StoreException("Not an anchor ID: " + anchorId);
          }
        } // next anchor
      }
      finally
      {
        sqlAnchor.close();
      }
      return anchors.toArray(new Anchor[0]);
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
  }
   
  /**
   * Gets a fragment of a graph, given its ID and the ID of an annotation in it that defines the 
   * desired fragment, and containing only the given layers.
   * <p>The given annotation defines both the start and end anchors of the fragment, and also 
   * which annotations on descendant layers will be included. 
   * So the resulting fragment will include:
   * <ul>
   *  <li>the given defining annotation</li>
   *  <li>its parent annotation, and all ancestors, but their anchors are included only if the 
   *      defining annotation t-includes the ancestor</li>
   *  <li>all descendants of the defining annotation or any of its ancestors, in the given
   *      layers, which are t-included by the defining annotation (but not annotations from
   *      those layers that the defining annotation t-includes but which aren't directly related,
   *      so in the case of simultaneous speech, only the words of the speaker of the defining
   *      utterance will be included, not words spoken by other speakers.
   *      </li>
   * </ul>
   * All annotations included in the fragment also have their {@link Layer} definition appear in 
   * graph's {@link Schema}, whether or not they're mentioned in the <var>layerId</var> list.
   * @param graphId The ID of the graph.
   * @param annotationId The ID of an annotation that defines the bounds of the fragment.
   * @param layerIds The IDs of the layers to load, or null if only graph data is required.
   * @return The identified graph fragment.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the graph was not found in the store.
   */
  public Graph getFragment(String graphId, String annotationId, String[] layerIds) 
    throws StoreException, PermissionException, GraphNotFoundException
  {
    try
    {
      final Graph graph = getGraph(graphId, null); // load just basic information
      final int ag_id = (Integer)graph.get("@ag_id");
      Schema schema = getSchema();

      final Graph fragment = new Graph();
      fragment.setGraph(graph);
      fragment.setMediaProvider(new StoreGraphMediaProvider(fragment, this));
      fragment.put("@ag_id", graph.get("@ag_id")); 
      fragment.getSchema().copyLayerIdsFrom(schema);

      // decompose the annotation ID
      String scope = null;
      Integer layer_id = null;
      Long annotation_id = null;
      try
      { // most likely a spanning annotation like 'utterance'
        Object[] o = fmtAnnotationId.parse(annotationId);
        scope = o[0].toString();
        layer_id = ((Long)o[1]).intValue();
        annotation_id = (Long)o[2];
      }
      catch(ParseException exception)
      {
        throw new StoreException("Invalid annotation ID: " + annotationId);
      }

      // keep track of layer loading, so we don't cover old ground
      HashSet<String> loadedLayers = new HashSet<String>();
      loadedLayers.add(fragment.getLayerId()); // count the schema root as loaded

      // get the layer of the defining annotation
      PreparedStatement sqlLayer = getConnection().prepareStatement(
        "SELECT short_description FROM layer WHERE layer_id = ?");
      sqlLayer.setInt(1, layer_id);
      ResultSet rsLayer = sqlLayer.executeQuery();
      if (!rsLayer.next()) throw new StoreException("Invalid layer_id for " + annotationId + ": " + layer_id);
      final Layer definingLayer = getSchema().getLayer(rsLayer.getString("short_description"));
      rsLayer.close();
      sqlLayer.close();
      fragment.addLayer(definingLayer);
      loadedLayers.add(definingLayer.getId());
	 
      // get the defining annotation and its anchors
      PreparedStatement sqlAnnotation = getConnection().prepareStatement(
        "SELECT layer.*,"
        +" start.offset AS start_offset, start.alignment_status AS start_alignment_status,"
        +" start.annotated_by AS start_annotated_by, start.annotated_when AS start_annotated_when,"
        +" end.offset AS end_offset, end.alignment_status AS end_alignment_status,"
        +" end.annotated_by AS end_annotated_by, end.annotated_when AS end_annotated_when"
        +" FROM annotation_layer_? layer"
        +" INNER JOIN anchor start ON layer.start_anchor_id = start.anchor_id"
        +" INNER JOIN anchor end ON layer.end_anchor_id = end.anchor_id"
        +" WHERE layer.ag_id = ? AND annotation_id = ?");
      sqlAnnotation.setInt(1, layer_id);
      sqlAnnotation.setInt(2, ag_id);
      sqlAnnotation.setLong(3, annotation_id);
      ResultSet rsAnnotation = sqlAnnotation.executeQuery();
      if (!rsAnnotation.next()) throw new StoreException("Could not find annotation: " + annotationId);
      final Annotation definingAnnotation = annotationFromResult(rsAnnotation, definingLayer, fragment);
      final Anchor definingStart = anchorFromResult(rsAnnotation, "start_");
      final Anchor definingEnd = anchorFromResult(rsAnnotation, "end_");
      int definingOrdinal = definingAnnotation.getOrdinal();
      fragment.addAnchor(definingStart);
      fragment.addAnchor(definingEnd);
      rsAnnotation.close();
      if (definingStart.getOffset() == null) throw new StoreException("Fragment for " + annotationId + ": start has no offset");
      if (definingEnd.getOffset() == null) throw new StoreException("Fragment for " + annotationId + ": end has no offset");	 
      fragment.setId(Graph.FragmentId(graph, definingStart, definingEnd));
      fragment.addAnnotation(definingAnnotation);
	 
      // get all ancestors too
      Layer ancestorLayer = getLayer(definingLayer.getParentId());
      String ancestorId = definingAnnotation.getParentId();
      String childLayerId = definingLayer.getId();
      int childOrdinal = definingOrdinal;
      while (!ancestorLayer.getId().equals(fragment.getSchema().getRoot().getId()))
      {
        // add layer to schema
        fragment.addLayer(ancestorLayer);
        loadedLayers.add(ancestorLayer.getId());

        // load the parent annotation
        try
        { // most likely a spanning annotation like 'utterance'
          Object[] o = fmtAnnotationId.parse(ancestorId);
          scope = o[0].toString();
          layer_id = ((Long)o[1]).intValue();
          annotation_id = (Long)o[2];
        }
        catch(ParseException exception)
        {
          try
          { // most likely a spanning annotation like 'utterance'
            Object[] o = fmtMetaAnnotationId.parse(ancestorId);
            scope = "";
            layer_id = ((Long)o[0]).intValue();
            annotation_id = Long.valueOf(o[1].toString());
          }
          catch(ParseException exception2)
          {
            throw new StoreException("Invalid ancestor ID on layer "+ancestorLayer+": " + ancestorId);
          }
        }
	    
        if (ancestorLayer.getId().equals(schema.getParticipantLayerId()))
        {
          // get the participant's name
          PreparedStatement sqlParticipant = getConnection().prepareStatement(
            "SELECT name FROM speaker WHERE speaker_number = ?");
          sqlParticipant.setInt(1, annotation_id.intValue());
          ResultSet rsParticipant = sqlParticipant.executeQuery();
          if (rsParticipant.next())
          {
            // add participant annotation
            Annotation participant = new Annotation(
              ancestorId, rsParticipant.getString("name"), ancestorLayer.getId());
            participant.setParentId(fragment.getId());
            participant.setOrdinalMinimum(childLayerId, childOrdinal);
            fragment.addAnnotation(participant);
            ancestorId = participant.getParentId();

            childLayerId = ancestorLayer.getId();
            childOrdinal = participant.getOrdinal();
          } // participant found

          rsParticipant.close();
          sqlParticipant.close();
        }
        else
        { // not participant layer
          sqlAnnotation.setInt(1, layer_id);
          sqlAnnotation.setLong(3, annotation_id);
          rsAnnotation = sqlAnnotation.executeQuery();
          if (!rsAnnotation.next()) break;
          Annotation annotation = annotationFromResult(rsAnnotation, ancestorLayer, fragment);

          annotation.setOrdinalMinimum(childLayerId, childOrdinal);

          fragment.addAnnotation(annotation);
          ancestorId = annotation.getParentId();

          childLayerId = ancestorLayer.getId();
          childOrdinal = annotation.getOrdinal();
	       
          // add anchors?
          Anchor ancestorStart = anchorFromResult(rsAnnotation, "start_");
          Anchor ancestorEnd = anchorFromResult(rsAnnotation, "end_");
          rsAnnotation.close();
          if (definingAnnotation.includesOffset(ancestorStart.getOffset())
              && (definingAnnotation.includesOffset(ancestorEnd.getOffset())
                  || definingEnd.getOffset().equals(ancestorEnd.getOffset())))
          {
            if (fragment.getAnchor(ancestorStart.getId()) == null)
            { // start anchor isn't in graph yet
              fragment.addAnchor(ancestorStart);
            }
            if (fragment.getAnchor(ancestorEnd.getId()) == null)
            { // end anchor isn't in graph yet
              fragment.addAnchor(ancestorEnd);
            }
          }
        } // not participant layer
	    
        // next ancestor
        ancestorLayer = getLayer(ancestorLayer.getParentId());	 
      } // next ancestor
      sqlAnnotation.close();
	 
      if (layerIds != null)
      { // set up full schema
        // add specified layers
        for (String layerId : layerIds)
        {
          if (loadedLayers.contains(layerId)) continue;
          Layer layer = getLayer(layerId);
          fragment.addLayer(layer);
        }
        // we also need to add any missing layers to ensure the hierarchy is complete
        // this is because we can't tell a grandchild is a descendant of the defining 
        // annotation unless the intervening child is present
        boolean foundMissingLayers = true;
        while (foundMissingLayers)
        {
          foundMissingLayers = false;
          for (Layer layer : new Vector<Layer>(fragment.getSchema().getLayers().values()))
          {
            // is the parent in the schema?
            if (layer.getParent() == null && layer != fragment.getSchema().getRoot())
            {
              fragment.addLayer(getLayer(layer.getParentId()));
              foundMissingLayers = true;
            }
          } // next existing layer
        } // next round of checking

        final PreparedStatement sqlAnnotationsByParent = getConnection().prepareStatement(
          "SELECT layer.*,"
          +" start.offset AS start_offset, start.alignment_status AS start_alignment_status,"
          +" start.annotated_by AS start_annotated_by, start.annotated_when AS start_annotated_when,"
          +" end.offset AS end_offset, end.alignment_status AS end_alignment_status,"
          +" end.annotated_by AS end_annotated_by, end.annotated_when AS end_annotated_when"
          +" FROM annotation_layer_? layer"
          +" INNER JOIN anchor start ON layer.start_anchor_id = start.anchor_id"
          +" INNER JOIN anchor end ON layer.end_anchor_id = end.anchor_id"
          +" WHERE layer.ag_id = ? AND parent_id = ?"
          +" ORDER BY start.offset, end.offset DESC, annotation_id");
        sqlAnnotationsByParent.setInt(2, ag_id);
        final PreparedStatement sqlAnnotationsByOffset = getConnection().prepareStatement(
          "SELECT layer.*,"
          +" start.offset AS start_offset, start.alignment_status AS start_alignment_status,"
          +" start.annotated_by AS start_annotated_by, start.annotated_when AS start_annotated_when,"
          +" end.offset AS end_offset, end.alignment_status AS end_alignment_status,"
          +" end.annotated_by AS end_annotated_by, end.annotated_when AS end_annotated_when"
          +" FROM annotation_layer_? layer"
          +" INNER JOIN anchor start ON layer.start_anchor_id = start.anchor_id"
          +" INNER JOIN anchor end ON layer.end_anchor_id = end.anchor_id"
          +" WHERE layer.ag_id = ? AND parent_id = ?"
          + " AND start.offset >= ? AND end.offset <= ?"
          +" ORDER BY start.offset, end.offset DESC, annotation_id");
        sqlAnnotationsByOffset.setInt(2, ag_id);

        final PreparedStatement sqlTranscriptAttribute = getConnection().prepareStatement(
          "SELECT * FROM annotation_transcript WHERE ag_id = ? AND layer = ?");
        sqlTranscriptAttribute.setInt(1, ag_id);
        final PreparedStatement sqlCorpusLanguage = getConnection().prepareStatement(
          "SELECT corpus_language FROM corpus"
          +" INNER JOIN transcript ON transcript.corpus_name = corpus.corpus_name"
          +" WHERE ag_id = ?");
        sqlCorpusLanguage.setInt(1, ag_id);

        // now we've got a complete schema, we traverse top-down through it, adding annotations
        new LayerHierarchyTraversal<HashSet<String>>(loadedLayers, fragment.getSchema())
        {
          protected void pre(Layer layer)
          {
            if (!getResult().contains(layer.getId()))
            {
              try
              {
                if (layer.get("@layer_id") != null)
                {
                  PreparedStatement sql = sqlAnnotationsByParent;
                  if (!layer.getAncestors().contains(definingLayer))
                  { // not a descendant of defining annotation
                    // has to be t-included as well
                    sql = sqlAnnotationsByOffset;
                    sql.setDouble(4, definingStart.getOffset());
                    sql.setDouble(5, definingEnd.getOffset());
                  } // not a descendant of defining annotation
                  // load all annotations on this layer that are children of known annotations
                  Integer layer_id = (Integer)layer.get("@layer_id");
                  if (layer_id >= 0)
                  {
                    sql.setInt(1, layer_id);
                    for (Annotation parent : fragment.list(layer.getParentId()))
                    {
                      String scope = null;
                      layer_id = null;
                      Long annotation_id = null;
                      if (layer.getParent().equals(fragment.getSchema().getRoot()))
                      {
                        annotation_id = ((Integer)fragment.get("@ag_id")).longValue();
                        scope = (String)layer.get("@scope");
                      }
                      else
                      {
                        try
                        { // most likely a spanning annotation like 'utterance'
                          Object[] o = fmtAnnotationId.parse(parent.getId());
                          scope = o[0].toString();
                          layer_id = ((Long)o[1]).intValue();
                          annotation_id = (Long)o[2];
                        }
                        catch(ParseException exception)
                        {
                          System.err.println("Could not parse parent ID " + parent.getId() + " on layer "+layer+": " + exception);
                        }
                      }
                      if (annotation_id == null) continue;
                      sql.setLong(3, annotation_id);
                      ResultSet rs = sql.executeQuery();
                      boolean setOrdinalMinimum = true;
                      while (rs.next())
                      {
                        Annotation annotation = annotationFromResult(rs, layer, fragment);
				    
                        if (setOrdinalMinimum)
                        {
                          parent.setOrdinalMinimum(layer.getId(), annotation.getOrdinal());
                          setOrdinalMinimum = false;
                        }
				    
                        fragment.addAnnotation(annotation);
				    
                        // add anchors?
                        Anchor start = anchorFromResult(rs, "start_");
                        Anchor end = anchorFromResult(rs, "end_");
                        if (definingAnnotation.includesOffset(start.getOffset())
                            && (definingAnnotation.includesOffset(end.getOffset())
                                || definingEnd.getOffset().equals(end.getOffset())))
                        { // add anchors too
                          if (fragment.getAnchor(start.getId()) == null)
                          { // start anchor isn't in graph yet
                            fragment.addAnchor(start);
                          }
                          if (fragment.getAnchor(end.getId()) == null)
                          { // end anchor isn't in graph yet
                            fragment.addAnchor(end);
                          }
                        } // add anchors too
                      } // next child
                      rs.close();
                    } // next parent
                  } // layer_id >= 0
                  else if (layer.getId().equals("episode"))
                  { // episode
                    Object[] annotationIdParts = {
                      layer.get("@layer_id"), graph.get("@family_id")};
                    Annotation episode = new Annotation(
                      fmtMetaAnnotationId.format(annotationIdParts), 
                      ""+graph.get("@series"), layer.getId());
                    episode.setParentId(fragment.getId());
                    fragment.addAnnotation(episode);		     
                  }
                  // TODO 'system' layers...
                } // temporal layer
                else if (layer.getId().equals("transcript_type"))
                { // transcript type
                  PreparedStatement sqlType = getConnection().prepareStatement(
                    "SELECT transcript_type, t.type_id FROM transcript t"
                    +" INNER JOIN transcript_type tt ON tt.type_id = t.type_id"
                    +" WHERE t.ag_id = ?");
                  sqlType.setInt(1, ag_id);
                  ResultSet rsType = sqlType.executeQuery();
                  if (rsType.next())
                  {
                    // add graph-tag annotation
                    Object[] annotationIdParts = {"type", Integer.valueOf(ag_id)};
                    Annotation type = new Annotation(
                      fmtTranscriptAttributeId.format(annotationIdParts), 
                      rsType.getString("transcript_type"), layer.getId());
                    type.setParentId(fragment.getId());
                    fragment.addAnnotation(type);
                  }
                  rsType.close();
                  sqlType.close();
                }
                else if (layer.getId().startsWith("transcript_"))
                { // transcript attribute
                  sqlTranscriptAttribute.setString(2, layer.get("@attribute").toString());
                  ResultSet rs = sqlTranscriptAttribute.executeQuery();
                  if (rs.next() && rs.getString("label").length() > 0)
                  {
                    Object[] annotationIdParts = {
                      layer.get("@attribute"), Integer.valueOf(rs.getInt("annotation_id"))};
                    Annotation attribute = new Annotation(
                      fmtTranscriptAttributeId.format(annotationIdParts),
                      rs.getString("label"), layer.getId());
                    attribute.setParentId(fragment.getId());
                    fragment.addAnnotation(attribute);
                  } 
                  else if (layer.getId().equals("transcript_language"))
                  { // transcript_language can magically inherit from corpus
                    rs.close();
                    rs = sqlCorpusLanguage.executeQuery();
                    if (rs.next())
                    {
                      Object[] annotationIdParts = {
                        layer.get("@attribute"), Integer.valueOf(ag_id)};
                      Annotation attribute = new Annotation(
                        fmtTranscriptAttributeId.format(annotationIdParts),
                        rs.getString("corpus_language"), layer.getId());
                      attribute.setParentId(fragment.getId());
                      fragment.addAnnotation(attribute);
                    }
                  }			  
                  rs.close();
                }
                else if (layer.getId().startsWith("participant_")) // TODO
                { // participant attribute
                }
              }
              catch(SQLException exception)
              {
                System.err.println(
                  "Couldn't load layer for fragment " + fragment.getId() + ": " + layer
                  +": " + exception);
              }
              getResult().add(layer.getId());
            } // not already loaded
          }
        };
        sqlCorpusLanguage.close();
        sqlTranscriptAttribute.close();
        sqlAnnotationsByParent.close();
        sqlAnnotationsByOffset.close();
      } // layerIds specified
	 
      // ensure that turn and utterance labels are set to the participant name
      for (Annotation a : fragment.list(fragment.getSchema().getTurnLayerId()))
      {
        Annotation who = a.my(fragment.getSchema().getParticipantLayerId());
        if (who != null) a.setLabel(who.getLabel());
      }
      for (Annotation a : fragment.list(fragment.getSchema().getUtteranceLayerId()))
      {
        Annotation who = a.my(fragment.getSchema().getParticipantLayerId());
        if (who != null) a.setLabel(who.getLabel());
      }

      fragment.commit();
      return fragment;
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
  }   

  /**
   * Gets a fragment of a graph, given its ID and the start/end offsets that define the 
   * desired fragment, and containing only the given layers.
   * @param graphId The ID of the graph.
   * @param start The start offset of the fragment.
   * @param end The end offset of the fragment.
   * @param layerId The IDs of the layers to load, or null if only graph data is required.
   * @return The identified graph fragment.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the graph was not found in the store.
   */
  public Graph getFragment(String graphId, double start, double end, String[] layerId) 
    throws StoreException, PermissionException, GraphNotFoundException
  {
    try
    {
      final Graph graph = getGraph(graphId, null); // load just basic information
      final int ag_id = (Integer)graph.get("@ag_id");
      Schema schema = getSchema();

      final Graph fragment = new Graph();
      fragment.setId(Graph.FragmentId(graph, start, end));
      fragment.setGraph(graph);
      fragment.setMediaProvider(new StoreGraphMediaProvider(fragment, this));
      fragment.put("@ag_id", graph.get("@ag_id")); 
      fragment.getSchema().copyLayerIdsFrom(schema);

      // load all annotations in the specified layers, by offset
      final PreparedStatement sqlAnnotationsByOffset = getConnection().prepareStatement(
        "SELECT layer.*,"
        +" start.offset AS start_offset, start.alignment_status AS start_alignment_status,"
        +" start.annotated_by AS start_annotated_by, start.annotated_when AS start_annotated_when,"
        +" end.offset AS end_offset, end.alignment_status AS end_alignment_status,"
        +" end.annotated_by AS end_annotated_by, end.annotated_when AS end_annotated_when"
        +" FROM annotation_layer_? layer"
        +" INNER JOIN anchor start ON layer.start_anchor_id = start.anchor_id"
        +" INNER JOIN anchor end ON layer.end_anchor_id = end.anchor_id"
          +" WHERE layer.ag_id = ?"
        + " AND start.offset >= ? AND end.offset <= ?"
        +" ORDER BY start.offset, end.offset DESC, annotation_id");
      sqlAnnotationsByOffset.setInt(2, ag_id);
      sqlAnnotationsByOffset.setDouble(3, start);
      sqlAnnotationsByOffset.setDouble(4, end);

      final HashSet<String> layersToLoad = new HashSet<String>(Arrays.asList(layerId));
      // traverse top-down through the schema, looking for layers to add
      final HashSet<String> loadedLayers = new HashSet<String>();
      new LayerHierarchyTraversal<HashSet<String>>(loadedLayers, schema)
      {
        // pre; before children means top-down
        protected void pre(Layer layer)
        {
          if (layersToLoad.contains(layer.getId()))
          { // load this layer
            Integer layer_id = (Integer)layer.get("@layer_id");
            if (layer_id != null && layer_id >= 0) // a temporal layer
            {
              fragment.addLayer((Layer)layer.clone());
              getResult().add(layer.getId());
              try
              {
                sqlAnnotationsByOffset.setInt(1, layer_id);
                ResultSet rs = sqlAnnotationsByOffset.executeQuery();
                boolean setOrdinalMinimum = true;
                while (rs.next())
                {
                  Annotation annotation = annotationFromResult(rs, layer, fragment);
                  if (layer.getId().equals(schema.getUtteranceLayerId())
                      || layer.getId().equals(schema.getTurnLayerId()))
                  {
                    // label is speaker.speaker_number but should be speaker.name
                    String id = "m_-2_" + annotation.getLabel();
                    try
                    {
                      Annotation participant = getParticipant(id);
                      if (participant != null) annotation.setLabel(participant.getLabel());
                    }
                    catch(Exception exception)
                    {}
                  }
                  System.out.println("Annotation " + annotation + " ("+layer+")" + annotation.getOrdinal());
                  
                  Annotation parent = fragment.getAnnotation(annotation.getParentId());
                  if (setOrdinalMinimum && parent != null)
                  {
                    parent.setOrdinalMinimum(layer.getId(), annotation.getOrdinal());
                    setOrdinalMinimum = false;
                  }
                  
                  fragment.addAnnotation(annotation);
                  
                  // add anchors?
                  if (fragment.getAnchor(annotation.getStartId()) == null)
                  { // start anchor isn't in graph yet
                    fragment.addAnchor(anchorFromResult(rs, "start_"));
                  }
                  if (fragment.getAnchor(annotation.getEndId()) == null)
                  { // end anchor isn't in graph yet
                    fragment.addAnchor(anchorFromResult(rs, "end_"));
                  }
                } // next annotation
                rs.close();
              }
              catch (SQLException x)
              {
                System.err.println(
                  "SqlGraphStore.getFragment: LayerHierarchyTraversal.pre: " + x);
              }
            } // temporal layer
          } // selected layer
        } // end of pre()
      }; // end of LayerHierarchyTraversal
      sqlAnnotationsByOffset.close();

      // now load all missing ancestors...

      // traverse bottom-up through the schema, looking for annotations without parents
      new LayerHierarchyTraversal<HashSet<String>>(loadedLayers, schema)
      {
        // post; after children means bottom-up
        protected void post(Layer layer)
        {
          // if we've loaded this layer
          if (getResult().contains(layer.getId()))
          { // check for missing parents
            Layer parentLayer = layer.getParent();
            if (parentLayer != null && !parentLayer.getId().equals(schema.getRoot().getId()))
            {
              if (!getResult().contains(parentLayer.getId()))
              { // haven't included the layer in the fragment yet
                fragment.addLayer((Layer)parentLayer.clone());
                getResult().add(parentLayer.getId());
              }

              // check for orphans
              for (Annotation annotation : fragment.list(layer.getId()))
              {
                if (annotation.getParent() == null)
                { // need to load the parent too
                  try
                  {
                    if (layer.getParentId().equals(schema.getParticipantLayerId()))
                    { // who
                      Annotation parent = getParticipant(annotation.getParentId());
                      if (parent != null)
                      {
                        fragment.addAnnotation(parent);
                      }
                    } // who
                    else
                    { // other layer
                      Annotation[] match = getMatchingAnnotations(
                        "id = '"+annotation.getParentId()+"'");
                      if (match.length > 0)
                      { // found parent
                        Annotation parent = match[0];
                        for (String childLayerId : parentLayer.getChildren().keySet())
                        {
                          Annotation firstChild = fragment.my(childLayerId);
                          if (firstChild != null)
                          {
                            parent.setOrdinalMinimum(childLayerId, firstChild.getOrdinal());
                          }
                        } // next child layer
                        fragment.addAnnotation(parent);
                        Vector<String> anchorsToLoad = new Vector<String>();
                        // load anchors?
                        if (fragment.getAnchor(parent.getStartId()) == null)
                        { // start anchor isn't in graph yet
                          anchorsToLoad.add(parent.getStartId());
                        }
                        if (fragment.getAnchor(parent.getEndId()) == null)
                        { // end anchor isn't in graph yet
                          anchorsToLoad.add(parent.getEndId());
                        }
                        Anchor[] anchors = getAnchors(
                          graph.getId(), anchorsToLoad.toArray(new String[0]));
                        for (Anchor anchor : anchors)
                        {
                          if (anchor.getOffset() != null
                              && anchor.getOffset() >= start
                              && anchor.getOffset() <= end)
                          {
                            graph.addAnchor(anchor);
                          }
                        } // next anchor
                      } // found parent
                    } // other layer
                  }
                  catch (Exception x)
                  {
                    System.err.println(
                      "SqlGraphStore.getFragment : LayerHierarchyTraversal.post: " + x);
                  }
                } // need to load parent
              } // next annotation
            } // layer parent should be processed
          } // have loaded this layer
        } // end of post
      }; // end of LayerHierarchyTraversal

      // if there are no bounding anchors, add dummy anchors so that media fragment will be correct
      fragment.getOrCreateAnchorAt(start);
      fragment.getOrCreateAnchorAt(end);
      
      fragment.commit();
      return fragment;
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
  }
  
  /**
   * Gets a series of fragments, given the series' ID, and only the given layers.
   * <p>This implementation expects <var>seriesId</var> to be a current <tt>result.search_id</tt>.
   * <p>The fragments are created lazily as required, so this method should return quickly.
   * @param seriesId The ID of the series.
   * @param layerId The IDs of the layers to load, or null if only graph data is required.
   * @return An enumerable series of fragments.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the series identified by <var>seriesId</var> was not found in the store.
   */
  public MonitorableSeries<Graph> getFragmentSeries(String seriesId, String[] layerId) 
    throws StoreException, PermissionException, GraphNotFoundException
  {
    try
    {
      return new ResultSeries(Long.parseLong(seriesId), this, layerId);
    }
    catch(Exception exception)
    {
      throw new StoreException(exception);
    }
  }
  
  /**
   * Saves the given graph. The graph can be partial e.g. include only some of the layers that the stored version of the graph contains, or be a fragment.
   * <p>The graph deltas are assumed to be set correctly, so if this is a new graph, then {@link Graph#getChange()} should return Change.Operation.Create, if it's an update, Change.Operation.Update, and to delete, Change.Operation.Delete.  Correspondingly, all {@link Anchor}s and {@link Annotation}s should have their changes set also.  If {@link Graph#getChanges()} returns no changes, no action will be taken, and this method returns false.
   * <p>After this method has executed, {@link Graph#commit()} is <em>not</em> called - this must be done by the caller, if they want changes to be committed.
   * @param graph The graph to save.
   * @return true if changes were saved, false if there were no changes to save.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the graph was not found in the store.
   */
  public boolean saveGraph(Graph graph) 
    throws StoreException, PermissionException, GraphNotFoundException
  {
    Timers timers = new Timers();
    timers.start("saveGraph");
    try
    {
      // validate the graph before saving it
      // TODO ensure all layers are loaded before validation
      Validator v = new Validator();
      v.setMaxLabelLength(247);
      if (graph.getChange() == Change.Operation.Create)
      {
        v.setFullValidation(true);

        // a new transcript must have corpus, episode, and transcript type

        Layer layer = graph.getLayer("corpus");
        if (layer == null)
        { // add the layer
          layer = getLayer("corpus");
          graph.addLayer(layer);
        }
        if (graph.my("corpus") == null)
        { // set to the first value
          graph.createTag(graph, layer.getId(), layer.getValidLabels().keySet().iterator().next())
            .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
        }

        layer = graph.getLayer("episode");
        if (layer == null)
        { // add the layer
          layer = getLayer("episode");
          graph.addLayer(layer);
        }
        if (graph.my("episode") == null)
        { // set to the graph name, without the extension
          graph.createTag(graph, layer.getId(), graph.getId().replaceAll("\\.[^.]*$",""))
            .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
        }
        else if (graph.my("episode").getLabel().length() == 0)
        {
          graph.my("corpus").setLabel(graph.getId().replaceAll("\\.[^.]*$",""));
          graph.my("corpus").setConfidence(Constants.CONFIDENCE_AUTOMATIC);
        }

        layer = graph.getLayer("transcript_type");
        if (layer == null)
        { // add the layer
          layer = getLayer("transcript_type");
          graph.addLayer(layer);
        }
        if (graph.my("transcript_type") == null)
        { // set to the first value
          graph.createTag(graph, layer.getId(), layer.getValidLabels().keySet().iterator().next())
            .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
        }
      }
      //v.setDebug(true);	 
      if (graph.containsKey("@valid")) // TODO remove this workaround
      {
        System.err.println("Graph " + graph.getId() + ": skipping validation");
      }
      else
      {
        timers.start("validate");
        Vector<Change> validationChanges = v.transform(graph);
        timers.end("validate");
        System.out.println("saveGraph: " + timers);
        if (v.getErrors().size() != 0)
        {
          StringBuffer messages = new StringBuffer();
          for (String s : v.getErrors())
          {
            messages.append(s);
            messages.append("\n");
          }
          System.err.println("Invalid graph: " + graph.getId() + "\n" + messages);
          if (graph.getChange() == Change.Operation.Create)
          {
            throw new StoreException("Invalid graph: " + graph.getId() + "\n" + messages);
          }
          else
          {
            System.err.println("Saving anyway..."); // TODO don't!
          }
        }
        else
        {
          System.err.println("Graph " + graph.getId() + " OK");
        }
      }

      // censor the graph?
      String censorshipRegexp = getSystemAttribute("censorshipRegexp");
      if (censorshipRegexp != null && censorshipRegexp.length() > 0)
      { // censorship required
        Schema schema = getSchema();
        Pattern censorshipPattern = Pattern.compile(censorshipRegexp);
        String censorshipLayer = getSystemAttribute("censorshipLayer");
        String censorshipLabel = getSystemAttribute("censorshipLabel");
        int censoredCount = 0;
        for (Annotation annotation : graph.list(censorshipLayer))
        {
          if (censorshipPattern.matcher(annotation.getLabel()).matches())
          { // matching annotation
            censoredCount++;
            // change all words to censorshipLabel
            for (Annotation word : annotation.list(schema.getWordLayerId()))
            {
              word.setLabel(censorshipLabel);
            } // next word
          } // matching annotation
        } // next annotation
      } // censorshipRegexp required

      if (graph.getChange() == Change.Operation.Create)
      {
        // create the graph, to generate the ag_id
        PreparedStatement sql = getConnection().prepareStatement(
          "INSERT INTO transcript (transcript_id, offset_units) VALUES (?,?)");
        sql.setString(1, graph.getId());
        sql.setString(2, graph.getOffsetUnits());
        sql.executeUpdate();
        sql.close();
        sql = getConnection().prepareStatement("SELECT LAST_INSERT_ID()");
        ResultSet rs = sql.executeQuery();
        rs.next();
        graph.put("@ag_id", Integer.valueOf(rs.getInt(1)));
        rs.close();
        sql.close();
        saveGraphChanges(graph);
      }
      else
      {
        // find the ag_id if it's not already known
        if (!graph.containsKey("@ag_id"))
        {
          PreparedStatement sql = getConnection().prepareStatement(
            "SELECT ag_id FROM transcript WHERE transcript_id = ?");
          sql.setString(1, graph.getId());
          ResultSet rs = sql.executeQuery();
          try
          {
		  
            if (!rs.next()) throw new GraphNotFoundException(graph.getId());
            graph.put("@ag_id", Integer.valueOf(rs.getInt("ag_id")));
          }
          finally
          {
            sql.close();
            rs.close();
          }
	       
        }
      }
      // parse from string, just in case it was set as a String not an Integer
      int iAgId = Integer.parseInt(graph.get("@ag_id").toString());

      // check changes
      Vector<Change> changes = graph.getChanges();
      if (changes.size() == 0) return false;

      timers.start("changes");
      Object lastObject = graph;
      for (Change change : changes)
      {
        if (change.getObject() == lastObject) continue; // already did this object
        lastObject = change.getObject();

        // must be able to parse object's ID
        if (change.getObject() instanceof Anchor)
        {
          try
          {
            if (change.getObject().getChange() != Change.Operation.Create)
            {
              Object[] o = fmtAnchorId.parse(change.getObject().getId());
              try
              {
                Long databaseId = (Long)o[0];
              }
              catch(ClassCastException castX)
              {
                throw new StoreException("Parsed anchor ID is not a Long integer:" + change.getObject().getId());
              }
            }
          }
          catch(ParseException parseX)
          {
            throw new StoreException("Could not parse anchor ID:" + change.getObject().getId());
          }
        } // Anchor change
        else if (change.getObject() instanceof Annotation)
        {
          if (change.getObject().getId().startsWith("m_"))
          {
            try
            {
              if (change.getObject().getChange() != Change.Operation.Create)
              {
                Object[] o = fmtMetaAnnotationId.parse(change.getObject().getId());
              }
            }
            catch(ParseException parseX)
            {
              throw new StoreException("Could not parse special annotation ID:" + change.getObject().getId());
            }
          }
          else if (change.getObject().getId().startsWith("t|"))
          {
            try
            {
              if (change.getObject().getChange() != Change.Operation.Create)
              {
                Object[] o = fmtTranscriptAttributeId.parse(change.getObject().getId());
              }
            }
            catch(ParseException parseX)
            {
              throw new StoreException("Could not parse transcript attribute ID:" + change.getObject().getId());
            }
          }
          else if (change.getObject().getId().startsWith("p|"))
          {
            try
            {
              if (change.getObject().getChange() != Change.Operation.Create)
              {
                Object[] o = fmtParticipantAttributeId.parse(change.getObject().getId());
              }
            }
            catch(ParseException parseX)
            {
              throw new StoreException("Could not parse participant attribute ID:" + change.getObject().getId());
            }
          }
          else
          {
            try
            {
              if (change.getObject().getChange() != Change.Operation.Create)
              {
                Object[] o = fmtAnnotationId.parse(change.getObject().getId());
                String scope = o[0].toString();
                if (scope.length() != 0 
                    && !scope.equalsIgnoreCase(SqlConstants.SCOPE_EPISODE) 
                    && !scope.equalsIgnoreCase(SqlConstants.SCOPE_META) 
                    && !scope.equalsIgnoreCase(SqlConstants.SCOPE_WORD) 
                    && !scope.equalsIgnoreCase(SqlConstants.SCOPE_SEGMENT) 
                    && !scope.equalsIgnoreCase(SqlConstants.SCOPE_PARTICIPANT))
                {
                  throw new StoreException("Parsed annotation scope is not recognised:" + change.getObject().getId() + " - " + scope);
                }
                try
                {
                  Long layerId = (Long)o[1];
                }
                catch(ClassCastException castX)
                {
                  throw new StoreException("Parsed annotation layer ID is not an Integer:" + change.getObject().getId() + " - " + o[1]);
                }
                try
                {
                  Long databaseId = (Long)o[2];
                }
                catch(ClassCastException castX)
                {
                  throw new StoreException("Parsed annotation ID is not a Long integer:" + change.getObject().getId() + " - " + o[2]);
                }
              }
            }
            catch(ParseException parseX)
            {
              throw new StoreException("Could not parse annotation ID:" + change.getObject().getId());
            }
          } // not a participant annotation
        } // Annotation change
        else
        {
          // unknown object type
          throw new StoreException("Unknown object type for change:" + change 
                                   + " - " + change.getObject().getClass().getName());
        }
      } // next change

      // create a lookup list of participant names
      HashMap<String,String> participantNameToNumber = new HashMap<String,String>();
      PreparedStatement sqlParticipant = getConnection().prepareStatement(
        "SELECT speaker.speaker_number, speaker.name"
        +" FROM speaker"
        +" INNER JOIN transcript_speaker ON transcript_speaker.speaker_number = speaker.speaker_number"
        +" WHERE ag_id = ? ORDER BY speaker.name");
      sqlParticipant.setInt(1, iAgId);
      ResultSet rsParticipant = sqlParticipant.executeQuery();
      while (rsParticipant.next())
      {
        participantNameToNumber.put(
          rsParticipant.getString("name"), rsParticipant.getString("speaker_number"));
      } // next participant
      rsParticipant.close();
      sqlParticipant.close();

      // process changes

      PreparedStatement sqlLastId = getConnection().prepareStatement("SELECT LAST_INSERT_ID()");
      PreparedStatement sqlInsertAnchor = getConnection().prepareStatement(
        "INSERT INTO anchor (ag_id, offset, alignment_status, annotated_by, annotated_when) VALUES (?, ?, ?, ?, ?)");
      sqlInsertAnchor.setInt(1, iAgId);
      PreparedStatement sqlUpdateAnchor = getConnection().prepareStatement(
        "UPDATE anchor SET offset = ?, alignment_status = ?, annotated_by = ?, annotated_when = ? WHERE anchor_id = ?");
      PreparedStatement sqlCheckAnchor = getConnection().prepareStatement(
        "SELECT COUNT(*) FROM annotation_layer_? WHERE start_anchor_id = ? OR end_anchor_id = ?");
      // create a list of layers to check before deleting an anchor
      PreparedStatement sqlLayers = getConnection().prepareStatement(
        "SELECT layer_id FROM layer ORDER BY layer_id");
      ResultSet rsLayers = sqlLayers.executeQuery();
      HashSet<Integer> layerIds = new HashSet<Integer>();
      while (rsLayers.next()) layerIds.add(Integer.valueOf(rsLayers.getInt("layer_id")));
      rsLayers.close();
      sqlLayers.close();
      PreparedStatement sqlDeleteAnchor = getConnection().prepareStatement(
        "DELETE FROM anchor WHERE anchor_id = ?");

      PreparedStatement sqlInsertFreeformAnnotation = getConnection().prepareStatement(
        "INSERT INTO annotation_layer_?"
        + " (ag_id, label, label_status, start_anchor_id, end_anchor_id,"
        + " parent_id, ordinal, annotated_by, annotated_when)"
        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
      sqlInsertFreeformAnnotation.setInt(2, iAgId);
      PreparedStatement sqlInsertMetaAnnotation = getConnection().prepareStatement(
        "INSERT INTO annotation_layer_?"
        + " (ag_id, label, label_status, start_anchor_id, end_anchor_id,"
        + " parent_id, ordinal, annotated_by, annotated_when,"
        + " turn_annotation_id)"
        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
      sqlInsertMetaAnnotation.setInt(2, iAgId);
      PreparedStatement sqlInsertWordAnnotation = getConnection().prepareStatement(
        "INSERT INTO annotation_layer_?"
        + " (ag_id, label, label_status, start_anchor_id, end_anchor_id,"
        + " parent_id, ordinal, annotated_by, annotated_when,"
        + " turn_annotation_id,"
        + " ordinal_in_turn, word_annotation_id)"
        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
      sqlInsertWordAnnotation.setInt(2, iAgId);
      PreparedStatement sqlInsertSegmentAnnotation = getConnection().prepareStatement(
        "INSERT INTO annotation_layer_?"
        + " (ag_id, label, label_status, start_anchor_id, end_anchor_id,"
        + " parent_id, ordinal, annotated_by, annotated_when,"
        + " turn_annotation_id," 
        + " ordinal_in_turn, word_annotation_id,"
        + " ordinal_in_word, segment_annotation_id)"
        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
      sqlInsertSegmentAnnotation.setInt(2, iAgId);
      PreparedStatement sqlUpdateFreeformAnnotation = getConnection().prepareStatement(
        "UPDATE annotation_layer_?"
        + " SET label = ?, label_status = ?, start_anchor_id = ?, end_anchor_id = ?,"
        + " parent_id = ?, ordinal = ?, annotated_by = ?, annotated_when = ?"
        + " WHERE annotation_id = ?");
      PreparedStatement sqlUpdateMetaAnnotation = getConnection().prepareStatement(
        "UPDATE annotation_layer_?"
        + " SET label = ?, label_status = ?, start_anchor_id = ?, end_anchor_id = ?,"
        + " turn_annotation_id = ?,"
        + " parent_id = ?, ordinal = ?, annotated_by = ?, annotated_when = ?"
        + " WHERE annotation_id = ?");
      PreparedStatement sqlUpdateWordAnnotation = getConnection().prepareStatement(
        "UPDATE annotation_layer_?"
        + " SET label = ?, label_status = ?, start_anchor_id = ?, end_anchor_id = ?,"
        + " turn_annotation_id = ?,"
        + " ordinal_in_turn = ?, word_annotation_id = ?,"
        + " parent_id = ?, ordinal = ?, annotated_by = ?, annotated_when = ?"
        + " WHERE annotation_id = ?");
      PreparedStatement sqlUpdateSegmentAnnotation = getConnection().prepareStatement(
        "UPDATE annotation_layer_?"
        + " SET label = ?, label_status = ?, start_anchor_id = ?,end_anchor_id = ?,"
        + " turn_annotation_id = ?,"
        + " ordinal_in_turn = ?, word_annotation_id = ?,"
        + " ordinal_in_word = ?, segment_annotation_id = ?,"
        + " parent_id = ?, ordinal = ?, annotated_by = ?, annotated_when = ?" 
        + " WHERE annotation_id = ?");
      PreparedStatement sqlSelectWordFields = getConnection().prepareStatement(
        "SELECT turn_annotation_id, ordinal_in_turn"
        + " FROM annotation_layer_" + SqlConstants.LAYER_TRANSCRIPTION
        + " WHERE annotation_id = ?");
      PreparedStatement sqlSelectSegmentFields = getConnection().prepareStatement(
        "SELECT ordinal_in_word"
        + " FROM annotation_layer_" + SqlConstants.LAYER_SEGMENT
        + " WHERE annotation_id = ?");
      PreparedStatement sqlUpdateTurnAnnotationId = getConnection().prepareStatement(
        "UPDATE annotation_layer_? SET turn_annotation_id = ? WHERE annotation_id = ?");
      PreparedStatement sqlUpdateWordAnnotationId = getConnection().prepareStatement(
        "UPDATE annotation_layer_? SET word_annotation_id = ? WHERE annotation_id = ?");
      PreparedStatement sqlUpdateSegmentAnnotationId = getConnection().prepareStatement(
        "UPDATE annotation_layer_? SET segment_annotation_id = ? WHERE annotation_id = ?");
      PreparedStatement sqlDeleteAnnotation = getConnection().prepareStatement(
        "DELETE FROM annotation_layer_? WHERE annotation_id = ?");

      PreparedStatement sqlInsertTranscriptAttribute = getConnection().prepareStatement(
        "INSERT INTO annotation_transcript (ag_id, layer, label, annotated_by, annotated_when) VALUES (?,?,?,?,?)");
      sqlInsertTranscriptAttribute.setInt(1, iAgId);
      PreparedStatement sqlUpdateTranscriptAttribute = getConnection().prepareStatement(
        "UPDATE annotation_transcript SET label = ?, annotated_by = ?, annotated_when = ? WHERE ag_id = ? AND layer = ? AND annotation_id = ?");
      sqlUpdateTranscriptAttribute.setInt(4, iAgId);
      PreparedStatement sqlDeleteTranscriptAttribute = getConnection().prepareStatement(
        "DELETE FROM annotation_transcript WHERE ag_id = ? AND layer = ? AND annotation_id = ?");
      sqlDeleteTranscriptAttribute.setInt(1, iAgId);
      // participant attributes look like children of graph in the schema
      // but they're actually store-wide entities.
      // for this reason, REPLACE INTO is required - an attribute that already exists
      // for the speaker may also be created by an uploaded graph.
      PreparedStatement sqlInsertParticipantAttribute = getConnection().prepareStatement(
        "REPLACE INTO annotation_participant (speaker_number, layer, label, annotated_by, annotated_when) VALUES (?,?,?,?,?)");
      PreparedStatement sqlUpdateParticipantAttribute = getConnection().prepareStatement(
        "UPDATE annotation_participant SET label = ?, annotated_by = ?, annotated_when = ? WHERE layer = ? AND annotation_id = ?");
      PreparedStatement sqlDeleteParticipantAttribute = getConnection().prepareStatement(
        "DELETE FROM annotation_participant WHERE layer = ? AND annotation_id = ?");
      PreparedStatement sqlDeleteAllParticipantAttributesOnLayer = getConnection().prepareStatement(
        "DELETE FROM annotation_participant WHERE speaker_number = ? AND layer = ?");

      PreparedStatement sqlAttributeLayers = getConnection().prepareStatement(
        "SELECT attribute FROM attribute_definition WHERE class_id = ?");
      HashSet<String> transcriptAttributeLayers = new HashSet<String>();
      sqlAttributeLayers.setString(1, "transcript");
      ResultSet rsAttributeLayers = sqlAttributeLayers.executeQuery();
      while (rsAttributeLayers.next())
      {
        transcriptAttributeLayers.add("transcript_"+rsAttributeLayers.getString("attribute"));
      } // next layer
      rsAttributeLayers.close();
      HashSet<String> participantAttributeLayers = new HashSet<String>();
      sqlAttributeLayers.setString(1, "speaker");
      rsAttributeLayers = sqlAttributeLayers.executeQuery();
      while (rsAttributeLayers.next())
      {
        participantAttributeLayers.add("participant_"+rsAttributeLayers.getString("attribute"));
      } // next layer
      rsAttributeLayers.close();
      sqlAttributeLayers.close();

      try
      {
        // there's a change for each changed attribute of each object
        // but we'll update the whole object when we get to the first change, 
        // then skip subsequent change elements until the next object is encountered
        lastObject = graph; // TODO save graph changes?
        // it's also possible that some annotations will change on the way that were
        // otherwise unchanged - e.g. as final anchor IDs are set, etc.
        HashSet<Annotation> extraUpdates = new HashSet<Annotation>();

        // multi-value attributes are implemented by concatenating values together,
        // so we gather up attribute changes, and process them afterwards
        LinkedHashMap<String,Annotation> transcriptAttributes = new LinkedHashMap<String,Annotation>(); 
        LinkedHashMap<String,Annotation> participantAttributes = new LinkedHashMap<String,Annotation>(); 
        for (Change change : changes)
        {
          if (change.getObject() == lastObject) continue; // already did this object
          lastObject = change.getObject();
          if (change.getObject() == graph)
          {
            saveGraphChanges(graph);
          } // Anchor change
          if (change.getObject() instanceof Anchor)
          {
            saveAnchorChanges((Anchor)change.getObject(), extraUpdates, 
                              sqlInsertAnchor, sqlLastId, sqlUpdateAnchor, 
                              sqlCheckAnchor, layerIds, sqlDeleteAnchor);
          } // Anchor change
          else if (change.getObject() instanceof Annotation
                   && !(change.getObject() instanceof Graph))
          {
            Annotation annotation = (Annotation)change.getObject();
            if (annotation.getLayerId().equals("episode")
                || annotation.getLayerId().equals("corpus")
                || annotation.getLayerId().equals("transcript_type")
                || annotation.getLayerId().equals("main_participant")
                || annotation.getLayerId().equals("who"))
            { // special layer annotation
              saveSpecialAnnotationChanges(annotation, participantNameToNumber);
            }
            else if (annotation.getLayer().getAncestors().contains(graph.getLayer("episode")))
            { // episode tag annotation
              saveEpisodeAnnotationChanges(annotation, sqlLastId);
            }
            else if (transcriptAttributeLayers.contains(annotation.getLayerId()))
            { // transcript attribute
              saveTranscriptAttributeChanges(
                annotation, 
                sqlInsertTranscriptAttribute, 
                sqlUpdateTranscriptAttribute, 
                sqlDeleteTranscriptAttribute);
            }
            else if (participantAttributeLayers.contains(annotation.getLayerId()))
            { // participant attribute
              saveParticipantAttributeChanges(
                annotation, 
                sqlInsertParticipantAttribute, 
                sqlUpdateParticipantAttribute, 
                sqlDeleteParticipantAttribute,
                sqlDeleteAllParticipantAttributesOnLayer);
            }
            else
            { // temporal annotation
              saveAnnotationChanges(
                annotation, extraUpdates, 
                sqlInsertFreeformAnnotation, sqlInsertMetaAnnotation, 
                sqlInsertWordAnnotation, sqlInsertSegmentAnnotation, sqlLastId, 
                sqlUpdateTurnAnnotationId, sqlUpdateWordAnnotationId, sqlUpdateSegmentAnnotationId, 
                sqlUpdateFreeformAnnotation, sqlUpdateMetaAnnotation, 
                sqlSelectWordFields, sqlSelectSegmentFields, 
                sqlUpdateWordAnnotation, sqlUpdateSegmentAnnotation, 
                sqlDeleteAnnotation,
                participantNameToNumber);
            }
          } // Annotation change
        } // next change
        timers.end("changes");
        System.out.println("saveGraph: " + timers);
	    
        // extras
        HashSet<Annotation> newExtraUpdates = new HashSet<Annotation>();
        for (Annotation annotation : extraUpdates)
        {
          saveAnnotationChanges(
            annotation, newExtraUpdates, 
            sqlInsertFreeformAnnotation, sqlInsertMetaAnnotation, 
            sqlInsertWordAnnotation, sqlInsertSegmentAnnotation, sqlLastId, 
            sqlUpdateTurnAnnotationId, sqlUpdateWordAnnotationId, sqlUpdateSegmentAnnotationId, 
            sqlUpdateFreeformAnnotation, sqlUpdateMetaAnnotation, 
            sqlSelectWordFields, sqlSelectSegmentFields, 
            sqlUpdateWordAnnotation, sqlUpdateSegmentAnnotation, 
            sqlDeleteAnnotation,
            participantNameToNumber);
        }
        assert newExtraUpdates.size() == 0 : "newExtraUpdates.size() == 0";

        // untag anchors and annotations
        for (Anchor a : graph.getAnchors().values()) a.remove("@SqlUpdated");
        for (Annotation a : graph.getAnnotationsById().values()) a.remove("@SqlUpdated");
      }
      finally
      {
        sqlInsertAnchor.close();
        sqlUpdateAnchor.close();
        sqlCheckAnchor.close();
        sqlDeleteAnchor.close();
        sqlInsertFreeformAnnotation.close();
        sqlInsertMetaAnnotation.close();
        sqlInsertWordAnnotation.close();
        sqlInsertSegmentAnnotation.close();
        sqlLastId.close();
        sqlUpdateTurnAnnotationId.close();
        sqlUpdateWordAnnotationId.close();
        sqlUpdateSegmentAnnotationId.close();
        sqlUpdateFreeformAnnotation.close();
        sqlUpdateMetaAnnotation.close();
        sqlSelectWordFields.close();
        sqlSelectSegmentFields.close();
        sqlUpdateWordAnnotation.close();
        sqlUpdateSegmentAnnotation.close();
        sqlDeleteAnnotation.close();
        sqlInsertTranscriptAttribute.close();
        sqlUpdateTranscriptAttribute.close();
        sqlDeleteTranscriptAttribute.close();
        sqlInsertParticipantAttribute.close();
        sqlUpdateParticipantAttribute.close();
        sqlDeleteParticipantAttribute.close();
        sqlDeleteAllParticipantAttributesOnLayer.close();
      }
    }
    catch(SQLException exception)
    {
      System.err.println(exception.toString());
      throw new StoreException(exception);
    }
    catch(TransformationException invalid)
    {
      System.err.println(invalid.toString());
      throw new StoreException("Graph was not valid", invalid);
    }
    catch(Throwable exception)
    {
      System.err.println(exception.toString());
      throw new StoreException(exception);
    }
    System.err.println("saveGraph finished.");
    timers.end("saveGraph");
    System.out.println("saveGraph: " + timers);
    return true;
  }

  /**
   * Saves the changes to the given graph.
   * @param graph The graph to save the changes of.
   * @throws SQLException If a database error occurs.
   */
  protected void saveGraphChanges(Graph graph) throws SQLException
  {
    // TODO save ordinal as episode index
    // TODO update offset in episode
  }   


  /**
   * Constructs an Annotation from the given query result row.
   * @param rsAnnotation The query result row.
   * @param layer The annotation layer.
   * @param graph The graph. This can be null, in which case turns/utterances will not have their participant name resolved, and freeform layers will not have their parent set
   * @return The annotation defined by the given row.
   * @throws SQLException
   */
  protected Annotation annotationFromResult(ResultSet rsAnnotation, Layer layer, Graph graph)
    throws SQLException
  {
    int iLayerId = ((Integer)layer.get("@layer_id")).intValue();
    String scope = (String)layer.get("@scope");
    Annotation annotation = new Annotation();
    Object[] annotationIdParts = {
      scope.toLowerCase(), Integer.valueOf(iLayerId), 
      Long.valueOf(rsAnnotation.getLong("annotation_id"))};
    if (scope.equalsIgnoreCase(SqlConstants.SCOPE_FREEFORM)) annotationIdParts[0] = "";
    annotation.setId(fmtAnnotationId.format(annotationIdParts));
    String turnParentId = null;
    if (iLayerId == SqlConstants.LAYER_TURN 
        || iLayerId == SqlConstants.LAYER_UTTERANCE) // turn or utterance
    { // convert speaker_number label into participant name
      turnParentId = "m_-2_"+rsAnnotation.getString("label");
      Annotation participant = null;
      if (graph != null)
      {
        participant = graph.getAnnotation(turnParentId);
      }
      if (participant != null)
      {
        annotation.setLabel(participant.getLabel());
      }
      else
      {
        annotation.setLabel(rsAnnotation.getString("label"));
      }
    }
    else
    {
      annotation.setLabel(rsAnnotation.getString("label"));
    }
    annotation.setConfidence(Integer.valueOf(rsAnnotation.getInt("label_status")));
    annotation.setLayerId(layer.getId());
      
    // parent:
    if (iLayerId == SqlConstants.LAYER_SEGMENT) // segment
    {
      annotationIdParts[0] = SqlConstants.SCOPE_WORD;
      annotationIdParts[1] = Integer.valueOf(SqlConstants.LAYER_TRANSCRIPTION); // transcript word
      annotationIdParts[2] = Long.valueOf(rsAnnotation.getLong("word_annotation_id"));
      annotation.setParentId(fmtAnnotationId.format(annotationIdParts));
      annotation.setOrdinal(rsAnnotation.getInt("ordinal_in_word"));
    }
    else if (iLayerId == SqlConstants.LAYER_TRANSCRIPTION) // transcription word
    {
      annotationIdParts[0] = SqlConstants.SCOPE_META;
      annotationIdParts[1] = Integer.valueOf(SqlConstants.LAYER_TURN); // turn
      annotationIdParts[2] = Long.valueOf(rsAnnotation.getLong("turn_annotation_id"));
      annotation.setParentId(fmtAnnotationId.format(annotationIdParts));
      annotation.setOrdinal(rsAnnotation.getInt("ordinal_in_turn"));
    }
    else if (iLayerId == SqlConstants.LAYER_UTTERANCE) // utterance
    {
      annotationIdParts[0] = SqlConstants.SCOPE_META;
      annotationIdParts[1] = Integer.valueOf(SqlConstants.LAYER_TURN); // turn
      annotationIdParts[2] = Long.valueOf(rsAnnotation.getLong("turn_annotation_id"));
      annotation.setParentId(fmtAnnotationId.format(annotationIdParts));
      annotation.setOrdinal(rsAnnotation.getInt("ordinal"));
    }
    else if (iLayerId == SqlConstants.LAYER_TURN) // turn
    {
      annotation.setParentId(turnParentId);
      annotation.setOrdinal(rsAnnotation.getInt("ordinal"));
    }
    else if (scope.equalsIgnoreCase(SqlConstants.SCOPE_SEGMENT)) // segment scope
    {
      annotationIdParts[0] = SqlConstants.SCOPE_SEGMENT;
      annotationIdParts[1] = Integer.valueOf(SqlConstants.LAYER_SEGMENT); // segment
      annotationIdParts[2] = Long.valueOf(rsAnnotation.getLong("segment_annotation_id"));
      annotation.setParentId(fmtAnnotationId.format(annotationIdParts));
      annotation.setOrdinal(rsAnnotation.getInt("ordinal"));
    } // segment scope
    else if (scope.equalsIgnoreCase(SqlConstants.SCOPE_WORD)) // word scope
    {
      annotationIdParts[0] = SqlConstants.SCOPE_WORD;
      annotationIdParts[1] = Integer.valueOf(SqlConstants.LAYER_TRANSCRIPTION); // transcription word
      annotationIdParts[2] = Long.valueOf(rsAnnotation.getLong("word_annotation_id"));
      annotation.setParentId(fmtAnnotationId.format(annotationIdParts));
      annotation.setOrdinal(rsAnnotation.getInt("ordinal"));
    } // word scope
    else if (scope.equalsIgnoreCase(SqlConstants.SCOPE_META)) // meta scope
    {
      annotationIdParts[0] = SqlConstants.SCOPE_META;
      annotationIdParts[1] = Integer.valueOf(SqlConstants.LAYER_TURN); // turn
      annotationIdParts[2] = Long.valueOf(rsAnnotation.getLong("turn_annotation_id"));
      annotation.setParentId(fmtAnnotationId.format(annotationIdParts));
      annotation.setOrdinal(rsAnnotation.getInt("ordinal"));
    } // meta scope
    else // freeform scope
    {
      if (graph != null)
      {
        annotation.setParentId(graph.getId());
      }
      annotation.setOrdinal(rsAnnotation.getInt("ordinal"));
    } // freeform scope

    if (rsAnnotation.getString("annotated_by") != null)
    {
      annotation.setAnnotator(rsAnnotation.getString("annotated_by"));
    }
    if (rsAnnotation.getTimestamp("annotated_when") != null)
    {
      annotation.setWhen(rsAnnotation.getTimestamp("annotated_when"));
    }

    // anchor IDs
    Object[] anchorIdParts = { Long.valueOf(rsAnnotation.getLong("start_anchor_id"))};
    annotation.setStartId(fmtAnchorId.format(anchorIdParts));
    anchorIdParts[0] = Long.valueOf(rsAnnotation.getLong("end_anchor_id"));
    annotation.setEndId(fmtAnchorId.format(anchorIdParts));
		  
    return annotation;
  } // end of annotationFromResult()

  /**
   * Constructs an anchor from the given query result row.
   * @param rsAnchor The query results row containing the definition.
   * @param prefix The prefix for field names.
   * @return The anchor defined by the row.
   * @throws SQLException
   */
  protected Anchor anchorFromResult(ResultSet rsAnchor, String prefix)
    throws SQLException
  {
    Object[] anchorIdParts = { Long.valueOf(rsAnchor.getLong(prefix + "anchor_id"))};
    Anchor anchor = new Anchor(
      fmtAnchorId.format(anchorIdParts), Double.valueOf(rsAnchor.getDouble(prefix + "offset")));
    anchor.setConfidence(Integer.valueOf(rsAnchor.getInt(prefix + "alignment_status")));
    if (rsAnchor.getString(prefix+"annotated_by") != null)
    {
      anchor.setAnnotator(rsAnchor.getString(prefix+"annotated_by"));
    }
    if (rsAnchor.getTimestamp(prefix+"annotated_when") != null)
    {
      anchor.setWhen(rsAnchor.getTimestamp(prefix+"annotated_when"));
    }
    return anchor;
  } // end of anchorFromResult()


  /**
   * Saves the changes to the given anchor, and updates related annotations if the anchor ID is changed.
   * @param anchor The anchor whose changes should be saved.
   * @param extraUpdates A set to add annotations which had no changes to save, but which now must be updated because the anchor's ID is changing.
   * @param sqlInsertAnchor Prepared statement for inserting an anchor row.
   * @param sqlLastId Prepared statement for retrieving the last database ID created.
   * @param sqlUpdateAnchor Prepared statement for updating an anchor row.
   * @param sqlCheckAnchor Prepared statement for counting the number of anchors currently using an anchor.
   * @param layerIds List of all layer_ids, for checking for annotations that use this anchor.
   * @param sqlDeleteAnchor Prepared statement for deleteing an anchor row.
   * @throws StoreException If an ID can't be parsed.
   * @throws SQLException If a database error occurs.
   */
  protected void saveAnchorChanges(Anchor anchor, HashSet<Annotation> extraUpdates, PreparedStatement sqlInsertAnchor, PreparedStatement sqlLastId, PreparedStatement sqlUpdateAnchor, PreparedStatement sqlCheckAnchor, HashSet<Integer> layerIds, PreparedStatement sqlDeleteAnchor) throws SQLException, StoreException
  {
    if (anchor.getConfidence() == null)
    {
      anchor.setConfidence(Integer.valueOf(Constants.CONFIDENCE_UNKNOWN));
    }
    switch (anchor.getChange())
    {
      case Create:
      {
        // create anchor record
        if (anchor.getOffset() != null)
        {
          sqlInsertAnchor.setDouble(2, anchor.getOffset());
        }
        else
        {
          sqlInsertAnchor.setNull(2, java.sql.Types.DOUBLE);
        }
        sqlInsertAnchor.setInt(3, anchor.getConfidence());
        if (anchor.getAnnotator() != null)
        {
          sqlInsertAnchor.setString(4, anchor.getAnnotator());
        }
        else
        {
          sqlInsertAnchor.setString(4, getUser());
        }
        // if (anchor.getWhen() != null)
        // {
        //    sqlInsertAnchor.setTimestamp(5, new Timestamp(anchor.getWhen().getTime()));
        // }
        // else
        {
          sqlInsertAnchor.setTimestamp(5, new Timestamp(new java.util.Date().getTime()));
        }
        sqlInsertAnchor.executeUpdate();
        ResultSet rs = sqlLastId.executeQuery();
        rs.next();
        String oldId = anchor.getId();
        Object[] anchorIdParts = { Long.valueOf(rs.getLong(1)) };
        String newId = fmtAnchorId.format(anchorIdParts);
        rs.close();

        // change anchor ID (this updates referencing annotations)
        anchor.setId(newId);
	    
        break;
      } // Create
      case Update:
      {
        // deduce the database anchor.anchor_id from the object anchor.id
        try
        {
          Object[] o = fmtAnchorId.parse(anchor.getId());
          Long anchorId = (Long)o[0];
          if (anchor.getOffset() != null)
          {
            sqlUpdateAnchor.setDouble(1, anchor.getOffset());
          }
          else
          {
            sqlUpdateAnchor.setNull(1, java.sql.Types.DOUBLE);
          }
          sqlUpdateAnchor.setInt(2, anchor.getConfidence());
          if (anchor.getAnnotator() != null)
          {
	       	  sqlUpdateAnchor.setString(3, anchor.getAnnotator());
          }
          else
          {
            sqlUpdateAnchor.setString(3, getUser());
          }
          // if (anchor.getWhen() != null)
          // {
          // 	  sqlUpdateAnchor.setTimestamp(4, new Timestamp(anchor.getWhen().getTime()));
          // }
          // else
          {
            sqlUpdateAnchor.setTimestamp(4, new Timestamp(new java.util.Date().getTime()));
          }
          sqlUpdateAnchor.setLong(5, anchorId);
          sqlUpdateAnchor.executeUpdate();
	       
        }
        catch(ParseException exception)
        {
          System.err.println("Error parsing anchor ID for "+anchor.getId());
          throw new StoreException("Error parsing anchor ID for "+anchor.getId(), exception);
        }
        break;
      }
      case Destroy:
      {
        // deduce the database anchor.anchor_id from the object anchor.id
        Long anchorId = null;
        try
        {
          Object[] o = fmtAnchorId.parse(anchor.getId());
          anchorId = (Long)o[0];
        }
        catch(ParseException exception)
        {
          System.err.println("Error parsing anchor ID for "+anchor.getId());
          throw new StoreException("Error parsing anchor ID for "+anchor.getId(), exception);
        }
        // check all layers in the database to see in any existing annotation uses the anchor
        for (Integer layerId : layerIds)
        {
          sqlCheckAnchor.setInt(1, layerId);
          sqlCheckAnchor.setLong(2, anchorId);
          sqlCheckAnchor.setLong(3, anchorId);
          ResultSet rs = sqlCheckAnchor.executeQuery();
          rs.next();
          if (rs.getInt(1) > 0)
          {
            // this anchor still has a reference to it so we can't delete it
            anchor.rollback();
          }
          rs.close();
        } // next layer
	    
        if (anchor.getChange() == Change.Operation.Destroy)
        { // wasn't rolled back, so go ahead and delete
          sqlDeleteAnchor.setLong(1, anchorId);
          sqlDeleteAnchor.executeUpdate();
        }

        break;
      } // Destroy
    } // switch on change type

    anchor.put("@SqlUpdated", Boolean.TRUE); // flag the anchor as having been updated
  } // end of saveAnchorChanges()

  /**
   * Saves the changes to the given anchor, and updates related annotations if the anchor ID is changed.
   * @param annotation The annotation whose changes should be saved.
   * @param extraUpdates A set to add annotations which had no changes to save, but which now must be updated because the anchor's ID is changing.
   * @param sqlInsertFreeformAnnotation Prepared statement for inserting a freeform annotation row.
   * @param sqlInsertMetaAnnotation Prepared statement for inserting a meta annotation row.
   * @param sqlInsertWordAnnotation Prepared statement for inserting a word annotation row.
   * @param sqlInsertSegmentAnnotation Prepared statement for inserting a segment annotation row.
   * @param sqlLastId Prepared statement for retrieving the last database ID created.
   * @param sqlUpdateTurnAnnotationId Prepared statement for updating turn_annotation_id.
   * @param sqlUpdateWordAnnotationId Prepared statement for updating word_annotation_id.
   * @param sqlUpdateSegmentAnnotationId Prepared statement for updating segment_annotation_id.
   * @param sqlUpdateFreeformAnnotation Prepared statement for updating a freeform annotation row.
   * @param sqlUpdateMetaAnnotation Prepared statement for updating a meta annotation row.
   * @param sqlSelectWordFields Prepared statement for finding word field values.
   * @param sqlSelectSegmentFields Prepared statement for finding segment field values.
   * @param sqlUpdateWordAnnotation Prepared statement for updating a word annotation row.
   * @param sqlUpdateSegmentAnnotation Prepared statement for updating a segment annotation row.
   * @param sqlDeleteAnnotation Prepared statement for deleteing an annotation row.
   * @param participantNameToNumber A lookup table for participant numbers, for turns/utterances
   * @throws SQLException If a database error occurs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  protected void saveAnnotationChanges(
    Annotation annotation, HashSet<Annotation> extraUpdates, 
    PreparedStatement sqlInsertFreeformAnnotation, PreparedStatement sqlInsertMetaAnnotation, 
    PreparedStatement sqlInsertWordAnnotation, PreparedStatement sqlInsertSegmentAnnotation, PreparedStatement sqlLastId, 
    PreparedStatement sqlUpdateTurnAnnotationId, PreparedStatement sqlUpdateWordAnnotationId, PreparedStatement sqlUpdateSegmentAnnotationId, 
    PreparedStatement sqlUpdateFreeformAnnotation, PreparedStatement sqlUpdateMetaAnnotation, 
    PreparedStatement sqlSelectWordFields, PreparedStatement sqlSelectSegmentFields,
    PreparedStatement sqlUpdateWordAnnotation, PreparedStatement sqlUpdateSegmentAnnotation, 
    PreparedStatement sqlDeleteAnnotation,
    HashMap<String,String> participantNameToNumber) throws SQLException, PermissionException, StoreException
  {
    if (annotation.getId().startsWith("m_")) return; // ignore participant changes for now

    if (annotation.getConfidence() == null)
    {
      annotation.setConfidence(Integer.valueOf(Constants.CONFIDENCE_UNKNOWN));
    }
    switch (annotation.getChange())
    {
      case Create:
      {
        // get the layer_id and its scope, so we can deduce what kind of row to insert
        Layer layer = annotation.getLayer();
        if (layer == null || !layer.containsKey("@layer_id"))
        { // load our own info
          layer = getLayer(annotation.getLayerId());
        }
        Integer layerId = (Integer)layer.get("@layer_id");
        String scope = (String)layer.get("@scope");
        assert scope != null : "scope != null - " + layer.getId() + " - " + layerId;

        PreparedStatement sql = sqlInsertFreeformAnnotation;
        if (scope.equalsIgnoreCase(SqlConstants.SCOPE_META))
        {
          sql = sqlInsertMetaAnnotation;
        }
        else if (scope.equalsIgnoreCase(SqlConstants.SCOPE_WORD))
        {
          sql = sqlInsertWordAnnotation;
        }
        else if (scope.equalsIgnoreCase(SqlConstants.SCOPE_SEGMENT))
        {
          sql = sqlInsertSegmentAnnotation;
        }
        sql.setInt(1, layerId);
        // parameter 2 is ag_id, which is already set
        if ((layerId.intValue() == SqlConstants.LAYER_TURN
             || layerId.intValue() == SqlConstants.LAYER_UTTERANCE)
            && participantNameToNumber.containsKey(annotation.getLabel()))
        { // label should be the speaker number, not the name
          sql.setString(3, participantNameToNumber.get(annotation.getLabel()));
        }
        else
        {
          sql.setString(3, annotation.getLabel());
        }
        sql.setInt(4, annotation.getConfidence());
        try
        {
          Object[] o = fmtAnchorId.parse(annotation.getStartId());
          Long anchorId = (Long)o[0];
          sql.setLong(5, anchorId);
        }
        catch(ParseException exception)
        {
          System.err.println("Error parsing start anchor for "+annotation.getLayerId()+":"+annotation.getId()+": " + annotation.getStartId());
          throw new StoreException("Error parsing start anchor for "+annotation.getLayerId()+":"+annotation.getId()+": " + annotation.getStartId(), exception);
        }
        try
        {
          Object[] o = fmtAnchorId.parse(annotation.getEndId());
          Long anchorId = (Long)o[0];
          sql.setLong(6, anchorId);
        }
        catch(ParseException exception)
        {
          System.err.println("Error parsing end anchor for "+annotation.getId()+": " + annotation.getEndId());
          throw new StoreException("Error parsing end anchor for "+annotation.getId()+": " + annotation.getEndId(), exception);
        }
        if (annotation.getParentId() == null)
        {
          sql.setNull(7, java.sql.Types.INTEGER);
        }
        else
        {
          if (scope.equalsIgnoreCase(SqlConstants.SCOPE_FREEFORM))
          { // freeform layers have the graph as the parent
            sql.setLong(7, ((Integer)annotation.getGraph().get("@ag_id")).longValue());
          }
          else if (annotation.getLayer().getParentId().equals("who"))
          { // turn layer
            try
            {
              // parent id has as a special layer format
              Object[] o = fmtMetaAnnotationId.parse(annotation.getParentId());
              sql.setLong(7, Long.parseLong(o[1].toString()));
            }
            catch(ParseException exception)
            {
              System.err.println("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId() + " on " + annotation.getLayerId());
              throw new StoreException("Error parsing who parent id for "+annotation.getId()+": " + annotation.getParentId() + " on " + annotation.getLayerId(), exception);
            }
          }
          else
          {
            try
            {
              Object[] o = fmtAnnotationId.parse(annotation.getParentId());
              sql.setLong(7, ((Long)o[2]).longValue());
            }
            catch(ParseException exception)
            {
              System.err.println("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId() + " on " + annotation.getLayerId());
              throw new StoreException("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId() + " on " + annotation.getLayerId(), exception);
            }
          }
        }
        sql.setInt(8, annotation.getOrdinal());
        if (annotation.getAnnotator() != null)
        {
          sql.setString(9, annotation.getAnnotator());
        }
        else
        {
          sql.setString(9, getUser());
        }
        // if (annotation.getWhen() != null)
        // {
        //    sql.setTimestamp(10, new Timestamp(annotation.getWhen().getTime()));
        // }
        // else
        {
          sql.setTimestamp(10, new Timestamp(new java.util.Date().getTime()));
        }

        if (sql != sqlInsertFreeformAnnotation)
        { // meta, word, or segment annotation
          // must set turn_annotation_id too
          if (layerId.intValue() == SqlConstants.LAYER_TURN)
          {
            // turn_annotation_id = annotation_id
            sql.setNull(11, java.sql.Types.INTEGER); // turn_annotation_id - set it later...
          }
          else if (sql == sqlInsertMetaAnnotation)
          {
            // turn is annotation.parent
            try
            {
              Object[] o = fmtAnnotationId.parse(annotation.getParentId());
              sql.setLong(11, ((Long)o[2]).longValue()); // turn_annotation_id
            }
            catch(ParseException exception)
            {
              System.err.println("Error parsing turn id for "+annotation.getId()+" (" + annotation.getStart() + "): " + annotation.getParentId());
              throw new StoreException("Error parsing turn id for "+annotation.getId()+" (" + annotation.getStart() + "): " + annotation.getParentId(), exception);
            }
          }
          else
          { // word or segment annotation
            if (layerId.intValue() == SqlConstants.LAYER_TRANSCRIPTION)
            {
              try
              {
                Object[] o = fmtAnnotationId.parse(annotation.getParentId());
                sql.setLong(11, ((Long)o[2]).longValue()); // turn_annotation_id
              }
              catch(ParseException exception)
              {
                System.err.println("Error parsing turn id for "+annotation.getId()+" (" + annotation.getStart() + "): " + annotation.getParentId());
                throw new StoreException("Error parsing turn id for "+annotation.getId()+" (" + annotation.getStart() + "): " + annotation.getParentId(), exception);
              }
              sql.setInt(12, annotation.getOrdinal()); // ordinal_in_turn
              sql.setNull(13, java.sql.Types.INTEGER); // word_annotation_id - set it later
            }
            else
            { // other word or segment annotation
              if (sql == sqlInsertWordAnnotation)
              {
                try
                {
                  Object[] o = fmtAnnotationId.parse(annotation.getParentId());
                  Long wordAnnotationId = ((Long)o[2]).longValue();
                  sqlSelectWordFields.setLong(1, wordAnnotationId); // word_annotation_id
                  ResultSet rs = sqlSelectWordFields.executeQuery();
                  rs.next();
                  sql.setLong(11, rs.getLong("turn_annotation_id"));
                  sql.setInt(12, rs.getInt("ordinal_in_turn"));
                  rs.close();
                  sql.setLong(13, wordAnnotationId); // word_annotation_id
                }
                catch(ParseException exception)
                {
                  System.err.println("Error parsing word ID for "+annotation.getId()+" (" + annotation.getStart() + "): " + annotation.getParentId());
                  throw new StoreException("Error parsing word ID for "+annotation.getId()+" (" + annotation.getStart() + "): " + annotation.getParentId(), exception);
                }
              }
              else 
              { // segment annotation
                if (layerId.intValue() == SqlConstants.LAYER_SEGMENT)
                {
                  try
                  {
                    Object[] o = fmtAnnotationId.parse(annotation.getParentId());
                    Long wordAnnotationId = ((Long)o[2]).longValue();
                    sqlSelectWordFields.setLong(1, wordAnnotationId); // word_annotation_id
                    ResultSet rs = sqlSelectWordFields.executeQuery();
                    rs.next();
                    sql.setLong(11, rs.getLong("turn_annotation_id"));
                    sql.setInt(12, rs.getInt("ordinal_in_turn"));
                    rs.close();
                    sql.setLong(13, wordAnnotationId); // word_annotation_id
                    sql.setInt(14, annotation.getOrdinal()); // ordinal_in_word
                    sql.setNull(15, java.sql.Types.INTEGER); // segment_annotation_id - set it later
                  }
                  catch(ParseException exception)
                  {
                    System.err.println("Error parsing word ID for "+annotation.getId()+" (" + annotation.getStart() + "): " + annotation.getParentId());
                    throw new StoreException("Error parsing word ID for "+annotation.getId()+" (" + annotation.getStart() + "): " + annotation.getParentId(), exception);
                  }
                }
                else
                { // other segment annotation
                  try
                  {
                    Object[] o = fmtAnnotationId.parse(annotation.getParent().getParentId());
                    Long wordAnnotationId = ((Long)o[2]).longValue();
                    sqlSelectWordFields.setLong(1, wordAnnotationId); // word_annotation_id
                    ResultSet rs = sqlSelectWordFields.executeQuery();
                    rs.next();
                    sql.setLong(11, rs.getLong("turn_annotation_id"));
                    sql.setInt(12, rs.getInt("ordinal_in_turn"));
                    rs.close();
                    sql.setLong(13, wordAnnotationId); // word_annotation_id
			      
                  }
                  catch(ParseException exception)
                  {
                    System.err.println("Error parsing word ID for segment "+annotation.getId()+" (" + annotation.getStart() + "): " + annotation.getParent().getParentId());
                    throw new StoreException("Error parsing word ID for segment "+annotation.getId()+" (" + annotation.getStart() + "): " + annotation.getParent().getParentId(), exception);
                  }
                  try
                  {
                    Object[] o = fmtAnnotationId.parse(annotation.getParentId());
                    Long segmentAnnotationId = ((Long)o[2]).longValue();
                    sqlSelectSegmentFields.setLong(1, segmentAnnotationId); // segment_annotation_id
                    ResultSet rs = sqlSelectSegmentFields.executeQuery();
                    rs.next();
                    sql.setInt(14, rs.getInt("ordinal_in_word")); // ordinal_in_word
                    sql.setLong(15, segmentAnnotationId); // segment_annotation_id
                    rs.close();
                  }
                  catch(ParseException exception)
                  {
                    System.err.println("Error parsing segment ID for "+annotation.getId()+" (" + annotation.getStart() + "): " + annotation.getParentId());
                    throw new StoreException("Error parsing segment ID for "+annotation.getId()+" (" + annotation.getStart() + "): " + annotation.getParentId(), exception);
                  }
                } // other segment annotation
              } // segment annotation
            } // other word or segment annotation
          } // word or segment annotation
        } // meta, word, or segment annotation
        sql.executeUpdate();

        ResultSet rs = sqlLastId.executeQuery();
        rs.next();
        String oldId = annotation.getId();
        long annotationId = rs.getLong(1);
        Object[] annotationIdParts = { scope, layerId, Long.valueOf(annotationId) };
        String newId = fmtAnnotationId.format(annotationIdParts);
        rs.close();
	    
        // change annotation ID
        annotation.setId(newId);

        // does it need to update its own ID in the database?
        switch (layerId.intValue())
        {
          case SqlConstants.LAYER_TURN:
          {		  
            sqlUpdateTurnAnnotationId.setInt(1, layerId);
            sqlUpdateTurnAnnotationId.setLong(2, annotationId);
            sqlUpdateTurnAnnotationId.setLong(3, annotationId);
            sqlUpdateTurnAnnotationId.executeUpdate();
            break;
          }
          case SqlConstants.LAYER_TRANSCRIPTION:
          {
            sqlUpdateWordAnnotationId.setInt(1, layerId);
            sqlUpdateWordAnnotationId.setLong(2, annotationId);
            sqlUpdateWordAnnotationId.setLong(3, annotationId);
            sqlUpdateWordAnnotationId.executeUpdate();
            break;
          }
          case SqlConstants.LAYER_SEGMENT:
          {
            sqlUpdateSegmentAnnotationId.setInt(1, layerId);
            sqlUpdateSegmentAnnotationId.setLong(2, annotationId);
            sqlUpdateSegmentAnnotationId.setLong(3, annotationId);
            sqlUpdateSegmentAnnotationId.executeUpdate();
            break;
          }
        }

        break;
      } // Create
      case Update:
      {
        // deduce the database anchor.anchor_id from the object anchor.id
        String scope = null;
        Long layerId = null;
        Long annotationId = null;
        try
        {
          Object[] o = fmtAnnotationId.parse(annotation.getId());
          scope = o[0].toString();
          layerId = (Long)o[1];
          annotationId = (Long)o[2];
        }
        catch(ParseException exception)
        {
          System.err.println("Error parsing ID for "+annotation.getId());
          throw new StoreException("Error parsing ID for "+annotation.getId(), exception);
        }
        PreparedStatement sql = sqlUpdateFreeformAnnotation;
        if (scope.equalsIgnoreCase(SqlConstants.SCOPE_META))
        {
          sql = sqlUpdateMetaAnnotation;
        }
        else if (scope.equalsIgnoreCase(SqlConstants.SCOPE_WORD))
        {
          sql = sqlUpdateWordAnnotation;
        }
        else if (scope.equalsIgnoreCase(SqlConstants.SCOPE_SEGMENT))
        {
          sql = sqlUpdateSegmentAnnotation;
        }
        sql.setInt(1, layerId.intValue());
        if ((layerId.intValue() == SqlConstants.LAYER_TURN
             || layerId.intValue() == SqlConstants.LAYER_UTTERANCE)
            && participantNameToNumber.containsKey(annotation.getLabel()))
        { // label should be the speaker number, not the name
          sql.setString(2, participantNameToNumber.get(annotation.getLabel()));
        }
        else
        {
          sql.setString(2, annotation.getLabel());
        }
        sql.setInt(3, annotation.getConfidence());
        try
        {
          Object[] o = fmtAnchorId.parse(annotation.getStartId());
          Long anchorId = (Long)o[0];
          sql.setLong(4, anchorId);
        }
        catch(ParseException exception)
        {
          System.err.println("Error parsing start anchor for "+annotation.getId()+": " + annotation.getStartId());
          throw new StoreException("Error parsing start anchor for "+annotation.getId()+": " + annotation.getStartId(), exception);
        }
        try
        {
          Object[] o = fmtAnchorId.parse(annotation.getEndId());
          Long anchorId = (Long)o[0];
          sql.setLong(5, anchorId);
        }
        catch(ParseException exception)
        {
          System.err.println("Error parsing end anchor for "+annotation.getId()+": " + annotation.getEndId());
          throw new StoreException("Error parsing end anchor for "+annotation.getId()+": " + annotation.getEndId(), exception);
        }
        if (sql == sqlUpdateFreeformAnnotation)
        {
          // for freeform layers, the parent is the graph
          sql.setLong(6, ((Integer)annotation.getGraph().get("@ag_id")).longValue());

          sql.setInt(7, annotation.getOrdinal());
          if (annotation.getAnnotator() != null)
          {
	       	  sql.setString(8, annotation.getAnnotator());
          }
          else
          {
            sql.setString(8, getUser());
          }
          // if (annotation.getWhen() != null)
          // {
          // 	  sql.setTimestamp(9, new Timestamp(annotation.getWhen().getTime()));
          // }
          // else
          {
            sql.setTimestamp(9, new Timestamp(new java.util.Date().getTime()));
          }
          sql.setLong(10, annotationId);
        }
        else
        { // meta, word, or segment annotation
          // must set turn_annotation_id too
          if (layerId.intValue() == SqlConstants.LAYER_TURN)
          {
            // turn_annotation_id = annotation_id
            sql.setLong(6, annotationId); // turn_annotation_id
            // parent_id for turns is paticipant number
            if (annotation.getParentId() == null)
            {
              sql.setLong(7, Long.parseLong(annotation.getLabel()));
            }
            else
            {
              try
              {
                Object[] o = fmtMetaAnnotationId.parse(annotation.getParentId());
                sql.setLong(7, Long.parseLong(o[1].toString()));
              }
              catch(ParseException exception)
              {
                System.err.println("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId());
                throw new StoreException("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId(), exception);
              }
            }
            sql.setInt(8, annotation.getOrdinal());
            if (annotation.getAnnotator() != null)
            {
              sql.setString(9, annotation.getAnnotator());
            }
            else
            {
              sql.setString(9, getUser());
            }
            // if (annotation.getWhen() != null)
            // {
            //    sql.setTimestamp(10, new Timestamp(annotation.getWhen().getTime()));
            // }
            // else
            {
              sql.setTimestamp(10, new Timestamp(new java.util.Date().getTime()));
            }
            // annotation_id
            sql.setLong(11, annotationId);
          }
          else if (sql == sqlUpdateMetaAnnotation)
          {
            // turn is annotation.parent
            try
            {
              Object[] o = fmtAnnotationId.parse(annotation.getParentId());
              sql.setLong(6, ((Long)o[2]).longValue()); // turn_annotation_id
            }
            catch(ParseException exception)
            {
              System.err.println("Error parsing turn ID for "+annotation.getId()+": " + annotation.getParentId());
              throw new StoreException("Error parsing turn ID for "+annotation.getId()+": " + annotation.getParentId(), exception);
            }
            if (annotation.getParentId() == null)
            {
              sql.setNull(7, java.sql.Types.INTEGER);
            }
            else
            {
              try
              {
                Object[] o = fmtAnnotationId.parse(annotation.getParentId());
                sql.setLong(7, ((Long)o[2]).longValue());
              }
              catch(ParseException exception)
              {
                System.err.println("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId());
                throw new StoreException("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId(), exception);
              }
            }
            sql.setInt(8, annotation.getOrdinal());
            if (annotation.getAnnotator() != null)
            {
              sql.setString(9, annotation.getAnnotator());
            }
            else
            {
              sql.setString(9, getUser());
            }
            // if (annotation.getWhen() != null)
            // {
            //    sql.setTimestamp(10, new Timestamp(annotation.getWhen().getTime()));
            // }
            // else
            {
              sql.setTimestamp(10, new Timestamp(new java.util.Date().getTime()));
            }
            // annotation_id
            sql.setLong(11, annotationId);
          }
          else
          { // word or segment annotation
            if (layerId.intValue() == SqlConstants.LAYER_TRANSCRIPTION)
            {
              try
              {
                Object[] o = fmtAnnotationId.parse(annotation.getParentId());
                sql.setLong(6, ((Long)o[2]).longValue()); // turn_annotation_id
              }
              catch(ParseException exception)
              {
                System.err.println("Error parsing turn ID for "+annotation.getId()+": " + annotation.getParentId());
                throw new StoreException("Error parsing turn ID for "+annotation.getId()+": " + annotation.getParentId(), exception);
              }
              sql.setInt(7, annotation.getOrdinal()); // ordinal_in_turn
              sql.setLong(8, annotationId); // word_annotation_id		     
              if (annotation.getParentId() == null)
              {
                sql.setNull(9, java.sql.Types.INTEGER);
              }
              else
              {
                try
                {
                  Object[] o = fmtAnnotationId.parse(annotation.getParentId());
                  sql.setLong(9, ((Long)o[2]).longValue());
                }
                catch(ParseException exception)
                {
                  System.err.println("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId());
                  throw new StoreException("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId(), exception);
                }
              }
              sql.setInt(10, annotation.getOrdinal());
              if (annotation.getAnnotator() != null)
              {
                sql.setString(11, annotation.getAnnotator());
              }
              else
              {
                sql.setString(11, getUser());
              }
              // if (annotation.getWhen() != null)
              // {
              // 	sql.setTimestamp(12, new Timestamp(annotation.getWhen().getTime()));
              // }
              // else
              {
                sql.setTimestamp(12, new Timestamp(new java.util.Date().getTime()));
              }
              sql.setLong(13, annotationId); // annotation_id		     
            }
            else
            { // other word or segment annotation
              if (sql == sqlUpdateWordAnnotation)
              {
                try
                {
                  Object[] o = fmtAnnotationId.parse(annotation.getParentId());
                  Long wordAnnotationId = ((Long)o[2]).longValue();
                  sqlSelectWordFields.setLong(1, wordAnnotationId); // word_annotation_id
                  ResultSet rs = sqlSelectWordFields.executeQuery();
                  rs.next();
                  sql.setLong(6, rs.getLong("turn_annotation_id"));
                  sql.setInt(7, rs.getInt("ordinal_in_turn"));
                  sql.setLong(8, wordAnnotationId); // word_annotation_id
                  if (annotation.getParentId() == null)
                  {
                    sql.setNull(9, java.sql.Types.INTEGER);
                  }
                  else
                  {
                    try
                    {
                      o = fmtAnnotationId.parse(annotation.getParentId());
                      sql.setLong(9, ((Long)o[2]).longValue());
                    }
                    catch(ParseException exception)
                    {
                      System.err.println("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId());
                      throw new StoreException("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId(), exception);
                    }
                  }
                  sql.setInt(10, annotation.getOrdinal());
                  if (annotation.getAnnotator() != null)
                  {
                    sql.setString(11, annotation.getAnnotator());
                  }
                  else
                  {
                    sql.setString(11, getUser());
                  }
                  // if (annotation.getWhen() != null)
                  // {
                  //    sql.setTimestamp(12, new Timestamp(annotation.getWhen().getTime()));
                  // }
                  // else
                  {
                    sql.setTimestamp(12, new Timestamp(new java.util.Date().getTime()));
                  }
                  sql.setLong(13, annotationId); // annotation_id
                  rs.close();
                }
                catch(ParseException exception)
                {
                  System.err.println("Error parsing word ID for "+annotation.getId()+": " + annotation.getParentId());
                  throw new StoreException("Error parsing word ID for "+annotation.getId()+": " + annotation.getParentId(), exception);
                }
              }
              else 
              { // segment annotation
                if (layerId.intValue() == SqlConstants.LAYER_SEGMENT)
                {
                  try
                  {
                    Object[] o = fmtAnnotationId.parse(annotation.getParentId());
                    Long wordAnnotationId = ((Long)o[2]).longValue();
                    sqlSelectWordFields.setLong(1, wordAnnotationId); // word_annotation_id
                    ResultSet rs = sqlSelectWordFields.executeQuery();
                    rs.next();
                    sql.setLong(6, rs.getLong("turn_annotation_id"));
                    sql.setInt(7, rs.getInt("ordinal_in_turn"));
                    rs.close();
                    sql.setLong(8, wordAnnotationId); // word_annotation_id
                    sql.setInt(9, annotation.getOrdinal()); // ordinal_in_word
                    sql.setLong(10, annotationId); // segment_annotation_id
                    if (annotation.getParentId() == null)
                    {
                      sql.setNull(11, java.sql.Types.INTEGER);
                    }
                    else
                    {
                      try
                      {
                        o = fmtAnnotationId.parse(annotation.getParentId());
                        sql.setLong(11, ((Long)o[2]).longValue());
                      }
                      catch(ParseException exception)
                      {
                        System.err.println("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId());
                        throw new StoreException("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId(), exception);
                      }
                    }
                    sql.setInt(12, annotation.getOrdinal());
                    if (annotation.getAnnotator() != null)
                    {
                      sql.setString(13, annotation.getAnnotator());
                    }
                    else
                    {
                      sql.setString(13, getUser());
                    }
                    // if (annotation.getWhen() != null)
                    // {
                    // 	 sql.setTimestamp(14, new Timestamp(annotation.getWhen().getTime()));
                    // }
                    // else
                    {
                      sql.setTimestamp(14, new Timestamp(new java.util.Date().getTime()));
                    }
                    sql.setLong(15, annotationId); // annotation_id
                  }
                  catch(ParseException exception)
                  {
                    System.err.println("Error parsing word ID for "+annotation.getId()+": " + annotation.getParentId());
                    throw new StoreException("Error parsing word ID for "+annotation.getId()+": " + annotation.getParentId(), exception);
                  }
                }
                else
                { // other segment annotation
                  try
                  {
                    Object[] o = fmtAnnotationId.parse(annotation.getParent().getParentId());
                    Long wordAnnotationId = ((Long)o[2]).longValue();
                    sqlSelectWordFields.setLong(1, wordAnnotationId); // word_annotation_id
                    ResultSet rs = sqlSelectWordFields.executeQuery();
                    rs.next();
                    sql.setLong(6, rs.getLong("turn_annotation_id"));
                    sql.setInt(7, rs.getInt("ordinal_in_turn"));
                    rs.close();
                    sql.setLong(8, wordAnnotationId); // word_annotation_id
                  }
                  catch(ParseException exception)
                  {
                    System.err.println("Error parsing word ID for parent "+annotation.getId()+": " + annotation.getParent().getParentId());
                    throw new StoreException("Error parsing word ID for parent "+annotation.getId()+": " + annotation.getParent().getParentId(), exception);
                  }
                  try
                  {
                    Object[] o = fmtAnnotationId.parse(annotation.getParentId());
                    Long segmentAnnotationId = ((Long)o[2]).longValue();
                    sqlSelectSegmentFields.setLong(1, segmentAnnotationId); // segment_annotation_id
                    ResultSet rs = sqlSelectSegmentFields.executeQuery();
                    rs.next();
                    sql.setInt(9, rs.getInt("ordinal_in_word")); // ordinal_in_word
                    sql.setLong(10, segmentAnnotationId); // segment_annotation_id
                    rs.close();
                  }
                  catch(ParseException exception)
                  {
                    System.err.println("Error parsing segment ID for "+annotation.getId()+": " + annotation.getParentId());
                    throw new StoreException("Error parsing segment ID for "+annotation.getId()+": " + annotation.getParentId(), exception);
                  }
                  if (annotation.getParentId() == null)
                  {
                    sql.setNull(11, java.sql.Types.INTEGER);
                  }
                  else
                  {
                    try
                    {
                      Object[] o = fmtAnnotationId.parse(annotation.getParentId());
                      sql.setLong(11, ((Long)o[2]).longValue());
                    }
                    catch(ParseException exception)
                    {
                      System.err.println("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId());
                      throw new StoreException("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId(), exception);
                    }
                  }
                  sql.setInt(12, annotation.getOrdinal());
                  if (annotation.getAnnotator() != null)
                  {
                    sql.setString(13, annotation.getAnnotator());
                  }
                  else
                  {
                    sql.setString(13, getUser());
                  }
                  // if (annotation.getWhen() != null)
                  // {
                  //    sql.setTimestamp(14, new Timestamp(annotation.getWhen().getTime()));
                  // }
                  // else
                  {
                    sql.setTimestamp(14, new Timestamp(new java.util.Date().getTime()));
                  }
                  sql.setLong(15, annotationId); // annotation_id
                } // other segment annotation
              } // segment annotation
            } // other word or segment annotation
          } // word or segment annotation
        } // meta, word, or segment annotation
        sql.executeUpdate();
	    
        break;
      }
      case Destroy:
      {
        try
        {
          Object[] o = fmtAnnotationId.parse(annotation.getId());
          Long layerId = (Long)o[1];
          Long annotationId = (Long)o[2];
          sqlDeleteAnnotation.setInt(1, layerId.intValue());
          sqlDeleteAnnotation.setLong(2, annotationId);
          sqlDeleteAnnotation.executeUpdate();
        }
        catch(ParseException exception)
        {
          System.err.println("Error parsing ID for "+annotation.getId());
          throw new StoreException("Error parsing ID for "+annotation.getId(), exception);
        }
        break;
      } // Destroy
    } // switch on change type

    annotation.put("@SqlUpdated", Boolean.TRUE); // flag the annotation as having been updated
  } // end of saveAnchorChanges()

   
  /**
   * Save changes to a 'special' annotation, e.g. corpus, series, etc.
   * @param annotation The annotation whose changes should be saved.
   * @param participantNameToNumber A map of participant names to speaker numbers.
   * @throws PermissionException If there's a permission problem.
   * @throws SQLException If a database error occurs.
   * @throws StoreException If some other error occurs.
   */
  public void saveSpecialAnnotationChanges(Annotation annotation, HashMap<String,String> participantNameToNumber)
    throws PermissionException, StoreException, SQLException
  {
    try
    {
      if (annotation.getLayerId().equals("episode"))
      {
        switch (annotation.getChange())
        {
          case Create:
          case Update:
          {
            int familyId = -1;
            int familySequence = 1;
            double familyOffset = 0.0;
            PreparedStatement sql = getConnection().prepareStatement(
              "SELECT transcript_family.family_id,"
              + " COALESCE(MAX(family_sequence) + 1, 1) AS family_sequence,"
              + " COALESCE(MAX(anchor.offset), 0.0) AS family_offset"
              + " FROM transcript_family"
              + " LEFT OUTER JOIN transcript ON transcript.family_id = transcript_family.family_id"
              + " LEFT OUTER JOIN anchor ON anchor.ag_id = transcript.ag_id"
              + " WHERE name = ?"
              + " GROUP BY transcript_family.family_id");
            sql.setString(1, annotation.getLabel());
            ResultSet rs = sql.executeQuery();
            if (!rs.next())
            { // doesn't exist, so create it		     
              rs.close();
              sql.close();
              sql = getConnection().prepareStatement(
                "SELECT MAX(family_id) + 1 AS family_id FROM transcript_family");
              rs = sql.executeQuery();
              rs.next();
              familyId = rs.getInt("family_id");
              rs.close();
              sql.close();
              sql = getConnection().prepareStatement(
                "INSERT INTO transcript_family (family_id, name) VALUES (?, ?)");
              sql.setString(2, annotation.getLabel());
              while (true) // until we succeed in adding the family
              {
                try
                {
                  sql.setInt(1, familyId);
                  sql.executeUpdate();
                  break;
                }
                catch(SQLException exception)
                { // somebody else got the ID first, so try the next one
                  familyId++;
                }
              }
            }
            else
            {
              familyId = rs.getInt("family_id");
              familySequence = rs.getInt("family_sequence");
              familyOffset = rs.getInt("family_offset");
            }
            try { rs.close(); } catch(Exception exception) {}
            sql.close();
            int agId = ((Integer)annotation.getGraph().get("@ag_id")).intValue();
            PreparedStatement sqlUpdate = getConnection().prepareStatement(
              "UPDATE transcript SET family_id = ?, family_sequence = ?, family_offset = ?"
              + " WHERE ag_id = ?");
            sqlUpdate.setInt(1, familyId);
            sqlUpdate.setInt(2, familySequence);
            sqlUpdate.setDouble(3, familyOffset);
            sqlUpdate.setInt(4, agId);
            sqlUpdate.executeUpdate();
            sqlUpdate.close();

            // update ID
            Object[] annotationIdParts = {
              getLayer(annotation.getLayerId()).get("@layer_id"), ""+familyId};
            annotation.setId(fmtMetaAnnotationId.format(annotationIdParts));

            // also update graph
            annotation.getGraph().put("@family_id", familyId);		  
            break;
          }
        } // switch on change type
      }
      else if (annotation.getLayerId().equals("corpus"))
      {
        switch (annotation.getChange())
        {
          case Create:
          case Update:
          {
            int agId = ((Integer)annotation.getGraph().get("@ag_id")).intValue();
            PreparedStatement sqlUpdate = getConnection().prepareStatement(
              "UPDATE transcript SET corpus_name = ? WHERE ag_id = ?");
            sqlUpdate.setString(1, annotation.getLabel());
            sqlUpdate.setInt(2, agId);
            sqlUpdate.executeUpdate();
            sqlUpdate.close();
            break;
          }
        } // switch on change type
      }
      else if (annotation.getLayerId().equals("transcript_type"))
      {
        switch (annotation.getChange())
        {
          case Create:
          case Update:
          {
            PreparedStatement sql = getConnection().prepareStatement(
              "SELECT type_id FROM transcript_type WHERE transcript_type = ?");
            sql.setString(1, annotation.getLabel());
            ResultSet rs = sql.executeQuery();
            if (rs.next())
            {
              int typeId = rs.getInt("type_id");
              int agId = ((Integer)annotation.getGraph().get("@ag_id")).intValue();
              PreparedStatement sqlUpdate = getConnection().prepareStatement(
                "UPDATE transcript SET type_id = ? WHERE ag_id = ?");
              sqlUpdate.setInt(1, typeId);
              sqlUpdate.setInt(2, agId);
              sqlUpdate.executeUpdate();
              sqlUpdate.close();
            }
            rs.close();
            sql.close();
            break;
          }
        } // switch on change type
      }
      else if (annotation.getLayerId().equals("main_participant"))
      {
        int agId = ((Integer)annotation.getGraph().get("@ag_id")).intValue();
        Object[] o = fmtMetaAnnotationId.parse(annotation.getParentId());
        int speakerNumber = Integer.parseInt(o[1].toString().replace(",",""));
        PreparedStatement sqlUpdate = getConnection().prepareStatement(
          "UPDATE transcript_speaker SET main_speaker = ?"
          +" WHERE ag_id = ? AND speaker_number = ?");
        sqlUpdate.setInt(2, agId);
        sqlUpdate.setInt(3, speakerNumber);
        switch (annotation.getChange())
        {
          case Create:
          case Update:
            sqlUpdate.setInt(1, 1); // is a main speaker
            break;
          case Destroy:
            sqlUpdate.setInt(1, 0); // isn't a main speaker
            break;
        } // switch on change type
        sqlUpdate.executeUpdate();
        sqlUpdate.close();
      }
      else if (annotation.getLayerId().equals("who"))
      {
        int agId = ((Integer)annotation.getGraph().get("@ag_id")).intValue();
        switch (annotation.getChange())
        {
          case Create:
          {
            // ensure speaker exists
            int speakerNumber = -1;
            PreparedStatement sql = getConnection().prepareStatement(
              "SELECT speaker_number FROM speaker WHERE name = ?");
            sql.setString(1, annotation.getLabel());
            ResultSet rs = sql.executeQuery();
            if (rs.next())
            {
              speakerNumber = rs.getInt("speaker_number");
            }
            else
            {
              // create the speaker
              PreparedStatement sqlInsert = getConnection().prepareStatement(
                "INSERT INTO speaker (name) VALUES (?)");
              sqlInsert.setString(1, annotation.getLabel());
              try
              {
                sqlInsert.executeUpdate();
                sqlInsert.close();
                sqlInsert = getConnection().prepareStatement("SELECT LAST_INSERT_ID()");
                ResultSet rsInsert = sqlInsert.executeQuery();
                rsInsert.next();
                speakerNumber = rsInsert.getInt(1);
                rsInsert.close();
              }
              catch(SQLException exception)
              { // maybe some simultaneous upload already inserted this speaker...
                rs.close();
                rs = sql.executeQuery();
                if (rs.next())
                { // yes, the speaker now exists, so get their identifier
                  speakerNumber = rs.getInt("speaker_number");
                }
                else
                { // no speaker still doesn't exist, so something else went wrong...
                  rs.close();
                  sql.close();
                  throw exception;
                }
              }
              sqlInsert.close();
            }
            rs.close();
            sql.close();
            annotation.put("@speaker_number", Integer.valueOf(speakerNumber));
            participantNameToNumber.put(annotation.getLabel(), ""+speakerNumber);
		  
            // reset the annotation ID
            Object[] args = { getLayer(annotation.getLayerId()).get("@layer_id"), ""+speakerNumber };
            annotation.setId(fmtMetaAnnotationId.format(args));

            // add the speaker to transcript_speaker
            sql = getConnection().prepareStatement(
              "REPLACE INTO transcript_speaker (speaker_number, ag_id, name) VALUES (?,?,?)");
            sql.setInt(1, speakerNumber);
            sql.setInt(2, agId);
            sql.setString(3, annotation.getLabel());
            sql.executeUpdate();
            sql.close();
            break;
          } // Create
          case Update:
          {
            Object[] o = fmtMetaAnnotationId.parse(annotation.getId());
            int speakerNumber = Integer.parseInt(o[1].toString().replace(",",""));
            // update the label (the only possible change)
            PreparedStatement sql = getConnection().prepareStatement(
              "UPDATE speaker SET name = ? WHERE speaker_number = ?");
            sql.setString(1, annotation.getLabel());
            sql.setInt(2, speakerNumber);
            sql.executeUpdate();
            sql.close();
            break;
          }
          case Destroy:
          {
            Object[] o = fmtMetaAnnotationId.parse(annotation.getId());
            int speakerNumber = Integer.parseInt(o[1].toString().replace(",",""));
            // delete from transcript_speaker only
            PreparedStatement sql = getConnection().prepareStatement(
              "DELETE FROM transcript_speaker WHERE speaker_number = ? AND ag_id = ?");
            sql.setInt(1, speakerNumber);
            sql.setInt(2, agId);
            sql.executeUpdate();
            sql.close();
            break;
          } // Destroy
        } // switch on change type
      }
      annotation.put("@SqlUpdated", Boolean.TRUE); // flag the anchor as having been updated
    }
    catch(ParseException exception)
    {
      System.err.println("Error parsing ID for special attribute: "+annotation.getId() + " on layer " + annotation.getLayerId());
      throw new StoreException("Error parsing ID for special attribute: "+annotation.getId() + " on layer " + annotation.getLayerId(), exception);
    }
  } // end of saveSpecialAnnotationChanges()

  /**
   * Save changes to an episode tag annotation.
   * @param annotation The annotation whose changes should be saved.
   * @param sqlLastId Prepared statement for retrieving the last database ID created.
   * @throws PermissionException If there's a permission problem.
   * @throws SQLException If a database error occurs.
   * @throws StoreException If some other error occurs.
   */
  public void saveEpisodeAnnotationChanges(Annotation annotation, PreparedStatement sqlLastId)
    throws PermissionException, StoreException, SQLException
  {
    try
    {
      if (annotation.getConfidence() == null)
      {
        annotation.setConfidence(Integer.valueOf(Constants.CONFIDENCE_UNKNOWN));
      }
      switch (annotation.getChange())
      {
        case Create:
        {
          Layer layer = getLayer(annotation.getLayerId());
          Integer layerId = (Integer)layer.get("@layer_id");
          PreparedStatement sql = getConnection().prepareStatement(
            "INSERT INTO `annotation_layer_"+layerId+"`"
            +" (family_id, label, label_status, parent_id, ordinal, annotated_by, annotated_when, data)"
            + " VALUES (?,?,?,?,?,?,?,?)");
          try
          {
            int familyId = (Integer)annotation.getGraph().get("@family_id");
            // delete any other annotations on this layer
            PreparedStatement sqlDelete = getConnection().prepareStatement(
              "DELETE FROM `annotation_layer_"+layerId+"` WHERE family_id = ?"
              // if peers are allowed, only delete peers that have the same label
              // (so as not to double-up)
              +(layer.getPeers()?" AND label = ?":""));
            try
            {
              sqlDelete.setInt(1, familyId);
              if (layer.getPeers()) sqlDelete.setString(2, annotation.getLabel());
              sqlDelete.executeUpdate();
            }
            finally
            {
              sqlDelete.close();
            }

            sql.setInt(1, familyId);
            sql.setString(2, annotation.getLabel());
            sql.setInt(3, annotation.getConfidence());
            if (annotation.getLayer().getParentId().equals("episode"))
            { // parent is episode, so parent_id is family_id
              sql.setInt(4, familyId);
            }
            else
            {
              Object[] o = fmtAnnotationId.parse(annotation.getParentId());
              sql.setLong(4, (Long)o[2]);
            }
            sql.setInt(5, annotation.getOrdinal());
            if (annotation.getAnnotator() != null)
            {
              sql.setString(6, annotation.getAnnotator());
            }
            else
            {
              sql.setString(6, getUser());
            }
            // if (annotation.getWhen() != null)
            // {
            //    sql.setTimestamp(7, new Timestamp(annotation.getWhen().getTime()));
            // }
            // else
            {
              sql.setTimestamp(7, new Timestamp(new java.util.Date().getTime()));
            }
            if (annotation.containsKey("data"))
            {
              sql.setBytes(8, (byte[])annotation.get("data"));
            }
            else
            {
              sql.setNull(8, java.sql.Types.BLOB);
            }
            sql.executeUpdate();

            ResultSet rs = sqlLastId.executeQuery();
            rs.next();
            String oldId = annotation.getId();
            long annotationId = rs.getLong(1);
            Object[] annotationIdParts = { "e", layerId, Long.valueOf(annotationId) };
            String newId = fmtAnnotationId.format(annotationIdParts);
            rs.close();
		  
            // change annotation ID
            annotation.setId(newId);
          }
          finally
          {
            sql.close();
          }
          break;
        }
        case Update:
        {
          Object[] o = fmtAnnotationId.parse(annotation.getId());
          Long layerId = (Long)o[1];
          Long annotationId = (Long)o[2];
          PreparedStatement sql = getConnection().prepareStatement(
            "UPDATE `annotation_layer_"+layerId+"`"
            + " SET label = ?, label_status = ?, "
            + " ordinal = ?, annotated_by = ?, annotated_when = ?,"
            + " data = ?"
            + " WHERE annotation_id = ?");
          try
          {
            sql.setString(1, annotation.getLabel());
            sql.setInt(2, annotation.getConfidence());
            sql.setInt(3, annotation.getOrdinal());
            if (annotation.getAnnotator() != null)
            {
              sql.setString(4, annotation.getAnnotator());
            }
            else
            {
              sql.setString(4, getUser());
            }
            // if (annotation.getWhen() != null)
            // {
            //    sql.setTimestamp(5, new Timestamp(annotation.getWhen().getTime()));
            // }
            // else
            {
              sql.setTimestamp(5, new Timestamp(new java.util.Date().getTime()));
            }
            if (annotation.containsKey("data"))
            {
              sql.setBytes(6, (byte[])annotation.get("data"));
            }
            else
            {
              sql.setNull(6, java.sql.Types.BLOB);
            }
            sql.setLong(7, annotationId);
            sql.executeUpdate();
          }
          finally
          {
            sql.close();
          }
          break;
        }
        case Destroy:
        {
          try
          {
            Object[] o = fmtAnnotationId.parse(annotation.getId());
            Long layerId = (Long)o[1];
            Long annotationId = (Long)o[2];
            PreparedStatement sql = getConnection().prepareStatement(
              "DELETE FROM `annotation_layer_"+layerId+"` WHERE annotation_id = ?");
            try
            {
              sql.setLong(1, annotationId);
              sql.executeUpdate();
            }
            finally
            {
              sql.close();
            }
          }
          catch(ParseException exception)
          {
            System.err.println("Error parsing ID for episode tag "+annotation.getId());
            throw new StoreException("Error parsing ID for episode tag "+annotation.getId(), exception);
          }
          break;
        } // switch on change type
      }
      annotation.put("@SqlUpdated", Boolean.TRUE); // flag the annotation as having been updated
    }
    catch(ParseException exception)
    {
      System.err.println("Error parsing ID for episode attribute: "+annotation.getId() + " on layer " + annotation.getLayerId() + " : " + exception);
      throw new StoreException("Error parsing ID for episode attribute: "+annotation.getId() + " on layer " + annotation.getLayerId(), exception);
    }
  } // end of saveEpisodeAnnotationChanges()
   
  /**
   * Saves changes to a transcript attribute annotation.
   * @param annotation The annotation whose changes should be saved.
   * @param sqlInsertTranscriptAttribute Prepared statement for inserting an attribute value.
   * @param sqlUpdateTranscriptAttribute Prepared statement for updating an attribute value.
   * @param sqlDeleteTranscriptAttribute Prepared statement for deleting an attribute value.
   * @throws StoreException If an ID cannot be parsed.
   * @throws SQLException If a database error occurs.
   */
  public void saveTranscriptAttributeChanges(Annotation annotation, PreparedStatement sqlInsertTranscriptAttribute, PreparedStatement sqlUpdateTranscriptAttribute, PreparedStatement sqlDeleteTranscriptAttribute) throws SQLException, StoreException
  {
    try
    {
      switch (annotation.getChange())
      {
        case Create:
        {
          String attribute = annotation.getLayerId().substring("transcript_".length());
          sqlInsertTranscriptAttribute.setString(2, attribute);
          sqlInsertTranscriptAttribute.setString(3, annotation.getLabel());
          if (annotation.getAnnotator() != null)
          {
	       	  sqlInsertTranscriptAttribute.setString(4, annotation.getAnnotator());
          }
          else
          {
            sqlInsertTranscriptAttribute.setString(4, getUser());
          }
          // if (annotation.getWhen() != null)
          // {
          // 	  sqlInsertTranscriptAttribute.setTimestamp(5, new Timestamp(annotation.getWhen().getTime()));
          // }
          // else
          {
            sqlInsertTranscriptAttribute.setTimestamp(5, new Timestamp(new java.util.Date().getTime()));
          }
          sqlInsertTranscriptAttribute.executeUpdate();
          break;
        } // Create
        case Update:
        {
          Object[] o = fmtTranscriptAttributeId.parse(annotation.getId());
          String attribute = o[0].toString();
          sqlUpdateTranscriptAttribute.setString(1, annotation.getLabel());
          if (annotation.getAnnotator() != null)
          {
	       	  sqlUpdateTranscriptAttribute.setString(2, annotation.getAnnotator());
          }
          else
          {
            sqlUpdateTranscriptAttribute.setString(2, getUser());
          }
          // if (annotation.getWhen() != null)
          // {
          // 	  sqlUpdateTranscriptAttribute.setTimestamp(3, new Timestamp(annotation.getWhen().getTime()));
          // }
          // else
          {
            sqlUpdateTranscriptAttribute.setTimestamp(3, new Timestamp(new java.util.Date().getTime()));
          }
          sqlUpdateTranscriptAttribute.setString(5, attribute);
          sqlUpdateTranscriptAttribute.setInt(6, ((Long)o[1]).intValue());
          sqlUpdateTranscriptAttribute.executeUpdate();
          break;
        }
        case Destroy:
        {
          Object[] o = fmtTranscriptAttributeId.parse(annotation.getId());
          String attribute = o[0].toString();
          sqlDeleteTranscriptAttribute.setString(2, attribute);
          sqlDeleteTranscriptAttribute.setInt(3, ((Long)o[1]).intValue());
          sqlDeleteTranscriptAttribute.executeUpdate();
          break;
        } // Destroy
      } // switch on change type
	 
      annotation.put("@SqlUpdated", Boolean.TRUE); // flag the anchor as having been updated
    }
    catch(ParseException exception)
    {
      System.err.println("Error parsing ID for transcript attribute: "+annotation.getId());
      throw new StoreException("Error parsing ID for transcript attribute: "+annotation.getId(), exception);
    }
  } // end of saveTranscriptAttributeChanges()

  /**
   * Saves changes to a transcript attribute annotation.
   * @param annotation The annotation whose changes should be saved.
   * @param sqlInsertParticipantAttribute Prepared statement for inserting an attribute value.
   * @param sqlUpdateParticipantAttribute Prepared statement for updating an attribute value.
   * @param sqlDeleteParticipantAttribute Prepared statement for deleting an attribute value.
   * @param sqlDeleteAllParticipantAttributesOnLayer Prepared statement for deleting all attribute values on a given layer.
   * @throws StoreException If an ID can't be parsed.
   * @throws SQLException If a database error occurs.
   */
  public void saveParticipantAttributeChanges(Annotation annotation, PreparedStatement sqlInsertParticipantAttribute, PreparedStatement sqlUpdateParticipantAttribute, PreparedStatement sqlDeleteParticipantAttribute, PreparedStatement sqlDeleteAllParticipantAttributesOnLayer) throws SQLException, StoreException, PermissionException
  {
    try
    {
      switch (annotation.getChange())
      {
        case Create:
        {
          String attribute = annotation.getLayerId().substring("participant_".length());
          Object[] o = fmtMetaAnnotationId.parse(annotation.getParentId());
          int speakerNumber = Integer.parseInt(o[1].toString().replace(",",""));

          Layer layer = annotation.getLayer();
          if (layer == null) layer = getLayer(annotation.getLayerId());
          if (!"1".equals(layer.get("@peers")))
          {
            // the attribute might be new in this graph, but already exist in the database
            // so delete first and then add
            sqlDeleteAllParticipantAttributesOnLayer.setInt(1, speakerNumber);
            sqlDeleteAllParticipantAttributesOnLayer.setString(2, attribute);
            sqlDeleteAllParticipantAttributesOnLayer.executeUpdate();
          }

          sqlInsertParticipantAttribute.setInt(1, speakerNumber);
          sqlInsertParticipantAttribute.setString(2, attribute);
          sqlInsertParticipantAttribute.setString(3, annotation.getLabel());
          if (annotation.getAnnotator() != null)
          {
	       	  sqlInsertParticipantAttribute.setString(4, annotation.getAnnotator());
          }
          else
          {
            sqlInsertParticipantAttribute.setString(4, getUser());
          }
          // if (annotation.getWhen() != null)
          // {
          // 	  sqlInsertParticipantAttribute.setTimestamp(5, new Timestamp(annotation.getWhen().getTime()));
          // }
          // else
          {
            sqlInsertParticipantAttribute.setTimestamp(5, new Timestamp(new java.util.Date().getTime()));
          }
          int updated = sqlInsertParticipantAttribute.executeUpdate();
          break;
        } // Create
        case Update:
        {
          Object[] o = fmtParticipantAttributeId.parse(annotation.getId());
          String attribute = o[0].toString();
          int annotationId = ((Long)o[1]).intValue();
          sqlUpdateParticipantAttribute.setString(1, annotation.getLabel());	    
          if (annotation.getAnnotator() != null)
          {
	       	  sqlUpdateParticipantAttribute.setString(2, annotation.getAnnotator());
          }
          else
          {
            sqlUpdateParticipantAttribute.setString(2, getUser());
          }
          // if (annotation.getWhen() != null)
          // {
          // 	  sqlUpdateParticipantAttribute.setTimestamp(3, new Timestamp(annotation.getWhen().getTime()));
          // }
          // else
          {
            sqlUpdateParticipantAttribute.setTimestamp(3, new Timestamp(new java.util.Date().getTime()));
          }
          sqlUpdateParticipantAttribute.setString(4, attribute);
          sqlUpdateParticipantAttribute.setInt(5, annotationId);
          sqlUpdateParticipantAttribute.executeUpdate();
          break;
        }
        case Destroy:
        {
          Object[] o = fmtParticipantAttributeId.parse(annotation.getId());
          String attribute = o[0].toString();
          int annotationId = ((Long)o[1]).intValue();
          sqlDeleteParticipantAttribute.setString(1, attribute);
          sqlDeleteParticipantAttribute.setInt(2, annotationId);
          sqlDeleteParticipantAttribute.executeUpdate();
          break;
        } // Destroy
      } // switch on change type
	 
      annotation.put("@SqlUpdated", Boolean.TRUE); // flag the anchor as having been updated
    }
    catch(ParseException exception)
    {
      System.err.println("Error parsing ID for transcript attribute: "+annotation.getId());
      throw new StoreException("Error parsing ID for transcript attribute: "+annotation.getId(), exception);
    }
  } // end of saveParticipantAttributeChanges()

  /**
   * Creates an annotation starting at <var>fromId</var> and ending at <var>toId</var>.
   * @param fromId The start anchor's ID, which can be null if the layer is a tag layer.
   * @param toId The end anchor's ID, which can be null if the layer is a tag layer.
   * @param layerId The layer ID of the resulting annotation.
   * @param label The label of the resulting annotation.
   * @param confidence The confidence rating.
   * @param parentId The new annotation's parent's ID.
   * @return The ID of the new annotation.
   */
  public String createAnnotation(String id, String fromId, String toId, String layerId, String label, Integer confidence, String parentId)
    throws StoreException, PermissionException, GraphNotFoundException
  {
    Layer layer = getSchema().getLayer(layerId);
    if (layer.get("@scope") == null) throw new StoreException("Only temporal layers are supported: " + layerId);
    Annotation[] annotations = getMatchingAnnotations("id = '"+parentId+"'");
    if (annotations.length < 1) throw new StoreException("Invalid parent: " + parentId);
    Annotation parent = annotations[0];
    if (!layer.getParentId().equals(parent.getLayerId())) throw new StoreException("Layer "+layerId+" is not a child of " + parent.getLayerId());
    Layer parentLayer = getSchema().getLayer(parent.getLayerId());
    if (layer.getAlignment() == Constants.ALIGNMENT_NONE)
    { // tag layer - ignore given anchors, and use the parent's instead
      fromId = parent.getStartId();
      toId = parent.getEndId();
    }
    String[] anchorIds = {fromId, toId};
    Anchor[] anchors = getAnchors(id, anchorIds);
    if (anchors.length < 1) throw new StoreException("Invalid start anchor: " + fromId);
    Anchor from = anchors[0];
    if (anchors.length < 2) throw new StoreException("Invalid end anchor: " + toId);
    Anchor to = anchors[1];
    try
    {
      Object[] parentAttributes = fmtAnnotationId.parse(parent.getId());
      Long parentAnnotationId = (Long)parentAttributes[2];
      if (!layer.getPeers())
      {
        // only one child allowed, so delete any children
        PreparedStatement sql = getConnection().prepareStatement(
          "DELETE FROM annotation_layer_"+layer.get("@layer_id") +" WHERE parent_id = ?");
        sql.setLong(1, parentAnnotationId);
        sql.executeUpdate();
        sql.close();
      }
      PreparedStatement sqlInsertAnnotation = getConnection().prepareStatement(
        "INSERT INTO annotation_layer_?"
        + " (ag_id, label, label_status, start_anchor_id, end_anchor_id,"
        + " parent_id, ordinal, annotated_by, annotated_when"
        + (!layer.get("@scope").equals("F")?
           ", turn_annotation_id":"")
        + (layer.get("@scope").equals("S")||layer.get("@scope").equals("W")?
           ", ordinal_in_turn, word_annotation_id":"")
        + (layer.get("@scope").equals("S")?", ordinal_in_word, segment_annotation_id":"")
        + ")"
        + " SELECT ag_id, ?, ?, "
        // non-aligned layers use their parent anchors, regardless of startId/endId
        +(layer.getAlignment()==Constants.ALIGNMENT_NONE?"start_anchor_id, end_anchor_id,":"?, ?,")
        + " annotation_id, 1, ?, Now()"
        + (!layer.get("@scope").equals("F")?
           ", turn_annotation_id" :"")
        + (layer.get("@scope").equals("S")||layer.get("@scope").equals("W")?
           ", ordinal_in_turn, word_annotation_id":"")
        + (layer.get("@scope").equals("S")?", ordinal_in_word, segment_annotation_id":"")
        + " FROM annotation_layer_?"
        +" WHERE annotation_id = ?");
      int p = 1;
      sqlInsertAnnotation.setInt(p++, ((Integer)layer.get("@layer_id")).intValue());
      sqlInsertAnnotation.setString(p++, label);
      sqlInsertAnnotation.setInt(p++, confidence);

      if (layer.getAlignment() != Constants.ALIGNMENT_NONE)
      { // explicit anchors
        Object[] anchorAttributes = fmtAnchorId.parse(from.getId());
        sqlInsertAnnotation.setLong(p++, (Long)anchorAttributes[0]);
        
        anchorAttributes = fmtAnchorId.parse(to.getId());
        sqlInsertAnnotation.setLong(p++, (Long)anchorAttributes[0]);
      }
	 
      sqlInsertAnnotation.setString(p++, getUser());
      sqlInsertAnnotation.setInt(p++, ((Integer)parentLayer.get("@layer_id")).intValue());
      sqlInsertAnnotation.setLong(p++, parentAnnotationId);
      sqlInsertAnnotation.executeUpdate();
      sqlInsertAnnotation.close();
      sqlInsertAnnotation = getConnection().prepareStatement("SELECT LAST_INSERT_ID()");
      ResultSet rsInsert = sqlInsertAnnotation.executeQuery();
      rsInsert.next();
      long annotation_id = rsInsert.getLong(1);
      rsInsert.close();
      sqlInsertAnnotation.close();
      Object[] annotationAttributes = {
        layer.get("@scope").equals("F")?"":((String)layer.get("@scope")).toLowerCase(),
        layer.get("@layer_id"),
        Long.valueOf(annotation_id)};
      return fmtAnnotationId.format(annotationAttributes);
    }
    catch(ParseException exception)
    {
      System.err.println("Error parsing parent ID: "+parent.getId());
      throw new StoreException("Error parsing parent ID: "+parent.getId());
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
  }

  /**
   * Destroys the annotation with the given ID.
   * @param id The ID of the graph.
   * @param annotationId The annotation's ID.
   */
  public void destroyAnnotation(String id, String annotationId)
    throws StoreException, PermissionException, GraphNotFoundException
  {
    try
    {
      Object[] attributes = fmtAnnotationId.parse(annotationId);
      Long layer_id = (Long)attributes[1];
	 
      PreparedStatement sql = getConnection().prepareStatement(
        "SELECT COUNT(*) FROM layer WHERE parent_id = ?");
      sql.setLong(1, layer_id);
      ResultSet rs = sql.executeQuery();
      rs.next();
      try
      {
        if (rs.getInt(1) > 0) throw new StoreException("There are child layers: " + layer_id);
      }
      finally
      {
        rs.close();
        sql.close();
      }
	 
      Long annotation_id = (Long)attributes[2];
      // only one child allowed, so delete any children
      sql = getConnection().prepareStatement(
        "DELETE FROM annotation_layer_"+layer_id +" WHERE annotation_id = ?");
      sql.setLong(1, annotation_id);
      sql.executeUpdate();
      sql.close();
    }
    catch(ParseException exception)
    {
      System.err.println("Error parsing ID: "+annotationId);
      throw new StoreException("Error parsing ID: "+annotationId);
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
  }

  /**
   * List the predefined media tracks available for transcripts.
   * @return An ordered list of media track definitions.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted. 
   */
  public MediaTrackDefinition[] getMediaTracks() throws StoreException, PermissionException
  {
    try
    {
      PreparedStatement sql = getConnection().prepareStatement(
        "SELECT * FROM media_track ORDER BY display_order, suffix");
      ResultSet rs = sql.executeQuery();
      Vector<MediaTrackDefinition> tracks = new Vector<MediaTrackDefinition>();
      while (rs.next())
      {
        tracks.add(new MediaTrackDefinition(rs.getString("suffix"), rs.getString("description")));
      } // next layer
      rs.close();
      sql.close();
      return tracks.toArray(new MediaTrackDefinition[0]);
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
  }

  /**
   * List the media available for the given graph.
   * @param id The graph ID.
   * @return List of media files available for the given graph.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the graph was not found in the store.
   */
  public MediaFile[] getAvailableMedia(String id) 
    throws StoreException, PermissionException, GraphNotFoundException
  {
    Vector<MediaFile> files = new Vector<MediaFile>();
    String[] layers = { "corpus", "episode" };
    Graph graph = getGraph(id, layers);
    if (graph.my("corpus") == null || graph.my("episode") == null)
    { // corpus/episode not correctly set
       return new MediaFile[0];
    }

    File corpusDir = new File(getFiles(), graph.my("corpus").getLabel());
    File episodeDir = new File(corpusDir, graph.my("episode").getLabel());
    MediaTrackDefinition[] tracks = getMediaTracks();
    String baseName = graph.getId().replaceAll("\\.[^.]*$","");
    File[] aDirs = episodeDir.listFiles(new FileFilter() 
      {
        public boolean accept(File file)
        {
          return file.isDirectory() && !file.getName().equals("trs");
        }
      });
    if (aDirs != null)
    {
      for (File dir : aDirs)
      {
        // look for a file with the same name as the transcript, and 
        // an extension that matches the directory name
        String extension = dir.getName();
        for (MediaTrackDefinition track : tracks)
        {
          String fileName = baseName + track.getSuffix() + "." + extension;
          File f = new File(dir, fileName);
          if (f.exists())
          {
            MediaFile mediaFile = new MediaFile(f, track.getSuffix());
            if (getBaseUrl() == null) // TODO check this isn't a security risk
            {
              mediaFile.setUrl(f.toURI().toString());
            }
            else
            {
              StringBuffer url = new StringBuffer(getBaseUrl());
              url.append("/files/");
              url.append(graph.my("corpus").getLabel());
              url.append("/");
              url.append(graph.my("episode").getLabel());
              url.append("/");
              url.append(mediaFile.getExtension());
              url.append("/");
              url.append(f.getName());
              mediaFile.setUrl(url.toString());
            }
            files.add(mediaFile);
          } // f.exists()
        } // next track
      } // next subdir
    }
    return files.toArray(new MediaFile[0]);
  }
 
  /**
   * Gets a given media track for a given graph.
   * @param id The graph ID.
   * @param trackSuffix The track suffix of the media - see {@link MediaTrackDefinition#suffix}.
   * @param mimeType The MIME type of the media, which may include parameters for type
   * conversion, e.g. "text/wav; samplerate=16000".
   * @return A URL to the given media for the given graph, or null if the given media doesn't exist.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the graph was not found in the store.
   */
  public String getMedia(String id, String trackSuffix, String mimeType) 
    throws StoreException, PermissionException, GraphNotFoundException
  {
    return getMedia(id, trackSuffix, mimeType, null, null);
  }
  /**
   * Gets a given media track for a given graph.
   * @param id The graph ID.
   * @param trackSuffix The track suffix of the media - see {@link MediaTrackDefinition#suffix}.
   * @param mimeType The MIME type of the media, which may include parameters for type
   * conversion, e.g. "text/wav; samplerate=16000"
   * @param startOffset The start offset of the media sample, or null for the start of the whole
   * recording. 
   * @param endOffset The end offset of the media sample, or null for the end of the whole recording.
   * @return A URL to the given media for the given graph, or null if the given media doesn't
   * exist. 
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the graph was not found in the store.
   */
  public String getMedia(String id, String trackSuffix, String mimeType, Double startOffset, Double endOffset) 
    throws StoreException, PermissionException, GraphNotFoundException
  {
    String[] layers = { "corpus", "episode" };
    Graph graph = getGraph(id, layers);
    File corpusDir = new File(getFiles(), graph.my("corpus").getLabel());
    File episodeDir = new File(corpusDir, graph.my("episode").getLabel());
    String[] mimeTypeParts = mimeType.split(";"); // e.g. "audio/wav; samplerate=16000"
    LinkedHashMap<String,String> mimeTypeParameters = new LinkedHashMap<String,String>();
    for (int p = 1; p < mimeTypeParts.length; p++)
    {
      int equals = mimeTypeParts[p].indexOf('=');
      if (equals >= 0)
      {
        mimeTypeParameters.put(mimeTypeParts[p].substring(0, equals).toLowerCase().trim(),
                               mimeTypeParts[p].substring(equals+1).trim());
      } // '=' is present
    } // next parameter
    mimeType = mimeTypeParts[0];
    String extension = MediaFile.MimeTypeToSuffix().get(mimeType);
    if (extension == null) throw new StoreException("Unknown MIME type: " + mimeType);
    File mediaDir = new File(episodeDir, extension);
    if (trackSuffix == null) trackSuffix = getMediaTracks()[0].getSuffix();
    String fileName = graph.getId().replaceAll("\\.[^.]*$","") + trackSuffix + "." + extension;
    File file = new File(mediaDir, fileName);
    if (file.exists())
    {
      if (startOffset == null && endOffset == null)
      {
        if (getBaseUrl() == null) // TODO check this isn't a security risk
        {
          return file.toURI().toString(); // TODO resampling?
        }
        else
        {
          try
          {
            StringBuffer url = new StringBuffer(getBaseUrl());
            url.append("/files/");
            url.append(URLEncoder.encode(graph.my("corpus").getLabel(), "UTF-8"));
            url.append("/");
            url.append(URLEncoder.encode(graph.my("episode").getLabel(), "UTF-8"));
            url.append("/");
            url.append(URLEncoder.encode(extension, "UTF-8"));
            url.append("/");
            url.append(URLEncoder.encode(fileName, "UTF-8"));
            return url.toString();
          }
          catch(UnsupportedEncodingException exception)
          {
            throw new StoreException(exception);
          }
        }
      }
      else // a fragment
      {
        if (getBaseUrl() == null) // TODO check this isn't a security risk
        {
          FragmentExtractor extractor = new FragmentExtractor();
          if (mimeTypeParameters.containsKey("samplerate"))
          {
            extractor.setSampleRate(
              Integer.parseInt(mimeTypeParameters.get("samplerate")));
          }
          extractor.setStart(startOffset);
          extractor.setEnd(endOffset);
          try
          {
            File fragment = File.createTempFile(
              "SqlGraphStore.saveMedia_",
              Graph.FragmentId(id, startOffset, endOffset) + "." + IO.Extension(file));
            fragment.deleteOnExit();
            MediaThread thread = extractor.start(mimeType, file, mimeType, fragment);          
            // wait for extractor to finish
            thread.join();
            if (thread.getLastError() == null)
            {
              return fragment.toURI().toString();
            }
            else
            {
              throw new StoreException(
                "Could not extract " + file.getName() + " to " + fragment.getName(),
                thread.getLastError());
            }
          }
          catch(Exception exception)
          {
            throw new StoreException(exception);
          }
        }
        else
        {
          StringBuffer url = new StringBuffer(getBaseUrl());
          url.append("/soundfragment");
          url.append("?id=");
          try
          {
            url.append(URLEncoder.encode(id, "UTF-8"));
          }
          catch(UnsupportedEncodingException exception)
          {
            throw new StoreException(exception);
          }
          url.append("&start=");
          url.append(startOffset.toString());
          url.append("&end=");
          url.append(endOffset.toString());          
          for (String name : mimeTypeParameters.keySet())
          {
            url.append("&");
            url.append(name);
            url.append("=");
            url.append(mimeTypeParameters.get(name));
          } // next parameter
          return url.toString();
        }
      }
    }
    else
    {
      return null;
    }
  }

  /**
   * Saves the given media for the given graph.
   * @param id The graph ID
   * @param trackSuffix The track suffix of the media - see {@link MediaTrackDefinition#suffix}.
   * @param mediaUrl A URL to the media content.
   * @throws StoreException If an error prevents the media from being saved.
   * @throws PermissionException If saving the media is not permitted.
   * @throws GraphNotFoundException If the graph doesn't exist.
   */
  public void saveMedia(String id, String trackSuffix, String mediaUrl)
    throws StoreException, PermissionException, GraphNotFoundException
  {
    Vector<File> toDelete = new Vector<File>();
    try
    {
      String[] layers = { "corpus", "episode" };
      Graph graph = getGraph(id, layers);
      File corpusDir = new File(getFiles(), graph.my("corpus").getLabel());
      if (!corpusDir.exists()) corpusDir.mkdir();
      File episodeDir = new File(corpusDir, graph.my("episode").getLabel());
      if (!episodeDir.exists()) episodeDir.mkdir();
	 
      // get the content
      File source = null;
      if (!mediaUrl.startsWith("file:"))
      { // not file URL, but we need one, so download content to a temporary file
        URL url = new URL(mediaUrl);
        URLConnection connection = url.openConnection();
        String mimeType = connection.getContentType();
        String extension = MediaFile.MimeTypeToSuffix().get(mimeType);
        if (extension == null) throw new StoreException("Unknown MIME type: " + mimeType);
        String destinationName = graph.getId().replaceAll("\\.[^.]*$","") 
          + trackSuffix + "." + extension;
        source = File.createTempFile(destinationName, "."+extension);
        source.deleteOnExit();
        toDelete.add(source);
        try
        {
          IO.SaveUrlConnectionToFile(connection, source);
        }
        catch(IOException exception)
        {
          throw new StoreException(
            "Could not save " + url + " to " + source.getPath(), exception);
        }	    
      } // not file URL
      else
      { // file URL
        // rename/take a copy of the file, so the caller can delete it with impunity
        File mediaFile = new File(new URI(mediaUrl));
        source = File.createTempFile("SqlGraphStore.saveMedia_", "_" + mediaFile.getName());
        source.deleteOnExit();
        toDelete.add(source);
        if (!mediaFile.renameTo(source))
        { // can't rename, have to copy the data
          try
          {
            IO.Copy(mediaFile, source);
          }
          catch(IOException exception)
          {
            throw new StoreException(
              "Could not copy " + mediaFile.getPath() + " to " + source.getPath(), exception);
          }
        } // copy instead of rename
      }

      // determine the destination path
      MediaFile mediaFile = new MediaFile(source, trackSuffix);
      File mediaDir = new File(
        episodeDir, mediaFile.getType().equals("other")?"doc":mediaFile.getExtension());
      if (!mediaDir.exists()) mediaDir.mkdir();
      String destinationName = graph.getId().replaceAll("\\.[^.]*$","") 
        + trackSuffix + "." + mediaFile.getExtension();
      File destination = new File(mediaDir, destinationName);
	 
      // backup old file if it exists
      IO.Backup(destination);

      String downsampleWav = getSystemAttribute("downsampleWav");
      if (downsampleWav.length() > 0 && mediaFile.getMimeType().equals("audio/wav"))
      {
        File downsampled = File.createTempFile("SqlGraphStore.saveMedia_", "_"+downsampleWav+"_" + mediaFile.getName());
        downsampled.deleteOnExit();
        toDelete.add(downsampled);
	    
        ParameterSet configuration = new ParameterSet();
        configuration.addParameter(new Parameter("sampleRate", downsampleWav.equals("mono16kHz")?16000:22050));
        IMediaConverter resampler = new Resampler();
        resampler.configure(configuration);

        // run the resampler
        MediaThread thread = resampler.start(mediaFile.getMimeType(), source, mediaFile.getMimeType(), downsampled);

        // wait for resampler to finish
        thread.join();
        if (thread.getLastError() == null)
        {
          source = downsampled;
        }
        else
        {
          throw new StoreException(
            "Could not resample " + source.getPath() + " to " + downsampleWav, thread.getLastError());
        }
      }

      // does the file need censoring?
      String censorshipRegexp = getSystemAttribute("censorshipRegexp");
      if (censorshipRegexp == null || censorshipRegexp.length() == 0)
      { // no censorship required, so just move the file
        if (!source.renameTo(destination))
        { // can't rename, have to copy the data
          try
          {
            IO.Copy(source, destination);
          }
          catch(IOException exception)
          {
            throw new StoreException(
              "Could not copy " + source.getPath() + " to " + destination.getPath(), exception);
          }
        } // copy instead of rename
      } // no censorship required, so just move the file
      else
      { // censorship required

        // check ffmpeg configuration
        Pattern censorshipPattern = Pattern.compile(censorshipRegexp);
        String censorshipLayer = getSystemAttribute("censorshipLayer");
        String censorshipFfmpegAudioFilter = getSystemAttribute("censorshipFfmpegAudioFilter");
        String ffmpegPath = getSystemAttribute("ffmpegPath");
        if (ffmpegPath == null || ffmpegPath.length() == 0)
        {
          throw new StoreException("Censorship is configured, but ffmpeg path is not set");
        }
        File exe = new File(ffmpegPath, "ffmpeg");
        if (!exe.exists())
        {
          throw new StoreException("Censorship is configured, but ffmpeg path is invalid: " + ffmpegPath);
        }

        // load the censorship layer
        Schema schema = getSchema();
        String[] censorshipLayers = {
          schema.getParticipantLayerId(),
          schema.getTurnLayerId(),
          schema.getUtteranceLayerId(),
          censorshipLayer };
        graph = getGraph(id, censorshipLayers);
	    
        // get censorship boundaries
        Vector<Double> boundaries = new Vector<Double>();
        double lastEnd = 0.0;
        // get all censor annotations, sorted by anchor to ensure that if list returns
        // them out of order, they're processed in temporal order
        for (Annotation annotation : new AnnotationsByAnchor(graph.list(censorshipLayer)))
        {
          if (censorshipPattern.matcher(annotation.getLabel()).matches())
          { // matching annotation
            try
            {
              // ensure points don't go backwards, and use offset Min/Max to widen interval
              // when alignment is uncertain
              double start = annotation.getStart().getOffset().doubleValue();
              if (annotation.getStart().getConfidence() < Constants.CONFIDENCE_AUTOMATIC)
              { // uncertain start
                // find the containing utterance, and use its start
                Annotation turn = annotation.my(schema.getTurnLayerId());
                for (Annotation line : turn.list(schema.getUtteranceLayerId()))
                {
                  if (line.includesOffset(start))
                  { // found the containing utterance
                    start = line.getStart().getOffset();
                    if (line.getStart().getConfidence() < Constants.CONFIDENCE_AUTOMATIC)
                    { // still uncertain start
                      // use turn start
                      start = turn.getStart().getOffset();
                    }
                    break;
                  } // found the containing utterance
                } // next line
              } // uncertain start
              // ensure doesn't got backwards
              start = Math.max(start, lastEnd);
              double end = annotation.getEnd().getOffset().doubleValue();
              if (annotation.getEnd().getConfidence() < Constants.CONFIDENCE_AUTOMATIC)
              { // uncertain end
                // find the containing utterance, and use its end
                Annotation turn = annotation.my(schema.getTurnLayerId());
                for (Annotation line : turn.list(schema.getUtteranceLayerId()))
                {
                  if (line.includesOffset(end))
                  { // found the containing utterance
                    end = line.getEnd().getOffset();
                    if (line.getEnd().getConfidence() < Constants.CONFIDENCE_AUTOMATIC)
                    { // still uncertain end
                      // use turn end
                      end = turn.getEnd().getOffset();
                    }
                    break;
                  } // found the containing utterance
                } // next line
              } // uncertain end
		     
              // add interval
              boundaries.add(start);
              boundaries.add(end);
		     
              lastEnd = end;
            }
            catch(NullPointerException exception)
            {
              // rollback possibl nullification of anchors, to give the most info possible
              annotation.getStart().rollback();
              annotation.getEnd().rollback();
              throw new StoreException("Uncertain boundaries for annotation: " + annotation
                                       + " ("+annotation.getStart()+"-"+annotation.getEnd()+")");
            }
          } // matching annotation
        } // next annotation

        // create a censor
        ParameterSet configuration = new ParameterSet();
        configuration.addParameter(new Parameter("ffmpegPath", exe.getParentFile()));
        configuration.addParameter(new Parameter("audioFilter", censorshipFfmpegAudioFilter));
        configuration.addParameter(new Parameter("deleteSource", Boolean.TRUE));
        IMediaCensor censor = new FfmpegCensor();
        censor.configure(configuration);

        // run the censor
        MediaThread thread = censor.start(mediaFile.getMimeType(), source, boundaries, destination);	    

      } // censorship required

    }
    catch (URISyntaxException urix)
    {
      throw new StoreException("Invalid URL: " + mediaUrl, urix);
    }
    catch (Throwable t)
    {
      throw new StoreException(t);
    }
    finally
    {
      // anything added to toDelete should be deleted, if it's still there
      for (File f : toDelete) f.delete();
    }
  }
  /**
   * Saves the given source file (transcript) for the given graph
   * @param id The graph ID
   * @param url A URL to the transcript.
   * @throws StoreException If an error prevents the media from being saved.
   * @throws PermissionException If saving the media is not permitted.
   * @throws GraphNotFoundException If the graph doesn't exist.
   */
  public void saveSource(String id, String url)
    throws StoreException, PermissionException, GraphNotFoundException
  {
    try
    {
      String[] layers = { "corpus", "episode" };
      Graph graph = getGraph(id, layers);
      File corpusDir = new File(getFiles(), graph.my("corpus").getLabel());
      if (!corpusDir.exists()) corpusDir.mkdir();
      File episodeDir = new File(corpusDir, graph.my("episode").getLabel());
      if (!episodeDir.exists()) episodeDir.mkdir();
	 
      // get the content
      if (url.startsWith("file:")) 
      { // file URL
        File sourceDir = new File(episodeDir, "trs");
        if (!sourceDir.exists()) sourceDir.mkdir();
        File source = new File(new URI(url));
        String destinationName = graph.getId().replaceAll("\\.[^.]*$","") + "." + IO.Extension(source);
        File destination = new File(sourceDir, destinationName);

        // backup old file if it exists
        IO.Backup(destination);

        if (!source.renameTo(destination))
        { // can't rename, have to copy the data TODO
          try
          {
            IO.Copy(source, destination);
          }
          catch(IOException exception)
          {
            throw new StoreException(
              "Could not copy " + source.getPath() + " to " + destination.getPath(), exception);
          }
        } // copy instead of rename
      } // file URL
      else
      { // not file URL
        String extension = url.substring(url.lastIndexOf('.') + 1);
        File sourceDir = new File(episodeDir, "trs");
        String destinationName = graph.getId().replaceAll("\\.[^.]*$","")  + "." + extension;
        File destination = new File(sourceDir, destinationName);
        try
        {
          IO.SaveUrlToFile(new URL(url), destination);
        }
        catch(IOException exception)
        {
          throw new StoreException(
            "Could not save " + url + " to " + destination.getPath(), exception);
        }	    
      } // not file URL
    }
    catch (URISyntaxException urix)
    {
      throw new StoreException("Invalid URL: " + url, urix);
    }
    catch (Throwable t)
    {
      throw new StoreException(t);
    }
  }

  /**
   * Saves the given document for the episode of the given graph.
   * @param id The graph ID
   * @param url A URL to the document.
   * @throws StoreException If an error prevents the media from being saved.
   * @throws PermissionException If saving the media is not permitted.
   * @throws GraphNotFoundException If the graph doesn't exist.
   */
  public void saveEpisodeDocument(String id, String url)
    throws StoreException, PermissionException, GraphNotFoundException
  {
    try
    {
      String[] layers = { "corpus", "episode" };
      Graph graph = getGraph(id, layers);
      File corpusDir = new File(getFiles(), graph.my("corpus").getLabel());
      if (!corpusDir.exists()) corpusDir.mkdir();
      File episodeDir = new File(corpusDir, graph.my("episode").getLabel());	
      if (!episodeDir.exists()) episodeDir.mkdir();
 
      // get the content
      if (url.startsWith("file:")) 
      { // file URL
        File docDir = new File(episodeDir, "doc");
        if (!docDir.exists()) docDir.mkdir();
        File source = new File(new URI(url));
        // docs saved with their own name
        File destination = new File(docDir, source.getName());

        // backup old file if it exists
        IO.Backup(destination);

        if (!source.renameTo(destination))
        { // can't rename, have to copy the data TODO
          try
          {
            IO.Copy(source, destination);
          }
          catch(IOException exception)
          {
            throw new StoreException(
              "Could not copy " + source.getPath() + " to " + destination.getPath(), exception);
          }
        } // copy instead of rename
      } // file URL
      else
      { // not file URL
        URL u = new URL(url);
        File docDir = new File(episodeDir, "doc");
        String destinationName = u.getPath();
        if (destinationName.indexOf('/') >= 0)
        {
          destinationName = destinationName.substring(destinationName.lastIndexOf('/') + 1);
          if (destinationName.length() == 0) destinationName = u.getPath();
        }
        File destination = new File(docDir, destinationName);
        try
        {
          IO.SaveUrlToFile(u, destination);
        }
        catch(IOException exception)
        {
          throw new StoreException(
            "Could not save " + url + " to " + destination.getPath(), exception);
        }	    
      } // not file URL
    }
    catch (URISyntaxException urix)
    {
      throw new StoreException("Invalid URL: " + url, urix);
    }
    catch (Throwable t)
    {
      throw new StoreException(t);
    }
  }

  /**
   * Get a list of documents associated with the episode of the given graph.
   * @param id The graph ID.
   * @return List of document files/URLs.
   * @throws StoreException If an error prevents the media from being saved.
   * @throws PermissionException If saving the media is not permitted.
   * @throws GraphNotFoundException If the graph doesn't exist.
   */
  public MediaFile[] getEpisodeDocuments(String id)
    throws StoreException, PermissionException, GraphNotFoundException
  {
    Vector<MediaFile> files = new Vector<MediaFile>();
    String[] layers = { "corpus", "episode" };
    Graph graph = getGraph(id, layers);
    File corpusDir = new File(getFiles(), graph.my("corpus").getLabel());
    File episodeDir = new File(corpusDir, graph.my("episode").getLabel());
    File docDir = new File(episodeDir, "doc");
    for (File f : docDir.listFiles())
    {
      MediaFile mediaFile = new MediaFile(f, "");
      if (getBaseUrl() == null) // TODO check this isn't a security risk
      {
        mediaFile.setUrl(f.toURI().toString());
      }
      else
      {
        StringBuffer url = new StringBuffer(getBaseUrl());
        url.append("/files/");
        url.append(graph.my("corpus").getLabel());
        url.append("/");
        url.append(graph.my("episode").getLabel());
        url.append("/doc/");
        url.append(f.getName());
        mediaFile.setUrl(url.toString());
      }
      files.add(mediaFile);
    } // next doc file
    return files.toArray(new MediaFile[0]);      
  }
   
   
  /**
   * Generates any media files that are not marked "on demand" and for which there are available conversions.
   * <p>This implementation starts conversion processes, and returns immediately, so new files may not exist immediately after the method returns.
   * @param id The graph ID.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the graph was not found in the store.
   */
  public void generateMissingMedia(String id)
    throws StoreException, PermissionException, GraphNotFoundException
  {
    try
    {
      // for now only ffmpeg conversion is supported TODO
      String ffmpegPath = getSystemAttribute("ffmpegPath");
      if (ffmpegPath == null || ffmpegPath.length() == 0)
      {
        System.out.println("SqlGraphStore.generateMissingMedia: ffmpegPath not set");
        return;
      }
      File exe = new File(ffmpegPath, "ffmpeg");
      if (!exe.exists())
      {
        System.out.println("SqlGraphStore.generateMissingMedia: ffmpegPath not found: " + exe.getPath());
        return;
      }
	 
      MediaFile[] availableMedia = getAvailableMedia(id);
      // first index them by track suffix, and then by MIME type
      HashMap<String,HashMap<String,MediaFile>> tracks = new HashMap<String,HashMap<String,MediaFile>>();
      for (MediaFile file : availableMedia)
      {
        if (!tracks.containsKey(file.getTrackSuffix()))
        {
          tracks.put(file.getTrackSuffix(), new HashMap<String,MediaFile>());
        }
        tracks.get(file.getTrackSuffix()).put(file.getMimeType(), file);
      } // next file

      // what media types do we want? (key=MIME type, value=extension)
      LinkedHashMap<String,String> wantedTypes = new LinkedHashMap<String,String>();
      PreparedStatement sql = getConnection().prepareStatement(
        "SELECT mimetype, extension FROM media_type WHERE on_demand = 0 ORDER BY mimetype");
      ResultSet rs = sql.executeQuery();
      while (rs.next())
      {
        wantedTypes.put(rs.getString("mimetype"), rs.getString("extension"));
      } // next layer
      rs.close();
      sql.close();

      Vector<MediaFile> wantedFiles = new Vector<MediaFile>();
      // check each track, and add any types that are missing
      for (String trackSuffix: tracks.keySet())
      {
        HashMap<String,MediaFile> filesInTrack = tracks.get(trackSuffix);
        for (String mediaType : wantedTypes.keySet())
        {
          if (!filesInTrack.containsKey(mediaType))
          {
            MediaFile wantedFile = new MediaFile();
            wantedFile.setTrackSuffix(trackSuffix);
            wantedFile.setMimeType(mediaType);
            // we'll set the rest of the details later...
            wantedFiles.add(wantedFile);
          }
        } // next wanted type
      } // next track suffix

      if (wantedFiles.size() > 0)
      { // there are missing files
	    
        // need some info about the graph...
        String[] layers = { "corpus", "episode" };
        Graph graph = getGraph(id, layers);
        File corpusDir = new File(getFiles(), graph.my("corpus").getLabel());
        File episodeDir = new File(corpusDir, graph.my("episode").getLabel());

        PreparedStatement sqlConversions = getConnection().prepareStatement(
          "SELECT from_mimetype, method, arguments FROM media_conversion"
          +" WHERE method = 'ffmpeg'" // TODO support other conversion methods
          +" AND to_mimetype = ? ORDER BY from_mimetype");

        // for each file we want:
        for (MediaFile wantedFile : wantedFiles)
        {
          // figure out file name
          String extension = MediaFile.MimeTypeToSuffix().get(wantedFile.getMimeType());
          if (extension == null) extension = wantedTypes.get(wantedFile.getMimeType());
          File mediaDir = new File(episodeDir, extension);
          String destinationName = graph.getId().replaceAll("\\.[^.]*$","") 
            + wantedFile.getTrackSuffix() + "." + extension;
          wantedFile.setFile(new File(mediaDir, destinationName));

          // look for a conversion
          sqlConversions.setString(1, wantedFile.getMimeType());
          ResultSet rsConversions = sqlConversions.executeQuery();
          while (rsConversions.next())
          {
            String fromMimeType = rsConversions.getString("from_mimetype");
            // do we have a file with that MIME type in the same track?
            HashMap<String,MediaFile> track = tracks.get(wantedFile.getTrackSuffix());
            if (track.containsKey(fromMimeType))
            { // the source MIME type exists - we have a winner
              MediaFile gotFile = track.get(fromMimeType);
              // ensure destination directory exists
              if (!wantedFile.getFile().getParentFile().exists())
              {
                if (!wantedFile.getFile().getParentFile().mkdir())
                {
                  System.err.println("SqlGraphStore.generateMissingMedia: mkdir " + wantedFile.getFile().getParentFile().getPath() + " - failed");
                }
              }
              // convert
              ParameterSet configuration = new ParameterSet();
              configuration.addParameter(new Parameter("ffmpegPath", exe.getParentFile()));
              configuration.addParameter(
                new Parameter("conversionCommandLine", rsConversions.getString("arguments")));
              IMediaConverter converter = new FfmpegConverter();
              converter.configure(configuration);
              MediaThread thread = converter.start(
                fromMimeType, gotFile.getFile(), wantedFile.getMimeType(), wantedFile.getFile());
              break;
            } // the source MIME type exists
          } // next conversion
          rsConversions.close();
        } // next wanted file
        sqlConversions.close();

      } // there are missing files
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
    catch(MediaException exception)
    {
      throw new StoreException(exception);
    }
      
  } // end of generateMissingMedia()

  /**
   * Deletes the given graph, and all associated files.
   * @param id The ID graph to save.
   * @throws StoreException If an error prevents the graph from being saved.
   * @throws PermissionException If saving the graph is not permitted.
   * @throws GraphNotFoundException If the graph doesn't exist.
   */
  public void deleteGraph(String id)
    throws StoreException, PermissionException, GraphNotFoundException
  {
    try
    {
      String[] layers = { "corpus", "episode" };
      Graph graph = getGraph(id, layers);
      int iAgId = ((Integer)graph.get("@ag_id")).intValue();

      // media
      for (MediaFile media : getAvailableMedia(graph.getId()))
      {
         if (!media.getFile().delete())
         {
            System.err.println("Could not delete " + media.getFile().getPath());
         }
      } // next media file
      
      if (graph.my("corpus") != null && graph.my("episode") != null)
      {
         // files to delete...
         File corpusDir = new File(getFiles(), graph.my("corpus").getLabel());
         File episodeDir = new File(corpusDir, graph.my("episode").getLabel());
         
         // transcript
         File trs = new File(episodeDir, "trs");
         File transcript = new File(trs, graph.getId());
         if (transcript.exists())
         {
            if (!transcript.delete())
            {
               System.err.println("Could not delete " + transcript.getPath());
            }
         }
      } // corpus/episode correctly set
	 
      // delete records from the database
	    
      // for each layer
      PreparedStatement selectLayers = getConnection().prepareStatement(
        "SELECT layer_id, scope FROM layer WHERE layer_id >= 0"
        +" ORDER BY layer_id DESC");
      ResultSet rsLayers = selectLayers.executeQuery();
      PreparedStatement deleteLayerData = getConnection().prepareStatement(
        "DELETE FROM annotation_layer_? WHERE ag_id = ?");
      deleteLayerData.setInt(2, iAgId);
      while (rsLayers.next())
      {
        int iLayerId = rsLayers.getInt("layer_id");
        deleteLayerData.setInt(1, iLayerId);
        deleteLayerData.executeUpdate();
      } // next layer
      deleteLayerData.close();
      rsLayers.close();
      selectLayers.close();
	 
      PreparedStatement deleteAnchors = getConnection().prepareStatement(
        "DELETE FROM anchor WHERE ag_id = ?");
      deleteAnchors.setInt(1, iAgId);
      deleteAnchors.executeUpdate();
      deleteAnchors.close();
	    
      PreparedStatement deleteSpeakers = getConnection().prepareStatement(
        "DELETE FROM transcript_speaker WHERE ag_id = ?");
      deleteSpeakers.setInt(1, iAgId);
      deleteSpeakers.executeUpdate();
      deleteSpeakers.close();
      PreparedStatement deleteAttributes = getConnection().prepareStatement(
        "DELETE FROM annotation_transcript WHERE ag_id = ?");
      deleteAttributes.setInt(1, iAgId);
      deleteAttributes.executeUpdate();
      deleteAttributes.close();
      PreparedStatement deleteTranscript = getConnection().prepareStatement(
        "DELETE FROM transcript WHERE ag_id = ?");
      deleteTranscript.setInt(1, iAgId);
      deleteTranscript.executeUpdate();
      deleteTranscript.close();
    }
    catch(SQLException exception)
    {
      throw new StoreException(exception);
    }
  }
   
  /**
   * Gets the value of a system attribute.
   * @param name Attribute name.
   * @return The value of the system attribute, or null if there is no value.
   * @throws SQLException
   */
  public String getSystemAttribute(String name)
    throws SQLException
  {
    String value = null;
    PreparedStatement sql = getConnection().prepareStatement(
      "SELECT value FROM system_attribute WHERE name = ?");
    sql.setString(1, name);
    try
    {
      ResultSet rs = sql.executeQuery();
      try
      {
        if (rs.next()) value = rs.getString("value");
      }
      finally
      {
        rs.close();
      }
    }
    finally
    {
      sql.close();
    }
    return value;
  } // end of getSystemAttribute()


} // end of class SqlGraphStore
