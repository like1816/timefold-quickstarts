# Vehicle Routing Auto Research — Agent-Driven

这是一个基于 Timefold AI 的车辆路径问题（VRPTW）Auto Research 实验。智能体版 — 超越 LLM 串行迭代，实现知识驱动、并行探索、自动分析的自动化求解器优化系统。

## 核心差异

| 维度 | 原版 | 智能体版 |
|---|---|---|
| 策略选择 | LLM 凭直觉 | 历史实验 + 文档知识 + 优先级评分 |
| 执行方式 | 串行（一次一个） | git worktree 并行 + 子智能体隔离 |
| 结果分析 | 只看最终分数 | 收敛曲线 + 评估密度 + 分数分布 |
| 策略组合 | 无 | 自动组合 + 冲突检测 |
| 终止条件 | 固定 50 次迭代 | 收敛检测 + 分数阈值 + 最大迭代 |
| 配置验证 | 无（运行时才发现错误） | XML schema 预验证 |

---

## 基础规则

### Setup

设置新实验时：

1. **约定运行标签**：基于日期建议标签（如 `may3`）。分支 `autoresearch/<tag>` 必须不存在。
2. **创建分支**：`git checkout -b autoresearch/<tag> autoresearch_alpha`
3. **读取关键文件**：
   - `README.md` — 项目概述
   - `SKILL.md` — 本文档
   - `pom.xml` — Maven 依赖配置
   - `results.tsv` — 历史实验结果
   - `experiences.md` — 经验记录（无效/有效策略、关键经验）
   - `src/main/java/org/acme/vehiclerouting/benchmark/VehicleRoutingBenchmarkRunner.java` — Benchmark 运行器
   - `src/main/resources/vehicleRoutingBenchmarkConfig.xml` — 求解器配置
   - `src/main/java/org/acme/vehiclerouting/score/VehicleRoutingConstraintProvider.java` — 约束定义
   - `src/main/java/org/acme/vehiclerouting/solver/` — 自定义组件
   - `src/main/java/org/acme/vehiclerouting/domain/` — 领域模型
4. **利用知识库**：一定查阅本地知识库或在线Timefold文档，尤其注意接口和xml文件编写格式。
5. **验证数据存在**：`src/main/resources/input/problems/` 包含 Solomon 问题实例
6. **确认 results.tsv**：此文件 git-ignored，本地持久化。不要重新创建，直接追加。
7. **确认开始**

### 可以做什么 / 不能做什么

**可以**：
- 修改 `src/main/resources/vehicleRoutingBenchmarkConfig.xml` — 调整 Local Search 策略、Move Selector 等
- 在 `src/main/java/org/acme/vehiclerouting/solver/` 下添加自定义 Java 组件

**不可以**：
- 修改 `benchmark/`、`domain/`、`score/`、`rest/`、`util/`、`app/`
- 修改评估套件（results.tsv 格式）
- 安装新包或添加依赖（只使用 pom.xml 已有依赖）
- 修改问题实例数据

### 目标

- **Hard=0**：满足所有硬约束（容量、时间窗）
- **Medium=0**：所有 visit 被分配
- **Soft 越接近 0 越好**：最小化行驶时间

### 运行命令

```bash
cd java/vehicle-routing
mvn compile -q
mvn exec:java -Dexec.mainClass="org.acme.vehiclerouting.benchmark.VehicleRoutingBenchmarkRunner"
```

> ⚠️ benchmark runner 读取 `target/classes/`，每次改配置后必须先 `mvn compile`，否则静默使用旧配置。

### 结果格式

TSV 表头：
```
commit  problem  benchmark_time  config_name  final_score  run_time_ms  score_calc_count  move_eval_count  initial_score  convergence_time_ms  status  report_dir
```

---

## 工作流程（6 Phase）

### Phase 0: History Analysis（历史实验复用）

**目标**：避免重复已知无效策略，复用已知有效策略。

1. **读取 `experiences.md`**：
   - 提取已知无效策略（不再测试）
   - 提取已知有效策略（可复用或微调）
   - 提取关键经验教训

2. **读取 results.tsv**（如存在）：
   - 解析所有历史实验结果
   - 统计各策略平均表现
   - 识别收敛最快的策略

3. **输出**：
   - `excluded_strategies`：已知无效策略列表
   - `candidate_strategies`：已知有效策略列表

### Phase 1: Knowledge Extraction（知识提取 + 配置模板）

**目标**：从文档中提取可优化维度和配置语法，避免运行时错误。

1. **利用 LLM 知识库或查阅在线 Timefold 文档**，提取：
   - Local Search 策略（Late Acceptance, Simulated Annealing, Tabu Search, Hill Climbing, Great Deluge）
   - Move Selector（Change, Swap, SubList, Union, Nearby）
   - Acceptor（Late Acceptance 历史大小, SA 温度, Tabu 大小, Fading Tabu）
   - Forager（acceptedCountLimit, pickEarlyType）
   - Construction Heuristic（First Fit, Best Fit, Cheapest Insertion, FFD）
   - 自定义组件（Comparator, Filter, MoveFactory）

2. **提取 XML 配置语法和约束规则**：
   - 元素顺序：`unionMoveSelector` → `acceptor` → `forager`
   - 互斥规则：`localSearchType` 与 `acceptor` 不能同时配置
   - 命名规则：benchmark name 只能包含 `[\w\d _\-\.()]`
   - 类型约束：`minimumSubListSize` ≥ 1, `lateAcceptanceSize` ≥ 3
   - Schema 限制：`entitySelector` 必须包裹在 move selector 中

3. **输出**：
   - 优化策略清单（排除 excluded_strategies）
   - 每个策略的配置模板（已验证语法）
   - 优先级评分：`score = expected_improvement / implementation_cost`

### Phase 2: Baseline（建立基线）

1. 运行当前配置的 benchmark
2. 记录基线分数、收敛曲线、评估次数
3. 分析基线瓶颈：
   - 收敛时间 vs 总时间（是否还有优化空间）
   - 评估次数密度（评估越快，可探索更多 move）
   - Hard/Medium 约束是否已满足

### Phase 3: Parallel Exploration（并行探索）

**目标**：使用 git worktree 隔离，子智能体并行测试独立策略。

1. **策略分组**（按独立性）：
   - **互斥组**（Local Search 策略）：LA, SA, Tabu, Hill Climbing — 串行测试
   - **可组合组**（Move Selector + Acceptor + Forager）：可与 LA 组合测试
   - **独立组**（Custom Components）：Comparator, Filter — 可并行测试

2. **git worktree 隔离**（解决并行 git 冲突）：
   ```bash
   git worktree add /tmp/ar-worker-1 autoresearch_belta
   git worktree add /tmp/ar-worker-2 autoresearch_belta
   # 每个 worker 独立修改配置、编译、运行
   git worktree remove /tmp/ar-worker-1
   ```

3. **配置预验证**（运行时前检查）：
   - XML 格式检查（元素顺序、必填字段）
   - 互斥规则检查
   - 命名规则检查
   - 数值范围检查

4. **子智能体执行**（每个 worker 独立）：
   - 修改配置（使用 Phase 1 的配置模板）
   - `git commit` → `mvn compile -q` → 运行 benchmark → 记录结果 → `git reset`（如果无效）

5. **有效策略判定**：
   - Soft score 改善 > 0.1%（相对于 baseline）
   - Hard = 0, Medium = 0（约束必须满足）
   - 收敛时间 < 总时间 × 0.9（未过早收敛）

### Phase 4: Combination（策略组合）

1. 从 Phase 3 筛选有效策略
2. 生成组合方案（笛卡尔积，排除已知冲突）
3. 测试组合效应：
   - 组合分数 > 各策略单独分数之和 → 正协同
   - 组合分数 < 最差策略 → 负协同（冲突）
4. 记录最佳组合

### Phase 5: Fine-tuning（微调 + 收敛检测）

1. 对最佳组合进行参数搜索：
   - Late Acceptance size：[30, 40, 50, 60, 75, 100]
   - acceptedCountLimit：[500, 1000, 2000]
   - SubList 大小范围：[1-5, 1-10, 1-20]

2. **收敛检测自动终止**：
   - 连续 5 次实验分数变化 < 0.05% → 终止微调
   - 达到最大迭代数（10 次）→ 终止
   - 达到分数阈值（接近 sintef 最优解）→ 终止

### Phase 6: Report（自动分析 + 报告生成）

1. **解析 BEST_SCORE.csv**（每个实验的报告目录）：
   - 提取初始分数、收敛时间、分数变化曲线
   - 计算收敛速度（分数/时间）

2. **生成实验报告**（`agent_research_report.md`）：
   - 基线 vs 最优分数对比
   - 策略有效性排名
   - 收敛曲线对比
   - 策略协同效应分析
   - 最终配置 + 配置模板
   - 排除策略及原因

3. **输出产物**：
   - `results.tsv` — 所有实验结果
   - `agent_research_report.md` — 智能体研究报告
   - 最优配置的 `vehicleRoutingBenchmarkConfig.xml`
   - 所有有效自定义 Java 组件

---

## 配置模板库（Timefold 2.0 验证版）

### Late Acceptance
```xml
<localSearch>
  <acceptor>
    <acceptorType>LATE_ACCEPTANCE</acceptorType>
    <lateAcceptanceSize>50</lateAcceptanceSize>
  </acceptor>
</localSearch>
```

### Simulated Annealing
```xml
<localSearch>
  <acceptor>
    <acceptorType>SIMULATED_ANNEALING</acceptorType>
    <simulatedAnnealingStartingTemperature>0hard/0medium/500soft</simulatedAnnealingStartingTemperature>
  </acceptor>
</localSearch>
```

### Tabu Search
```xml
<localSearch>
  <acceptor>
    <acceptorType>TABU_SEARCH</acceptorType>
    <entityTabuSize>3</entityTabuSize>
  </acceptor>
</localSearch>
```

### Union Move Selector
```xml
<localSearch>
  <unionMoveSelector>
    <changeMoveSelector/>
    <swapMoveSelector/>
    <subListChangeMoveSelector>
      <subListSelector>
        <minimumSubListSize>1</minimumSubListSize>
        <maximumSubListSize>10</maximumSubListSize>
      </subListSelector>
    </subListChangeMoveSelector>
  </unionMoveSelector>
</localSearch>
```

### Forager
```xml
<localSearch>
  <forager>
    <acceptedCountLimit>1000</acceptedCountLimit>
  </forager>
</localSearch>
```

### Construction Heuristic
```xml
<constructionHeuristic>
  <constructionHeuristicType>FIRST_FIT_DECREASING</constructionHeuristicType>
</constructionHeuristic>
```

### 完整配置模板
```xml
<?xml version="1.0" encoding="UTF-8"?>
<plannerBenchmark xmlns="https://timefold.ai/xsd/benchmark">
  <benchmarkDirectory>local/benchmarkReport</benchmarkDirectory>
  <inheritedSolverBenchmark>
    <solver>
      <solutionClass>org.acme.vehiclerouting.domain.VehicleRoutePlan</solutionClass>
      <entityClass>org.acme.vehiclerouting.domain.Vehicle</entityClass>
      <entityClass>org.acme.vehiclerouting.domain.Visit</entityClass>
      <scoreDirectorFactory>
        <constraintProviderClass>org.acme.vehiclerouting.score.VehicleRoutingConstraintProvider</constraintProviderClass>
      </scoreDirectorFactory>
      <termination>
        <secondsSpentLimit>60</secondsSpentLimit>
      </termination>
      <constructionHeuristic/>
      <localSearch>
        <!-- Move Selector, Acceptor, Forager 按此顺序 -->
      </localSearch>
    </solver>
  </inheritedSolverBenchmark>
  <solverBenchmark>
    <name>Config-Name-Here</name>
  </solverBenchmark>
</plannerBenchmark>
```

---

## XML 配置约束规则

| 规则 | 说明 | 违反后果 |
|---|---|---|
| 元素顺序 | `unionMoveSelector` → `acceptor` → `forager` | XML 验证失败 |
| localSearchType 互斥 | 不能与 `acceptor` 同时配置 | IllegalArgumentException |
| benchmark name | 只能包含 `[\w\d _\-\.()]` | IllegalArgumentException |
| minimumSubListSize | 必须 ≥ 1 | IllegalArgumentException |
| lateAcceptanceSize | 必须 ≥ 3 | IllegalArgumentException |
| entitySelector | 必须包裹在 move selector 中 | XML 验证失败 |
| acceptedCountLimit | forager 的子元素，不是 selectedCountLimit | XML 验证失败 |
| simulatedAnnealingStartingTemperature | 需要完整分数格式 `0hard/0medium/500soft` | IllegalArgumentException |

---

## 原则

1. **Think Before Coding**：在修改任何东西之前，先理解代码库和问题。问自己：为什么这个修改会有帮助？期望的结果是什么？
2. **Simplicity First**：在其他条件相同的情况下，越简单越好。一个添加了丑陋复杂度的微小改进不值得。
3. **Surgical Changes**：每次只改一个东西，验证结果后再进行下一个。不要同时修改多个东西。
4. **Goal-Driven Execution**：始终记住目标：Hard=0、Medium=0、Soft 越接近 0 越好。如果一个修改不能让你更接近目标，就回退它。
