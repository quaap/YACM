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

import com.quaap.yacm.storage.bean.Metadata;
import java.util.Date;

/**
 * Metadata generated by hbm2java
 */
public class SafeMetadata implements java.io.Serializable {

   final private Metadata unsafemetadata;

   public SafeMetadata(final Metadata m) {
      unsafemetadata = m;
   }

   public Integer getId() {
      return unsafemetadata.getId();
   }

   public Integer getContent() {
      return unsafemetadata.getContent();
   }

   public String getName() {
      return unsafemetadata.getName();
   }

   public String getValue() {
      return unsafemetadata.getValue();
   }

//   public Integer getSubmitter() {
//      return unsafemetadata.getSubmitter();
//   }
//
//   public Date getCreateDate() {
//      return unsafemetadata.getCreateDate();
//   }
//
//   public Date getModifiedDate() {
//      return unsafemetadata.getModifiedDate();
//   }
//
//   public String getType() {
//      return unsafemetadata.getType();
//   }

   public Metadata.DataType getDataType() {
      return unsafemetadata.getDataType();
   }

}