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
import com.quaap.yacm.api.beans.SafeContentHistory;
import com.quaap.yacm.plugin.PluginFactory;
import com.quaap.yacm.utils.MimeTypes;
import com.quaap.yacm.render.FreeMarkerRenderer;
import com.quaap.yacm.render.RenderException;
import com.quaap.yacm.storage.Storage;
import com.quaap.yacm.storage.bean.BinContent;
import com.quaap.yacm.storage.bean.Content;
import com.quaap.yacm.storage.bean.ContentHistory;
import com.quaap.yacm.storage.bean.ContentPermission;
import com.quaap.yacm.storage.bean.ContentState;
import com.quaap.yacm.storage.bean.Group;
import com.quaap.yacm.storage.bean.User;
import com.quaap.yacm.storage.bean.UserAuth;

import com.quaap.yacm.storage.bean.Visit;
import com.quaap.yacm.utils.Uploader;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



//TODO: generalize and move a lot of this logic into the api and context classes
/**
 *
 * @author tom
 */
public class MainServlet extends HttpServlet {

   private Properties staticpages = null;

   private boolean doneInit = false;


   @Override
   public void init(ServletConfig config) throws ServletException {
      super.init(config);

      //System.out.println("init called");


   }


   /**
    * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
    * @param request servlet request
    * @param response servlet response
    * @throws ServletException if a servlet-specific error occurs
    * @throws IOException if an I/O error occurs
    */
   protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

      Context context = getContext(new Storage(), request, response);
      String action = context.getAction();
      String path = context.getPath();
      Visit visit = new Visit();
      try {

         doInit(context);


         context.getStore().getTransaction();

         visit.setAction(action);
         visit.setPath(path);
         visit.setAccessDate(new Date());
         visit.setBrowser(request.getHeader("User-Agent"));
         visit.setIp(request.getRemoteAddr());
         visit.setQuerystr(request.getQueryString());
         visit.setReferrer(request.getHeader("referer"));
         visit.setUser(context.getUser().getId());

         //check for reserved pages
         for (String page : staticpages.stringPropertyNames()) {
            if (page.equals(path) && context.isContentAction()) {
               if (!action.equals("view")) {
                  context.getApi().redirect("view", path);
               } else {
                  context.setRendererVariable("isStaticPage", true);
                  renderPage(context, staticpages.getProperty(page));
               }
               return;
            }
         }

         /**
          * Based on the Action, dispatch to the appropriate handler
          */
         if (action.equals("view")) {
            viewPage(context);
         } else if (action.equals("index")) {
            indexPage(context);
         } else if (action.equals("edit")) {
            editPage(context);
         } else if (action.equals("upload")) {
            uploadPage(context);
         } else if (action.equals("editcomment")) {
            editcommentPage(context);
         } else if (action.equals("viewcomments")) {
            viewcommentsPage(context);
         } else if (action.equals("history")) {
            viewPageHistory(context);
         } else if (action.equals("user")) {
            userPage(context);
         } else if (action.equals("edituser")) {
            edituserPage(context);
         } else if (action.equals("saveuser")) {
            saveUser(context);
         } else if (action.equals("group")) {
            groupPage(context);
         } else if (action.equals("editgroup")) {
            editgroupPage(context);
         } else if (action.equals("savegroup")) {
            saveGroup(context);

         } else if (action.equals("metadata")) {
            metadataPage(context);
         } else if (action.equals("editmetadata")) {
            editmetadataPage(context);
         } else if (action.equals("savemetadatar")) {
            saveMetadata(context);

         } else if (action.equals("permissions")) {
            permissionsPage(context);
         } else if (action.equals("save")) {
            savePage(context);
         } else if (action.equals("delete")) {
            deletePage(context);
         } else if (action.equals("admin")) {
            adminPage(context);
         } else if (action.equals("login")) {
            login(context);
         } else if (action.equals("logout")) {
            logout(context);
         } else if (action.equals("get")) {
            get(context);
         } else if (action.equals("download")) {
            get(context);
         } else if (action.equals("rsscomments")) {
            rssPage(context);
         } else if (action.equals("rssnew")) {
            rssPage(context);
         } else if (action.equals("rssupdates")) {
            rssPage(context);
         } else {
            errorPage(null, "Unknown action '" + action + "'", context);
         }
      } catch (ContentNotFoundException e) {
         try {
            visit.setError("ContentNotFoundException");
            //print stack for debugging
            e.printStackTrace();
            errorPage(null, e.getMessage(), context);
         } catch (Exception ex) {
            //print stack for debugging
            Logger.getLogger(MainServlet.class.getName()).log(Level.SEVERE, null, ex);
         }


      } catch (PermissionException e) {
         try {
            visit.setError("PermissionException");
            //print stack for debugging
            //e.printStackTrace();
            errorPage(null, e.getMessage(), context);
         } catch (Exception ex) {
            //print stack for debugging
            Logger.getLogger(MainServlet.class.getName()).log(Level.SEVERE, null, ex);
         }

      } catch (SecurityException e) {
         try {
            visit.setError("SecurityException");
            //print stack for debugging
            //e.printStackTrace();
            errorPage(null, e.getMessage(), context);
         } catch (Exception ex) {
            //print stack for debugging
            Logger.getLogger(MainServlet.class.getName()).log(Level.SEVERE, null, ex);
         }


      } catch (Exception e) {
         try {
            visit.setError(e.getMessage());
            //print stack for debugging
            e.printStackTrace();
            errorPage(e, "", context);
         } catch (Exception ex) {
            //print stack for debugging
            Logger.getLogger(MainServlet.class.getName()).log(Level.SEVERE, null, ex);
         }
      } finally {
         context.getStore().save(visit);
         context.getStore().commitTransaction();
      }
   }



   /**
    * Uses the given template to render the content given by context.getPath and
    *   sends it to the client
    *
    * @param context
    * @param template
    * @throws java.io.IOException
    * @throws com.quaap.yacm.PermissionException
    * @throws com.quaap.yacm.ContentNotFoundException
    * @throws com.quaap.yacm.render.RenderException
    */

   private void renderPage(Context context, final String template) throws IOException, PermissionException, ContentNotFoundException, RenderException {
      //context.sendSecurityKey();

      if(context.getAction().equals("view")) {
         List<String> recentPages = context.getRecentPages();
         String last = null;
         if (recentPages.size() > 0) {
            last = recentPages.get(recentPages.size() - 1);
         }
         if (last == null || !context.getPath().equals(last)) {
            recentPages.add(context.getPath());
            if (recentPages.size() > 50) {
               recentPages.remove(0);
            }
         }
      }

      context.setRendererVariable("content", null);
      try {
         String r = context.getRequest().getParameter("r");
         if (r != null) {
            context.hasPermissionThrow(ContentPermission.Rights.history);
            try {
               int rev = Integer.parseInt(r);
               context.setRendererVariable("content", context.getApi().makeSafe(context.getStore().getContentVersion(context.getPath(), rev)));
            } catch (NumberFormatException numberFormatException) {
            }
         } else {
            context.setRendererVariable("content", context.getApi().makeSafe(context.getContent()));
         }
      } catch (ContentNotFoundException e) {
      }

      String text = context.getApi().renderContentUsingTemplate(context.getAction(), template);

      writeTextToClient("text/html;charset=UTF-8", text, context);
   }

   /**
    * Sends text data to the client
    *
    * @param mimetype    The mimetype/contenttype to send
    * @param text        The text to send
    * @param context
    * @throws java.io.IOException
    */
   private void writeTextToClient(String mimetype, CharSequence text, Context context) throws IOException {
      context.getResponse().setContentType(mimetype);
      PrintWriter out = context.getResponse().getWriter();
      try {
         out.print(text);
      } finally {
         out.close();
      }

   }

   /**
    * Set up and display the history page for context.getPath().
    *
    * @param context
    * @throws java.io.IOException
    * @throws com.quaap.yacm.PermissionException
    * @throws com.quaap.yacm.ContentNotFoundException
    * @throws com.quaap.yacm.render.RenderException
    */
   private void viewPageHistory(Context context) throws IOException, PermissionException, ContentNotFoundException, RenderException {
      try {
         context.hasPermissionThrow(ContentPermission.Rights.history);
         context.hasPermissionThrow(ContentPermission.Rights.view);
         context.canAccessInCurrentStateThrow();
         List<ContentHistory> hist = context.getStore().getChangeHistories(context.getPath());
         List<SafeContentHistory> safehist= new ArrayList<SafeContentHistory>();
         for (ContentHistory ch: hist) {
            safehist.add(new SafeContentHistory(ch));
         }
         context.setRendererVariable("history", safehist);
         context.setRendererVariable("securityKey", context.getSecurityKey());
         renderPage(context, "history.static");
      } catch (ContentNotFoundException ce) {
         renderPage(context, "nopage.static");
      }
   }

   /**
    * Set up and display the normal "view" page for context.getPath()
    *
    * @param context
    * @throws java.io.IOException
    * @throws com.quaap.yacm.PermissionException
    * @throws com.quaap.yacm.ContentNotFoundException
    * @throws com.quaap.yacm.render.RenderException
    */
   private void viewPage(Context context) throws IOException, PermissionException, ContentNotFoundException, RenderException {
      try {
         context.hasPermissionThrow(ContentPermission.Rights.view);
         context.canAccessInCurrentStateThrow();

         renderPage(context, "view.static");
      } catch (ContentNotFoundException ce) {
         renderPage(context, "nopage.static");
      }
   }

   /**
    * Set up and display the view comments page for context.getPath()
    *
    * @param context
    * @throws java.io.IOException
    * @throws com.quaap.yacm.PermissionException
    * @throws com.quaap.yacm.ContentNotFoundException
    * @throws com.quaap.yacm.render.RenderException
    */
   private void viewcommentsPage(Context context) throws IOException, PermissionException, ContentNotFoundException, RenderException {
      try {
         context.hasPermissionThrow(ContentPermission.Rights.view);
         context.canAccessInCurrentStateThrow();
         renderPage(context, "viewcomments.static");
      } catch (ContentNotFoundException ce) {
         renderPage(context, "nopage.static");
      }
   }

   /**
    * Set up and display the index page for context.getPath()
    * The index page show all subpages of the page.
    *
    * @param context
    * @throws java.io.IOException
    * @throws com.quaap.yacm.PermissionException
    * @throws com.quaap.yacm.ContentNotFoundException
    * @throws com.quaap.yacm.render.RenderException
    */
   private void indexPage(Context context) throws IOException, PermissionException, ContentNotFoundException, RenderException {
      try {
         context.hasPermissionThrow(ContentPermission.Rights.view);
         context.canAccessInCurrentStateThrow();

         renderPage(context, "index.static");
      } catch (ContentNotFoundException ce) {
         renderPage(context, "nopage.static");
      }
   }

   /**
    * Show an rss page based on context.getPath()
    *
    * @param context
    * @throws java.io.IOException
    * @throws com.quaap.yacm.PermissionException
    * @throws com.quaap.yacm.ContentNotFoundException
    * @throws com.quaap.yacm.render.RenderException
    */
   private void rssPage(Context context) throws IOException, PermissionException, ContentNotFoundException, RenderException {
      try {
         context.hasPermissionThrow(ContentPermission.Rights.view);
         context.canAccessInCurrentStateThrow();
         renderPage(context, "rss.static");
      } catch (ContentNotFoundException ce) {
         renderPage(context, "nopage.static");
      }
   }

   /**
    * Show the edit page for context.getPath()
    *
    * @param context
    * @throws java.io.IOException
    * @throws com.quaap.yacm.PermissionException
    * @throws com.quaap.yacm.ContentNotFoundException
    * @throws com.quaap.yacm.render.RenderException
    */

   private void editPage(Context context) throws IOException, PermissionException, ContentNotFoundException, RenderException {
      if (context.hasContent()) {
         context.hasPermissionThrow(ContentPermission.Rights.edit);
         context.canAccessInCurrentStateThrow();
      } else {
         context.canOverrideTemplateThrow();
         context.hasPermissionThrow(ContentPermission.Rights.add);
      }
      context.setRendererVariable("securityKey", context.getSecurityKey());
      renderPage(context, "edit.static");
   }

   /**
    * Show the edit comment page for either context.getContent().getParent() (if new comment) or
    *  context.getPath() (if existing  comment)
    *
    * @param context
    * @throws java.io.IOException
    * @throws com.quaap.yacm.PermissionException
    * @throws com.quaap.yacm.ContentNotFoundException
    * @throws com.quaap.yacm.render.RenderException
    */
   private void editcommentPage(Context context) throws IOException, PermissionException, ContentNotFoundException, RenderException {

      Content parent = null;
      if (context.hasContent()) {
         context.hasPermissionThrow(ContentPermission.Rights.edit);
         context.canAccessInCurrentStateThrow();
         try {
            parent = context.getContent().getParent();
         } catch (NullPointerException e) {
            System.out.println(e.getMessage());
         }
      } else {
         context.hasPermissionThrow(ContentPermission.Rights.comment);
         String parentstr = context.getRequest().getParameter("pageparent");
         if ( context.getApi().contentExists(parentstr)) {
            parent = context.getApi().getContentForPath(parentstr);
         }
      }

      if (parent != null) {
         context.setRendererVariable("parent", context.getApi().makeSafe(parent));
         context.setRendererVariable("securityKey", context.getSecurityKey());
         renderPage(context, "editcomment.static");
      } else {
         errorPage(null, "Can't add comment", context);
      }
   }


   /**
    * Show the permissions page
    *
    * @param context
    * @throws java.io.IOException
    * @throws com.quaap.yacm.PermissionException
    * @throws com.quaap.yacm.ContentNotFoundException
    * @throws com.quaap.yacm.render.RenderException
    */
   private void permissionsPage(Context context) throws IOException, PermissionException, ContentNotFoundException, RenderException {
      context.hasPermissionThrow(ContentPermission.Rights.admin);
      try {
         context.canAccessInCurrentStateThrow();
         context.setRendererVariable("securityKey", context.getSecurityKey());
         renderPage(context, "permissions.static");
      } catch (ContentNotFoundException ce) {
         renderPage(context, "nopage.static");
      }
   }

   /**
    * Show the binary upload page
    *
    * @param context
    * @throws java.io.IOException
    * @throws com.quaap.yacm.PermissionException
    * @throws com.quaap.yacm.ContentNotFoundException
    * @throws com.quaap.yacm.render.RenderException
    */
   private void uploadPage(Context context) throws IOException, PermissionException, ContentNotFoundException, RenderException {
      if (context.hasContent()) {
         context.hasPermissionThrow(ContentPermission.Rights.edit);
         context.canAccessInCurrentStateThrow();
      } else {
         context.canOverrideTemplateThrow();
         context.hasPermissionThrow(ContentPermission.Rights.add);
      }
      context.setRendererVariable("securityKey", context.getSecurityKey());
      renderPage(context, "upload.static");
   }


   /**
    * Show the edit page for context.getPath()
    *
    * @param context
    * @throws java.io.IOException
    * @throws com.quaap.yacm.PermissionException
    * @throws com.quaap.yacm.ContentNotFoundException
    * @throws com.quaap.yacm.render.RenderException
    */

   private void metadataPage(Context context) throws IOException, PermissionException, ContentNotFoundException, RenderException {
      context.hasPermissionThrow(ContentPermission.Rights.view);
      context.canAccessInCurrentStateThrow();
      renderPage(context, "metadata.static");
   }

  /**
    * Show the edit page for context.getPath()
    *
    * @param context
    * @throws java.io.IOException
    * @throws com.quaap.yacm.PermissionException
    * @throws com.quaap.yacm.ContentNotFoundException
    * @throws com.quaap.yacm.render.RenderException
    */

   private void editmetadataPage(Context context) throws IOException, PermissionException, ContentNotFoundException, RenderException {
      if (context.hasContent()) {
         context.hasPermissionThrow(ContentPermission.Rights.edit);
         context.canAccessInCurrentStateThrow();
      } else {
         context.canOverrideTemplateThrow();
         context.hasPermissionThrow(ContentPermission.Rights.add);
      }
      context.setRendererVariable("securityKey", context.getSecurityKey());
      renderPage(context, "editmetadata.static");
   }

   private void saveMetadata(Context context) throws PermissionException, ContentNotFoundException, IOException, ServletException {
      if (context.hasContent()) {
         context.hasPermissionThrow(ContentPermission.Rights.edit);
         context.canAccessInCurrentStateThrow();
      } else {
         context.canOverrideTemplateThrow();
         context.hasPermissionThrow(ContentPermission.Rights.add);
      }
      for (int i = 0; i < 100; i++) {
         String metadataname = context.getRequest().getParameter("metadataname" + i);
      }


      context.getApi().redirect("metadata", context.getPath());
   }


   /**
    * Show a user's profile page
    *
    * @param context
    * @throws java.io.IOException
    * @throws com.quaap.yacm.PermissionException
    * @throws com.quaap.yacm.ContentNotFoundException
    * @throws com.quaap.yacm.render.RenderException
    * @throws javax.servlet.ServletException
    */
   private void userPage(Context context) throws IOException, PermissionException, ContentNotFoundException, RenderException, ServletException {

      context.setRendererVariable("securityKey", context.getSecurityKey());
      renderPage(context, "profile.static");
   }

   /**
    * Show a group's page
    *
    * @param context
    * @throws java.io.IOException
    * @throws com.quaap.yacm.PermissionException
    * @throws com.quaap.yacm.ContentNotFoundException
    * @throws com.quaap.yacm.render.RenderException
    * @throws javax.servlet.ServletException
    */
   private void groupPage(Context context) throws IOException, PermissionException, ContentNotFoundException, RenderException, ServletException {
      context.setRendererVariable("securityKey", context.getSecurityKey());
      renderPage(context, "group.static");
   }


   /**
    * Show the main administration page
    *
    * @param context
    * @throws java.io.IOException
    * @throws com.quaap.yacm.PermissionException
    * @throws com.quaap.yacm.ContentNotFoundException
    * @throws com.quaap.yacm.render.RenderException
    */
   private void adminPage(Context context) throws IOException, PermissionException, ContentNotFoundException, RenderException {
      if (!context.isAdmin()) {
         throw context.getApi().getPermissionException(context.getUser());
         //throw new PermissionException("User " + context.getUser().getUsername() + " cannot perform this function.");
      }
      context.setRendererVariable("securityKey", context.getSecurityKey());
      renderPage(context, "admin.static");
   }

   private void saveGroup(Context context) throws IOException, PermissionException, ContentNotFoundException, RenderException, ServletException, SecurityException {
      context.securityKeysValidThrow();
      if (context.isAdmin()) {
         String groupname = context.getRequest().getParameter("groupname");
         if (groupname!=null) {
            Group group = context.getApi().getGroup(context.getPath());
            Group ngroup = context.getApi().getGroup(groupname);
            if (!groupname.equals(context.getPath())) {
               if (group!=null && ngroup!=null) {
                  throw new ServletException("Group '" + group.getGroupname() + "' already exists");
               }
               ngroup = group;
            }
            if (ngroup == null) {
               ngroup = new Group();
            }
            ngroup.setGroupname(groupname);
            context.getStore().save(ngroup);

            String [] usernames = context.getRequest().getParameterValues("users");
            if (usernames!=null) {
               for (String username: usernames) {
                  if (!ngroup.userIsMember(username)) {
                     ngroup.addUser(context.getApi().getUser(username));
                  }
               }
            }
            for(Iterator<User> it = ngroup.getUsers().iterator(); it.hasNext();) {
               User guser = it.next();
               boolean present = false;
               for (String username: usernames) {
                  if (username.equals(guser.getUsername())) {
                     present=true;
                     break;
                  }
               }
               if (!present) {
                  it.remove();
               }
            }
            context.getStore().save(ngroup);

         }
         context.getApi().redirect("group", groupname);
      } else {
         throw new PermissionException("User " + context.getUser().getUsername() + " cannot perform this function.");
      }
   }

   private void saveUser(Context context) throws IOException, PermissionException, ContentNotFoundException, RenderException, ServletException, SecurityException {
      context.securityKeysValidThrow();
      String username = context.getRequest().getParameter("username");
      User user = context.getUser();
      if (context.isAdmin() || user.getUsername().equals(username)) {
         if (username != null) {
            User nuser = context.getApi().getUser(username);
            User ouser = context.getApi().getUser(context.getPath());
            if (ouser!=null && !ouser.getUsername().equalsIgnoreCase(username)) {
               if (nuser!=null) {
                   throw new ServletException("Group '" + nuser.getUsername() + "' already exists");
              }
              nuser = ouser;
            }
            if (nuser == null) {
               nuser = new User();
               nuser.setCreateDate(new Date());
            }
            
            populateAndSaveUser(context, nuser);

            String [] groupnames = context.getRequest().getParameterValues("groups");
            if (groupnames!=null) {
               System.out.println("groupnames");
               for (String groupname: groupnames) {
                  System.out.println(groupname);
                  addUserToGroup(nuser, groupname, context);
               }
            }
            for (Iterator<Group> it = nuser.getGroups().iterator(); it.hasNext(); ) {
               Group ug = it.next();
               boolean present = false;
               if (groupnames!=null) {
                  for (String groupname: groupnames) {
                     Group g = context.getStore().getGroup(groupname);
                     if (g!=null && ug.getGroupname().equals(g.getGroupname())) {
                        present=true;
                     }
                  }
               }
               if (!present) {
                  it.remove();
               }

            }
            context.getStore().save(nuser);

         }

         context.getApi().redirect("user", username);
      } else {
         throw new PermissionException("User " +  context.getUser().getUsername() + " cannot perform this function.");
      }

   }

   private void edituserPage(Context context) throws IOException, PermissionException, ContentNotFoundException, RenderException, ServletException {
      if (!context.isAdmin()) {
         throw new PermissionException("User " + context.getUser().getUsername() + " cannot perform this function.");
      }

      context.setRendererVariable("securityKey", context.getSecurityKey());
      renderPage(context, "useredit.static");
   }

   private void editgroupPage(Context context) throws IOException, PermissionException, ContentNotFoundException, RenderException, ServletException {
      if (!context.isAdmin()) {
         throw new PermissionException("User " + context.getUser().getUsername() + " cannot perform this function.");
      }

      context.setRendererVariable("securityKey", context.getSecurityKey());
      renderPage(context, "groupedit.static");
   }

   private void addUserToGroup(User user, String groupname, Context context) {
      if (!user.isInGroup(groupname)) {
         Group g = context.getStore().getGroup(groupname);
               System.out.println("add " + groupname);
         user.addGroup(g);
      }
   }


   private void get(Context context) throws IOException, PermissionException, ContentNotFoundException, ServletException {
      context.hasPermissionThrow(ContentPermission.Rights.view);

      if ( context.hasContent() ) {
         Content content = context.getContent();
         String type = content.getContenttype();
         if (type==null) {
            type = MimeTypes.getMimeType(context.getPath(), "default");
         }
         if (type.equals("default")) type = "text/html";
         context.getResponse().setContentType(type);
         ServletOutputStream out = context.getResponse().getOutputStream();
         try {
            if (content.isBlob()) {
               BinContent bc = content.getBinContent();
               try {
                  bc.writeBlobToOutputStream(out);
               } catch (SQLException ex) {
                  throw new ServletException(ex);
               }
            } else {
               out.print(content.getContent());
            }
         } finally {
            out.close();
         }
      } else {
         System.out.println("get file not found");
         context.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, context.getPath() + " not found.");
//         InputStream in = context.getApi().loadResourceAsStream(context.getPath());
//         //String content = context.getApi().loadResource(context.getPath());
//         String type = MimeTypes.getMimeType(context.getPath());
//         //System.out.println(type);
//         context.getResponse().setContentType(type);
//         context.getResponse().setHeader("Expires", Long.toString(System.currentTimeMillis() + 2*24*60*60*1000));
//         ServletOutputStream out = context.getResponse().getOutputStream();
//         try {
//            int read = 0;
//            byte bbuf [] = new byte[2048];
//            while( (read = in.read(bbuf, 0, bbuf.length))!=-1 ) {
//               out.write(bbuf,0,read);
//            }
//         } finally {
//            out.close();
//         }
      }


   }


   private void deletePage(Context context) throws IOException, PermissionException, ServletException, ContentNotFoundException, RenderException, SecurityException {
      context.hasPermissionThrow(ContentPermission.Rights.delete);
      try {
         context.getContent();
         String action = context.getRequest().getParameter("action");
         String confirm = context.getRequest().getParameter("confirm");

         if (action != null && action.equalsIgnoreCase("delete") && confirm!=null && confirm.equals("1")) {
            context.securityKeysValidThrow();

            context.getStore().deleteContent(context.getPath());
            context.getApi().redirect("view", context.getPath());

            return;
         } else {
            context.setRendererVariable("securityKey", context.getSecurityKey());
            renderPage(context, "delete.static");
            return;
         }
      } catch (ContentNotFoundException ce) {
         renderPage(context, "nopage.static");
      }

   }



   private void savePage(Context context) throws IOException, PermissionException, ServletException, ContentNotFoundException, RenderException, SecurityException {
      context.hasPermissionThrow(ContentPermission.Rights.edit);
      context.canOverrideTemplateThrow();
      Uploader uploader = new Uploader(context.getRequest());
      String formseckey = uploader.getParameter("seckey");
      context.securityKeysValidThrow(formseckey);


      String action = uploader.getParameter("action");
      if (action != null && action.equalsIgnoreCase("save")) {
         String parent = uploader.getParameter("pageparent");
         Content pcontent = null;
         if (parent!=null) {
            //context.hasPermissionThrow(ContentPermission.Rights.comment);
            if (!context.getApi().hasPermission(parent, context.getUser(), ContentPermission.Rights.comment)) {
               throw context.getApi().getPermissionException(context.getUser(), ContentPermission.Rights.comment);
            }
            pcontent = context.getApi().getContentForPath(parent);
         }
         String contentstr = uploader.getParameter("pagecontent");
         InputStream blobstream = uploader.getFileDataStream("pageblobcontent");

         if (contentstr != null || blobstream != null) {
            String title = uploader.getParameter("pagetitle");
            String pagecomment = uploader.getParameter("pagecomment");
            String baseversion = uploader.getParameter("pageversion");

            if (title!=null) {
               //title = XmlBuilder.xmlSanitize(title);
               title = title.replace("â[X80][X9c]", "``").replace("â[X80][X9d]", "''") ;
            }
            if (contentstr!=null) {
               //System.out.println(contentstr);
              // contentstr = XmlBuilder.xmlSanitize(contentstr);
               contentstr = contentstr.replace("â[X80][X9c]", "``").replace("â[X80][X9d]", "''") ;
            }

            String mimetype = null;
            if(context.hasPermission(MarkupType.wikihtml.getRequiredPermission())) {
               mimetype = uploader.getParameter("pagemimetype");
            }

            if (mimetype==null || mimetype.trim().length()==0) {
               mimetype = uploader.getMimeType("pageblobcontent");
            }

            String mtype = uploader.getParameter("pagemarkuptype");
           // System.out.println(mtype);
            int markuptype = MarkupType.wiki.getId();
            if (mtype!=null) {
               try {
                  markuptype = Integer.parseInt(mtype);
               } catch (NumberFormatException ne) {
                  errorPage(null, "Incorrect markuptype value", context);
                  return;
               }
            }

            String state = uploader.getParameter("pagestate");
            int pagestate = ContentState.published.getId();
            if (state!=null) {
               try {
                  pagestate = Integer.parseInt(state);
               } catch (NumberFormatException ne) {
                  errorPage(null, "Incorrect pagestate value", context);
                  return;
              }
            }

            int bversion = 0;
            if (baseversion!=null) {
               try {
                  bversion = Integer.parseInt(baseversion);
               } catch (NumberFormatException ne) {
                  errorPage(null, "Incorrect baseversion value", context);
                  return;
              }
            }


            Content nc = new Content();
            nc.setParent(pcontent);
//             if (pcontent!=null) {
//              System.out.println("setting parent to " + pcontent.getPath());
//            } else {
//               System.out.println("not setting parent");
//            }
            nc.setPath(context.getPath());
            nc.setTitle(title);
            nc.setVersion(bversion);
            nc.setContent(contentstr);
            nc.setChangeReason(pagecomment);
            nc.setLastModifier(context.getUser());
            nc.setContenttype(mimetype);
            nc.setMarkupType(MarkupType.getMarkupType(markuptype));
            nc.setContentState(ContentState.getContentState(pagestate));
            nc.setBlob(blobstream != null);
            if (blobstream != null) {
               nc.setBinContent(new BinContent(blobstream));
            }

            context.getApi().saveContent(nc);

            if (pcontent!=null) {
               context.getApi().redirect("viewcomments", pcontent.getPath() + "#comments");
               return;
            }

            //context.getApi().saveContent(context.getPath(), title, contentstr, comment, context.getUser(), blobstream, mimetype, MarkupType.getMarkupType(markuptype), ContentState.getContentState(pagestate));

         } else {
            getPermissionsFromRequest(context);
            context.getApi().redirect("permissions", context.getPath());
            return;

         }
      } else if (action!=null && action.equalsIgnoreCase("replace")) {
         int version = 0;
         try {
            version = Integer.parseInt(context.getRequest().getParameter("r"));
            context.replaceContentWithHistory(version);
         } catch (NumberFormatException ne) {
            errorPage(null, "No revision selected or bad revision number", context);
            return;
         }

      }
      context.getApi().redirect("view", context.getPath());
      return;
   }

   protected void getPermissionsFromRequest(Context context) throws NumberFormatException {
      for (int i = 0; i < 100; i++) {
         String permid = context.getRequest().getParameter("permid" + i);
         if (permid != null) {
            ContentPermission cp = context.getStore().getContentPermission(Integer.parseInt(permid));
            //System.out.println(cp.getId());
            String delete = context.getRequest().getParameter("deleteperm" + i);
            if (delete != null && delete.equals(permid)) {
               context.getStore().delete(cp);
               //System.out.println("delete: " + delete);
            } else {
               //System.out.println("no delete");
               String username = context.getRequest().getParameter("permusername" + i);
               String groupname = context.getRequest().getParameter("permgroupname" + i);
               if ((username != null && username.trim().length() > 0) || (groupname != null && groupname.trim().length() > 0)) {
                  populatePermissionFromRequest(cp, "" + i, context);
                  context.getStore().save(cp);
               }
            }
         }
      }
      Content content = context.getStore().getContent(context.getPath());
      String username = context.getRequest().getParameter("permusername");
      String groupname = context.getRequest().getParameter("permgroupname");
      if (content != null && ((username != null && username.trim().length() > 0) || (groupname != null && groupname.trim().length() > 0))) {
         ContentPermission cp = new ContentPermission();
         cp.setContent(content.getId());
         populatePermissionFromRequest(cp, "", context);
         context.getStore().save(cp);
      }
   }

   private void populatePermissionFromRequest(ContentPermission cp, String postfix, Context context) {
      User user = null;
      Group group = null;
      String username = context.getRequest().getParameter("permusername" + postfix);
      String groupname = context.getRequest().getParameter("permgroupname" + postfix);
      if (username!=null) user = context.getStore().getUser(username);
      if (groupname!=null) group = context.getStore().getGroup(groupname);
      cp.setUser(user);
      cp.setGroup(group);
      cp.setView(Boolean.parseBoolean(context.getRequest().getParameter("permview" + postfix)));
      cp.setHistory(Boolean.parseBoolean(context.getRequest().getParameter("permhistory" + postfix)));
      cp.setComment(Boolean.parseBoolean(context.getRequest().getParameter("permcomment" + postfix)));
      cp.setEdit(Boolean.parseBoolean(context.getRequest().getParameter("permedit" + postfix)));
      cp.setDelete(Boolean.parseBoolean(context.getRequest().getParameter("permdelete" + postfix)));
      cp.setAdd(Boolean.parseBoolean(context.getRequest().getParameter("permadd" + postfix)));
      if (context.isAdmin()) {
         cp.setHtml(Boolean.parseBoolean(context.getRequest().getParameter("permhtml" + postfix)));
         cp.setProgramming(Boolean.parseBoolean(context.getRequest().getParameter("permprogramming" + postfix)));
      }
      cp.setAdmin(Boolean.parseBoolean(context.getRequest().getParameter("permadmin" + postfix)));
   }

   private void errorPage(Exception ex, String message, Context context) throws IOException, ContentNotFoundException, PermissionException, RenderException {
      context.getResponse().setContentType("text/html;charset=UTF-8");
      PrintWriter out = context.getResponse().getWriter();
      try {
         context.setRendererVariable("message", message);
         context.setRendererVariable("exception", context.getApi().getExceptionAsAtring(ex));
         out.print(context.getApi().loadTemplate("error.static"));
      // out.print("foot");
      } finally {
         out.close();
      }
   }

   private void login(Context context) throws IOException, ServletException, ContentNotFoundException, RenderException, PermissionException, SecurityException {

      String username = context.getRequest().getParameter("username");
      String auth = context.getRequest().getParameter("auth");
      if (username != null && auth != null && auth.trim().length() > 0) {
         context.securityKeysValidThrow();
         User user = context.getStore().getUser(username);
         if (user != null) {
            try {
               UserAuth userauth = context.getStore().getUserAuth(user.getId());
               if (userauth == null) {
                  if (username.equals("admin")) {
                     // This only called once at init time.
                     user = context.getStore().getUser("admin");
                     context.setUser(user);
                     context.getApi().redirect("user", "admin");
                     return;
                  } else {
                     System.out.println("null password?");         
                  }
               } else {

                   try {
                     boolean same = context.getApi().compareHashes(auth, userauth);
                     if (same) {
                        //if (user.getAuth().equals(auth)) {
                        context.setUser(user);
                        if ( context.getRequest().getParameter("rememberme")!=null) {
                           context.getApi().sendLoginCookie(user, userauth, auth, context.getSecurityKey());
                        }

                        String xfer = context.getRequest().getParameter("xfer");
                        if (xfer==null) {
                           context.getApi().redirect("view", context.getPath());
                        } else {
                           context.getApi().redirect(xfer);
                        }
                        return;
                     } else {
                        System.out.println("login failed");
                     }
                  } catch (Exception ex) {
                     throw new ServletException(ex);
                  }
               }
            } catch (Exception ex) {
               throw new ServletException(ex);
            }
         }
      }
      context.getResponse().setContentType("text/html;charset=UTF-8");
      PrintWriter out = context.getResponse().getWriter();
      try {
         String xfer = context.getRequest().getParameter("xfer");
         if (xfer!=null) {
            context.setRendererVariable("xfer", xfer);
         }
         context.setRendererVariable("securityKey", context.getSecurityKey());
         out.print(context.getApi().loadTemplate("login.static"));
      } finally {
         out.close();
      }
   }

   private void logout(Context context) throws IOException, ServletException {
      context.getApi().deleteLoginCookie();
      context.unsetUser();
      context.getRequest().getSession().invalidate();
      String xfer = context.getRequest().getParameter("xfer");
      if (xfer==null) {
         context.getApi().redirect("view", context.getPath());
      } else {
         context.getApi().redirect(xfer);
      }
   }


   private void populateAndSaveUser(Context context, User user) throws ServletException, IOException, ContentNotFoundException, PermissionException, RenderException, SecurityException {
      context.securityKeysValidThrow();

      String username = context.getRequest().getParameter("username");

      if (username != null) {
         String changepw = context.getRequest().getParameter("changepassword");
         String auth1 = context.getRequest().getParameter("auth1");
         String auth2 = context.getRequest().getParameter("auth2");
         String name = context.getRequest().getParameter("name");
         String info = context.getRequest().getParameter("info");
         if (!context.getPath().equalsIgnoreCase("admin") && !context.getApi().isAnon(context.getPath())) {
            user.setUsername(username);
         }
         user.setName(name);
         user.setInfo(info);
         user.setModifiedDate(new Date());
         context.getStore().save(user);
         
         if (changepw != null && auth1 != null && auth2 != null) {
            if (auth1.trim().length() == 0 || !auth1.equals(auth2)) {
               throw new IllegalArgumentException("passwords are blank or not equal");
            }
            try {
               UserAuth userauth = context.getApi().createUserAuth(user, auth1);
               context.getStore().save(userauth);
            } catch (Exception ex) {
               throw new ServletException(ex);
            }
         }
      }
   }

   private void doInit(Context context) throws IOException, ContentNotFoundException, ServletException {
      if (!doneInit) {
         doneInit = true;


         staticpages = new Properties();
         InputStream in = context.getRequest().getSession().getServletContext().getResourceAsStream("/WEB-INF/pages.properties");
         try {
            staticpages.load(in);
         } catch (IOException e) {
            throw new ServletException(e);
         }

         PluginFactory.getInstance().init();
         
         try {
            FreeMarkerRenderer.init(context.getRequest().getSession().getServletContext(), context.getRequest().getContextPath());
         } catch (Exception e) {
            throw new ServletException(e);
         }


         context.getStore().getTransaction();
         String[] groups = new String[]{"admin", "users"};
         for (String gname : groups) {
            Group g = context.getStore().getGroup(gname);
            if (g == null) {
               g = new Group();
               g.setGroupname(gname);
               context.getStore().save(g);
            }
         }
         User admin = context.getStore().getUser("admin");
         if (admin == null) {
            admin = new User();
            admin.setCreateDate(new Date());
            admin.setUsername("admin");
            admin.setName("Administrator");
            admin.setInfo("The Administrator");
            admin.setModifiedDate(new Date());
            context.getStore().save(admin);
            admin = context.getStore().getUser("admin");
            //admin.getGroups().add(context.getStore().getGroup("admin"));
            //admin.setModifiedDate(new Date());
            //context.getStore().save(admin);
         }
         User anon = context.getStore().getUser("anonymous");
         if (anon == null) {
            anon = new User();
            anon.setCreateDate(new Date());
            anon.setUsername("anonymous");
            anon.setName("Anonymous");
            anon.setInfo("The anonymous user");
            anon.setModifiedDate(new Date());
            context.getStore().save(anon);
         }
         if (!context.getApi().contentExists(Api.settingsPath)) {
            context.getApi().addTextContent(Api.settingsPath, "global settings", "=Global Settings\nA page for global settings", "Added automatically", admin);
         }
         if (!context.getApi().contentExists("default")) {
            context.getApi().addTextContent("default", "The main page", "=Welcome\nWelcome to the new place", "Added automatically", admin);
         }
         context.getStore().commitTransaction();
      }
   }


   private Context getContext(Storage store, HttpServletRequest request, HttpServletResponse response) {
      Context context = (Context) request.getAttribute("context");
      if (context == null) {
         context = new Context(request, response, store);
         request.setAttribute("context", context);
      }
      return context;
   }



   // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

//   public long getLastModified(HttpServletRequest req) {
//      Long time = rendercache.getAddedTime(req)
//       return
//   }
   
   /**
    * Handles the HTTP <code>GET</code> method.
    * @param request servlet request
    * @param response servlet response
    * @throws ServletException if a servlet-specific error occurs
    * @throws IOException if an I/O error occurs
    */
   @Override
   protected void doGet(HttpServletRequest request, HttpServletResponse response)
           throws ServletException, IOException {
      processRequest(request, response);
   }

   /**
    * Handles the HTTP <code>POST</code> method.
    * @param request servlet request
    * @param response servlet response
    * @throws ServletException if a servlet-specific error occurs
    * @throws IOException if an I/O error occurs
    */
   @Override
   protected void doPost(HttpServletRequest request, HttpServletResponse response)
           throws ServletException, IOException {
      processRequest(request, response);
   }

   /**
    * Returns a short description of the servlet.
    * @return a String containing servlet description
    */
   @Override
   public String getServletInfo() {
      return "Short description";
   }// </editor-fold>
}
