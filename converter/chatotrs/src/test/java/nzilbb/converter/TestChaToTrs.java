//
// Copyright 2021 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.converter;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Vector;
import java.net.URL;
import nzilbb.editpath.EditStep;
import nzilbb.editpath.MinimumEditPath;

public class TestChaToTrs {
  
  @Test public void convert() throws Exception {
    File dir = getDir();
    File input = new File(dir, "clan.cha");
    ChaToTrs converter = new ChaToTrs();
    converter.convert(input);
    File actual = new File(dir, "clan.trs");
    File expected = new File(dir, "expected_clan.trs");
    String differences = diff(expected, actual, ".*version_date.*");
    if (differences != null) {
      fail(differences);
    } else {
      actual.delete();
    }
  }

  // TODO @Test public void griffin() throws Exception {
  //   File dir = getDir();
  //   File input = new File(dir, "griffin.cha");
  //   ChaToTrs converter = new ChaToTrs();
  //   converter.convert(input);
  //   File actual = new File(dir, "griffin.eaf");
  //   File expected = new File(dir, "expected_griffin.eaf");
  //   String differences = diff(expected, actual);
  //   if (differences != null) {
  //     fail(differences);
  //   } else {
  //     actual.delete();
  //   }
  // }
  
  /**
   * Diffs two files.
   * @param expected
   * @param actual
   * @return null if the files are the same, and a String describing differences if not.
   */
  public String diff(File expected, File actual) {
    return diff(expected, actual, null);
  }
  
  /**
   * Diffs two files.
   * @param expected
   * @param actual
   * @param ignorePattern An optional regular expression identifying changes to ignore.
   * @return null if the files are the same, and a String describing differences if not.
   */
  public String diff(File expected, File actual, String ignorePattern) {
    
    StringBuffer d = new StringBuffer();      
    try {
      // compare with what we expected
      Vector<String> actualLines = new Vector<String>();
      BufferedReader reader = new BufferedReader(new FileReader(actual));
      String line = reader.readLine();
      while (line != null) {
        actualLines.add(line);
        line = reader.readLine();
      }
      Vector<String> expectedLines = new Vector<String>();
      reader = new BufferedReader(new FileReader(expected));
      line = reader.readLine();
      while (line != null) {
        expectedLines.add(line);
        line = reader.readLine();
      }
      MinimumEditPath<String> comparator = new MinimumEditPath<String>();
      List<EditStep<String>> path = comparator.minimumEditPath(expectedLines, actualLines);
      for (EditStep<String> step : path) {
        
        // ignore this difference?
        if (ignorePattern != null && step.getFrom().matches(ignorePattern)) continue;
        
        // report the difference
        switch (step.getOperation()) {
          case CHANGE:
            d.append("\n"+expected.getPath()+":"+(step.getFromIndex()+1)+": Expected:\n" 
                     + step.getFrom() 
                     + "\n"+actual.getPath()+":"+(step.getToIndex()+1)+": Found:\n" + step.getTo());
            break;
          case DELETE:
            d.append("\n"+expected.getPath()+":"+(step.getFromIndex()+1)+": Deleted:\n" 
                     + step.getFrom()
                     + "\n"+actual.getPath()+":"+(step.getToIndex()+1)+": Missing");
            break;
          case INSERT:
            d.append("\n"+expected.getPath()+":"+(step.getFromIndex()+1)+": Missing" 
                     + "\n"+actual.getPath()+":"+(step.getToIndex()+1)+": Inserted:\n" 
                     + step.getTo());
            break;
        }
      } // next step
    } catch(Exception exception) {
      d.append("\n" + exception);
    }
    if (d.length() > 0) return d.toString();
    return null;
  } // end of diff()
  
  /**
   * Directory for text files.
   * @see #getDir()
   * @see #setDir(File)
   */
  protected File fDir;
  /**
   * Getter for {@link #fDir}: Directory for text files.
   * @return Directory for text files.
   */
  public File getDir() { 
    if (fDir == null) {
      try {
        URL urlThisClass = getClass().getResource(getClass().getSimpleName() + ".class");
        File fThisClass = new File(urlThisClass.toURI());
        fDir = fThisClass.getParentFile();
      } catch(Throwable t) {
        System.out.println("" + t);
      }
    }
    return fDir; 
  }
  /**
   * Setter for {@link #fDir}: Directory for text files.
   * @param fNewDir Directory for text files.
   */
  public void setDir(File fNewDir) { fDir = fNewDir; }
  
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.converter.TestChaToTrs");
  }
}
