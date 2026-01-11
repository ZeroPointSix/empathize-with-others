package com.empathy.ai.presentation.ui.screen.contact.persona

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTouchInput
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
 * PersonaTabV2 UI测试
 *
 * 测试标签画像V2界面的UI交互
 */
class PersonaTabV2Test {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testColor = CategoryColor(
        titleColor = 0xFFB71C1C,
        tagBackgroundColor = 0xFFFFCDD2,
        tagTextColor = 0xFFB71C1C
    )

    private fun createFact(
        id: String = "fact_1",
        key: String = "性格特点",
        value: String = "开朗"
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
        isExpanded: Boolean = true
    ) = FactCategory(
        key = key,
        facts = facts,
        color = testColor,
        isExpanded = isExpanded
    )

    @Test
    fun 显示搜索栏_非编辑模式() {
        composeTestRule.setContent {
            EmpathyTheme {
                PersonaTabV2(
                    categories = emptyList(),
                    searchState = PersonaSearchState(),
                    editModeState = EditModeState(),
                    availableCategories = emptyList(),
                    isDarkMode = false,
                    onSearchQueryChange = {},
                    onClearSearch = {},
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

        // 验证搜索栏显示
        composeTestRule.onNodeWithText("搜索标签或分类...").assertIsDisplayed()
    }

    @Test
    fun 显示编辑模式顶栏_编辑模式() {
        composeTestRule.setContent {
            EmpathyTheme {
                PersonaTabV2(
                    categories = emptyList(),
                    searchState = PersonaSearchState(),
                    editModeState = EditModeState.activated("fact_1"),
                    availableCategories = emptyList(),
                    isDarkMode = false,
                    onSearchQueryChange = {},
                    onClearSearch = {},
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

        // 验证编辑模式顶栏显示
        composeTestRule.onNodeWithText("已选择 1 项").assertIsDisplayed()
    }

    @Test
    fun 显示分类卡片() {
        val categories = listOf(
            createCategory("性格特点", listOf(createFact("1", "性格特点", "开朗")))
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

        // 验证分类卡片显示
        composeTestRule.onNodeWithText("性格特点").assertIsDisplayed()
        composeTestRule.onNodeWithText("开朗").assertIsDisplayed()
    }

    @Test
    fun 分类折叠展开_点击标题() {
        var toggledKey: String? = null
        val categories = listOf(
            createCategory("性格特点", listOf(createFact()), isExpanded = true)
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
                    onToggleCategoryExpand = { toggledKey = it },
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

        // 点击分类标题
        composeTestRule.onNodeWithText("性格特点").performClick()

        // 验证回调被调用
        assert(toggledKey == "性格特点")
    }

    @Test
    fun 标签点击_非编辑模式() {
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

        // 点击标签
        composeTestRule.onNodeWithText("开朗").performClick()

        // 验证回调被调用
        assert(clickedFactId == "1")
    }

    @Test
    fun 标签长按_进入编辑模式() {
        var longClickedFactId: String? = null
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
                    onFactClick = {},
                    onFactLongClick = { longClickedFactId = it },
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

        // 长按标签
        composeTestRule.onNodeWithText("开朗").performTouchInput { longClick() }

        // 验证回调被调用
        assert(longClickedFactId == "1")
    }

    @Test
    fun 批量操作栏显示_有选中项() {
        val fact = createFact("1", "性格特点", "开朗")
        val categories = listOf(createCategory("性格特点", listOf(fact)))

        composeTestRule.setContent {
            EmpathyTheme {
                PersonaTabV2(
                    categories = categories,
                    searchState = PersonaSearchState(),
                    editModeState = EditModeState.activated("1"),
                    availableCategories = emptyList(),
                    isDarkMode = false,
                    onSearchQueryChange = {},
                    onClearSearch = {},
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

        // 验证批量操作栏显示
        composeTestRule.onNodeWithText("移动分类").assertIsDisplayed()
        composeTestRule.onNodeWithText("删除 (1)").assertIsDisplayed()
    }

    @Test
    fun 搜索输入_触发回调() {
        var searchQuery = ""

        composeTestRule.setContent {
            EmpathyTheme {
                PersonaTabV2(
                    categories = emptyList(),
                    searchState = PersonaSearchState(),
                    editModeState = EditModeState(),
                    availableCategories = emptyList(),
                    isDarkMode = false,
                    onSearchQueryChange = { searchQuery = it },
                    onClearSearch = {},
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

        // 输入搜索内容
        composeTestRule.onNodeWithText("搜索标签或分类...").performTextInput("性格")

        // 验证回调被调用
        assert(searchQuery == "性格")
    }

    @Test
    fun 空状态显示() {
        composeTestRule.setContent {
            EmpathyTheme {
                PersonaTabV2(
                    categories = emptyList(),
                    searchState = PersonaSearchState(),
                    editModeState = EditModeState(),
                    availableCategories = emptyList(),
                    isDarkMode = false,
                    onSearchQueryChange = {},
                    onClearSearch = {},
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

        // 验证空状态提示
        composeTestRule.onNodeWithText("暂无标签").assertIsDisplayed()
    }

    @Test
    fun 搜索无结果显示() {
        composeTestRule.setContent {
            EmpathyTheme {
                PersonaTabV2(
                    categories = emptyList(),
                    searchState = PersonaSearchState(query = "不存在的关键词"),
                    editModeState = EditModeState(),
                    availableCategories = emptyList(),
                    isDarkMode = false,
                    onSearchQueryChange = {},
                    onClearSearch = {},
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

        // 验证搜索无结果提示
        composeTestRule.onNodeWithText("未找到匹配的标签").assertIsDisplayed()
    }
}



