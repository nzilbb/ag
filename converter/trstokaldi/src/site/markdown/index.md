# TrsToKaldi

Converts Transcriber .trs files to corpus input files for Kaldi

The participant genders from the Transcriber transcripts are used, if present, to generate the spk2gender file.
 The following participant meta-data is lost during conversion:
- dialect
- accent
- scope
- version
- version date
- air date
- scribe
- language
 
The following Transcriber annotations are lost during conversion:
- phrase language annotations
- named entity annotations
- comments
- noises
- lexical tags
- pronounce tags
 
By default, all words are converted to lowercase, and extraneous punctuation is removed.
 To disable this behaviour, use the --cleanOrthography=false command line switch.

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

## Serializing to "Kaldi Files" text/x-kaldi-text

Command-line configuration parameters for serialization:

|   |   |
|:--|:--|
| `--orthographyLayer=`*Layer* | Orthography tags |
| `--pronunciationLayer=`*Layer* | Pronunciation tags |
| `--genderLayer=`*Layer* | Participant gender |
| `--prefixUtteranceId=`*Boolean* | Whether to prefix utterance IDs with the speaker ID or not. |
| `--wavBasePath=`*String* | Base path to prefix all wav files names. |
