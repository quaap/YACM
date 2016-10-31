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
 */
package com.quaap.yacm.render;

import com.quaap.yacm.parser.FormatTag;
import com.quaap.yacm.storage.ContentChecker;
import com.quaap.yacm.storage.Storage;


import java.util.EmptyStackException;
import java.util.Stack;
import java.util.regex.Matcher;
import static com.quaap.yacm.parser.Utils.*;
import com.quaap.yacm.parser.WikiParser;
import com.quaap.yacm.plugin.MacroPlugin;
import com.quaap.yacm.plugin.PluginFactory;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tom
 */
public class MarkupEngine extends WikiParser {

   //private static Map<Pattern, String> macros = new LinkedHashMap<Pattern, String>();
   private static MarkupProperties props;

   public static String renderXHTML(String wikiText, MarkupProperties mprops) {
      props = mprops;
      MarkupEngine me = new MarkupEngine();
      return me.process(wikiText).toString();
   }
   private List<CharSequence> footnotes;
   private List<Header> headers;
   private ContentChecker contentchecker;

   private boolean sectionCollapse = false;

   private int headingLevel = 0;

   private List<MacroPlugin> macroplugins;

   protected MarkupEngine() {
      super();
      macroplugins = PluginFactory.getInstance().getPluginsOfType(MacroPlugin.class);
      contentchecker = new Storage(true);
      footnotes = new ArrayList<CharSequence>();
      headers = new ArrayList<Header>();
   }

   @Override
   protected StringBuilder process(String wikiText) {
      StringBuilder text = super.process(wikiText);

      if (sectionCollapse) {
         while(headingLevel-- > 0) {
            text.append("</div>closing\n");
         }
      }

      for (MacroPlugin macro: macroplugins) {
         CharSequence ptext = macro.getPrependText();
         if (ptext!=null && ptext.length()>0) {
            text.insert(0, ptext);
         }
         CharSequence atext = macro.getAppendText();
         if (atext!=null && atext.length()>0) {
            text.append(atext);
         }
      }

      if (headers.size() > 1) { // don't do TOC for just one header
         int level = 1;
         StringBuilder h = new StringBuilder(headers.size() * 128);
         h.append("<div class=\"headers\">\n");
         h.append("Table of Contents\n");
         h.append("<ol>\n");
         for (Header header : headers) {
            if (header.level == level && level > 1) {
               h.append("</li>\n");
            } else if (header.level > level) {
               h.append("<ol>\n");
               level++;
            } else if (header.level < level) {
               h.append("</li></ol></li>\n");
               level--;
            }
            h.append("<li><a href=\"#");
            h.append(header.name);
            h.append("\">");
            h.append(header.text);
            h.append("</a>\n");
         }
         while (level > 1) {
            h.append("</li></ol></li>\n");
            level--;
         }
         h.append("</ol>\n");
         h.append("</div>\n");
         text.insert(0, h);
      }

      if (footnotes.size() > 0) {
         text.append("<ul class=\"footnotes\">");
         for (int f = 0; f < footnotes.size(); f++) {
            text.append("<li><a name=\"footnote");
            text.append(f + 1);
            text.append("\"/>");
            text.append("<a href=\"#footnote-ref");
            text.append(f + 1);
            text.append("\">");
            text.append(f + 1);
            text.append("</a>. ");
            text.append(footnotes.get(f));
            text.append("</li>");
         }
         text.append("</ul>");
      }
      return text;
   }

   private String normalize(String path) {
      String newpath = "";

      String[] paths = path.split("/+");
      Stack<String> newpaths = new Stack<String>();
      for (String p : paths) {
         if (p.equals("..")) {
            try {
               newpaths.pop();
            } catch (EmptyStackException e) {
               System.out.println("normalize path extra '..'");
            }
         } else if (path.equals("...")) {
            newpaths.removeAllElements();
         } else if (!p.equals(".")) {
            newpaths.push(p);
         }
      }
      for (int i = 0; i < newpaths.size(); i++) {
         newpath += newpaths.get(i);
         if (i < newpaths.size() - 1) {
            newpath += "/";
         }
      }

      return newpath;
   }

   private String fixRelativePath(String path) {
      if (path.startsWith("/")) {
         return path;
      }
      // System.out.println("fixRelativePath in '" + path + "' currPath " + currPath);
      String newpath = path;
      if (path.startsWith("./")) {
         newpath = props.getCurrentPath() + "/" + path;
      } else if (path.startsWith(".../")) {
         newpath = path.substring(4);
      } else if (path.startsWith("$ROOT")) {
         newpath = props.getContextPath() + "/" + path.substring(5);
      } else {
         int pos = props.getCurrentPath().lastIndexOf("/");
         if (pos > 0) {
            newpath = props.getCurrentPath().substring(0, pos + 1) + path;
         }
      }
      // System.out.println("fixRelativePath out '" + newpath + "'");
      newpath = normalize(newpath);
      // System.out.println("fixRelativePath out normalized '" + newpath + "'");
      return newpath;
   }

   private String makeLinkname(String path) {
      return path.replaceFirst("^.*/", "");
   }

   @Override
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
         sb2.append("<img class=\"yacm-img external-img\" src=\"" + escapeHTML(uri.toString()) + "\" alt=\"" + alt + "\" title=\"" + alt + "\" />");
      } else {
         String alt = escapeHTML(link.length >= 2 && !isEmpty(link[1].trim()) ? link[1] : makeLinkname(link[0]));
         String path = fixRelativePath(link[0].trim());
         if (path.startsWith("/")) {
            sb2.append("<img class=\"yacm-img internal-img\" src=\"" + escapeHTML(path) + "\" alt=\"" + alt + "\" title=\"" + alt + "\" />");
         } else if (contentchecker.contentExists(path)) {
            sb2.append("<img class=\"yacm-img internal-img\" src=\"" + props.getContextPath() + "/get/" + escapeHTML(path) + "\" alt=\"" + alt + "\" title=\"" + alt + "\" />");
         } else {
            sb2.append("<a class=\"yacm-link internal-link\" href=\"" + props.getContextPath() + "/upload/" + escapeHTML(path) + "\">");
            sb2.append("create image '" + path + "'");
            sb2.append("</a>");
         }
      }
      return sb2;
   }

   @Override
   protected CharSequence getLink(String text) {
      if (text == null || text.length()==0 ) {
         return "[[]]";
      }
      String[] link = split(text, '|');

      if (link[0].trim().length()==0) {
         return "[[" + text + "]]";
      }

      String linktitle = " ";
      if (link.length>=2 && link[1]!=null && link[1].trim().length()>0) {
         linktitle = link[1];
      } else {
         linktitle = link[0];
         int len = linktitle.length();
         if (len>60) {
            linktitle = linktitle.substring(0,30) + " ... " + linktitle.substring(len-20, len);
         }
      }

      StringBuilder sb2 = new StringBuilder();
      URI uri = null;
      try { // validate URI
         uri = new URI(link[0].trim());
      } catch (URISyntaxException e) {
      }
      if (uri != null && uri.isAbsolute()) {
         sb2.append("<a class=\"yacm-link external-link\" href=\"" + escapeHTML(uri.toString()) + "\" target=\"_blank\" title=\"Link to " + escapeHTML(uri.toString()) + "\" " + (props.isIsAnon() ? "rel=\"nofollow\"" : "") + ">");
         sb2.append(escapeHTML(linktitle));
         sb2.append("</a>");
      } else {
         String path = fixRelativePath(link[0].trim());
         String extraclass = "";
         linktitle = path;
         if (!path.startsWith("/")) {
            if (!contentchecker.contentExists(path)) {
               extraclass = "new-link";
               linktitle = "Page '" + path + "' does not yet exist.";
            } else {
               linktitle = "Link to page '" + path + "'";
            }

            path = props.getContextPath() + "/view/" + path;
         } else {
            linktitle = "Link to '" + path + "'";
         }

         sb2.append("<a class=\"yacm-link internal-link " + escapeHTML(extraclass) + "\" href=\"" + escapeHTML(path) + "\" title=\"" + escapeHTML(linktitle) + "\" >");
         sb2.append(escapeHTML(link.length >= 2 && !isEmpty(link[1].trim()) ? link[1] : makeLinkname(link[0])));
         sb2.append("</a>");
      }
      return sb2;
   }

   @Override
   protected CharSequence getEmail(String text) {
      String mail = "";
      String maildisp = "";
      for(int i=0;i<text.length();i++) {
         mail += "'" + text.charAt(i) + "'";
         maildisp += "&#x" + Integer.toHexString(text.charAt(i)) + ";";
         if (i<text.length()-1) mail += "+" ;
      }
      return "<a href=\"#sendmail\" onclick=\"this.href='ma'+'ilto'+':'+" + mail + "\">" + maildisp + "</a>";
   }


   @Override
   protected CharSequence getPlainText(String text) {
      if (props.isEscapeHTML()) {
         return escapeHTML(text);
      } else {
         return text;
      }
   }


   @Override
   protected CharSequence getMacro(String text) {
      if (text == null) {
         return "null text in macro?";
      }
      StringBuilder sb2 = new StringBuilder();

      boolean matched = false;

      for (MacroPlugin macro: macroplugins) {
         Matcher m = macro.getPattern().matcher(text);
         if (m.matches()) {
            matched = true;
            sb2.append(macro.processMacro(m, text));
         }
      }

      if (!matched) {
         sb2.append("&lt;&lt;Unknown Macro:");
         sb2.append(escapeHTML(text));
         sb2.append("&gt;&gt;");
      }
      return sb2;
   }
   private int rowcount = 0;

   @Override
   protected CharSequence getTableOpen() {
      rowcount = 0;
      return "<table class=\"yacm-table\">\n";
   }

   @Override
   protected CharSequence getTableRow(CharSequence text) {
      rowcount++;
      return "<tr class=\"yacm-table-row " + (rowcount % 2 == 0 ? "row-even" : "row-odd") + "\">" + text + "</tr>\n";
   }

   private int hnum=0;
   @Override
   protected CharSequence getHeader(CharSequence text, int level) {
      hnum++;
      String name = text.toString().trim().replaceAll("\\W+", "_") + "_" + hnum;
      String id = "_" + name;
      headers.add(new Header(level, name, text));

      //CharSequence header = super.getHeader(text, level);

      StringBuilder sb = new StringBuilder(text.length() + 100);

      if (sectionCollapse) {
         if (level == headingLevel) {
            sb.append("</div>\n");
         } else {
            while(level <= headingLevel) {
               sb.append("</div>\n");
               headingLevel--;
            }
         }

         if (headingLevel==0) headingLevel=1;

         while(level-1 > headingLevel) {
            sb.append("<div class=\"collapsable-section-opened\">\n");
            headingLevel++;
         }
      }

      sb.append("<a name='");
      sb.append(name);
      sb.append("'/>");

      if (sectionCollapse) {
         sb.append("<h");
         sb.append(level);
         sb.append(" id='");
         sb.append(id);
         sb.append("' class=\"collapsable-link-opened\" onclick=\"showHideSection(this, '");
         sb.append(id);
         sb.append("-section'); return false;\">");
         sb.append(text);
         sb.append("</h");
         sb.append(level);
         sb.append(">\n");

         if(level > headingLevel) {
            headingLevel++;
         }
         sb.append("<div id='");
         sb.append(id);
         sb.append("-section' class='collapsable-section-opened'>\n");
      } else {
         sb.append("<h");
         sb.append(level);
         sb.append(">");
         sb.append(text);
         sb.append("</h");
         sb.append(level);
         sb.append(">\n");

      }
      return sb;
   }

   @Override
   protected CharSequence getFormatSection(FormatTag formatType, CharSequence text) {
      if (formatType.getName().equals("footnote")) {
         footnotes.add(text);
         int note = footnotes.size();
         return "<a name=\"footnote-ref" + note + "\"/><sup class=\"footnote-link\"><a href=\"#footnote" + note + "\">" + note + "</a></sup>";
      } else {
         return super.getFormatSection(formatType, text);
      }
   }

   public static String escapeURL(String s) {
      try {
         return URLEncoder.encode(s, "utf-8");
      } catch (UnsupportedEncodingException e) {
         e.printStackTrace();
         return null;
      }
   }

   private class Header {

      public int level = 0;
      public CharSequence name = "";
      public CharSequence text = "";

      public Header(int level, CharSequence name, CharSequence text) {
         this.level = level;
         this.name = name;
         this.text = text;
      }
   }
}
