/*
 *    Copyright (c) 2009 Thomas Kliethermes, thamus@kc.rr.com
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.quaap.yacm.plugin;

import com.quaap.yacm.plugin.plugins.DefaultMacroLoaderPlugin;
import com.quaap.yacm.render.MarkupEngine;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tom
 */
public class PluginFactory {

   private Map<String, Plugin> plugins = new HashMap<String, Plugin>();

   private static PluginFactory factory = new PluginFactory();

   public static PluginFactory getInstance() {
      return factory;
   }


   private PluginFactory() {

   }

   public void init() {
      InputStream in = PluginFactory.class.getClassLoader().getResourceAsStream("plugins.properties");
      if (in != null) {
         Properties props = new Properties();
         try {
            props.load(in);
            for (String name : props.stringPropertyNames()) {
               if (name.endsWith(".class")) {
                  String pluginclass = props.getProperty(name);
                  //MacroPlugin plugin = (MacroPlugin) Class.forName(macroclass).newInstance();
                  List<String> params = new ArrayList<String>();
                  int pos = name.indexOf(".class");
                  String prefix = name.substring(0, pos);
                  for (int i = 1; i < 20; i++) {
                     String param = props.getProperty(prefix + ".param" + i);
                     if (param != null) {
                        params.add(param);
                     }
                  }
                  addPlugin(prefix, pluginclass, params.toArray(new String[0]));

               }
            }

         } catch (PluginInstantiationException ex) {
            Logger.getLogger(DefaultMacroLoaderPlugin.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
         } catch (IOException ex) {
            Logger.getLogger(MarkupEngine.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
         }
      }

   }

//   public void addPlugin(String pluginclass, String ... params) throws PluginInstantiationException {
//      addPlugin(pluginclass, params);
//   }

   public void addPlugin(String alias, String pluginclass, String ... params) throws PluginInstantiationException {
      try {
         Plugin p = (Plugin) Class.forName(pluginclass).newInstance();
         p.init(params);
         if (alias==null) {
            alias = p.getName();
         }
         if (plugins.containsKey(alias)) {
            throw new DuplicatePluginNameException(alias);
         }
         plugins.put(alias, p);

      } catch (Exception ex) {
         throw new PluginInstantiationException(ex);
      }

   }

   public void addPlugin(String alias, Plugin plugin) throws DuplicatePluginNameException {
      if (alias==null) {
         alias = plugin.getName();
      }
      if (plugins.containsKey(alias)) {
         throw new DuplicatePluginNameException(alias);
      }
      plugins.put(alias, plugin);
   }

   public <T extends Plugin> T getPlugin(final String name) throws NoSuchPluginException {
      if (plugins.containsKey(name)) {
         return (T)plugins.get(name).clone();
      }
      throw new NoSuchPluginException(name);
   }



   public void destroy() {
      // destroy each plugin;
   }


  // private Map<Class<? extends Plugin>, List<? extends Plugin>> typedLists = new HashMap<Class<? extends Plugin>, List<? extends Plugin>>();

   public <T extends Plugin> List<T> getPluginsOfType(Class<T> type) {
     // System.out.println("getting plugins of type " + type.getName());

      //if (!typedLists.containsKey(type)) {
         List<T> typedList = new ArrayList<T>();
         for (Plugin p: plugins.values()) {
            //System.out.println(p.getName());
            if (type.isInstance(p)) {
              // System.out.println(p.getName() + " matches");
               typedList.add((T) p.clone());
            }
         }
         return typedList;
        // typedLists.put(type, typedList);
     // }

      //return (List<T>)typedLists.get(type);
   }













   public class DuplicatePluginNameException extends Exception {
      public DuplicatePluginNameException(String pluginName) {
         super(pluginName);
      }
   }

   
   public class NoSuchPluginException extends Exception {
      public NoSuchPluginException(String pluginName) {
         super(pluginName);
      }
   }


   public class PluginInstantiationException extends Exception {
      public PluginInstantiationException(String message) {
         super(message);
      }

      public PluginInstantiationException(Exception e) {
         super(e);
      }
   }
}
