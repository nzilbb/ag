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

package nzilbb.ag.ql.test;

import org.junit.*;
import static org.junit.Assert.*;

import nzilbb.ag.ql.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class TestAGQLListener
{
  @Test public void graphId() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        @Override public void exitGraphIdExpression(AGQLParser.GraphIdExpressionContext ctx)
        {
          parse.append(ctx.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
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
                 "my('graph').label", parse.toString());

    parse.setLength(0);
    lexer.setInputStream(CharStreams.fromString("my(\"graph\").id = 'something'"));
    tokens = new CommonTokenStream(lexer);
    parser = new AGQLParser(tokens);
    tree = parser.booleanExpression();
    ParseTreeWalker.DEFAULT.walk(listener, tree);
    assertTrue("No errors: " + error.toString(), error.length() == 0);
    assertEquals("Parse structure: " + parse,
                 "my(\"graph\").id", parse.toString());
    
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
                 "graph.label", parse.toString());

  }

  @Test public void corpusLabel() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        @Override public void exitCorpusLabelExpression(AGQLParser.CorpusLabelExpressionContext ctx)
        {
          parse.append(ctx.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
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
                 "my('corpus').label", parse.toString());

  }

  @Test public void corpusLabels() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        @Override public void exitCorpusLabelsExpression(AGQLParser.CorpusLabelsExpressionContext ctx)
        {
          parse.append(ctx.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
          error.append(node.getText());
        }
      };

    AGQLLexer lexer = new AGQLLexer(
      CharStreams.fromString("'something' IN labels('corpus')"));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    AGQLParser parser = new AGQLParser(tokens);
    AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

    ParseTreeWalker.DEFAULT.walk(listener, tree);
    assertTrue("No errors: " + error.toString(), error.length() == 0);
    assertEquals("Parse structure: " + parse,
                 "labels('corpus')", parse.toString());

  }

  @Test public void episodeLabel() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        @Override public void exitEpisodeLabelExpression(AGQLParser.EpisodeLabelExpressionContext ctx)
        {
          parse.append(ctx.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
          error.append(node.getText());
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
                 "my('episode').label", parse.toString());

  }

  @Test public void whoLabel() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        @Override public void exitWhoLabelExpression(AGQLParser.WhoLabelExpressionContext ctx)
        {
          parse.append(ctx.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
          error.append(node.getText());
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
                 "my('who').label", parse.toString());

  }

  @Test public void whoLabels() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        @Override public void exitWhoLabelsExpression(AGQLParser.WhoLabelsExpressionContext ctx)
        {
          parse.append(ctx.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
          error.append(node.getText());
        }
      };

    AGQLLexer lexer = new AGQLLexer(
      CharStreams.fromString("\"something\" IN labels('who')"));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    AGQLParser parser = new AGQLParser(tokens);
    AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

    ParseTreeWalker.DEFAULT.walk(listener, tree);
    assertTrue("No errors: " + error.toString(), error.length() == 0);
    assertEquals("Parse structure: " + parse,
                 "labels('who')", parse.toString());

  }

  @Test public void anchorId() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        @Override public void exitAnchorIdOperand(AGQLParser.AnchorIdOperandContext ctx)
        {
          if (parse.length() > 0) parse.append(" ");
          parse.append(ctx.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
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
    assertEquals("Parse structure: " + parse,
                 "start.id end.id", parse.toString());

  }
  
  @Test public void anchorOffset() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        // @Override public void exitEveryRule(ParserRuleContext ctx)
        // {
        //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
        // }
        @Override public void exitAnchorOffsetOperand(AGQLParser.AnchorOffsetOperandContext ctx)
        {
          if (parse.length() > 0) parse.append(" ");
          parse.append(ctx.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
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
  
  @Test public void label() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        @Override public void exitOtherLabelExpression(AGQLParser.OtherLabelExpressionContext ctx)
        {
          parse.append("other: " + ctx.stringLiteral().quotedString.getText());
        }
        @Override public void exitThisLabelExpression(AGQLParser.ThisLabelExpressionContext ctx)
        {
          parse.append(ctx.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
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
                 "other: 'transcript'", parse.toString());

    parse.setLength(0);
    lexer.setInputStream(CharStreams.fromString("label = 'something'"));
    tokens = new CommonTokenStream(lexer);
    parser = new AGQLParser(tokens);
    tree = parser.booleanExpression();
    ParseTreeWalker.DEFAULT.walk(listener, tree);
    assertTrue("No errors: " + error.toString(), error.length() == 0);
    assertEquals("Parse structure: " + parse,
                 "label", parse.toString());
  }
  
  @Test public void id() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        // @Override public void exitEveryRule(ParserRuleContext ctx)
        // {
        //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
        // }
        @Override public void exitOtherIdExpression(AGQLParser.OtherIdExpressionContext ctx)
        {
          parse.append("other: ");
          parse.append(ctx.stringLiteral().quotedString.getText());
        }
        @Override public void exitThisIdExpression(AGQLParser.ThisIdExpressionContext ctx)
        {
          parse.append(ctx.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
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
                 "other: 'transcript'", parse.toString());

    parse.setLength(0);
    // TODO for some reason the parser prints:
    // TODO "line 1:31 mismatched input '<EOF>' expecting '.'"
    // TODO ...to stderr with this expression - no errors are raised, but need to figure out why
    lexer.setInputStream(CharStreams.fromString("id NOT MATCHES \"Ada.+\""));
    tokens = new CommonTokenStream(lexer);
    parser = new AGQLParser(tokens);
    tree = parser.booleanExpression();
    ParseTreeWalker.DEFAULT.walk(listener, tree);
    assertTrue("No errors: " + error.toString(), error.length() == 0);
    assertEquals("Parse structure: " + parse,
                 "id", parse.toString());    
  }
  
  @Test public void list() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        // @Override public void exitEveryRule(ParserRuleContext ctx)
        // {
        //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
        // }
        @Override public void exitListExpression(AGQLParser.ListExpressionContext ctx)
        {
          parse.append(ctx.stringLiteral().quotedString.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
          // System.out.println(node.getText());
          error.append(node.getText());
        }
      };

    AGQLLexer lexer = new AGQLLexer(
      CharStreams.fromString("list('transcript') = 'something'"));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    AGQLParser parser = new AGQLParser(tokens);
    AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

    ParseTreeWalker.DEFAULT.walk(listener, tree);
    assertTrue("No errors: " + error.toString(), error.length() == 0);
    assertEquals("Parse structure: " + parse,
                 "'transcript'", parse.toString());

  }

  @Test public void listLength() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        // @Override public void exitEveryRule(ParserRuleContext ctx)
        // {
        //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
        // }
        @Override public void exitListLengthExpression(AGQLParser.ListLengthExpressionContext ctx)
        {
          parse.append(ctx.stringLiteral().quotedString.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
          // System.out.println(node.getText());
          error.append(node.getText());
        }
      };

    AGQLLexer lexer = new AGQLLexer(
      CharStreams.fromString("list('transcript').length = 0"));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    AGQLParser parser = new AGQLParser(tokens);
    AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

    ParseTreeWalker.DEFAULT.walk(listener, tree);
    assertTrue("No errors: " + error.toString(), error.length() == 0);
    assertEquals("Parse structure: " + parse,
                 "'transcript'", parse.toString());

    parse.setLength(0);
    lexer.setInputStream(CharStreams.fromString("labels('transcript').length = 0"));
    tokens = new CommonTokenStream(lexer);
    parser = new AGQLParser(tokens);
    tree = parser.booleanExpression();
    ParseTreeWalker.DEFAULT.walk(listener, tree);
    assertTrue("No errors: " + error.toString(), error.length() == 0);
    assertEquals("Parse structure: " + parse,
                 "'transcript'", parse.toString());
  }
  
  @Test public void labels() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        // @Override public void exitEveryRule(ParserRuleContext ctx)
        // {
        //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
        // }
        @Override public void exitLabelsExpression(AGQLParser.LabelsExpressionContext ctx)
        {
          parse.append(ctx.stringLiteral().quotedString.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
          // System.out.println(node.getText());
          error.append(node.getText());
        }
      };

    AGQLLexer lexer = new AGQLLexer(
      CharStreams.fromString("'something' IN labels('transcript')"));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    AGQLParser parser = new AGQLParser(tokens);
    AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

    ParseTreeWalker.DEFAULT.walk(listener, tree);
    assertTrue("No errors: " + error.toString(), error.length() == 0);
    assertEquals("Parse structure: " + parse,
                 "'transcript'", parse.toString());

  }

  @Test public void annotator() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        @Override public void exitAnnotatorOperand(AGQLParser.AnnotatorOperandContext ctx)
        {
          parse.append(ctx.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
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
                 "annotator", parse.toString());

  }

  @Test public void annotators() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        // @Override public void exitEveryRule(ParserRuleContext ctx)
        // {
        //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
        // }
        @Override public void exitAnnotatorsExpression(AGQLParser.AnnotatorsExpressionContext ctx)
        {
          parse.append(ctx.stringLiteral().quotedString.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
          // System.out.println(node.getText());
          error.append(node.getText());
        }
      };

    AGQLLexer lexer = new AGQLLexer(
      CharStreams.fromString("'someone' IN annotators('transcript')"));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    AGQLParser parser = new AGQLParser(tokens);
    AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

    ParseTreeWalker.DEFAULT.walk(listener, tree);
    assertTrue("No errors: " + error.toString(), error.length() == 0);
    assertEquals("Parse structure: " + parse,
                 "'transcript'", parse.toString());

  }

  @Test public void ordinal() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        @Override public void exitOrdinalOperand(AGQLParser.OrdinalOperandContext ctx)
        {
          parse.append(ctx.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
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
                 "ordinal", parse.toString());

  }

  @Test public void when() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        // @Override public void exitEveryRule(ParserRuleContext ctx)
        // {
        //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
        // }
        @Override public void exitWhenOperand(AGQLParser.WhenOperandContext ctx)
        {
          parse.append(ctx.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
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
                 "when", parse.toString());

  }

  @Test public void logicalOperators() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        // @Override public void exitEveryRule(ParserRuleContext ctx)
        // {
        //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
        // }
        @Override public void exitLogicalOperator(AGQLParser.LogicalOperatorContext ctx)
        {
          parse.append(ctx.operator.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
          // System.out.println("ERROR: " + node.getText());
          error.append(node.getText());
        }
      };

    AGQLLexer lexer = new AGQLLexer(
      CharStreams.fromString("id NOT MATCHES 'Ada.+' AND my('corpus').label = 'CC'"));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    AGQLParser parser = new AGQLParser(tokens);
    AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

    ParseTreeWalker.DEFAULT.walk(listener, tree);
    assertTrue("AND: No errors: " + error.toString(), error.length() == 0);
    assertEquals("AND: Parse structure: " + parse,
                 " AND ", parse.toString());

    parse.setLength(0);
    lexer.setInputStream(
      CharStreams.fromString("id NOT MATCHES 'Ada.+' OR my('corpus').label = 'CC'"));
    tokens = new CommonTokenStream(lexer);
    parser = new AGQLParser(tokens);
    tree = parser.booleanExpression();
    ParseTreeWalker.DEFAULT.walk(listener, tree);
    assertTrue("OR: No errors: " + error.toString(), error.length() == 0);
    assertEquals("OR: Parse structure: " + parse,
                 " OR ", parse.toString());
    
    parse.setLength(0);
    lexer.setInputStream(CharStreams.fromString(
                           "id NOT MATCHES 'Ada.+'"
                           +" AND my('corpus').label = 'CC'"
                           +" AND 'labbcat' NOT IN annotators('transcript_rating')"));
    tokens = new CommonTokenStream(lexer);
    parser = new AGQLParser(tokens);
    tree = parser.booleanExpression();
    ParseTreeWalker.DEFAULT.walk(listener, tree);
    assertTrue("Chaining: No errors: " + error.toString(), error.length() == 0);
    assertEquals("Chaining: Parse structure: " + parse,
                 " AND  AND ", parse.toString());
  }

  @Test public void comparisonOperators() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        // @Override public void exitEveryRule(ParserRuleContext ctx)
        // {
        //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
        // }
        @Override public void exitComparisonOperator(AGQLParser.ComparisonOperatorContext ctx)
        {
          parse.append(ctx.operator.getText().trim());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
          // System.out.println("ERROR: " + node.getText());
          error.append(node.getText());
        }
      };

    String[] operators =
      {"=", "<>", "MATCHES", "NOT MATCHES", "<", ">", "<=", ">=", "IN", "NOT IN" };

    for (String operator : operators)
    {
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

  @Test public void atomicExpressions() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        // @Override public void exitEveryRule(ParserRuleContext ctx)
        // {
        //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
        // }
        @Override public void exitBarePredicate(AGQLParser.BarePredicateContext ctx)
        {
          parse.append(ctx.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
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

  @Test public void listExpressions() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        // @Override public void exitEveryRule(ParserRuleContext ctx)
        // {
        //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
        // }
        @Override public void exitAtomListOperand(AGQLParser.AtomListOperandContext ctx)
        {
          parse.append(ctx.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
          error.append(node.getText());
        }
      };

    AGQLLexer lexer = new AGQLLexer(
      CharStreams.fromString("('something')"));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    AGQLParser parser = new AGQLParser(tokens);
    AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();

    ParseTreeWalker.DEFAULT.walk(listener, tree);
    assertTrue("Single literal - No errors: " + error.toString(), error.length() == 0);
    assertEquals("Single literal: " + parse,
                 "('something')", parse.toString());
    
    parse.setLength(0);
    lexer.setInputStream(CharStreams.fromString("('foo', 'bar')"));
    tokens = new CommonTokenStream(lexer);
    parser = new AGQLParser(tokens);
    tree = parser.booleanExpression();
    ParseTreeWalker.DEFAULT.walk(listener, tree);
    assertTrue("Two strings - No errors: " + error.toString(), error.length() == 0);
    assertEquals("Two strings: " + parse,
                 "('foo','bar')", parse.toString());

    parse.setLength(0);
    lexer.setInputStream(CharStreams.fromString("(1, 2.2, 'three', something)"));
    tokens = new CommonTokenStream(lexer);
    parser = new AGQLParser(tokens);
    tree = parser.booleanExpression();
    ParseTreeWalker.DEFAULT.walk(listener, tree);
    assertTrue("Various things - No errors: " + error.toString(), error.length() == 0);
    assertEquals("Various things: " + parse,
                 "(1,2.2,'three',something)", parse.toString());
    
  }

  @Test public void orderEpressions() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        // @Override public void exitEveryRule(ParserRuleContext ctx)
        // {
        //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
        // }
        @Override public void exitAscendingOrderExpression(AGQLParser.AscendingOrderExpressionContext ctx)
        {
          parse.append(" ascending " + ctx.order.getText());
        }
        @Override public void exitDescendingOrderExpression(AGQLParser.DescendingOrderExpressionContext ctx)
        {
          parse.append(" descending " + ctx.order.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
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

  @Test public void emptyString() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        // @Override public void exitEveryRule(ParserRuleContext ctx)
        // {
        //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
        // }
        @Override public void exitBarePredicate(AGQLParser.BarePredicateContext ctx)
        {
          parse.append(ctx.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
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

  @Test public void quoteEscaping() 
  {
    final StringBuffer parse = new StringBuffer();
    final StringBuffer error = new StringBuffer();
    AGQLListener listener = new AGQLBaseListener() {
        // @Override public void exitEveryRule(ParserRuleContext ctx)
        // {
        //   System.out.println(ctx.getClass().getSimpleName() + ": " + ctx.getText());
        // }
        @Override public void exitBarePredicate(AGQLParser.BarePredicateContext ctx)
        {
          parse.append(ctx.getText());
        }
        @Override public void visitErrorNode(ErrorNode node)
        {
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

  public static void main(String args[]) 
  {
    org.junit.runner.JUnitCore.main("nzilbb.ag.ql.test.TestAGQLListener");
  }

}
