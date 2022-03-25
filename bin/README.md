# Binary Files

This directory contains various compiled binary files, representing the latest
version of various *nzilbb.ag* components.

These include:
* nzilbb.ag.jar - the primary API/object model for nzilbb.ag
* nzilbb.formatter.???.jar - a number of de/serialization modules that can, for example, be
  installed in LaBB-CAT to add support for format conversions.
* ???-to-???.jar - stand-alone utilities to perform specific format conversions - see below...

## Standalone Format Converters

There are a number converter utilities that can be used to convert files from one format
to another, including:

* trs - [Transcriber](http://trans.sourceforge.net/en/presentation.php) transcripts
* eaf - [ELAN](https://archive.mpi.nl/tla/elan) files
* vtt - [web subtitles (Web VTT)](https://en.wikipedia.org/wiki/WebVTT)
* slt - [SALT](https://www.saltsoftware.com/) transcripts
* cha - [CLAN](https://dali.talkbank.org/clan/) CHAT transcripts
* textgrid - [Praat](https://praat.org) TextGrids
* pdf - *PDF* files
* tex - *LaTeX* files
* txt - *plain text* files
* kaldi - input files for the [Kaldi](https://kaldi-asr.org/) automatic speech recognition training system

| to↓ from→ | trs | eaf  | vtt | slt | cha | textgrid | txt |
| --- | :---: | :---:  | :---: | :---: | :---: | :---: | :---: |
| **trs** | | [eaf-to-trs](eaf-to-trs.jar?raw=true) | [vtt-to-trs](vtt-to-trs.jar?raw=true) | [slt-to-trs](slt-to-trs.jar?raw=true) | [cha-to-trs](cha-to-trs.jar?raw=true) | [textgrid-to-trs](textgrid-to-trs.jar?raw=true) | |
| **eaf** | [trs-to-eaf](trs-to-eaf.jar?raw=true) | | [vtt-to-eaf](vtt-to-eaf.jar?raw=true) | [slt-to-eaf](slt-to-eaf.jar?raw=true) | [cha-to-eaf](cha-to-eaf.jar?raw=true) | | [txt-to-eaf](txt-to-eaf.jar?raw=true) |
| **vtt** | [trs-to-vtt](trs-to-vtt.jar?raw=true) | [eaf-to-vtt](eaf-to-vtt.jar?raw=true) | | [slt-to-vtt](slt-to-vtt.jar?raw=true) | [cha-to-vtt](cha-to-vtt.jar?raw=true) | [textgrid-to-vtt](textgrid-to-vtt.jar?raw=true) | |
| **slt** | [trs-to-slt](trs-to-slt.jar?raw=true) | [eaf-to-slt](eaf-to-slt.jar?raw=true) | | | | |
| **cha** | [trs-to-cha](trs-to-cha.jar?raw=true) | [eaf-to-cha](eaf-to-cha.jar?raw=true) | [vtt-to-cha](vtt-to-cha.jar?raw=true) | | | | |
| **textgrid** | [trs-to-textgrid](trs-to-textgrid.jar?raw=true) | [eaf-to-textgrid](eaf-to-textgrid.jar?raw=true) | [vtt-to-textgrid](vtt-to-textgrid.jar?raw=true) | [slt-to-textgrid](slt-to-textgrid.jar?raw=true) | [cha-to-textgrid](cha-to-textgrid.jar?raw=true) | | |
| **pdf** | [trs-to-pdf](trs-to-pdf.jar?raw=true) |  [eaf-to-pdf](eaf-to-pdf.jar?raw=true) |  [vtt-to-pdf](vtt-to-pdf.jar?raw=true) | [slt-to-pdf](slt-to-pdf.jar?raw=true) | [cha-to-pdf](cha-to-pdf.jar?raw=true) | [textgrid-to-pdf](textgrid-to-pdf.jar?raw=true) | |
| **tex** | [trs-to-tex](trs-to-tex.jar?raw=true) | [eaf-to-tex](eaf-to-tex.jar?raw=true) | [vtt-to-tex](vtt-to-tex.jar?raw=true) |  [slt-to-tex](slt-to-tex.jar?raw=true) | | [textgrid-to-tex](textgrid-to-tex.jar?raw=true) | |
| **txt** | [trs-to-txt](trs-to-txt.jar?raw=true) | | | | | | |
| **kaldi** | [trs-to-kaldi](trs-to-kaldi.jar?raw=true) | [eaf-to-kaldi](eaf-to-kaldi.jar?raw=true) | | | | [textgrid-to-kaldi](textgrid-to-kaldi.jar?raw=true) | |

To use a particular converter, you need to have Java installed on your
system. Download the file, and double-click it to run.

If double-clicking doesn't work, you can run the converter from the
command line, by entering:
```
java -jar vtt-to-textgrid.jar
```

By default converters display a window on to which you can drag and drop files for
converting. However, they can also be run in 'batch mode', which allows you to
automatically convert a list of files from the command line - e.g.

```
java -jar trs-to-textgrid.jar --batchmode *.trs
```

Some conversions have configurable output, e.g.

```
java -jar trs-to-txt.jar *.txt
```

...will include annotations and participant names in the output text files, but:

```
java -jar trs-to-txt.jar --textonly *.txt
```

...produces text files that exclude all annotations and participant names.

The `--usage` command-line switch prints information about command-line options.

As many formats do not support the meta-data, annotation granularity or ontology of other
formats, many of these conversions necessarily entail loss of data. However, mappings are
made from one format to another wherever possible.

For notes about specific correspondences or data losses, use the `--help` command-line
switch, or use the *Help|Information* menu option of the conversion utility concerned.

## Getting TextGrid Transcripts of YouTube Videos

The basic process of making a YouTube video corpus with TextGrid transcripts is:

1. Download Youtube videos with their closed-caption subtitle files.
2. Convert the closed-caption subtitle files to TextGrids.

### 1. Download Youtube videos with subtitles

There's a tool called [youtube-dl](https://rg3.github.io/youtube-dl/) which can be used for
downloading videos from YouTube. It's a command-line program that can be given a URL for
downloading. If the URL is a playlist, then it will download all videos in the playlist into
separate files.

It has many useful command-line options:

* `--help` - Displays information about all the other command-line options.
*  `-f` - Sets the format for the video, e.g. `-f wav` should download WAV files,
  `mp4` will download MP4's, etc.
* `--extract-audio` - Extracts the audio from the video after downloading it. This
  option is useful if `-f wav` doesn't work because WAV is not available.
* `--audio-format` - Specifies what format to use if you're using `--extract-audio`
* `--sub-lang` - Downloads subtitle/closed-caption files for the given ISO language
  code. e.g. `--sub-lang en-GB` will download British English subtitles.  The subtitle files
  are saved in ...vtt files.

So for example the following command will download MP4 files and English closed-captions
for the given playlist:  
```
youtube-dl -f mp4 --sub-lang en https://www.youtube.com/playlist?list=PLdsZeeCVYnY3em55M2d3Iq3H-jSPLIWBF
```

...and the following will download WAV files instead of MP4 for the given playlist:  
```
youtube-dl  --extract-audio --audio-format wav --sub-lang en https://www.youtube.com/playlist?list=PLdsZeeCVYnY3em55M2d3Iq3H-jSPLIWBF
```

**Caveat:** There's a potential problem with using YouTube's automatically-generated captions;
they often have a whole bunch of repeated phrases as it goes through the recording, so much of
the resulting transcript is doubled up. So make sure you eyeball your TextGrids at the end to
ensure the results are what you expect!


### 2. Convert subtitles to Praat TextGrids

[vtt-to-textgrid.jar](https://github.com/nzilbb/ag/blob/master/bin/vtt-to-textgrid.jar?raw=true) 
is a utility for converting VTT files to TextGrids.

(If you prefer ELAN transcripts, try
[vtt-to-eaf.jar](https://github.com/nzilbb/ag/blob/master/bin/vtt-to-eaf.jar?raw=true)
instead)

It's a Java program, so you must have a recent version of Java installed for it to work.

Downloading the 
[vtt-to-textgrid.jar](https://github.com/nzilbb/ag/blob/master/bin/vtt-to-textgrid.jar?raw=true)
file and double-clicking it should work, but if not, you can run it from the command line like this:

```
java -jar vtt-to-textgrid.jar
```

When it starts, it looks like this:

![vtt-to-textgrid](https://raw.githubusercontent.com/nzilbb/ag/master/docs/vtt-to-textgrid.png)

You need to drag/drop vtt files into the big white space (or use the + button), and then click
*Convert*. This will save each TextGrid in the same folder as the VTT file.

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
