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

import java.util.Date;

/**
 *
 * @author tom
 */
public class Session {
   private int id;
   private String ip;
   private String uniqueid;
   private User user;
   private String seckey;
   private Date lastaccessed;

   /**
    * @return the id
    */
   public int getId() {
      return id;
   }

   /**
    * @param id the id to set
    */
   public void setId(int id) {
      this.id = id;
   }

   /**
    * @return the ip
    */
   public String getIp() {
      return ip;
   }

   /**
    * @param ip the ip to set
    */
   public void setIp(String ip) {
      this.ip = ip;
   }

   /**
    * @return the user
    */
   public User getUser() {
      return user;
   }

   /**
    * @param user the user to set
    */
   public void setUser(User user) {
      this.user = user;
   }

   /**
    * @return the seckey
    */
   public String getSeckey() {
      return seckey;
   }

   /**
    * @param seckey the seckey to set
    */
   public void setSeckey(String seckey) {
      this.seckey = seckey;
   }

   /**
    * @return the lastaccessed
    */
   public Date getLastaccessed() {
      return lastaccessed;
   }

   /**
    * @param lastaccessed the lastaccessed to set
    */
   public void setLastaccessed(Date lastaccessed) {
      this.lastaccessed = lastaccessed;
   }

   /**
    * @return the uniqueid
    */
   public String getUniqueid() {
      return uniqueid;
   }

   /**
    * @param uniqueid the uniqueid to set
    */
   public void setUniqueid(String uniqueid) {
      this.uniqueid = uniqueid;
   }


}
