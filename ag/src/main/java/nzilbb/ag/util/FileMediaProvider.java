//
// Copyright 2019-2020 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.util;

import java.io.File;
import java.util.Vector;
import nzilbb.ag.Graph;
import nzilbb.ag.GraphMediaProvider;
import nzilbb.ag.MediaFile;
import nzilbb.ag.MediaTrackDefinition;
import nzilbb.ag.PermissionException;
import nzilbb.ag.StoreException;

/**
 * Simple GraphMediaProvider implementation that provides a single media file, or a single
 * collection of files. 
 * @author Robert Fromont robert@fromont.net.nz
 */
public class FileMediaProvider implements GraphMediaProvider {

  Vector<MediaFile> files = new Vector<MediaFile>();
  
  /**
   * Adds a file to the media available.
   * @param f
   * @param trackSuffix
   * @return This object.
   */
  public FileMediaProvider withFile(File f, String trackSuffix) {
    if (trackSuffix == null) trackSuffix = "";
    files.add(new MediaFile(f, trackSuffix)
              .setUrl(f.toURI().toString()));
    return this;
  } // end of withFile()
  
  /**
   * Adds a file to the media available.
   * @param f
   * @return This object.
   */
  public FileMediaProvider withFile(File f) {
    return withFile(f, "");
  } // end of withFile()
  
  /**
   * Default constructor.
   */
  public FileMediaProvider() {
  } // end of constructor
  
  /**
   * List the media available for the graph.
   * @return List of media files available for the given graph.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public MediaFile[] getAvailableMedia() throws StoreException, PermissionException {
    return files.toArray(new MediaFile[0]);
  }
  
  /**
   * Gets a given media track for the graph.
   * @param trackSuffix The track suffix of the media - see {@link MediaTrackDefinition#suffix}.
   * @param mimeType The MIME type of the media.
   * @return A URL to the given media for the given graph, or null if the given media
   * doesn't exist. 
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String getMedia(String trackSuffix, String mimeType)
    throws StoreException, PermissionException {
    if (trackSuffix == null) trackSuffix = "";
    for (MediaFile file : files) {
      if (file.getTrackSuffix().equals(trackSuffix)
          && file.getMimeType().equals(mimeType)) {
        return file.getUrl();
      }
    }
    return null;
  }
  
  /**
   * Provides another instance of the implementing class, for the given graph. The main use for
   * this method is to allow graphs to generate fragments that have their own media providers.
   * @param graph
   * @return A new object that can provide media for the given graph, or null if none can be
   * provided. 
   */
  public GraphMediaProvider providerForGraph(Graph graph) {
    return null;
  }
} // end of class FileMediaProvider
