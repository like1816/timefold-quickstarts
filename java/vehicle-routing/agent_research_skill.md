# Agent-Driven Auto Research

智能体版 autoresearch — 超越 LLM 串行迭代，实现知识驱动、并行探索、自动分析的自动化求解器优化系统。

## 核心差异

| 维度 | 原版 (skill.md) | 智能体版 |
|---|---|---|
| 策略选择 | LLM 凭直觉 | 系统性阅读文档 → 提取策略 → 优先级排序 |
| 执行方式 | 串行（一次一个） | 并行（多策略同时测试） |
| 结果分析 | 只看最终分数 | 分析收敛曲线、评估密度、分数分布 |
| 策略组合 | 无 | 自动组合已验证的有效策略 |
| 终止条件 | 固定 50 次迭代 | 收敛检测 + 分数阈值 + 最大迭代 |

## 工作流程

### Phase 1: Knowledge Extraction（知识提取）

1. 读取 `docs/` 目录下所有 Timefold 文档
2. 提取所有可优化的配置维度：
   - Local Search 策略（Late Acceptance, Simulated Annealing, Tabu Search）
   - Move Selector（Change, Swap, SubList, Nearby）
   - Acceptor（Late Acceptance 历史大小, SA 温度, Tabu 大小）
   - Forager（acceptedCountLimit, pickEarlyType）
   - Construction Heuristic（First Fit, Best Fit, Cheapest Insertion）
   - 自定义组件（Comparator, Filter, MoveFactory）
3. 输出：优化策略清单 + 优先级排序

### Phase 2: Baseline（建立基线）

1. 运行当前配置的 benchmark
2. 记录基线分数和收敛曲线
3. 分析基线的瓶颈（收敛速度、评估次数等）

### Phase 3: Parallel Exploration（并行探索）

1. 将 Phase 1 的策略按独立性分组
2. 每个组内的策略可以并行测试（互不影响）
3. 每个策略：
   - 修改配置
   - `git commit`
   - 编译 + 运行 benchmark
   - 记录结果
   - `git reset`（如果无效）
4. 使用子智能体并行执行独立策略

### Phase 4: Combination（策略组合）

1. 从 Phase 3 中筛选出有效的策略
2. 组合有效策略，测试协同效应
3. 记录最佳组合

### Phase 5: Fine-tuning（微调）

1. 对最佳组合进行参数微调
2. 使用更细粒度的搜索（如不同的 historySize、temperature）
3. 记录最优参数

### Phase 6: Report（报告）

1. 生成完整的实验报告
2. 包含：基线 vs 最优分数、策略有效性排名、收敛曲线对比、最终配置

## 当前项目可优化维度

基于项目代码分析，以下是具体的优化方向：

### 1. Local Search 策略
- 当前：空（Timefold 默认 Hill Climbing）
- 可尝试：Late Acceptance, Simulated Annealing, Tabu Search

### 2. Move Selector
- 当前：无（使用默认 Change Move）
- 可尝试：
  - `swapMoveSelector` — 交换两个 Visit 的位置
  - `subListChangeMoveSelector` — 移动 Visit 子序列
  - `unionMoveSelector` — 组合多个 selector

### 3. Nearby Selection
- 当前：无
- 可尝试：添加 `nearbySelectionVariableMimicSelector`，只选择空间上接近的 Visit 进行交换，大幅减少搜索空间

### 4. Construction Heuristic
- 当前：空（默认 First Fit）
- 可尝试：Cheapest Insertion, Best Fit

### 5. Custom Components
- 当前：`VisitWaitingTimeComparator`（未使用）
- 可尝试：
  - 在 Local Search 中使用 Comparator 排序 Move
  - 添加 Filter 过滤无效 Move
  - 添加 Distance-based Comparator

### 6. Termination
- 当前：60 秒
- 可尝试：调整时间、使用 ScoreCalculationCountLimit

## 输出产物

1. `results.tsv` — 所有实验结果
2. `agent_research_report.md` — 智能体研究报告
3. 最优配置的 `vehicleRoutingBenchmarkConfig.xml`
4. 所有有效自定义 Java 组件
