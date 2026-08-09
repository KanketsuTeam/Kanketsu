# Kanketsu

> **未知终有定，今始尽分明。**

[English README](README.md)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kanketsuteam/kanketsu-core)](https://search.maven.org/artifact/io.github.kanketsuteam/kanketsu-core)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![GraalVM](https://img.shields.io/badge/GraalVM-Ready-brightgreen)](https://www.graalvm.org/)

**Kanketsu** 是一个轻量级、零反射的 Java 命令行应用框架。
Kanketsu 不是另一个参数解析器，
而是一个面向现代 Java CLI 应用的基础框架。

围绕**稳定的核心**和**可扩展的生态**设计，Kanketsu 专注于命令路由和参数解析，同时保持对 GraalVM Native Image 的友好支持。

---

## ✨ 为什么选择 Kanketsu？

Kanketsu 遵循三个简单的设计原则。

* **✨ 显式优于魔法** —— 命令直接在代码中声明，绝不通过运行时发现。
* **⚙️ 静态优于动态** —— 尽可能避免反射和运行时扫描。
* **📦 模块化优于单体** —— 保持核心精简，通过可选模块扩展功能。

这些原则让 Kanketsu 可预测、轻量级，且天然适合提前编译（AOT）。

---

## 📦 模块

| 模块                                  | 说明                                                                     |  Java   |
|:--------------------------------------|:-------------------------------------------------------------------------|:-------:|
| **kanketsu-core**                     | 命令路由和参数解析。                                                     | **17+** |
| **kanketsu-repl**                     | 基于 JLine 的交互式 Shell，支持历史和 Tab 补全。                         | **25+** |
| **kanketsu-json**                     | JSON 输入支持，自动识别字符串/文件，精确定位错误。                       | **17+** |
| **kanketsu-completion-maven-plugin**  | Maven 插件，为你的 CLI 自动生成 Bash/Zsh/Fish 补全脚本。                 | **17+** |

核心模块**零传递依赖**，始终保持精简。

---

## 📥 安装

**核心模块：**

```xml
<dependency>
    <groupId>io.github.kanketsuteam</groupId>
    <artifactId>kanketsu-core</artifactId>
    <version>2.1.0</version>
</dependency>
```

**JSON 模块（可选）：**

```xml
<dependency>
    <groupId>io.github.kanketsuteam</groupId>
    <artifactId>kanketsu-json</artifactId>
    <version>2.1.0</version>
</dependency>
```

**REPL 模块（可选）：**

```xml
<dependency>
    <groupId>io.github.kanketsuteam</groupId>
    <artifactId>kanketsu-repl</artifactId>
    <version>2.1.0</version>
</dependency>
```

---

## 🚀 Hello World

```java
CLI.builder()
    .command("hello", command -> command
        .action(ctx -> System.out.println("Hello, Kanketsu!")))
    .build()
    .execute("hello");
```

---

## 🌳 嵌套命令

```java
CLI cli = CLI.builder()
    .command("git", git -> git
        .command("commit", commit -> commit
            .option("message", option -> option
                .shortOpt("m")
                .hasArg(true)
                .converter(Converters.STRING))
            .action(ctx -> {
                String message = ctx.getOptionValue("message", String.class);
                System.out.println(message);
            })))
    .build();

cli.execute("git", "commit", "-m", "Initial commit");
```

---

## 📄 JSON 输入示例

```java
CLI.builder()
    .converter(JsonTypeConverter.INSTANCE)
    .command("parse", cmd -> cmd
        .option("config", opt -> opt
            .hasArg(true)
            .converter(Converters.STRING)
            .required(true))
        .action(ctx -> {
            JsonObject config = ctx.getOptionValueAs("config", JsonObject.class);
            System.out.println("Host: " + config.get("host"));
        }))
    .build()
    .execute("parse", "--config", "{\"host\":\"localhost\"}");
```

---

## ⚖️ 对比

| 特性                          |       Kanketsu        |          Picocli          |        Spring Shell       |
| :---------------------------- |:---------------------:|:-------------------------:|:-------------------------:|
| 核心体积                      |       **34 KB**       |          ~100 KB          |           >1 MB           |
| 反射                          |          ❌           |             ✅             |             ✅             |
| 运行时扫描                    |          ❌           |             ⚠️             |             ✅             |
| 零传递依赖                    |          ✅           |             ✅             |             ❌             |
| GraalVM Native Image          | ✅ 开箱即用           | ⚠️ 需要额外配置           | ⚠️ 需要额外配置           |

Kanketsu 专为重视以下特性的开发者设计：

* 🚀 轻量级库
* 🏗 显式 API
* ⚡ AOT 友好架构
* 📦 模块化设计

---

## 📚 文档

| 指南                                                                    | 说明                                                                  |
|:------------------------------------------------------------------------|:----------------------------------------------------------------------|
| [🚀 快速开始](docs/cn/%E5%BF%AB%E9%80%9F%E5%BC%80%E5%A7%8B.md)          | 构建你的第一个 Kanketsu 应用。                                        |
| [🧠 核心概念](docs/cn/%E6%A6%82%E5%BF%B5.md)                            | 了解 Kanketsu 背后的核心概念。                                        |
| [💡 示例](docs/cn/%E7%A4%BA%E4%BE%8B.md)                                | 实战示例和常见 CLI 模式。                                             |
| [📦 模块](docs/cn/%E6%A8%A1%E5%9D%97.md)                                | 了解核心模块和可选生态模块。                                          |
| [🏛 设计哲学](docs/cn/%E8%AE%BE%E8%AE%A1%E7%90%86%E5%BF%B5.md)          | 理解 Kanketsu 的设计理念。                                            |
| [⚡ 性能](docs/cn/%E6%80%A7%E8%83%BD.md)                                | 基准测试结果和 Native Image 性能。                                    |
| [📋 更新日志](docs/cn/%E6%9B%B4%E6%96%B0%E6%97%A5%E5%BF%97.md)          | 版本历史和迁移说明。                                                  |
| [📦 Native Image 指南](docs/cn/%E5%8E%9F%E7%94%9F%E9%95%9C%E5%83%8F.md) | 使用 GraalVM Native Image 构建。                                      |
| [💦 扩展指南](docs/cn/%E6%89%A9%E5%B1%95%E6%8C%87%E5%8D%97.md)          | 本文档将介绍如何通过 Builder 链注入自定义实现，以及目前可用的扩展点。 |

---

## ❤️ 理念

Kanketsu 有意保持核心的精简。

Spring 集成、YAML 支持、注解处理、Shell 补全以及其他生态能力，都属于可选模块而非核心本身。

这种方式让框架保持轻量，同时让生态可以独立演进。

---

## 📄 许可证

基于 Apache License 2.0 开源。