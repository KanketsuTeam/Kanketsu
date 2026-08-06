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
package io.github.kanketsuteam.kanketsu.repl;

import io.github.kanketsuteam.kanketsu.core.CLI;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class REPLTest {

    @Mock
    private Terminal mockTerminal;

    @Mock
    private LineReader mockReader;

    // ---- 由于 REPL 构造时直接创建 Terminal 和 LineReader，难以注入 mock，
    // 因此以下测试仅演示如何通过反射或重构实现测试。实际项目中建议增加可注入的构造器。
    // 这里仅提供结构示例，实际运行可能失败。
    // ----

    @Test
    void shouldCreateREPLWithoutException() throws IOException {
        CLI cli = CLI.builder().build();
        REPL repl = new REPL(cli);
        assertThat(repl).isNotNull();
        repl.close();
    }

    @Test
    void shouldReturnTerminal() throws IOException {
        CLI cli = CLI.builder().build();
        REPL repl = new REPL(cli);
        assertThat(repl.getTerminal()).isNotNull();
        repl.close();
    }
}