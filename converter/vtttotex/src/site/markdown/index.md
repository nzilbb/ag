# VttToTex

Converts WebVTT subtitle files to LaTeX documents

The resulting .tex files each include a definition for a new '	urn' command which is used throughout the trancript to format speaker turns; this can be customized directly in the .tex files after conversion, or with the --texTurnCommand command line switch. e.g. "--texTurnCommand=\item[#1:] #2"
 (#1 = Speaker ID, #2 = Turn Text)

## Deserializing from "WebVTT subtitles" text/vtt

There are no configuration parameters for deserialization.

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
