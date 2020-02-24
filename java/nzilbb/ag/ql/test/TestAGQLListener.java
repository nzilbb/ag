//
// Copyright 2019-2020 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.ag.ql.test;

import org.junit.*;
import static org.junit.Assert.*;

import nzilbb.ag.ql.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class TestAGQLListener {
   @Test public void graphId() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            @Override public void exitMyMethodCall(AGQLParser.MyMethodCallContext ctx) {
               parse.append("layer:"+ctx.layer.quotedString.getText());
            }
            @Override public void exitLabelExpression(AGQLParser.LabelExpressionContext ctx) { 
               parse.append("->label");
            }
            @Override public void exitIdExpression(AGQLParser.IdExpressionContext ctx) { 
               parse.append("->id");
            }
            @Override public void exitGraphIdExpression(AGQLParser.GraphIdExpressionContext ctx) {
               parse.append("graph.id");
            }
            @Override public void visitErrorNode(ErrorNode node) {
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("my('graph').label = \"something\""));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "layer:'graph'->label", parse.toString());

      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString("my(\"graph\").id = 'something'"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "layer:\"graph\"->id", parse.toString());
    
      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString("graph.id = 'something'"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "graph.id", parse.toString());
    
      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString("graph.label = 'something'"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "graph.id", parse.toString());

   }

   @Test public void corpusLabel() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            @Override public void exitMyMethodCall(AGQLParser.MyMethodCallContext ctx) {
               parse.append("layer:"+ctx.layer.quotedString.getText());
            }
            @Override public void exitLabelExpression(AGQLParser.LabelExpressionContext ctx) { 
               parse.append("->label");
            }
            @Override public void visitErrorNode(ErrorNode node) {
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("my('corpus').label = \"something\""));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "layer:'corpus'->label", parse.toString());

   }

   @Test public void corpusLabels() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            @Override public void exitLabelsMethodCall(AGQLParser.LabelsMethodCallContext ctx) { 
               parse.append("layer:"+ctx.layer.quotedString.getText()+"->labels");
            }
            @Override public void visitErrorNode(ErrorNode node) {
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("labels('corpus').includes('something')"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "layer:'corpus'->labels", parse.toString());

   }

   @Test public void episodeLabel() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            @Override public void exitMyMethodCall(AGQLParser.MyMethodCallContext ctx) {
               parse.append("layer:"+ctx.layer.quotedString.getText());
            }
            @Override public void exitLabelExpression(AGQLParser.LabelExpressionContext ctx) { 
               parse.append("->label");
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("my('episode').label = \"something\""));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "layer:'episode'->label", parse.toString());

   }

   @Test public void whoLabel() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            @Override public void exitMyMethodCall(AGQLParser.MyMethodCallContext ctx) {
               parse.append("layer:"+ctx.layer.quotedString.getText());
            }
            @Override public void exitLabelExpression(AGQLParser.LabelExpressionContext ctx) { 
               parse.append("->label");
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("my('who').label = \"something\""));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "layer:'who'->label", parse.toString());

   }

   @Test public void whoLabels() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            @Override public void exitLabelsMethodCall(AGQLParser.LabelsMethodCallContext ctx) { 
               parse.append("layer:"+ctx.layer.quotedString.getText()+"->labels");
            }
            @Override public void visitErrorNode(ErrorNode node) {
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("labels('who').includes(\"something\")"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "layer:'who'->labels", parse.toString());

   }

   @Test public void anchorId() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            @Override public void exitAnchorIdOperand(AGQLParser.AnchorIdOperandContext ctx) {
               if (parse.length() > 0) parse.append(" ");
               parse.append(ctx.getText());
            }
            @Override public void visitErrorNode(ErrorNode node) {
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("start.id = end.id"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure - dot: " + parse,
                   "start.id end.id", parse.toString());

      parse.setLength(0);
      lexer = new AGQLLexer(
         CharStreams.fromString("startId = endId"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure - no dot: " + parse,
                   "startId endId", parse.toString());

   }
  
   @Test public void anchorOffset() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            // @Override public void exitEveryRule(ParserRuleContext ctx)
            // {
            //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
            // }
            @Override public void exitAnchorOffsetOperand(AGQLParser.AnchorOffsetOperandContext ctx) {
               if (parse.length() > 0) parse.append(" ");
               parse.append(ctx.getText());
            }
            @Override public void visitErrorNode(ErrorNode node) {
               // System.out.println(node.getText());
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("start.offset = end.offset"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "start.offset end.offset", parse.toString());

   }
  
   @Test public void layerId() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            @Override public void exitLayerExpression(AGQLParser.LayerExpressionContext ctx) { 
               if (parse.length() > 0) parse.append(" ");
               parse.append(ctx.getText());
            }
            @Override public void visitErrorNode(ErrorNode node) {
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("layer.id = layerId"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "layer.id layerId", parse.toString());

   }
  
   @Test public void parentId() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            @Override public void exitParentExpression(AGQLParser.ParentExpressionContext ctx) {
               if (parse.length() > 0) parse.append(" ");
               parse.append(ctx.getText());
            }
            @Override public void exitIdExpression(AGQLParser.IdExpressionContext ctx) { 
               parse.append("->id");
            }
            @Override public void exitParentIdExpression(AGQLParser.ParentIdExpressionContext ctx) { 
               if (parse.length() > 0) parse.append(" ");
               parse.append(ctx.getText());
            }
            @Override public void visitErrorNode(ErrorNode node) {
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("parent.id = parentId"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "parent->id parentId", parse.toString());

   }
  
   @Test public void label() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            @Override public void exitMyMethodCall(AGQLParser.MyMethodCallContext ctx) {
               parse.append("layer:"+ctx.layer.quotedString.getText());
            }
            @Override public void exitLabelExpression(AGQLParser.LabelExpressionContext ctx) { 
               parse.append("->label");
            }
            @Override public void visitErrorNode(ErrorNode node) {
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("my('transcript').label = 'something'"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "layer:'transcript'->label", parse.toString());

      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString("label = 'something'"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "->label", parse.toString());
   }
  
   @Test public void id() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            // @Override public void exitEveryRule(ParserRuleContext ctx)
            // {
            //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
            // }
            @Override public void exitMyMethodCall(AGQLParser.MyMethodCallContext ctx) {
               parse.append("layer:"+ctx.layer.quotedString.getText());
            }
            @Override public void exitIdExpression(AGQLParser.IdExpressionContext ctx) { 
               parse.append("->id");
            }
            @Override public void visitErrorNode(ErrorNode node) {
               // System.out.println("error: " + node.getText());
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("my('transcript').id = 'something'"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "layer:'transcript'->id", parse.toString());

      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString("/Ada.+/.test(id)"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "->id", parse.toString());    
   }

   @Test public void list() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            // @Override public void exitEveryRule(ParserRuleContext ctx)
            // {
            //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
            // }
            @Override public void exitListMethodCall(AGQLParser.ListMethodCallContext ctx) {
               parse.append("layer:"+ctx.layer.quotedString.getText()+"->list");
            }
            @Override public void visitErrorNode(ErrorNode node) {
               // System.out.println(node.getText());
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("list('transcript').includes('something')"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "layer:'transcript'->list", parse.toString());

   }

   @Test public void listLength() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            // @Override public void exitEveryRule(ParserRuleContext ctx)
            // {
            //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
            // }
            @Override public void exitListMethodCall(AGQLParser.ListMethodCallContext ctx) {
               parse.append("layer:"+ctx.layer.quotedString.getText()+"->list");
            }
            @Override public void exitLabelsMethodCall(AGQLParser.LabelsMethodCallContext ctx) { 
               parse.append("layer:"+ctx.layer.quotedString.getText()+"->labels");
            }
            @Override public void exitListLengthExpression(AGQLParser.ListLengthExpressionContext ctx) {
               parse.append("->length");
            }
            @Override public void visitErrorNode(ErrorNode node) {
               // System.out.println(node.getText());
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("list('transcript').length == 0"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "layer:'transcript'->list->length", parse.toString());

      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString("labels('transcript').length = 0"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "layer:'transcript'->labels->length", parse.toString());
   }
  
   @Test public void labels() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            // @Override public void exitEveryRule(ParserRuleContext ctx)
            // {
            //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
            // }
            @Override public void exitLabelsMethodCall(AGQLParser.LabelsMethodCallContext ctx) { 
               parse.append("layer:"+ctx.layer.quotedString.getText()+"->labels");
            }
            @Override public void visitErrorNode(ErrorNode node) {
               // System.out.println(node.getText());
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("labels('transcript').includes('something')"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "layer:'transcript'->labels", parse.toString());

   }

   @Test public void annotator() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            @Override public void exitMyMethodCall(AGQLParser.MyMethodCallContext ctx) {
               parse.append("layer:"+ctx.layer.quotedString.getText());
            }
            @Override public void exitAnnotatorExpression(AGQLParser.AnnotatorExpressionContext ctx) { 
               parse.append("->annotator");
            }
            @Override public void visitErrorNode(ErrorNode node) {
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("annotator = 'somebody'"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "->annotator", parse.toString());

      parse.setLength(0);
      lexer = new AGQLLexer(
         CharStreams.fromString("my('pos').annotator = 'somebody'"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "layer:'pos'->annotator", parse.toString());

   }

   @Test public void annotators() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            // @Override public void exitEveryRule(ParserRuleContext ctx)
            // {
            //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
            // }
            @Override public void exitAnnotatorsMethodCall(AGQLParser.AnnotatorsMethodCallContext ctx) { 
               parse.append("layer:"+ctx.layer.quotedString.getText()+"->annotators");
            }
            @Override public void visitErrorNode(ErrorNode node) {
               // System.out.println(node.getText());
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("annotators('transcript').includes('someone')"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "layer:'transcript'->annotators", parse.toString());

   }

   @Test public void ordinal() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            @Override public void exitMyMethodCall(AGQLParser.MyMethodCallContext ctx) {
               parse.append("layer:"+ctx.layer.quotedString.getText());
            }
            @Override public void exitOrdinalOperand(AGQLParser.OrdinalOperandContext ctx) {
               parse.append("->ordinal");
            }
            @Override public void visitErrorNode(ErrorNode node) {
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("ordinal = 1"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "->ordinal", parse.toString());

      parse.setLength(0);
      lexer = new AGQLLexer(
         CharStreams.fromString("my('transcript').ordinal = 1"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "layer:'transcript'->ordinal", parse.toString());

   }

   @Test public void when() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            // @Override public void exitEveryRule(ParserRuleContext ctx)
            // {
            //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
            // }
            @Override public void exitMyMethodCall(AGQLParser.MyMethodCallContext ctx) {
               parse.append("layer:"+ctx.layer.quotedString.getText());
            }
            @Override public void exitWhenExpression(AGQLParser.WhenExpressionContext ctx) { 
               parse.append("->when");
            }
            @Override public void visitErrorNode(ErrorNode node) {
               // System.out.println("ERROR: " + node.getText());
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("when < '2018-01-01 00:00:00'"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "->when", parse.toString());

      parse.setLength(0);
      lexer = new AGQLLexer(
         CharStreams.fromString("my('transcript').when < '2018-01-01 00:00:00'"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("No errors: " + error.toString(), error.length() == 0);
      assertEquals("Parse structure: " + parse,
                   "layer:'transcript'->when", parse.toString());

   }

   @Test public void logicalOperatorsSQLStyle() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            // @Override public void exitEveryRule(ParserRuleContext ctx)
            // {
            //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
            // }
            @Override public void exitLogicalOperator(AGQLParser.LogicalOperatorContext ctx) {
               if (ctx.AND() != null)
               {
                  parse.append(" AND ");
               }
               else if (ctx.OR() != null)
               {
                  parse.append(" OR ");
               }
               else
               {
                  parse.append(ctx.operator.getText());
               }
            }
            @Override public void visitErrorNode(ErrorNode node) {
               // System.out.println("ERROR: " + node.getText());
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("/Ada.+/.test(id) AND my('corpus').label = 'CC'"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("AND: No errors: " + error.toString(), error.length() == 0);
      assertEquals("AND: Parse structure: " + parse,
                   " AND ", parse.toString());

      parse.setLength(0);
      lexer.setInputStream(
         CharStreams.fromString("!/'Ada.+'/.test(id) OR my('corpus').label = 'CC'"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("OR: No errors: " + error.toString(), error.length() == 0);
      assertEquals("OR: Parse structure: " + parse,
                   " OR ", parse.toString());
    
      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString(
                              "!/Ada.+/.test(id)"
                              +" AND my('corpus').label = 'CC'"
                              +" AND !annotators('transcript_rating').includes('labbcat')"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("Chaining: No errors: " + error.toString(), error.length() == 0);
      assertEquals("Chaining: Parse structure: " + parse,
                   " AND  AND ", parse.toString());
   }

   @Test public void logicalOperatorsJSStyle() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            // @Override public void exitEveryRule(ParserRuleContext ctx)
            // {
            //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
            // }
            @Override public void exitLogicalOperator(AGQLParser.LogicalOperatorContext ctx) {
               if (ctx.AND() != null)
               {
                  parse.append(" AND ");
               }
               else if (ctx.OR() != null)
               {
                  parse.append(" OR ");
               }
               else
               {
                  parse.append(ctx.operator.getText());
               }
            }
            @Override public void visitErrorNode(ErrorNode node) {
               // System.out.println("ERROR: " + node.getText());
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("!/Ada.+/.test(id) && my('corpus').label = 'CC'"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("AND: No errors: " + error.toString(), error.length() == 0);
      assertEquals("AND: Parse structure: " + parse,
                   " AND ", parse.toString());

      parse.setLength(0);
      lexer.setInputStream(
         CharStreams.fromString("!/Ada.+/.test(id) || my('corpus').label = 'CC'"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("OR: No errors: " + error.toString(), error.length() == 0);
      assertEquals("OR: Parse structure: " + parse,
                   " OR ", parse.toString());
    
      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString(
                              "!/Ada.+/.test(id)"
                              +" && my('corpus').label = 'CC'"
                              +" && !annotators('transcript_rating').includes('labbcat')"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("Chaining: No errors: " + error.toString(), error.length() == 0);
      assertEquals("Chaining: Parse structure: " + parse,
                   " AND  AND ", parse.toString());
   }

   @Test public void comparisonOperators() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            // @Override public void exitEveryRule(ParserRuleContext ctx)
            // {
            //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
            // }
            @Override public void exitComparisonOperator(AGQLParser.ComparisonOperatorContext ctx) {
               parse.append(ctx.operator.getText().trim());
            }
            @Override public void visitErrorNode(ErrorNode node) {
               // System.out.println("ERROR: " + node.getText());
               error.append(node.getText());
            }
         };

      String[] operators = {"=", "==", "<>", "<", ">", "<=", ">=" };

      for (String operator : operators) {
         parse.setLength(0);
         AGQLLexer lexer = new AGQLLexer(CharStreams.fromString("this "+operator+" that"));
         CommonTokenStream tokens = new CommonTokenStream(lexer);
         AGQLParser parser = new AGQLParser(tokens);
         AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();
         ParseTreeWalker.DEFAULT.walk(listener, tree);
         assertTrue(operator + ": No errors: " + error.toString(), error.length() == 0);
         assertEquals(operator + ": Parse structure: " + parse,
                      operator, parse.toString());
      } // next operator
   }

   @Test public void includesExpression() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            // @Override public void exitEveryRule(ParserRuleContext ctx)
            // {
            //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
            // }
            @Override public void exitIncludesExpression(AGQLParser.IncludesExpressionContext ctx) {
               parse.append(ctx.singletonOperand.getText()
                            + (ctx.negation!=null?" NOT IN ":" IN ")
                            + ctx.listOperand.getText());
            }
            @Override public void visitErrorNode(ErrorNode node) {
               // System.out.println("ERROR: " + node.getText());
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("'something' IN labels('corpus')"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();
    
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("IN: No errors: " + error.toString(), error.length() == 0);
      assertEquals("IN: Parse structure: " + parse,
                   "'something' IN labels('corpus')", parse.toString());

      parse.setLength(0);
      lexer.setInputStream(
         CharStreams.fromString("labels('corpus').includes('something')"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("INCLUDES: No errors: " + error.toString(), error.length() == 0);
      assertEquals("INCLUDES: Parse structure: " + parse,
                   "'something' IN labels('corpus')", parse.toString());

      parse.setLength(0);
      lexer.setInputStream(
         CharStreams.fromString("'something' NOT IN labels('corpus')"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("NEGATED IN: No errors: " + error.toString(), error.length() == 0);
      assertEquals("NEGATED  IN: Parse structure: " + parse,
                   "'something' NOT IN labels('corpus')", parse.toString());

      parse.setLength(0);
      lexer.setInputStream(
         CharStreams.fromString("!labels('corpus').includes('something')"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("NEGATED INCLUDES: No errors: " + error.toString(), error.length() == 0);
      assertEquals("NEGATED INCLUDES: Parse structure: " + parse,
                   "'something' NOT IN labels('corpus')", parse.toString());
   }

   @Test public void patternMatchExpression() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            // @Override public void exitEveryRule(ParserRuleContext ctx)
            // {
            //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
            // }
            @Override public void exitPatternMatchExpression(AGQLParser.PatternMatchExpressionContext ctx) {
               parse.append(ctx.singletonOperand.getText()
                            + (ctx.negation!=null?" NOT REGEXP ":" REGEXP ")
                            + ctx.patternOperand.getText());
            }
            @Override public void visitErrorNode(ErrorNode node) {
               // System.out.println("ERROR: " + node.getText());
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("'something' MATCHES 'regexp'"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();
    
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("MATCHES: No errors: " + error.toString(), error.length() == 0);
      assertEquals("MATCHES: Parse structure: " + parse,
                   "'something' REGEXP 'regexp'", parse.toString());

      parse.setLength(0);
      lexer.setInputStream(
         CharStreams.fromString("/regexp/.test('something')"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("TEST: No errors: " + error.toString(), error.length() == 0);
      assertEquals("TEST: Parse structure: " + parse,
                   "'something' REGEXP /regexp/", parse.toString());

      parse.setLength(0);
      lexer.setInputStream(
         CharStreams.fromString("'something' NOT MATCHES 'regexp'"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("NEGATED MATCHES: No errors: " + error.toString(), error.length() == 0);
      assertEquals("NEGATED MATCHES: Parse structure: " + parse,
                   "'something' NOT REGEXP 'regexp'", parse.toString());

      parse.setLength(0);
      lexer.setInputStream(
         CharStreams.fromString("!/regexp/.test('something')"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("NEGATED TEST: No errors: " + error.toString(), error.length() == 0);
      assertEquals("NEGATED TEST: Parse structure: " + parse,
                   "'something' NOT REGEXP /regexp/", parse.toString());
   }

   @Test public void atomicExpressions() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            // @Override public void exitEveryRule(ParserRuleContext ctx)
            // {
            //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
            // }
            @Override public void exitBarePredicate(AGQLParser.BarePredicateContext ctx) {
               parse.append(ctx.getText());
            }
            @Override public void visitErrorNode(ErrorNode node) {
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("'something'"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("String literal - No errors: " + error.toString(), error.length() == 0);
      assertEquals("String literal: " + parse,
                   "'something'", parse.toString());
    
      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString("something"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("Identifier - No errors: " + error.toString(), error.length() == 0);
      assertEquals("Identifier: " + parse,
                   "something", parse.toString());

      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString("label"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("Label - No errors: " + error.toString(), error.length() == 0);
      assertEquals("Label: " + parse,
                   "label", parse.toString());
    
      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString("id"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("ID - No errors: " + error.toString(), error.length() == 0);
      assertEquals("ID: " + parse,
                   "id", parse.toString());
    
   }

   @Test public void listExpressions() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            // @Override public void exitEveryRule(ParserRuleContext ctx)
            // {
            //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
            // }
            @Override public void exitAtomListExpression(AGQLParser.AtomListExpressionContext ctx) {
               parse.append(ctx.getText());
            }
            @Override public void visitErrorNode(ErrorNode node) {
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("('something').includes('x')"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("Single literal - No errors: " + error.toString(), error.length() == 0);
      assertEquals("Single literal: " + parse,
                   "('something')", parse.toString());
    
      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString("('foo', 'bar').includes('x')"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("Two strings - No errors: " + error.toString(), error.length() == 0);
      assertEquals("Two strings: " + parse,
                   "('foo','bar')", parse.toString());

      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString("(1, 2.2, 'three', something).includes('x')"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("Various things - No errors: " + error.toString(), error.length() == 0);
      assertEquals("Various things: " + parse,
                   "(1,2.2,'three',something)", parse.toString());
    
      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString("['something'].includes('x')"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("JS Array - Single literal - No errors: " + error.toString(),
                 error.length() == 0);
      assertEquals("JS Array - Single literal: " + parse,
                   "['something']", parse.toString());

      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString("['foo', 'bar'].includes('x')"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("JS Array - Two strings - No errors: " + error.toString(), error.length() == 0);
      assertEquals("JS Array - Two strings: " + parse,
                   "['foo','bar']", parse.toString());

      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString("[1, 2.2, 'three', something].includes('x')"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("JS Array - Various things - No errors: " + error.toString(),
                 error.length() == 0);
      assertEquals("JS Array - Various things: " + parse,
                   "[1,2.2,'three',something]", parse.toString());
    
   }

   @Test public void orderEpressions() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            // @Override public void exitEveryRule(ParserRuleContext ctx)
            // {
            //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
            // }
            @Override public void exitAscendingOrderExpression(AGQLParser.AscendingOrderExpressionContext ctx) {
               parse.append(" ascending " + ctx.order.getText());
            }
            @Override public void exitDescendingOrderExpression(AGQLParser.DescendingOrderExpressionContext ctx) {
               parse.append(" descending " + ctx.order.getText());
            }
            @Override public void visitErrorNode(ErrorNode node) {
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("id ASC"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.OrderListExpressionContext tree = parser.orderListExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("ASC - No errors: " + error.toString(), error.length() == 0);
      assertEquals("ASC: " + parse,
                   " ascending id", parse.toString());
    
      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString("my('episode').label DESC"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.orderListExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("DESC - No errors: " + error.toString(), error.length() == 0);
      assertEquals("DESC: " + parse,
                   " descending my('episode').label", parse.toString());

      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString("my('episode').label ASC, ordinal DESC"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.orderListExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("DESC - No errors: " + error.toString(), error.length() == 0);
      assertEquals("DESC: " + parse,
                   " ascending my('episode').label descending ordinal", parse.toString());

      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString("label"));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.orderListExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("Bare operand - Error: " + error.toString(), error.length() > 0);
      assertEquals("Bare operand not an order expression: " + parse,
                   "", parse.toString());
    
   }

   @Test public void emptyString() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            // @Override public void exitEveryRule(ParserRuleContext ctx)
            // {
            //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
            // }
            @Override public void exitBarePredicate(AGQLParser.BarePredicateContext ctx) {
               parse.append(ctx.getText());
            }
            @Override public void visitErrorNode(ErrorNode node) {
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("''"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("Single quoted - No errors: " + error.toString(), error.length() == 0);
      assertEquals("Single quoted: " + parse,
                   "''", parse.toString());
    
      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString("\"\""));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("Double quoted - No errors: " + error.toString(), error.length() == 0);
      assertEquals("Double quoted: " + parse,
                   "\"\"", parse.toString());
   }

   @Test public void quoteEscaping() {
      final StringBuffer parse = new StringBuffer();
      final StringBuffer error = new StringBuffer();
      AGQLListener listener = new AGQLBaseListener() {
            // @Override public void exitEveryRule(ParserRuleContext ctx)
            // {
            //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
            // }
            @Override public void exitBarePredicate(AGQLParser.BarePredicateContext ctx) {
               parse.append(ctx.getText());
            }
            @Override public void visitErrorNode(ErrorNode node) {
               error.append(node.getText());
            }
         };

      AGQLLexer lexer = new AGQLLexer(
         CharStreams.fromString("'O\\'Reilly'"));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("Single quote escape - No errors: " + error.toString(), error.length() == 0);
      assertEquals("Single quote escape: " + parse,
                   "'O\\'Reilly'", parse.toString());
    
      parse.setLength(0);
      lexer.setInputStream(CharStreams.fromString("\"\\\"quoted\\\"\""));
      tokens = new CommonTokenStream(lexer);
      parser = new AGQLParser(tokens);
      tree = parser.booleanExpression();
      ParseTreeWalker.DEFAULT.walk(listener, tree);
      assertTrue("Double quote escape - No errors: " + error.toString(), error.length() == 0);
      assertEquals("Double quote escape: " + parse,
                   "\"\\\"quoted\\\"\"", parse.toString());

   }

   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.ag.ql.test.TestAGQLListener");
   }

}
