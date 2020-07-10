//
// Copyright 2020 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.automation.util;

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
import nzilbb.ag.automation.Annotator;
import nzilbb.util.IO;

/**
 * Provides information about an Annotator implementation, including what resources it
 * provides and requires. 
 * @author Robert Fromont robert@fromont.net.nz
 */
public class AnnotatorDescriptor {
   
   // Attributes:

   /**
    * Fully-qualified annotator class name.
    * @see #getAnnotatorClassName()
    */
   protected String annotatorClassName;
   /**
    * Getter for {@link #annotatorClassName}: Fully-qualified annotator class name.
    * @return Fully-qualified annotator class name.
    */
   public String getAnnotatorClassName() { return annotatorClassName; }

   /**
    * Source of the class implementation.
    * @see #getAnnotatorClassLoader()
    */
   protected ClassLoader annotatorClassLoader;
   /**
    * Getter for {@link #annotatorClassLoader}: Source of the class implementation.
    * @return Source of the class implementation.
    */
   public ClassLoader getAnnotatorClassLoader() { return annotatorClassLoader; }

   /**
    * Implementing class of the annotator.
    * @see #getAnnotatorClass()
    */
   @SuppressWarnings("rawtypes")
   protected Class annotatorClass;
   /**
    * Getter for {@link #annotatorClass}: Implementing class of the annotator.
    * @return Implementing class of the annotator.
    */
   @SuppressWarnings("rawtypes")
   public Class getAnnotatorClass() { return annotatorClass; }

   /**
    * Instance of the annotator class.
    * @see #getInstance()
    */
   protected Annotator instance;
   /**
    * Getter for {@link #instance}: Instance of the annotator class.
    * @return Instance of the annotator class.
    */
   public Annotator getInstance() { return instance; }
   
   // Methods:
   
   /**
    * Constructor.
    * @param annotatorClassName Fully-qualified annotator class name.
    * @param annotatorClassLoader Source of the class implementation.
    * @throws ClassNotFoundException If the annotator is not found by 
    * <var>annotatorClassLoader</var>.
    * @throws NoSuchMethodException If the annotator has no default constructor.
    * @throws IllegalAccessException If the annotator's default constructor is not public.
    * @throws InvocationTargetException If the annotator's constructor throws an exception.
    * @throws InstantiationException If the annotator is an abstract class.
    * @throws ClassCastException If the annotator does not extend {@link Annotator}.
    */
   @SuppressWarnings("unchecked")
   public AnnotatorDescriptor(String annotatorClassName, ClassLoader annotatorClassLoader)
      throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
      IllegalAccessException, InvocationTargetException, ClassCastException {
      this.annotatorClassName = annotatorClassName;
      this.annotatorClassLoader = annotatorClassLoader;
      this.annotatorClass = Class.forName(annotatorClassName, true, annotatorClassLoader);
      this.instance = (Annotator)this.annotatorClass.getConstructor().newInstance();
   } // end of constructor

   /**
    * Constructor.
    * @param annotatorJar Java archive that contains the annotator implementation.
    * @throws ClassNotFoundException If the annotator is not found by 
    * <var>annotatorClassLoader</var>.
    * @throws NoSuchMethodException If the annotator has no default constructor.
    * @throws IllegalAccessException If the annotator's default constructor is not public.
    * @throws InvocationTargetException If the annotator's constructor throws an exception.
    * @throws InstantiationException If the annotator is an abstract class.
    * @throws ClassCastException If the annotator does not extend {@link Annotator}.
    */
   @SuppressWarnings("unchecked")
   public AnnotatorDescriptor(File annotatorJar) throws ClassNotFoundException, IOException {

      if (!annotatorJar.exists()) {
         throw new FileNotFoundException(annotatorJar.getPath());
      }
      
      instance = (Annotator)IO.FindImplementorInJar(
         annotatorJar, getClass().getClassLoader(), Annotator.class);
      if (instance == null) throw new ClassNotFoundException(Annotator.class.getName());

      annotatorClass = instance.getClass();

      annotatorClassName = annotatorClass.getName();

      annotatorClassLoader = annotatorClass.getClassLoader();
      
   } // end of constructor

   /**
    * Retrieves HTML-encoded description of the function of the annotator, for displaying
    * to the user.
    * @return The contents of the info.html file deployed with the annotator, or null if
    * no information was provided. 
    */
   public String getInfo() {
      URL url = annotatorClass.getResource("info.html");
      if (url == null) return null;
      try {
         return IO.InputStreamToString(url.openConnection().getInputStream());
      } catch(IOException exception) {
         return null;
      }
   } // end of getInfo()
   
   /**
    * Determines whether the annotator includes a web-app for installation or general
    * configuration.
    * @return true if the class includes a web-app at config/index.html, false otherwise.
    */
   public boolean hasConfigWebapp() {
      return annotatorClass.getResource("config/index.html") != null;
   } // end of hasConfigWebapp()

   /**
    * Determines whether the annotator includes a web-app for task parameter configuration.
    * @return true if the class includes a web-app at task/index.html, false otherwise.
    */
   public boolean hasTaskWebapp() {
      return annotatorClass.getResource("task/index.html") != null;
   } // end of hasTaskWebapp()

   /**
    * Determines whether the annotator includes an extras web-app.
    * @return true if the class includes a web-app at ext/index.html, false otherwise.
    */
   public boolean hasExtWebapp() {
      return annotatorClass.getResource("ext/index.html") != null;
   } // end of hasTaskWebapp()
   
   /**
    * Provides access to the given resource, supplied by the annotator class.
    * @param path The path to the resource, relative to the annotator class.
    * @return The content of the resource.
    */
   public InputStream getResource(String path) {
      URL url = annotatorClass.getResource(path);
      if (url == null) return null;
      try {
         return url.openConnection().getInputStream();
      } catch(IOException exception) {
         System.out.println("exception " + exception);
         return null;
      }
   } // end of getResource()

} // end of class AnnotatorDescriptor
