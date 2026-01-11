package com.empathy.ai.presentation.ui.screen.contact.persona

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.empathy.ai.domain.model.CategoryColor
import com.empathy.ai.domain.model.EditModeState
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactCategory
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.model.PersonaSearchState
import com.empathy.ai.presentation.theme.EmpathyTheme
import org.junit.Rule
import org.junit.Test

/**
 * PersonaFlow 端到端流程测试
 *
 * 测试标签画像V2的完整用户流程
 */
class PersonaFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testColor = CategoryColor(
        titleColor = 0xFFB71C1C,
        tagBackgroundColor = 0xFFFFCDD2,
        tagTextColor = 0xFFB71C1C
    )

    private val testColor2 = CategoryColor(
        titleColor = 0xFF1B5E20,
        tagBackgroundColor = 0xFFC8E6C9,
        tagTextColor = 0xFF1B5E20
    )

    private fun createFact(
        id: String,
        key: String,
        value: String
    ) = Fact(
        id = id,
        key = key,
        value = value,
        timestamp = System.currentTimeMillis(),
        source = FactSource.MANUAL
    )

    private fun createCategory(
        key: String,
        facts: List<Fact>,
        color: CategoryColor = testColor,
        isExpanded: Boolean = true
    ) = FactCategory(
        key = key,
        facts = facts,
        color = color,
        isExpanded = isExpanded
    )

    // ==================== 搜索过滤完整流程 ====================

    @Test
    fun 搜索过滤完整流程() {
        var searchState by mutableStateOf(PersonaSearchState())
        var categories by mutableStateOf(
            listOf(
                createCategory("性格特点", listOf(
                    createFact("1", "性格特点", "开朗"),
                    createFact("2", "性格特点", "乐观")
                )),
                createCategory("兴趣爱好", listOf(
                    createFact("3", "兴趣爱好", "读书"),
                    createFact("4", "兴趣爱好", "运动")
                ), testColor2)
            )
        )

        composeTestRule.setContent {
            EmpathyTheme {
                PersonaTabV2(
                    categories = categories,
                    searchState = searchState,
                    editModeState = EditModeState(),
                    availableCategories = emptyList(),
                    isDarkMode = false,
                    onSearchQueryChange = { query ->
                        searchState = searchState.updateQuery(query)
                        // 模拟搜索过滤
                        categories = if (query.isBlank()) {
                            listOf(
                                createCategory("性格特点", listOf(
                                    createFact("1", "性格特点", "开朗"),
                                    createFact("2", "性格特点", "乐观")
                                )),
                                createCategory("兴趣爱好", listOf(
                                    createFact("3", "兴趣爱好", "读书"),
                                    createFact("4", "兴趣爱好", "运动")
                                ), testColor2)
                            )
                        } else {
                            listOf(
                                createCategory("性格特点", listOf(
                                    createFact("1", "性格特点", "开朗")
                                ))
                            )
                        }
                    },
                    onClearSearch = {
                        searchState = PersonaSearchState()
                        categories = listOf(
                            createCategory("性格特点", listOf(
                                createFact("1", "性格特点", "开朗"),
                                createFact("2", "性格特点", "乐观")
                            )),
                            createCategory("兴趣爱好", listOf(
                                createFact("3", "兴趣爱好", "读书"),
                                createFact("4", "兴趣爱好", "运动")
                            ), testColor2)
                        )
                    },
                    onToggleCategoryExpand = {},
                    onFactClick = {},
                    onFactLongClick = {},
                    onToggleFactSelection = {},
                    onExitEditMode = {},
                    onSelectAll = {},
                    onDeselectAll = {},
                    onShowDeleteConfirm = {},
                    onShowMoveDialog = {},
                    onConfirmDelete = {},
                    onHideDeleteConfirm = {},
                    onConfirmMove = {},
                    onHideMoveDialog = {}
                )
            }
        }

        // 初始状态：显示所有分类
        composeTestRule.onNodeWithText("性格特点").assertIsDisplayed()
        composeTestRule.onNodeWithText("兴趣爱好").assertIsDisplayed()
        composeTestRule.onNodeWithText("开朗").assertIsDisplayed()
        composeTestRule.onNodeWithText("读书").assertIsDisplayed()

        // 输入搜索关键词
        composeTestRule.onNodeWithText("搜索标签或分类...").performTextInput("开朗")

        // 验证过滤结果
        composeTestRule.onNodeWithText("性格特点").assertIsDisplayed()
        composeTestRule.onNodeWithText("开朗").assertIsDisplayed()
        // 其他分类和标签应该被过滤掉
        composeTestRule.onNodeWithText("兴趣爱好").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("读书").assertIsNotDisplayed()
    }

    // ==================== 编辑模式批量删除流程 ====================

    @Test
    fun 编辑模式批量删除完整流程() {
        var editModeState by mutableStateOf(EditModeState())
        var showDeleteConfirm by mutableStateOf(false)
        var deletedFactIds: List<String> = emptyList()

        val categories = listOf(
            createCategory("性格特点", listOf(
                createFact("1", "性格特点", "开朗"),
                createFact("2", "性格特点", "乐观")
            ))
        )

        composeTestRule.setContent {
            EmpathyTheme {
                PersonaTabV2(
                    categories = categories,
                    searchState = PersonaSearchState(),
                    editModeState = editModeState,
                    availableCategories = emptyList(),
                    isDarkMode = false,
                    onSearchQueryChange = {},
                    onClearSearch = {},
                    onToggleCategoryExpand = {},
                    onFactClick = {},
                    onFactLongClick = { factId ->
                        editModeState = EditModeState.activated(factId)
                    },
                    onToggleFactSelection = { factId ->
                        editModeState = editModeState.toggleSelection(factId)
                    },
                    onExitEditMode = {
                        editModeState = EditModeState()
                    },
                    onSelectAll = {},
                    onDeselectAll = {},
                    onShowDeleteConfirm = {
                        showDeleteConfirm = true
                        editModeState = editModeState.showDeleteConfirmDialog()
                    },
                    onShowMoveDialog = {},
                    onConfirmDelete = {
                        deletedFactIds = editModeState.selectedFactIds.toList()
                        editModeState = EditModeState()
                        showDeleteConfirm = false
                    },
                    onHideDeleteConfirm = {
                        showDeleteConfirm = false
                        editModeState = editModeState.hideDeleteConfirmDialog()
                    },
                    onConfirmMove = {},
                    onHideMoveDialog = {}
                )

                if (showDeleteConfirm) {
                    BatchDeleteConfirmDialog(
                        selectedCount = editModeState.selectedCount,
                        onConfirm = {
                            deletedFactIds = editModeState.selectedFactIds.toList()
                            editModeState = EditModeState()
                            showDeleteConfirm = false
                        },
                        onDismiss = {
                            showDeleteConfirm = false
                            editModeState = editModeState.hideDeleteConfirmDialog()
                        }
                    )
                }
            }
        }

        // 步骤1：长按标签进入编辑模式
        composeTestRule.onNodeWithText("开朗").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("已选择 1 项").assertIsDisplayed()

        // 步骤2：选择更多标签
        composeTestRule.onNodeWithText("乐观").performClick()
        composeTestRule.onNodeWithText("已选择 2 项").assertIsDisplayed()

        // 步骤3：点击删除按钮
        composeTestRule.onNodeWithText("删除 (2)").performClick()

        // 步骤4：确认删除对话框显示
        composeTestRule.onNodeWithText("确认删除").assertIsDisplayed()
        composeTestRule.onNodeWithText("确定要删除选中的 2 个标签吗？此操作不可撤销。").assertIsDisplayed()

        // 步骤5：确认删除
        composeTestRule.onNodeWithText("删除").performClick()

        // 验证删除结果
        assert(deletedFactIds.contains("1"))
        assert(deletedFactIds.contains("2"))
    }

    // ==================== 编辑模式批量移动流程 ====================

    @Test
    fun 编辑模式批量移动完整流程() {
        var editModeState by mutableStateOf(EditModeState())
        var showMoveDialog by mutableStateOf(false)
        var movedToCategory: String? = null

        val categories = listOf(
            createCategory("性格特点", listOf(
                createFact("1", "性格特点", "开朗")
            )),
            createCategory("兴趣爱好", listOf(
                createFact("2", "兴趣爱好", "读书")
            ), testColor2)
        )

        composeTestRule.setContent {
            EmpathyTheme {
                PersonaTabV2(
                    categories = categories,
                    searchState = PersonaSearchState(),
                    editModeState = editModeState,
                    availableCategories = listOf("性格特点", "兴趣爱好"),
                    isDarkMode = false,
                    onSearchQueryChange = {},
                    onClearSearch = {},
                    onToggleCategoryExpand = {},
                    onFactClick = {},
                    onFactLongClick = { factId ->
                        editModeState = EditModeState.activated(factId)
                    },
                    onToggleFactSelection = { factId ->
                        editModeState = editModeState.toggleSelection(factId)
                    },
                    onExitEditMode = {
                        editModeState = EditModeState()
                    },
                    onSelectAll = {},
                    onDeselectAll = {},
                    onShowDeleteConfirm = {},
                    onShowMoveDialog = {
                        showMoveDialog = true
                        editModeState = editModeState.showMoveCategoryDialog()
                    },
                    onConfirmDelete = {},
                    onHideDeleteConfirm = {},
                    onConfirmMove = { targetCategory ->
                        movedToCategory = targetCategory
                        editModeState = EditModeState()
                        showMoveDialog = false
                    },
                    onHideMoveDialog = {
                        showMoveDialog = false
                        editModeState = editModeState.hideMoveCategoryDialog()
                    }
                )

                if (showMoveDialog) {
                    MoveCategoryDialog(
                        selectedCount = editModeState.selectedCount,
                        existingCategories = listOf("性格特点", "兴趣爱好"),
                        onConfirm = { targetCategory ->
                            movedToCategory = targetCategory
                            editModeState = EditModeState()
                            showMoveDialog = false
                        },
                        onDismiss = {
                            showMoveDialog = false
                            editModeState = editModeState.hideMoveCategoryDialog()
                        }
                    )
                }
            }
        }

        // 步骤1：长按标签进入编辑模式
        composeTestRule.onNodeWithText("开朗").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("已选择 1 项").assertIsDisplayed()

        // 步骤2：点击移动按钮
        composeTestRule.onNodeWithText("移动分类").performClick()

        // 步骤3：移动对话框显示
        composeTestRule.onNodeWithText("移动 1 个标签到").assertIsDisplayed()

        // 步骤4：选择目标分类
        composeTestRule.onNodeWithText("兴趣爱好").performClick()

        // 步骤5：确认移动
        composeTestRule.onNodeWithText("确认移动").performClick()

        // 验证移动结果
        assert(movedToCategory == "兴趣爱好")
    }

    // ==================== 分类折叠展开流程 ====================

    @Test
    fun 分类折叠展开完整流程() {
        var categories by mutableStateOf(
            listOf(
                createCategory("性格特点", listOf(
                    createFact("1", "性格特点", "开朗")
                ), isExpanded = true)
            )
        )

        composeTestRule.setContent {
            EmpathyTheme {
                PersonaTabV2(
                    categories = categories,
                    searchState = PersonaSearchState(),
                    editModeState = EditModeState(),
                    availableCategories = emptyList(),
                    isDarkMode = false,
                    onSearchQueryChange = {},
                    onClearSearch = {},
                    onToggleCategoryExpand = { categoryKey ->
                        categories = categories.map { category ->
                            if (category.key == categoryKey) {
                                category.toggleExpanded()
                            } else {
                                category
                            }
                        }
                    },
                    onFactClick = {},
                    onFactLongClick = {},
                    onToggleFactSelection = {},
                    onExitEditMode = {},
                    onSelectAll = {},
                    onDeselectAll = {},
                    onShowDeleteConfirm = {},
                    onShowMoveDialog = {},
                    onConfirmDelete = {},
                    onHideDeleteConfirm = {},
                    onConfirmMove = {},
                    onHideMoveDialog = {}
                )
            }
        }

        // 初始状态：展开，标签可见
        composeTestRule.onNodeWithText("性格特点").assertIsDisplayed()
        composeTestRule.onNodeWithText("开朗").assertIsDisplayed()

        // 点击分类标题折叠
        composeTestRule.onNodeWithText("性格特点").performClick()

        // 折叠后标签不可见
        composeTestRule.onNodeWithText("开朗").assertIsNotDisplayed()

        // 再次点击展开
        composeTestRule.onNodeWithText("性格特点").performClick()

        // 展开后标签可见
        composeTestRule.onNodeWithText("开朗").assertIsDisplayed()
    }

    // ==================== 与现有PersonaTab兼容性测试 ====================

    @Test
    fun 与现有功能兼容性_标签点击触发详情() {
        var clickedFactId: String? = null
        val fact = createFact("1", "性格特点", "开朗")
        val categories = listOf(createCategory("性格特点", listOf(fact)))

        composeTestRule.setContent {
            EmpathyTheme {
                PersonaTabV2(
                    categories = categories,
                    searchState = PersonaSearchState(),
                    editModeState = EditModeState(),
                    availableCategories = emptyList(),
                    isDarkMode = false,
                    onSearchQueryChange = {},
                    onClearSearch = {},
                    onToggleCategoryExpand = {},
                    onFactClick = { clickedFactId = it },
                    onFactLongClick = {},
                    onToggleFactSelection = {},
                    onExitEditMode = {},
                    onSelectAll = {},
                    onDeselectAll = {},
                    onShowDeleteConfirm = {},
                    onShowMoveDialog = {},
                    onConfirmDelete = {},
                    onHideDeleteConfirm = {},
                    onConfirmMove = {},
                    onHideMoveDialog = {}
                )
            }
        }

        // 非编辑模式下点击标签应触发详情查看
        composeTestRule.onNodeWithText("开朗").performClick()

        // 验证回调
        assert(clickedFactId == "1")
    }
}


