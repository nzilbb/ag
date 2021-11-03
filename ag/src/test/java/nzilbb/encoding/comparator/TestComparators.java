//
// Copyright 2018-2021 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.encoding.comparator;
	      
import org.junit.*;
import static org.junit.Assert.*;
import java.util.Vector;
import java.util.List;
import java.util.StringTokenizer;
import nzilbb.editpath.*;

/** Test encoding-based edit-path comparators */
public class TestComparators {
  
  @Test public void DISCToDISCMapping() {
    DISC2DISCComparator comparator = new DISC2DISCComparator();
    MinimumEditPath<String> mp = new MinimumEditPath<String>(comparator);
    String from = "dIf@rHt";
    String to = "dIfr@nt";
    List<EditStep<String>> path = mp.minimumEditPath(stringToVector(from), stringToVector(to));
    //System.out.println(pathToString(path));
    assertEquals(pathToString(path), 8, path.size());
    int i = 0;
    assertEquals(pathToString(path), "d", path.get(i).getFrom());
    assertEquals(pathToString(path), "d", path.get(i++).getTo());
    assertEquals(pathToString(path), "I", path.get(i).getFrom());
    assertEquals(pathToString(path), "I", path.get(i++).getTo());
    assertEquals(pathToString(path), "f", path.get(i).getFrom());
    assertEquals(pathToString(path), "f", path.get(i++).getTo());
    assertEquals(pathToString(path), "@", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo()); 
    assertEquals(pathToString(path), "r", path.get(i).getFrom());
    assertEquals(pathToString(path), "r", path.get(i++).getTo());
    assertNull(pathToString(path), path.get(i).getFrom());
    assertEquals(pathToString(path), "@", path.get(i++).getTo());
    assertEquals(pathToString(path), "H", path.get(i).getFrom());
    assertEquals(pathToString(path), "n", path.get(i++).getTo());
    assertEquals(pathToString(path), "t", path.get(i).getFrom());
    assertEquals(pathToString(path), "t", path.get(i++).getTo());
    
    from = "f2@f2t@";
    to = "f2rf2L@r";
    path = mp.minimumEditPath(stringToVector(from), stringToVector(to));
//      System.out.println(pathToString(path));
    assertEquals(pathToString(path), 9, path.size());
    i = 0;
    assertEquals(pathToString(path), "f", path.get(i).getFrom());
    assertEquals(pathToString(path), "f", path.get(i++).getTo());
    assertEquals(pathToString(path), "2", path.get(i).getFrom());
    assertEquals(pathToString(path), "2", path.get(i++).getTo());
    assertNull(pathToString(path), path.get(i).getFrom());
    assertEquals(pathToString(path), "r", path.get(i++).getTo());
    assertEquals(pathToString(path), "@", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo());
    assertEquals(pathToString(path), "f", path.get(i).getFrom());
    assertEquals(pathToString(path), "f", path.get(i++).getTo());
    assertEquals(pathToString(path), "2", path.get(i).getFrom());
    assertEquals(pathToString(path), "2", path.get(i++).getTo());
    assertEquals(pathToString(path), "t", path.get(i).getFrom());
    assertEquals(pathToString(path), "L", path.get(i++).getTo());
    assertEquals(pathToString(path), "@", path.get(i).getFrom());
    assertEquals(pathToString(path), "@", path.get(i++).getTo());
    assertNull(pathToString(path), path.get(i).getFrom());
    assertEquals(pathToString(path), "r", path.get(i++).getTo());
    
    from = "w3dI";
    to = "w3rLi";
    path = mp.minimumEditPath(stringToVector(from), stringToVector(to));
//      System.out.println(pathToString(path));
    assertEquals(pathToString(path), 5, path.size());
    i = 0;
    assertEquals(pathToString(path), "w", path.get(i).getFrom());
    assertEquals(pathToString(path), "w", path.get(i++).getTo());
    assertEquals(pathToString(path), "3", path.get(i).getFrom());
    assertEquals(pathToString(path), "3", path.get(i++).getTo());
    assertNull(pathToString(path), path.get(i).getFrom());
    assertEquals(pathToString(path), "r", path.get(i++).getTo());
    assertEquals(pathToString(path), "d", path.get(i).getFrom());
    assertEquals(pathToString(path), "L", path.get(i++).getTo());
    assertEquals(pathToString(path), "I", path.get(i).getFrom());
    assertEquals(pathToString(path), "i", path.get(i++).getTo());
  }
  
  @Test public void OrthographyToDISCMapping() {
    Orthography2DISCComparator comparator = new Orthography2DISCComparator();
    MinimumEditPath<String> mp = new MinimumEditPath<String>(comparator);
    String from = "Different";
    String to = "dIf@rHt";
    List<EditStep<String>> path = mp.minimumEditPath(stringToVector(from), stringToVector(to));
    //System.out.println(pathToString(path));
    assertEquals(pathToString(path), 9, path.size());
    int i = 0;
    assertEquals(pathToString(path), "D", path.get(i).getFrom());
    assertEquals(pathToString(path), "d", path.get(i++).getTo());
    assertEquals(pathToString(path), "i", path.get(i).getFrom());
    assertEquals(pathToString(path), "I", path.get(i++).getTo());
    assertEquals(pathToString(path), "f", path.get(i).getFrom());
    assertEquals(pathToString(path), "f", path.get(i++).getTo());
    assertEquals(pathToString(path), "f", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo());
    assertEquals(pathToString(path), "e", path.get(i).getFrom());
    assertEquals(pathToString(path), "@", path.get(i++).getTo());
    assertEquals(pathToString(path), "r", path.get(i).getFrom());
    assertEquals(pathToString(path), "r", path.get(i++).getTo());
    assertEquals(pathToString(path), "e", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo());
    assertEquals(pathToString(path), "n", path.get(i).getFrom());
    assertEquals(pathToString(path), "H", path.get(i++).getTo());
    assertEquals(pathToString(path), "t", path.get(i).getFrom());
    assertEquals(pathToString(path), "t", path.get(i++).getTo());
    
    from = "firefighter";
    to = "f2rf2L@r";
    path = mp.minimumEditPath(stringToVector(from), stringToVector(to));
    //System.out.println(pathToString(path));
    assertEquals(pathToString(path), 11, path.size());
    i = 0;
    assertEquals(pathToString(path), "f", path.get(i).getFrom());
    assertEquals(pathToString(path), "f", path.get(i++).getTo());
    assertEquals(pathToString(path), "i", path.get(i).getFrom());
    assertEquals(pathToString(path), "2", path.get(i++).getTo());
    assertEquals(pathToString(path), "r", path.get(i).getFrom());
    assertEquals(pathToString(path), "r", path.get(i++).getTo());
    assertEquals(pathToString(path), "e", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo());
    assertEquals(pathToString(path), "f", path.get(i).getFrom());
    assertEquals(pathToString(path), "f", path.get(i++).getTo());
    assertEquals(pathToString(path), "i", path.get(i).getFrom());
    assertEquals(pathToString(path), "2", path.get(i++).getTo());
    assertEquals(pathToString(path), "g", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo());
    assertEquals(pathToString(path), "h", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo());
    assertEquals(pathToString(path), "t", path.get(i).getFrom());
    assertEquals(pathToString(path), "L", path.get(i++).getTo());
    assertEquals(pathToString(path), "e", path.get(i).getFrom());
    assertEquals(pathToString(path), "@", path.get(i++).getTo());
    assertEquals(pathToString(path), "r", path.get(i).getFrom());
    assertEquals(pathToString(path), "r", path.get(i++).getTo());
    
    from = "them";
    to = "DEm";
    path = mp.minimumEditPath(stringToVector(from), stringToVector(to));
    //System.out.println(pathToString(path));
    assertEquals(pathToString(path), 4, path.size());
    i = 0;
    assertEquals(pathToString(path), "t", path.get(i).getFrom());
    assertEquals(pathToString(path), "D", path.get(i++).getTo());
    assertEquals(pathToString(path), "h", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo());
    assertEquals(pathToString(path), "e", path.get(i).getFrom());
    assertEquals(pathToString(path), "E", path.get(i++).getTo());
    assertEquals(pathToString(path), "m", path.get(i).getFrom());
    assertEquals(pathToString(path), "m", path.get(i++).getTo());
    
    from = "enough";
    to = "@nVf";
    path = mp.minimumEditPath(stringToVector(from), stringToVector(to));
    //System.out.println(pathToString(path));
    assertEquals(pathToString(path), 6, path.size());
    i = 0;
    assertEquals(pathToString(path), "e", path.get(i).getFrom());
    assertEquals(pathToString(path), "@", path.get(i++).getTo());
    assertEquals(pathToString(path), "n", path.get(i).getFrom());
    assertEquals(pathToString(path), "n", path.get(i++).getTo());
    assertEquals(pathToString(path), "o", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo());
    assertEquals(pathToString(path), "u", path.get(i).getFrom());
    assertEquals(pathToString(path), "V", path.get(i++).getTo());
    assertEquals(pathToString(path), "g", path.get(i).getFrom());
    assertEquals(pathToString(path), "f", path.get(i++).getTo());
    assertEquals(pathToString(path), "h", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo());
  }
  
  @Test public void OrthographyToArpabetMapping() {
    Orthography2ARPAbetComparator comparator = new Orthography2ARPAbetComparator();
    MinimumEditPath<String> mp = new MinimumEditPath<String>(comparator);
    String from = "Different";
    String to = "D IH1 F ER0 AH0 N T";
    List<EditStep<String>> path = mp.minimumEditPath(stringToVector(from), arpabetToVector(to));
    //System.out.println(pathToString(path));
    assertEquals(pathToString(path), 9, path.size());
    int i = 0;
    assertEquals(pathToString(path), "D", path.get(i).getFrom());
    assertEquals(pathToString(path), "D", path.get(i++).getTo());
    assertEquals(pathToString(path), "i", path.get(i).getFrom());
    assertEquals(pathToString(path), "IH1", path.get(i++).getTo());
    assertEquals(pathToString(path), "f", path.get(i).getFrom());
    assertEquals(pathToString(path), "F", path.get(i++).getTo());
    assertEquals(pathToString(path), "f", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo());
    assertEquals(pathToString(path), "e", path.get(i).getFrom());
    assertEquals(pathToString(path), "ER0", path.get(i++).getTo());
    assertEquals(pathToString(path), "r", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo());
    assertEquals(pathToString(path), "e", path.get(i).getFrom());
    assertEquals(pathToString(path), "AH0", path.get(i++).getTo());
    assertEquals(pathToString(path), "n", path.get(i).getFrom());
    assertEquals(pathToString(path), "N", path.get(i++).getTo());
    assertEquals(pathToString(path), "t", path.get(i).getFrom());
    assertEquals(pathToString(path), "T", path.get(i++).getTo());
    
    from = "firefighter";
    to = "F AY1 R F AY2 T ER0";
    path = mp.minimumEditPath(stringToVector(from), arpabetToVector(to));
    //System.out.println(pathToString(path));
    assertEquals(pathToString(path), 11, path.size());
    i = 0;
    assertEquals(pathToString(path), "f", path.get(i).getFrom());
    assertEquals(pathToString(path), "F", path.get(i++).getTo());
    assertEquals(pathToString(path), "i", path.get(i).getFrom());
    assertEquals(pathToString(path), "AY1", path.get(i++).getTo());
    assertEquals(pathToString(path), "r", path.get(i).getFrom());
    assertEquals(pathToString(path), "R", path.get(i++).getTo());
    assertEquals(pathToString(path), "e", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo());
    assertEquals(pathToString(path), "f", path.get(i).getFrom());
    assertEquals(pathToString(path), "F", path.get(i++).getTo());
    assertEquals(pathToString(path), "i", path.get(i).getFrom());
    assertEquals(pathToString(path), "AY2", path.get(i++).getTo());
    assertEquals(pathToString(path), "g", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo());
    assertEquals(pathToString(path), "h", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo());
    assertEquals(pathToString(path), "t", path.get(i).getFrom());
    assertEquals(pathToString(path), "T", path.get(i++).getTo());
    assertEquals(pathToString(path), "e", path.get(i).getFrom());
    assertEquals(pathToString(path), "ER0", path.get(i++).getTo());
    assertEquals(pathToString(path), "r", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo());
    
    from = "enough";
    to = "IH0 N AH1 F";
    path = mp.minimumEditPath(stringToVector(from), arpabetToVector(to));
    //System.out.println(pathToString(path));
    assertEquals(pathToString(path), 6, path.size());
    i = 0;
    assertEquals(pathToString(path), "e", path.get(i).getFrom());
    assertEquals(pathToString(path), "IH0", path.get(i++).getTo());
    assertEquals(pathToString(path), "n", path.get(i).getFrom());
    assertEquals(pathToString(path), "N", path.get(i++).getTo());
    assertEquals(pathToString(path), "o", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo());
    assertEquals(pathToString(path), "u", path.get(i).getFrom());
    assertEquals(pathToString(path), "AH1", path.get(i++).getTo());
    assertEquals(pathToString(path), "g", path.get(i).getFrom());
    assertEquals(pathToString(path), "F", path.get(i++).getTo());
    assertEquals(pathToString(path), "h", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo());
  }
  
  @Test public void CharacterToCharacterMapping() {
    Char2CharComparator comparator = new Char2CharComparator();
    MinimumEditPath<String> mp = new MinimumEditPath<String>(comparator);
    String from = " plomo 1";
    String to = "Principe2";
    List<EditStep<String>> path = mp.minimumEditPath(stringToVector(from), stringToVector(to));
    //System.out.println(pathToString(path));
    assertEquals(pathToString(path), 11, path.size());
    int i = 0;
    assertEquals(pathToString(path), " ", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo());
    assertEquals(pathToString(path), "p", path.get(i).getFrom());
    assertEquals(pathToString(path), "P", path.get(i++).getTo());
    assertEquals(pathToString(path), "l", path.get(i).getFrom());
    assertEquals(pathToString(path), "r", path.get(i++).getTo());
    assertEquals(pathToString(path), "o", path.get(i).getFrom());
    assertEquals(pathToString(path), "i", path.get(i++).getTo());
    assertEquals(pathToString(path), "m", path.get(i).getFrom());
    assertEquals(pathToString(path), "n", path.get(i++).getTo());
    assertNull(pathToString(path), path.get(i).getFrom());
    assertEquals(pathToString(path), "c", path.get(i++).getTo());
    assertEquals(pathToString(path), "o", path.get(i).getFrom());
    assertEquals(pathToString(path), "i", path.get(i++).getTo());
    assertNull(pathToString(path), path.get(i).getFrom());
    assertEquals(pathToString(path), "p", path.get(i++).getTo());
    assertNull(pathToString(path), path.get(i).getFrom());
    assertEquals(pathToString(path), "e", path.get(i++).getTo());
    assertEquals(pathToString(path), " ", path.get(i).getFrom());
    assertNull(pathToString(path), path.get(i++).getTo());
    assertEquals(pathToString(path), "1", path.get(i).getFrom());
    assertEquals(pathToString(path), "2", path.get(i++).getTo());
  }
  
  /**
   * Not to be confused with the famous Google algorithm, this method simply breaks the
   * given string into a Vector for String objects. 
   * @param s
   * @return A list of label elements, one for each character in the string
   */
  public Vector<String> stringToVector(String s) {
    Vector<String> v = new Vector<String>();
    for (char c : s.toCharArray()) v.add(""+c);
    return v;
  } // end of stringToVector()
  
  /**
   * Breaks the given ARPABET transcription (with phones delimited by spaces) into a
   * Vector for String objects. 
   * @param s
   * @return A list of label elements, one for each character in the string
   */
  public Vector<String> arpabetToVector(String s) {
    Vector<String> v = new Vector<String>();
    StringTokenizer tokens = new StringTokenizer(s, " ");
    while (tokens.hasMoreTokens()) v.add(new String(tokens.nextToken()));
    return v;
  } // end of stringToVector()
  
  /**
   * Converts the path to a String.
   * @param path
   * @return A string representation of the path.
   */
  public String pathToString(List<EditStep<String>> path) {
    StringBuffer printPath = new StringBuffer();
    int distance = 0;
    for (EditStep<String> step : path) {
      if (printPath.length() > 0) printPath.append("\n");
      printPath.append(step.toString());
      printPath.append("\t");
      printPath.append(step.getStepDistance());
      printPath.append("\t");
      printPath.append(step.totalDistance());
    }
    return printPath.toString();
  } // end of pathToString()
  
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.encoding.comparator.TestComparators");
  }
}
