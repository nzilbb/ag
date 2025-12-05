//
// Copyright 2025 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.transcriber.evaluator;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;
import nzilbb.ag.Anchor;
import nzilbb.ag.Annotation;
import nzilbb.ag.Change;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.StoreException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.editpath.EditStep;
import nzilbb.editpath.MinimumEditPath;
import nzilbb.util.IO;

/**
 * Unit tests for the the transcriber evaluator.
 */
public class TestEvaluate {
  
  /** Ensure the evaluator generally works, using pre-transcribed transcripts. */
  @Test public void basic() throws Exception {

    // files
    File dir = dir();
    File audio = new File(dir, "transcript1.wav");
    File transcript = new File(dir, "transcript1.txt");
    // pre-transcribed files to compare to 
    File pretranscribedDir = new File(dir, "pretranscribed");
    // outputs
    File[] outputs = {
      new File(dir, "std.out"),
      new File(dir, "utterances-Pretranscribed.tsv"),
      new File(dir, "paths-Pretranscribed.tsv")
    };

    Evaluate evaluate = new Evaluate();
    // capture main output
    evaluate.stdout = new FileOutputStream(outputs[0]);
    // set command-line arguments    
    evaluate.processArguments(new String[]{ pretranscribedDir.getPath(), dir().getPath() });
    //evaluate.setVerbose(true);
    evaluate.start();

    evaluate.stdout.close();
    // ensure outputs are as expected
    for (File output : outputs) {
      assertTrue("Output exists: " + output.getName(), output.exists());
      String differences = diff(
        new File(dir, "expected_basic_" + output.getName()), output);
      if (differences != null) {
        fail(differences);
      } else {
        output.delete();
      }
    }
  }   

  public static File dir() throws Exception { 
    URL urlThisClass = TestEvaluate.class.getResource(
      TestEvaluate.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }

  /**
   * Diffs two files.
   * @param expected
   * @param actual
   * @return null if the files are the same, and a String describing differences if not.
   */
  public String diff(File expected, File actual) {
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
  
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.transcriber.evluator.TestEvaluate");
  }
}
