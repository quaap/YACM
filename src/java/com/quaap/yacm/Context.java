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

import com.quaap.yacm.api.SafeContext;
import com.quaap.yacm.render.FreeMarkerRenderer;
import com.quaap.yacm.render.Renderer;
import com.quaap.yacm.storage.Storage;
import com.quaap.yacm.storage.bean.Content;
import com.quaap.yacm.storage.bean.ContentPermission;
import com.quaap.yacm.storage.bean.Metadata;
import com.quaap.yacm.storage.bean.User;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *  This class is meant to hold and work on "state" data relating to the current session.
 *
 * @author tom
 */
public class Context {

   private Api api;
   private HttpServletRequest request;
   private HttpServletResponse response;
   private String action;
   private String path;
   private Storage store;
   private Content content;
   private Map<String, Object> map;

   public Context(HttpServletRequest request, HttpServletResponse response, Storage store) {
      map = new HashMap<String, Object>();
      setApi(new Api(this));
      setRequest(request);
      setResponse(response);
      //setRenderer(renderer);
      setStore(store);
      setAction(Api.getActionFromRequest(request));
      setPath(Api.getPathFromRequest(request));
   }

   public Object get(final String name) {
      return map.get(name);
   }

   public Object put(final String name, final Object value) {
      return map.put(name, value);
   }

   /**
    * @return the request
    */
   public HttpServletRequest getRequest() {
      return request;
   }

   /**
    * @param request the request to set
    */
   private void setRequest(HttpServletRequest request) {
      this.request = request;
   }

   /**
    * @return the user
    */
   public synchronized User getUser() {
      User user = (User) request.getSession().getAttribute("user");
      if (user == null) {
         String username = "anonymous";
         try {
            String info = api.decodeLoginCookie();
            if (info!=null) {
               System.out.println(info);
               String [] parts = info.split(":", 3);
               if (api.authenticateUser(parts[0], parts[2])) {
                  username = parts[0];
                  setSecurityKey(parts[1]);
               }
            }
         } catch (Exception ex) {
            Logger.getLogger(Context.class.getName()).log(Level.SEVERE, null, ex);
         }
         user = store.getUser(username);
         setUser(user);
      }
      return user;
   }

  /**
    * @param user the user to set
    */
   public synchronized void setUser(User user) {
      getRequest().getSession().removeAttribute("user");
      getRequest().getSession().setAttribute("user", user);
   }

   public synchronized void unsetUser() {
      getRequest().getSession().removeAttribute("user");
   }

   public boolean isContentAction() {
      return api.isContentAction(action);
   }

   /**
    * @return the action
    */
   public String getAction() {
      return action;
   }

   /**
    * @param action the action to set
    */
   private void setAction(String action) {
      this.action = action;
   }

   /**
    * @return the path
    */
   public String getPath() {
      return path;
   }

   /**
    * @param path the path to set
    */
   private void setPath(String path) {
      this.path = path;
   }


   public boolean hasContent() throws PermissionException, ContentNotFoundException {
      return api.contentExists(getPath());
   }

   public Content getContent() throws PermissionException, ContentNotFoundException {
      if (content == null) {
         if (!api.hasPermission(path, getUser(), ContentPermission.Rights.view)) {
            throw new PermissionException("User " + getUser().getUsername() + " cannot perform this function.");
         }
         content = api.getContentForPath(getPath());
      }
      return content;
   }

   public void setRendererVariable(String name, Object value) throws IOException {
      getRenderer().addContextVariable(name, value);
   }


   public void setSessionVariable(String name, Object value) {
      request.getSession().setAttribute(name, value);
   }

   public Object getSessionVariable(String name) {
      return request.getSession().getAttribute(name);
   }

   public void setRequestVariable(String name, Object value) {
      request.setAttribute(name, value);
   }

   public Object getRequestVariable(String name) {
      return request.getAttribute(name);
   }


   public boolean isAdmin() {
      User user = getUser();
      return api.isAdmin(user);
   }

   public boolean isAnon() {
      User user = getUser();
      return api.isAnon(user);
   }

   public List<ContentPermission> getPermissions() {
      return getStore().getPermissionsOnContent(getPath());
   }


//   public boolean hasPermission(String permission) {
//      return hasPermission(ContentPermission.Rights.valueOf(permission));
//   }

   public boolean hasPermission(ContentPermission.Rights permission) {
      return api.hasPermission(getPath(), getUser(), permission);
   }


   public void hasPermissionThrow(ContentPermission.Rights permission) throws PermissionException {
      if (!hasPermission(permission)) {
         throw api.getPermissionException(getUser(), permission);
      }
   }

   public void canAccessInCurrentStateThrow() throws PermissionException, ContentNotFoundException {
      hasPermissionThrow(getContent().getContentState().getRequiredPermission());
   }

   public boolean canAccessUnpublished() throws PermissionException, ContentNotFoundException {
       return hasPermission(getContent().getContentState().getRequiredPermission());
   }


   public void canOverrideTemplateThrow() throws PermissionException, IOException {
      if (api.templateExists(path) && !isAdmin()) {
         throw api.getPermissionException(getUser());
      }
   }


   // This security key is there to prevent CSRF problems.
   // A cookie is sent on each rendered page and each form contains the same token in a hidden field.
   //  If the two do not match on form submission, reject the submission.
   //private String securitykey = null;

   private void setSecurityKey(String securityKey) {
      setSessionVariable("securitykey", securityKey);
   }

   public String getSecurityKey() {
      String securitykey = (String)getSessionVariable("securitykey");
      if (securitykey==null) {
         SecureRandom random;
         try {
            random = SecureRandom.getInstance("SHA1PRNG");
            securitykey = Long.toHexString(random.nextLong());
         } catch (NoSuchAlgorithmException ex) {
            System.out.println(ex.getMessage());
            securitykey = Double.toHexString(Math.random() * 1000000);
         }
         setSecurityKey(securitykey);
      }
      return securitykey;
   }

//   public void sendSecurityKey() {
//
//      //System.out.println("current key: " + getSecurityKey());
//      //getApi().setCookie("seckey", getSecurityKey(), request.getSession().getMaxInactiveInterval());
//      getApi().setCookie("seckey", getSecurityKey(), request.getContextPath(), 4 * 60 * 60, "A security token to prevent CSRF attacks");
//
//   }
//
//   public String getReturnedSecurityKey() {
//      return getApi().getCookie("seckey");
//   }

   public boolean securityKeysValid() {
      String formseckey = getRequest().getParameter("seckey");
      return securityKeysValid(formseckey);
   }

   public void securityKeysValidThrow() throws SecurityException {
      if (!securityKeysValid()) {
         throw new SecurityException("Security error.  Perhaps your session has timed out.");
      }
   }
   
   public void securityKeysValidThrow(String formseckey) throws SecurityException {
      if (!securityKeysValid(formseckey)) {
         throw new SecurityException("Security error.  Perhaps your session has timed out.");
      }
   }


   public boolean securityKeysValid(String formseckey) {
      if (isAnon()) {
         return true; //Don't care about security key validation for anonymous
      }
      String securitykey = (String)getSessionVariable("securitykey");
      if (formseckey==null) {
         System.out.println(action + "/" + path + ": Form key is null");
         return false;
      }
      if (securitykey==null) {
         System.out.println(action + "/" + path + ": Session key is null");
         return false;
      }
      if (!securitykey.equals(formseckey) ) {
         System.out.println(action + "/" + path + ": Keys do not match: " + formseckey + " "  + securitykey );
         return false;
      }
      System.out.println(action + "/" + path + ": Keys match: " + formseckey + " " + securitykey );
      return true;
   }
   /**
    * @return the renderer
    */
   public Renderer getRenderer() throws IOException {
      Renderer renderer = (Renderer) request.getAttribute("renderer");
      if (renderer == null) {
         renderer = new FreeMarkerRenderer();
         request.setAttribute("renderer", renderer);

         //renderer.addContextVariable("user", getUser());

         Map<String, Object> params = new HashMap<String, Object>();

         Map<String, String[]> rparams = request.getParameterMap();
         for (String name : rparams.keySet()) {
            String[] value = rparams.get(name);
            if (value.length > 1) {
               params.put(name, value);
            } else if (value.length == 1) {
               params.put(name, value[0]);
//               System.out.print(name);
//               System.out.print(" ");
//               System.out.println(value[0]);
            }
         }

         renderer.addContextVariable("paramMap", rparams);
         renderer.addContextVariable("param", params);

//         Map<String, Object> sessionvars = new HashMap<String, Object>();
//         Enumeration<String> sv = request.getSession().getAttributeNames();
//         while (sv.hasMoreElements()) {
//            String att = sv.nextElement();
//            sessionvars.put(att, request.getSession().getAttribute(att));
//         }
 //        renderer.addContextVariable("metadata", getStore().getMetadataMapForPath(getPath()));
 //        renderer.addContextVariable("path", getPath());
         //renderer.addContextVariable("session", sessionvars);
//         renderer.addContextVariable("request", request);
         SafeContext c = new SafeContext(this);
         renderer.addContextVariable("context", c);
         renderer.addContextVariable("api", c.getApi());
         //renderer.addContextVariable("api", api);
      }
      return renderer;
   }


   public Map<String,Metadata> getMetadataMap() {
      return getStore().getMetadataMapForPath(getPath());
   }

   public List<Metadata> getMetadataList() {
      return getStore().getMetadataForPath(path);
   }

   public List<String> getRecentPages() {
      List<String> breadcrumbs = (List<String>) request.getSession().getAttribute("breadcrumbs");
      if (breadcrumbs == null) {
         breadcrumbs = new ArrayList<String>();
         request.getSession().setAttribute("breadcrumbs", breadcrumbs);
      }
      return breadcrumbs;
   }

   public void replaceContentWithHistory(int version) {
      Content content = getStore().getContent(getPath());
      content = new Content(content);
      Content vcontent = getStore().getContentVersion(getPath(), version);
      content.setTitle(vcontent.getTitle());
      content.setContent(vcontent.getContent());
      content.setContenttype(vcontent.getContenttype());
      content.setModifiedDate(new Date());
      content.setLastModifier(getUser());
      content.setChangeReason(vcontent.getChangeReason());
      //content.setBinContent(vcontent.getBinContent());
      getStore().save(content);
   }



   public String getServerURL() {
      HttpServletRequest r = getRequest();
      int port = r.getServerPort();
      return r.getScheme() + "://" + r.getServerName() + (port!=80 ? ":"+port : "") + r.getContextPath();
   }

   public String getServerBaseURL() {
      HttpServletRequest r = getRequest();
      int port = r.getServerPort();
      return r.getScheme() + "://" + r.getServerName() + (port!=80 ? ":"+port : "") ;
   }

   public String getPageURL() {
      HttpServletRequest r = getRequest();
      return r.getRequestURL().toString();
   }

   public String getPageURI() {
      HttpServletRequest r = getRequest();
      return r.getRequestURI().toString();
   }

   /**
    * @return the store
    */
   public Storage getStore() {
      return store;
   }

   /**
    * @param store the store to set
    */
   private void setStore(Storage store) {
      this.store = store;
   }

   /**
    * @return the response
    */
   public HttpServletResponse getResponse() {
      return response;
   }

   /**
    * @param response the response to set
    */
   private void setResponse(HttpServletResponse response) {
      this.response = response;
   }

   /**
    * @return the api
    */
   public Api getApi() {
      return api;
   }


   /**
    * @param api the api to set
    */
   private void setApi(Api api) {
      this.api = api;
   }


}
