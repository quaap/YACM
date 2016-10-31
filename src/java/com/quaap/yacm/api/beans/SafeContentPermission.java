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

import com.quaap.yacm.storage.bean.*;


/**
 * ContentPermission generated by hbm2java
 */
public class SafeContentPermission implements java.io.Serializable {

   final ContentPermission unsafecp;
   public SafeContentPermission(ContentPermission cp) {
      unsafecp = cp;
   }

   public Integer getId() {
      return unsafecp.getId();
   }

   public Integer getContent() {
      return unsafecp.getContent();
   }


   public SafeUser getUser() {
      if (unsafecp.getUser()==null) return null;
      return new SafeUser(unsafecp.getUser());
   }

   /**
    * @return the group
    */
   public SafeGroup getGroup() {
      if (unsafecp.getGroup()==null) return null;
      return new SafeGroup(unsafecp.getGroup());
   }

   /**
    * @return the view
    */
   public Boolean getView() {
      return unsafecp.getView();
   }

   /**
    * @return the history
    */
   public Boolean getHistory() {
      return unsafecp.getHistory();
   }

   /**
    * @return the edit
    */
   public Boolean getEdit() {
      return unsafecp.getEdit();
   }


   /**
    * @return the admin
    */
   public Boolean getAdmin() {
      return unsafecp.getAdmin();
   }

   /**
    * @return the delete
    */
   public Boolean getDelete() {
      return unsafecp.getDelete();
   }

   /**
    * @return the add
    */
   public Boolean getAdd() {
      return unsafecp.getAdd();
   }


   /**
    * @return the comment
    */
   public Boolean getComment() {
      return unsafecp.getComment();
   }

   public Boolean getHtml() {
      return unsafecp.getHtml();
   }

   public Boolean getProgramming() {
      return unsafecp.getProgramming();
   }
}


