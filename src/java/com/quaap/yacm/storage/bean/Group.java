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
package com.quaap.yacm.storage.bean;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author tom
 */
public class Group implements java.io.Serializable {

   private Integer id;
   private String groupname;
   private Set<User> users;

   public Group() {
   }

   public Group(Integer id, String groupname) {
      this.id = id;
      this.groupname = groupname;
   }

   /**
    * @return the id
    */
   public Integer getId() {
      return id;
   }

   /**
    * @param id the id to set
    */
   public void setId(Integer id) {
      this.id = id;
   }

   /**
    * @return the groupname
    */
   public String getGroupname() {
      return groupname;
   }

   /**
    * @param groupname the groupname to set
    */
   public void setGroupname(String groupname) {
      this.groupname = groupname;
   }

   /**
    * @return the users
    */
   public Set<User> getUsers() {
      if (users == null) {
         users = new HashSet<User>();
      }
      return users;
   }

   /**
    * @param users the users to set
    */
   public void setUsers(Set<User> users) {
      this.users = users;
   }

   public void addUser(User user) {
      if (users == null) {
         users = new HashSet<User>();
      }
      users.add(user);
   }

   public boolean userIsMember(final String username) {
      if (getUsers()==null || username == null) {
         return false;
      }

      for (User u: getUsers()) {
         if (u.getUsername().equals(username)) {
            return true;
         }
      }
      return false;
   }

}
