//
// Copyright 2025 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.formatter.trmparsercsv;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;
import nzilbb.ag.Annotation;
import nzilbb.ag.Graph;
import nzilbb.labbcat.LabbcatView;
import nzilbb.util.CommandLineProgram;
import nzilbb.util.IO;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

// command line somthing like:
//java -cp nzilbb.ag-1.2.4-SNAPSHOT.jar:/home/robert/.m2/repository/org/apache/commons/commons-csv/1.8/commons-csv-1.8.jar:/home/robert/.m2/repository/org/glassfish/javax.json/1.1.4/javax.json-1.1.4.jar:/home/robert/.m2/repository/nz/ilbb/labbcat/nzilbb.labbcat/1.4.0/nzilbb.labbcat-1.4.0.jar:nzilbb.formatter.trmparsercsv-0.1.0.jar nzilbb.formatter.trmparsercsv.ParseGloss --url=http://localhost:8080/labbcat/ --username=xxx --password=xxx data.csv


// TODO graph parsing, maybe with: https://graphstream-project.org/doc/Install/

/*
  Import of POS tags:

  "ki taku whakarongo atu hoki"

  Gloss:    part/def.s-1s/N/mod/mod
  Biggsian: $part.loc/def.s-1s/N/mod/mod

  dot:
  digraph "K002M-01__43.462-47.181" {
 n0 [label="$"];
 n1 [label=<<u>ki/part</u>>];
 n0 -> n1;
 n2 [label="ref"];
 n3 [label=<<u>taku/def.s-1s</u>>];
 n2 -> n3;
 n4 [label="desc"];
 n5 [label=<<u>whakarongo/N</u>>];
 n4 -> n5;
 n6 [label="mod"];
 n7 [label=<<u>atu/mod</u>>];
 n6 -> n7;
 n8 [label=<<u>hoki/mod</u>>];
 n6 -> n8;
 n4 -> n6;
 n2 -> n4;
 n0 -> n2;
 s0 -> n0;
}


  "ā ngā āhuatanga mō te kaupapa nei he painga atu anō mō te ."

  Gloss:    anc.p.a/def.p/N/part.irr-o/def.s/N/mod.prox/part/N/mod/mod/part.irr-o/def.s/N
  Biggsian: $anc.p.a/def.p/N/$part.irr-o/def.s/N/mod.prox/$part/N/mod/mod/$part.irr-o/def.s/N
  dot:
  digraph "K002M-01__47.181-51.931" {
 n0 [label="$"];
 n1 [label="dem"];
 n2 [label=<<u>ā/anc.p.a</u>>];
 n1 -> n2;
 n3 [label="ref"];
 n4 [label=<<u>ngā/def.p</u>>];
 n3 -> n4;
 n5 [label=<<u>āhuatanga/N</u>>];
 n3 -> n5;
 n1 -> n3;
 n0 -> n1;
 s0 -> n0;
 n6 [label="$"];
 n7 [label=<<u>mō/part.irr-o</u>>];
 n6 -> n7;
 n8 [label="ref"];
 n9 [label=<<u>te/def.s</u>>];
 n8 -> n9;
 n10 [label="desc"];
 n11 [label=<<u>kaupapa/N</u>>];
 n10 -> n11;
 n12 [label=<<u>nei/mod.prox</u>>];
 n10 -> n12;
 n8 -> n10;
 n6 -> n8;
 s0 -> n6;
 n13 [label="$"];
 n14 [label=<<u>he/part</u>>];
 n13 -> n14;
 n15 [label="desc"];
 n16 [label=<<u>painga/N</u>>];
 n15 -> n16;
 n17 [label="mod"];
 n18 [label=<<u>atu/mod</u>>];
 n17 -> n18;
 n19 [label=<<u>anō/mod</u>>];
 n17 -> n19;
 n15 -> n17;
 n13 -> n15;
 s0 -> n13;
 n20 [label="$"];
 n21 [label=<<u>mō/part.irr-o</u>>];
 n20 -> n21;
 n22 [label="ref"];
 n23 [label=<<u>te/def.s</u>>];
 n22 -> n23;
 n24 [label=<<u>./N</u>>];
 n22 -> n24;
 n20 -> n22;
 s0 -> n20;
}
  
*/

/**
 * Commane line utility that takes a CSV file that's been output by TrmParserCsv
 * - i.e. with rows identifying the MatchIds and texts of possibly syntactic chunks -
 * and then had a <tt>Gloss</tt> column added by Connor's parser.
 * <p> The results POS tags are to be saved to word layer in
 * LaBB-CAT. Rather than doing that directly, this utility creates
 * another CSV file, with one row per token (rather than one row per
 * fragment), and a column for the token's POS tag, and another
 * identifying whether the token is 'Biggsian final'.
 * <p> This second CSV file can then be manually checked and uploaded
 * to LaBB-CAT separately.
 * <p> The input CSV file has IDs only for the first and last token in
 * each fragment, not for the intervening tokens. This utility uses
 * the given LaBB-CAT URL/credentials to identify the missing token IDs.
 */
@ProgramDescription(value="Parses output of trmparsercsv that's had a Gloss column added to it, producing a file for uploading token tags to LaBB-CART",arguments="fragment-file.csv")
public class ParseGloss extends CommandLineProgram {
  public static void main(String argv[]) {
    ParseGloss application = new ParseGloss();
    if (application.processArguments(argv)) {
      application.start();
    }
  }
  
  /**
   * LaBB-CAT URL.
   * @see #getUrl()
   * @see #setUrl(String)
   */
  protected String url;
  /**
   * Getter for {@link #url}: LaBB-CAT URL.
   * @return LaBB-CAT URL.
   */
  public String getUrl() { return url; }
  /**
   * Setter for {@link #url}: LaBB-CAT URL.
   * @param newUrl LaBB-CAT URL.
   */
  @Switch(value="URL for LaBB-CAT database (either http for web API, or jdbc for direct SQL connection and direct tag insertion)",compulsory=true)
  public ParseGloss setUrl(String newUrl) { url = newUrl; return this; }
  
  /**
   * LaBB-CAT user name.
   * @see #getUsername()
   * @see #setUsername(String)
   */
  protected String username;
  /**
   * Getter for {@link #username}: LaBB-CAT user name.
   * @return LaBB-CAT user name.
   */
  public String getUsername() { return username; }
  /**
   * Setter for {@link #username}: LaBB-CAT user name.
   * @param newUsername LaBB-CAT user name.
   */
  @Switch(value="LaBB-CAT user name")
  public ParseGloss setUsername(String newUsername) { username = newUsername; return this; }
  
  /**
   * LaBB-CAT password
   * @see #getPassword()
   * @see #setPassword(String)
   */
  protected String password;
  /**
   * Getter for {@link #password}: LaBB-CAT password
   * @return LaBB-CAT password
   */
  public String getPassword() { return password; }
  /**
   * Setter for {@link #password}: LaBB-CAT password
   * @param newPassword LaBB-CAT password
   */
  @Switch(value="LaBB-CAT password")
  public ParseGloss setPassword(String newPassword) { password = newPassword; return this; }

  private LabbcatView labbcat;
  private Connection db;
  private PreparedStatement sql;
  private PreparedStatement insert;
  private int trmPOSLayerId = -1;
  private int biggsianFinalLayerId = -1;
  
  public void start() {
    try {
      if (url.startsWith("jdbc")) { // connect directly to the database
        db = DriverManager.getConnection(url, username, password);
        
        // get layer_id's for add annotations
        sql = db.prepareStatement("SELECT layer_id FROM layer WHERE short_description = ?");
        sql.setString(1, "trmPOS");
        ResultSet rs = sql.executeQuery();
        if (rs.next()) {
          trmPOSLayerId = rs.getInt("layer_id");
          System.out.println("Tags will be added directly into the trmPOS layer.");
        } else {
          System.out.println("There is no trmPOS layer, tags will be output to CSV only.");
        }
        rs.close();
        sql.setString(1, "biggsianFinal");
        rs = sql.executeQuery();
        if (rs.next()) {
          biggsianFinalLayerId = rs.getInt("layer_id");
          System.out.println("Tags will be added directly into the biggsianFinal layer.");
        } else {
          System.out.println(
            "There is no biggsianFinal layer, tags will be output to CSV only.");
        }
        rs.close();
        sql.close();

        // query for finding fragment words:
        sql = db.prepareStatement(
          "SELECT CONCAT('ew_0_', word.annotation_id) AS id, word.label"
          +" FROM annotation_layer_0 first_word"
          +" INNER JOIN annotation_layer_0 last_word"
          +" ON first_word.turn_annotation_id = last_word.turn_annotation_id"
          +" INNER JOIN annotation_layer_0 word"
          +" ON word.turn_annotation_id = first_word.turn_annotation_id"
          +" AND word.ordinal_in_turn >= first_word.ordinal_in_turn"
          +" AND word.ordinal_in_turn <= last_word.ordinal_in_turn"
          +" WHERE first_word.annotation_id = ? AND last_word.annotation_id = ?"
          // exclude pause-only 'words':
          +" AND word.label REGEXP '.*[a-zA-Z<>].*'"
          +" ORDER BY word.ordinal_in_turn");

        // update for creating tags:
        insert = db.prepareStatement(
          "INSERT INTO annotation_layer_?"
          +"(ag_id, label, label_status, start_anchor_id, end_anchor_id,"
          +" turn_annotation_id, ordinal_in_turn, word_annotation_id, parent_id,"
          +" ordinal, annotated_when)"
          +" SELECT ag_id, ?, 50, start_anchor_id, end_anchor_id,"
          +" turn_annotation_id, ordinal_in_turn, annotation_id, annotation_id,"
          +" 1, Now()"
          +" FROM annotation_layer_0 word"
          +" WHERE word.annotation_id = ?");
      } else {
        labbcat = new LabbcatView(url, username, password);
      }
    } catch(Exception exception) {
      System.err.println("Could not connect to " + url + " : " + exception);
      return;
    }
    
    for (String fragmentCsv: arguments) {
      System.out.println(fragmentCsv);
      File inputFile = new File(fragmentCsv);
      if (!inputFile.exists()) {
        System.err.println("File doesn't exist: " + fragmentCsv);
      } else {
        try {       
          File outputFile = new File(
            inputFile.getParentFile(), IO.WithoutExtension(inputFile) + "-tokens.csv");
          File rejectsFile = new File(
            inputFile.getParentFile(), IO.WithoutExtension(inputFile) + "-error.csv");
          processFile(inputFile, outputFile, rejectsFile);
        } catch(Exception exception) {
          System.err.println(fragmentCsv + ": " + exception);
        }
      } // input file exists
    } // next argument

    if (sql != null) try { sql.close(); } catch(SQLException exception) {}
    if (insert != null) try { insert.close(); } catch(SQLException exception) {}
    if (db != null) try { db.close(); } catch(SQLException exception) {}
  }  
  
  /**
   * Read fragment records from the given input file, and write
   * corresponding token records to the output file.
   * @param inputFile
   * @param outputFile
   * @throws IOException
   */
  public void processFile(File inputFile, File outputFile, File rejectsFile)
    throws IOException {
    System.out.println(
      "processFile("+inputFile.getPath()+", "+outputFile.getPath()+", "
      +rejectsFile.getPath()+")");
    long rejectCount = 0;
    MessageFormat fragmentIdFormat = new MessageFormat(
      "{0}__{1,number,#.###}-{2,number,0.000}");
    MessageFormat matchIdFormat = new MessageFormat(
      "{0};{1};{2}-{3};{4};#={5};[0]={6};[1]={7}");
    CSVFormat format = CSVFormat.EXCEL;
    try (CSVParser in = new CSVParser(new FileReader(inputFile), format.withHeader());
         CSVPrinter out = new CSVPrinter(new FileWriter(outputFile), format);
         CSVPrinter err = new CSVPrinter(new FileWriter(rejectsFile), format)) {
      
      // write headers
      out.print("Transcript");
      out.print("Speaker");
      out.print("FragmentID");
      out.print("MatchId");
      out.print("Target word");
      out.print("FragmentToken");
      out.print("trmPOS");
      out.print("biggsianFinal");
      
      long r = 0;
      for (CSVRecord fragment : in) {
        r++;
        String transcriptId = fragment.get("Document");
        String participantId = fragment.get("Speaker");

        // something like: "K001M-01__4.595-5.001"
        String fragmentId = fragment.get("ID");
        
        // something like:
        // g_165;em_12_1564;n_41146-n_41154;p_194;#=ew_0_26828;[0]=ew_0_26828;[1]=ew_0_26828
        String fragmentMatchId = fragment.get("MatchId");
        
        //String fragmentText = fragment.get("Fragment");
        // the incoming Fragment column is truncated in some cases,
        // but the WithPauses column is correct, so we use that, removing the pauses
        String fragmentText = fragment.get("WithPauses")
          .replaceAll("(\\w*) -?\\d+\\.\\d{3}","$1");
        
        String gloss = fragment.get("Gloss");
        try {
          // parse the IDs
          Object[] matchIdParts = matchIdFormat.parse(fragmentMatchId);
          Object[] idParts = fragmentIdFormat.parse(fragmentId);

          // get the fragment from LaBB-CAT
          Double start = (idParts[1] instanceof Long)?((Long)idParts[1]).doubleValue():
            (Double)idParts[1];
          Double end = (idParts[2] instanceof Long)?((Long)idParts[2]).doubleValue():
            (Double)idParts[2];
          Annotation[] words = retrieveWords(
            transcriptId, start, end, (String)matchIdParts[6], (String)matchIdParts[7]);
          if (words.length == 0) {
            rejectCount = rejectFragment(
              rejectCount, fragment, err, "No words retrieved from " + url);
            continue;
          }

          // check the token IDs match
          if (!words[0].getId().equals(matchIdParts[6])) {
            rejectCount = rejectFragment(
              rejectCount, fragment, err,
              "First token should be "+matchIdParts[6]+" but is "+words[0].getId());
            continue;
          }
          if (!words[words.length-1].getId().equals(matchIdParts[7])) {
            rejectCount = rejectFragment(
              rejectCount, fragment, err,
              "Last token should be "+matchIdParts[7]+" but is "+words[0].getId());
            continue;
          }

          String tokens[] = fragmentText.split(" ");
          if (tokens.length != words.length) {
            rejectCount = rejectFragment(
              rejectCount, fragment, err,
              "There are "+words.length+" LaBB-CAT words but "+tokens.length
              +" fragment tokens");
            continue;
          }

          String pos[] = gloss.split("/");
          if (tokens.length > pos.length) {
            rejectCount = rejectFragment(
              rejectCount, fragment, err,
              "More tokens ("+tokens.length+") than parts of speech ("+pos.length+")");
            continue;
          }
          if (pos.length % tokens.length != 0) {
            rejectCount = rejectFragment(
              rejectCount, fragment, err,
              "POS count ("+pos.length+") not a multiple of token count ("+pos.length+")");
            continue;
          }

          // there may be multiple sets of POS tags for the tokens
          // when a token has multiple distinct POS's, concatenate them together
          Set<String>[] tokenPOS = new LinkedHashSet[tokens.length];
          Set<String>[] tokenFinal = new LinkedHashSet[tokens.length];
          for (int t = 0; t < tokens.length; t++) {
            tokenPOS[t] = new LinkedHashSet<String>();
            tokenFinal[t] = new LinkedHashSet<String>();
          }

          // determine the aggregate POS/Biggsian tags
          for (int t = 0, p = 0; p < pos.length; t++, p++) {
            if (t > tokenPOS.length - 1) t = 0; // start a new set of POS tags
            
            tokenPOS[t].add(pos[p].replace("$",""));
            
            // Biggsian final if it's just before a $ or the last token
            boolean isFinal = false;
            if (t == tokens.length - 1 // it's the last token or
                || pos[p+1].startsWith("$")) { // the next POS is a Biggsian start
              isFinal = true;
            }
            tokenFinal[t].add(isFinal?"yes":"no");
          } // next POS
          
          for (int t = 0; t < tokens.length; t++) {
            // the POS/Biggsian tags are all the distinct labels we found, delimited by _
            String trmPOS = tokenPOS[t].stream().collect(Collectors.joining("_"));
            String isFinal = tokenFinal[t].stream().collect(Collectors.joining("_"));
            
            matchIdParts[5] = matchIdParts[6] = matchIdParts[7] = words[t].getId();
            String tokenMatchId = matchIdFormat.format(matchIdParts);
            saveToken(
              out, transcriptId, participantId, fragmentId, tokenMatchId,
              words[t], tokens[t], trmPOS, isFinal);
          }
        } catch (Exception x) {
          rejectCount = rejectFragment(
            rejectCount, fragment, err, x.toString());
          x.printStackTrace(System.err);
        }

        if (r % 100 == 0) {
          System.out.println(new java.util.Date().toString() + " - Fragments: "+r);
        }
        
      } // next fragment
    } // close files
    
    System.out.println("Finished " + outputFile.getName());
    if (rejectCount > 0) {
      System.out.println(""+rejectCount+" rejects written to " + rejectsFile.getName());
    } else {
      rejectsFile.delete();
    }
  } // end of processFile()
  
  /**
   * Gets the fragment words from the LaBB-CAT database.
   * @param transcriptId The ID of the transcript.
   * @param start The fragment start offset.
   * @param end The fragment end offset.
   * @param firstWordId The ID of the fragment's first word.
   * @param lastWordId The ID of the fragment's last word.
   * @return The word token annotations from the database.
   * @throws Exception
   */
  public Annotation[] retrieveWords(
    String transcriptId, Double start, Double end, String firstWordId, String lastWordId)
    throws Exception {
    if (labbcat != null) {
      Graph graph = labbcat.getFragment(transcriptId, start, end, new String[] {"word"});
      return graph.all("word");
    } else { // use SQL
      sql.setLong(1, Long.parseLong(firstWordId.replace("ew_0_","")));
      sql.setLong(2, Long.parseLong(lastWordId.replace("ew_0_","")));
      try (ResultSet rs = sql.executeQuery()) {      
        Vector<Annotation> words = new Vector<Annotation>();
        while (rs.next()) {
          words.add(new Annotation(rs.getString("id"), rs.getString("label"), "word"));
        } // next word
        return words.toArray(new Annotation[words.size()]);
      }
    }
  } // end of retrieveWords()

  /**
   * Saves the given token to the output file, and if connected directly to the database, creates annotations in the database as well.
   * @param transcriptId
   * @param participantId
   * @param fragmentId
   * @param tokenMatchId
   * @param word
   * @param fragmentToken
   * @param trmPOS
   * @param biggsianFinal
   * @throws Exception
   */
  public void saveToken(
    CSVPrinter out,
    String transcriptId, String participantId, String fragmentId, String tokenMatchId,
    Annotation word, String fragmentToken, String trmPOS, String biggsianFinal)
    throws Exception {
    out.println();
    out.print(transcriptId);
    out.print(participantId);
    out.print(fragmentId);
    out.print(tokenMatchId);
    out.print(word.getLabel());
    out.print(fragmentToken);
    out.print(trmPOS);
    out.print(biggsianFinal);

    if (insert != null) {
      insert.setLong(3, Long.parseLong(word.getId().replace("ew_0_","")));
      if (trmPOSLayerId > 0) {
        insert.setLong(1, trmPOSLayerId);
        insert.setString(2, trmPOS);
        insert.executeUpdate();
      }
      if (biggsianFinalLayerId > 0) {
        insert.setLong(1, biggsianFinalLayerId);
        insert.setString(2, biggsianFinal);
        insert.executeUpdate();
      }
    }
  } // end of saveToken()
  
  /**
   * Saves a row to the rejects file for later analysis.
   * @param rejectCount Current number of rejects.
   * @param fragment The rejected row.
   * @param err Where to write the rejected row.
   * @param reason The reason for rejection.
   * @return The new value of rejectsCount.
   * @throws IOException
   */
  public long rejectFragment(
    long rejectCount, CSVRecord fragment, CSVPrinter err, String reason)
    throws IOException {
    List<String> columns = fragment.getParser().getHeaderNames();
    if (rejectCount == 0) { // no rejects yet
      // write the headers to the rejects file
      for (String header : columns) {
        err.print(header);
      } // next source header
      err.print("Error");
    } // write headers
    err.println();
    for (String column : columns) {
      err.print(fragment.get(column));
    }
    err.print(reason);
    return rejectCount + 1;
  } // end of rejectFragment()

}
