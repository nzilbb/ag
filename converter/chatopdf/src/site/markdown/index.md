# ChaToPdf

Converts CLAN CHAT transcripts to PDF documents

The CLAN parser is *not exhaustive*; it one parses:
- Disfluency marking with &+ - e.g. `so &+sund Sunday`
- Non-standard form expansion - e.g. `gonna [: going to]`
- Incomplete word completion - e.g. `dinner doin(g) all`
- Acronym/proper name joining with _ - e.g. `no T_V in my room`
- Retracing - e.g. `<some friends and I> [//] uh` or `and sit [//] sets him`
- Repetition/stuttered false starts - e.g. `the <picnic> [/] picnic` or `the Saturday [/] in the morning`
- Errors - e.g. `they've <work up a hunger> [* s:r]` or `they got [* m] to`

## Deserializing from "CLAN CHAT transcript" text/x-chat

Command-line configuration parameters for deserialization:

|   |   |
|:--|:--|
| `--cUnitLayer=`*Layer* | Layer for marking c-units |
| `--tokenLayer=`*Layer* | Output word tokens come from this layer |
| `--disfluencyLayer=`*Layer* | Layer for disfluency annotations |
| `--nonWordLayer=`*Layer* | Layer for non-word noises |
| `--expansionLayer=`*Layer* | Layer for expansion annotations |
| `--errorsLayer=`*Layer* | Layer for error  annotations |
| `--linkageLayer=`*Layer* | Layer for linkage annotations |
| `--repetitionsLayer=`*Layer* | Layer for repetition annotations |
| `--retracingLayer=`*Layer* | Layer for retracing annotations |
| `--pauseLayer=`*Layer* | Layer for marking unfilled pauses |
| `--completionLayer=`*Layer* | Layer for completion annotations |
| `--morLayer=`*Layer* | Layer for morphosyntactic tags |
| `--morPrefixLayer=`*Layer* | Layer for prefixes in MOR tags |
| `--morPartOfSpeechLayer=`*Layer* | Layer for parts of speech in MOR tags |
| `--morPartOfSpeechSubcategoryLayer=`*Layer* | Layer for subcategories of parts of speech in MOR tags |
| `--morStemLayer=`*Layer* | Layer for stems in MOR tags |
| `--morFusionalSuffixLayer=`*Layer* | Layer for fusional suffixes in MOR tags |
| `--morSuffixLayer=`*Layer* | Layer for (non-fusional) suffixes in MOR tags |
| `--morGlossLayer=`*Layer* | Layer for English glosses in MOR tags |
| `--gemLayer=`*Layer* | Layer for gems |
| `--transcriberLayer=`*Layer* | Layer for transcriber name |
| `--languagesLayer=`*Layer* | Layer for transcriber language |
| `--dateLayer=`*Layer* | Layer for date of the interaction |
| `--locationLayer=`*Layer* | Layer for location of the interaction |
| `--recordingQualityLayer=`*Layer* | Layer for recording quality |
| `--roomLayoutLayer=`*Layer* | Layer for room layout |
| `--tapeLocationLayer=`*Layer* | Layer for tape and location on the tape covered by the transcription |
| `--targetParticipantLayer=`*Layer* | Layer for identifying target participants |
| `--SESLayer=`*Layer* | Layer for SES |
| `--roleLayer=`*Layer* | Layer for role |
| `--educationLayer=`*Layer* | Layer for education |
| `--sexLayer=`*Layer* | Layer for sex |
| `--customLayer=`*Layer* | Layer for custom |
| `--corpusLayer=`*Layer* | Layer for corpus |
| `--languageLayer=`*Layer* | Layer for language |
| `--ageLayer=`*Layer* | Layer for age |
| `--groupLayer=`*Layer* | Layer for group |
| `--includeTimeCodes=`*Boolean* | Include utterance sychronization information when exporting transcripts |
| `--splitMorTagGroups=`*Boolean* | Split alternative MOR taggings into separate annotations |
| `--splitMorWordGroups=`*Boolean* | Split MOR word morphemes (clitics, components of compounds ) into separate annotations. This is only supported when Split MOR Tag Groups is also enabled. |

## Serializing to "PDF Document" application/pdf

Command-line configuration parameters for serialization:

|   |   |
|:--|:--|
| `--noiseLayer=`*Layer* | Background noises |
| `--orthographyLayer=`*Layer* | Orthography |
| `--mainParticipantLayer=`*Layer* | Main Participant |
| `--logoFile=`*String* | An image file for a head logo to insert at the beginning of the PDF |
| `--logoScalePercent=`*Integer* | Logo size, in percent of original size. |
