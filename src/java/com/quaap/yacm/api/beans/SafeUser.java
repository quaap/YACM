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
package com.quaap.yacm.api.beans;
// Generated Feb 14, 2009 10:05:10 PM by Hibernate Tools 3.2.1.GA

import com.quaap.yacm.storage.bean.Group;
import com.quaap.yacm.storage.bean.User;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

/**
 * User generated by hbm2java
 */
public class SafeUser implements java.io.Serializable {

   final private User unsafeuser;
   private Set<SafeGroup> groups;

   public SafeUser(final User u) {
      unsafeuser = u;
   }

   public Integer getId() {
      return unsafeuser.getId();
   }


   public String getUsername() {
      return unsafeuser.getUsername();
   }

   public String getName() {
      return unsafeuser.getName();
   }


   public String getInfo() {
      return unsafeuser.getInfo();
   }


   public Date getCreateDate() {
      return unsafeuser.getCreateDate();
   }


   public Date getModifiedDate() {
      return unsafeuser.getModifiedDate();
   }

   public Set<SafeGroup> getGroups() {
      if (this.groups==null) {
         this.groups = new TreeSet<SafeGroup>();
         for (Group g : unsafeuser.getGroups()) {
            this.groups.add(new SafeGroup(g));
         }
      }
      return this.groups;
   }

   public boolean isInGroup(final String groupname) {
      return unsafeuser.isInGroup(groupname);
   }
}

