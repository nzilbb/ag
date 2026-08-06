# Binary Files

This directory contains various compiled binary files, representing the latest
version of various *nzilbb.ag* components.

These include:
* nzilbb.ag.jar - the primary API/object model for nzilbb.ag
* nzilbb.formatter.???.jar - a number of de/serialization modules that can, for example, be
  installed in LaBB-CAT to add support for format conversions.

## Validate Transcriber Transcripts

Some earlier versions of [Transcriber](http://trans.sourceforge.net) sometimes output
transcript files that had inconsistent turn alignments: the end time of a turn could be
after the start time of the next turn.

These transcripts cause problems when processing transcripts for force-alignment, etc.

The transcriber deserialization module here,
[transcriber-validator.jar](https://github.com/nzilbb/ag/raw/master/bin/transcriber-validator.jar),
is a command-line tool that can be used to fix up such corrupted transcripts. If you
download transcriber-validator.jar, you can invoke it using your command shell, like this:

```
java -jar transcriber-validator.jar some-transcript.trs
```

By default, the utility checks and validates the given transcript(s), saving the results
in a subdirectory called *valid*. This ensures that transcripts are copied rather that
directly changed, and so the original transcript files are untouched.

Transcripts can be changed in-situ if required (i.e. changing the original file) by using
the `--replace` command-line switch.

And if you name a directory instead of a .trs file, then the directory is recursively
scanned for .trs files to process.

So check/fix all transcripts in a given directory, use the command:

```
java -jar transcriber-validator.jar --replace /path/to/directory/with/trs/files
```

For full information about command line options:

```
java -jar transcriber-validator.jar --usage
```
