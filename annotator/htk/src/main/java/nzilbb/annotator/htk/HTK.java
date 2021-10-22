//
// Copyright 2004-2021 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.annotator.htk;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Provides access to the toolkit executables.
 * @author Robert Fromont
 */
public class HTK {
  // Attributes:
  /** Currently running process */
  private Process proc;

  /**
   * Full path to the toolkit executables
   */
  private String sToolkitPath;
  /**
   * TookitPath accessor
   * @return Full path to the toolkit executables
   */
  public String getToolkitPath() { return sToolkitPath; }
  /**
   * TookitPath mutator
   * @param sNewToolkitPath Full path to the toolkit executables
   */
  public HTK setToolkitPath(String sNewToolkitPath) { sToolkitPath = sNewToolkitPath; return this; }

  /**
   * Log file for executable output
   */
  private File fLogFile;
  /**
   * LogFile accessor
   * @return Log file for executable output
   */
  public File getLogFile() { return fLogFile; }
  /**
   * LogFile mutator
   * @param fNewLogFile Log file for executable output
   */
  public HTK setLogFile(File fNewLogFile) {
    fLogFile = fNewLogFile;
    if (fLogFile != null) {      
      try {
        // write an informative message at the top of the file, so bemused users know why
        // they're getting a log file and not a success message
        FileWriter out = new FileWriter(getLogFile(), true);
        out.write((new java.util.Date()).toString() + "\r\n");
        out.write(
          "This log file contains all HTK commands executed and the layer manager status log.\r\n");
        out.write(
          "If forced alignment has failed, this log should contain clues about what went wrong\r\n");
        out.close();
      } catch(Throwable exception) {}
    }
    return this;
  }
      
  /**
   * figures out what extension needs to be added to the executable name, based on what
   * platform we're executing on - i.e. on Windows, ".exe" is added, but on UNIX,
   * nothing. 
   * (Created 18/08/2006)
   * @return An extension that should be added to tool names to give the executable file
   * name, or an empty string if no extension should be added.
   */
  public String getPlatformExeExtension() {
    if (java.lang.System.getProperty("os.name").startsWith("Win")) {
      return ".exe";
    } else {
      return "";
    }
  } // end of getPlatformExeExtension()

  protected ByteArrayOutputStream lastErrorStream;

  /**
   * Constructor
   */
  public HTK(File fToolkitPath, File logFile) {
    setToolkitPath(fToolkitPath.getPath());
    setLogFile(logFile);
  } // end of constructor

  /**
   * Simply wraps quotes around a string - intended for path arguments that might contain
   * spaces.  <em>NB</em> Quotes are only included on Windows system, which is the only
   * platform for which they are required. 
   * @param sString The string to wrap.
   * @return the original string, enclosed in double quotes only if running on Windows
   */
  public static String quoted(String sString) {
    if (java.lang.System.getProperty("os.name").startsWith("Win")) {
      return "\"" + sString + "\"";
    } else {
      return sString;
    }
  } // end of quoted()

  /**
   * Creates an array for command execution, that includes the executable name, all the
   * arguments, then executes it, writing stdout and stderr to the log file. 
   * @param sExecutableName The executable file.
   * @param argv The arguments.
   * @return An array that could be passed to exec
   */
  public int runCommand(String sExecutableName, String[] argv)
    throws InterruptedException, IOException {
    int argc = Array.getLength(argv);
    String[] aCmd = new String[argc + 1];
    // executable:
    aCmd[0] = getToolkitPath() + File.separator 
      + sExecutableName + getPlatformExeExtension();

    // arguments
    for (int i = 0; i < argc; i++) {
      aCmd[i+1] = argv[i];
    }

    logCommand(aCmd);

    int iReturnValue = -1;

    // dump streams into log file
    FileOutputStream outStream = new FileOutputStream(getLogFile(), true);
    lastErrorStream = new ByteArrayOutputStream();
    InputStream inStream = null;
    InputStream errStream = null;
    byte[] buffer = new byte[1024];
    try {
      proc = Runtime.getRuntime().exec(aCmd);

      inStream = proc.getInputStream();
      errStream = proc.getErrorStream();
	    
      // loop waiting for the process to exit, all the while reading from 
      // the input stream to stop it from hanging
      boolean bStillRunning = true;
      // there seems to be some overhead in querying the input streams,
      // so we need sleep while waiting to not barrage the process with
      // requests. However, we don't want to sleep too long for
      // processes that terminate quickly or we'll be needlessly waiting.
      // So we start with short sleeps, and exponentially
      // increase the wait time, with a maximum sleep of 30 seconds
      int iMSSleep = 1;
      while (bStillRunning) {
        try {
          iReturnValue = proc.exitValue();
		  
          // if exitValue returns, the process has finished
          bStillRunning = false;
        } catch(IllegalThreadStateException exception) { // still executing
		  
          // sleep for a while
          try {
            Thread.sleep(iMSSleep);
          } catch(Exception sleepX) {
            String sMessage = "Exception while sleeping: " 
              + sleepX.toString() + "\r\n";
            outStream.write(sMessage.getBytes(), 0, sMessage.length());	
          }
          iMSSleep *= 2; // backoff exponentially
          if (iMSSleep > 10000) iMSSleep = 10000; // max 10 sec
        }

	  
        // data ready?
        int bytesRead = inStream.available();
        while(bytesRead > 0) {
          // if there's data coming, sleep a shorter time
          iMSSleep = 1;

          // write to the log file
          bytesRead = inStream.read(buffer);
          outStream.write(buffer, 0, bytesRead);

          // data ready?
          bytesRead = inStream.available();
        } // next chunk of data
	       
        // data ready?
        bytesRead = errStream.available();
        while(bytesRead > 0) {
          // if there's data coming, sleep a shorter time
          iMSSleep = 1;

          // write to the log file
          bytesRead = errStream.read(buffer);
          outStream.write(buffer, 0, bytesRead);
          lastErrorStream.write(buffer, 0, bytesRead);

          // data ready?
          bytesRead = errStream.available();
        } // next chunk of data
      } // bStillRunning
    } finally {
      outStream.close();
      lastErrorStream.close();
      if (inStream != null) inStream.close();
      if (errStream != null) errStream.close();
    }
	 
    proc = null;
	 
    return iReturnValue;
  } // end of createCommandArray()
   
  /**
   * Returns the contents of the error stream for the last command executed.
   * @return The contents of the error stream for the last command executed, which may be
   * an empty string, or null if there was no last command. 
   */
  public String getLastError() {
    if (lastErrorStream == null) return null;
    return lastErrorStream.toString();
  } // end of getLastError()
  
  /**
   * Writes a timestamped entry to the log file for the given command.
   * @param aCmd The command line executed.
   */
  public void logCommand(String[] aCmd) {
    try {
      FileWriter out = new FileWriter(getLogFile(), true);
      out.write("\r\n----------------------------\r\n");
      out.write((new java.util.Date()).toString() + "\r\n");
      int iArgCount = Array.getLength(aCmd);
      for (int i = 0; i < iArgCount; i++) {
        out.write(" " + aCmd[i]);
      }
      out.write("\r\n----------------------------\r\n\r\n");
      out.close();
    }
    catch (Exception ex) {
    }
  } // end of logCommand()
      
  /**
   * Run HCopy with the given arguments.
   * @param argv The arguments.
   * @return the execution return code
   */
  public int HCopy(String[] argv) throws InterruptedException, IOException {
    return runCommand("HCopy", argv);
  } // end of HCopy()

  /**
   * run HCopy for feature extraction.
   * @param sFormat Input format, e.g. "WAV"
   * @param dStartTime in seconds
   * @param dEndTime in seconds
   * @param source Source audio file.
   * @param destination Destination audio file.
   * @return execution return code
   * @throws InterruptedException, IOException
   */
  public int HCopy(
    String sFormat, double dStartTime, double dEndTime, File source, File destination)
    throws InterruptedException, IOException {
    DecimalFormat formatter = new DecimalFormat(
      "#0", new DecimalFormatSymbols(Locale.US));
    dStartTime *= 10000000;
    dEndTime *= 10000000;
	 
    String[] args = {
      "-F", sFormat,
      "-T", "2",
      "-s", formatter.format(dStartTime),
      "-e", formatter.format(dEndTime),
      quoted(source.getPath()),
      quoted(destination.getPath())
    };
    return HCopy(args);
  } // end of HCopy()

  /**
   * run HCopy for feature extraction.
   * @param config Configuration file
   * @param sFormat Input format, e.g. "WAV"
   * @param dStartTime in seconds
   * @param dEndTime in seconds
   * @param source Source audio file.
   * @param destination Destination audio file.
   * @return execution return code
   * @throws IOException
   * @throws InterruptedException
   */
  public int HCopy(
    File config, String sFormat, double dStartTime, double dEndTime, File source, File destination)
    throws InterruptedException, IOException {
    DecimalFormat formatter = new DecimalFormat(
      "#0", new DecimalFormatSymbols(Locale.US));
    dStartTime *= 10000000;
    dEndTime *= 10000000;
	 
    String[] args = {
      "-F", sFormat,
      "-T", "2",
      "-C", quoted(config.getPath()),
      "-s", formatter.format(dStartTime),
      "-e", formatter.format(dEndTime),
      quoted(source.getPath()),
      quoted(destination.getPath())
    };
    return HCopy(args);
  } // end of HCopy()

  /**
   * run HCopy for feature extraction.
   * @param config Configuration file
   * @param sFormat Input format, e.g. "WAV"
   * @param source Source audio file.
   * @param destination Destination audio file.
   * @return execution return code
   * @throws IOException
   * @throws InterruptedException
   */
  public int HCopy(File config, String sFormat, File source, File destination)
    throws InterruptedException, IOException {
    String[] args = {
      "-F", sFormat,
      "-T", "2",
      "-C", quoted(config.getPath()),
      quoted(source.getPath()),
      quoted(destination.getPath())
    };
    return HCopy(args);
  } // end of HCopy()

  /**
   * run HCopy for feature extraction.
   * @param sFormat Input format, e.g. "WAV"
   * @param source Source audio file.
   * @param destination Destination audio file.
   * @return execution return code
   * @throws IOException
   * @throws InterruptedException
   */
  public int HCopy(String sFormat, File source, File destination)
    throws InterruptedException, IOException {
    String[] args = {
      "-F", sFormat,
      "-T", "2",
      quoted(source.getPath()),
      quoted(destination.getPath())
    };
    return HCopy(args);
  } // end of HCopy()

  /**
   * HParse.
   * @param fGrammar
   * @param fWordNet
   * @return executable return result
   * @throws IOException
   * @throws InterruptedException
   */
  public int HParse(File fGrammar, File fWordNet) throws IOException, InterruptedException {
    String[] args = {
      quoted(fGrammar.getPath()),
      quoted(fWordNet.getPath())
    };
    return runCommand("HParse", args);
  } // end of HParse()

  /**
   * HDMan.
   * (Created 18/08/2006)
   * @param fPhonemeList
   * @param fHdmanLog
   * @param targetDictionary
   * @param sourceDictionary
   * @return executable return value
   * @throws IOException
   * @throws InterruptedException
   */
  public int HDMan(
    File fPhonemeList, File fScript, File fHdmanLog, File targetDictionary, File sourceDictionary)
    throws IOException, InterruptedException {
    String[] args = {
      "-b", "sp",
      "-g", quoted(fScript.getPath()),
      "-n", quoted(fPhonemeList.getPath()),
      "-l", quoted(fHdmanLog.getPath()),
      quoted(targetDictionary.getPath()),
      quoted(sourceDictionary.getPath())
    };
    return runCommand("HDMan", args);
  } // end of HDMan()

      
  /**
   * HLEd.
   * @param sOption Option for generatring the fOther file
   * @param fOther File generated depending on the sOption flag
   * @param fTarget
   * @param fScript
   * @param fSource
   * @return execution return result
   * @throws IOException
   * @throws InterruptedException
   */
  public int HLEd(String sOption, File fOther, File fTarget, File fScript, File fSource)
    throws IOException, InterruptedException {
    String[] args = {
      "-l", "*",
      "-" + sOption, quoted(fOther.getPath()),
      "-i", quoted(fTarget.getPath()),
      quoted(fScript.getPath()),
      quoted(fSource.getPath())
    };
    return runCommand("HLEd", args);
  } // end of HLEd()


  /**
   * HCompV.
   * @param fConfiguration
   * @param f
   * @param fScp
   * @param dir
   * @param fPrototype
   * @return execution return result
   * @throws IOException
   * @throws InterruptedException
   */
  public int HCompV(File fConfiguration, double f, File fScp, File dir, File fPrototype)
    throws IOException, InterruptedException {
    DecimalFormat formatter = new DecimalFormat(
      "#0.00", new DecimalFormatSymbols(Locale.US));
    String[] args = {
      "-C", quoted(fConfiguration.getPath()),
      "-f", formatter.format(f),
      "-m",
      "-S", quoted(fScp.getPath()),
      "-M", quoted(dir.getPath()),
      quoted(fPrototype.getPath())
    };
    return runCommand("HCompV", args);
  } // end of HCompV()

  /**
   * HERest.
   * @param fConfiguration
   * @param fMlf
   * @param dThreshold1
   * @param dThreshold2
   * @param dThreshold3
   * @param fScp
   * @param fInputHmmList
   * @param fOutputDirectory
   * @param fMph
   * @return return value of the execution
   * @throws IOException
   * @throws InterruptedException
   */
  public int HERest(
    File fConfiguration, File fMlf, double dThreshold1, 
    double dThreshold2, double dThreshold3, File fScp, 
    File fInputMacros, 
    File fInputHmmList, File fOutputDirectory, File fMph)
    throws IOException, InterruptedException {
    DecimalFormat formatter = new DecimalFormat(
      "#0.0", new DecimalFormatSymbols(Locale.US));
    String[] args = {
      "-C", quoted(fConfiguration.getPath()),
      "-I", quoted(fMlf.getPath()),
      "-t", formatter.format(dThreshold1), formatter.format(dThreshold2),
      formatter.format(dThreshold3),
      "-S", quoted(fScp.getPath()),
      "-H", quoted(fInputMacros.getPath()),
      "-H", quoted(fInputHmmList.getPath()),
      "-M", quoted(fOutputDirectory.getPath()),
      quoted(fMph.getPath())
    };
    return runCommand("HERest", args);
  } // end of HERest()

  /**
   * HERest.
   * @param fConfiguration
   * @param fMlf
   * @param dThreshold1
   * @param dThreshold2
   * @param dThreshold3
   * @param fStats
   * @param fScp
   * @param fInputHmmList
   * @param fOutputDirectory
   * @param fMph
   * @return return value of the execution
   * @throws IOException
   * @throws InterruptedException
   */
  public int HERest(
    File fConfiguration, File fMlf, double dThreshold1, 
    double dThreshold2, double dThreshold3, 
    File fStats,
    File fScp, File fInputMacros, 
    File fInputHmmList, File fOutputDirectory, File fMph)
    throws IOException, InterruptedException {
    DecimalFormat formatter = new DecimalFormat(
      "#0.0", new DecimalFormatSymbols(Locale.US));
    String[] args = {
      "-C", quoted(fConfiguration.getPath()),
      "-I", quoted(fMlf.getPath()),
      "-t", formatter.format(dThreshold1), formatter.format(dThreshold2),
      formatter.format(dThreshold3),
      "-s", quoted(fStats.getPath()),
      "-S", quoted(fScp.getPath()),
      "-H", quoted(fInputMacros.getPath()),
      "-H", quoted(fInputHmmList.getPath()),
      "-M", quoted(fOutputDirectory.getPath()),
      quoted(fMph.getPath())
    };
    return runCommand("HERest", args);
  } // end of HERest()

  /**
   * HHEd.
   * @param fInputMacros
   * @param fInputHmmList
   * @param fOutputDirectory
   * @param fScript
   * @param fMph
   * @return execution return result
   * @throws IOException
   * @throws InterruptedException
   */
  public int HHEd(
    File fInputMacros, File fInputHmmList, 
    File fOutputDirectory, File fScript, File fMph) throws IOException, InterruptedException {
    String[] args = {
      "-H", quoted(fInputMacros.getPath()),
      "-H", quoted(fInputHmmList.getPath()),
      "-M", quoted(fOutputDirectory.getPath()),
      quoted(fScript.getPath()),
      quoted(fMph.getPath())
    };
    return runCommand("HHEd", args);
  } // end of HHEd()

  /**
   * HVite.
   * @param sO Choose how the output labels should be formatted. s is a string with
   * certain letters (from NSCTWM). 
   * @param sB The sentence boundary during alignment.
   * @param fConfiguration
   * @param fInputMacros
   * @param fInputHmmList
   * @param fOutputMlf
   * @param dT Enable beam searching such that any model whose maximum log probability token
   * falls more than dT below the maximum for all models is deactivated.
   * @param fWordsMlf
   * @param fScp
   * @param fDictionary
   * @param fPhonemes
   * @return execution return result
   * @throws IOException
   * @throws InterruptedException
   */
  public int HVite(
    String sO, String sB, File fConfiguration, 
    File fInputMacros, File fInputHmmList, File fOutputMlf,
    double dT, File fWordsMlf, File fScp, File fDictionary, 
    File fPhonemes) throws IOException, InterruptedException {
    DecimalFormat formatter = new DecimalFormat(
      "#0.0", new DecimalFormatSymbols(Locale.US));
    // TODO add -m so the 'sp's can be stipped off and we get pauses aligned
    String[] args = {
      "-l", "*",
      "-o", sO, 
      "-b", sB,
      "-C", quoted(fConfiguration.getPath()),
      "-a",
      "-m",
      "-H", quoted(fInputMacros.getPath()),
      "-H", quoted(fInputHmmList.getPath()),
      "-i", quoted(fOutputMlf.getPath()),
      //"-m", // keep track of model boundaries
      "-t", formatter.format(dT),
      "-y", "lab",
      "-I", quoted(fWordsMlf.getPath()),
      "-S", quoted(fScp.getPath()),
      quoted(fDictionary.getPath()),
      quoted(fPhonemes.getPath())
    };
    return runCommand("HVite", args);
  } // end of HVite()

  /**
   * HVite.
   * @param sO Choose how the output labels should be formatted. s is a string with
   * certain letters (from  NSCTWM).
   * @param sB The sentence boundary during alignment.
   * @param dP The word insertion log probability
   * @param dS The grammar scale factor. This factor post-multiplies the language model
   * likelihoods from the word lattices. 
   * @param fInputMacros
   * @param fInputHmmList
   * @param fOutputMlf
   * @param fWordsMlf
   * @param fScp
   * @param fDictionary
   * @param fPhonemes
   * @return execution return result
   * @throws IOException
   * @throws InterruptedException
   */
  public int HVite(
    String sO, String sB, double dP, double dS, 
    File fInputMacros, File fInputHmmList, File fOutputMlf, 
    File fWordsMlf, File fScp, File fDictionary, File fPhonemes)
    throws IOException, InterruptedException {
    DecimalFormat formatter = new DecimalFormat(
      "#0.0", new DecimalFormatSymbols(Locale.US));
    // TODO add -m so the 'sp's can be stipped off and we get pauses aligned
    String[] args = {
      "-l", "*",
      "-o", sO, 
      "-b", sB,
      "-p", formatter.format(dP), 
      "-s", formatter.format(dS),
      "-a",
      "-m",
      "-H", quoted(fInputMacros.getPath()),
      "-H", quoted(fInputHmmList.getPath()),
      "-i", quoted(fOutputMlf.getPath()),
      //"-m", // keep track of model boundaries
      //"-t", formatter.format(dT),
      "-y", "lab",
      "-I", quoted(fWordsMlf.getPath()),
      "-S", quoted(fScp.getPath()),
      quoted(fDictionary.getPath()),
      quoted(fPhonemes.getPath())
    };
    return runCommand("HVite", args);
  } // end of HVite()

  /**
   * Kills any currently executing process.
   */
  public void killCurrentProcess() {
    if (proc != null) {
      try {
        proc.destroy();
      } catch(Exception exception) {
      }
    }
  } // end of killCurrentProcess()

} // end of class HTK
