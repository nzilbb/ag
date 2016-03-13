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
package nzilbb.ag.util;

import java.util.Vector;
import java.util.regex.Pattern;
import nzilbb.ag.*;

/**
 * Transformer that breaks annotation labels on one layer (e.g. utterance) into token annotations on another layer (e.g. word), based on a character delimiter (space by default).
 * @author Robert Fromont robert@fromont.net.nz
 */

public class SimpleTokenizer
  implements IGraphTransformer
{
   // Attributes:
   
   /**
    * Regular expression to match delimiters for tokenization - default is <code>"[ \n\r\t]"</code>.
    * @see #getDelimiters()
    * @see #setDelimiters(String)
    */
   protected String delimiters;
   /**
    * Getter for {@link #delimiters}: Regular expression to match delimiters for tokenization.
    * @return Regular expression to match delimiters for tokenization.
    */
   public String getDelimiters() { if (delimiters == null) return "[ \n\r\t]"; return delimiters; }
   /**
    * Setter for {@link #delimiters}: Regular expression to match delimiters for tokenization.
    * @param newDelimiters Regular expression to match delimiters for tokenization.
    */
   public void setDelimiters(String newDelimiters) { delimiters = newDelimiters; }
   
   /**
    * Layer ID of the layer to tokenize.
    * @see #getSourceLayerId()
    * @see #setSourceLayerId(String)
    */
   protected String sourceLayerId;
   /**
    * Getter for {@link #sourceLayerId}: Layer ID of the layer to tokenize.
    * @return Layer ID of the layer to tokenize.
    */
   public String getSourceLayerId() { return sourceLayerId; }
   /**
    * Setter for {@link #sourceLayerId}: Layer ID of the layer to tokenize.
    * @param newSourceLayerId Layer ID of the layer to tokenize.
    */
   public void setSourceLayerId(String newSourceLayerId) { sourceLayerId = newSourceLayerId; }

   /**
    * Layer ID of the individual tokens.
    * @see #getDestinationLayerId()
    * @see #setDestinationLayerId(String)
    */
   protected String destinationLayerId;
   /**
    * Getter for {@link #destinationLayerId}: Layer ID of the individual tokens.
    * @return Layer ID of the individual tokens.
    */
   public String getDestinationLayerId() { return destinationLayerId; }
   /**
    * Setter for {@link #destinationLayerId}: Layer ID of the individual tokens.
    * @param newDestinationLayerId Layer ID of the individual tokens.
    */
   public void setDestinationLayerId(String newDestinationLayerId) { destinationLayerId = newDestinationLayerId; }
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public SimpleTokenizer()
   {
   } // end of constructor

   /**
    * Constructor from attribute values.
    * @param sourceLayerId Layer ID of the layer to tokenize.
    * @param destinationLayerId Layer ID of the individual tokens.
    * @param delimiters Regular expression to match delimiters for tokenization.
    */
   public SimpleTokenizer(String sourceLayerId, String destinationLayerId, String delimiters)
   {
      setSourceLayerId(sourceLayerId);
      setDestinationLayerId(destinationLayerId);
      setDelimiters(delimiters);
   } // end of constructor

   /**
    * Constructor from attribute values. Delimiters are the default for {@link #delimiters}.
    * @param sourceLayerId Layer ID of the layer to tokenize.
    * @param destinationLayerId Layer ID of the individual tokens.
    */
   public SimpleTokenizer(String sourceLayerId, String destinationLayerId)
   {
      setSourceLayerId(sourceLayerId);
      setDestinationLayerId(destinationLayerId);
   } // end of constructor

   /**
    * Transforms the graph.
    * @param graph The graph to transform.
    * @return The changes introduced by the tranformation.
    * @throws TransformationException If the transformation cannot be completed.
    */
   public Vector<Change> transform(Graph graph) throws TransformationException
   {
      if (graph.getLayer(getSourceLayerId()) == null) 
	 throw new TransformationException(this, "No source layer: " + getSourceLayerId());
      if (graph.getLayer(getDestinationLayerId()) == null) 
	 throw new TransformationException(this, "No destination layer: " + getDestinationLayerId());
      if (getDestinationLayerId().equals(getSourceLayerId())) 
	 throw new TransformationException(this, "Source and destination layer are the same: " + getDestinationLayerId());
      Vector<Change> changes = new Vector<Change>();
      boolean sharedParent = graph.getLayer(getSourceLayerId()).getParentId()
	 .equals(graph.getLayer(getDestinationLayerId()).getParentId());
      Pattern regexDelimiters = Pattern.compile(getDelimiters());
      for (Annotation source : new AnnotationsByAnchor(graph.getAnnotations(getSourceLayerId())))
      {
	 String[] tokens = regexDelimiters.split(source.getLabel());
	 Anchor start = source.getStart();
	 for (int t = 0; t < tokens.length; t++)
	 {
	    String sToken = tokens[t];
	    if (sToken.length() == 0) continue;
	    Anchor end = new Anchor();
	    if (t == tokens.length - 1)
	    { // last token
	       // use the source annotation's end instead
	       end = source.getEnd();
	    }
	    else
	    { 
	       // add the new end anchor to the graph
	       graph.addAnchor(end);
	       changes.addAll(end.getChanges());
	    }
	    Annotation token = new Annotation(
	       null, sToken, 
	       getDestinationLayerId(), 
	       start.getId(), end.getId(), 
	       sharedParent?source.getParentId():source.getId());
	    graph.addAnnotation(token);
	    changes.addAll(token.getChanges());

	    // the next annotation's start will be this one's end
	    start = end;
	 } // next token
      } // next source annotation
      return changes;
   }

} // end of class SimpleTokenizer
