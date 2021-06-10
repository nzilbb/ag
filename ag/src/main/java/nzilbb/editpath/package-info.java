/**
 * Implementation of Wagner-Fischer algorithm to determine the minimum edit path.
 * <p> These classes provide suport for computing the minimum edit path between two lists
 * of objects of any class, and providing access to the mapping from one list to the
 * other, and minimum edit distance.
 * <p> e.g. The edit path between two lists of Integers can be determined using:
 * <pre>
 * MinimumEditPath&lt;Integer&gt; mp = new MinimumEditPath&lt;Integer&gt;();
 * List&lt;EditStep&lt;Integer&gt;&gt; path = mp.minimumEditPath(vFrom, vTo)
 * for (EditStep&lt;Integer&gt; step: path) {
 *   System.out.println("from " + step.getFrom() + " to " + step.getTo() 
 *                      + " : " + step.getOperation() + " distance " + step.getStepDistance());
 * }</pre>
 * <p> The equality comparison can be customized, e.g.:
 * <pre>
 * MinimumEditPath&lt;String&gt; mp = new MinimumEditPath&lt;String&gt;(
 *   new DefaultEditComparator&lt;String&gt;(new EqualsComparator&lt;String&gt;() {
 *     public int compare(String o1, String o2) {
 *      return o1.toLowerCase().compareTo(o2.toLowerCase());
 *     }
 *    }));</pre>
 * @author Robert Fromont robert.fromont@canterbury.ac.nz
 */
package nzilbb.editpath;
