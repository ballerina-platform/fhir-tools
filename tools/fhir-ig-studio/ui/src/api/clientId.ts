/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Who this browser is, as far as the service is concerned.
 *
 * <p>The service keeps a working copy of every package being edited. Those copies used to be keyed by the
 * package alone, so everyone pointed at one instance shared them: two people editing the same guide would
 * silently overwrite each other, or be shown work that was not theirs. Sending an id per browser gives each
 * person their own copy.
 *
 * <p>This is not authentication and does not pretend to be. It separates people who are not trying to reach
 * each other's work; anyone who learns a workspace id and the matching client id could still use it. Put the
 * service behind a real sign-in before that distinction matters.
 */

const KEY = 'fhir-codegen.clientId';

/** Used when storage is unavailable, so a private window still works -- for one session, without persisting. */
let fallback: string | null = null;

function mint(): string {
  try {
    return window.crypto.randomUUID();
  } catch {
    return `c${Date.now().toString(36)}${Math.random().toString(36).slice(2, 10)}`;
  }
}

export function clientId(): string {
  try {
    const existing = window.localStorage.getItem(KEY);
    if (existing) {
      return existing;
    }
    const created = mint();
    window.localStorage.setItem(KEY, created);
    return created;
  } catch {
    // A browser that refuses storage still gets a usable id; it just will not be the same one next time,
    // which costs a fresh working copy rather than an error.
    fallback = fallback ?? mint();
    return fallback;
  }
}
