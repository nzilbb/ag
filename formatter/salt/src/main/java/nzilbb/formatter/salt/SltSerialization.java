//
// Copyright 2021-2022 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of nzilbb.ag.
//
//    nzilbb.ag is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 3 of the License, or
//    (at your option) any later version.
//
//    nzilbb.ag is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with nzilbb.ag; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package nzilbb.formatter.salt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Spliterator;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import nzilbb.ag.*;
import nzilbb.ag.cli.Deserialize;
import nzilbb.ag.serialize.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.AnnotationComparatorByAnchor;
import nzilbb.ag.util.ConventionTransformer;
import nzilbb.ag.util.DefaultOffsetGenerator;
import nzilbb.ag.util.SimpleTokenizer;
import nzilbb.ag.util.SpanningConventionTransformer;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.util.IO;
import nzilbb.util.ISO639;
import nzilbb.util.TempFileInputStream;

/**
 * Serialization for .slt files produced by SALT.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class SltSerialization extends Deserialize implements GraphDeserializer, GraphSerializer {
  
  // Attributes:
  private ISO639 iso639 = new ISO639(); // for standard ISO 639 language code processing
  protected Vector<String> warnings;

  // support fractional seconds even though SALT doesn't
  private MessageFormat timesStampFormat = new MessageFormat("- {0,number,0}:{1,number,00.###}");
  
  /**
   * Name of the .slt file.
   * @see #getName()
   * @see #setName(String)
   */
  protected String name;
  /**
   * Getter for {@link #name}: Name of the .slt file.
   * @return Name of the .slt file.
   */
  public String getName() { return name; }
  /**
   * Setter for {@link #name}: Name of the .slt file.
   * @param newName Name of the .slt file.
   */
  public void setName(String newName) { name = newName; }
   
  /**
   * Transcript lines.
   * @see #getLines()
   * @see #setLines(Vector)
   */
  protected Vector<String> lines;
  /**
   * Getter for {@link #lines}: Transcript lines.
   * @return Transcript lines.
   */
  public Vector<String> getLines() { return lines; }
  /**
   * Setter for {@link #lines}: Transcript lines.
   * @param newLines Transcript lines.
   */
  public void setLines(Vector<String> newLines) { lines = newLines; }

  /**
   * Header lines
   * @see #getHeaders()
   * @see #setHeaders(Vector)
   */
  protected Vector<String> headers;
  /**
   * Getter for {@link #headers}: Header lines
   * @return Header lines
   */
  public Vector<String> getHeaders() { return headers; }
  /**
   * Setter for {@link #headers}: Header lines
   * @param newHeaders Header lines
   */
  public void setHeaders(Vector<String> newHeaders) { headers = newHeaders; }

  /**
   * The first line of the transcript, which defines the participants.
   * @see #getParticipantsHeader()
   * @see #setParticipantsHeader(String)
   */
  protected String participantsHeader;
  /**
   * Getter for {@link #participantsHeader}: The first line of the transcript, which
   * defines the participants. 
   * @return The first line of the transcript, which defines the participants.
   */
  public String getParticipantsHeader() { return participantsHeader; }
  /**
   * Setter for {@link #participantsHeader}: The first line of the transcript, which
   * defines the participants. 
   * @param newParticipantsHeader The first line of the transcript, which defines the participants.
   */
  public SltSerialization setParticipantsHeader(String newParticipantsHeader) { participantsHeader = newParticipantsHeader; return this; }
  
  /**
   * Layer schema.
   * @see #getSchema()
   * @see #setSchema(Schema)
   */
  protected Schema schema;
  /**
   * Getter for {@link #schema}: Layer schema.
   * @return Layer schema.
   */
  public Schema getSchema() { return schema; }
  /**
   * Setter for {@link #schema}: Layer schema.
   * @param newSchema Layer schema.
   */
  public void setSchema(Schema newSchema) { schema = newSchema; }

  /**
   * Layer that marks target participants.
   * @see #getTargetParticipantLayer()
   * @see #setTargetParticipantLayer(Layer)
   */
  protected Layer targetParticipantLayer;
  /**
   * Getter for {@link #targetParticipantLayer}: Layer that marks target participants.
   * @return Layer that marks target participants.
   */
  public Layer getTargetParticipantLayer() { return targetParticipantLayer; }
  /**
   * Setter for {@link #targetParticipantLayer}: Layer that marks target participants.
   * @param newTargetParticipantLayer Layer that marks target participants.
   */
  public SltSerialization setTargetParticipantLayer(Layer newTargetParticipantLayer) { targetParticipantLayer = newTargetParticipantLayer; return this; }

  /**
   * Layer for comments.
   * <p> Comments can be marked with:
   * <ul>
   *  <li> a line starting with <tt>+</tt> in the body of the transcript (i.e. not a
   *       header line). </li>
   *  <li> a line starting with <tt>=</tt> </li>
   *  <li> curly braces - e.g. <tt>{picks up book}</tt> </li>
   * </ul>
   * @see #getCommentLayer()
   * @see #setCommentLayer(Layer)
   */
  protected Layer commentLayer;
  /**
   * Getter for {@link #commentLayer}: Layer for comments.
   * @return Layer for comments.
   */
  public Layer getCommentLayer() { return commentLayer; }
  /**
   * Setter for {@link #commentLayer}: Layer for comments.
   * @param newCommentLayer Layer for comments.
   */
  public SltSerialization setCommentLayer(Layer newCommentLayer) { commentLayer = newCommentLayer; return this; }

  /**
   * C-Unit layer.
   * <p> An 'utterance' is a "Communication Unit" (C-Unit). These are grammatical not
   * phonological units (e.g. not delimited by pauses in speech), and are defined by rules
   * and guidelines, which help standardisation for comparison between samples, and
   * reference databases. 
   * <p> A C-unit is an "independent clause and it's modifiers", including subordinate
   * clauses (but not coordinated clauses) - i.e. it can't be broken down without losing
   * meaning, and is generally a clause with a subject and a verb. 
   * <p> C-Units are terminated with the following characters:
   * <ul>
   *  <li> <tt>?</tt> - question,</li>
   *  <li> <tt>!</tt> - surprise,</li>
   *  <li> <tt>~</tt> - intonation prompt,</li>
   *  <li> <tt>^</tt> - interrupted utterance (e.g. trailing off), or</li>
   *  <li> <tt>.</tt> - for all other utterances.</li>
   * </ul>
   * @see #getCUnitLayer()
   * @see #setCUnitLayer(Layer)
   */
  protected Layer cUnitLayer;
  /**
   * Getter for {@link #cUnitLayer}: C-Unit layer.
   * @return C-Unit layer.
   */
  public Layer getCUnitLayer() { return cUnitLayer; }
  /**
   * Setter for {@link #cUnitLayer}: C-Unit layer.
   * @param newCUnitLayer C-Unit layer.
   */
  public void setCUnitLayer(Layer newCUnitLayer) { cUnitLayer = newCUnitLayer; }
  
  /**
   * Layer for annotating parenthetical remarks.
   * <p>  Parenthetical remarks, which offer some explanation or comment but don't
   * contribute to the meaning of the surrounding utterances, may appear independently or
   * embedded in the utterance. They are marked with double-parentheses - e.g. <br>
   * <tt>Then the boy ((I can/'t remember his name)) left the house.</tt>
   * @see #getParentheticalLayer()
   * @see #setParentheticalLayer(Layer)
   */
  protected Layer parentheticalLayer;
  /**
   * Getter for {@link #parentheticalLayer}: Layer for annotating parenthetical remarks.
   * @return Layer for annotating parenthetical remarks.
   */
  public Layer getParentheticalLayer() { return parentheticalLayer; }
  /**
   * Setter for {@link #parentheticalLayer}: Layer for annotating parenthetical remarks.
   * @param newParentheticalLayer Layer for annotating parenthetical remarks.
   */
  public SltSerialization setParentheticalLayer(Layer newParentheticalLayer) { parentheticalLayer = newParentheticalLayer; return this; }

  /**
   * Layer for annotating proper names.
   * <p> Parts of proper names are separated with underscores <tt>_</tt> to make them
   * typographically single words, instead of using spaces - e.g. <tt>Mr_Jones</tt>. 
   * @see #getProperNameLayer()
   * @see #setProperNameLayer(Layer)
   */
  protected Layer properNameLayer;
  /**
   * Getter for {@link #properNameLayer}: Layer for annotating proper names.
   * @return Layer for annotating proper names.
   */
  public Layer getProperNameLayer() { return properNameLayer; }
  /**
   * Setter for {@link #properNameLayer}: Layer for annotating proper names.
   * @param newProperNameLayer Layer for annotating proper names.
   */
  public SltSerialization setProperNameLayer(Layer newProperNameLayer) { properNameLayer = newProperNameLayer; return this; }

  /**
   * Layer for annotating repetitions.
   * <p>  Repetition for emphasis is marked by joining the second and subsequent
   * repetitions with underscore <tt>_</tt>. Optionally, these can be tagged with the
   * repeated word using the vertical bar <tt>|</tt> characters. For example, in the
   * following, the speaker says "there were heaps and heaps and heaps of them", saying
   * the word "heaps", and then repeating it twice more for emphasis. The repetitions
   * tagged to count as one instance of "heaps": <br>
   * <tt>there were heaps and heaps_and_heaps|heaps of them</tt>
   * @see #getRepetitionsLayer()
   * @see #setRepetitionsLayer(Layer)
   */
  protected Layer repetitionsLayer;
  /**
   * Getter for {@link #repetitionsLayer}: Layer for annotating repetitions.
   * @return Layer for annotating repetitions.
   */
  public Layer getRepetitionsLayer() { return repetitionsLayer; }
  /**
   * Setter for {@link #repetitionsLayer}: Layer for annotating repetitions.
   * @param newRepetitionsLayer Layer for annotating repetitions.
   */
  public SltSerialization setRepetitionsLayer(Layer newRepetitionsLayer) { repetitionsLayer = newRepetitionsLayer; return this; }
  
  /**
   * Layer for tagging words with their root form.
   * <p> The vertical bar may also used to mark the root form of a word that is an
   * irregular form (e.g. <tt>went|go</tt>), and always when uttered with an incorrect
   * form e.g. if the child says "he wented home", this would be transcribed: <br> 
   * <tt>he wented|went to school</tt>
   * @see #getRootLayer()
   * @see #setRootLayer(Layer)
   */
  protected Layer rootLayer;
  /**
   * Getter for {@link #rootLayer}: Layer for tagging words with their root form.
   * @return Layer for tagging words with their root form.
   */
  public Layer getRootLayer() { return rootLayer; }
  /**
   * Setter for {@link #rootLayer}: Layer for tagging words with their root form.
   * @param newRootLayer Layer for tagging words with their root form.
   */
  public SltSerialization setRootLayer(Layer newRootLayer) { rootLayer = newRootLayer; return this; }
  
  /**
   * Layer for tagging errors.
   * <p> Any word or utterance can be tagged with a 'code' in square brackets
   * <tt>[]</tt>. The codes may contain letters, numbers, colons, but no spaces or other
   * characters.
   * <p> A code that starts with E is interpreted to be an error code, e.g. <br>
   * <tt>he wented|went[EO:went] to school</tt>
   * @see #getErrorLayer()
   * @see #setErrorLayer(Layer)
   */
  protected Layer errorLayer;
  /**
   * Getter for {@link #errorLayer}: Layer for tagging errors.
   * @return Layer for tagging errors.
   */
  public Layer getErrorLayer() { return errorLayer; }
  /**
   * Setter for {@link #errorLayer}: Layer for tagging errors.
   * @param newErrorLayer Layer for tagging errors.
   */
  public SltSerialization setErrorLayer(Layer newErrorLayer) { errorLayer = newErrorLayer; return this; }

  /**
   * Layer for tagging sound effects.
   * <p> 'Sound effects' like animal noises are prefixed with the percent symbol
   * <tt>%</tt>, e.g.: <br>
   *  <tt>%yip_yip went Schnitzel_von_Crumm</tt>
   * @see #getSoundEffectLayer()
   * @see #setSoundEffectLayer(Layer)
   */
  protected Layer soundEffectLayer;
  /**
   * Getter for {@link #soundEffectLayer}: Layer for tagging sound effects.
   * @return Layer for tagging sound effects.
   */
  public Layer getSoundEffectLayer() { return soundEffectLayer; }
  /**
   * Setter for {@link #soundEffectLayer}: Layer for tagging sound effects.
   * @param newSoundEffectLayer Layer for tagging sound effects.
   */
  public SltSerialization setSoundEffectLayer(Layer newSoundEffectLayer) { soundEffectLayer = newSoundEffectLayer; return this; }

  /**
   * Layer for pause annotations.
   * <p>  Pauses are marked in several ways:
   * <ul>
   *  <li> Pauses during utterances are marked inline, with a colon <tt>:</tt> optionally followed
   *       by the length of the pause in seconds, e.g. <br>
   *    <tt>I liked the movie : alot :02 because</tt> </li>
   *  <li> Pauses between utterances are marked on their own line, again optionally with the
   *       duration of the pause, prefixed by a colon : if the speaker turn or topic
   *       changes – e.g. <br>
   *    <tt>: :05 {somebody enters the room}</tt> </li>
   *  <li> or a semicolon ; if the same speaker continues – e.g. <br>
   *    <tt>; :05</tt> </li>
   * </ul>
   * @see #getPauseLayer()
   * @see #setPauseLayer(Layer)
   */
  protected Layer pauseLayer;
  /**
   * Getter for {@link #pauseLayer}: Layer for pause annotations.
   * @return Layer for pause annotations.
   */
  public Layer getPauseLayer() { return pauseLayer; }
  /**
   * Setter for {@link #pauseLayer}: Layer for pause annotations.
   * @param newPauseLayer Layer for pause annotations.
   */
  public SltSerialization setPauseLayer(Layer newPauseLayer) { pauseLayer = newPauseLayer; return this; }
  
  /**
   * Layer for bound morpheme annotations.
   * <p> In order to analyse morphological performance and error rates, word inflections
   * are annotated using a slash <samp>/</samp> character. The base word is transcribed in
   * it's <i>uninflected</i> form, whether its orthography is regular
   * (e.g. <samp>tree/s</samp>) or not (e.g. <samp>baby/s</samp>). However, where the
   * inflection is phonologically irregular (i.e. pronunciation of the root part is
   * significantly changed), the irregular form is used (i.e. <samp>geese</samp> not
   * <samp>goose/s</samp>), in order to make transcription easier. 
   * <ul>
   *  <li> Plural forms are annotated with <tt>/s</tt>, but only if a singular form
   *       exists – i.e. <tt>baby/s</tt> but <tt>pants</tt> </li>
   *  <li> Possessives are transcribed as <tt>/z</tt> but not possessive pronouns –
   *       e.g. <tt>baby/z</tt> but <tt>his</tt> </li>
   *  <li> In the case of plural possessives, both annotations are used –
   *       i.e. <tt>cow/s/z</tt> </li>
   *  <li> Contractions are split into morphemes using a slash <tt>/</tt>, except
   *       those where the root word is phonologically different from the contracted
   *       version - e.g. <tt>can/'t</tt>, <tt>I/'m</tt>, <tt>did/n't</tt> but
   *       <tt>won't</tt>, <tt>don't</tt> </li>
   *  <li> 3rd-person singular verb inflections (for English) are annotated with
   *       <tt>/3s</tt>, except irregular verbs – e.g. <tt>look/3s</tt>,
   *       <tt>jump/3s</tt>, but <tt>has</tt>, <tt>does</tt> </li>
   *  <li> Progressive verb forms are marked <tt>/ing</tt>, but so called 'gerunds'
   *       (nouns formed by adding ~ing to a verb) are not – i.e. <tt>I was fish/ing</tt>
   *       but <tt>Fishing is fun</tt> </li>
   *  <li> Regular past-tense- and perfect-inflected verbs are annotated with
   *       <tt>/ed</tt>, while irregular forms are not – e.g. <tt>I walk/ed</tt>,
   *       <tt>I have walk/ed</tt>, but <tt>I went</tt> and <tt>I have been</tt> </li>
   *  <li> Past participles, passives, and adjectival forms are not annotated with
   *       <tt>/ed</tt> – e.g. <tt>I was </tt><tt>tired</tt>, <tt>I was
   *       robbed</tt>, <tt>she seems irritated</tt> </li>
   * </ul>
   * @see #getBoundMorphemeLayer()
   * @see #setBoundMorphemeLayer(Layer)
   */
  protected Layer boundMorphemeLayer;
  /**
   * Getter for {@link #boundMorphemeLayer}: Layer for bound morpheme annotations.
   * @return Layer for bound morpheme annotations.
   */
  public Layer getBoundMorphemeLayer() { return boundMorphemeLayer; }
  /**
   * Setter for {@link #boundMorphemeLayer}: Layer for bound morpheme annotations.
   * @param newBoundMorphemeLayer Layer for bound morpheme annotations.
   */
  public SltSerialization setBoundMorphemeLayer(Layer newBoundMorphemeLayer) { boundMorphemeLayer = newBoundMorphemeLayer; return this; }
  
  /**
   * Layer for maze annotations.
   * <p> 'Mazes' are false/starts, repetitions, and reformulations. They are delimited by
   * parentheses <tt>( )</tt>, are used in analysis of possible utterance formulation
   * problems, and are excluded from mean utterance length computations. 
   * @see #getMazeLayer()
   * @see #setMazeLayer(Layer)
   */
  protected Layer mazeLayer;
  /**
   * Getter for {@link #mazeLayer}: Layer for maze annotations.
   * @return Layer for maze annotations.
   */
  public Layer getMazeLayer() { return mazeLayer; }
  /**
   * Setter for {@link #mazeLayer}: Layer for maze annotations.
   * @param newMazeLayer Layer for maze annotations.
   */
  public SltSerialization setMazeLayer(Layer newMazeLayer) { mazeLayer = newMazeLayer; return this; }
  
  /**
   * Layer for tagging partial words.
   * <p> Stuttering and partial words are also marked with an asterisk <tt>*</tt>, e.g.
   * <tt>(and ah we e* ever*) and everybody can come</tt>
   * @see #getPartialWordLayer()
   * @see #setPartialWordLayer(Layer)
   */
  protected Layer partialWordLayer;
  /**
   * Getter for {@link #partialWordLayer}: Layer for tagging partial words.
   * @return Layer for tagging partial words.
   */
  public Layer getPartialWordLayer() { return partialWordLayer; }
  /**
   * Setter for {@link #partialWordLayer}: Layer for tagging partial words.
   * @param newPartialWordLayer Layer for tagging partial words.
   */
  public SltSerialization setPartialWordLayer(Layer newPartialWordLayer) { partialWordLayer = newPartialWordLayer; return this; }

  /**
   * Layer for annotating omissions.
   * <p>  Omitted words (i.e. where a word is not uttered but it's grammatically required)
   * are included prefixed by an asterisk <tt>*</tt> e.g. <br>
   * "This a cookie" → <tt>This *is a cookie</tt>
   * @see #getOmissionLayer()
   * @see #setOmissionLayer(Layer)
   */
  protected Layer omissionLayer;
  /**
   * Getter for {@link #omissionLayer}: Layer for annotating omissions.
   * @return Layer for annotating omissions.
   */
  public Layer getOmissionLayer() { return omissionLayer; }
  /**
   * Setter for {@link #omissionLayer}: Layer for annotating omissions.
   * @param newOmissionLayer Layer for annotating omissions.
   */
  public SltSerialization setOmissionLayer(Layer newOmissionLayer) { omissionLayer = newOmissionLayer; return this; }

  /**
   * Layer for code annotations (except errors, which use {@link #errorLayer}).
   * <p> Any word or utterance can be tagged with a 'code' in square brackets
   * <tt>[]</tt>. The codes may contain letters, numbers, colons, but no spaces or other
   * characters.
   * <p> A code that starts with E is interpreted to be an error code; other codes are
   * annotated on this layer.
   * @see #getCodeLayer()
   * @see #setCodeLayer(Layer)
   */
  protected Layer codeLayer;
  /**
   * Getter for {@link #codeLayer}: Layer for code annotations (except errors, which use
   * {@link #errorLayer}).  
   * @return Layer for code annotations (except errors, which use #errorLayer).
   */
  public Layer getCodeLayer() { return codeLayer; }
  /**
   * Setter for {@link #codeLayer}: Layer for code annotations (except errors, which use
   * {@link #errorLayer}). 
   * @param newCodeLayer Layer for code annotations (except errors, which use
   * {@link #errorLayer}).
   */
  public SltSerialization setCodeLayer(Layer newCodeLayer) { codeLayer = newCodeLayer; return this; }

  /**
   * Transcript language layer.
   * @see #getLanguageLayer()
   * @see #setLanguageLayer(Layer)
   */
  protected Layer languageLayer;
  /**
   * Getter for {@link #languageLayer}: Transcript language layer.
   * @return Transcript language layer.
   */
  public Layer getLanguageLayer() { return languageLayer; }
  /**
   * Setter for {@link #languageLayer}: Transcript language layer.
   * @param newLanguageLayer Transcript language layer.
   */
  public SltSerialization setLanguageLayer(Layer newLanguageLayer) { languageLayer = newLanguageLayer; return this; }

  /**
   * Target Participant ID layer.
   * @see #getParticipantIdLayer()
   * @see #setParticipantIdLayer(Layer)
   */
  protected Layer participantIdLayer;
  /**
   * Getter for {@link #participantIdLayer}: Target Participant ID layer.
   * @return Target Participant ID layer.
   */
  public Layer getParticipantIdLayer() { return participantIdLayer; }
  /**
   * Setter for {@link #participantIdLayer}: Target Participant ID layer.
   * @param newParticipantIdLayer Target Participant ID layer.
   */
  public SltSerialization setParticipantIdLayer(Layer newParticipantIdLayer) { participantIdLayer = newParticipantIdLayer; return this; }

  /**
   * Target participant gender layer.
   * @see #getGenderLayer()
   * @see #setGenderLayer(Layer)
   */
  protected Layer genderLayer;
  /**
   * Getter for {@link #genderLayer}: Target participant gender layer.
   * @return Target participant gender layer.
   */
  public Layer getGenderLayer() { return genderLayer; }
  /**
   * Setter for {@link #genderLayer}: Target participant gender layer.
   * @param newGenderLayer Target participant gender layer.
   */
  public SltSerialization setGenderLayer(Layer newGenderLayer) { genderLayer = newGenderLayer; return this; }

  /**
   * Target participant date of birth layer.
   * @see #getDobLayer()
   * @see #setDobLayer(Layer)
   */
  protected Layer dobLayer;
  /**
   * Getter for {@link #dobLayer}: Target participant date of birth layer.
   * @return Target participant date of birth layer.
   */
  public Layer getDobLayer() { return dobLayer; }
  /**
   * Setter for {@link #dobLayer}: Target participant date of birth layer.
   * @param newDobLayer Target participant date of birth layer.
   */
  public SltSerialization setDobLayer(Layer newDobLayer) { dobLayer = newDobLayer; return this; }

  /**
   * Date of elicitation layer.
   * @see #getDoeLayer()
   * @see #setDoeLayer(Layer)
   */
  protected Layer doeLayer;
  /**
   * Getter for {@link #doeLayer}: Date of elicitation layer.
   * @return Date of elicitation layer.
   */
  public Layer getDoeLayer() { return doeLayer; }
  /**
   * Setter for {@link #doeLayer}: Date of elicitation layer.
   * @param newDoeLayer Date of elicitation layer.
   */
  public SltSerialization setDoeLayer(Layer newDoeLayer) { doeLayer = newDoeLayer; return this; }

  /**
   * Current age layer.
   * @see #getCaLayer()
   * @see #setCaLayer(Layer)
   */
  protected Layer caLayer;
  /**
   * Getter for {@link #caLayer}: Current age layer.
   * @return Current age layer.
   */
  public Layer getCaLayer() { return caLayer; }
  /**
   * Setter for {@link #caLayer}: Current age layer.
   * @param newCaLayer Current age layer.
   */
  public SltSerialization setCaLayer(Layer newCaLayer) { caLayer = newCaLayer; return this; }

  /**
   * Target participant ethnicity layer.
   * @see #getEthnicityLayer()
   * @see #setEthnicityLayer(Layer)
   */
  protected Layer ethnicityLayer;
  /**
   * Getter for {@link #ethnicityLayer}: Target participant ethnicity layer.
   * @return Target participant ethnicity layer.
   */
  public Layer getEthnicityLayer() { return ethnicityLayer; }
  /**
   * Setter for {@link #ethnicityLayer}: Target participant ethnicity layer.
   * @param newEthnicityLayer Target participant ethnicity layer.
   */
  public SltSerialization setEthnicityLayer(Layer newEthnicityLayer) { ethnicityLayer = newEthnicityLayer; return this; }

  /**
   * Sampling context layer.
   * @see #getContextLayer()
   * @see #setContextLayer(Layer)
   */
  protected Layer contextLayer;
  /**
   * Getter for {@link #contextLayer}: Sampling context layer.
   * @return Sampling context layer.
   */
  public Layer getContextLayer() { return contextLayer; }
  /**
   * Setter for {@link #contextLayer}: Sampling context layer.
   * @param newContextLayer Sampling context layer.
   */
  public SltSerialization setContextLayer(Layer newContextLayer) { contextLayer = newContextLayer; return this; }

  /**
   * Subgroup/story layer.
   * @see #getSubgroupLayer()
   * @see #setSubgroupLayer(Layer)
   */
  protected Layer subgroupLayer;
  /**
   * Getter for {@link #subgroupLayer}: Subgroup/story layer.
   * @return Subgroup/story layer.
   */
  public Layer getSubgroupLayer() { return subgroupLayer; }
  /**
   * Setter for {@link #subgroupLayer}: Subgroup/story layer.
   * @param newSubgroupLayer Subgroup/story layer.
   */
  public SltSerialization setSubgroupLayer(Layer newSubgroupLayer) { subgroupLayer = newSubgroupLayer; return this; }
  
  /**
   * Collection point layer.
   * @see #getCollectLayer()
   * @see #setCollectLayer(Layer)
   */
  protected Layer collectLayer;
  /**
   * Getter for {@link #collectLayer}: Collection point layer.
   * @return Collection point layer.
   */
  public Layer getCollectLayer() { return collectLayer; }
  /**
   * Setter for {@link #collectLayer}: Collection point layer.
   * @param newCollectLayer Collection point layer.
   */
  public SltSerialization setCollectLayer(Layer newCollectLayer) { collectLayer = newCollectLayer; return this; }
  
  /**
   * Location layer.
   * @see #getLocationLayer()
   * @see #setLocationLayer(Layer)
   */
  protected Layer locationLayer;
  /**
   * Getter for {@link #locationLayer}: Location layer.
   * @return Location layer.
   */
  public Layer getLocationLayer() { return locationLayer; }
  /**
   * Setter for {@link #locationLayer}: Location layer.
   * @param newLocationLayer Location layer.
   */
  public SltSerialization setLocationLayer(Layer newLocationLayer) { locationLayer = newLocationLayer; return this; }

  /**
   * Format for dates in the SALT file. The default is "M/d/YYYY".
   * @see #getDateFormat()
   * @see #setDateFormat(String)
   */
  protected String dateFormat = "M/d/YYYY";
  /**
   * Getter for {@link #dateFormat}: Format for dates in the SALT file. The default is "M/d/YYYY".
   * @return Format for dates in the SALT file. The default is "M/d/YYYY".
   */
  public String getDateFormat() { return dateFormat; }
  /**
   * Setter for {@link #dateFormat}: Format for dates in the SALT file.
   * @param newDateFormat Format for dates in the SALT file.
   */
  public SltSerialization setDateFormat(String newDateFormat) { dateFormat = newDateFormat; return this; }
  
  /**
   * Whether to parse the in-line conventions when deserializing. Default is true.
   * <p> If false, then only meta-data headers, comment lines, and time stamps are parsed;
   * all in-line annotation conventions are left as-is in the resulting graph. 
   * @see #getParseInlineConventions()
   * @see #setParseInlineConventions(Boolean)
   */
  protected Boolean parseInlineConventions;
  /**
   * Getter for {@link #parseInlineConventions}: Whether to parse the in-line conventions
   * when deserializing. Default is true. 
   * <p> If false, then only meta-data headers, comment lines, and time stamps are parsed;
   * all in-line annotation conventions are left as-is in the resulting graph. 
   * @return Whether to parse the in-line conventions when deserializing. Default is true.
   */
  public Boolean getParseInlineConventions() { return parseInlineConventions; }
  /**
   * Setter for {@link #parseInlineConventions}: Whether to parse the in-line conventions
   * when deserializing. Default is true. 
   * <p> If false, then only meta-data headers, comment lines, and time stamps are parsed;
   * all in-line annotation conventions are left as-is in the resulting graph. 
   * @param newParseInlineConventions Whether to parse the in-line conventions when deserializing. Default is true.
   */
  public SltSerialization setParseInlineConventions(Boolean newParseInlineConventions) { parseInlineConventions = newParseInlineConventions; return this; }
  
  /**
   * Duration of the media file in seconds, if known.
   * @see #getMediaDurationSeconds()
   * @see #setMediaDurationSeconds(Double)
   */
  protected Double mediaDurationSeconds;
  /**
   * Getter for {@link #mediaDurationSeconds}: Duration of the media file in seconds, if known.
   * @return Duration of the media file in seconds, if known.
   */
  public Double getMediaDurationSeconds() { return mediaDurationSeconds; }
  /**
   * Setter for {@link #mediaDurationSeconds}: Duration of the media file in seconds, if known.
   * @param newMediaDurationSeconds Duration of the media file in seconds, if known.
   */
  public SltSerialization setMediaDurationSeconds(Double newMediaDurationSeconds) { mediaDurationSeconds = newMediaDurationSeconds; return this; }
  
  /**
   * Utterance tokenizer.  The default is {@link SimpleTokenizer}.
   * @see #getTokenizer()
   * @see #setTokenizer(GraphTransformer)
   */
  protected GraphTransformer tokenizer;
  /**
   * Getter for {@link #tokenizer}: Utterance tokenizer.
   * @return Utterance tokenizer.
   */
  public GraphTransformer getTokenizer() { return tokenizer; }
  /**
   * Setter for {@link #tokenizer}: Utterance tokenizer.
   * @param newTokenizer Utterance tokenizer.
   */
  public void setTokenizer(GraphTransformer newTokenizer) { tokenizer = newTokenizer; }
   
  private long graphCount = 0;
  private long consumedGraphCount = 0;
  /**
   * Determines how far through the serialization is.
   * @return An integer between 0 and 100 (inclusive), or null if progress can not be calculated.
   */
  public Integer getPercentComplete() {
    if (graphCount < 0) return null;
    return (int)((consumedGraphCount * 100) / graphCount);
  }

  /**
   * Serialization marked for cancelling.
   * @see #getCancelling()
   * @see #setCancelling(boolean)
   */
  protected boolean cancelling;
  /**
   * Getter for {@link #cancelling}: Serialization marked for cancelling.
   * @return Serialization marked for cancelling.
   */
  public boolean getCancelling() { return cancelling; }
  /**
   * Setter for {@link #cancelling}: Serialization marked for cancelling.
   * @param newCancelling Serialization marked for cancelling.
   */
  public SltSerialization setCancelling(boolean newCancelling) { cancelling = newCancelling; return this; }
   
  /**
   * Cancel the serialization in course (if any).
   */
  public void cancel() {
    setCancelling(true);
  }
   
  // Methods:
   
  /**
   * Default constructor.
   */
  public SltSerialization() {
  } // end of constructor
   
  /**
   * Resete state.
   */
  public void reset() {
    warnings = new Vector<String>();
    participantsHeader = "";
    lines = new Vector<String>();
    headers = new Vector<String>();
  } // end of reset()

  // IStreamDeserializer methods:

  /**
   * Returns the deserializer's descriptor
   * @return The deserializer's descriptor
   */
  public SerializationDescriptor getDescriptor() {
    return new SerializationDescriptor(
      "SALT transcript", getClass().getPackage().getImplementationVersion(),
      "text/x-salt", ".slt", "1.0.0", getClass().getResource("icon.png"));
  }

  /**
   * Sets parameters for deserializer as a whole.  This might include database connection
   * parameters, locations of supporting files, etc.
   * <p>When the deserializer is installed, this method should be invoked with an empty parameter
   * set, to discover what (if any) general configuration is required. If parameters are returned,
   * and user interaction is possible, then the user may be presented with an interface for
   * setting/confirming these parameters. Once the parameters are set, this method can be
   * invoked again with the required values. 
   * @param schema The layer schema, definining layers and the way they interrelate.
   * @return A list of configuration parameters that must be set before
   * {@link GraphDeserializer#setParameters(ParameterSet)} can be invoked. 
   * If this is an empty list, 
   * {@link GraphDeserializer#setParameters(ParameterSet)} can be invoked. 
   * If it's not an empty list, this method must be invoked again with the returned
   * parameters' values set. 
   */
  public ParameterSet configure(ParameterSet configuration, Schema schema) {
    setSchema(schema);
    targetParticipantLayer = null;
    commentLayer = null;
    cUnitLayer = null;
    parentheticalLayer = null;
    properNameLayer = null;
    repetitionsLayer = null;
    rootLayer = null;
    errorLayer = null;
    soundEffectLayer = null;
    pauseLayer = null;
    boundMorphemeLayer = null;
    mazeLayer = null;
    partialWordLayer = null;
    omissionLayer = null;
    codeLayer = null;
    languageLayer = null;
    participantIdLayer = null;
    genderLayer = null;
    dobLayer = null;
    doeLayer = null;
    caLayer = null;
    ethnicityLayer = null;
    contextLayer = null;
    subgroupLayer = null;
    collectLayer = null;
    locationLayer = null;

    configuration.apply(this);

    LinkedHashMap<String,Layer> wordTagLayers = new LinkedHashMap<String,Layer>();
    LinkedHashMap<String,Layer> participantTagLayers = new LinkedHashMap<String,Layer>();
    LinkedHashMap<String,Layer> transcriptTagLayers = new LinkedHashMap<String,Layer>();
    LinkedHashMap<String,Layer> spanLayers = new LinkedHashMap<String,Layer>();
    LinkedHashMap<String,Layer> phraseLayers = new LinkedHashMap<String,Layer>();
    for (Layer top : schema.getRoot().getChildren().values()) {
      if (top.getChildren().size() == 0) {
        if (top.getAlignment() == Constants.ALIGNMENT_NONE) {
          transcriptTagLayers.put(top.getId(), top);
        } else {
          spanLayers.put(top.getId(), top);
        }
      } // childless
    } // next top level layer
    for (Layer child : schema.getParticipantLayer().getChildren().values()) {
      if (child.getChildren().size() == 0) {
        if (child.getAlignment() == Constants.ALIGNMENT_NONE) {
          participantTagLayers.put(child.getId(), child);
        }
      } // childless child
    } // next level layer
    for (Layer child : schema.getTurnLayer().getChildren().values()) {
      if (child.getChildren().size() == 0) {
        if (child.getAlignment() == Constants.ALIGNMENT_INTERVAL
            && !child.getId().equals(schema.getWordLayerId())
            && !child.getId().equals(schema.getUtteranceLayerId())) {
          phraseLayers.put(child.getId(), child);
        }
      } // childless child
    } // next level layer
    for (Layer child : schema.getWordLayer().getChildren().values()) {
      if (child.getChildren().size() == 0) {
        if (child.getAlignment() == Constants.ALIGNMENT_NONE) {
          wordTagLayers.put(child.getId(), child);
        }
      } // childless child
    } // next word layer

    // all participant and transcript attribute layers together:
    LinkedHashMap<String,Layer> attributeLayers = new LinkedHashMap<String,Layer>();
    for (String id : participantTagLayers.keySet()) {
      attributeLayers.put(id, participantTagLayers.get(id));
    }
    for (String id : transcriptTagLayers.keySet()) {
      attributeLayers.put(id, transcriptTagLayers.get(id));
    }
         
    Parameter p = configuration.containsKey("cUnitLayer")?configuration.get("cUnitLayer")
      :configuration.addParameter(
        new Parameter("cUnitLayer", Layer.class, "C-Unit layer", "Layer for marking c-units"));
    String[] possibilitiesCUnit = {"c-unit","cunit","sentence"};
    p.setValue(Utility.FindLayerById(phraseLayers, Arrays.asList(possibilitiesCUnit)));
    p.setPossibleValues(phraseLayers.values());

    p = configuration.containsKey("targetParticipantLayer")?
      configuration.get("targetParticipantLayer"):
      configuration.addParameter(
        new Parameter("targetParticipantLayer", Layer.class, "Target Participant Layer",
                      "Layer for marking the target participant"));
    String[] targetParticipantLayerPossibilities = {
      "main_participant", "main", "target", "participant_target"};
    p.setValue(Utility.FindLayerById(
                 participantTagLayers, Arrays.asList(targetParticipantLayerPossibilities)));
    p.setPossibleValues(participantTagLayers.values());
      
    p = configuration.containsKey("commentLayer")?
      configuration.get("commentLayer"):
      configuration.addParameter(
        new Parameter("commentLayer", Layer.class, "Comment Layer", "Layer for comments"));
    String[] commentLayerPossibilities = {"comment", "comments" };
    p.setValue(Utility.FindLayerById(
                 spanLayers, Arrays.asList(commentLayerPossibilities)));
    p.setPossibleValues(spanLayers.values());
      
    p = configuration.containsKey("parentheticalLayer")?
      configuration.get("parentheticalLayer"):
      configuration.addParameter(
                new Parameter("parentheticalLayer", Layer.class, "Parenthetical Layer",
                              "Layer for marking parenthetical remarks by the speaker"));
    String[] parentheticalLayerPossibilities = {"parenthetical", "parentheticals" };
    p.setValue(Utility.FindLayerById(
                 phraseLayers, Arrays.asList(parentheticalLayerPossibilities)));
    p.setPossibleValues(phraseLayers.values());
      
    p = configuration.containsKey("properNameLayer")?
      configuration.get("properNameLayer"):
      configuration.addParameter(
        new Parameter("properNameLayer", Layer.class, "Proper Name Layer",
                      "Layer for tagging proper names"));
    String[] properNameLayerPossibilities = {
      "propername", "name", "namedentity", "entity"};
    p.setValue(Utility.FindLayerById(
                 phraseLayers, Arrays.asList(properNameLayerPossibilities)));
    p.setPossibleValues(phraseLayers.values());
      
    p = configuration.containsKey("repetitionsLayer")?
      configuration.get("repetitionsLayer"):
      configuration.addParameter(
        new Parameter("repetitionsLayer", Layer.class, "Repetitions Layer",
                      "Layer for annotating repetitions"));
    String[] repetitionsLayerPossibilities = {
      "repetition", "repetitions", "repeat", "repeated"};
    p.setValue(Utility.FindLayerById(
                 phraseLayers, Arrays.asList(repetitionsLayerPossibilities)));
    p.setPossibleValues(phraseLayers.values());
      
    p = configuration.containsKey("rootLayer")?
      configuration.get("rootLayer"):
      configuration.addParameter(
        new Parameter("rootLayer", Layer.class, "Root Layer",
                      "Layer for tagging words with their root form"));
    String[] rootLayerPossibilities = {
      "root", "rootform", "lemma", "stem"};
    p.setValue(Utility.FindLayerById(
                 wordTagLayers, Arrays.asList(rootLayerPossibilities)));
    p.setPossibleValues(wordTagLayers.values());
      
    p = configuration.containsKey("errorLayer")?
      configuration.get("errorLayer"):
      configuration.addParameter(
        new Parameter("errorLayer", Layer.class, "Error Layer", "Layer for marking errors"));
    String[] errorLayerPossibilities = {"error", "errors", "code", "codes"};
    p.setValue(Utility.FindLayerById(
                 phraseLayers, Arrays.asList(errorLayerPossibilities)));
    p.setPossibleValues(phraseLayers.values());
      
    p = configuration.containsKey("soundEffectLayer")?
      configuration.get("soundEffectLayer"):
      configuration.addParameter(
        new Parameter("soundEffectLayer", Layer.class, "Sound Effects Layer",
                      "Layer for marking non-word verbal sound effects"));
    String[] soundEffectLayerPossibilities = {
      "soundeffect", "soundeffects", "sound", "nonword", "noise"};
    p.setValue(Utility.FindLayerById(
                 phraseLayers, Arrays.asList(soundEffectLayerPossibilities)));
    p.setPossibleValues(phraseLayers.values());
      
    p = configuration.containsKey("pauseLayer")?
      configuration.get("pauseLayer"):
      configuration.addParameter(
        new Parameter("pauseLayer", Layer.class, "Pause Layer",
                      "Layer for marking pauses in speech"));
    String[] pauseLayerPossibilities = { "pause", "pauses", "silence" };
    p.setValue(Utility.FindLayerById(
                 phraseLayers, Arrays.asList(pauseLayerPossibilities)));
    p.setPossibleValues(phraseLayers.values());
      
    p = configuration.containsKey("boundMorphemeLayer")?
      configuration.get("boundMorphemeLayer"):
      configuration.addParameter(
        new Parameter("boundMorphemeLayer", Layer.class, "Bound Morpheme Layer",
                      "Layer for marking bound morpheme annotations"));
    String[] boundMorphemeLayerPossibilities = {"boundmorpheme", "morpheme" };
    p.setValue(Utility.FindLayerById(
                 wordTagLayers, Arrays.asList(boundMorphemeLayerPossibilities)));
    p.setPossibleValues(wordTagLayers.values());
      
    p = configuration.containsKey("mazeLayer")?
      configuration.get("mazeLayer"):
      configuration.addParameter(
        new Parameter("mazeLayer", Layer.class, "Maze Layer",
                      "Layer for marking false starts, repetitions, and reformulations"));
    String[] mazeLayerPossibilities = { "maze", "mazes", };
    p.setValue(Utility.FindLayerById(
                 phraseLayers, Arrays.asList(mazeLayerPossibilities)));
    p.setPossibleValues(phraseLayers.values());
      
    p = configuration.containsKey("partialWordLayer")?
      configuration.get("partialWordLayer"):
      configuration.addParameter(
        new Parameter("partialWordLayer", Layer.class, "Partial Word Layer",
                      "Layer for marking stuttered or interrupted words"));
    String[] partialWordLayerPossibilities = {
      "partialword", "incomplete", "fragment", "wordfragment"};
    p.setValue(Utility.FindLayerById(
                 wordTagLayers, Arrays.asList(partialWordLayerPossibilities)));
    p.setPossibleValues(wordTagLayers.values());
      
    p = configuration.containsKey("omissionLayer")?
      configuration.get("omissionLayer"):
      configuration.addParameter(
        new Parameter("omissionLayer", Layer.class, "Omission Layer",
                      "Layer for marking missing words"));
    String[] omissionLayerPossibilities = { "omission", "omissions", "missing" };
    p.setValue(Utility.FindLayerById(
                 phraseLayers, Arrays.asList(omissionLayerPossibilities)));
    p.setPossibleValues(phraseLayers.values());
      
    p = configuration.containsKey("codeLayer")?
      configuration.get("codeLayer"):
      configuration.addParameter(
        new Parameter("codeLayer", Layer.class, "Code Layer", "Layer for non-error codes"));
    String[] codeLayerPossibilities = { "code", "codes" };
    p.setValue(Utility.FindLayerById(
                 phraseLayers, Arrays.asList(codeLayerPossibilities)));
    p.setPossibleValues(phraseLayers.values());
      
    p = configuration.containsKey("languageLayer")?
      configuration.get("languageLayer"):
      configuration.addParameter(
        new Parameter("languageLayer", Layer.class, "Language Attribute",
                      "Layer for recording the language of the speech"));
    String[] languageLayerPossibilities = { "transcript_language", "language", "lang" };
    p.setValue(Utility.FindLayerById(
                 transcriptTagLayers, Arrays.asList(languageLayerPossibilities)));
    p.setPossibleValues(transcriptTagLayers.values());
      
    p = configuration.containsKey("participantIdLayer")?
      configuration.get("participantIdLayer"):
      configuration.addParameter(
        new Parameter("participantIdLayer", Layer.class, "Participant ID Attribute",
                      "Layer for recording the target participant's ID"));
    String[] participantIdLayerPossibilities = { "participant_id", "id" };
    p.setValue(Utility.FindLayerById(
                 participantTagLayers, Arrays.asList(participantIdLayerPossibilities)));
    p.setPossibleValues(participantTagLayers.values());
      
    p = configuration.containsKey("genderLayer")?
      configuration.get("genderLayer"):
      configuration.addParameter(
        new Parameter("genderLayer", Layer.class, "Gender Attribute",
                      "Layer for recording the gender of the target participant"));
    String[] genderLayerPossibilities = {
      "participant_gender", "gender", "participant_sex", "sex"};
    p.setValue(Utility.FindLayerById(
                 participantTagLayers, Arrays.asList(genderLayerPossibilities)));
    p.setPossibleValues(participantTagLayers.values());
      
    p = configuration.containsKey("dobLayer")?
      configuration.get("dobLayer"):
      configuration.addParameter(
        new Parameter("dobLayer", Layer.class, "Date of Birth Attribute",
                      "Layer for recording the birth date of the target participant"));
    String[] dobLayerPossibilities = {
      "participant_dob", "participant_date_of_birth", "participant_birth_date",
      "dob", "date_of_birth", "birth_date"};
    p.setValue(Utility.FindLayerById(
                 participantTagLayers, Arrays.asList(dobLayerPossibilities)));
    p.setPossibleValues(participantTagLayers.values());
      
    p = configuration.containsKey("doeLayer")?
      configuration.get("doeLayer"):
      configuration.addParameter(
        new Parameter("doeLayer", Layer.class, "Date of Sample Attribute",
                      "Layer for recording the date the recording was elicited"));
    String[] doeLayerPossibilities = {
      "transcriptdoe", "transcriptrecordingdate", "transcriptairdate", "transcriptcreationdate",
      "doe", "recordingdate", "airdate", "creationdate" };
    p.setValue(Utility.FindLayerById(
                 transcriptTagLayers, Arrays.asList(doeLayerPossibilities)));
    p.setPossibleValues(transcriptTagLayers.values());
      
    p = configuration.containsKey("caLayer")?
      configuration.get("caLayer"):
      configuration.addParameter(
                new Parameter("caLayer", Layer.class, "Current Age Attribute",
                              "Layer for recording the target participant's age when recorded"));
    String[] caLayerPossibilities = {
      "participantca", "participantage", "participantcurrentage",
      "transcriptca", "transcriptage", "transcriptcurrentage",
      "ca", "age", "currentage"};
    p.setValue(Utility.FindLayerById(
                 attributeLayers, Arrays.asList(caLayerPossibilities)));
    p.setPossibleValues(attributeLayers.values());
      
    p = configuration.containsKey("ethnicityLayer")?
      configuration.get("ethnicityLayer"):
      configuration.addParameter(
        new Parameter("ethnicityLayer", Layer.class, "Ethnicity Attribute",
                      "Layer for recording the ethnicity of the target participant"));
    String[] ethnicityLayerPossibilities = { "participant_ethnicity", "ethnicity" };
    p.setValue(Utility.FindLayerById(
                 participantTagLayers, Arrays.asList(ethnicityLayerPossibilities)));
    p.setPossibleValues(participantTagLayers.values());
      
    p = configuration.containsKey("contextLayer")?
      configuration.get("contextLayer"):
      configuration.addParameter(
        new Parameter("contextLayer", Layer.class, "Context Attribute",
                      "Layer for recording the sampling context"));
    String[] contextLayerPossibilities = { "transcriptcontext", "context" };
    p.setValue(Utility.FindLayerById(
                 transcriptTagLayers, Arrays.asList(contextLayerPossibilities)));
    p.setPossibleValues(transcriptTagLayers.values());
      
    p = configuration.containsKey("subgroupLayer")?
      configuration.get("subgroupLayer"):
      configuration.addParameter(
        new Parameter("subgroupLayer", Layer.class, "Sub-Group Attribute",
                      "Layer for recording the sub-group/story"));
    String[] subgroupLayerPossibilities = {
      "transcriptsubgroup", "transcriptstory", "subgroup", "story"};
    p.setValue(Utility.FindLayerById(
                 transcriptTagLayers, Arrays.asList(subgroupLayerPossibilities)));
    p.setPossibleValues(transcriptTagLayers.values());
      
    p = configuration.containsKey("collectLayer")?
      configuration.get("collectLayer"):
      configuration.addParameter(
        new Parameter("collectLayer", Layer.class, "Collection Point Attribute",
                      "Layer for recording the collection point of the elicitation"));
    String[] collectLayerPossibilities = {
      "transcriptcollectionpoint", "transcriptcollect", "collectionpoint", "collect"};
    p.setValue(Utility.FindLayerById(
                 transcriptTagLayers, Arrays.asList(collectLayerPossibilities)));
    p.setPossibleValues(transcriptTagLayers.values());
      
    p = configuration.containsKey("locationLayer")?
      configuration.get("locationLayer"):
      configuration.addParameter(
        new Parameter("locationLayer", Layer.class, "Location Attribute",
                      "Layer for recording the location of the elicitation"));
    String[] locationLayerPossibilities = {"transcriptlocation", "location" };
    p.setValue(Utility.FindLayerById(
                 transcriptTagLayers, Arrays.asList(locationLayerPossibilities)));
    p.setPossibleValues(transcriptTagLayers.values());

    Parameter dateFormat = configuration.get("dateFormat");
    if (dateFormat == null) {
      dateFormat = new Parameter(
        "dateFormat", String.class, "Date format",
        "Format used in SALT files for dates (e.g. Dob, Doe) - either M/d/yyyy or d/M/yyyy."
        +" NB: the default date format is inferred from your locale settings");
      dateFormat.setPossibleValues(Arrays.asList("M/d/yyyy", "d/M/yyyy", "M/d/yy", "d/M/yy"));
      configuration.addParameter(dateFormat);
    }
    if (dateFormat.getValue() == null) {
      // detect the date format from the system...
      
      // default to US month-first format
      dateFormat.setValue("M/d/yyyy");

      // get a known date
      Calendar knownDate = Calendar.getInstance();
      knownDate.set(1972, 3, 4);
      if (DateFormat.getDateInstance(DateFormat.SHORT).format(knownDate.getTime())
          .startsWith("4")) {
        // day is first
        dateFormat.setValue("d/M/yyyy");
      }
    }

    Parameter parseInlineConventions = configuration.get("parseInlineConventions");
    if (parseInlineConventions == null) {
      parseInlineConventions = new Parameter(
        "parseInlineConventions", Boolean.class, "Parse Inline Conventions",
        "Whether to use SALT in-line conventions when deserializing."
        +" If false, then only meta-data headers, comment lines, and time stamps are parsed;"
        +" all in-line annotation conventions are left as-is");
      configuration.addParameter(parseInlineConventions);
    }
    if (parseInlineConventions.getValue() == null) {
      // default is to use conventions
      parseInlineConventions.setValue(Boolean.TRUE);
    }

    return configuration;
  }

  /**
   * Loads the serialized form of the graph, using the given set of named streams.
   * @param streams A list of named streams that contain all the transcription/annotation
   * data required. 
   * @param schema The layer schema, definining layers and the way they interrelate.
   * @return A list of parameters that require setting before 
   * {@link GraphDeserializer#deserialize()} can be invoked. This may be an empty list, and may
   * include parameters with the value already set to a workable default. If there are
   * parameters, and user interaction is possible, then the user may be presented with an
   * interface for setting/confirming these parameters, before they are then passed to
   * {@link GraphDeserializer#setParameters(ParameterSet)}. 
   * @throws SerializationException If the graph could not be loaded.
   * @throws IOException On IO error.
   * @throws SerializerNotConfiguredException If the configuration is not sufficient for
   * deserialization. 
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public ParameterSet load(NamedStream[] streams, Schema schema)
    throws IOException, SerializationException, SerializerNotConfiguredException {
    // take the first slt stream, ignore all others.
    NamedStream slt = null;
    for (NamedStream stream : streams) {	 
      if (stream.getName().toLowerCase().endsWith(".slt") 
          || "text/x-salt".equals(stream.getMimeType())
          || "text/x-slt".equals(stream.getMimeType())) {
        slt = stream;
        break;
      }
    } // next stream
    if (slt == null) throw new SerializationException("No SALT stream found");
    setName(slt.getName());

    reset();

    // parameters allow codes that are found in the stream to be mapped to phrase/word layers
    // (e.g. lexical/pronounce annotations)
    // and comments of particular type to span/phrase layers (e.g. {NOISE:...} comments)
    parameters = new ParameterSet();
    LinkedHashMap<String,Layer> phraseWordLayers = new LinkedHashMap<String,Layer>();
    LinkedHashMap<String,Layer> spanPhraseLayers = new LinkedHashMap<String,Layer>();
    for (Layer layer : schema.getLayers().values()) {
      if ((layer.getParentId() == null || layer.getParentId().equals(schema.getRoot().getId()))
          && (layer.getAlignment() == Constants.ALIGNMENT_INTERVAL
              || layer.getAlignment() == Constants.ALIGNMENT_INSTANT)) { // span layer
        spanPhraseLayers.put(layer.getId(), layer);
      } else if (layer.getParentId().equals(schema.getTurnLayerId())
                 && !layer.getId().equals(schema.getWordLayerId())
                 && !layer.getId().equals(schema.getUtteranceLayerId())
                 && layer.getAlignment() == Constants.ALIGNMENT_INTERVAL) { // phrase layer
        phraseWordLayers.put(layer.getId(), layer);
        spanPhraseLayers.put(layer.getId(), layer);
      } else if (schema.getWordLayerId().equals(layer.getParentId())
                 && layer.getAlignment() == Constants.ALIGNMENT_NONE) { // word tag layer
        phraseWordLayers.put(layer.getId(), layer);
      }
    } // next layer
    Pattern codePattern = Pattern.compile("\\[([^\\]]+)\\]");
    Pattern inlineCommentPattern = Pattern.compile("\\{([^\\}]+):[^\\}]+\\}");
    Pattern lineCommentPattern = Pattern.compile("^\\+ ([^:]+):.+$");
    Pattern utterancePattern = Pattern.compile("^[a-zA-Z] .*");
    Pattern simultaneousSpeechPattern = Pattern.compile("^[a-zA-Z] <.*>$");
    
    // read stream line by line
    boolean inHeader = true;
    BufferedReader reader = new BufferedReader(new InputStreamReader(slt.getStream(), "UTF-8"));
    String line = reader.readLine();
    boolean utteranceHasStartTime = false;
    // remove byte-order-mark if any
    if (line != null) line = line.replace("\uFEFF", "");
    while (line != null) {
      // skip blank lines
      if (line.trim().length() > 0) {
      
        if (!line.startsWith("$") && !line.startsWith("+")) {
          inHeader = false;
        }
        if (inHeader) {
          if (line.startsWith("$")) { // participants line
            participantsHeader = line.replaceFirst("^\\$","").trim();
          } else {
            headers.add(line.replaceFirst("^\\+","").trim());
          }
        } else { // transcript line
          lines.add(line);
          
          try { // try to parse it at a time-stamp
            timesStampFormat.parse(line);
            utteranceHasStartTime = true;
          } catch(ParseException exception) { // not a timestamp
            // is it an utterance?
            if (utterancePattern.matcher(line).matches()) { // utterance
              if (!utteranceHasStartTime // no recent start time
                  && !simultaneousSpeechPattern.matcher(line).matches()) { // not simultanous
                warnings.add("Utterance with unknown start time: " + line);
              }
              utteranceHasStartTime = false;
            }
          }

          if (parseInlineConventions) {      
            // any codes?
            Matcher codeMatcher = codePattern.matcher(line);
            while (codeMatcher.find()) {
              int colon = codeMatcher.group(1).indexOf(':');
              final String code = (colon > 0)?codeMatcher.group(1).substring(0, colon)
                :codeMatcher.group(1);
              // do we already have a parameter for this code?
              if (!parameters.containsKey("code_" + code)) { // no parameter yet
                Parameter p = new Parameter(
                  "code_" + code, Layer.class, "Codes: " + code, "Layer for '"+code+"' codes");
                p.setValue(Utility.FindLayerById(phraseWordLayers, new Vector(){{add(code);}}));
                if (p.getValue() == null) {
                  // error codes start with "E" by convention
                  if (code.startsWith("E") && errorLayer != null) {
                    p.setValue(errorLayer);
                  } else {
                    p.setValue(codeLayer);
                  }
                }
                p.setPossibleValues(phraseWordLayers.values());
                parameters.addParameter(p);
              }
            } // next code
            // any comments of the form {key:value}?
            Matcher commentMatcher = inlineCommentPattern.matcher(line);
            while (commentMatcher.find()) {
              final String key = commentMatcher.group(1);
              // do we already have a parameter for this key?
              if (!parameters.containsKey("comment_" + key)) { // no parameter yet
                Parameter p = new Parameter(
                  "comment_" + key, Layer.class, "Comments: " + key,
                  "Layer for '"+key+"' comments");
                p.setValue(Utility.FindLayerById(spanPhraseLayers, new Vector(){{add(key);}}));
                if (p.getValue() == null) {
                  p.setValue(commentLayer);
                }
                p.setPossibleValues(spanPhraseLayers.values());
                parameters.addParameter(p);
              }
            } // next comment
            
            // any whole-line comments of the form + key:value?
            commentMatcher = lineCommentPattern.matcher(line);
            while (commentMatcher.find()) {
              final String key = commentMatcher.group(1);
              // do we already have a parameter for this key?
              if (!parameters.containsKey("comment_" + key)) { // no parameter yet
                Parameter p = new Parameter(
                  "comment_" + key, Layer.class, "Comments: " + key,
                  "Layer for '"+key+"' comments");
                p.setValue(Utility.FindLayerById(spanPhraseLayers, new Vector(){{add(key);}}));
                if (p.getValue() == null) {
                  p.setValue(commentLayer);
                }
                p.setPossibleValues(spanPhraseLayers.values());
                parameters.addParameter(p);
              }
            } // next comment
          } // parseInlineConventions
        } // transcript line
      } // not a blank line
      line = reader.readLine();
    } // next line
    if (!utteranceHasStartTime) { // no final time stamp
      warnings.add("End time of transcript is unknown.");
    }

    // validation
    if (participantsHeader.trim().length() == 0) {
      throw new SerializationException(
        SerializationException.ErrorType.InvalidDocument,
        "No participant list was defined in the transcript header");
    }

    // media duration
    NamedStream wav = Utility.FindSingleStream(streams, ".wav", "audio/wav");
    if (wav != null) {
      // save the media file
      File fMedia = File.createTempFile("SltSerialization-", wav.getName());
      fMedia.deleteOnExit();
      try {
        
        // we cannot just use the stream directly, because AudioSystem.getAudioInputStream()
        // requires a mark/reset-able stream, which we can't guarantee that we have
        // so we save the stream to a file, and give AudioSystem.getAudioInputStream() that file
        IO.SaveInputStreamToFile​(wav.getStream(), fMedia);
        
        // determine the duration of the media file
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(fMedia);
        AudioFormat format = audioInputStream.getFormat();
        long frames = audioInputStream.getFrameLength();
        if (frames > 0) {
          double durationInSeconds = ((double)frames) / format.getFrameRate(); 
          mediaDurationSeconds = durationInSeconds;
        } else {
          warnings.add("Ignoring media: " + wav.getName() + " is valid but contains no frames.");
        }
      } catch(Exception exception) {
        warnings.add("Ignoring media: " + wav.getName() + " ERROR: " + exception);
      } finally {
        fMedia.delete();
      }
    }
    return parameters;
  }

  ParameterSet parameters = null;
  /**
   * Sets parameters for a given deserialization operation, after loading the serialized
   * form of the graph. This might include mappings from format-specific objects like
   * tiers to graph layers, etc. 
   * @param parameters The configuration for a given deserialization operation.
   * @throws SerializationParametersMissingException If not all required parameters have a value.
   */
  public void setParameters(ParameterSet parameters)
    throws SerializationParametersMissingException {
    this.parameters = parameters;
  }
  
  /**
   * Deserializes the serialized data, generating one or more {@link Graph}s.
   * <p>Many data formats will only yield one graph (e.g. Transcriber
   * transcript or Praat textgrid), however there are formats that
   * are capable of storing multiple transcripts in the same file
   * (e.g. AGTK, Transana XML export), which is why this method
   * returns a list.
   * @return A list of valid (if incomplete) {@link Graph}s. 
   * @throws SerializerNotConfiguredException if the object has not been configured.
   * @throws SerializationParametersMissingException if the parameters for this particular
   * graph have not been set. 
   * @throws SerializationException if errors occur during deserialization.
   */
  public Graph[] deserialize() 
    throws SerializerNotConfiguredException, SerializationParametersMissingException,
    SerializationException {
    // if there are errors, accumlate as many as we can before throwing SerializationException
    SerializationException errors = null;

    Layer participantLayer = schema.getParticipantLayer();
    Layer turnLayer = schema.getTurnLayer();
    Layer utteranceLayer = schema.getUtteranceLayer();
    Layer wordLayer = schema.getWordLayer();
    
    Graph graph = new Graph();
    graph.setId(getName());
    // add layers to the graph
    // we don't just copy the whole schema, because that would imply that all the extra layers
    // contained no annotations, which is not necessarily true
    graph.addLayer((Layer)participantLayer.clone());
    graph.getSchema().setParticipantLayerId(participantLayer.getId());
    graph.addLayer((Layer)turnLayer.clone());
    graph.getSchema().setTurnLayerId(turnLayer.getId());
    graph.addLayer((Layer)utteranceLayer.clone());
    graph.getSchema().setUtteranceLayerId(utteranceLayer.getId());
    graph.addLayer((Layer)wordLayer.clone());
    graph.getSchema().setWordLayerId(schema.getWordLayerId());
    
    if (targetParticipantLayer != null) graph.addLayer((Layer)targetParticipantLayer.clone());
    if (commentLayer != null) graph.addLayer((Layer)commentLayer.clone());
    if (cUnitLayer != null) graph.addLayer((Layer)cUnitLayer.clone());
    if (parentheticalLayer != null) graph.addLayer((Layer)parentheticalLayer.clone());
    if (properNameLayer != null) graph.addLayer((Layer)properNameLayer.clone());
    if (repetitionsLayer != null) graph.addLayer((Layer)repetitionsLayer.clone());
    if (rootLayer != null) graph.addLayer((Layer)rootLayer.clone());
    if (errorLayer != null) graph.addLayer((Layer)errorLayer.clone());
    if (soundEffectLayer != null) graph.addLayer((Layer)soundEffectLayer.clone());
    if (pauseLayer != null) graph.addLayer((Layer)pauseLayer.clone());
    if (boundMorphemeLayer != null) graph.addLayer((Layer)boundMorphemeLayer.clone());
    if (mazeLayer != null) graph.addLayer((Layer)mazeLayer.clone());
    if (partialWordLayer != null) graph.addLayer((Layer)partialWordLayer.clone());
    if (omissionLayer != null) graph.addLayer((Layer)omissionLayer.clone());
    if (codeLayer != null) graph.addLayer((Layer)codeLayer.clone());
    if (languageLayer != null) graph.addLayer((Layer)languageLayer.clone());
    if (participantIdLayer != null) graph.addLayer((Layer)participantIdLayer.clone());
    if (genderLayer != null) graph.addLayer((Layer)genderLayer.clone());
    if (dobLayer != null) graph.addLayer((Layer)dobLayer.clone());
    if (doeLayer != null) graph.addLayer((Layer)doeLayer.clone());
    if (caLayer != null) graph.addLayer((Layer)caLayer.clone());
    if (ethnicityLayer != null) graph.addLayer((Layer)ethnicityLayer.clone());
    if (contextLayer != null) graph.addLayer((Layer)contextLayer.clone());
    if (subgroupLayer != null) graph.addLayer((Layer)subgroupLayer.clone());
    if (collectLayer != null) graph.addLayer((Layer)collectLayer.clone());
    if (locationLayer != null) graph.addLayer((Layer)locationLayer.clone());
    for (Parameter p : parameters.values()) {
      Layer l = (Layer)p.getValue();
      if (l != null && graph.getLayer(l.getId()) == null) {          
        graph.addLayer((Layer)l.clone());
      }
    } // next parameter

    Anchor start = graph.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE_MANUAL);
    
    // participants:
    // the header is something like "Child, Examiner"
    HashMap<String,Annotation> idToParticipant = new HashMap<String,Annotation>();
    Annotation targetParticipant = null;
    for (String p : participantsHeader.split(",")) {
      p = p.trim();
      String id = p.substring(0,1); // Child -> C, Examiner -> E, etc.
      String name = p;
      Annotation participant = graph.createTag(graph, participantLayer.getId(), name);
      idToParticipant.put(id, participant);
      if (targetParticipant == null) { // first participant is target
        targetParticipant = participant;
        if (targetParticipantLayer != null) {
          graph.createTag(participant, targetParticipantLayer.getId(), name);
        }
      }
    } // next participant
    
    // headers
    SimpleDateFormat saltDateFormat = new SimpleDateFormat(dateFormat);
    SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    for (String header : headers) {
      int colon = header.indexOf(':');
      if (colon > 0) {
        String key = header.substring(0, colon).trim().toLowerCase();
        String value = header.substring(colon + 1).trim();
        if (value.length() > 0) {
          if (key.equals("name")) {
            // a Name header value is used instead of the file name for the participant prefix
            setName(value);
          } else if (key.equals("language") && languageLayer != null) {
            graph.createTag(graph, languageLayer.getId(),
                            // ISO639 alpha-2 code if possible
                            iso639.alpha2(value).orElse(value));
          } else if (key.equals("participantid") && participantIdLayer != null) {
            graph.createTag(targetParticipant, participantIdLayer.getId(), value);
          } else if (key.equals("gender") && genderLayer != null) {
            graph.createTag(targetParticipant, genderLayer.getId(), value);
          } else if (key.equals("dob") && dobLayer != null) {
            try { // parse as configured format
              Date date = saltDateFormat.parse(value);
              // but save with ISO format
              graph.createTag(targetParticipant, dobLayer.getId(), isoDateFormat.format(date));
            } catch(ParseException exception) {
              warnings.add("Could not parse dob \""+value+"\": " + exception.getMessage());
              graph.createTag(targetParticipant, dobLayer.getId(), value);
            }
          } else if (key.equals("doe") && doeLayer != null) {
            try { // parse as configured format
              Date date = saltDateFormat.parse(value);
              // but save with ISO format
              graph.createTag(targetParticipant, doeLayer.getId(), isoDateFormat.format(date));
            } catch(ParseException exception) {
              warnings.add("Could not parse dob \""+value+"\": " + exception.getMessage());
              graph.createTag(targetParticipant, doeLayer.getId(), value);
            }
          } else if (key.equals("ca") && caLayer != null) {
            if (participantLayer.getId().equals(caLayer.getParentId())) {
              // participant attribute
              graph.createTag(targetParticipant, caLayer.getId(), value); // TODO validate? format?
            } else {
              // transcript attribute
              graph.createTag(graph, caLayer.getId(), value); // TODO validate? format?
            }
          } else if (key.equals("ethnicity") && ethnicityLayer != null) {
            graph.createTag(targetParticipant, ethnicityLayer.getId(), value);
          } else if (key.equals("context") && contextLayer != null) {
            graph.createTag(graph, contextLayer.getId(), value);
          } else if (key.equals("subgroup") && subgroupLayer != null) {
            graph.createTag(graph, subgroupLayer.getId(), value);
          } else if (key.equals("collect") && collectLayer != null) {
            graph.createTag(graph, collectLayer.getId(), value);
          } else if (key.equals("location") && locationLayer != null) {
            graph.createTag(graph, locationLayer.getId(), value);
          }
        } // there's a value specified
      } // there's a colon separator
    } // next header
    
    // In SALT files, participants are named a general code like 'Child', 'Parent' etc.
    // to uniquify these for insertion into large corpora, we prepent this with the name
    // of the file
    String participantPrefix = IO.WithoutExtension(getName()) + "-";
    for (Annotation participant : idToParticipant.values()) {
      participant.setLabel(participantPrefix + participant.getLabel());
    }

    // ensure we have an utterance tokenizer
    if (getTokenizer() == null) {
      setTokenizer(new SimpleTokenizer(utteranceLayer.getId(), wordLayer.getId()));
    }

    // utterances
    Annotation currentTurn = new Annotation(null, "", turnLayer.getId());
    Annotation lastUtterance = null;
    Annotation cUnit = null;
    Anchor lastAnchor = start;
    Anchor lastAlignedAnchor = start;
    String prefixNextUtterance = "";
    boolean misalignedUtterancesExist = false;
    int consecutiveTimeStamps = 0;
    for (String line : lines) {
      // skip blank lines
      if (line.trim().length() == 0) continue;

      // is it a time stamp?
      Object[] timestampParts = null;
      try { // try to parse it at a time-stamp
        timestampParts = timesStampFormat.parse(line);
      } catch(ParseException exception) {}      
      if (timestampParts != null) { // time stamp
        
        long minutes = (Long)timestampParts[0];        
        double seconds = 0;
        if (timestampParts[1] instanceof Long) {
          seconds = (Long)timestampParts[1];
        } else {
          seconds = (Double)timestampParts[1];
        }
        double offset = minutes * 60 + seconds;
        int confidence = Constants.CONFIDENCE_MANUAL;
        if (offset != 0.0
            && lastAlignedAnchor != null && offset <= lastAlignedAnchor.getOffset()) {
          warnings.add("Time stamp \"" + line + "\" isn't after the previous one ("
                       +lastAlignedAnchor+") bumping this offset forward");
          // the timestamp isn't after the last one, so we bump it forward 1s
          // offset += 1.0;
          offset = lastAlignedAnchor.getOffset() + 1.0;
          // and downgrade its confidence
          confidence = Constants.CONFIDENCE_DEFAULT;
          misalignedUtterancesExist = true;
        }
        
        if (lastAnchor.getOffset() == null) {
          lastAnchor.setOffset(offset);
          lastAnchor.setConfidence(confidence);
        } else {
          lastAnchor = graph.getOrCreateAnchorAt(offset, confidence);
        }
        lastAlignedAnchor = lastAnchor;
        consecutiveTimeStamps++;
        
      } else if (line.startsWith("+") || line.startsWith("=")) { // comment line
        
        String label = line.substring(1).trim();
        String layerId = commentLayer == null?null:commentLayer.getId();
        
        // look for matching comments-mapped-to-layer parameter
        for (Parameter p : parameters.values()) {
          if (p.getName().startsWith("comment_")) {
            Layer layer = (Layer)p.getValue();
            String key = p.getName().substring("comment_".length());
            if (label.startsWith(key+":")) {
              if (layer != null) {
                layerId = layer.getId();
              } else {
                layerId = null;
              }
              if (layer != null
                  && (commentLayer == null || !layer.getId().equals(commentLayer.getId()))) {
                // layer isn't the comment layer, so strip the "${key}:" prefix off the label
                label = label.substring((key+":").length());
              } // layer isn't the comment layer
              break; // found it, so we can stop looking
            } // parameter matches
          } // comment parameter
        } // next parameter

        if (layerId != null && label.length() > 0) {            
          graph.addAnnotation(
            new Annotation()
            .setLayerId(layerId)
            .setLabel(label)
            .setStartId(lastAnchor.getId())
            .setEndId(lastAnchor.getId()));
        }
        
      } else if (line. startsWith(";") || line. startsWith(":")) { // pause line
        
        if (pauseLayer != null) {
          String pauseLabel = line.substring(1).trim();
          if (lastAnchor == lastAlignedAnchor) { // pause line is after a time stamp
            // prefix it to the next line
            prefixNextUtterance += pauseLabel + " ";
          } else { // append it to the last line
            LinkedHashSet<Annotation> endingUtterances = lastAnchor.endOf(utteranceLayer.getId());
            if (endingUtterances.size() == 0) { // no prior utterance
              // prefix it to the next line
              prefixNextUtterance += pauseLabel + " ";
            } else {
              Annotation previousUtterance = endingUtterances.iterator().next();
              previousUtterance.setLabel(
                previousUtterance.getLabel()
                // insert before utterance code if any
                .replaceFirst(
                  "(?<utterance>.*)(?<terminator>[.?!~^>]?)(?<code> \\[[\\w0-9:]+\\])$",
                  "${utterance} " + pauseLabel + "${terminator}${code}"));
            }
          }
        }
        
      } else { // utterance

        String p = line.substring(0,1);
        Annotation participant = idToParticipant.get(p);
        if (participant == null) {
          warnings.add("Unknown speaker: \"" + p + "\" - ignoring line: " + line);
          continue;
        }

        String label = line.substring(1).trim();
        boolean simultaneousSpeech = lastUtterance != null
          && lastUtterance.getLabel().startsWith("<") && lastUtterance.getLabel().endsWith(">")
          && label.startsWith("<") && label.endsWith(">");
        
        Anchor utteranceStart = simultaneousSpeech?
          lastUtterance.getStart() : lastAnchor;
        Anchor utteranceEnd = simultaneousSpeech?
          lastUtterance.getEnd() : graph.addAnchor(new Anchor());
        // new speaker?
        if (!participant.getLabel().equals(currentTurn.getLabel())
            || consecutiveTimeStamps > 1) { // consecutive timestamp trigger new turn
          currentTurn = new Annotation()
            .setLayerId(turnLayer.getId())
            .setLabel(participant.getLabel())
            .setStartId(utteranceStart.getId())
            .setEndId(utteranceEnd.getId())
            .setParentId(participant.getId());
          graph.addAnnotation(currentTurn);
        } // new turn

        if (prefixNextUtterance.length() > 0) {
          label = prefixNextUtterance + label;
          prefixNextUtterance = "";
        }

        Annotation utterance = new Annotation()
          .setLayerId(utteranceLayer.getId())
          .setLabel(label)
          .setStartId(utteranceStart.getId())
          .setEndId(utteranceEnd.getId())
          .setParentId(currentTurn.getId());
        graph.addAnnotation(utterance);
        currentTurn.setEndId(utteranceEnd.getId());
        lastAnchor = utteranceEnd;
        lastUtterance = utterance;

        // reset count of consecutive timestamp lines
        consecutiveTimeStamps = 0;
      } // utterance
    } // next line

    // sometimes there's no ending timestamp!
    if (lastAnchor.getOffset() == null) {
      if (mediaDurationSeconds != null) {
        // we know how long the media is, so use that as the end time
        lastAnchor.setOffset(mediaDurationSeconds);
        lastAnchor.setConfidence(Constants.CONFIDENCE_MANUAL);
      } else { // end time unknown
        // so we add 1s to the last known timestamp and mark it with low confidence
        lastAnchor.setOffset(lastAlignedAnchor.getOffset() + 1.0);
        lastAnchor.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
      }
    }
    graph.setEnd(lastAnchor);

    if (misalignedUtterancesExist) {
      try {
        // we've bumped the alignments of some utterances because they didn't go forward in
        // time, so lets spread them evenly out
        new DefaultOffsetGenerator().transform(graph).commit();      
      } catch(TransformationException exception) {
        warnings.add("Could not evenly spread misaligned lines: \"" + exception);
      }
    }

    // at this point, each utterance is labelled with the line transcript, and there are
    // no word tokens yet
    
    graph.trackChanges();
    if (parseInlineConventions) {      
      try {
        
        // utterance codes
        for (Parameter p : parameters.values()) {
          if (p.getName().startsWith("code_")) {
            Layer layer = (Layer)p.getValue();
            String code = p.getName().substring("code_".length());
            ConventionTransformer transformer = new ConventionTransformer(
              utteranceLayer.getId(),
              "(?<line>.*) \\[(?<code>"+code+"(?::[^\\]]*)?)\\](?<terminator>[.?!~^>]?)$",
              "${line}${terminator}");
            if (layer != null) {
              transformer.addDestinationResult(layer.getId(), "${code}");
            }
            transformer.transform(graph).commit();
            if (layer != null
                && (codeLayer == null || !layer.getId().equals(codeLayer.getId()))
                && (errorLayer == null || !layer.getId().equals(errorLayer.getId()))) {
              // layer isn't the code/error layer, so strip the "${key}:" prefix off the labels
              new ConventionTransformer(layer.getId(), code+":(?<value>.+)", "${value}")
                .transform(graph).commit();
            } // layer isn't the comment layer
          }
        } // next parameter        
      } catch(TransformationException exception) {
        if (errors == null) errors = new SerializationException();
        if (errors.getCause() == null) errors.initCause(exception);
        errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
      }
      
    } // parseInlineConventions
        
    try { // tokenize utterances into words
      getTokenizer().transform(graph);
      graph.commit();
    } catch(TransformationException exception) {
      if (errors == null) errors = new SerializationException();
      if (errors.getCause() == null) errors.initCause(exception);
      errors.addError(SerializationException.ErrorType.Tokenization, exception.getMessage());
    }
    
    if (parseInlineConventions) {      
      try { // parse out in-situ word/phrase annotations
        
        // sound effects - something like "%yip_yip"
        ConventionTransformer transformer = new ConventionTransformer(
          wordLayer.getId(), "%(?<sound>.+)");
        if (soundEffectLayer != null) {
          transformer.addDestinationResult(soundEffectLayer.getId(), "${sound}");
        }
        transformer.transform(graph).commit();
        
        // repetitions - something like "heaps_and_heaps|heaps"
        // in order to not confuse these with proper names and with root forms, we annotate
        // them beforehand in three phases:
        // 1. identify the w_x[_y]...|z pattern, and create repetition annotions, marking the
        // words with w`_x[_y] along the way
        // 2. convert the w`_x[_y] words to w`x`y
        // 3. split `-containing words into multiple tokens
        
        // phase 1: split off annotation 'root'
        transformer = new ConventionTransformer(
          wordLayer.getId(),
          "(?<first>[a-zA-Z0-9]+)(?<subsequent>_[^|]+)\\|(?<word>[a-zA-Z0-9]+)(?<punctuation>\\W*)",
          "${first}`${subsequent}${punctuation}"
          );
        if (repetitionsLayer != null) {
          transformer.addDestinationResult(repetitionsLayer.getId(), "$3");
        }
        transformer.transform(graph).commit();
        
        // phase 2: delimited repetitions with `
        for (Annotation word : graph.all(wordLayer.getId())) {
          if (word.getLabel().contains("`_")) { // a label created in phase 1
            // convert _ into `
            word.setLabel(word.getLabel().replaceAll("`","").replaceAll("_","`"));
          } // a label created in phase 1
        } // next word
        
        // phase 3: split words on `
        SimpleTokenizer repetitionSplitter = new SimpleTokenizer(
          wordLayer.getId(), null, "`", true);
        repetitionSplitter.transform(graph).commit();
        
        // proper names - something like "Schnitzel_von_Crumm"
        SimpleTokenizer linkageSplitter = new SimpleTokenizer(
          wordLayer.getId(), properNameLayer != null?properNameLayer.getId():null, "_", true);
        linkageSplitter.transform(graph).commit();

        // inline (word) codes
        int numPasses = 2; // how many stacked word tags are possible - e.g. "s[PRON:s][LEX:is]*"
        for (int pass = 0; pass < numPasses; pass++) {
          for (Parameter p : parameters.values()) {
            if (p.getName().startsWith("code_")) {
              Layer layer = (Layer)p.getValue();
              String code = p.getName().substring("code_".length());
              transformer = new ConventionTransformer(
                wordLayer.getId(),
                "(?<word>.+)\\[(?<code>"+code+"(?::[^\\]]*)?)\\](?<punctuation>\\W*)",
                "${word}${punctuation}");
              if (layer != null) {
                transformer.addDestinationResult(layer.getId(), "${code}");
              }
              transformer.transform(graph).commit();
              if (layer != null
                  && (codeLayer == null || !layer.getId().equals(codeLayer.getId()))
                  && (errorLayer == null || !layer.getId().equals(errorLayer.getId()))) {
                // layer isn't the code/error layer, so strip the "${key}:" prefix off the labels
                new ConventionTransformer(layer.getId(), code+":(?<value>.+)", "${value}")
                  .transform(graph).commit();
              } // layer isn't the code/error layer
            } // is a mapped code
          } // next parameter
        } // next pass
        // root forms - something like "falled|fall"
        transformer = new ConventionTransformer(
          wordLayer.getId(),
          "(?<word>[^|]+)\\|(?<root>\\w+)(?<punctuation>\\W*)",
          "${word}${punctuation}");
        if (rootLayer != null) {
          transformer.addDestinationResult(rootLayer.getId(), "${root}");
        }
        transformer.transform(graph).commit();
        
        // omissions - something like "We *went home"
        transformer = new ConventionTransformer(wordLayer.getId(), "\\*(?<omission>.+)");
        if (omissionLayer != null) {
          transformer.addDestinationResult(omissionLayer.getId(), "${omission}");
        }
        transformer.transform(graph).commit();        

        // bound morphemes  - something like "bird/s/z" ...

        // just stripping out the slashes doesn't produce well-formed words
        // so we apply a series of heuristic transformations based on English norms and exceptions
        String[] boundMorphemeTransformations = {
          // some exceptions up front before other processing catches them...
          
          // the error go/s (which should be go/3s) -> "goes"
          "go/s(?<punc>\\W*)", "goes${punc}", "go/3s",

          // the error say/s (which should be say/3s) -> "says" (not "saies")
          "say/s(?<punc>\\W*)", "says${punc}", "say/3s",

          // NOUN INFLECTIONS:
          
          // /s/z - Plural and Possessive. Example: baby/s/z ...        
          // first make ...y/s/z -> ...ies' - e.g. "babies'"
          "(?<w>[^/]+)y/s/z(?<punc>\\W*)", "${w}ies'${punc}", "${w}y/s/z",
          // and make ...s/s/z -> ...ses' - e.g. "buses'"
          "(?<w>[^/]+)s/s/z(?<punc>\\W*)", "${w}ses'${punc}", "${w}s/s/z",
          // and make ...sh/s/z -> ...shes' - e.g. "bushes'"
          "(?<w>[^/]+)sh/s/z(?<punc>\\W*)", "${w}shes'${punc}", "${w}sh/s/z",
          // now make .../s/z -> ...s'
          "(?<w>[^/]+)/s/z(?<punc>\\W*)", "${w}s'${punc}", "${w}/s/z",
          
          // /p - Extension: Plural Possessive. Example: "the two girl/p bicycle/s."...
          // first make ...y/p -> ...ies' - e.g. "babies'"
          "(?<w>[^/]+)y/p(?<punc>\\W*)", "${w}ies'${punc}", "${w}y/p",
          // now make .../p -> ...s'
          "(?<w>[^/]+)/p(?<punc>\\W*)", "${w}s'${punc}", "${w}/p",
          
          // /s - Plural noun. Examples: doggie/s, baby/s...
          // first make ...y/s -> ...ies - e.g. "babies" but not "guys"
          "(?<w>[^/]+[^u])y/s(?<punc>\\W*)", "${w}ies${punc}", "${w}y/s",
          // and make ...s/s -> ...ses - e.g. "buses"
          "(?<w>[^/]+)s/s(?<punc>\\W*)", "${w}ses${punc}", "${w}s/s",
          // and make ...sh/s -> ...shes - e.g. "bushes"
          "(?<w>[^/]+)sh/s(?<punc>\\W*)", "${w}shes${punc}", "${w}sh/s",
          // make .../s -> ...s
          "(?<w>[^/]+)/s(?<punc>\\W*)", "${w}s${punc}", "${w}/s",
          
          // /z - Possessive inflection. Examples: dad/z, Mary/z
          // first make it/z -> its
          "(?<w>[iI]t)/z(?<punc>\\W*)", "${w}s${punc}", "${w}/z",
          // make .../z -> ...'s
          // and also the error .../'z -> ...'s
          "(?<w>[^/]+)/'?z(?<punc>\\W*)", "${w}'s${punc}", "${w}/z",
          
          // VERB INFLECTIONS:
          
          // /3s - 3 rd Person Singular verb form. Examples: go/3s, tell/3s, try/3s
          // first make ...ay/3s -> ...ays - e.g. "says"
          "(?<w>[^/]+)ay/3s(?<punc>\\W*)", "${w}ays${punc}", "${w}ay/3s",
          // but make ...y/3s -> ...ies - e.g. "tries"
          "(?<w>[^/]+)y/3s(?<punc>\\W*)", "${w}ies${punc}", "${w}y/3s",
          // and make ...o/3s -> ...oes - e.g. "goes"
          "(?<w>[^/]+)o/3s(?<punc>\\W*)", "${w}oes${punc}", "${w}o/3s",
          // make .../3s -> ...s
          "(?<w>[^/]+)/3s(?<punc>\\W*)", "${w}s${punc}", "${w}/3s",
          
          // /ed - Past tense. Examples: love/ed, die/ed
          // first make ...e/ed -> ...ed - e.g. "loved"
          "(?<w>[^/]+)e/ed(?<punc>\\W*)", "${w}ed${punc}", "${w}e/ed",
          // and make ...y/ed -> ...ied - e.g. "carried"
          "(?<w>[^/]+)y/ed(?<punc>\\W*)", "${w}ied${punc}", "${w}y/ed",
          // and make ...[vowel]b/ed -> ...bbed - e.g. "grabbed"
          // and make ...[vowel]p/ed -> ...pped - e.g. "slapped" (but not "helped")
          // and make ...[vowel]g/ed -> ...gged - e.g. "hugged"
          "(?<w>[^/]+[aeiou])(?<c>[bgp])/ed(?<punc>\\W*)", "${w}${c}${c}ed${punc}", "${w}${c}/ed",
          // make .../ed -> ...ed
          "(?<w>[^/]+)/ed(?<punc>\\W*)", "${w}ed${punc}", "${w}/ed",
          
          // /ed2 - Extension: Past participle. Examples: "I had climb/ed2 to the top."
          // first make ...e/ed2 -> ...ed - e.g. "had loved"
          "(?<w>[^/]+)e/ed2(?<punc>\\W*)", "${w}ed${punc}", "${w}e/ed2",
          // and make ...[vowel]p/ed2 -> ...pped - e.g. "had dropped" but "had helped"
          "(?<w>[^/]+[aeiou])p/ed2(?<punc>\\W*)", "${w}pped${punc}", "${w}p/ed2",
          // make .../ed2 -> ...ed
          "(?<w>[^/]+)/ed2(?<punc>\\W*)", "${w}ed${punc}", "${w}/ed2",
          
          // /en - Past participle. Examples: take/en, eat/en, prove/en
          // first make ...e/en -> ...en - e.g. "taken"
          "(?<w>[^/]+)e/en(?<punc>\\W*)", "${w}en${punc}", "${w}e/en",
          // make .../en -> ...en
          "(?<w>[^/]+)/en(?<punc>\\W*)", "${w}en${punc}", "${w}/en",
          
          // /ing - Progressive verb form. Examples: go/ing, run/ing, bike/ing
          // first make ...e/ing -> ...ing - e.g. "biking" (but "seeing")
          "(?<w>[^/]+[^e])e/ing(?<punc>\\W*)", "${w}ing${punc}", "${w}e/ing",
          // and make ...ain/ing -> ...aining - e.g. "raining"
          // and make ...ait/ing -> ...aiting - e.g. "waiting"
          "(?<w>[^/]+)ai(?<c>[nt])/ing(?<punc>\\W*)", "${w}ai${c}ing${punc}", "${w}ai${c}/ing",
          // and make ...en/ing -> ...ening - e.g. "happening"
          "(?<w>[^/]+en)/ing(?<punc>\\W*)", "${w}ing${punc}", "${w}/ing",
          // and make ...rn/ing -> ...rning - e.g. "learning"
          "(?<w>[^/]+rn)/ing(?<punc>\\W*)", "${w}ing${punc}", "${w}/ing",
          // and make ...eat/ing -> ...eating - e.g. "eating" "fleeting" "meeting"
          "(?<w>[^/]*e[ea]t)/ing(?<punc>\\W*)", "${w}ing${punc}", "${w}/ing",
          // but make ...t/ing -> ...tting - e.g. "putting" (but not ...nt/ing e.g. "hunting")
          // and make ...d/ing -> ...dding - e.g. "nodding" (but not ...nd/ing e.g. "landing")
          // and make ...p/ing -> ...pping - e.g. "shopping" (but not ...mp/ing e.g. "jumping")
          //                                                 (and not ...lp/ing e.g. "helping")
          // and make ...b/ing -> ...bbing - e.g. "mobbing" (but not ...mb/ing e.g. "numbing")
          // and make ...g/ing -> ...gging - e.g. "hugging" (but not ...ng/ing e.g. "ringing")
          // and make ...n/ing -> ...nning - e.g. "running"
          // and make ...m/ing -> ...mming - e.g. "drumming"
          "(?<w>[^/]+[^lmn])(?<c>[tdpbgnm])/ing(?<punc>\\W*)", "${w}${c}${c}ing${punc}",
          "${w}${c}/ing",
          // make .../ing -> ...ing
          "(?<w>[^/]+)/ing(?<punc>\\W*)", "${w}ing${punc}", "${w}/ing",
          
          // CONTRACTIONS:
          
          // /n't, /'t - Negative contractions. Examples: can/'t, does/n't
          // first some common errors: can/'nt can/n't
          "(?<w>[^/]+[^n])n?/'nt(?<punc>\\W*)", "${w}n't${punc}", "${w}n/'t",
          "(?<w>[^/]+[^n])n/n't(?<punc>\\W*)", "${w}n't${punc}", "${w}n/'t",
          // then make .../n't -> ...n't
          "(?<w>[^/]+)/n't(?<punc>\\W*)", "${w}n't${punc}", "${w}/n't",
          // ('t handled below)
          
          // /h's, /h'd, /d's, /d'd, /'us Contracted → HAS, HAD, DOES, DID, US
          // Examples: He/h's been sick. We/h'd better go. What/d's he do for a living?
          //           Why/d'd the boy look there? Let/'us go.
          "(?<w>[^/]+)/(?<suff>[hd]'[ds])(?<punc>\\W*)", "${w}${suff}${punc}", "${w}/${suff}",
          
          // contract /'us to just 's e.g. "let/'us" -> "let's"
          "(?<w>[^/]+)/'us(?<punc>\\W*)", "${w}'s${punc}", "${w}/'us",

          // /'ll, /'m, /'d, /'re, /'s, /'ve - Contracted → WILL, AM, WOULD, ARE, IS, HAVE
          // Examples:  I/'ll, I/'m, I/'d, we/'re, he/'s, we/'ve

          // just strip the slash from these
          "(?<w>[^/]+)/'(?<suff>..?)(?<punc>\\W*)", "${w}'${suff}${punc}", "${w}/'${suff}",
          
          // EXTENSIONS - these are not in the SALT specification, but are used at UC:
          
          // Adjectival inflections:
          
          // /er - comparative: a dog is fast/er than a pig. there 's a big/er cup.
          // first make ...e/er -> ...er
          "(?<w>[^/]+)e/er(?<punc>\\W*)", "${w}er${punc}", "${w}e/er",
          // and make ...g/er -> ...gger
          "(?<w>[^/]+)g/er(?<punc>\\W*)", "${w}gger${punc}", "${w}g/er",
          // make .../er -> ...er
          "(?<w>[^/]+)/er(?<punc>\\W*)", "${w}er${punc}", "${w}/er",
          
          // /est - superlative: this is the big/est tractor. you 're the fast/est.
          // first make ...e/est -> ...est
          "(?<w>[^/]+)e/est(?<punc>\\W*)", "${w}est${punc}", "${w}e/est",
          // and make ...g/est -> ...ggest
          "(?<w>[^/]+)g/est(?<punc>\\W*)", "${w}ggest${punc}", "${w}g/est",
          // make .../est -> ...est
          "(?<w>[^/]+)/est(?<punc>\\W*)", "${w}est${punc}", "${w}/est",
          
          // Derivational morphemes - e.g. soon/ish, it 's kind of blue/y.
          // first make ...d/y -> ...ddy e.g. "muddy"
          // and make ...t/y -> ...tty e.g. "fatty"
          "(?<w>[^/]+)(?<c>[td])/y(?<punc>\\W*)", "${w}${c}${c}y${punc}", "${w}${c}/y",
          // and make ...e/y -> ...y e.g. "gently"
          "(?<w>[^/]+)e/y(?<punc>\\W*)", "${w}y${punc}", "${w}e/y",
          // and errors like ...le/ly -> ...ly e.g. "gentle/ly"
          "(?<w>[^/]+)le/ly(?<punc>\\W*)", "${w}ly${punc}", "${w}le/y",
          // make .../y -> ...y
          "(?<w>[^/]+)/y(?<punc>\\W*)", "${w}y${punc}", "${w}/y",
          // first make ...d/ish -> ...ddish e.g. "reddish"
          // and make ...t/ish -> ...ttish e.g. "skittish"
          "(?<w>[^/]+)(?<c>[td])/ish(?<punc>\\W*)", "${w}${c}${c}ish${punc}", "${w}${c}/ish",
          // make .../ish -> ...ish
          "(?<w>[^/]+)/ish(?<punc>\\W*)", "${w}ish${punc}", "${w}/ish",
          
          // omitted bound morphemes
          "(?<w>[^/]+)/(?<mph>\\*[\\w0-9']+)(?<punc>\\W*)", "${w}${punc}", "${w}/${mph}",
          
          // and finally, any others just get the slashes stripped
          "(?<w>[^/]+)/(?<mph>[\\w0-9']+)(?<b2>/(?<mph2>[\\w0-9']+))?(?<punc>\\W*)",
          "${w}${mph}${mph2}${punc}", "${w}/${mph}${b2}"
        };      
        // process each triple in the array
        for (int t = 0; t < boundMorphemeTransformations.length; t += 3) {
          String sourcePattern = boundMorphemeTransformations[t];
          String sourceResult = boundMorphemeTransformations[t + 1];
          String destinationResult = boundMorphemeTransformations[t + 2];
          transformer = new ConventionTransformer(wordLayer.getId(), sourcePattern, sourceResult);
          if (boundMorphemeLayer != null) {
            // on the bound morpheme layer, include the base word
            transformer.addDestinationResult(boundMorphemeLayer.getId(), destinationResult);
          }
          transformer.transform(graph).commit();
        } // next transformation

        // comments mapped to layers
        for (Parameter p : parameters.values()) {
          if (p.getName().startsWith("comment_")) {
            Layer layer = (Layer)p.getValue();
            String key = p.getName().substring("comment_".length());
            SpanningConventionTransformer spanningTransformer = new SpanningConventionTransformer(
              wordLayer.getId(), "\\{("+key+":.*)", "(.*)\\}", true, null, null, 
              layer==null?null:layer.getId(), "$1", "$1", false,
              // if we're not keeping comments, close up the resulting gaps between words
              layer==null);
            spanningTransformer.transform(graph).commit();
            if (layer != null
                && (commentLayer == null || !layer.getId().equals(commentLayer.getId()))) {
              // layer isn't the comment layer, so strip the "${key}:" prefix off the labels
              new ConventionTransformer(layer.getId(), key+":(?<value>.+)", "${value}")
                .transform(graph).commit();
            } // layer isn't the comment layer
          } // comment parameter
        } // next parameter

        // infra-line comments - something like "{points to self}"
        SpanningConventionTransformer spanningTransformer = new SpanningConventionTransformer(
          wordLayer.getId(), "\\{(.*)", "(.*)\\}", true, null, null, 
          commentLayer==null?null:commentLayer.getId(), "$1", "$1", false,
          // if we're not keeping comments, close up the resulting gaps between words
          commentLayer==null);
        spanningTransformer.transform(graph).commit();
        
        // parentheticals - something like "((where was I))"
        spanningTransformer = new SpanningConventionTransformer(
          wordLayer.getId(), "\\(\\((.*)", "(.*)\\)\\)(?<punc>\\W*)", false, "$1", "$1${punc}", 
          parentheticalLayer==null?null:parentheticalLayer.getId(), "(($1...", "...$1))",
          false, false);
        spanningTransformer.transform(graph).commit();      
        if (parentheticalLayer != null) {
          // single-word parentheticals - get rid of the ellipses
          new ConventionTransformer(
            parentheticalLayer.getId(), "\\.\\.\\.\\(\\((.+)\\)\\)", "(($1))")
            .transform(graph).commit();
        }
        
        // mazes - something like "They (put them) put it"
        spanningTransformer = new SpanningConventionTransformer(
          wordLayer.getId(), "\\((.*)", "(.*)\\)(?<punc>\\W*)", false, "$1", "$1${punc}", 
          mazeLayer==null?null:mazeLayer.getId(), "($1...", "...$1)", false, false);
        spanningTransformer.transform(graph).commit();
        if (mazeLayer != null) {
          // single-word mazes - get rid of the ellipses
          new ConventionTransformer(mazeLayer.getId(), "\\.\\.\\.\\((.+)\\)", "($1)")
            .transform(graph).commit();
        }
        
        // pauses - something like ":03"
        transformer = new ConventionTransformer(wordLayer.getId(), ":(?<pause>.+)");
        if (pauseLayer != null) {
          transformer.addDestinationResult(pauseLayer.getId(), "${pause}");
        }
        transformer.transform(graph).commit();      
        
        // c-units
        if (cUnitLayer != null) {
          // multi-word c-units
          spanningTransformer = new SpanningConventionTransformer(
            wordLayer.getId(),
            "(?<firstWord>.+)", "(?<lastWord>.*)(?<terminator>[.?!~^>])", false,
            "${firstWord}", "${lastWord}${terminator}", 
            cUnitLayer.getId(), null, "${terminator}", false, false);
          spanningTransformer.transform(graph).commit();
        }
        
        // partial words  - something like "stu*"
        transformer = new ConventionTransformer(
          wordLayer.getId(),
          "(?<word>[^/]+)\\*(?<punctuation>\\W*)",
          "${word}~${punctuation}");
        if (partialWordLayer != null) {
          transformer.addDestinationResult(
            partialWordLayer.getId(), "${word}");
        }
        transformer.transform(graph).commit();
        
        // unintelligible speech:
        //  - "X" - for a single word
        //  - "XX" - for multiple words
        //  - "XXX" - for the entire utterance
        // we change these to underscores, to ensure that dictionary lookups fail
        // (e.g. we don't want the pronunciation of "X" being tagged as /eks/)
        // As some transcribers seem to use lowercase, we tolerate that too
        new ConventionTransformer(
          wordLayer.getId(), "[Xx](?<punctuation>\\W*)", "_${punctuation}")
          .transform(graph);
        new ConventionTransformer(
          wordLayer.getId(), "[Xx][Xx](?<punctuation>\\W*)", "__${punctuation}")
          .transform(graph);
        new ConventionTransformer(
          wordLayer.getId(), "[Xx][Xx][Xx](?<punctuation>\\W*)", "___${punctuation}")
          .transform(graph);
        
      } catch(TransformationException exception) {
        if (errors == null) errors = new SerializationException();
        if (errors.getCause() == null) errors.initCause(exception);
        errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
      }
      if (errors != null) throw errors;
    } // parseInlineConventions

    // set all annotations to manual confidence
    for (Annotation a : graph.getAnnotationsById().values()) {
      a.setConfidence(Constants.CONFIDENCE_MANUAL);
      // and ensure graph attributes 
    }
    graph.commit();

    // reset all change tracking
    graph.getTracker().reset();
    graph.setTracker(null);

    Graph[] graphs = { graph };
    return graphs;
  }

  /**
   * Returns any warnings that may have arisen during the last execution of
   * {@link #deserialize()}.
   * @return A possibly empty lilayersst of warnings.
   */
  public String[] getWarnings() {
    return warnings.toArray(new String[0]);
  }
   
  // GraphSerializer methods
   
  /**
   * Determines which layers, if any, must be present in the graph that will be serialized.
   * @return A list of IDs of layers that must be present in the graph that will be serialized.
   * @throws SerializationParametersMissingException If not all required parameters have a value.
   */
  public String[] getRequiredLayers() throws SerializationParametersMissingException {
    Vector<String> requiredLayers = new Vector<String>();
    requiredLayers.add(schema.getParticipantLayerId());
    requiredLayers.add(schema.getTurnLayerId());
    requiredLayers.add(schema.getUtteranceLayerId());
    requiredLayers.add(schema.getWordLayerId());

    if (targetParticipantLayer != null) requiredLayers.add(targetParticipantLayer.getId());
    if (commentLayer != null) requiredLayers.add(commentLayer.getId());
    if (cUnitLayer != null) requiredLayers.add(cUnitLayer.getId());
    if (parentheticalLayer != null) requiredLayers.add(parentheticalLayer.getId());
    if (properNameLayer != null) requiredLayers.add(properNameLayer.getId());
    if (repetitionsLayer != null) requiredLayers.add(repetitionsLayer.getId());
    if (rootLayer != null) requiredLayers.add(rootLayer.getId());
    if (errorLayer != null) requiredLayers.add(errorLayer.getId());
    if (soundEffectLayer != null) requiredLayers.add(soundEffectLayer.getId());
    if (pauseLayer != null) requiredLayers.add(pauseLayer.getId());
    if (boundMorphemeLayer != null) requiredLayers.add(boundMorphemeLayer.getId());
    if (mazeLayer != null) requiredLayers.add(mazeLayer.getId());
    if (partialWordLayer != null) requiredLayers.add(partialWordLayer.getId());
    if (omissionLayer != null) requiredLayers.add(omissionLayer.getId());
    if (codeLayer != null) requiredLayers.add(codeLayer.getId());
    if (languageLayer != null) requiredLayers.add(languageLayer.getId());
    if (participantIdLayer != null) requiredLayers.add(participantIdLayer.getId());
    if (genderLayer != null) requiredLayers.add(genderLayer.getId());
    if (dobLayer != null) requiredLayers.add(dobLayer.getId());
    if (doeLayer != null) requiredLayers.add(doeLayer.getId());
    if (caLayer != null) requiredLayers.add(caLayer.getId());
    if (ethnicityLayer != null) requiredLayers.add(ethnicityLayer.getId());
    if (contextLayer != null) requiredLayers.add(contextLayer.getId());
    if (subgroupLayer != null) requiredLayers.add(subgroupLayer.getId());
    if (collectLayer != null) requiredLayers.add(collectLayer.getId());
    if (locationLayer != null) requiredLayers.add(locationLayer.getId());
    
    return requiredLayers.toArray(new String[0]);
  } // getRequiredLayers()
   
  /**
   * Determines the cardinality between graphs and serialized streams.
   * <p>The cardinality of this deseerializer is NToN.
   * @return {@link GraphSerializer.Cardinality}.NToN.
   */
  public Cardinality getCardinality() {
    return Cardinality.NToN;
  }

  /**
   * Serializes the given series of graphs, generating one or more {@link NamedStream}s.
   * <p>Many data formats will only yield one stream per graph (e.g. Transcriber
   * transcript or Praat textgrid), however there are formats that use multiple files for
   * the same transcript (e.g. XWaves, EmuR), and others still that will produce one
   * stream from many Graphs (e.g. CSV).
   * <p>The method is synchronous in the sense that it should not return until all graphs
   * have been serialized.
   * @param graphs The graphs to serialize.
   * @param layerIds The IDs of the layers to include, or null for all layers.
   * @param consumer The consumer receiving the streams.
   * @param warnings A consumer for (non-fatal) warning messages.
   * @param errors A consumer for (fatal) error messages.
   * @throws SerializerNotConfiguredException if the object has not been configured.
   */
  public void serialize(
    Spliterator<Graph> graphs, String[] layerIds, Consumer<NamedStream> consumer,
    Consumer<String> warnings, Consumer<SerializationException> errors) 
    throws SerializerNotConfiguredException {
    reset();
    graphCount = graphs.getExactSizeIfKnown();
    graphs.forEachRemaining(graph -> {
        if (getCancelling()) return;
        try {
          consumer.accept(serializeGraph(graph, layerIds));
        } catch(SerializationException exception) {
          errors.accept(exception);
        }
        consumedGraphCount++;
      }); // next graph
  }

  /**
   * Serializes the given graph, generating a {@link NamedStream}.
   * @param graph The graph to serialize.
   * @return A named stream that contains the TextGrid. 
   * @throws SerializationException if errors occur during deserialization.
   */
  protected NamedStream serializeGraph(Graph graph, String[] layerIds)
    throws SerializationException {
    SerializationException errors = null;
    
    LinkedHashSet<String> selectedLayers = new LinkedHashSet<String>();
    if (layerIds != null) {
      for (String l : layerIds) {
        Layer layer = graph.getSchema().getLayer(l);
        if (layer != null) {
          selectedLayers.add(l);
        }
      } // next layeyId
    } else {
      for (Layer l : graph.getSchema().getLayers().values()) selectedLayers.add(l.getId());
    }
      
    try {
      // write the text to a temporary file
      File f = File.createTempFile(graph.getId(), ".slt");
      PrintWriter writer = new PrintWriter(
        new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));

      Schema schema = graph.getSchema();

      // partial words w~ to w*
      if (partialWordLayer == null) {
        new ConventionTransformer(
          schema.getWordLayerId(), "(.+)~", "$1*", null, null)
          .transform(graph);
      } else { // suffix words with *
        for (Annotation partialWord : graph.all(partialWordLayer.getId())) {
          Annotation word = partialWord.first(schema.getWordLayerId());
          if (word != null) {
            word.setLabel(partialWord.getLabel() + "*");
          }
        }
      }

      // participants...
      
      // participants have to start with a unique letter
      HashSet<Character> codes = new HashSet<Character>();
      Annotation[] participants = graph.all(graph.getSchema().getParticipantLayerId());

      String nameHeader = null;
      // first pass looking for standard names
      String[] standardNames = {
        "Child", "Parent", "Mother", "Father", "Brother", "Sister", "Participant", "Investigator",
        "Examiner", "Teacher", "Interviewer", "Unknown", "Unidentified"
      };
      for (Annotation participant : participants){
        Character code = null;
        for (String name : standardNames) {
          if (participant.getLabel().toLowerCase().contains(name.toLowerCase())
              && !codes.contains(name.charAt(0))) {
            // if the label is something like "ADAL-Child" then we want "ADAL" as the name header
            if (nameHeader == null) {
              nameHeader = participant.getLabel().replaceAll("(?i)-"+name,"");
              if (nameHeader.trim().length() == 0 || nameHeader.equals(participant.getLabel())) {
                // no replacement made, so we still don't know if there should be a name header
                nameHeader = null;
              }
            }
            participant.setLabel(name);
            code = participant.getLabel().charAt(0);
            break;
          } // matched a name
        } // next standard name
        if (code != null) {
          codes.add(code);
          participant.put("@code", code);
        }
      } // next participant
      
      // second pass trying to use current labels
      boolean residualParticipants = false;
      for (Annotation participant : participants) {
        if (!participant.containsKey("@code")) {
          if (!codes.contains(participant.getLabel().charAt(0))) {
            Character code = participant.getLabel().charAt(0);
            participant.put("@code", code);
            codes.add(code);
          } else {
            residualParticipants = true;
          }
        } // no code yet
      } // next participant

      if (residualParticipants) {
        // third pass - just assign them a code and prepend the label
        char nextCode = 'A';
        for (Annotation participant : participants) {
          if (!participant.containsKey("@code")) {
            // find the next available code
            while (codes.contains(nextCode)) nextCode++;
            // use it
            participant.setLabel(""+nextCode+"-"+participant.getLabel());
            codes.add(nextCode);
            participant.put("@code", nextCode);
          } // no code yet
        } // next participant
      }

      // relabel all turns with participant label
      for (Annotation participant : participants) {
        for (Annotation turn : participant.all(graph.getSchema().getTurnLayerId())) {
          turn.setLabel(participant.getLabel());
        }
      } // next participant

      // target participant
      StringBuilder participantsHeader = new StringBuilder();
      Annotation targetParticipant = null;
      String participantPrefix = IO.WithoutExtension(graph.getLabel()) + "-";
      if (targetParticipantLayer != null) { // target participant first
        for (Annotation participant : graph.all(targetParticipantLayer.getId())) {
          participant = participant.getParent(); // it's the parent we want
          if (targetParticipant == null) targetParticipant = participant;
          
          if (participantsHeader.length() == 0) {
            participantsHeader.append("$ ");
          } else {
            participantsHeader.append(", ");
          }
          // remove filename prefix, if any
          if (participant.getLabel().startsWith(participantPrefix)) {
            participant.setLabel(participant.getLabel().substring(participantPrefix.length()));
          }

          // write participant name
          participantsHeader.append(participant.getLabel());
        } // next target participant
      }
      // non-target participants
      for (Annotation participant : graph.all(schema.getParticipantLayerId())) {
        if (targetParticipantLayer != null // skip target participants
            && participant.first(targetParticipantLayer.getId()) != null) {
          continue;
        }
        if (targetParticipant == null) targetParticipant = participant;
        
        if (participantsHeader.length() == 0) {
          participantsHeader.append("$ ");
        } else {
          participantsHeader.append(", ");
        }
        // remove filename prefix, if any
        if (participant.getLabel().startsWith(participantPrefix)) {
          participant.setLabel(participant.getLabel().substring(participantPrefix.length()));
        }
        
        // write participant name
        participantsHeader.append(participant.getLabel());
      } // next participant
      writer.println(participantsHeader);
      
      // headers
      if (languageLayer != null) {
        Annotation annotation = graph.first(languageLayer.getId());
        if (annotation != null) {
          writer.print("+ Language: ");
          writer.println(iso639.name(annotation.getLabel()) // language name if possible
                         .orElse(annotation.getLabel()));
        }
      }
      if (nameHeader != null) {
        writer.print("+ Name: ");
        writer.println(nameHeader);
      }
      if (participantIdLayer != null) {
        Annotation annotation = targetParticipant.first(participantIdLayer.getId());
        if (annotation != null) {
          writer.print("+ ParticipantId: ");
          writer.println(annotation.getLabel());
        }
      }
      if (genderLayer != null) {
        Annotation annotation = targetParticipant.first(genderLayer.getId());
        if (annotation != null) {
          writer.print("+ Gender: ");
          String gender = annotation.getLabel();
          // try to standardize it
          if (gender.length() > 0) {
            if (gender.toLowerCase().charAt(0) == 'f') {
              gender = "F";
            } else if (gender.toLowerCase().charAt(0) == 'm') {
              gender = "M";
            }
          }
          writer.println(gender);
        }
      }
      SimpleDateFormat saltDateFormat = new SimpleDateFormat(dateFormat);
      SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd");
      if (dobLayer != null) {
        Annotation annotation = targetParticipant.first(dobLayer.getId());
        if (annotation != null) {
          writer.print("+ Dob: ");
          try { // parse as iso format
            Date date = isoDateFormat.parse(annotation.getLabel());
            // but save with configured format
            writer.println(saltDateFormat.format(date));
          } catch(ParseException exception) {
              warnings.add(
                "Could not parse dob \""+annotation.getLabel()+"\": " + exception.getMessage());
              writer.println(annotation.getLabel());
          }          
        }
      }
      if (doeLayer != null) {
        Annotation annotation = graph.first(doeLayer.getId());
        if (annotation != null) {
          writer.print("+ Doe: ");
          try { // parse as iso format
            Date date = isoDateFormat.parse(annotation.getLabel());
            // but save with configured format
            writer.println(saltDateFormat.format(date));
          } catch(ParseException exception) {
              warnings.add(
                "Could not parse doe \""+annotation.getLabel()+"\": " + exception.getMessage());
              writer.println(annotation.getLabel());
          }
        }
      }
      if (caLayer != null) {
        Annotation annotation = graph.first(caLayer.getId());
        if (annotation != null) {
          writer.print("+ Ca: ");
          writer.println(annotation.getLabel()); // TODO standardize?
        }
      }
      if (ethnicityLayer != null) {
        Annotation annotation = targetParticipant.first(ethnicityLayer.getId());
        if (annotation != null) {
          writer.print("+ Ethnicity: ");
          writer.println(annotation.getLabel());
        }
      }
      if (contextLayer != null) {
        Annotation annotation = graph.first(contextLayer.getId());
        if (annotation != null) {
          writer.print("+ Context: ");
          writer.println(annotation.getLabel());
        }
      }
      if (subgroupLayer != null) {
        Annotation annotation = graph.first(subgroupLayer.getId());
        if (annotation != null) {
          writer.print("+ Subgroup: ");
          writer.println(annotation.getLabel());
        }
      }
      if (collectLayer != null) {
        Annotation annotation = graph.first(collectLayer.getId());
        if (annotation != null) {
          writer.print("+ Collect: ");
          writer.println(annotation.getLabel());
        }
      }
      if (locationLayer != null) {
        Annotation annotation = graph.first(locationLayer.getId());
        if (annotation != null) {
          writer.print("+ Location: ");
          writer.println(annotation.getLabel());
        }
      }
         
      // for each utterance...
      Annotation currentParticipant = null;
         
      // order utterances by anchor so that simultaneous speech comes out in utterance order
      TreeSet<Annotation> utterancesByAnchor = new TreeSet<Annotation>(
        new AnnotationComparatorByAnchor()
        .setLongestFirst(false));
      utterancesByAnchor.addAll(Arrays.asList(graph.all(schema.getUtteranceLayerId())));

      // tag utterances that overlap
      Annotation lastUtterance = null;
      for (Annotation utterance : utterancesByAnchor) {
        if (lastUtterance != null) {
          if (lastUtterance.distance(utterance) < 0) { // utterances overlap
            utterance.put("@simultaneousWith", lastUtterance);
            lastUtterance.put("@simultaneousWith", utterance);
          }
        }
        lastUtterance = utterance;
      }

      // first timestamp
      Annotation firstUtterance = utterancesByAnchor.first();
      double lastOffset = printTimeStamp(
        firstUtterance.getStart(), writer, Double.NEGATIVE_INFINITY, graph);
      Anchor lastUtteranceEnd = null;

      // for each utterance
      Pattern tokenPattern = Pattern.compile("^(?<word>\\w+)(?<punctuation>[\\p{Punct}&&[^']]*)$");
      String delimiter = " ";
      for (Annotation utterance : utterancesByAnchor) {
        if (cancelling) break;

        // time stamp(s)
        if (lastUtteranceEnd != null && utterance.getStart() != null) {
          if (graph.compareOffsets(
                utterance.getStart().getOffset(), lastUtteranceEnd.getOffset()) >= 0) {
            lastOffset = printTimeStamp(lastUtteranceEnd, writer, lastOffset, graph);
          } else {
            lastOffset = lastUtteranceEnd.getOffset();
          }
        }
        lastOffset = printTimeStamp(utterance.getStart(), writer, lastOffset, graph);

        // preceding pause lines
        if (pauseLayer != null) {
          for (Annotation pause : utterance.getStart().startOf(pauseLayer.getId())) {
            if (!pause.containsKey("@alreadyPrinted")) {
              writer.print(": :");
              writer.println(pause.getLabel());
              pause.put("@alreadyPrinted", Boolean.TRUE);
            }
          } // next code
        }        

        // preceding comment lines
        if (commentLayer != null) {
          for (Annotation comment : utterance.getStart().endOf(commentLayer.getId())) {
            if (!comment.containsKey("@alreadyPrinted")) {
              writer.print("+ ");
              writer.println(comment.getLabel());
              comment.put("@alreadyPrinted", Boolean.TRUE);
            }
          } // next code
        }        

        writer.print(utterance.getParent().getParent().getLabel().substring(0,1));
        Annotation otherUtterance = (Annotation)utterance.get("@simultaneousWith");
        boolean inOverlap = false;
        for (Annotation token : utterance.all(schema.getWordLayerId())) {
          writer.print(delimiter);

          // preceding annotions...
          
          // comment?
          if (commentLayer != null) {
            for (Annotation comment : token.getStart().endOf(commentLayer.getId())) {
              if (!comment.containsKey("@alreadyPrinted")) {
                writer.write("{");
                writer.write(comment.getLabel());
                writer.write("} ");
                comment.put("@alreadyPrinted", Boolean.TRUE);
              }
            } // next comment
          }

          // pause?
          if (pauseLayer != null) {
            for (Annotation pause : token.getStart().endOf(pauseLayer.getId())) {
              if (!pause.containsKey("@alreadyPrinted")) {
                writer.write(":");
                writer.write(pause.getLabel());
                writer.write(" ");
                pause.put("@alreadyPrinted", Boolean.TRUE);
              }
            } // next pause
          }

          // soundEffect?
          if (soundEffectLayer != null) {
            for (Annotation soundEffect : token.getStart().endOf(soundEffectLayer.getId())) {
              writer.write("%");
              writer.write(soundEffect.getLabel());
              writer.write(" ");
            } // next soundEffect
          }

          // omission?
          if (omissionLayer != null) {
            for (Annotation omission : token.getStart().endOf(omissionLayer.getId())) {
              writer.write("*");
              writer.write(omission.getLabel());
              writer.write(" ");
            } // next omission
          }

          // in a repetition?
          if (repetitionsLayer != null) {
            if (token.getStart().isStartOn(repetitionsLayer.getId())) {
              delimiter = "_"; // underscores between words
            }
          }
          // in a proper name?
          if (properNameLayer != null) {
            if (token.getStart().isStartOn(properNameLayer.getId())) {
              delimiter = "_"; // underscores between words
            }
          }

          // parentheticals
          if (parentheticalLayer != null) {
            if (token.getStart().isStartOn(parentheticalLayer.getId())) {
              writer.print("((");
            }
          }

          // mazes
          if (mazeLayer != null) {
            if (token.getStart().isStartOn(mazeLayer.getId())) {
              writer.print("(");
            }
          }

          // split word from any trailing punctuation
          String word = token.getLabel();
          String trailingPuncuation = "";
          Matcher tokenParts = tokenPattern.matcher(word);
          if (tokenParts.matches()) {
            word = tokenParts.group("word");
            trailingPuncuation = tokenParts.group("punctuation");
          }
          
          // _ -> X (unintelligible)
          if (word.equals("_")) word = "X";
          else if (word.equals("__")) word = "XX";
          else if (word.equals("___")) word = "XXX";

          // partial word?
          Annotation partialWordTag = partialWordLayer != null?
            token.first(partialWordLayer.getId()) : null;
          if (partialWordTag != null) {
            word = partialWordTag.getLabel();
          } else { // change the convention: ~ -> *
            word = word.replace('~', '*');
          }

          // bound morpheme tag?
          Annotation boundMorphemeTag = boundMorphemeLayer != null?
            token.first(boundMorphemeLayer.getId()) : null;
          if (boundMorphemeTag != null) {
            word = boundMorphemeTag.getLabel();
            // remove any possessive apostrophes
            trailingPuncuation = trailingPuncuation.replaceAll("^'","");
          }
          
          if (otherUtterance != null) { // overlaps with another utterance
            if (!inOverlap) {
              Double distance = token.distance(otherUtterance);
              if (distance != null && distance < 0) { // overlaps with otherUtterance
                // overlap starts here
                if (!word.startsWith("<")) { // isn't already in the transcript
                  writer.print("<");
                }                
                inOverlap = true;
              }
            }
          }          

          writer.print(word);

          // following annotions...
          
          // finishing a repetition?
          if (repetitionsLayer != null) {
            if (token.getEnd().isEndOn(repetitionsLayer.getId())) {
              for (Annotation repeated : token.getEnd().endOf(repetitionsLayer.getId())) {
                writer.print("|");
                writer.print(repeated.getLabel());
                // there's only one
                break;
              }
              delimiter = " "; // back to spaces between words
            }
          }
          // finished a proper name?
          if (properNameLayer != null) {
            if (token.getEnd().isEndOn(properNameLayer.getId())) {
              delimiter = " "; // back to spaces between words
            }
          }

          // root?
          Annotation rootTag = rootLayer != null?
            token.first(rootLayer.getId()) : null;
          if (rootTag != null) {
            writer.write("|");
            writer.write(rootTag.getLabel());
          }

          // non-error codes
          if (codeLayer != null) {
            for (Annotation code : token.tagsOn(codeLayer.getId())) {
              writer.print("[");
              writer.print(code.getLabel());
              writer.print("]");
            } // next code
          }
          
          // error codes
          if (errorLayer != null) {
            for (Annotation code : token.tagsOn(errorLayer.getId())) {
              writer.print("[");
              writer.print(code.getLabel());
              writer.print("]");
            } // next code
          }

          // mazes
          if (mazeLayer != null) {
            if (token.getEnd().isEndOn(mazeLayer.getId())) {
              writer.print(")");
            }
          }

          // parentheticals
          if (parentheticalLayer != null) {
            if (token.getEnd().isEndOn(parentheticalLayer.getId())) {
              writer.print("))");
            }
          }
          
          if (trailingPuncuation.length() > 0) writer.print(trailingPuncuation);

          if (otherUtterance != null) { // overlaps with another utterance
            if (inOverlap) {
              if (otherUtterance.getEnd().getOffset() < token.getEnd().getOffset()) {
                // no longer during otherUtterance, so overlap ends here
                if (!trailingPuncuation.contains(">")) { // not already in the transcript
                  writer.print(">");
                }
                inOverlap = false;
              }
            }
          }

        } // next token
        if (inOverlap) {
          writer.print(">");
          inOverlap = false;
        }

        // utterance codes
        boolean firstUtteranceCode = true;
        
        // non-error codes
        if (codeLayer != null) {
          for (Annotation code : utterance.tagsOn(codeLayer.getId())) {
            if (firstUtteranceCode) {
              writer.print(" ");
              firstUtteranceCode = false;
            }
            writer.print("[");
            writer.print(code.getLabel());
            writer.print("]");
          } // next code
        }        
        // error codes
        if (errorLayer != null) {
          for (Annotation code : utterance.tagsOn(errorLayer.getId())) {
            if (firstUtteranceCode) {
              writer.print(" ");
              firstUtteranceCode = false;
            }
            writer.print("[");
            writer.print(code.getLabel());
            writer.print("]");
          } // next code
        }
        
        writer.println();

        // following comment lines
        if (commentLayer != null) {
          for (Annotation comment : utterance.getEnd().startOf(commentLayer.getId())) {
            if (!comment.containsKey("@alreadyPrinted")) {
              writer.print("+ ");
              writer.println(comment.getLabel());
              comment.put("@alreadyPrinted", Boolean.TRUE);
            }
          } // next code
        }        

        // following pause lines
        if (pauseLayer != null) {
          for (Annotation pause : utterance.getEnd().endOf(pauseLayer.getId())) {
            if (!pause.containsKey("@alreadyPrinted")) {
              writer.print(": :");
              writer.println(pause.getLabel());
              pause.put("@alreadyPrinted", Boolean.TRUE);
            }
          } // next code
        }        

        if (utterance.getEnd().getOffset() != null) {
          lastUtteranceEnd = utterance.getEnd();
        }
        
      } // next utterance
      if (lastUtteranceEnd != null) {
        lastOffset = printTimeStamp(lastUtteranceEnd, writer, lastOffset, graph);
      }
        
      writer.close();

      TempFileInputStream in = new TempFileInputStream(f);
         
      // return a named stream from the file
      String streamName = graph.getId();
      if (!IO.Extension(streamName).equals("slt")) {
        streamName = IO.WithoutExtension(streamName) + ".slt";
      }
      return new NamedStream(in, IO.SafeFileNameUrl(streamName));
    } catch(Exception exception) {
      errors = new SerializationException();
      errors.initCause(exception);
      errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
      throw errors;
    }      
  }

  /**
   * Conditionally prints a time stamp line for the given anchor.
   * <p> The time stamp is <em>not</em> printed if:
   * <ul>
   *  <li> the offset is null, </li>
   *  <li> the anchor confidence is not greater than CONFIDENCE_AUTOMATIC (50), </li>
   *  <li> the offset is less than or equal to the <var>lastOffset</var>. </li>
   * </ul>
   * @param anchor The anchor whose offset should perhaps be printed.
   * @param writer Where to print the time stamp.
   * @param lastOffset The last offset that was printed.
   * @param graph The annotato graph, so that {@link Graph#compareOffsets(double,double)}
   * can be invoked to compare the offets correctly.
   * @return The new value of lastOffset.
   */
  public double printTimeStamp(
    Anchor anchor, PrintWriter writer, double lastOffset, Graph graph) {
    if (anchor.getConfidence() != null
        && anchor.getConfidence() > Constants.CONFIDENCE_AUTOMATIC) {
      if (anchor.getOffset() != null) {
        if (graph.compareOffsets(anchor.getOffset(), lastOffset) > 0) {
          double seconds = anchor.getOffset();
          int minutes = (int)(seconds / 60);
          seconds -= minutes * 60;
          Object[] parts = { minutes, seconds };
          writer.println(timesStampFormat.format(parts));
          lastOffset = anchor.getOffset();
        } // offset not already past
      } // offset set
    } // confidence high
    return lastOffset;
  } // end of printTimeStamp()

  
  /** Command line interface, which takes transcript file names, and outputs JSON-encoded
   * annotation graphs. */
  public static void main(String argv[]) {
    SltSerialization application = new SltSerialization();
    if (application.processArguments(argv)) {
      application.start();
    }
  }
  /**
   * Specify the schema to used by  {@link Deserialize}.
   * @return The schema.
   */
  @Override protected Schema getDefaultSchema() {
    Schema schema = super.getDefaultSchema();
    // include SALT layers
    schema.addLayer(
      new Layer("transcript_language", "Language").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_recording_date", "Recording date")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_ca", "Current Age").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_context", "Context").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_subgroup", "Subgroup/Story").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_collect", "Collection Point").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_location", "Location").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("comment", "Comments").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(true).setSaturated(false));
    schema.addLayer(
      new Layer("noise", "Background noises").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("main_participant", "Target Speaker").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("participant_id", "Participant ID").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("participant_gender", "Gender").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("participant_dob", "Birth Date").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("participant_ethnicity", "Ethnicity").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("pause", "Pauses").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("cunit", "C-Unit").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("parenthetical", "Parentheticals").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("repetition", "Repetitions").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("error", "Errors").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("code", "Non-error Codes").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("maze", "Mazes").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("sound_effects", "Verbal sound effects etc.")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("entity", "Proper Names").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("omission", "Omissions").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("root", "Root form").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getWordLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("bound_morpheme", "Bound Morphemes").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getWordLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("partial_word", "Partial Words").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getWordLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("pronounce", "Pronunciation tags").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getWordLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("lexical", "Lexical tags").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getWordLayerId()).setParentIncludes(true));
    return schema;
  }
} // end of class SltSerialization
