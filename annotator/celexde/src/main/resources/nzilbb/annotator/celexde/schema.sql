/*-------------------------*/

/*Table structure for table lemma */

CREATE TABLE cxde_lemma (
  IdNumLemma int(11) NOT NULL,
  HeadDia varchar(50) default NULL,
  Mann int(11) default NULL,
  HeadSylDia varchar(50) default NULL,
  HeadSylChg varchar(1) default NULL,
  StemDia varchar(50) default NULL,
  StemSylDia varchar(50) default NULL,
  StemSylChg varchar(1) default NULL,
  PRIMARY KEY  (IdNumLemma)
/*,  KEY IX_HeadDia (HeadDia)*/
) ENGINE=MyISAM;

CREATE INDEX IX_CXDE_HeadDia ON cxde_lemma(HeadDia);

/*Table structure for table lemmaphonology */

CREATE TABLE cxde_lemmaphonology (
  IdNumLemma int(11) NOT NULL,
  Mann int(11) default NULL,
  PhonStrsDISC varchar(60) default NULL,
  PhonSylBCLX varchar(60) default NULL,
  PhonStrsStDISC varchar(60) default NULL,
  PhonSylStBCLX varchar(60) default NULL,
  PhonCVBr varchar(60) default NULL,
  PhonStCVBr varchar(60) default NULL,
  PhonolSAM varchar(60) default NULL,
  PhonolCLX varchar(60) default NULL,
  PRIMARY KEY  (IdNumLemma)
) ENGINE=MyISAM; 

/*Table structure for table lemmamorphology */

CREATE TABLE cxde_lemmamorphology (
  IdNumLemma int(11) NOT NULL,
  Mann int(11) default NULL,
  MorphStatus varchar(50) default NULL,
  PRIMARY KEY  (IdNumLemma)
) ENGINE=MyISAM; 

/*Table structure for table lemmamorphologyparse */

CREATE TABLE cxde_lemmamorphologyparse (
  IdNumLemma int(11) NOT NULL,
  Variant int(11) NOT NULL,
  DerComp varchar(1) default NULL,
  Comp varchar(1) default NULL,
  Def varchar(1) default NULL,
  Imm varchar(50) default NULL,
  ImmClass varchar(4) default NULL,
  ImmAllo varchar(1) default NULL,
  ImmOpac varchar(1) default NULL,
  ImmUml varchar(1) default NULL,
  StrucLab varchar(150) default NULL,
  StrucAllo varchar(1) default NULL,
  StrucOpac varchar(1) default NULL,
  StrucUml varchar(1) default NULL,
  Sepa varchar(1) default NULL,
  InflPar varchar(7) default NULL,
  InflVar varchar(1) default NULL,
  PRIMARY KEY  (IdNumLemma,Variant)
) ENGINE=MyISAM; 

/*Table structure for table lemmafrequency */

CREATE TABLE cxde_lemmafrequency (
  IdNumLemma int(11) NOT NULL,
  Mann int(11) default NULL,
  MannDev float default NULL,
  MannMln float default NULL,
  MannLog float default NULL,
  MannW float default NULL,
  MannWMln float default NULL,
  MannWLog float default NULL,
  MannS float default NULL,
  MannSMln float default NULL,
  MannSLog float default NULL,
  PRIMARY KEY  (IdNumLemma)
) ENGINE=MyISAM; 


/*Table structure for table lemmasyntax */

CREATE TABLE cxde_lemmasyntax (
  IdNumLemma int(11) NOT NULL,
  Mann int(11) default NULL,
  ClassNum int(11) default NULL,
  GendNum varchar(3) default NULL,
  PropNum varchar(1) default NULL,
  SingTant varchar(1) default NULL,
  PlurTant varchar(1) default NULL,
  AuxNum varchar(2) default NULL,
  SubClassVNum varchar(3) default NULL,
  CompComp varchar(160) default NULL,
  CompEsSubj varchar(1) default NULL,
  CompSubj varchar(1) default NULL,
  CompAcc varchar(1) default NULL,
  CompSecAcc varchar(1) default NULL,
  CompDat varchar(1) default NULL,
  CompGen varchar(1) default NULL,
  CompPrep varchar(1) default NULL,
  CompSecPrep varchar(1) default NULL,
  CompAdv varchar(1) default NULL,
  Grad varchar(3) default NULL,
  CardOrdNum varchar(1) default NULL,
  SubClassPNum varchar(1) default NULL,
  `Case` varchar(2) default NULL,
  PRIMARY KEY  (IdNumLemma)
) ENGINE=MyISAM; 

/*Table structure for table wordform */

CREATE TABLE cxde_wordform (
  IdNum int(11) NOT NULL,
  IdNumLemma int(11) NOT NULL,
  Mann int(11) default NULL,
  WordDia varchar(50) default NULL,
  WordSylDia varchar(50) default NULL,
  WordSylChg varchar(1) default NULL,
  PRIMARY KEY  (IdNum)
/*,  KEY IX_IdNumLemma (IdNumLemma),*/
/*  KEY IX_WordDia (WordDia)*/
) ENGINE=MyISAM; 

CREATE INDEX IX_CXDE_IdNumLemma ON cxde_wordform(IdNumLemma);
CREATE INDEX IX_CXDE_WordDia ON cxde_wordform(WordDia);

/*Table structure for table wordformphonology */

CREATE TABLE cxde_wordformphonology (
  IdNum int(11) NOT NULL,
  Mann int(11) default NULL,
  PhonStrsDISC varchar(50) default NULL,
  PhonSylBCLX varchar(60) default NULL,
  PhonCVBr varchar(60) default NULL,
  PRIMARY KEY  (IdNum)
) ENGINE=MyISAM; 

/*Table structure for table wordformfrequency */

CREATE TABLE cxde_wordformfrequency (
  IdNum int(11) NOT NULL,
  Mann int(11) default NULL,
  MannDev float default NULL,
  MannMln float default NULL,
  MannLog float default NULL,
  MannW float default NULL,
  MannWMln float default NULL,
  MannWLog float default NULL,
  MannS float default NULL,
  MannSMln float default NULL,
  MannSLog float default NULL,
  PRIMARY KEY  (IdNum)
) ENGINE=MyISAM; 

CREATE TABLE cxde_gender (
  GendNum int(11) NOT NULL,
  Label varchar(3) NOT NULL default '',
  Gend varchar(100) NOT NULL default '',
  PRIMARY KEY  (GendNum)
) ENGINE=MyISAM; 

INSERT INTO cxde_gender VALUES (1,'M','Masculine'),(2,'F','Feminine'),(3,'N','Neuter'),(12,'MF','Masculine/Feminine'),(13,'MN','Masculine/Neuter'),(21,'FM','Feminine/Masculine'),(213,'FMN','Feminine/Masculine/Neuter'),(23,'FN','Feminine/Neuter'),(31,'NM','Neuter/Masculine'),(32,'NF','Neuter/F');


CREATE TABLE cxde_propernountype (
  PropNum int(11) NOT NULL,
  Label varchar(1) NOT NULL default '',
  Prop varchar(100) NOT NULL default '',
  PRIMARY KEY  (PropNum)
) ENGINE=MyISAM; 

INSERT INTO cxde_propernountype VALUES (1,'G','Geographical names'),(2,'P','Names of people'),(3,'B','Company or product names');

CREATE TABLE cxde_auxiliary (
  AuxNum int(11) NOT NULL,
  Label varchar(10) NOT NULL default '',
  Aux varchar(100) NOT NULL default '',
  PRIMARY KEY  (AuxNum)
) ENGINE=MyISAM; 

INSERT INTO cxde_auxiliary VALUES (1,'haben','haben'),(2,'sein','sein'),(12,'haben/sein','haben or sein');

CREATE TABLE cxde_subclass (
  SubClassVNum int(11) NOT NULL,
  Label varchar(1) NOT NULL default '',
  SubClassV varchar(100) NOT NULL default '',
  PRIMARY KEY  (SubClassVNum)
) ENGINE=MyISAM; 

INSERT INTO cxde_subclass VALUES (1,'a','Auxiliary verb'),(2,'c','Copula'),(3,'i','Impersonal verb'),(4,'m','Modal verb'),(5,'l','Lexical verb'),(6,'r','Reflexive verb');

CREATE TABLE cxde_cardord (
  CardOrdNum int(11) NOT NULL,
  Label varchar(14) NOT NULL default '',
  PRIMARY KEY  (CardOrdNum)
) ENGINE=MyISAM; 

INSERT INTO cxde_cardord VALUES (1,'cardinal'),(2,'ordinal'),(3,'fraction'),(4,'classificatory'),(5,'multiplicative');

CREATE TABLE cxde_pronounsubclass (
  SubClassPNum int(11) NOT NULL,
  Label varchar(13) NOT NULL default '',
  SubClassP varchar(100) NOT NULL default '',
  PRIMARY KEY  (SubClassPNum)
) ENGINE=MyISAM; 

INSERT INTO cxde_pronounsubclass VALUES (1,'personal','Personal'),(2,'demonstative','Demonstative'),(3,'possessive','Possessive'),(4,'relative','Relative'),(5,'interrogative','Interrogative'),(6,'reflexive','Reflexive'),(7,'reciprocal','Reciprocal'),(8,'indefinite','Indefinite');

/*Table structure for table wordclass */

CREATE TABLE cxde_wordclass (
  ClassNum int(11) NOT NULL,
  Label varchar(4) NOT NULL default '',
  Class varchar(100) NOT NULL default '',
  PRIMARY KEY  (ClassNum)
) ENGINE=MyISAM; 

INSERT INTO cxde_wordclass VALUES (1,'N','Noun'),(2,'A','Adjective'),(3,'NUM','Numeral'),(4,'V','Verb'),(5,'ART','Article'),(6,'PRON','Pronoun'),(7,'ADV','Adverb'),(8,'PREP','Preposition'),(9,'C','Conjunction'),(10,'I','Interjection'),(11,'SCON','Single Contraction'),(12,'CCON','Complex Contraction');

/*Table structure for table wordformmorphology */

CREATE TABLE cxde_wordformmorphology (
  IdNum int(11) NOT NULL,
  Mann int(11) default NULL,
  FlectType varchar(50) default NULL,
  FlectTypeDescription varchar(300) default NULL,
  PRIMARY KEY  (IdNum)
) ENGINE=MyISAM; 
