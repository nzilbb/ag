# SltToPdf

Converts SALT transcripts to PDF documents

PDF doesn't support meta-data like Dob, Doe, Ethnicity, etc. so all SALT header meta-data is lost when converting to .vtt.
Also, inline annotations (mazes, codes, bound morphemes, etc.) are parsed out so that the text uses normal orthography with no transcription conventions.

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

## Serializing to "PDF Document" application/pdf

Command-line configuration parameters for serialization:

|   |   |
|:--|:--|
| `--noiseLayer=`*Layer* | Background noises |
| `--orthographyLayer=`*Layer* | Orthography |
| `--mainParticipantLayer=`*Layer* | Main Participant |
| `--logoFile=`*String* | An image file for a head logo to insert at the beginning of the PDF |
| `--logoScalePercent=`*Integer* | Logo size, in percent of original size. |
