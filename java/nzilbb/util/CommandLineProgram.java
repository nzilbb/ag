//
// Copyright 2016-2020 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.jar.JarFile;
import javax.swing.*;
import javax.swing.table.*;

/**
 * Base class that standardizes various common functions for utilitiy applications -
 * whether they be run as full applications from the command line or via JNLP or as
 * applets within a browser. 
 * <p>To implement a self-documenting, self-configuring application,
 * simply extend this class, annotate any setXXX() methods with a {@link Switch}
 * annotation, and call <code>processArguments(String argv[])</code> from the derived
 * class's <code>public static void main(String argv[])</code>
 * <p>To provide further information, you may use the {@link ProgramDescription} 
 * annotation on the derived class itself.
 * <p>Doing this has the following effect:
 * <ul>
 *  <li>If the application is run from the command line with the -usage flag
 *  then a list of switches (one for each @Switch annotated setter) is listed
 *  to stderr, along with description information annotated with {@link ProgramDescription}</li>
 *  <li>If the application is run from the command line with switches that 
 *  correspond to @Switch annotated setters, then the setter will be called
 *  with the given value - e.g. if --myswitch=myvalue is called, then the
 *  equivalent of <code>myObject.setMyswitch("myvalue")</code> is executed</li>
 *  <li>If the application is run from the command line and any switches that
 *  are marked as compulsory are not set, then execution is halted with an
 *  error message and the usage information written to stderr</li>
 *  <li>If the application is run from the command line with arguments that
 *  don't start with '--' then these are added to a {@link #arguments}</li>
 *  <li>When the application is run, if it finds a .properties file that matches
 *  the main class name, it is used to call @Switch annotated setter corresponding
 *  to the properties defined in the file.  Arguments can also be specified by
 *  defining properties called arg[0], arg[1], etc.</li>
 * </ul>
 * <p> e.g.
 * <pre>
 * import nzilbb.util.CommandLineProgram;
 * import nzilbb.util.ProgramDescription;
 * import nzilbb.util.Switch;
 * 
 * &#64;ProgramDescription(value="A very useful utility",arguments="extra-arg-1 extra-arg-2")
 * public class Useful extends CommandLineProgram
 * {
 *    public static void main(String argv[])
 *    {
 *       Useful application = new Useful();
 *       if (application.processArguments(argv))
 *       {
 *          application.start();
 *       }
 *    }
 * 
 *    &#64;Switch(value="This is a compulsory string switch that fulfils some purpose",compulsory=true)
 *    public void setSomeString(String s) {...}
 * 
 *    &#64;Switch("This is an optional boolean switch")
 *    public void setSomeBoolean(Boolean b) {...}
 * 
 *    public void start()
 *    {
 *       for (String sArgument: arguments)
 *       {
 * 	 ...
 *       }
 *    }
 * }
 * </pre>
 * <p>This could then be invoked as:<br>
 * <tt>java Useful --SomeString=Hello --SomeBoolean sundryArg1 sundryArg2</tt>
 * @author Robert Fromont robert@fromont.net.nz
 */
public class CommandLineProgram {
   
   static final long serialVersionUID = 1;      

   // Attributes:

   /**
    * Whether or not to display usage information
    * @see #getUsage()
    * @see #setUsage(Boolean)
    */
   protected Boolean usage = Boolean.FALSE;
   /**
    * Getter for {@link #usage}: Whether or not to display usage information
    * @return Whether or not to display usage information
    */
   public Boolean getUsage() { return usage; }
   /**
    * Setter for {@link #usage}: Whether or not to display usage information
    * @param bNewUsage Whether or not to display usage information
    */
   @Switch("Whether or not to display usage information")
   public CommandLineProgram setUsage(Boolean bNewUsage) { usage = bNewUsage; return this; }

   /**
    * Version information.
    * @see #getV()
    * @see #setV(String)
    */
   protected String v;
   /**
    * Getter for {@link #v}: Version information.
    * @return Version information.
    */
   public String getV() { return v; }
   /**
    * Setter for {@link #v}: Version information.
    * @param newV Version information.
    */
   public CommandLineProgram setV(String newV) { v = newV; return this; }

   /**
    * Print version information.
    * @see #getVersion()
    * @see #setVersion(Boolean)
    */
   protected Boolean version = Boolean.FALSE;
   /**
    * Getter for {@link #version}: Print version information.
    * @return Print version information.
    */
   public Boolean getVersion() { return version; }
   /**
    * Setter for {@link #version}: Print version information.
    * @param newVersion Print version information.
    */
   @Switch("Print version information.")
   public CommandLineProgram setVersion(Boolean newVersion) { version = newVersion; return this; }

   /**
    * Arguments passed in on the command line.  i.e. command line arguments that
    * don't start with '--'.  Command-line arguments that start with '--' are 
    * interpreted as <i>switches</i>, which set bean attributes.
    */
   protected Vector<String> arguments = new Vector<String>();
   
   // Methods:

   /** Constructor */
   public CommandLineProgram() {
      // get our version info from the comment of the jar file we're built into
      try {
         URL thisClassUrl = getClass().getResource(getClass().getSimpleName() + ".class");
         if (thisClassUrl.toString().startsWith("jar:")) {
            URI thisJarUri = new URI(thisClassUrl.toString().replaceAll("jar:(.*)!.*","$1"));
            JarFile thisJarFile = new JarFile(new File(thisJarUri));
            v = thisJarFile.getComment();
         }
      } catch (Throwable t) {
      }      
   }
   
   /**
    * Main entrypoint if run as an application - this should be called by the
    * <code>public static void main(String argv[])</code> of the derived class.
    * @param argv Command-line arguments.
    * @return true if all obligatory arguments were present, false otherwise
    */
   public boolean processArguments(String argv[]) {
      
      interpretPropertiesParameters();

      @SuppressWarnings("rawtypes")
      Class myClass = getClass();
      Method[] methods = myClass.getMethods();
      // arguments
      for (String sArg : argv) {
	 if (sArg.equals("-help") || sArg.equals("--help") || sArg.equals("-h")
	     || sArg.equals("--usage")) {
            // recognise common ways of asking for usage info
	    setUsage(true);
	 } else if (sArg.startsWith("--")) { // switch
	    sArg = sArg.substring(2);
	    int iEquals = sArg.indexOf('=');
	    String sName = sArg;
	    String sValue = "true";
	    if (iEquals >= 0) {
	       sName = sArg.substring(0, iEquals);
	       sValue = sArg.substring(iEquals + 1);
	    }
	    if (sName.length() > 0) {
	       // check for a bean setter with that name
	       Method setter = null;
	       for (Method method : methods) {
		  if (method.getAnnotation(Switch.class) != null
		      && method.getName().equalsIgnoreCase("set" + sName)
		      && method.getParameterTypes().length == 1) {
		     setter = method;
		     break;
		  }
	       } // next method
	       if (setter != null) {
		  try {
		     @SuppressWarnings("rawtypes")
		     Class parameterClass = setter.getParameterTypes()[0];
		     if (parameterClass.equals(String.class)) {
			setter.invoke(this, sValue);
		     } else if (parameterClass.equals(Boolean.class)) {
			setter.invoke(this, Boolean.valueOf(sValue));
		     } else if (parameterClass.equals(Integer.class)) {
			setter.invoke(this, Integer.valueOf(sValue));
		     } else if (parameterClass.equals(Double.class)) {
			setter.invoke(this, Double.valueOf(sValue));
		     } else if (parameterClass.equals(URL.class)) {
			setter.invoke(this, new URL(sValue));
		     } else if (parameterClass.equals(File.class)) {
			setter.invoke(this, new File(sValue));
		     }
		  } catch (Throwable t) {
		     System.err.println("Error interpreting switch: " + sArg 
					+ " : " + t);
		  }
	       } else {
		  System.err.println("Ignoring unknown switch: " + sArg);
	       }
	    } // sName is not ""
	 } else { // argument
	    arguments.add(sArg);
	 }
	 
      } // next argument

      boolean bArgsOk = true;

      Vector<String> errors = new Vector<String>();

      // check that all compulsory switches are set
      Vector<String> vSwitches = new Vector<String>();
      Vector<String> vCompulsorySwitches = new Vector<String>();
      Vector<String> vOptionalSwitches = new Vector<String>();
      for (Method method : methods) {
	 Switch switchAnnotation = method.getAnnotation(Switch.class);
	 if (switchAnnotation != null && method.getParameterTypes().length == 1) {
	    String sSwitchName = method.getName().replaceFirst("set", "");
	    @SuppressWarnings("rawtypes")
	    Class parameterClass = method.getParameterTypes()[0];
	    String sEg = "--" + sSwitchName + "=<value>";
	    if (parameterClass.equals(Boolean.class)) {
	       if (sSwitchName.equals("Usage") || sSwitchName.equals("Version"))
	       {
		  sEg = "--" + sSwitchName;
	       } else {
		  sEg = "--" + sSwitchName + " or --" + sSwitchName + "=false";
	       }
	    } else if (parameterClass.equals(Integer.class)
	       || parameterClass.equals(Double.class)) {
	       sEg = "--" + sSwitchName + "=<number>";
	    } else if (parameterClass.equals(URL.class)) {
	       sEg = "--" + sSwitchName + "=<URL>";
	    } else if (method.getParameterTypes()[0].equals(File.class)) {
	       sEg = "--" + sSwitchName + "=<path>";
	    }
	    String sUsage = sEg + "\t" + switchAnnotation.value();
	    if (switchAnnotation.compulsory()) {
	       try
	       {
		  @SuppressWarnings("unchecked")
		  Method getter = myClass.getMethod(method.getName().replaceFirst("set", "get"));
		  if (getter != null) {
		     try
		     {
			if (getter.invoke(this) == null) {
			   bArgsOk = false;
			   errors.add("compulsory switch '"  + sSwitchName + "' not specified");
			   setUsage(true);
			}
		     } catch (IllegalAccessException x) {
			System.err.println(x.toString());
		     } catch (InvocationTargetException y) {
			System.err.println(y.toString());
		     }
		  }
		  vCompulsorySwitches.add(sUsage);
		  vSwitches.add(sEg);
	       }
	       catch (NoSuchMethodException x){}
	    } else { // optional
	       vOptionalSwitches.add(sUsage);
	       vSwitches.add("[" + sEg + "]");
	    } // optional
	 } // method is annotated
      } // next method

      if (version) {
         System.err.println(myClass.getSimpleName() + " ("+(v==null?"version unknown":v)+")");
         bArgsOk = false;
      } else {
         for (String error : errors) System.err.println(error);
         
         // display usage?
         if (getUsage()) {
            System.err.println(
               myClass.getSimpleName() + " ("+(v==null?"version unknown":v)+"):");
            @SuppressWarnings("unchecked")
               ProgramDescription myAnnotation 
               = (ProgramDescription)myClass.getAnnotation(ProgramDescription.class);
            if (myAnnotation != null) System.err.println(myAnnotation.value());
            String sSwitchEgs = "";
            for (String s : vSwitches) sSwitchEgs += " " + s;
            System.err.println("java " 
                               + myClass.getName() + sSwitchEgs
                               + (myAnnotation != null 
                                  && myAnnotation.arguments().length() > 0?
                                  " "+myAnnotation.arguments():""));
            if (vCompulsorySwitches.size() > 0) {
               System.err.println("compulsory switches:");
               for (String s : vCompulsorySwitches) System.err.println("\t"+s);
            }
            if (vOptionalSwitches.size() > 0) {
               System.err.println("optional switches:");
               for (String s : vOptionalSwitches) System.err.println("\t"+s);
            }
            return bArgsOk;		    
         } // usage
      }

      return bArgsOk;
   }

   /**
    * This method looks for a .properties file matching the class name, and if it finds
    * one, uses it to set bean attributes and arguments appropiately. 
    * Arguments are interpreted is being the values of Properties named arg[0], arg[1], etc.
    */
   public void interpretPropertiesParameters() {
      // look for properties file
      URL settingsUrl = getClass().getResource(
	 getClass().getSimpleName() + ".properties");
      if (settingsUrl != null) {
	 try {
	    URLConnection cnxn = settingsUrl.openConnection();
	    InputStream is = cnxn.getInputStream();
	    Properties settings = new Properties();
	    settings.load(is);
	    is.close();
	    
	    // set switches from parameters
	    for (Method setter : getClass().getMethods()) {
	       Switch switchAnnotation = setter.getAnnotation(Switch.class);
	       if (switchAnnotation != null && setter.getParameterTypes().length == 1) {
		  String sSwitchName = setter.getName().replaceFirst("set", "");
		  String sValue = settings.getProperty(sSwitchName);
		  if (sValue != null) {
		     try {
			@SuppressWarnings("rawtypes")
			Class parameterClass = setter.getParameterTypes()[0];
			if (parameterClass.equals(String.class)) {
			   setter.invoke(this, sValue);
			} else if (parameterClass.equals(Boolean.class)) {
			   setter.invoke(this, Boolean.valueOf(sValue));
			} else if (parameterClass.equals(Integer.class)) {
			   setter.invoke(this, Integer.valueOf(sValue));
			} else if (parameterClass.equals(Double.class)) {
			   setter.invoke(this, Boolean.valueOf(sValue));
			} else if (parameterClass.equals(URL.class)) {
			   setter.invoke(this, new URL(sValue));
			} else if (parameterClass.equals(File.class)) {
			   setter.invoke(this, new File(sValue));
			}
		     } catch (Throwable t) {
			System.err.println("Error interpreting parameter: " 
					   + sSwitchName + " : " + t);
		     }
		  } // there is a parameter
	       } // this method is a switch
	    } // next method

	    // now look for (unnamed) arguments
	    int i = 0;
	    while (settings.getProperty("arg["+i+"]") != null) {
	       arguments.add(settings.getProperty("arg["+i+"]"));
	       i++;
	    } // next argument
            
	 } catch(Exception exception) {
	    System.err.println(exception.toString());
	 }
      }

   } // end of interpretPropertiesParameters()
   
   /**
    * Display a message
    * @param message The message to display.
    */
   public void message(String message) {
      System.out.println(message);
   } // end of message()
   
   /**
    * Show error message
    * @param message The error message.
    */
   public void error(String message) {
      System.err.println(message);
   } // end of error()

   /**
    * Show error message
    * @param t The error.
    */
   public void error(Throwable t) {
      System.err.println(t.toString());
   } // end of error()

   /**
    * Show warning message
    * @param message The warning message.
    */
   public void warning(String message) {
      System.err.println("WARNING: " + message);
   } // end of error()

   /**
    * Show warning message
    * @param t The error.
    */
   public void warning(Throwable t) {
      message("WARNING: " + t.toString());
   } // end of error()
   
} // end of class CommandLineProgram
