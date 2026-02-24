# 策略引擎管理系统 - 项目总览

## 📋 项目简介

策略引擎管理系统是一个用于管理和配置策略引擎的企业级应用。系统支持创建引擎、配置标签规则、定义场景策略，并为每个场景配置标签权重。

**技术栈**: Spring Boot 2.7.18 + MyBatis-Plus 3.5.3.1 + MySQL 8.0

---

## 🏗️ 核心架构

### 三层数据模型

```
引擎 (Strategy Engine)
  ├── 标签池 (Tag Rules)
  │   ├── 标签1: 知识点掌握度
  │   ├── 标签2: 错题率
  │   └── 标签3: 学习时长
  │
  └── 场景策略 (Scene Strategies)
      ├── 场景1: 薄弱知识点推荐
      │   ├── 标签1 (权重: 8)
      │   └── 标签2 (权重: 5)
      │
      └── 场景2: 错题专项训练
          └── 标签2 (权重: 10)
```

### 数据隔离

- 每个引擎有独立的标签池
- 每个引擎有独立的场景策略
- 场景只能使用同一引擎的标签

---

## 📁 目录结构

```
ThreePart/
├── docs/                           # 文档目录
│   ├── IMPLEMENTATION_SUMMARY.md   # 实现方案说明 ⭐
│   ├── API_REFERENCE.md            # API 接口文档 ⭐
│   ├── FINAL_SOLUTION.md           # 最终方案总结
│   ├── API_PAGINATION_GUIDE.md     # 分页设计说明
│   ├── STEP_BY_STEP_SAVE_DESIGN.md # 分步保存设计
│   └── UNIFIED_SAVE_DESIGN.md      # 统一保存设计（备用）
│
├── src/main/java/com/strategy/engine/
│   ├── controller/                 # 控制器层
│   │   ├── StrategyEngineController.java
│   │   ├── TagRuleController.java
│   │   ├── SceneStrategyController.java
│   │   └── EngineFullConfigController.java  # 备用方案
│   │
│   ├── service/                    # 服务层
│   │   ├── StrategyEngineService.java
│   │   ├── TagRuleService.java
│   │   ├── SceneStrategyService.java
│   │   └── EngineFullConfigService.java     # 备用方案
│   │
│   ├── entity/                     # 实体类
│   │   ├── StrategyEngine.java     # 策略引擎
│   │   ├── TagRule.java            # 标签规则
│   │   ├── SceneStrategy.java      # 场景策略
│   │   └── SceneTagRelation.java   # 场景标签关联
│   │
│   ├── dto/                        # 数据传输对象
│   ├── vo/                         # 视图对象
│   ├── mapper/                     # Mapper 接口
│   ├── enums/                      # 枚举类
│   ├── exception/                  # 异常处理
│   ├── common/                     # 通用类
│   └── config/                     # 配置类
│
└── src/main/resources/
    ├── application.yml              # 应用配置
    └── sql/
        └── schema.sql               # 数据库建表脚本
```

---

## 🚀 快速开始

### 1. 环境准备

- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+

### 2. 初始化数据库

```sql
-- 执行建表脚本
mysql -u root -p < src/main/resources/sql/schema.sql
```

### 3. 配置数据库连接

编辑 `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/strategy_engine
    username: root
    password: root
```

### 4. 启动项目

```bash
mvn clean install
mvn spring-boot:run
```

### 5. 访问接口文档

```
http://localhost:8080/api/swagger-ui.html
```

---

## 🔑 核心接口

### 引擎管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/engine/page` | GET | 分页查询引擎列表 |
| `/api/engine/{id}` | GET | 查询引擎详情 |
| `/api/engine` | POST | 创建引擎 |
| `/api/engine/{id}` | PUT | 更新引擎 |
| `/api/engine/{id}` | DELETE | 删除引擎 |

### 标签管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/tag/list/{engineId}` | GET | 获取所有标签（不分页）⭐ |
| `/api/tag` | POST | 创建标签 ⭐ |
| `/api/tag/{id}` | DELETE | 删除标签 ⭐ |
| `/api/tag/{id}/usage` | GET | 查询标签使用情况 ⭐ |

### 场景管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/scene/list/{engineId}` | GET | 获取所有场景（不分页）⭐ |
| `/api/scene` | POST | 创建场景 ⭐ |
| `/api/scene/{sceneId}/tags` | POST | 配置场景标签 ⭐ |

> ⭐ 标注的是编辑页核心接口

---

## 📝 使用示例

### 场景：创建引擎并配置标签和场景

```javascript
// Step 1: 创建引擎
POST /api/engine
{
  "name": "综合复习引擎",
  "type": "COMPREHENSIVE_REVIEW",
  "applicableObject": "STUDENT",
  "status": 1
}
→ 返回 engineId: 1

// Step 2: 创建标签
POST /api/tag
{
  "engineId": 1,
  "name": "知识点掌握度",
  "description": "学生对知识点的掌握程度",
  "status": 1
}
→ 返回 tagId: 10

POST /api/tag
{
  "engineId": 1,
  "name": "错题率",
  "description": "历史错题比例",
  "status": 1
}
→ 返回 tagId: 11

// Step 3: 创建场景
POST /api/scene
{
  "engineId": 1,
  "name": "薄弱知识点推荐",
  "description": "基于知识点掌握度推荐题目",
  "status": 1
}
→ 返回 sceneId: 20

// Step 4: 配置场景标签
POST /api/scene/20/tags
[
  {
    "tagId": 10,
    "enabled": 1,
    "weightCoefficient": 8
  },
  {
    "tagId": 11,
    "enabled": 1,
    "weightCoefficient": 5
  }
]
→ 配置成功
```

---

## 💡 设计亮点

### 1. 分步保存 vs 统一保存

系统采用**分步保存方案**，每个 Tab 独立操作：

✅ **优势**
- 前端逻辑简单，无需维护草稿状态
- Tab 2 新增的标签，Tab 3 立即可用
- 操作即生效，符合用户习惯

同时保留了**统一保存方案**作为备选，支持一次性保存所有配置。

### 2. 不分页接口

编辑页使用不分页接口 (`/list/{engineId}`)：

✅ **原因**
- 数据量可控（标签 < 100，场景 < 50）
- 一次性加载，用户体验好
- 减少请求次数

列表页使用分页接口 (`/page/{engineId}`)，支持大数据量。

### 3. 删除前检查

删除标签前调用 `/api/tag/{id}/usage`，检查使用情况：

✅ **好处**
- 避免误删被使用的标签
- 给用户明确提示
- 保证数据一致性

### 4. 自动维护统计字段

引擎的 `tagCount` 和 `sceneCount` 自动更新：

✅ **实现**
- 创建/删除标签时自动更新 `tagCount`
- 创建/删除场景时自动更新 `sceneCount`
- 无需手动维护

---

## 📚 文档导航

### 必读文档

1. [IMPLEMENTATION_SUMMARY.md](./docs/IMPLEMENTATION_SUMMARY.md) - **实现方案说明**（推荐首先阅读）
2. [API_REFERENCE.md](./docs/API_REFERENCE.md) - **完整的 API 接口文档**

### 设计文档

3. [FINAL_SOLUTION.md](./docs/FINAL_SOLUTION.md) - 最终方案总结
4. [API_PAGINATION_GUIDE.md](./docs/API_PAGINATION_GUIDE.md) - 分页 vs 不分页的设计说明
5. [STEP_BY_STEP_SAVE_DESIGN.md](./docs/STEP_BY_STEP_SAVE_DESIGN.md) - 分步保存详细设计

### 备用方案

6. [UNIFIED_SAVE_DESIGN.md](./docs/UNIFIED_SAVE_DESIGN.md) - 统一保存详细设计（备用）

---

## ⚠️ 注意事项

1. **编辑页使用不分页接口**
   - 标签：`GET /api/tag/list/{engineId}`
   - 场景：`GET /api/scene/list/{engineId}`

2. **列表页使用分页接口**
   - 标签：`GET /api/tag/page/{engineId}`
   - 场景：`GET /api/scene/page/{engineId}`

3. **删除标签前检查**
   - 调用 `GET /api/tag/{id}/usage`
   - 显示使用情况并确认

4. **操作即生效**
   - 所有操作立即保存到数据库
   - 无需额外的"保存"按钮

5. **数据隔离**
   - 每个引擎的数据完全独立
   - 场景只能使用同一引擎的标签

---

## 🎯 下一步

1. ✅ **已完成**：项目基础架构搭建
2. ✅ **已完成**：核心接口实现（分步保存）
3. ✅ **已完成**：备用方案实现（统一保存）
4. ⏳ **待完成**：数据库初始化脚本补充完整
5. ⏳ **待完成**：前端页面开发
6. ⏳ **待完成**：集成测试

---

## 📞 技术支持

如有问题，请参考以下文档：

- 接口不清楚：查看 [API_REFERENCE.md](./docs/API_REFERENCE.md)
- 分页问题：查看 [API_PAGINATION_GUIDE.md](./docs/API_PAGINATION_GUIDE.md)
- 方案选择：查看 [IMPLEMENTATION_SUMMARY.md](./docs/IMPLEMENTATION_SUMMARY.md)

---

## 📊 项目状态

| 模块 | 状态 | 说明 |
|------|------|------|
| 数据库设计 | ✅ 完成 | 4 张核心表 |
| 实体类 | ✅ 完成 | Entity + DTO + VO |
| Mapper 层 | ✅ 完成 | MyBatis-Plus |
| Service 层 | ✅ 完成 | 分步保存 + 统一保存（备用）|
| Controller 层 | ✅ 完成 | RESTful API |
| 接口文档 | ✅ 完成 | Swagger + Markdown |
| 前端页面 | ⏳ 待开发 | - |
| 测试 | ⏳ 待完成 | - |

---

**最后更新**: 2024-01-XX
**当前版本**: v1.0.0
**采用方案**: 分步保存
