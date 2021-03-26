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
package nzilbb.sql;

import java.sql.Connection;
import java.sql.SQLException;
import nzilbb.sql.mysql.MySQLTranslator;

/**
 * Interface for a factory that supplies new SQL connections and SQL Translators.
 * @author Robert Fromont robert@fromont.net.nz
 */

public interface ConnectionFactory {
   
   /**
    * Creates a new database connection.
    * @return A connected database connection.
    * @throws SQLException If there's a problem connecting to the database.
    */
   public Connection newConnection() throws SQLException;
   
   /**
    * Constructs an SQL translator appropriate for the type of connection made by 
    * {@link #newConnection()}. 
    * @return An SQL translator.
    */
   public MySQLTranslator newSQLTranslator();

} // end of class ConnectionFactory
