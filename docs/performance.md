# ⚡ Performance

Kanketsu is designed for **predictable performance**, **minimal startup time**, and **excellent GraalVM Native Image compatibility**.

Rather than relying on reflection, annotation processing, or runtime scanning, Kanketsu performs command registration explicitly and resolves commands using lightweight data structures.

The following benchmarks demonstrate the performance characteristics of the current implementation.

---

# 🚀 Highlights

| Metric                   | Result                  |
| ------------------------ |-------------------------|
| Native Image Startup     | **~6 ms**               |
| Single-thread Throughput | **~1.13 million ops/s** |
| 16-thread Throughput     | **~3.06 million ops/s** |
| Average Parse Latency    | **~351 ns/op**          |

---

# 🚀 Native Image Startup

One of Kanketsu's primary goals is to provide excellent startup performance for GraalVM Native Image applications.

## Result

```text
$ time ./KanketsuTest

HI

real    0m0.006s
user    0m0.000s
sys     0m0.006s
```

### Interpretation

| Metric   |  Result  | Description                                                                                       |
| :------- | :------: | :------------------------------------------------------------------------------------------------ |
| **real** | **6 ms** | Total wall-clock execution time                                                                   |
| **user** | **0 ms** | Rounded to zero by the Linux `time` utility (actual execution time is below measurable precision) |
| **sys**  | **6 ms** | Operating system and virtualization overhead                                                      |

The measured startup time is approximately **6 ms** under WSL2 using GraalVM Native Image.

---

# ⚡ Parsing Throughput

Long-running applications benefit from high parsing throughput.

The following benchmark measures Kanketsu under randomly generated command combinations using JMH.

## Multi-thread (16 Threads)

## Single-thread

| Metric          |         Result          |
| :-------------- |:-----------------------:|
| Throughput      | **~1.13 million ops/s** |
| Average Latency |    **~884.96 ns/op**    |

## Multi-thread

| Metric     |         Result          |
| :--------- |:-----------------------:|
| Throughput | **~3.06 million ops/s** |

The benchmark uses randomly generated nested commands (up to three command levels) to simulate realistic CLI workloads.

---

# 📊 Comparison

The following comparison uses identical benchmark scenarios.

| Metric                   |      Kanketsu       |              Picocli               |
| :----------------------- |:-------------------:|:----------------------------------:|
| Native Image Startup     |      **~6 ms**      |               ~3 ms*               |
| Single-thread Throughput |  **~1.13 M ops/s**  |            ~42 K ops/s             |
| 16-thread Throughput     |  **~3.06 M ops/s**  |            ~184 K ops/s            |
| Average Latency          |  **~884.96 ns/op**  |            ~23.7 μs/op             |

> *Picocli startup time is quoted from the official Picocli GraalVM benchmark article.

---

# 💡 Why Is Kanketsu Fast?

Kanketsu's performance is a direct result of its architecture.

## 🚫 Zero Reflection

Command registration and execution never rely on runtime reflection.

This eliminates reflection overhead while making GraalVM Native Image compilation straightforward.

---

## 🌳 Lightweight Command Tree

Commands are stored as a lightweight tree structure.

Resolving a command such as:

```text
git remote add
```

is simply a traversal of that tree.

---

## 📝 Lightweight Parser

The parser is implemented using straightforward string processing.

Supported features include:

* short options (`-v`)
* combined short options (`-abc`)
* attached values (`-fconfig.yml`)
* long options (`--file`)
* long option assignment (`--file=config.yml`)
* option terminator (`--`)

No parser generators or external libraries are involved.

---

## 📦 Zero Transitive Dependencies

The Core module introduces no transitive dependencies.

This minimizes:

* startup overhead;
* class loading;
* dependency size.

---

## 🏗 AOT First

Kanketsu is designed for GraalVM Native Image from the beginning.

The Core avoids:

* reflection;
* runtime scanning;
* annotation processing.

As a result, no additional reflection configuration is required.

---

# 🧪 Benchmark Environment

| Component        | Version                           |
| :--------------- | :-------------------------------- |
| CPU              | AMD Ryzen 7 2700 (8C / 16T, Zen+) |
| Operating System | Windows 11 / WSL2                 |
| JDK              | GraalVM JDK 25.0.4                |
| Build Tool       | Maven                             |
| Native Compiler  | GraalVM Native Image (CE)         |

---

# 📄 Benchmark Source

## Native Image Startup

### Simple.java

```java
import core.io.github.kanketsuteam.kanketsu.CLI;

public class Simple {
    public static void main(String[] args) {
        CLI cli = CLIHolder.cli;
        cli.execute("hi");
    }
}
```

### CLIHolder.java

```java
import core.io.github.kanketsuteam.kanketsu.CLI;

public class CLIHolder {

    public static CLI cli = CLI.builder()
            .command("hi", hi -> hi
                    .action(ctx -> System.out.println("HI")))
            .build();

}
```

The command tree is initialized during class loading so that the benchmark measures only application startup and execution.

---

## JMH Benchmark

Benchmark implementation:

* `KanketsuStressTest`

Complete benchmark logs:

* `KanketsuMultithreading`
* `KanketsuSingleThreaded`

---

# 📚 Reference Benchmark (Picocli)

## Native Image Startup

Startup measurements reference the official Picocli benchmark article:

> **Picocli on GraalVM: Blazingly Fast Command Line Apps**

## JMH Results

### 16 Threads

```text
Benchmark                     Mode  Cnt    Score    Error   Units
PicocliStressTest.testParse  thrpt   40  183.891 ± 3.157  ops/ms
```

### Single Thread

```text
Benchmark                     Mode  Cnt    Score    Error   Units
PicocliStressTest.testParse  thrpt   40   42.179 ± 0.426  ops/ms
```

| Metric                   |      Result      |
| :----------------------- | :--------------: |
| 16-thread Throughput     | **~184 K ops/s** |
| Single-thread Throughput |  **~42 K ops/s** |
| Average Latency          |  **~23.7 μs/op** |

The same benchmark workload was used for both frameworks to provide a meaningful comparison.
