# VttToPdf

Converts WebVTT subtitle files to PDF documents

## Deserializing from "WebVTT subtitles" text/vtt

There are no configuration parameters for deserialization.

## Serializing to "PDF Document" application/pdf

Command-line configuration parameters for serialization:

|   |   |
|:--|:--|
| `--noiseLayer=`*Layer* | Background noises |
| `--orthographyLayer=`*Layer* | Orthography |
| `--mainParticipantLayer=`*Layer* | Main Participant |
| `--logoFile=`*String* | An image file for a head logo to insert at the beginning of the PDF |
| `--logoScalePercent=`*Integer* | Logo size, in percent of original size. |
