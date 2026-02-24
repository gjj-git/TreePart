# 项目实现方案说明

## 当前采用方案：分步保存

系统采用**分步保存方案**，编辑页面的三个 Tab 独立操作，每次操作立即生效。

### 核心接口

- **Tab 1 基本信息**：`PUT /api/engine/{id}` - 更新引擎基本信息
- **Tab 2 标签规则**：
  - `GET /api/tag/list/{engineId}` - 加载所有标签（不分页）
  - `POST /api/tag` - 新增标签（立即保存）
  - `DELETE /api/tag/{id}` - 删除标签（删除前检查使用情况）
- **Tab 3 场景策略**：
  - `GET /api/scene/list/{engineId}` - 加载所有场景（不分页）
  - `POST /api/scene` - 创建场景
  - `POST /api/scene/{sceneId}/tags` - 配置场景标签关联

### 工作流程

```
Tab 2: 新增标签
  ↓
POST /api/tag (立即保存到数据库)
  ↓
GET /api/tag/list/{engineId} (刷新列表)
  ↓
Tab 3: 配置场景
  ↓
GET /api/tag/list/{engineId} (获取所有标签，包含刚才新增的)
  ↓
用户选择标签并设置权重
  ↓
POST /api/scene/{sceneId}/tags (保存配置)
```

### 优势

✅ **前端逻辑简单** - 不需要维护复杂的草稿状态
✅ **实时生效** - Tab 2 保存的标签，Tab 3 立即可用
✅ **用户体验自然** - 符合用户的操作习惯
✅ **后端实现简单** - 复用标准 CRUD 接口

---

## 备用方案：统一保存（已实现但未启用）

系统中保留了**统一保存方案**的完整实现，作为备选方案。

### 相关文件（已标注"备用"）

以下文件已添加清晰的注释说明，标注为"备用方案 - 当前未使用"：

- `EngineFullConfigController` - 统一保存接口
- `EngineFullConfigService` + 实现类 - 统一保存业务逻辑
- `EngineFullConfigDTO` - 统一保存请求对象
- `EngineFullConfigVO` - 统一保存响应对象

### 备用接口

- `GET /api/engine-config/{engineId}` - 获取引擎完整配置
- `POST /api/engine-config` - 保存引擎完整配置（事务性）
- `POST /api/engine-config/validate` - 验证配置有效性

### 何时考虑启用统一保存

如果业务场景出现以下需求，可以考虑启用统一保存方案：

1. **需要"取消所有更改"功能** - 用户编辑后可以一键取消所有未保存的修改
2. **需要草稿功能** - 允许用户保存草稿，稍后继续编辑
3. **需要事务回滚** - 要求所有配置要么全部成功，要么全部失败

详细设计文档：[UNIFIED_SAVE_DESIGN.md](./UNIFIED_SAVE_DESIGN.md)

---

## 文档索引

| 文档 | 说明 |
|------|------|
| [FINAL_SOLUTION.md](./FINAL_SOLUTION.md) | 最终方案总结（分步保存） |
| [API_REFERENCE.md](./API_REFERENCE.md) | 完整的 API 接口文档 |
| [API_PAGINATION_GUIDE.md](./API_PAGINATION_GUIDE.md) | 分页 vs 不分页的设计说明 |
| [STEP_BY_STEP_SAVE_DESIGN.md](./STEP_BY_STEP_SAVE_DESIGN.md) | 分步保存详细设计 |
| [UNIFIED_SAVE_DESIGN.md](./UNIFIED_SAVE_DESIGN.md) | 统一保存详细设计（备用） |

---

## 快速参考

### 编辑页数据加载

```javascript
// 加载所有标签（不分页）
GET /api/tag/list/{engineId}
→ 返回数组 [{ id, name, description, status }, ...]

// 加载所有场景（不分页）
GET /api/scene/list/{engineId}
→ 返回数组 [{ id, name, description, status, tags: [...] }, ...]
```

### 标签操作

```javascript
// 新增标签
POST /api/tag
{ engineId, name, description, status }
→ 返回标签ID

// 删除标签（先检查）
GET /api/tag/{id}/usage
→ 返回 { tagId, sceneCount, scenes: [...] }

// 确认后删除
DELETE /api/tag/{id}
```

### 场景标签配置

```javascript
// 创建场景
POST /api/scene
{ engineId, name, description, status }
→ 返回场景ID

// 配置场景标签
POST /api/scene/{sceneId}/tags
[
  { tagId, enabled, weightCoefficient },
  { tagId, enabled, weightCoefficient }
]
```

---

## 注意事项

1. **编辑页使用不分页接口** - `/list/{engineId}` 一次性加载所有数据
2. **列表页使用分页接口** - `/page/{engineId}` 支持分页、排序、筛选
3. **删除标签前检查** - 调用 `/api/tag/{id}/usage` 避免破坏场景配置
4. **操作即生效** - 所有操作立即保存到数据库，无需额外的"保存"按钮

---

## 总结

当前系统采用**分步保存方案**，简单、高效、易于维护。同时保留了**统一保存方案**作为备选，如果将来业务需求变化，可以快速切换。

两套方案都有完整的实现和文档，可以根据实际需求灵活选择。
