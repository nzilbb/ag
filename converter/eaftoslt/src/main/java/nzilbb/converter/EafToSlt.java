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

import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.HashSet;
import nzilbb.ag.Annotation;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.GraphSerializer;
import nzilbb.formatter.elan.EAFSerialization;
import nzilbb.formatter.salt.SltSerialization;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;

/**
 * Converts ELAN .eaf files SALT .slt transcripts.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts ELAN .eaf files SALT .slt transcripts",arguments="file1.eaf file2.eaf ...")
public class EafToSlt extends Converter {

  /**
   * Default constructor.
   */
  public EafToSlt() {
    setDefaultWindowTitle("ELAN to SALT converter");
    // default to false, as it's what users of this converter most likely expect,
    setSwitch("useConventions", "false");
  } // end of constructor
  
  public static void main(String argv[]) {
    new EafToSlt().mainRun(argv);
  }
  
  /** File filter for identifying files of the correct type */
  protected FileNameExtensionFilter getFileFilter() {
    return new FileNameExtensionFilter("ELAN files", "eaf");
  }

  /**
   * Gets the deserializer that #convert(File) uses.
   * @return The deserializer to use.
   */
  public GraphDeserializer getDeserializer() {
    return new EAFSerialization();
  }
  
  /**
   * Gets the serializer that #convert(File) uses.
   * @return The serializer to use.
   */
  public GraphSerializer getSerializer() {
    return new SltSerialization();
  }
  
  /**
   * Rename speakers to SALT-friendly names. 
   * @param graphs
   */
  @Override
  public void processGraphs(Graph[] transcripts) {
    HashSet<Character> codes = new HashSet<Character>();
    for (Graph transcript : transcripts) {

      // participants have to start with a unique letter
      Annotation[] participants = transcript.all(transcript.getSchema().getParticipantLayerId());
      
      // first pass looking for standard names
      for (Annotation participant : participants){
        Character code = null;
        if (participant.getLabel().toLowerCase().contains("child")
            && !codes.contains("C")) {
          participant.setLabel("Child");
          code = participant.getLabel().charAt(0);
        } else if (participant.getLabel().toLowerCase().contains("parent")
                   && !codes.contains("P")) {
          participant.setLabel("Parent");
          code = participant.getLabel().charAt(0);
        } else if (participant.getLabel().toLowerCase().contains("mother")
                   && !codes.contains("M")) {
          participant.setLabel("Mother");
          code = participant.getLabel().charAt(0);
        } else if (participant.getLabel().toLowerCase().contains("father")
                   && !codes.contains("F")) {
          participant.setLabel("Father");
          code = participant.getLabel().charAt(0);
        } else if (participant.getLabel().toLowerCase().contains("brother")
                   && !codes.contains("B")) {
          participant.setLabel("Brother");
          code = participant.getLabel().charAt(0);
        } else if (participant.getLabel().toLowerCase().contains("sister")
                   && !codes.contains("S")) {
          participant.setLabel("Sister");
          code = participant.getLabel().charAt(0);
        } else if (participant.getLabel().toLowerCase().contains("participant")
                   && !codes.contains("P")) {
          participant.setLabel("Participant");
          code = participant.getLabel().charAt(0);
        } else if (participant.getLabel().toLowerCase().contains("investigator")
                   && !codes.contains("I")) {
          participant.setLabel("Investigator");
          code = participant.getLabel().charAt(0);
        } else if (participant.getLabel().toLowerCase().contains("examiner")
                   && !codes.contains("E")) {
          participant.setLabel("Examiner");
          code = participant.getLabel().charAt(0);
        } else if (participant.getLabel().toLowerCase().contains("teacher")
                   && !codes.contains("T")) {
          participant.setLabel("Examiner");
          code = participant.getLabel().charAt(0);
        } else if (participant.getLabel().toLowerCase().contains("interviewer")
                   && !codes.contains("I")) {
          participant.setLabel("Interviewer");
          code = participant.getLabel().charAt(0);
        } else if (participant.getLabel().toLowerCase().contains("unknown")
                   && !codes.contains("U")) {
          participant.setLabel("Unknown");
          code = participant.getLabel().charAt(0);
        } else if (participant.getLabel().toLowerCase().contains("unidentified")
                   && !codes.contains("U")) {
          participant.setLabel("unidentified");
          code = participant.getLabel().charAt(0);
        }
        if (code != null) {
          codes.add(code);
          participant.put("@code", code);
        }
      } // next participant
      
      // second pass trying to use current labels
      boolean residualParticipants = false;
      for (Annotation participant : participants) {
        if (!participant.containsKey("@code")) {
          if (!codes.contains(participant.getLabel().charAt(0))) {
            Character code = participant.getLabel().charAt(0);
            participant.put("@code", code);
            codes.add(code);
          } else {
            residualParticipants = true;
          }
        } // no code yet
      } // next participant

      if (residualParticipants) {
        // third pass - just assign them a code and prepend the label
        char nextCode = 'A';
        for (Annotation participant : participants) {
          if (!participant.containsKey("@code")) {
            // find the next available code
            while (codes.contains(nextCode)) nextCode++;
            // use it
            participant.setLabel(""+nextCode+"-"+participant.getLabel());
            codes.add(nextCode);
            participant.put("@code", nextCode);
          } // no code yet
        } // next participant
      }

      // relabel all turns with participant label
      for (Annotation participant : participants) {
        for (Annotation turn : participant.all(transcript.getSchema().getTurnLayerId())) {
          turn.setLabel(participant.getLabel());
        }
      } // next participant
    } // next transcript
  } // end of processGraphs()

  /**
   * Specifies which layers should be given to the serializer. The default implementaion
   * returns only the "utterance" layer.
   * @return An array of layer IDs.
   */
  public String[] getLayersToSerialize() {
    String[] layers = { "utterance" };
    return layers;
  } // end of getLayersToSerialize()
  
  private static final long serialVersionUID = -1;
} // end of class EafToSlt
