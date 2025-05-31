# SltToEaf

Converts SALT .slt transcripts to ELAN .eaf files

ELAN doesn't natively support meta-data like Dob, Doe, Ethnicity, etc. however this data is added to the .eaf HEADER in PROPERTY tags, which are preserved by ELAN.
By default, inline annotations (mazes, codes, bound morphemes, etc.) are not interpreted. If you want them to be processed, use --parseInlineConventions

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
