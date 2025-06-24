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

import org.junit.*;
import static org.junit.Assert.*;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/** Test QL convenience functions. */
public class TestQL {
  /** Test quote escaping. */
  @Test public void Esc() {
    assertEquals("No single quotes", "\"test\"", QL.Esc("\"test\""));

    // prefix single quotes with backslash
    assertEquals("One single quote", "O\\'Rourke", QL.Esc("O'Rourke"));
    assertEquals("Multiple single quotes", "O\\'Rourke\\'s", QL.Esc("O'Rourke's"));
    assertEquals("Leading single quote", "\\'Rourke", QL.Esc("'Rourke"));
    assertEquals("Trailing single quote", "teachers\\'", QL.Esc("teachers'"));
    
    // no change if the backslash is already there
    assertEquals("One single quote - already escaped",
                 "O\\'Rourke", QL.Esc("O\\'Rourke"));
    assertEquals("Multiple single quotes - already escaped",
                 "O\\'Rourke\\'s", QL.Esc("O\\'Rourke\\'s"));
    assertEquals("Leading single quote - already escaped",
                 "\\'Rourke", QL.Esc("\\'Rourke"));
    assertEquals("Trailing single quote - already escaped",
                 "teachers\\'", QL.Esc("teachers\\'"));

    // don't get confused by literal backslashes
    // i.e. escaped backslash followed by unescaped quote
    assertEquals("One single quote - preceded by escaped backslash",
                 "O\\\\\\'Rourke", QL.Esc("O\\\\'Rourke"));
    assertEquals("Multiple single quotes - preceded by escaped backslash",
                 "O\\\\\\'Rourke\\\\\\'s", QL.Esc("O\\\\'Rourke\\\\'s"));
    assertEquals("Leading single quote - preceded by escaped backslash",
                 "\\\\\\'Rourke", QL.Esc("\\\\'Rourke"));
    assertEquals("Trailing single quote - preceded by escaped backslash",
                 "teachers\\\\\\'", QL.Esc("teachers\\\\'"));
    
    // and escaped backslash followed by escaped quote
    assertEquals("One single quote - preceded by escaped backslash",
                 "O\\\\\\'Rourke", QL.Esc("O\\\\\\'Rourke"));
    assertEquals("Multiple single quotes - preceded by escaped backslash",
                 "O\\\\\\'Rourke\\\\\\'s", QL.Esc("O\\\\\\'Rourke\\\\\\'s"));
    assertEquals("Leading single quote - preceded by escaped backslash",
                 "\\\\\\'Rourke", QL.Esc("\\\\\\'Rourke"));
    assertEquals("Trailing single quote - preceded by escaped backslash",
                 "teachers\\\\\\'", QL.Esc("teachers\\\\\\'"));
    
  }
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.ag.ql.TestQL");
  }

}
