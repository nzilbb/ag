/**
 * Implementation of an Annotation Graph API for linguistic annotations.
 * <p> Annotation Graphs are a data structure conceived by Steven Bird and Mark Liberman
 * <ul>
 *  <li><a href="http://lanl.arxiv.org/abs/cs/9907003">http://lanl.arxiv.org/abs/cs/9907003</a></li>
 *  <li><a href="http://xxx.lanl.gov/PS_cache/cs/pdf/9903/9903003v1.pdf">http://xxx.lanl.gov/PS_cache/cs/pdf/9903/9903003v1.pdf</a></li>
 * </ul>
 *
 * <p> The structure is designed to be a tool-independent way of representing annotated
 * linguistic data, and essentially defines an Annotation Graph as a directed acyclic
 * graph where:
 * <ul>
 *  <li>nodes are 'anchors' that represent a point in time (in seconds) or a point in a
 *   text (in characters) (although the time/character offset label is optional), and</li> 
 *  <li>edges are 'annotations' which have a 'label' (the content of the annotation) and a
 *   'type' (the kind of annotation, analogous with an 'tier' or 'layer')</li> 
 * </ul>
 *
 * <p>This particular implementation, which is used for
 * <a href="https://labbcat.canterbury.ac.nz">LaBB-CAT</a>,  developed by the
 * <a href="http://www.nzilbb.canterbury.ac.nz">NZILBB</a>, includes extra features that
 * allow tier hierarchies and parent/child constraints to be defined.  More details on
 * extra features are available in 
 * <a href="http://dx.doi.org/10.1016/j.csl.2017.01.004">http://dx.doi.org/10.1016/j.csl.2017.01.004</a> 
 *
 * <p>An <q>annotation graph</q> represented by the {@link Graph} class, and is a collection of
 * {@link Annotation}s (edges) joined by {@link Anchor}s (nodes) which may or may not have
 * temporal/character offsets. Superimposed over this temporally anchored graph is
 * another heirarchical graph, where annotations are nodes and edges are child-to-parent links. 
 *
 * <figure>
 *   <img src="doc-files/annotation-graph-example.svg">
 *   <figcaption>An example of a heirarchical annotation graph</figcaption>
 * </figure>
 *
 * <p>In addition to containing the nodes/edges, this class inherits from 
 * {@link Annotation} so that it can: 
 * <ul>
 *   <li>be the root node of the annotation hierarchy - i.e. be the parent of annotations
 *    at the top of the layer hierarchy</li> 
 *   <li>have start/end anchors</li>
 * </ul>
 * <p>In addition to this, the graph also has:
 * <ul>
 *  <li>a corpus attribute representing the collection to which it belongs (see 
 *   {@link Graph#getCorpus()}, {@link Graph#setCorpus(String)}),</li> 
 *  <li>definitions of annotation {@link Layer}s and their hierarchy</li>
 * </ul>
 *
 * <p>It is recommended that other graph attributes are represented as annotations that
 * 'tag' the whole graph, and that speakers/participants are also represented as such
 * annotations, on a "participant" layer, which is the parent of a "turn" layer which
 * defines speaker turns. 
 *
 * <p>The {@link Graph} class can also represent graph fragments (sub-graphs).  If this is a whole
 * graph, {@link Graph#getGraph()} == <var>this</var>, but if it's a fragment, then 
 * {@link Graph#getGraph()} != <var>this</var>. The {@link Graph#isFragment()}
 * convenience method captures this principle. The annotations in a graph fragment have 
 * the fragment object (not the whole-graph object) set as their {@link
 * Graph#getGraph()}. 
 *
 * @author Robert Fromont robert.fromont@canterbury.ac.nz
 */
package nzilbb.ag;
