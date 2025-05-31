# TextToEaf

Converts time-aligned plain text .txt transcripts to ELAN .eaf files

The plain text transcript must include synchronisation information
- i.e. time codes - and must end in a timecode, indicating the end time

of the last utterance.


Check the --timestampFormat setting matches your time codes.

This setting uses Java SimpleDateFormat format:

https://docs.oracle.com/javase/8/docs/api/index.html?java/text/SimpleDateFormat.html

## Deserializing from "Plain Text Document" text/plain

Command-line configuration parameters for deserialization:

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

## Serializing to "ELAN EAF Transcript" text/x-eaf+xml

Command-line configuration parameters for serialization:

|   |   |
|:--|:--|
| `--commentLayer=`*Layer* | Commentary |
| `--noiseLayer=`*Layer* | Noise annotations |
| `--lexicalLayer=`*Layer* | Lexical tags |
| `--pronounceLayer=`*Layer* | Manual pronunciation tags |
| `--authorLayer=`*Layer* | Name of transcriber |
| `--dateLayer=`*Layer* | Document date |
| `--languageLayer=`*Layer* | The language of the whole transcript |
| `--phraseLanguageLayer=`*Layer* | For tagging individual phrases with a language |
| `--useConventions=`*Boolean* | Whether to use text conventions for comment, noise, lexical, and pronounce annotations |
| `--ignoreBlankAnnotations=`*Boolean* | Whether to skip annotations with no label, or process them |
| `--minimumTurnPauseLength=`*Double* | Minimum amount of time between two turns by the same speaker, with no intervening speaker, for which the inter-turn pause counts as a turn change boundary. If the pause is shorter than this, the turns are merged into one. |
