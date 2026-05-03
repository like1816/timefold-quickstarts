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

### Phase 0: History Analysis（历史实验分析）
读取 `experiences.md` + `results.tsv`，输出实验数据分析：各策略的分数、收敛时间、评估密度、与 baseline 的差距。**不做主观判断**——不标记"好/坏"策略，只呈现数据。

### Phase 1: Direction Analysis（方向分析）
参考完整模板 `src/main/resources/vehicleRoutingBenchmarkConfig_FULL_TEMPLATE.xml`，结合 Phase 0 的数据分析，识别：哪些维度还没探索过、哪些方向有改进空间、哪些组合还没试过。产出 Phase 3 的**实验方向建议**（不是任务列表）。

### Phase 2: Baseline（建立基线）
运行当前配置 benchmark，记录基线分数、收敛曲线、评估次数。分析瓶颈：收敛时间占比、评估密度、Hard/Medium 约束状态。

### Phase 3: Parallel Exploration（并行探索）
**主智能体分配方向 → 5 个子智能体并行实验 → 主智能体选优 commit。** 每轮 = 种群大小 5 的并行搜索，仅最优者保留。

1. **分配方向**：主智能体从 Phase 1 的实验方向建议中挑选 5 个方向，确保覆盖不同维度。每个方向包含：基准配置（当前 best-known）、要探索的维度。**子智能体自主决定具体策略组合**，不受历史实验限制。
2. **创建 worktree**：`git worktree add /tmp/ar-w{1-5} autoresearch_beta`
3. **子智能体执行**：在 worktree 中改配置 → `mvn compile -q` → 跑 benchmark → 返回 JSON 结果。**不执行 git 写操作。**
4. **收集结果 + 选优**：按 `soft_score` 排序，参考 `convergence_time_ms` / `move_eval_count`。
   - 有改善（> 0.1%）：应用最优配置到主分支
   - 全部失败：记录所有结果到 results.tsv，进入下一轮
   - 次优保留：接近 baseline 且收敛快的配置作为下一轮起点
5. **提交代码**：`git add` → `git commit -m "Phase3: <config_name> (<improvement>%)"` → 更新 best-known
6. **清理 worktree**：`git worktree remove /tmp/ar-w{1-5}`

**结果上报格式**（子智能体必须返回 JSON）：
```json
{"config_name": "LA size 45", "soft_score": -96000, "final_score": "0hard/0medium/-96000soft", "convergence_time_ms": 4500, "move_eval_count": 8500000, "status": "success", "error_log": ""}
```

**判定规则**：改善 > 0.1%、Hard=0、Medium=0、收敛时间 < 总时间 × 0.9。**5 个实验全部写入 results.tsv**（status: keep/discard/crash），不浪费任何算力——失败实验的收敛曲线、评估密度、错误模式都是 Phase 0 数据分析的重要素材。

### Phase 4: Combination（策略组合）
从 Phase 3 有效策略生成组合方案（笛卡尔积，排除冲突），测试协同效应（正/负协同），记录最佳组合。

### Phase 5: Fine-tuning（微调 + 收敛检测）
对最佳配置进行参数微调（LA size、acceptedCountLimit、SubList 范围）。收敛检测：连续 5 次变化 < 0.05% → 终止；最大 15 次迭代；或达到分数阈值。

### Phase 6: Report（自动分析 + 报告生成）
解析 BEST_SCORE.csv 提取收敛曲线，生成 `agent_research_report.md`（基线对比、策略排名、曲线对比、协同分析、最终配置）。输出：results.tsv、报告、最优配置 XML、有效自定义组件。

---

## 原则

遵守 karpathy-guidelines 技能：Think Before Coding、Simplicity First、Surgical Changes、Goal-Driven Execution。
