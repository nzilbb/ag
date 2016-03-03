//
// Copyright 2015 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.configure;

/**
 * A parameter that needs to be set for a some operation or configuration.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class Parameter
{
   // Attributes:

   /**
    * The paramater's name.
    * @see #getName()
    * @see #setName(String)
    */
   protected String name;
   /**
    * Getter for {@link #name}: The paramater's name.
    * @return The paramater's name.
    */
   public String getName() { return name; }
   /**
    * Setter for {@link #name}: The paramater's name.
    * @param newName The paramater's name.
    */
   public void setName(String newName) { name = newName; }

   /**
    * A label that might be presented to a user.
    * @see #getLabel()
    * @see #setLabel(String)
    */
   protected String label;
   /**
    * Getter for {@link #label}: A label that might be presented to a user.
    * @return A label that might be presented to a user.
    */
   public String getLabel() { if (label != null ) { return label; } else { return getName(); } }
   /**
    * Setter for {@link #label}: A label that might be presented to a user.
    * @param newLabel A label that might be presented to a user.
    */
   public void setLabel(String newLabel) { label = newLabel; }

   /**
    * A text hint that might be displayed to a user.
    * @see #getHint()
    * @see #setHint(String)
    */
   protected String hint;
   /**
    * Getter for {@link #hint}: A text hint that might be displayed to a user.
    * @return A text hint that might be displayed to a user.
    */
   public String getHint() { if (hint != null) { return hint; } else { return getLabel(); } }
   /**
    * Setter for {@link #hint}: A text hint that might be displayed to a user.
    * @param newHint A text hint that might be displayed to a user.
    */
   public void setHint(String newHint) { hint = newHint; }

   /**
    * The type of the parameter.
    * @see #getType()
    * @see #setType(String)
    */
   protected String type;
   /**
    * Getter for {@link #type}: The type of the parameter.
    * @return The type of the parameter.
    */
   public String getType() { return type; }
   /**
    * Setter for {@link #type}: The type of the parameter.
    * @param newType The type of the parameter.
    */
   public void setType(String newType) { type = newType; }

   /**
    * The value (or default value) of the parameter.
    * @see #getValue()
    * @see #setValue(Object)
    */
   protected Object value;
   /**
    * Getter for {@link #value}: The value (or default value) of the parameter.
    * @return The value (or default value) of the parameter.
    */
   public Object getValue() { return value; }
   /**
    * Setter for {@link #value}: The value (or default value) of the parameter.
    * @param newValue The value (or default value) of the parameter.
    */
   public void setValue(Object newValue) { value = newValue; }

   
   /**
    * Whether the parameter is required (true) or optional (false - the default).
    * @see #getRequired()
    * @see #setRequired(boolean)
    */
   protected boolean required = false;
   /**
    * Getter for {@link #required}: Whether the parameter is required (true) or optional (false - the default).
    * @return Whether the parameter is required (true) or optional (false).
    */
   public boolean getRequired() { return required; }
   /**
    * Setter for {@link #required}: Whether the parameter is required (true) or optional (false).
    * @param newRequired Whether the parameter is required (true) or optional (false).
    */
   public void setRequired(boolean newRequired) { required = newRequired; }

   
   // Methods:
   
   /**
    * Default constructor.
    */
   public Parameter()
   {
   } // end of constructor

   /**
    * Constructor from attributes.
    * @param name The paramater's name.
    * @param label A label that might be presented to a user.
    * @param hint A text hint that might be displayed to a user.
    * @param type The type of the parameter.
    * @param required Whether the parameter is required (true) or optional (false).
    * @param value The value (or default value) of the parameter.
    */
   public Parameter(String name, String type, String label, String hint, boolean required, Object value)
   {
      setName(name);
      setType(type);
      setLabel(label);
      setHint(hint);
      setRequired(required);
      setValue(value);
   } // end of constructor
   /**
    * Constructor from attributes.
    * @param name The paramater's name.
    * @param label A label that might be presented to a user.
    * @param hint A text hint that might be displayed to a user.
    * @param type The type of the parameter.
    * @param required Whether the parameter is required (true) or optional (false).
    */
   public Parameter(String name, String type, String label, String hint, boolean required)
   {
      setName(name);
      setType(type);
      setLabel(label);
      setHint(hint);
      setRequired(required);
   } // end of constructor
   /**
    * Constructor from attributes.
    * @param name The paramater's name.
    * @param label A label that might be presented to a user.
    * @param hint A text hint that might be displayed to a user.
    * @param type The type of the parameter.
    * @param value The value (or default value) of the parameter.
    */
   public Parameter(String name, String type, String label, String hint, Object value)
   {
      setName(name);
      setType(type);
      setLabel(label);
      setHint(hint);
      setValue(value);
   } // end of constructor
   /**
    * Constructor from attributes.
    * @param name The paramater's name.
    * @param label A label that might be presented to a user.
    * @param hint A text hint that might be displayed to a user.
    * @param type The type of the parameter.
    */
   public Parameter(String name, String type, String label, String hint)
   {
      setName(name);
      setType(type);
      setLabel(label);
      setHint(hint);
   } // end of constructor
   /**
    * Constructor from attributes.
    * @param name The paramater's name.
    * @param label A label that might be presented to a user.
    * @param hint A text hint that might be displayed to a user.
    */
   public Parameter(String name, String type, String label)
   {
      setName(name);
      setType(type);
      setLabel(label);
   } // end of constructor
   /**
    * Constructor from attributes.
    * @param name The paramater's name.
    * @param type The type of the parameter.
    */
   public Parameter(String name, String type)
   {
      setName(name);
      setType(type);
   } // end of constructor
} // end of class SerializationParameter
