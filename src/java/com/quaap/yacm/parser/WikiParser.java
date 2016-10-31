/*
 *  Copyright (c) 2009 Thomas Kliethermes, thamus@kc.rr.com
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/*
 * 2009-03-16: modified by thamus@kc.rr.com
 *
 * Copyright (c) 2007 Yaroslav Stavnichiy, yarosla@gmail.com
 *
 * Latest version of this software can be obtained from:
 *   http://web-tec.info/WikiParser/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * If you make use of this code, I'd appreciate hearing about it.
 * Comments, suggestions, and bug reports welcome: yarosla@gmail.com
 */
package com.quaap.yacm.parser;

import java.io.IOException;
import java.io.InputStream;
import static com.quaap.yacm.parser.Utils.*;

import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WikiParser.renderXHTML() is the main method of this class.
 * It takes wiki-text and returns XHTML.
 *
 * WikiParser's behavior can be customized by overriding appendXxx() methods,
 * which should make integration of this class into any wiki/blog/forum software
 * easy and painless.
 *
 * @author Yaroslav Stavnichiy (yarosla@gmail.com)
 *
 */
public class WikiParser {

   private int wikiLength;
   private char wikiChars[];
   private StringBuilder sb; //=new StringBuilder();
   private String wikiText;
   private int pos = 0;
   private int listLevel = -1;
   private char listLevels[] = new char[6]; // max number of levels allowed
   private boolean blockquoteBR = false;
   private boolean inTable = false;

   private static enum ContextType {

      PARAGRAPH, LIST_ITEM, TABLE_CELL, HEADER, NOWIKI_BLOCK
   };

   private static final String[] ESCAPED_INLINE_SEQUENCES = {"{{{", "{{", "}}}", "\\\\", "[[", "<<<", "<<", "~", "|", "&"};
   private static final String LIST_CHARS = "*-#>:;";
   private static final String[] LIST_OPEN = {"<ul><li>", "<ul><li>", "<ol><li>", "<blockquote>", "<div class='dd'>", "<div class='dt'>"};
   private static final String[] LIST_CLOSE = {"</li></ul>\n", "</li></ul>\n", "</li></ol>\n", "</blockquote>\n", "</div>\n", "</div>\n"};

   private static final String[] XHTML_ENTITIES = {"&nbsp;", "&iexcl;", "&cent;", "&pound;", "&curren;", "&yen;", "&brvbar;", "&sect;", "&uml;", "&copy;",
      "&ordf;", "&laquo;", "&not;", "&shy;", "&reg;", "&macr;", "&deg;", "&plusmn;", "&sup2;", "&sup3;", "&acute;", "&micro;", "&para;", "&middot;", "&cedil;", "&sup1;", "&ordm;",
      "&raquo;", "&frac14;", "&frac12;", "&frac34;", "&iquest;", "&Agrave;", "&Aacute;", "&Acirc;", "&Atilde;", "&Auml;", "&Aring;", "&AElig;", "&Ccedil;",
      "&Egrave;", "&Eacute;", "&Ecirc;", "&Euml;", "&Igrave;", "&Iacute;", "&Icirc;", "&Iuml;", "&ETH;", "&Ntilde;", "&Ograve;", "&Oacute;", "&Ocirc;", "&Otilde;", "&Ouml;",
      "&times;", "&Oslash;", "&Ugrave;", "&Uacute;", "&Ucirc;", "&Uuml;", "&Yacute;", "&THORN;", "&szlig;", "&agrave;", "&aacute;", "&acirc;", "&atilde;",
      "&auml;", "&aring;", "&aelig;", "&ccedil;", "&egrave;", "&eacute;", "&ecirc;", "&euml;", "&igrave;", "&iacute;", "&icirc;", "&iuml;", "&eth;", "&ntilde;",
      "&ograve;", "&oacute;", "&ocirc;", "&otilde;", "&ouml;", "&divide;", "&oslash;", "&ugrave;", "&uacute;", "&ucirc;", "&uuml;", "&yacute;", "&thorn;", "&yuml;",
      "&fnof;", "&Alpha;", "&Beta;", "&Gamma;", "&Delta;", "&Epsilon;", "&Zeta;", "&Eta;", "&Theta;", "&Iota;", "&Kappa;", "&Lambda;", "&Mu;", "&Nu;", "&Xi;", "&Omicron;",
      "&Pi;", "&Rho;", "&Sigma;", "&Tau;", "&Upsilon;", "&Phi;", "&Chi;", "&Psi;", "&Omega;", "&alpha;", "&beta;", "&gamma;", "&delta;", "&epsilon;", "&zeta;", "&eta;",
      "&theta;", "&iota;", "&kappa;", "&lambda;", "&mu;", "&nu;", "&xi;", "&omicron;", "&pi;", "&rho;", "&sigmaf;", "&sigma;", "&tau;", "&upsilon;", "&phi;", "&chi;",
      "&psi;", "&omega;", "&thetasym;", "&upsih;", "&piv;", "&bull;", "&hellip;", "&prime;", "&Prime;", "&oline;", "&frasl;", "&weierp;", "&image;", "&real;",
      "&trade;", "&alefsym;", "&larr;", "&uarr;", "&rarr;", "&darr;", "&harr;", "&crarr;", "&lArr;", "&uArr;", "&rArr;", "&dArr;", "&hArr;", "&forall;", "&part;",
      "&exist;", "&empty;", "&nabla;", "&isin;", "&notin;", "&ni;", "&prod;", "&sum;", "&minus;", "&lowast;", "&radic;", "&prop;", "&infin;", "&ang;", "&and;", "&or;",
      "&cap;", "&cup;", "&int;", "&there4;", "&sim;", "&cong;", "&asymp;", "&ne;", "&equiv;", "&le;", "&ge;", "&sub;", "&sup;", "&nsub;", "&sube;", "&supe;", "&oplus;",
      "&otimes;", "&perp;", "&sdot;", "&lceil;", "&rceil;", "&lfloor;", "&rfloor;", "&lang;", "&rang;", "&loz;", "&spades;", "&clubs;", "&hearts;", "&diams;",
      "&quot;", "&amp;", "&lt;", "&gt;", "&OElig;", "&oelig;", "&Scaron;", "&scaron;", "&Yuml;", "&circ;", "&tilde;", "&ensp;", "&emsp;", "&thinsp;", "&zwnj;", "&zwj;",
      "&lrm;", "&rlm;", "&ndash;", "&mdash;", "&lsquo;", "&rsquo;", "&sbquo;", "&ldquo;", "&rdquo;", "&bdquo;", "&dagger;", "&Dagger;", "&permil;",
      "&lsaquo;", "&rsaquo;", "&euro;"};
   private static char[] syms_firstchar = new char[]{};
   private static String[] syms = new String[]{};
   private static Map<String, String> symsMap = new HashMap<String, String>();
   private static FormatTag[] formatTags;


   static {
      initFormats();
      initSymbols();
   }

   private static void initFormats() {
      InputStream in = WikiParser.class.getClassLoader().getResourceAsStream("formats.properties");
      if (in != null) {
         Properties formats = new Properties();
         try {
            formats.load(in);
            String[] names = formats.getProperty("formatnames", "").split("(?:\\s|\\n)*,(?:\\s|\\n)*");
            List<FormatTag> fmats = new ArrayList<FormatTag>(names.length);
            //formatTags = new FormatTag[names.length];
            for (int i = 0; i < names.length; i++) {
               String name = names[i];
               if (formats.getProperty(name + ".opendelim")==null || formats.getProperty(name + ".closedelim")==null) {
                  System.out.println("Skipping '" + name + "': null");
               } else {
                  fmats.add(new FormatTag(
                          name,
                          formats.getProperty(name + ".opendelim"),
                          formats.getProperty(name + ".closedelim"),
                          formats.getProperty(name + ".opentag"),
                          formats.getProperty(name + ".closetag"))
                        );
               }
            }

            formatTags = fmats.toArray(new FormatTag[0]);
            
            Arrays.sort(formatTags, new Comparator<FormatTag>() {

               public int compare(FormatTag o1, FormatTag o2) {
                  if (o1.getOpendelimiter().length() < o2.getOpendelimiter().length()) {
                     return 1;
                  }
                  if (o1.getOpendelimiter().length() > o2.getOpendelimiter().length()) {
                     return -1;
                  }
                  return o1.getOpendelimiter().compareToIgnoreCase(o2.getOpendelimiter());
               }
            });
         } catch (IOException ex) {
            Logger.getLogger(WikiParser.class.getName()).log(Level.SEVERE, null, ex);
         }
      }

   }

   private static void initSymbols() {
      InputStream in = WikiParser.class.getClassLoader().getResourceAsStream("symbols.properties");
      if (in != null) {
         Properties symbols = new Properties();
         try {
            symbols.load(in);
            syms = symbols.keySet().toArray(new String[0]);
            Arrays.sort(syms, new Comparator<String>() {

               public int compare(String o1, String o2) {
                  if (o1.length() < o2.length()) {
                     return 1;
                  }
                  if (o1.length() > o2.length()) {
                     return -1;
                  }
                  return o1.compareToIgnoreCase(o2);
               }
            });


            List<Character> seen = new ArrayList<Character>();
            for (String key : syms) {
               symsMap.put(key, symbols.getProperty(key, "!!missing!!"));

               // build a list of just the first characters
               if (!seen.contains(key.charAt(0))) {
                  seen.add(key.charAt(0));
               }

            //System.out.print("| ~" + key + " | " + key + " | ");
            //if (++i % 3 == 0) System.out.println();
            }
            syms_firstchar = new char[seen.size()];
            for (int k = 0; k < seen.size(); k++) {
               syms_firstchar[k] = seen.get(k);
            }
            Arrays.sort(syms_firstchar);
         } catch (IOException ex) {
            Logger.getLogger(WikiParser.class.getName()).log(Level.SEVERE, null, ex);
         }
      }

   }

   private static FormatTag getFormat(String text, int offset) {
      if (text == null) {
         return null;
      }
      for (FormatTag f : formatTags) {
         if (text.startsWith(f.getOpendelimiter(), offset)) {
            return f;
         }
      }

      return null;
   }

   public static String renderXHTML(String wikiText) {
      WikiParser parser = new WikiParser();
      return parser.process(wikiText).toString();
   }

   protected WikiParser() {
      wikiLength = 0;
      wikiChars = null;
      sb = null; //new StringBuilder();
      wikiText = null;
      pos = 0;
      listLevel = -1;
      listLevels = new char[6]; // max number of levels allowed
      blockquoteBR = false;
      inTable = false;
   }

   protected StringBuilder process(String wikiText) {
      wikiText = preprocessWikiText(wikiText);

      this.wikiText = wikiText;
      wikiLength = this.wikiText.length();
      wikiChars = new char[wikiLength];
      this.wikiText.getChars(0, wikiLength, wikiChars, 0);

      sb = new StringBuilder(wikiLength);

      while (parseBlock()) {
      }

      sb.append(closeListsAndTables());
      return sb;
   }

   @Override
   public String toString() {
      return sb.toString();
   }

   private CharSequence closeListsAndTables() {
      StringBuilder sb2 = new StringBuilder();
      // close unclosed lists
      while (listLevel >= 0) {
         sb2.append(LIST_CLOSE[LIST_CHARS.indexOf(listLevels[listLevel--])]);
      }
      if (inTable) {
         sb2.append(getTableClose());
         inTable = false;
      }
      return sb2;
   }

   private boolean parseBlock() {
      for (; pos < wikiLength && wikiChars[pos] <= ' ' && wikiChars[pos] != '\n'; pos++) {
      } // skip whitespace
      if (pos >= wikiLength) {
         return false;
      }

      char c = wikiChars[pos];

      if (c == '\n') { // blank line => end of list/table; no other meaning
         sb.append(closeListsAndTables());
         pos++;
         return true;
      }

      if (c == '|') { // table
         if (!inTable) {
            sb.append(closeListsAndTables()); // close lists if any
            sb.append(getTableOpen());
            inTable = true;
         }
         ParseState ps2 = parseTableRow(pos + 1);
         sb.append(ps2.getText());
         pos = ps2.getPos();
         return true;
      } else {
         if (inTable) {
            sb.append(getTableClose());
            inTable = false;
         }
      }

      if (listLevel >= 0 || LIST_CHARS.indexOf(c)>=0) { // lists
         int lc;
         // count list level
         for (lc = 0; lc <= listLevel && pos + lc < wikiLength && wikiChars[pos + lc] == listLevels[lc]; lc++) {
         }

         if (lc <= listLevel) { // end list block(s)
            do {
               sb.append(LIST_CLOSE[LIST_CHARS.indexOf(listLevels[listLevel--])]);
            } while (lc <= listLevel);
            // list(s) closed => retry from the same position
            return true;
         } else {
            if (pos + lc >= wikiLength) {
               return false;
            }
            char cc = wikiChars[pos + lc];
            int listType = LIST_CHARS.indexOf(cc);
            if (listType >= 0 && pos + lc + 1 < wikiLength && wikiChars[pos + lc + 1] != cc) { // new list block
               sb.append(LIST_OPEN[listType]);
               listLevels[++listLevel] = cc;
               ParseState ps2 = parseListItem(pos + lc + 1);
               pos = ps2.getPos();
               sb.append(ps2.getText());
               return true;
            } else if (listLevel >= 0) { // list item - same level
               if (listLevels[listLevel] == '>'){
                  sb.append('\n');
               } else if (listLevels[listLevel] == ';') {
                  sb.append(LIST_CLOSE[LIST_CHARS.indexOf(listLevels[listLevel])]);
                  sb.append('\n');
                  sb.append(LIST_OPEN[LIST_CHARS.indexOf(listLevels[listLevel])]);
               } else if (listLevels[listLevel] == ':') {
                  sb.append(LIST_CLOSE[LIST_CHARS.indexOf(listLevels[listLevel])]);
                  sb.append('\n');
                  sb.append(LIST_OPEN[LIST_CHARS.indexOf(listLevels[listLevel])]);
               } else {
                  sb.append("</li>\n<li>");
               }
               ParseState ps2 = parseListItem(pos + lc);
               pos = ps2.getPos();
               sb.append(ps2.getText());
               return true;
            }
         }
      }

      if (c == '=') { // heading
         int hc;
         // count heading level
         for (hc = 1; hc < 6 && pos + hc < wikiLength && wikiChars[pos + hc] == '='; hc++) {
         }
         if (pos + hc >= wikiLength) {
            return false;
         }
         int p;
         for (p = pos + hc; p < wikiLength && (wikiChars[p] == ' ' || wikiChars[p] == '\t'); p++) {
         } // skip spaces
         ParseState ps2 = parseItem(p, wikiText.substring(pos, pos + hc), ContextType.HEADER);
         pos = ps2.getPos();
         CharSequence htext = getHeader(ps2.getText(), hc);
         String hid = htext.toString().replaceAll("<.+?>|\\W+", "_");
         sb.append(htext);
         return true;
      } else if (c == '{') { // nowiki-block?
         if (pos + 3 < wikiLength && wikiChars[pos + 1] == '{' && wikiChars[pos + 2] == '{') {
            int startNowiki = pos + 3;
            int endNowiki = findEndOfNowiki(startNowiki);
            int endPos = endNowiki + 3;
            if (wikiText.lastIndexOf('\n', endNowiki) >= startNowiki) { // block <pre>
               if (wikiChars[startNowiki] == '\n') {
                  startNowiki++; // skip the very first '\n'
               }
               if (wikiChars[endNowiki - 1] == '\n') {
                  endNowiki--; // omit the very last '\n'
               }
               sb.append("<pre>");
               sb.append(getNoWiki(wikiText.substring(startNowiki, endNowiki)));
               sb.append("</pre>\n");
               pos = endPos;
               return true;
            }
         // else inline <nowiki> - proceed to regular paragraph handling
         }
      } else if (c == '-' && wikiText.startsWith("----", pos)) {
         int p;
         for (p = pos + 4; p < wikiLength && (wikiChars[p] == ' ' || wikiChars[p] == '\t'); p++) {
         } // skip spaces
         if (p == wikiLength || wikiChars[p] == '\n') {
            sb.append(getHr());
            pos = p;
            return true;
         }
//      } else if (c == '<' && pos+1 < wikiLength && wikiChars[pos + 1]=='<') {
//       // <<macro/plugin>>
//         int endExt = wikiText.indexOf(">>", pos + 2);
//         if (endExt >= 0 && endExt < wikiLength) {
//            sb.append(getMacro(wikiText.substring(pos + 2, endExt)));
//            pos = endExt + 2;
//            return true;
//         }
      } else if (c == '~') { // block-level escaping: '*' '-' '#' '>' ':' '|' '='
         if (pos + 1 < wikiLength) {
            char nc = wikiChars[pos + 1];
            if (nc == '>' || nc == ':' || nc == '|' || nc == '=') { // can't be inline markup
               pos++; // skip '~' and proceed to regular paragraph handling
               c = nc;
            } else if (nc == '*' || nc == '-' || nc == '#') { // might be inline markup so need to double check
               char nnc = pos + 2 < wikiLength ? wikiChars[pos + 2] : 0;
               if (nnc != nc) {
                  pos++; // skip '~' and proceed to regular paragraph handling
                  c = nc;
               }
            // otherwise escaping will be done at line level
            }
         }
      }

      { // paragraph handling
         //sb.append(getParagraphOpen());
         ParseState ps2 = parseItem(pos, null, ContextType.PARAGRAPH);
         pos = ps2.getPos();
         //sb.append(ps2.getText());
         //sb.append(getParagraphClose());
         sb.append(getParagraph(ps2.getText()));
         return true;
      }
   }

   /**
    * Finds first closing '}}}' for nowiki block or span.
    * Skips escaped sequences: '~}}}'.
    *
    * @param startBlock points to first char after '{{{'
    * @return position of first '}' in closing '}}}'
    */
   private int findEndOfNowiki(int startBlock) {
      // NOTE: this method could step back one char from startBlock position
      int endBlock = startBlock - 3;
      do {
         endBlock = wikiText.indexOf("}}}", endBlock + 3);
         if (endBlock < 0) {
            return wikiLength; // no matching '}}}' found
         }
         while (endBlock + 3 < wikiLength && wikiChars[endBlock + 3] == '}') {
            endBlock++; // shift to end of sequence of more than 3x'}' (eg. '}}}}}')
         }
      } while (wikiChars[endBlock - 1] == '~');
      return endBlock;
   }

   /**
    * Greedy version of findEndOfNowiki().
    * It finds the last possible closing '}}}' before next opening '{{{'.
    * Also uses escapes '~{{{' and '~}}}'.
    *
    * @param startBlock points to first char after '{{{'
    * @return position of first '}' in closing '}}}'
    */
   @SuppressWarnings("unused")
   private int findEndOfNowikiGreedy(int startBlock) {
      // NOTE: this method could step back one char from startBlock position
      int nextBlock = startBlock - 3;
      do {
         do {
            nextBlock = wikiText.indexOf("{{{", nextBlock + 3);
         } while (nextBlock > 0 && wikiChars[nextBlock - 1] == '~');
         if (nextBlock < 0) {
            nextBlock = wikiLength;
         }
         int endBlock = wikiText.lastIndexOf("}}}", nextBlock);
         if (endBlock >= startBlock && wikiChars[endBlock - 1] != '~') {
            return endBlock;
         }
      } while (nextBlock < wikiLength);
      return wikiLength;
   }

   /**
    * @param start points to first char after pipe '|'
    * @return
    */
   private ParseState parseTableRow(int start) {
      ParseState ps = new ParseState();
      if (start >= wikiLength) {
         ps.setPos(wikiLength);
         return ps;
      }

      StringBuilder tb = new StringBuilder();
      boolean endOfRow = false;
      do {
         int colspan = 0;
         while (start + colspan < wikiLength && wikiChars[start + colspan] == '|') {
            colspan++;
         }
         start += colspan;
         colspan++;
         boolean th = start < wikiLength && wikiChars[start] == '=';
         start += (th ? 1 : 0);
         while (start < wikiLength && wikiChars[start] <= ' ' && wikiChars[start] != '\n') {
            start++; // trim whitespace from the start
         }
         if (start >= wikiLength || wikiChars[start] == '\n') { // skip last empty column
            start++; // eat '\n'
            break;
         }

         ParseState ps2 = parseItem(start, null, ContextType.TABLE_CELL);
         start = ps2.getPos();
         if (ps2.isEndOfSubContext()) { // end of cell
            //start=ps2.getPos();
            if (start >= wikiLength) {
               endOfRow = true;
            } else if (wikiChars[start] == '\n') {
               start++; // eat '\n'
               endOfRow = true;
            }
         }
         if (ps2.isEndOfContext()) {
            //start=ps2.getPos();
            endOfRow = true;
         }
         if (th) {
            tb.append(getTableHeaderCell(ps2.getText(), colspan));
         } else {
            tb.append(getTableCell(ps2.getText(), colspan));
         }
      } while (!endOfRow/* && start<wikiLength && wikiChars[start]!='\n'*/);
      //ps.append("<tr>");
      ps.append(getTableRow(tb));
      //ps.append("</tr>\n");
      ps.setPos(start);
      return ps;
   }

   /**
    * Same as parseItem(); blank line adds &lt;br/&gt;&lt;br/&gt;
    *
    * @param start
    */
   private ParseState parseListItem(int start) {
      while (start < wikiLength && wikiChars[start] <= ' ' && wikiChars[start] != '\n') {
         start++; // skip spaces
      }
      ParseState ps = parseItem(start, null, ContextType.LIST_ITEM);
      int end = ps.getPos();
      if ((listLevels[listLevel] == '>' || listLevels[listLevel] == ':') && wikiText.substring(start, end).trim().length() == 0) { // empty line within blockquote/div
         if (!blockquoteBR) {
            ps.append("<br/><br/>");
            blockquoteBR = true;
         }
      } else {
         blockquoteBR = false;
      }
      return ps;
   }

   /**
    * @param p points to first slash in suspected URI (scheme://etc)
    * @param start points to beginning of parsed item
    * @param start points to end of parsed item
    *
    * @return array of two integer offsets [begin_uri, end_uri] if matched, null otherwise
    */
   private int[] checkURI(int p, int start, int end) {
      if (p > start && wikiChars[p - 1] == ':') { // "://" found
         int pb = p - 1;
         while (pb > start && isLatinLetterOrDigit(wikiChars[pb - 1])) {
            pb--;
         }
         int pe = p + 2;
         while (pe < end && isUrlChar(wikiChars[pe])) {
            pe++;
         }
         URI uri = null;
         do {
            while (pe > p + 2 && ",.%;".indexOf(wikiChars[pe - 1]) >= 0) {
               pe--; // don't want these chars at the end of URI
            }
            try { // verify URL syntax
               uri = new URI(wikiText.substring(pb, pe));
            } catch (URISyntaxException e) {
               pe--; // try choping from the end
            }
         } while (uri == null && pe > p + 2);
         if (uri != null && uri.isAbsolute()) {
            int offs[] = {pb, pe};
            return offs;
         }
      }
      return null;
   }

   /**
    * @param p points to at sign in suspected email
    * @param start points to beginning of parsed item
    * @param start points to end of parsed item
    *
    * @return array of two integer offsets [begin_email, end_email] if matched, null otherwise
    */
   private int[] checkEmail(int p, int start, int end) {
      if (p > start && isEmailChar(wikiChars[p - 1])) { // "@" found
         int pb = p - 1;
         while (pb > start && isEmailChar(wikiChars[pb - 1])) {
            pb--;
         }
         while (pb < p && !isLatinLetterOrDigit(wikiChars[pb])) {
            pb++;
         }

         int pe = p + 1;
         while (pe < end && isEmailDomainChar(wikiChars[pe])) {
            pe++;
         }

         while (pe > p + 1 && !isLatinLetterOrDigit(wikiChars[pe - 1])) {
            pe--; // don't want special chars at the end of URI
         }
         if (p-pb>0 && pe - p > 4 && wikiText.substring(pb, pe).matches("[\\p{Ll}\\p{Lu}\\p{Lo}._']+@[a-zA-Z0-9]+\\.[a-zA-Z0-9._]+[a-zA-Z]")) {
            int offs[] = {pb, pe};
            return offs;
         }
      }
      return null;
   }


//  private int parseItem(int start, String delimiter, ContextType context) {
//    try {
//      return parseItemThrow(start, delimiter, context);
//    }
//    catch (EndOfContextException e) {
//      return e.position;
//    }
//  }
   private ParseState parseItem(int start, String delimiter, ContextType context) {
      ParseState ps = new ParseState();
      StringBuilder tb = new StringBuilder();

      boolean specialCaseDelimiterHandling = "//".equals(delimiter);
      int p = start;
      int end = wikiLength;

      try {
         nextChar:
         while (true) {
            if (p >= end) {
               ps.setPos(end);
               ps.setEndOfContext(true);
               return ps;
            //throw new EndOfContextException(end);
            } //break;

            if (delimiter != null && wikiText.startsWith(delimiter, p)) {
               if (!specialCaseDelimiterHandling || checkURI(p, start, end) == null) {
                  p += delimiter.length();
                  ps.setPos(p);
                  return ps;
               }
            }

            char c = wikiChars[p];

            // context-defined break test
            if (c == '\n') {
               if (context == ContextType.HEADER || context == ContextType.TABLE_CELL) {
                  p++;
                  ps.setPos(p);
                  ps.setEndOfContext(true);
                  return ps;
               //throw new EndOfContextException(p);
               }
               if (p + 1 < end && wikiChars[p + 1] == '\n') { // blank line delimits everything
                  p++; // leave one '\n' unparsed so parseBlock() can close all lists
                  ps.setPos(p);
                  ps.setEndOfContext(true);
                  return ps;
               // throw new EndOfContextException(p);
               }
               for (p++; p < end && wikiChars[p] <= ' ' && wikiChars[p] != '\n'; p++) {
               } // skip whitespace
               if (p >= end) {
                  ps.setPos(p);
                  ps.setEndOfContext(true);
                  return ps;
               //throw new EndOfContextException(p);
               }

               c = wikiChars[p];

               if (LIST_CHARS.indexOf(c) >= 0) { // lists
                  if (context == ContextType.LIST_ITEM) {
                     ps.setPos(p);
                     ps.setEndOfContext(true);
                     return ps;
                  //throw new EndOfContextException(p);
                  }
                  if (p + 1 < end && wikiChars[p + 1] != c) {
                     ps.setPos(p);
                     ps.setEndOfContext(true);
                     return ps;
                  //throw new EndOfContextException(p);
                  }
                  // also check for ---- <hr/>
                  if (wikiText.startsWith("----", p)) {
                     int pp;
                     for (pp = p + 4; pp < end && (wikiChars[pp] == ' ' || wikiChars[pp] == '\t'); pp++) {
                     } // skip spaces
                     if (pp == end || wikiChars[pp] == '\n') {
                        ps.setPos(p);
                        ps.setEndOfContext(true);
                        return ps;
                     //throw new EndOfContextException(p);
                     } // yes, it's <hr/>
                  }
               } else if (c == '=') { // header
                  ps.setPos(p);
                  ps.setEndOfContext(true);
                  return ps;
               //throw new EndOfContextException(p);
               } else if (c == '|') { // table
                  ps.setPos(p);
                  ps.setEndOfContext(true);
                  return ps;
               //throw new EndOfContextException(p);
               }

               // if none matched add '\n' to text buffer
               tb.append('\n');
            // p and c already shifted past the '\n' and whitespace after, so go on
            } else if (c == '|') {
               if (context == ContextType.TABLE_CELL) {
                  p++;
                  ps.setPos(p);
                  ps.setEndOfSubContext(true);
                  return ps;
               //throw new EndOfSubContextException(p);
               }
            }

            FormatTag formatType = null;

            if (c == '{' && p + 1 < end && wikiChars[p + 1] == '{') {
               if (p + 2 < end && wikiChars[p + 2] == '{') { // inline or block <nowiki>
                  ps.append(getPlainText(tb.toString()));
                  tb.delete(0, tb.length()); // flush text buffer
                  int startNowiki = p + 3;
                  int endNowiki = findEndOfNowiki(startNowiki);
                  p = endNowiki + 3;
                  if (wikiText.lastIndexOf('\n', endNowiki) >= startNowiki) { // block <pre>
                     if (wikiChars[startNowiki] == '\n') {
                        startNowiki++; // skip the very first '\n'
                     }
                     if (wikiChars[endNowiki - 1] == '\n') {
                        endNowiki--; // omit the very last '\n'
                     }
                     ps.append("<pre>");
                     ps.append(getNoWiki(wikiText.substring(startNowiki, endNowiki)));
                     ps.append("</pre>\n");
                  //if (context==ContextType.NOWIKI_BLOCK) return p; // in this context return immediately after nowiki
                  } else { // inline <nowiki>
                     ps.append(getNoWiki(wikiText.substring(startNowiki, endNowiki)));
                  }
                  continue nextChar;
               } else  { // {{image}}
                  int endImg = wikiText.indexOf("}}", p + 2);
                  if (endImg >= 0 && endImg < end) {
                     ps.append(getPlainText(tb.toString()));
                     tb.delete(0, tb.length()); // flush text buffer
                     ps.append(getImage(wikiText.substring(p + 2, endImg)));
                     p = endImg + 2;
                     continue nextChar;
                  }
               }
            } else if (c == '[' && p + 1 < end && wikiChars[p + 1] == '[') { // [[link]]
              int endLink = wikiText.indexOf("]]", p + 2);
               if (endLink >= 0 && endLink < end) {
                  ps.append(getPlainText(tb.toString()));
                  tb.delete(0, tb.length()); // flush text buffer
                  ps.append(getLink(wikiText.substring(p + 2, endLink)));
                  p = endLink + 2;
                  continue nextChar;
               }
            } else if (c == '\\' && p + 1 < end && wikiChars[p + 1] == '\\') {// \\ = <br/>
               ps.append(getPlainText(tb.toString()));
               tb.delete(0, tb.length()); // flush text buffer
               ps.append(getLineBreak());
               p += 2;
               continue nextChar;
            } else if (c == '<' && p+1 < end && wikiChars[p + 1]=='<') {
               if (p + 2 < end && wikiChars[p + 2] == '<') { // <<<placeholder>>>
                  int endMacro = wikiText.indexOf(">>>", p + 3);
                  if (endMacro >= 0 && endMacro < end) {
                     ps.append(getPlainText(tb.toString()));
                     tb.delete(0, tb.length()); // flush text buffer
                     ps.append(getPlaceholder(wikiText.substring(p + 3, endMacro)));
                     p = endMacro + 3;
                     continue nextChar;
                  }
               } else { // <<macro/plugin>>
                  int endExt = wikiText.indexOf(">>", p + 2);
                  if (endExt >= 0 && endExt < end) {
                     ps.append(getPlainText(tb.toString()));
                     tb.delete(0, tb.length()); // flush text buffer
                     ps.append(getMacro(wikiText.substring(p + 2, endExt)));
                     p = endExt + 2;
                     continue nextChar;
                  }
               }

            } else if (((formatType = getFormat(wikiText, p)) != null) && p + formatType.getOpendelimiter().length() < end && wikiChars[p + formatType.getOpendelimiter().length()] != ' ' && wikiChars[p + formatType.getOpendelimiter().length()] != '\n') {
               ps.append(getPlainText(tb.toString()));
               tb.delete(0, tb.length()); // flush text buffer
               if (c == '/') { // special case for "//" - check if it is part of URL (scheme://etc)
                  int[] uriOffs = checkURI(p, start, end);
                  if (uriOffs != null) {
                     int pb = uriOffs[0], pe = uriOffs[1];
                     if (pb > start && wikiChars[pb - 1] == '~') {
                        ps.getText().delete(ps.getText().length() - (p - pb + 1), ps.getText().length()); // roll back URL + tilde
                        ps.append(escapeHTML(wikiText.substring(pb, pe)));
                     } else {
                        ps.getText().delete(ps.getText().length() - (p - pb), ps.getText().length()); // roll back URL
                        ps.append(getLink(wikiText.substring(pb, pe)));
                     }
                     p = pe;
                     continue nextChar;
                  }
               }
               ParseState ps1 = parseItem(p + formatType.getOpendelimiter().length(), formatType.getClosedelimiter(), context);
               p = ps1.getPos();

//            ps.append(FORMAT_TAG_OPEN[formatType]);
//            ps.append(ps1.getText());
//            ps.append(FORMAT_TAG_CLOSE[formatType]);
               ps.append(getFormatSection(formatType, ps1.getText()));
               continue nextChar;
            } else if (c == '@') { // check for email
               int[] emailOffs = checkEmail(p, start, end);
               if (emailOffs != null) {
                  ps.append(getPlainText(tb.toString()));
                  tb.delete(0, tb.length()); // flush text buffer
                  int pb = emailOffs[0], pe = emailOffs[1];
                  if (pb > start && wikiChars[pb - 1] == '~') {
                     ps.getText().delete(ps.getText().length() - (p - pb + 1), ps.getText().length()); // roll back email + tilde
                     ps.append(escapeHTML(wikiText.substring(pb, pe)));
                  } else {
                     ps.getText().delete(ps.getText().length() - (p - pb), ps.getText().length()); // roll back email
                     ps.append(getEmail(wikiText.substring(pb, pe)));
                  }
                  p = pe;
                  continue nextChar;
               }


            } else if (c == '~' ) { // escape
               // start line escapes are dealt with in parseBlock()
               for (final String e : ESCAPED_INLINE_SEQUENCES) {
                  if (wikiText.startsWith(e, p + 1)) {
                     ps.append(getPlainText(tb.toString()));
                     tb.delete(0, tb.length()); // flush text buffer
                     ps.append(getPlainText(e));
                     p += 1 + e.length();
                     continue nextChar;
                  }
               }
               for (final FormatTag f: formatTags) {
                  if (wikiText.startsWith(f.getOpendelimiter(), p + 1)) {
                     ps.append(getPlainText(tb.toString()));
                     tb.delete(0, tb.length()); // flush text buffer
                     ps.append(getPlainText(f.getOpendelimiter()));
                     p += 1 + f.getOpendelimiter().length();
                     continue nextChar;
                  }
               }
               if (p + 1 < end && Arrays.binarySearch(syms_firstchar, wikiChars[p + 1]) >= 0) { // only look through the list if we need to.
                  for (final String e : syms) {
                     if (e.length() == 0) {
                        continue;
                     }
                     if (wikiText.startsWith(e, p + 1)) {
                        ps.append(getPlainText(tb.toString()));
                        tb.delete(0, tb.length()); // flush text buffer
                        ps.append(getPlainText(e));
                        p += 1 + e.length();
                        continue nextChar;
                     }
                  }
               }
            } else if (c == '&' && p+2 < end && isLatinLetterOrDigit(wikiChars[p + 1]) && isLatinLetterOrDigit(wikiChars[p + 2])) { // is an entity
               for (final String s : XHTML_ENTITIES) {
                  if (wikiText.startsWith(s, p)) {
                     ps.append(getPlainText(tb.toString()));
                     tb.delete(0, tb.length()); // flush text buffer
                     ps.append(s);
                     p += s.length();
                     continue nextChar;
                  }
               }

            } //        else if (c=='-') { // ' -- ' => &ndash;
            //          if (p+2<end && wikiChars[p+1]=='-' && wikiChars[p+2]==' ' && p>start && wikiChars[p-1]==' ') {
            //            appendText(tb.toString()); tb.delete(0, tb.length()); // flush text buffer
            //            sb.append("&ndash; ");
            //            p+=3;
            //            continue;
            //          } else if (p+2<end && wikiChars[p+1]=='-' && wikiChars[p+2]=='-' && wikiChars[p+3]==' ' && p>start && wikiChars[p-1]==' ') {
            //            appendText(tb.toString()); tb.delete(0, tb.length()); // flush text buffer
            //            sb.append("&mdash; ");
            //            p+=4;
            //            continue;
            //          }
            // }
            else if (Arrays.binarySearch(syms_firstchar, c) >= 0) { // only look through the list if we need to.  This needs to be last so we don't suck up another pattern.
               for (final String e : syms) {
                  if (e.length() == 0) {
                     continue;
                  }
                  boolean matched = true;
                  for (int k = 0; k < e.length(); k++) {
                     if (p + k < end && wikiChars[p + k] != e.charAt(k)) {
                        matched = false;
                        break;
                     }
                  }
                  if (matched) {
                     ps.append(getPlainText(tb.toString()));
                     tb.delete(0, tb.length()); // flush text buffer
                     String repl = symsMap.get(e);
                     if (repl.startsWith("image:")) {
                        ps.append(getImage(repl.substring(6)));
                     } else {
                        ps.append(repl);
                     }
                     p += e.length();
                     continue nextChar;
                  }
               }
            }
            tb.append(c);
            p++;
         }
      } finally {
         ps.append(getPlainText(tb.toString()));
         tb.delete(0, tb.length()); // flush text buffer
      }
   }

   protected CharSequence getPlaceholder(String text) {
      StringBuilder sb2 = new StringBuilder();
      sb2.append("&lt;&lt;&lt;Placeholder:");
      sb2.append(escapeHTML(text));
      sb2.append("&gt;&gt;&gt;");
      return sb2;
   }

   protected CharSequence getMacro(String text) {
      StringBuilder sb2 = new StringBuilder();
      sb2.append("&lt;&lt;Macro:");
      sb2.append(escapeHTML(text));
      sb2.append("&gt;&gt;");
      return sb2;
   }

   protected CharSequence getLink(String text) {
      if (text == null || text.length()==0 ) {
         return "[[]]";
      }
      String[] link = split(text, '|');

      if (link[0].trim().length()==0) {
         return "[[" + text + "]]";
      }
      StringBuilder sb2 = new StringBuilder();
      URI uri = null;
      try { // validate URI
         uri = new URI(link[0].trim());
      } catch (URISyntaxException e) {
      }
      if (uri != null && uri.isAbsolute()) {
         sb2.append("<a href=\"" + escapeHTML(uri.toString()) + "\" rel=\"nofollow\">");
         sb2.append(escapeHTML(link.length >= 2 && !isEmpty(link[1].trim()) ? link[1] : link[0]));
         sb2.append("</a>");
      } else {
         sb2.append("<a href=\"#\" title=\"Internal link\">");
         sb2.append(escapeHTML(link.length >= 2 && !isEmpty(link[1].trim()) ? link[1] : link[0]));
         sb2.append("</a>");
      }
      return sb2;
   }

   protected CharSequence getEmail(String text) {
      text = escapeHTML(text);
      return "<a href=\"mailto:" + text + "\">" + text + "</a>";
   }

   protected CharSequence getImage(String text) {
      StringBuilder sb2 = new StringBuilder();
      String[] link = split(text, '|');
      URI uri = null;
      try { // validate URI
         uri = new URI(link[0].trim());
      } catch (URISyntaxException e) {
      }
      if (uri != null && uri.isAbsolute()) {
         String alt = escapeHTML(link.length >= 2 && !isEmpty(link[1].trim()) ? link[1] : link[0]);
         sb2.append("<img src=\"" + escapeHTML(uri.toString()) + "\" alt=\"" + alt + "\" title=\"" + alt + "\" />");
      } else {
         sb2.append("&lt;&lt;&lt;Internal image(?): ");
         sb2.append(escapeHTML(text));
         sb2.append("&gt;&gt;&gt;");
      }
      return sb2;
   }

   protected CharSequence getFormatSection(FormatTag formatType, CharSequence text) {
      return formatType.getOpentag() + text + formatType.getClosetag();
   }

   protected CharSequence getPlainText(String text) {
      return escapeHTML(text);
   }

   protected CharSequence getNoWiki(String text) {
      return escapeHTML(replaceString(replaceString(text, "~{{{", "{{{"), "~}}}", "}}}"));
   }

   protected CharSequence getTableRow(CharSequence text) {
      return "<tr>" + text + "</tr>\n";
   }

   protected CharSequence getTableHeaderCell(CharSequence text, int colspan) {
      if (colspan > 1) {
         return "<th colspan=\"" + colspan + "\">" + text + "</th>\n";
      } else {
         return "<th>" + text + "</th>\n";
      }
   }

   protected CharSequence getTableCell(CharSequence text, int colspan) {
      if (colspan > 1) {
         return "<td colspan=\"" + colspan + "\">" + text + "</td>\n";
      } else {
         return "<td>" + text + "</td>\n";
      }
   }

   protected CharSequence getTableOpen() {
      return "<table border='1'>\n";
   }

   protected CharSequence getTableClose() {
      return "</table>\n";
   }

   protected CharSequence getHeader(CharSequence text, int level) {
      return "<h" + level + ">" + text + "</h" + level + ">\n";
   }

   protected CharSequence getHr() {
      return "\n<hr/>\n";
   }

   protected CharSequence getLineBreak() {
      return "<br/>\n";
   }

   protected CharSequence getParagraph(CharSequence text) {
      return "\n<p>" + text + "</p>\n";
   }


//  private static class EndOfContextException extends Exception {
//    private static final long serialVersionUID=1L;
//    int position;
//    public EndOfContextException(int position) {
//      super();
//      this.position=position;
//    }
//  }
//
//  private static class EndOfSubContextException extends EndOfContextException {
//    private static final long serialVersionUID=1L;
//    public EndOfSubContextException(int position) {
//      super(position);
//    }
//  }
   private class ParseState {

      private int pos;
      private StringBuilder text = new StringBuilder(2048);
      private boolean endOfContext = false;
      private boolean endOfSubContext = false;

      public int getPos() {
         return pos;
      }

      public void setPos(int pos) {
         this.pos = pos;
      }

      public StringBuilder getText() {
         return text;
      }

      public void append(char c) {
         this.text.append(c);
      }

      public void append(CharSequence text) {
         this.text.append(text);
      }

      /**
       * @return the endOfContext
       */
      public boolean isEndOfContext() {
         return endOfContext;
      }

      /**
       * @param endOfContext the endOfContext to set
       */
      public void setEndOfContext(boolean endOfContext) {
         this.endOfContext = endOfContext;
      }

      /**
       * @return the endOfSubContext
       */
      public boolean isEndOfSubContext() {
         return endOfSubContext;
      }

      /**
       * @param endOfSubContext the endOfSubContext to set
       */
      public void setEndOfSubContext(boolean endOfSubContext) {
         this.endOfSubContext = endOfSubContext;
      }
   }
}
