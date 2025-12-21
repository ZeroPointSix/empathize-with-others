#!/bin/bash

echo "🔍 共情AI助手 - 精简代码统计"
echo "==============================="
echo ""

# 项目基本信息
echo "📊 项目基本信息"
echo "----------------"
echo "项目名称: 共情AI助手 (Empathy AI Assistant)"
echo "技术栈: Kotlin + Android + Clean Architecture"
echo "统计时间: $(date)"
echo ""

# 源代码统计
echo "📝 源代码统计"
echo "----------------"
MAIN_FILES=$(find app/src/main/java -name "*.kt" 2>/dev/null | wc -l)
MAIN_LINES=$(find app/src/main/java -name "*.kt" -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}')
echo "Kotlin源代码: $MAIN_FILES 个文件, $MAIN_LINES 行"

# 测试代码统计
echo ""
echo "🧪 测试代码统计"
echo "----------------"
TEST_FILES=$(find app/src/test -name "*.kt" 2>/dev/null | wc -l)
TEST_LINES=$(find app/src/test -name "*.kt" -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}')
echo "单元测试: $TEST_FILES 个文件, $TEST_LINES 行"

ANDROID_TEST_FILES=$(find app/src/androidTest -name "*.kt" 2>/dev/null | wc -l)
ANDROID_TEST_LINES=$(find app/src/androidTest -name "*.kt" -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}')
echo "Android测试: $ANDROID_TEST_FILES 个文件, $ANDROID_TEST_LINES 行"

# Kotlin总计
echo ""
echo "📊 Kotlin总计"
echo "----------------"
TOTAL_KT_FILES=$((MAIN_FILES + TEST_FILES + ANDROID_TEST_FILES))
TOTAL_KT_LINES=$((MAIN_LINES + TEST_LINES + ANDROID_TEST_LINES))
echo "总文件数: $TOTAL_KT_FILES"
echo "总行数: $TOTAL_KT_LINES"

# 测试覆盖率
if [ $MAIN_LINES -gt 0 ]; then
    TEST_COVERAGE=$(awk "BEGIN {printf \"%.1f\", ($TEST_LINES + $ANDROID_TEST_LINES) * 100 / $MAIN_LINES}")
    echo "测试覆盖率: $TEST_COVERAGE% (测试代码/源代码)"
fi

# 其他文件
echo ""
echo "📄 其他文件统计"
echo "----------------"
XML_FILES=$(find . -name "*.xml" ! -path "*/build/*" ! -path "*/.git/*" 2>/dev/null | wc -l)
XML_LINES=$(find . -name "*.xml" ! -path "*/build/*" ! -path "*/.git/*" -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}')
echo "XML配置文件: $XML_FILES 个, $XML_LINES 行"

MD_FILES=$(find . -name "*.md" ! -path "*/build/*" ! -path "*/.git/*" 2>/dev/null | wc -l)
MD_LINES=$(find . -name "*.md" ! -path "*/build/*" ! -path "*/.git/*" -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}')
echo "Markdown文档: $MD_FILES 个, $MD_LINES 行"

# 项目规模总结
echo ""
echo "📈 项目规模总结"
echo "=================="
PROJECT_TOTAL=$((TOTAL_KT_LINES + XML_LINES + MD_LINES))
echo "📁 项目总代码行数: $PROJECT_TOTAL 行"

# 转换为万行
WAN_LINES=$((PROJECT_TOTAL / 10000))
REMAINDER=$((PROJECT_TOTAL % 10000))
if [ $REMAINDER -lt 100 ]; then
    REMAINDER_STR="0$(echo $REMAINDER | cut -c1)"
else
    REMAINDER_STR=$(echo $REMAINDER | cut -c1-2)
fi

echo "🎯 项目规模: 约 $WAN_LINES.$REMAINDER_STR 万行代码"

# 项目评级
echo ""
echo "🏆 项目评级"
echo "============"
if [ $WAN_LINES -ge 10 ]; then
    echo "规模: 大型项目 (10万行+)"
elif [ $WAN_LINES -ge 5 ]; then
    echo "规模: 中大型项目 (5-10万行)"
elif [ $WAN_LINES -ge 1 ]; then
    echo "规模: 中型项目 (1-5万行)"
else
    echo "规模: 小型项目 (<1万行)"
fi

if [[ $TEST_COVERAGE != "" && $(echo "$TEST_COVERAGE >= 50" | bc -l) -eq 1 ]]; then
    echo "质量: 优秀 (高测试覆盖率)"
elif [[ $TEST_COVERAGE != "" && $(echo "$TEST_COVERAGE >= 25" | bc -l) -eq 1 ]]; then
    echo "质量: 良好 (中等测试覆盖率)"
else
    echo "质量: 一般 (低测试覆盖率)"
fi

if [ $MD_FILES -gt 100 ]; then
    echo "文档: 完善 (100+文档文件)"
elif [ $MD_FILES -gt 50 ]; then
    echo "文档: 良好 (50-100文档文件)"
else
    echo "文档: 基础 (<50文档文件)"
fi

echo ""
echo "✅ 统计完成"