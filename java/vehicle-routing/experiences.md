# Auto Research 经验记录

持续跟踪优化经验。此文件与 results.tsv 一样需要持久化。

## 已知无效策略

| 策略 | 分数 | 原因 |
|---|---|---|
| Tabu Search（所有变体） | -139620soft | 极差，比 baseline 差 45.4% |
| Simulated Annealing | 未测试 | 需要起始温度，且历史实验未成功 |
| Great Deluge | -102780soft | 比 baseline 差 7.0% |
| Nearby Selection | 不可用 | 需要企业版许可证 |
| WEAKEST_FIT / STRONGEST_FIT | crash | 需要 domain model 修改 |

## 已知有效策略

| 策略 | 分数 | 说明 |
|---|---|---|
| Default Local Search | -96060soft | 最优，所有尝试均未超越 |
| LA size=50 | -96120soft | 最接近 baseline（差距 0.06%） |
| LA size=75 | -96180soft | 次优（差距 0.1%） |
| FFD + Union + LA 50 | -96060soft | 与 baseline 相同 |
| First Fit / Allocate From Queue / Allocate From Pool | -96060soft | 与 baseline 相同 |

## 关键经验

1. **每次修改配置后必须先 `mvn compile -q`**：benchmark runner 读取 `target/classes/`，不编译会使用缓存旧配置。
2. **配置读取顺序**：`inheritedSolverBenchmark` 中的配置会被 `solverBenchmark` 中的配置覆盖/追加。相同类型的 phase 不能重复定义。
3. **60 秒已收敛**：r101 在 60 秒和 300 秒结果完全相同，说明 r101 在 60 秒内已收敛到最优。
4. **子智能体隔离**：使用 `git worktree` 为每个子智能体创建独立工作目录，避免 git 冲突。
5. **XML 元素顺序**：`unionMoveSelector` → `acceptor` → `forager`，顺序错误会导致 XML 验证失败。
6. **配置名不能包含特殊字符**：benchmark name 只能包含 `[\w\d _\-\.()]`。

## 新经验（持续追加）

<!-- 在此追加新的经验发现 -->
