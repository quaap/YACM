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

import com.quaap.yacm.Api;
import com.quaap.yacm.ContentNotFoundException;
import com.quaap.yacm.Context;
import com.quaap.yacm.api.beans.SafeContent;
import com.quaap.yacm.api.beans.SafeGroup;
import com.quaap.yacm.render.MarkupType;
import com.quaap.yacm.api.beans.SafeUser;
import com.quaap.yacm.storage.bean.ContentPermission.Rights;
import com.quaap.yacm.storage.bean.ContentState;
import com.quaap.yacm.storage.bean.Group;
import com.quaap.yacm.storage.bean.User;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author tom
 */
public class SafeApi {

   //private Context unsafecontext;
   private Api unsafeapi;

   public SafeApi(Context context) {
     // unsafecontext = context;
      unsafeapi = context.getApi();
   }

   public String getUrl(String action, String path) {
      return unsafeapi.getUrl(action, path);
   }

   public String getUrl(String action, String path, String params) {
      return unsafeapi.getUrl(action, path, params);
   }

   public String getAbsUrl(String action, String path) {
      return unsafeapi.getContext().getServerBaseURL() + unsafeapi.getUrl(action, path);
   }

   public String getAbsUrl(String action, String path, String params) {
      return unsafeapi.getContext().getServerBaseURL() + unsafeapi.getUrl(action, path, params);
   }

   public String getUserLink(final SafeUser user) {
      return unsafeapi.getUserLink(user);
   }

   public String XMLEncode(final String text) {
      return unsafeapi.XMLEncode(text);
   }

   public String URLEncode(final String text) {
      return unsafeapi.URLEncode(text);
   }

   public String truncate(final String text, final int max) {
      if (text==null) return "";
      if (text.length()<max) return text;
      return text.substring(0, max-3) + "...";

   }

   public boolean hasPermission(SafeUser user, SafeContent content, Rights right) {
      return unsafeapi.hasPermission(content.getPath(), unsafeapi.makeUnSafe(user), right);
   }

   public List<SafeContent> search(String searchtext, int max) {
      return unsafeapi.makeSafe(unsafeapi.search(searchtext, max));
   }

   public SafeUser getUser(String username) {
      User user = unsafeapi.getUser(username);
      if (user!=null) return new SafeUser(user);
      return null;
   }

   public SafeGroup getGroup(String groupname) {
      Group group = unsafeapi.getGroup(groupname);
      if (group!=null) return new SafeGroup(group);
      return null;
   }

   public boolean isAnon(SafeUser user) {
      return unsafeapi.isAnon(user.getUsername());
   }

   public boolean isAnon(SafeGroup group) {
      return unsafeapi.isAnon(group.getGroupname());
   }

   public List<String> getGroupnames() {
      return unsafeapi.getGroupnames();
   }

   public List<String> getUsernames() {
      return unsafeapi.getUsernames();
   }

   private List<SafeContent> removeUnpublished(List<SafeContent> list) {
      for (Iterator<SafeContent> it = list.iterator();it.hasNext();) {
         SafeContent c = it.next();
         if (c.getContentState() != ContentState.published) {
            it.remove();
         }
      }
      return list;
   }

   public List<SafeContent> getNewestContent(int start, int max) {
      List<SafeContent> list = unsafeapi.makeSafe(unsafeapi.getNewestContent(start, max));
      return removeUnpublished(list);
   }

   public List<SafeContent> getRecentlyModifiedContent(int start, int max) {
      List<SafeContent> list =unsafeapi.makeSafe(unsafeapi.getRecentlyModifiedContent(start, max));
      for (Iterator<SafeContent> it = list.iterator();it.hasNext();) {
         SafeContent c = it.next();
         if (c.getModifiedDate().equals(c.getCreateDate())) {
            it.remove();
         }
      }

      return removeUnpublished(list);
   }

   public List<SafeContent> getNewestContent(SafeContent content, int start, int max) {
      List<SafeContent> list = unsafeapi.makeSafe(unsafeapi.getNewestContent(content.getPath(), start, max));
      return removeUnpublished(list);
   }

   public List<SafeContent> getRecentlyModifiedContent(SafeContent content, int start, int max) {
      List<SafeContent> list =unsafeapi.makeSafe(unsafeapi.getRecentlyModifiedContent(content.getPath(), start, max));
      for (Iterator<SafeContent> it = list.iterator();it.hasNext();) {
         SafeContent c = it.next();
         if (c.getModifiedDate().equals(c.getCreateDate())) {
            it.remove();
         }
      }
      return removeUnpublished(list);
   }

   public List<SafeContent> getComments(SafeContent content) throws ContentNotFoundException {
       return unsafeapi.makeSafe(unsafeapi.getCommentsForPath(content.getPath()));
   }

   public MarkupType[] getRenderTypes() {
      return MarkupType.values();
   }

   public MarkupType getWikiMarkupType() {
      return MarkupType.wiki;
   }

   public MarkupType getWikiHtmlMarkupType() {
      return MarkupType.wikihtml;
   }

   public MarkupType getTemplatingMarkupType() {
      return MarkupType.htmltemplating;
   }

   public MarkupType getNoMarkupType() {
      return MarkupType.none;
   }

   public MarkupType getHtmlEscape() {
      return MarkupType.htmlescape;
   }

   public ContentState[] getContentStates() {
      return ContentState.values();
   }

   public ContentState getPublishedState() {
      return ContentState.published;
   }

   public ContentState getUnpublishedState() {
      return ContentState.unpublished;
   }

   public Rights getViewRight() {
      return Rights.view;
   }

   public Rights getHistoryRight() {
      return Rights.history;
   }

   private static final SimpleDateFormat RFC822DATE = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z");

   public String makeRfc822Date() {
      return makeRfc822Date(new Date());
   }

   public String makeRfc822Date(Date date) {

      return RFC822DATE.format(date);
   }
}
