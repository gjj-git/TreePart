# CLAUDE.md

本文件为 Claude Code 提供项目引导信息，每次对话开始时自动读取。

## 沟通语言规范

- 与用户的所有沟通、说明、分析输出默认使用**中文**
- 代码中的变量名、类名、方法名保持**英文**（符合 Java 命名规范）
- CLAUDE.md 及项目文档使用**中文**

---

## 文档维护规则

- **CLAUDE.md（本文件）**：每次代码改动编译通过后自动更新，保持与代码实际状态同步，确保新对话的上下文准确。
- **docs/strategy-engine-design.md**：仅在用户明确要求时更新（如"更新设计文档"）。这是面向人阅读的正式设计文档，只反映用户已验收的变更。

---

## 项目目录

- **后端项目**：`E:\claudeProjects\ThreePart`（当前目录，Spring Boot，已废弃，策略引擎已迁移至 qbank）
- **前端项目（当前主力）**：`E:\Projects\Frontend\kgraph-frontend`（Vue 3 + TypeScript，对接 qbank 服务）
- **前端项目（旧）**：`E:\claudeProjects\ThreePart-Web`（已不再主动开发，可作为参考）
- **后端项目（当前主力）**：`C:\Users\Administrator\.ssh\Qbank\qbank-app`（Spring Boot，本地端口 8291）

> **重要**：所有前端修改必须在 `E:\Projects\Frontend\kgraph-frontend` 进行，不要修改 `E:\claudeProjects\ThreePart-Web` 或 `F:\ThreePart-Web`。

---

## 构建与运行命令

```bash
# 构建（跳过测试）
mvn clean package -DskipTests

# 运行应用
mvn spring-boot:run

# 运行所有测试
mvn test

# 运行指定测试类
mvn test -Dtest=YourTestClassName

# 直接运行 JAR
java -jar target/strategy-engine-management-1.0.0.jar
```

**前置条件**：MySQL 运行在 `localhost:3306`，数据库 `strategy_engine` 通过 `src/main/resources/sql/schema.sql` 初始化。默认账号：root/root。

**Swagger UI**：`http://localhost:8080/api/swagger-ui.html`

---

## 架构概览

本项目是教育场景知识点优先级计算引擎的**配置管理后端**。系统只负责存储规则和权重，实际的打标签与计分由调用方完成。

### 调用方使用流程

1. 获取启用的标签规则：`GET /api/tag/list/{engineId}/enabled`（返回含 `ruleConfig` JSON 的规则列表）
2. 获取场景权重配置：`GET /api/scene/list/{engineId}`
3. 对每个知识点调用 `RuleMatchEngine.match(ruleConfigJson, dataMap)` 判断命中哪些标签
4. 得分 = Σ（命中标签在该场景下的权重系数）

### 三层数据结构

```
StrategyEngine（策略引擎）
  └── StrategyTagRule（标签规则）— 包含条件树 ruleConfig JSON
  └── StrategyScene（场景策略）
        └── StrategySceneTag（场景↔标签关联，存储 weightCoefficient 1-10）
```

`StrategyTagField` 是独立的字段元数据表，为前端规则编辑器提供字段库，与其他表无外键关联。

### 包结构

```
com.strategy.engine
├── controller/    REST 接口
├── service/       业务逻辑接口 + 实现
├── mapper/        MyBatis-Plus Mapper（2个有自定义SQL：StrategySceneTagMapper、StrategyTagFieldMapper）
├── entity/        5个实体类，对应数据库表
├── dto/           请求对象（含校验注解）
├── vo/            响应对象
├── rule/          RuleMatchEngine + RuleNode + RuleToSqlTranslator — 条件树求值器与 SQL 转换器（独立模块，无 Spring 依赖）
├── enums/         EngineType、ApplicableObject、StatusEnum
└── exception/     BusinessException + GlobalExceptionHandler
```

### 重要设计决策

- **`StrategyScene` 无 `status` 字段** — 场景要么存在要么删除，不需要启用/禁用
- **`StrategySceneTag` 无 `enabled` 字段** — 有关联即生效，不需要额外开关
- **`StrategySceneTag` 使用物理删除**（无 `deleted` 列），其他主要实体均使用 `deleted` 字段逻辑删除
- **`StrategyTagField.operators` 和 `applicableObjects`** 是 MySQL JSON 列，通过 `@TableName(autoResultMap=true)` + `JacksonTypeHandler` 映射为 `List<String>`
- **`tag_count` / `scene_count`** 是 `StrategyEngine` 上的冗余统计字段，每次标签/场景增删时重新 COUNT 写回
- **`StrategySceneTagMapper.insertBatch`** 是自定义 `@Insert` + `<foreach>`，非 MyBatis-Plus 内置
- **`EngineFullConfigController`**（`/api/engine-config`）已实现但非主流程，作为批量保存引擎完整配置的备用方案
- **`GET /api/scene/{sceneId}/available-tags`** 分页查询场景未关联的标签，支持标签名称模糊搜索，参数：`name`、`pageNum`、`pageSize`
- **`rule_sql` 自动生成**：`strategy_tag_rule` 表新增 `rule_sql`（TEXT）列，保存标签规则时后端调用 `RuleToSqlTranslator.translate(ruleConfig)` 自动将 JSON 条件树转为 SQL WHERE 片段并双写，供 Superset/BI 直接使用。`RuleMatchEngine` 继续使用 `rule_config` JSON 进行 Java 内存求值，两条路径互不影响。
- **主键生成策略**：所有实体类主键使用 `@TableId(type = IdType.ASSIGN_ID)`，采用 MyBatis-Plus 内置雪花算法生成 19 位数字 ID，INSERT 前由应用层生成，数据库 `AUTO_INCREMENT` 保留但不触发。
- **分页插件**：`MybatisPlusConfig` 注册 `PaginationInnerInterceptor(DbType.MYSQL)`，所有 `selectPage` 调用均会执行 count 查询，`total`/`pages` 正确返回。
- **引擎组（StrategyEngineGroup）**：新增引擎组概念，一个引擎组可包含多个引擎，同组内 `type + applicableObject` 组合唯一（最多 8 个）。系统维护全局唯一默认引擎组（`is_default=1`），调用方通过 `GET /qbank/strategy/engine-group/resolve-engine?type=&applicableObject=` 从默认引擎组获取对应引擎。`t_strategy_engine.is_default` 字段已删除。
- **`StrategyEngineGroupRel` 使用物理删除**（无 `is_deleted` 列），无审计字段，对照 `t_strategy_scene_tag` 设计。

### RuleMatchEngine 条件树格式

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

支持的运算符：`=`/`EQ`、`!=`/`NEQ`、`>`/`GT`、`>=`/`GTE`、`<`/`LT`、`<=`/`LTE`、`CONTAINS`、`NOT_CONTAINS`、`IN`、`NOT_IN`（IN/NOT_IN 的值为逗号分隔字符串，匹配前会 trim 空格）。

### StrategyTagField 字段过滤

`StrategyTagFieldMapper.listByApplicableObject` 使用 `JSON_CONTAINS` 返回 `applicable_objects` 包含 `"ALL"` 或当前引擎适用对象的字段，Service 层按 INHERENT → EXAM → COMPREHENSIVE 分组返回。

### 标签删除/禁用保护

删除或禁用 `StrategyTagRule` 前，Service 检查 `strategy_scene_tag WHERE tag_id = {id}`，若 count > 0 则抛出 `BusinessException`。

### 默认引擎唯一性

`setDefault` 使用单条原子 SQL 避免并发问题：

```sql
UPDATE strategy_engine SET is_default = CASE WHEN id = #{id} THEN 1 ELSE 0 END WHERE deleted = 0
```

一条语句同时完成"清除其他默认"和"设置目标默认"，无竞态窗口。

### 按适用对象获取默认引擎

`GET /api/engine/default?applicableObject=BUREAU` 查询 `WHERE is_default=1 AND applicable_object=?`。若全局默认引擎的适用对象与传入值不匹配，`data` 返回 `null`（HTTP 200），调用方自行降级处理。**策略：全局唯一默认，不做 per-applicableObject 独立默认。**

### 枚举值接口

`GET /api/engine/enums` 返回所有 `EngineType` 和 `ApplicableObject` 的 code + 中文 label，供前端下拉框使用。逻辑直接在 Controller 中实现，无需 Service 层。

---

## 设计意图（作者说明）

### 为什么场景没有 status 字段

场景（Scene）不需要启用/禁用状态。场景要么存在、要么删除，不存在"暂时禁用某个场景"的需求。如果某个场景不再需要，直接删除即可。

### 为什么 StrategySceneTag 没有 enabled 字段

场景与标签建立关联本身就代表该标签在此场景中生效，不需要额外的开关。"产生关联了就生效"，多余的 `enabled` 字段只会增加维护复杂度。

### 表命名规范

所有表名统一使用 `strategy_` 前缀（`strategy_engine`、`strategy_tag_rule`、`strategy_scene`、`strategy_scene_tag`、`strategy_tag_field`），一眼就能识别出属于策略引擎模块的表。Java 类名同样统一使用 `Strategy` 前缀。

### StrategyTagField 的用途

条件字段元数据表（`strategy_tag_field`）是为了让前端规则编辑器的"字段库"可以由后台动态管理，而不是硬编码在前端。字段按适用对象（`applicable_objects` JSON 列）过滤，`["ALL"]` 表示所有引擎通用，其余值对应具体适用对象（STUDENT/CLASS 等）。字段分三类：INHERENT（知识点固有属性）、EXAM（单次考试维度）、COMPREHENSIVE（跨多次考试的综合维度）。

### EngineFullConfigController 状态

`/api/engine-config` 已完整实现（支持一次性提交引擎基本信息 + 标签规则 + 场景配置），作为备用方案预留。当前主流程是分步操作（分别调用 engine/tag/scene 各自的接口）。若前端改为单页全量提交模式，可启用此 Controller 替代分步接口。

### 已讨论并确认的设计决策（不再重复讨论）

- **默认引擎是否按适用对象独立** → 项目经理决定：全局唯一默认，不做 per-applicableObject 独立默认，获取不到就返回 null，调用方自行降级。

---

## 迁移至 qbank 项目

本项目的策略引擎模块已迁移到 qbank 项目中，当前前端项目（`E:\claudeProjects\ThreePart-Web`）已配置为指向 qbank 服务进行联调测试。

### qbank 项目位置

- **项目路径**：`C:\Users\Administrator\.ssh\Qbank\qbank-app`
- **本地服务端口**：`8291`

### qbank 中策略引擎相关文件

- **迁移 SQL**：`src/main/resources/db/migration/V20260303.001.0001__create_strategy_engine_tables.sql`
- **实体类**：`src/main/java/com/mirayai/qbank/dao/entity/Strategy*.java`
- **工具类**：`src/main/java/com/mirayai/qbank/common/util/strategy/`（RuleMatchEngine、RuleNode、RuleToSqlTranslator）
- **Controller**：`src/main/java/com/mirayai/qbank/web/controller/Strategy*Controller.java`

### 迁移差异对照

| 项目 | ThreePart（原） | qbank（迁移后） |
|------|-----------------|-----------------|
| API 前缀 | `/api/engine`、`/api/tag`、`/api/scene`、`/api/field` | `/qbank/strategy/engine`、`/qbank/strategy/tag`、`/qbank/strategy/scene`、`/qbank/strategy/field` |
| 表名前缀 | `strategy_` | `t_strategy_` |
| 主键字段名 | `id` | `strategy_*_id`（如 `strategy_engine_id`） |
| 时间字段 | `created_time` / `updated_time` | `gmt_create` / `gmt_modified` |
| 逻辑删除字段 | `deleted`（tinyint） | `is_deleted`（int） |
| 审计字段 | 无 | `creator_id`、`creator_name`、`modifier_id`、`modifier_name` |
| URL 命名风格 | 驼峰（`toggleStatus`、`setDefault`、`cancelDefault`） | 短横线（`toggle-status`、`set-default`、`cancel-default`） |

### 前端联调配置（已修改）

前端项目（`E:\claudeProjects\ThreePart-Web`）已做以下调整以指向 qbank 服务：

- `vite.config.ts`：代理 `/qbank/strategy` → `http://localhost:8291`
- `src/api/http.ts`：`baseURL` 改为 `/qbank/strategy`；响应拦截器改为 `!result.success` 判断（qbank Result.code 为 `"00000"` 字符串，通过 `success: boolean` 字段判断成功）
- `src/api/engine.ts`：`toggleStatus` → `toggle-status`，`setDefault` → `set-default`，`cancelDefault` → `cancel-default`
- `src/api/tag.ts`：`toggleStatus` → `toggle-status`
- `src/types/index.ts`：所有 VO/DTO 主键字段由 `id` 改为各自完整名（`strategyEngineId`、`strategyTagRuleId`、`strategySceneId`、`strategyTagFieldId`）；时间字段由 `createdTime`/`updatedTime` 改为 `gmtCreate`/`gmtModified`；`StrategySceneTagVO` 新增 `tagDescription` 字段
- `src/views/engine/EngineList.vue`、`src/views/tag/TagList.vue`、`src/views/tag/TagEdit.vue`、`src/views/scene/SceneList.vue`、`src/views/field/FieldList.vue`：所有取主键的 `.id` 改为对应完整字段名

如需切回 ThreePart 本地开发，`git checkout` 恢复上述相关文件即可。

---

## kgraph-frontend 前端项目说明

### 项目路径
`E:\Projects\Frontend\kgraph-frontend`

### 技术栈
Vue 3 + TypeScript + Element Plus + Tailwind CSS + Pinia

### 策略引擎相关文件
```
src/
├── views/strategy/
│   ├── engine/
│   │   ├── EngineList.vue       引擎列表页（已移除"是否默认"列和设默认操作）
│   │   └── EngineConfig.vue     引擎配置页（含标签管理/场景管理 Tab，父容器 h-[calc(100vh-100px)]）
│   ├── engine-group/
│   │   └── EngineGroupList.vue  引擎组列表页（含展开行展示组内引擎、添加/移除引擎弹窗）
│   ├── tag/
│   │   ├── TagList.vue          标签列表页（根容器用 h-full，不能用 h-[calc(100vh-100px)]）
│   │   └── TagEdit.vue          标签编辑页（新建/编辑标签规则，含 RuleEditor）
│   ├── scene/
│   │   └── SceneList.vue        场景列表页（含标签权重管理弹窗 + 关联新标签弹窗）
│   └── field/
│       └── FieldList.vue        字段管理页
├── components/RuleEditor/
│   ├── index.vue                规则编辑器容器（左侧字段面板 + 右侧画布）
│   ├── RuleGroup.vue            分组节点（AND/OR 切换，支持嵌套）
│   └── RuleCondition.vue        条件节点（字段选择/运算符/值，bg-blue-100 蓝色背景）
├── api/
│   ├── strategyEngine.ts        引擎相关 API（已移除 setDefault/cancelDefault/getDefault）
│   ├── strategyEngineGroup.ts   引擎组相关 API（新增）
│   ├── strategyTag.ts           标签相关 API
│   ├── strategyScene.ts         场景相关 API
│   └── strategyField.ts         字段相关 API
└── store/
    ├── strategyEngine.ts
    ├── strategyEngineGroup.ts   引擎组 Store（新增）
    ├── strategyTag.ts
    ├── strategyScene.ts
    └── strategyEnum.ts
```

### 代理配置
`vite.config.ts` 代理 `/qbank/strategy` → `http://localhost:8291`

### 布局注意事项
- `EngineConfig.vue` 是父容器，已设 `h-[calc(100vh-100px)]`，内容区 `flex-1 overflow-hidden`
- 子页面（TagList、SceneList）根容器必须用 `h-full`，否则分页会被挤出可视区域
- 表格用 `class="flex-1 min-h-0"`，分页放在表格区外部底部

### RuleEditor 样式规范
- 整体外框：`border border-gray-200 rounded-lg`
- 左侧字段面板：`bg-white`，字段项：`bg-[#F2F3F5]`
- 右侧画布：`bg-white`
- 根分组：`bg-white`（无边框）
- 子分组：`bg-white border border-dashed border-gray-300`
- 条件行：`bg-blue-100`（蓝色背景），内部三个输入框默认白色不需要额外覆盖
- AND/OR 激活色：`bg-blue-600`
- 且/或分隔文字：`text-center text-sm text-gray-500`
