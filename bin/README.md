# Binary Files

This directory contains various compiled binary files, representing the latest
version of various *nzilbb.ag* components.

These include:
* nzilbb.ag.jar - the primary API/object model for nzilbb.ag
* nzilbb.???.jar - a number of serialization modules that can, for example, be installed in
  LaBB-CAT to add support for format conversions.
* ???-to-???.jar - stand-alone utilities to perform specific format conversions - see below...

## Standalone Format Converters

There are a number converter utilities that can be used to convert files from one format
to another, including:

* [vtt-to-textgrid.jar](https://github.com/nzilbb/ag/blob/master/bin/vtt-to-textgrid.jar?raw=true) - convert from *web subtitles (Web VTT)* to *Praat TextGrids*
* [vtt-to-eaf.jar](https://github.com/nzilbb/ag/blob/master/bin/vtt-to-eaf.jar?raw=true) - convert from *web subtitles (Web VTT)* to *ELAN* files (.eaf)
* [vtt-to-trs.jar](https://github.com/nzilbb/ag/blob/master/bin/vtt-to-trs.jar?raw=true) - convert from *web subtitles (Web VTT)* to *Transcriber* transcripts (.trs) 
* [trs-to-vtt.jar](https://github.com/nzilbb/ag/blob/master/bin/trs-to-vtt.jar?raw=true) - convert from *Transcriber* transcripts (.trs) to *web subtitles (Web VTT)*
* [trs-to-textgrid.jar](https://github.com/nzilbb/ag/blob/master/bin/trs-to-textgrid.jar?raw=true) - convert from *Transcriber* transcripts (.trs) to *Praat TextGrids*
* [trs-to-pdf.jar](https://github.com/nzilbb/ag/blob/master/bin/trs-to-pdf.jar?raw=true) - convert from *Transcriber* transcripts (.trs) to *PDF* files
* [trs-to-text.jar](https://github.com/nzilbb/ag/blob/master/bin/trs-to-text.jar?raw=true) - convert from *Transcriber* transcripts (.trs) to *plain text* files
* [trs-to-eaf.jar](https://github.com/nzilbb/ag/blob/master/bin/trs-to-eaf.jar?raw=true) - convert from *Transcriber* transcripts (.trs) to *ELAN* files (.eaf)
* [eaf-to-trs.jar](https://github.com/nzilbb/ag/blob/master/bin/eaf-to-trs.jar?raw=true) - convert from *ELAN* files (.eaf) to *Transcriber* transcripts (.trs)

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
java -jar trs-to-text.jar *.txt
```

...will include annotations and participant names in the output text files, but:

```
java -jar trs-to-text.jar --textonly *.txt
```

...produces text files that exclude all annotations and participant names.

The `--usage` command-line switch prints information about command-line options.

## Getting TextGrid Transcripts of YouTube Videos

The basic process of making a YouTube video corpus with TextGrid transcripts is:

1. Download Youtube videos with their closed-caption subtitle files.
2. Convert the closed-caption subtitle files to TextGrids.

### 1. Download Youtube videos with subtitles}

There's a tool called [youtube-dl](https://rg3.github.io/youtube-dl/) which can be used for
downloading videos from YouTube. It's a command-line program that can be given a URL for
downloading. If the URL is a playlist, then it will download all videos in the playlist into
separate files.

It has many useful command-line options:

1. `--help` -- Displays information about all the other command-line options.
2.  `-f` -- Sets the format for the video, e.g. `-f wav` should download WAV files,
  `mp4` will download MP4's, etc.
3. `--extract-audio` -- Extracts the audio from the video after downloading it. This
  option is useful if `-f wav` doesn't work because WAV is not available.
4. `--audio-format` -- Specifies what format to use if you're using `--extract-audio`
5. `--sub-lang` -- Downloads subtitle/closed-caption files for the given ISO language
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

