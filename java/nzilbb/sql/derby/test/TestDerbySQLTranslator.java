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

package nzilbb.sql.derby.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import nzilbb.ag.automation.*;
import nzilbb.ag.automation.util.*;
import nzilbb.sql.derby.DerbySQLTranslator;

public class TestDerbySQLTranslator {
   
   @Test public void create() {
      
      DerbySQLTranslator t = new DerbySQLTranslator();
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
      
      DerbySQLTranslator t = new DerbySQLTranslator();
      assertEquals("REPLACE -> INSERT",
                   "INSERT INTO t (field1, field2) VALUES ('REPLACE INTO', 1)",
                   t.apply("REPLACE INTO t (field1, field2) VALUES ('REPLACE INTO', 1)"));
      assertEquals("NULL and NOT NULL not removed",
                   "INSERT INTO t1 (field)"
                   +" SELECT field FROM t2 WHERE field1 IS NOT NULL AND field2 IS NULL",
                   t.apply("INSERT INTO t1 (field)"
                           +" SELECT field FROM t2 WHERE field1 IS NOT NULL AND field2 IS NULL"));
   }

   @Test public void limit() {
      
      DerbySQLTranslator t = new DerbySQLTranslator();
      assertEquals("LIMIT l -> FETCH NEXT l ROWS ONLY",
                   "SELECT a FROM b FETCH NEXT 10 ROWS ONLY",
                   t.apply("SELECT a FROM b LIMIT 10"));
      assertEquals("LIMIT p, l -> oOFFSET p ROWS FETCH NEXT l ROWS ONLY",
                   "SELECT a FROM b OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY",
                   t.apply("SELECT a FROM b LIMIT 20, 10"));
   }

   @Test public void booleans() {
      
      DerbySQLTranslator t = new DerbySQLTranslator();
      assertEquals("TRUE -> <> 0",
                   "SELECT a FROM b WHERE c <> 0",
                   t.apply("SELECT a FROM b WHERE c = TRUE"));
      assertEquals("FALSE -> 0",
                   "SELECT a FROM b WHERE c = 0",
                   t.apply("SELECT a FROM b WHERE c = FALSE"));
   }

   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.sql.derby.test.TestDerbySQLTranslator");
   }
}
