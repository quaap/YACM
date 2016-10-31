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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author tom
 */
public class Uploader {

   private Map<String, MimeBodyPart> params = null;
   private HttpServletRequest request = null;

   public static boolean isMultiPart(HttpServletRequest req) {
      if (req == null || req.getContentType() == null) {
         return false;
      }
      return req.getContentType().toLowerCase().contains("multipart/form-data");
   }

   public Uploader(HttpServletRequest req) throws ServletException, IOException {
      request = req;
      if (isMultiPart(req)) {
         try {
            params = new HashMap<String, MimeBodyPart>();
            StreamDataSource sds = new StreamDataSource(req);
            MimeMultipart multi = new MimeMultipart(sds);

            for (int i = 0; i < multi.getCount(); i++) {
               MimeBodyPart content = (MimeBodyPart) multi.getBodyPart(i);

               String name = Integer.toString(i);
               for (String header : content.getHeader("Content-Disposition")) {
                  name = header.replaceAll(".*\\bname=\"(.+?)\".*", "$1");
               }

               params.put(name, content);

            }
         } catch (MessagingException ex) {
            throw new ServletException(ex);
         }
      }
   }

   public String getParameter(final String name) throws ServletException {
      try {
         if (params!=null && params.get(name)!=null) {
            return params.get(name).getContent().toString();
         } else {
            return request.getParameter(name);
         }
      } catch (Exception ex) {
         throw new ServletException(ex);
      }
   }

   public String getMimeType(final String name) throws ServletException {
      try {
         if (params!=null && params.get(name)!=null) {
            return params.get(name).getDataHandler().getContentType();
         }
      } catch (Exception ex) {
         throw new ServletException(ex);
      }
      return "default";
   }

   public String getFileName(final String name) throws ServletException {
      try {
         if (params!=null && params.get(name)!=null) {
            return params.get(name).getDataHandler().getName();
         }
      } catch (Exception ex) {
         throw new ServletException(ex);
      }
      return null;
   }


   public InputStream getFileDataStream(final String name) throws ServletException {
      try {
         if (params!=null && params.get(name)!=null) {
            return params.get(name).getInputStream();
         }
      } catch (Exception ex) {
         throw new ServletException(ex);
      }
      return null;
   }

   public byte[] getFileData(final String name) throws ServletException {
      if (params!=null && params.get(name)!=null) {
         int bsize = 4096;
         ByteArrayOutputStream out = new ByteArrayOutputStream(bsize);
         try {
            DataInputStream din = new DataInputStream(params.get(name).getInputStream());
            int read = 0;
            byte [] b = new byte[bsize];
            while ((read = din.read(b)) != -1) {
               out.write(b, 0, read);
            }
            return out.toByteArray();
         } catch (Exception ex) {
            throw new ServletException(ex);
         }
      }
      return null;
   }


   /**
    * This class maps the request stream to the content parser that is
    * able to pick files from it.
    */
   private class StreamDataSource implements DataSource {

      private HttpServletRequest m_req;

      public StreamDataSource(HttpServletRequest req) {
         m_req = req;
      }

      /**
       * Returns the content type for the request stream.
       */
      public String getContentType() {
         return m_req.getContentType();
      }

      /**
       * Returns a stream from the request.
       */
      public InputStream getInputStream() throws IOException {
         return m_req.getInputStream();
      }

      /**
       * This method is useless and it always returns a null.
       */
      public String getName() {
         return "butt";
      }

      public OutputStream getOutputStream() throws IOException {
         throw new UnsupportedOperationException("Not supported yet.");
      }


   }
}
