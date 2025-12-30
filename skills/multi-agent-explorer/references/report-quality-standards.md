# 探索报告质量标准

> ⚠️ **所有探索智能体必须遵守本规范**

## 核心原则：宁长勿短

```
┌─────────────────────────────────────────────────────────┐
│  探索报告的价值 = 详细程度 × 自包含程度                    │
│                                                         │
│  简短的报告 = 没有价值的报告                              │
│  详细的报告 = 可以指导后续开发的宝贵资产                   │
└─────────────────────────────────────────────────────────┘
```

---

## 最低字数要求

| 报告类型 | 最低字数 | 最低代码行数 |
|---------|---------|-------------|
| Bug 修复报告 | 2000 字 | 50 行 |
| 功能开发报告 | 3000 字 | 100 行 |
| 测试探索报告 | 2500 字 | 80 行 |
| 架构审查报告 | 3000 字 | 50 行 |
| 自由探索报告 | 2000 字 | 50 行 |
| 错误报告 | 1500 字 | 30 行 |

**注意**：这是最低要求，鼓励写得更详细。

---

## 详细程度检查清单

### ✅ 每个代码变更必须包含

- [ ] **修改前的完整代码**（不是片段，是完整的方法/类）
- [ ] **修改后的完整代码**
- [ ] **修改原因**（为什么要这样改，不少于 3 句话）
- [ ] **影响分析**（这个修改会影响哪些地方）
- [ ] **替代方案**（考虑过但没采用的方案，以及为什么不采用）

### ✅ 每个测试用例必须包含

- [ ] **完整的测试代码**（包括 Given/When/Then 注释）
- [ ] **测试意图说明**（这个测试要验证什么，不少于 2 句话）
- [ ] **边界情况说明**（为什么选择这个测试数据）
- [ ] **我的判断**（建议保留/需讨论/建议丢弃）
- [ ] **判断理由**（不少于 3 句话解释为什么这样判断）

### ✅ 每个发现的问题必须包含

- [ ] **问题描述**（不少于 3 句话）
- [ ] **复现步骤**（如果适用）
- [ ] **根因分析**（不少于 5 句话）
- [ ] **相关代码**（完整的问题代码）
- [ ] **修复建议**（具体的修复方案，包含代码）
- [ ] **风险评估**（不修复会有什么后果）

### ✅ 探索日志必须包含

- [ ] **每个尝试的详细记录**
- [ ] **尝试的思路**（为什么要这样尝试）
- [ ] **尝试的结果**（成功/失败）
- [ ] **失败的原因分析**（如果失败）
- [ ] **从失败中学到的教训**

---

## 写作风格要求

### 1. 解释你的思考过程

```markdown
❌ 错误示例（太简短）：
"修复了空指针问题"

✅ 正确示例（详细解释）：
"在分析 ContactRepository.getById() 方法时，我发现当数据库中不存在对应 ID 的记录时，
DAO 层会返回 null。但是调用方 ContactDetailViewModel 在使用返回值时没有进行空值检查，
直接调用了 contact.name，这会导致 NullPointerException。

我考虑了两种修复方案：
1. 在 Repository 层抛出异常 - 这样可以强制调用方处理不存在的情况
2. 在 ViewModel 层添加空值检查 - 这样更灵活但容易遗漏

最终选择方案 1，因为根据 Clean Architecture 原则，Repository 应该明确表达业务语义，
'联系人不存在'是一个业务异常，应该在 Repository 层就明确抛出，而不是让调用方猜测 null 的含义。"
```

### 2. 展示你的分析过程

```markdown
❌ 错误示例（只有结论）：
"测试覆盖率不足"

✅ 正确示例（展示分析）：
"我对 domain 模块的测试覆盖率进行了详细分析：

当前状态：
- 总文件数：148 个
- 有测试的文件：28 个
- 测试覆盖率：18.9%

未覆盖的关键代码：
1. AnalyzeChatUseCase - 核心业务逻辑，但只有 2 个测试用例，缺少边界情况测试
2. PolishDraftUseCase - 完全没有测试
3. GenerateReplyUseCase - 只测试了正常情况，没有测试异常情况

风险评估：
- AnalyzeChatUseCase 是用户最常用的功能，缺少测试可能导致回归 Bug
- PolishDraftUseCase 涉及 AI 调用，没有测试很难保证稳定性

建议优先级：
1. 【高】为 PolishDraftUseCase 添加基础测试
2. 【高】为 AnalyzeChatUseCase 添加边界情况测试
3. 【中】为 GenerateReplyUseCase 添加异常情况测试"
```

### 3. 记录所有尝试，包括失败的

```markdown
❌ 错误示例（只记录成功）：
"使用方案 A 修复了问题"

✅ 正确示例（记录所有尝试）：
"## 尝试记录

### 尝试 1：在 ViewModel 层添加空值检查
**思路**：最简单的修复方式，直接在使用处检查
**代码**：
```kotlin
val contact = repository.getById(id)
if (contact == null) {
    _uiState.update { it.copy(error = "联系人不存在") }
    return
}
```
**结果**：❌ 失败
**原因**：这样修复后，其他调用 getById 的地方也需要添加同样的检查，容易遗漏
**教训**：应该在更底层统一处理，而不是在每个调用处重复

### 尝试 2：在 Repository 层返回 Result<Contact>
**思路**：使用 Result 包装返回值，强制调用方处理
**代码**：
```kotlin
suspend fun getById(id: String): Result<Contact> {
    val contact = dao.getById(id)
    return if (contact != null) {
        Result.success(contact)
    } else {
        Result.failure(NotFoundException("Contact not found: $id"))
    }
}
```
**结果**：✅ 成功
**原因**：符合项目规范（所有 UseCase 返回 Result），调用方必须处理失败情况
**额外收益**：统一了错误处理方式，代码更一致"
```

---

## 代码嵌入规范

### 1. 嵌入完整的方法，不是片段

```markdown
❌ 错误示例（只有片段）：
```kotlin
return dao.getById(id) ?: throw NotFoundException()
```

✅ 正确示例（完整方法）：
```kotlin
// file: data/src/main/kotlin/com/empathy/ai/data/repository/ContactRepositoryImpl.kt
// 修改前
class ContactRepositoryImpl @Inject constructor(
    private val dao: ContactDao
) : ContactRepository {
    
    override suspend fun getById(id: String): Contact? {
        return dao.getById(id)
    }
}

// 修改后
class ContactRepositoryImpl @Inject constructor(
    private val dao: ContactDao
) : ContactRepository {
    
    override suspend fun getById(id: String): Contact {
        return dao.getById(id)
            ?: throw NotFoundException("Contact not found: $id")
    }
}
```
```

### 2. 包含必要的上下文

```markdown
❌ 错误示例（缺少上下文）：
```kotlin
@Test
fun test1() {
    assertEquals(null, result)
}
```

✅ 正确示例（包含上下文）：
```kotlin
// file: domain/src/test/kotlin/com/empathy/ai/domain/usecase/GetContactUseCaseTest.kt
// 测试意图：验证当联系人 ID 不存在时，UseCase 应该返回 Result.failure

class GetContactUseCaseTest {
    
    private lateinit var useCase: GetContactUseCase
    private lateinit var repository: ContactRepository
    
    @Before
    fun setup() {
        repository = mockk()
        useCase = GetContactUseCase(repository)
    }
    
    @Test
    fun `getContact_whenIdNotFound_returnsFailure`() {
        // Given: 数据库中不存在 ID 为 "non-existent" 的联系人
        val nonExistentId = "non-existent"
        coEvery { repository.getById(nonExistentId) } throws NotFoundException("Contact not found")
        
        // When: 调用 UseCase 获取联系人
        val result = runBlocking { useCase(nonExistentId) }
        
        // Then: 应该返回 failure 结果
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NotFoundException)
    }
}
```
```

---

## 报告结构要求

### 必须包含的章节

1. **基本信息** - 日期、分支、状态、探索者
2. **探索目标** - 要解决什么问题/实现什么功能
3. **探索过程** - 详细的尝试记录（包括失败的）
4. **发现与结论** - 发现了什么问题/实现了什么功能
5. **代码变更** - 完整的代码（修改前+修改后）
6. **测试情况** - 测试代码和结果
7. **成果清单** - 分类列出所有成果
8. **合并建议** - 建议合并/不建议合并，以及理由
9. **后续工作** - 还有什么需要做的

### 每个章节的最低内容要求

| 章节 | 最低要求 |
|------|---------|
| 基本信息 | 完整填写所有字段 |
| 探索目标 | 不少于 100 字 |
| 探索过程 | 不少于 500 字，至少记录 3 次尝试 |
| 发现与结论 | 不少于 300 字 |
| 代码变更 | 每个变更包含完整的前后代码 |
| 测试情况 | 每个测试包含完整代码和判断理由 |
| 成果清单 | 按 A/B/C/D 类分类列出 |
| 合并建议 | 不少于 100 字，包含具体理由 |
| 后续工作 | 至少列出 2 项 |

---

## 质量检查

### 报告完成前的自检清单

在提交报告前，智能体必须自检：

```markdown
## 报告质量自检

### 字数检查
- [ ] 总字数达到最低要求（____字，要求____字）
- [ ] 代码行数达到最低要求（____行，要求____行）

### 内容完整性
- [ ] 所有必须章节都已填写
- [ ] 每个代码变更都包含修改前+修改后
- [ ] 每个测试都包含完整代码和判断理由
- [ ] 探索过程记录了所有尝试（包括失败的）

### 自包含检查
- [ ] 删除分支后，仅凭报告能完全理解所有内容
- [ ] 所有代码片段都是完整的（不是片段）
- [ ] 所有代码都标注了文件路径

### 可操作性检查
- [ ] 成果清单按 A/B/C/D 类分类
- [ ] 每个成果都有明确的合并建议
- [ ] 后续工作有具体的行动项
```

---

## 示例：高质量报告 vs 低质量报告

### 低质量报告（❌ 不合格）

```markdown
# Bug 修复报告

修复了 ContactRepository 的空值问题。

## 修改
在 getById 方法添加了空值检查。

## 测试
添加了一个测试。

## 建议
建议合并。
```

**问题**：
- 没有代码
- 没有分析过程
- 没有解释原因
- 无法指导后续开发

### 高质量报告（✅ 合格）

```markdown
# Bug 修复探索报告

## 基本信息
| 项目 | 内容 |
|------|------|
| 日期 | 2024-12-30 |
| 分支 | explore/bugfix-20241230-contact-npe |
| 状态 | ✅ 可合并 |
| 探索者 | bugfix-explorer |

## 问题描述

### Bug 现象
用户在联系人详情页面点击一个已删除的联系人时，应用崩溃。

### 复现步骤
1. 打开联系人列表
2. 在另一个设备上删除某个联系人
3. 在当前设备上点击该联系人
4. 应用崩溃

### 错误日志
```
java.lang.NullPointerException: contact.name must not be null
    at com.empathy.ai.presentation.viewmodel.ContactDetailViewModel.loadContact(ContactDetailViewModel.kt:45)
    at com.empathy.ai.presentation.viewmodel.ContactDetailViewModel.init(ContactDetailViewModel.kt:30)
```

## 根因分析

### 问题定位过程
1. 首先查看崩溃堆栈，定位到 ContactDetailViewModel.loadContact() 方法
2. 分析代码发现 repository.getById() 返回 Contact?，但 ViewModel 直接使用了 contact.name
3. 追溯到 ContactRepositoryImpl，发现 DAO 返回 null 时没有处理

### 根本原因
ContactRepositoryImpl.getById() 方法在联系人不存在时返回 null，但：
1. 方法签名是 `Contact?`，调用方容易忽略空值检查
2. ViewModel 层没有处理 null 情况
3. 缺少统一的"联系人不存在"错误处理机制

### 问题代码
```kotlin
// file: data/src/main/kotlin/com/empathy/ai/data/repository/ContactRepositoryImpl.kt
class ContactRepositoryImpl @Inject constructor(
    private val dao: ContactDao
) : ContactRepository {
    
    override suspend fun getById(id: String): Contact? {
        return dao.getById(id)?.toDomain()  // 问题：返回 null 时调用方可能不处理
    }
}
```

```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/viewmodel/ContactDetailViewModel.kt
@HiltViewModel
class ContactDetailViewModel @Inject constructor(
    private val repository: ContactRepository
) : ViewModel() {
    
    fun loadContact(id: String) {
        viewModelScope.launch {
            val contact = repository.getById(id)
            _uiState.update { 
                it.copy(name = contact.name)  // 问题：contact 可能为 null
            }
        }
    }
}
```

## 探索过程

### 尝试 1：在 ViewModel 层添加空值检查
**思路**：最简单的修复方式
**代码**：
```kotlin
fun loadContact(id: String) {
    viewModelScope.launch {
        val contact = repository.getById(id)
        if (contact == null) {
            _uiState.update { it.copy(error = "联系人不存在") }
            return@launch
        }
        _uiState.update { it.copy(name = contact.name) }
    }
}
```
**结果**：❌ 不采用
**原因**：
1. 其他调用 getById 的地方也需要同样的检查，容易遗漏
2. 不符合项目规范（UseCase 应该返回 Result）
3. 错误处理逻辑分散在各个 ViewModel 中

### 尝试 2：在 Repository 层抛出异常
**思路**：在数据层统一处理，强制调用方处理异常
**代码**：
```kotlin
override suspend fun getById(id: String): Contact {
    return dao.getById(id)?.toDomain()
        ?: throw NotFoundException("Contact not found: $id")
}
```
**结果**：⚠️ 部分采用
**原因**：
1. 统一了错误处理
2. 但直接抛异常不符合项目规范（应该返回 Result）

### 尝试 3：Repository 返回 Result<Contact>（最终方案）
**思路**：符合项目规范，使用 Result 包装返回值
**代码**：见下方修复方案
**结果**：✅ 采用
**原因**：
1. 符合项目的 Clean Architecture 规范
2. 强制调用方处理失败情况
3. 错误信息可以携带更多上下文

## 修复方案

### 修改 1：ContactRepository 接口
```kotlin
// file: domain/src/main/kotlin/com/empathy/ai/domain/repository/ContactRepository.kt
// 修改前
interface ContactRepository {
    suspend fun getById(id: String): Contact?
}

// 修改后
interface ContactRepository {
    suspend fun getById(id: String): Result<Contact>
}
```

**修改原因**：
使用 Result 包装返回值，明确表达"获取联系人可能失败"的语义。
调用方必须处理 Result.failure 情况，避免遗漏空值检查。

### 修改 2：ContactRepositoryImpl 实现
```kotlin
// file: data/src/main/kotlin/com/empathy/ai/data/repository/ContactRepositoryImpl.kt
// 修改前
class ContactRepositoryImpl @Inject constructor(
    private val dao: ContactDao
) : ContactRepository {
    
    override suspend fun getById(id: String): Contact? {
        return dao.getById(id)?.toDomain()
    }
}

// 修改后
class ContactRepositoryImpl @Inject constructor(
    private val dao: ContactDao
) : ContactRepository {
    
    override suspend fun getById(id: String): Result<Contact> {
        return try {
            val entity = dao.getById(id)
            if (entity != null) {
                Result.success(entity.toDomain())
            } else {
                Result.failure(NotFoundException("Contact not found: $id"))
            }
        } catch (e: Exception) {
            Result.failure(DatabaseException("Failed to get contact: ${e.message}", e))
        }
    }
}
```

**修改原因**：
1. 返回 Result 类型，符合项目规范
2. 区分"联系人不存在"和"数据库错误"两种失败情况
3. 捕获可能的数据库异常，避免崩溃

### 修改 3：ContactDetailViewModel 调用处
```kotlin
// file: presentation/src/main/kotlin/com/empathy/ai/presentation/viewmodel/ContactDetailViewModel.kt
// 修改前
fun loadContact(id: String) {
    viewModelScope.launch {
        val contact = repository.getById(id)
        _uiState.update { it.copy(name = contact.name) }
    }
}

// 修改后
fun loadContact(id: String) {
    viewModelScope.launch {
        repository.getById(id)
            .onSuccess { contact ->
                _uiState.update { it.copy(name = contact.name, error = null) }
            }
            .onFailure { error ->
                _uiState.update { it.copy(error = error.message) }
            }
    }
}
```

**修改原因**：
使用 Result 的 onSuccess/onFailure 处理成功和失败情况，
代码更清晰，且编译器会强制处理两种情况。

## 测试情况

### 新增测试 1：联系人不存在时返回 failure
**测试意图**：验证当数据库中不存在对应 ID 的联系人时，Repository 应该返回 Result.failure

**我的判断**：✅ 建议保留

**判断理由**：
这个测试验证的是接口契约（Repository 在联系人不存在时应该返回 failure），
而不是实现细节。这是核心业务逻辑，必须有测试覆盖。

**完整测试代码**：
```kotlin
// file: data/src/test/kotlin/com/empathy/ai/data/repository/ContactRepositoryImplTest.kt
class ContactRepositoryImplTest {
    
    private lateinit var repository: ContactRepositoryImpl
    private lateinit var dao: ContactDao
    
    @Before
    fun setup() {
        dao = mockk()
        repository = ContactRepositoryImpl(dao)
    }
    
    @Test
    fun `getById_whenContactNotFound_returnsFailure`() = runTest {
        // Given: 数据库中不存在 ID 为 "non-existent" 的联系人
        val nonExistentId = "non-existent"
        coEvery { dao.getById(nonExistentId) } returns null
        
        // When: 调用 Repository 获取联系人
        val result = repository.getById(nonExistentId)
        
        // Then: 应该返回 failure 结果，且异常类型为 NotFoundException
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is NotFoundException)
        assertEquals("Contact not found: non-existent", exception?.message)
    }
}
```

### 新增测试 2：数据库异常时返回 failure
**测试意图**：验证当数据库操作抛出异常时，Repository 应该捕获并返回 Result.failure

**我的判断**：✅ 建议保留

**判断理由**：
这个测试验证了异常处理逻辑，确保数据库错误不会导致应用崩溃，
而是被优雅地转换为 Result.failure。

**完整测试代码**：
```kotlin
// file: data/src/test/kotlin/com/empathy/ai/data/repository/ContactRepositoryImplTest.kt
@Test
fun `getById_whenDatabaseError_returnsFailure`() = runTest {
    // Given: 数据库操作抛出异常
    val contactId = "test-id"
    coEvery { dao.getById(contactId) } throws SQLiteException("Database locked")
    
    // When: 调用 Repository 获取联系人
    val result = repository.getById(contactId)
    
    // Then: 应该返回 failure 结果，且异常类型为 DatabaseException
    assertTrue(result.isFailure)
    val exception = result.exceptionOrNull()
    assertTrue(exception is DatabaseException)
    assertTrue(exception?.message?.contains("Database locked") == true)
}
```

## 成果清单

### A类：文档报告（直接合并）
- [x] 本报告：BUGFIX-20241230-contact-npe.md

### B类：测试用例（需审查）
| 测试文件 | 新增数量 | 我的判断 | 理由 |
|---------|---------|---------|------|
| ContactRepositoryImplTest.kt | 2 | ✅ 建议保留 | 测试的是接口契约 |

### C类：Bug 修复（需审查）
| 文件 | 修改内容 | 风险等级 |
|-----|---------|---------|
| ContactRepository.kt | 返回类型改为 Result | 中（接口变更） |
| ContactRepositoryImpl.kt | 实现 Result 返回 | 低 |
| ContactDetailViewModel.kt | 处理 Result | 低 |

### D类：代码重构（需详细Review）
- 无

## 合并建议

**建议合并** ✅

**理由**：
1. 修复了一个会导致应用崩溃的严重 Bug
2. 修复方案符合项目的 Clean Architecture 规范
3. 添加了完整的测试覆盖
4. 代码变更范围可控，风险较低

**注意事项**：
1. 需要检查其他调用 ContactRepository.getById() 的地方，确保都处理了 Result
2. 建议在合并后运行完整的单元测试套件

## 后续工作

1. 【高优先级】检查其他 Repository 方法是否有类似的空值问题
2. 【中优先级】为 ContactRepository 的其他方法添加类似的测试
3. 【低优先级】考虑创建统一的 RepositoryException 基类

## 报告质量自检

### 字数检查
- [x] 总字数达到最低要求（约 3500 字，要求 2000 字）
- [x] 代码行数达到最低要求（约 150 行，要求 50 行）

### 内容完整性
- [x] 所有必须章节都已填写
- [x] 每个代码变更都包含修改前+修改后
- [x] 每个测试都包含完整代码和判断理由
- [x] 探索过程记录了所有尝试（包括失败的）

### 自包含检查
- [x] 删除分支后，仅凭报告能完全理解所有内容
- [x] 所有代码片段都是完整的（不是片段）
- [x] 所有代码都标注了文件路径
```

---

## 总结

**记住**：探索报告是探索工作的最终产出，它的价值远大于分支里的代码。

写报告时问自己：
1. 如果分支被删除，仅凭这份报告，后续的 AI 能完全理解我做了什么吗？
2. 如果我是审查者，这份报告能帮我快速做出合并决策吗？
3. 如果需要重新实现，这份报告能提供足够的指导吗？

如果答案是"不能"，那就继续写，直到答案变成"能"。
