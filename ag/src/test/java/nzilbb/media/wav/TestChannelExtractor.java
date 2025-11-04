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

package nzilbb.media.wav;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import nzilbb.media.*;
import nzilbb.configure.*;

/** Tester for ChannelExtractor */
public class TestChannelExtractor {

  /** Ensure default left channel extraction works. */
  @Test public void leftChannel() throws Exception {
    ChannelExtractor converter = new ChannelExtractor();
    ParameterSet configuration = new ParameterSet();
    configuration = converter.configure(configuration);
    assertNotNull("channel parameter requested", configuration.get("channel"));
    assertEquals("channel parameter default is 0",
                 Integer.valueOf(0), (Integer)(configuration.get("channel").getValue()));
    assertEquals("channel parameter set in converter",
                 Integer.valueOf(0), converter.getChannel());
    assertTrue("conversion wav to wav supported",
               converter.conversionSupported("audio/wav", "audio/wav"));

    File stereo = new File(getDir(), "stereo.wav");
    assertTrue("stereo file exists", stereo.exists());
    assertEquals("stereo file has two channels", 2, WAV.Channels(stereo));
    File mono = new File(getDir(), "left.wav");
    MediaThread thread = converter.start("audio/wav", stereo, "audio/wav", mono);
    thread.join();
    assertTrue("mono file now exists", mono.exists());
    assertEquals("mono file has only one channel", 1, WAV.Channels(mono));
  }
   
  /** Ensure selected right channel extraction works. */
  @Test public void rightChannel() throws Exception {
    ChannelExtractor converter = new ChannelExtractor();
    ParameterSet configuration = new ParameterSet();
    configuration = converter.configure(configuration);
    assertNotNull("channel parameter requested", configuration.get("channel"));
    configuration.get("channel").setValue(ChannelExtractor.RIGHT);
    assertEquals("channel parameter",
                 Integer.valueOf(1), configuration.get("channel").getValue());
    converter.configure(configuration);
    assertEquals("channel parameter set in converter",
                 Integer.valueOf(1), converter.getChannel());
    assertTrue("conversion wav to wav supported",
               converter.conversionSupported("audio/wav", "audio/wav"));

    File stereo = new File(getDir(), "stereo.wav");
    assertTrue("stereo file exists", stereo.exists());
    assertEquals("stereo file has two channels", 2, WAV.Channels(stereo));
    File mono = new File(getDir(), "right.wav");
    MediaThread thread = converter.start("audio/wav", stereo, "audio/wav", mono);
    thread.join();
    assertTrue("mono file now exists", mono.exists());
    assertEquals("mono file has only one channel", 1, WAV.Channels(mono));
  }
   
  /**
   * Directory for text files.
   * @see #getDir()
   * @see #setDir(File)
   */
  protected File dir;
  /**
   * Getter for {@link #dir}: Directory for text files.
   * @return Directory for text files.
   */
  public File getDir() { 
    if (dir == null) {
      try {
        URL urlThisClass = getClass().getResource(getClass().getSimpleName() + ".class");
        File fThisClass = new File(urlThisClass.toURI());
        dir = fThisClass.getParentFile();
      } catch(Throwable t) {
        System.out.println("" + t);
      }
    }
    return dir; 
  }
  
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.media.wav.TestChannelExtractor");
  }
}
