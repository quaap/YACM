/*
 *    Copyright (c) 2009 Thomas Kliethermes, thamus@kc.rr.com
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.quaap.yacm.plugin.plugins;

import com.quaap.yacm.Context;
import com.quaap.yacm.notify.Notification;
import com.quaap.yacm.notify.Notification.Event;
import com.quaap.yacm.notify.NotificationTarget;
import com.quaap.yacm.plugin.Plugin;
import com.quaap.yacm.storage.bean.Content;

/**
 *
 * @author tom
 */
public class TagPlugin implements Plugin, NotificationTarget{

   public void init(String... params) {
      Notification.getInstance().registerNotificationTarget(Notification.Event.ContentSaved, this);
   }

   public String getName() {
      return "Tag";
   }

   public void destroy() {
      
   }

   public Plugin clone() {
      return this;
   }

   public void notify(Event event, Context context, Content content) {
      
   }



}
