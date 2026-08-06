# Converters

**Implementation for converters has moved to here:**

**<https://github.com/nzilbb/converter#readme>**

Standalone programs that convert transcripts from one tool format to another, e.g.

* trs - [Transcriber](http://trans.sourceforge.net/en/presentation.php) transcripts
* eaf - [ELAN](https://archive.mpi.nl/tla/elan) files
* vtt - [web subtitles (Web VTT)](https://en.wikipedia.org/wiki/WebVTT)
* slt - [SALT](https://www.saltsoftware.com/) transcripts
* cha - [CLAN](https://dali.talkbank.org/clan/) CHAT transcripts
* textgrid - [Praat](https://praat.org) TextGrids
* pdf - *PDF* files
* tex - *LaTeX* files
* txt - *plain text* files
* kaldi - input files for the [Kaldi](https://kaldi-asr.org/) automatic speech recognition training system

| to↓ from→ | trs | eaf  | vtt | slt | cha | textgrid | txt |
| --- | :---: | :---:  | :---: | :---: | :---: | :---: | :---: |
| **trs** | | [eaf-to-trs](https://nzilbb.github.io/converter/eaf-to-trs/) | [vtt-to-trs](https://nzilbb.github.io/converter/vtt-to-trs/) | [slt-to-trs](https://nzilbb.github.io/converter/slt-to-trs/) | [cha-to-trs](https://nzilbb.github.io/converter/cha-to-trs/) | [textgrid-to-trs](https://nzilbb.github.io/converter/textgrid-to-trs/) | |
| **eaf** | [trs-to-eaf](https://nzilbb.github.io/converter/trs-to-eaf/) | | [vtt-to-eaf](https://nzilbb.github.io/converter/vtt-to-eaf/) | [slt-to-eaf](https://nzilbb.github.io/converter/slt-to-eaf/) | [cha-to-eaf](https://nzilbb.github.io/converter/cha-to-eaf/) | [textgrid-to-eaf](https://nzilbb.github.io/converter/textgrid-to-eaf/) | [txt-to-eaf](https://nzilbb.github.io/converter/txt-to-eaf/) |
| **vtt** | [trs-to-vtt](https://nzilbb.github.io/converter/trs-to-vtt/) | [eaf-to-vtt](https://nzilbb.github.io/converter/eaf-to-vtt/) | | [slt-to-vtt](https://nzilbb.github.io/converter/slt-to-vtt/) | [cha-to-vtt](https://nzilbb.github.io/converter/cha-to-vtt/) | [textgrid-to-vtt](https://nzilbb.github.io/converter/textgrid-to-vtt/) | |
| **slt** | [trs-to-slt](https://nzilbb.github.io/converter/trs-to-slt/) | [eaf-to-slt](https://nzilbb.github.io/converter/eaf-to-slt/) | | | | | |
| **cha** | [trs-to-cha](https://nzilbb.github.io/converter/trs-to-cha/) | [eaf-to-cha](https://nzilbb.github.io/converter/eaf-to-cha/) | [vtt-to-cha](https://nzilbb.github.io/converter/vtt-to-cha/) | | | | |
| **textgrid** | [trs-to-textgrid](https://nzilbb.github.io/converter/trs-to-textgrid/) | [eaf-to-textgrid](https://nzilbb.github.io/converter/eaf-to-textgrid/) | [vtt-to-textgrid](https://nzilbb.github.io/converter/vtt-to-textgrid/) | [slt-to-textgrid](https://nzilbb.github.io/converter/slt-to-textgrid/) | [cha-to-textgrid](https://nzilbb.github.io/converter/cha-to-textgrid/) | | |
| **txt** | [trs-to-txt](https://nzilbb.github.io/converter/trs-to-txt/) | | | | | | |
| **pdf** | [trs-to-pdf](https://nzilbb.github.io/converter/trs-to-pdf/) | [eaf-to-pdf](https://nzilbb.github.io/converter/eaf-to-pdf/) | [vtt-to-pdf](https://nzilbb.github.io/converter/vtt-to-pdf/) | [slt-to-pdf](https://nzilbb.github.io/converter/slt-to-pdf/) | [cha-to-pdf](https://nzilbb.github.io/converter/cha-to-pdf/) | [textgrid-to-pdf](https://nzilbb.github.io/converter/textgrid-to-pdf/) | |
| **tex** | [trs-to-tex](https://nzilbb.github.io/converter/trs-to-tex/) | [eaf-to-tex](https://nzilbb.github.io/converter/eaf-to-tex/) | [vtt-to-tex](https://nzilbb.github.io/converter/vtt-to-tex/) | [slt-to-tex](https://nzilbb.github.io/converter/slt-to-tex/) | | [textgrid-to-tex](https://nzilbb.github.io/converter/textgrid-to-tex/) | |
| **kaldi** | [trs-to-kaldi](https://nzilbb.github.io/converter/trs-to-kaldi/) | [eaf-to-kaldi](https://nzilbb.github.io/converter/eaf-to-kaldi/) | | | | [textgrid-to-kaldi](https://nzilbb.github.io/converter/textgrid-to-kaldi/) | |

These use the serializers/deserializers in the *formatter* directory of this repository
to read a file in one format, convert it to an annotation graph, and then write that graph
out as a file in another format. As pointed out by
Cochran et al. (2007 - *Report from TILR Working Group 1 : Tools interoperability and input/output formats*)
this saves having order *n<sup>2</sup>* explicit conversion algorithms between formats;
only *2n* format conversions are required
(as some of these formats above are output-only, it's actually less than *2n*).

This exemplifies an approach to linguistic data interoperability called the *interlingua
philosophy on interoperability* by
[Witt et al. (2009)](https://www.w3.org/People/fsasaki/docs/lre-intro.pdf)
and uses annotation graphs as an 'interlingua' similar to work by 
[Schmidt et al. (2008)](https://ids-pub.bsz-bw.de/frontdoor/deliver/index/docId/2308/file/Schmidt%20etc_An_exchange_format_for_multimodal_annotations_2008.pdf),
except that rather using a third file format as a persistent intermediary, the annotation
graph models of the linguistic data are ephemeral, existing in memory only for the duration of the
conversion.

As there is no persistent intermediate file, and many formats do not support the
meta-data, annotation granularity or ontology of other formats, many of these conversions
necessarily entail loss of data. However, mappings are made from one format to another
wherever possible.

For notes about specific correspondences or data losses, use the `--help` command-line
switch, or use the *Help|Information* menu option of the conversion utility concerned.
