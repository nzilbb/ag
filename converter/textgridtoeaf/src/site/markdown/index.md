# TextGridToEaf

Converts Praat .TextGrid files to ELAN .eaf files

Praat tiers are converted directly to ELAN tiers as-is, except for point tiers which are not supported by ELAN.

## Deserializing from "Praat TextGrid" text/praat-textgrid

Command-line configuration parameters for deserialization:

|   |   |
|:--|:--|
| `--commentLayer=`*Layer* | Commentary |
| `--noiseLayer=`*Layer* | Noise annotations |
| `--lexicalLayer=`*Layer* | Lexical tags |
| `--pronounceLayer=`*Layer* | Manual pronunciation tags |
| `--renameShortNumericSpeakers=`*Boolean* | Short speaker names like 'S1' should be prefixed with the transcript name during import |
| `--allowPeerOverlap=`*Boolean* | Allows TextGrids with, for example, multiple segment tiers, if the underlying annotations are invalid and have overlapping segments. |
| `--utteranceThreshold=`*Double* | Minimum inter-word pause to trigger an utterance boundary, when no utterance layer is mapped. 0 means 'do not infer utterance boundaries'. |
| `--useConventions=`*Boolean* | Whether to use text conventions for comment, noise, lexical, and pronounce annotations |
| `--ignoreLabels=`*String* | Regular expression for annotation to ignore, e.g. <p:> to ignore MAUS pauses |

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
