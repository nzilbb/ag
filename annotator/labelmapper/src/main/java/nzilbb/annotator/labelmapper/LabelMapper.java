//
// Copyright 2020-2021 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.annotator.labelmapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesGraphStore;
import nzilbb.ag.automation.UsesRelationalDatabase;
import nzilbb.editpath.*;
import nzilbb.encoding.comparator.*;
import nzilbb.sql.ConnectionFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * This annotator creates a mapping between pairs of layers, by finding the minimum edit
 * path between them. 
 * <p> It supports a 'sub-mapping' for alignment comparisons, where a pair of word layers
 * are mapped together, and then a pair of phone layers are mapped together. 
 * <p> For sub-mappings, the edit paths from the 'label' to 'token' layers are stored in
 * the relational database, with labels, edit distances, and offsets, which can then be
 * accessed in order to compare alignments.
 * @author Robert Fromont robert@fromont.net.nz
 */
@UsesRelationalDatabase @UsesGraphStore
public class LabelMapper extends Annotator {

  /** Get the minimum version of the nzilbb.ag API supported by the serializer.*/
  public String getMinimumApiVersion() { return "1.0.5"; }
  
  /** Layer ID for the layer to chunk labels and tokens. */
  protected String scopeLayerId;
  
  /**
   * Layer ID for the source of labels.
   * @see #getSourceLayerId()
   * @see #setSourceLayerId(String)
   */
  protected String sourceLayerId;
  /**
   * Getter for {@link #sourceLayerId}: Layer ID for the source of labels.
   * @return Layer ID for the source of labels.
   */
  public String getSourceLayerId() { return sourceLayerId; }
  /**
   * Setter for {@link #sourceLayerId}: Layer ID for the source of labels.
   * @param newSourceLayerId Layer ID for the source of labels.
   */
  public LabelMapper setSourceLayerId(String newSourceLayerId) { sourceLayerId = newSourceLayerId; return this; }
  
  /**
   * Whether/how to split labels of annotations on the label layer. Valid values are:
   * <dl>
   *  <dt>""</dt>      <dd> Use all annotations in the scope, and do not split their labels. 
   *                        (e.g. phone alignments to other phone alignments) </dd>
   *  <dt>"char"</dt>  <dd> Use the first annotation in the scope, split its label into
   *                        characters, and map each to labels on the token layer. 
   *                        (e.g. DISC word transcriptions to phones) </dd> 
   *  <dt>"space"</dt> <dd> Use the first annotation in the scope, split its label on
   *                        spaces, and map each to labels on the token layer. 
   *                        (e.g. ARPAbet word transcriptions to phones) </dd>  
   * </dl>
   * @see #getSplitLabels()
   * @see #setSplitLabels(String)
   */
  protected String splitLabels = "";
  /**
   * Getter for {@link #splitLabels}: Whether/how to split labels of annotations on the
   * label layer. 
   * @return Whether/how to split labels of annotations on the label layer.
   */
  public String getSplitLabels() { return splitLabels; }
  /**
   * Setter for {@link #splitLabels}: Whether/how to split labels of annotations on the
   * label layer. 
   * @param newSplitLabels Whether/how to split labels of annotations on the label layer.
   */
  public LabelMapper setSplitLabels(String newSplitLabels) { splitLabels = newSplitLabels; return this; }
  
  /**
   * Layer ID for the output layer.
   * @see #getMappingLayerId()
   * @see #setMappingLayerId(String)
   */
  protected String mappingLayerId;
  /**
   * Getter for {@link #mappingLayerId}: Layer ID for the output layer.
   * @return Layer ID for the output layer.
   */
  public String getMappingLayerId() { return mappingLayerId; }
  /**
   * Setter for {@link #mappingLayerId}: Layer ID for the output layer.
   * @param newMappingLayerId Layer ID for the output layer.
   */
  public LabelMapper setMappingLayerId(String newMappingLayerId) { mappingLayerId = newMappingLayerId; return this; }

  /**
   * Layer ID of tokens to tag.
   * @see #getTargetLayerId()
   * @see #setTargetLayerId(String)
   */
  protected String targetLayerId;
  /**
   * Getter for {@link #targetLayerId}: Layer ID of tokens to tag.
   * @return Layer ID of tokens to tag.
   */
  public String getTargetLayerId() { return targetLayerId; }
  /**
   * Setter for {@link #targetLayerId}: Layer ID of tokens to tag.
   * @param newTargetLayerId Layer ID of tokens to tag.
   */
  public LabelMapper setTargetLayerId(String newTargetLayerId) { targetLayerId = newTargetLayerId; return this; }

  /**
   * How to compare the label and token layers. Valid options are "CharacterToCharacter",
   * "OrthographyToDISC", "OrthographyToArpabet", or "DISCToDISC". 
   * @see #getComparator()
   * @see #setComparator(String)
   */
  protected String comparator;
  /**
   * Getter for {@link #comparator}: How to compare the label and token layers. Valid
   * options are "CharacterToCharacter", "OrthographyToDISC", "OrthographyToArpabet", or
   * "DISCToDISC". 
   * @return How to compare the label and token layers. Valid options are
   * "CharacterToCharacter", "OrthographyToDISC", "OrthographyToArpabet", or "DISCToDISC".
   */
  public String getComparator() { return comparator; }
  /**
   * Setter for {@link #comparator}: How to compare the label and token layers. Valid options 
   * are "CharacterToCharacter", "OrthographyToDISC", "OrthographyToArpabet", or "DISCToDISC". 
   * @param newComparator How to compare the label and token layers. Valid options are 
   * "CharacterToCharacter", "OrthographyToDISC", "OrthographyToArpabet", or "DISCToDISC".
   */
  public LabelMapper setComparator(String newComparator) { comparator = newComparator; return this; }
  
  /**
   * Label layer for sub-mapping.
   * <p> A sub-mapping is available when {@link #sourceLayerId} and {@link #targetLayerId}
   * are two word token layers, {@link #comparator} is set to "CharacterToCharacter", and 
   * {@link #splitLabels} is set to "". In this case, a secondary mapping can be done
   * between phones of a word token on {@link #sourceLayerId} and phones of the
   * corresponding word on {@link #targetLayerId}.
   * <ul>
   *  <li> Words on {@link #sourceLayerId} have phones on {@link #subSourceLayerId}. </li>
   *  <li> Words on {@link #targetLayerId} have phones on {@link #subTargetLayerId}. </li>
   * </ul>
   * @see #getSubSourceLayerId()
   * @see #setSubSourceLayerId(String)
   */
  protected String subSourceLayerId;
  /**
   * Getter for {@link #subSourceLayerId}: Label layer for sub-mapping.
   * @return Label layer for sub-mapping.
   */
  public String getSubSourceLayerId() { return subSourceLayerId; }
  /**
   * Setter for {@link #subSourceLayerId}: Label layer for sub-mapping.
   * @param newSubSourceLayerId Label layer for sub-mapping.
   */
  public LabelMapper setSubSourceLayerId(String newSubSourceLayerId) { subSourceLayerId = newSubSourceLayerId; return this; }
  
  /**
   * Token layer for sub-mapping.
   * <p> A sub-mapping is available when {@link #sourceLayerId} and {@link #targetLayerId}
   * are two word token layers, {@link #comparator} is set to "CharacterToCharacter", and 
   * {@link #splitLabels} is set to "". In this case, a secondary mapping can be done
   * between phones of a word token on {@link #sourceLayerId} and phones of the
   * corresponding word on {@link #targetLayerId}.
   * <ul>
   *  <li> Words on {@link #sourceLayerId} have phones on {@link #subSourceLayerId}. </li>
   *  <li> Words on {@link #targetLayerId} have phones on {@link #subTargetLayerId}. </li>
   * </ul>
   * @see #getSubTargetLayerId()
   * @see #setSubTargetLayerId(String)
   */
  protected String subTargetLayerId;
  /**
   * Getter for {@link #subTargetLayerId}: Token layer for sub-mapping.
   * @return Token layer for sub-mapping.
   */
  public String getSubTargetLayerId() { return subTargetLayerId; }
  /**
   * Setter for {@link #subTargetLayerId}: Token layer for sub-mapping.
   * @param newSubTargetLayerId Token layer for sub-mapping.
   */
  public LabelMapper setSubTargetLayerId(String newSubTargetLayerId) { subTargetLayerId = newSubTargetLayerId; return this; }
  
  /**
   * Output mapping layer for sub-mapping.
   * <p> A sub-mapping is available when {@link #sourceLayerId} and {@link #targetLayerId}
   * are two word token layers, {@link #comparator} is set to "CharacterToCharacter", and 
   * {@link #splitLabels} is set to "". In this case, a secondary mapping can be done
   * between phones of a word token on {@link #sourceLayerId} and phones of the
   * corresponding word on {@link #targetLayerId}.
   * <ul>
   *  <li> Words on {@link #sourceLayerId} have phones on {@link #subSourceLayerId}. </li>
   *  <li> Words on {@link #targetLayerId} have phones on {@link #subTargetLayerId}. </li>
   * </ul>
   * @see #getSubMappingLayerId()
   * @see #setSubMappingLayerId(String)
   */
  protected String subMappingLayerId;
  /**
   * Getter for {@link #subMappingLayerId}: Output mapping layer for sub-mapping.
   * @return Output mapping layer for sub-mapping.
   */
  public String getSubMappingLayerId() { return subMappingLayerId; }
  /**
   * Setter for {@link #subMappingLayerId}: Output mapping layer for sub-mapping.
   * @param newSubMappingLayerId Output mapping layer for sub-mapping.
   */
  public LabelMapper setSubMappingLayerId(String newSubMappingLayerId) { subMappingLayerId = newSubMappingLayerId; return this; }
  
  /**
   * How to compare the label and token layers in the sub-mapping. 
   * <p> A sub-mapping is available when {@link #sourceLayerId} and {@link #targetLayerId}
   * are two word token layers, {@link #comparator} is set to "CharacterToCharacter", and 
   * {@link #splitLabels} is set to "". In this case, a secondary mapping can be done
   * between phones of a word token on {@link #sourceLayerId} and phones of the
   * corresponding word on {@link #targetLayerId}.
   * <ul>
   *  <li> Words on {@link #sourceLayerId} have phones on {@link #subSourceLayerId}. </li>
   *  <li> Words on {@link #targetLayerId} have phones on {@link #subTargetLayerId}. </li>
   * </ul>
   * Valid options are "CharacterToCharacter", "OrthographyToDISC",
   * "OrthographyToArpabet", or "DISCToDISC".
   * @see #getSubComparator()
   * @see #setSubComparator(String)
   */
  protected String subComparator;
  /**
   * Getter for {@link #subComparator}: How to compare the label and token layers in the
   * sub-mapping. Valid options are "CharacterToCharacter", "OrthographyToDISC",
   * "OrthographyToArpabet", or "DISCToDISC". 
   * @return How to compare the label and token layers. Valid options are
   * "CharacterToCharacter", "OrthographyToDISC", "OrthographyToArpabet", or "DISCToDISC".
   */
  public String getSubComparator() { return subComparator; }
  /**
   * Setter for {@link #subComparator}: How to compare the label and token layers in the
   * sub-mapping. Valid options  are "CharacterToCharacter", "OrthographyToDISC",
   * "OrthographyToArpabet", or "DISCToDISC". 
   * @param newSubComparator How to compare the label and token layers. Valid options are 
   * "CharacterToCharacter", "OrthographyToDISC", "OrthographyToArpabet", or "DISCToDISC".
   */
  public LabelMapper setSubComparator(String newSubComparator) { subComparator = newSubComparator; return this; }
  
  /**
   * {@link UsesRelationalDatabase} method that sets the information required for
   * connecting to the relational database. 
   * <p> This override ensures that the database schema has been created
   * @param db Connection factory for getting new database connections.
   * @throws SQLException If the annotator can't connect to the given database.
   */
  @Override
  public void setRdbConnectionFactory(ConnectionFactory db) throws SQLException {
    super.setRdbConnectionFactory(db);
    
    // get DB connection
    Connection rdb = newConnection();
    
    try {
      
      // check the schema has been created
      try { // either of prepareStatement or executeQuery may fail if the table doesn't exist
        PreparedStatement sql = rdb.prepareStatement(
          sqlx.apply("SELECT * FROM "+getAnnotatorId()+"_mapping LIMIT 1"));
        try {
          ResultSet rsCheck = sql.executeQuery();
          rsCheck.close();
        } finally {
          sql.close();
        }
      } catch(SQLException exception) {
        
        PreparedStatement sql = rdb.prepareStatement(
          sqlx.apply(
            "CREATE TABLE "+getAnnotatorId()+"_mapping ("
            +" transcript VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL"
            +" COMMENT 'Transcript ID',"
            +" scope VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL"
            +" COMMENT 'Utterance/Word ID',"
            +" step INTEGER NOT NULL"
            +" COMMENT 'The edit step index in the sequence',"
            +" sourceLayer VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL"
            +" COMMENT 'Layer of the source annotations',"
            +" sourceParentId VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
            +" COMMENT 'ID of the parent of source annotation, if this is a sub-mapping',"
            +" sourceParentLabel VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
            +" COMMENT 'Label of the parent of source annotation, if this is a sub-mapping',"
            +" sourceId VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
            +" COMMENT 'ID of the source annotation',"
            +" sourceLabel VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
            +" COMMENT 'Label of the source annotation',"
            +" sourceStart DOUBLE"
            +" COMMENT 'Start offset of the source annotation',"
            +" sourceEnd DOUBLE"
            +" COMMENT 'End offset of the source annotation',"
            +" targetLayer VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL"
            +" COMMENT 'Layer of the target annotations',"
            +" targetParentId VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
            +" COMMENT 'ID of the parent of target annotation, if this is a sub-mapping',"
            +" targetParentLabel VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
            +" COMMENT 'Label of the parent of target annotation, if this is a sub-mapping',"
            +" targetId VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
            +" COMMENT 'ID of the target annotation',"
            +" targetLabel VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
            +" COMMENT 'Label of the target annotation',"
            +" targetStart DOUBLE"
            +" COMMENT 'Start offset of the target annotation',"
            +" targetEnd DOUBLE"
            +" COMMENT 'End offset of the target annotation',"
            +" operation CHAR(1) NOT NULL"
            +" COMMENT 'The edit operation: + for insert, - for delete, ! for change, = for no change',"
            +" distance INTEGER NOT NULL"
            +" COMMENT 'Distance (cost) for this edit step',"
            +" hierarchy VARCHAR(6)"
            +" COMMENT 'This mappings position in the sub-mapping hierarchy: parent, child, or none',"
            +" overlapRate DOUBLE"
            +" COMMENT 'As per Paulo and Oliveira (2004), 0 means no overlap at all, 1 means they complete overlap',"
            +" PRIMARY KEY (transcript,scope,sourceLayer,targetLayer,step)"
            +") ENGINE=MyISAM"));
        sql.executeUpdate();
        sql.close();
      }
    } finally {
      try { rdb.close(); } catch(SQLException x) {}
    }      
  }
  
  /**
   * Runs any processing required to uninstall the annotator.
   * <p> In this case, the table created in rdbConnectionFactory() is DROPped.
   */
  @Override
  public void uninstall() {
    try {
      Connection rdb = newConnection();      
      try {
        
        // check the schema has been created
        PreparedStatement sql = rdb.prepareStatement(
          sqlx.apply("DROP TABLE "+getAnnotatorId()+"_mapping"));
        sql.executeUpdate();
        sql.close();
        
      } finally {
        try { rdb.close(); } catch(SQLException x) {}
      }      
    } catch (SQLException x) {
    }
  }
   
  /**
   * Sets the configuration for a given annotation task.
   * @param parameters The configuration of the annotator; a value of <tt> null </tt>
   * will apply the default task parameters, with {@link #sourceLayerId} set to the
   * {@link Schema#wordLayerId} and {@link #stemLayerId} set to <q>stem</q>.
   * @throws InvalidConfigurationException
   */
  public void setTaskParameters(String parameters) throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");
      
    if (parameters == null) { // there is no possible default parameter
      throw new InvalidConfigurationException(this, "Parameters not set.");         
    }

    // set basic attributes
    beanPropertiesFromQueryString(parameters);

    // validate parameters
    if (sourceLayerId == null || sourceLayerId.length() == 0)
      throw new InvalidConfigurationException(this, "No label layer set.");
    Layer sourceLayer = schema.getLayer(sourceLayerId);
    if (sourceLayer == null) 
      throw new InvalidConfigurationException(this, "Invalid label layer: " + sourceLayerId);
    if (targetLayerId == null || targetLayerId.length() == 0)
      throw new InvalidConfigurationException(this, "No token layer set.");
    Layer targetLayer = schema.getLayer(targetLayerId);
    if (targetLayer == null) 
      throw new InvalidConfigurationException(this, "Invalid token layer: " + targetLayerId);
    if (comparator == null || comparator.length() == 0)
      throw new InvalidConfigurationException(this, "Comparator not set.");
    if (splitLabels == null)
      throw new InvalidConfigurationException(this, "Split Labels setting not set.");
    if (splitLabels.length() > 0 && !splitLabels.equals("char") && !splitLabels.equals("space"))
      throw new InvalidConfigurationException(
        this, "Invalid value for Split Labels: \"" + splitLabels
        + "\" - must be \"\", \"char\", or \"space\"");
    if (mappingLayerId != null && mappingLayerId.length() == 0) mappingLayerId = null;
    if (subSourceLayerId != null && subSourceLayerId.length() == 0) subSourceLayerId = null;
    if (subTargetLayerId != null && subTargetLayerId.length() == 0) subTargetLayerId = null;
    if (subMappingLayerId != null && subMappingLayerId.length() == 0) subMappingLayerId = null;
    if (subComparator != null && subComparator.length() == 0) subComparator = null;
    if (mappingLayerId == null // mapping layer must be set
        && (subSourceLayerId == null || subTargetLayerId == null)) { // unless it's a sub-mapping
      throw new InvalidConfigurationException(this, "No mapping layer set.");
    }
    // layers must all be distinct
    if (sourceLayerId.equals(targetLayerId)) {
      throw new InvalidConfigurationException(
        this, "Source and target layers cannot be the same: " + sourceLayerId);
    }
    if (mappingLayerId != null) {
      if (mappingLayerId.equals(targetLayerId)) {
        throw new InvalidConfigurationException(
          this, "Mapping and target layers cannot be the same: " + mappingLayerId);
      }
      if (mappingLayerId.equals(sourceLayerId)) {
        throw new InvalidConfigurationException(
          this, "Mapping and source layers cannot be the same: " + mappingLayerId);
      }
    }

    if (subSourceLayerId != null && subTargetLayerId != null) {
      // sub-mapping is enabled, so validate relationships
      Layer subSourceLayer = schema.getLayer(subSourceLayerId);
      if (subSourceLayer == null) 
        throw new InvalidConfigurationException(
          this, "Invalid sub-mapping label layer: " + subSourceLayerId);
      Layer subTargetLayer = schema.getLayer(subTargetLayerId);
      if (subTargetLayer == null) 
        throw new InvalidConfigurationException(
          this, "Invalid sub-mapping token layer: " + subTargetLayerId);
      if (targetLayer.getParentId().equals(schema.getWordLayerId())
          || targetLayer.getId().equals(schema.getWordLayerId())) { // word layer
        if (!subTargetLayer.getParentId().equals(schema.getWordLayerId())) {
          throw new InvalidConfigurationException(
            this, "Sub-mapping token layer "+subTargetLayerId+" must be a word layer");
        }
      } else { // phrase layer
        if (!subTargetLayer.getParentId().equals(schema.getTurnLayerId())) {
          throw new InvalidConfigurationException(
            this, "Sub-mapping token layer "+subTargetLayerId+" must be a phrase layer");
        }
      }
      if (splitLabels != null && splitLabels.length() > 0) {
        throw new InvalidConfigurationException(
          this, "Sub-mapping is only valid when not splitting "+sourceLayerId+" labels");
      }
      if (subComparator == null)
        throw new InvalidConfigurationException(this, "Sub-mapping comparator not set.");
    }
    
    // does the mapping layer need to be added to the schema?
    if (mappingLayerId != null) {
      Layer mappingLayer = schema.getLayer(mappingLayerId);
      if (mappingLayer == null) {
        String mappingParentId = targetLayerId;
        int alignment = Constants.ALIGNMENT_NONE;
        if (targetLayerId.equals(schema.getWordLayerId())
            || targetLayer.isAncestor(schema.getWordLayerId())) { 
          if (targetLayer.getAlignment() == Constants.ALIGNMENT_NONE) { // word tag
            // mapping layer is a word layer
            mappingParentId = schema.getWordLayerId();
          }
          alignment = Constants.ALIGNMENT_NONE; // tag layer
          // (otherwise, most likely a segment layer)
        } else if (targetLayer.isAncestor(schema.getTurnLayerId())) { 
          // mapping layer is a phrase layer
          mappingParentId = schema.getTurnLayerId();
          alignment = Constants.ALIGNMENT_INTERVAL; // another phrase layer
        }
        // tag layer
        schema.addLayer(
          new Layer(mappingLayerId)
          .setAlignment(alignment)
          .setPeers(false)
          .setParentId(mappingParentId)
          .setType(sourceLayer.getType()));
      }
    }
    
    if (subMappingLayerId != null) {
      // sub-mapping tag layer
      Layer subMappingLayer = schema.getLayer(subMappingLayerId);
      if (subMappingLayer == null) {
        String subMappingParentId = subTargetLayerId;
        int alignment = Constants.ALIGNMENT_NONE;
        Layer subTargetLayer = schema.getLayer(subTargetLayerId);
        Layer subSourceLayer = schema.getLayer(subSourceLayerId);
        if (subTargetLayerId.equals(schema.getWordLayerId())
            || subTargetLayer.isAncestor(schema.getWordLayerId())) { 
          if (subTargetLayer.getAlignment() == Constants.ALIGNMENT_NONE) { // word tag
            // subMapping layer is a word layer
            subMappingParentId = schema.getWordLayerId();
          }
          alignment = Constants.ALIGNMENT_NONE; // tag layer
          // (otherwise, most likely a segment layer)
        } else if (subTargetLayer.isAncestor(schema.getTurnLayerId())) { 
          // subMapping layer is a phrase layer
          subMappingParentId = schema.getTurnLayerId();
          alignment = Constants.ALIGNMENT_INTERVAL; // another phrase layer
        }
        // tag layer
        schema.addLayer(
          new Layer(subMappingLayerId)
          .setAlignment(alignment)
          .setPeers(false)
          .setParentId(subMappingParentId)
          .setType(subSourceLayer.getType()));
      }
    }
  }
   
  /**
   * Determines which layers the annotator requires in order to annotate a graph.
   * @return A list of layer IDs. In this case, the annotator only requires the schema's
   * word layer.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public String[] getRequiredLayers() throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");
    if (sourceLayerId == null)
      throw new InvalidConfigurationException(this, "No label layer set.");
    if (targetLayerId == null)
      throw new InvalidConfigurationException(this, "No token layer set.");
    HashSet<String> requiredLayers = new HashSet<String>();
    requiredLayers.add(sourceLayerId);
    requiredLayers.add(targetLayerId);
    return requiredLayers.toArray(new String[0]);
  }

  /**
   * Determines which layers the annotator will create/update/delete annotations on.
   * @return A list of layer IDs. In this case, the annotator has no task web-app for
   * specifying an output layer, and doesn't update any layers, so this method returns an
   * empty array.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public String[] getOutputLayers() throws InvalidConfigurationException {
    HashSet<String> outputLayers = new HashSet<String>();
    if (mappingLayerId != null) outputLayers.add(mappingLayerId);
    if (subMappingLayerId != null) outputLayers.add(subMappingLayerId);
    return outputLayers.toArray(new String[0]);
  }

  class LabelElement {
    Annotation source;
    String label;
    public LabelElement(Annotation s, String l) {
      source = s;
      label = l;
    }
    public LabelElement(Annotation s) {
      source = s;
      label = s.getLabel();
    }
    public LabelElement(String l) {
      label = l;
    }
    public LabelElement(char l) {
      label = ""+l;
    }
    public String toString() { // must return just the label, as this is used by comparators
      return label;
    }    
  }
  
  /**
   * Transforms the graph. In this case, the graph is simply summarized, by counting all
   * tokens of each word type, and printing out the result to stdout.
   * @param graph The graph to transform.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph transform(Graph graph) throws TransformationException {
    setRunning(true);      
    Layer sourceLayer = graph.getSchema().getLayer(sourceLayerId);
    if (sourceLayer == null) {
      throw new InvalidConfigurationException(
        this, "Invalid label layer: " + sourceLayer);
    }
    Layer targetLayer = graph.getSchema().getLayer(targetLayerId);
    if (targetLayer == null) {
      throw new InvalidConfigurationException(
        this, "Invalid token layer: " + targetLayer);
    }
    Layer mappingLayer = mappingLayerId==null?null:graph.getSchema().getLayer(mappingLayerId);

    // create a comparator for the mapping
    EditComparator<LabelElement> comparator = new Orthography2OrthographyComparator<LabelElement>();
    if (getComparator().equalsIgnoreCase("DISCToDISC")) {
      comparator = new DISC2DISCComparator<LabelElement>();
    } else if (getComparator().equalsIgnoreCase("OrthographyToDISC")) {
      comparator = new Orthography2DISCComparator<LabelElement>();
    } else if (getComparator().equalsIgnoreCase("OrthographyToArpabet")) {
      comparator = new Orthography2ARPAbetComparator<LabelElement>();
    } else if (getComparator().equalsIgnoreCase("DISCToArpabet")) {
      comparator = new DISC2ARPAbetComparator<LabelElement>();
    } else if (getComparator().equalsIgnoreCase("ArpabetToDISC")) {
      comparator = new ARPAbet2DISCComparator<LabelElement>();
    } else if (splitLabels.equalsIgnoreCase("char")) {
      // Char2Char only makes sense if splitting by char
      comparator = new Char2CharComparator<LabelElement>();
    }
    MinimumEditPath<LabelElement> mp = new MinimumEditPath<LabelElement>(comparator);
    EditComparator<LabelElement> subMappingComparator = null;
    boolean subMapping 
      = subSourceLayerId != null && subTargetLayerId != null && getSubComparator() != null;

    try {
      
      // get DB connection if we're sub-mapping
      Connection rdb = subMapping?newConnection():null;
      PreparedStatement insertEditStep = null;
      
      try {
        
        if (subMapping) {
          
          // define comparator for sub-mapping
          if (getSubComparator().equalsIgnoreCase("DISCToDISC")) {
            subMappingComparator = new DISC2DISCComparator<LabelElement>();
          } else if (getSubComparator().equalsIgnoreCase("OrthographyToDISC")) {
            subMappingComparator = new Orthography2DISCComparator<LabelElement>();
          } else if (getSubComparator().equalsIgnoreCase("OrthographyToArpabet")) {
            subMappingComparator = new Orthography2ARPAbetComparator<LabelElement>();
          } else if (getSubComparator().equalsIgnoreCase("DISCToArpabet")) {
            subMappingComparator = new DISC2ARPAbetComparator<LabelElement>();
          } else if (getSubComparator().equalsIgnoreCase("ArpabetToDISC")) {
            subMappingComparator = new ARPAbet2DISCComparator<LabelElement>();
          }
          
          // delete prior edit steps in relational database
          PreparedStatement deleteEditSteps = rdb.prepareStatement(
            "DELETE FROM "+getAnnotatorId()+"_mapping"
            +" WHERE transcript = ? AND sourceLayer = ? AND targetLayer = ?");
          deleteEditSteps.setString(1, graph.getId());
          // main mapping
          deleteEditSteps.setString(2, sourceLayerId);
          deleteEditSteps.setString(3, targetLayerId);
          deleteEditSteps.executeUpdate();
          // sub mapping
          deleteEditSteps.setString(2, subSourceLayerId);
          deleteEditSteps.setString(3, subTargetLayerId);
          deleteEditSteps.executeUpdate();
          
          // prepare edit-step insertion statement
          insertEditStep = rdb.prepareStatement(
            "INSERT INTO "+getAnnotatorId()+"_mapping"
            +" (transcript, scope, step,"
            +" sourceLayer, sourceParentId, sourceParentLabel, sourceId, sourceLabel,"
            +" sourceStart, sourceEnd,"
            +" targetLayer, targetParentId, targetParentlabel, targetId, targetLabel,"
            +" targetStart, targetEnd,"
            +" operation, distance, hierarchy, overlapRate) VALUES "
            +" (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
          insertEditStep.setString(1, graph.getId());
        } // subComparator is set
        MinimumEditPath<LabelElement> subMp
          = new MinimumEditPath<LabelElement>(subMappingComparator);
        
        if ((targetLayerId.equals(schema.getWordLayerId())
             || targetLayer.isAncestor(schema.getWordLayerId()))
            && (sourceLayerId.equals(schema.getWordLayerId())
                || sourceLayer.isAncestor(schema.getWordLayerId()))) { 
          scopeLayerId = schema.getWordLayerId();
        } else {
          scopeLayerId = schema.getUtteranceLayerId();
        }
        
        // delete any existing annotations
        if (mappingLayerId != null) {
          for (Annotation a : graph.all(mappingLayerId)) a.destroy();
        }
        if (subMappingLayerId != null) {
          for (Annotation a : graph.all(subMappingLayerId)) a.destroy();
        }
        
        boolean concatDelimiter = !splitLabels.equals("char");
        setStatus("for each " + scopeLayerId + " map " + sourceLayerId + " → " + targetLayerId);
        // for each scope annotator
        for (Annotation scope : graph.list(scopeLayerId)) {
          if (isCancelling()) break;
          
          // create source element list
          Annotation[] tokens = scope.all(targetLayerId);
          if (tokens.length == 0) {
            setStatus(scope.getLabel() + " ("+scope.getStart()+") no " + targetLayerId);
            continue;
          }
          Vector<LabelElement> vTokens = new Vector<LabelElement>();
          for (Annotation s : tokens) vTokens.add(new LabelElement(s));
          
          // create destination element list
          Vector<LabelElement> vLabels = new Vector<LabelElement>();
          if (splitLabels.length() > 0) { // split the label of the first annotation
            Annotation labels = scope.first(sourceLayerId);
            if (labels == null) {
              setStatus(scope.getLabel() + " ("+scope.getStart()+") no " + sourceLayerId);
              continue;
            }
            if (splitLabels.equals("char")) { // split label into chars (e.g. DISC transcription)
              for (char c : labels.getLabel().toCharArray()) vLabels.add(new LabelElement(c));
            } else { // split label on spaces (e.g. ARPAbet transcription)
              for (String p : labels.getLabel().split(" ")) vLabels.add(new LabelElement(p));
            }
          } else { // use full labels of all annotations
            Annotation[] labels = scope.all(sourceLayerId);
            if (labels.length == 0) {
              setStatus(scope.getLabel() + " ("+scope.getStart()+") no " + sourceLayerId);
              continue;
            }
            for (Annotation l : labels) vLabels.add(new LabelElement(l));
          }
          
          // find the minimum edit path between them
          List<EditStep<LabelElement>> path = mp.minimumEditPath(vLabels, vTokens);
          // collapse INSERT-then-DELETE into just CHANGE
          path = mp.collapse(path);
          setStatus(scope.getLabel() + " ("+scope.getStart()+") edit path: " + path.size());
          
          // create tags on the source layer from the path
          Annotation lastTag = null;
          String initialInserts = "";
          int s = 0;
          int subS = 0;
          for (EditStep<LabelElement> step : path) {
            switch(step.getOperation()) {
              case NONE:
              case CHANGE: {
                if (mappingLayerId != null) {
                  lastTag = step.getTo().source.createTag(
                    mappingLayerId, step.getFrom().label);
                  lastTag.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
                  if (!subMapping
                      && initialInserts.length() > 0) { // prepend inserts we had already found
                    lastTag.setLabel(initialInserts + lastTag.getLabel());
                    initialInserts = "";
                  }
                }
                if (subMapping) { // subMapping
                  Annotation from = step.getFrom().source;
                  Annotation[] subFrom = from.all(subSourceLayerId);
                  Annotation to = step.getTo().source;
                  Annotation[] subTo = to.all(subTargetLayerId);
                  String hierarchy = subFrom.length * subTo.length > 0?"parent":"none";
                  
                  // save in relational database
                  insertEditStep.setString(2, scope.getId()); // scope
                  insertEditStep.setInt(3, ++s); // step
                  insertEditStep.setString(4, from.getLayerId()); // sourceLayer
                  insertEditStep.setNull(5, Types.VARCHAR); // sourceParentId
                  insertEditStep.setNull(6, Types.VARCHAR); // sourceParentLabel
                  insertEditStep.setString(7, from.getId()); // sourceId
                  insertEditStep.setString(8, from.getLabel()); // sourceLabel
                  insertEditStep.setDouble(9, from.getStart().getOffset()); // sourceStart
                  insertEditStep.setDouble(10, from.getEnd().getOffset()); // sourceEnd
                  insertEditStep.setString(11, to.getLayerId()); // targetLayer
                  insertEditStep.setNull(12, Types.VARCHAR); // targetParentId
                  insertEditStep.setNull(13, Types.VARCHAR); // targetParentLabel
                  insertEditStep.setString(14, to.getId()); // targetId
                  insertEditStep.setString(15, to.getLabel()); // targetLabel
                  insertEditStep.setDouble(16, to.getStart().getOffset()); // targetStart
                  insertEditStep.setDouble(17, to.getEnd().getOffset()); // targetEnd
                  insertEditStep.setString(18, operation(step.getOperation())); // operation
                  insertEditStep.setInt(19, step.getStepDistance()); // distance
                  insertEditStep.setString(20, hierarchy); // hierarchy
                  insertEditStep.setDouble(21, OverlapRate(from, to)); // overlapRate
                  insertEditStep.executeUpdate();
                  
                  // map children
                  if (subFrom.length > 0 && subTo.length > 0) {
                    subS = subMapping(from, subFrom, to, subTo, subMp, insertEditStep, subS);
                  }
                } // sub-mapping
                
                break;
              }
              case DELETE: {
                if (!subMapping) {
                  // append to the previous one
                  if (lastTag != null) {
                    if (concatDelimiter) {                
                      lastTag.setLabel(lastTag.getLabel() + " " + step.getFrom().label);
                    } else {
                      lastTag.setLabel(lastTag.getLabel() + step.getFrom().label);
                    }
                  } else { // remember the label until we can prepend it to something
                    initialInserts += step.getFrom().label;
                    if (concatDelimiter) initialInserts += " ";
                  }
                } else { // sub-mapping
                  Annotation from = step.getFrom().source;
                  // save in relational database
                  // (scope was already set)
                  insertEditStep.setInt(3, ++s); // step
                  insertEditStep.setString(4, from.getLayerId()); // sourceLayer
                  insertEditStep.setNull(5, Types.VARCHAR); // sourceParentId
                  insertEditStep.setNull(6, Types.VARCHAR); // sourceParentLabel
                  insertEditStep.setString(7, from.getId()); // sourceId
                  insertEditStep.setString(8, from.getLabel()); // sourceLabel
                  insertEditStep.setDouble(9, from.getStart().getOffset()); // sourceStart
                  insertEditStep.setDouble(10, from.getEnd().getOffset()); // sourceEnd
                  insertEditStep.setString(11, targetLayerId); // targetLayer
                  insertEditStep.setNull(12, Types.VARCHAR); // targetParentId
                  insertEditStep.setNull(13, Types.VARCHAR); // targetParentLabel
                  insertEditStep.setNull(14, Types.VARCHAR); // targetId
                  insertEditStep.setNull(15, Types.VARCHAR); // targetLabel
                  insertEditStep.setNull(16, Types.DOUBLE); // targetStart
                  insertEditStep.setNull(17, Types.DOUBLE); // targetEnd
                  insertEditStep.setString(18, operation(step.getOperation())); // operation
                  insertEditStep.setInt(19, step.getStepDistance()); // distance
                  insertEditStep.setNull(20, Types.VARCHAR); // hierarchy
                  insertEditStep.setNull(21, Types.DOUBLE); // overlapRate
                  insertEditStep.executeUpdate();
                }
                break;
              }
              case INSERT: {
                if (subMapping) {
                  Annotation to = step.getTo().source;
                  // save in relational database
                  // (scope was already set)
                  insertEditStep.setInt(3, ++s); // step
                  insertEditStep.setString(4, sourceLayerId); // sourceLayer
                  insertEditStep.setNull(5, Types.VARCHAR); // sourceParentId
                  insertEditStep.setNull(6, Types.VARCHAR); // sourceParentLabel
                  insertEditStep.setNull(7, Types.VARCHAR); // sourceId
                  insertEditStep.setNull(8, Types.VARCHAR); // sourceLabel
                  insertEditStep.setNull(9, Types.DOUBLE); // sourceStart
                  insertEditStep.setNull(10, Types.DOUBLE); // sourceEnd
                  insertEditStep.setString(11, to.getLayerId()); // targetLayer
                  insertEditStep.setNull(12, Types.VARCHAR); // targetParentId
                  insertEditStep.setNull(13, Types.VARCHAR); // targetParentLabel
                  insertEditStep.setString(14, to.getId()); // targetId
                  insertEditStep.setString(15, to.getLabel()); // targetLabel
                  insertEditStep.setDouble(16, to.getStart().getOffset()); // targetStart
                  insertEditStep.setDouble(17, to.getEnd().getOffset()); // targetEnd
                  insertEditStep.setString(18, operation(step.getOperation())); // operation
                  insertEditStep.setInt(19, step.getStepDistance()); // distance
                  insertEditStep.setNull(20, Types.VARCHAR); // hierarchy
                  insertEditStep.setNull(21, Types.DOUBLE); // overlapRate
                  insertEditStep.executeUpdate();
                }
                break;
              }
            }
          } // next step
        } // next scope annotation
      } finally {
        if (rdb != null) {
          try { rdb.close(); } catch(SQLException x) {}
        }
      }
    } catch (SQLException x) {
      throw new TransformationException(this, x);
    }
    
    setRunning(false);
    return graph;
  }   
  
  /**
   * Map the sub-source annotations of the given source annotation to the sub-target
   * annotations of the given target annotation. 
   * @param mainSource The annotation used to identify which sub-sources to select for mapping.
   * @param subSources The sub-source annotations.
   * @param mainTarget The annotation used to identify which sub-targets to select for mapping.
   * @param subTargets The sub-target annotations.
   * @param mp The minimum edit path processor for the sub-mapping.
   * @param insertEditStep Prepared statement for recording edit steps in the relational database.
   * @param s The last index used when inserting steps into the relational database.
   * @return The last index used when inserting steps into the relational database.
   * @throws SQLException
   */
  protected int subMapping(
    Annotation mainSource, Annotation[] subSources, Annotation mainTarget, Annotation[] subTargets,
    MinimumEditPath<LabelElement> mp, PreparedStatement insertEditStep, int s)
    throws SQLException {
    // get the sub-label annotations
    Vector<LabelElement> vLabels = new Vector<LabelElement>();
    for (Annotation l : subSources) {
      vLabels.add(new LabelElement(l));
    }
    if (vLabels.size() == 0) {
      setStatus(
        mainSource.getLabel() + " ("+mainSource.getStart()+") no " + subSourceLayerId);
    } else {
      // get the sub-token annotations
      Vector<LabelElement> vTokens = new Vector<LabelElement>();
      for (Annotation t : subTargets) {
        vTokens.add(new LabelElement(t));
      }
      if (vTokens.size() == 0) {
        setStatus(
          mainTarget.getLabel() + " ("+mainTarget.getStart()+") no " + subTargetLayerId);
      } else { // both have sub annotations
        // find the minimum edit path between then
        List<EditStep<LabelElement>> path = mp.minimumEditPath(vLabels, vTokens);
        // collapse INSERT-then-DELETE into just CHANGE
        path = mp.collapse(path);
        setStatus(
          mainSource.getLabel()+" ("+mainSource.getStart()+") edit path: "+path.size());
        
        // map them together
        for (EditStep<LabelElement> step : path) {
          switch(step.getOperation()) {
            case NONE:
            case CHANGE: {
              Annotation from = step.getFrom().source;
              String fromParent = from.getId();
              if (from.getLayer().getParentId().equals(schema.getWordLayerId())) {
                fromParent = from.getParentId();
              }
              Annotation to = step.getTo().source;
              String toParent = to.getId();
              if (to.getLayer().getParentId().equals(schema.getWordLayerId())) {
                toParent = to.getParentId();
              }
              if (subMappingLayerId != null) {
                to.createTag(
                  subMappingLayerId, step.getFrom().label)
                  .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
              }
              // save in relational database
              // (scope was already set)
              insertEditStep.setInt(3, ++s); // step
              insertEditStep.setString(4, from.getLayerId()); // sourceLayer
              insertEditStep.setString(5, fromParent); // sourceParentId
              insertEditStep.setString(6, mainSource.getLabel()); // sourceParentLabel
              insertEditStep.setString(7, from.getId()); // sourceId
              insertEditStep.setString(8, from.getLabel()); // sourceLabel
              insertEditStep.setDouble(9, from.getStart().getOffset()); // sourceStart
              insertEditStep.setDouble(10, from.getEnd().getOffset()); // sourceEnd
              insertEditStep.setString(11, to.getLayerId()); // targetLayer
              insertEditStep.setString(12, toParent); // targetParentId
              insertEditStep.setString(13, mainTarget.getLabel()); // targetParentLabel
              insertEditStep.setString(14, to.getId()); // targetId
              insertEditStep.setString(15, to.getLabel()); // targetLabel
              insertEditStep.setDouble(16, to.getStart().getOffset()); // targetStart
              insertEditStep.setDouble(17, to.getEnd().getOffset()); // targetEnd
              insertEditStep.setString(18, operation(step.getOperation())); // operation
              insertEditStep.setInt(19, step.getStepDistance()); // distance
              insertEditStep.setString(20, "child"); // hierarchy
              insertEditStep.setDouble(21, OverlapRate(from, to)); // overlapRate
              insertEditStep.executeUpdate();
              break;
            }
            case DELETE: {
              Annotation from = step.getFrom().source;
              String fromParent = from.getId();
              if (from.getLayer().getParentId().equals(schema.getWordLayerId())) {
                fromParent = from.getParentId();
              }
              // save in relational database
              // (scope was already set)
              insertEditStep.setInt(3, ++s); // step
              insertEditStep.setString(4, from.getLayerId()); // sourceLayer
              insertEditStep.setString(5, fromParent); // sourceParentId
              insertEditStep.setString(6, mainSource.getLabel()); // sourceParentLabel
              insertEditStep.setString(7, from.getId()); // sourceId
              insertEditStep.setString(8, from.getLabel()); // sourceLabel
              insertEditStep.setDouble(9, from.getStart().getOffset()); // sourceStart
              insertEditStep.setDouble(10, from.getEnd().getOffset()); // sourceEnd
              insertEditStep.setString(11, subTargetLayerId); // targetLayer
              insertEditStep.setNull(12, Types.VARCHAR); // targetParentId
              insertEditStep.setString(13, mainTarget.getLabel()); // targetParentLabel
              insertEditStep.setNull(14, Types.VARCHAR); // targetId
              insertEditStep.setNull(15, Types.VARCHAR); // targetLabel
              insertEditStep.setNull(16, Types.DOUBLE); // targetStart
              insertEditStep.setNull(17, Types.DOUBLE); // targetEnd
              insertEditStep.setString(18, operation(step.getOperation())); // operation
              insertEditStep.setInt(19, step.getStepDistance()); // distance
              insertEditStep.setString(20, "child"); // hierarchy
              insertEditStep.setNull(21, Types.DOUBLE); // overlapRate
              insertEditStep.executeUpdate();
              break;
            }
            case INSERT: {
              Annotation to = step.getTo().source;
              String toParent = to.getId();
              if (to.getLayer().getParentId().equals(schema.getWordLayerId())) {
                toParent = to.getParentId();
              }
              // save in relational database
              // (scope was already set)
              insertEditStep.setInt(3, ++s); // step
              insertEditStep.setString(4, subSourceLayerId); // sourceLayer
              insertEditStep.setNull(5, Types.VARCHAR); // sourceParentId
              insertEditStep.setString(6, mainSource.getLabel()); // sourceParentLabel
              insertEditStep.setNull(7, Types.VARCHAR); // sourceId
              insertEditStep.setNull(8, Types.VARCHAR); // sourceLabel
              insertEditStep.setNull(9, Types.DOUBLE); // sourceStart
              insertEditStep.setNull(10, Types.DOUBLE); // sourceEnd
              insertEditStep.setString(11, to.getLayerId()); // targetLayer
              insertEditStep.setString(12, toParent); // targetParentId
              insertEditStep.setString(13, mainTarget.getLabel()); // targetParentLabel
              insertEditStep.setString(14, to.getId()); // targetId
              insertEditStep.setString(15, to.getLabel()); // targetLabel
              insertEditStep.setDouble(16, to.getStart().getOffset()); // targetStart
              insertEditStep.setDouble(17, to.getEnd().getOffset()); // targetEnd
              insertEditStep.setString(18, operation(step.getOperation())); // operation
              insertEditStep.setInt(19, step.getStepDistance()); // distance
              insertEditStep.setString(20, "child"); // hierarchy
              insertEditStep.setNull(21, Types.DOUBLE); // overlapRate
              insertEditStep.executeUpdate();
              break;
            }
          }
        } // next sub-step
      } // there are sub tokens
    } // there are sub labels
    return s;
  } // end of subMapping()
  
  /**
   * Converts an EditStep#StepOperation into a character code for saving in the relational 
   * database.  
   * @param operation
   * @return Code representing the operation.
   */
  protected String operation(EditStep.StepOperation operation) {
    switch(operation) {
      case DELETE: return "-";
      case CHANGE: return "!";
      case INSERT: return "+";
      default: return " ";
    }
  } // end of operation()
  
  /**
   * Calculates the Overlap Rate of two annotations.
   * <p> <a href="http://dx.doi.org/10.1007/978-3-540-30228-5_4">Paulo and Oliveira (2004)</a> 
   * devised Overlap Rate (OvR) to compare alignments, which measures how much two
   * intervals overlap, independent of their absolute durations. OvR is calculated as
   * follows: <br>
   * OvR = CommonDur / DurMax = CommonDur / (Dur1 + Dur2 - CommonDur)
   * @param i1 Annotation representing one of the intervals.
   * @param i2 Annotation representing the other interval.
   * @return A value between 0 and 1. A value of 0 means that the two intervals do not
   * overlap at all, with 1 meaning they completely overlap. 
   */
  public static double OverlapRate(Annotation i1, Annotation i2) {
    // if either annotation has a null start or end, return 0.0
    if (!i1.getAnchored() || !i2.getAnchored()) return 0.0;
    
    // get offsets
    double start1 = i1.getStart().getOffset();
    double end1 = i1.getEnd().getOffset();
    double dur1 = end1 - start1;
    double start2 = i2.getStart().getOffset();
    double end2 = i2.getEnd().getOffset();
    double dur2 = end2 - start2;
    double latestStart = Math.max(start1, start2);
    double earliestEnd = Math.min(end1, end2);
    double commonDur = latestStart < earliestEnd? // if they overlap
      // the common duration is the difference between the latest start and the earliest end
      earliestEnd - latestStart
      // otherwise, there's no common duration
      : 0.0;
    return commonDur / (dur1 + dur2 - commonDur);
  } // end of overlapRate()

  /**
   * Lists tracked mappings. 
   * @return A list of strings formatted sourceLayerId→targetLayerId, representing
   * tracker mappings that can be accessed via {@link #mappingToCsv(String)}.
   * @throws SQLException
   * @see #mappingToCsv(String)
   */
  public List<String> listMappings() throws SQLException {
    Vector<String> mappings = new Vector<String>();
    Connection rdb = newConnection();
    PreparedStatement sql = rdb.prepareStatement(
      sqlx.apply(
        "SELECT DISTINCT sourceLayer, targetLayer"
        +" FROM "+getAnnotatorId()+"_mapping"
        +" ORDER BY sourceLayer, targetLayer"));
    ResultSet rs = sql.executeQuery();
    try {
      while (rs.next()) {
        mappings.add(rs.getString(1) + "→" + rs.getString(2));
      }
    } finally {
      rs.close();
      sql.close();
      rdb.close();
    }
    return mappings;
  } // end of listMappings()
  
  /**
   * Provides access to the mapping between the given two layers, as a CSV stream.
   * @param mappingId A string of the form sourceLayerId→targetLayerId that identifies a
   * tracked mapping between layers.
   * @return A stream of CSV records.
   * @see #listMappings()
   * @see #mappingToCsv(String,String)
   */
  public InputStream mappingToCsv(String mappingId)
    throws IOException, SQLException {
    int arrowPos = mappingId.indexOf("→");
    if (arrowPos < 0) throw new SQLException("Invalid mapping ID: " + mappingId);
    return mappingToCsv(mappingId.substring(0, arrowPos), mappingId.substring(arrowPos + 1));
  }
  
  /**
   * Provides access to the mapping between the given two layers, as a CSV stream.
   * @param sourceLayerId The source layer ID.
   * @param targetLayerId The target layer ID.
   * @return A stream of CSV records.
   */
  public InputStream mappingToCsv(String sourceLayerId, String targetLayerId)
    throws IOException, SQLException {
    
    File csv = File.createTempFile("LabelMapper_", "_mapping.csv");
    csv.deleteOnExit();
    CSVPrinter out = new CSVPrinter(
      new OutputStreamWriter(new FileOutputStream(csv), "UTF-8"), CSVFormat.EXCEL);
    String transcriptUrl = null;
    try {
      if (getStore() != null && getStore().getId() != null
          && getStore().getId().startsWith("http")) { // we have a URL
        transcriptUrl = getStore().getId()
          + (getStore().getId().endsWith("/")?"":"/")
          + "transcript?id=";
      }
    } catch(Exception exception) {}
    try {
      Connection rdb = newConnection();
      PreparedStatement sql = rdb.prepareStatement(
        sqlx.apply(
          "SELECT transcript, scope, step,"
          +" sourceLayer, sourceParentId, sourceParentLabel, sourceId, sourceLabel,"
          +" sourceStart, sourceEnd,"
          +" targetLayer, targetParentId, targetParentLabel, targetId, targetLabel,"
          +" targetStart, targetEnd,"
          +" operation, distance, hierarchy, overlapRate"
          +" FROM "+getAnnotatorId()+"_mapping"
          +" WHERE sourceLayer = ? AND targetLayer = ?"
          +" ORDER BY transcript, scope, step"));
      sql.setString(1, sourceLayerId);
      sql.setString(2, targetLayerId);

      try {

        out.print("transcript");
        out.print("scope");
        if(transcriptUrl != null) out.print("URL");
        out.print("step");
        out.print("sourceLayer");
        out.print("sourceParentId");
        out.print("sourceParentLabel");
        out.print("sourceId");
        out.print("sourceLabel");
        out.print("sourceStart");
        out.print("sourceEnd");
        out.print("targetLayer");
        out.print("targetParentId");
        out.print("targetParentLabel");
        out.print("targetId");
        out.print("targetLabel");
        out.print("targetStart");
        out.print("targetEnd");
        out.print("operation");
        out.print("distance");
        out.print("hierarchy");
        out.print("overlapRate");
        out.println();
        
        ResultSet rs = sql.executeQuery();
        try {
          while (rs.next()) {
            for (int i = 1; i <= 21; i++) {
              out.print(rs.getString(i));
              if (i == 2 && transcriptUrl != null) { // scope
                // insert URL
                out.print(transcriptUrl + rs.getString(1) + "#" + rs.getString(2));
              }
            } // next field
            out.println();
          }
        } finally {
          rs.close();
        }
      } finally {
        sql.close();
        rdb.close();
      }
    } catch (SQLException x) {
      throw x;
    } finally {
      out.close();
    }
    return new FileInputStream(csv);
  } // end of mappingToCsv()

  /**
   * Provides access to the mapping between the given two layers, summarized by utterance,
   * as a CSV stream. 
   * @param mappingId A string of the form sourceLayerId→targetLayerId that identifies a
   * tracked mapping between layers.
   * @return A stream of CSV records.
   */
  public InputStream utteranceSummaryToCsv(String mappingId)
    throws IOException, SQLException {
    int arrowPos = mappingId.indexOf("→");
    if (arrowPos < 0) throw new SQLException("Invalid mapping ID: " + mappingId);
    return utteranceSummaryToCsv(
      mappingId.substring(0, arrowPos), mappingId.substring(arrowPos + 1));
  }
  
  /**
   * Provides access to the mapping between the given two layers, summarized by utterance,
   * as a CSV stream. 
   * @param sourceLayerId The source layer ID.
   * @param targetLayerId The target layer ID.
   * @return A stream of CSV records.
   */
  public InputStream utteranceSummaryToCsv(String sourceLayerId, String targetLayerId)
    throws IOException, SQLException {
    
    File csv = File.createTempFile("LabelMapper_", "_utterances.csv");
    csv.deleteOnExit();
    CSVPrinter out = new CSVPrinter(
      new OutputStreamWriter(new FileOutputStream(csv), "UTF-8"), CSVFormat.EXCEL);
    String transcriptUrl = null;
    try {
      if (getStore() != null && getStore().getId() != null
          && getStore().getId().startsWith("http")) { // we have a URL
        transcriptUrl = getStore().getId()
          + (getStore().getId().endsWith("/")?"":"/")
          + "transcript?id=";
      }
    } catch(Exception exception) {}
    try {
      Connection rdb = newConnection();
      PreparedStatement sql = rdb.prepareStatement(
        sqlx.apply(
          "SELECT transcript, scope, COUNT(*) AS `stepCount`,"
          +" AVG(overlapRate) AS `meanOverlapRate`"
          +" FROM "+getAnnotatorId()+"_mapping"
          +" WHERE sourceLayer = ? AND targetLayer = ?"
          +" GROUP BY transcript, scope"));
      sql.setString(1, sourceLayerId);
      sql.setString(2, targetLayerId);

      try {

        out.print("transcript");
        out.print("scope");
        if(transcriptUrl != null) out.print("URL");
        out.print("stepCount");
        out.print("meanOverlapRate");
        out.println();
        
        ResultSet rs = sql.executeQuery();
        try {
          while (rs.next()) {
            for (int i = 1; i <= 4; i++) {
              out.print(rs.getString(i));
              if (i == 2 && transcriptUrl != null) { // scope
                // insert URL
                out.print(transcriptUrl + rs.getString(1) + "#" + rs.getString(2));
              }
            } // next field
            out.println();
          }
        } finally {
          rs.close();
        }
      } finally {
        sql.close();
        rdb.close();
      }
    } catch (SQLException x) {
      throw x;
    } finally {
      out.close();
    }
    return new FileInputStream(csv);
  } // end of utteranceSummaryToCsv()

  /**
   * Provides summary information about the given mapping.
   * @param mappingId A string of the form sourceLayerId→targetLayerId that identifies a
   * tracked mapping between layers.
   * @return A map containing summary statistics.
   * @throws SQLException
   */
  public Map<String,Double> summarizeMapping(String mappingId) throws SQLException {
    int arrowPos = mappingId.indexOf("→");
    if (arrowPos < 0) throw new SQLException("Invalid mapping ID: " + mappingId);
    return summarizeMapping(mappingId.substring(0, arrowPos), mappingId.substring(arrowPos + 1));
  } // end of summarizeMapping()
  
  /**
   * Provides summary information about the mapping between the given layers.
   * @param sourceLayerId The source layer ID.
   * @param targetLayerId The target layer ID.
   * @return A map containing summary statistics.
   * @throws SQLException
   */
  public Map<String,Double> summarizeMapping(String sourceLayerId, String targetLayerId)
    throws SQLException {
    TreeMap<String,Double> summary = new TreeMap<String,Double>();
    
    Connection rdb = newConnection();
    PreparedStatement sql = rdb.prepareStatement(
      sqlx.apply(
        "SELECT COUNT(DISTINCT scope) AS `utteranceCount`, COUNT(*) AS `stepCount`,"
        +" AVG(overlapRate) AS `meanOverlapRate`"
        +" FROM "+getAnnotatorId()+"_mapping"
        +" WHERE sourceLayer = ? AND targetLayer = ?"));
    sql.setString(1, sourceLayerId);
    sql.setString(2, targetLayerId);
    ResultSet rs = sql.executeQuery();
    try {
      if (rs.next()) {
        ResultSetMetaData metaData = rs.getMetaData();
        for (int c = 1; c <= metaData.getColumnCount(); c++) {
          summary.put(metaData.getColumnName(c), rs.getDouble(c));
        } // next column
        
        rs.close();
        sql.close();        
        sql = rdb.prepareStatement(
          sqlx.apply(
            "SELECT COUNT(DISTINCT sourceId) AS `sourceCount`"
            +" FROM "+getAnnotatorId()+"_mapping"
            +" WHERE sourceLayer = ? AND targetLayer = ?"));
        sql.setString(1, sourceLayerId);
        sql.setString(2, targetLayerId);
        rs = sql.executeQuery();
        if (rs.next()) {
          metaData = rs.getMetaData();
          for (int c = 1; c <= metaData.getColumnCount(); c++) {
            summary.put(metaData.getColumnName(c), rs.getDouble(c));
          } // next column
        }
        
        rs.close();
        sql.close();        
        sql = rdb.prepareStatement(
          sqlx.apply(
            "SELECT COUNT(DISTINCT targetId) AS `targetCount`"
            +" FROM "+getAnnotatorId()+"_mapping"
            +" WHERE sourceLayer = ? AND targetLayer = ?"));
        sql.setString(1, sourceLayerId);
        sql.setString(2, targetLayerId);
        rs = sql.executeQuery();
        if (rs.next()) {
          metaData = rs.getMetaData();
          for (int c = 1; c <= metaData.getColumnCount(); c++) {
            summary.put(metaData.getColumnName(c), rs.getDouble(c));
          } // next column
        }
      }
    } finally {
      rs.close();
      sql.close();
      rdb.close();
    }
    return summary;
  } // end of summarizeMapping()

  /**
   * Deletes given mapping.
   * @param mappingId A string of the form sourceLayerId→targetLayerId that identifies a
   * tracked mapping between layers.
   * @throws SQLException
   */
  public void deleteMapping(String mappingId) throws SQLException {
    int arrowPos = mappingId.indexOf("→");
    if (arrowPos < 0) throw new SQLException("Invalid mapping ID: " + mappingId);
    deleteMapping(mappingId.substring(0, arrowPos), mappingId.substring(arrowPos + 1));
  } // end of summarizeMapping()
  
  
  /**
   * Deletes mapping data between the given layers.
   * @param sourceLayerId The source layer ID.
   * @param targetLayerId The target layer ID.
   * @throws SQLException
   */
  public void deleteMapping(String sourceLayerId, String targetLayerId)
    throws SQLException {
    Connection rdb = newConnection();
    PreparedStatement sql = rdb.prepareStatement(
      sqlx.apply(
        "DELETE FROM "+getAnnotatorId()+"_mapping WHERE sourceLayer = ? AND targetLayer = ?"));
    try {
      sql.setString(1, sourceLayerId);
      sql.setString(2, targetLayerId);
      sql.executeUpdate();
    } finally {
      sql.close();
      rdb.close();
    }
  }
} // end of class LabelMapper
