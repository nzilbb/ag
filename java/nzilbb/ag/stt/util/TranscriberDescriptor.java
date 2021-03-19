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
package nzilbb.ag.stt.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarException;
import nzilbb.ag.stt.Transcriber;
import nzilbb.util.CloneableBean;
import nzilbb.util.ClonedProperty;
import nzilbb.util.IO;

/**
 * Provides information about an Transcriber implementation, including what resources it
 * provides and requires. 
 * @author Robert Fromont robert@fromont.net.nz
 */
public class TranscriberDescriptor implements CloneableBean {
   
   // Attributes:

   /**
    * Fully-qualified transcriber class name.
    * @see #getTranscriberClassName()
    */
   protected String transcriberClassName;
   /**
    * Getter for {@link #transcriberClassName}: Fully-qualified transcriber class name.
    * @return Fully-qualified transcriber class name.
    */
   public String getTranscriberClassName() { return transcriberClassName; }

   /**
    * Source of the class implementation.
    * @see #getTranscriberClassLoader()
    */
   protected ClassLoader transcriberClassLoader;
   /**
    * Getter for {@link #transcriberClassLoader}: Source of the class implementation.
    * @return Source of the class implementation.
    */
   public ClassLoader getTranscriberClassLoader() { return transcriberClassLoader; }

   /**
    * Implementing class of the transcriber.
    * @see #getTranscriberClass()
    */
   @SuppressWarnings("rawtypes")
   protected Class transcriberClass;
   /**
    * Getter for {@link #transcriberClass}: Implementing class of the transcriber.
    * @return Implementing class of the transcriber.
    */
   @SuppressWarnings("rawtypes")
   public Class getTranscriberClass() { return transcriberClass; }

   /**
    * Instance of the transcriber class.
    * @see #getInstance()
    */
   protected Transcriber instance;
   /**
    * Getter for {@link #instance}: Instance of the transcriber class.
    * @return Instance of the transcriber class.
    */
   public Transcriber getInstance() { return instance; }
   
   /**
    * Unique name for the transcriber, which is immutable across versions of the implemetantation.
    * @return The transcriber's ID.
    */
   @ClonedProperty
   public String getTranscriberId() {
      return instance.getTranscriberId();
   }

   /**
    * Version of this implementation; versions will typically be numeric, but this is not
    * a requirement.
    * @return Transcriber version.
    */
   @ClonedProperty
   public String getVersion() {
      return instance.getVersion();
   }
   /**
    * Get the minimum version of the nzilbb.ag API supported by the serializer. 
    * @return Minimum version of the nzilbb.ag API supported by the serializer.
    * @see nzilbb.ag.Constants#VERSION
    */
   @ClonedProperty
   public String getMinimumApiVersion() {
      return instance.getMinimumApiVersion();
   }
   
   // Methods:
   
   /**
    * Constructor.
    * @param transcriberClassName Fully-qualified transcriber class name.
    * @param transcriberClassLoader Source of the class implementation.
    * @throws ClassNotFoundException If the transcriber is not found by 
    * <var>transcriberClassLoader</var>.
    * @throws NoSuchMethodException If the transcriber has no default constructor.
    * @throws IllegalAccessException If the transcriber's default constructor is not public.
    * @throws InvocationTargetException If the transcriber's constructor throws an exception.
    * @throws InstantiationException If the transcriber is an abstract class.
    * @throws ClassCastException If the transcriber does not extend {@link Transcriber}.
    */
   @SuppressWarnings("unchecked")
   public TranscriberDescriptor(String transcriberClassName, ClassLoader transcriberClassLoader)
      throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
      IllegalAccessException, InvocationTargetException, ClassCastException {
      this.transcriberClassName = transcriberClassName;
      this.transcriberClassLoader = transcriberClassLoader;
      this.transcriberClass = Class.forName(transcriberClassName, true, transcriberClassLoader);
      this.instance = (Transcriber)this.transcriberClass.getConstructor().newInstance();
   } // end of constructor

   /**
    * Constructor.
    * @param transcriberJar Java archive that contains the transcriber implementation.
    * @throws ClassNotFoundException If no transcriber is found.
    * @throws NoSuchMethodException If the transcriber has no default constructor.
    * @throws IllegalAccessException If the transcriber's default constructor is not public.
    * @throws InvocationTargetException If the transcriber's constructor throws an exception.
    * @throws InstantiationException If the transcriber is an abstract class.
    * @throws ClassCastException If the transcriber does not extend {@link Transcriber}.
    */
   @SuppressWarnings("unchecked")
   public TranscriberDescriptor(File transcriberJar) throws ClassNotFoundException, IOException {

      if (!transcriberJar.exists()) {
         throw new FileNotFoundException(transcriberJar.getPath());
      }
      
      instance = (Transcriber)IO.FindImplementorInJar(
         transcriberJar, getClass().getClassLoader(), Transcriber.class);
      if (instance == null) throw new ClassNotFoundException(Transcriber.class.getName());

      transcriberClass = instance.getClass();

      transcriberClassName = transcriberClass.getName();

      transcriberClassLoader = transcriberClass.getClassLoader();
      
   } // end of constructor

   /**
    * Retrieves HTML-encoded description of the function of the transcriber, for displaying
    * to the user.
    * @return The contents of the info.html file deployed with the transcriber, or null if
    * no information was provided. 
    */
   @ClonedProperty
   public String getInfo() {
      URL url = transcriberClass.getResource("info.html");
      if (url == null) return null;
      try {
         return IO.InputStreamToString(url.openConnection().getInputStream());
      } catch(IOException exception) {
         return null;
      }
   } // end of getInfo()
   
   /**
    * Determines whether the transcriber includes a web-app for installation or general
    * configuration.
    * @return true if the class includes a web-app at config/index.html, false otherwise.
    */
   public boolean hasConfigWebapp() {
      return transcriberClass.getResource("config/index.html") != null;
   } // end of hasConfigWebapp()

   /**
    * Bean-getter version of {@link #hasConfigWebapp}
    */
   @ClonedProperty
   public Boolean getHasConfigWebapp() {
      return hasConfigWebapp();
   }

   /**
    * Provides access to the given resource, supplied by the transcriber class.
    * @param path The path to the resource, relative to the transcriber class.
    * @return The content of the resource.
    */
   public InputStream getResource(String path) {
      URL url = transcriberClass.getResource(path);
      if (url == null) return null;
      try {
         return url.openConnection().getInputStream();
      } catch(IOException exception) {
         System.out.println("exception " + exception);
         return null;
      }
   } // end of getResource()
   
   /**
    * A description of the transcriber.
    * @return A description of the transcriber.
    */
   @Override
   public String toString() {
      return instance.getTranscriberId() + " ("+instance.getVersion()+")";
   } // end of toString()

} // end of class TranscriberDescriptor
