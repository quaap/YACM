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

package com.quaap.yacm.notify;

import com.quaap.yacm.Context;
import com.quaap.yacm.storage.bean.Content;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tom
 */
public class Notification {

   public enum Event { ContentSaving, ContentSaved, ContentLoaded };

   private Map<Event,List<NotificationTarget>> targets = new HashMap<Event,List<NotificationTarget>>();

   private static Notification notificationVar = new Notification();

   public static Notification getInstance() {

      return notificationVar;
   }


   public Notification() {
      for (Event e: Event.values()) {
         targets.put(e, new ArrayList<NotificationTarget>());
      }
   }

   public void registerNotificationTarget(Event event, NotificationTarget target) {
      targets.get(event).add(target);
   }

   public void deregisterNotificationTarget(Event event, NotificationTarget target) {
      targets.get(event).remove(target);
   }


   public void notifyTargets(Event event, Context context, Content content) {
      for (NotificationTarget target: targets.get(event)) {
         target.notify(event, context, content);
      }
   }

}
