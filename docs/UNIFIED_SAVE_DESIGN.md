# 引擎编辑页统一保存方案设计

## 问题描述

在引擎编辑页面，用户需要在三个Tab中配置数据：
1. **Tab 1: 基本信息** - 引擎名称、类型等
2. **Tab 2: 标签规则** - 定义标签池
3. **Tab 3: 场景策略** - 配置场景及其关联的标签权重

### 核心矛盾

- 如果用户在Tab 2新增标签后**立即调用API保存**：
  - ✅ Tab 3可以立即看到新标签
  - ❌ 用户点击"取消"时，标签已经保存到数据库，无法回滚

- 如果用户在Tab 2新增标签**不调用API**：
  - ✅ 用户点击"取消"时，没有数据写入数据库
  - ❌ Tab 3看不到新增的标签，无法配置场景

## 解决方案：草稿模式 + 统一提交

### 方案概述

采用**前端草稿状态 + 后端统一保存**的方式：

1. 用户在三个Tab中的所有操作都保存在前端状态中（草稿）
2. 点击"保存"按钮时，一次性提交所有数据到后端
3. 后端使用事务性操作，要么全部成功，要么全部失败

### 前端交互流程

```
用户进入编辑页
    ↓
调用 GET /api/engine-config/{engineId}
获取完整配置（三个Tab的所有数据）
    ↓
渲染三个Tab，所有数据存储在前端状态中
    ↓
用户在各Tab中操作（新增/编辑/删除）
    ↓
前端状态实时更新，三个Tab之间数据互通
    ↓
用户点击"保存"按钮
    ↓
调用 POST /api/engine-config
提交完整配置（包含所有操作记录）
    ↓
后端事务性保存
    ↓
成功：返回引擎ID，跳转到列表页
失败：显示错误信息，用户可继续编辑
```

### 前端数据结构示例

```javascript
// 前端状态管理（React/Vue）
const [formData, setFormData] = useState({
  engineId: null, // 编辑时有值，新增时为null

  // Tab 1: 基本信息
  basicInfo: {
    name: '',
    type: '',
    applicableObject: '',
    description: '',
    status: 1,
    isDefault: 0
  },

  // Tab 2: 标签规则
  tags: [
    {
      tempId: 'temp_tag_1', // 前端临时ID
      id: null,             // 数据库ID（新增时为null）
      name: '知识点掌握度',
      description: '学生对知识点的掌握程度',
      status: 1,
      action: 'add'         // add/update/delete
    },
    {
      tempId: 'temp_tag_2',
      id: 123,              // 已有标签的数据库ID
      name: '错题率',
      description: '历史错题比例',
      status: 1,
      action: 'update'
    }
  ],

  // Tab 3: 场景策略
  scenes: [
    {
      id: null,
      name: '薄弱知识点推荐',
      description: '基于知识点掌握度推荐题目',
      status: 1,
      action: 'add',
      tagConfigs: [
        {
          tagTempId: 'temp_tag_1',  // 引用新增标签的临时ID
          tagId: null,
          enabled: 1,
          weightCoefficient: 8
        },
        {
          tagTempId: null,
          tagId: 123,               // 引用已有标签的数据库ID
          enabled: 1,
          weightCoefficient: 5
        }
      ]
    }
  ]
});
```

### 前端操作示例

#### 1. 在Tab 2新增标签

```javascript
// 用户点击"新增标签"按钮
function handleAddTag() {
  const newTag = {
    tempId: `temp_tag_${Date.now()}`, // 生成唯一临时ID
    id: null,
    name: '',
    description: '',
    status: 1,
    action: 'add'
  };

  setFormData({
    ...formData,
    tags: [...formData.tags, newTag]
  });
}
```

#### 2. 在Tab 3配置场景标签

```javascript
// 用户在场景中选择标签
function handleAddTagToScene(sceneIndex, selectedTag) {
  const newTagConfig = {
    // 如果是新增的标签，使用tempId；如果是已有标签，使用id
    tagTempId: selectedTag.action === 'add' ? selectedTag.tempId : null,
    tagId: selectedTag.id,
    enabled: 1,
    weightCoefficient: 5
  };

  const updatedScenes = [...formData.scenes];
  updatedScenes[sceneIndex].tagConfigs.push(newTagConfig);

  setFormData({
    ...formData,
    scenes: updatedScenes
  });
}
```

#### 3. 获取可用标签列表（用于场景配置）

```javascript
// 在Tab 3中，获取所有未被删除的标签
function getAvailableTags() {
  return formData.tags.filter(tag => tag.action !== 'delete');
}
```

#### 4. 提交保存

```javascript
async function handleSave() {
  try {
    const response = await fetch('/api/engine-config', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formData)
    });

    if (response.ok) {
      const result = await response.json();
      // 跳转到列表页
      navigate('/engine-list');
    } else {
      // 显示错误信息
      alert('保存失败');
    }
  } catch (error) {
    console.error('保存出错', error);
  }
}
```

### 后端处理流程

#### 数据接收

```java
POST /api/engine-config
{
  "engineId": null,  // 新增时为null
  "basicInfo": { ... },
  "tags": [
    {
      "tempId": "temp_tag_1",
      "id": null,
      "name": "知识点掌握度",
      "action": "add"
    }
  ],
  "scenes": [
    {
      "id": null,
      "name": "薄弱知识点推荐",
      "action": "add",
      "tagConfigs": [
        {
          "tagTempId": "temp_tag_1",  // 引用新增标签
          "tagId": null,
          "weightCoefficient": 8
        }
      ]
    }
  ]
}
```

#### 后端处理逻辑

```java
@Transactional(rollbackFor = Exception.class)
public Long saveFullConfig(EngineFullConfigDTO dto) {
    // 1. 保存引擎基本信息
    StrategyEngine engine = saveEngine(dto.getBasicInfo());
    Long engineId = engine.getId();

    // 2. 保存标签，建立临时ID到真实ID的映射
    Map<String, Long> tempIdToRealIdMap = new HashMap<>();
    for (TagRuleItem tag : dto.getTags()) {
        if ("add".equals(tag.getAction())) {
            TagRule newTag = createTag(tag, engineId);
            tempIdToRealIdMap.put(tag.getTempId(), newTag.getId());
        } else if ("update".equals(tag.getAction())) {
            updateTag(tag);
        } else if ("delete".equals(tag.getAction())) {
            deleteTag(tag.getId());
        }
    }

    // 3. 保存场景，使用映射表解析标签引用
    for (SceneStrategyItem scene : dto.getScenes()) {
        if ("add".equals(scene.getAction())) {
            SceneStrategy newScene = createScene(scene, engineId);

            // 保存场景标签关联
            for (SceneTagConfig tagConfig : scene.getTagConfigs()) {
                Long tagId;
                if (tagConfig.getTagId() != null) {
                    // 使用已有标签的ID
                    tagId = tagConfig.getTagId();
                } else {
                    // 使用新增标签的临时ID，从映射表获取真实ID
                    tagId = tempIdToRealIdMap.get(tagConfig.getTagTempId());
                }

                createSceneTagRelation(newScene.getId(), tagId, tagConfig);
            }
        } else if ("update".equals(scene.getAction())) {
            updateScene(scene);
        } else if ("delete".equals(scene.getAction())) {
            deleteScene(scene.getId());
        }
    }

    // 4. 更新统计字段
    updateEngineStatistics(engineId);

    return engineId;
}
```

### 关键优势

#### ✅ 用户体验好
- 用户可以在三个Tab间自由切换
- 新增的标签立即可用于场景配置
- 点击"取消"可以丢弃所有更改

#### ✅ 数据一致性强
- 使用数据库事务，保证原子性
- 要么全部成功，要么全部失败
- 避免部分数据保存导致的不一致

#### ✅ 逻辑清晰
- 前端负责草稿管理
- 后端负责数据持久化
- 职责分离明确

### 附加功能

#### 1. 自动保存草稿（可选）

```javascript
// 定时保存到 localStorage
useEffect(() => {
  const timer = setInterval(() => {
    localStorage.setItem('engine_draft', JSON.stringify(formData));
  }, 30000); // 每30秒自动保存

  return () => clearInterval(timer);
}, [formData]);

// 页面加载时恢复草稿
useEffect(() => {
  const draft = localStorage.getItem('engine_draft');
  if (draft) {
    setFormData(JSON.parse(draft));
  }
}, []);
```

#### 2. 离开提示

```javascript
// 用户编辑后未保存时离开页面，给出提示
useEffect(() => {
  const handleBeforeUnload = (e) => {
    if (hasUnsavedChanges) {
      e.preventDefault();
      e.returnValue = '您有未保存的更改，确定要离开吗？';
    }
  };

  window.addEventListener('beforeunload', handleBeforeUnload);
  return () => window.removeEventListener('beforeunload', handleBeforeUnload);
}, [hasUnsavedChanges]);
```

#### 3. 验证提示

```javascript
// 保存前验证
async function handleSave() {
  // 调用验证接口
  const validateResponse = await fetch('/api/engine-config/validate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(formData)
  });

  if (!validateResponse.ok) {
    alert('配置验证失败，请检查');
    return;
  }

  // 执行保存
  // ...
}
```

## API 接口总结

### 新增接口（推荐使用）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/engine-config/{engineId}` | GET | 获取引擎完整配置 |
| `/api/engine-config` | POST | 保存引擎完整配置 |
| `/api/engine-config/validate` | POST | 验证配置有效性 |

### 原有接口（可保留，用于独立操作）

| 接口 | 方法 | 说明 | 使用场景 |
|------|------|------|----------|
| `/api/tag/*` | * | 标签管理 | 单独管理标签时使用 |
| `/api/scene/*` | * | 场景管理 | 单独管理场景时使用 |
| `/api/engine/*` | * | 引擎管理 | 引擎列表页使用 |

## 总结

这个方案完美解决了你提出的问题：

1. **新增标签不会立即保存到数据库**
2. **新增标签可以立即在场景配置中使用**（通过前端状态共享）
3. **用户点击取消时，所有更改都会丢弃**
4. **用户点击保存时，所有更改都会事务性提交**

前端使用 `tempId` 作为新增标签的临时引用，后端保存时建立临时ID到真实ID的映射表，确保场景标签关联能正确建立。
