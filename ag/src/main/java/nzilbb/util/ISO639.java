//
// Copyright 2004-2021 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of LaBB-CAT.
//
//    LaBB-CAT is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    LaBB-CAT is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with LaBB-CAT; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package nzilbb.util;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Manages various operations for validating and translating ISO 639 standard language codes
 * and language names. 
 * @author Robert Fromont robert@fromont.net.nz
 */
public class ISO639 {
   
   /** Lookup table by two-letter language code to English name. */
   protected Properties alpha2ToName = new Properties();
   
   /** Lookup table by English name to two-letter language code. */
   protected Properties nameToAlpha2 = new Properties();
   
   /** Lookup table by lowercase English name to two-letter language code. */
   protected Properties lowercaseNameToAlpha2 = new Properties();
   
   /** Lookup table by ISO639-2 3-letter code. */
   protected Properties alpha3ToAlpha2 = new Properties();
   
   /** Lookup table for converting ISO639-1 2-letter codes to ISO639-2 3-letter codes. */
   protected Properties alpha2ToAlpha3 = new Properties();
   
   /** Default constructor */
   public ISO639() {
      try {
         InputStream in = getClass().getResource("ISO639.txt").openStream();
         alpha2ToName.load(in);
         in.close();
      } catch(Throwable t) {
         System.err.println("ISO639: Could not load ISO639.txt - " + t);
         // provide some basic ones
         alpha2ToName.setProperty("en", "English");
         alpha2ToName.setProperty("fr", "French");
         alpha2ToName.setProperty("de", "German");
         alpha2ToName.setProperty("mi", "Maori");
         alpha2ToName.setProperty("zh", "Chinese");
         alpha2ToName.setProperty("fi", "Finnish");
         alpha2ToName.setProperty("it", "Italian");	
         alpha2ToName.setProperty("es", "Spanish");
      }
      
      try {
         InputStream in = getClass().getResource("ISO639-2-1.txt").openStream();
         alpha3ToAlpha2.load(in);
         in.close();
      } catch(Throwable t) {
         System.err.println("ISO639: Could not load ISO639-2-1.txt - " + t);
         // provide some basic ones
         alpha3ToAlpha2.setProperty("eng", "en");
         alpha3ToAlpha2.setProperty("fre", "fr");
         alpha3ToAlpha2.setProperty("fra", "fr");
         alpha3ToAlpha2.setProperty("ger", "de");
         alpha3ToAlpha2.setProperty("deu", "de");
         alpha3ToAlpha2.setProperty("mri", "mi");
         alpha3ToAlpha2.setProperty("mao", "mi");
         alpha3ToAlpha2.setProperty("chi", "zh");
         alpha3ToAlpha2.setProperty("zho", "zh");
         alpha3ToAlpha2.setProperty("fin", "fi");
         alpha3ToAlpha2.setProperty("ita", "it");	
         alpha3ToAlpha2.setProperty("spa", "es");
      }
      
      // build reverse-lookup list
      for (Object oCode : alpha3ToAlpha2.keySet()) {
         String name = alpha3ToAlpha2.getProperty(oCode.toString());
         alpha2ToAlpha3.setProperty(name, oCode.toString());
      } // next key
      // build name lookups
      for (Object oCode : alpha2ToName.keySet()) {
         String name = alpha2ToName.getProperty(oCode.toString());
         nameToAlpha2.setProperty(name, oCode.toString());
         lowercaseNameToAlpha2.setProperty(name.toLowerCase(), oCode.toString());
      } // next key
   } // end of constructor
   
   /**
    * Determines whether the given string is a language name in English.
    * @param s
    * @return true if the given string is a known language name (in English), false otherwise.
    */
   public boolean isName(String s) {
      return lowercaseNameToAlpha2.containsKey(s.toLowerCase());
   } // end of isName()
   
   /**
    * Determines whether the given string is a 2-letter ISO639 code.
    * @param s
    * @return true if the given string is a known 2-letter ISO639, false otherwise.
    */
   public boolean isAlpha2(String s) {
      return alpha2ToName.containsKey(s.toLowerCase());
   } // end of isName()
   
   /**
    * Determines whether the given string is a 3-letter ISO639 code.
    * @param s
    * @return true if the given string is a known 3-letter ISO639, false otherwise.
    */
   public boolean isAlpha3(String s) {
      return alpha3ToAlpha2.containsKey(s.toLowerCase());
   } // end of isName()
   
   /**
    * Gets the name of a language, given its alpha-2 code. Case is ignored.
    * @param code
    * @return The name of a language with the given two-letter code, or an empty Optional
    * if the code is unknown. 
    */
   public Optional<String> nameFromAlpha2(String code) {
      if (code == null) return Optional.empty();
      return Optional.ofNullable(
         alpha2ToName.getProperty(code.toLowerCase()));
   } // end of nameFromCode()
   
   /**
    * Gets the name of a language, given its alpha-3 code. Case is ignored.
    * @param code
    * @return The name of a language with the given three-letter code, or an empty Optional
    * if the code is unknown. 
    */
   public Optional<String> nameFromAlpha3(String code) {
      if (code == null) return Optional.empty();
      return Optional.ofNullable(
         alpha2ToName.getProperty(
            alpha3ToAlpha2.getProperty(code.toLowerCase())));
   } // end of nameFromCode()
   
   /**
    * Gets the alpha 2 code of a language, given its name. Case is ignored.
    * @param name
    * @return The two-letter code of a language with the given name, or an empty Optional
    * if the name is unknown. 
    */
   public Optional<String> alpha2FromName(String name) {
      return Optional.ofNullable(
         lowercaseNameToAlpha2.getProperty(name.toLowerCase()));
   } // end of codeFromName()   
   
   /**
    * Gets the alpha 3 code of a language, given its name. Case is ignored.
    * @param name
    * @return The two-letter code of a language with the given name, or an empty Optional
    * if the name is unknown.
    */
   public Optional<String> alpha3FromName(String name) {
      return Optional.ofNullable(
         alpha2ToAlpha3.getProperty(
            alpha2FromName(name.toLowerCase()).orElse("")));
   } // end of codeFromName()   
   
   /**
    * Gets the alpha 2 code of a language, given its alpha-3 code. Case is ignored.
    * @param code An alpha-3 code.
    * @return The two-letter code of a language with the given three-letter code, or an
    * empty Optional if the name is unknown.
    */
   public Optional<String> alpha2FromAlpha3(String code) {
      return Optional.ofNullable(
         alpha3ToAlpha2.getProperty(code.toLowerCase()));
   } // end of codeFromName()   

   /**
    * Gets the alpha 3 code of a language, given its alpha-2 code. Case is ignored.
    * @param code An alpha-2 code.
    * @return The three-letter code of a language with the given two-letter code, or an empty
    * Optional if the name is unknown.
    */
   public Optional<String> alpha3FromAlpha2(String code) {
      return Optional.ofNullable(
         alpha2ToAlpha3.getProperty(code.toLowerCase()));
   } // end of codeFromName()   

   /**
    * Gets the alpha 3 code of a language, given some language identifier.
    * @param id A string identifying the language, which may be an alpha-3 code, an
    * alpha-2 code, or a name. 
    * @return The three-letter code of the language with the given ID, or an empty Optional 
    * if the identifier is unknown.
    */
   public Optional<String> alpha3(String id) {
      if (id == null) return Optional.empty();
      if (isAlpha3(id)) return Optional.of(id);
      return Optional.ofNullable(
         alpha3FromAlpha2(id)
         .orElse(alpha3FromName(id)
                 .orElse(null)));
   } // end of codeFromName()
   
   /**
    * Gets the alpha 2 code of a language, given some language identifier.
    * @param id A string identifying the language, which may be an alpha-3 code, an
    * alpha-2 code, or a name. 
    * @return The two-letter code of the language with the given ID, or an empty Optional 
    * if the identifier is unknown.
    */
   public Optional<String> alpha2(String id) {
      if (id == null) return Optional.empty();
      if (isAlpha2(id)) return Optional.of(id);
      return Optional.ofNullable(
         alpha2FromAlpha3(id)
         .orElse(alpha2FromName(id)
                 .orElse(null)));
   } // end of codeFromName()
   
   /**
    * Gets the standard English name of a language, given some language identifier.
    * @param id A string identifying the language, which may be an alpha-3 code, an
    * alpha-2 code, or a name. 
    * @return The English name of the language with the given ID, or an empty Optional 
    * if the identifier is unknown.
    */
   public Optional<String> name(String id) {
      if (id == null) return Optional.empty();
      if (isName(id)) return Optional.of(id);
      return Optional.ofNullable(
         nameFromAlpha3(id)
         .orElse(nameFromAlpha3(id)
                 .orElse(null)));
   } // end of codeFromName()
   
   /**
    * Provides a list of all known alpha-2 codes.
    * @return An alphabetical list of two-letter codes.
    */
   public Set<String> alpha2Codes() {
      return new TreeSet<String>(
         alpha2ToName.keySet().stream()
         .map(o->o.toString())
         .collect(Collectors.toSet()));
   } // end of codes()
   
   /**
    * Provides a list of all known alpha-3 codes.
    * @return An alphabetical list of three-letter codes.
    */
   public Set<String> alpha3Codes() {
      return new TreeSet<String>(
         alpha3ToAlpha2.keySet().stream()
         .map(o->o.toString())
         .collect(Collectors.toSet()));
   } // end of codes()
   
   /**
    * Provides a list of all known names.
    * @return An alphabetical list of language names.
    */
   public Set<String> names() {
      return new TreeSet<String>(
         nameToAlpha2.keySet().stream()
         .map(o->o.toString())
         .collect(Collectors.toSet()));
   } // end of names()

} // end of class ISO639
