#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
用户意图分析Hook实现
分析用户输入意图，检查工作状态，推荐合适的AI代理
"""

import os
import re
import time
import json
import logging
from typing import Dict, List, Tuple, Optional, Any
from dataclasses import dataclass
from enum import Enum
from pathlib import Path
import yaml


# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger("IntentAnalyzer")


class IntentType(Enum):
    """意图类型枚举"""
    DEVELOPMENT = "开发任务"
    DOCUMENTATION = "文档任务"
    DESIGN = "设计任务"
    REVIEW = "审查任务"
    MANAGEMENT = "管理任务"


class ComplexityLevel(Enum):
    """复杂度级别枚举"""
    SIMPLE = "简单"
    MEDIUM = "中等"
    COMPLEX = "复杂"


class AgentType(Enum):
    """AI代理类型枚举"""
    KIRO = "Kiro"
    ROO = "Roo"
    CLAUDE = "Claude"
    PRODUCT_MANAGER = "产品经理"
    SECURITY_REVIEWER = "安全审查员"


@dataclass
class AnalysisResult:
    """分析结果数据类"""
    user_input: str
    intent_type: IntentType
    complexity: ComplexityLevel
    estimated_workload: str
    recommended_agent: AgentType
    has_conflicts: bool
    dependencies_met: bool
    resources_available: bool
    execution_advice: str
    security_passed: bool
    security_issues: List[str]


class SecurityValidator:
    """安全验证器类"""
    
    def __init__(self, config: Dict[str, Any]):
        """
        初始化安全验证器
        
        Args:
            config: 安全配置字典
        """
        self.max_input_length = config.get('max-input-length', 10000)
        self.blocked_keywords = config.get('blocked_keywords', [])
        self.allowed_paths = config.get('allowed_paths', [])
        self.max_analysis_time = config.get('max-analysis-time', 5000)
    
    def validate_input(self, user_input: str) -> Tuple[bool, List[str]]:
        """
        验证用户输入安全性
        
        Args:
            user_input: 用户输入字符串
            
        Returns:
            Tuple[bool, List[str]]: (是否通过验证, 安全问题列表)
        """
        security_issues = []
        
        # 检查输入长度
        if len(user_input) > self.max_input_length:
            security_issues.append(f"输入长度超过限制({self.max_input_length}字符)")
        
        # 检查恶意关键词
        for keyword in self.blocked_keywords:
            if keyword in user_input:
                security_issues.append(f"检测到潜在恶意关键词: {keyword}")
        
        # 检查代码注入模式
        injection_patterns = [
            r'__import__\s*\(',
            r'eval\s*\(',
            r'exec\s*\(',
            r'open\s*\(',
            r'file\s*\(',
            r'subprocess\.',
            r'os\.system',
            r'os\.popen',
            r'\.\./.*\.\.',
            r'rm\s+-rf',
            r'del\s+\/',
        ]
        
        for pattern in injection_patterns:
            if re.search(pattern, user_input, re.IGNORECASE):
                security_issues.append(f"检测到潜在代码注入模式: {pattern}")
        
        return len(security_issues) == 0, security_issues
    
    def validate_file_access(self, file_path: str) -> bool:
        """
        验证文件访问权限
        
        Args:
            file_path: 文件路径
            
        Returns:
            bool: 是否允许访问
        """
        # 规范化路径
        normalized_path = os.path.normpath(file_path)
        
        # 检查是否在允许的路径列表中
        for allowed_path in self.allowed_paths:
            if normalized_path == os.path.normpath(allowed_path):
                return True
        
        # 检查路径遍历攻击
        if '..' in normalized_path:
            return False
        
        return False


class IntentClassifier:
    """意图分类器类"""
    
    def __init__(self):
        """初始化意图分类器"""
        # 意图关键词映射
        self.intent_keywords = {
            IntentType.DEVELOPMENT: [
                '实现', '开发', '编码', '编程', '代码', '函数', '类', '方法',
                '调试', '测试', '构建', '编译', '部署', '修复', 'bug', 'error',
                '功能', '模块', '组件', '接口', 'api', '数据库', 'sql', '查询'
            ],
            IntentType.DOCUMENTATION: [
                '文档', '编写', '说明', '指南', '手册', 'readme', '注释',
                '规范', '标准', '模板', '示例', '教程', '介绍', '概述'
            ],
            IntentType.DESIGN: [
                '设计', '架构', '方案', '规划', '布局', '界面', 'ui', 'ux',
                '原型', '流程', '结构', '模式', '框架', '组件设计', '系统设计'
            ],
            IntentType.REVIEW: [
                '审查', '检查', '审核', '评估', '分析', '验证', '测试',
                '代码审查', '安全检查', '质量保证', '优化', '重构'
            ],
            IntentType.MANAGEMENT: [
                '管理', '计划', '任务', '进度', '协调', '分配', '跟踪',
                '报告', '状态', '更新', '优先级', '里程碑', '发布', '版本'
            ]
        }
        
        # 复杂度关键词
        self.complexity_keywords = {
            ComplexityLevel.SIMPLE: [
                '简单', '快速', '小', '单一', '直接', '基本', '修复', '添加'
            ],
            ComplexityLevel.MEDIUM: [
                '中等', '多个', '集成', '优化', '改进', '扩展', '重构'
            ],
            ComplexityLevel.COMPLEX: [
                '复杂', '系统', '架构', '全面', '完整', '大规模', '多模块',
                '重构', '迁移', '集成', '协调', '多领域'
            ]
        }
    
    def classify_intent(self, user_input: str) -> IntentType:
        """
        分类用户意图
        
        Args:
            user_input: 用户输入字符串
            
        Returns:
            IntentType: 意图类型
        """
        # 转换为小写以便匹配
        input_lower = user_input.lower()
        
        # 计算每种意图的匹配分数
        intent_scores = {}
        for intent_type, keywords in self.intent_keywords.items():
            score = 0
            for keyword in keywords:
                if keyword in input_lower:
                    score += 1
            intent_scores[intent_type] = score
        
        # 返回得分最高的意图类型
        if max(intent_scores.values()) == 0:
            # 如果没有匹配到任何关键词，默认返回开发任务
            return IntentType.DEVELOPMENT
        
        return max(intent_scores, key=intent_scores.get)
    
    def assess_complexity(self, user_input: str) -> ComplexityLevel:
        """
        评估任务复杂度
        
        Args:
            user_input: 用户输入字符串
            
        Returns:
            ComplexityLevel: 复杂度级别
        """
        input_lower = user_input.lower()
        
        # 计算每种复杂度的匹配分数
        complexity_scores = {}
        for complexity, keywords in self.complexity_keywords.items():
            score = 0
            for keyword in keywords:
                if keyword in input_lower:
                    score += 1
            complexity_scores[complexity] = score
        
        # 返回得分最高的复杂度级别
        if max(complexity_scores.values()) == 0:
            # 如果没有匹配到任何关键词，默认返回中等复杂度
            return ComplexityLevel.MEDIUM
        
        return max(complexity_scores, key=complexity_scores.get)
    
    def estimate_workload(self, complexity: ComplexityLevel) -> str:
        """
        根据复杂度估算工作量
        
        Args:
            complexity: 复杂度级别
            
        Returns:
            str: 工作量估算
        """
        workload_map = {
            ComplexityLevel.SIMPLE: "1-2小时",
            ComplexityLevel.MEDIUM: "半天到1天",
            ComplexityLevel.COMPLEX: "1-3天"
        }
        return workload_map.get(complexity, "未知")


class AgentRecommender:
    """代理推荐器类"""
    
    def __init__(self):
        """初始化代理推荐器"""
        # 意图类型到推荐代理的映射
        self.intent_agent_map = {
            IntentType.DEVELOPMENT: [AgentType.KIRO, AgentType.CLAUDE],
            IntentType.DOCUMENTATION: [AgentType.CLAUDE, AgentType.PRODUCT_MANAGER],
            IntentType.DESIGN: [AgentType.CLAUDE, AgentType.PRODUCT_MANAGER],
            IntentType.REVIEW: [AgentType.ROO, AgentType.SECURITY_REVIEWER],
            IntentType.MANAGEMENT: [AgentType.PRODUCT_MANAGER, AgentType.CLAUDE]
        }
        
        # 复杂度到推荐代理的调整
        self.complexity_adjustment = {
            ComplexityLevel.SIMPLE: 0,  # 无调整
            ComplexityLevel.MEDIUM: 0,  # 无调整
            ComplexityLevel.COMPLEX: 1  # 复杂任务优先考虑经验更丰富的代理
        }
    
    def recommend_agent(self, intent_type: IntentType, complexity: ComplexityLevel) -> AgentType:
        """
        根据意图类型和复杂度推荐代理
        
        Args:
            intent_type: 意图类型
            complexity: 复杂度级别
            
        Returns:
            AgentType: 推荐的代理类型
        """
        # 获取基础推荐列表
        base_agents = self.intent_agent_map.get(intent_type, [AgentType.KIRO])
        
        # 根据复杂度调整
        adjustment = self.complexity_adjustment.get(complexity, 0)
        
        # 返回调整后的推荐代理
        if adjustment < len(base_agents):
            return base_agents[adjustment]
        return base_agents[0]


class WorkspaceChecker:
    """工作空间检查器类"""
    
    def __init__(self, workspace_path: str):
        """
        初始化工作空间检查器
        
        Args:
            workspace_path: 工作空间路径
        """
        self.workspace_path = workspace_path
        self.workspace_file = os.path.join(workspace_path, "WORKSPACE.md")
        self.rules_file = os.path.join(workspace_path, "Rules", "RulesReadMe.md")
    
    def check_conflicts(self, user_input: str) -> Tuple[bool, List[str]]:
        """
        检查是否存在冲突
        
        Args:
            user_input: 用户输入字符串
            
        Returns:
            Tuple[bool, List[str]]: (是否存在冲突, 冲突详情列表)
        """
        conflicts = []
        
        # 尝试读取WORKSPACE.md文件
        try:
            if os.path.exists(self.workspace_file):
                with open(self.workspace_file, 'r', encoding='utf-8') as f:
                    workspace_content = f.read()
                
                # 检查是否有正在进行的任务
                if "正在进行的任务" in workspace_content:
                    # 简单检查是否有高优先级任务
                    if "🔴 高" in workspace_content:
                        conflicts.append("存在高优先级任务正在进行中")
                
                # 检查用户输入是否与现有任务相关
                # 这里可以添加更复杂的逻辑来检测任务相关性
                task_patterns = [
                    r'任务\s*ID[:：]\s*(TD-\d+|BUG-\d+|PRD-\d+|FD-\d+)',
                    r'(TD|BUG|PRD|FD|TDD|IMPL|CR|DR)-\d+',
                ]
                
                for pattern in task_patterns:
                    matches = re.findall(pattern, user_input)
                    for match in matches:
                        if isinstance(match, tuple):
                            match = match[0] if match[0] else match[1] if len(match) > 1 else ""
                        
                        if match and match in workspace_content:
                            conflicts.append(f"任务 {match} 已在工作空间中记录")
        except Exception as e:
            logger.warning(f"读取工作空间文件失败: {e}")
            conflicts.append("无法检查工作空间状态")
        
        return len(conflicts) > 0, conflicts
    
    def check_dependencies(self, user_input: str) -> Tuple[bool, List[str]]:
        """
        检查依赖是否满足
        
        Args:
            user_input: 用户输入字符串
            
        Returns:
            Tuple[bool, List[str]]: (依赖是否满足, 未满足依赖列表)
        """
        unmet_deps = []
        
        # 检查是否有前置任务依赖
        dep_patterns = [
            r'需要.*完成',
            r'等待.*完成',
            r'依赖.*',
            r'前置.*',
        ]
        
        for pattern in dep_patterns:
            if re.search(pattern, user_input):
                unmet_deps.append("检测到潜在的前置任务依赖")
        
        # 检查技术债务
        try:
            if os.path.exists(self.workspace_file):
                with open(self.workspace_file, 'r', encoding='utf-8') as f:
                    workspace_content = f.read()
                
                if "技术债务" in workspace_content and "🔴 高" in workspace_content:
                    unmet_deps.append("存在高优先级技术债务")
        except Exception as e:
            logger.warning(f"检查技术债务失败: {e}")
        
        return len(unmet_deps) == 0, unmet_deps
    
    def check_resources(self) -> bool:
        """
        检查资源是否可用
        
        Returns:
            bool: 资源是否可用
        """
        # 简单检查工作空间是否可访问
        return os.path.exists(self.workspace_path) and os.access(self.workspace_path, os.R_OK)


class IntentAnalyzer:
    """主意图分析器类"""
    
    def __init__(self, config_path: str = None):
        """
        初始化意图分析器
        
        Args:
            config_path: 配置文件路径，默认为.kiro/settings/hooks.yaml
        """
        if config_path is None:
            # 默认配置文件路径
            current_dir = os.path.dirname(os.path.abspath(__file__))
            config_path = os.path.join(current_dir, '..', 'settings', 'hooks.yaml')
        
        # 加载配置
        self.config = self._load_config(config_path)
        self.hook_config = self.config.get('user-intent-analysis', {})
        
        # 初始化组件
        self.security_validator = SecurityValidator(self.hook_config.get('security', {}))
        self.intent_classifier = IntentClassifier()
        self.agent_recommender = AgentRecommender()
        
        # 获取工作空间路径
        self.workspace_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..'))
        self.workspace_checker = WorkspaceChecker(self.workspace_path)
        
        # 文件缓存
        self.file_cache = {}
        self.cache_duration = self.hook_config.get('cache-duration', 300)  # 5分钟
    
    def _load_config(self, config_path: str) -> Dict[str, Any]:
        """
        加载配置文件
        
        Args:
            config_path: 配置文件路径
            
        Returns:
            Dict[str, Any]: 配置字典
        """
        try:
            with open(config_path, 'r', encoding='utf-8') as f:
                return yaml.safe_load(f)
        except Exception as e:
            logger.error(f"加载配置文件失败: {e}")
            return {}
    
    def _read_file_with_cache(self, file_path: str) -> Optional[str]:
        """
        带缓存的文件读取
        
        Args:
            file_path: 文件路径
            
        Returns:
            Optional[str]: 文件内容，读取失败返回None
        """
        current_time = time.time()
        
        # 检查缓存
        if file_path in self.file_cache:
            cached_data = self.file_cache[file_path]
            if current_time - cached_data['timestamp'] < self.cache_duration:
                return cached_data['content']
        
        # 读取文件
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 更新缓存
            self.file_cache[file_path] = {
                'content': content,
                'timestamp': current_time
            }
            
            return content
        except Exception as e:
            logger.error(f"读取文件失败 {file_path}: {e}")
            return None
    
    def _force_read_rules(self) -> bool:
        """
        强制读取规则文件
        
        Returns:
            bool: 是否成功读取
        """
        if not self.hook_config.get('force-read-rules', True):
            return True
        
        rules_file = os.path.join(self.workspace_path, "Rules", "RulesReadMe.md")
        workspace_file = os.path.join(self.workspace_path, "WORKSPACE.md")
        
        # 验证文件访问权限
        if not self.security_validator.validate_file_access(rules_file):
            logger.error(f"无权限访问规则文件: {rules_file}")
            return False
        
        if not self.security_validator.validate_file_access(workspace_file):
            logger.error(f"无权限访问工作空间文件: {workspace_file}")
            return False
        
        # 读取文件
        rules_content = self._read_file_with_cache(rules_file)
        workspace_content = self._read_file_with_cache(workspace_file)
        
        return rules_content is not None and workspace_content is not None
    
    def analyze(self, user_input: str) -> AnalysisResult:
        """
        分析用户意图
        
        Args:
            user_input: 用户输入字符串
            
        Returns:
            AnalysisResult: 分析结果
        """
        start_time = time.time()
        
        # 安全验证
        security_passed, security_issues = self.security_validator.validate_input(user_input)
        
        if not security_passed:
            logger.warning(f"安全验证失败: {security_issues}")
            return AnalysisResult(
                user_input=user_input,
                intent_type=IntentType.DEVELOPMENT,  # 默认值
                complexity=ComplexityLevel.MEDIUM,   # 默认值
                estimated_workload="未知",
                recommended_agent=AgentType.KIRO,   # 默认值
                has_conflicts=False,
                dependencies_met=False,
                resources_available=False,
                execution_advice="安全检查未通过，建议修改输入",
                security_passed=False,
                security_issues=security_issues
            )
        
        # 强制读取规则文件
        if not self._force_read_rules():
            logger.error("无法读取规则文件")
        
        # 意图分类
        intent_type = self.intent_classifier.classify_intent(user_input)
        complexity = self.intent_classifier.assess_complexity(user_input)
        estimated_workload = self.intent_classifier.estimate_workload(complexity)
        
        # 代理推荐
        recommended_agent = self.agent_recommender.recommend_agent(intent_type, complexity)
        
        # 工作状态检查
        has_conflicts, conflict_details = self.workspace_checker.check_conflicts(user_input)
        dependencies_met, dependency_details = self.workspace_checker.check_dependencies(user_input)
        resources_available = self.workspace_checker.check_resources()
        
        # 生成执行建议
        if has_conflicts or not dependencies_met or not resources_available:
            execution_advice = "延迟执行"
        elif complexity == ComplexityLevel.COMPLEX:
            execution_advice = "分解执行"
        else:
            execution_advice = "立即执行"
        
        # 检查分析时间
        analysis_time = (time.time() - start_time) * 1000  # 转换为毫秒
        max_time = self.hook_config.get('security', {}).get('max-analysis-time', 5000)
        if analysis_time > max_time:
            logger.warning(f"分析时间过长: {analysis_time:.2f}ms")
        
        return AnalysisResult(
            user_input=user_input,
            intent_type=intent_type,
            complexity=complexity,
            estimated_workload=estimated_workload,
            recommended_agent=recommended_agent,
            has_conflicts=has_conflicts,
            dependencies_met=dependencies_met,
            resources_available=resources_available,
            execution_advice=execution_advice,
            security_passed=security_passed,
            security_issues=security_issues
        )
    
    def format_result(self, result: AnalysisResult) -> str:
        """
        格式化分析结果
        
        Args:
            result: 分析结果
            
        Returns:
            str: 格式化后的结果字符串
        """
        formatted_result = """
🔍 用户意图分析结果
========================================

📝 用户指令: {}

🧠 意图分析:
   - 主要意图: {}
   - 复杂度: {}
   - 预计工作量: {}

🤖 推荐代理: {}

📋 工作状态检查:
   - 冲突检测: {}
   - 依赖检查: {}
   - 资源状态: {}

💡 执行建议: {}

🔒 安全检查: {}
""".format(
            result.user_input,
            result.intent_type.value,
            result.complexity.value,
            result.estimated_workload,
            result.recommended_agent.value,
            "⚠️ 存在冲突" if result.has_conflicts else "✅ 无冲突",
            "⚠️ 未满足" if not result.dependencies_met else "✅ 已满足",
            "⚠️ 占用" if not result.resources_available else "✅ 可用",
            result.execution_advice,
            "⚠️ 需要注意" if not result.security_passed else "✅ 通过"
        )
        
        # 如果有安全问题，添加详细信息
        if result.security_issues:
            formatted_result += "\n🚨 安全问题:\n"
            for issue in result.security_issues:
                formatted_result += f"   - {issue}\n"
        
        return formatted_result


# Hook入口函数
def on_user_input(user_input: str) -> str:
    """
    用户输入Hook入口函数
    
    Args:
        user_input: 用户输入字符串
        
    Returns:
        str: 分析结果
    """
    try:
        # 创建意图分析器
        analyzer = IntentAnalyzer()
        
        # 执行分析
        result = analyzer.analyze(user_input)
        
        # 格式化结果
        formatted_result = analyzer.format_result(result)
        
        # 记录日志
        logger.info(f"用户意图分析完成: {result.intent_type.value}, {result.complexity.value}")
        
        return formatted_result
    except Exception as e:
        error_msg = f"用户意图分析失败: {e}"
        logger.error(error_msg)
        return error_msg


if __name__ == "__main__":
    # 测试代码
    test_inputs = [
        "实现一个新的用户登录功能",
        "编写项目文档",
        "设计系统架构",
        "审查代码质量",
        "更新项目进度"
    ]
    
    analyzer = IntentAnalyzer()
    
    for test_input in test_inputs:
        print(f"\n测试输入: {test_input}")
        result = analyzer.analyze(test_input)
        print(analyzer.format_result(result))