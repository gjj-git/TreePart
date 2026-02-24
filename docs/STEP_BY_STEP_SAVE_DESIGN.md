# 引擎编辑页 - 分步保存方案（推荐）

## 设计思路

采用**分步保存 + 实时同步**的方式，每个 Tab 独立保存，避免前端状态管理的复杂性。

## 交互流程

### 方案A：每个Tab独立保存（推荐）

```
用户进入编辑页
    ↓
Tab 1: 基本信息
    - 点击"保存基本信息"按钮
    - 调用 PUT /api/engine/{id}
    - 保存成功，停留在当前页
    ↓
Tab 2: 标签规则
    - 显示当前引擎的所有标签
    - 点击"新增标签"
    - 填写标签信息
    - 点击"保存" → 调用 POST /api/tag
    - 标签立即保存到数据库
    - 列表刷新，显示新标签
    ↓
Tab 3: 场景策略
    - 点击"新增场景"
    - 填写场景信息
    - 点击"配置标签" → 弹出标签选择器
    - 调用 GET /api/tag/list/{engineId} 获取所有标签
    - 选择标签并设置权重
    - 点击"保存场景" → 调用 POST /api/scene + POST /api/scene/{id}/tags
    - 场景和标签关联都保存成功
```

### 核心优势

✅ **前端逻辑简单**：不需要维护复杂的草稿状态
✅ **数据实时可见**：Tab 2 新增的标签，Tab 3 立即可用
✅ **用户体验自然**：符合用户的操作习惯（编辑即保存）
✅ **后端接口简单**：每个操作对应一个独立接口

### 用户取消怎么办？

**关键点：编辑页面不需要"取消"按钮**

- 用户点击"返回"或关闭页面，直接返回列表
- 已保存的数据保留（这是用户操作的结果）
- 如果用户不想要某个标签，可以在 Tab 2 中删除它

### 如果一定要支持"取消所有更改"

可以在进入编辑页时创建一个**快照**：

```
进入编辑页
    ↓
调用 POST /api/engine/{id}/snapshot
后端创建当前引擎的完整快照
    ↓
用户编辑并保存各种操作
    ↓
用户点击"取消所有更改"
    ↓
调用 POST /api/engine/{id}/restore-snapshot
后端恢复到快照状态
```

但这个功能通常**不推荐**，因为：
- 实现复杂
- 用户很少使用
- 可能导致数据混乱

## 前端代码示例

### Tab 1: 基本信息

```javascript
// 加载引擎基本信息
useEffect(() => {
  async function loadEngine() {
    const response = await fetch(`/api/engine/${engineId}`);
    const data = await response.json();
    setBasicInfo(data.data);
  }
  loadEngine();
}, [engineId]);

// 保存基本信息
async function handleSaveBasicInfo() {
  const response = await fetch(`/api/engine/${engineId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(basicInfo)
  });

  if (response.ok) {
    message.success('基本信息保存成功');
  }
}
```

### Tab 2: 标签规则

```javascript
// 加载标签列表
useEffect(() => {
  async function loadTags() {
    const response = await fetch(`/api/tag/page/${engineId}?pageNum=1&pageSize=100`);
    const data = await response.json();
    setTags(data.data.records);
  }
  loadTags();
}, [engineId]);

// 新增标签
async function handleAddTag() {
  // 显示新增标签表单
  setShowAddForm(true);
}

// 保存新增的标签
async function handleSaveTag(tagData) {
  const response = await fetch('/api/tag', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      engineId: engineId,
      ...tagData
    })
  });

  if (response.ok) {
    message.success('标签保存成功');
    // 刷新标签列表
    loadTags();
    setShowAddForm(false);
  }
}

// 删除标签
async function handleDeleteTag(tagId) {
  const response = await fetch(`/api/tag/${tagId}`, {
    method: 'DELETE'
  });

  if (response.ok) {
    message.success('标签删除成功');
    // 刷新标签列表
    loadTags();
  }
}
```

### Tab 3: 场景策略

```javascript
// 加载场景列表
useEffect(() => {
  async function loadScenes() {
    const response = await fetch(`/api/scene/page/${engineId}?pageNum=1&pageSize=100`);
    const data = await response.json();
    setScenes(data.data.records);
  }
  loadScenes();
}, [engineId]);

// 新增场景 - 第一步：填写基本信息
async function handleAddScene() {
  setShowSceneForm(true);
}

// 保存场景基本信息
async function handleSaveScene(sceneData) {
  const response = await fetch('/api/scene', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      engineId: engineId,
      ...sceneData
    })
  });

  if (response.ok) {
    const result = await response.json();
    const sceneId = result.data;
    message.success('场景创建成功');

    // 保存成功后，显示标签配置弹窗
    setCurrentSceneId(sceneId);
    setShowTagConfigDialog(true);
  }
}

// 配置场景标签 - 第二步：配置标签权重
async function handleConfigSceneTags(sceneId) {
  // 加载所有可用标签
  const response = await fetch(`/api/tag/page/${engineId}?pageNum=1&pageSize=100`);
  const data = await response.json();
  setAvailableTags(data.data.records);

  // 显示标签配置弹窗
  setCurrentSceneId(sceneId);
  setShowTagConfigDialog(true);
}

// 保存场景标签配置
async function handleSaveTagConfig(sceneId, tagConfigs) {
  const response = await fetch(`/api/scene/${sceneId}/tags`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(tagConfigs)
  });

  if (response.ok) {
    message.success('标签配置保存成功');
    setShowTagConfigDialog(false);
    // 刷新场景列表
    loadScenes();
  }
}
```

## UI 交互示例

### Tab 2: 标签规则

```
┌─────────────────────────────────────────────────┐
│ 标签规则                                         │
├─────────────────────────────────────────────────┤
│  [+ 新增标签]                                    │
│                                                  │
│  ┌────────────────────────────────────────────┐ │
│  │ 知识点掌握度          [编辑] [删除]         │ │
│  │ 学生对知识点的掌握程度                      │ │
│  └────────────────────────────────────────────┘ │
│                                                  │
│  ┌────────────────────────────────────────────┐ │
│  │ 错题率                [编辑] [删除]         │ │
│  │ 历史错题比例                                │ │
│  └────────────────────────────────────────────┘ │
│                                                  │
└─────────────────────────────────────────────────┘

点击 [+ 新增标签] 后：

┌─────────────────────────────────────────────────┐
│ 新增标签                                    [×]  │
├─────────────────────────────────────────────────┤
│  标签名称: [_____________________]              │
│  标签说明: [_____________________]              │
│  状态:     (●) 启用  ( ) 禁用                   │
│                                                  │
│           [取消]  [保存]                         │
└─────────────────────────────────────────────────┘

点击 [保存] → 调用 POST /api/tag → 关闭弹窗 → 刷新列表
```

### Tab 3: 场景策略

```
┌─────────────────────────────────────────────────┐
│ 场景策略                                         │
├─────────────────────────────────────────────────┤
│  [+ 新增场景]                                    │
│                                                  │
│  ┌────────────────────────────────────────────┐ │
│  │ 薄弱知识点推荐      [配置标签] [编辑] [删除]│ │
│  │ 基于知识点掌握度推荐题目                    │ │
│  │                                              │ │
│  │ 已配置标签:                                  │ │
│  │  • 知识点掌握度 (权重: 8)                   │ │
│  │  • 错题率 (权重: 5)                         │ │
│  └────────────────────────────────────────────┘ │
│                                                  │
└─────────────────────────────────────────────────┘

点击 [配置标签] 后：

┌─────────────────────────────────────────────────┐
│ 配置场景标签                                [×]  │
├─────────────────────────────────────────────────┤
│  可用标签列表:                                   │
│                                                  │
│  ☑ 知识点掌握度    权重: [8_]   [启用]          │
│  ☑ 错题率          权重: [5_]   [启用]          │
│  ☐ 学习时长        权重: [__]   [禁用]          │
│                                                  │
│           [取消]  [保存]                         │
└─────────────────────────────────────────────────┘

点击 [保存] → 调用 POST /api/scene/{id}/tags → 关闭弹窗 → 刷新列表
```

## 优化建议

### 1. 标签立即可用

在 Tab 2 保存标签后，不需要任何特殊处理，Tab 3 直接调用接口获取最新标签列表即可。

### 2. 减少请求次数

```javascript
// 在整个编辑页面级别，缓存标签列表
const [tags, setTags] = useState([]);

// 在任何 Tab 需要标签列表时，使用缓存
function useTags() {
  useEffect(() => {
    if (tags.length === 0) {
      loadTags();
    }
  }, []);

  return tags;
}

// 在 Tab 2 保存标签后，刷新缓存
function refreshTags() {
  loadTags();
}
```

### 3. 删除标签的验证

在删除标签前，检查是否被场景使用：

```javascript
async function handleDeleteTag(tagId) {
  // 检查是否被使用
  const response = await fetch(`/api/tag/${tagId}/usage`);
  const data = await response.json();

  if (data.data.sceneCount > 0) {
    Modal.confirm({
      title: '该标签正在被场景使用',
      content: `有 ${data.data.sceneCount} 个场景使用了该标签，删除后这些场景的标签配置也会被删除。确定要删除吗？`,
      onOk: () => {
        // 执行删除
        deleteTag(tagId);
      }
    });
  } else {
    // 直接删除
    deleteTag(tagId);
  }
}
```

## 需要添加的后端接口

```java
/**
 * 查询标签使用情况
 */
@GetMapping("/tag/{id}/usage")
public Result<TagUsageVO> getTagUsage(@PathVariable Long id) {
    // 统计有多少场景使用了该标签
    LambdaQueryWrapper<SceneTagRelation> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(SceneTagRelation::getTagId, id);
    Long sceneCount = sceneTagRelationMapper.selectCount(wrapper);

    TagUsageVO vo = new TagUsageVO();
    vo.setTagId(id);
    vo.setSceneCount(sceneCount);

    return Result.success(vo);
}

/**
 * 获取引擎的所有标签（不分页）
 */
@GetMapping("/tag/list/{engineId}")
public Result<List<TagRuleVO>> listByEngineId(@PathVariable Long engineId) {
    LambdaQueryWrapper<TagRule> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(TagRule::getEngineId, engineId)
            .eq(TagRule::getStatus, 1) // 只返回启用的标签
            .orderByDesc(TagRule::getCreatedTime);

    List<TagRule> tags = tagRuleMapper.selectList(wrapper);
    List<TagRuleVO> vos = tags.stream()
        .map(tag -> BeanUtil.copyProperties(tag, TagRuleVO.class))
        .collect(Collectors.toList());

    return Result.success(vos);
}
```

## 总结

### 分步保存方案的优势

1. **前端逻辑简单**：每个操作独立，不需要维护复杂状态
2. **实时生效**：Tab 2 保存的标签，Tab 3 立即可用
3. **用户体验自然**：类似于表单的"保存"操作
4. **后端实现简单**：使用现有的 CRUD 接口即可

### 与统一保存方案的对比

| 特性 | 分步保存 | 统一保存 |
|------|---------|---------|
| 前端复杂度 | ⭐⭐ 简单 | ⭐⭐⭐⭐ 复杂 |
| 后端复杂度 | ⭐⭐ 简单 | ⭐⭐⭐⭐ 复杂 |
| 用户体验 | ⭐⭐⭐⭐ 自然 | ⭐⭐⭐ 需要学习 |
| 数据一致性 | ⭐⭐⭐ 较好 | ⭐⭐⭐⭐⭐ 完美 |
| 取消操作 | ⭐⭐ 较难 | ⭐⭐⭐⭐⭐ 容易 |

### 推荐

**采用分步保存方案**，因为：
- 原型已经设计了"保存基本信息"按钮
- 前端和后端实现都更简单
- 用户体验更自然
- 已有的 CRUD 接口可以直接复用
