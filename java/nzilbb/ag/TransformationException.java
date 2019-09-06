//
// Copyright 2015-2016 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag;

/**
 * An exception ocurring during a graph transformation.
 * @author Robert Fromont robert@fromont.net.nz
 * @see IGraphTransformer
 */
@SuppressWarnings("serial")
public class TransformationException
   extends Exception
{
   
   /**
    * The transformer rasing the exception.
    * @see #getTransformer()
    * @see #setTransformer(IGraphTransformer)
    */
   protected IGraphTransformer transformer;
   /**
    * Getter for {@link #transformer}: The transformer rasing the exception.
    * @return The transformer rasing the exception.
    */
   public IGraphTransformer getTransformer() { return transformer; }
   /**
    * Setter for {@link #transformer}: The transformer rasing the exception.
    * @param newTransformer The transformer rasing the exception.
    */
   public TransformationException setTransformer(IGraphTransformer newTransformer) { transformer = newTransformer; return this; }

   /**
    * Default constructor.
    * @param transformer The transformer rasing the exception.
    */
   public TransformationException(IGraphTransformer transformer)
   {
      setTransformer(transformer);
   } // end of constructor
   /**
    * Constructor with message.
    * @param transformer The transformer rasing the exception.
    * @param message The error message.
    */
   public TransformationException(IGraphTransformer transformer, String message)
   {
      super(message);
      setTransformer(transformer);
   } // end of constructor
   /**
    * Constructor with cause.
    * @param transformer The transformer rasing the exception.
    * @param cause The root cause of the error.
    */
   public TransformationException(IGraphTransformer transformer, Throwable cause)
   {
      super(cause);
      setTransformer(transformer);
   } // end of constructor
   /**
    * Constructor with message and cause.
    * @param transformer The transformer rasing the exception.
    * @param message The error message.
    * @param cause The root cause of the error.
    */
   public TransformationException(IGraphTransformer transformer, String message, Throwable cause)
   {
      super(message, cause);
      setTransformer(transformer);
   } // end of constructor
} // end of class TransformationException
