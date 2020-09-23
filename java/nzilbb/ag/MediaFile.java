//
// Copyright 2015-2020 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.json.JsonObject;
import nzilbb.util.CloneableBean;
import nzilbb.util.ClonedProperty;

/**
 * A single media file, which may exist or may be creatable by conversion from some other
 * media file.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class MediaFile implements CloneableBean
{
   // Attributes:
   
   /**
    * The track suffix of the media - see {@link MediaTrackDefinition#suffix}.
    * @see #getTrackSuffix()
    * @see #setTrackSuffix(String)
    */
   protected String trackSuffix;
   /**
    * Getter for {@link #trackSuffix}: The track suffix of the media - see {@link MediaTrackDefinition#suffix}.
    * @return The track suffix of the media - see {@link MediaTrackDefinition#suffix}.
    */
   @ClonedProperty
   public String getTrackSuffix() { return trackSuffix; }
   /**
    * Setter for {@link #trackSuffix}: The track suffix of the media - see {@link MediaTrackDefinition#suffix}.
    * @param newTrackSuffix The track suffix of the media - see {@link MediaTrackDefinition#suffix}.
    */
   public MediaFile setTrackSuffix(String newTrackSuffix) { trackSuffix = newTrackSuffix; return this; }

   /**
    * The MIME type of the file.
    * @see #getMimeType()
    * @see #setMimeType(String)
    */
   protected String mimeType;
   /**
    * Getter for {@link #mimeType}: The MIME type of the file.
    * @return The MIME type of the file.
    */
   @ClonedProperty
   public String getMimeType() { return mimeType; }
   /**
    * Setter for {@link #mimeType}: The MIME type of the file.
    * @param newMimeType The MIME type of the file.
    */
   public MediaFile setMimeType(String newMimeType) { mimeType = newMimeType; return this; }

   /**
    * URL to the content of the file.
    * @see #getUrl()
    * @see #setUrl(String)
    */
   protected String url;
   /**
    * Getter for {@link #url}: URL to the content of the file.
    * @return URL to the content of the file.
    */
   @ClonedProperty
   public String getUrl() { return url; }
   /**
    * Setter for {@link #url}: URL to the content of the file.
    * @param newUrl URL to the content of the file.
    */
   public MediaFile setUrl(String newUrl) { url = newUrl; return this; }

   /**
    * Name of the file
    * @see #getName()
    * @see #setName(String)
    */
   protected String name;
   /**
    * Getter for {@link #name}: Name of the file
    * @return Name of the file
    */
   @ClonedProperty
   public String getName() { return name; }
   /**
    * Setter for {@link #name}: Name of the file
    * @param newName Name of the file
    */
   public MediaFile setName(String newName) 
   { 
      name = newName; 
      if (getMimeType() == null) inferType();
      return this;
   }

   /**
    * The media file from which this one could be generated, or null if the file already exists.
    * @see #getGenerateFrom()
    * @see #setGenerateFrom(MediaFile)
    */
   protected MediaFile generateFrom;
   /**
    * Getter for {@link #generateFrom}: The media file from which this one could be generated, or null if the file already exists.
    * @return The media file from which this one could be generated, or null if the file already exists.
    */
   public MediaFile getGenerateFrom() { return generateFrom; }
   /**
    * Setter for {@link #generateFrom}: The media file from which this one could be generated, or null if the file already exists.
    * @param newGenerateFrom The media file from which this one could be generated, or null if the file already exists.
    */
   public MediaFile setGenerateFrom(MediaFile newGenerateFrom) { generateFrom = newGenerateFrom; return this; }
   
   /**
    * The local media file, if any.
    * @see #getFile()
    * @see #setFile(File)
    */
   protected File file;
   /**
    * Getter for {@link #file}: The local media file, if any.
    * @return The local media file, if any.
    */
   public File getFile() { return file; }
   /**
    * Setter for {@link #file}: The local media file, if any.
    * @param newFile The local media file, if any.
    */
   public MediaFile setFile(File newFile) 
   { 
      file = newFile; 
      if (getName() == null && file != null) setName(file.getName());
      return this;
   }

   // Methods:
   
   /**
    * Default constructor.
    */
   public MediaFile()
   {
   } // end of constructor

   /**
    * Constructor from a file.
    * @param file File
    */
   public MediaFile(File file)
   {
      setFile(file);
   } // end of constructor   

   /**
    * Constructor from a file and suffix.
    * @param file The file.
    * @param trackSuffix The suffix.
    */
   public MediaFile(File file, String trackSuffix)
   {
      setFile(file);
      setTrackSuffix(trackSuffix);
   } // end of constructor   
   
   /**
    * Constructor from another file that it can be generated from.
    * @param file The file.
    * @param from The MediaFile it can be generated from.
    */
   public MediaFile(File file, MediaFile from)
   {
      setFile(file);
      setTrackSuffix(from.getTrackSuffix());
      setGenerateFrom(from);
   } // end of constructor   
   
   /**
    * Constructor from JSON.
    * @param json A JSON representation of the object.
    */
   public MediaFile(JsonObject json)
   {
      fromJson(json);
   } // end of constructor

   /**
    * The file's extension, not including the dot.
    * @return The file's extension.
    */
   public String getExtension()
   {
      if (getName().indexOf('.') < 0) return "";
      return getName().substring(getName().lastIndexOf('.') + 1);
   } // end of getExtension()   

   /**
    * A URL-safe version of #getName()
    * @return #getName() with spaces converted to "%20", etc.
    */
   public String getNameUrlSafe()
   {
      return getName().replace(" ","%20");
   } // end of getNameUrlSafe()

   /**
    * A javascript-safe version of #getName()
    * @return #getName() with quotes converted to underscores.
    */
   public String getJavascriptName()
   {
      return getName().replace('\'','_').replace('"','_');
   } // end of getNameUrlSafe()

   /**
    * Strip of the existension of the file name.
    * @return The file name, without the extension.
    */
   public String getNameWithoutSuffix()
   {
      if (getName().indexOf('.') < 0) return getName();
      return getName().substring(0, getName().lastIndexOf('.'));
   } // end of getNameWithoutSuffix()

   /**
    * Infers the MIME type and the media type from the name.
    */
   protected void inferType()
   {
      String extension = getExtension().toLowerCase();
      if (SuffixToMimeType().containsKey(extension)) setMimeType(SuffixToMimeType().get(extension));
   } // end of inferType()

   
   /**
    * Gets the media type of the file.
    * @return The top-level type of the file, based on the MIME type - i.e. "audio", "video", "image", or "other".
    */
   public String getType()
   {
      if (mimeType == null) return "other";
      String topLevelType = mimeType.substring(0, mimeType.indexOf('/'));
      if (topLevelType.equals("application")) return "other";
      return topLevelType;
   } // end of getType()


   private static Map<String,String> mSuffixToMimeType;  
   /**
    * A map from filename extension to MIME type.
    * @return A map from filename extension to MIME type.
    */
   public static Map<String,String> SuffixToMimeType()
   {
      if (mSuffixToMimeType == null)
      {
	 mSuffixToMimeType = new HashMap<String,String>();
	 mSuffixToMimeType.put("wav", "audio/wav");
	 mSuffixToMimeType.put("mp3", "audio/mpeg");
	 mSuffixToMimeType.put("aif", "audio/aiff");
	 mSuffixToMimeType.put("au", "audio/basic");
	 mSuffixToMimeType.put("oga", "audio/ogg");
	 mSuffixToMimeType.put("mp4", "video/mp4");
	 mSuffixToMimeType.put("mpg", "video/mpeg");
	 mSuffixToMimeType.put("mpeg", "video/mpeg");
	 mSuffixToMimeType.put("avi", "video/avi");
	 mSuffixToMimeType.put("mov", "video/quicktime");
	 mSuffixToMimeType.put("wmv", "video/x-ms-wmv");
	 mSuffixToMimeType.put("ogv", "video/ogg");
	 mSuffixToMimeType.put("ogg", "video/ogg");
	 mSuffixToMimeType.put("webm", "video/webm");
	 mSuffixToMimeType.put("jpg", "image/jpeg");
	 mSuffixToMimeType.put("jpeg", "image/jpeg");
	 mSuffixToMimeType.put("gif", "image/gif");
	 mSuffixToMimeType.put("png", "image/png");
      }
      return mSuffixToMimeType;
   } // end of SuffixToMimeType()

   private static Map<String,String> mMimeTypeToSuffix;  
   /**
    * A map from MIME type to filename extension.
    * @return A map from MIME type to filename extension.
    */
   public static Map<String,String> MimeTypeToSuffix()
   {
      if (mMimeTypeToSuffix == null)
      {
	 mMimeTypeToSuffix = new HashMap<String,String>();
	 mMimeTypeToSuffix.put("audio/wav", "wav");
	 mMimeTypeToSuffix.put("audio/mpeg", "mp3");
	 mMimeTypeToSuffix.put("audio/aiff", "aif");
	 mMimeTypeToSuffix.put("audio/basic", "au");
	 mMimeTypeToSuffix.put("audio/ogg", "oga");
	 mMimeTypeToSuffix.put("video/mp4", "mp4");
	 mMimeTypeToSuffix.put("video/mpeg", "mpg");
	 mMimeTypeToSuffix.put("video/avi", "avi");
	 mMimeTypeToSuffix.put("video/quicktime", "mov");
	 mMimeTypeToSuffix.put("video/x-ms-wmv", "wmv");
	 mMimeTypeToSuffix.put("video/ogg", "ogv");
	 mMimeTypeToSuffix.put("video/webm", "webm");
	 mMimeTypeToSuffix.put("image/jpeg", "jpg");
	 mMimeTypeToSuffix.put("image/gif", "gif");
	 mMimeTypeToSuffix.put("image/png", "png");
      }
      return mMimeTypeToSuffix;
   } // end of MimeTypeToSuffix()


} // end of class MediaFile
