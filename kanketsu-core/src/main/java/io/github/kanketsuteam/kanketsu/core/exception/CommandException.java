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
package io.github.kanketsuteam.kanketsu.core.exception;

public class CommandException extends RuntimeException{
    private final int code;

    public CommandException(int code, String message){
        super(message);
        this.code = code;
    }

    public CommandException(String message, Throwable cause) {
        super(message, cause);
        this.code = 1;
    }

    public CommandException(int code, String message, Throwable cause){
        super(message);
        this.code = code;
    }

    public CommandException(int code, String message, String description) {
        super(message);
        this.code = code;
    }

    public int getCode(){
        return code;
    }
}
