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

import com.quaap.yacm.storage.Storage;
import com.quaap.yacm.storage.bean.Content;
import freemarker.cache.TemplateLoader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 *
 * @author tom
 */
public class DBLoader implements TemplateLoader {

   private Storage store;

   public DBLoader() {
      this.store = new Storage(true);
   }

   public Object findTemplateSource(String name) throws IOException {
      if (store.contentExists(name)) {
         return name;
      } else {
         return null;
      }
   }

   public long getLastModified(Object templateSource) {
      return store.getContentDate((String) templateSource).getTime();
   }

   public Reader getReader(Object templateSource, String encoding) throws IOException {
      Content c = store.getContent((String) templateSource);
      StringReader reader = new StringReader(c.getContent());
      return reader;
   }

   public void closeTemplateSource(Object templateSource) throws IOException {
   }
}
