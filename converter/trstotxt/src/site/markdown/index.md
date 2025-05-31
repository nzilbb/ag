# TrsToText

Converts Transcriber .trs files to plain text files

The following participant meta-data is lost during conversion:
- gender
- dialect
- accent
- scope
 
If the --metaData command-line switch is used, then the following Transcriber meta-data will be included as a header to the file:
- version
- version date
- air date
- scribe
- language
Otherwise, this meta-data is lost during conversion
 
The following Transcriber annotations are lost during conversion:
- phrase language annotations
- named entity annotations
 
The following Transcriber annotations are converted using bracketed, inline text conventions:
- comments
- noises
- lexical tags
- pronounce tags
To disable these conventions (and thus lose these annotations during conversion) use the --useConventions=false command line switch.
 
If the --textOnly command-line switch is used, then the output text includes only the transcribed speech, and all annotations and meta-data are lost.

## Deserializing from "Transcriber transcript" text/xml-transcriber

Command-line configuration parameters for deserialization:

|   |   |
|:--|:--|
| `--topicLayer=`*Layer* | Topic tags |
| `--commentLayer=`*Layer* | Commentary |
| `--noiseLayer=`*Layer* | Noise annotations |
| `--languageLayer=`*Layer* | Inline language tags |
| `--lexicalLayer=`*Layer* | Lexical tags |
| `--pronounceLayer=`*Layer* | Manual pronunciation tags |
| `--entityLayer=`*Layer* | Named entities |
| `--scribeLayer=`*Layer* | Name of transcriber |
| `--versionLayer=`*Layer* | Version of transcriber |
| `--versionDateLayer=`*Layer* | Version date of transcriber |
| `--programLayer=`*Layer* | Name of the program recorded |
| `--airDateLayer=`*Layer* | Date the program aired |
| `--transcriptLanguageLayer=`*Layer* | The language of the whole transcript |
| `--participantCheckLayer=`*Layer* | Participant checked |
| `--genderLayer=`*Layer* | Gender - participant 'type' |
| `--dialectLayer=`*Layer* | Participant's dialect |
| `--accentLayer=`*Layer* | Participant's accent |
| `--scopeLayer=`*Layer* | Participant's 'scope' |

## Serializing to "Plain Text Document" text/plain

Command-line configuration parameters for serialization:

|   |   |
|:--|:--|
| `--commentLayer=`*Layer* | Commentary |
| `--noiseLayer=`*Layer* | Background noises |
| `--lexicalLayer=`*Layer* | Lexical tags |
| `--pronounceLayer=`*Layer* | Non-standard pronunciation tags |
| `--orthographyLayer=`*Layer* | Orthography |
| `--useConventions=`*Boolean* | Whether to use text conventions for comment, noise, lexical, and pronounce annotations |
| `--maxParticipantLength=`*Integer* | The maximum length of a participant name |
| `--maxHeaderLines=`*Integer* | The maximum number of lines in a meta-data header |
| `--participantFormat=`*String* | Format for marking a change of turn within the transcript body - e.g. {0}:, where {0} is a place-holder for the participant ID/name |
| `--metaDataFormat=`*String* | Format for a meta-data line in the header - e.g. {0}={1}, where {0} is a place-holder for the attribute name or key, and {1} is a place-holder for the attribute value |
| `--timestampFormat=`*String* | Format for a time stamp - e.g. HH:mm:ss.SSS |
