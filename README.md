# nzilbb.ag

Implementations of an Annotation Graph API for linguistic annotations.

Annotation Graphs are a data structure conceived by Steven Bird and Mark Liberman:
(http://lanl.arxiv.org/abs/cs/9907003)
(http://xxx.lanl.gov/PS_cache/cs/pdf/9903/9903003v1.pdf)

The structure is designed to be a tool-independent way of representing annotated linguistic data,
and essentially defines an Annotation Graph as a directed acyclic graph where:
 * nodes are 'anchors' that represent a point in time (in seconds) or a point in a text
 (in characters) (although the time/character offset label is optional), and 
 * edges are 'annotations' which have a 'label' (the content of the annotation) and a
 'type' (the kind of annotation, analogous with an 'tier' or 'layer') 

This particular implementation, which is used for
[LaBB-CAT](https://labbcat.canterbury.ac.nz), 
developed by the
[NZILBB](http://www.nzilbb.canterbury.ac.nz), 
includes extra features that allow tier hierarchies and parent/child constraints to be defined.
More details on extra features are available in
[http://dx.doi.org/10.1016/j.csl.2017.01.004](http://dx.doi.org/10.1016/j.csl.2017.01.004)

More API documentation is available [here](https://nzilbb.github.io/ag/)

## Format Conversion

Apart from use within LaBB-CAT, the object model can be used for other purposes like
format conversion, e.g. 
 * [vtt-to-textgrid](https://github.com/nzilbb/ag/blob/master/bin/vtt-to-textgrid.jar?raw=true) -
 a utility for converting subtitles downloaded from YouTube to
 [Praat](http://praat.org) TextGrids 
 * [trs-to-eaf](https://github.com/nzilbb/ag/blob/master/bin/trs-to-textgrid.jar?raw=true) -
 a utility for converting
 [Transcriber](http://trans.sourceforge.net/en/presentation.php) files to
 [ELAN](https://tla.mpi.nl/tools/tla-tools/elan/) files 

These use the serializers/deserializers in the *formatter* directory of this repository
to read a file in one format, convert it to an annotation graph, and then write that graph
out as a file in another format. As pointed out by
Cochran et al. (2007 - *Report from TILR Working Group 1 : Tools interoperability and input/output formats*)
this saves having order *n<sup>2</sup>* explicit conversion algorithms between formats;
only *2n* format conversions are required
(as some of these formats above are output-only, it's actually less than *2n*).

This exemplifies an approach to linguistic data interoperability called the *interlingua
philosophy on interoperability* by
[Witt et al. (2009)](https://www.w3.org/People/fsasaki/docs/lre-intro.pdf)
and uses annotation graphs as an 'interlingua' similar to work by 
[Schmidt et al. (2008)](https://ids-pub.bsz-bw.de/frontdoor/deliver/index/docId/2308/file/Schmidt%20etc_An_exchange_format_for_multimodal_annotations_2008.pdf),
except that rather using a third file format as a persistent intermediary, the annotation
graph models of the linguistic data are ephemeral, existing in memory only for the duration of the
conversion.

More format conversions are available
[here](https://github.com/nzilbb/ag/blob/master/bin/README.md)

## Building from source

### Prerequisites

* The JDK for at least Java 8
  ```
  sudo apt install default-jdk
  ```
* Maven
  ```
  sudo apt install maven
  ```

### Build nzilbb.ag.jar and also all format, annotator, and transcriber modules 

```
mvn package
```

### Build only nzilbb.ag.jar

```
mvn package -pl :nzilbb.ag
```

### Build all transcriber modules only

```
mvn package -pl :nzilbb.stt
```

### Build a specific transcriber module

```
mvn package -pl :nzilbb.stt.deepspeech
```

etc...

### Run all unit tests

```
mvn test
```

## Build documentation site

```
cd ag
mvn site
```