# 策略引擎管理系统 - 最终方案总结

## 问题回顾

你提出的核心问题：
> "我还是没有想明白，getFullConfig 是你想要直接传递给前端展示的内容是吗，但是查询到的这个结果到标签规则 tab 就进行了新增标签规则，而每一个场景中都需要该引擎所关联的全量标签用于选择，所以此时场景的标签规则中会包含之前存在的以及当前新增的临时标签，我感觉好乱啊，我不知道前端怎么处理"

## 解决方案：分步保存（推荐）

经过讨论，我们采用**分步保存 + 实时同步**的方案，这样前端逻辑最简单。

### 核心思想

1. **每个 Tab 独立保存**：不需要等到最后统一提交
2. **操作即生效**：Tab 2 保存标签后，Tab 3 立即可用
3. **不需要草稿状态**：前端无需维护复杂的临时数据

### 工作流程

```
┌─────────────────────────────────────────────────────────┐
│ 用户进入引擎编辑页                                         │
│ URL: /engine/edit/{engineId}                             │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ Tab 1: 基本信息                                           │
│ - 调用 GET /api/engine/{engineId} 获取引擎信息            │
│ - 用户修改引擎名称、类型等                                 │
│ - 点击"保存基本信息"按钮                                   │
│ - 调用 PUT /api/engine/{engineId} 保存                   │
│ - 保存成功，停留在当前页，显示成功提示                      │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ Tab 2: 标签规则                                           │
│ - 调用 GET /api/tag/page/{engineId} 获取标签列表          │
│ - 显示现有标签列表                                         │
│                                                           │
│ 【操作1：新增标签】                                        │
│   - 点击"新增标签"按钮                                     │
│   - 弹出标签表单                                          │
│   - 填写标签名称、说明                                     │
│   - 点击"保存" → POST /api/tag                           │
│   - 标签保存到数据库                                       │
│   - 关闭表单，刷新列表                                     │
│                                                           │
│ 【操作2：编辑标签】                                        │
│   - 点击某个标签的"编辑"按钮                               │
│   - 修改标签信息                                          │
│   - 点击"保存" → PUT /api/tag                            │
│   - 更新成功                                              │
│                                                           │
│ 【操作3：删除标签】                                        │
│   - 点击某个标签的"删除"按钮                               │
│   - 调用 GET /api/tag/{id}/usage 检查使用情况             │
│   - 如果被场景使用，弹出确认框                             │
│   - 确认后调用 DELETE /api/tag/{id}                       │
│   - 删除成功，刷新列表                                     │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ Tab 3: 场景策略                                           │
│ - 调用 GET /api/scene/page/{engineId} 获取场景列表        │
│ - 显示现有场景列表                                         │
│                                                           │
│ 【操作1：新增场景】                                        │
│   Step 1: 创建场景基本信息                                │
│     - 点击"新增场景"按钮                                   │
│     - 填写场景名称、说明                                   │
│     - 点击"下一步" → POST /api/scene                      │
│     - 场景创建成功，获得 sceneId                          │
│                                                           │
│   Step 2: 配置场景标签                                    │
│     - 自动弹出"配置标签"对话框                             │
│     - 调用 GET /api/tag/list/{engineId} 获取所有可用标签   │
│     - 显示标签列表（包括刚才在 Tab 2 新增的标签）           │
│     - 用户勾选标签，设置权重（1-10）                       │
│     - 点击"保存" → POST /api/scene/{sceneId}/tags        │
│     - 标签配置保存成功                                     │
│     - 关闭对话框，刷新场景列表                             │
│                                                           │
│ 【操作2：编辑场景标签配置】                                │
│   - 点击某个场景的"配置标签"按钮                           │
│   - 调用 GET /api/tag/list/{engineId} 获取所有可用标签    │
│   - 调用 GET /api/scene/{sceneId} 获取当前配置             │
│   - 显示标签选择器，已配置的标签勾选并显示当前权重         │
│   - 用户修改配置                                          │
│   - 点击"保存" → POST /api/scene/{sceneId}/tags          │
│   - 更新成功                                              │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ 用户点击"返回列表"                                         │
│ - 直接返回引擎列表页                                       │
│ - 所有修改已经保存                                         │
└─────────────────────────────────────────────────────────┘
```

## API 接口总览

### 引擎管理
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/engine/page` | GET | 分页查询引擎列表 |
| `/api/engine/{id}` | GET | 查询引擎详情 |
| `/api/engine` | POST | 创建引擎 |
| `/api/engine/{id}` | PUT | **更新引擎基本信息** |
| `/api/engine/{id}` | DELETE | 删除引擎 |
| `/api/engine/{id}/toggle-status` | PUT | 切换引擎状态 |
| `/api/engine/{id}/set-default` | PUT | 设置为默认引擎 |

### 标签管理
| 接口 | 方法 | 说明 | 使用场景 |
|------|------|------|----------|
| `/api/tag/page/{engineId}` | GET | 分页查询标签 | 标签管理列表页（未来扩展） |
| `/api/tag/list/{engineId}` | GET | 获取所有标签（不分页） | **Tab 2: 显示标签列表**<br>**Tab 3: 选择标签** |
| `/api/tag/{id}` | GET | 查询标签详情 | 编辑标签时回显 |
| `/api/tag` | POST | **创建标签** | **Tab 2: 新增标签** |
| `/api/tag` | PUT | 更新标签 | Tab 2: 编辑标签 |
| `/api/tag/{id}` | DELETE | **删除标签** | **Tab 2: 删除标签** |
| `/api/tag/{id}/usage` | GET | **查询标签使用情况** | **删除前检查** |

### 场景管理
| 接口 | 方法 | 说明 | 使用场景 |
|------|------|------|----------|
| `/api/scene/page/{engineId}` | GET | 分页查询场景 | 场景管理列表页（未来扩展） |
| `/api/scene/list/{engineId}` | GET | 获取所有场景（不分页） | **Tab 3: 显示场景列表** |
| `/api/scene/{id}` | GET | 查询场景详情 | 编辑场景时回显 |
| `/api/scene` | POST | **创建场景** | **Tab 3: 新增场景** |
| `/api/scene` | PUT | 更新场景 | Tab 3: 编辑场景 |
| `/api/scene/{id}` | DELETE | 删除场景 | Tab 3: 删除场景 |
| `/api/scene/{sceneId}/tags` | POST | **配置场景标签** | **Tab 3: 配置标签权重** |

## 前端实现要点

### 1. Tab 间数据不需要共享状态

```javascript
// ❌ 不推荐：维护全局草稿状态
const [draftData, setDraftData] = useState({
  basicInfo: {},
  tags: [],
  scenes: []
});

// ✅ 推荐：每个 Tab 独立管理数据
// Tab 1
const [basicInfo, setBasicInfo] = useState({});

// Tab 2
const [tags, setTags] = useState([]);

// Tab 3
const [scenes, setScenes] = useState([]);
```

### 2. Tab 3 获取最新标签列表

```javascript
// 在 Tab 3 点击"配置标签"时
async function openTagConfigDialog(sceneId) {
  // 实时从后端获取最新标签列表
  const response = await fetch(`/api/tag/list/${engineId}`);
  const data = await response.json();

  // 这个列表包含了 Tab 2 中新增的所有标签
  setAvailableTags(data.data);

  setCurrentSceneId(sceneId);
  setShowTagConfigDialog(true);
}
```

### 3. 删除标签前的检查

```javascript
async function handleDeleteTag(tagId) {
  // 先检查标签是否被使用
  const response = await fetch(`/api/tag/${tagId}/usage`);
  const data = await response.json();

  if (data.data.sceneCount > 0) {
    // 弹出确认框
    Modal.confirm({
      title: '确认删除',
      content: `该标签被 ${data.data.sceneCount} 个场景使用，删除后这些场景的标签配置也会被删除。确定要删除吗？`,
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        // 执行删除
        await fetch(`/api/tag/${tagId}`, { method: 'DELETE' });
        message.success('删除成功');
        loadTags(); // 刷新列表
      }
    });
  } else {
    // 直接删除
    await fetch(`/api/tag/${tagId}`, { method: 'DELETE' });
    message.success('删除成功');
    loadTags();
  }
}
```

### 4. 完整的场景配置流程

```javascript
// 新增场景
async function handleAddScene() {
  // Step 1: 显示场景基本信息表单
  setShowSceneForm(true);
}

// 保存场景基本信息
async function handleSaveSceneBasicInfo(sceneData) {
  // 创建场景
  const response = await fetch('/api/scene', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      engineId: engineId,
      name: sceneData.name,
      description: sceneData.description,
      status: 1
    })
  });

  if (response.ok) {
    const result = await response.json();
    const sceneId = result.data;

    message.success('场景创建成功');
    setShowSceneForm(false);

    // Step 2: 自动打开标签配置对话框
    openTagConfigDialog(sceneId);
  }
}

// 打开标签配置对话框
async function openTagConfigDialog(sceneId) {
  // 获取所有可用标签
  const response = await fetch(`/api/tag/list/${engineId}`);
  const data = await response.json();
  setAvailableTags(data.data);

  // 如果是编辑，还要获取当前配置
  if (sceneId) {
    const sceneResponse = await fetch(`/api/scene/${sceneId}`);
    const sceneData = await sceneResponse.json();
    setCurrentTagConfigs(sceneData.data.tags || []);
  }

  setCurrentSceneId(sceneId);
  setShowTagConfigDialog(true);
}

// 保存标签配置
async function handleSaveTagConfig() {
  // tagConfigs 格式：
  // [
  //   { tagId: 1, enabled: 1, weightCoefficient: 8 },
  //   { tagId: 2, enabled: 1, weightCoefficient: 5 }
  // ]

  const response = await fetch(`/api/scene/${currentSceneId}/tags`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(tagConfigs)
  });

  if (response.ok) {
    message.success('标签配置保存成功');
    setShowTagConfigDialog(false);
    loadScenes(); // 刷新场景列表
  }
}
```

## 后端关键实现

### 1. 删除标签时级联删除关联

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void delete(Long id) {
    TagRule tag = tagRuleMapper.selectById(id);
    if (tag == null) {
        throw new BusinessException("标签不存在");
    }

    // 先删除场景标签关联
    LambdaQueryWrapper<SceneTagRelation> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(SceneTagRelation::getTagId, id);
    sceneTagRelationMapper.delete(wrapper);

    // 再删除标签本身
    tagRuleMapper.deleteById(id);

    // 更新引擎的标签总数
    updateEngineTagCount(tag.getEngineId());
}
```

### 2. 查询标签使用情况

```java
@Override
public TagUsageVO getTagUsage(Long tagId) {
    TagUsageVO vo = new TagUsageVO();
    vo.setTagId(tagId);

    // 查询使用该标签的场景标签关联
    LambdaQueryWrapper<SceneTagRelation> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(SceneTagRelation::getTagId, tagId);
    List<SceneTagRelation> relations = sceneTagRelationMapper.selectList(wrapper);

    vo.setSceneCount((long) relations.size());

    // 查询场景详细信息
    if (!relations.isEmpty()) {
        List<Long> sceneIds = relations.stream()
                .map(SceneTagRelation::getSceneId)
                .collect(Collectors.toList());

        List<SceneStrategy> scenes = sceneStrategyMapper.selectBatchIds(sceneIds);
        List<TagUsageVO.SceneInfo> sceneInfos = scenes.stream()
                .map(scene -> {
                    TagUsageVO.SceneInfo info = new TagUsageVO.SceneInfo();
                    info.setSceneId(scene.getId());
                    info.setSceneName(scene.getName());
                    return info;
                })
                .collect(Collectors.toList());

        vo.setScenes(sceneInfos);
    }

    return vo;
}
```

### 3. 获取所有可用标签（不分页）

```java
@Override
public List<TagRuleVO> listByEngineId(Long engineId) {
    LambdaQueryWrapper<TagRule> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(TagRule::getEngineId, engineId)
            .eq(TagRule::getStatus, 1) // 只返回启用的标签
            .orderByDesc(TagRule::getCreatedTime);

    List<TagRule> tags = tagRuleMapper.selectList(wrapper);
    return tags.stream()
            .map(tag -> BeanUtil.copyProperties(tag, TagRuleVO.class))
            .collect(Collectors.toList());
}
```

## 方案优势总结

### ✅ 前端逻辑简单
- 不需要维护复杂的草稿状态
- 不需要 tempId 映射机制
- 每个 Tab 独立，代码清晰

### ✅ 用户体验自然
- 操作即生效，符合直觉
- Tab 2 保存的标签，Tab 3 立即可用
- 不需要理解"草稿"的概念

### ✅ 后端实现简单
- 复用现有的 CRUD 接口
- 每个操作独立事务
- 不需要复杂的统一保存逻辑

### ✅ 数据一致性好
- 每个操作都有事务保证
- 删除标签时级联删除关联
- 自动维护统计字段

## 可选的统一保存方案

如果你的原型确实需要"统一保存"功能（所有修改最后一起提交），我也提供了完整的实现：

- [EngineFullConfigDTO.java](../src/main/java/com/strategy/engine/dto/EngineFullConfigDTO.java)
- [EngineFullConfigVO.java](../src/main/java/com/strategy/engine/vo/EngineFullConfigVO.java)
- [EngineFullConfigService.java](../src/main/java/com/strategy/engine/service/EngineFullConfigService.java)
- [EngineFullConfigServiceImpl.java](../src/main/java/com/strategy/engine/service/impl/EngineFullConfigServiceImpl.java)
- [EngineFullConfigController.java](../src/main/java/com/strategy/engine/controller/EngineFullConfigController.java)

详细设计文档：[UNIFIED_SAVE_DESIGN.md](./UNIFIED_SAVE_DESIGN.md)

## 建议

**强烈推荐使用分步保存方案**，因为：
1. 你提到原型有"保存基本信息"按钮，说明原型设计就是分步保存
2. 前端和后端实现都更简单
3. 用户体验更自然
4. 已有的接口可以直接使用，无需大改

如果后续确实需要"取消所有更改"功能，可以通过快照机制实现，但通常不推荐，因为实现复杂且用户很少使用。
