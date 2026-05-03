# Auto Research Plan — Agent-Driven

## Baseline
- 当前配置：Construction Heuristic only，empty Local Search
- 基线分数：**-100920soft**

## Phase 3: Parallel Exploration

### Group A: Local Search 策略（互斥，串行测试）
| # | 策略 | 预期效果 | 配置 |
|---|---|---|---|
| A1 | Late Acceptance + historySize=200 | 避免局部最优 | `<localSearchType>LATE_ACCEPTANCE</localSearchType>` + `<acceptor><lateAcceptanceSize>200</lateAcceptanceSize></acceptor>` |
| A2 | Late Acceptance + historySize=500 | 更长的记忆窗口 | historySize=500 |
| A3 | Simulated Annealing | 早期接受差移动 | `<acceptorType>SIMULATED_ANNEALING</acceptorType>` + `0hard/0medium/500soft` |
| A4 | Tabu Search | 避免循环 | `<acceptorType>TABU_SEARCH</acceptorType>` + `tabuSize=50` |

### Group B: Move Selector（可与 Group A 组合）
| # | 策略 | 预期效果 | 配置 |
|---|---|---|---|
| B1 | Swap Move | 交换两个 Visit | `<swapMoveSelector/>` |
| B2 | SubList Change | 移动 Visit 子序列 | `<subListChangeMoveSelector><subListSizeRatio>0.1</subListSizeRatio></subListChangeMoveSelector>` |
| B3 | Union(Change + Swap) | 同时探索两种 move | `<unionMoveSelector><changeMoveSelector/><swapMoveSelector/></unionMoveSelector>` |
| B4 | Union(Change + Swap + SubList) | 全面探索 | 三种 selector 组合 |

### Group C: Nearby Selection（减少搜索空间）
| # | 策略 | 预期效果 |
|---|---|---|
| C1 | Nearby 10 visits | 只交换空间上接近的 Visit |
| C2 | Nearby 20 visits | 更大的邻近范围 |

### Group D: Custom Components
| # | 策略 | 预期效果 |
|---|---|---|
| D1 | VisitWaitingTimeComparator in LS | 优先优化等待时间长的 Visit |
| D2 | Distance-based Filter | 过滤远距离无效 Move |

## Phase 4: Combination
- 取 Group A 最佳 + Group B 最佳 → 组合测试
- 取 A+B 最佳 + Group C → 组合测试

## Phase 5: Fine-tuning
- 微调最优策略的参数

## 执行顺序
1. A1-A4 串行（互斥策略）
2. B1-B4 并行（使用最佳 LA 配置）
3. C1-C2 并行（使用 A+B 最佳）
4. D1-D2 串行
5. 组合 + 微调

## 预计迭代数
- Phase 3: ~12 次
- Phase 4: ~4 次
- Phase 5: ~5 次
- 总计: ~21 次（远少于原版 50 次）
