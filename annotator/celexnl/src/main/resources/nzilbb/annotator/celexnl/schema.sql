/*-------------------------*/

/*Table structure for table lemma */

CREATE TABLE cxnl_lemma (
  IdNumLemma int(11) NOT NULL,
  HeadDia varchar(50) default NULL,
  Inl int(11) default NULL,
  PRIMARY KEY  (IdNumLemma)
) ENGINE=MyISAM; 

/*Table structure for table lemmafrequency */

CREATE TABLE cxnl_lemmafrequency (
  IdNumLemma int(11) NOT NULL,
  Inl int(11) default NULL,
  InlDev float default NULL,
  InlMln float default NULL,
  InlLog float default NULL,
  Dict varchar(1) default NULL,
  PRIMARY KEY  (IdNumLemma)
) ENGINE=MyISAM; 

/*Table structure for table lemmamorphology */

CREATE TABLE cxnl_lemmamorphology (
  IdNumLemma int(11) NOT NULL,
  Inl int(11) default NULL,
  MorphStatus varchar(50) default NULL,
  PRIMARY KEY  (IdNumLemma)
) ENGINE=MyISAM; 

/*Table structure for table lemmamorphologyparse */

CREATE TABLE cxnl_lemmamorphologyparse (
  IdNumLemma int(11) NOT NULL,
  Variant int(11) NOT NULL,
  DerComp varchar(50) default NULL,
  Comp varchar(50) default NULL,
  Def varchar(50) default NULL,
  Imm varchar(50) default NULL,
  ImmSubCat varchar(50) default NULL,
  ImmAllo varchar(50) default NULL,
  ImmSubst varchar(50) default NULL,
  StrucLab varchar(150) default NULL,
  StrucAllo varchar(50) default NULL,
  StrucSubst varchar(50) default NULL,
  Sepa varchar(50) default NULL,
  PRIMARY KEY  (IdNumLemma,Variant)
) ENGINE=MyISAM; 

/*Table structure for table lemmaortho */

CREATE TABLE cxnl_lemmaortho (
  IdNumLemma int(11) NOT NULL,
  Variant int(11) NOT NULL,
  HeadDia varchar(50) default NULL,
  OrthoStatus varchar(50) default NULL,
  InlSpellFreq int(11) default NULL,
  InlSpellDev int(11) default NULL,
  HeadSylDia varchar(50) default NULL,
  StemDia varchar(50) default NULL,
  StemSylDia varchar(50) default NULL,
  AbStemDia varchar(50) default NULL,
  PRIMARY KEY  (IdNumLemma,Variant)
) ENGINE=MyISAM; 

/*Table structure for table lemmaphonology */

CREATE TABLE cxnl_lemmaphonology (
  IdNumLemma int(11) NOT NULL,
  Head varchar(50) default NULL,
  Inl int(11) default NULL,
  PhonStrsDISC varchar(50) default NULL,
  PhonCVBr varchar(150) default NULL,
  PhonSylBCLX varchar(150) default NULL,
  PhonStrsStDISC varchar(50) default NULL,
  PhonStCVBr varchar(150) default NULL,
  PhonSylStBCLX varchar(150) default NULL,
  PhonolCLX varchar(150) default NULL,
  PhonolCPA varchar(150) default NULL,
  PRIMARY KEY  (IdNumLemma)
) ENGINE=MyISAM; 

/*Table structure for table lemmasyntax */

CREATE TABLE cxnl_lemmasyntax (
  IdNumLemma int(11) NOT NULL,
  Inl int(11) default NULL,
  ClassNum int(11) default NULL,
  GendNum int(11) default NULL,
  DeHetNum int(11) default NULL,
  PropNum int(11) default NULL,
  AuxNum int(11) default NULL,
  SubClassVNum int(11) default NULL,
  SubCatNum int(11) default NULL,
  AdvNum int(11) default NULL,
  CardOrdNum int(11) default NULL,
  SubClassPNum int(11) default NULL,
  PRIMARY KEY  (IdNumLemma)
) ENGINE=MyISAM; 

/*Table structure for table wordclass */

CREATE TABLE cxnl_wordclass (
  ClassNum int(11) NOT NULL,
  Label varchar(4) default '',
  Class varchar(100) default '',
  PRIMARY KEY  (ClassNum)
) ENGINE=MyISAM; 

INSERT INTO cxnl_wordclass VALUES 
(0,'EXP','Expression'),
(1,'N','Noun'),
(2,'A','Adjective'),
(3,'NUM','Numeral'),
(4,'V','Verb'),
(5,'ART','Article'),
(6,'PRON','Pronoun'),
(7,'ADV','Adverb'),
(8,'PREP','Preposition'),
(9,'C','Conjunction'),
(10,'I','Interjection');

CREATE TABLE cxnl_gender (
  GendNum int(11) NOT NULL,
  Label varchar(6) default '',
  Gender varchar(100) default '',
  PRIMARY KEY  (GendNum)
) ENGINE=MyISAM; 

INSERT INTO cxnl_gender VALUES
(-1,'?','?'),
(0,'',''),
(1,'m','Male'),
(2,'v','Female'),
(3,'o','Neutral'),
(4,'v(m)','Female (Male)'),
(5,'m-v','Context'),
(13,'m o','Male or Neutral'),
(23,'v o','Female or Neutral'),
(31,'o m','Neutral or Male'),
(43,'v(m) o','Female (Male) or Neutral'),
(34,'o v(m)','Neutral or Female (Male)');

CREATE TABLE cxnl_dehet (
  DeHetNum int(11) NOT NULL,
  Label varchar(6) default '',
  DeHet varchar(100) default '',
  PRIMARY KEY  (DeHetNum)
) ENGINE=MyISAM; 

INSERT INTO cxnl_dehet VALUES
(1,'de','De(non-neutral)'),
(2,'het','Het(neutral)'),
(12,'de/het','De or Het');

CREATE TABLE cxnl_prop (
  PropNum int(11) NOT NULL,
  Label varchar(6) default '',
  ProperNoun varchar(100) default '',
  PRIMARY KEY  (PropNum)
) ENGINE=MyISAM; 

INSERT INTO cxnl_prop VALUES 
(1,'geo','Geographical'),
(2,'pers','Person'),
(3,'merk','Company/Brand'),
(4,'over','Other');

CREATE TABLE cxnl_aux (
  AuxNum int(11) NOT NULL,
  Label varchar(11) default '',
  Auxiliary varchar(100) default '',
  PRIMARY KEY  (AuxNum)
) ENGINE=MyISAM; 

INSERT INTO cxnl_aux VALUES
(1,'hebben','hebben'),
(2,'zijn','zijn'),
(12,'hebben/zijn','hebben or zijn');

CREATE TABLE cxnl_subclassv (
  SubClassVNum int(11) NOT NULL,
  Label varchar(11) default '',
  Subclass varchar(100) default '',
  PRIMARY KEY  (SubClassVNum)
) ENGINE=MyISAM; 

INSERT INTO cxnl_subclassv VALUES
(1,'zelfst','Lexical'),
(2,'hulp','Auxiliary'),
(3,'koppel','Copula'),
(4,'onpers','Impersonal');


CREATE TABLE cxnl_subcat (
  SubCatNum int(11) NOT NULL,
  Label varchar(21) default '',
  Subcategorization varchar(100) default '',
  PRIMARY KEY  (SubCatNum)
) ENGINE=MyISAM; 

INSERT INTO cxnl_subcat VALUES
(1,'intrans','Intransitive'),
(2,'trans','Transitive'),
(3,'wederk','Reflexive'),
(12,'intrans/trans','Intransitive/Transitive'),
(13,'intrans/wederk','Intransitive/Reflexive'),
(23,'trans/wederk','Transitive/Reflexive'),
(123,'intrans/trans/wederk','Intransitive/Transitive/Reflexive');

CREATE TABLE cxnl_adv (
  AdvNum int(11) NOT NULL,
  Label varchar(21) default '',
  Adverb varchar(100) default '',
  PRIMARY KEY  (AdvNum)
) ENGINE=MyISAM; 

INSERT INTO cxnl_adv VALUES
(1,'adverb','Adverb'),
(2,'not adverb','Not Adverb');

CREATE TABLE cxnl_cardord (
  CardOrdNum int(11) NOT NULL,
  Label varchar(21) default '',
  CardinalOrdinal varchar(100) default '',
  PRIMARY KEY  (CardOrdNum)
) ENGINE=MyISAM; 

INSERT INTO cxnl_cardord VALUES
(1,'card','Cardinal'),
(2,'ord','Ordinal');

CREATE TABLE cxnl_subclassp (
  SubClassPNum int(11) NOT NULL,
  Label varchar(16) default '',
  Subclass varchar(100) default '',
  PRIMARY KEY  (SubClassPNum)
) ENGINE=MyISAM; 

INSERT INTO cxnl_subclassp VALUES
(-1,'?','?'),
(1,'pers','Personal'),
(2,'aanw','Demonstrative'),
(3,'bez','Possessive'),
(4,'betr','Relative'),
(5,'vraag','Interrogative'),
(6,'wknd','Reflexive'),
(7,'wkg','Reciprocal'),
(8,'onbep','Indefinite'),
(9,'uitr','Exclamatory'),
(24,'aanw/betr','Demonstrative/Relative'),
(924,'uitr/aanw/betr','Exclamatory/Demonstrative/Relative'),
(13,'pers/bez','Personal/Possessive'),
(18,'pers/onbep','Personal/Indefinite'),
(138,'pers/bez/onbep','Personal/Possessive/Indefinite'),
(16,'pers/wknd','Personal/Reflexive'),
(458,'betr/vraag/onbep','Relative/Interrogative/Indefinite');

/*Table structure for table wordform */

CREATE TABLE cxnl_wordform (
  IdNum int(11) NOT NULL,
  IdNumLemma int(11) NOT NULL,
  Inl int(11) default NULL,
  PRIMARY KEY  (IdNum)
) ENGINE=MyISAM; 

CREATE INDEX IX_CXNL_IdNumLemma ON cxnl_wordform(IdNumLemma);

/*Table structure for table wordformfrequency */

CREATE TABLE cxnl_wordformfrequency (
  IdNum int(11) NOT NULL,
  Inl int(11) default NULL,
  InlDev float default NULL,
  InlMln float default NULL,
  InlLog float default NULL,
  PRIMARY KEY  (IdNum)
) ENGINE=MyISAM; 

/*Table structure for table wordformortho */

CREATE TABLE cxnl_wordformortho (
  IdNum int(11) NOT NULL,
  Variant int(11) NOT NULL,
  WordDia varchar(50) default NULL,
  OrthoStatus varchar(50) default NULL,
  InlSpellFreq int(11) default NULL,
  InlSpellDev int(11) default NULL,
  WordSylDia varchar(50) default NULL,
  PRIMARY KEY (IdNum,Variant)
) ENGINE=MyISAM; 

CREATE INDEX IX_CXNL_WordDia ON cxnl_wordformortho(WordDia);

/*Table structure for table wordformphonology */

CREATE TABLE cxnl_wordformphonology (
  IdNum int(11) NOT NULL,
  Inl int(11) default NULL,
  PhonStrsDISC varchar(50) default NULL,
  PhonCVBr varchar(150) default NULL,
  PhonSylBCLX varchar(150) default NULL,
  PRIMARY KEY  (IdNum)
) ENGINE=MyISAM; 

/*Table structure for table abbreviations */

CREATE TABLE cxnl_abbreviation (
  AbbrId int(11) NOT NULL,
  Abbr varchar(50) NOT NULL,
  AbbrDia varchar(50) default NULL,
  Meaning varchar(200) default NULL,
  Inl int(11) default NULL,
  InlDev float default NULL,
  InlMln float default NULL,
  InlLog float default NULL,
  OrthoCnt int(11) default NULL,
  PRIMARY KEY  (AbbrId)
) ENGINE=MyISAM;

CREATE INDEX IX_CXNL_Abbr ON cxnl_abbreviation(Abbr);

/*Table structure for table corpustype */

CREATE TABLE cxnl_corpustype (
   Type varchar(50) NOT NULL,	
   Freq int(11) default NULL,
   Disp int(11) default NULL,
   Indet varchar(1) default NULL,
   Status varchar(1) default NULL,
  PRIMARY KEY  (Type)
) ENGINE=MyISAM; 


/*Table structure for table syllablefrequency */

CREATE TABLE cxnl_syllablefrequency (
  Syllable varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  SylPos int(11) default NULL,
  SylInlMln int(11) default NULL,
  SylTotInlMln int(11) default NULL,
  PRIMARY KEY  (Syllable,SylPos)
) ENGINE=MyISAM; 

/*Table structure for table wordformmorphology */

CREATE TABLE cxnl_wordformmorphology (
  IdNum int(11) NOT NULL,
  Inl int(11) default NULL,
  FlectType varchar(50) default NULL,
  PRIMARY KEY  (IdNum)
) ENGINE=MyISAM; 

