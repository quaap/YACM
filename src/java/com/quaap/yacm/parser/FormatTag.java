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
package com.quaap.yacm.parser;

/**
 *
 * @author tom
 */
public class FormatTag {

   private String name;
   private String opendelimiter;
   private String closedelimiter;
   private String opentag;
   private String closetag;
   private char firstChar;



   public FormatTag(String name, String odelim, String cdelim, String opentag, String closetag) {
      if (odelim.length()!=2) {
        // throw new IllegalArgumentException("The opening delimiter must be exactly 2 characters long. " + name);
      }
      this.name = name;
      this.opendelimiter = odelim;
      this.closedelimiter = cdelim;
      this.firstChar = this.opendelimiter.charAt(0);
      this.opentag = opentag;
      this.closetag = closetag;
   }

   /**
    * @return the opendelimiter
    */
   public String getOpendelimiter() {
      return opendelimiter;
   }

   /**
    * @return the closedelimiter
    */
   public String getClosedelimiter() {
      return closedelimiter;
   }

   /**
    * @return the opentag
    */
   public String getOpentag() {
      return opentag;
   }

   /**
    * @return the closetag
    */
   public String getClosetag() {
      return closetag;
   }

   /**
    * @return the firstChar
    */
   public char getFirstChar() {
      return firstChar;
   }

   /**
    * @return the name
    */
   public String getName() {
      return name;
   }
}
