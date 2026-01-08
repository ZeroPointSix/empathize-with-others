package com.empathy.ai.presentation.ui.floating

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.empathy.ai.presentation.R
import com.empathy.ai.domain.model.ActionType
import com.google.android.material.button.MaterialButton

/**
 * 悬浮窗Tab切换组件
 *
 * 提供四个Tab（分析/润色/回复/问答）的切换功能
 * 使用预加载方式，通过VISIBLE/GONE切换避免重复inflate
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
 * @see PRD-00031 悬浮窗快速知识回答功能需求
 */
class TabSwitcher @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var tabAnalyze: MaterialButton? = null
    private var tabPolish: MaterialButton? = null
    private var tabReply: MaterialButton? = null
    private var tabKnowledge: MaterialButton? = null  // PRD-00031: 新增问答Tab

    private var selectedTab: ActionType = ActionType.ANALYZE
    private var onTabSelectedListener: ((ActionType) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.floating_tab_switcher, this, true)
        initViews()
        setupClickListeners()
        updateTabStates()
    }

    private fun initViews() {
        tabAnalyze = findViewById(R.id.tab_analyze)
        tabPolish = findViewById(R.id.tab_polish)
        tabReply = findViewById(R.id.tab_reply)
        tabKnowledge = findViewById(R.id.tab_knowledge)  // PRD-00031: 新增问答Tab
    }

    private fun setupClickListeners() {
        tabAnalyze?.setOnClickListener { selectTab(ActionType.ANALYZE) }
        tabPolish?.setOnClickListener { selectTab(ActionType.POLISH) }
        tabReply?.setOnClickListener { selectTab(ActionType.REPLY) }
        tabKnowledge?.setOnClickListener { selectTab(ActionType.KNOWLEDGE) }  // PRD-00031: 新增问答Tab
    }

    /**
     * 选择指定的Tab
     *
     * @param tab 要选择的Tab类型
     */
    fun selectTab(tab: ActionType) {
        if (selectedTab != tab) {
            selectedTab = tab
            updateTabStates()
            onTabSelectedListener?.invoke(tab)
        }
    }

    /**
     * 设置当前选中的Tab（不触发回调）
     *
     * @param tab 要设置的Tab类型
     */
    fun setSelectedTab(tab: ActionType) {
        selectedTab = tab
        updateTabStates()
    }

    /**
     * 获取当前选中的Tab
     *
     * @return 当前选中的Tab类型
     */
    fun getSelectedTab(): ActionType = selectedTab

    /**
     * 设置Tab选择监听器
     *
     * @param listener Tab选择回调
     */
    fun setOnTabSelectedListener(listener: (ActionType) -> Unit) {
        onTabSelectedListener = listener
    }

    private fun updateTabStates() {
        // 更新分析Tab状态
        tabAnalyze?.isSelected = selectedTab == ActionType.ANALYZE
        tabAnalyze?.isChecked = selectedTab == ActionType.ANALYZE

        // 更新润色Tab状态
        tabPolish?.isSelected = selectedTab == ActionType.POLISH
        tabPolish?.isChecked = selectedTab == ActionType.POLISH

        // 更新回复Tab状态
        tabReply?.isSelected = selectedTab == ActionType.REPLY
        tabReply?.isChecked = selectedTab == ActionType.REPLY

        // PRD-00031: 更新问答Tab状态
        tabKnowledge?.isSelected = selectedTab == ActionType.KNOWLEDGE
        tabKnowledge?.isChecked = selectedTab == ActionType.KNOWLEDGE
    }

    companion object {
        private const val TAG = "TabSwitcher"
    }
}
