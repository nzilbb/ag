# Transcriber Evaluator

A command line utility for automated evaluation of automatic Transcriber modules.

This can be invoked from the command line to compare automatic transcripts of recordings saved in a given directory, with correspondings plain-text manual transcriptions, like this:

```
java -jar nzilbb.transcriber.evaluate.jar \
 nzilbb.transcriber.whisper.jar \
 /path/to/recordings/and/transcripts
```

It can also compare automatic transcripts of transcribed recordings stored in a LaBB-CAT instance, like this:

```
java -jar nzilbb.transcriber.evaluate.jar \
 nzilbb.transcriber.whisper.jar \
 --Labbcat=https://labbcat.canterbury.ac.nz/demo/ \
 "--transcripts=first('corpus').label == 'UC'"
```

Either way, the utility produces two tab-separated outputs:

- To std out, a list of recordings with a word count and Word Error Rate (WER)
- To a `path....tsv` file, minimum edit paths for all words, including:
  - the word from the reference transcript 
  - the word from the automatic transcriber
  - what the step does (insert +, delete -, change ~, or no change)
  - the edit distance represented by the step

