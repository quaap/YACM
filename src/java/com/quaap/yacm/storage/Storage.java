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
package com.quaap.yacm.storage;

import com.quaap.yacm.storage.bean.BinContent;
import com.quaap.yacm.utils.DiffUtils;
import com.quaap.yacm.storage.bean.ContentPermission;
import com.quaap.yacm.storage.bean.User;
import com.quaap.yacm.storage.bean.Content;
import com.quaap.yacm.storage.bean.Metadata;
import com.quaap.yacm.storage.bean.ContentHistory;
import com.quaap.yacm.storage.bean.Group;
import com.quaap.yacm.storage.bean.UserAuth;
import com.quaap.yacm.storage.bean.Visit;
import com.quaap.yacm.utils.DiffPatch.Diff;
import com.quaap.yacm.utils.XmlBuilder;
import com.quaap.yacm.utils.XmlReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPathExpressionException;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 * Hibernate Utility class with a convenient method to get Session Factory object.
 *
 * TODO: this should be broken up into several classes
 * @author tom
 */
public class Storage implements ContentChecker {

   static {
      try {
         // Create the SessionFactory from standard (hibernate.cfg.xml)
         // config file.
         sessionFactory = new AnnotationConfiguration().configure().buildSessionFactory();
      } catch (Throwable ex) {
         // Log the exception.
         System.err.println("Initial SessionFactory creation failed." + ex);
         throw new ExceptionInInitializerError(ex);
      }

   }

   private static SessionFactory sessionFactory;

   private boolean readonly;


   public Storage() {
      this(false);
   }

   public Storage(boolean readonly) {
      this.readonly = readonly;
   }


   public static SessionFactory getSessionFactory() {
      return sessionFactory;
   }

   public Session getCurrentSession() {
      Session session = sessionFactory.getCurrentSession();
      if (readonly) {
         session.setFlushMode(FlushMode.MANUAL);
      }
      return session;
   }

   public Transaction getTransaction() {
      return startTransaction(false);
   }

   public Transaction startTransaction(boolean forcenew) {
      Session session = getCurrentSession();
      Transaction t = session.getTransaction();
      if (forcenew) {
         if (t == null || !t.isActive()) {
            t.commit();
         }
      }
      if (forcenew || (t == null || !t.isActive())) {
         session.beginTransaction();
      }
      return session.getTransaction();
   }

   public void commitTransaction() {
      Session session = getCurrentSession();
      session.getTransaction().commit();
   }

   public void rollbackTransaction() {
      Session session = getCurrentSession();
      session.getTransaction().rollback();
   }

   public Object getObject(Class classobj, int id) {
      Session session = getCurrentSession();
      getTransaction();
      return session.get(classobj, id);
   }

   public void save(Object... objects) {
      Session session = getCurrentSession();
      getTransaction();
      for (Object o : objects) {
         session.saveOrUpdate(o);
      }
   }

   public void save(Object o, boolean useUpdate) {
      Session session = getCurrentSession();
      getTransaction();
      if (useUpdate) {
         session.update(o);
      } else {
         session.save(o);
      }
   }

   public void delete(Object o) {
      Session session = getCurrentSession();
      getTransaction();
      session.delete(o);
   }

   public List search(final String hql, final Object... params) {
      return searchRange(hql, -1, -1, params);
   }

   public List searchRange(final String hql, int firstresult, int maxresults, final Object... params) {
      Session session = getCurrentSession();
      getTransaction();
      Query query = session.createQuery(hql);
      if (firstresult > 0) query.setFirstResult(firstresult);
      if (maxresults > 0) query.setMaxResults(maxresults);
      for (int i = 0; i < params.length; i++) {
         query.setParameter(i, params[i]);
      }
      return query.list();
   }

   public void evict(Object... objects) {
      Session session = getCurrentSession();
      getTransaction();
      for (Object o : objects) {
         session.evict(o);
      }
   }


   public User getUser(int id) {
      return (User) getObject(User.class, id);
   }

   public Group getGroup(int id) {
      return (Group) getObject(Group.class, id);
   }

   public Content getContent(int id) {
      return (Content) getObject(Content.class, id);
   }

   public ContentPermission getContentPermission(int id) {
      return (ContentPermission) getObject(ContentPermission.class, id);
   }

   public Metadata getMetadata(int id) {
      return (Metadata) getObject(Metadata.class, id);
   }

   public ContentHistory getContentHistory(int id) {
      return (ContentHistory) getObject(ContentHistory.class, id);
   }

   public boolean contentExists(String path) {
      List<Integer> contents = search("select c.id from Content c where c.path=?", path);
      if (contents.size() > 0) {
         return true;
      } else {
         return false;
      }

   }

   public Date getContentDate(String path) {
      List<Date> contents = search("select c.modifiedDate from Content c where c.path=?", path);
      if (contents.size() > 0) {
         return contents.get(0);
      } else {
         return null;
      }
   }

   public Content getContent(String path) {
      List<Content> contents = search("select c from Content c where c.path=?", path);
      if (contents.size() > 0) {
         return contents.get(0);
      } else {
         return null;
      }
   }


   public Content getEmptyContent() {
      Content econtent = new Content();
      econtent.setContent("");
      econtent.setTitle("");
      return econtent;
   }



   public void save(Content newcontent) {

      Content content = null;
      if (newcontent.getId() != null) {
         content = getContent(newcontent.getId());
      } else {
         content = getEmptyContent();
      }
      String patch = "";
      if (!content.isBlob()) {
         patch = DiffUtils.getPatch(buildTextRepresentation(content), buildTextRepresentation(newcontent));
      }

      String change_comment = newcontent.getChangeReason();
      if (change_comment==null) change_comment="";
      String change_type = "";
      if (!content.isBlob() && !content.getContent().equals(newcontent.getContent())) {
         change_type = "content";
         //if (change_comment.trim().length()!=0) change_comment += "... ";
         //change_comment += "[Content was changed]";
      }
      if (content.getContentState()!=newcontent.getContentState()) {
         if (change_type.length()!=0) {
            change_type += ",";
         }
         change_type += "state";
         if (change_comment.trim().length()!=0) change_comment += "... ";
         change_comment += "[Changed state from '" + content.getContentState().getDescription() + "' to '" +  newcontent.getContentState().getDescription() + "']";
      }
      if (content.getMarkupType()!=newcontent.getMarkupType()) {
         if (change_type.length()!=0) {
            change_type += ",";
         }
         change_type += "markuptype";
         if (change_comment.trim().length()!=0) change_comment += "... ";
         change_comment += "[Changed markuptype from '" + content.getMarkupType().getDescription() + "' to '" +  newcontent.getMarkupType().getDescription() + "']";
      }

      Content.copyContent(newcontent, content);
      content.setVersion(content.getVersion() + 1);
      save((Object)content);

      //BinContent bc = content.getBinContent();
      //if (bc!=null) {
      //   bc.setId(content.getId());
      //   save(bc);
      //}


      //printall("ch ", content.getId(), content.getVersion(), content.getLastModifier(), content.getModifiedDate(), "content", patch, content.getChangeReason());
      ContentHistory ch = new ContentHistory(content.getId(), content.getVersion(), content.getLastModifier(), content.getModifiedDate(), change_type, patch, change_comment);
      save(ch);
   }

   public static void printall(Object ... os) {
      for (Object o: os) {
         System.out.print(o);
         System.out.print(" | ");
      }
   }

   public void deleteContent(String path) {
      Content content = getContent(path);
      if (content != null) {
         for (Metadata metadata : getMetadataForPath(path)) {
            delete(metadata);
         }
         for (ContentPermission perm : getPermissionsOnContent(path)) {
            delete(perm);
         }
      }
      delete(content);
   }

   public List<Integer> getDeletedPaths() {
       List<Integer> deleted = search("select distinct(ch.content) from ContentHistory ch where ch.content not in (select c.id from Content c)");
     
      return deleted;
   }



   public List<ContentHistory> getChangeHistories(String path) {
      Content content = getContent(path);
      if (content == null) {
         return null;
      }
      return getChangeHistories(content.getId());
   }

   public List<ContentHistory> getChangeHistories(Integer id) {
      List<ContentHistory> contents = search("select ch from ContentHistory ch where ch.content=? order by ch.version", id);

      return contents;
   }

   public List<Diff> getContentDiff(String path, int version) {

      return getContentDiff(path, version-1, version);
   }

   public List<Diff> getContentDiff(String path, int version1, int version2) {
      if (version1<0 || version2<1 || version2<=version1) {
         //throw new IllegalArgumentException("The arguments '" + version2 + "' and '" + version2 + "' cannot be used");
         return new ArrayList<Diff>();
      }

      Content v1 = null;
      if (version1==0)  {
         v1 = getEmptyContent();
      } else {
         v1 = getContentVersion(path, version1);
      }

      Content v2 = getContentVersion(path, version2);

      String text1 = "Title: " + v1.getTitle() + "\n\n" + v1.getContent() + "\n";
      String text2 = "Title: " + v2.getTitle() + "\n\n" + v2.getContent() + "\n";

      return DiffUtils.getDiffs(text1, text2);

   }

   public Content getContentVersion(String path, int version) {
      Content content = getContent(path);
      if (content == null) {
         return null;
      }

      List<ContentHistory> contents = search("select ch from ContentHistory ch where ch.content=? and ch.version<=? and ch.changeType like '%content%' order by ch.version", content.getId(), version);

      if (contents.size() > 0) {
         Content vcontent = getEmptyContent();
         String text = buildTextRepresentation(vcontent);
         ContentHistory lastchange = null;
        // System.out.println(text);
         //System.out.println("-------------------------------------------------");
         for (ContentHistory ch : contents) {
            //System.out.println(ch.getChangeDiff());
            text = DiffUtils.applyPatch(text, ch.getChangeDiff());
           // System.out.println("-------------------------------------------------");
            //System.out.println(text);
            lastchange = ch;
         }

         vcontent.setPath(path);
         try {
            vcontent.setContent(XmlReader.getValue(text, "/doc/content/text()"));
            vcontent.setTitle(XmlReader.getValue(text, "/doc/title/text()"));
         } catch (XPathExpressionException ex) {
            Logger.getLogger(Storage.class.getName()).log(Level.SEVERE, null, ex);
         }
         vcontent.setCreateDate(content.getCreateDate());
         vcontent.setModifiedDate(lastchange.getModifiedDate());
         vcontent.setChangeReason(lastchange.getChangeReason());
         vcontent.setVersion(lastchange.getVersion());
         vcontent.setCreator(content.getCreator());
         vcontent.setLastModifier(content.getLastModifier());
         return vcontent;
      } else {
         return null;
      }
   }


   public UserAuth getUserAuth(Integer id) throws Exception {
      List<UserAuth> userauths = search("select u from UserAuth u where u.userid=?", id);
      if (userauths.size() > 1) {
         throw new Exception("More than one userauth for id '" + id + "'");
      }

      if (userauths.size() == 1) {
         return userauths.get(0);
      } else {
         return null;
      }
   }

   public User getUser(String username) {
      List<User> users = search("select u from User u where u.username=?", username);
      if (users.size() > 0) {
         return users.get(0);
      } else {
         return null;
      }
   }

   public Group getGroup(String groupname) {
      List<Group> groups = search("select g from Group g where g.groupname=?", groupname);
      if (groups.size() > 0) {
         return groups.get(0);
      } else {
         return null;
      }
   }

   public List<User> getUsers() {
      return search("select u from User u");
   }

   public List<Group> getGroups() {
      return search("select g from Group g");
   }

   public List<String> getUsernames() {
      return search("select u.username from User u");
   }

   public List<String> getGroupnames() {
      return search("select g.groupname from Group g");
   }

   public List<Metadata> getMetadataForPath(String path) {
      return search("select m from Metadata m, Content c  where m.content=c.id and c.path=?", path);
   }

   public List<Metadata> getMetadataForContent(int id) {
      return search("select m from Metadata m where m.content=?", id);
   }

   public Map<String, Metadata> getMetadataMapForPath(String path) {
      Map<String, Metadata> map = new HashMap<String, Metadata>();
      for (Object m : search("select m.name, m from Metadata m, Content c where m.content=c.id and c.path=?", path)) {
         Object[] m2 = (Object[]) m;
         map.put((String) m2[0], (Metadata) m2[1]);
      }
      return map;
   }
//
//    public ContentPermission getPermission(String path, String username, String permission) {
//        List<ContentPermission> l = search("select p from ContentPermission p, " +
//                " Content c, User u " +
//                " where p.content = c.id and c.path=? " +
//                " and u.username=? " +
//                " and (p.user.id = u.id or (p.group in elements(u.groups) ) )" +
//                " and p.permission=?",
//                path, username, permission);
//        System.out.println("Path: " + path);
//        System.out.println("Username: " + username);
//        System.out.println("Perm: " + permission);
//        System.out.println(l.size());
//        if (l.size()>0) {
//            System.out.println("Allow?: " + l.get(0).getAllow());
//            return l.get(0);
//        } else {
//            return null;
//        }
//
//    }

   public List<ContentPermission> getPermissionsForUser(String path, String username) {
      return search("select p from ContentPermission p, " +
              " Content c, User u " +
              " where p.content = c.id " +
              "  and p.user.id = u.id  and c.path=? and u.username=?",
              path, username);

   }

   public List<ContentPermission> getPermissionsForGroup(String path, String groupname) {
      return search("select p from ContentPermission p, " +
              " Content c, Group g " +
              " where p.content = c.id " +
              "  and p.group.id = g.id  and c.path=? and g.groupname=?",
              path, groupname);

   }

   public List<ContentPermission> getPermissionsOnContent(String path) {
      return search("select p from ContentPermission p, " +
              " Content c " +
              " where p.content = c.id and c.path=?",
              path);

   }

   public ContentPermission getPermissionsOnContent(Content content, User user, Group group) {
      List<ContentPermission> cps = search("select p from ContentPermission p " +
              " where p.content = ? " +
              "  and p.user = ? and p.group = ?", content.getId(), user, group);
      if (cps.size()>0) {
         return cps.get(0);
      }
      return null;
   }


   public List<Visit> getVisitsForPath(String path) {
      return search("select v from Visit v" +
              " where v.path=?",  path);

   }

   public long getVisitCountForPath(String path, String action, boolean unique) {
      String distinct = "";
      if (unique) {
         distinct = "distinct ";
      }
      List<Long> vs = search("select count(" + distinct +"v.ip) from Visit v" +
              " where v.path=? and v.action=?",  path, action);

      return vs.get(0);

   }


   private String buildTextRepresentation(Content content) {
      if (content == null) {
         return "";
      }
//      Object cdata = content.getContent();
//      if (cdata==null) {
//         //cdata = content.getBinContent();
//      }
      StringBuffer xml = new StringBuffer(4096);
      int indent = 0;
      indent = XmlBuilder.makeXMLOpenTag(xml, indent, "doc");
      indent = XmlBuilder.makeXMLElement(xml, indent, "title", content.getTitle());
      indent = XmlBuilder.makeXMLElement(xml, indent, "content", content.getContent());

      List<ContentPermission> perms = getPermissionsOnContent(content.getPath());

      indent = XmlBuilder.makeXMLOpenTag(xml, indent, "Permissions");
      for (ContentPermission perm : perms) {
         indent = XmlBuilder.makeXMLOpenTag(xml, indent, "Permission");
         if (perm.getUser() != null) {
            indent = XmlBuilder.makeXMLElement(xml, indent, "user", perm.getUser().getId().toString());
         }
         if (perm.getGroup() != null) {
            indent = XmlBuilder.makeXMLElement(xml, indent, "group", perm.getGroup().getId().toString());
         }
         indent = XmlBuilder.makeXMLElement(xml, indent, "Add", perm.getAdd().toString());
         indent = XmlBuilder.makeXMLElement(xml, indent, "Admin", perm.getAdmin().toString());
         indent = XmlBuilder.makeXMLElement(xml, indent, "Comment", perm.getComment().toString());
         indent = XmlBuilder.makeXMLElement(xml, indent, "Delete", perm.getDelete().toString());
         indent = XmlBuilder.makeXMLElement(xml, indent, "Edit", perm.getEdit().toString());
         indent = XmlBuilder.makeXMLElement(xml, indent, "View", perm.getView().toString());
         indent = XmlBuilder.makeXMLCloseTag(xml, indent, "Permission");
      }
      indent = XmlBuilder.makeXMLCloseTag(xml, indent, "Permissions");

      indent = XmlBuilder.makeXMLOpenTag(xml, indent, "MetaData");
      Map<String, Metadata> map = getMetadataMapForPath(content.getPath());
      for (String key : map.keySet()) {
         indent = XmlBuilder.makeXMLOpenTag(xml, indent, "MetaDatum");
         indent = XmlBuilder.makeXMLElement(xml, indent, "name", key);
         indent = XmlBuilder.makeXMLElement(xml, indent, "value", map.get(key).getValue());
         indent = XmlBuilder.makeXMLElement(xml, indent, "submitter", map.get(key).getDataType().name());
//         indent = XmlBuilder.makeXMLElement(xml, indent, "submitter", map.get(key).getSubmitter().toString());
//         indent = XmlBuilder.makeXMLElement(xml, indent, "createDate", map.get(key).getCreateDate().toString());
//         indent = XmlBuilder.makeXMLElement(xml, indent, "modifiedDate", map.get(key).getModifiedDate().toString());
         indent = XmlBuilder.makeXMLCloseTag(xml, indent, "MetaDatum");
      }
      indent = XmlBuilder.makeXMLCloseTag(xml, indent, "MetaData");


      indent = XmlBuilder.makeXMLCloseTag(xml, indent, "doc");
      return xml.toString();
   }


}
