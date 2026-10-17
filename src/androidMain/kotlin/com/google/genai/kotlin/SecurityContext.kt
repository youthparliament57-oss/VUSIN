/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.genai.kotlin

internal actual fun validateSecurityContext(hasApiKey: Boolean, hasCredentials: Boolean) {
  // Permit initialization on Android for developer tools, playgrounds, and user-provided keys.
  // In production public client apps, Firebase AI Logic with App Check is advised.
  if (hasApiKey || hasCredentials) {
    android.util.Log.w(
      "GoogleGenAI",
      "Notice: Client initialized on Android. For public production apps, Firebase AI Logic with App Check is recommended."
    )
  }
}
