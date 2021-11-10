//
// Copyright 2020-2021 New Zealand Institute of Language, Brain and Behaviour, 
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import nzilbb.ag.automation.Annotator;
import nzilbb.util.CloneableBean;
import nzilbb.util.IO;
import org.apache.commons.fileupload.MultipartStream;

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
    * @param uri The URI of the request.
    * @param contentType The encoding of the body if any, e.g. "multipart/form-data" for
    * uploading files. 
    * @param body The body of the request.
    * @return The response to the request.
    * @throws RequestException If the request was unsuccessful.
    */
   @SuppressWarnings({"rawtypes", "unchecked"})
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
      
      Object result = null;
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
         result = invokeMethod(classMethod, parameterValues, method, uri);

      } else if ("PUT".equals(method)) { // body is the file

         // take the body of the request as the contents of the file
         Method classMethod = null;
         for (Method m : possibleMethods) {
            if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == File.class) {
               classMethod = m;
               break;
            }
         } // next possible method
         if (classMethod == null) {
            throw new RequestException(400, "Wrong number of parameters.", method, uri);
         }
         try {
            // create a file
            File file = File.createTempFile(
               annotator.getClass().getSimpleName() + "_", "_" + classMethod.getName());
            // save the body content into it
            IO.SaveInputStreamToFile(body, file);
            // invoke the method
            result = classMethod.invoke(annotator, file);
            // delete file
            file.delete();
         } catch (IOException fileError) {
            throw new RequestException(500, method, uri, fileError);
         } catch (Throwable error) {
            throw new RequestException(400, method, uri, error);
         }
      } else if ("POST".equals(method)
                 && contentType.startsWith("multipart/form-data; boundary=")) { 
         // save files in a temp directory
         File dir = null;
         try {
            dir = File.createTempFile(
            annotator.getClass().getSimpleName() + "_", "_" + path);
            dir.delete();
            dir.mkdir();

            // parse the parts out of the request
            Vector parts = new Vector();
            String boundary = contentType.substring("multipart/form-data; boundary=".length());
            MultipartStream bodyParts = new MultipartStream(body, boundary.getBytes(), 1024, null);
            boolean nextPart = true;
            nextPart = bodyParts.skipPreamble();
            while (nextPart) {
               String headers = bodyParts.readHeaders();
               Matcher fileNameParser = Pattern.compile("filename=\"([^\"]+)\"").matcher(headers);
               if (fileNameParser.find()) {
                  String fileName = fileNameParser.group(1);
                  File file = new File(dir, fileName);
                  FileOutputStream output = new FileOutputStream(file);
                  bodyParts.readBodyData(output);
                  parts.add(file);
               } else {
                  ByteArrayOutputStream output = new ByteArrayOutputStream();
                  bodyParts.readBodyData(output);
                  parts.add(output.toString("UTF-8"));
               }
               
               nextPart = bodyParts.readBoundary();
            }
         
            // find a methods that matches the parameters passed
            Method classMethod = null;
            for (Method m : possibleMethods) {
               if (m.getParameterCount() == parts.size()) {
                  classMethod = m;
                  // check the parameters are the right type
                  for (int p = 0; p < m.getParameterTypes().length; p++) {
                     if (!m.getParameterTypes()[p].equals(parts.elementAt(p).getClass())) {
                        // not the right type
                        classMethod = null;
                        break;
                     }
                  }
                  if (classMethod != null) break;
               }
            } // next possible method
            if (classMethod == null) {
               throw new RequestException(400, "Wrong number of parameters.", method, uri);
            }
            Object[] parameterValues = parts.toArray(new Object[0]);
            // invoke the method
            result = invokeMethod(classMethod, parameterValues, method, uri);

            
         } catch(UnsupportedEncodingException encodingError) {
            throw new RequestException(500, method, uri, encodingError);
         } catch(MultipartStream.MalformedStreamException streamError) {
            throw new RequestException(500, method, uri, streamError);
         } catch(IOException fileError) {
            throw new RequestException(500, method, uri, fileError);
         } finally {
            // delete files
            IO.RecursivelyDelete(dir);
         }
      } else {
         System.err.println(
            "Method " + method + " Content-Type " + contentType + " not supported.");
         
      }
      
      // return the result
      if (result == null) {
         return null;
      } else if (result instanceof InputStream) {
         return (InputStream)result;
      } else if (result == annotator) { // method returns a reference to its object
         return null;
      } else if (result instanceof CloneableBean) {
         return new ByteArrayInputStream(((CloneableBean)result).toJson().toString().getBytes());
      } else if (result instanceof List) {
         JsonArrayBuilder list = Json.createArrayBuilder();
         for (Object e : (List)result) {
            list.add(e.toString());
         }
         return new ByteArrayInputStream(list.build().toString().getBytes());
      } else if (result instanceof Map) {
         JsonObjectBuilder object = Json.createObjectBuilder();
         Map map = (Map)result;
         for (Object k : map.keySet()) {
           if (map.get(k) instanceof Double) {
             object.add(k.toString(), (Double)map.get(k));
           } else if (map.get(k) instanceof Integer) {
             object.add(k.toString(), (Integer)map.get(k));
           } else if (map.get(k) instanceof Boolean) {
             object.add(k.toString(), (Boolean)map.get(k));
           } else {             
             object.add(k.toString(), map.get(k) == null?null:map.get(k).toString());
           }
         }
         return new ByteArrayInputStream(object.build().toString().getBytes());
      } else {
         return new ByteArrayInputStream(result.toString().getBytes());
      }
   } // end of request()
   
   /**
    * Invokes the given method with the given parameters
    * @param classMethod The method to invoke.
    * @param parameterValues The parameters for the method.
    * @param method The HTTP method of the request (for error reporting).
    * @param uri The URI of the request (for error reporting).
    * @return The result of the request, if any.
    * @throws RequestException
    */
   protected Object invokeMethod(
      Method classMethod, Object[] parameterValues, String method, URI uri)
      throws RequestException {
      try {
         switch (parameterValues.length) {
            case 0:
               return classMethod.invoke(annotator);
            case 1:
               return classMethod.invoke(annotator, parameterValues[0]);
            case 2:
               return classMethod.invoke(annotator, parameterValues[0], parameterValues[1]);
            case 3:
               return classMethod.invoke(
                  annotator, parameterValues[0], parameterValues[1], parameterValues[2]);
            case 4:
               return classMethod.invoke(
                  annotator, parameterValues[0], parameterValues[1], parameterValues[2],
                  parameterValues[3]);
            case 5:
               return classMethod.invoke(
                  annotator, parameterValues[0], parameterValues[1], parameterValues[2],
                  parameterValues[3], parameterValues[4]);
            default:
               throw new RequestException(
                  500, "More than 5 parameters not unsupported, but "
                  + parameterValues.length + " were specified.", method, uri);
         }
      } catch (Throwable error) {
         throw new RequestException(400, method, uri, error);
      }    
   } // end of invokeMethod()

} // end of class RequestRouter
