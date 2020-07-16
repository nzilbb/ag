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
package nzilbb.ag.automation.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import nzilbb.ag.automation.Annotator;
import nzilbb.util.IO;

/**
 * Routes requests from a webapp to a given Annotator.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class RequestRouter
{
   // Attributes:

   private HashMap<String,List<Method>> routes;

   /**
    * The annotator to route requests to.
    * @see #getAnnotator()
    * @see #setAnnotator(Annotator)
    */
   protected Annotator annotator;
   /**
    * Getter for {@link #annotator}: The annotator to route requests to.
    * @return The annotator to route requests to.
    */
   public Annotator getAnnotator() { return annotator; }
   /**
    * Setter for {@link #annotator}: The annotator to route requests to.
    * @param newAnnotator The annotator to route requests to.
    */
   public RequestRouter setAnnotator(Annotator newAnnotator) {
      annotator = newAnnotator;

      // index method names as routes
      routes = new HashMap<String,List<Method>>();
      for (Method method : annotator.getClass().getDeclaredMethods()) {
         registerMethod(method);
      } // next method

      // allow version to be retrieved
      try {
       registerMethod(annotator.getClass().getMethod("getVersion"));
      } catch(NoSuchMethodException impossible) {}
      
      // allow progress to be tracked
      try {
       registerMethod(annotator.getClass().getMethod("getPercentComplete"));
      } catch(NoSuchMethodException impossible) {}
      
      // allow status to be tracked
      try {
       registerMethod(annotator.getClass().getMethod("getStatus"));
      } catch(NoSuchMethodException impossible) {}
      
      return this;
   }
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public RequestRouter() {
   } // end of constructor

   
   /**
    * Registers a method as a possible route.
    * @param method
    */
   protected void registerMethod(Method method) {
      // ensure the index has an entry for this route
      if (!routes.containsKey(method.getName())) {
         routes.put(method.getName(), new Vector<Method>());
      }
      // add this method to the route
      routes.get(method.getName()).add(method);
   } // end of registerMethod()

   
   /**
    * Constructor.
    * @param annotator The annotator to route requests to.
    */
   public RequestRouter(Annotator annotator) {
      setAnnotator(annotator);
   } // end of constructor
   
   /**
    * Make a request of the annotator.
    * @param method The HTTP method of the request, e.g. "GET" or "POST"
    * @param bodyEncoding The encoding of the body if any, e.g. "multipart/form-data" for
    * uploading files. 
    * @param body The body of the request.
    * @return The response to the request.
    * @throws RequestException If the request was unsuccessful.
    * @throws URISyntaxException If <var> uri </var> is invalid.
    */
   @SuppressWarnings("rawtypes")
   public InputStream request(String method, String uri, String bodyEncoding, InputStream body)
      throws RequestException, URISyntaxException {
      return request(method, new URI(uri), bodyEncoding, body);
   }
   
   /**
    * Make a request of the annotator.
    * @param method The HTTP method of the request, e.g. "GET" or "POST"
    * @param contentType The encoding of the body if any, e.g. "multipart/form-data" for
    * uploading files. 
    * @param body The body of the request.
    * @return The response to the request.
    * @throws RequestException If the request was unsuccessful.
    */
   @SuppressWarnings("rawtypes")
   public InputStream request(String method, URI uri, String contentType, InputStream body)
      throws RequestException {
      if (annotator == null) throw new RequestException(404, "No annotator set.", method, uri);
      
      // is there a route? - use only the part after the last slash
      String path = uri.getPath().replaceAll(".*/([^/]*)$","$1");
      List<Method> possibleMethods = routes.get(path);
      if (possibleMethods == null) throw new RequestException(404, "Not found.", method, uri);

      String query = uri.getRawQuery();
      if ("application/x-www-form-urlencoded".equals(contentType)
          && "POST".equals(method) && body != null) {
         try {
            query = IO.InputStreamToString(body);
         } catch(IOException exception) {
            throw new RequestException(400, method, uri, exception);
         }
      } 
      
      if ("GET".equals(method)
          || "application/x-www-form-urlencoded".equals(contentType)) {
         // how many parameters are there?
         String[] parameterStrings = query == null?new String[0] : query.split(",");
         
         // find a matching method
         Method classMethod = null;
         for (Method m : possibleMethods) {
            if (m.getParameterCount() == parameterStrings.length) {
               classMethod = m;
               break;
            }
         } // next possible method
         if (classMethod == null) {
            throw new RequestException(400, "Wrong number of parameters.", method, uri);
         }         

         // interpret parameters
         Object[] parameterValues = new Object[parameterStrings.length];
         Class[] parameterTypes = classMethod.getParameterTypes();
         for (int p = 0; p < parameterStrings.length; p++) {
            Class type = parameterTypes[p];
            try {
               if (type.equals(String.class)) {
                  try {
                     parameterValues[p] = URLDecoder.decode(parameterStrings[p], "UTF-8");
                  } catch(UnsupportedEncodingException impossible) {
                     System.err.println(
                        "RequestRouter: ignoring supposedly impossible exception: " + impossible);
                     parameterValues[p] = parameterStrings[p];
                  }
               } else if (type.equals(Integer.class)) {
                  parameterValues[p] = Integer.valueOf(parameterStrings[p]);
               } else if (type.equals(int.class)) {
                  parameterValues[p] = Integer.valueOf(parameterStrings[p]);
               } else if (type.equals(Float.class)) {
                  parameterValues[p] = Float.valueOf(parameterStrings[p]);
               } else if (type.equals(float.class)) {
                  parameterValues[p] = Float.valueOf(parameterStrings[p]);
               } else if (type.equals(Double.class)) {
                  parameterValues[p] = Double.valueOf(parameterStrings[p]);
               } else if (type.equals(double.class)) {
                  parameterValues[p] = Double.valueOf(parameterStrings[p]);
               } else if (type.equals(Boolean.class)) {
                  parameterValues[p] = Boolean.valueOf(parameterStrings[p]);
               } else {
                  throw new RequestException(
                     500, "Parameter " + (p+1) + " ("+parameterStrings[p]+") unsupported type: "
                     + type.getSimpleName(), method, uri);
               }
            } catch (NumberFormatException error) {
               throw new RequestException(
                  400, "Parameter " + (p+1) + " ("+parameterStrings[p]+") could not parse as "
                  + type.getSimpleName() + ": " + error.getMessage(), method, uri);
            }
         } // next parameter

         // invoke the method
         Object result = null;
         try {
            switch (parameterValues.length) {
               case 0:
                  result = classMethod.invoke(annotator);
                  break;
               case 1:
                  result = classMethod.invoke(annotator, parameterValues[0]);
                  break;
               case 2:
                  result = classMethod.invoke(annotator, parameterValues[0], parameterValues[1]);
                  break;
               case 3:
                  result = classMethod.invoke(
                     annotator, parameterValues[0], parameterValues[1], parameterValues[2]);
                  break;
               case 4:
                  result = classMethod.invoke(
                     annotator, parameterValues[0], parameterValues[1], parameterValues[2],
                     parameterValues[3]);
                  break;
               case 5:
                  result = classMethod.invoke(
                     annotator, parameterValues[0], parameterValues[1], parameterValues[2],
                     parameterValues[3], parameterValues[4]);
                  break;
               default:
                  throw new RequestException(
                     500, "More than 5 parameters not unsupported, but "
                     + parameterValues.length + " were specified.", method, uri);
            }
         } catch (Throwable error) {
            throw new RequestException(400, method, uri, error);
         }

         // return the result
         if (result == null) {
            return null;
         } else if (result instanceof InputStream) {
            return (InputStream)result;
         } else if (result == annotator) { // method returns a reference to its object
            return null;
         } else {
            return new ByteArrayInputStream(result.toString().getBytes());
         }
      } // TODO POST multipart
      return null; //TODO
   } // end of request()

} // end of class RequestRouter
