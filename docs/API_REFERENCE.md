# 策略引擎管理系统 - 接口总览

## 核心接口列表

### 引擎管理接口

| 接口路径 | HTTP方法 | 功能说明 | 使用场景 |
|---------|---------|---------|---------|
| `/api/engine/page` | GET | 分页查询引擎列表 | 引擎列表页 |
| `/api/engine/{id}` | GET | 查询引擎详情 | 编辑页 Tab 1 |
| `/api/engine` | POST | 创建引擎 | 新增引擎 |
| `/api/engine/{id}` | PUT | 更新引擎 | 编辑页 Tab 1 保存 |
| `/api/engine/{id}` | DELETE | 删除引擎 | 列表页删除操作 |
| `/api/engine/{id}/toggle-status` | PUT | 切换引擎状态 | 启用/禁用引擎 |
| `/api/engine/{id}/set-default` | PUT | 设置为默认引擎 | 设置默认 |
| `/api/engine/{id}/cancel-default` | PUT | 取消默认引擎 | 取消默认 |

### 标签管理接口（核心）

| 接口路径 | HTTP方法 | 功能说明 | 使用场景 | 返回格式 |
|---------|---------|---------|---------|----------|
| `/api/tag/list/{engineId}` | GET | **获取所有标签（不分页）** | **编辑页 Tab 2/3** | `List<TagRuleVO>` |
| `/api/tag/page/{engineId}` | GET | 分页查询标签 | 标签管理列表页 | `Page<TagRuleVO>` |
| `/api/tag/{id}` | GET | 查询标签详情 | 编辑标签回显 | `TagRuleVO` |
| `/api/tag` | POST | **创建标签** | **Tab 2 新增** | `Long` (标签ID) |
| `/api/tag` | PUT | 更新标签 | Tab 2 编辑 | `void` |
| `/api/tag/{id}` | DELETE | **删除标签** | **Tab 2 删除** | `void` |
| `/api/tag/{id}/usage` | GET | **查询标签使用情况** | **删除前检查** | `TagUsageVO` |

### 场景管理接口（核心）

| 接口路径 | HTTP方法 | 功能说明 | 使用场景 | 返回格式 |
|---------|---------|---------|---------|----------|
| `/api/scene/list/{engineId}` | GET | **获取所有场景（不分页）** | **编辑页 Tab 3** | `List<SceneStrategyVO>` |
| `/api/scene/page/{engineId}` | GET | 分页查询场景 | 场景管理列表页 | `Page<SceneStrategyVO>` |
| `/api/scene/{id}` | GET | 查询场景详情 | 编辑场景回显 | `SceneStrategyVO` |
| `/api/scene` | POST | **创建场景** | **Tab 3 新增** | `Long` (场景ID) |
| `/api/scene` | PUT | 更新场景 | Tab 3 编辑 | `void` |
| `/api/scene/{id}` | DELETE | 删除场景 | Tab 3 删除 | `void` |
| `/api/scene/{sceneId}/tags` | POST | **配置场景标签** | **Tab 3 标签配置** | `void` |

### 统一保存接口（可选方案）

| 接口路径 | HTTP方法 | 功能说明 | 使用场景 |
|---------|---------|---------|---------|
| `/api/engine-config/{engineId}` | GET | 获取引擎完整配置 | 编辑页初始化 |
| `/api/engine-config` | POST | 保存引擎完整配置 | 统一保存按钮 |
| `/api/engine-config/validate` | POST | 验证配置有效性 | 保存前验证 |

## 编辑页接口调用流程

### 页面初始化

```javascript
// 1. 进入编辑页，加载引擎基本信息
GET /api/engine/{engineId}
→ 返回引擎的基本字段

// 2. 切换到 Tab 2，加载标签列表
GET /api/tag/list/{engineId}
→ 返回该引擎的所有标签（数组，不分页）

// 3. 切换到 Tab 3，加载场景列表
GET /api/scene/list/{engineId}
→ 返回该引擎的所有场景（数组，每个场景包含标签配置）
```

### Tab 2: 标签规则操作

```javascript
// 新增标签
POST /api/tag
{
  "engineId": 1,
  "name": "知识点掌握度",
  "description": "学生对知识点的掌握程度",
  "status": 1
}
→ 返回标签ID

// 刷新标签列表
GET /api/tag/list/{engineId}
→ 包含刚才新增的标签

// 删除标签前检查
GET /api/tag/{tagId}/usage
→ 返回使用情况
{
  "tagId": 1,
  "sceneCount": 3,
  "scenes": [
    { "sceneId": 10, "sceneName": "薄弱知识点推荐" },
    { "sceneId": 11, "sceneName": "错题专项训练" }
  ]
}

// 确认后删除
DELETE /api/tag/{tagId}
```

### Tab 3: 场景策略操作

```javascript
// Step 1: 创建场景基本信息
POST /api/scene
{
  "engineId": 1,
  "name": "薄弱知识点推荐",
  "description": "基于知识点掌握度推荐题目",
  "status": 1
}
→ 返回场景ID: 10

// Step 2: 获取所有可用标签（供用户选择）
GET /api/tag/list/{engineId}
→ 返回所有标签（包括刚才在 Tab 2 新增的）
[
  { "id": 1, "name": "知识点掌握度", "status": 1 },
  { "id": 2, "name": "错题率", "status": 1 }
]

// Step 3: 配置场景标签
POST /api/scene/10/tags
[
  {
    "tagId": 1,
    "enabled": 1,
    "weightCoefficient": 8
  },
  {
    "tagId": 2,
    "enabled": 1,
    "weightCoefficient": 5
  }
]

// 刷新场景列表
GET /api/scene/list/{engineId}
→ 返回所有场景及其标签配置
```

## 接口返回格式

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": <具体数据>
}
```

### 不分页接口（推荐用于编辑页）

```json
// GET /api/tag/list/{engineId}
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "engineId": 1,
      "name": "知识点掌握度",
      "description": "学生对知识点的掌握程度",
      "status": 1,
      "createdTime": "2024-01-01 10:00:00"
    },
    {
      "id": 2,
      "engineId": 1,
      "name": "错题率",
      "description": "历史错题比例",
      "status": 1,
      "createdTime": "2024-01-01 10:05:00"
    }
  ]
}
```

### 分页接口（用于列表页）

```json
// GET /api/tag/page/{engineId}?pageNum=1&pageSize=10
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      { "id": 1, "name": "知识点掌握度", ... },
      { "id": 2, "name": "错题率", ... }
    ],
    "total": 50,
    "size": 10,
    "current": 1,
    "pages": 5
  }
}
```

### 场景详情（包含标签配置）

```json
// GET /api/scene/{id}
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 10,
    "engineId": 1,
    "name": "薄弱知识点推荐",
    "description": "基于知识点掌握度推荐题目",
    "status": 1,
    "createdTime": "2024-01-01 11:00:00",
    "tags": [
      {
        "id": 100,
        "tagId": 1,
        "tagName": "知识点掌握度",
        "enabled": 1,
        "weightCoefficient": 8
      },
      {
        "id": 101,
        "tagId": 2,
        "tagName": "错题率",
        "enabled": 1,
        "weightCoefficient": 5
      }
    ]
  }
}
```

### 标签使用情况

```json
// GET /api/tag/{id}/usage
{
  "code": 200,
  "message": "success",
  "data": {
    "tagId": 1,
    "sceneCount": 3,
    "scenes": [
      {
        "sceneId": 10,
        "sceneName": "薄弱知识点推荐"
      },
      {
        "sceneId": 11,
        "sceneName": "错题专项训练"
      },
      {
        "sceneId": 12,
        "sceneName": "知识点巩固"
      }
    ]
  }
}
```

## 前端调用示例

### React 示例

```jsx
import { useState, useEffect } from 'react';

function EngineEditPage({ engineId }) {
  const [tags, setTags] = useState([]);
  const [scenes, setScenes] = useState([]);

  // 加载标签列表（不分页）
  async function loadTags() {
    const response = await fetch(`/api/tag/list/${engineId}`);
    const data = await response.json();
    if (data.code === 200) {
      setTags(data.data); // 直接是数组
    }
  }

  // 加载场景列表（不分页）
  async function loadScenes() {
    const response = await fetch(`/api/scene/list/${engineId}`);
    const data = await response.json();
    if (data.code === 200) {
      setScenes(data.data); // 直接是数组
    }
  }

  // 新增标签
  async function handleAddTag(tagData) {
    const response = await fetch('/api/tag', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        engineId: engineId,
        ...tagData
      })
    });

    if (response.ok) {
      message.success('标签创建成功');
      loadTags(); // 刷新列表
    }
  }

  // 删除标签（带检查）
  async function handleDeleteTag(tagId) {
    // 先检查使用情况
    const usageResponse = await fetch(`/api/tag/${tagId}/usage`);
    const usageData = await usageResponse.json();

    if (usageData.data.sceneCount > 0) {
      Modal.confirm({
        title: '确认删除',
        content: `该标签被 ${usageData.data.sceneCount} 个场景使用，删除后这些场景的标签配置也会被删除。确定要删除吗？`,
        onOk: async () => {
          await fetch(`/api/tag/${tagId}`, { method: 'DELETE' });
          message.success('删除成功');
          loadTags();
        }
      });
    } else {
      await fetch(`/api/tag/${tagId}`, { method: 'DELETE' });
      message.success('删除成功');
      loadTags();
    }
  }

  // 配置场景标签
  async function handleConfigSceneTags(sceneId, tagConfigs) {
    const response = await fetch(`/api/scene/${sceneId}/tags`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(tagConfigs)
    });

    if (response.ok) {
      message.success('标签配置保存成功');
      loadScenes(); // 刷新场景列表
    }
  }

  useEffect(() => {
    loadTags();
    loadScenes();
  }, [engineId]);

  return (
    <Tabs>
      <TabPane key="1" tab="基本信息">
        <BasicInfoForm engineId={engineId} />
      </TabPane>
      <TabPane key="2" tab="标签规则">
        <TagList
          tags={tags}
          onAdd={handleAddTag}
          onDelete={handleDeleteTag}
          onRefresh={loadTags}
        />
      </TabPane>
      <TabPane key="3" tab="场景策略">
        <SceneList
          scenes={scenes}
          availableTags={tags}
          onConfigTags={handleConfigSceneTags}
          onRefresh={loadScenes}
        />
      </TabPane>
    </Tabs>
  );
}
```

## 关键要点

### ✅ 推荐做法

1. **编辑页使用不分页接口** (`/api/tag/list/{engineId}`, `/api/scene/list/{engineId}`)
   - 数据量可控（标签 < 100，场景 < 50）
   - 一次性加载，用户体验好
   - 减少请求次数

2. **列表页使用分页接口** (`/api/tag/page/{engineId}`, `/api/scene/page/{engineId}`)
   - 数据量不可控
   - 性能更好
   - 支持排序、筛选

3. **删除前检查使用情况**
   - 调用 `/api/tag/{id}/usage` 查看哪些场景使用了该标签
   - 给用户明确提示，避免误删

4. **操作即生效**
   - Tab 2 新增标签立即保存
   - Tab 3 立即可用（通过 `/api/tag/list/{engineId}` 获取最新列表）

### ❌ 不要这样做

1. 编辑页使用分页接口 → 用户需要翻页，体验差
2. 不检查直接删除标签 → 破坏场景配置，数据不一致
3. 使用统一保存方案 → 前端状态管理复杂，除非业务确实需要

## 相关文档

- [完整解决方案](./FINAL_SOLUTION.md)
- [分步保存设计](./STEP_BY_STEP_SAVE_DESIGN.md)
- [分页 vs 不分页指南](./API_PAGINATION_GUIDE.md)
- [统一保存方案](./UNIFIED_SAVE_DESIGN.md)（备选）
