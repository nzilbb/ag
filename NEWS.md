# nzilbb.ag 1.2.3

- Add category order/description to Schema.
- Add "tree" layer type.
- Improve annotation graph change tracking.
- Include offsetUnits are serialized to JSON.
- Performance improvements to annotation graph change merging
- Improvements to graph validation.

For LaBB-CAT version 20250430.1200

# nzilbb.ag 1.2.2

- Larger buffer for downloads.
- Ensure ARPAbet label definitions result in correct interpretations tweaking definition order.

For LaBB-CAT version 20241111.1200

# nzilbb.ag 1.2.1

For LaBB-CAT version 20241015.1456

- Improve JSON serialization to ensure LaBB-CAT's layer.validLabelsDefinition is supported.
- Standardize phoneme pickers with nzilbb.encoding.ValidLabelsDefinitions.
- Improve graph merge, particularly in relation to 'noise' annotations keeping word links.
- Ensure SemanticVersionComparator works with R package versions.
- Implement IO.OnlyASCII() to non-ASCII characters, and strip accents from letters.
- Improve filename sanitization.

# nzilbb.ag 1.2.0

For LaBB-CAT version 20240628.1316

- Improve graph merge for aligned-word-child layers (e.g. phones and POS tags).
- Ensure forced-alignment doesn't leave default-confidence anchors when word boundary
  coincides with utterance boundary.
- Ensure unrelated children are never deleted during merge, even automatically generated
  ones. 
- Ensure that MediaFile.generateFrom is serialized, so that callers can tell which files
  already exist. 
- GraphStore.saveMedia: swap order of mediaUrl/trackSuffix parameters, for consistency
  with other API functions, and so that the nullable parameter is last.
- GraphStore.deleteMedia: new method.
- Make encoding mapping tables explicit in javadoc documentation.

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
