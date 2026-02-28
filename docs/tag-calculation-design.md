# 标签计算系统设计文档

---

## 一、项目概述

### 1.1 背景

考试结束后：

1. 计算每个学生在某考试下的知识点（层级）属性（掌握度等）
2. 存入 MongoDB
3. 根据策略引擎中配置的标签规则进行批量计算
4. 命中的知识点打标签
5. 场景为标签配置权重，用于排序

### 1.2 设计目标

构建一个：

- 支持 DSL 规则定义
- 基于 DuckDB 批量计算
- 可重复执行
- 可扩展
- 架构简单

的标签计算系统。

---

## 二、总体架构设计

### 2.1 架构原则

1. 计算与存储分离
2. Mongo 不做复杂计算
3. DuckDB 专门负责规则批量计算
4. 单场考试一次性计算
5. 不引入分布式

### 2.2 总体架构图

```
MongoDB（知识点属性数据）
        ↓
考试结束触发
        ↓
数据抽取（exam_id 维度）
        ↓
导入 DuckDB 临时表
        ↓
DSL → SQL
        ↓
批量标签计算
        ↓
结果回写 Mongo
        ↓
场景权重排序
```

---

## 三、技术选型

**计算引擎：DuckDB**

原因：
- 嵌入式，无需独立部署
- 单机高性能，支持复杂 SQL
- 适合单场批量计算

**数据源：MongoDB**

---

## 四、数据模型设计

### 4.1 Mongo 原始知识点集合

```json
{
  "student_id": 1001,
  "exam_id": 2001,
  "knowledge_point_id": 301,
  "level": 2,
  "mastery": 0.82,
  "correct_rate": 0.75,
  "score": 8.5
}
```

索引：
- `{ exam_id: 1 }`
- `{ student_id: 1, exam_id: 1 }`

### 4.2 DuckDB 临时事实表 `student_knowledge_fact`

```sql
CREATE TABLE student_knowledge_fact (
    student_id          BIGINT,
    exam_id             BIGINT,
    knowledge_point_id  BIGINT,
    level               INTEGER,
    mastery             DOUBLE,
    correct_rate        DOUBLE,
    score               DOUBLE
);
```

特点：
- 每次按 `exam_id` 全量导入
- 不做历史存储
- 计算完成后可清空

### 4.3 标签表（Mongo）

```json
{
  "tag_id": 1,
  "engine_id": 10,
  "name": "高掌握度",
  "score_weight": 10,
  "rule_config": {
    "type": "group",
    "operator": "AND",
    "children": [
      { "type": "condition", "field": "mastery", "operator": ">", "value": "0.8" },
      { "type": "condition", "field": "level", "operator": "=", "value": "2" }
    ]
  },
  "rule_sql": "mastery > 0.8 AND level = 2"
}
```

> `rule_config` 为条件树 DSL JSON，供前端规则编辑器回显使用；`rule_sql` 为后端在保存时自动转换的 SQL WHERE 片段，供 DuckDB 计算服务直接使用。

### 4.4 标签结果表（Mongo）

```json
{
  "student_id": 1001,
  "exam_id": 2001,
  "knowledge_point_id": 301,
  "tag_id": 1
}
```

---

## 五、DSL 设计

### 5.1 设计原则

1. DSL 必须可映射为 SQL WHERE 条件
2. 支持任意层级嵌套的 AND / OR 逻辑组合
3. 不支持子查询
4. 不支持嵌套聚合
5. 字段名仅允许字母、数字、下划线，防止注入

### 5.2 节点类型

DSL 由两种节点递归组合：

| 节点类型 | type 值 | 说明 |
|---------|---------|------|
| 条件组 | `group` | 包含 `operator`（AND/OR）和 `children` 子节点列表 |
| 叶子条件 | `condition` | 包含 `field`、`operator`、`value` |

### 5.3 支持的运算符

| 运算符 | 别名 | 说明 | 适用数据类型 |
|--------|------|------|-------------|
| `=` | `EQ` | 等于 | NUMBER / STRING / ENUM |
| `!=` | `NEQ` | 不等于 | NUMBER / STRING / ENUM |
| `>` | `GT` | 大于 | NUMBER |
| `>=` | `GTE` | 大于等于 | NUMBER |
| `<` | `LT` | 小于 | NUMBER |
| `<=` | `LTE` | 小于等于 | NUMBER |
| `CONTAINS` | - | 包含（模糊匹配） | STRING |
| `NOT_CONTAINS` | - | 不包含 | STRING |
| `IN` | - | 在集合内，value 为逗号分隔的多值字符串 | ENUM / STRING |
| `NOT_IN` | - | 不在集合内，value 为逗号分隔的多值字符串 | ENUM / STRING |

### 5.4 DSL 结构定义

```json
{
  "type": "group",
  "operator": "AND | OR",
  "children": [
    {
      "type": "condition",
      "field": "字段名",
      "operator": "运算符",
      "value": "值（IN/NOT_IN 时为逗号分隔的多值字符串）"
    }
  ]
}
```

字段说明：

| 字段 | 类型 | 说明 |
|------|------|------|
| `type` | String | `group` 或 `condition` |
| `operator` | String | group 时为 `AND`/`OR`；condition 时为比较运算符 |
| `children` | Array | group 专属，子节点列表，可嵌套 group |
| `field` | String | condition 专属，对应 DuckDB 事实表列名 |
| `value` | String | condition 专属，IN/NOT_IN 时用逗号分隔多个值 |

### 5.5 value 字段设计说明

`value` 统一使用 `String` 类型而非 `Object` 或 `List`，原因如下：

1. **结构统一**：所有运算符的条件节点结构完全一致，序列化/反序列化无需针对不同运算符做特殊处理
2. **业务约束明确**：当前系统的字段枚举值（学段、难度等级等）由字段元数据表统一管理，枚举值本身不包含逗号，`IN`/`NOT_IN` 使用逗号分隔多值是安全的
3. **前后端实现简单**：前端多选结果 `join(",")` 即可，后端 `split(",")` 还原，无需引入额外的类型判断逻辑
4. **与 SQL 映射自然**：`"2,3,4"` → `IN (2, 3, 4)`，转换逻辑直接，无歧义

> **约束**：`IN`/`NOT_IN` 的各值本身不得包含逗号。这是在当前业务边界内有意识的简化。若未来出现枚举值含逗号的场景，可将 `value` 升级为 `Object` 类型支持数组，仅需改动 `RuleNode`、`RuleMatchEngine`、`RuleToSqlTranslator` 三个文件。

### 5.6 DSL 示例

**示例 1：单层 AND 条件**

```json
{
  "type": "group",
  "operator": "AND",
  "children": [
    { "type": "condition", "field": "mastery", "operator": ">", "value": "0.8" },
    { "type": "condition", "field": "level", "operator": "=", "value": "2" }
  ]
}
```

**示例 2：嵌套 AND + OR**

```json
{
  "type": "group",
  "operator": "AND",
  "children": [
    { "type": "condition", "field": "mastery", "operator": ">", "value": "0.6" },
    {
      "type": "group",
      "operator": "OR",
      "children": [
        { "type": "condition", "field": "level", "operator": "=", "value": "2" },
        { "type": "condition", "field": "level", "operator": "=", "value": "3" }
      ]
    }
  ]
}
```

**示例 3：IN 多值条件**

```json
{
  "type": "group",
  "operator": "AND",
  "children": [
    { "type": "condition", "field": "mastery", "operator": ">", "value": "0.8" },
    { "type": "condition", "field": "level", "operator": "IN", "value": "2,3,4" }
  ]
}
```

---

## 六、DSL → SQL 生成规则

### 6.1 转换规则

| DSL | SQL |
|-----|-----|
| `type: group, operator: AND` | 子条件用 `AND` 连接 |
| `type: group, operator: OR` | 子条件用 `OR` 连接 |
| 嵌套 group（子节点 > 1 个） | 整体加括号保证优先级 |
| `op: =` / `EQ` | `field = value` |
| `op: !=` / `NEQ` | `field != value` |
| `op: >` / `GT` | `field > value` |
| `op: >=` / `GTE` | `field >= value` |
| `op: <` / `LT` | `field < value` |
| `op: <=` / `LTE` | `field <= value` |
| `op: CONTAINS` | `field LIKE '%value%'` |
| `op: NOT_CONTAINS` | `field NOT LIKE '%value%'` |
| `op: IN` | `field IN ('v1', 'v2', ...)` |
| `op: NOT_IN` | `field NOT IN ('v1', 'v2', ...)` |
| 纯数字 value | 不加引号，如 `mastery > 0.8` |
| 非数字 value | 加单引号并转义，如 `level = '初中'` |

### 6.2 生成 SQL 示例

标签规则 DSL：

```json
{
  "type": "group",
  "operator": "AND",
  "children": [
    { "type": "condition", "field": "mastery", "operator": ">", "value": "0.8" },
    { "type": "condition", "field": "level", "operator": "=", "value": "2" }
  ]
}
```

生成 WHERE 片段（存入 `rule_sql` 列）：

```sql
mastery > 0.8 AND level = 2
```

完整批量计算 SQL（多标签合并，避免多次全表扫描）：

```sql
SELECT student_id,
       exam_id,
       knowledge_point_id,
       CASE WHEN mastery > 0.8 AND level = 2        THEN 1 ELSE NULL END AS tag1,
       CASE WHEN score < 5.0                         THEN 2 ELSE NULL END AS tag2,
       CASE WHEN mastery > 0.6 AND level IN (2,3,4)  THEN 3 ELSE NULL END AS tag3
FROM student_knowledge_fact
WHERE exam_id = ?;
```

### 6.3 rule_sql 的存储与使用

- 标签规则保存时，后端自动将 `rule_config`（DSL JSON）转换为 `rule_sql`（SQL WHERE 片段）并存入数据库
- 调用方（计算服务）读取 `rule_sql` 直接拼入 DuckDB `CASE WHEN` 语句，无需运行时转换
- `rule_config` 保留原始 DSL，用于前端规则编辑器回显

---

## 七、完整执行流程

### 7.1 阶段 1：考试结束

1. Mongo 存知识点属性
2. 触发策略引擎计算

### 7.2 阶段 2：导入 DuckDB

1. 查询 Mongo：`exam_id = ?`
2. 将结果批量写入 DuckDB，可采用：
   - JDBC 批量 insert
   - 或导出 CSV 再 `COPY`

### 7.3 阶段 3：批量标签计算

> 不要一个标签跑一条 SQL，应合并为单次全表扫描：

```sql
SELECT student_id,
       exam_id,
       knowledge_point_id,
       CASE WHEN mastery > 0.8 THEN 1 ELSE NULL END AS tag1,
       CASE WHEN score < 5     THEN 2 ELSE NULL END AS tag2
FROM student_knowledge_fact;
```

然后展开非空 tag，避免多次扫描。

### 7.4 阶段 4：写回 Mongo

生成 `student_knowledge_tag` 集合，批量写入。

### 7.5 阶段 5：场景排序

场景配置：`tag_id → weight`

```sql
SELECT student_id,
       knowledge_point_id,
       SUM(weight) AS scene_score
FROM student_knowledge_tag
GROUP BY student_id, knowledge_point_id
ORDER BY scene_score DESC;
```

---

## 八、性能设计

### 8.1 计算粒度

- 单场考试一次计算
- 不跨考试

### 8.2 内存要求

DuckDB 需要单场考试数据 < 可用内存，100 万行级别完全没问题。

### 8.3 并发控制

- 同一 `exam_id` 不重复执行
- 使用分布式锁（Redis 可选）

---

## 九、异常处理

1. DSL 校验失败 → 禁止执行
2. 导入失败 → 回滚
3. 计算失败 → 支持重试
4. 结果写入失败 → 记录错误日志

---

## 十、容量评估

假设：
- 5000 学生
- 100 知识点
- 单场 50 万记录

DuckDB 单机场景完全可支持。

---

## 十一、扩展路径

未来如果：
- 数据上千万
- 需要跨考试分析
- 需要报表

再升级：
- ClickHouse
- 或数据仓库

当前阶段不需要。

---

## 十二、总结

本方案：

- Mongo 存储事实
- DuckDB 批量规则计算
- DSL 仅用于生成 WHERE 条件
- 标签单独存储
- 场景负责加权排序

结构清晰，复杂度可控，性能足够。
