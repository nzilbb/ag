# nzilbb.ag

[![DOI](https://zenodo.org/badge/49908890.svg)](https://zenodo.org/badge/latestdoi/49908890)


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

**API documentation is available at https://nzilbb.github.io/ag/**

## Format Conversion

Apart from use within LaBB-CAT, the object model can be used for other purposes like
format conversion, e.g. 
 * [vtt-to-textgrid](https://github.com/nzilbb/ag/blob/main/bin/vtt-to-textgrid.jar?raw=true) -
 a utility for converting subtitles downloaded from YouTube to
 [Praat](http://praat.org) TextGrids 
 * [trs-to-eaf](https://github.com/nzilbb/ag/blob/main/bin/trs-to-textgrid.jar?raw=true) -
 a utility for converting
 [Transcriber](http://trans.sourceforge.net/en/presentation.php) files to
 [ELAN](https://tla.mpi.nl/tools/tla-tools/elan/) files 

These use the serializers/deserializers in the *formatter* directory of this repository
to read a file in one format, convert it to an annotation graph, and then write that graph
out as a file in another format. As pointed out by
Cochran et al. (2007 - *Report from TILR Working Group 1 : Tools interoperability and input/output formats*)
this saves having order *n<sup>2</sup>* explicit conversion algorithms between formats;
only order *n* format conversions are required.

This exemplifies an approach to linguistic data interoperability called the *interlingua
philosophy on interoperability* by
[Witt et al. (2009)](https://www.w3.org/People/fsasaki/docs/lre-intro.pdf)
and uses annotation graphs as an 'interlingua' similar to work by 
[Schmidt et al. (2008)](https://ids-pub.bsz-bw.de/frontdoor/deliver/index/docId/2308/file/Schmidt%20etc_An_exchange_format_for_multimodal_annotations_2008.pdf),
except that rather using a third file format as a persistent intermediary, the annotation
graph models of the linguistic data are ephemeral, existing in memory only for the duration of the
conversion.

More format conversions are available
[here](https://github.com/nzilbb/ag/blob/main/bin/README.md#standalone-format-converters)

## Building from source

### Prerequisites

* The JDK for at least Java 11
  ```
  sudo apt install openjdk-11-jdk-headless
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
mvn package -pl :nzilbb.transcriber
```

### Build a specific transcriber module

```
mvn package -pl :nzilbb.transcriber.deepspeech
```

etc...

### Run all unit tests

```
mvn test
```

## Build documentation site

The documentation site, deployed in the `docs` subdirectory, includes:

- General information about annotation graphs, and the nzilbb.ag implementation.
- Javadoc class documentation.
- Documentation about sub-modules, including format converters, etc.

The command to ensure a clean build of the documentation is:

```
mvn clean package site site:deploy -Dmaven.test.skip
```

## Deploying to OSSRH

OSSRH is the central Maven repository where nzilbb.ag modules are deployed (published).

There are two type of deployment:

- *snapshot*: a transient deployment that can be updated during development/testing
- *release*: an official published version that cannot be changed once it's deployed

A *snapshot* deployment is done when the module version (`version` tag in pom.xml) ends with
`-SNAPSHOT`. Otherwise, any deployment is a *release*.

### Snapshot Deployment

To perform a snapshot deployment:

1. Ensure the `version` in pom.xml *is* suffixed with `-SNAPSHOT`
2. Execute the command:  
   ```
   mvn clean deploy -pl :nzilbb.ag
   ```

### Release Deployment

To perform a release deployment:

1. Ensure the `version` in pom.xml *isn't* suffixed with `-SNAPSHOT` e.g. use something
   like the following command from within the ag directory:  
   ```
   mvn versions:set -DnewVersion=1.1.0 -pl :nzilbb.ag
   ```
2. Execute the command:  
   ```
   mvn clean deploy -P release -pl :nzilbb.ag
   ```
3. Happy with everything? Complete the release with:
   ```
   mvn nexus-staging:release -P release -pl :nzilbb.ag
   ```
   (Or Publish via web interface: <https://central.sonatype.com/publishing/deployments>)\
   Otherwise:
   ```
   mvn nexus-staging:drop -P release -pl :nzilbb.ag
   ```
   ...and start again.
4. Regenerate the citation file:
   ```
   mvn cff:create -pl :nzilbb.ag
   ```
5. Commit/push all changes and create a release in GitHub

To release another module (e.g. formatters, annotators, etc.)

1. Ensure the `version` in pom.xml *isn't* suffixed with `-SNAPSHOT`  
   *NB* Don't use `mvn versions:set` for this if the module is a nzilbb.formatter, because it
   will fix versions in nzilbb.converter projects, which are manually set in their pom.xml
2. Execute the command:  
   ```
   mvn clean deploy -P release -pl :nzilbb.formatter.praat
   ```
3. Happy with everything? Complete the release with:
   ```
   mvn nexus-staging:release -P release -pl :nzilbb.formatter.praat
   ```
   Otherwise:
   ```
   mvn nexus-staging:drop -P release -pl :nzilbb.formatter.praat
   ```
   ...and start again.
4. Start a new .SNAPSHOT version.  
   *NB* Don't use `mvn versions:set` for this if the module is a nzilbb.formatter, because it
   will fix versions in nzilbb.converter projects, which are manually set in their pom.xml
