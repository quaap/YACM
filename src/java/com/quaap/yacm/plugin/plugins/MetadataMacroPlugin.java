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

package com.quaap.yacm.plugin.plugins;

import com.quaap.yacm.plugin.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  skeleton for a demo
 * @author tom
 */
public class MetadataMacroPlugin implements MacroPlugin {

   private Pattern pattern = Pattern.compile("metadata (.*)");

   public void init(String ... params) {
      for (String p: params) {
         //System.out.println(p);
      }
   }

   public void destroy() {
      // nothing to destroy
   }

   public String getName() {
      return "MetadataMacro";
   }

   @Override
   public MetadataMacroPlugin clone() {
      return this;
   }

   public Pattern getPattern() {
      return pattern;
   }

   public CharSequence processMacro(Matcher matcher, CharSequence macrotext) {
      return "metadata " + macrotext;
   }

   public CharSequence getPrependText() {
      return ""; //Nothing to do for these
   }

   public CharSequence getAppendText() {
      return ""; //Nothing to do for these
   }

}
