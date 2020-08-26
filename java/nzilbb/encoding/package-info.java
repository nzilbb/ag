/**
 * Handing for difference encodings of phonemes.
 * <p> Phonemic and phonetic transcriptions may be expressed using a number of systems,
 * for example:
 * <dl>
 *  <dt>Unicode IPA</dt> 
 *   <dd> One or more Unicode character per phoneme, possibly including diacritics, 
 *        e.g. <q>there'll</q> = <samp>ðɛəɹl̩</samp></dd>
 *  <dt>CELEX DISC</dt> 
 *   <dd> Exactly one ASCII character per phoneme, 
 *        e.g. <q>there'll</q> = <samp>D8r@l</samp></dd>
 *  <dt>ARPAbet</dt>
 *   <dd> Phonemes are one or two uppercase ASCII characters, possibly suffixed with a
 *        digit indicating stress.
 *        e.g. <q>there'll</q> = <samp>DH EH1 R AX0 L</samp></dd>
 *  <dt>CMU</dt>
 *   <dd> A subset of ARPAbet, which excludes certain phonemes, including <tt>AX</tt> (schwa)
 *        e.g. <q>there'll</q> = <samp>DH EH1 R IH L</samp></dd>
 * </dl>
 * @author Robert Fromont robert@fromont.net.nz
 */
package nzilbb.encoding;
