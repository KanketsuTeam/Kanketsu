# Kanketsu

> Everything Unknown, Now Defined

[![Maven Central](https://img.shields.io/maven-central/v/io.github.fascesaedi/kanketsu-core)](https://search.maven.org/artifact/io.github.fascesaedi/kanketsu-core)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![GraalVM](https://img.shields.io/badge/GraalVM-Ready-brightgreen)](https://www.graalvm.org/)

**Kanketsu** 是一个极简、无反射、GraalVM 友好的 Java 命令行框架。它用不到 20KB 的核心实现了命令路由与参数解析，不依赖任何第三方库，适合构建 CLI 工具、管理后台、运维脚本等场景。

---

## 📖 目录

- [为什么选择 Kanketsu？](#-为什么选择-kanketsu)
- [安装](#-安装)
- [快速开始](#-快速开始)
- [更多示例](#-更多示例)
- [核心概念](#-核心概念)
- [模块介绍](#-模块介绍)
- [与主流框架对比](#-与主流框架对比)
- [性能基准](#-性能基准)
- [设计哲学](#-设计哲学)
- [贡献](#-贡献)
- [许可证](#-许可证)
---

## 🎯 为什么选择 Kanketsu？

- **极致轻量** – 核心仅 19KB，零传递依赖，引入即用。
- **无反射魔法** – 所有解析基于 `Map.get` 和字符串处理，原生镜像无需任何 `reflect-config`。
- **透明可控** – 命令注册即代码，IDE 直接跳转，无运行时扫描或注解处理。
- **模块按需组合** – `core` / `repl` / `logging` 互不耦合，只引入你需要的部分。

---

## 📦 安装

```xml
<dependency>
    <groupId>io.github.fascesaedi</groupId>
    <artifactId>kanketsu-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

如需 REPL 或日志支持，额外引入：

```xml
<dependency>
    <groupId>io.github.fascesaedi</groupId>
    <artifactId>kanketsu-repl</artifactId>
    <version>1.0.0</version>
</dependency>
<!-- 或 logging -->
```

---

## 🚀 快速开始

下面是一个hello命令示例：

```java
public class Main {
    public static void main(String[] args) {
        CLI.builder()
                .command("hello", cmd -> cmd
                        .action(ctx -> System.out.println("Hello, Kanketsu!"))
                )
                .build()
                .execute("hello");
    }
}
```

下面是一个完整的子命令嵌套示例
```java
CLI cli = CLI.builder()
        .logger(logger)
        .command("git", git -> git
                .command("commit", commit -> commit
                        .option("message", "m", "Commit message", true)
                        .action(ctx -> {
                            String msg = ctx.getOption("message");
                            logger.info("Committing: " + msg);
                        })
                )
        )
        .build();

cli.execute(new String[]{"git", "commit", "--message", "Initial commit"});
```

## 🧪 更多示例

- **交互式 REPL（带 Tab 补全）** – 查看 [ReplTest](kanketsu-examples/src/main/java/io/github/fascesaedi/kanketsu/ReplTest.java) 了解如何结合 JLine 实现带自动补全的命令行界面。
- **压力测试** – 参考 [StressTest](kanketsu-examples/src/main/java/io/github/fascesaedi/kanketsu/StressTest.java) 了解框架在高并发下的表现。

---

## 🧩 核心概念

### 命令树 (Command Tree)
所有命令注册在树状结构中，支持子命令嵌套（如 `git commit -m "msg"`）。

### 选项解析 (Options)

#### 🎯 命令行解析特性

Kanketsu 的解析器虽小，但覆盖了日常 CLI 的绝大多数习惯用法：

- **短选项组合**：`-abc` 自动展开为 `-a -b -c`，无需空格分隔。
- **粘合参数**：`-fconfig.yml` 等同于 `-f config.yml`，选项与其值可紧贴。
- **长选项**：`--file test.yml` 或 `--file=test.yml` 均支持。
- **分隔符 `--`**：`--` 之后的所有参数均作为位置参数，不再解析选项（例如 `git -- -f` 将 `-f` 当作普通参数）。
- **未知选项保护**：输入未定义的短/长选项（如 `-x`）会得到清晰的错误提示，不会导致程序崩溃。
- **混合使用**：`-a -v -f test.txt` 等任意顺序组合均正确解析。

所有解析逻辑**零反射、纯字符串处理**，对 GraalVM 原生镜像完全友好，无需额外配置。

此外，选项还支持**默认值**：使用 `option(longOpt, shortOpt, description, hasArg, defaultValue)` 重载方法设置，若用户未传入则返回默认值。

### 命令上下文 (CommandContext)
封装解析后的参数、选项、原始输入以及执行环境（如工作目录、环境变量）。所有信息通过 `Context` 传递，便于测试和扩展。

### API速查表

| 方法 | 说明 |
| :--- | :--- |
| `command(name, consumer)` | 注册命令（支持嵌套） |
| `option(longOpt, ...)` | 声明选项（支持短名、描述、是否带值、默认值、是否必选） |
| `action(ctx -> {...})` | 定义命令执行逻辑 |
| `ctx.getOption(key)` | 获取选项值（有默认值则返回默认值） |
| `ctx.getPositionalArgs()` | 获取位置参数列表 |
| `ctx.hasOption(key)` | 检查选项是否被显式传入 |

---

## 🧰 模块介绍

| 模块 | 说明                          | 依赖 |
|------|-----------------------------|------|
| **kanketsu-core** | 核心路由与解析，必选                  | 无 |
| **kanketsu-repl** | 基于 JLine 的交互式 Shell，提供命令补全、历史记录、基础读行 | JLine (可选传递) |
| **kanketsu-logging** | 基于 Imperium 的彩色日志，提供统一日志输出  | Imperium (可选) |

三个模块独立打包，可按需组合。

---

## 🎯 与主流框架对比

| 特性 | Kanketsu  | Picocli | Spring Shell |
|------|-----------|---------|--------------|
| 核心体积 | **19KB**  | ~100KB | >1MB (含Spring) |
| 外部依赖 | **0**     | 0 | Spring 全家桶 |
| 反射使用 | **无**     | 有（注解处理） | 有（大量） |
| GraalVM 原生镜像 | **零配置**   | 需 reflect-config | 需复杂配置 |
| REPL 支持 | 可选模块（轻量）  | 无（需自行集成） | 内置，但笨重 |
| 学习曲线 | 极低（纯Java） | 中等（注解） | 高（Spring生态） |

**结论**：如果你追求极致轻量、AOT 友好、且不想被框架绑定，Kanketsu 是最佳选择。

---

## ⚡ 性能基准

详细数据在[性能](PERFORMANCE.md)；

---

## 🧠 设计哲学

- **极简主义**：只做命令路由与参数解析，不添加任何多余功能（Core 内置帮助生成器，REPL 模块提供 Tab 自动补全）。
- **透明性**：所有行为均显式编码，无隐式扫描、代理或字节码生成，运行时行为完全可预测。
- **AOT 优先**：从 API 设计到内部实现，均避免使用反射和动态类加载，确保原生镜像编译顺畅。

---

## 🤝 贡献

欢迎提交 Issue！

- 开发环境：JDK 11+
- 构建工具：Maven

---

## 📄 许可证

Apache License 2.0

---

*Made with ❤️ by the Kanketsu team.*