//
// Copyright 2019 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.sql;

import java.util.List;

/**
 * Exception thrown when an AGQL expression is invalid.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class AGQLException
  extends Exception
{
  // Attributes:

  /**
   * AGQL expression that caused the error.
   * @see #getExpression()
   * @see #setExpression(String)
   */
  protected String expression;
  /**
   * Getter for {@link #expression}.
   * @return AGQL expression that caused the error.
   */
  public String getExpression() { return expression; }
  /**
   * Setter for {@link #expression}.
   * @param expression AGQL expression that caused the error.
   * @return <var>this</var>.
   */
  public AGQLException setExpression(String expression) { this.expression = expression; return this; }
  
  /**
   * List of errors.
   * @see #getErrors()
   * @see #setErrors(List)
   */
  protected List<String> errors;
  /**
   * Getter for {@link #errors}.
   * @return List of errors.
   */
  public List<String> getErrors() { return errors; }
  /**
   * Setter for {@link #errors}.
   * @param errors List of errors.
   * @return <var>this</var>.
   */
  public AGQLException setErrors(List<String> errors) { this.errors = errors; return this; }
  
  // Methods:
  
  /**
   * Default constructor.
   */
  public AGQLException()
  {
  } // end of constructor
  
  /**
   * Constructor with message.
   */
  public AGQLException(String message)
  {
    super(message);
  } // end of constructor

  /**
   * Constructor.
   */
  public AGQLException(String expression, List<String> errors)
  {
    super("Error parsing \""+expression+"\": " + errors.toString());
    setExpression(expression);
    setErrors(errors);
  } // end of constructor

} // end of class AGQLException
