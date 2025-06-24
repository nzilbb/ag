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
package nzilbb.ag.ql;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Useful functions relating to QL.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class QL {
  static Pattern quotePattern = Pattern.compile("(^|[^\\\\])'");
  
  /**
   * Escapes single quotes (only) in the given string for inclusion in QL or SQL queries.
   * @param s The string to escape.
   * @return The given string, with quotes escaped.
   */
  public static String Esc(String s) {
    // if (s == null) return "";
    // String escaped = s;
    // // ensure that any single quotes are escaped
    // // replaceAll() doesn't work for consecutive quotes so we need to create
    // // a new match for each replacement
    // Matcher matcher = quotePattern.matcher(escaped);
    // while (matcher.find()) {
    //   escaped = matcher.replaceFirst("$1\\\\'");
    //   // look again, including replacement we just match
    //   matcher = quotePattern.matcher(escaped);
    // }
    StringBuilder escaped = new StringBuilder();
    if (s != null) {
      // move through character by character looking for ' and \
      for (int c = 0; c < s.length(); c++) {
        char thisChar = s.charAt(c);
        char nextChar = c < s.length()-1?s.charAt(c+1):'\0';
        if (thisChar == '\\') { // backslash
          if (nextChar == '\\' || nextChar == '\'') { // it's quoting a backslash or quote
            escaped.append(thisChar); // pass through the backslash
            escaped.append(nextChar); // and what it's escaping
            c++; // and skip the next char
          } else {
            escaped.append(thisChar); // just pass it through
          }
        } else if (thisChar == '\'') { // single quote
          escaped.append('\\'); // escape it
          escaped.append(thisChar); // pass through the quote
        } else {
          escaped.append(thisChar); // just pass it through
        }
      } // next character
    }
    return escaped.toString();
  } // end of esc()
} // end of class QL
