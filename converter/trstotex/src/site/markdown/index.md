# TrsToTex

Converts Transcriber .trs files to LaTeX documents

The resulting .tex files each include a definition for a new '	urn' command which is used throughout the trancript to format speaker turns; this can be customized directly in the .tex files after conversion, or with the --texTurnCommand command line switch. e.g. "--texTurnCommand=\item[#1:] #2"
 (#1 = Speaker ID, #2 = Turn Text)

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

## Serializing to "LaTeX Document" application/x-tex

Command-line configuration parameters for serialization:

|   |   |
|:--|:--|
| `--noiseLayer=`*Layer* | Background noises |
| `--orthographyLayer=`*Layer* | Orthography |
| `--texPreamble=`*String* | TeX code to include in the preamble, e.g. "\usepackage{lineno, blindtext}" |
| `--texBeginTranscript=`*String* | TeX to insert before the first turn, e.g. "\begin{description}" |
| `--texTurnCommand=`*String* | TeX command for formatting a speech turn, e.g. "\item[#1:] #2" - #1 = Speaker ID, #2 = Turn Text |
| `--texEndTranscript=`*String* | TeX to insert after the last turn, e.g. "\end{description}" |
