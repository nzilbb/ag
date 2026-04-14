# WhisperToEaf

Converts JSON-formatted Whisper ASR files to ELAN files

## Deserializing from "Whisper ASR transcript" text/whisper+plain

Command-line configuration parameters for deserialization:

|   |   |
|:--|:--|
| `--minShortPauseLength=`*Double* | The minimum inter-word pause length, in seconds, before a pause counts as a 'short pause'. |
| `--shortPauseLabel=`*String* | If an inter-word pause has a duration between minShortPauseLength and minMediumPauseLength, then the word before the pause will have this string appended to its label (after a space). |
| `--minMediumPauseLength=`*Double* | The minimum inter-word pause length, in seconds, before a pause counts as a 'medium pause' e.g. (.) |
| `--mediumPauseLabel=`*String* | If an inter-word pause has a duration between minMediumPauseLength and minLongPauseLength, then the word before the pause will have this string appended to its label (after a space) e.g (..) |
| `--minLongPauseLength=`*Double* | The minimum inter-word pause length, in seconds, before a pause counts as a 'long pause'. |
| `--longPauseLabel=`*String* | If an inter-word pause has a duration more than minLongPauseLength, then the word before the pause will have this string appended to its label (after a space) e.g. for the the lengh of the pause in parentheses, use: ({0.000}) |
| `--maxUtteranceDuration=`*Double* | Utterances longer than this will be split on longer inter-word pauses, where possible. |
| `--utterancePadding=`*Double* | Maximum number of seconds to subtract from the start time and add to the end time of each utterance, to allow for alignment errors of first/last word in each segment. |

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
| `--wordTierPattern=`*String* | A regular expression that matches the tier that contains word tokens, e.g. .*word.* |
