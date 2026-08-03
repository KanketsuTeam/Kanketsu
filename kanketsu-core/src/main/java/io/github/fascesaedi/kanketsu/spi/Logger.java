/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.fascesaedi.kanketsu.spi;

public interface Logger {
    void log(String message);

    default void debug(String message){ log("[DEBUG] " + message);}
    default void info(String message) { log("[INFO] " + message); }
    default void warn(String message) { log("[WARN] " + message); }
    default void error(String message) { log("[ERROR] " + message); }
    default void success(String message) { log("[SUCCESS] " + message); }
    default boolean isDebugEnabled() {
        return Boolean.getBoolean("kanketsu.debug");
    }

    static Logger system() {
        return System.out::println;
    }

    static Logger noop() {
        return message -> {};
    }
}
