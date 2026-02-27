# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build (skip tests)
mvn clean package -DskipTests

# Run the application
mvn spring-boot:run

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=YourTestClassName

# Build and run the JAR directly
java -jar target/strategy-engine-management-1.0.0.jar
```

**Prerequisites**: MySQL running at `localhost:3306`, database `strategy_engine` initialized via `src/main/resources/sql/schema.sql`. Default credentials: root/root.

**Swagger UI**: `http://localhost:8080/api/swagger-ui.html`

---

## Architecture Overview

This is a **configuration management backend** for a knowledge-point scoring engine used in education. The system stores rules and weights; the actual scoring is done by the caller, not this service.

### Data flow (caller's perspective)
1. Caller fetches enabled tag rules: `GET /api/tag/list/{engineId}/enabled` (returns `ruleConfig` JSON trees)
2. Caller fetches scene weight configs: `GET /api/scene/list/{engineId}`
3. For each knowledge point, caller invokes `RuleMatchEngine.match(ruleConfigJson, dataMap)` to determine which tags apply
4. Score = Σ (matched tag's `weightCoefficient` in that scene)

### Three-layer hierarchy
```
StrategyEngine (引擎)
  └── StrategyTagRule (标签规则) — has ruleConfig JSON (condition tree)
  └── StrategyScene (场景)
        └── StrategySceneTag (scene↔tag join, stores weightCoefficient 1-10)
```
`StrategyTagField` is an independent metadata table that provides the field library for the frontend rule editor — it has no FK relationships to other tables.

### Package structure
```
com.strategy.engine
├── controller/    REST endpoints
├── service/       Business logic interfaces + impls
├── mapper/        MyBatis-Plus mappers (2 have custom SQL: StrategySceneTagMapper, StrategyTagFieldMapper)
├── entity/        5 entities matching DB tables
├── dto/           Request objects (with validation annotations)
├── vo/            Response objects
├── rule/          RuleMatchEngine + RuleNode — condition tree evaluator (standalone, no Spring dependencies)
├── enums/         EngineType, ApplicableObject, StatusEnum
└── exception/     BusinessException + GlobalExceptionHandler
```

### Important design decisions
- **`StrategyScene` has no `status` field** — scenes are either present or deleted, no enable/disable
- **`StrategySceneTag` has no `enabled` field** — having the association means it is active
- **`StrategySceneTag` uses physical delete** (no `deleted` column); all other main entities use logical delete via `deleted` field
- **`StrategyTagField.operators` and `applicableObjects`** are MySQL JSON columns mapped to `List<String>` via `@TableName(autoResultMap=true)` + `JacksonTypeHandler`
- **`tag_count` / `scene_count`** on `StrategyEngine` are denormalized counters, recalculated on every tag/scene add/delete
- **`StrategySceneTagMapper.insertBatch`** is a custom `@Insert` with `<foreach>` — not a MyBatis-Plus built-in
- **`EngineFullConfigController`** (`/api/engine-config`) is implemented but not the primary flow; it's a backup for batch save of all engine config in one request

### `RuleMatchEngine` condition tree format
```json
{
  "type": "group",
  "operator": "AND",
  "children": [
    { "type": "condition", "field": "exam_mastery", "operator": ">", "value": "60" },
    { "type": "condition", "field": "difficulty_level", "operator": "IN", "value": "HIGH,VERY_HIGH" }
  ]
}
```
Supported operators: `=`/`EQ`, `!=`/`NEQ`, `>`/`GT`, `>=`/`GTE`, `<`/`LT`, `<=`/`LTE`, `CONTAINS`, `NOT_CONTAINS`, `IN`, `NOT_IN` (IN/NOT_IN values are comma-separated strings).

### `StrategyTagField` filtering
`StrategyTagFieldMapper.listByApplicableObject` uses `JSON_CONTAINS` to return fields where `applicable_objects` contains `"ALL"` or the engine's specific `applicableObject` value. Results are grouped into INHERENT → EXAM → COMPREHENSIVE by the service layer.

### Tag deletion/disable protection
Before deleting or disabling a `StrategyTagRule`, the service checks `strategy_scene_tag WHERE tag_id = {id}`. If count > 0, it throws `BusinessException`.

### Default engine uniqueness
`setDefault` runs in a single transaction: first clears all `is_default = 0`, then sets the target to `is_default = 1`.

---

## Design Intent (from author)

### Why scenes have no `status` field
场景（Scene）不需要启用/禁用状态。场景要么存在、要么删除，不存在"暂时禁用某个场景"的需求。如果某个场景不再需要，直接删除即可。

### Why `StrategySceneTag` has no `enabled` field
场景与标签建立关联本身就代表该标签在此场景中生效，不需要额外的开关。"产生关联了就生效"，多余的 `enabled` 字段只会增加维护复杂度。

### Table naming convention
所有表名统一使用 `strategy_` 前缀（`strategy_engine`、`strategy_tag_rule`、`strategy_scene`、`strategy_scene_tag`、`strategy_tag_field`），一眼就能识别出属于策略引擎模块的表。Java 类名同样统一使用 `Strategy` 前缀（`StrategyEngine` 保持不变，其余均以 `Strategy` 开头）。

### `StrategyTagField` purpose
条件字段元数据表（`strategy_tag_field`）是为了让前端规则编辑器的"字段库"可以由后台动态管理，而不是硬编码在前端。字段按适用对象（`applicable_objects` JSON 列）过滤，`["ALL"]` 表示所有引擎通用，其余值对应具体适用对象（STUDENT/CLASS 等）。字段分三类：INHERENT（知识点固有属性）、EXAM（单次考试维度）、COMPREHENSIVE（跨多次考试的综合维度）。

### `EngineFullConfigController` status
`/api/engine-config` 已完整实现（支持一次性提交引擎基本信息 + 标签规则 + 场景配置），作为备用方案预留。当前主流程是分步操作（分别调用 engine/tag/scene 各自的接口）。若前端改为单页全量提交模式，可启用此 Controller 替代分步接口。
