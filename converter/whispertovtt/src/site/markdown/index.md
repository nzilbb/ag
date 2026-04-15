# WhisperToVtt

Converts JSON-formatted Whisper ASR files to WebVTT subtitles

## Deserializing from "Whisper ASR transcript" text/whisper+plain

Command-line configuration parameters for deserialization:

|   |   |
|:--|:--|
| `--languageLayer=`*Layer* | The language of the whole transcript |
| `--minShortPauseLength=0.35` | The minimum inter-word pause length, in seconds, before a pause counts as a 'short pause'. |
| `--shortPauseLabel=(.)` | If an inter-word pause has a duration between minShortPauseLength and minMediumPauseLength, then the word before the pause will have this string appended to its label (after a space). |
| `--minMediumPauseLength=0.7` | The minimum inter-word pause length, in seconds, before a pause counts as a 'medium pause' |
| `--mediumPauseLabel=(..)` | If an inter-word pause has a duration between minMediumPauseLength and minLongPauseLength, then the word before the pause will have this string appended to its label (after a space) |
| `--minLongPauseLength=1.4` | The minimum inter-word pause length, in seconds, before a pause counts as a 'long pause'. |
| `--longPauseLabel=(...)` | If an inter-word pause has a duration more than minLongPauseLength, then the word before the pause will have this string appended to its label (after a space) e.g. for the the length of the pause in parentheses, use: ({0.000}) |
| `--maxUtteranceDuration=15.0` | Utterances longer than this will be split on longer inter-word pauses, where possible. |
| `--utterancePadding=0.5` | Maximum number of seconds to subtract from the start time and add to the end time of each utterance, to allow for alignment errors of first/last word in each segment. |

## Serializing to "WebVTT subtitles" text/vtt

Command-line configuration parameters for serialization:

|   |   |
|:--|:--|
| `--nonWordPattern=(\([0-9]+\.[0-9]+\))|([\p{Punct}&&[^_]])` | Regular expression to identify non-word characters for joining to a neighboring words e.g. (\([0-9]+\.[0-9]+\))|([\p{Punct}&&[^_]]) - set this blank to simply tokenize on spaces. |
