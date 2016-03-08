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
package nzilbb.ag.sql;

import nzilbb.ag.*;
import nzilbb.ag.util.Validator;
import java.sql.*;
import java.util.Vector;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.SortedSet;
import java.util.HashSet;
import java.text.MessageFormat;
import java.text.ParseException;

/**
 * Graph store that uses a relational database as its back end.
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

   /** Format of annotation IDs for transcript attributes, where {0} = attribute and {1} = ag_id */
   protected MessageFormat fmtTranscriptAttributeId = new MessageFormat("t_{0}_{1,number,0}");

   /** Format of annotation IDs for participant attributes, where {0} = attribute and {1} = speaker number */
   protected MessageFormat fmtParticipantAttributeId = new MessageFormat("p_{0}_{1,number,0}");

   /** Format of anchor IDs, where {0} = anchor_id */
   protected MessageFormat fmtAnchorId = new MessageFormat("n_{0,number,0}");

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
   public void setConnection(Connection newConnection) { connection = newConnection; }

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
   public void setId(String newId) { id = newId; }

   
   // Methods:
   
   /**
    * Default constructor.
    */
   public SqlGraphStore()
   {
   } // end of constructor

   /**
    * Constructor with connection.
    * @param connection An opened database connection.
    */
   public SqlGraphStore(Connection connection)
   {
      setConnection(connection);
   } // end of constructor

   /**
    * Constructor with connection parameters.
    * @param connectString The database connection string.
    * @param user The database username.
    * @param password The databa password.
    * @throws SQLException If an error occurs during connection.
    */
   public SqlGraphStore(String connectString, String user, String password)
      throws SQLException
   {
      setConnection(DriverManager.getConnection (connectString, user, password));
      setId(connectString);
   } // end of constructor

   /**
    * Called when the object is garbage-collected.
    */
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

   /**
    * Gets the layer schema.
    * @return A schema defining the layers and how they relate to each other.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public Schema getSchema() throws StoreException, PermissionException
   {
      Schema schema = new Schema();
      for (String layerId : getLayerIds())
      {
	 Layer layer = getLayer(layerId);
	 schema.addLayer(layer);
	 if (layer.get("@layer_id").equals(new Integer(11)))
	 {
	    schema.setTurnLayerId(layer.getId());
	 }
	 else if (layer.get("@layer_id").equals(new Integer(12)))
	 {
	    schema.setUtteranceLayerId(layer.getId());
	 } 
	 else if (layer.get("@layer_id").equals(new Integer(0)))
	 {
	    schema.setWordLayerId(layer.getId());
	 } 
      } // next layer
      schema.setParticipantLayerId("who");
      return schema;
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
	 PreparedStatement sqlParentId = getConnection().prepareStatement(
	    "SELECT short_description FROM layer WHERE layer_id = ?");
	 PreparedStatement sql = getConnection().prepareStatement(
	    "SELECT * FROM layer WHERE short_description = ?");
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

	    // other attributes
	    layer.put("@layer_id", new Integer(rs.getInt("layer_id")));
	    layer.put("@type", rs.getString("type"));
	    layer.put("@user_id", rs.getString("user_id"));
	    layer.put("@layer_manager_id", rs.getString("layer_manager_id"));
	    layer.put("@extra", rs.getString("extra"));
	    layer.put("@scope", rs.getString("scope"));
	    layer.put("@enabled", rs.getString("enabled"));
	    layer.put("@notes", rs.getString("notes"));
	    layer.put("@project_id", rs.getString("project_id"));
	    layer.put("@data_mime_type", rs.getString("data_mime_type"));
	    layer.put("@alignment", rs.getString("alignment"));
	    
	    rs.close();
	    sql.close();
	    sqlParentId.close();

	    return layer;
	 }
	 else
	 {
	    rs.close();
	    sql.close();
	    sqlParentId.close();
	    
	    // maybe a transcript attribute
	    sql = getConnection().prepareStatement(
	       "SELECT * FROM attribute_definition WHERE CONCAT('transcript_', attribute) = ?");
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
	       layer.setPeers(false);
	       layer.setPeersOverlap(false);
	       layer.setSaturated(true);

	       // other attributes
	       layer.put("@class_id", rs.getString("class_id"));
	       layer.put("@attribute", rs.getString("attribute"));
	       layer.put("@category", rs.getString("category"));
	       layer.put("@type", rs.getString("type"));
	       layer.put("@style", rs.getString("style"));
	       layer.put("@label", rs.getString("label"));
	       layer.put("@description", rs.getString("description"));
	       layer.put("@display_order", rs.getString("display_order"));
	       layer.put("@searchable", rs.getString("searchable"));
	       layer.put("@access", rs.getString("access"));
	    
	       rs.close();
	       sql.close();
	       sqlParentId.close();
	       
	       return layer;
	    }
	    else
	    {
	       rs.close();
	       sql.close();
	       sqlParentId.close();
	       
	       // maybe a participant attribute
	       sql = getConnection().prepareStatement(
		  "SELECT * FROM attribute_definition WHERE CONCAT('participant_', attribute) = ?");
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
		  layer.setPeers(false);
		  layer.setPeersOverlap(false);
		  layer.setSaturated(true);
		  
		  // other attributes
		  layer.put("@class_id", rs.getString("class_id"));
		  layer.put("@attribute", rs.getString("attribute"));
		  layer.put("@category", rs.getString("category"));
		  layer.put("@type", rs.getString("type"));
		  layer.put("@style", rs.getString("style"));
		  layer.put("@label", rs.getString("label"));
		  layer.put("@description", rs.getString("description"));
		  layer.put("@display_order", rs.getString("display_order"));
		  layer.put("@searchable", rs.getString("searchable"));
		  layer.put("@access", rs.getString("access"));
		  
		  rs.close();
		  sql.close();
		  sqlParentId.close();
		  
		  return layer;
	       }
	       else
	       {
		  throw new StoreException("Layer not found: " + id);
	       }
	    } // not a transcript attribute
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
	    "SELECT transcript_id FROM transcript ORDER BY transcript_id");
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
    * @param corpus A corpus ID.
    * @return A list of graph IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getGraphIdsInCorpus(String corpus) throws StoreException, PermissionException
   {
      try
      {
	 PreparedStatement sql = getConnection().prepareStatement(
	    "SELECT transcript_id FROM transcript WHERE corpus_name = ? ORDER BY transcript_id");
	 sql.setString(1, corpus);
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
    * Gets a list of IDs of graphs that include the given participant.
    * @param participant A participant ID.
    * @return A list of graph IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getGraphIdsWithParticipant(String participant) throws StoreException, PermissionException
   {
      try
      {
	 PreparedStatement sql = getConnection().prepareStatement(
	    "SELECT transcript.transcript_id"
	    +" FROM transcript"
	    +" INNER JOIN transcript_speaker ON transcript.ag_id = transcript_speaker.ag_id"
	    +" INNER JOIN speaker ON speaker.speaker_number = transcript_speaker.speaker_number"
	    +" WHERE speaker.name = ? AND COALESCE(speaker.name,'') <> ''"
	    +" ORDER BY transcript_id");
	 sql.setString(1, participant);
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
      try
      {
	 // in layer_id order, to ensure that comment parents (transcript & turn) are loaded first
	 PreparedStatement sql = getConnection().prepareStatement(
	    "SELECT short_description FROM layer ORDER BY layer_id");
	 ResultSet rs = sql.executeQuery();
	 Vector<String> layerIds = new Vector<String>();
	 while (rs.next())
	 {
	    layerIds.add(rs.getString("short_description"));
	 } // next layer
	 rs.close();
	 sql.close();

	 return getGraph(id, layerIds.toArray(new String[0]));
      }
      catch (SQLException x)
      {
	 throw new StoreException(x);
      }
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

	 PreparedStatement sql = getConnection().prepareStatement(
	    "SELECT transcript.*, transcript_family.name AS series,"
	    +" transcript_type.transcript_type"
	    +" FROM transcript"
	    +" INNER JOIN transcript_family ON transcript.family_id = transcript.family_id"
	    +" INNER JOIN transcript_type ON transcript.type_id = transcript_type.type_id"
	    +" WHERE transcript_id = ?");
	 sql.setString(1, id);
	 ResultSet rs = sql.executeQuery();
	 if (!rs.next())
	 { // graph not found - maybe we've been given an ag_id?
	    rs.close();
	    sql.close();
	    sql = getConnection().prepareStatement(
	       "SELECT transcript.*, transcript_family.name AS series,"
	       +" transcript_type.transcript_type"
	       +" FROM transcript"
	       +" INNER JOIN transcript_family ON transcript.family_id = transcript.family_id"
	       +" INNER JOIN transcript_type ON transcript.type_id = transcript_type.type_id"
	       +" WHERE ag_id = ?");
	    sql.setString(1, id);
	    rs = sql.executeQuery();
	 }
	 if (!rs.next()) throw new GraphNotFoundException(id);
	 
	 graph.setId(rs.getString("transcript_id"));
	 int iAgId = rs.getInt("ag_id");
	 graph.put("@ag_id", new Integer(iAgId));
	 graph.setCorpus(rs.getString("corpus_name"));
	 graph.put("@transcript_type", rs.getString("transcript_type"));
	 graph.put("@series", rs.getString("series"));
	 graph.setOrdinal(rs.getInt("family_sequence"));
	 graph.put("@offset_in_series", new Double(rs.getInt("family_offset")));

	 rs.close();

	 Vector<String> setStartEndLayers = new Vector<String>();

	 if (layerIds != null)
	 {
	    // load annotations
	    PreparedStatement sqlAnnotation = getConnection().prepareStatement(
	       "SELECT layer.*,"
	       +" start.offset AS start_offset, start.alignment_status AS start_alignment_status,"
	       +" end.offset AS end_offset, end.alignment_status AS end_alignment_status"
	       +" FROM annotation_layer_? layer"
	       +" INNER JOIN anchor start ON layer.start_anchor_id = start.anchor_id"
	       +" INNER JOIN anchor end ON layer.end_anchor_id = end.anchor_id"
	       +" WHERE layer.ag_id = ? ORDER BY start_offset, end_offset DESC, annotation_id");
	    sqlAnnotation.setInt(2, iAgId);
	    for (String layerId : layerIds)
	    {
	       if (layerId.equals("graph"))
	       { // special case
		  continue;
	       }
	       else if (layerId.equals("who"))
	       {
		  // create participant layer...
		  // thereby creating a lookup list of participant names
		  Layer participantLayer = getLayer(layerId);
		  graph.addLayer(participantLayer);
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
			participantLayer.get("@layer_id"), rsParticipant.getString("speaker_number")};
		     Annotation participant = new Annotation(
			fmtMetaAnnotationId.format(annotationIdParts), 
			rsParticipant.getString("name"), participantLayer.getId());
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
		  Layer episodeLayer = getLayer(layerId);
		  graph.addLayer(episodeLayer);
		  PreparedStatement sqlEpisode = getConnection().prepareStatement(
		     "SELECT t.family_id, e.name"
		     +" FROM transcript t"
		     +" INNER JOIN transcript_family e ON e.family_id = t.family_id"
		     +" WHERE t.ag_id = ?");
		  sqlEpisode.setInt(1, iAgId);
		  ResultSet rsEpisode = sqlEpisode.executeQuery();
		  if (rsEpisode.next())
		  {
		     // add graph-tag annotation
		     Object[] annotationIdParts = {
			episodeLayer.get("@layer_id"), rsEpisode.getString("family_id")};
		     Annotation episode = new Annotation(
			fmtMetaAnnotationId.format(annotationIdParts), 
			rsEpisode.getString("name"), episodeLayer.getId());
		     episode.setParentId(graph.getId());
		     graph.addAnnotation(episode);		     
		  }
		  rsEpisode.close();
		  sqlEpisode.close();
		  setStartEndLayers.add(layerId);
		  continue;
	       }
	       else if (layerId.equals("corpus"))
	       {
		  Layer corpusLayer = getLayer(layerId);
		  graph.addLayer(corpusLayer);
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
			corpusLayer.get("@layer_id"), rsCorpus.getString("corpus_id")};
		     Annotation corpus = new Annotation(
			fmtMetaAnnotationId.format(annotationIdParts), 
			rsCorpus.getString("corpus_name"), corpusLayer.getId());
		     corpus.setParentId(graph.getId());
		     graph.addAnnotation(corpus);		     
		  }
		  rsCorpus.close();
		  sqlCorpus.close();
		  setStartEndLayers.add(layerId);
		  continue;
	       }
	       
	       if (layerId.startsWith("transcript_"))
	       { // probably a transcript attribute layer
		  Layer attributeLayer = getLayer(layerId);
		  if ("transcript".equals(attributeLayer.get("@class_id")))
		  { // definitedly a transcript attribute layer
		     graph.addLayer(attributeLayer);
		     PreparedStatement sqlValue = getConnection().prepareStatement(
			"SELECT value FROM transcript_attribute"
			+" WHERE ag_id = ? AND name = ?");
		     sqlValue.setInt(1, iAgId);
		     sqlValue.setString(2, attributeLayer.get("@attribute").toString());
		     ResultSet rsValue = sqlValue.executeQuery();
		     if (rsValue.next())
		     {
			// add graph-tag annotation
			Object[] annotationIdParts = {
			   attributeLayer.get("@attribute"), new Integer(iAgId)};
			Annotation attribute = new Annotation(
			   fmtTranscriptAttributeId.format(annotationIdParts), 
			   rsValue.getString("value"), attributeLayer.getId());
			attribute.setParentId(graph.getId());
			graph.addAnnotation(attribute);
		     }
		     rsValue.close();
		     sqlValue.close();
		     setStartEndLayers.add(layerId);
		     continue;
		  } // definitely a transcript attribute layer
	       } // probably a transcript attribute layer
	       
	       if (layerId.startsWith("participant_"))
	       { // probably a transcript attribute layer
		  Layer attributeLayer = getLayer(layerId);
		  if ("speaker".equals(attributeLayer.get("@class_id")))
		  { // definitedly a participant attribute layer
		     graph.addLayer(attributeLayer);
		     PreparedStatement sqlValue = getConnection().prepareStatement(
			"SELECT a.speaker_number, a.value FROM speaker_attribute a"
			+" INNER JOIN transcript_speaker ts ON ts.speaker_number = a.speaker_number"
			+" WHERE ts.ag_id = ? AND a.name = ?");
		     sqlValue.setInt(1, iAgId);
		     sqlValue.setString(2, attributeLayer.get("@attribute").toString());
		     ResultSet rsValue = sqlValue.executeQuery();
		     while (rsValue.next())
		     {
			// add graph-tag annotation
			Object[] annotationIdParts = {
			   attributeLayer.get("@attribute"), new Integer(rsValue.getInt("speaker_number"))};
			Annotation attribute = new Annotation(
			   fmtParticipantAttributeId.format(annotationIdParts), 
			   rsValue.getString("value"), attributeLayer.getId());
			attribute.setParentId("m_-2_"+rsValue.getString("speaker_number"));
			graph.addAnnotation(attribute);
		     }
		     rsValue.close();
		     sqlValue.close();
		     setStartEndLayers.add(layerId);
		     continue;
		  } // definitely a transcript attribute layer
	       } // probably a transcript attribute layer
	       
	       Layer layer = getLayer(layerId);
	       graph.addLayer(layer);
	       int iLayerId = ((Integer)layer.get("@layer_id")).intValue();
	       String scope = (String)layer.get("@scope");
	       sqlAnnotation.setInt(1, iLayerId);
	       ResultSet rsAnnotation = sqlAnnotation.executeQuery();
	       while (rsAnnotation.next())
	       {
		  Annotation annotation = new Annotation();
		  Object[] annotationIdParts = {
		     scope.toLowerCase(), new Integer(iLayerId), 
		     new Long(rsAnnotation.getLong("annotation_id"))};
		  if (scope.equalsIgnoreCase(SqlConstants.SCOPE_FREEFORM)) annotationIdParts[0] = "";
		  annotation.setId(fmtAnnotationId.format(annotationIdParts));
		  String turnParentId = null;
		  if (iLayerId == SqlConstants.LAYER_TURN 
		      || iLayerId == SqlConstants.LAYER_UTTERANCE) // turn or utterance
		  { // convert speaker_number label into participant name
		     turnParentId = "m_-2_"+rsAnnotation.getString("label");
		     Annotation participant = graph.getAnnotation(turnParentId);
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
		  annotation.put("confidence", new Integer(rsAnnotation.getInt("label_status")));
		  annotation.setLayerId(layer.getId());

		  // parent:
		  if (iLayerId == SqlConstants.LAYER_SEGMENT) // segment
		  {
		     annotationIdParts[0] = SqlConstants.SCOPE_WORD;
		     annotationIdParts[1] = new Integer(SqlConstants.LAYER_TRANSCRIPTION); // transcript word
		     annotationIdParts[2] = new Long(rsAnnotation.getLong("word_annotation_id"));
		     annotation.setParentId(fmtAnnotationId.format(annotationIdParts));
		     annotation.setOrdinal(rsAnnotation.getInt("ordinal_in_word"));
		  }
		  else if (iLayerId == SqlConstants.LAYER_TRANSCRIPTION) // transcription word
		  {
		     annotationIdParts[0] = SqlConstants.SCOPE_META;
		     annotationIdParts[1] = new Integer(SqlConstants.LAYER_TURN); // turn
		     annotationIdParts[2] = new Long(rsAnnotation.getLong("turn_annotation_id"));
		     annotation.setParentId(fmtAnnotationId.format(annotationIdParts));
		     annotation.setOrdinal(rsAnnotation.getInt("ordinal_in_turn"));
		  }
		  else if (iLayerId == SqlConstants.LAYER_UTTERANCE) // utterance
		  {
		     annotationIdParts[0] = SqlConstants.SCOPE_META;
		     annotationIdParts[1] = new Integer(SqlConstants.LAYER_TURN); // turn
		     annotationIdParts[2] = new Long(rsAnnotation.getLong("turn_annotation_id"));
		     annotation.setParentId(fmtAnnotationId.format(annotationIdParts));
		  }
		  else if (iLayerId == SqlConstants.LAYER_TURN) // turn
		  {
		     annotation.setParentId(turnParentId);
		  }
		  else if (scope.equalsIgnoreCase(SqlConstants.SCOPE_SEGMENT)) // segment scope
		  {
		     annotationIdParts[0] = SqlConstants.SCOPE_SEGMENT;
		     annotationIdParts[1] = new Integer(SqlConstants.LAYER_SEGMENT); // segment
		     annotationIdParts[2] = new Long(rsAnnotation.getLong("segment_annotation_id"));
		     annotation.setParentId(fmtAnnotationId.format(annotationIdParts));
		  } // segment scope
		  else if (scope.equalsIgnoreCase(SqlConstants.SCOPE_WORD)) // word scope
		  {
		     annotationIdParts[0] = SqlConstants.SCOPE_WORD;
		     annotationIdParts[1] = new Integer(SqlConstants.LAYER_TRANSCRIPTION); // transcription word
		     annotationIdParts[2] = new Long(rsAnnotation.getLong("word_annotation_id"));
		     annotation.setParentId(fmtAnnotationId.format(annotationIdParts));
		  } // word scope
		  else if (scope.equalsIgnoreCase(SqlConstants.SCOPE_META)) // meta scope
		  {
		     annotationIdParts[0] = SqlConstants.SCOPE_META;
		     annotationIdParts[1] = new Integer(SqlConstants.LAYER_TURN); // turn
		     annotationIdParts[2] = new Long(rsAnnotation.getLong("turn_annotation_id"));
		     annotation.setParentId(fmtAnnotationId.format(annotationIdParts));
		  } // meta scope
		  else // freeform scope
		  {
		     annotation.setParentId(graph.getId());
		  } // freeform scope

		  // start anchor
		  Object[] anchorIdParts = { new Long(rsAnnotation.getLong("start_anchor_id"))};
		  annotation.setStartId(fmtAnchorId.format(anchorIdParts));
		  if (graph.getAnchor(annotation.getStartId()) == null)
		  { // start anchor isn't in graph yet
		     Anchor anchor = new Anchor(annotation.getStartId(), new Double(rsAnnotation.getDouble("start_offset")));
		     anchor.put("confidence", new Integer(rsAnnotation.getInt("start_alignment_status")));
		     graph.addAnchor(anchor);
		  } // start anchor isn't in graph yet 
		  
		  // end anchor
		  anchorIdParts[0] = new Long(rsAnnotation.getLong("end_anchor_id"));
		  annotation.setEndId(fmtAnchorId.format(anchorIdParts));
		  if (graph.getAnchor(annotation.getEndId()) == null)
		  { // start anchor isn't in graph yet
		     Anchor anchor = new Anchor(annotation.getEndId(), new Double(rsAnnotation.getDouble("end_offset")));
		     anchor.put("confidence", new Integer(rsAnnotation.getInt("end_alignment_status")));
		     graph.addAnchor(anchor);
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
	       for (Annotation a : graph.getAnnotations(layerId))
	       {
		  a.setStartId(firstAnchor.getId());
		  a.setEndId(lastAnchor.getId());
	       } // next annotation
	    } // next layer
	 } // there are anchors

	 return graph;
      }
      catch(SQLException exception)
      {
	 throw new StoreException(exception);
      }
   }

   /**
    * Saves the given graph. The graph can be partial e.g. include only some of the layers that the stored version of the graph contains.
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
      try
      {
	 // validate the graph before saving it
	 // TODO ensure all layers are loaded before validation
	 Validator v = new Validator();
	 //v.setDebug(true);
	 Vector<Change> validationChanges = v.transform(graph);
	 if (v.getErrors().size() != 0)
	 {
	    StringBuffer messages = new StringBuffer();
	    for (String s : v.getErrors())
	    {
	       messages.append(s);
	       messages.append("\n");
	       System.out.println(s);
	    }
	    System.out.println("Invalid graph (saving anyway): " + graph.getId() + "\n" + messages);
	 }

	 if (graph.getChange() == Change.Operation.Create)
	 {
	    // create the graph, to generate the ag_id
	    PreparedStatement sql = getConnection().prepareStatement(
	       "INSERT INTO transcript (transcript_id) VALUES (?)");
	    sql.setString(1, graph.getId());
	    sql.executeUpdate();
	    sql.close();
	    sql = getConnection().prepareStatement("SELECT LAST_INSERT_ID()");
	    ResultSet rs = sql.executeQuery();
	    rs.next();
	    graph.put("@ag_id", new Integer(rs.getInt(1)));
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
		  graph.put("@ag_id", new Integer(rs.getInt("ag_id")));
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
	       else if (change.getObject().getId().startsWith("t_"))
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
		     throw new StoreException("Could not parse special annotation ID:" + change.getObject().getId());
		  }
	       }
	       else if (change.getObject().getId().startsWith("p_"))
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
		     throw new StoreException("Could not parse special annotation ID:" + change.getObject().getId());
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
	    "INSERT INTO anchor (ag_id, offset, alignment_status) VALUES (?, ?, ?)");
	 sqlInsertAnchor.setInt(1, iAgId);
	 PreparedStatement sqlUpdateAnchor = getConnection().prepareStatement(
	    "UPDATE anchor SET offset = ?, alignment_status = ? WHERE anchor_id = ?");
	 PreparedStatement sqlCheckAnchor = getConnection().prepareStatement(
	    "SELECT COUNT(*) FROM annotation_layer_? WHERE start_anchor_id = ? OR end_anchor_id = ?");
	 // create a list of layers to check before deleting an anchor
	 PreparedStatement sqlLayers = getConnection().prepareStatement(
	    "SELECT layer_id FROM layer ORDER BY layer_id");
	 ResultSet rsLayers = sqlLayers.executeQuery();
	 HashSet<Integer> layerIds = new HashSet<Integer>();
	 while (rsLayers.next()) layerIds.add(new Integer(rsLayers.getInt("layer_id")));
	 rsLayers.close();
	 sqlLayers.close();
	 PreparedStatement sqlDeleteAnchor = getConnection().prepareStatement(
	    "DELETE FROM anchor WHERE anchor_id = ?");

	 PreparedStatement sqlInsertFreeformAnnotation = getConnection().prepareStatement(
	    "INSERT INTO annotation_layer_?"
	    + " (ag_id, label, label_status, start_anchor_id, end_anchor_id,"
	    + " parent_id, ordinal)"
	    + " VALUES (?, ?, ?, ?, ?, ?, ?)");
	 sqlInsertFreeformAnnotation.setInt(2, iAgId);
	 PreparedStatement sqlInsertMetaAnnotation = getConnection().prepareStatement(
	    "INSERT INTO annotation_layer_?"
	    + " (ag_id, label, label_status, start_anchor_id, end_anchor_id,"
	    + " parent_id, ordinal,"
	    + " turn_annotation_id)"
	    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
	 sqlInsertMetaAnnotation.setInt(2, iAgId);
	 PreparedStatement sqlInsertWordAnnotation = getConnection().prepareStatement(
	    "INSERT INTO annotation_layer_?"
	    + " (ag_id, label, label_status, start_anchor_id, end_anchor_id,"
	    + " parent_id, ordinal,"
	    + " turn_annotation_id,"
	    + " ordinal_in_turn, word_annotation_id)"
	    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
	 sqlInsertWordAnnotation.setInt(2, iAgId);
	 PreparedStatement sqlInsertSegmentAnnotation = getConnection().prepareStatement(
	    "INSERT INTO annotation_layer_?"
	    + " (ag_id, label, label_status, start_anchor_id, end_anchor_id,"
	    + " parent_id, ordinal,"
	    + " turn_annotation_id,"
	    + " ordinal_in_turn, word_annotation_id,"
	    + " ordinal_in_word, segment_annotation_id)"
	    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
	 sqlInsertSegmentAnnotation.setInt(2, iAgId);
	 PreparedStatement sqlUpdateFreeformAnnotation = getConnection().prepareStatement(
	    "UPDATE annotation_layer_?"
	    + " SET label = ?, label_status = ?, start_anchor_id = ?, end_anchor_id = ?,"
	    + " parent_id = ?, ordinal = ?"
	    + " WHERE annotation_id = ?");
	 PreparedStatement sqlUpdateMetaAnnotation = getConnection().prepareStatement(
	    "UPDATE annotation_layer_?"
	    + " SET label = ?, label_status = ?, start_anchor_id = ?, end_anchor_id = ?,"
	    + " turn_annotation_id = ?,"
	    + " parent_id = ?, ordinal = ?"
	    + " WHERE annotation_id = ?");
	 PreparedStatement sqlUpdateWordAnnotation = getConnection().prepareStatement(
	    "UPDATE annotation_layer_?"
	    + " SET label = ?, label_status = ?, start_anchor_id = ?, end_anchor_id = ?,"
	    + " turn_annotation_id = ?,"
	    + " ordinal_in_turn = ?, word_annotation_id = ?,"
	    + " parent_id = ?, ordinal = ?"
	    + " WHERE annotation_id = ?");
	 PreparedStatement sqlUpdateSegmentAnnotation = getConnection().prepareStatement(
	    "UPDATE annotation_layer_?"
	    + " SET label = ?, label_status = ?, start_anchor_id = ?,end_anchor_id = ?,"
	    + " turn_annotation_id = ?,"
	    + " ordinal_in_turn = ?, word_annotation_id = ?,"
	    + " ordinal_in_word = ?, segment_annotation_id = ?,"
	    + " parent_id = ?, ordinal = ?" 
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
	    "INSERT INTO transcript_attribute (ag_id, name, value) VALUES (?,?,?)");
	 sqlInsertTranscriptAttribute.setInt(1, iAgId);
	 PreparedStatement sqlUpdateTranscriptAttribute = getConnection().prepareStatement(
	    "UPDATE transcript_attribute SET value = ? WHERE ag_id = ? AND name = ?");
	 sqlUpdateTranscriptAttribute.setInt(2, iAgId);
	 PreparedStatement sqlDeleteTranscriptAttribute = getConnection().prepareStatement(
	    "DELETE FROM transcript_attribute WHERE ag_id = ? AND name = ?");
	 sqlDeleteTranscriptAttribute.setInt(1, iAgId);
	 PreparedStatement sqlInsertParticipantAttribute = getConnection().prepareStatement(
	    "INSERT INTO speaker_attribute (speaker_number, name, value) VALUES (?,?,?)");
	 PreparedStatement sqlUpdateParticipantAttribute = getConnection().prepareStatement(
	    "UPDATE speaker_attribute SET value = ? WHERE speaker_number = ? AND name = ?");
	 PreparedStatement sqlDeleteParticipantAttribute = getConnection().prepareStatement(
	    "DELETE FROM speaker_attribute WHERE speaker_number = ? AND name = ?");

	 try
	 {
	    // there's a change for each changed attribute of each object
	    // but we'll update the whole object when we get to the first change, 
	    // then skip subsequent change elements until the next object is encountered
	    lastObject = graph; // TODO save graph changes?
	    // it's also possible that some annotations will change on the way that were
	    // otherwise unchanged - e.g. as final anchor IDs are set, etc.
	    HashSet<Annotation> extraUpdates = new HashSet<Annotation>();
	    for (Change change : changes)
	    {
	       if (change.getObject() == lastObject) continue; // already did this object
	       lastObject = change.getObject();
	       
	       if (change.getObject() instanceof Anchor)
	       {
		  saveAnchorChanges((Anchor)change.getObject(), extraUpdates, 
				    sqlInsertAnchor, sqlLastId, sqlUpdateAnchor, 
				    sqlCheckAnchor, layerIds, sqlDeleteAnchor);
	       } // Anchor change
	       else if (change.getObject() instanceof Annotation
			&& !(change.getObject() instanceof Graph))
	       {
		  if (change.getObject().getId().startsWith("m_"))
		  { // special layer annotation
		     saveSpecialAnnotationChanges((Annotation)change.getObject());
		  }
		  else if (change.getObject().getId().startsWith("t_"))
		  { // transcript attribute
		     saveTranscriptAttributeChanges(
			(Annotation)change.getObject(), 
			sqlInsertTranscriptAttribute, 
			sqlUpdateTranscriptAttribute, 
			sqlDeleteTranscriptAttribute);
		  }
		  else if (change.getObject().getId().startsWith("p_"))
		  { // participant attribute
		     saveParticipantAttributeChanges(
			(Annotation)change.getObject(), 
			sqlInsertParticipantAttribute, 
			sqlUpdateParticipantAttribute, 
			sqlDeleteParticipantAttribute);
		  }
		  else
		  { // temporal annotation
		     saveAnnotationChanges(
			(Annotation)change.getObject(), extraUpdates, 
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
	 }
      }
      catch(ParseException shouldntBeThrown)
      {
	 throw new StoreException(shouldntBeThrown);
      }
      catch(SQLException exception)
      {
	 throw new StoreException(exception);
      }
      catch(TransformationException invalid)
      {
	 System.out.println(invalid.toString());
	 throw new StoreException("Graph was not valid", invalid);
      }
      return true;
   }
   
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
    * @throws SQLException If a database error occurs.
    * @throws ParseException Shouldn't be thrown, assuming annotation ids have been checked
    */
   protected void saveAnchorChanges(Anchor anchor, HashSet<Annotation> extraUpdates, PreparedStatement sqlInsertAnchor, PreparedStatement sqlLastId, PreparedStatement sqlUpdateAnchor, PreparedStatement sqlCheckAnchor, HashSet<Integer> layerIds, PreparedStatement sqlDeleteAnchor) throws SQLException, ParseException
   {
      if (!anchor.containsKey("confidence")
	  || (!(anchor.get("confidence") instanceof Integer)))
      {
	 anchor.put("confidence", new Integer(Constants.CONFIDENCE_UNKNOWN));
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
	    sqlInsertAnchor.setInt(3, ((Integer)anchor.get("confidence")).intValue());
	    sqlInsertAnchor.executeUpdate();
	    ResultSet rs = sqlLastId.executeQuery();
	    rs.next();
	    String oldId = anchor.getId();
	    Object[] anchorIdParts = { new Long(rs.getLong(1)) };
	    String newId = fmtAnchorId.format(anchorIdParts);
	    rs.close();
	    
	    // change anchor ID
	    anchor.getGraph().getAnchors().remove(oldId);
	    anchor.setId(newId);
	    anchor.getGraph().getAnchors().put(anchor.getId(), anchor);
	    
	    // update all annotations that use the anchor
	    for (Annotation startsHere : anchor.getStartingAnnotations())
	    {
	       // check it still uses this anchor
	       if (startsHere.getStartId().equals(oldId))
	       {
		  if (startsHere.getChange() == Change.Operation.NoChange
		      || startsHere.containsKey("@SqlUpdated"))
		  { // ensure the anchor change gets caught later
		     extraUpdates.add(startsHere);
		  }
		  startsHere.setStartId(anchor.getId());
	       }
	    } // next annotation
	    for (Annotation endsHere : anchor.getEndingAnnotations())
	    {
	       // check it still uses this anchor
	       if (endsHere.getEndId().equals(oldId))
	       {
		  if (endsHere.getChange() == Change.Operation.NoChange
		      || endsHere.containsKey("@SqlUpdated"))
		  { // ensure the anchor change gets caught later
		     extraUpdates.add(endsHere);
		  }
		  endsHere.setEndId(anchor.getId());
		  }
	    } // next annotation

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
	       sqlUpdateAnchor.setInt(2, ((Integer)anchor.get("confidence")).intValue());
	       sqlUpdateAnchor.setLong(3, anchorId);
	       sqlUpdateAnchor.executeUpdate();
	       
	    }
	    catch(ParseException exception)
	    {
	       System.out.println("Error parsing anchor ID for "+anchor.getId());
	       throw exception;
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
	       System.out.println("Error parsing anchor ID for "+anchor.getId());
	       throw exception;
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
    * @throws ParseException Shouldn't be thrown, assuming annotation ids have been checked
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
      HashMap<String,String> participantNameToNumber) throws SQLException, ParseException, PermissionException, StoreException
   {
      if (annotation.getId().startsWith("m_")) return; // ignore participant changes for now

      if (!annotation.containsKey("confidence")
	  || (!(annotation.get("confidence") instanceof Integer)))
      {
	 annotation.put("confidence", new Integer(Constants.CONFIDENCE_UNKNOWN));
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
	    sql.setInt(4, ((Integer)annotation.get("confidence")).intValue());
	    try
	    {
	       Object[] o = fmtAnchorId.parse(annotation.getStartId());
	       Long anchorId = (Long)o[0];
	       sql.setLong(5, anchorId);
	    }
	    catch(ParseException exception)
	    {
	       System.out.println("Error parsing start anchor for "+annotation.getId()+": " + annotation.getStartId());
	       throw exception;
	    }
	    try
	    {
	       Object[] o = fmtAnchorId.parse(annotation.getEndId());
	       Long anchorId = (Long)o[0];
	       sql.setLong(6, anchorId);
	    }
	    catch(ParseException exception)
	    {
	       System.out.println("Error parsing end anchor for "+annotation.getId()+": " + annotation.getEndId());
	       throw exception;
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
	       else
	       {
		  try
		  {
		     Object[] o = fmtAnnotationId.parse(annotation.getParentId());
		     sql.setLong(7, ((Long)o[2]).longValue());
		  }
		  catch(ParseException exception)
		  {
		     System.out.println("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId() + " on " + annotation.getLayerId());
		     throw exception;
		  }
	       }
	    }
	    sql.setInt(8, annotation.getOrdinal());

	    if (sql != sqlInsertFreeformAnnotation)
	    { // meta, word, or segment annotation
	       // must set turn_annotation_id too
	       if (layerId.intValue() == SqlConstants.LAYER_TURN)
	       {
		  // turn_annotation_id = annotation_id
		  sql.setNull(9, java.sql.Types.INTEGER); // turn_annotation_id - set it later...
	       }
	       else if (sql == sqlInsertMetaAnnotation)
	       {
		  // turn is annotation.parent
		  try
		  {
		     Object[] o = fmtAnnotationId.parse(annotation.getParentId());
		     sql.setLong(9, ((Long)o[2]).longValue()); // turn_annotation_id
		  }
		  catch(ParseException exception)
		  {
		     System.out.println("Error parsing turn id for "+annotation.getId()+": " + annotation.getParentId());
		     throw exception;
		  }
	       }
	       else
	       { // word or segment annotation
		  if (layerId.intValue() == SqlConstants.LAYER_TRANSCRIPTION)
		  {
		     try
		     {
			Object[] o = fmtAnnotationId.parse(annotation.getParentId());
			sql.setLong(9, ((Long)o[2]).longValue()); // turn_annotation_id
		     }
		     catch(ParseException exception)
		     {
			System.out.println("Error parsing turn id for "+annotation.getId()+": " + annotation.getParentId());
			throw exception;
		     }
		     sql.setInt(10, annotation.getOrdinal()); // ordinal_in_turn
		     sql.setNull(11, java.sql.Types.INTEGER); // word_annotation_id - set it later
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
			   sql.setLong(9, rs.getLong("turn_annotation_id"));
			   sql.setInt(10, rs.getInt("ordinal_in_turn"));
			   rs.close();
			   sql.setLong(11, wordAnnotationId); // word_annotation_id
			}
			catch(ParseException exception)
			{
			   System.out.println("Error parsing word ID for "+annotation.getId()+": " + annotation.getParentId());
			   throw exception;
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
			      sql.setLong(9, rs.getLong("turn_annotation_id"));
			      sql.setInt(10, rs.getInt("ordinal_in_turn"));
			      rs.close();
			      sql.setLong(11, wordAnnotationId); // word_annotation_id
			      sql.setInt(12, annotation.getOrdinal()); // ordinal_in_word
			      sql.setNull(13, java.sql.Types.INTEGER); // segment_annotation_id - set it later
			   }
			   catch(ParseException exception)
			   {
			      System.out.println("Error parsing word ID for "+annotation.getId()+": " + annotation.getParentId());
			      throw exception;
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
			      sql.setLong(9, rs.getLong("turn_annotation_id"));
			      sql.setInt(10, rs.getInt("ordinal_in_turn"));
			      rs.close();
			      sql.setLong(11, wordAnnotationId); // word_annotation_id
			      
			   }
			   catch(ParseException exception)
			   {
			      System.out.println("Error parsing word ID for segment "+annotation.getId()+": " + annotation.getParent().getParentId());
			      throw exception;
			   }
			   try
			   {
			      Object[] o = fmtAnnotationId.parse(annotation.getParentId());
			      Long segmentAnnotationId = ((Long)o[2]).longValue();
			      sqlSelectSegmentFields.setLong(1, segmentAnnotationId); // segment_annotation_id
			      ResultSet rs = sqlSelectSegmentFields.executeQuery();
			      rs.next();
			      sql.setInt(12, rs.getInt("ordinal_in_word")); // ordinal_in_word
			      sql.setLong(13, segmentAnnotationId); // segment_annotation_id
			      rs.close();
			   }
			   catch(ParseException exception)
			   {
			      System.out.println("Error parsing segment ID for "+annotation.getId()+": " + annotation.getParentId());
			      throw exception;
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
	    Object[] annotationIdParts = { scope, layerId, new Long(annotationId) };
	    String newId = fmtAnnotationId.format(annotationIdParts);
	    rs.close();
	    
	    // change annotation ID
	    annotation.getGraph().getAnnotationsById().remove(oldId);
	    annotation.setId(newId);
	    annotation.getGraph().getAnnotationsById().put(annotation.getId(), annotation);
	    
	    // update all child parentIds
	    for (Vector<Annotation> children : annotation.getAnnotations().values())
	    {
	       for (Annotation child : children)
	       {
		  // check it still uses this anchor
		  if (child.getParentId().equals(oldId))
		  {
		     if (child.getChange() == Change.Operation.NoChange
			 || child.containsKey("@SqlUpdated"))
		     { // ensure the anchor change gets caught later
			extraUpdates.add(child);
		     }
		     child.setParentId(annotation.getId());
		  }
	       } // next child
	    } // next child layer

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
	       System.out.println("Error parsing ID for "+annotation.getId());
	       throw exception;
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
	    sql.setInt(3, ((Integer)annotation.get("confidence")).intValue());
	    try
	    {
	       Object[] o = fmtAnchorId.parse(annotation.getStartId());
	       Long anchorId = (Long)o[0];
	       sql.setLong(4, anchorId);
	    }
	    catch(ParseException exception)
	    {
	       System.out.println("Error parsing start anchor for "+annotation.getId()+": " + annotation.getStartId());
	       throw exception;
	    }
	    try
	    {
	       Object[] o = fmtAnchorId.parse(annotation.getEndId());
	       Long anchorId = (Long)o[0];
	       sql.setLong(5, anchorId);
	    }
	    catch(ParseException exception)
	    {
	       System.out.println("Error parsing end anchor for "+annotation.getId()+": " + annotation.getEndId());
	       throw exception;
	    }
	    if (sql == sqlUpdateFreeformAnnotation)
	    {
	       if (annotation.getParentId() == null)
	       {
		  sql.setNull(6, java.sql.Types.INTEGER);
	       }
	       else
	       {
		  try
		  {
		     Object[] o = fmtAnnotationId.parse(annotation.getParentId());
		     sql.setLong(6, ((Long)o[2]).longValue());
		  }
		  catch(ParseException exception)
		  {
		     System.out.println("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId());
		     throw exception;
		  }
	       }
	       sql.setInt(7, annotation.getOrdinal());
	       sql.setLong(8, annotationId);
	    }
	    else
	    { // meta, word, or segment annotation
	       // must set turn_annotation_id too
	       if (layerId.intValue() == SqlConstants.LAYER_TURN)
	       {
		  // turn_annotation_id = annotation_id
		  sql.setLong(6, annotationId); // turn_annotation_id
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
			System.out.println("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId());
			throw exception;
		     }
		  }
		  sql.setInt(8, annotation.getOrdinal());
		  // annotation_id
		  sql.setLong(9, annotationId);
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
		     System.out.println("Error parsing turn ID for "+annotation.getId()+": " + annotation.getParentId());
		     throw exception;
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
			System.out.println("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId());
			throw exception;
		     }
		  }
		  sql.setInt(8, annotation.getOrdinal());
		  // annotation_id
		  sql.setLong(9, annotationId);
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
			System.out.println("Error parsing turn ID for "+annotation.getId()+": " + annotation.getParentId());
			throw exception;
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
			   System.out.println("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId());
			   throw exception;
			}
		     }
		     sql.setInt(10, annotation.getOrdinal());
		     sql.setLong(11, annotationId); // annotation_id		     
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
				 System.out.println("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId());
				 throw exception;
			      }
			   }
			   sql.setInt(10, annotation.getOrdinal());
			   sql.setLong(11, annotationId); // annotation_id
			   rs.close();
			}
			catch(ParseException exception)
			{
			   System.out.println("Error parsing word ID for "+annotation.getId()+": " + annotation.getParentId());
			   throw exception;
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
				    System.out.println("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId());
				    throw exception;
				 }
			      }
			      sql.setInt(12, annotation.getOrdinal());
			      sql.setLong(13, annotationId); // annotation_id
			   }
			   catch(ParseException exception)
			   {
			      System.out.println("Error parsing word ID for "+annotation.getId()+": " + annotation.getParentId());
			      throw exception;
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
			      System.out.println("Error parsing word ID for parent "+annotation.getId()+": " + annotation.getParent().getParentId());
			      throw exception;
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
			      System.out.println("Error parsing segment ID for "+annotation.getId()+": " + annotation.getParentId());
			      throw exception;
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
				 System.out.println("Error parsing parent id for "+annotation.getId()+": " + annotation.getParentId());
				 throw exception;
			      }
			   }
			   sql.setInt(12, annotation.getOrdinal());
			   sql.setLong(13, annotationId); // annotation_id
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
	    // deduce the database anchor.anchor_id from the object anchor.id
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
	       System.out.println("Error parsing ID for "+annotation.getId());
	       throw exception;
	    }
	    break;
	 } // Destroy
      } // switch on change type

      annotation.put("@SqlUpdated", Boolean.TRUE); // flag the anchor as having been updated
   } // end of saveAnchorChanges()

   
   /**
    * Save changes to a 'special' annotation, e.g. corpus, series, etc.
    * @param annotation The annotation whose changes should be saved.
    * @throws SQLException If a database error occurs.
    * @throws ParseException Shouldn't be thrown, assuming annotation ids have been checked
    */
   public void saveSpecialAnnotationChanges(Annotation annotation)
    throws SQLException, ParseException
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
		  PreparedStatement sql = getConnection().prepareStatement(
		     "SELECT family_id FROM transcript_family WHERE name = ?");
		  sql.setString(1, annotation.getLabel());
		  ResultSet rs = sql.executeQuery();
		  if (rs.next())
		  {
		     int seriesId = rs.getInt("family_id");
		     int agId = ((Integer)annotation.getGraph().get("@ag_id")).intValue();
		     PreparedStatement sqlUpdate = getConnection().prepareStatement(
			"UPDATE transcript SET family_id = ? WHERE ag_id = ?");
		     sqlUpdate.setInt(1, seriesId);
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
	 else if (annotation.getLayerId().equals("main_participant"))
	 {
	    int agId = ((Integer)annotation.getGraph().get("@ag_id")).intValue();
	    Object[] o = fmtMetaAnnotationId.parse(annotation.getParentId());
	    int speakerNumber = Integer.parseInt(o[1].toString());
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
	 else if (annotation.getLayerId().equals("participant"))
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
		     sqlInsert.executeUpdate();
		     sqlInsert.close();
		     sqlInsert = getConnection().prepareStatement("SELECT LAST_INSERT_ID()");
		     ResultSet rsInsert = sqlInsert.executeQuery();
		     rsInsert.next();
		     speakerNumber = rsInsert.getInt(1);
		     rsInsert.close();
		     sqlInsert.close();
		  }
		  rs.close();
		  sql.close();
		  annotation.put("@speaker_number", new Integer(speakerNumber));

		  // add the speaker to transcript_speaker
		  sql = getConnection().prepareStatement(
		     "INSERT INTO transcript_speaker (speaker_number, ag_id, name) VALUES (?,?,?)");
		  sql.setInt(1, speakerNumber);
		  sql.setInt(2, agId);
		  sql.setString(3, annotation.getLabel());
		  sql.executeUpdate();
		  sql.close();
		  break;
	       } // Create
	       case Update:
	       {
		  Object[] o = fmtTranscriptAttributeId.parse(annotation.getId());
		  int speakerNumber = Integer.parseInt(o[1].toString());
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
		  Object[] o = fmtTranscriptAttributeId.parse(annotation.getId());
		  int speakerNumber = Integer.parseInt(o[1].toString());
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
	 System.out.println("Error parsing ID for special attribute: "+annotation.getId());
	 throw exception;
      }
   } // end of saveSpecialAnnotationChanges()

   
   /**
    * Saves changes to a transcript attribute annotation.
    * @param annotation The annotation whose changes should be saved.
    * @param sqlInsertTranscriptAttribute Prepared statement for inserting an attribute value.
    * @param sqlUpdateTranscriptAttribute Prepared statement for updating an attribute value.
    * @param sqlDeleteTranscriptAttribute Prepared statement for deleting an attribute value.
    * @throws SQLException If a database error occurs.
    * @throws ParseException Shouldn't be thrown, assuming annotation ids have been checked
    */
   public void saveTranscriptAttributeChanges(Annotation annotation, PreparedStatement sqlInsertTranscriptAttribute, PreparedStatement sqlUpdateTranscriptAttribute, PreparedStatement sqlDeleteTranscriptAttribute) throws SQLException, ParseException
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
	       sqlInsertTranscriptAttribute.executeUpdate();
	       break;
	    } // Create
	    case Update:
	    {
	       Object[] o = fmtTranscriptAttributeId.parse(annotation.getId());
	       String attribute = o[0].toString();
	       sqlUpdateTranscriptAttribute.setString(1, annotation.getLabel());	    
	       sqlUpdateTranscriptAttribute.setString(3, attribute);
	       sqlUpdateTranscriptAttribute.executeUpdate();
	       break;
	    }
	    case Destroy:
	    {
	       Object[] o = fmtTranscriptAttributeId.parse(annotation.getId());
	       String attribute = o[0].toString();
	       sqlDeleteTranscriptAttribute.setString(2, attribute);
	       sqlDeleteTranscriptAttribute.executeUpdate();
	       break;
	    } // Destroy
	 } // switch on change type
	 
	 annotation.put("@SqlUpdated", Boolean.TRUE); // flag the anchor as having been updated
      }
      catch(ParseException exception)
      {
	 System.out.println("Error parsing ID for transcript attribute: "+annotation.getId());
	 throw exception;
      }
   } // end of saveTranscriptAttributeChanges()

   /**
    * Saves changes to a transcript attribute annotation.
    * @param annotation The annotation whose changes should be saved.
    * @param sqlInsertParticipantAttribute Prepared statement for inserting an attribute value.
    * @param sqlUpdateParticipantAttribute Prepared statement for updating an attribute value.
    * @param sqlDeleteParticipantAttribute Prepared statement for deleting an attribute value.
    * @throws SQLException If a database error occurs.
    * @throws ParseException Shouldn't be thrown, assuming annotation ids have been checked
    */
   public void saveParticipantAttributeChanges(Annotation annotation, PreparedStatement sqlInsertParticipantAttribute, PreparedStatement sqlUpdateParticipantAttribute, PreparedStatement sqlDeleteParticipantAttribute) throws SQLException, ParseException
   {
      try
      {
	 switch (annotation.getChange())
	 {
	    case Create:
	    {
	       String attribute = annotation.getLayerId().substring("participant_".length());
	       Object[] o = fmtMetaAnnotationId.parse(annotation.getParentId());
	       int speakerNumber = Integer.parseInt(o[1].toString());
	       sqlInsertParticipantAttribute.setInt(1, speakerNumber);
	       sqlInsertParticipantAttribute.setString(2, attribute);
	       sqlInsertParticipantAttribute.setString(3, annotation.getLabel());
	       sqlInsertParticipantAttribute.executeUpdate();
	       break;
	    } // Create
	    case Update:
	    {
	       Object[] o = fmtParticipantAttributeId.parse(annotation.getId());
	       String attribute = o[0].toString();
	       int speakerNumber = ((Long)o[1]).intValue();
	       sqlUpdateParticipantAttribute.setString(1, annotation.getLabel());	    
	       sqlUpdateParticipantAttribute.setInt(2, speakerNumber);
	       sqlUpdateParticipantAttribute.setString(3, attribute);
	       sqlUpdateParticipantAttribute.executeUpdate();
	       break;
	    }
	    case Destroy:
	    {
	       Object[] o = fmtParticipantAttributeId.parse(annotation.getId());
	       String attribute = o[0].toString();
	       int speakerNumber = ((Long)o[1]).intValue();
	       sqlUpdateParticipantAttribute.setInt(1, speakerNumber);
	       sqlDeleteParticipantAttribute.setString(2, attribute);
	       sqlDeleteParticipantAttribute.executeUpdate();
	       break;
	    } // Destroy
	 } // switch on change type
	 
	 annotation.put("@SqlUpdated", Boolean.TRUE); // flag the anchor as having been updated
      }
      catch(ParseException exception)
      {
	 System.out.println("Error parsing ID for transcript attribute: "+annotation.getId());
	 throw exception;
      }
   } // end of saveParticipantAttributeChanges()

} // end of class SqlGraphStore
