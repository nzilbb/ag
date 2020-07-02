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

import java.util.Vector;
import nzilbb.ag.*;
import nzilbb.util.MonitorableTask;

/**
 * Interface an automated annotation module must implement.
 * <p> When {@link IGraphTransformer#transform(Graph)} is invoked, it 
 * @author Robert Fromont robert@fromont.net.nz
 */
public abstract class Annotator implements IGraphTransformer, MonitorableTask {

   /**
    * Unique name for the annotator, which is immutable across versions of the implemetantation.
    * @return The annotator's ID.
    */
   public abstract String getAnnotatorId();

   /**
    * Version of this implementation; versions will typically be numeric, but this is not
    * a requirement. 
    * @return Annotator version.
    */
   public abstract String getVersion();
   
   /**
    * The layer schema.
    * @see #getSchema()
    * @see #setSchema(Schema)
    */
   protected Schema schema;
   /**
    * Getter for {@link #schema}: The layer schema.
    * @return The layer schema.
    */
   public Schema getSchema() { return schema; }
   /**
    * Setter for {@link #schema}: The layer schema.
    * @param newSchema The layer schema.
    */
   public Annotator setSchema(Schema newSchema) { schema = newSchema; return this; }
   
   /**
    * Runs any processing required to install the annotator.
    * <p> This processing is assumed to be synchronous (this method doesn't return until
    * it's complete) and long-running, so the {@link MonitorableTask} methods should
    * provide a way for the caller to monitor/cancel processing.
    * <p> If the user should provide information before this method is called, a 
    * <tt> config </tt> web-app must be provided to implement the user interface, which sets
    * any required configuration by invoking methods of the annotator as required, and
    * invoking <tt> install </tt> when configuration is ready.
    * @throws InvalidConfigurationException
    */
   public abstract void install() throws InvalidConfigurationException;
   
   /**
    * Runs any processing required to uninstall the annotator.
    */
   public void uninstall() { }
   
   /**
    * Sets the configuration for a given annotation task.
    * <p> If the user should provide information before an annotation task is run for the
    * first time, a <tt> task </tt> web-app must be provided to implement the user
    * interface, which may be provided with an existing configurtaion, and invoking
    * <tt> setTaskParameters </tt> when ready.
    * @param parameters The configuration of the annotator, encoded in a String using
    * whatever mechanism is preferred (serialization of Properties object, JSON, etc.)
    * @throws InvalidConfigurationException
    */
   public abstract void setTaskParameters(String parameters) throws InvalidConfigurationException;
   
   /**
    * Determines which layers the annotator requires in order to annotate a graph.
    * @return A list of layer IDs.
    * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
    * {@link #setSchema(Schema)} have not yet been called.
    */
   public abstract String[] getRequiredLayers() throws InvalidConfigurationException;

   /**
    * Determines which layers the annotator will create/update/delete annotations on.
    * @return A list of layer IDs.
    * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
    * {@link #setSchema(Schema)} have not yet been called.
    */
   public abstract String[] getOutputLayers() throws InvalidConfigurationException;

   private boolean ignoreSetPercentComplete = false;
   private Integer percentComplete;
   /**
    * Setter for {@link #percentComplete}: Progress indicator; set to 100 when processing
    * is complete. 
    * @param newPercentComplete Progress indicator; set to 100 when processing is complete.
    */
   protected Annotator setPercentComplete(Integer newPercentComplete) {
      if (!ignoreSetPercentComplete) percentComplete = newPercentComplete;
      return this;
   }
   /**
    * Getter for {@link #percentComplete}: Progress indicator; set to 100 when processing
    * is complete. 
    * @return Progress indicator; set to 100 when processing is complete.
    */
   public Integer getPercentComplete() { return percentComplete; }

   private boolean cancelling = false;   
   /**
    * Determines whether the user has requested that processing be cancelled.
    * @return true if {@link #cancel()} has been called, false otherwise.
    */
   protected boolean isCancelling() {
      return cancelling;
   } // end of isCancelling()
   /**
    * Cancels spliteration; the next call to tryAdvance will return false.
    */
   public void cancel() {
      cancelling = true;
   }

   /**
    * Transforms all graphs from the given graph store that match the given graph
    * expression.
    * <p> This can be overridden for optimized cross-graph updates. The default
    * implementation simply calls {@link IGraphStoreQuery#getMatchingTranscriptIds(String)}
    * and calls {@link IGraphStoreQuery#getGraph(String,String[])} and 
    * {@link IGraphTransformer#transform(Graph)} serially for each returned ID.
    * @param store
    * @param expression
    * @throws TransformationException
    * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
    * {@link #setSchema(Schema)} have not yet been called.
    * @throws StoreException If one of the <var> store </var> methods throws this exception.
    * @throws PermissionException If one of the <var> store </var> methods throws this exception.
    */
   public void transformTranscripts(IGraphStore store, String expression)
      throws TransformationException, InvalidConfigurationException, StoreException, PermissionException {
      Vector<String> layerIds = new Vector<String>();
      for (String layerId : getRequiredLayers()) layerIds.add(layerId);
      for (String layerId : getOutputLayers()) layerIds.add(layerId);
      String[] ids = store.getMatchingTranscriptIds(expression);
      ignoreSetPercentComplete = true; // global progress
      percentComplete = 0;
      try {
         StoreException transcriptException = null;
         int soFar = 0;
         for (String id : ids) {
            try {
               if (cancelling) break;
               Graph graph = store.getTranscript(id, layerIds.toArray(new String[0]));
               if (cancelling) break;
               transform(graph);
               if (cancelling) break;
               store.saveTranscript(graph);
               percentComplete = (int)((double)(soFar * 100) / (double)ids.length);
            } catch (StoreException storeX) {
               // we don't let a single transcript's problem stop all the others from
               // being annotated, but we save the exception for throwing later               
               if (transcriptException == null) transcriptException = storeX;
            } catch (GraphNotFoundException extremelyUnlikely) {
               // we just got the ID from the store, so this is pretty unlikely, and ignorable
               System.err.println("Annotator.transformTranscripts: " + extremelyUnlikely);
            }
         } // next transcript
         percentComplete = 100;
         if (transcriptException != null) throw transcriptException;
      } finally {
         ignoreSetPercentComplete = false;
      }
   } // end of transformTranscripts()
   
} // end of class Annotator
