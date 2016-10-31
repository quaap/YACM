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
package com.quaap.yacm.render;


import com.quaap.yacm.storage.bean.ContentPermission.Rights;

public enum MarkupType {

   //NOTICE: Do not change the IDs of the types.  They end up stored in the database.
   wiki(1, "Wiki markup (default, html escaped)", Rights.edit),
   htmlescape(5, "Plain text (html escaped)", Rights.edit),
   wikitemplating(2, "Templating + wiki markup (html escaped)", Rights.programming),
   templating(6, "Templating (html escaped)", Rights.programming),
   wikihtml(3, "Templating + wiki markup (html not escaped)", Rights.html),
   htmltemplating(4, "Templating (html not escaped)", Rights.html),
   none(0, "Unprocessed (html not escaped)", Rights.html);

   public static MarkupType getMarkupType(int id) {
      for (MarkupType r : MarkupType.values()) {
         if (r.id == id) {
            return r;
         }
      }
      return null;
   }
   
   private int id;
   private String description;
   private Rights perm;

   MarkupType(int id, String description, Rights perm) {
      this.id = id;
      this.description = description;
      this.perm = perm;
   }

   public int getId() {
      return id;
   }

   public String getDescription() {
      return description;
   }

   public Rights getRequiredPermission() {

      return perm;
   }
}