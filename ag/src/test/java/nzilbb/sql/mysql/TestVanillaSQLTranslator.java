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
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import nzilbb.ag.automation.*;
import nzilbb.ag.automation.util.*;
import nzilbb.sql.mysql.VanillaSQLTranslator;

public class TestVanillaSQLTranslator {
   
   @Test public void create() {
      
      VanillaSQLTranslator t = new VanillaSQLTranslator();
      assertEquals("Engine, trailing semicolon, and trim",
                   "CREATE TABLE t (field VARCHAR(100))",
                   t.apply(" CREATE TABLE t (field VARCHAR(100)) ENGINE=MyISAM; "));
      assertEquals("COMMENT",
                   "CREATE TABLE t (field VARCHAR(100))",
                   t.apply(
                      "CREATE TABLE t (field VARCHAR(100)"
                      +" COMMENT 'this comment should be stripped')"));
      assertEquals("Character set utf8mb4",
                   "CREATE TABLE t (field VARCHAR(100))",
                   t.apply("CREATE TABLE t (field VARCHAR(100) CHARACTER SET utf8mb4)"));
      assertEquals("Character set and collation utf8mb4",
                   "CREATE TABLE t (field VARCHAR(100))",
                   t.apply(
                      "CREATE TABLE t ("
                      +"field VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci)"));
      assertEquals("NULL and NOT NULL",
                   "CREATE TABLE t (field1 VARCHAR(100),field2 VARCHAR(100))",
                   t.apply(
                      "CREATE TABLE t ("
                      +"field1 VARCHAR(100) NULL,"
                      +"field2 VARCHAR(100) NOT NULL"
                      +")"));
      assertEquals("BIT -> SMALLINT",
                   "CREATE TABLE t (field SMALLINT)",
                   t.apply("CREATE TABLE t (field BIT)"));
   }
   
   @Test public void replaceInto() {
      
      VanillaSQLTranslator t = new VanillaSQLTranslator();
      assertEquals("REPLACE -> INSERT",
                   "INSERT INTO t (field1, field2) VALUES ('REPLACE INTO', 1)",
                   t.apply("REPLACE INTO t (field1, field2) VALUES ('REPLACE INTO', 1)"));
      assertEquals("NULL and NOT NULL not removed",
                   "INSERT INTO t1 (field)"
                   +" SELECT field FROM t2 WHERE field1 IS NOT NULL AND field2 IS NULL",
                   t.apply("INSERT INTO t1 (field)"
                           +" SELECT field FROM t2 WHERE field1 IS NOT NULL AND field2 IS NULL"));
   }

   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.sql.mysql.TestVanillaSQLTranslator");
   }
}
