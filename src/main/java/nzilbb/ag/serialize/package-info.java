/**
 * Contains interfaces that are required to convert too/from specific file formats.
 *
 * <p>These interfaces must implemented in order to convert between annotation graphs
 * and the specific file formats of different tools (e.g. Transcriber, ELAN, plain text).
 *
 * <p>There are no such implementations provided here, only format interfaces that must be
 * instantiated in order for a serializer/deserializer to work with other API components.
 * 
 * <p> Modules can be defined which perform specific conversions for saving or loading
 * annotation graphs. Such a serialization module must be packaged in a .jar file with the
 * following: 
 * <ul>
 *  <li> at least one class that implements the {@link IDeserializer} and/or
 *       {@link ISerializer} interface, </li>
 *  <li> manifest attributes called <q>nzilbb-ag-serialize-IDeserializer</q> whose values
 *       are the fully-qualified class names of the IDeserializer-implementing classes,
 *       and/or  </li>
 *  <li> manifest attributes called <q>nzilbb-ag-serialize-ISerializer</q> whose values
 *       are the fully-qualified class names of the ISerializer-implementing classes. </li>
 * </ul>
 *
 * <p> e.g. a module implementing both IDeserializer and ISerializer with a class called
 * <tt> org.fancy.FancySerialization </tt>
 * should be deployed in a .jar archive with:
 * <ul>
 *  <li>a <q>nzilbb-ag-serialize-IDeserializer</q> manifest attribute with the value
 *      <q>org.fancy.FancySerialization</q>,</li>
 *  <li>a <q>nzilbb-ag-serialize-ISerializer</q> manifest attribute with the value
 *      <q>org.fancy.FancySerialization</q>,</li>
 *  <li>a org/fancy/FancySerialization.class implementation file</li>
 * </ul>
 *
 * @author Robert Fromont robert.fromont@canterbury.ac.nz
 **/
package nzilbb.ag.serialize;
