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

More format conversions are available
[here](https://github.com/nzilbb/ag/blob/master/bin/README.md)

