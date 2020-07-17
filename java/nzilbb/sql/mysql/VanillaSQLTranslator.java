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
package nzilbb.sql.mysql;

/**
 * Object that translates statements designed for MySQL's flavour of SQL to standard SQL.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class VanillaSQLTranslator extends MySQLTranslator {

   /** Default constructor */
   public VanillaSQLTranslator() {
      rdbms = "SQL";
   }
   
   /** 
    * Translate the given statement.
    * <p> This implementation strips out (many) MySQL-specific syntax.
    * @param sql The SQL statement to translate
    * @return The translated version of the the SQL statement.
    */
   public String apply(String sql) {
      if (trace) System.out.println("SQL before: " + sql);
      String translated = sql.trim();
      // TODO COALESCE
      if (translated.startsWith("REPLACE INTO")) {
         translated = translated
            .replaceFirst("REPLACE INTO","INSERT INTO"); // this isn't strictly valid!
      } else if (translated.startsWith("CREATE TABLE")) {
         translated = translated
            .replace(" ENGINE=MyISAM","")
            .replace(" CHARACTER SET utf8mb4","")
            .replace(" COLLATE utf8mb4_general_ci","")
            .replace(" NOT NULL","")
            .replace(" NULL","")
            .replace(" BIT"," SMALLINT")
            .replaceAll(" COMMENT '[^']*'","");
      }
      if (translated.endsWith(";")) translated = translated.substring(0, translated.length() - 1);
      translated = translated.trim();
      if (trace) System.out.println("SQL after: " + translated);
      return translated;
   }
   
} // end of class VanillaSQLTranslator
