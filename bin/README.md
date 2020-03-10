# Binary Files

These are various compiled binary files, representing the latest
version of various *nzilbb.ag* components.

These include:
* nzilbb.ag.jar - the primary API/object model for nzilbb.ag
* nzilbb.???.jar - a number of serialization modules that cane, for example, be installed in
  LaBB-CAT to add support for format conversions.
* ???-to-???.jar - stand-alone utilities to perform specific format conversions - see below...

## Stand-alone Format Converters

There are a number converter utilities that can be used to convert files from one format
to another, including:

* [vtt-to-textgrid.jar](https://github.com/nzilbb/ag/blob/master/bin/vtt-to-textgrid.jar?raw=true) - convert from web subtitles (Web VTT) to Praat TextGrid
* [vtt-to-eaf.jar](https://github.com/nzilbb/ag/blob/master/bin/vtt-to-eaf.jar?raw=true) - convert from web subtitles (Web VTT) to ELAN files (.eaf)
* [trs-to-textgrid.jar](https://github.com/nzilbb/ag/blob/master/bin/trs-to-textgrid.jar?raw=true) - convert from Transcriber transcripts (.trs) to Praat TextGrid
* [trs-to-eaf.jar](https://github.com/nzilbb/ag/blob/master/bin/trs-to-eaf.jar?raw=true) - convert from Transcriber transcripts (.trs) to ELAN files (.eaf)
* [trs-to-pdf.jar](https://github.com/nzilbb/ag/blob/master/bin/trs-to-pdf.jar?raw=true) - convert from Transcriber transcripts (.trs) to PDF files

To use a particular converter, you need to have Java installed on your
system. Download the file, and double-click it to run.

If double-clicking doesn't work, you can run the converter from the
command line, by entering:
```
java -jar vtt-to-textgrid.jar
```

By default converters display a window on to which you can drag and drop files for
converting. However, they can also be run in 'batch mode', which allows you to
automatically convert a list of files using the command line - e.g.

```
java -jar trs-to-textgrid.jar --batchmode *.trs
```
