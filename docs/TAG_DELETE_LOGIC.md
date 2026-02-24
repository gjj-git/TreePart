# 标签删除逻辑说明

## 设计原则

**强制约束**：标签被场景使用时，**不允许删除**

## 业务逻辑

### 删除标签的流程

```
用户点击删除标签
  ↓
前端调用 DELETE /api/tag/{id}
  ↓
后端检查标签是否被使用
  ↓
┌─ 被使用 → 返回错误：该标签正在被 X 个场景使用，请先移除场景中的标签配置后再删除
│
└─ 未被使用 → 删除成功
```

### 正确的删除顺序

```
1. 用户想删除标签A
2. 后端检查：标签A被场景1、场景2使用
3. 返回错误：该标签正在被 2 个场景使用
4. 用户到场景1中移除标签A的配置
5. 用户到场景2中移除标签A的配置
6. 再次删除标签A → 成功
```

## 接口说明

### DELETE /api/tag/{id} - 删除标签

**逻辑**：
1. 检查标签是否存在
2. 检查标签是否被场景使用
3. 如果被使用，抛出异常
4. 如果未被使用，删除标签

**错误响应**：
```json
{
  "code": 500,
  "message": "该标签正在被 3 个场景使用，请先移除场景中的标签配置后再删除",
  "data": null
}
```

### GET /api/tag/{id}/usage - 查询标签使用情况

**作用**：
- **不是删除前必须调用的**
- 用于用户主动查询"这个标签在哪些场景被使用"
- 删除失败后，前端可以调用获取详细信息

**成功响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "tagId": 1,
    "sceneCount": 3,
    "scenes": [
      { "sceneId": 10, "sceneName": "薄弱知识点推荐" },
      { "sceneId": 11, "sceneName": "错题专项训练" },
      { "sceneId": 12, "sceneName": "知识点巩固" }
    ]
  }
}
```

## 前端实现示例

### 方案1：简单提示（推荐）

```javascript
async function handleDeleteTag(tagId) {
  try {
    await fetch(`/api/tag/${tagId}`, { method: 'DELETE' });
    message.success('删除成功');
    loadTags(); // 刷新列表
  } catch (error) {
    // 后端返回错误信息
    message.error(error.message);
    // 错误信息：该标签正在被 3 个场景使用，请先移除场景中的标签配置后再删除
  }
}
```

### 方案2：显示详细信息

```javascript
async function handleDeleteTag(tagId) {
  try {
    await fetch(`/api/tag/${tagId}`, { method: 'DELETE' });
    message.success('删除成功');
    loadTags();
  } catch (error) {
    if (error.message.includes('正在被使用')) {
      // 删除失败，获取详细的使用情况
      const usageResponse = await fetch(`/api/tag/${tagId}/usage`);
      const usageData = await usageResponse.json();

      // 显示模态框，列出使用该标签的场景
      Modal.error({
        title: '无法删除标签',
        width: 500,
        content: (
          <div>
            <p>该标签被以下场景使用，请先移除场景中的标签配置：</p>
            <ul>
              {usageData.data.scenes.map(scene => (
                <li key={scene.sceneId}>
                  <a onClick={() => navigateToScene(scene.sceneId)}>
                    {scene.sceneName}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        )
      });
    } else {
      message.error(error.message);
    }
  }
}
```

### 方案3：禁用删除按钮（最佳用户体验）

```javascript
// 在标签列表中，标记哪些标签正在被使用
async function loadTags() {
  const response = await fetch(`/api/tag/list/${engineId}`);
  const tags = await response.json();

  // 为每个标签添加使用情况标记
  const tagsWithUsage = await Promise.all(
    tags.data.map(async (tag) => {
      const usageResponse = await fetch(`/api/tag/${tag.id}/usage`);
      const usageData = await usageResponse.json();
      return {
        ...tag,
        isUsed: usageData.data.sceneCount > 0,
        usageCount: usageData.data.sceneCount
      };
    })
  );

  setTags(tagsWithUsage);
}

// 渲染标签列表
<Table dataSource={tags}>
  <Column title="标签名称" dataIndex="name" />
  <Column title="说明" dataIndex="description" />
  <Column
    title="使用情况"
    render={(_, record) => (
      record.isUsed ? (
        <Tag color="blue">{record.usageCount} 个场景使用</Tag>
      ) : (
        <Tag color="gray">未使用</Tag>
      )
    )}
  />
  <Column
    title="操作"
    render={(_, record) => (
      <Space>
        <Button type="link" onClick={() => handleEdit(record)}>编辑</Button>
        <Button
          type="link"
          danger
          disabled={record.isUsed}  // 被使用的标签禁用删除按钮
          onClick={() => handleDelete(record.id)}
        >
          删除
        </Button>
        {record.isUsed && (
          <Tooltip title="请先移除场景中的标签配置">
            <InfoCircleOutlined />
          </Tooltip>
        )}
      </Space>
    )}
  />
</Table>
```

## 优势

### ✅ 数据一致性
- 不会意外破坏场景配置
- 场景始终引用有效的标签

### ✅ 逻辑清晰
- 强制约束：必须先删除场景配置，再删除标签
- 用户清楚知道为什么不能删除

### ✅ 用户体验
- 错误提示明确，告诉用户该怎么做
- 可选：显示详细的使用情况，方便用户处理

## 其他相关操作

### 移除场景中的标签配置

用户需要到场景编辑页面，重新配置场景标签：

```javascript
// 重新配置场景标签（不包含要删除的标签）
POST /api/scene/{sceneId}/tags
[
  { "tagId": 2, "enabled": 1, "weightCoefficient": 5 },
  { "tagId": 3, "enabled": 1, "weightCoefficient": 8 }
  // 不再包含标签1
]
```

### 批量删除标签

批量删除同样需要检查使用情况：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void batchDelete(Long engineId) {
    // 查询该引擎的所有标签
    LambdaQueryWrapper<TagRule> tagWrapper = new LambdaQueryWrapper<>();
    tagWrapper.eq(TagRule::getEngineId, engineId);
    List<TagRule> tags = tagRuleMapper.selectList(tagWrapper);

    if (tags.isEmpty()) {
        return;
    }

    // 检查是否有标签正在被使用
    List<Long> tagIds = tags.stream().map(TagRule::getId).collect(Collectors.toList());
    LambdaQueryWrapper<SceneTagRelation> relationWrapper = new LambdaQueryWrapper<>();
    relationWrapper.in(SceneTagRelation::getTagId, tagIds);
    Long usageCount = sceneTagRelationMapper.selectCount(relationWrapper);

    if (usageCount > 0) {
        throw new BusinessException("该引擎的标签正在被场景使用，请先移除场景中的标签配置后再删除");
    }

    // 未被使用，可以批量删除
    tagRuleMapper.delete(tagWrapper);

    // 更新引擎的标签总数
    updateEngineTagCount(engineId);
}
```

## 总结

- ✅ **删除标签前必须检查是否被使用**
- ✅ **被使用的标签不允许删除**
- ✅ **`GET /api/tag/{id}/usage` 是查询接口，不是删除前置条件**
- ✅ **用户必须先移除场景配置，再删除标签**
