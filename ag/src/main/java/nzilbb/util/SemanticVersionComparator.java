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
package nzilbb.util;

import java.util.Comparator;
import java.text.MessageFormat;
import java.text.ParseException;

/**
 * Compares semantic version strings.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class SemanticVersionComparator implements Comparator<String> {

   MessageFormat versionParser = new MessageFormat(
      "{0,number,integer}.{1,number,integer}.{2,number,integer}{3}");
   MessageFormat versionParserHyphenPatch = new MessageFormat(
      "{0,number,integer}.{1,number,integer}-{2,number,integer}{3}");
   /**
    * Default constructor.
    */
   public SemanticVersionComparator() {
   } // end of constructor

   /**
    * Compares its two arguments for order. Returns a negative integer, zero, or a
    * positive integer as the first argument is less than, equal to, or greater than the
    * second. 
    */
   public int compare(String version1, String version2) {
      
      // try to interpret the strings as semantic versions
      Object[] parse1 = null;
      try {
         parse1 = versionParser.parse(version1);
      } catch(ParseException exception) {
        try {
          parse1 = versionParserHyphenPatch.parse(version1);
        } catch(ParseException exception2) {}
      }
      Object[] parse2 = null;
      try {
         parse2 = versionParser.parse(version2);
      } catch(ParseException exception) {
        try {
          parse2 = versionParserHyphenPatch.parse(version2);
        } catch(ParseException exception2) {}
      }

      // if they're both parseable
      if (parse1 != null && parse2 != null) {
         // compare their version parts
         Long major1 = (Long)parse1[0];
         Long minor1 = (Long)parse1[1];
         Long patch1 = (Long)parse1[2];
         String suffix1 = (String)parse1[3];
         Long major2 = (Long)parse2[0];
         Long minor2 = (Long)parse2[1];
         Long patch2 = (Long)parse2[2];
         String suffix2 = (String)parse2[3];
         int comparison = major1.compareTo(major2);
         if (comparison != 0) return comparison;
         comparison = minor1.compareTo(minor2);
         if (comparison != 0) return comparison;
         comparison = patch1.compareTo(patch2);
         if (comparison != 0) return comparison;
         // the version with a suffix is less than the version without
         if (suffix1.length() == 0 && suffix2.length() > 0) return 1;
         if (suffix2.length() == 0 && suffix1.length() > 0) return -1;         
         if (suffix2.length() > 0 && suffix1.length() > 0) return suffix1.compareTo(suffix2);
         // if we got this far, everything is the same
         return 0;
      } else if (parse1 != null) { // version1 is semantic, so it is 'greater than' version2
         return 10;
      } else if (parse2 != null) { // version2 is semantic, so it is 'greater than' version1
         return -10;
      } else { // neither is semantic, so compare then as plain character strings
         return version1.compareTo(version2);
      }
   }
} // end of class SemanticVersionComparator
