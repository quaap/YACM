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

import com.quaap.yacm.plugin.Plugin;
import com.quaap.yacm.plugin.PluginFactory;
import com.quaap.yacm.plugin.PluginFactory.PluginInstantiationException;
import com.quaap.yacm.render.MarkupEngine;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tom
 */
public class DefaultMacroLoaderPlugin implements Plugin {


   public void init(String... params) {
      //System.out.println("DefaultMacroLoaderPlugin");
      PluginFactory plugins = PluginFactory.getInstance();
      InputStream in = DefaultMacroLoaderPlugin.class.getClassLoader().getResourceAsStream("macros.properties");
      if (in != null) {
         Properties macrosprops = new Properties();
         try {
            macrosprops.load(in);
            for (String name : macrosprops.stringPropertyNames()) {
               if (name.endsWith(".find")) {
                  int pos = name.indexOf(".find");
                  String mname = name.substring(0, name.lastIndexOf("."));
                  String prefix = name.substring(0, pos);
                  String find = macrosprops.getProperty(name);
                  String repl = macrosprops.getProperty(prefix + ".repl");
                  plugins.addPlugin(mname, "com.quaap.yacm.plugin.plugins.DefaultMacroPlugin", mname, find, repl);
               }

            }
         } catch (PluginInstantiationException ex) {
            Logger.getLogger(DefaultMacroLoaderPlugin.class.getName()).log(Level.SEVERE, null, ex);
         } catch (IOException ex) {
            Logger.getLogger(MarkupEngine.class.getName()).log(Level.SEVERE, null, ex);
         }
      }
   }

   @Override
   public DefaultMacroLoaderPlugin clone() {
      return this;
   }


   public String getName() {
      return "DefaultMacroLoaderPlugin";
   }

   public void destroy() {
   }

}
