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

/** Tester for FragmentExtractor */
public class TestFragmentExtractor {

  /** Ensure a fragment can be extracted. */
  @Test public void extract() throws Exception {
    FragmentExtractor converter = new FragmentExtractor();
    ParameterSet configuration = new ParameterSet();
    configuration = converter.configure(configuration);
    assertNotNull("start parameter requested", configuration.get("start"));
    assertTrue("start parameter required", configuration.get("start").getRequired());
    configuration.get("start").setValue(Double.valueOf(0.25));
    assertNotNull("end parameter requested", configuration.get("end"));
    assertTrue("end parameter required", configuration.get("end").getRequired());
    configuration.get("end").setValue(Double.valueOf(0.75));
    converter.configure(configuration);
    assertEquals("start parameter set in converter",
                 Double.valueOf(0.25), converter.getStart());
    assertEquals("end parameter set in converter",
                 Double.valueOf(0.75), converter.getEnd());
    assertTrue("conversion wav to wav supported",
               converter.conversionSupported("audio/wav", "audio/wav"));

    File whole = new File(getDir(), "stereo.wav");
    assertTrue("whole file exists", whole.exists());
    File fragment = new File(getDir(), "fragment.wav");
    MediaThread thread = converter.start("audio/wav", whole, "audio/wav", fragment);
    thread.join();
    assertTrue("fragment file now exists", fragment.exists());
    assertEquals("fragment has correct duration",
                 Double.valueOf(0.5), WAV.Duration(fragment));
  }
  
  /** Ensure a recording can be resampled. */
  @Test public void resample() throws Exception {
    FragmentExtractor converter = new FragmentExtractor();
    ParameterSet configuration = new ParameterSet();
    configuration = converter.configure(configuration);
    assertNotNull("sampleRate parameter requested", configuration.get("sampleRate"));
    assertNull("sampleRate default is null", configuration.get("sampleRate").getValue());
    assertFalse("sampleRate parameter not required",
               configuration.get("sampleRate").getRequired());
    configuration.get("sampleRate").setValue(Integer.valueOf(16000));
    configuration.get("start").setValue(Double.valueOf(0)); // from the start
    configuration.get("end").setValue(Double.valueOf(0));   // until the end
    converter.configure(configuration);
    assertEquals("sampleRate parameter set in converter",
                 Integer.valueOf(16000), converter.getSampleRate());
    assertTrue("conversion wav to wav supported",
               converter.conversionSupported("audio/wav", "audio/wav"));

    File original = new File(getDir(), "stereo.wav");
    assertTrue("original file exists", original.exists());
    File resampled = new File(getDir(), "resampled.wav");
    MediaThread thread = converter.start("audio/wav", original, "audio/wav", resampled);
    thread.join();
    assertTrue("resampled file now exists", resampled.exists());
    assertTrue("entire duration extracted: "
               + (WAV.Duration(original) - WAV.Duration(resampled)),
               // might not be exact, so we check the difference is very small:
               Math.abs(WAV.Duration(original) - WAV.Duration(resampled)) < 0.001);
  }
   
  /** Ensure a channel can be extracted. */
  @Test public void extractChannel() throws Exception {
    FragmentExtractor converter = new FragmentExtractor();
    ParameterSet configuration = new ParameterSet();
    configuration = converter.configure(configuration);
    assertNotNull("channel parameter requested", configuration.get("channel"));
    assertNull("channel default is null", configuration.get("channel").getValue());
    assertFalse("channel parameter not required",
               configuration.get("channel").getRequired());
    configuration.get("channel").setValue(ChannelExtractor.RIGHT);
    configuration.get("start").setValue(Double.valueOf(0.25));
    configuration.get("end").setValue(Double.valueOf(0.75));
    converter.configure(configuration);
    assertEquals("channel parameter set in converter",
                 Integer.valueOf(1), converter.getChannel());
    assertTrue("conversion wav to wav supported",
               converter.conversionSupported("audio/wav", "audio/wav"));

    File stereo = new File(getDir(), "stereo.wav");
    assertTrue("stereo file exists", stereo.exists());
    assertEquals("stereo has correct number of channels", 2, WAV.Channels(stereo));
    File rightFragment = new File(getDir(), "rightFragment.wav");
    MediaThread thread = converter.start("audio/wav", stereo, "audio/wav", rightFragment);
    thread.join();
    assertTrue("fragment file now exists", rightFragment.exists());
    assertEquals("fragment has correct number of channels", 1, WAV.Channels(rightFragment));
  }
   
  /**
   * Directory for text files.
   * @see #getDir()
   * @see #setDir(File)
   */
  protected File dir;
  /**
   * Getter for {@link #fDir}: Directory for text files.
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
    org.junit.runner.JUnitCore.main("nzilbb.media.wav.TestFragmentExtractor");
  }
}
