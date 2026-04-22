# WhisperToTextGrid

Converts JSON-formatted Whisper ASR files to Praat TextGrids

## Deserializing from "Whisper ASR transcript" text/whisper+plain

Command-line configuration parameters for deserialization:

|   |   |
|:--|:--|
| `--languageLayer=`*Layer* | The language of the whole transcript |
| `--minShortPauseLength=`*Double* | The minimum inter-word pause length, in seconds, before a pause counts as a 'short pause'. |
| `--shortPauseLabel=`*String* | If an inter-word pause has a duration between minShortPauseLength and minMediumPauseLength, then the word before the pause will have this string appended to its label (after a space). |
| `--minMediumPauseLength=`*Double* | The minimum inter-word pause length, in seconds, before a pause counts as a 'medium pause' |
| `--mediumPauseLabel=`*String* | If an inter-word pause has a duration between minMediumPauseLength and minLongPauseLength, then the word before the pause will have this string appended to its label (after a space) |
| `--minLongPauseLength=`*Double* | The minimum inter-word pause length, in seconds, before a pause counts as a 'long pause'. |
| `--longPauseLabel=`*String* | If an inter-word pause has a duration more than minLongPauseLength, then the word before the pause will have this string appended to its label (after a space) e.g. for the the length of the pause in parentheses, use: ({0.000}) |
| `--maxUtteranceDuration=`*Double* | Utterances longer than this will be split on longer inter-word pauses, where possible. |
| `--utterancePadding=`*Double* | Maximum number of seconds to subtract from the start time and add to the end time of each utterance, to allow for alignment errors of first/last word in each segment. |

## Serializing to "Praat TextGrid" text/praat-textgrid

Command-line configuration parameters for serialization:

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
| `--includeMetaData=`*Boolean* | Whether to include transcript attributes as one-annotation tiers or ignore them |
