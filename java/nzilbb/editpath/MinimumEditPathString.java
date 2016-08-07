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
package nzilbb.editpath;

import java.util.List;
import java.util.Vector;

/**
 * Utility subclass of MinimumEditPath for handling Strings as sequences of Characters
 * @author Robert Fromont robert@fromont.net.nz
 */

public class MinimumEditPathString
   extends MinimumEditPath<Character>
{
   // Attributes:
   
   // Methods:
   
   /**
    * Default constructor
    */
   public MinimumEditPathString()
   {
      super();
   } // end of constructor

   /**
    * Constructor
    * @param comparator Element comparator to use.
    */
   public MinimumEditPathString(IEditComparator<Character> comparator)
   {
      super(comparator);
   } // end of constructor

   /**
    * Computes the minimum path from one sequence to another.
    * @param sFrom
    * @param sTo
    * @return The edit path between the two sequences that has the minimum edit distance.
    */
   public List<EditStep<Character>> minimumEditPath(String sFrom, String sTo)
   {
      Vector<Character> from = new Vector<Character>();
      for (char c : sFrom.toCharArray()) from.add(new Character(c));
      Vector<Character> to = new Vector<Character>();
      for (char c : sTo.toCharArray()) to.add(new Character(c));
      return minimumEditPath(from, to);
   }
   /**
    * Computes the minimum edit distance between two sequences.
    * @param sFrom
    * @param sTo
    * @return The minimum edit distance between the two sequences.
    */
   public int minimumEditDistance(String sFrom, String sTo)
   {
      List<EditStep<Character>> path = minimumEditPath(sFrom, sTo);
      if (path.size() > 0)
      {
	 return path.get(path.size() - 1).totalDistance();
      }
      else
      { // empty path
	 return 0;
      }
   } // end of minimumDistance()

   /**
    * Computes the minimum edit distance between two sequences.
    * <p>Equivalent to <code>minimumEditDistance(sFrom, sTo)</code>
    * @param sFrom
    * @param sTo
    * @return The minimum edit distance between the two sequences.
    */
   public int levenshteinDistance(String sFrom, String sTo)
   {
      return minimumEditDistance(sFrom, sTo);
   } // end of minimumDistance()

   /**
    * Computes the minimum edit distance between two strings.
    * @param sFrom
    * @param sTo
    * @return The minimum edit distance between the two strings.
    */
   public static int LevenshteinDistance(String sFrom, String sTo)
   {
      return new MinimumEditPathString().levenshteinDistance(sFrom, sTo);
   } // end of minimumDistance()   
   
   /**
    * Prints the edit path, by displaying the from string on one line and the to string on the next line, vertically aligned to indicate changes, and marking inserts/deletes with a mid-dot '·' (a character unlikely to really appear in a string, so will likely unambiguously indicate a lack in one of the strings).
    <p> Each line is prefixed, and the second appended, with "\r\n" so that vertical alignment is visually more likely regardless of the surrounding context or the platform.
    * @param path
    * @return A string representation of the edit path between two strings.
    */
   public static String printPath(List<EditStep<Character>> path)
   {
      StringBuilder sFrom = new StringBuilder();
      sFrom.append("\r\n");
      StringBuilder sTo = new StringBuilder();
      sTo.append("\r\n");
      for (EditStep<Character> step : path)
      {
	 char c = '·';
	 if (step.getFrom() != null) c = step.getFrom().charValue();
	 sFrom.append(c);
	 c = '·';
	 if (step.getTo() != null) c = step.getTo().charValue();
	 sTo.append(c);
      } // next step
      sFrom.append(sTo);
      sFrom.append("\r\n");
      return sFrom.toString();
   } // end of printPath()


} // end of class MinimumEditPathString
