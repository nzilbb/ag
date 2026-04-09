# VttToEaf

Converts WebVTT subtitle files to ELAN files

## Deserializing from "WebVTT subtitles" text/vtt

Command-line configuration parameters for deserialization:

|   |   |
|:--|:--|
| `--nonWordPattern=`*String* | Regular expression to identify non-word characters for joining to a neighboring words e.g. [\p{Punct}&&[^_]] - set this blank to simply tokenize on spaces. |

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
