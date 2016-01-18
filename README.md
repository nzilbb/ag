# nzilbb.ag

Implementations of an Annotation Graph API for linguistic annotations.

Annotation Graphs are a data structure conceived by Steven Bird and Mark Liberman:
(http://lanl.arxiv.org/abs/cs/9907003)
(http://xxx.lanl.gov/PS_cache/cs/pdf/9903/9903003v1.pdf)

The structure is designed to be a tool-independent way of representing annotated linguistic data,
and essentially defines an Annotation Graph as a directed acyclic graph where:
 * nodes are 'anchors' that represent a point in time (in seconds) or a point in a text (in characters) (although the time/character offset label is optional), and
 * edges are 'annotations' which have a 'label' (the content of the annotation) and a 'type' (the kind of annotation, analogous with an 'tier' or 'layer')

This particular implementation, which is used for [LaBB-CAT](https://labbcat.canterbury.ac.nz), 
developed by the [NZILBB](http://www.nzilbb.canterbury.ac.nz),
includes extra features that allow tier hierarchies and parent/child constraints to be defined.

