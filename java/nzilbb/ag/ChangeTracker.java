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
package nzilbb.ag;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Something that listens for {@link Change}s to {@link TrackedMap} objects. 
 * @author Robert Fromont robert@fromont.net.nz
 */

public class ChangeTracker
   implements Consumer<Change>
{
   // map of object IDs to attribute maps, which are keyed by attribute key
   private HashMap<String,HashMap<String,Change>> idToChanges
   = new HashMap<String,HashMap<String,Change>>();

   /**
    * Consumers to pass the events to.
    * @see #getOthers()
    * @see #setOthers(LinkedHashSet)
    */
   protected LinkedHashSet<Consumer<Change>> others = new LinkedHashSet<Consumer<Change>>();
   /**
    * Getter for {@link #others}: Consumers to pass the events to.
    * @return Consumers to pass the events to.
    */
   public LinkedHashSet<Consumer<Change>> getOthers() { return others; }
   /**
    * Setter for {@link #others}: Consumers to pass the events to.
    * @param newOthers Consumers to pass the events to.
    */
   public ChangeTracker setOthers(LinkedHashSet<Consumer<Change>> newOthers) { others = newOthers; return this; }
   
   /**
    * Register a {@link TrackedMap} object {@link Change}.
    * @param change The change, which includes information about the object that is
    * changing, which attribute, what the old value was, and what the new value is.
    */
   public void accept(Change change)
   {
      final Change changeForListeners = change;
      if (change != null)
      {      
         String id = change.getObject().getId();
         if (id != null)
         {
            if (!idToChanges.containsKey(id)) idToChanges.put(id, new HashMap<String,Change>());

            HashMap<String,Change> attributeMap = idToChanges.get(id);
            if (attributeMap.containsKey(change.getKey()))
            { // attribute has changed previously
               // take a copy of the change, so that listeners get an unaltered version
               change = (Change)change.clone();
               
               // conserve the original old value
               Change earlierChange = attributeMap.get(change.getKey());
               change.setOldValue(earlierChange.getOldValue());
            }
            // System.out.println("ChangeTracker.accept: " + change);
            attributeMap.put(change.getKey(), change);
         } // id != null
      } // change != null
      // pass on the change to any listeners that are interested
      others.forEach(o -> o.accept(changeForListeners));
   }
   
   /**
    * De-register a {@link TrackedMap} object {@link Change}.
    * @param change The change, which should have been previously registered via 
    * {@link #accept(Change)}. 
    */
   public void reject(Change change)
   {
      if (change == null) return;
      
      String id = change.getObject().getId();
      if (id == null) return;

      if (!idToChanges.containsKey(id)) return;
      
      HashMap<String,Change> attributeMap = idToChanges.get(id);
      attributeMap.remove(change.getKey());

      // ensure that, if all changes are rejected, idToChanges is empty
      if (attributeMap.size() == 0) idToChanges.remove(id);
   }
   
   /**
    * Gets all the changes for the given attribute of the given object.
    * @param id The {@link TrackedMap#id} of the changed object. 
    * @param key The key of the changed attribute, or null for Create/Destroy changes.
    * @return A (possibly empty) set of changes that were registered.
    */
   public Optional<Change> getChange(String id, String key)
   {      
      if (id == null) return Optional.empty();
      
      if (!idToChanges.containsKey(id)) return Optional.empty();
      
      HashMap<String,Change> attributeMap = idToChanges.get(id);
      if (!attributeMap.containsKey(key)) return Optional.empty();
      
      return Optional.ofNullable(attributeMap.get(key));
   }
   
   /**
    * Gets all the changes for the identified object.
    * @param id The {@link TrackedMap#id} of the changed object. 
    * @return A (possibly empty) set of changes that were registered.
    */
   public Set<Change> getChanges(String id)
   {
      final LinkedHashSet<Change> changes = new LinkedHashSet<Change>();
      if (id != null)
      {
         if (idToChanges.containsKey(id))
         {
            idToChanges.get(id).values().stream()
               .sorted(new Comparator<Change>(){
                     public int compare(Change c1, Change c2)
                     {
                        if (c1.getOperation() == Change.Operation.Create) return -1;
                        if (c2.getOperation() == Change.Operation.Create) return 1;
                        if (c1.getOperation() == Change.Operation.Destroy) return 1;
                        if (c2.getOperation() == Change.Operation.Destroy) return -1;
                        return c1.getKey().compareTo(c2.getKey());
                     }
                  })
               .forEach(c -> changes.add(c));
         } // idToChanges.containsKey(id)
      } // id != null
      // System.out.println("Changes for " + id + ": " + changes);
      return changes;
   }
   
   /**
    * Determines whether there are any changes.
    * @return true if there are any changes registered, false otherwise.
    */
   public boolean hasChanges()
   {
      return idToChanges.size() > 0;
   } // end of hasChanges()

   /**
    * Gets all the changes.
    * @return A (possibly empty) set of changes that were registered.
    */
   public Set<Change> getChanges()
   {
      final LinkedHashSet<Change> changes = new LinkedHashSet<Change>();
      idToChanges.keySet().forEach(id -> changes.addAll(getChanges(id)));
      return changes;
   }
   
   /**
    * Clear all tracked changes. After calling this, previously registered changes cannot
    * be rolled back.
    */
   public void reset()
   {
      idToChanges = new HashMap<String,HashMap<String,Change>>();
   } // end of reset()

   /**
    * Add another consumer to also receive the changes.
    * @param other
    */
   public void addListener(Consumer<Change> other)
   {
      if (other != null) others.add(other);
   } // end of addListener()   

   /**
    * Stop a registered consumer receiving further the changes.
    * @param other
    */
   public void removeListener(Consumer<Change> other)
   {
      if (other != null) others.remove(other);
   } // end of removeListener()

} // end of ChangeTracker
