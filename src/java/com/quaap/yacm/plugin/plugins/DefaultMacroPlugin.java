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
package com.quaap.yacm.plugin.plugins;

import com.quaap.yacm.plugin.MacroPlugin;
import com.quaap.yacm.utils.XmlBuilder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author tom
 */
public class DefaultMacroPlugin implements MacroPlugin {

   private Pattern pattern;
   private String repl;
   private String name;

   public void init(String... params) {
      if (params.length != 3) {
         throw new IllegalArgumentException("This macro should receive exactly 3 paramters!");
      }
      name = params[0];
      pattern = Pattern.compile(params[1]);
      repl = params[2];
      if (repl == null) {
         repl = "";
      }
   }

   @Override
   public DefaultMacroPlugin clone() {
      return this;
   }

   public Pattern getPattern() {
      return pattern;
   }

   public CharSequence processMacro(Matcher matcher, CharSequence macrotext) {
      String newtext = repl;
      for (int i = 1; i <= matcher.groupCount(); i++) {
         newtext = newtext.replaceAll("\\$" + i, XmlBuilder.xmlEscape(matcher.group(i)).toString());
      }

      return newtext;
   }

   public String getName() {
      return name;
   }

   public void destroy() {
   }

   public CharSequence getPrependText() {
      return ""; //Nothing to do for these
   }

   public CharSequence getAppendText() {
      return ""; //Nothing to do for these
   }

}
