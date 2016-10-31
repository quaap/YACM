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


import com.quaap.yacm.api.SafeContext;
//import com.quaap.yacm.cache.Cache;
//import com.quaap.yacm.cache.CacheService;
import com.quaap.yacm.utils.XmlBuilder;
import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.StringModel;
import freemarker.template.Configuration;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

/**
 *
 * @author tom
 */
public class MarkupDirective implements TemplateDirectiveModel {

   //private static final Cache<String,CharSequence> cache = CacheService.getCache("sectionrender", 20, 200, 500);

   private String servletContextPath;

   public MarkupDirective(String contextPath) {
      this.servletContextPath = contextPath;
   }

   public static final String [] parameters = new String [] {"template", "contextpath", "markuplevel", "cachekey", "cachetimeout"};
   public void execute(Environment env, Map params, TemplateModel[] loopVars,
           TemplateDirectiveBody body) throws TemplateException, IOException {

      
      Configuration cfg = env.getConfiguration();
      for (Object name : params.keySet()) {
         boolean valid = false;
         for (String param: parameters) {
            if (((String) name).equals(param)) {
               valid = true;
            }
         }
         if (!valid) {
            throw new TemplateModelException("Parameter '" + name + "' not allowed.");
         }
      }

      String template = getStringParamValue(params, "template");

      String docContextPath = getStringParamValue(params, "contextpath");
//
//      String cachekey = getStringParamValue(params, "cachekey");
//
//      if (cachekey == null) {
//         cachekey = template;
//      }

//      Integer cachetimeout = getIntParamValue(params, "cachetimeout");
//      if (cachetimeout==null) {
//         cachetimeout = 0;
//      }
//
//      if (cachekey!=null) {
//         CharSequence value = cache.get(cachekey);
//         if (value!=null) {
//            System.out.println("Using cachekey '" + cachekey + "', Cachetimeout '" + cachetimeout + "', ");
//
//            env.getOut().append(value);
//            return;
//         }
//         System.out.println("Cachekey '" + cachekey + "', Cachetimeout '" + cachetimeout + "', ");
//
//      }

      Boolean isAnon = true;

      BeanModel bcontext = (BeanModel) env.getVariable("context");
      if (bcontext != null) {
         SafeContext context = null;
         context = (SafeContext) (bcontext.getWrappedObject());

         if (context != null) {

            if (docContextPath == null) {
               //Way to get the path of the current page
               docContextPath = context.getPath();
            //System.out.println("context path '" + docContextPath + "'");
            }
            try {
               isAnon = context.getContent().getLastModifier().getUsername().equalsIgnoreCase("anonymous");
            } catch (Exception ex) {
            }
         }
         //System.out.println("isAnon(): " + isAnon);

      }

      Integer markuplevel = getIntParamValue(params, "markuplevel");

      if (markuplevel == null) markuplevel = MarkupType.wiki.getId();

      MarkupType markuptype = MarkupType.getMarkupType(markuplevel);

      MarkupProperties props = new MarkupProperties();

      props.setContextPath(servletContextPath);
      props.setCurrentPath(docContextPath);
      props.setEscapeHTML(true);
      props.setIsAnon(isAnon);

      if (template != null) {
         Template t = cfg.getTemplate(template);
         StringWriter writer = new StringWriter(2048);
         StringWriter outwriter = new StringWriter(2048);
         switch(markuptype) {
            case wiki:
               t.dump(writer);
               outwriter.append(MarkupEngine.renderXHTML(writer.toString(), props));
               //env.getOut().append(cacheIt(MarkupEngine.renderXHTML(writer.toString(), props), cachekey, cachetimeout));
               break;
            case wikitemplating:
               t.process(env.getDataModel(), writer);
               outwriter.append(MarkupEngine.renderXHTML(writer.toString(), props));
               //env.getOut().append(cacheIt(MarkupEngine.renderXHTML(writer.toString(), props), cachekey, cachetimeout));
               break;
            case wikihtml:
               t.process(env.getDataModel(), writer);
               props.setEscapeHTML(false);
               outwriter.append(MarkupEngine.renderXHTML(writer.toString(), props));
               //env.getOut().append(cacheIt(MarkupEngine.renderXHTML(writer.toString(), props), cachekey, cachetimeout));
               break;
            case templating:
               t.process(env.getDataModel(), writer);
               outwriter.append(XmlBuilder.xmlEscape(writer.getBuffer()));
               //env.getOut().append(cacheIt(XmlBuilder.xmlEscape(writer.getBuffer()), cachekey, cachetimeout));
               break;
            case htmltemplating:
               t.process(env.getDataModel(), outwriter);
               //env.getOut().append(cacheIt(writer.toString(), cachekey, cachetimeout));
               break;
            case htmlescape:
               t.dump(outwriter);
              // env.getOut().append("<pre>");
               //env.getOut().append(cacheIt(XmlBuilder.xmlEscape(writer.getBuffer()), cachekey, cachetimeout));
              // env.getOut().append("</pre>");
               break;
            case none:
               //StringWriter out = new StringWriter();
               //t.dump(env.getOut());
               t.dump(outwriter);
               //cacheIt(out.getBuffer(), cachekey, cachetimeout);
               break;
            default:
               throw new TemplateModelException("Unknown markup type: '" + markuptype + "'");
         }
         CharSequence out = outwriter.getBuffer();
//         if (cachekey!=null && cachetimeout>0) {
//            cache.put(cachekey, out, cachetimeout);
//         }
         env.getOut().append(out);

      }


      if (body != null) {
         StringWriter writer = new StringWriter(2048);
         StringWriter outwriter = new StringWriter(2048);
         body.render(writer);
         String text;
         switch(markuptype) {
            case wiki:
               outwriter.append(MarkupEngine.renderXHTML(writer.toString(), props));
              // env.getOut().append(cacheIt(MarkupEngine.renderXHTML(writer.toString(), props), cachekey, cachetimeout));
               break;
            case wikitemplating:
               text = processAsTemplate(writer.toString(), env);
               outwriter.append(MarkupEngine.renderXHTML(text, props));
               //env.getOut().append(cacheIt(MarkupEngine.renderXHTML(text, props), cachekey, cachetimeout));
               break;
            case wikihtml:
               text = processAsTemplate(writer.toString(), env);
               props.setEscapeHTML(false);
               outwriter.append(MarkupEngine.renderXHTML(text, props));
               //env.getOut().append(cacheIt(MarkupEngine.renderXHTML(text, props), cachekey, cachetimeout));
               break;
            case templating:
               text = processAsTemplate(writer.toString(), env);
               outwriter.append(XmlBuilder.xmlEscape(text));
               //env.getOut().append(cacheIt(XmlBuilder.xmlEscape(text), cachekey, cachetimeout));
               break;
            case htmltemplating:
               text = processAsTemplate(writer.toString(), env);
               outwriter.append(text);
               //env.getOut().append(cacheIt(text, cachekey, cachetimeout));
               break;
            case htmlescape:
               //env.getOut().append("<pre>");
               outwriter.append(XmlBuilder.xmlEscape(writer.getBuffer()));
               //env.getOut().append(cacheIt(XmlBuilder.xmlEscape(writer.getBuffer()), cachekey, cachetimeout));
               //env.getOut().append("</pre>");
               break;
            case none:
               outwriter = writer;
               //env.getOut().append(cacheIt(writer.getBuffer(), cachekey, cachetimeout));
               break;
            default:
               throw new TemplateModelException("Unknown markup type: '" + markuptype + "'");
         }

         CharSequence out = outwriter.getBuffer();
//         if (cachekey!=null && cachetimeout>0) {
//            cache.put(cachekey, out, cachetimeout);
//         }
         env.getOut().append(out);

      }
   }


   private String getStringParamValue(Map params, String name) {
      TemplateModel paramValue = (TemplateModel) params.get(name);
      if (paramValue != null && (paramValue instanceof StringModel || paramValue instanceof SimpleScalar)) {
         return paramValue.toString();
      }
      return null;
   }

   private Integer getIntParamValue(Map params, String name) throws TemplateModelException {
      TemplateModel paramValue = (TemplateModel) params.get(name);
      if (paramValue == null || !(paramValue instanceof TemplateNumberModel)) {
         return null;
      }
      return ((TemplateNumberModel) paramValue).getAsNumber().intValue();
   }

   private String processAsTemplate(String text, Environment env) throws IOException, TemplateException {
      StringReader reader = new StringReader(text);
      Template t = new Template("markup", reader, env.getConfiguration());
      StringWriter writer = new StringWriter();
      t.process(env.getDataModel(), writer);
      return writer.toString();
   }
}
