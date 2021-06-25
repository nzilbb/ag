//
// Copyright 2021 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.annotator.mor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.script.ScriptException;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.SerializationParametersMissingException;
import nzilbb.ag.serialize.SerializerNotConfiguredException;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.util.DefaultOffsetGenerator;
import nzilbb.ag.util.Normalizer;
import nzilbb.configure.ParameterSet;
import nzilbb.editpath.*;
import nzilbb.formatter.clan.ChatSerialization;
import nzilbb.util.Execution;
import nzilbb.util.IO;
import nzilbb.util.ISO639;

/**
 * Annotator that annotates words tags from the the Talk Bank 
 * <a href="https://talkbank.org/manuals/MOR.pdf"> MOR </a> 
 * morphosyntactic tagger.
 */
@UsesFileSystem
public class MorTagger extends Annotator {
  /** Get the minimum version of the nzilbb.ag API supported by the annotator.*/
  public String getMinimumApiVersion() { return "1.0.3"; }
  
  /**
   * Runs any processing required to uninstall the annotator.
   * <p> In this case, the table created in rdbConnectionFactory() is DROPped.
   */
  @Override
  public void uninstall() {
  }
  
  /**
   * Provides the overall configuration of the annotator. 
   * @return The overall configuration of the annotator, which will be passed to the
   * <i> config/index.html </i> configuration web-app, if any. This configuration may be
   * null, or a string that serializes the annotators configuration state in any encoding
   * the implementor prefers. The resulting string must be interpretable by the
   * <i> config/index.html </i> web-app. 
   * @see #setConfig(String)
   * @see #beanPropertiesToQueryString()
   */
  public String getConfig() {
    return null;
  }
  
  /**
   * The MOR executable file.
   * @return The MOR command-line program.
   */
  public File getMorExe() {
    return new File(getWorkingDirectory(), "mor"); // TODO or exe?
  } // end of getMorExe()

  /**
   * The POST executable file.
   * @return The POST command-line program.
   */
  public File getPostExe() {
    return new File(getWorkingDirectory(), "post"); // TODO or exe?
  } // end of getMorExe()
   
  /**
   * Builds the MOR, and installs the default english grammar.
   * <p> If there is no <i> mor </i> program file, the program is compiled from the source
   * code in the <i> unix-clan </i> directory. If no <i> unix-clan.zip </i> file
   * containing the source code has been uploaded by the user, it is downloaded from 
   * <a href="https://dali.talkbank.org/clan/unix-clan.zip">
   * https://dali.talkbank.org/clan/unix-clan.zip</a> and then <i> mor </i> is compiled. 
   * <p> If there are no other directories, then a default English grammar is downloaded from:
   * <a href="https://talkbank.org/morgrams/eng.zip">https://talkbank.org/morgrams/eng.zip</a>
   * and unzipped.
   * @throws InvalidConfigurationException
   * @see #getConfig()
   * @see #beanPropertiesFromQueryString(String)
   */ 
  public void setConfig(String config) throws InvalidConfigurationException {
    running = true;
    setPercentComplete(0);
    setStatus(""); // clear any residual status from the last run...

    try {

      // are the "mor" and "post" programs present?
      File mor = getMorExe();
      File post = getPostExe();
      if (!mor.exists() || !post.exists()) {     
        // if not, is there a source code directory?
        File unixClan = new File(getWorkingDirectory(), "unix-clan");
        // will need these later...
        File src = new File(unixClan, "src");
        File makefile = new File(src, "makefile");
        
        if (!unixClan.exists()) {
          // if not, get source code
          File zip = new File(getWorkingDirectory(), "unix-clan.zip");
          if (!zip.exists()) { // download it
            String url = "https://dali.talkbank.org/clan/unix-clan.zip";
            setStatus("Downloading " + url);
            IO.SaveUrlToFile​(
              new URL(url), zip,
              new IntConsumer() { public void accept​(int value) {
                setPercentComplete(value / 5); }});
          }
          setStatus("Upzipping " + zip.getName());
          IO.Unzip(
            zip, getWorkingDirectory(),
            new IntConsumer() { public void accept​(int value) {
              setPercentComplete(20 + value / 5); }});
          if (!unixClan.exists()) {
            throw new InvalidConfigurationException(
              this, "Zip file '"+zip.getName()
              +"' does not contain source folder '"+unixClan.getName()+"'");
          }
          
          // prepend makefile with CFLAGS definition
          // (but only if we've just unzipped the source code, so that it's possible to
          //  manually edit the makefile to get things working)
          setStatus("Configuring makefile...");
          //BufferedReader in = new BufferedReader(new FileReader(makefile));
          File editedMakefile = new File(src, "makefile-edited");
          FileOutputStream out = new FileOutputStream(editedMakefile);
          // although the makefile includes some pre-baked lines we could un-comment,
          // POST compiles but doesn't run on 64-bit systems without the -m32 switch
          // so we add our own definition of CFLAGS instead, based on the "4.4.1-4ubuntu9" config
          out.write("CC = g++\n".getBytes());
          out.write(
            "CFLAGS = -DUNX -Wno-deprecated -Wno-deprecated-declarations -m32\n".getBytes());
          // now copy the rest of the makefiles
          IO.Pump(new FileInputStream(makefile), out);
          out.close();
          makefile.delete();
          editedMakefile.renameTo(makefile);
        } // unix-clan directory doesn't exist
        
        // build the source code

        setPercentComplete(40);
        // in src "make mor"
        setStatus("Running 'make mor'...");
        Execution make = new Execution()
          .setWorkingDirectory(src)
          .setExe("make")
          .arg("mor");
        make.run();
        setStatus(make.stdout());
        if (make.stderr().length() > 0) {
          setStatus("ERROR: " + make.stderr());
          System.out.println(make.stderr());
          // fall through to compiledMor check - maybe stderr output isn't fatal...
        }

        // then "make post"
        setStatus("Running 'make post'...");
        make = new Execution()
          .setWorkingDirectory(src)
          .setExe("make")
          .arg("post");
        make.run();
        setStatus(make.stdout());
        if (make.stderr().length() > 0) {
          setStatus("ERROR: " + make.stderr());
          System.out.println(make.stderr());
          // fall through to compiledMor check - maybe stderr output isn't fatal...
        }
        
        // copy mor to base directory
        File compiledMor = new File(new File(new File(unixClan, "unix"), "bin"), "mor");
        if (!compiledMor.exists()) {
          throw new InvalidConfigurationException(
            this, "Compilation failed to create "+compiledMor.getPath());
        }
        if (!compiledMor.renameTo(mor)) {
            throw new InvalidConfigurationException(
              this, "Could not install compiled '"+mor.getName()+"'");
        }
        // copy post to base directory
        File compiledPost = new File(new File(new File(unixClan, "unix"), "bin"), "post");
        if (!compiledPost.exists()) {
          throw new InvalidConfigurationException(
            this, "Compilation failed to create "+compiledPost.getPath());
        }
        if (!compiledPost.renameTo(post)) {
            throw new InvalidConfigurationException(
              this, "Could not install compiled '"+post.getName()+"'");
        }
      } // mor didn't exist
      setPercentComplete(60);

      // is there at least one grammar?
      List<String> grammars = availableGrammars();
      if (grammars.size() <= 0) {
        // if not, download one
        File zip = new File(getWorkingDirectory(), "eng.zip");
        String url = "https://talkbank.org/morgrams/eng.zip";
        setStatus("Downloading " + url);
        IO.SaveUrlToFile​(
          new URL(url), zip,
          new IntConsumer() { public void accept​(int value) {
            setPercentComplete(60 + value / 5); }});
        // unzip it
        IO.Unzip(
          zip, getWorkingDirectory(),
          new IntConsumer() { public void accept​(int value) {
            setPercentComplete(80 + value / 4); }});
      }
      grammars = availableGrammars();
      if (grammars.size() <= 0) {
        throw new InvalidConfigurationException(
          this, "Could not install default grammar from 'eng.zip'");
      }
      
      setStatus("Finished.");
      setPercentComplete(100);
    } catch (IOException x) {
      throw new InvalidConfigurationException(this, x);
    } finally {
      running = false;
    }
  }
  
  /**
   * Takes a file to be used instead of the built-in copy of cmudict.txt
   * @param file The lexicon file.
   * @return null if upload was successful, an error message otherwise.
   */
  public String uploadZip(File file) {
    if (!file.getName().endsWith(".zip")) {
      return file.getName() + " is not a .zip file.";
    }

    try {
      IO.Unzip(file, getWorkingDirectory());
    } catch(IOException exception) {
      return "Could not unzip " + file.getName() + ": " + exception.getMessage();
    }
    return null;
  } // end of uploadLexicon()
  
  /**
   * Lists the grammars that are available for use.
   * @return A list of file names that can be selected.
   */
  public List<String> availableGrammars() {
    return Arrays.stream(
      getWorkingDirectory().listFiles(new FileFilter() {
          public boolean accept(File f) {
            return f.isDirectory() && !f.getName().equals("unix-clan");
          }
        })).map(f->f.getName())
      .collect(Collectors.toList());
  } // end of availableGrammars()
  
  /**
   * ID of the layer that determines the language of the whole transcript.
   * @see #getLanguagesLayerId()
   * @see #setLanguagesLayerId(String)
   */
  protected String languagesLayerId;
  /**
   * Getter for {@link #languagesLayerId}: ID of the layer that determines the
   * language of the whole transcript. 
   * @return ID of the layer that determines the language of the whole transcript.
   */
  public String getLanguagesLayerId() { return languagesLayerId; }
  /**
   * Setter for {@link #languagesLayerId}: ID of the layer that determines the
   * language of the whole transcript. 
   * @param newLanguagesLayerId ID of the layer that determines the language of
   * the whole transcript. 
   */
  public MorTagger setLanguagesLayerId(String newLanguagesLayerId) {
    if (newLanguagesLayerId != null // empty string means null
        && newLanguagesLayerId.trim().length() == 0) {
      newLanguagesLayerId = null;
    }
    languagesLayerId = newLanguagesLayerId;
    return this;
  }
  
  /**
   * ID of the word tokens input layer.
   * @see #getTokenLayerId()
   * @see #setTokenLayerId(String)
   */
  protected String tokenLayerId;
  /**
   * Getter for {@link #tokenLayerId}: ID of the word tokens input layer.
   * @return ID of the word tokens input layer.
   */
  public String getTokenLayerId() { return tokenLayerId; }
  /**
   * Setter for {@link #tokenLayerId}: ID of the word tokens input layer.
   * @param newTokenLayerId ID of the word tokens input layer.
   */
  public MorTagger setTokenLayerId(String newTokenLayerId) { tokenLayerId = newTokenLayerId; return this; }
  
  /**
   * ID of the output layer.
   * @see #getMorLayerId()
   * @see #setMorLayerId(String)
   */
  protected String morLayerId;
  /**
   * Getter for {@link #morLayerId}: ID of the output layer.
   * @return ID of the output layer.
   */
  public String getMorLayerId() { return morLayerId; }
  /**
   * Setter for {@link #morLayerId}: ID of the output layer.
   * @param newMorLayerId ID of the output layer.
   */
  public MorTagger setMorLayerId(String newMorLayerId) {
    if (newMorLayerId != null // empty string means null
        && newMorLayerId.trim().length() == 0) {
      newMorLayerId = null;
    }
    morLayerId = newMorLayerId;
    return this;
  }
  
  /**
   * Split alternative MOR taggings into separate annotations.
   * @see #getSplitMorTagGroups()
   * @see #setSplitMorTagGroups(boolean)
   */
  protected boolean splitMorTagGroups = true;
  /**
   * Getter for {@link #splitMorTagGroups}: Split alternative MOR taggings into separate
   * annotations. 
   * @return Split alternative MOR taggings into separate annotations.
   */
  public boolean getSplitMorTagGroups() { return splitMorTagGroups; }
  /**
   * Setter for {@link #splitMorTagGroups}: Split alternative MOR taggings into separate
   * annotations. 
   * @param newSplitMorTagGroups Split alternative MOR taggings into separate annotations.
   */
  public MorTagger setSplitMorTagGroups(boolean newSplitMorTagGroups) { splitMorTagGroups = newSplitMorTagGroups; return this; }
  
  /**
   * Split MOR word groups into separate annotations.
   * @see #getSplitMorWordGroups()
   * @see #setSplitMorWordGroups(boolean)
   */
  protected boolean splitMorWordGroups = true;
  /**
   * Getter for {@link #splitMorWordGroups}: Split MOR word groups into separate annotations.
   * @return Split MOR word groups into separate annotations.
   */
  public boolean getSplitMorWordGroups() { return splitMorWordGroups; }
  /**
   * Setter for {@link #splitMorWordGroups}: Split MOR word groups into separate annotations.
   * @param newSplitMorWordGroups Split MOR word groups into separate annotations.
   */
  public MorTagger setSplitMorWordGroups(boolean newSplitMorWordGroups) { splitMorWordGroups = newSplitMorWordGroups; return this; }
  
  /**
   * Layer for prefixes in MOR tags.
   * @see #getPrefixLayerId()
   * @see #setPrefixLayerId(String)
   */
  protected String prefixLayerId;
  /**
   * Getter for {@link #prefixLayerId}: Layer for prefixes in MOR tags.
   * @return Layer for prefixes in MOR tags.
   */
  public String getPrefixLayerId() { return prefixLayerId; }
  /**
   * Setter for {@link #prefixLayerId}: Layer for prefixes in MOR tags.
   * @param newPrefixLayerId Layer for prefixes in MOR tags.
   */
  public MorTagger setPrefixLayerId(String newPrefixLayerId) {
    if (newPrefixLayerId != null // empty string means null
        && newPrefixLayerId.trim().length() == 0) {
      newPrefixLayerId = null;
    }
    prefixLayerId = newPrefixLayerId;
    return this;
  }
  
  /**
   * Layer for parts-of-speech in MOR tags.
   * @see #getPartOfSpeechLayerId()
   * @see #setPartOfSpeechLayerId(String)
   */
  protected String partOfSpeechLayerId;
  /**
   * Getter for {@link #partOfSpeechLayerId}: Layer for parts-of-speech in MOR tags.
   * @return Layer for parts-of-speech in MOR tags.
   */
  public String getPartOfSpeechLayerId() { return partOfSpeechLayerId; }
  /**
   * Setter for {@link #partOfSpeechLayerId}: Layer for parts-of-speech in MOR tags.
   * @param newPartOfSpeechLayerId Layer for parts-of-speech in MOR tags.
   */
  public MorTagger setPartOfSpeechLayerId(String newPartOfSpeechLayerId) {
    if (newPartOfSpeechLayerId != null // empty string means null
        && newPartOfSpeechLayerId.trim().length() == 0) {
      newPartOfSpeechLayerId = null;
    }
    partOfSpeechLayerId = newPartOfSpeechLayerId;
    return this;
  }
  
  /**
   * Layer for part-of-speech subcategories in MOR tags.
   * @see #getPartOfSpeechSubcategoryLayerId()
   * @see #setPartOfSpeechSubcategoryLayerId(String)
   */
  protected String partOfSpeechSubcategoryLayerId;
  /**
   * Getter for {@link #partOfSpeechSubcategoryLayerId}: Layer for part-of-speech
   * subcategories in MOR tags. 
   * @return Layer for part-of-speech subcategories in MOR tags.
   */
  public String getPartOfSpeechSubcategoryLayerId() { return partOfSpeechSubcategoryLayerId; }
  /**
   * Setter for {@link #partOfSpeechSubcategoryLayerId}: Layer for part-of-speech
   * subcategories in MOR tags. 
   * @param newPartOfSpeechSubcategoryLayerId Layer for part-of-speech subcategories in MOR tags.
   */
  public MorTagger setPartOfSpeechSubcategoryLayerId(String newPartOfSpeechSubcategoryLayerId) {
    if (newPartOfSpeechSubcategoryLayerId != null // empty string means null
        && newPartOfSpeechSubcategoryLayerId.trim().length() == 0) {
      newPartOfSpeechSubcategoryLayerId = null;
    }
    partOfSpeechSubcategoryLayerId = newPartOfSpeechSubcategoryLayerId;
    return this;
  }
  
  /**
   * Layer for stems in MOR tags.
   * @see #getStemLayerId()
   * @see #setStemLayerId(String)
   */
  protected String stemLayerId;
  /**
   * Getter for {@link #stemLayerId}: Layer for stems in MOR tags.
   * @return Layer for stems in MOR tags.
   */
  public String getStemLayerId() { return stemLayerId; }
  /**
   * Setter for {@link #stemLayerId}: Layer for stems in MOR tags.
   * @param newStemLayerId Layer for stems in MOR tags.
   */
  public MorTagger setStemLayerId(String newStemLayerId) {
    if (newStemLayerId != null // empty string means null
        && newStemLayerId.trim().length() == 0) {
      newStemLayerId = null;
    }
    stemLayerId = newStemLayerId;
    return this;
  }
  
  /**
   * Layer for fusional suffixes in MOR tags.
   * @see #getFusionalSuffixLayerId()
   * @see #setFusionalSuffixLayerId(String)
   */
  protected String fusionalSuffixLayerId;
  /**
   * Getter for {@link #fusionalSuffixLayerId}: Layer for fusional suffixes in MOR tags.
   * @return Layer for fusional suffixes in MOR tags.
   */
  public String getFusionalSuffixLayerId() { return fusionalSuffixLayerId; }
  /**
   * Setter for {@link #fusionalSuffixLayerId}: Layer for fusional suffixes in MOR tags.
   * @param newFusionalSuffixLayerId Layer for fusional suffixes in MOR tags.
   */
  public MorTagger setFusionalSuffixLayerId(String newFusionalSuffixLayerId) {
    if (newFusionalSuffixLayerId != null // empty string means null
        && newFusionalSuffixLayerId.trim().length() == 0) {
      newFusionalSuffixLayerId = null;
    }
    fusionalSuffixLayerId = newFusionalSuffixLayerId;
    return this;
  }
  
  /**
   * Layer for (non-fusional) suffixes in MOR tags.
   * @see #getSuffixLayerId()
   * @see #setSuffixLayerId(String)
   */
  protected String suffixLayerId;
  /**
   * Getter for {@link #suffixLayerId}: Layer for (non-fusional) suffixes in MOR tags.
   * @return Layer for (non-fusional) suffixes in MOR tags.
   */
  public String getSuffixLayerId() { return suffixLayerId; }
  /**
   * Setter for {@link #suffixLayerId}: Layer for (non-fusional) suffixes in MOR tags.
   * @param newSuffixLayerId Layer for (non-fusional) suffixes in MOR tags.
   */
  public MorTagger setSuffixLayerId(String newSuffixLayerId) {
    if (newSuffixLayerId != null // empty string means null
        && newSuffixLayerId.trim().length() == 0) {
      newSuffixLayerId = null;
    }
    suffixLayerId = newSuffixLayerId;
    return this;
  }
  
  /**
   * Layer for English glosses in MOR tags.
   * @see #getGlossLayerId()
   * @see #setGlossLayerId(String)
   */
  protected String glossLayerId;
  /**
   * Getter for {@link #glossLayerId}: Layer for English glosses in MOR tags.
   * @return Layer for English glosses in MOR tags.
   */
  public String getGlossLayerId() { return glossLayerId; }
  /**
   * Setter for {@link #glossLayerId}: Layer for English glosses in MOR tags.
   * @param newGlossLayerId Layer for English glosses in MOR tags.
   */
  public MorTagger setGlossLayerId(String newGlossLayerId) {
    if (newGlossLayerId != null // empty string means null
        && newGlossLayerId.trim().length() == 0) {
      newGlossLayerId = null;
    }
    glossLayerId = newGlossLayerId;
    return this;
  }
  
  /**
   * Sets the configuration for a given annotation task.
   * @param parameters The configuration of the annotator; a value of <tt> null </tt>
   * will apply the default task parameters, with the {@link #morLayerId} set to <q>mor</q>.
   * @throws InvalidConfigurationException
   */
  public void setTaskParameters(String parameters) throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");

    if (parameters == null) { // apply default configuration
      
      try {
        // default input layer
        if (schema.getLayer("orthography") != null) {
          tokenLayerId = "orthography";
        } else {
          tokenLayerId = schema.getWordLayerId();
        }
        
        // default transcript language layer
        Layer[] candidates = schema.getMatchingLayers(
          "layer.parentId == schema.root.id && layer.alignment == 0" // transcript attribute
          +" && /.*lang.*/.test(layer.id)"); // with 'lang' in the name
        if (candidates.length > 0) languagesLayerId = candidates[0].getId();
        
        // mor layer
        candidates = schema.getMatchingLayers(
          "layer.parentId == schema.wordLayerId"
          +" && (/^[Mm][Oo][Rr]$/.test(layer.id) || /^[Mm]orpho[Ss]yntax$/.test(layer.id))");
        if (candidates.length > 0) {
          morLayerId = candidates[0].getId();
        } else { // suggest adding a new one
          morLayerId = "mor";
        }

        // prefix layer
        candidates = schema.getMatchingLayers(
          "layer.parentId == schema.wordLayerId"
          +" && (/.*[Pp]refix.*/.test(layer.id))");
        if (candidates.length > 0) {
          prefixLayerId = candidates[0].getId();
        } else { // suggest adding a new one
          prefixLayerId = "morPrefix";
        }

        // pos layer
        candidates = schema.getMatchingLayers(
          "layer.parentId == schema.wordLayerId" // word tag
          +" && (/.*POS.*/.test(layer.id) || /.*pos.*/.test(layer.id)"
          +" || /.*[Pp]art.*[Oo]f.*[Ss]peech.*/.test(layer.id))");
        if (candidates.length > 0) {
          partOfSpeechLayerId = candidates[0].getId();
        } else { // suggest adding a new one
          partOfSpeechLayerId = "morPOS";
        }

        // pos subcategory layer
        candidates = schema.getMatchingLayers(
          "layer.parentId == schema.wordLayerId" // word tag
          +" && (/.*[Ss]ubcategory.*/.test(layer.id))");
        if (candidates.length > 0) {
          partOfSpeechSubcategoryLayerId = candidates[0].getId();
        } else { // suggest adding a new one
          partOfSpeechSubcategoryLayerId = "morPOSSubcategory";
        }

        // stem layer
        candidates = schema.getMatchingLayers(
          "layer.parentId == schema.wordLayerId" // word tag
          +" && (/.*[Ss]tem.*/.test(layer.id))");
        if (candidates.length > 0) {
          stemLayerId = candidates[0].getId();
        } else { // suggest adding a new one
          stemLayerId = "morStem";
        }

        // fusional suffix layer
        candidates = schema.getMatchingLayers(
          "layer.parentId == schema.wordLayerId" // word tag
          +" && (/.*[Ff]usional.*[Ss]uffix.*/.test(layer.id))");
        if (candidates.length > 0) {
          fusionalSuffixLayerId = candidates[0].getId();
        } else { // suggest adding a new one
          fusionalSuffixLayerId = "morFusionalSuffix";
        }

        // suffix layer
        candidates = schema.getMatchingLayers(
          "layer.parentId == schema.wordLayerId" // word tag
          +" && (/.*[Ss]uffix.*/.test(layer.id))");
        if (candidates.length > 0) {
          suffixLayerId = candidates[0].getId();
        } else { // suggest adding a new one
          suffixLayerId = "morSuffix";
        }

        // gloss layer
        candidates = schema.getMatchingLayers(
          "layer.parentId == schema.wordLayerId" // word tag
          +" && (/.*[Gg]loss.*/.test(layer.id) || /.*[Ee]nglish.*/.test(layer.id))");
        if (candidates.length > 0) {
          glossLayerId = candidates[0].getId();
        } else { // suggest adding a new one
          glossLayerId = "morGloss";
        }
        

      } catch(ScriptException impossible) {}
      
    } else {
      beanPropertiesFromQueryString(parameters);
    }
    
    if (tokenLayerId == null) {
      throw new InvalidConfigurationException(
        this, "Word token layer not set.");
    } else if (schema.getLayer(tokenLayerId) == null)
      throw new InvalidConfigurationException(
        this, "Word token layer not found: " + tokenLayerId);
    
    if (languagesLayerId != null && schema.getLayer(languagesLayerId) == null) 
      throw new InvalidConfigurationException(
        this, "Transcript language layer not found: " + languagesLayerId);
      
    // do the output layers need to be added to the schema?

    // mor
    if (morLayerId != null) {
      Layer layer = schema.getLayer(morLayerId);
      if (layer == null) {
        schema.addLayer(
          new Layer(morLayerId)
          .setAlignment(Constants.ALIGNMENT_INTERVAL)
          .setPeers(true)
          .setParentId(schema.getWordLayerId()));
      } else {
        if (morLayerId.equals(schema.getWordLayerId())
            || morLayerId.equals(languagesLayerId)) {
          throw new InvalidConfigurationException(this, "Invalid MOR layer: " + morLayerId);
        }
      }
    }

    // prefix
    if (prefixLayerId != null) {
      Layer layer = schema.getLayer(prefixLayerId);
      if (layer == null) {
        schema.addLayer(
          new Layer(prefixLayerId)
          .setAlignment(Constants.ALIGNMENT_INTERVAL)
          .setPeers(true)
          .setParentId(schema.getWordLayerId()));
      } else {
        if (prefixLayerId.equals(schema.getWordLayerId())
            || prefixLayerId.equals(languagesLayerId)) {
          throw new InvalidConfigurationException(this, "Invalid prefix layer: " + prefixLayerId);
        }
      }
    }

    // POS
    if (partOfSpeechLayerId != null) {
      Layer layer = schema.getLayer(partOfSpeechLayerId);
      if (layer == null) {
        schema.addLayer(
          new Layer(partOfSpeechLayerId)
          .setAlignment(Constants.ALIGNMENT_INTERVAL)
          .setPeers(true)
          .setParentId(schema.getWordLayerId()));
      } else {
        if (partOfSpeechLayerId.equals(schema.getWordLayerId())
            || partOfSpeechLayerId.equals(languagesLayerId)) {
          throw new InvalidConfigurationException(
            this, "Invalid POS layer: " + partOfSpeechLayerId);
        }
      }
    }

    // POS subcategory
    if (partOfSpeechSubcategoryLayerId != null) {
      Layer layer = schema.getLayer(partOfSpeechSubcategoryLayerId);
      if (layer == null) {
        schema.addLayer(
          new Layer(partOfSpeechSubcategoryLayerId)
          .setAlignment(Constants.ALIGNMENT_INTERVAL)
          .setPeers(true)
          .setParentId(schema.getWordLayerId()));
      } else {
        if (partOfSpeechSubcategoryLayerId.equals(schema.getWordLayerId())
            || partOfSpeechSubcategoryLayerId.equals(languagesLayerId)) {
          throw new InvalidConfigurationException(
            this, "Invalid POS Subcategory layer: " + partOfSpeechSubcategoryLayerId);
        }
      }
    }

    // stem
    if (stemLayerId != null) {
      Layer layer = schema.getLayer(stemLayerId);
      if (layer == null) {
        schema.addLayer(
          new Layer(stemLayerId)
          .setAlignment(Constants.ALIGNMENT_INTERVAL)
          .setPeers(true)
          .setParentId(schema.getWordLayerId()));
      } else {
        if (stemLayerId.equals(schema.getWordLayerId())
            || stemLayerId.equals(languagesLayerId)) {
          throw new InvalidConfigurationException(this, "Invalid Stem layer: " + stemLayerId);
        }
      }
    }

    // fusional suffix
    if (fusionalSuffixLayerId != null) {
      Layer layer = schema.getLayer(fusionalSuffixLayerId);
      if (layer == null) {
        schema.addLayer(
          new Layer(fusionalSuffixLayerId)
          .setAlignment(Constants.ALIGNMENT_INTERVAL)
          .setPeers(true)
          .setParentId(schema.getWordLayerId()));
      } else {
        if (fusionalSuffixLayerId.equals(schema.getWordLayerId())
            || fusionalSuffixLayerId.equals(languagesLayerId)) {
          throw new InvalidConfigurationException(
            this, "Invalid Fusion Suffix layer: " + fusionalSuffixLayerId);
        }
      }
    }

    // suffix
    if (suffixLayerId != null) {
      Layer layer = schema.getLayer(suffixLayerId);
      if (layer == null) {
        schema.addLayer(
          new Layer(suffixLayerId)
          .setAlignment(Constants.ALIGNMENT_INTERVAL)
          .setPeers(true)
          .setParentId(schema.getWordLayerId()));
      } else {
        if (suffixLayerId.equals(schema.getWordLayerId())
            || suffixLayerId.equals(languagesLayerId)) {
          throw new InvalidConfigurationException(this, "Invalid Suffix layer: " + suffixLayerId);
        }
      }
    }

    // gloss
    if (glossLayerId != null) {
      Layer layer = schema.getLayer(glossLayerId);
      if (layer == null) {
        schema.addLayer(
          new Layer(glossLayerId)
          .setAlignment(Constants.ALIGNMENT_INTERVAL)
          .setPeers(true)
          .setParentId(schema.getWordLayerId()));
      } else {
        if (glossLayerId.equals(schema.getWordLayerId())
            || glossLayerId.equals(languagesLayerId)) {
          throw new InvalidConfigurationException(
            this, "Invalid English Gloss layer: " + glossLayerId);
        }
      }
    }
  }
  
  /**
   * Determines which layers the annotator requires in order to annotate a graph.
   * @return A list of layer IDs.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public String[] getRequiredLayers() throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");
    if (tokenLayerId == null)
      throw new InvalidConfigurationException(this, "No input word token layer set.");
    if (languagesLayerId == null)
      throw new InvalidConfigurationException(this, "No input transcript language layer set.");
    Vector<String> requiredLayers = new Vector<String>();
    requiredLayers.add(schema.getParticipantLayerId());
    requiredLayers.add(schema.getTurnLayerId());
    requiredLayers.add(schema.getUtteranceLayerId());
    requiredLayers.add(schema.getWordLayerId());
    if (!schema.getWordLayerId().equals(tokenLayerId)) requiredLayers.add(tokenLayerId);
    requiredLayers.add(languagesLayerId);
    return requiredLayers.toArray(new String[0]);
  }
  
  /**
   * Determines which layers the annotator will create/update/delete annotations on.
   * @return A list of layer IDs.
   * empty array.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public String[] getOutputLayers() throws InvalidConfigurationException {
    if (morLayerId == null && prefixLayerId == null && partOfSpeechLayerId == null
        && partOfSpeechSubcategoryLayerId == null && stemLayerId == null
        && fusionalSuffixLayerId == null && suffixLayerId == null && glossLayerId == null)
      throw new InvalidConfigurationException(this, "No output layer set.");
    Vector<String> ids = new Vector<String>();
    if (morLayerId != null) ids.add(morLayerId);
    if (prefixLayerId != null) ids.add(prefixLayerId);
    if (partOfSpeechLayerId != null) ids.add(partOfSpeechLayerId);
    if (partOfSpeechSubcategoryLayerId != null) ids.add(partOfSpeechSubcategoryLayerId);
    if (stemLayerId != null) ids.add(stemLayerId);
    if (fusionalSuffixLayerId != null) ids.add(fusionalSuffixLayerId);
    if (suffixLayerId != null) ids.add(suffixLayerId);
    if (glossLayerId != null) ids.add(glossLayerId);
    return ids.toArray(new String[0]);
  }
  
  private ISO639 iso639 = new ISO639(); // for standard ISO 639 language code processing

  /**
   * Annotate the transcript tokens with morphosyntactic tags from MOR.
   * @param graph The graph to transform.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph transform(Graph graph) throws TransformationException {
    running = true;
    try {
      setStatus("Tagging " + graph.getId());
      setPercentComplete(0);
      
      Layer languagesLayer = graph.getSchema().getLayer(languagesLayerId);
      if (languagesLayer == null) {
        throw new InvalidConfigurationException(
          this, "Invalid languages layer: " + languagesLayerId);
      }

      // determine the grammar from the language
      final Annotation language = graph.first(languagesLayer.getId());
      if (language == null) {
        throw new InvalidConfigurationException(
          this, "Cannot determine grammar, no language set for " + graph.getId());
      }
      String iso639Alpha3 = iso639.alpha3(language.getLabel()).orElseThrow(
        ()->new InvalidConfigurationException(
          this, "Cannot determine grammar, language is not an ISO639 alpha-3 code: "
          + language.getLabel()));
      File grammar = new File(getWorkingDirectory(), iso639Alpha3);
      if (!grammar.exists()) {
        throw new InvalidConfigurationException(this, "Grammar not installed: " + iso639Alpha3);
      }
      
      // save the transcript in CHAT format
      ChatSerialization converter = new ChatSerialization();
      ParameterSet configuration = converter.configure(new ParameterSet(), schema);
      configuration.get("tokenLayer").setValue(
        graph.getSchema().getLayer(tokenLayerId));
      configuration.get("morLayer").setValue(
        graph.getSchema().getLayer(morLayerId));
      configuration.get("morPrefixLayer").setValue(
        graph.getSchema().getLayer(prefixLayerId));
      configuration.get("morPartOfSpeechLayer").setValue(
        graph.getSchema().getLayer(partOfSpeechLayerId));
      configuration.get("morPartOfSpeechSubcategoryLayer").setValue(
        graph.getSchema().getLayer(partOfSpeechSubcategoryLayerId));
      configuration.get("morStemLayer").setValue(
        graph.getSchema().getLayer(stemLayerId));
      configuration.get("morFusionalSuffixLayer").setValue(
        graph.getSchema().getLayer(fusionalSuffixLayerId));
      configuration.get("morSuffixLayer").setValue(
        graph.getSchema().getLayer(suffixLayerId));
      configuration.get("morGlossLayer").setValue(
        graph.getSchema().getLayer(glossLayerId));
      configuration.get("languagesLayer").setValue(
        languagesLayer);
      configuration.get("splitMorTagGroups").setValue(
        Boolean.valueOf(splitMorTagGroups));
      configuration.get("splitMorWordGroups").setValue(
        Boolean.valueOf(splitMorWordGroups));
      
      converter.configure(configuration, schema);

      // remove any existing annotations
      destroyAnnotations(morLayerId, graph);
      destroyAnnotations(prefixLayerId, graph);
      destroyAnnotations(partOfSpeechLayerId, graph);
      destroyAnnotations(partOfSpeechSubcategoryLayerId, graph);
      destroyAnnotations(stemLayerId, graph);
      destroyAnnotations(fusionalSuffixLayerId, graph);
      destroyAnnotations(suffixLayerId, graph);
      destroyAnnotations(glossLayerId, graph);
      
      // break the transcript into utterance chunks, and process one utterance at a time,
      // because MOR segmentation faults on very large files(?)
      String[] fragmentLayers = Stream.concat(
        Arrays.stream(getRequiredLayers()), Arrays.stream(getOutputLayers()))
        .collect(Collectors.toList()).toArray(new String[0]);
      Annotation[] utterances = graph.all(schema.getUtteranceLayerId());
      int u = 0;
      for (Annotation utterance : utterances) {
        Graph fragment = graph.getFragment(utterance, fragmentLayers);
        
        final Vector<SerializationException> exceptions = new Vector<SerializationException>();
        final Vector<NamedStream> serializeStreams = new Vector<NamedStream>();
        String[] serializationLayers = { schema.getWordLayerId(), tokenLayerId, languagesLayerId };
        Graph[] graphs = { fragment };
        try {
          converter.serialize(Arrays.spliterator(graphs), serializationLayers,
                              stream -> serializeStreams.add(stream),
                              warning -> setStatus(warning),
                              exception -> exceptions.add(exception));
        } catch(SerializerNotConfiguredException x) {
          throw new TransformationException(this, x);
        }
        if (exceptions.size() > 0) {
          Throwable firstException = null;
          for (SerializationException x : exceptions) {
            if (firstException == null) firstException = x;
            setStatus("ERROR: " + x.getMessage());
            throw new TransformationException(this, firstException.getMessage(), firstException);
          }
        }
        try {        
          File cha = File.createTempFile(fragment.getId() + "-", ".cha", getWorkingDirectory());
          IO.SaveInputStreamToFile(serializeStreams.elementAt(0).getStream(), cha);
          
          try {
            // run MOR on the CHAT file ...
            // weirdly, if we run "mor +Lgrammar .cha" the process hangs
            // so instead, we use -f, write the .cha to stdin, and read the result from stdout
            Execution mor = new Execution()
              .setExe(getMorExe())
              .arg("+L"+grammar.getPath())
              .arg("-f");
            // start the process in its own thread
            setStatus("Running mor...");
            new Thread(mor).start();
            // wait until we've got a process 
            while (mor.getProcess() == null) {
              try { Thread.sleep(500); } catch(Exception exception) {}
            }
            // write the .cha file to stdin
            IO.Pump(new FileInputStream(cha), mor.getProcess().getOutputStream());
            // wait for mor to finish
            while (!mor.getFinished()) try { Thread.sleep(500); } catch(Exception exception) {}
            setStatus("Finished mor.");
            // the annotated version has been written to stdout, so save it to a file...
            IO.SaveInputStreamToFile(new StringBufferInputStream(mor.stdout()), cha);

            // run POST on the CHAT file for disambiguation
            File postDb = new File(grammar, "post.db");
            Execution post = new Execution()
              .setExe(getPostExe())
              .arg("+d"+postDb.getPath())
              // the 64 bit linux version of POST seems to contain a bug that produces
              // corrupt output when replacing the existing %mor line, so we use +g1
              // so that it adds a new %pos line instead:
              .arg("+g1")
              .arg(cha.getPath());
            // start the process in its own thread
            setStatus("Running post...");
            new Thread(post).start();
            while (!post.getFinished()) try { Thread.sleep(500); } catch(Exception exception) {}
            setStatus("Finished post.");            
                                     
            // parse the CHAT file
            NamedStream[] deserializeStreams = {
              new NamedStream(new FileInputStream(cha), fragment.getId()+".cha") };
            ParameterSet defaultParamaters = converter.load(
              deserializeStreams, graph.getSchema());
            converter.setParameters(defaultParamaters);
            graphs = converter.deserialize();
            for (String warning : converter.getWarnings()) setStatus(warning);
            Graph tagged = graphs[0];
            tagged.trackChanges();
            new Normalizer().transform(tagged);
            tagged.commit();

            // remove words that are just "." or "-" - these are tokens added by mor
            for (Annotation w : tagged.all(schema.getWordLayerId())) {
              if (w.getLabel().matches("^\\W+$")) {
                w.destroy();
              }
            }
            tagged.commit();
            
            // merge the changes into our graph by matching up the tokens
            // use MinimumEditPath because sometimes mor adds or removes  tokens
            MinimumEditPath<Annotation> mp = new MinimumEditPath<Annotation>(
              new DefaultEditComparator<Annotation>(new EqualsComparator<Annotation>() {
                  public int compare(Annotation o1, Annotation o2) {
                    return o1.getLabel().compareTo(o2.getLabel());
                  }
                }));
            List<Annotation> originalWords = Arrays.asList(
              utterance.all(tokenLayerId)); // output was token layer, so compare against that
            List<Annotation> chaWords = Arrays.asList(
              tagged.all(schema.getWordLayerId())); // tokens come in on word layer
            for (EditStep<Annotation> step : mp.minimumEditPath(chaWords, originalWords)) {
              if (step.getFrom() != null && step.getTo() != null) {
                copyAnnotations(morLayerId, step.getFrom(), step.getTo(), graph);
                copyAnnotations(prefixLayerId, step.getFrom(), step.getTo(), graph);
                copyAnnotations(partOfSpeechLayerId, step.getFrom(), step.getTo(), graph);
                copyAnnotations(
                  partOfSpeechSubcategoryLayerId, step.getFrom(), step.getTo(), graph);
                copyAnnotations(stemLayerId, step.getFrom(), step.getTo(), graph);
                copyAnnotations(fusionalSuffixLayerId, step.getFrom(), step.getTo(), graph);
                copyAnnotations(suffixLayerId, step.getFrom(), step.getTo(), graph);
                copyAnnotations(glossLayerId, step.getFrom(), step.getTo(), graph);
              }
            } // next token
            
            setPercentComplete(++u * 100 / utterances.length);
          } finally {
            cha.delete();
          }
	
        } catch (SerializationParametersMissingException x) {
          throw new TransformationException(this, x);
        } catch (SerializerNotConfiguredException x) {
          throw new TransformationException(this, x);
        } catch (SerializationException x) {
          throw new TransformationException(this, x);
        } catch (IOException x) {
          throw new TransformationException(this, x);
        }
      } // next utterance

      // if any words are anchored
      boolean alignedWords = Arrays.stream(graph.all(schema.getWordLayerId()))
        .filter(w->w.getAnchored()).findAny().isPresent();
      if (alignedWords) {
        // ensure mor tags are also anchored
        // (they can be chained across the duration of the word, with null offsets)
        new DefaultOffsetGenerator().transform(graph);
      }
      
      setStatus("Finished " + graph.getId());
      return graph;
    } finally {
      running = false;
    }
  }
  
  /**
   * Mark for deletion all utterances on the given layer in the given graph.
   * @param layerId
   * @param graph
   */
  public void destroyAnnotations(String layerId, Graph graph) {
    if (layerId != null) {
      for (Annotation a : graph.all(layerId)) {
        a.destroy();
      }
    }
  } // end of destroyAnnotations()
  
   /**
    * Copy the annotations of the given cha token on the given layer to the given graph token.
    * @param layerId
    * @param chaWord
    * @param originalToken
    * @param graph
    */
   private void copyAnnotations(
     String layerId, Annotation chaWord, Annotation originalToken, Graph graph) {
     
     if (layerId != null) {
       for (Annotation tag : chaWord.getAnnotations(layerId)) {
         Anchor start = originalToken.getStart();
         if (!tag.getStartId().equals(chaWord.getStartId())) { // chained annotation
           if (!chaWord.getStart().containsKey("@start")) {
             // create a new anchor in the original graph
             Anchor a = new Anchor();
             a.create();
             chaWord.getStart().put("@start", graph.addAnchor(a));
           }
           start = (Anchor)chaWord.getStart().get("@start");
         }
         Anchor end = originalToken.getEnd();
         if (!tag.getEndId().equals(chaWord.getEndId())) { // chained annotation
           if (!chaWord.getEnd().containsKey("@end")) {
             // create a new anchor in the original graph
             Anchor a = new Anchor();
             a.create();
             chaWord.getEnd().put("@end", graph.addAnchor(a));
           }
           end = (Anchor)chaWord.getEnd().get("@end");
         }
         Annotation parent = originalToken;
         if (!tokenLayerId.equals(graph.getSchema().getWordLayerId())) {
           // original token is a peer, so get its word
           parent = originalToken.first(graph.getSchema().getWordLayerId());
           assert parent != null :
             "parent != null - " + originalToken + " ("+originalToken.getLayerId()+")";
         }
         Annotation a = new Annotation().setLayerId(layerId)
           .setLabel(tag.getLabel())
           .setParentId(parent.getId())
           .setStartId(start.getId())
           .setEndId(end.getId());
         a.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
         a.create();
         graph.addAnnotation(a);
       }
     }
   } // end of copyAnnotations()

}
