# Vehicle Routing Auto Research — Agent-Driven

这是一个基于 Timefold AI 的车辆路径问题（VRPTW）Auto Research 实验。智能体版 — 超越 LLM 串行迭代，实现知识驱动、并行探索、自动分析的自动化求解器优化系统。

## 基础规则

### Setup
⚠️ **这个过程非常重要，必须要和用户确认执行结果。**
设置新实验时：

1. **约定运行标签**：基于日期建议标签（如 `may4`）。分支 `autoresearch/<tag>` 必须不存在。
2. **创建分支**：`git checkout -b autoresearch/<tag> autoresearch_beta`
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
4. **利用知识库**：一定查阅关于Timefold的本地的LLM Wiki知识库或线上官方文档，尤其注意接口和xml文件编写格式。
5. **验证数据存在**：`src/main/resources/input/problems/` 包含 Solomon 问题实例
6. **确认 results.tsv**：此文件 git-ignored，本地持久化。不要重新创建，直接追加。
7. **基线确认**：运行当前配置 benchmark，记录基线分数、收敛曲线、评估次数。
8. **确认开始**：以上步骤完成后，等待用户确认再进入自动流程。

### 可以做什么 / 不能做什么 (非常重要，主、子智能体**必须**遵守)

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
commit	problem	benchmark_time	config_name	final_score	run_time_ms	score_calc_count	move_eval_count	initial_score	convergence_time_ms	status	description
```

> ⚠️ `description` 列：简短描述策略内容（如 "LA size=50 + Forager pickEarly=FIRST_BEST"）

---

## 自动循环流程

Setup 完成后，自动执行以下循环：

### 每轮流程（共 20 轮，每轮 5 个子智能体）

**主智能体职责**：

1. **分析历史**：读取 `results.tsv` + `experiences.md`，分析上一轮结果
2. **分配方案**：给出 5 个优化方案，可以从五个方向，可以是类似策略的组合，也可以是同一种策略的不同参数
3. **收集结果**：收集 5 个子智能体的返回结果
4. **选优 + 提交/回退**：
   - 选出最优配置（改善 > 0.1%，Hard=0, Medium=0, 收敛时间 < 总时间 × 0.9）
   - **改善**：`git add` → `git commit -m "Round N: <config_name> (<improvement>%)"` → 作为下一轮起点
   - **全部失败**：回退代码 → 继续下一轮
5. **总结经验**：写入 `experiences.md`
6. **清理 worktree**：`git worktree remove /tmp/ar-w{1-5}`

**子智能体职责**：

1. 在 worktree 中执行代码优化
2. `mvn compile -q` → 跑 benchmark → 返回 JSON 结果
3. **不执行 git 写操作**

**子智能体返回格式**（JSON）：
```json
{"config_name": "LA size 45", "soft_score": -96000, "final_score": "0hard/0medium/-96000soft", "convergence_time_ms": 4500, "move_eval_count": 8500000, "status": "success", "error_log": "", "description": "LA size=45, 比基线 LA400 更激进"}
```

**异常处理**：
- 子智能体崩溃：主智能体决定是否调试或记录 crash
- 子智能体超时/异常：主智能体可随时终止（打断）
- 所有 5 个实验全部写入 results.tsv（status: keep/discard/crash）

---

## 全局结束条件

**20 轮循环完成**（每轮 5 个子智能体，共 100 次实验）。

⚠️ **重要**：
- `results.tsv` 是**追加模式**，不清空
- 每轮结束后只保留最优配置（改善 > 0.1%），失败配置回退
- 结束条件：20 轮循环完成，或连续 5 轮无改善提前终止

---

## 报告生成

20 轮完成后，主智能体生成 `agent_research_report.md`：
- 基线对比
- 策略排名（按分数改善排序）
- 收敛曲线对比
- 策略协同效应分析
- 最终配置 + 配置模板
- 排除策略及原因

报告打印给用户。

---

## 原则

遵守 karpathy-guidelines 技能：Think Before Coding、Simplicity First、Surgical Changes、Goal-Driven Execution。
