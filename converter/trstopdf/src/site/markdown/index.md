# TrsToPdf

Converts Transcriber .trs files to PDFs

As PDF is not related to a linguistic annotation tool, all annotations that Transcriber supports are lost during this conversion, including:
- topics
- comments
- noises
- phrase language annotations
- named entity annotations
- lexical tags
- pronounce tags
- version
- version date
- air date
- scribe
- language
- participant gender
- participant dialect
- participant accent
- participant scope

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

## Serializing to "PDF Document" application/pdf

Command-line configuration parameters for serialization:

|   |   |
|:--|:--|
| `--noiseLayer=`*Layer* | Background noises |
| `--orthographyLayer=`*Layer* | Orthography |
| `--mainParticipantLayer=`*Layer* | Main Participant |
| `--logoFile=`*String* | An image file for a head logo to insert at the beginning of the PDF |
| `--logoScalePercent=`*Integer* | Logo size, in percent of original size. |
