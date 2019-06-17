/**
 * Annotation Graph Query Language grammar.
 * Copyright 2015-2019 New Zealand Institute of Language, Brain and Behaviour, 
 * University of Canterbury
 * Written by Robert Fromont - robert.fromont@canterbury.ac.nz
 *
 *    This file is part of nzilbb.ag.
 *
 *    nzilbb.ag is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    nzilbb.ag is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with nzilbb.ag; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
grammar AGQL;

@header {
package nzilbb.ag.ql;
}

/* Parser rules: */

query : booleanExpression EOF;

booleanExpression
  : booleanExpression logicalOperator predicate        # CompositeExpression
  | predicate                                          # PredicateExpression
  ;

predicate
  : operand comparisonOperator operand                 # ComparisonPredicate
  | operand                                            # BarePredicate
  ;

operand
  : graphIdExpression                                  # GraphIdOperand
  | corpusLabelExpression                              # CorpusLabelOperand
  | corpusLabelsExpression                             # CorpusLabelsOperand
  | episodeLabelExpression                             # EpisodeLabelOperand
  | whoExpression                                      # WhoOperand
  | anchorIdExpression                                 # AnchorIdOperand
  | anchorOffsetExpression                             # AnchorOffsetOperand
  | labelExpression                                    # LabelOperand
  | labelsExpression                                   # LabelsOperand
  | idExpression                                       # IdOperand
  | listExpression                                     # ListOperand
  | listLengthExpression                               # ListLengthOperand
  | annotatorsExpression                               # AnnotatorsOperand
  | ORDINAL                                            # OrdinalOperand
  | ANNOTATOR                                          # AnnotatorOperand
  | WHEN                                               # WhenOperand
  | attribute                                          # AttributeOperand
  | method                                             # MethodOperand
  | atom                                               # AtomOperand
  ;

attribute : atom DOT IDENTIFIER;

graphIdExpression
  : MY OPEN_PAREN GRAPH_LITERAL CLOSE_PAREN DOT LABEL
  | MY OPEN_PAREN GRAPH_LITERAL CLOSE_PAREN DOT ID
  | GRAPH DOT LABEL
  | GRAPH DOT ID
  ;

corpusLabelExpression : MY OPEN_PAREN CORPUS_LITERAL CLOSE_PAREN DOT LABEL ;
corpusLabelsExpression : LABELS OPEN_PAREN CORPUS_LITERAL CLOSE_PAREN DOT LABEL ;
episodeLabelExpression : MY OPEN_PAREN EPISODE_LITERAL CLOSE_PAREN DOT LABEL ;
whoExpression
  : LABELS OPEN_PAREN WHO_LITERAL CLOSE_PAREN          # WhoLabelsExpression
  | MY OPEN_PAREN WHO_LITERAL CLOSE_PAREN DOT LABEL    # WhoLabelExpression
  ;
anchorIdExpression
  : START DOT ID                                       # StartIdExpression
  | END DOT ID                                         # EndIdExpression
  ;
anchorOffsetExpression
  : START DOT OFFSET                                   # StartOffsetExpression
  | END DOT OFFSET                                     # EndOffsetExpression
  ;
labelExpression
  : MY OPEN_PAREN stringLiteral CLOSE_PAREN DOT LABEL  # OtherLabelExpression
  | LABEL                                              # ThisLabelExpression
  ;
idExpression
  : MY OPEN_PAREN stringLiteral CLOSE_PAREN DOT ID     # OtherIdExpression
  | ID                                                 # ThisIdExpression
  ;
listExpression : LIST OPEN_PAREN stringLiteral CLOSE_PAREN ;
listLengthExpression
  : LIST OPEN_PAREN stringLiteral CLOSE_PAREN DOT LENGTH
  | LABELS OPEN_PAREN stringLiteral CLOSE_PAREN DOT LENGTH
  ;
labelsExpression : LABELS OPEN_PAREN stringLiteral CLOSE_PAREN ;
annotatorsExpression : ANNOTATORS OPEN_PAREN stringLiteral CLOSE_PAREN ;
    
method
  : atom DOT IDENTIFIER OPEN_PAREN CLOSE_PAREN         # MethodNoArgs 
  | atom DOT IDENTIFIER OPEN_PAREN arglist CLOSE_PAREN # MethodArgs 
  ;

arglist : operand (COMMA atom)* ;

atom
  : literal                   # LiteralAtom
  | IDENTIFIER                # IdentifierAtom
  ;

literal
  : stringLiteral
  | INTEGER_LITERAL
  | NUMBER_LITERAL
  ;

stringLiteral
  : quotedString=DOUBLE_QUOTED_STRING
  | quotedString=SINGLE_QUOTED_STRING
  ;

comparisonOperator
  : operator=EQ
  | operator=NE
  | operator=MATCHES
  | operator=NOT_MATCHES
  | operator=LT
  | operator=GT
  | operator=LTE
  | operator=GTE
  | operator=IN
  | operator=NOT_IN
  ;

logicalOperator
  : operator=AND
  | operator=OR
  ;

/* Lexer rules: */

/* special layers */
WHO_LITERAL           : '"who"' | '\'who\'' ;
GRAPH_LITERAL         : '"graph"' | '\'graph\'' ;
CORPUS_LITERAL        : '"corpus"' | '\'corpus\'' ;
EPISODE_LITERAL       : '"episode"' | '\'episode\'' ;

/* special variables */
GRAPH                 : 'graph' ;

/* attributes */
ID                    : 'id' ;
ORDINAL               : 'ordinal' ;
LABEL                 : 'label' ;
START                 : 'start' ;
END                   : 'end' ;
OFFSET                : 'offset' ;
ANNOTATOR             : 'annotator' ;
WHEN                  : 'when' ;
LENGTH                : 'length' ;

/* methods */
MY                    : 'my' ;
LIST                  : 'list' ;
LABELS                : 'labels' ;
ANNOTATORS            : 'annotators' ;

/* other stuff */
DOT                   : '.' ;
COMMA                 : ',' ;
IDENTIFIER            : [a-zA-Z][a-zA-Z0-9_]* ;
AND                   : ' AND ' ;
OR                    : ' OR ' ;
EQ                    : '=' ;
NE                    : '<>' ;
MATCHES               : ' MATCHES ' ;
NOT_MATCHES           : ' NOT MATCHES ' ;
LT                    : '<' ;
GT                    : '>' ;
LTE                   : '<=' ;
GTE                   : '>=' ;
IN                    : ' IN ' ;
NOT_IN                : ' NOT IN ' ;
OPEN_PAREN            : '(' ;
CLOSE_PAREN           : ')' ;
DOUBLE_QUOTED_STRING  : '"' (~'"')* ('\\"' (~'"')*)* '"';
SINGLE_QUOTED_STRING  : '\'' (~'\'')* ('\\\'' (~'\'')*)* '\'';
INTEGER_LITERAL       : '-'? [0-9]+ ;
NUMBER_LITERAL        : '-'? [0-9]+ '.' [0-9]+ ; 

/* ignore white space */
WS : [ \n\t\r]+ -> channel(HIDDEN);
