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

import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.cache.WebappTemplateLoader;
import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;

/**
 *
 * @author tom
 */
public class FreeMarkerRenderer implements Renderer, TemplateExceptionHandler {

   private static Configuration cfg;
   private Map root;

   public static void init(ServletContext servletContext, String contextpath) throws IOException {
      cfg = new Configuration();

      DBLoader dbloader = new DBLoader();
      WebappTemplateLoader waloader = new WebappTemplateLoader(servletContext, "templates");
      MultiTemplateLoader mtl = new MultiTemplateLoader(new TemplateLoader[]{dbloader, waloader});
      cfg.setTemplateLoader(mtl);

      cfg.setCacheStorage(new freemarker.cache.MruCacheStorage(20, 200));
      cfg.setSharedVariable("markup", new MarkupDirective(contextpath));

      DefaultObjectWrapper bw = new DefaultObjectWrapper();
      bw.setExposureLevel(DefaultObjectWrapper.EXPOSE_SAFE);
      cfg.setObjectWrapper(bw);
      cfg.setTemplateExceptionHandler(new FreeMarkerRenderer());

   }

   public FreeMarkerRenderer() throws IOException {
      root = new HashMap();
   }

   public void addContextVariable(String name, Object value) {
      root.put(name, value);
   }

   public String render(String content, String label) throws RenderException, IOException {
      StringReader reader = new StringReader(content);
      Template t = new Template(label, reader, cfg);
      StringWriter writer = new StringWriter();
      try {
         t.process(root, writer);
      } catch (TemplateException ex) {
         throw new RenderException(ex.getMessage(), ex);
      }
      return writer.toString();
   }

   public String render(String path) throws RenderException, IOException {

      Template t = cfg.getTemplate(path);
      StringWriter writer = new StringWriter();
      try {
         t.process(root, writer);
      } catch (TemplateException ex) {
         throw new RenderException(ex.getMessage(), ex);
      }
      return writer.toString();
   }

   public void handleTemplateException(TemplateException te, Environment env, java.io.Writer out) throws TemplateException {
      PrintWriter pw = new PrintWriter(out);
      pw.write("Error: " + te.getMessage());
      pw.write("{{{");
      te.printStackTrace(pw);
      pw.write("}}}\n");
   }
}
