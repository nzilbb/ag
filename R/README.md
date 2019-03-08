# nzilbb.ag packages for R

Currently there is one R package - nzilbb.labbcat - which provides
functionality for querying and extracting data from LaBB-CAT corpora,
directly from R.  

This package currently provides access to quite basic data, but does
include a function for extraction sound fragments given a transcript
name and start/end times, so results CSV files can be processed
directy from R.

Future releases will include:
 * *getTranscriptAttributes* - for retrieving given attributes of a
 given set of transcripts.
 * *getParticipantAttributes* - for retrieving given attributes of a
 given set of participants.
 * *getGraph* - for retrieving annotations of a given transcript.
 * *countAnnotations* and *getAnnotations* - for counting/retieving
 annotations of a given transcript on a given layer.
 * *getMediaTracks* - for retrieving media track configuration.
 * *getAvaialableMedia* - for listing media availablle for a given
 transcript.
 * *getMedia* - for downloading a given media track of a given type
 for a given transcript.
 * *search* - for searching for annotation patterns in the corpus.
