# 接口设计说明 - 分页 vs 不分页

## 设计原则

根据实际使用场景，我们提供了两套接口：

### 1. 分页接口（用于列表页）
- **使用场景**：引擎列表页、数据量大的情况
- **特点**：支持分页、排序、筛选
- **返回格式**：`Page<T>` 包含总数、当前页数据等

### 2. 不分页接口（用于编辑页）
- **使用场景**：编辑页面、下拉选择、数据量可控的情况
- **特点**：返回全部数据、性能更好（单次查询）
- **返回格式**：`List<T>` 简单列表

## API 接口对照表

### 标签管理

| 场景 | 接口 | 方法 | 说明 |
|------|------|------|------|
| **列表页** | `/api/tag/page/{engineId}` | GET | 分页查询标签（支持大数据量） |
| **编辑页 Tab 2** | `/api/tag/list/{engineId}` | GET | 获取所有标签（用于展示标签列表） |
| **编辑页 Tab 3** | `/api/tag/list/{engineId}` | GET | 获取所有标签（用于场景配置时选择） |

```javascript
// Tab 2: 标签规则列表（使用不分页接口）
async function loadTagsForEdit() {
  const response = await fetch(`/api/tag/list/${engineId}`);
  const data = await response.json();
  setTags(data.data); // 直接是数组，无需解构 data.records
}

// Tab 3: 场景配置 - 选择标签（使用不分页接口）
async function loadTagsForSelection() {
  const response = await fetch(`/api/tag/list/${engineId}`);
  const data = await response.json();
  setAvailableTags(data.data); // 所有可用标签
}
```

### 场景管理

| 场景 | 接口 | 方法 | 说明 |
|------|------|------|------|
| **列表页** | `/api/scene/page/{engineId}` | GET | 分页查询场景（支持大数据量） |
| **编辑页 Tab 3** | `/api/scene/list/{engineId}` | GET | 获取所有场景（用于展示场景列表） |

```javascript
// Tab 3: 场景策略列表（使用不分页接口）
async function loadScenesForEdit() {
  const response = await fetch(`/api/scene/list/${engineId}`);
  const data = await response.json();
  setScenes(data.data); // 直接是数组，包含每个场景的标签配置
}
```

### 引擎管理

| 场景 | 接口 | 方法 | 说明 |
|------|------|------|------|
| **列表页** | `/api/engine/page` | GET | 分页查询引擎（支持筛选、排序） |
| **编辑页 Tab 1** | `/api/engine/{id}` | GET | 获取单个引擎详情 |

## 为什么要区分分页和不分页？

### 编辑页不需要分页的原因

1. **数据量可控**
   - 一个引擎的标签通常不超过 50 个
   - 场景策略通常不超过 20 个
   - 不会造成性能问题

2. **用户体验更好**
   - 一次性加载所有数据，无需翻页
   - 方便搜索和筛选（前端实现）
   - 减少请求次数

3. **业务逻辑需要**
   - Tab 3 配置场景标签时，需要看到所有可用标签
   - 用户需要全局视图来理解配置关系

### 列表页需要分页的原因

1. **数据量不可控**
   - 系统中可能有上百个引擎
   - 分页避免一次性加载大量数据

2. **性能优化**
   - 减少数据库查询量
   - 降低网络传输成本
   - 提升页面响应速度

3. **符合列表页规范**
   - 支持排序、筛选
   - 显示总数、跳转页码等功能

## 接口返回格式对比

### 分页接口返回格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      { "id": 1, "name": "标签1" },
      { "id": 2, "name": "标签2" }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

### 不分页接口返回格式

```json
{
  "code": 200,
  "message": "success",
  "data": [
    { "id": 1, "name": "标签1" },
    { "id": 2, "name": "标签2" },
    { "id": 3, "name": "标签3" }
  ]
}
```

## 前端使用示例

### 场景1: 编辑页加载数据

```javascript
// 引擎编辑页组件
function EngineEditPage({ engineId }) {
  const [tags, setTags] = useState([]);
  const [scenes, setScenes] = useState([]);

  // 加载所有标签（不分页）
  useEffect(() => {
    async function loadTags() {
      const response = await fetch(`/api/tag/list/${engineId}`);
      const data = await response.json();
      setTags(data.data); // 直接使用数组
    }
    loadTags();
  }, [engineId]);

  // 加载所有场景（不分页）
  useEffect(() => {
    async function loadScenes() {
      const response = await fetch(`/api/scene/list/${engineId}`);
      const data = await response.json();
      setScenes(data.data); // 直接使用数组
    }
    loadScenes();
  }, [engineId]);

  return (
    <Tabs>
      <TabPane key="1" tab="基本信息">
        <BasicInfoForm engineId={engineId} />
      </TabPane>
      <TabPane key="2" tab="标签规则">
        <TagList tags={tags} onRefresh={loadTags} />
      </TabPane>
      <TabPane key="3" tab="场景策略">
        <SceneList
          scenes={scenes}
          availableTags={tags}
          onRefresh={loadScenes}
        />
      </TabPane>
    </Tabs>
  );
}
```

### 场景2: 列表页加载数据

```javascript
// 引擎列表页组件
function EngineListPage() {
  const [pageData, setPageData] = useState({
    records: [],
    total: 0,
    current: 1,
    size: 10
  });

  // 加载分页数据
  async function loadPage(pageNum, pageSize) {
    const response = await fetch(
      `/api/engine/page?pageNum=${pageNum}&pageSize=${pageSize}`
    );
    const data = await response.json();
    setPageData(data.data); // Page 对象
  }

  useEffect(() => {
    loadPage(1, 10);
  }, []);

  return (
    <div>
      <Table
        dataSource={pageData.records}
        pagination={{
          total: pageData.total,
          current: pageData.current,
          pageSize: pageData.size,
          onChange: (page, size) => loadPage(page, size)
        }}
      />
    </div>
  );
}
```

## 性能对比

### 假设数据量

- 引擎数量：100 个
- 每个引擎的标签：平均 20 个
- 每个引擎的场景：平均 10 个

### 列表页访问（使用分页）

```
请求：GET /api/engine/page?pageNum=1&pageSize=10
返回：10 个引擎
数据大小：~5KB
```

### 编辑页访问（使用不分页）

```
请求1：GET /api/tag/list/{engineId}
返回：20 个标签
数据大小：~2KB

请求2：GET /api/scene/list/{engineId}
返回：10 个场景（每个场景包含标签配置）
数据大小：~3KB

总计：2 个请求，~5KB
```

### 如果编辑页也用分页（不推荐）

```
请求1：GET /api/tag/page/{engineId}?pageNum=1&pageSize=10
返回：前 10 个标签
问题：需要翻页才能看到全部标签

请求2：GET /api/scene/page/{engineId}?pageNum=1&pageSize=10
返回：前 10 个场景
问题：配置场景时看不到全部标签选项

总计：至少 2 个请求，可能需要多次翻页
用户体验：差
```

## 总结

### ✅ 推荐做法

- **列表页**：使用分页接口 `/page/{engineId}`
- **编辑页**：使用不分页接口 `/list/{engineId}`

### ✅ 优势

1. **性能优化**：列表页分页避免大数据量加载
2. **用户体验**：编辑页一次性加载，无需翻页
3. **开发便捷**：前端处理简单，逻辑清晰

### ❌ 不要这样做

- 编辑页使用分页接口 → 用户需要翻页查看标签
- 列表页使用不分页接口 → 数据量大时性能差

## 后端实现要点

### 不分页接口的性能优化

```java
@Override
public List<TagRuleVO> listByEngineId(Long engineId) {
    LambdaQueryWrapper<TagRule> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(TagRule::getEngineId, engineId)
            .eq(TagRule::getStatus, 1) // 只返回启用的标签
            .orderByDesc(TagRule::getCreatedTime);
            // 不设置 limit，返回所有数据

    List<TagRule> tags = tagRuleMapper.selectList(wrapper);
    return tags.stream()
            .map(tag -> BeanUtil.copyProperties(tag, TagRuleVO.class))
            .collect(Collectors.toList());
}
```

### 数据量控制建议

如果担心数据量过大，可以添加业务限制：

1. **限制标签数量**：每个引擎最多 100 个标签
2. **限制场景数量**：每个引擎最多 50 个场景
3. **添加告警**：当数据量接近上限时提醒用户

```java
@Override
public Long create(TagRuleDTO dto) {
    // 检查标签数量
    Long count = tagRuleMapper.selectCount(
        new LambdaQueryWrapper<TagRule>()
            .eq(TagRule::getEngineId, dto.getEngineId())
    );

    if (count >= 100) {
        throw new BusinessException("每个引擎最多支持100个标签");
    }

    // 创建标签
    TagRule tag = BeanUtil.copyProperties(dto, TagRule.class);
    tagRuleMapper.insert(tag);

    return tag.getId();
}
```

这样既保证了用户体验，又控制了性能风险。
