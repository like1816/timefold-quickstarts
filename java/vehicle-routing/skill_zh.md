# Vehicle Routing Auto Research

这是一个基于 Timefold AI 的车辆路径问题（VRPTW）Auto Research 实验。让 LLM 自动进行算法改进。

## Setup

设置新实验时，和用户配合：

1. **约定一个运行标签**：基于今天的日期建议一个标签（例如 `may5`）。分支 `autoresearch/<tag>` 必须不存在 — 这是一个全新的运行。
2. **创建分支**：执行 `git checkout -b autoresearch/<tag> autoresearch_alpha`。
3. **读取范围内的文件**：
   - `README.md` — 项目概述
   - `skill_zh.md` — Auto Research 工作流文档
   - `pom.xml` — Maven 依赖配置
   - `results.tsv` — 历史实验结果
   - `src/main/java/org/acme/vehiclerouting/benchmark/VehicleRoutingBenchmarkRunner.java` — Benchmark 运行器
   - `src/main/resources/vehicleRoutingBenchmarkConfig.xml` — 求解器配置
   - `src/main/java/org/acme/vehiclerouting/score/VehicleRoutingConstraintProvider.java` — 约束定义
   - `src/main/java/org/acme/vehiclerouting/solver/` — Auto Research 所有自定义组件（比较器、过滤器等），与 benchmark config 配套使用
   - `src/main/java/org/acme/vehiclerouting/domain/` — 领域模型（Vehicle, Visit, Location, Depot）
4. **利用知识库**：如果你的智能体有访问 Timefold 文档的 LLM wiki，请利用它。否则，请查阅在线 Timefold 文档，包括但不限于：
   - Local Search 策略（Late Acceptance、Simulated Annealing、Tabu Search）
   - Move 选择（Nearby Selection、SubList moves）
   - 评分计算（增量评分、constraint matches）
   - 构造启发式（First Fit、Best Fit、Cheapest Insertion）
5. **验证数据存在**：检查 `src/main/resources/input/problems/` 是否包含 Solomon 问题实例（如 r101.txt, c101.txt 等）。
6. **确认 results.tsv 存在**：此文件已被 git ignore，在本地持久化保存。不要重新创建 — 实验过程中直接追加新结果即可。（详见下面的 ## Output Format 和 ## Logging Results。）
7. **确认并开始**：确认 Setup 没问题。

获得确认后，开始实验。

## Experimentation

每个实验的运行时间通过 `vehicleRoutingBenchmarkConfig.xml` 中的 `<secondsSpentLimit>` 配置，默认约 5 分钟。运行命令：

```bash
cd java/vehicle-routing
mvn exec:java -Dexec.mainClass="org.acme.vehiclerouting.benchmark.VehicleRoutingBenchmarkRunner"
```

**你可以做什么：**
- 修改 `src/main/resources/vehicleRoutingBenchmarkConfig.xml` — 调整 Local Search 策略、Move Selector 等
- 在 `src/main/java/org/acme/vehiclerouting/solver/` 下添加新的自定义 Java 组件（比较器、过滤器等）

**你不可以做什么：**
- 修改 `src/main/java/org/acme/vehiclerouting/benchmark/` — Benchmark 运行器
- 修改 `src/main/java/org/acme/vehiclerouting/domain/` — 领域模型
- 修改 `src/main/java/org/acme/vehiclerouting/score/` — 评分约束（评价标准）
- 修改 `src/main/java/org/acme/vehiclerouting/rest/` — REST API
- 修改 `src/main/java/org/acme/vehiclerouting/util/` — 工具类
- 修改 `src/main/java/org/acme/vehiclerouting/app/` — 应用程序
- 修改评估套件（`results.tsv` 格式）
- 安装新包或添加依赖。只允许使用 `pom.xml` 中已有的依赖。
- 修改问题实例数据

**目标：** 最大化 Soft 分数（行驶时间），即使其接近 0（负数越小越好）。Timefold 中分数是负数（0 是最优）。例如，-96000 比 -100000 更好。
- Hard=0：满足所有硬约束（容量、时间窗）
- Medium=0：所有 visit 都被分配
- Soft：最小化行驶时间（越接近 0 越好）

## 原则

**1. 先想后写（Think Before Coding）**：在修改任何东西之前，先理解代码库和问题。问自己：为什么这个修改会有帮助？期望的结果是什么？

**2. 简单优先（Simplicity First）**：在其他条件相同的情况下，越简单越好。一个添加了丑陋复杂度的微小改进不值得。同样或更好的结果但通过简化代码实现是巨大的胜利。例如：提升了 0.01% 但增加了 50 行复杂配置？可能不值得。通过简化配置获得提升？绝对保留。

**3. 精准修改（Surgical Changes）**：每次只改一个东西，验证结果后再进行下一个。不要同时修改多个东西 — 这会让你无法知道什么起了作用、什么没有。

**4. 目标驱动执行（Goal-Driven Execution）**：始终记住目标：Hard=0（约束满足）、Medium=0（所有 visit 被分配）、Soft 越接近 0 越好（行驶时间越少）。如果一个修改不能让你更接近目标，就回退它。

**第一次运行**：你的第一次运行应该始终是建立基准。

## Output Format

脚本完成后会打印类似这样的摘要：

```
Benchmark completed! Report in local/benchmarkReport/2026-05-02_160405
Results logged to: results.tsv
Logged: 5708c51	r101	30556	Visit Waiting Time Sorter	0hard/0medium/-100920soft	30003	2534407	2534404	0	29050	success	local\benchmarkReport\2026-05-02_160405\2026-05-02_160406
```

## Logging Results

实验完成后，结果会自动记录到 `results.tsv`（制表符分隔）。

TSV 表头：

```
commit	problem	benchmark_time	config_name	final_score	run_time_ms	score_calc_count	move_eval_count	initial_score	convergence_time_ms	status	report_dir
```

字段说明：
1. `commit` — Git commit 哈希（7 字符）
2. `problem` — 问题实例（如 r101）
3. `benchmark_time` — 总耗时（毫秒）
4. `config_name` — 配置名称
5. `final_score` — 最终分数（格式：hard/medium/soft）
6. `run_time_ms` — 求解器运行时间（毫秒）
7. `score_calc_count` — 分数计算次数
8. `move_eval_count` — Move 评估次数
9. `initial_score` — 初始构造启发式后的分数
10. `convergence_time_ms` — 收敛时间（最后一次改进的时间）
11. `status` — `success` 或 `crash`
12. `report_dir` — 报告目录路径

示例：

```
5708c51	r101	30556	Visit Waiting Time Sorter	0hard/0medium/-100920soft	30003	2534407	2534404	0	29050	success	local/benchmarkReport/2026-05-02_160405/2026-05-02_160406
```

## 实验循环

实验在专用分支上运行（如 `autoresearch/may5`）。

循环（最多 50 次迭代）：

1. **查看 git 状态**：当前分支/提交
2. **每次只改一个东西**（精准修改原则）：
   - 编辑 `vehicleRoutingBenchmarkConfig.xml` 调整 Local Search 策略
   - 或在 `src/main/java/org/acme/vehiclerouting/solver/` 下添加自定义组件
3. **`git commit`**：记录更改（用于恢复）
4. **运行 benchmark**：`mvn exec:java -Dexec.mainClass="org.acme.vehiclerouting.benchmark.VehicleRoutingBenchmarkRunner"`
5. **检查结果**：查看控制台输出和 `results.tsv`
6. **如果崩溃**：调试控制台错误，尝试修复；如果想法从根本上被打破，记录 "crash" 并继续
7. **记录**：结果自动追加到 `results.tsv`（保持不被 git 跟踪）
8. **比较并决定**：
   - 如果分数改善（Soft 越接近 0，Hard=0, Medium=0）：保留 commit，推进分支
   - 如果分数相同或更差：`git reset` 回起点

**超时**：求解器的运行时间通过 `vehicleRoutingBenchmarkConfig.xml` 中的 `<secondsSpentLimit>` 配置，默认约 5 分钟。正常情况下，让求解器自动跑完 — 只有在崩溃或出现异常时才干预。如果运行时间远超配置时间，终止并视为失败。

**崩溃**：如果运行崩溃（OOM、bug 等），请使用你的判断：如果是愚蠢且容易修复的东西（例如拼写错误、缺少导入），修复它并重新运行。如果想法本身从根本上被打破，跳过它，记录为 "crash"，然后继续。

**迭代限制**：实验循环最多运行 **50 次迭代**。每次迭代约 6 分钟（5 分钟求解 + 1 分钟编译、统计、提交），共约 300 分钟（5 小时）。如果达到 50 次迭代仍未找到改进，停止并向用户报告。