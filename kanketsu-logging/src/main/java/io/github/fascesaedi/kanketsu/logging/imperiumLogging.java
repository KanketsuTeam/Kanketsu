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
package io.github.fascesaedi.kanketsu.logging;

import io.github.fascesaedi.kanketsu.spi.Logger;
import xyz.imperium.log.Log;

public class imperiumLogging implements Logger {
    @Override
    public void log(String message) {
        Log.norm(message);
    }

    @Override
    public void warn(String message) {
        Log.warn(message);
    }

    @Override
    public void info(String message) {
        Log.info(message);
    }

    @Override
    public void error(String message) {
        Log.error(message);
    }

    @Override
    public void success(String message) {
        Log.success(message);
    }
}
