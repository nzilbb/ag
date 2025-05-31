# TrsToTextGrid

Converts Transcriber .trs files to Praat TextGrids

Praat does not support meta-data as Transcriber does, so the following meta-data is lost during conversion:
- version
- version date
- air date
- scribe
- language
- participant gender
- participant dialect
- participant accent
- participant scope
The following Transcriber annotations are not supported by Praat, and are lost:
- phrase language annotations
- named entity annotations
- topic tags
The following Transcriber annotations are not directly supported by Praat, and are converted using bracketed, inline conventions within annotation labels:
- comments
- noises
- lexical tags
- pronounce tags
To disable these conventions (and thus lose these annotations during conversion) use the --useConventions=false command line switch.

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

## Serializing to "Praat TextGrid" text/praat-textgrid

Command-line configuration parameters for serialization:

|   |   |
|:--|:--|
| `--commentLayer=`*Layer* | Commentary |
| `--noiseLayer=`*Layer* | Noise annotations |
| `--lexicalLayer=`*Layer* | Lexical tags |
| `--pronounceLayer=`*Layer* | Manual pronunciation tags |
| `--renameShortNumericSpeakers=`*Boolean* | Short speaker names like 'S1' should be prefixed with the transcript name during import |
| `--useConventions=`*Boolean* | Whether to use text conventions for comment, noise, lexical, and pronounce annotations |
