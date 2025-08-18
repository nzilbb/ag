# VttToTextGrid

Converts WebVTT subtitle files to Praat TextGrids

## Deserializing from "WebVTT subtitles" text/vtt

There are no configuration parameters for deserialization.

## Serializing to "Praat TextGrid" text/praat-textgrid

Command-line configuration parameters for serialization:

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
