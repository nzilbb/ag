//
// Copyright 2023 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.media.wav;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Utility functions for WAV files.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class WAV {
  /** Private constructor to prevent instantiation. */
  private WAV(){}
  
  /**
   * Determine the duration in seconds of the given WAV file.
   * @param wav
   * @return The duration in seconds of the given file, or null if the duration couldn't
   * be determined.
   * @throw UnsupportedAudioFileException If the given file is not of a supported format.
   * @throw IOException If the given file is not accessible.
   */
  public static Double duration(File wav) throws UnsupportedAudioFileException, IOException {
    // determine the duration of the media file
    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(wav);
    AudioFormat format = audioInputStream.getFormat();
    long frames = audioInputStream.getFrameLength();
    if (frames > 0) {
      return ((double)frames) / format.getFrameRate(); 
    }
    return null;
  } // end of duration()

} // end of class WAV
