# TextGridToTrs

Converts Praat TextGrids to Transcriber .trs transcripts

The Praat TextGrid format is extremely flexible and there are many different
 possible ways a transcript can be structured. This converter assumes the following principles:
- the TextGrid is generally an orthographic transcription of speech
- each tier is named after the speaker
- all tiers are labelled intervals
- the interval labels are utterance transcripts - i.e. contain multiple word orthographies
 

All tiers will be interpreted as transcription of participant speech. If some tiers contain other annotations, use the --ignoreTiers command line switch to exclude them from the conversion using a regular expression, e.g.:
 --ignoreTiers=(segments.*)|(target)
 

Praat has no direct mechanism for marking non-speech annotations in their position within the transcript text.  However, this converter supports the use of textual conventions in various ways to make certain annotations: 
 - To tag a word with its pronunciation, enter the pronunciation in square brackets, directly following the word (i.e. with no intervening space), e.g.: 
 …this was at Wingatui[wIN@tui]…
 - To tag a word with its full orthography (if the transcript doesn't include it), enter the orthography in round parentheses, directly following the word (i.e. with no intervening space), e.g.: 
 …I can't remem~(remember)…
 - To insert a noise annotation within the text, enclose it in square brackets (surrounded by spaces so it's not taken as a pronunciation annotation), e.g.: 
 …sometimes me [laughs] not always but sometimes…
 - To insert a comment annotation within the text, enclose it in curly braces (surrounded by spaces), e.g.: 
 …beautifully warm {softly} but its…

To disable these transcription conventions, use the --useConventions=false command-line switch.

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

## Serializing to "Transcriber transcript" text/xml-transcriber

Command-line configuration parameters for serialization:

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
