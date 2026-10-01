/*-------------------------*/

/*Table structure for table lemma */

CREATE TABLE cxen_lemma (
  IdNumLemma int(11) NOT NULL,
  HeadDia varchar(50) default NULL,
  Cob int(11) default NULL,
  PRIMARY KEY  (IdNumLemma)
) ENGINE=MyISAM; 

/*Table structure for table lemmafrequency */

CREATE TABLE cxen_lemmafrequency (
  IdNumLemma int(11) NOT NULL,
  Cob int(11) default NULL,
  CobDev float default NULL,
  CobMln float default NULL,
  CobLog float default NULL,
  CobW float default NULL,
  CobWMln float default NULL,
  CobWLog float default NULL,
  CobS float default NULL,
  CobSMln float default NULL,
  CobSLog float default NULL,
  PRIMARY KEY  (IdNumLemma)
) ENGINE=MyISAM; 

/*Table structure for table lemmamorphology */

CREATE TABLE cxen_lemmamorphology (
  IdNumLemma int(11) NOT NULL,
  Cob int(11) default NULL,
  MorphStatus varchar(50) default NULL,
  Lang varchar(50) default NULL,
  PRIMARY KEY  (IdNumLemma)
) ENGINE=MyISAM; 

/*Table structure for table lemmamorphologyparse */

CREATE TABLE cxen_lemmamorphologyparse (
  IdNumLemma int(11) NOT NULL,
  Variant int(11) NOT NULL,
  NVAffComp varchar(50) default NULL,
  Der varchar(50) default NULL,
  Comp varchar(50) default NULL,
  DerComp varchar(50) default NULL,
  Def varchar(50) default NULL,
  Imm varchar(50) default NULL,
  ImmSubCat varchar(50) default NULL,
  ImmSA varchar(50) default NULL,
  ImmAllo varchar(50) default NULL,
  ImmSubst varchar(50) default NULL,
  ImmOpac varchar(50) default NULL,
  TransDer varchar(50) default NULL,
  ImmInfix varchar(50) default NULL,
  ImmRevers varchar(50) default NULL,
  FlatSA varchar(50) default NULL,
  StrucLab varchar(120) default NULL,
  StrucAllo varchar(50) default NULL,
  StrucSubst varchar(50) default NULL,
  StrucOpac varchar(50) default NULL,
  PRIMARY KEY  (IdNumLemma,Variant)
) ENGINE=MyISAM; 

/*Table structure for table lemmaortho */

CREATE TABLE cxen_lemmaortho (
  IdNumLemma int(11) NOT NULL,
  Variant int(11) NOT NULL,
  OrthoStatus varchar(50) default NULL,
  CobSpellFreq int(11) default NULL,
  CobSpellDev int(11) default NULL,
  HeadSylDia varchar(50) default NULL,
  PRIMARY KEY  (IdNumLemma,Variant)
) ENGINE=MyISAM; 

/*Table structure for table lemmaphonology */

CREATE TABLE cxen_lemmaphonology (
  IdNumLemma int(11) NOT NULL,
  Cob int(11) default NULL,
  PRIMARY KEY  (IdNumLemma)
) ENGINE=MyISAM; 

/*Table structure for table lemmaphonologypron */

CREATE TABLE cxen_lemmaphonologypron (
  IdNumLemma int(11) NOT NULL,
  Variant int(11) NOT NULL,
  PronStatus varchar(50) default NULL,
  PhonStrsDISC varchar(50) default NULL,
  PhonCVBr varchar(60) default NULL,
  PhonSylBCLX varchar(60) default NULL,
  PRIMARY KEY  (IdNumLemma,Variant)
) ENGINE=MyISAM; 

/*Table structure for table lemmasyntax */

CREATE TABLE cxen_lemmasyntax (
  IdNumLemma int(11) NOT NULL,
  Cob int(11) default NULL,
  ClassNum int(11) default NULL,
  C_N varchar(50) default NULL,
  Unc_N varchar(50) default NULL,
  Sing_N varchar(50) default NULL,
  Plu_N varchar(50) default NULL,
  GrC_N varchar(50) default NULL,
  GrUnc_N varchar(50) default NULL,
  Attr_N varchar(50) default NULL,
  PostPos_N varchar(50) default NULL,
  Voc_N varchar(50) default NULL,
  Proper_N varchar(50) default NULL,
  Exp_N varchar(50) default NULL,
  Trans_V varchar(50) default NULL,
  TransComp_V varchar(50) default NULL,
  Intrans_V varchar(50) default NULL,
  Ditrans_V varchar(50) default NULL,
  Link_V varchar(50) default NULL,
  Phr_V varchar(50) default NULL,
  Prep_V varchar(50) default NULL,
  PhrPrep_V varchar(50) default NULL,
  Exp_V varchar(50) default NULL,
  Ord_A varchar(50) default NULL,
  Attr_A varchar(50) default NULL,
  Pred_A varchar(50) default NULL,
  PostPos_A varchar(50) default NULL,
  Exp_A varchar(50) default NULL,
  Ord_ADV varchar(50) default NULL,
  Pred_ADV varchar(50) default NULL,
  PostPos_ADV varchar(50) default NULL,
  Comb_ADV varchar(50) default NULL,
  Exp_ADV varchar(50) default NULL,
  Card_NUM varchar(50) default NULL,
  Ord_NUM varchar(50) default NULL,
  Exp_NUM varchar(50) default NULL,
  Pers_PRON varchar(50) default NULL,
  Dem_PRON varchar(50) default NULL,
  Poss_PRON varchar(50) default NULL,
  Refl_PRON varchar(50) default NULL,
  Wh_PRON varchar(50) default NULL,
  Det_PRON varchar(50) default NULL,
  Pron_PRON varchar(50) default NULL,
  Exp_PRON varchar(50) default NULL,
  Cor_C varchar(50) default NULL,
  Sub_C varchar(50) default NULL,
  PRIMARY KEY  (IdNumLemma)
) ENGINE=MyISAM; 

/*Table structure for table wordclass */

CREATE TABLE cxen_wordclass (
  ClassNum int(11) NOT NULL,
  Label varchar(4) NOT NULL default '',
  Class varchar(100) NOT NULL default '',
  PRIMARY KEY  (ClassNum)
) ENGINE=MyISAM; 

insert into cxen_wordclass values (1,'N','Noun'),(2,'A','Adjective'),(3,'NUM','Numeral'),(4,'V','Verb'),(5,'ART','Article'),(6,'PRON','Pronoun'),(7,'ADV','Adverb'),(8,'PREP','Preposition'),(9,'C','Conjunction'),(10,'I','Interjection'),(11,'SCON','Single Contraction'),(12,'CCON','Complex Contraction'),(13,'LET','Letter'),(14,'ABB','Abbreviation'),(15,'TO','TO');


/*Table structure for table wordform */

CREATE TABLE cxen_wordform (
  IdNum int(11) NOT NULL,
  IdNumLemma int(11) NOT NULL,
  Cob int(11) default NULL,
  PRIMARY KEY  (IdNum)
/*,  KEY IX_IdNumLemma (IdNumLemma)*/
) ENGINE=MyISAM;

CREATE INDEX IX_IdNumLemma ON cxen_wordform(IdNumLemma);

/*Table structure for table wordformfrequency */

CREATE TABLE cxen_wordformfrequency (
  IdNum int(11) NOT NULL,
  Cob int(11) default NULL,
  CobDev float default NULL,
  CobMln float default NULL,
  CobLog float default NULL,
  CobW float default NULL,
  CobWMln float default NULL,
  CobWLog float default NULL,
  CobS float default NULL,
  CobSMln float default NULL,
  CobSLog float default NULL,
  PRIMARY KEY  (IdNum)
) ENGINE=MyISAM; 

/*Table structure for table wordformortho */

CREATE TABLE cxen_wordformortho (
  IdNum int(11) NOT NULL,
  Variant int(11) NOT NULL,
  WordDia varchar(50) default NULL,
  WordSylDia varchar(50) default NULL,
  OrthoStatus varchar(50) default NULL,
  CobSpellFreq int(11) default NULL,
  CobSpellDev int(11) default NULL,
  PRIMARY KEY  (IdNum,Variant)
/* , KEY WordDia (WordDia)*/
) ENGINE=MyISAM;

CREATE INDEX WordDia ON cxen_wordformortho(WordDia);

/*Table structure for table wordformphonology */

CREATE TABLE cxen_wordformphonology (
  IdNum int(11) NOT NULL,
  Cob int(11) default NULL,
  PRIMARY KEY  (IdNum)
) ENGINE=MyISAM; 

/*Table structure for table wordformphonologypron */

CREATE TABLE cxen_wordformphonologypron (
  IdNum int(11) NOT NULL,
  Variant int(11) NOT NULL,
  PronStatus varchar(50) default NULL,
  PhonStrsDISC varchar(50) default NULL,
  PhonCVBr varchar(60) default NULL,
  PhonSylBCLX varchar(60) default NULL,
  PRIMARY KEY  (IdNum,Variant)
) ENGINE=MyISAM; 

/*Table structure for table wordformmorphology */

CREATE TABLE cxen_wordformmorphology (
  IdNum int(11) NOT NULL,
  Cob int(11) default NULL,
  FlectType varchar(50) default NULL,
  TransInfl varchar(50) default NULL,
  PRIMARY KEY  (IdNum)
) ENGINE=MyISAM; 

