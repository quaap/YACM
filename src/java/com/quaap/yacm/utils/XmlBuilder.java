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
package com.quaap.yacm.utils;

public final class XmlBuilder {

   /**
    * Escapes greater than, less than, double quotes, single quotes, ampersands,
    * and invalid characters.
    *
    * The text returned is suitable for use in the text or value portions of an
    * XML document.
    *
    * @param value  The string to escape
    * @return  The escaped value
    */
   public static CharSequence xmlEscape(final CharSequence value) {

      if (value==null) {
         return "";
      }

      StringBuffer xout = new StringBuffer(value.length() + 32);

      for (int i = 0; i < value.length(); i++) {
         char c = value.charAt(i);
         if (!isValidXMLChar(c)) {
            xout.append("[X");
            xout.append(Integer.toHexString(c));
            xout.append("]");
         } else {
            switch (c) {
               case '&':
                  xout.append("&amp;");
                  break;
               case '<':
                  xout.append("&lt;");
                  break;
               case '>':
                  xout.append("&gt;");
                  break;
               case '"':
                  xout.append("&quot;");
                  break;
               case '\'':
                  xout.append("&apos;");
                  break;

               default:
                  xout.append(c);
                  break;
            }
         }
      }
      return xout.toString();
   }

   /**
    * Scan the given text value and replace invalid characters.  Also properly escape
    * the sequence "]]>" so the the cdata section is not prematurely ended.
    *
    * This does not perform XML escaping, such as changing "&" to "&amp;" etc, because
    * those values are usable in a cdata section.
    *
    * The returned text is suitable for including in a CDATA section
    *
    * @param value  The text to escape.
    * @return  The sanitized and escaped string.
    */
   public static String cdataEscape(final CharSequence value) {
      return xmlSanitize(value).replaceAll("\\]\\]>", "]]>" + "]]&gt;" + "<![CDATA[");
   }

   /**
    * Scan the given text value and replace invalid characters.
    * This does not perform XML escaping, such as changing "&" to "&amp;" etc.
    *
    * @param value
    * @return  The sanitized string.
    */
   public static String xmlSanitize(final CharSequence value) {

      if (value==null) {
         return "";
      }
      StringBuffer xout = new StringBuffer(value.length() + 32);

      for (int i = 0; i < value.length(); i++) {
         char c = value.charAt(i);
         if (!isValidXMLChar(c)) {
            xout.append("[X");
            xout.append(Integer.toHexString(c));
            xout.append("]");
         } else {
            xout.append(c);
         }
      }
      return xout.toString();
   }

   /**
    * Detemine if the character is a valid character for an XML document.
    *
    * @param c
    * @return  true if the character falls into one of the forbidden ranges.
    */
   public static final boolean isValidXMLChar(char c) {
      if ((c >= 0x00 && c <= 0x08) ||
              (c >= 0x0B && c <= 0x0C) ||
              (c >= 0x0E && c <= 0x1F) ||
              (c >= 0x7F && c <= 0x84) ||
              (c >= 0x86 && c <= 0x9F)) {
         return false;
      } else {
         return true;
      }
   }

   public static int makeXMLElement(StringBuffer xml, int indent, final String name, final CharSequence value, final String... attributes) {
      return makeXMLElement(xml, indent, name, value, true, attributes);
   }

   public static int makeXMLElement(StringBuffer xml, int indent, final String name, final CharSequence value, boolean escapeValue, final String... attributes) {
      for (int i = 0; i < indent; i++) {
         xml.append(" ");
      }
      xml.append("<");
      xml.append(name);
      handleAttributes(xml, indent, attributes);
      if (value == null || value.equals("")) {
         xml.append("/>\n");
      } else {
         xml.append(">");
         if (escapeValue) {
            xml.append(xmlEscape(value));
         } else {
            xml.append(value);
         }
         xml.append("</");
         xml.append(name);
         xml.append(">\n");
      }

      return indent;
   }

   public static int makeXMLOpenTag(StringBuffer xml, int indent, final String name, final String... attributes) {
      for (int i = 0; i < indent; i++) {
         xml.append(" ");
      }
      xml.append("<");
      xml.append(name);
      handleAttributes(xml, indent, attributes);
      xml.append(">\n");
      return ++indent;
   }

   public static int makeXMLCloseTag(StringBuffer xml, int indent, final String name) {
      indent--;
      for (int i = 0; i < indent; i++) {
         xml.append(" ");
      }
      xml.append("</");
      xml.append(name);
      xml.append(">\n");
      return indent;
   }

   private static void handleAttributes(StringBuffer xml, int indent, final String[] attributes) {
      if (attributes != null) {
         if (attributes.length % 2 != 0) {
            throw new IllegalArgumentException("Unmatched entry in the attributes array: " + attributes.length + " entries in the array.");
         } else {
            for (int i = 0; i < attributes.length; i += 2) {
               xml.append(" ");
               xml.append(attributes[i]);
               xml.append("=\"");
               xml.append(xmlEscape(attributes[i + 1]));
               xml.append("\"");
            }
         }
      }

   }
}

