//
// Copyright 2016 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of LaBB-CAT.
//
//    LaBB-CAT is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    LaBB-CAT is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with LaBB-CAT; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//

package nzilbb.ag.util;

import java.util.Vector;
import nzilbb.ag.*;

/**
 * Transformer that joins isolated punctuation (and other non-orthographic) 'words' to the
 * preceding (or following, if first) word.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class OrthographyClumper implements IGraphTransformer
{
   // Attributes
   
   /**
    * A regular expression that identifies a non-orthographic character - the default is <code>"[^\\p{javaLetter}\\p{javaDigit}]"</code>
    * @see #getNonOrthoCharacterPattern()
    * @see #setNonOrthoCharacterPattern(String)
    */
   protected String nonOrthoCharacterPattern = "[^\\p{javaLetter}\\p{javaDigit}]";
   /**
    * Getter for {@link #nonOrthoCharacterPattern}: A regular expression that identifies a non-orthographic character.
    * @return A regular expression that identifies a non-orthographic character.
    */
   public String getNonOrthoCharacterPattern() { return nonOrthoCharacterPattern; }
   /**
    * Setter for {@link #nonOrthoCharacterPattern}: A regular expression that identifies a non-orthographic character.
    * @param sNewNonOrthoCharacterPattern A regular expression that identifies a non-orthographic character.
    */
   public void setNonOrthoCharacterPattern(String sNewNonOrthoCharacterPattern) { nonOrthoCharacterPattern = sNewNonOrthoCharacterPattern; }
   
   /**
    * ID of the layer to transform.
    * @see #getWordLayerId()
    * @see #setWordLayerId(String)
    */
   protected String wordLayerId;
   /**
    * Getter for {@link #wordLayerId}: ID of the layer to transform.
    * @return ID of the layer to transform.
    */
   public String getWordLayerId() { return wordLayerId; }
   /**
    * Setter for {@link #wordLayerId}: ID of the layer to transform.
    * @param newWordLayerId ID of the layer to transform.
    */
   public void setWordLayerId(String newWordLayerId) { wordLayerId = newWordLayerId; }
   
   // Methods

   /** 
    * Default constructor.
    */
   public OrthographyClumper()
   {
   }

   /** 
    * Constructor.
    * @param wordLayerId ID of the layer to transform.
    */
   public OrthographyClumper(String wordLayerId)
   {
      setWordLayerId(wordLayerId);
   }
   
   /**
    * Transforms the graph.
    * @param graph The graph to transform.
    * @return The changes introduced by the tranformation.
    * @throws TransformationException If the transformation cannot be completed.
    */
   public Vector<Change> transform(Graph graph) throws TransformationException
   {
      Layer wordLayer = graph.getLayer(getWordLayerId());
      if (wordLayer == null) 
	 throw new TransformationException(this, "No layer: " + getWordLayerId());
      Vector<Change> changes = new Vector<Change>();
      // for each parent
      for (Annotation parent : graph.list(wordLayer.getParentId()))
      {
	 Annotation last = null;
	 Annotation toPrepend = null;
	 Anchor prependEnd = null;
	 for (Annotation token : parent.getAnnotations(getWordLayerId()))
	 {
	    if (token.getLabel().replaceAll(nonOrthoCharacterPattern, "").length() == 0)
	    {
	       changes.add( // register change of:
		  token.destroy());
	       // System.out.println("to remove " + token);
	       if (last != null 
		   // if there are no intervening annotations or gaps
		   && token.getStart() == last.getEnd())
	       {
		  changes.addAll( // register change of:
		     last.setLabel(last.getLabel() + " " + token.getLabel()));
		  changes.addAll( // register change of:
		     last.setEnd(token.getEnd()));

		  // move childen to last token
		  for (String childLayerId : token.getAnnotations().keySet())
		  {
		     for (Annotation child : token.getAnnotations().get(childLayerId))
		     {
			changes.addAll( // register change of:
			   child.setParent(last));
		     }
		  }
		  
	       } // last != null
	       else
	       { // this is the first token, or anchors aren't shared with the last
		  if (toPrepend == null) 
		  { // prepend this to the next real word
		     toPrepend = token;
		  }
		  else
		  { // already something to prepend
		     // if there are no intervening annotations or gaps
		     if (toPrepend.getEnd() == token.getStart())
		     { // add this to what's already to be prepended
			changes.addAll( // register change of:
			   toPrepend.setLabel(toPrepend.getLabel() + " " + token.getLabel()));
			changes.addAll( // register change of:
			   toPrepend.setEnd(token.getEnd()));

			// move childen to token
			for (String childLayerId : token.getAnnotations().keySet())
			{
			   for (Annotation child : token.getAnnotations().get(childLayerId))
			   {
			      changes.addAll( // register change of:
				 child.setParent(toPrepend));
			   }
			}

		     }
		     else
		     { // there's a gap or intervening annotation
			// so what we were going to prepend before can't be prepended to what follows
			toPrepend.rollback(); // TODO remove the change
			// System.out.println("unremoving " + token);
			last = toPrepend;
			// but maybe we can prepend this to what follows...
			toPrepend = token;
		     } 
		  }
	       } // this is the first token
	    } // non-orthographic 'word'
	    else
	    { // orthographic 'word'
	       if (toPrepend != null)
	       { // something to prepend
		  if (toPrepend.getEnd() == token.getStart())
		  { // no intevening annotations/pauses
		     changes.addAll( // register change of:
			token.setLabel(toPrepend.getLabel() + " " + token.getLabel()));
		     changes.addAll( // register change of:
			token.setStart(toPrepend.getStart()));
		     changes.addAll( // register change of:
			token.setOrdinal(toPrepend.getOrdinal()));

		     // move childen to last token
		     for (String childLayerId : toPrepend.getAnnotations().keySet())
		     {
			for (Annotation child : toPrepend.getAnnotations().get(childLayerId))
			{
			   changes.addAll( // register change of:
			      child.setParent(token));
			}
		     }
		  }
		  else
		  { // we're going to keep toPrepend in the end, so don't delete it
		     toPrepend.rollback(); // TODO remove the change
		  }
		  toPrepend = null;
	       } // something to prepend
	       else if (last != null)
	       {
		  changes.addAll( // register change of:
		     token.setOrdinal(last.getOrdinal() + 1));
	       }
	       last = token;
	    } //orthographic 'word'
	 } // next token
	 
	 if (toPrepend != null)
	 { // something to prepend, but we haven't prepended it yet
	    // so don't delete it
	    toPrepend.rollback(); // TODO remove the change
	    // System.out.println("unremoving final " + toPrepend);
	 }

      } // next utterance
      return changes;
   }
   
   
} // end of class IGraphTransformer
