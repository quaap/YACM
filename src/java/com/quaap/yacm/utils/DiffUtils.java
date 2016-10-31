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
package com.quaap.yacm.utils;

import com.quaap.yacm.utils.DiffPatch.Diff;
import java.util.List;

/**
 *
 * @author tom
 */
public class DiffUtils {

   private static DiffPatch dp = new DiffPatch();

   public static String getPatch(final String text1, final String text2) {

      return dp.patch_toText(dp.patch_make(text1, text2));
   }

   public static String applyPatch(final String text, final String patch) {

      Object[] res = dp.patch_apply(dp.patch_fromText(patch), text);

      String patchedtext = (String) res[0];

      for (boolean b : (boolean[]) res[1]) {
         if (!b) {
            System.out.println("A patch failed");
            //throw new Exception("Patch failed!");
            //return null;
         }
      }

      return patchedtext;
   }

   public static List<Diff> getDiffs(String text1, String text2) {
      return dp.diff_main(text1, text2);
   }

}
