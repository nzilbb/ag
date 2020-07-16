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
package nzilbb.ag.automation;

import java.util.function.UnaryOperator;

/**
 * Object that translates statements designed for MySQL's flavour of SQL to the flavour
 * used by the graph service.
 * <p> Where necessary for performance or functionality, annotators may need to use
 * features of SQL that are specific to MySQL, e.g. specifying the <tt> ENGINE </tt> when
 * creating tables, using the <q>utf8mb4</q> <tt> CHARACTER SET </tt> etc.
 * <p> For implementations that don't use MySQL (e.g. for local command-line annatotator
 * processing), this class provides the possibility of intercepting SQL statements and
 * converting them into variants that will work with whatever RDBMS has been chosen. Other
 * implementations migth simply strip out unsupported syntax, or may convert operators or
 * functions to equivalants.
 * <p> The default implementation simply passes statements through unchanged.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class MySQLTranslator implements UnaryOperator<String> {
   
   /**
    * Name of the Relational Database Management System the translator is for.
    * @see #getRdbms()
    */
   protected String rdbms = "MySQL";
   /**
    * Getter for {@link #rdbms}: Name of the Relational Database Management System the
    * translator is for. 
    * @return Name of the Relational Database Management System the translator is for.
    */
   public String getRdbms() { return rdbms; }

   /**
    * Whether to print statements passed to {@link #apply(String)}
    * @see #getTrace()
    * @see #setTrace(boolean)
    */
   protected boolean trace = true;
   /**
    * Getter for {@link #trace}: Whether to print statements passed to {@link #apply(String)}
    * @return Whether to print statements passed to {@link #apply(String)}
    */
   public boolean getTrace() { return trace; }
   /**
    * Setter for {@link #trace}: Whether to print statements passed to {@link #apply(String)}
    * @param newTrace Whether to print statements passed to {@link #apply(String)}
    */
   public MySQLTranslator setTrace(boolean newTrace) { trace = newTrace; return this; }

   /** 
    * Translate the given statement.
    * <p> This implementation simply returns the given statement.
    * @param sql The SQL statement to translate
    * @return The translated version of the the SQL statement.
    */
   public String apply(String sql) {
      if (trace) System.out.println(sql);
      return sql;
   }
   
} // end of class MySQLTranslator
