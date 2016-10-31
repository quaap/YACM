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
package com.quaap.yacm;

import com.quaap.yacm.render.MarkupType;
import com.quaap.yacm.api.beans.SafeContent;
import com.quaap.yacm.api.beans.SafeMetadata;
import com.quaap.yacm.api.beans.SafeUser;
import com.quaap.yacm.render.RenderException;
import com.quaap.yacm.render.Renderer;
import com.quaap.yacm.storage.bean.BinContent;
import com.quaap.yacm.storage.bean.Content;
import com.quaap.yacm.storage.bean.ContentPermission;
import com.quaap.yacm.storage.bean.ContentPermission.Rights;
import com.quaap.yacm.storage.bean.ContentState;
import com.quaap.yacm.storage.bean.Group;
import com.quaap.yacm.storage.bean.Metadata;
import com.quaap.yacm.storage.bean.User;
import com.quaap.yacm.storage.bean.UserAuth;
import com.quaap.yacm.utils.XmlBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 *  This class is do the work and perform actions needed by the servlet, context, and rendered pages.
 *
 * @author tom
 */
public class Api {

   private Context context;

   public static final String settingsPath = "global-settings";
   
   public Api(Context context) {

      this.context = context;
   }

   public String getUrl(String action, String path) {
      return context.getRequest().getContextPath() + "/" + URLEncode(action) + "/" + XMLEncode(path);
   }

   public String getUrl(String action, String path, String params) {
      return getUrl(action, path) + "?" + XMLEncode(params);
   }

   public void setCookie(String name, String value, String path, int age, String purpose) {
      Cookie cookie = new Cookie(URLEncode(name), URLEncode(value));
      cookie.setPath(path);
      cookie.setMaxAge(age);
      cookie.setComment(purpose);
      context.getResponse().addCookie(cookie);
   }

   public String getCookie(String name) {
      for (Cookie cookie : context.getRequest().getCookies()) {
         if (cookie.getName().equals(URLEncode(name))) {
            return URLDecode(cookie.getValue());
         }
      }
      return null;
   }

   public void deleteCookie(String name) {
      for (Cookie cookie : context.getRequest().getCookies()) {
         if (cookie.getName().equals(URLEncode(name))) {
            cookie.setMaxAge(0);
            cookie.setPath(context.getRequest().getContextPath());
            context.getResponse().addCookie(cookie);
         }
      }
   }


   public String renderContentUsingTemplate(String action, String template) throws IOException, ContentNotFoundException, RenderException, PermissionException {
      String contentstr = null;

      Renderer renderer = getContext().getRenderer();
      renderer.addContextVariable("lpath", template);
      contentstr = renderer.render(template);

      return contentstr;
   }

   public String renderString(String contentstr, String path) throws IOException, ContentNotFoundException, RenderException, PermissionException {


      Renderer renderer = getContext().getRenderer();
      renderer.addContextVariable("lpath", path);
      contentstr = renderer.render(contentstr, path);

      return contentstr;
   }

   public String loadTemplate(String template) throws IOException, ContentNotFoundException, RenderException, PermissionException {
      return renderString(loadFile("templates/" + template), template);
   }

   public boolean templateExists(String template) throws IOException {
      return fileExists("templates/" + template);
   }

   public String loadResource(String resource) throws IOException {
      return loadFile("resources/" + resource);
   }

   public InputStream loadResourceAsStream(String resource) throws IOException {
      return getFileAsStream("resources/" + resource);
   }

   public boolean resourceExists(String resource) throws IOException {
      return fileExists("resources/" + resource);
   }

   public String getExceptionAsAtring(Throwable t) {
      if (t == null) {
         return "";
      }
      StringBuffer sb = new StringBuffer();
      sb.append("<br/>\n<pre>");
      sb.append(t.toString());
      sb.append("\n");
      for (StackTraceElement el : t.getStackTrace()) {
         sb.append("   ");
         sb.append(el.toString());
         sb.append("\n");
      }
      sb.append("</pre>");
      if (t.getCause() != null) {
         sb.append("Caused by:<br/>\n");
         sb.append(getExceptionAsAtring(t.getCause()));
      }
      return sb.toString();
   }

   public boolean isAdmin(User user) {
      if (user.getUsername().equals("admin") || user.isInGroup("admin")) {
         return true;
      }
      return false;
   }

   public boolean isAnon(User user) {
      if (user==null) return false;
      return isAnon(user.getUsername());
   }

   public boolean isAnon(Group group) {
      if (group==null) return false;
      return isAnon(group.getGroupname());
   }

   public boolean isAnon(String username) {
      if (username==null) return false;
      if (username.equalsIgnoreCase("anonymous")) {
         return true;
      }
      return false;
   }

   public boolean isContentAction(final String action) {
      if (action.equals("view") || action.equals("viewcomments") || action.equals("editcomment")
              || action.equals("edit") || action.equals("upload")
              || action.equals("history") || action.equals("permissions") || action.equals("save")
              || action.equals("delete") || action.equals("get") || action.equals("download")) {
         return true;
      }
      return false;
   }


   public PermissionException getPermissionException(User user) {
      return new PermissionException("User " + user.getUsername() + " lacks the rights needed to perform this function.");

   }

   public PermissionException getPermissionException(User user, Rights permission) {
      return new PermissionException("User " + user.getUsername() + " lacks the permission " + permission.toString() + " needed to perform this function.");

   }

   private final static ContentPermission defaultcp = new ContentPermission();

   public boolean hasPermission(String path, User user, Rights permission) {
      //    System.out.println("in " + path + " " +  permission.toString());
//      if (!isContentAction(action)) {
//         throw new IllegalStateException("hasPermission should only be called on content pages.");
//      }
      //User user = getContext().getUser();

      if (isAdmin(user)) {
         return true;
      }

      boolean allow = defaultcp.has(permission);

//      //for view: default to true
//      if (permission == Rights.view || permission == Rights.history) {
//         allow = true;
//      }


      //Global permissions
      allow = checkPermissionsForUserGroups(settingsPath, user, permission, allow);
      allow = checkPermissionsForUser(settingsPath, user, permission, allow);

      // Parent dir permissions
      String[] subpaths = path.split("/");
      if (subpaths.length > 1) {
         String subpath = "";
         for (int i = 0; i < subpaths.length; i++) {
            if (i > 0) {
               subpath += "/";
            }
            subpath += subpaths[i];
            //System.out.println("checking subpath " + subpath);

            allow = checkPermissionsForUserGroups(subpath, user, permission, allow);
            allow = checkPermissionsForUser(subpath, user, permission, allow);
         }
      }


      // page group permissions
      allow = checkPermissionsForUserGroups(path, user, permission, allow);

      // page user permissions
      allow = checkPermissionsForUser(path, user, permission, allow);

      //  System.out.println("out " + path + " " +  permission.toString() + " " + allow);
      return allow;
   }

   protected boolean checkPermissionsForUser(String path, User user, Rights permission, boolean allow) {
      List<ContentPermission> perms = getContext().getStore().getPermissionsForUser(path, user.getUsername());
      for (ContentPermission perm : perms) {
         if (perm.getAdmin()) {
            allow = true;
         } else {
            allow = perm.has(permission);
         }
      }
      return allow;
   }

   public boolean checkPermissionsForUserGroups(String path, User user, Rights permission, boolean allow) {
      for (Group group : user.getGroups()) {
         List<ContentPermission> gperms = getContext().getStore().getPermissionsForGroup(path, group.getGroupname());
         for (ContentPermission perm : gperms) {
            if (perm.getAdmin()) {
               allow = true;
            } else {
               allow = perm.has(permission);
            }
         }
      }
      return allow;
   }


   
   public boolean contentExists(String path) {
      return getContext().getStore().contentExists(path);
   }

   public Content getContentForPath(String path) throws ContentNotFoundException {
      Content content = getContext().getStore().getContent(path);
      if (content == null) {
         throw new ContentNotFoundException("Content for path '" + path + "' not found");
      }
      return content;
   }

   public SafeUser makeSafe(User user) {
      if (user==null) {
         return null;
      }
      return new SafeUser(user);
   }

   public SafeContent makeSafe(Content content) {
      if (content==null) {
         return null;
      }
      return new SafeContent(content);
   }

   public SafeMetadata makeSafe(Metadata metadata) {
      if (metadata==null) {
         return null;
      }
      return new SafeMetadata(metadata);
   }

   public List<SafeContent> makeSafe(List<Content> unsafelist) {
      List<SafeContent> safelist = new ArrayList<SafeContent>();
      for (Content c: unsafelist) {
         safelist.add(makeSafe(c));
      }
      return safelist;
   }   

   public List<SafeMetadata> makeSafeMetadata(List<Metadata> unsafelist) {
      List<SafeMetadata> safelist = new ArrayList<SafeMetadata>();
      for (Metadata m: unsafelist) {
         safelist.add(makeSafe(m));
      }
      return safelist;

   }

   public User makeUnSafe(SafeUser user) {
      if (user==null) {
         return null;
      }
      return getUser(user.getUsername());
   }


   public List<Content> search(final String searchtext, int max) {
      String st = "%" + searchtext.replaceAll("[%|?\\\\]", "_") + "%";
      return getContext().getStore().searchRange("select c from Content c " +
              " where c.content like ? or title like ? order by c.modifiedDate desc", 0, max, st, st);
   }

   private String likeEscape(String text) {
      return text.replaceAll("([_%])", "\\$1");
   }

   public List<Content> getNewestContent(int start, int max) {
      return getContext().getStore().searchRange("select c from Content c " +
              " where c.parent is null order by c.createDate desc", start, max);
   }

   public List<Content> getNewestContent(String path, int start, int max) {
      return getContext().getStore().searchRange("select c from Content c " +
              " where c.parent is null and (c.path=? or c.path like ?) order by c.createDate desc", start, max, likeEscape(path), likeEscape(path) + "/%");
   }

   public List<Content> getRecentlyModifiedContent(int start, int max) {
      return getContext().getStore().searchRange("select c from Content c " +
              " where c.parent is null order by c.modifiedDate desc", start, max);
   }

   public List<Content> getRecentlyModifiedContent(String path, int start, int max) {
      return getContext().getStore().searchRange("select c from Content c " +
              " where c.parent is null and (c.path=? or c.path like ?)  order by c.modifiedDate desc", start, max, likeEscape(path), likeEscape(path) + "/%");
   }

   public long getSubPageCount(String path) {
      List<Long> list = getContext().getStore().search("select count(c) from Content c " +
              " where c.parent is null and (c.path like ?)  order by c.modifiedDate desc", likeEscape(path) + "/%");
      return list.get(0);
   }

   public List<Content> getCommentsForPath(String path) throws ContentNotFoundException {
      Content parent = getContentForPath(path);
      return getContext().getStore().search("select c from Content c " +
              " where c.parent =? order by c.createDate", parent);
   }


   public boolean hasComments(String path) throws ContentNotFoundException {
      return getCommentCount(path)>0;
   }

   public long getCommentCount(String path) throws ContentNotFoundException {
      Content parent = getContentForPath(path);
      List<Long> list = getContext().getStore().search("select count(c) from Content c " +
              " where c.parent =? order by c.createDate", parent);
      return list.get(0);
   }

   public ContentPermission addPermission(Content content, boolean overwrite, User user, Group group, EnumSet<Rights> rights) {
      ContentPermission cp = context.getStore().getPermissionsOnContent(content, user, group);
      boolean isnew = false;
      if (cp == null) {
         isnew=true;
         cp = new ContentPermission();
         cp.setContent(content.getId());
         cp.setUser(user);
         cp.setGroup(group);
      }
      if (isnew || overwrite) {
         cp.setView(rights.contains(Rights.view));
         cp.setHistory(rights.contains(Rights.history));
         cp.setComment(rights.contains(Rights.comment));
         cp.setEdit(rights.contains(Rights.edit));
         cp.setDelete(rights.contains(Rights.delete));
         cp.setAdd(rights.contains(Rights.add));
         cp.setHtml(rights.contains(Rights.html));
         cp.setProgramming(rights.contains(Rights.programming));
         cp.setAdmin(rights.contains(Rights.admin));
         context.getStore().save(cp);
      }
      return cp;

   }

//   public ContentPermission addPermission(Content content, boolean overwrite, User user, Group group, Boolean view, Boolean comment, Boolean edit, Boolean delete, Boolean add, Boolean html, Boolean programming, Boolean admin) {
//
//      ContentPermission cp = context.getStore().getPermissionsOnContent(content, user, group);
//      boolean isnew = false;
//      if (cp == null) {
//         isnew=true;
//         cp = new ContentPermission();
//         cp.setContent(content.getId());
//         cp.setUser(user);
//         cp.setGroup(group);
//      }
//      if (isnew || overwrite) {
//         cp.setView(view);
//         cp.setComment(comment);
//         cp.setEdit(edit);
//         cp.setDelete(delete);
//         cp.setAdd(add);
//         cp.setHtml(html);
//         cp.setProgramming(programming);
//         cp.setAdmin(admin);
//         context.getStore().save(cp);
//      }
//      return cp;
//   }

   public Content addTextContent(String path, String title, String contentstr, String comment, User creator) throws IOException, ContentNotFoundException {
      if (contentExists(path)) {
         throw new IllegalStateException("Content already exists.");
      }

      Content content = new Content();
      content.setCreateDate(new Date());
      content.setCreator(creator);
      content.setBlob(false);
      content.setContenttype("text/html");
      content.setMarkupType(MarkupType.wiki);
      content.setTitle(title);
      content.setPath(path);
      content.setContent(contentstr);
      content.setModifiedDate(new Date());
      content.setLastModifier(creator);
      content.setChangeReason(comment);
//      BinContent bc = content.getBinContent();
//      if (bc == null) {
//         bc = new BinContent();
//         content.setBinContent(bc);
//      }

      context.getStore().save(content);
      content = getContentForPath(path);
      if (!creator.getUsername().equals("admin") && !isAnon(creator)) {
        // addPermission(content, false, creator, null, true, true, true, true, true, false, false, true);

         addPermission(content, false, creator, null,  EnumSet.of(Rights.add, Rights.history, Rights.admin, Rights.comment, Rights.delete, Rights.edit, Rights.view));

      }
      return content;
   }

   public void saveContent(Content newcontent) throws IOException, ContentNotFoundException {
      boolean newpage = false;

      Content content;
      if (contentExists(newcontent.getPath())) {
         content = getContentForPath(newcontent.getPath());
         content = new Content(content);
      } else {
         content = new Content();
         content.setCreateDate(new Date());
         content.setCreator(newcontent.getLastModifier());
         content.setBlob(newcontent.isBlob());
         content.setContentState(ContentState.published);
         content.setParent(newcontent.getParent());
         newpage = true;
      }
      MarkupType markuptype = newcontent.getMarkupType();
      if (markuptype==null) markuptype = MarkupType.wiki;
      if(hasPermission(newcontent.getPath(), newcontent.getLastModifier(), markuptype.getRequiredPermission())) {
         content.setMarkupType(markuptype);
      } else {
         content.setMarkupType(MarkupType.wiki);
      }
      if(hasPermission(newcontent.getPath(), newcontent.getLastModifier(), newcontent.getContentState().getRequiredPermission())) {
         content.setContentState(newcontent.getContentState());
      }

      content.setContenttype(newcontent.getContenttype());

      content.setTitle(newcontent.getTitle());
      content.setPath(newcontent.getPath());
      content.setContent(newcontent.getContent());
      content.setModifiedDate(new Date());
      content.setLastModifier(newcontent.getLastModifier());
      content.setChangeReason(newcontent.getChangeReason());
      //context.getStore().save(content);

      BinContent bc = content.getBinContent();
      if (bc == null) {
         bc = new BinContent();
         content.setBinContent(bc);

      }
      if (newcontent.getBinContent() != null) {
         try {
            bc.setBlobFromStream(newcontent.getBinContent().getBlobStream());
            //bc.setContent(content);
         } catch (SQLException ex) {
            System.out.println(ex.getMessage());
         }
      }
      context.getStore().save(content);
      //bc.setId(content.getId());
      //store.save(bc);

      if (newpage) {
         content = getContentForPath(newcontent.getPath());
         boolean isAnon = isAnon(newcontent.getLastModifier());
         if (isAnon && content.getParent()!=null) { //is a comment: anon user should not be able to edit comments
            //addPermission(content, false, newcontent.getLastModifier(), null, true, true, false, false, false, false, false, false);
            addPermission(content, false, newcontent.getLastModifier(), null, EnumSet.of(Rights.comment, Rights.view, Rights.history));
         } else if (isAnon) {
           // addPermission(content, false, newcontent.getLastModifier(), null, true, true, true, false, true, false, false, false);
            addPermission(content, false, newcontent.getLastModifier(), null, EnumSet.of(Rights.add, Rights.comment, Rights.edit, Rights.view, Rights.history));
          } else  if (!isAdmin(newcontent.getLastModifier())) {
            //addPermission(content, false, newcontent.getLastModifier(), null, true, true, true, true, true, false, false, true);
            addPermission(content, false, newcontent.getLastModifier(), null, EnumSet.of(Rights.add, Rights.admin, Rights.comment, Rights.delete, Rights.edit, Rights.view, Rights.history));
         }
      }
   }

//   protected void saveContent(String path, String title, String contentstr, String comment, User modifier, InputStream blobstream, String mimetype, MarkupType markuptype, ContentState state) throws IOException, ContentNotFoundException {
//      boolean newpage = false;
//
//      Content content;
//      if (contentExists(path)) {
//         content = getContentForPath(path);
//         content = new Content(content);
//      } else {
//         content = new Content();
//         content.setCreateDate(new Date());
//         content.setCreator(modifier);
//         content.setBlob(blobstream != null);
//         newpage = true;
//      }
//      if (markuptype==null) markuptype = MarkupType.wiki;
//      if(hasPermission(path, modifier, MarkupType.wikihtml.getRequiredPermission())) {
//         content.setContenttype(mimetype);
//      }
//      if(hasPermission(path, modifier, markuptype.getRequiredPermission())) {
//         content.setMarkupType(markuptype);
//      } else {
//         content.setMarkupType(MarkupType.wiki);
//      }
//      if(hasPermission(path, modifier, state.getRequiredPermission())) {
//         content.setContentState(state);
//      }
//
//      content.setTitle(title);
//      content.setPath(path);
//      content.setContent(contentstr);
//      content.setModifiedDate(new Date());
//      content.setLastModifier(modifier);
//      content.setChangeReason(comment);
//      //context.getStore().save(content);
//
//      BinContent bc = content.getBinContent();
//      if (bc == null) {
//         bc = new BinContent();
//         content.setBinContent(bc);
//      }
//      if (blobstream != null) {
//         bc.setBlobFromStream(blobstream);
//      }
//      context.getStore().save(content);
//      //bc.setId(content.getId());
//      //store.save(bc);
//
//      if (newpage) {
//         content = getContentForPath(path);
//         if (!modifier.getUsername().equals("admin")) {
//            addPermission(content, false, modifier, null, true, true, true, true, true, false, false, true);
//         }
//      }
//   }

   public User getUser(final String username) {
      return context.getStore().getUser(username);
   }

   public Group getGroup(final String groupname) {
      return context.getStore().getGroup(groupname);
   }

   public String getUserLink(final User user) {
      return "<a href='" + getUrl("user", user.getUsername()) + "'>" + XMLEncode(user.getName()) + "</a>";
   }

   public String getUserLink(final SafeUser user) {
      return "<a href='" + getUrl("user", user.getUsername()) + "'>" + XMLEncode(user.getName()) + "</a>";
   }

   public List<String> getUsernames() {
      List<String> list = context.getStore().getUsernames();
      //list.add(0, "");
      return list;
   }

   public List<String> getGroupnames() {
      List<String> list = context.getStore().getGroupnames();
     // list.add(0, "");
      return list;
   }


   public String cleanFilename(String filename) {
      return filename.replaceAll("[^\\w/\\-\\.]|\\.\\.", "");

   }

   private boolean fileExists(String resource) throws IOException {
      if (resource == null) {
         return false;
      }
      resource = cleanFilename(resource);
      InputStream is = null;
      try {
         is = getContext().getRequest().getSession().getServletContext().getResourceAsStream(resource);
         if (is != null) {
            return true;
         }
      } finally {
         if (is!=null) is.close();
      }
      return false;
   }

   private InputStream getFileAsStream(String resource) throws IOException {
      if (resource == null) {
         return null;
      }
      resource = cleanFilename(resource);
      return getContext().getRequest().getSession().getServletContext().getResourceAsStream(resource);

   }

   private String loadFile(String resource) throws IOException {
      if (resource == null) {
         return "null";
      }
      resource = cleanFilename(resource);
      InputStream is = null;
      try {
         is = getContext().getRequest().getSession().getServletContext().getResourceAsStream(resource);
         if (is != null) {
            BufferedReader bf = new BufferedReader(new InputStreamReader(is));
            StringBuffer text = new StringBuffer(4086);
            String line = "";
            while ((line = bf.readLine()) != null) {
               text.append(line);
               text.append("\n");
            }
            bf.close();
            return text.toString();
         } else {
            return "No such file " + resource;
         }
      } finally {
         if (is!=null) is.close();
      }

   }

   public String XMLEncode(final String text) {
      return XmlBuilder.xmlEscape(text).toString();
   }

   public String URLEncode(final String text) {
      if (text == null) {
         return "";
      }
      try {
         return java.net.URLEncoder.encode(text, "UTF-8");
      } catch (UnsupportedEncodingException ex) {
         return ex.getMessage();
      }
   }

   public String URLDecode(final String text) {
      if (text == null) {
         return "";
      }
      try {
         return java.net.URLDecoder.decode(text, "UTF-8");
      } catch (UnsupportedEncodingException ex) {
         return ex.getMessage();
      }
   }

   /**
    * @return the context
    */
   public Context getContext() {
      return context;
   }

   public void redirect(String action, String path) throws ServletException, IOException {
      context.getResponse().sendRedirect(context.getApi().getUrl(action, path));
   }

   public void redirect(String url) throws ServletException, IOException {
      context.getResponse().sendRedirect(url);
   }

   public void forward(String action, String path) throws ServletException, IOException {
      context.getRequest().getRequestDispatcher(context.getApi().getUrl(action, path)).forward(context.getRequest(), context.getResponse());
   }

   public static String getActionFromRequest(HttpServletRequest request) {
      String action = request.getServletPath();
      if (action == null) {
         action = "";
      }
      if (action.startsWith("/")) {
         action = action.substring(1);
      }
      if (action.length() == 0) {
         action = "view";
      }
      return action;
   }

   public static String getPathFromRequest(HttpServletRequest request) {
      String path = request.getPathInfo();
      if (path == null) {
         path = "";
      }
      if (path.startsWith("/")) {
         path = path.substring(1);
      }
      if (path.length() == 0) {
         path = "default";
      }
      return path;
   }


   public boolean authenticateUser(String username, String auth) throws Exception {
      if (username != null && username.trim().length() > 0 && auth != null && auth.trim().length() > 0) {
         User user = context.getStore().getUser(username);
         if (user != null) {
            UserAuth userauth = context.getStore().getUserAuth(user.getId());
            if (userauth != null) {
               boolean same = context.getApi().compareHashes(auth, userauth);
               if (same) {
                  return true;
               } else {
                  System.out.println("login failed");
               }
            }
         }
      }


      return false;
   }


   public void deleteLoginCookie() {
      deleteCookie("username");
      deleteCookie("authinfo");
      
   }

   public String decodeLoginCookie() throws Exception {
      String username = getCookie("username");
      String authinfo = getCookie("authinfo");
      if (username!=null && authinfo!=null) {
         User user = context.getStore().getUser(username);
         if (user!=null) {
            UserAuth auth = context.getStore().getUserAuth(user.getId());
            if (auth!=null && auth.getSalt()!=null) {
               return decrypt(new sun.misc.BASE64Encoder().encode(auth.getSalt()), authinfo);
            }
         }
      }
      return null;
   }

   public void sendLoginCookie(User user, UserAuth auth, String auth1, String key) {
      int expire = 24 * 60 * 60 * 30;
      String authname = encrypt(new sun.misc.BASE64Encoder().encode(auth.getSalt()), user.getUsername() + ":" + key + ":" + auth1);
      setCookie("username", user.getUsername(), context.getRequest().getContextPath(), expire, "");
      setCookie("authinfo", authname, context.getRequest().getContextPath(), expire, "");

   }


   public UserAuth createUserAuth(User user, String auth1) throws NoSuchAlgorithmException, Exception {
      UserAuth userauth = context.getStore().getUserAuth(user.getId());
      //System.out.println("saveuser3");
      // Uses a secure Random not a simple Random
      SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
      // Salt generation 64 bits long
      byte[] salt = new byte[8];
      random.nextBytes(salt);
      if (userauth == null) {
         //System.out.println("saveuser4");
         userauth = new UserAuth();
         userauth.setUserid(user.getId());
      }
      userauth.setSalt(salt);
      userauth.setAuth(getHash(auth1, salt));
      return userauth;
   }

   public boolean compareHashes(String auth, UserAuth userauth) throws Exception {
      if (userauth==null || userauth.getAuth()==null || userauth.getSalt()==null) {
         throw new IllegalArgumentException("Authentication is invalid.");
      }
      boolean same = true;
      byte[] newhash = getHash(auth, userauth.getSalt());
      byte[] oldhash = userauth.getAuth();
      if (oldhash.length != newhash.length) {
         same = false;
      } else {
         for (int i = 0; i < oldhash.length; i++) {
            if (newhash[i] != oldhash[i]) {
               same = false;
               break;
            }
         }
      }
      return same;
   }

   public synchronized byte[] getHash(String plaintext, byte[] salt) throws Exception {
      MessageDigest md = MessageDigest.getInstance("SHA");
      md.reset();
      md.update(salt);
      md.update(plaintext.getBytes("UTF-8"));
      return md.digest();
   }

    public static String encrypt(String keystring, String msg){
        try {
            java.security.spec.KeySpec keySpec = new javax.crypto.spec.DESKeySpec(keystring.getBytes());
            javax.crypto.SecretKey key = javax.crypto.SecretKeyFactory.getInstance("DES").generateSecret(keySpec);
            javax.crypto.Cipher ecipher = javax.crypto.Cipher.getInstance(key.getAlgorithm());
            ecipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key);
            //Encode the string into bytes using utf-8
            byte[] utf8 = msg.getBytes("UTF8");
            //Encrypt
            byte[] enc = ecipher.doFinal(utf8);
            //Encode bytes to base64 to get a string
            return new sun.misc.BASE64Encoder().encode(enc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String decrypt(String keystring, String msg){
        try {
            java.security.spec.KeySpec keySpec = new javax.crypto.spec.DESKeySpec(keystring.getBytes());
            javax.crypto.SecretKey key = javax.crypto.SecretKeyFactory.getInstance("DES").generateSecret(keySpec);
            javax.crypto.Cipher decipher = javax.crypto.Cipher.getInstance(key.getAlgorithm());
            decipher.init(javax.crypto.Cipher.DECRYPT_MODE, key);
            // Decode base64 to get bytes
            byte[] dec = new sun.misc.BASE64Decoder().decodeBuffer(msg);
            //Decrypt
            byte[] utf8 = decipher.doFinal(dec);
            //Decode using utf-8
            return new String(utf8, "UTF8");
        } catch (Exception e) {
            e.printStackTrace();
        }
      return null;
    }

}
