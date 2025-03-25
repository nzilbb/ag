//
// Copyright 2017-2021 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;
import java.util.function.Consumer;

/**
 * Manages the execution of an external program, ensuring that streams are processed, etc.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class Execution implements Runnable {
   
  // Attributes:
   
  /**
   * Executable file.
   * @see #getExe()
   * @see #setExe(File)
   */
  protected File exe;
  /**
   * Getter for {@link #exe}: Executable file.
   * @return Executable file.
   */
  public File getExe() { return exe; }
  /**
   * Setter for {@link #exe}: Executable file.
   * @param newExe Executable file.
   */
  public Execution setExe(File newExe) { exe = newExe; return this; }
  /**
   * Setter for {@link #exe}: Executable file.
   * @param command Command.
   */
  public Execution setExe(String command) { exe = new File(command); return this; }

  /**
   * The working directory for execution, or null for the current directory.
   * @see #getWorkingDirectory()
   * @see #setWorkingDirectory(File)
   */
  protected File workingDirectory;
  /**
   * Getter for {@link #workingDirectory}: The working directory for execution, or null
   * for the current directory. 
   * @return The working directory for execution, or null for the current directory.
   */
  public File getWorkingDirectory() { return workingDirectory; }
  /**
   * Setter for {@link #workingDirectory}: The working directory for execution, or null for 
   * the current directory. 
   * @param newWorkingDirectory The working directory for execution, or null for the
   * current directory. 
   */
  public Execution setWorkingDirectory(File newWorkingDirectory) { workingDirectory = newWorkingDirectory; return this; }
   
  /**
   * Environment variables.
   * @see #getEnvironmentVariables()
   * @see #setEnvironmentVariables(LinkedHashMap)
   */
  protected LinkedHashMap<String,String> environmentVariables = new LinkedHashMap<String,String>();;
  /**
   * Getter for {@link #environmentVariables}: Environment variables.
   * @return Environment variables.
   */
  public LinkedHashMap<String,String> getEnvironmentVariables() { return environmentVariables; }
  /**
   * Setter for {@link #environmentVariables}: Environment variables.
   * @param newEnvironmentVariables Environment variables.
   */
  public Execution setEnvironmentVariables(LinkedHashMap<String,String> newEnvironmentVariables) { environmentVariables = newEnvironmentVariables; return this; }
   
  /**
   * Command line arguments.
   * @see #getArguments()
   * @see #setArguments(Vector)
   */
  protected Vector<String> arguments;
  /**
   * Getter for {@link #arguments}: Command line arguments.
   * @return Command line arguments.
   */
  public Vector<String> getArguments() { return arguments; }
  /**
   * Setter for {@link #arguments}: Command line arguments.
   * @param newArguments Command line arguments.
   */
  public Execution setArguments(Vector<String> newArguments) { arguments = newArguments; return this; }
   
  /**
   * The executed process.
   * @see #getProcess()
   * @see #setProcess(Process)
   */
  protected Process process;
  /**
   * Getter for {@link #process}: The executed process.
   * @return The executed process.
   */
  public Process getProcess() { return process; }
  /**
   * Setter for {@link #process}: The executed process.
   * @param newProcess The executed process.
   */
  public Execution setProcess(Process newProcess) { process = newProcess; return this; }
   
  /**
   * Text from stdout
   * @see #getInput()
   * @see #setInput(StringBuffer)
   */
  protected StringBuffer input = new StringBuffer();
  /**
   * Getter for {@link #input}: Text from stdout
   * @return Text from stdout
   */
  public StringBuffer getInput() { return input; }
  /**
   * Setter for {@link #input}: Text from stdout
   * @param newInput Text from stdout
   */
  public Execution setInput(StringBuffer newInput) { input = newInput; return this; }

  /**
   * Text from stderr
   * @see #getError()
   * @see #setError(StringBuffer)
   */
  protected StringBuffer error = new StringBuffer();
  /**
   * Getter for {@link #error}: Text from stderr
   * @return Text from stderr
   */
  public StringBuffer getError() { return error; }
  /**
   * Setter for {@link #error}: Text from stderr
   * @param newError Text from stderr
   */
  public Execution setError(StringBuffer newError) { error = newError; return this; }

  /**
   * Whether the execution is currently running.
   * @see #getRunning()
   */
  protected boolean running = false;
  /**
   * Getter for {@link #running}: Whether the execution is currently running.
   * @return Whether the execution is currently running.
   */
  public boolean getRunning() { return running; }

  /**
   * Whether the execution has completed.
   * @see #getFinished()
   */
  protected boolean finished = false;
  /**
   * Getter for {@link #finished}: Whether the execution has completed.
   * @return Whether the execution has completed.
   */
  public boolean getFinished() { return finished; }

  /**
   * Whether to log verbose debug information to stdout or not.
   * @see #getVerbose()
   * @see #setVerbose(boolean)
   */
  protected boolean verbose = false;
  /**
   * Getter for {@link #verbose}: Whether to log verbose debug information to stdout or not.
   * @return Whether to log verbose debug information to stdout or not.
   */
  public boolean getVerbose() { return verbose; }
  /**
   * Setter for {@link #verbose}: Whether to log verbose debug information to stdout or not.
   * @param newVerbose Whether to log verbose debug information to stdout or not.
   */
  public Execution setVerbose(boolean newVerbose) { verbose = newVerbose; return this; }
  
  /**
   * Listeners for strings coming from stdout.
   * @see #getStdoutObservers()
   */
  protected List<Consumer<String>> stdoutObservers = new Vector<Consumer<String>>();
  /**
   * Getter for {@link #stdoutObservers}: Listeners for strings coming from stdout.
   * @return Listeners for strings coming from stdout.
   */
  public List<Consumer<String>> getStdoutObservers() { return stdoutObservers; }
  
  /**
   * Listeners for strings coming from stderr.
   * @see #getStderrObservers()
   */
  protected List<Consumer<String>> stderrObservers = new Vector<Consumer<String>>();
  /**
   * Getter for {@link #stderrObservers}: Listeners for strings coming from stderr.
   * @return Listeners for strings coming from stderr.
   */
  public List<Consumer<String>> getStderrObservers() { return stderrObservers; }
  
  // Methods:
   
  /**
   * Default constructor.
   */
  public Execution() {
    stdoutObservers.add(s->input.append(s));
    stderrObservers.add(s->error.append(s));
  } // end of constructor

  /**
   * Constructor from attributes.
   */
  public Execution(File exe, Vector<String> arguments) {
    stdoutObservers.add(s->input.append(s));
    stderrObservers.add(s->error.append(s));
    setExe(exe);
    setArguments(arguments);
  } // end of constructor
   
  /**
   * Builder-style method for adding an argument to {@link #arguments}.
   * @param argument The argument to add.
   * @return A reference to this object.
   */
  public Execution arg(String argument)
  {
    if (argument != null) {
      if (arguments == null) {
        arguments = new Vector<String>();
      }
      arguments.add(argument);
    }
    return this;
  } // end of arg()

  /**
   * Builder-style method for adding an environment variable to {@link #environmentVariables}.
   * @param variable The variable to add.
   * @param value The value for the variable.
   * @return A reference to this object.
   */
  public Execution env(String variable, String value)
  {
    if (environmentVariables == null) {
      environmentVariables = new LinkedHashMap<String,String>();
    }
    environmentVariables.put(variable, value);
    return this;
  } // end of arg()
  
  /**
   * Builder-pattern method for adding a stdout observer.
   * @param observer
   * @return This Execution.
   */
  public Execution addStdoutObserver(Consumer<String> observer) {
    stdoutObservers.add(observer);
    return this;
  } // end of addStdoutObserver()

  /**
   * Builder-pattern method for adding a stderr observer.
   * @param observer
   * @return This Execution.
   */
  public Execution addStderrObserver(Consumer<String> observer) {
    stderrObservers.add(observer);
    return this;
  } // end of addStderrObserver()

  /**
   * Runs the executable, monitors it, and returns when done.
   */
  public void run() {
    if (verbose) System.out.println("Execution: run...");
    running = true;
    finished = false;
    input = new StringBuffer();
    error = new StringBuffer();
       
    Vector<String> vArguments = new Vector<String>();
    if (exe.exists()) {
      vArguments.add(exe.getPath());
    } else { // let the shell find it?
      vArguments.add(exe.getName());
    }
    if (arguments != null) vArguments.addAll(arguments);
    
    if (verbose) System.out.println("Execution: " + vArguments);
    try {

      Vector<String> envp = new Vector<String>();
      for (String variable : environmentVariables.keySet()) {
        envp.add(variable + "=" + environmentVariables.get(variable)); 
        if (verbose) {
          System.out.println("Execution: "+variable+"=" + environmentVariables.get(variable));
        }
      }

      if (envp.size() == 0) {
        if (workingDirectory != null) {
          if (verbose) System.out.println("Execution: exec in " + workingDirectory.getPath());
          setProcess(Runtime.getRuntime().exec(
                       vArguments.toArray(new String[0]),
                       null,
                       workingDirectory));
        } else {
          if (verbose) System.out.println("Execution: exec");
          setProcess(Runtime.getRuntime().exec(
                       vArguments.toArray(new String[0])));
        }
      } else {
        if (workingDirectory != null) {
          if (verbose) {
            System.out.println("Execution: exec with envp in " + workingDirectory.getPath());
          }
          setProcess(Runtime.getRuntime().exec(
                       vArguments.toArray(new String[0]),
                       envp.toArray(new String[0]),
                       workingDirectory));
        } else {
          if (verbose) System.out.println("Execution: exec with envp");
          setProcess(Runtime.getRuntime().exec(
                       vArguments.toArray(new String[0]),
                       envp.toArray(new String[0])));
        }
      }
      InputStream inStream = process.getInputStream();
      InputStream errStream = process.getErrorStream();
      byte[] buffer = new byte[1024];
	 
      // loop waiting for the process to exit, all the while reading from
      //  the input stream to stop it from hanging
      // there seems to be some overhead in querying the input streams,
      // so we need sleep while waiting to not barrage the process with
      // requests. However, we don't want to sleep too long for processes
      // that terminate quickly or we'll be needlessly waiting.
      // So we start with short sleeps, and exponentially increase the 
      // wait time, with a maximum sleep of 30 seconds
      int iMSSleep = 1;
      while (running) {
        try {
          int iReturnValue = process.exitValue();
          if (verbose) System.out.println("Execution: Return value " + iReturnValue);

          // if exitValue returns, the process has finished
          running = false;
        } catch(IllegalThreadStateException exception) { // still executing
          if (verbose) System.out.println("Execution: Waiting for " + iMSSleep + "ms");
          // sleep for a while
          try {
            Thread.sleep(iMSSleep);
          } catch(Exception sleepX) {
            System.err.println("Execution: " + exe.getName() + " Exception while sleeping: "
                               + sleepX.toString() + "\r\n");	
          }
          iMSSleep *= 2; // backoff exponentially
          if (iMSSleep > 10000) iMSSleep = 10000; // max 10 sec
        }
	    
        try {
          // data ready?
          int bytesRead = inStream.available();
          if (verbose) System.out.println("Execution: stdout bytes ready: " + bytesRead);
          String sMessages = "";
          while(bytesRead > 0) {
            // if there's data coming, sleep a shorter time
            iMSSleep = 1;		     
            // write to the log file
            bytesRead = inStream.read(buffer);
            if (verbose) System.out.println("Execution: stdout read " + bytesRead + " bytes");
            String s = new String(buffer, 0, bytesRead);
            for (Consumer<String> observer : stdoutObservers) {
              try {
                observer.accept(s);
              } catch (Throwable t) {}
            }
            // data ready?
            bytesRead = inStream.available();
            if (verbose) System.out.println("Execution: stdin bytes ready: " + bytesRead);
          } // next chunk of data	       
        } catch(IOException exception) {
          System.err.println("Execution: ERROR reading conversion input stream: "
                             + exe.getName() + " - " + exception);
        }
	    
        try {
          // data ready from error stream?
          int bytesRead = errStream.available();
          if (verbose) System.out.println("Execution: stderr bytes ready: " + bytesRead);
          while(bytesRead > 0) {
            // if there's data coming, sleep a shorter time
            iMSSleep = 1;	    
            bytesRead = errStream.read(buffer);
            if (verbose) System.out.println("Execution: stderr read " + bytesRead + " bytes");
            String s = new String(buffer, 0, bytesRead);
            for (Consumer<String> observer : stderrObservers) {
              try {
                observer.accept(s);
              } catch (Throwable t) {}
            }
            // data ready?
            bytesRead = errStream.available();
            if (verbose) System.out.println("Execution: stderr bytes ready: " + bytesRead);
          } // next chunk of data
        } catch(IOException exception) {
          System.err.println("Execution: ERROR reading conversion error stream: "
                             + exe.getName() + " - " + exception);
        }
        if (verbose) System.out.println("Execution: still running...");
      } // running
    } catch(IOException exception) {
      System.err.println("Execution: Could not execute: " + exception);
      error.append("Could not execute: " + exception);
    }
    finished = true;
    if (verbose) System.out.println("Execution: finished.");
  } // end of run()
  
  /**
   * Returns output printed to stdout.
   * @return Output printed to stdout.
   * @see getInput()
   */
  public String stdout() {
    return getInput().toString();
  } // end of stdout()

  /**
   * Returns output printed to stderr.
   * @return Output printed to stderr.
   * @see getError()
   */
  public String stderr() {
    return getError().toString();
  } // end of stderr()

  /**
   * Runs the "which" command to determine if a command is available.
   * @param command The command we want to location of.
   * @return The executable file for the command, or null if it can't be identified.
   */
  public static File Which(String command) {
    File path = null;
    try {
      Execution which = new Execution()
        .setExe(new File("/usr/bin/which"))
        .arg(command);
      which.run();
      if (which.getInput().toString().trim().length() > 0) {
        path = new File(which.getInput().toString().trim());
      }
    } catch (Throwable t) {
      System.err.println("Execution.Which("+command+"): " + t);
    }
    return path;
  } // end of which()

} // end of class Execution
