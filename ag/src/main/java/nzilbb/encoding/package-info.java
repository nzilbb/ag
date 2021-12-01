/**
 * Handling for translation between different phoneme encodings.
 *
 * <style type="text/css"> tt:before { content: '/'; } tt:after { content: '/'; } </style>
 * <p> Phonemic and phonetic transcriptions may be expressed using a number of systems,
 * for example:
 * <dl>
 *  <dt>Unicode IPA</dt> 
 *   <dd> One or more Unicode character per phoneme, possibly including diacritics, 
 *        e.g. <q>there'll</q> → <tt>ðɛəɹl̩</tt></dd>
 *  <dt>CELEX DISC</dt> 
 *   <dd> Exactly one ASCII character per phoneme, 
 *        e.g. <q>there'll</q> → <tt>D8r@l</tt></dd>
 *  <dt>ARPAbet</dt>
 *   <dd> Phonemes are one or two uppercase ASCII characters, possibly suffixed with a
 *        digit indicating stress.
 *        e.g. <q>there'll</q> → <tt>DH&nbsp;EH1&nbsp;R&nbsp;AX0&nbsp;L</tt></dd>
 *  <dt>CMU</dt>
 *   <dd> A subset of ARPAbet, which excludes certain phonemes, including <tt>AX</tt> (schwa)
 *        e.g. <q>there'll</q> → <tt>DH&nbsp;EH1&nbsp;R&nbsp;AH0&nbsp;L</tt></dd>
 * </dl>
 *
 * <p> This package includes classes for translating from one phoneme encoding to another.
 *
 * <p>The following table presents some common encodings and equivalences or near-equivalences
 * between phonemes.
 * <a class="sdfootnoteanc" href="#sdfootnote209sym" name="sdfootnote209anc"><sup>1</sup></a></p>
 * <style type="text/css"> #equiv td:not(:first-child) { text-align: center; } </style>
 * <table id="equiv" border="1" style="border-collapse: collapse;">
 *  <thead><tr>
 *    <th> Example </th>
 *    <th> IPA </th>
 *    <th> SAM-PA </th>
 *    <th> DISC<a class="sdfootnoteanc" href="#sdfootnote210sym" name="sdfootnote210anc"><sup>2</sup></a> </th>
 *    <th> CPA<a class="sdfootnoteanc" href="#sdfootnote211sym" name="sdfootnote211anc"><sup>3</sup></a> </th>
 *    <th> Kirshenbaum<a class="sdfootnoteanc" href="#sdfootnote212sym" name="sdfootnote212anc"><sup>4</sup></a> </th>
 *    <th> ARPAbet </th>
 *    <th> CMU Dict </th>
 *  </tr></thead><tbody>
 *   <tr><th colspan="8"> Vowels </th></tr>
 *   <tr><td> kit     </td><td> ɪ </td><td> I </td><td> I </td><td> I </td><td> I </td><td> IH </td><td> IH </td></tr>
 *   <tr><td> dress   </td><td> ɛ </td><td> <b>E</b> </td><td> E </td><td> E </td><td> E </td><td> EH </td><td> EH </td></tr>
 *   <tr><td> trap    </td><td> &aelig; </td><td> <b>{</b> </td><td> {</td><td> <b>^/</b> </td><td> <b>&amp;</b> </td><td> AE </td><td> AE </td></tr>
 *   <tr><td> strut   </td><td> ʌ </td><td> <b>V</b> </td><td> V </td><td> <b>^</b> </td><td> V </td><td> AH </td><td> AH </td></tr>
 *   <tr><td> foot    </td><td> ʊ </td><td> <b>U</b> </td><td> U </td><td> U </td><td> U </td><td> UH </td><td> UH </td></tr>
 *   <tr><td> <b>a</b>nother </td><td> ǝ </td><td> <b>@</b> </td><td> @ </td><td> @ </td><td> @ </td><td> AX </td><td> &nbsp; </td></tr>
 *   <tr><td> fleece  </td><td> iː </td><td> i: </td><td> i </td><td> i: </td><td> i: </td><td> IY </td><td> IY </td></tr>
 *   <tr><td> bath    </td><td> ɑː </td><td> <b>A:</b> </td><td> <b>#</b> </td><td> A: </td><td> <b>A:</b> </td><td> AA </td><td> AA </td></tr>
 *   <tr><td> lot     </td><td> ɒ </td><td> <b>Q</b> </td><td> Q </td><td> Q </td><td> <b>A.</b> </td> <td rowspan="2"> AO </td> <td rowspan="2"> AO </td></tr>
 *   <tr><td> thought </td><td> ɔː </td><td> <b>O:</b> </td><td> <b>$</b> </td><td> O: </td><td> O: </td></tr>
 *   <tr><td> goose   </td><td> uː </td><td> u: </td><td> u </td><td> u: </td><td> u: </td><td> UW </td><td> UW </td></tr>
 *   <tr><td> nurse   </td><td> ɜː </td><td> 3ː </td> <td> <b>3</b> </td><td> <b>@:</b> </td><td> <b>V&rdquo;</b> </td><td> ER </td><td> ER </td></tr>
 *   <tr><td> face    </td><td> eɪ </td><td> eI </td> <td> <b>1</b> </td><td> <b>e/</b> </td><td> eI </td><td> EY </td><td> EY </td></tr>
 *   <tr><td> price   </td><td> aɪ </td><td> aI </td> <td> <b>2</b> </td><td> <b>a/</b> </td><td> aI </td><td> AY </td><td> AY </td></tr>
 *   <tr><td> choice  </td><td> ɔɪ </td><td> <b>OI</b> </td> <td> <b>4</b> </td><td> <b>o/</b> </td><td> OI </td><td> OY </td><td> OY </td></tr>
 *   <tr><td> goat    </td><td> ǝʊ </td><td> <b>@U</b> </td> <td> <b>5</b> </td><td> <b>O/</b> </td><td> @U </td><td> OW </td><td> OW </td></tr>
 *   <tr><td> mouth   </td><td> aʊ </td><td> aU </td> <td> <b>6</b> </td><td> <b>A/</b> </td><td> aU </td><td> AW </td><td> AW </td></tr>
 *   <tr><td> near    </td><td> ɪǝ </td><td> <b>I@</b> </td> <td> <b>7</b> </td><td> <b>I/</b> </td><td> I@ </td><td> <i>IY R</i> </td><td> <i>IY R</i> </td></tr>
 *   <tr><td> square  </td><td> ɛǝ </td><td> <b>E@</b> </td> <td> <b>8</b> </td><td> <b>E/</b> </td><td> E@ </td><td> <i>EH R</i> </td><td> <i>EH R</i> </td></tr>
 *   <tr><td> cure    </td><td> ʊǝ </td><td> <b>U@</b> </td> <td> <b>9</b> </td><td> <b>U/</b> </td><td> U@ </td><td> <i>UH R</i> </td><td> <i>UH R</i> </td></tr>
 *   <tr><td> t<b>i</b>mbre </td><td> &aelig; </td><td> <b>{~</b> </td><td> <b>c</b> </td><td> <b>^/~</b> </td><td> <b>&amp;</b>~ </td><td> &nbsp; </td><td> &nbsp; </td></tr>
 *   <tr><td> d&eacute;t<b>en</b>te </td><td> ɑ̃ː </td><td> <b>A~:</b> </td><td> <b>q</b> </td><td> <b>A~:</b> </td><td> A~: </td><td> &nbsp; </td><td> &nbsp; </td></tr>
 *   <tr><td> l<b>in</b>gerie </td><td> &aelig;̃ː </td><td> <b>{~:</b> </td> <td> <b>0</b> </td><td> <b>^/~:</b> </td><td> <b>&amp;</b>~: </td><td> &nbsp; </td><td> &nbsp; </td></tr>
 *   <tr><td> bouill<b>on</b> </td><td> ɒ̃ː </td><td> <b>O~:</b> </td><td> <b>~</b> </td><td> <b>O~:</b> </td><td> A.~: </td><td> &nbsp; </td><td> &nbsp; </td></tr>
 *   <tr><th colspan="8"> Consonants </th></tr>
 *   <tr><td> <b>p</b>at      </td><td> p </td><td> p </td><td> p </td><td> p </td><td> p </td><td> P </td><td> P </td></tr>
 *   <tr><td> <b>b</b>ad      </td><td> b </td><td> b </td><td> b </td><td> b </td><td> b </td><td> B </td><td> B </td></tr>
 *   <tr><td> <b>t</b>ack     </td><td> t </td><td> t </td><td> t </td><td> t </td><td> t </td><td> T </td><td> T </td></tr>
 *   <tr><td> <b>d</b>ad      </td><td> d </td><td> d </td><td> d </td><td> d </td><td> d </td><td> D </td><td> D </td></tr>
 *   <tr><td> <b>c</b>ad      </td><td> k </td><td> k </td><td> k </td><td> k </td><td> k </td><td> K </td><td> K </td></tr>
 *   <tr><td> <b>g</b>ame     </td><td> g </td><td> g </td><td> g </td><td> g </td><td> g </td><td> G </td><td> G </td></tr>
 *   <tr><td> ba<b>ng</b>     </td><td> ŋ </td><td> <b>N</b> </td><td> <b>N</b> </td><td> <b>N</b> </td><td> N </td><td> NG </td><td> NG </td></tr>
 *   <tr><td> <b>m</b>ad      </td><td> m </td><td> m </td><td> m </td><td> m </td><td> m </td><td> M </td><td> M </td></tr>
 *   <tr><td> <b>n</b>at      </td><td> n </td><td> n </td><td> n </td><td> n </td><td> n </td><td> N </td><td> N </td></tr>
 *   <tr><td> <b>l</b>ad      </td><td> l </td><td> l </td><td> l </td><td> l </td><td> l </td><td> L </td><td> L </td></tr>
 *   <tr><td> <b>r</b>at      </td><td> r </td><td> r </td><td> r </td><td> r </td><td> r </td><td> R </td><td> R </td></tr>
 *   <tr><td> <b>f</b>at      </td><td> f </td><td> f </td><td> f </td><td> f </td><td> f </td><td> F </td><td> F </td></tr>
 *   <tr><td> <b>v</b>at      </td><td> v </td><td> v </td><td> v </td><td> v </td><td> v </td><td> V </td><td> V </td></tr>
 *   <tr><td> <b>th</b>in     </td><td> Ɵ </td><td> <b>T</b> </td><td> T </td><td> T </td><td> T </td><td> TH </td><td> TH </td></tr>
 *   <tr><td> <b>th</b>en     </td><td> &eth; </td><td> <b>D</b> </td><td> D </td><td> D </td><td> D </td><td> DH </td><td> DH </td></tr>
 *   <tr><td> <b>s</b>ap      </td><td> s </td><td> s </td><td> s </td><td> s </td><td> s </td><td> S </td><td> S </td></tr>
 *   <tr><td> <b>z</b>ap      </td><td> z </td><td> z </td><td> z </td><td> z </td><td> z </td><td> Z </td><td> Z </td></tr>
 *   <tr><td> <b>sh</b>eep    </td><td> ʃ </td><td> <b>S</b> </td><td> S </td><td> S </td><td> S </td><td> SH </td><td> SH </td></tr>
 *   <tr><td> mea<b>s</b>ure  </td><td> Ʒ </td><td> <b>Z</b> </td><td> Z </td><td> Z </td><td> Z </td><td> ZH </td><td> ZH </td></tr>
 *   <tr><td> <b>y</b>ank     </td><td> j </td><td> j </td><td> j </td><td> j </td><td> j </td><td> Y </td><td> Y </td></tr>
 *   <tr><td> <b>h</b>ad      </td><td> h </td><td> h </td><td> h </td><td> h </td><td> h </td><td> HH </td><td> HH </td></tr>
 *   <tr><td> <b>w</b><span>et</span> </td><td> w </td><td> w </td><td> w </td><td> w </td><td> w </td><td> W </td><td> W </td></tr>
 *   <tr><td> <b>ch</b>eap    </td><td> ʧ </td><td> tS </td><td> <b>J</b> </td><td> <b>T/</b> </td><td> tS </td><td> CH </td><td> CH </td></tr>
 *   <tr><td> <b>j</b>eep     </td><td> ʤ </td><td> dZ </td><td> <b>_</b> </td><td> <b>J/</b> </td><td> dZ </td><td> JH </td><td> JH </td></tr>
 *   <tr><td> lo<b>ch</b>     </td><td> x </td><td> x </td><td> x </td><td> x </td><td> x </td><td> &nbsp; </td><td> &nbsp; </td></tr>
 *   <tr><td> baco<b>n</b>    </td><td> ŋ̩ </td><td> <b>N,</b> </td><td> <b>C</b> </td><td> N, </td><td> <b>N-</b> </td><td> &nbsp; </td><td> &nbsp; </td></tr>
 *   <tr><td> idealis<b>m</b> </td><td> m̩ </td><td> <b>m,</b> </td><td> <b>F</b> </td><td> m, </td><td> <b>m-</b> </td><td> &nbsp; </td><td> &nbsp; </td></tr>
 *   <tr><td> burde<b>n</b>   </td><td> n̩ </td><td> <b>n,</b> </td><td> <b>H</b> </td><td> n, </td><td> <b>n-</b> </td><td> &nbsp; </td><td> &nbsp; </td></tr>
 *   <tr><td> dang<b>l</b>e   </td><td> l </td><td> <b>l,</b> </td><td> <b>P</b> </td><td> l, </td><td> <b>l-</b> </td><td> &nbsp; </td><td> &nbsp; </td></tr>
 *   <tr><td> ca<b>r</b> alarm </td><td> * </td><td> <b>r*</b> </td><td> <b>R</b> </td><td> r* </td><td> &nbsp; </td><td> &nbsp; </td><td> &nbsp; </td></tr>
 *   <tr><td> uh<b>-</b>oh    </td><td> ʔ </td><td> <b>?</b> </td><td> &nbsp; </td><td> &nbsp; </td><td> ? </td><td> Q </td><td> &nbsp; </td> </tr>
 *   <tr><td> fathe<b>r</b>   </td><td> ɚ </td><td> &nbsp; </td><td> &nbsp; </td><td> &nbsp; </td><td> &nbsp; </td><td> AXR </td><td> &nbsp; </td></tr>
 *   <tr><td> we<b>tt</b>er   </td><td> ɾ </td><td> &nbsp; </td><td> &nbsp; </td><td> &nbsp; </td><td> &nbsp; </td><td> DX </td><td> &nbsp; </td></tr>
 * </tbody></table>
 *
 * <div id="sdfootnote209">
 *  <p class="sdfootnote">
 *   <sup><a class="sdfootnotesym" href="#sdfootnote209anc" name="sdfootnote209sym">1</a></sup> 
 *   In the table, some phoneme
 *   representations are highlighted with a <b>bold</b> typeface; this highlighting is
 *   intended to indicate representations that are unpredictable in some way, either because
 *   they're substantially different from IPA or from English orthographical convention,
 *   or they're different from the corresponding representation in an otherwise-similar
 *   set of representations. Others are highlighted with an <i>italic</i> typeface; these are
 *   examples of representations that actually use a combination of two phonemes, where in
 *   other sets only one phoneme is used.</p> 
 * </div>
 * 
 * <div id="sdfootnote210">
 *  <p class="sdfootnote">
 *   <sup><a class="sdfootnotesym" href="#sdfootnote210anc" name="sdfootnote210sym">2</a></sup>
 *   SAM-PA and DISC phonemes taken from CELEX English Guide (1995) 
 *   &sect; 2.4.1 pp. 31-32, Tables 3 &amp; 4.</p> 
 * </div>
 * 
 * <div id="sdfootnote211">
 *  <p class="sdfootnote">
 *   <sup><a class="sdfootnotesym" href="#sdfootnote211anc" name="sdfootnote211sym">3</a></sup> 
 *   The Computer Phonetic Alphabet (CPA) was developed for seven European languages, based on
 *   the IPA - Kugler-Kruse (1987)</p> 
 * </div>
 * 
 * <div id="sdfootnote212">
 *  <p class="sdfootnote">
 *   <sup><a class="sdfootnotesym" href="#sdfootnote212anc" name="sdfootnote212sym">4</a></sup>
 *   <a href="http://en.wikipedia.org/wiki/Kirshenbaum">http://en.wikipedia.org/wiki/Kirshenbaum</a></p>
 * </div>
 * 
 * @author Robert Fromont robert@fromont.net.nz
 */
package nzilbb.encoding;
