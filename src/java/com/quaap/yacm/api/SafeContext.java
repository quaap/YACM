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

package com.quaap.yacm.api;

import com.quaap.yacm.ContentNotFoundException;
import com.quaap.yacm.Context;
import com.quaap.yacm.PermissionException;
import com.quaap.yacm.api.beans.SafeContent;
import com.quaap.yacm.api.beans.SafeContentPermission;
import com.quaap.yacm.api.beans.SafeMetadata;
import com.quaap.yacm.api.beans.SafeUser;
import com.quaap.yacm.storage.bean.ContentPermission;
import com.quaap.yacm.utils.DiffPatch.Diff;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tom
 */
public class SafeContext {
   private SafeApi safeapi;

   private Context unsafecontext;
   private List<SafeContentPermission> perms;
   private List<SafeContent> recentuniquepages = null;

   private Map<String,Object> lexicals = new HashMap<String,Object>();

   public SafeContext(Context context) {
      this.unsafecontext = context;
      this.safeapi = new SafeApi(context);
   }

   public String getPath() {
      return unsafecontext.getPath();
   }

   public String getAction() {
      return unsafecontext.getAction();
   }

   public Object getLexical(String name) {
      return lexicals.get(name);
   }

   public void putLexical(String name, Object value) {
      lexicals.put(name, value);
   }

   public List<String> getRecentPages() {

      return unsafecontext.getRecentPages();
   }


   public List<SafeContent> getUniqueRecentPages() {
      if (recentuniquepages == null) {

         List<SafeContent> list = new ArrayList<SafeContent>();
         Map<String,Boolean> seen = new HashMap<String, Boolean>();

         for (String page: unsafecontext.getRecentPages()) {
            if (!seen.containsKey(page)) {
               try {
                  list.add(0, new SafeContent(unsafecontext.getApi().getContentForPath(page)));
               } catch (ContentNotFoundException ex) {
                  //ex.printStackTrace();
               }
               seen.put(page, true);
            }
         }
         recentuniquepages = list;
      }
      return recentuniquepages;
   }

   public boolean hasSubpages() {
      return unsafecontext.getApi().getSubPageCount(getPath())>0;
   }

   public SafeContent getContent() throws PermissionException, ContentNotFoundException {
      return unsafecontext.getApi().makeSafe(unsafecontext.getContent());
   }

   public SafeUser getUser() {
      return new SafeUser(unsafecontext.getUser());
   }

   public boolean isAdmin() {
      return unsafecontext.isAdmin();
   }

   public boolean isAnon() {
      return unsafecontext.isAnon();
   }

   public boolean hasRight(ContentPermission.Rights right) {
      return unsafecontext.hasPermission(right);
   }


   public boolean hasAdminRight() {
      return unsafecontext.hasPermission(ContentPermission.Rights.admin);
   }

   public boolean hasAddRight() {
      return unsafecontext.hasPermission(ContentPermission.Rights.add);
   }

   public boolean hasCommentRight() {
      return unsafecontext.hasPermission(ContentPermission.Rights.comment);
   }

   public boolean hasHistoryRight() {
      return unsafecontext.hasPermission(ContentPermission.Rights.history);
   }

   public boolean hasDeleteRight() {
      return unsafecontext.hasPermission(ContentPermission.Rights.delete);
   }

   public boolean hasEditRight() {
      return unsafecontext.hasPermission(ContentPermission.Rights.edit);
   }

   public boolean hasViewRight() {
      return unsafecontext.hasPermission(ContentPermission.Rights.view);
   }

   public boolean hasHtmlRight() {
      return unsafecontext.hasPermission(ContentPermission.Rights.html);
   }

   public boolean hasProgrammingRight() {
      return unsafecontext.hasPermission(ContentPermission.Rights.programming);
   }

   public List<SafeContentPermission> getPermissions() {
      if (perms==null) {
         perms = new ArrayList<SafeContentPermission>();
         for (ContentPermission cp: unsafecontext.getPermissions()) {
            perms.add(new SafeContentPermission(cp));
         }
      }
      return perms;
   }


   public boolean hasComments() throws ContentNotFoundException {
      return unsafecontext.getApi().hasComments(getPath());
   }

   public long getCommentCount() throws ContentNotFoundException {
      return unsafecontext.getApi().getCommentCount(getPath());
   }

   public boolean isContentAction() {
      return unsafecontext.isContentAction();
   }

   private List<SafeMetadata> metadata = null;
   public List<SafeMetadata> getMetadata() {
      if (metadata == null) {
         metadata = unsafecontext.getApi().makeSafeMetadata(unsafecontext.getMetadataList());
      }
      return metadata;
   }

   public long getViewCount() {
      return unsafecontext.getStore().getVisitCountForPath(getPath(), "view", true);
   }

   public long getSaveCount() {
      return unsafecontext.getStore().getVisitCountForPath(getPath(), "save", false);
   }

//   public String getSecurityKey() {
//      return unsafecontext.getSecurityKey();
//   }

   public List<Diff> getContentDiff(String version1, String version2) {
      return unsafecontext.getStore().getContentDiff(getPath(),Integer.parseInt(version1),Integer.parseInt(version2));
   }

   public List<Diff> getContentDiff(int version1, int version2) {
      return unsafecontext.getStore().getContentDiff(getPath(),version1,version2);
   }

   public List<Diff> getContentDiff(String version) {
      return getContentDiff(Integer.parseInt(version));
   }

   public List<Diff> getContentDiff(int version) {
      return unsafecontext.getStore().getContentDiff(getPath(), version);
   }


   public String getServerURL() {
      return unsafecontext.getServerURL();
   }

   public String getServerBaseURL() {
      return unsafecontext.getServerBaseURL();
   }

   public String getPageURL() {
      return unsafecontext.getPageURL();
   }

   public String getPageURI() {
      return unsafecontext.getPageURI();
   }

   public SafeApi getApi() {
      return safeapi;
   }

   public String getNewCommentPath() {
      String uname = "";
      if (isAnon()) {
         uname = "anon";
      } else {
         uname = getUser().getUsername();
      }

      return getPath() + "/" + uname + "-" + Long.toHexString((new Date().getTime())/100);
   }
}
