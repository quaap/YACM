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

/**
 *
 * @author tom
 */
public class DiffPatchTest {

   public static void main(String[] args) {
      DiffPatch dp = new DiffPatch();
      String patch = dp.patch_toText(dp.patch_make("a\nb\nc\n", "a\nb\nc\n"));
      //String patch = dp.patch_toText(dp.patch_make("a\nb\nc\n", "a\nd\nc\n"));
      System.out.println("patch=" + patch);

      Object[] res = dp.patch_apply(dp.patch_fromText(patch), "a\nb\nc\n");

      String patched = (String) res[0];

      System.out.println("patched=" + patched);

      for (boolean b : (boolean[]) res[1]) {
         System.out.println(b);
      }



//      LinkedList<Diff> diffs = dp.diff_main("a\nb\nc\n", "a\nd\nc\n");
//      for(Diff d: diffs) {
//         System.out.print(d.operation.name());
//         System.out.println(d.text);
//      }
   }
}
