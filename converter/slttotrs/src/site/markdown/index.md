# SltToTrs

Converts SALT .slt transcripts to Transcriber .trs files

The SALT 'Context' header becomes the Transcriber 'Program'.
The SALT 'Subgroup' header becomes the Transcriber 'Topic'.
SALT meta-data that is not supported by Transcriber is added in comments at the beginning of the transcript.
By default, inline annotations (mazes, codes, bound morphemes, etc.) are not iterpreted. If you want them to be processed, use --parseInlineConventions
By default certain SALT word codes are converted into Transcriber 'pronounce' or 'lexical' events - i.e. those of the form "[PRONOUNCE:...]" and "[LEXICAL:...]" respectively.
Also certain SALT comments are converted into Transcriber 'noise' events - i.e. those of the form "{NOISE:...}". Use the command-line switches --pronounceCodePattern, --lexicalCodePattern, and --noiseCommentPattern to control this behaviour.
e.g. if you specify --pronounceCodePattern=WP:{0} then all word codes like [WP:...] will be pronounce events in the Transcriber transcript.
Similarly if you specify --pronounceCodePattern=WL:{0} then all word codes like [WL:...] will be lexical events in the Transcriber transcript.
Similarly if you specify --noiseCommentPattern=BG:{0} then all comments like [BG:...] will be noise events in the Transcriber transcript.
To disable these conversions, use "--pronounceCodePattern= --lexicalCodePattern= --noiseCommentPattern=" on the command line.
The format for dates is taken from your system settings; to override this, use the --dateFormat command line setting.

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
