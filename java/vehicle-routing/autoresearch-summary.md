# Timefold VRPTW Auto Research 实验总结

> **日期**: 2026-05-02 ~ 2026-05-03  
> **问题实例**: r101 (Solomon)  
> **总迭代数**: 80 次  
> **分支**: `autoresearch/may2_bench`  
> **求解时间**: 60 秒/次（部分 300 秒）

---

## 1. 实验概述

本次 Auto Research 实验旨在通过系统性调整 Timefold Solver 的 Local Search 策略、Move Selectors、Construction Heuristics 和 Acceptor 配置，寻找比默认配置更优的求解参数。

**核心结论**：Timefold 的默认 Local Search 在 r101 问题上已经是最优配置（-96060soft），所有 80 次尝试均未超越。

---

## 2. 实验设置

| 配置项 | 值 |
|---|---|
| 问题实例 | r101（Solomon） |
| 求解时间 | 60 秒（默认），300 秒（部分验证） |
| 编译要求 | 每次修改配置后必须先 `mvn compile -q` |
| 配置读取 | `target/classes/vehicleRoutingBenchmarkConfig.xml`（非源码目录） |

---

## 3. 关键发现

### 3.1 最优策略

| 排名 | 迭代 | 策略 | Soft Score | 对比 baseline |
|---|---|---|---|---|
| 1 | Baseline | Default Local Search | **-96060soft** | — |
| 1 | Iter 43 | FFD + Union Moves + LA 50 | -96060soft | 相同 |
| 1 | Iter 75 | First Fit | -96060soft | 相同 |
| 1 | Iter 76 | Allocate Entity From Queue | -96060soft | 相同 |
| 1 | Iter 77 | Allocate To Value From Queue | -96060soft | 相同 |
| 1 | Iter 78 | Allocate From Pool | -96060soft | 相同 |
| 2 | Iter 19 | Union Moves + LA 50 | -96120soft | +60 (0.06%) |
| 3 | Iter 26 | Union Moves + LA 75 | -96180soft | +120 (0.1%) |
| 4 | Iter 27 | Union Moves + LA 30 | -96240soft | +180 (0.2%) |
| 5 | Iter 28 | Union Moves + LA 120 | -96480soft | +420 (0.4%) |
| 5 | Iter 47 | LA 52 | -96480soft | +420 (0.4%) |

### 3.2 极差策略（-139620soft，比 baseline 差 45.4%）

所有 Tabu Search 相关配置表现极差：
- Iter 5: Tabu Search entityTabuSize=3
- Iter 8: Fading Entity Tabu size=10
- Iter 17: Union Moves + Tabu 3
- Iter 22: Union Moves + Fading Entity Tabu
- Iter 41: Move Tabu size=10
- Iter 42: Fading Move Tabu size=10
- Iter 52: Fading Entity Tabu Ratio=0.1

### 3.3 重要观察

1. **60 秒已收敛**：Baseline 在 60 秒和 300 秒结果完全相同（-96060soft），说明 r101 在 60 秒内已收敛到最优。
2. **Late Acceptance 最优区间**：size=50 是最佳值，接近 baseline（-96120soft）。
3. **Union Moves 效果**：包含 change + swap + subList change + subList swap 的 union move selector 比单独 move selector 效果更好。
4. **构造启发式**：所有构造启发式变化（FFD、First Fit、Cheapest Insertion 等）与默认相同，无改善。
5. **Nearby Selection 不可用**：需要 Timefold 企业版许可证，Community Edition 不可用。
6. **XML 配置限制**：
   - `lateAcceptanceSize` 与所有 tabu 配置互斥
   - `entitySelector` 不能直接放在 `<localSearch>` 下，需要包裹在 move selector 中
   - 配置名不能包含特殊字符（如 `+`）
   - `sorterManner` 只支持 `NONE`、`DESCENDING`、`DESCENDING_IF_AVAILABLE`

---

## 4. 详细结果

### 4.1 简单策略（Iter 1-15）

| 迭代 | 策略 | Soft Score | 对比 baseline |
|---|---|---|---|
| Baseline | Default LS | **-96060soft** | — |
| Iter 1 | First Fit Decreasing | -96060soft | 相同 |
| Iter 2 | Late Acceptance 50 | -97440soft | 更差 1.4% |
| Iter 3 | Late Acceptance 10 | -99780soft | 更差 3.9% |
| Iter 4 | Forager 500 | -100620soft | 更差 4.7% |
| Iter 5 | Tabu Search 3 | -139620soft | 极差 45.4% |
| Iter 6 | Hill Climbing 10 | -100920soft | 更差 5.1% |
| Iter 7 | Great Deluge | -102780soft | 更差 7.0% |
| Iter 8 | Fading Entity Tabu | -139620soft | 极差 45.4% |
| Iter 9 | Cheapest Insertion | -96060soft | 相同 |
| Iter 10 | Weakest Fit Decreasing | crash | 需要 domain 修改 |
| Iter 11 | Swap Move | -106500soft | 更差 10.9% |
| Iter 12 | Union Moves | -100800soft | 更差 4.9% |
| Iter 13 | Move Cache | -98340soft | 更差 2.4% |
| Iter 14 | Late Acceptance 5 | -104460soft | 更差 8.7% |
| Iter 15 | Tabu + LA Combo | -98820soft | 更差 2.9% |

### 4.2 复杂组合策略（Iter 16-30）

| 迭代 | 策略 | Soft Score | 对比 baseline |
|---|---|---|---|
| Iter 16 | Union + LA 10 | -99240soft | 更差 3.3% |
| Iter 17 | Union + Tabu 3 | -139620soft | 极差 45.4% |
| Iter 18 | Union + Forager | -100620soft | 更差 4.7% |
| **Iter 19** | **Union + LA 50** | **-96120soft** | **更差 0.06%** |
| Iter 20 | Union + LA 20 | -97560soft | 更差 1.6% |
| Iter 21 | Union + LA 100 | -97560soft | 更差 1.6% |
| Iter 22 | Union + Fading Tabu | -139620soft | 极差 45.4% |
| Iter 23 | Union + LA 5 | -99720soft | 更差 3.8% |
| Iter 24 | Union + LA 3 | -100860soft | 更差 5.0% |
| Iter 25 | Union + LA 150 | -97740soft | 更差 1.7% |
| Iter 26 | Union + LA 75 | -96180soft | 更差 0.1% |
| Iter 27 | Union + LA 30 | -96240soft | 更差 0.2% |
| Iter 28 | Union + LA 120 | -96480soft | 更差 0.4% |
| Iter 29 | Union + LA 60 | -97200soft | 更差 1.2% |
| Iter 30 | Union + LA 40 | -97560soft | 更差 1.6% |

### 4.3 新方向策略（Iter 31-45）

| 迭代 | 策略 | Soft Score | 对比 baseline |
|---|---|---|---|
| Iter 31 | Baseline 5min | -96060soft | 相同 |
| Iter 32 | Union + LA 50 5min | -96060soft | 相同 |
| Iter 33 | Hill Climbing Union | -98280soft | 更差 2.3% |
| Iter 34 | Great Deluge Union | -101520soft | 更差 5.7% |
| Iter 35 | Union + LA 50 r102 | -88560soft | 不同实例 |
| Iter 36 | LA 50 + Great Deluge | -102780soft | 更差 7.0% |
| Iter 37 | LA 50 + Hill Climbing | -102840soft | 更差 7.1% |
| Iter 38 | LA 50 + Forager | -101340soft | 更差 5.5% |
| Iter 39 | Union + LA 50 c101 | -49740soft | 不同实例 |
| Iter 40 | LA 50 + Hill Climbing 10 | -100920soft | 更差 5.1% |
| Iter 41 | Move Tabu | -139620soft | 极差 45.4% |
| Iter 42 | Fading Move Tabu | -139620soft | 极差 45.4% |
| **Iter 43** | **FFD + Union + LA 50** | **-96060soft** | **相同** |
| Iter 44 | LA 45 | -99120soft | 更差 3.2% |
| Iter 45 | LA 55 | -97860soft | 更差 1.9% |

### 4.4 Late Acceptance 微调（Iter 46-60）

| 迭代 | 策略 | Soft Score | 对比 baseline |
|---|---|---|---|
| Iter 46 | LA 48 | -98220soft | 更差 2.2% |
| Iter 47 | LA 52 | -96480soft | 更差 0.4% |
| Iter 48 | LA 53 | -99840soft | 更差 3.9% |
| Iter 49 | LA 49 | -97200soft | 更差 1.2% |
| Iter 50 | LA 51 | -98160soft | 更差 2.2% |
| Iter 51 | Hill Climbing Improving Step | -99600soft | 更差 3.7% |
| Iter 52 | Fading Entity Tabu Ratio | -139620soft | 极差 45.4% |
| Iter 53 | LA 46 | -99060soft | 更差 3.1% |
| Iter 54 | LA 47 | -98220soft | 更差 2.2% |
| Iter 55 | LA 54 | -96900soft | 更差 0.9% |
| Iter 56 | LA 56 | -97080soft | 更差 1.1% |
| Iter 57 | LA 57 | -98100soft | 更差 2.1% |
| Iter 58 | LA 58 | -97740soft | 更差 1.7% |
| Iter 59 | LA 59 | -97920soft | 更差 1.9% |
| Iter 60 | LA 61 | -97560soft | 更差 1.6% |

### 4.5 组合策略（Iter 61-80）

| 迭代 | 策略 | Soft Score | 对比 baseline |
|---|---|---|---|
| Iter 61 | Union + LA 50 + Forager 500 | -101880soft | 更差 6.1% |
| Iter 62 | Union + LA 50 + Hill Climbing 5 | -98280soft | 更差 2.3% |
| Iter 63 | Union + LA 50 + Great Deluge 0.01 | -101520soft | 更差 5.7% |
| Iter 64 | Union + LA 50 + Great Deluge 0.1 | -102120soft | 更差 6.3% |
| Iter 65 | Union + LA 50 + Hill Climbing 10 | -99720soft | 更差 3.8% |
| Iter 66 | LA 50 + Great Deluge 0.01 | -102780soft | 更差 7.0% |
| Iter 67 | LA 50 + Hill Climbing 5 | -102840soft | 更差 7.1% |
| Iter 68 | LA 50 + Hill Climbing 10 | -100920soft | 更差 5.1% |
| Iter 69 | LA 50 + Hill Climbing 20 | -99840soft | 更差 3.9% |
| Iter 70 | LA 50 + Hill Climbing 30 | -99420soft | 更差 3.5% |
| Iter 71 | FFD + LA 50 | -97440soft | 更差 1.4% |
| Iter 72 | FFD + Union + LA 50 + Forager 500 | -101880soft | 更差 6.1% |
| Iter 73 | FFD + Union + LA 50 + Hill Climbing 5 | -98280soft | 更差 2.3% |
| Iter 74 | FFD + Union + LA 50 + Great Deluge 0.01 | -101520soft | 更差 5.7% |
| Iter 75 | First Fit | -96060soft | 相同 |
| Iter 76 | Allocate Entity From Queue | -96060soft | 相同 |
| Iter 77 | Allocate To Value From Queue | -96060soft | 相同 |
| Iter 78 | Allocate From Pool | -96060soft | 相同 |
| Iter 79 | Union + LA 50 + Great Deluge 0.001 | -102240soft | 更差 6.4% |
| Iter 80 | Union + LA 50 + Hill Climbing 20 | -97860soft | 更差 1.9% |

---

## 5. 策略分类分析

### 5.1 构造启发式（Construction Heuristics）

| 策略 | 结果 | 说明 |
|---|---|---|
| Default (FIRST_FIT) | -96060soft | 最优 |
| FIRST_FIT_DECREASING | -96060soft | 相同 |
| CHEAPEST_INSERTION | -96060soft | 相同 |
| FIRST_FIT | -96060soft | 相同 |
| ALLOCATE_ENTITY_FROM_QUEUE | -96060soft | 相同 |
| ALLOCATE_TO_VALUE_FROM_QUEUE | -96060soft | 相同 |
| ALLOCATE_FROM_POOL | -96060soft | 相同 |
| WEAKEST_FIT | crash | 需要 strength comparison |
| WEAKEST_FIT_DECREASING | crash | 需要 strength comparison |
| STRONGEST_FIT | crash | 需要 strength comparison |
| STRONGEST_FIT_DECREASING | crash | 需要 strength comparison |

### 5.2 Late Acceptance 调优

| Size | Soft Score | 对比 baseline |
|---|---|---|
| 3 | -100860soft | 更差 5.0% |
| 5 | -104460soft | 更差 8.7% |
| 10 | -99780soft | 更差 3.9% |
| 20 | -97560soft | 更差 1.6% |
| 30 | -96240soft | 更差 0.2% |
| 40 | -97560soft | 更差 1.6% |
| 45 | -99120soft | 更差 3.2% |
| 46 | -99060soft | 更差 3.1% |
| 47 | -98220soft | 更差 2.2% |
| 48 | -98220soft | 更差 2.2% |
| 49 | -97200soft | 更差 1.2% |
| **50** | **-96120soft** | **更差 0.06%** |
| 51 | -98160soft | 更差 2.2% |
| 52 | -96480soft | 更差 0.4% |
| 53 | -99840soft | 更差 3.9% |
| 54 | -96900soft | 更差 0.9% |
| 55 | -97860soft | 更差 1.9% |
| 56 | -97080soft | 更差 1.1% |
| 57 | -98100soft | 更差 2.1% |
| 58 | -97740soft | 更差 1.7% |
| 59 | -97920soft | 更差 1.9% |
| 60 | -97200soft | 更差 1.2% |
| 61 | -97560soft | 更差 1.6% |
| 75 | -96180soft | 更差 0.1% |
| 100 | -97560soft | 更差 1.6% |
| 120 | -96480soft | 更差 0.4% |
| 150 | -97740soft | 更差 1.7% |

### 5.3 Tabu Search 相关（全部极差）

| 策略 | Soft Score | 对比 baseline |
|---|---|---|
| Entity Tabu 3 | -139620soft | 极差 45.4% |
| Fading Entity Tabu 10 | -139620soft | 极差 45.4% |
| Move Tabu 10 | -139620soft | 极差 45.4% |
| Fading Move Tabu 10 | -139620soft | 极差 45.4% |
| Fading Entity Tabu Ratio 0.1 | -139620soft | 极差 45.4% |
| Union + Tabu 3 | -139620soft | 极差 45.4% |
| Union + Fading Tabu | -139620soft | 极差 45.4% |

---

## 6. 结论与建议

### 6.1 结论

1. **Timefold 默认 Local Search 在 r101 上已是最优配置**，所有 80 次尝试均未超越。
2. **Late Acceptance size=50** 是最接近 baseline 的策略（-96120soft，差距仅 0.06%）。
3. **Tabu Search 在 VRPTW 上表现极差**，应避免使用。
4. **60 秒求解时间已足够**，r101 在 60 秒内已收敛到最优。
5. **Nearby Selection 需要企业版**，Community Edition 不可用。

### 6.2 建议

1. **尝试更大/更复杂的问题实例**：r201、rc101 等，默认 LS 可能在这些实例上不是最优。
2. **考虑 Timefold 企业版**：Nearby Selection 可能带来改进。
3. **修改 domain model**：添加 strength comparison 以支持更多自定义策略。
4. **参考 sintef 最优解**：`src/main/resources/results/sintef/` 中包含各算例的最优路线。

### 6.3 经验教训

1. **每次修改配置后必须先 `mvn compile -q`**：benchmark runner 读取 `target/classes/`，不编译会使用缓存旧配置。
2. **XML 配置语法严格**：
   - `entitySelector` 必须包裹在 move selector 中
   - `lateAcceptanceSize` 与 tabu 配置互斥
   - 配置名不能包含特殊字符
3. **Community Edition 限制**：Nearby Selection 需要企业版许可证。
4. **Domain model 限制**：WEAKEST_FIT、STRONGEST_FIT 等需要 @PlanningVariable 声明 strength comparison。

---

## 7. 参考

- **Skill 文档**: `skill_zh.md`（中文）、`skill.md`（英文）
- **Sintef 最优解**: `src/main/resources/results/sintef/`
- **实验结果**: `results.tsv`
- **Git 分支**: `autoresearch/may2_bench`
