//
// Copyright 2019 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.sql.*;
import java.util.List;
import java.util.Vector;
import java.util.function.UnaryOperator;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import nzilbb.ag.Schema;
import nzilbb.ag.Layer;
import nzilbb.ag.ql.*;

/**
 * Converts AGQL expressions into SQL queries for matching graphs (transcripts).
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class GraphAgqlToSql
{
  // Attributes:
  
  /**
   * Layer schema.
   * @see #getSchema()
   * @see #setSchema(Schema)
   */
  protected Schema schema;
  /**
   * Getter for {@link #schema}.
   * @return Layer schema.
   */
  public Schema getSchema() { return schema; }
  /**
   * Setter for {@link #schema}.
   * @param schema Layer schema.
   * @return <var>this</var>.
   */
  public GraphAgqlToSql setSchema(Schema schema) { this.schema = schema; return this; }
  
  // Methods:
  
  /**
   * Default constructor.
   */
  public GraphAgqlToSql()
  {
  } // end of constructor
  
  /**
   * Attribute constructor.
   */
  public GraphAgqlToSql(Schema schema)
  {
    setSchema(schema);
  } // end of constructor
  
  /**
   * Transforms the given AGQL query into an SQL query.
   * @param expression The graph-matching expression, for example:
   * <ul>
   *  <li><code>id MATCHES 'Ada.+'</code></li>
   *  <li><code>my('corpus').label = 'CC'</code></li>
   *  <li><code>'Robert' IN labels('who')</code></li>
   *  <li><code>id NOT MATCHES 'Ada.+' AND my('corpus').label = 'CC' AND 'Robert' IN
   * labels('who')</code></li> 
   * </ul>
   * @param sqlSelectClause The SQL expression that is to go between SELECT and FROM.
   * @param userWhereClause The expression to add to the WHERE clause to ensure the user doesn't
   * get access to data to which they're not entitled, or null.
   * @param orderClause A comma-separated list of AGQL expressions to determine the order of
   * results; e.g. "my('corpus').label, id", or null. 
   * @param slqLimitClause The SQL LIMIT clause to append, or null for no LIMIT clause. 
   * @throws AGQLException If the expression is invalid.
   */
  public Query sqlFor(String expression, String sqlSelectClause, String userWhereClause, String orderClause, String sqlLimitClause)
    throws AGQLException
  {
    // ensure there's always a sensible order
    if (orderClause == null || orderClause.trim().length() == 0) orderClause = "id";
    final Query q = new Query();
    final StringBuilder conditions = new StringBuilder();
    final Vector<String> errors = new Vector<String>();
    AGQLBaseListener listener = new AGQLBaseListener() {
        private void space()
        {
          if (conditions.length() > 0 && conditions.charAt(conditions.length() - 1) != ' ')
          {
            conditions.append(" ");
          }
        }
        private String unquote(String s)
        {
          return s.substring(1, s.length() - 1);
        }
        private String attribute(String s)
        {
          return s.replaceAll("^(participant|transcript)_","");
        }
        private String escape(String s)
        {
          return s.replaceAll("\\'", "\\\\'");
        }
        @Override public void exitThisIdExpression(AGQLParser.ThisIdExpressionContext ctx)
        {
          space();
          conditions.append("transcript.transcript_id");
        }
        @Override public void exitThisLabelExpression(AGQLParser.ThisLabelExpressionContext ctx)
        {
          space();
          conditions.append("transcript.transcript_id");
        }
        @Override public void exitCorpusLabelOperand(AGQLParser.CorpusLabelOperandContext ctx)
        {
          space();
          conditions.append("transcript.corpus_name");
        }
        @Override public void enterCorpusLabelsExpression(AGQLParser.CorpusLabelsExpressionContext ctx)
        {
          space();
          conditions.append("(SELECT transcript.corpus_name)");
        }
        @Override public void exitEpisodeLabelOperand(AGQLParser.EpisodeLabelOperandContext ctx)
        {
          space();
          conditions.append(
            "(SELECT name"
            +" FROM transcript_family"
            +" WHERE transcript_family.family_id = transcript.family_id)");
        }
        @Override public void enterWhoLabelsExpression(AGQLParser.WhoLabelsExpressionContext ctx)
        {
          space();
          conditions.append(
            "(SELECT speaker.name"
            +" FROM transcript_speaker"
            +" INNER JOIN speaker ON transcript_speaker.speaker_number = speaker.speaker_number"
            +" WHERE transcript_speaker.ag_id = transcript.ag_id)");
        }
        @Override public void enterWhoLabelExpression(AGQLParser.WhoLabelExpressionContext ctx)
        {
          space();
          conditions.append(
            "(SELECT speaker.name"
            +" FROM transcript_speaker"
            +" INNER JOIN speaker ON transcript_speaker.speaker_number = speaker.speaker_number"
            +" WHERE transcript_speaker.ag_id = transcript.ag_id"
            // the first one
            +" ORDER BY speaker.name LIMIT 1)");
        }
        @Override public void enterLabelsExpression(AGQLParser.LabelsExpressionContext ctx)
        {
          space();
          String layerId = unquote(ctx.stringLiteral().quotedString.getText());
          Layer layer = getSchema().getLayer(layerId);
          if (layer == null)
          {
            errors.add("Invalid layer: " + ctx.getText());
          }
          else
          {
            String attribute = attribute(layerId);
            if ("transcript".equals(layer.get("@class_id")))
            {
              conditions.append(
                "(SELECT DISTINCT label"
                +" FROM annotation_transcript USE INDEX(IDX_AG_ID_NAME)"
                +" WHERE annotation_transcript.layer = '"+escape(attribute)+"'"
                +" AND transcript_speaker.ag_id = transcript.ag_id)");
            } // transcript attribute
            else if ("speaker".equals(layer.get("@class_id")))
            {
              conditions.append(
                "(SELECT DISTINCT label"
                +" FROM annotation_participant"
                +" INNER JOIN transcript_speaker"
                +" ON annotation_participant.speaker_number = transcript_speaker.speaker_number"
                +" AND annotation_participant.layer = '"+escape(attribute)+"'"
                +" WHERE transcript_speaker.ag_id = transcript.ag_id)");
            } // participant attribute
            else if (schema.getEpisodeLayerId().equals(layer.getParentId()))
            { // episode attribute
              conditions.append(
                "(SELECT label"
                +" FROM `annotation_layer_" + layer.get("@layer_id") + "` annotation"
                +" WHERE annotation.family_id = transcript.family_id)");
            } // episode attribute
            else
            { // regular temporal layer
              conditions.append(
                "(SELECT label"
                +" FROM annotation_layer_" + layer.get("@layer_id") + " annotation"
                +" WHERE annotation.ag_id = transcript.ag_id)");
            } // regular temporal layer
          } // valid layer
        }
        @Override public void enterOtherLabelExpression(AGQLParser.OtherLabelExpressionContext ctx)
        {
          space();
          String layerId = unquote(ctx.stringLiteral().quotedString.getText());
          Layer layer = getSchema().getLayer(layerId);
          if (layer == null)
          {
            errors.add("Invalid layer: " + ctx.getText());
          }
          else
          {
            String attribute = attribute(layerId);
            if ("transcript".equals(layer.get("@class_id")))
            {
              conditions.append(
                "(SELECT label"
                +" FROM annotation_transcript USE INDEX(IDX_AG_ID_NAME)"
                +" WHERE annotation_transcript.layer = '"+escape(attribute)+"'"
                +" AND annotation_transcript.ag_id = transcript.ag_id"
                +" ORDER BY annotation_id LIMIT 1)");
            } // transcript attribute
            else if ("speaker".equals(layer.get("@class_id")))
            { // participant attribute
              conditions.append(
                "(SELECT label"
                +" FROM annotation_participant"
                +" INNER JOIN transcript_speaker"
                +" ON annotation_participant.speaker_number = transcript_speaker.speaker_number"
                +" AND annotation_participant.layer = '"+escape(attribute)+"'"
                +" WHERE transcript_speaker.ag_id = transcript.ag_id"
                +" ORDER BY annotation_id LIMIT 1)");
            } // participant attribute 
            else if (schema.getEpisodeLayerId().equals(layer.getParentId()))
            { // episode attribute
              conditions.append(
                "(SELECT label"
                +" FROM `annotation_layer_" + layer.get("@layer_id") + "` annotation"
                +" WHERE annotation.family_id = transcript.family_id"
                +" ORDER BY annotation.ordinal LIMIT 1)");
            } // episode attribute
            else
            { // regular temporal layer
              conditions.append(
                "(SELECT label"
                +" FROM annotation_layer_" + layer.get("@layer_id") + " annotation"
                +" INNER JOIN anchor ON annotation.start_anchor_id = anchor.anchor_id"
                +" WHERE annotation.ag_id = transcript.ag_id"
                +" ORDER BY anchor.offset, annotation.annotation_id LIMIT 1)");
            } // regular temporal layer
          } // valid layer
        }
        @Override public void exitListLengthExpression(AGQLParser.ListLengthExpressionContext ctx)
        {
          space();
          String layerId = unquote(ctx.stringLiteral().quotedString.getText());
          Layer layer = getSchema().getLayer(layerId);
          if (layer == null)
          {
            errors.add("Invalid layer: " + ctx.getText());
          }
          else
          {
            String attribute = attribute(layerId);
            if ("transcript".equals(layer.get("@class_id")))
            {
              conditions.append(
                "(SELECT COUNT(*)"
                +" FROM annotation_transcript USE INDEX(IDX_AG_ID_NAME)"
                +" WHERE annotation_transcript.layer = '"+escape(attribute)+"'"
                +" AND transcript_speaker.ag_id = transcript.ag_id)");
            } // transcript attribute
            else if ("speaker".equals(layer.get("@class_id")))
            {
              conditions.append(
                "(SELECT COUNT(*)"
                +" FROM annotation_participant"
                +" INNER JOIN transcript_speaker"
                +" ON annotation_participant.speaker_number = transcript_speaker.speaker_number"
                +" AND annotation_participant.layer = '"+escape(attribute)+"'"
                +" WHERE transcript_speaker.ag_id = transcript.ag_id)");
            } // participant attribute
            else if (schema.getEpisodeLayerId().equals(layer.getParentId()))
            { // episode attribute
              conditions.append(
                "(SELECT COUNT(*)"
                +" FROM `annotation_layer_" + layer.get("@layer_id") + "` annotation"
                +" WHERE annotation.family_id = transcript.family_id)");
            } // episode attribute
            else
            { // regular temporal layer
              conditions.append(
                "(SELECT COUNT(*)"
                +" FROM annotation_layer_" + layer.get("@layer_id") + " annotation"
                +" WHERE annotation.ag_id = transcript.ag_id)");
            } // regular temporal layer
          } // valid layer
        }
        @Override public void enterAnnotatorsExpression(AGQLParser.AnnotatorsExpressionContext ctx)
        {
          space();
          String layerId = unquote(ctx.stringLiteral().quotedString.getText());
          Layer layer = getSchema().getLayer(layerId);
          if (layer == null)
          {
            errors.add("Invalid layer: " + ctx.getText());
          }
          else
          {
            String attribute = attribute(layerId);
            if ("transcript".equals(layer.get("@class_id")))
            {
              conditions.append(
                "(SELECT DISTINCT annotated_by"
                +" FROM annotation_transcript USE INDEX(IDX_AG_ID_NAME)"
                +" WHERE annotation_transcript.layer = '"+escape(attribute)+"'"
                +" AND transcript_speaker.ag_id = transcript.ag_id)");
            } // transcript attribute
            else if ("speaker".equals(layer.get("@class_id")))
            {
              conditions.append(
                "(SELECT DISTINCT annotated_by"
                +" FROM annotation_participant"
                +" INNER JOIN transcript_speaker"
                +" ON annotation_participant.speaker_number = transcript_speaker.speaker_number"
                +" AND annotation_participant.layer = '"+escape(attribute)+"'"
                +" WHERE transcript_speaker.ag_id = transcript.ag_id)");
            } // participant attribute
            else if (schema.getEpisodeLayerId().equals(layer.getParentId()))
            { // episode attribute
              conditions.append(
                "(SELECT DISTINCT annotated_by"
                +" FROM `annotation_layer_" + layer.get("@layer_id") + "` annotation"
                +" WHERE annotation.family_id = transcript.family_id)");
            } // episode attribute
            else
            { // regular temporal layer
              conditions.append(
                "(SELECT DISTINCT annotated_by"
                +" FROM annotation_layer_" + layer.get("@layer_id") + " annotation"
                +" WHERE annotation.ag_id = transcript.ag_id)");
            } // regular temporal layer
          } // valid layer
        }
        @Override public void exitOrdinalOperand(AGQLParser.OrdinalOperandContext ctx)
        {
          space();
          conditions.append("transcript.family_sequence");
        }
        @Override public void enterComparisonOperator(AGQLParser.ComparisonOperatorContext ctx)
        {
          space();
          String operator = ctx.operator.getText().trim();
          if (operator.equals("MATCHES")) operator = "REGEXP";
          if (operator.equals("NOT MATCHES")) operator = "NOT REGEXP";
          conditions.append(operator);
        }
        @Override public void exitLogicalOperator(AGQLParser.LogicalOperatorContext ctx)
        {
          space();
          conditions.append(ctx.operator.getText().trim());
        }
        @Override public void exitLiteralAtom(AGQLParser.LiteralAtomContext ctx)
        {
          space();
          try
          { // ensure string literals use single, not double, quotes
            conditions.append("'"+unquote(ctx.literal().stringLiteral().getText())+"'");
          }
          catch(Exception exception)
          { // not a string literal
            conditions.append(ctx.getText());
          }
        }
        @Override public void exitIdentifierAtom(AGQLParser.IdentifierAtomContext ctx)
        {
          space();
          conditions.append(ctx.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
          errors.add(node.getText());
        }
      };
    AGQLLexer lexer = new AGQLLexer(CharStreams.fromString(expression));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    AGQLParser parser = new AGQLParser(tokens);
    AGQLParser.QueryContext tree = parser.query();
    ParseTreeWalker.DEFAULT.walk(listener, tree);

    if (errors.size() > 0)
    {
      throw new AGQLException(expression, errors);
    }
    StringBuilder sql = new StringBuilder();
    sql.append("SELECT ");
    sql.append(sqlSelectClause);
    sql.append(" FROM transcript");
    if (conditions.length() > 0)
    {
      sql.append(" WHERE ");
      sql.append(conditions);
    }
    if (userWhereClause != null && userWhereClause.trim().length() > 0)
    {
      sql.append(conditions.length() > 0?" AND ":" WHERE ");
      sql.append(userWhereClause);
    }

    // now order clause
    StringBuilder order = new StringBuilder();
    for (String part : orderClause.split(","))
    {
      order.append(order.length() == 0?" ORDER BY":",");
      conditions.setLength(0);
      lexer.setInputStream(CharStreams.fromString(part));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.query();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      order.append(" ");
      order.append(conditions);
    } // next orderClause part
    sql.append(order);

    if (sqlLimitClause != null && sqlLimitClause.trim().length() > 0)
    {
      sql.append(" ");
      sql.append(sqlLimitClause);
    }

    q.sql = sql.toString();
    return q;
  } // end of sqlFor()

  /** 
   * Encapsulates the results of {@link #getSqlFor(String,String,String)} including the SQL
   * string and the parameters to set.
   */
  public static class Query
  {
    public String sql;
    public List<Object> parameters = new Vector<Object>();
    
    /**
     * Creates a prepared statement from the sql string and the parameters.
     * @param db A connection to the database.
     * @return A prepared statement with parameters set.
     * @throws SqlException
     */
    public PreparedStatement prepareStatement(Connection db)
      throws SQLException
    {
      PreparedStatement query = db.prepareStatement(sql);
      int p = 1;
      for (Object parameter : parameters)
      {
        if (parameter instanceof Integer) query.setInt(p++, (Integer)parameter);
        else if (parameter instanceof Double) query.setDouble(p++, (Double)parameter);
        else query.setString(p++, parameter.toString());
      } // next parameter
      return query;
    } // end of prepareStatement()
  }
} // end of class GraphAgqlToSql
