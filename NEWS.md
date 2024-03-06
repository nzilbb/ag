# nzilbb.ag 1.1.3

For LaBB-CAT version 20240306.1320

- Implement general dependency graph resolution class: nzilbb.util.DependencyGraph
- Move nzilbb.converter.Converter into nzilbb.ag to eliminate an unneccesary one-class jar
  dependency 
- Add helper function for getting duration of audio.
- Add setters that include confidence.

Implementations of an Annotation Graph API for linguistic annotations.

Annotation Graphs are a data structure conceived by Steven Bird and Mark Liberman:
(http://lanl.arxiv.org/abs/cs/9907003)
(http://xxx.lanl.gov/PS_cache/cs/pdf/9903/9903003v1.pdf)

The structure is designed to be a tool-independent way of representing annotated
linguistic data, and essentially defines an Annotation Graph as a directed acyclic graph
where: 
- nodes are 'anchors' that represent a point in time (in seconds) or a point in a text
   (in characters) (although the time/character offset label is optional), and  
- edges are 'annotations' which have a 'label' (the content of the annotation) and a
   'type' (the kind of annotation, analogous with an 'tier' or 'layer')  
