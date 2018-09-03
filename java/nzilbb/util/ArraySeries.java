//
// Copyright 2018 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of LaBB-CAT.
//
//    LaBB-CAT is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    LaBB-CAT is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with LaBB-CAT; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package nzilbb.util;

import java.util.NoSuchElementException;

/**
 * Implementation of ISeries that enumerates arrays.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class ArraySeries<T>
   implements ISeries<T>
{
   // Attributes:

   
   /**
    * The underlying array.
    * @see #getArray()
    * @see #setArray(T[])
    */
   protected T[] array;
   /**
    * Getter for {@link #array}: The underlying array.
    * @return The underlying array.
    */
   public T[] getArray() { return array; }
   /**
    * Setter for {@link #array}: The underlying array.
    * @param newArray The underlying array.
    */
   public void setArray(T[] newArray) { array = newArray; }


   /**
    * Index of current position.
    * @see #getI()
    * @see #setI(int)
    */
   protected int i = 0;
   /**
    * Getter for {@link #i}: Index of current position.
    * @return Index of current position.
    */
   public int getI() { return i; }
   /**
    * Setter for {@link #i}: Index of current position.
    * @param newI Index of current position.
    */
   public void setI(int newI) { i = newI; }
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public ArraySeries(T[] array)
   {
      setArray(array);
   } // end of constructor

   // Enumeration methods

   /**
    * Tests if this enumeration contains more elements.
    */
   public boolean hasMoreElements()
   {
      return i < array.length;
   }

   /**
    * Returns the next element of this enumeration if this enumeration object has at least one more element to provide.
    */
   public T nextElement()
      throws NoSuchElementException
   {
      return array[i++];
   }

   // Series methods

   /**
    * Counts the elements in the series, if possible.
    * @return The number of elements in the series, or null if the number is unknown.
    */
   public Long countElements()
   {
      try
      {
	 return new Long(array.length);
      }
      catch(NullPointerException exception)
      {
	 return null;
      }
   }
   
   /**
    * Determines how far through the serialization is.
    * @return An integer between 0 and 100 (inclusive), or null if progress can not be calculated.
    */
   public Integer percentComplete()
   {
      try
      {
	 return (i * 100) / array.length;
      }
      catch(NullPointerException exception)
      {
	 return null;
      }
   }   
   
} // end of class ArraySeries
