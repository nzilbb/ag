//
// Copyright 2016-2025 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;
import nzilbb.ag.*;

/**
 * Transformer that joins isolated punctuation (and other non-orthographic) 'words' to the
 * preceding (or following, if first) word.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class OrthographyClumper implements GraphTransformer {
  // Attributes
   
  /**
   * A regular expression that identifies a non-orthographic character - the default is
   * <code>"[\\p{Punct}&amp;&amp;[^_]]"</code> 
   * @see #getNonOrthoCharacterPattern()
   * @see #setNonOrthoCharacterPattern(String)
   */
  protected String nonOrthoCharacterPattern = "[\\p{Punct}&&[^_]]";
  /**
   * Getter for {@link #nonOrthoCharacterPattern}: A regular expression that identifies a
   * non-orthographic character. 
   * @return A regular expression that identifies a non-orthographic character.
   */
  public String getNonOrthoCharacterPattern() { return nonOrthoCharacterPattern; }
  /**
   * Setter for {@link #nonOrthoCharacterPattern}: A regular expression that identifies a
   * non-orthographic character. 
   * @param sNewNonOrthoCharacterPattern A regular expression that identifies a
   * non-orthographic character. 
   */
  public OrthographyClumper setNonOrthoCharacterPattern(String sNewNonOrthoCharacterPattern) { nonOrthoCharacterPattern = sNewNonOrthoCharacterPattern; return this; }
  
  /**
   * A regular expression that identifies characters that should be clumped to the
   * following rather than previous word, e.g. opening parentheses. 
   * Default value is <q>[({\[&lt;]</q>.
   * @see #getClumpForwardPattern()
   * @see #setClumpForwardPattern(String)
   */
  protected String clumpForwardPattern = "[({\\[<]";
  /**
   * Getter for {@link #clumpForwardPattern}: A regular expression that identifies
   * characters that should be clumped to the following rather than previous word,
   * e.g. opening parentheses. 
   * Default value is <q>[({\[&lt;]</q>.
   * @return A regular expression that identifies characters that should be clumped to the
   * following rather than previous word, e.g. opening parentheses. 
   */
  public String getClumpForwardPattern() { return clumpForwardPattern; }
  /**
   * Setter for {@link #clumpForwardPattern}: A regular expression that identifies
   * characters that should be clumped to the following rather than previous word,
   * e.g. opening parentheses. 
   * @param newClumpForwardPattern A regular expression that identifies characters that
   * should be clumped to the following rather than previous word, e.g. opening
   * parentheses. 
   */
  public OrthographyClumper setClumpForwardPattern(String newClumpForwardPattern) { clumpForwardPattern = newClumpForwardPattern; return this; }
  
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
  public OrthographyClumper setWordLayerId(String newWordLayerId) { wordLayerId = newWordLayerId; return this; }
   
  /**
   * ID of a partition layer, such that words can't be clumped across partitions.
   * @see #getPartitionLayerId()
   * @see #setPartitionLayerId(String)
   */
  protected String partitionLayerId;
  /**
   * Getter for {@link #partitionLayerId}: ID of a partition layer, such that words can't
   * be clumped across partitions. 
   * @return ID of a partition layer, such that words can't be clumped across partitions.
   */
  public String getPartitionLayerId() { return partitionLayerId; }
  /**
   * Setter for {@link #partitionLayerId}: ID of a partition layer, such that words can't
   * be clumped across partitions. 
   * @param newPartitionLayerId ID of a partition layer, such that words can't be clumped
   * across partitions. 
   */
  public OrthographyClumper setPartitionLayerId(String newPartitionLayerId) { partitionLayerId = newPartitionLayerId; return this; }

  // Methods

  /** 
   * Default constructor.
   */
  public OrthographyClumper() {
  }

  /** 
   * Constructor.
   * @param wordLayerId ID of the layer to transform.
   */
  public OrthographyClumper(String wordLayerId) {
    setWordLayerId(wordLayerId);
  }

  /** 
   * Constructor.
   * @param wordLayerId ID of the layer to transform.
   * @param partitionLayerId ID of a partition layer, such that words can't be clumped across partitions.
   */
  public OrthographyClumper(String wordLayerId, String partitionLayerId) {
    setWordLayerId(wordLayerId);
    setPartitionLayerId(partitionLayerId);
  }

  /**
   * Transforms the graph.
   * @param graph The graph to transform.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph transform(Graph graph) throws TransformationException {
    Layer wordLayer = graph.getLayer(getWordLayerId());
    if (wordLayer == null) 
      throw new TransformationException(this, "No layer: " + getWordLayerId());
    Pattern clumpForward = null;
    if (clumpForwardPattern != null && clumpForwardPattern.length() > 0) {
      try {
       clumpForward = Pattern.compile(clumpForwardPattern);
      } catch(Exception exception) {
        throw new TransformationException(this, exception);
      }
    }

    // for each parent
    for (Annotation parent : graph.all(wordLayer.getParentId())) {
      Annotation last = null;
      Annotation toPrepend = null;
      Anchor prependEnd = null;
	 
      // we'll build a new list of children in the right order
      Vector<Annotation> newChildren = new Vector<Annotation>();
      boolean changedOrdinals = false;

      List<Annotation> tokens = new Vector<Annotation>(parent.getAnnotations(getWordLayerId()));
      for (Annotation token : tokens) {
        if (token.getLabel().replaceAll(nonOrthoCharacterPattern, "").length() == 0) {
          // mark the token for destruction (we might change our minds)
          token.put("@toDestroy", Boolean.TRUE);
          if (last != null // not the first token
              // not already prepending
              && toPrepend == null 
              // if there are no intervening annotations or gaps
              && token.getStart() == last.getEnd()
              // no partition annotations ending here
              && (partitionLayerId == null || !token.getStart().isEndOn(partitionLayerId))
              // not a forward-clumping token
              && (clumpForward == null || !clumpForward.matcher(token.getLabel()).matches())) {
            
            last.setLabel(last.getLabel() + " " + token.getLabel());

            Anchor oldEnd = last.getEnd();
            // move all annotations that end at last.end to token.end
            oldEnd.moveEndingAnnotations(token.getEnd());		  
            // move all annotations that start at last.end to token.end
            oldEnd.moveStartingAnnotations(token.getEnd());

            // move childen to last token
            for (String childLayerId : token.getAnnotations().keySet()) {
              for (Annotation child
                     : new Vector<Annotation>(token.getAnnotations().get(childLayerId))) {
                child.setParent(last);
              }
            }
		  
          } else { // this is the first token, or anchors aren't shared with the last
            if (toPrepend == null) { // prepend this to the next real word
              toPrepend = token;
            } else { // already something to prepend
              // if there are no intervening annotations or gaps
              if (toPrepend.getEnd() == token.getStart()
                  // no partition annotations ending here
                  && (partitionLayerId == null || !token.getStart().isEndOn(partitionLayerId))) {
                // add this to what's already to be prepended
                toPrepend.setLabel(toPrepend.getLabel() + " " + token.getLabel());
                Anchor oldEnd = toPrepend.getEnd();
                // move all annotations that end at last.end to token.end
                oldEnd.moveEndingAnnotations(token.getEnd());			
                // move all annotations that start at last.end to token.end
                oldEnd.moveStartingAnnotations(token.getEnd());

                // move childen to token
                for (String childLayerId : token.getAnnotations().keySet()) {
                  for (Annotation child
                         : new Vector<Annotation>(token.getAnnotations().get(childLayerId))) {
                    child.setParent(toPrepend);
                  }
                }
              } else { // there's a gap or intervening annotation
                // so what we were going to prepend before can't be prepended to what follows
                // unmark the token for destruction
                token.remove("@toDestroy");
                last = toPrepend;
                // but maybe we can prepend this to what follows...
                toPrepend = token;
              } 
            }
          } // this is the first token
        } else { // orthographic 'word'
          if (toPrepend != null) { // something to prepend
            if (toPrepend.getEnd() == token.getStart() // no intervening annotations/pauses
                // a partition isn't starting
                && (partitionLayerId == null || !token.getStart().isEndOn(partitionLayerId))) { 
              token.setLabel(toPrepend.getLabel() + " " + token.getLabel());
              // don't change the ordinal yet, as it will move peers
              // - we'll just correct the whole lot in one pass at the end
              token.put("@newOrdinal", toPrepend.getOrdinal());
              changedOrdinals = true;
              
              Anchor oldStart = token.getStart();
              // move all annotations that end at last.end to token.end
              oldStart.moveEndingAnnotations(toPrepend.getStart());		     
              // move all annotations that start at last.end to token.end
              oldStart.moveStartingAnnotations(toPrepend.getStart());
		     
              // move childen to last token
              for (String childLayerId : toPrepend.getAnnotations().keySet()) {
                for (Annotation child
                       : new Vector<Annotation>(toPrepend.getAnnotations().get(childLayerId))) {
                  child.setParent(token);
                }
              }
            } else { // we're going to keep toPrepend in the end, so don't delete it
              // unmark the token for destruction
              toPrepend.remove("@toDestroy");
            }
            toPrepend = null;
          } else if (last != null) {
            token.put("@newOrdinal", last.getOrdinal() + 1);
            changedOrdinals = true;
          }
          last = token;
        } //orthographic 'word'
      } // next token

      if (toPrepend != null) { // something to prepend, but we haven't prepended it yet
        // there's more than one token
        if (last != null
            // and there's no intervening gap
            && toPrepend.getStart() == last.getEnd()
            // and no partition annotations ending here
            && (partitionLayerId == null || !toPrepend.getStart().isEndOn(partitionLayerId))) {
          // can just append to last token
          last.setLabel(last.getLabel() + " " + toPrepend);
        } else { // no last token
          // so don't delete it
          // unmark the token for destruction
          toPrepend.remove("@toDestroy");
        }
      }

      // now really mark tokens for destruction, and correct ordinals as we go
      int o = 1;
      for (Annotation token : tokens) {
        if (token.containsKey("@toDestroy")) {
          // destroy but don't cascade ordinal corrections, we're not finished yet
          token.bulkDestroy(); 
          token.remove("@toDestroy");
        } else {
          // prevent parent.correctOrdinals
          token.setParent(null);
          // set and increment ordinal
          token.setOrdinal(o++);
          // re-set parent
          token.setParent(parent, false);
        }
      }
    } // next utterance
    return graph;
  }   
   
} // end of class OrthographyClumper
