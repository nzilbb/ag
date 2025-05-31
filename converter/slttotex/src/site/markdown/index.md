# SltToTex

Converts SALT transcripts to LaTeX documents

Almost all SALT header meta-data is lost when converting to .tex.
By default, inline annotations (mazes, codes, bound morphemes, etc.) are parsed (and thus removed). If you want them to be included in the output as-is, use the --parseInlineConventions=false command line switch.
 
The resulting .tex files each include a definition for a new '	urn' command which is used throughout the trancript to format speaker turns; this can be customized directly in the .tex files after conversion, or with the --texTurnCommand command line switch. e.g. "--texTurnCommand=\item[#1:] #2"
 (#1 = Speaker ID, #2 = Turn Text)

## Deserializing from "SALT transcript" text/x-salt

Command-line configuration parameters for deserialization:

|   |   |
|:--|:--|
| `--cUnitLayer=`*Layer* | Layer for marking c-units |
| `--targetParticipantLayer=`*Layer* | Layer for marking the target participant |
| `--commentLayer=`*Layer* | Layer for comments |
| `--parentheticalLayer=`*Layer* | Layer for marking parenthetical remarks by the speaker |
| `--properNameLayer=`*Layer* | Layer for tagging proper names |
| `--repetitionsLayer=`*Layer* | Layer for annotating repetitions |
| `--rootLayer=`*Layer* | Layer for tagging words with their root form |
| `--errorLayer=`*Layer* | Layer for marking errors |
| `--soundEffectLayer=`*Layer* | Layer for marking non-word verbal sound effects |
| `--pauseLayer=`*Layer* | Layer for marking pauses in speech |
| `--boundMorphemeLayer=`*Layer* | Layer for marking bound morpheme annotations |
| `--mazeLayer=`*Layer* | Layer for marking false starts, repetitions, and reformulations |
| `--partialWordLayer=`*Layer* | Layer for marking stuttered or interrupted words |
| `--omissionLayer=`*Layer* | Layer for marking missing words |
| `--codeLayer=`*Layer* | Layer for non-error codes |
| `--languageLayer=`*Layer* | Layer for recording the language of the speech |
| `--participantIdLayer=`*Layer* | Layer for recording the target participant's ID |
| `--genderLayer=`*Layer* | Layer for recording the gender of the target participant |
| `--dobLayer=`*Layer* | Layer for recording the birth date of the target participant |
| `--doeLayer=`*Layer* | Layer for recording the date the recording was elicited |
| `--caLayer=`*Layer* | Layer for recording the target participant's age when recorded |
| `--ethnicityLayer=`*Layer* | Layer for recording the ethnicity of the target participant |
| `--contextLayer=`*Layer* | Layer for recording the sampling context |
| `--subgroupLayer=`*Layer* | Layer for recording the sub-group/story |
| `--collectLayer=`*Layer* | Layer for recording the collection point of the elicitation |
| `--locationLayer=`*Layer* | Layer for recording the location of the elicitation |
| `--dateFormat=`*String* | Format used in SALT files for dates (e.g. Dob, Doe) - either M/d/yyyy or d/M/yyyy. NB: the default date format is inferred from your locale settings |
| `--parseInlineConventions=`*Boolean* | Whether to use SALT in-line conventions when deserializing. If false, then only meta-data headers, comment lines, and time stamps are parsed; all in-line annotation conventions are left as-is |

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
