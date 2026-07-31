# 性能

## 测试环境
- **CPU**: AMD Ryzen 7 2700 (8C/16T, 3.2GHz Base, Zen+ 架构)
- **操作系统**: Windows 11 + WSL 2 (Native Image 测试) / Windows Native (JMH 测试)
- **JDK**: GraalVM JDK 21.0.12 (Java HotSpot VM)
- **构建工具**: Maven + Native Image (GraalVM CE)

## 目录

[Kanketsu](#Kanketsu)

[Picocli](#Picocli)

## Kanketsu

### GraalVM Native Image WSL 模拟Linux理论启动极限

#### 实测结果
```bash
$ time ./KanketsuTest
HI

real    0m0.006s
user    0m0.000s
sys     0m0.006s
```

#### 性能换算
| 指标 | 数值   | 解析     |
| :--- |:-----|:--|
| **real** | 6ms  | **物理世界总响应时间。** |
| **user** | 0ms  | **代码逻辑已达物理测量极限。** |
| **sys** | 6ms | **系统调用与虚拟化开销。** |
>因为**Linux**的**time**已经无法捕捉`user`的时间开销，四舍五入为0ms。（实际应为<**0.5ms**）



### JMH 稳态吞吐量压测 (JIT 峰值性能)

> 模拟生产环境长时间运行下的框架吞吐上限，命令组合完全随机。

#### 实测结果
```bash
Benchmark                      Mode  Cnt     Score    Error   Units
KanketsuStressTest.testParse  thrpt   40  6464.102 ± 64.673  ops/ms
```

#### 性能换算
| 指标 | 数值               | 说明 |
| :--- |:-----------------| :- |
| **16 线程并发吞吐量** | **646 万 ops/s**  | 基于 16 线程测试，反映整体处理能力，但不可直接换算延迟。|
| **单线程吞吐量** | **	285 万 ops/s** | 基于 1 线程测试。 |
| **单线程平均延迟** | **~351 ns/op**   | 基于 1 线程测试，代表单次解析的真实耗时。 |

> **解读**：  
> 在包含 `git remote add`、`git config set` 等深度嵌套子命令（最深 3 级）的场景下，单线程下每次解析耗时约 351 纳秒，16 线程并发总吞吐量可达 646 万次/秒，远超常规 CLI 解析器（通常需 1~5 微秒），足以支撑高频指令交互场景。
> 该性能已远超常规 CLI 解析器（通常需 1~5 微秒），足以支撑高频指令交互场景（如实时 REPL、自动化脚本引擎）。


### 测试代码

#### GraalVM Native Image的极简代码
Simple:
```Java
import io.github.fascesaedi.kanketsu.core.CLI;

public class Simple {
    public static void main(String[] args) {
        CLI cli = CLIHolder.cli;
        cli.execute("hi");
    }
}

```
CLIHolder:
```Java
import io.github.fascesaedi.kanketsu.core.CLI;

public class CLIHolder {
    public static CLI cli = CLI
            .builder()
            .command("hi", hi -> hi
                    .action(cxt -> {
                        System.out.println("HI");
                    })).build();
}
```
>编译时将注册命令部分移至编译期

#### JMH 测试代码
KanketsuStressTest:
[KanketsuStressTest](kanketsu-benchmark/src/main/java/io/github/fascesaedi/kanketsu/KanketsuStressTest.java)
##### 完整测试结果：
16线程测试结果：
[KanketsuMultithreading](kanketsu-benchmark/src/logs/KanketsuMultithreading)
1线程测试结果：
[KanketsuSingleThreaded](kanketsu-benchmark/src/logs/KanketsuSingleThreaded)

## Picocli

### GraalVM Native Image启动速度

此处引用[Picocli on GraalVM: Blazingly Fast Command Line Apps](https://picocli.info/picocli-on-graalvm.html)的数据 `3ms` ，详细请阅读 **Picocli on GraalVM: Blazingly Fast Command Line Apps** 的文章内容。

### JMH 稳态吞吐量压测 (JIT 峰值性能)

> 模拟生产环境长时间运行下的框架吞吐上限，命令组合完全随机，与 Kanketsu 使用完全相同的测试用例（深度嵌套子命令，最多 3 级）。

#### 实测结果

```bash
# 16 线程并发
Benchmark                     Mode  Cnt    Score    Error   Units
PicocliStressTest.testParse  thrpt   40  183.891 ±  3.157  ops/ms

# 单线程
Benchmark                     Mode  Cnt    Score    Error   Units
PicocliStressTest.testParse  thrpt   40   42.179 ±  0.426  ops/ms
```

#### 性能换算

| 指标 | 数值 | 说明 |
| :--- | :--- | :--- |
| **16 线程并发吞吐量** | **183.891 ops/ms ≈ 18.4 万 ops/s** | 基于 16 线程测试，反映整体处理能力 |
| **单线程吞吐量** | **42.179 ops/ms ≈ 4.2 万 ops/s** | 基于 1 线程测试 |
| **单线程平均延迟** | **~23.7 µs/op** | 基于 1 线程测试，代表单次解析的真实耗时 |

> **解读**：  
> Picocli 基于注解 + 反射实现，在相同复杂命令结构下，单次解析耗时约 **23.7 微秒**，16 线程并发吞吐量约 **18.4 万次/秒**。该性能足以满足大多数常规 CLI 应用需求，但与 Kanketsu（单次 ~351 纳秒，646 万/秒）相比，存在约两个数量级的差距，主要源于反射开销和注解处理机制。

---

### 测试代码
PicocliStressTest:
[PicocliStressTest](kanketsu-benchmark/src/main/java/io/github/fascesaedi/kanketsu/PicocliStressTest.java)