# 策略引擎管理系统

## 项目简介

策略引擎管理系统是一个基于 Spring Boot + MyBatis-Plus 的企业级应用，用于管理和配置策略引擎、标签规则和场景策略。

## 技术栈

- Java 1.8
- Spring Boot 2.7.18
- MyBatis-Plus 3.5.3.1
- MySQL 8.0
- Druid 连接池
- Lombok
- Hutool 工具类
- Swagger/OpenAPI 3.0

## 项目结构

```
ThreePart/
├── src/
│   ├── main/
│   │   ├── java/com/strategy/engine/
│   │   │   ├── common/              # 通用响应类
│   │   │   ├── config/              # 配置类
│   │   │   ├── controller/          # 控制器层
│   │   │   ├── dto/                 # 数据传输对象
│   │   │   ├── entity/              # 实体类
│   │   │   ├── enums/               # 枚举类
│   │   │   ├── exception/           # 异常处理
│   │   │   ├── mapper/              # Mapper 接口
│   │   │   ├── service/             # Service 接口
│   │   │   │   └── impl/            # Service 实现
│   │   │   └── vo/                  # 视图对象
│   │   │   └── StrategyEngineApplication.java  # 启动类
│   │   └── resources/
│   │       ├── application.yml      # 应用配置
│   │       └── sql/
│   │           └── schema.sql       # 数据库初始化脚本
│   └── test/
└── pom.xml
```

## 核心模块

### 1. 引擎层 (Engine)
- 引擎列表管理
- 引擎详情编辑（基本信息、标签规则、场景策略）
- 状态管理（启用/禁用、默认引擎设置）

### 2. 标签规则层 (Tag Rules)
- 标签池管理
- 标签的增删改查
- 标签状态控制

### 3. 场景策略层 (Scene Strategies)
- 场景管理
- 场景与标签的多对多权重配置
- 权重系数设置（1-10）

## 数据库设计

### 主要表结构

1. **strategy_engine** - 策略引擎表
2. **tag_rule** - 标签规则表
3. **scene_strategy** - 场景策略表
4. **scene_tag_relation** - 场景标签关联表（权重配置）

详细建表语句请查看 `src/main/resources/sql/schema.sql`

## 快速开始

### 1. 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+

### 2. 数据库配置

修改 `src/main/resources/application.yml` 中的数据库连接配置:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/strategy_engine?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root
```

### 3. 初始化数据库

执行 `src/main/resources/sql/schema.sql` 中的 SQL 脚本创建数据库和表结构。

### 4. 启动项目

```bash
mvn clean install
mvn spring-boot:run
```

### 5. 访问接口文档

启动成功后，访问 Swagger UI 查看 API 文档:

```
http://localhost:8080/api/swagger-ui.html
```

## API 接口说明

> **当前系统采用分步保存方案**，每个 Tab 的操作独立保存。详细接口文档请查看：[docs/API_REFERENCE.md](docs/API_REFERENCE.md)

### 引擎管理

- `GET /api/engine/page` - 分页查询引擎列表
- `GET /api/engine/{id}` - 查询引擎详情
- `POST /api/engine` - 创建引擎
- `PUT /api/engine/{id}` - **更新引擎基本信息**
- `DELETE /api/engine/{id}` - 删除引擎
- `PUT /api/engine/{id}/toggle-status` - 切换引擎状态
- `PUT /api/engine/{id}/set-default` - 设置为默认引擎
- `PUT /api/engine/{id}/cancel-default` - 取消默认引擎

### 标签管理

- `GET /api/tag/list/{engineId}` - **获取所有标签（不分页，用于编辑页）**
- `GET /api/tag/page/{engineId}` - 分页查询标签列表
- `GET /api/tag/{id}` - 查询标签详情
- `POST /api/tag` - **创建标签**
- `PUT /api/tag` - 更新标签
- `DELETE /api/tag/{id}` - **删除标签**
- `GET /api/tag/{id}/usage` - **查询标签使用情况（删除前检查）**
- `DELETE /api/tag/batch/{engineId}` - 根据引擎ID批量删除标签

### 场景管理

- `GET /api/scene/list/{engineId}` - **获取所有场景（不分页，用于编辑页）**
- `GET /api/scene/page/{engineId}` - 分页查询场景列表
- `GET /api/scene/{id}` - 查询场景详情
- `POST /api/scene` - **创建场景**
- `PUT /api/scene` - 更新场景
- `DELETE /api/scene/{id}` - 删除场景
- `POST /api/scene/{sceneId}/tags` - **批量配置场景标签关联**

### 备用接口（统一保存方案，当前未使用）

以下接口提供统一保存功能，保留作为备选方案：

- `GET /api/engine-config/{engineId}` - 获取引擎完整配置
- `POST /api/engine-config` - 保存引擎完整配置
- `POST /api/engine-config/validate` - 验证配置有效性

> 详细设计文档：[docs/UNIFIED_SAVE_DESIGN.md](docs/UNIFIED_SAVE_DESIGN.md)

## 枚举类型说明

### 引擎类型 (EngineType)
- `COMPREHENSIVE_REVIEW` - 综合复习
- `SINGLE_EXAM` - 单场考试

### 适用对象 (ApplicableObject)
- `STUDENT` - 学生
- `CLASS` - 班级
- `GRADE` - 年级
- `BUREAU` - 教育局

### 状态 (StatusEnum)
- `0` - 禁用
- `1` - 启用

## 开发规范

- 使用 Lombok 简化实体类代码
- 统一异常处理（GlobalExceptionHandler）
- 统一响应格式（Result）
- 参数校验使用 JSR-303 注解
- 支持逻辑删除
- 自动填充创建时间和更新时间

## 注意事项

1. 数据库字段使用下划线命名，Java 实体类使用驼峰命名，MyBatis-Plus 自动映射
2. 所有 DELETE 操作为逻辑删除
3. 引擎的 tagCount 和 sceneCount 字段会在相关操作时自动更新
4. 设置默认引擎时会自动取消其他引擎的默认状态
5. 删除场景时会自动删除关联的场景标签配置
