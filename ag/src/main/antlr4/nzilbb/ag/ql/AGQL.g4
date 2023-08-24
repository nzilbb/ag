/**
 * Annotation Graph Query Language grammar.
 * Copyright 2015-2023 New Zealand Institute of Language, Brain and Behaviour, 
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

/* Parser rules: */

agqlExpression
  : booleanExpression EOF
  | orderListExpression EOF
  | EOF
  ;

orderListExpression : orderExpression (COMMA orderExpression)* ;

orderExpression
  : order=operand ASC                                  # AscendingOrderExpression
  | order=operand DESC                                 # DescendingOrderExpression
  ;

booleanExpression
  : booleanExpression logicalOperator predicate        # CompositeExpression
  | predicate                                          # PredicateExpression
  ;

predicate
  : operand comparisonOperator operand                 # ComparisonPredicate
  | includesExpression                                 # IncludesPredicate
  | includesAnyExpression                              # IncludesAnyPredicate
  | patternMatchExpression                             # PatterMatchPredicate
  | operand                                            # BarePredicate
  ;

includesExpression
  : singletonOperand=operand negation=NOT? IN listOperand=listExpression
  | negation=NOT? listOperand=listExpression DOT INCLUDES OPEN_PAREN singletonOperand=operand CLOSE_PAREN
  ; 

includesAnyExpression
  : negation=NOT? leftOperand=listExpression DOT INCLUDESANY OPEN_PAREN rightOperand=listExpression CLOSE_PAREN
  ; 

patternMatchExpression
  : singletonOperand=operand negation=NOT? MATCHES patternOperand=stringLiteral
  /* ad-hockery: operand for pattern match can also be a list, meaning 'any member matches'*/
  | negation=NOT? patternOperand=stringLiteral DOT TEST OPEN_PAREN listOperand=listExpression CLOSE_PAREN
  | negation=NOT? patternOperand=stringLiteral DOT TEST OPEN_PAREN singletonOperand=operand CLOSE_PAREN
  ; 

operand
  : ordinalExpression          # OrdinalOperand
  | labelExpression            # LabelOperand
  | annotatorExpression        # AnnotatorOperand
  | whenExpression             # WhenOperand
  | layerExpression            # LayerOperand
  | idExpression               # IdOperand
  | parentIdExpression         # ParentIdOperand
  | confidenceExpression       # ConfidenceOperand
  | anchorIdExpression         # AnchorIdOperand
  | anchorOffsetExpression     # AnchorOffsetOperand
  | anchorConfidenceExpression # AnchorConfidenceOperand
  | graphIdExpression          # GraphIdOperand
  | listLengthExpression       # ListLengthOperand
  | coalesceExpression         # CoalesceOperand
  | atom                       # AtomOperand
  ;

coalesceParameter
  : ordinalExpression          # CoalesceOrdinalOperand
  | labelExpression            # CoalesceLabelOperand
  | annotatorExpression        # CoalesceAnnotatorOperand
  | whenExpression             # CoalesceWhenOperand
  | layerExpression            # CoalesceLayerOperand
  | idExpression               # CoalesceIdOperand
  | parentIdExpression         # CoalesceParentIdOperand
  | confidenceExpression       # CoalesceConfidenceOperand
  | anchorIdExpression         # CoalesceAnchorIdOperand
  | anchorOffsetExpression     # CoalesceAnchorOffsetOperand
  | anchorConfidenceExpression # CoalesceAnchorConfidenceOperand
  | graphIdExpression          # CoalesceGraphIdOperand
  | listLengthExpression       # CoalesceListLengthOperand
  | atom                       # CoalesceAtomOperand
  ;
  
coalesceExpression
  : COALESCE OPEN_PAREN leftOperand=coalesceParameter COMMA rightOperand=coalesceParameter CLOSE_PAREN
  | leftOperand=coalesceParameter COALESCING rightOperand=coalesceParameter
  ;

listExpression
  : valueListExpression
  | annotationListExpression
  | atomListExpression
  ;

valueListExpression
  : labelsMethodCall
  | annotatorsMethodCall
  ;

annotationListExpression
  : allMethodCall
  ;

listLengthExpression
  : listExpression DOT LENGTH
  ;

annotationExpression
  : firstMethodCall
  | parentExpression
  | nextExpression
  | previousExpression
  ;

idExpression
  : other=annotationExpression DOT ID
  | ID                               
  ;

ordinalExpression
  : other=annotationExpression DOT ORDINAL
  | ORDINAL                               
  ;

labelExpression
  : other=annotationExpression DOT LABEL
  | LABEL                               
  ;

annotatorExpression
  : other=annotationExpression DOT ANNOTATOR
  | ANNOTATOR                               
  ;

whenExpression
  : other=annotationExpression DOT WHEN
  | WHEN                               
  ;

layerExpression
  : other=annotationExpression DOT LAYER                     
  | LAYER                                              
  | other=annotationExpression DOT LAYERID                   
  | LAYERID                                            
  | other=annotationExpression DOT LAYER DOT ID              
  | LAYER DOT ID                                       
  ;

parentExpression
  : PARENT                               
  /*: other=annotationExpression DOT PARENT*/
  ;

nextExpression 
  : NEXT                               
  /*: other=annotationExpression DOT NEXT*/
  ;

previousExpression 
  : PREVIOUS                               
  /*: other=annotationExpression DOT PREVIOUSx*/
  ;

parentIdExpression
  : other=annotationExpression DOT PARENTID
  | PARENTID                               
  ;

confidenceExpression
  : other=annotationExpression DOT CONFIDENCE
  | CONFIDENCE                               
  ;

anchorExpression
  : other=annotationExpression DOT START
  | START                               
  | other=annotationExpression DOT END        
  | END                                 
  ;

anchorIdExpression
  : other=annotationExpression DOT STARTID
  | STARTID                               
  | other=annotationExpression DOT ENDID
  | ENDID                               
  | anchorExpression DOT ID             
  ;

anchorOffsetExpression : anchorExpression DOT OFFSET ;
anchorConfidenceExpression : anchorExpression DOT CONFIDENCE ;

firstMethodCall
  : FIRST OPEN_PAREN layer=stringLiteral CLOSE_PAREN
/*  | annotationExpression DOT FIRST OPEN_PAREN stringLiteral CLOSE_PAREN # OtherFirstMethodCall*/
  ;

allMethodCall
  : ALL OPEN_PAREN layer=stringLiteral CLOSE_PAREN
  | other=annotationExpression ALL OPEN_PAREN stringLiteral CLOSE_PAREN
  ;

labelsMethodCall
  : LABELS OPEN_PAREN layer=stringLiteral CLOSE_PAREN
  | other=annotationExpression LABELS OPEN_PAREN layer=stringLiteral CLOSE_PAREN
  ;

annotatorsMethodCall
  : ANNOTATORS OPEN_PAREN layer=stringLiteral CLOSE_PAREN
  | other=annotationExpression ANNOTATORS OPEN_PAREN layer=stringLiteral CLOSE_PAREN
  ;

graphIdExpression
  : GRAPH DOT LABEL
  | GRAPH DOT ID
  ;
    
atom
  : literal                                            # LiteralAtom
  | IDENTIFIER                                         # IdentifierAtom
  ;

atomListExpression
  : OPEN_PAREN firstAtom (COMMA subsequentAtom)* CLOSE_PAREN
  | OPEN_SQUARE_PAREN firstAtom (COMMA subsequentAtom)* CLOSE_SQUARE_PAREN
  ;

firstAtom : atom ;
subsequentAtom : atom ;

literal
  : stringLiteral
  | INTEGER_LITERAL
  | NUMBER_LITERAL
  ;

stringLiteral
  : quotedString=DOUBLE_QUOTED_STRING
  | quotedString=SINGLE_QUOTED_STRING
  | quotedString=SLASH_QUOTED_STRING
  ;

comparisonOperator
  : operator=EQ
  | operator=NE
  | operator=LT
  | operator=GT
  | operator=LTE
  | operator=GTE
  ;

logicalOperator
  : operator=AND
  | operator=OR
  ;

/* Lexer rules: */

/* special variables */
GRAPH                 : 'graph' ;

/* attributes */
ID                    : 'id' ;
ORDINAL               : 'ordinal' ;
LABEL                 : 'label' ;
ANNOTATOR             : 'annotator' ;
WHEN                  : 'when' ;
LENGTH                : 'length' ;
LAYER                 : 'layer' ;
LAYERID               : 'layerId' ;
PARENT                : 'parent' ;
PARENTID              : 'parentId' ;
NEXT                  : 'next' ;
PREVIOUS              : 'previous' ;
CONFIDENCE            : 'confidence' ;
START                 : 'start' ;
END                   : 'end' ;
STARTID               : 'startId' ;
ENDID                 : 'endId' ;
OFFSET                : 'offset' ;

/* methods */
FIRST                 : 'first' | 'my' ; /* 'my' for backward compatibility */
ALL                   : 'all' | 'list' ; /* 'list; for backward compatibility */
LABELS                : 'labels' ;
ANNOTATORS            : 'annotators' ;
INCLUDES              : 'includes' ;
INCLUDESANY           : 'includesAny' ;
TEST                  : 'test' ;

/* other stuff */
ASC                   : 'ASC' ;
DESC                  : 'DESC' ;
DOT                   : '.' ;
COMMA                 : ',' ;
IDENTIFIER            : [a-zA-Z][a-zA-Z0-9_]* ;
AND                   : ' AND ' | '&&' ;
OR                    : ' OR '  | '||';
EQ                    : '=' | '==' ;
NE                    : '<>' | '≠';
MATCHES               : ' MATCHES ' ;
LT                    : '<' ;
GT                    : '>' ;
LTE                   : '<=' | '≤';
GTE                   : '>=' | '≥' ;
IN                    : ' IN ' ;
OPEN_PAREN            : '(' ;
CLOSE_PAREN           : ')' ;
OPEN_SQUARE_PAREN     : '[' ;
CLOSE_SQUARE_PAREN    : ']' ;
DOUBLE_QUOTED_STRING  : '"' (~'"')* ('\\"' (~'"')*)* '"';
SINGLE_QUOTED_STRING  : '\'' (~'\'')* ('\\\'' (~'\'')*)* '\'';
SLASH_QUOTED_STRING  : '/' (~'/')* ('\\/' (~'/')*)* '/';
INTEGER_LITERAL       : '-'? [0-9]+ ;
NUMBER_LITERAL        : '-'? [0-9]+ '.' [0-9]+ ; 
SLASH                 : '/' ;
NOT                   : ' NOT' | '!' ;
COALESCE              : 'COALESCE' ;
COALESCING            : '??' ;

/* ignore white space */
WS : [ \n\t\r]+ -> channel(HIDDEN);
