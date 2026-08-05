# 🏗 Building Native Images with GraalVM

Kanketsu's zero‑reflection design makes it **trivial** to compile your CLI application into a native executable using GraalVM Native Image. This guide walks you through the configuration using a real‑world example — **Gitetsu**, a GitHub CLI tool built with Kanketsu.

---

## 📋 Prerequisites

- **GraalVM JDK** 25 or later (download from [GraalVM](https://www.graalvm.org/))  
  > *JDK 25 is required if your application uses the FFM API (Foreign Function & Memory API). For applications that don't use FFM, JDK 25+ works as well.*
- **Maven** 3.9+ (or Gradle, if you prefer)
- Your Kanketsu application compiled and tested

---

## 🧩 Maven Configuration

Below is the complete `pom.xml` configuration used by **Gitetsu** to build a native executable. You can adapt it to your own project.

```xml
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
    <version>0.11.0</version>
    <extensions>true</extensions>
    <executions>
        <execution>
            <id>build-native</id>
            <goals>
                <goal>compile-no-fork</goal>
            </goals>
            <phase>package</phase>
        </execution>
    </executions>
    <configuration>
        <mainClass>com.yourcompany.yourapp.Main</mainClass>
        <buildArgs>
            <buildArg>--no-fallback</buildArg>
            <buildArg>-H:+ReportExceptionStackTraces</buildArg>
            <buildArg>--enable-native-access=ALL-UNNAMED</buildArg>
            <buildArg>--initialize-at-build-time=com.yourcompany.yourapp.CLIHolder</buildArg>
            <buildArg>--initialize-at-build-time=com.yourcompany.yourapp.MyAppLogger</buildArg>
            <buildArg>--initialize-at-build-time=io.github.fascesaedi.kanketsu</buildArg>
        </buildArgs>
        <jvmArgs>
            <jvmArg>--enable-native-access=ALL-UNNAMED</jvmArg>
        </jvmArgs>
    </configuration>
</plugin>
```

---

## 🔧 Explanation of Configuration Options

| Argument / Option | Purpose |
|-------------------|---------|
| `--no-fallback` | Forces a **pure native executable**; prevents fallback to a JVM‑based image (ensures optimal startup). |
| `-H:+ReportExceptionStackTraces` | Preserves full stack traces in the native image for easier debugging. |
| `--enable-native-access=ALL-UNNAMED` | Required if your application uses `java.net.http.HttpClient` (common for REST APIs) or other modules that need native access. |
| `--initialize-at-build-time=...` | **Critical** — forces classes to be initialized **at build time** rather than at runtime. This is essential for classes that hold **static state**, such as a pre‑built command tree. Without this, the CLI tree may be re‑initialized twice, leading to undefined behavior. |
| `<jvmArgs>` | These are passed to the JVM that runs the `native-image` tool itself (not to your application). The `--enable-native-access=ALL-UNNAMED` here is often necessary to allow the tool to access internal JDK classes. |

### Which classes to initialize at build time?

- ✅ **Classes that build your command tree in a static initializer** (e.g., `CLIHolder`).
- ✅ **Custom logger implementations** (e.g., `MyAppLogger`).
- ✅ **Any class that holds application‑wide state** that should be frozen at build time.
- ❌ **Avoid** initializing classes that depend on runtime input (e.g., file paths, user home, environment variables).

---

## 🏗 Recommended Project Structure for Native Image

Building a native image requires careful attention to **class initialization order**. To avoid runtime surprises, we recommend a specific project structure that separates **static setup** from **runtime logic** and ensures the command tree is built at the right time.

### Recommended Layout

```
src/main/java/com/yourcompany/yourapp/
├── Main.java                    # Entry point (contains static REPL/terminal setup)
├── CLIHolder.java               # Holds the static CLI instance (command tree built here)
├── Commands/                    # Command implementations
│   ├── ApiCommand.java
│   ├── AuthCommand.java
│   ├── IssueCommand.java
│   ├── PrCommand.java
│   └── RepoCommand.java
├── Config.java                  # Configuration / token storage
├── Http.java                    # HTTP client wrapper
└── MyAppLogger.java             # Custom Logger implementation
```

### Component Responsibilities

| Component | Responsibility | Native Image Consideration |
|-----------|----------------|----------------------------|
| **`Main.java`** | Entry point: sets up terminal/REPL, handles arguments, starts REPL loop. | Should be minimal; avoid heavy static state. |
| **`CLIHolder.java`** | **Holds static `CLI` instance** — builds the entire command tree in a static initializer. | **Must be initialized at build time** (`--initialize-at-build-time`). This freezes the command tree at build time, ensuring consistent behavior and faster startup. |
| **`Commands/*.java`** | Command logic (actions). Each command is a `public static void execute(CommandContext ctx)` method referenced from `CLIHolder`. | Stateless (no static fields) → safe at runtime. |
| **`Config.java`** | Loads/saves configuration (e.g., token from `~/.config/yourapp/`). | **Do not** initialize at build time — it depends on runtime environment (user home, file system). |
| **`Http.java`** | HTTP client wrapper (uses `java.net.http.HttpClient`). | Requires `--enable-native-access=ALL-UNNAMED` at build time. |
| **`MyAppLogger.java`** | Custom logger implementation. | If it holds static state, it may need build‑time initialization. |

### The `CLIHolder` Pattern

The **static holder pattern** is the key to a clean native‑image‑friendly design:

```java
package com.yourcompany.yourapp;

import io.github.fascesaedi.kanketsu.core.CLI;
import io.github.fascesaedi.kanketsu.spi.Logger;

public class CLIHolder {
    public static Logger logger = new MyAppLogger();
    public static CLI cli = CLI.builder()
            .logger(logger)
            .command("version", "Show version", version -> version
                    .action(ctx -> logger.log("MyApp 1.0.0")))
            .command("api", "Make API requests", api -> api
                    .option("endpoint", opt -> opt
                            .shortOpt("e")
                            .hasArg(true)
                            .required(true))
                    .action(ctx -> ApiCommand.execute(ctx)))
            // ... more commands ...
            .build();
}
```

### `Main.java` Entry Point

The `main` method should be minimal — it only delegates to `CLIHolder.cli`:

```java
package com.yourcompany.yourapp;

public class Main {
    public static void main(String[] args) {
        // For CLI mode: execute args directly
        if (args.length > 0) {
            System.exit(CLIHolder.cli.execute(args));
            return;
        }

        // For REPL mode (optional, requires kanketsu-repl)
        // REPL repl = new REPL(CLIHolder.cli);
        // repl.run();
    }
}
```

### Why This Pattern Works for Native Image

1. **Command tree is built once at build time** — when `CLIHolder` is marked with `--initialize-at-build-time`, the entire tree is constructed during native image generation, not at application startup. This reduces startup time to **~6 ms**.

2. **Commands are stateless** — each command is a `static` method that receives `CommandContext`. No instance state means no surprises during runtime initialization.

3. **Runtime‑dependent logic is isolated** — configuration loading, HTTP requests, and file I/O happen **inside command actions**, not in static initializers. This avoids premature initialization of classes that depend on runtime environment.

### What to Avoid

| Anti‑pattern | Why It Fails in Native Image |
|--------------|------------------------------|
| Building CLI in `main()` | The tree is built at runtime, losing the AOT benefit and potentially causing slower startup. |
| Loading config files in a static initializer | The file may not exist at build time → build failure or runtime errors. |
| Using `System.getenv()` or `System.getProperty()` in static initializers | Environment may differ at build time vs. runtime → inconsistent behavior. |
| Heavy static state in command classes | May cause unexpected initialization order issues. |

### Example: Correct vs. Incorrect

❌ **Bad** — loading config in static initializer:
```java
public class ApiCommand {
    private static String token = Config.loadToken();  // File may not exist at build time!
    // ...
}
```

✅ **Good** — loading config inside the command action:
```java
public class ApiCommand {
    public static void execute(CommandContext ctx) {
        String token = Config.loadToken();  // Loaded at runtime, safe!
        // ...
    }
}
```

---

## 🧩 Putting It All Together

This structure, combined with the Maven configuration above, ensures:

- ✅ **Predictable native image builds** — no surprises from class initialization order.
- ✅ **Fast startup (~6 ms)** — the command tree is pre‑built at image generation time.
- ✅ **Clean separation of concerns** — CLI definition, command logic, and infrastructure are decoupled.
- ✅ **Easy to test** — command actions are simple static methods.

You can see this pattern in action in the **Gitetsu** project:
🔗 [https://github.com/FascesAedi/gitetsu](https://github.com/FascesAedi/gitetsu)

---

## 🚀 Building the Native Executable

Run the Maven package phase to trigger the native build:

```bash
mvn clean package
```

After a successful build, the native executable will be located in the `target/` directory with the same name as your artifact.

To run it directly:

```bash
./target/yourapp
```

---

## 🧪 Testing the Native Image

You can quickly test that your native image works as expected:

```bash
./target/yourapp --help
./target/yourapp version
```

---

## 📌 Important Notes

### ✅ No Reflection Configuration Needed

Because Kanketsu **never uses reflection**, you do **not** need to provide:
- `reflect-config.json`
- `resource-config.json`
- `jni-config.json`

This is one of the biggest advantages of the framework — your native build remains simple and maintainable.

### ⚠️ Class Initialization Caution

If your command tree is built in a static block (like `CLIHolder.cli` in the example), you **must** include that class in `--initialize-at-build-time`. Otherwise, the tree will be built once at build time and again at runtime, potentially causing duplication or subtle bugs.

### 🔗 HTTP Client Users

If your application uses `java.net.http.HttpClient`, the `--enable-native-access=ALL-UNNAMED` flag is **required**. Without it, you'll see errors like:

```
java.lang.IllegalAccessError: class jdk.internal.net.http.HttpClientBuilderImpl
```

### 🧩 Dependency Management

Make sure your `pom.xml` includes the correct Kanketsu version and all required dependencies. For reference:

```xml
<dependency>
    <groupId>io.github.fascesaedi</groupId>
    <artifactId>kanketsu-core</artifactId>
    <version>2.0.0</version>
</dependency>
<dependency>
    <groupId>io.github.fascesaedi</groupId>
    <artifactId>kanketsu-repl</artifactId>
    <version>2.0.0</version>
</dependency>
```

---

## 📚 Reference: Gitetsu Project

The configuration above is taken from the **Gitetsu** project — a production‑grade GitHub CLI tool built entirely on Kanketsu. You can explore the full source code and native build setup:

🔗 [https://github.com/FascesAedi/gitetsu](https://github.com/FascesAedi/gitetsu)

---

## 🧩 Gradle Equivalent (for reference)

If you're using Gradle, the `graalvm-native-image-plugin` configuration would look like this:

```gradle
graalvmNative {
    binaries {
        main {
            buildArgs.add('--no-fallback')
            buildArgs.add('-H:+ReportExceptionStackTraces')
            buildArgs.add('--enable-native-access=ALL-UNNAMED')
            buildArgs.add('--initialize-at-build-time=com.yourcompany.yourapp.CLIHolder')
            buildArgs.add('--initialize-at-build-time=com.yourcompany.yourapp.MyAppLogger')
            buildArgs.add('--initialize-at-build-time=io.github.fascesaedi.kanketsu')
        }
    }
}
```

> **Note**: Replace `com.yourcompany.yourapp` with your actual package name.

---

## 🚀 What's Next?

Now that you've built a native image, explore other Kanketsu topics:

- [⚡ Performance](performance.md) — Detailed benchmark results.
- [📦 Modules](modules.md) — Extend Kanketsu with REPL, logging, and more.