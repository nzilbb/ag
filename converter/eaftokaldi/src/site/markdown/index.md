# EafToKaldi

Converts ELAN .eaf files to corpus input files for Kaldi

All tiers will be interpreted as transcription of participant speech. If some tiers contain other annotations, use the --ignoreTiers command line switch to exclude them from the conversion using a regular expression, e.g.:
 --ignoreTiers=Noise|Topic
 
By default, all words are converted to lowercase, and extraneous punctuation is removed.
 To disable this behaviour, use the --cleanOrthography=false command line switch.
 
ELAN doesn't support participant meta-data, so the 'spk2gender' file is not generated.
 
ELAN has no direct mechanism for marking non-speech annotations in their position within the transcript text.  However, this converter supports the use of textual conventions in various ways to make certain annotations: 
 - To tag a word with its pronunciation, enter the pronunciation in square brackets, directly following the word (i.e. with no intervening space), e.g.: 
 …this was at Wingatui[wIN@tui]…
 - To tag a word with its full orthography (if the transcript doesn't include it), enter the orthography in round parentheses, directly following the word (i.e. with no intervening space), e.g.: 
 …I can't remem~(remember)…
 - To insert a noise annotation within the text, enclose it in square brackets (surrounded by spaces so it's not taken as a pronunciation annotation), e.g.: 
 …sometimes me [laughs] not always but sometimes…
 - To insert a comment annotation within the text, enclose it in curly braces (surrounded by spaces), e.g.: 
 …beautifully warm {softly} but its…
To enable these transcription conventions, use the --useConventions command-line switch.

## Deserializing from "ELAN EAF Transcript" text/x-eaf+xml

Command-line configuration parameters for deserialization:

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

## Serializing to "Kaldi Files" text/x-kaldi-text

Command-line configuration parameters for serialization:

|   |   |
|:--|:--|
| `--orthographyLayer=`*Layer* | Orthography tags |
| `--pronunciationLayer=`*Layer* | Pronunciation tags |
| `--genderLayer=`*Layer* | Participant gender |
| `--prefixUtteranceId=`*Boolean* | Whether to prefix utterance IDs with the speaker ID or not. |
| `--wavBasePath=`*String* | Base path to prefix all wav files names. |
