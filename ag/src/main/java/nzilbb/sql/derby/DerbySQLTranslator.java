//
// Copyright 2020-2023 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.sql.derby;

import nzilbb.sql.mysql.VanillaSQLTranslator;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

// TODO implement REGEXP function, something like:
// TODO  CREATE FUNCTION COALESCE
// TODO  ( P1 VARCHAR, P2 VARCHAR )
// TODO  RETURNS VARCHAR
// TODO  PARAMETER STYLE JAVA
// TODO  NO SQL LANGUAGE JAVA
// TODO  CALLED ON NULL INPUT
// TODO  DETERMINISTIC
// TODO  EXTERNAL NAME 'nzilbb.derby.MySQLFunction.REGEXP'

/**
 * Object that translates statements designed for MySQL's flavour of SQL to the 
 * <a href="https://db.apache.org/derby/"> derby </a> SQL engine. 
 * @author Robert Fromont robert@fromont.net.nz
 */
public class DerbySQLTranslator extends VanillaSQLTranslator {

  /** Default constructor */
  public DerbySQLTranslator() {
    rdbms = "derby";
  }
  
  /** 
   * Translate the given statement.
   * <p> This first uses the general translations made by {@link VanillaSQLTranslator}, then
   * converts MySQL-specific constructions to Derby-specific ones. 
   * @param sql The SQL statement to translate
   * @return The translated version of the the SQL statement.
   */
  @Override public String apply(String sql) {
    String translated = super.apply(sql);
    translated = translated
      .replaceAll("\\s+LIMIT\\s+([0-9]+)\\s*,\\s*([0-9]+)", " OFFSET $1 ROWS FETCH NEXT $2 ROWS ONLY")
      .replaceAll("\\s+LIMIT\\s+([0-9]+)", " FETCH NEXT $1 ROWS ONLY")
      .replaceAll("`([^`]+)`", "\"$1\"")
      .replace(" = TRUE", " <> 0")
      .replace(" = FALSE", " = 0")
      .replaceAll("\\s+AUTO_INCREMENT",
                  " GENERATED ALWAYS AS IDENTITY(Start with 1, Increment by 1)")
      .replaceAll(",?\\s*INDEX\\s+\\w+\\s*\\([^)]+\\)","")
      // Grr! CASTing VARCHAR to BLOB is not supported in Derby, so BINARY comparison can't work
      // // 'x' = BINARY 'y'
      // .replaceAll("\\s+(['\"])([^'\"=]+)\\1\\s+=\\s+BINARY\\s+(['\"])([^'\"]+)\\3", 
      //             " CAST($1$2$1 AS BLOB) = CAST($3$4$3 AS BLOB)")
      // // x = BINARY 'y'
      // .replaceAll("\\s+(\\w+)\\s+=\\s+BINARY\\s+(['\"])([^'\"]+)\\2",
      //             " CAST($1 AS BLOB) = CAST($2$3$2 AS BLOB)")
      // // 'x' = BINARY y
      // .replaceAll("\\s+(['\"])([^'\"=]+)\\1\\s+=\\s+BINARY\\s+(\\S+)",
      //             " CAST($1$2$1 AS BLOB) = CAST($3 AS BLOB)")
      // // x = BINARY y
      // .replaceAll("\\s+(\\w+)\\s+=\\s+BINARY\\s+(\\S+)", 
      //             " CAST($1 AS BLOB) = CAST($2 AS BLOB)")
      .replaceAll("\\s+BINARY\\s+"," ")
      .replace("LAST_INSERT_ID()", "IDENTITY_VAL_LOCAL()");
    if (trace) System.out.println("SQL after: " + translated);
    return translated;
  }

  /**
   * Creates custom functions.
   * @param connection An open connection.
   * @return The given connection.
   * @throws SQLException
   */
  public static Connection CreateFunctions(Connection connection) throws SQLException {
    try {
      PreparedStatement sql = connection.prepareStatement("DROP FUNCTION REPLACE");
      try {
        sql.executeUpdate();
        sql.close();
      } catch (SQLException x) {}
    } catch (SQLException x) {}
    PreparedStatement sql = connection.prepareStatement(
      "CREATE FUNCTION REPLACE"
      +" ( HAYSTACK VARCHAR(32672), NEEDLE VARCHAR(32672), REPLACEMENT VARCHAR(32672) )"
      +" RETURNS VARCHAR(32672)"
      +" PARAMETER STYLE JAVA"
      +" NO SQL LANGUAGE JAVA"
      +" CALLED ON NULL INPUT"
      +" DETERMINISTIC"
      +" EXTERNAL NAME 'nzilbb.derby.DerbySQLTranslator.REPLACE'");
    sql.executeUpdate();
    sql.close();
    return connection;
  } // end of CreateFunctions()
  
  /**
   * Implementation for the REPLACE SQL function.
   * @param haystack
   * @param needle
   * @param replacement
   * @return The given string with replacements made.
   */
  public static String REPLACE(String haystack, String needle, String replacement) {
    if (haystack == null) return null;
    if (needle == null) return haystack;
    if (replacement == null) replacement = "";
    return haystack.replace(needle, replacement);
  } // end of REPLACE()
  
} // end of class DerbySQLTranslator
