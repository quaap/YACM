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

import java.io.IOException;
import java.io.InputStream;

import java.util.Properties;

/**
 *
 * @author tom
 */
public class MimeTypes {

   static Properties properties;


   static {
      reloadTypes();
   }

   public static void reloadTypes() {
      InputStream in = MimeTypes.class.getResourceAsStream("mimetypes.properties");
      properties = new Properties();
      try {
         properties.load(in);
      } catch (IOException ex) {
         System.out.println(ex.getMessage());
      }
   }

   public static String getMimeType(String filename, String defaultType) {
      for (String p : properties.stringPropertyNames()) {
         if (filename.toLowerCase().endsWith(p.toLowerCase())) {
            return properties.getProperty(p, "missingtype");
         }
      }
      return defaultType;
   }

   public static String getMimeType(String filename) {
      return getMimeType(filename, "application/octet-stream");
   }
}
