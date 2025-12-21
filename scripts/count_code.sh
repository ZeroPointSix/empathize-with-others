#!/bin/bash

# 代码统计脚本
# 统计项目中各种类型的代码行数

echo "====================================="
echo "🔍 共情AI助手 - 项目代码统计报告"
echo "====================================="
echo ""

# 项目根目录
PROJECT_ROOT="."

echo "📁 统计范围: $PROJECT_ROOT"
echo "⏰ 统计时间: $(date)"
echo ""

# 1. Kotlin源代码统计
echo "📊 1. Kotlin 代码统计"
echo "-------------------------------------"

# 源代码
MAIN_KT_FILES=$(find $PROJECT_ROOT/app/src/main -name "*.kt" -type f 2>/dev/null | wc -l)
MAIN_KT_LINES=$(find $PROJECT_ROOT/app/src/main -name "*.kt" -type f -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}' || echo 0)

# 测试代码
TEST_KT_FILES=$(find $PROJECT_ROOT/app/src/test -name "*.kt" -type f 2>/dev/null | wc -l)
TEST_KT_LINES=$(find $PROJECT_ROOT/app/src/test -name "*.kt" -type f -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}' || echo 0)

# Android测试
ANDROID_TEST_KT_FILES=$(find $PROJECT_ROOT/app/src/androidTest -name "*.kt" -type f 2>/dev/null | wc -l)
ANDROID_TEST_KT_LINES=$(find $PROJECT_ROOT/app/src/androidTest -name "*.kt" -type f -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}' || echo 0)

# Kotlin总计
TOTAL_KT_FILES=$((MAIN_KT_FILES + TEST_KT_FILES + ANDROID_TEST_KT_FILES))
TOTAL_KT_LINES=$((MAIN_KT_LINES + TEST_KT_LINES + ANDROID_TEST_KT_LINES))

printf "%-20s %8s %10s %10s\n" "类型" "文件数" "行数" "占比"
if command -v bc >/dev/null 2>&1; then
    MAIN_PERCENT=$(echo "scale=1; $MAIN_KT_LINES * 100 / $TOTAL_KT_LINES" | bc -l)
    TEST_PERCENT=$(echo "scale=1; $TEST_KT_LINES * 100 / $TOTAL_KT_LINES" | bc -l)
    ANDROID_PERCENT=$(echo "scale=1; $ANDROID_TEST_KT_LINES * 100 / $TOTAL_KT_LINES" | bc -l)
else
    MAIN_PERCENT=$(awk "BEGIN {printf \"%.1f\", $MAIN_KT_LINES * 100 / $TOTAL_KT_LINES}")
    TEST_PERCENT=$(awk "BEGIN {printf \"%.1f\", $TEST_KT_LINES * 100 / $TOTAL_KT_LINES}")
    ANDROID_PERCENT=$(awk "BEGIN {printf \"%.1f\", $ANDROID_TEST_KT_LINES * 100 / $TOTAL_KT_LINES}")
fi

printf "%-20s %8s %10s %10s\n" "源代码(main)" "$MAIN_KT_FILES" "$MAIN_KT_LINES" "${MAIN_PERCENT}%"
printf "%-20s %8s %10s %10s\n" "单元测试(test)" "$TEST_KT_FILES" "$TEST_KT_LINES" "${TEST_PERCENT}%"
printf "%-20s %8s %10s %10s\n" "Android测试" "$ANDROID_TEST_KT_FILES" "$ANDROID_TEST_KT_LINES" "${ANDROID_PERCENT}%"
printf "%-20s %8s %10s %10s\n" "Kotlin总计" "$TOTAL_KT_FILES" "$TOTAL_KT_LINES" "100%"
echo ""

# 2. XML文件统计
echo "📊 2. XML 配置统计"
echo "-------------------------------------"

XML_FILES=$(find $PROJECT_ROOT -name "*.xml" -type f ! -path "*/build/*" ! -path "*/.git/*" 2>/dev/null | wc -l)
XML_LINES=$(find $PROJECT_ROOT -name "*.xml" -type f ! -path "*/build/*" ! -path "*/.git/*" -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}' || echo 0)

echo "XML文件总数: $XML_FILES"
echo "XML代码总行数: $XML_LINES"
echo ""

# 3. 项目文档统计
echo "📊 3. 项目文档统计"
echo "-------------------------------------"

MD_FILES=$(find $PROJECT_ROOT -name "*.md" -type f ! -path "*/build/*" ! -path "*/.git/*" 2>/dev/null | wc -l)
MD_LINES=$(find $PROJECT_ROOT -name "*.md" -type f ! -path "*/build/*" ! -path "*/.git/*" -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}' || echo 0)

echo "Markdown文档数: $MD_FILES"
echo "文档总行数: $MD_LINES"
echo ""

# 4. 按模块详细统计
echo "📊 4. 模块代码分布"
echo "-------------------------------------"

# Domain层 - 只统计main源代码
DOMAIN_FILES=$(find $PROJECT_ROOT/app/src/main/java/com/empathy/ai/domain -name "*.kt" -type f 2>/dev/null | wc -l)
DOMAIN_LINES=$(find $PROJECT_ROOT/app/src/main/java/com/empathy/ai/domain -name "*.kt" -type f -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}' || echo 0)

# Data层 - 只统计main源代码
DATA_FILES=$(find $PROJECT_ROOT/app/src/main/java/com/empathy/ai/data -name "*.kt" -type f 2>/dev/null | wc -l)
DATA_LINES=$(find $PROJECT_ROOT/app/src/main/java/com/empathy/ai/data -name "*.kt" -type f -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}' || echo 0)

# Presentation层 - 只统计main源代码
PRESENTATION_FILES=$(find $PROJECT_ROOT/app/src/main/java/com/empathy/ai/presentation -name "*.kt" -type f 2>/dev/null | wc -l)
PRESENTATION_LINES=$(find $PROJECT_ROOT/app/src/main/java/com/empathy/ai/presentation -name "*.kt" -type f -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}' || echo 0)

# DI层 - 只统计main源代码
DI_FILES=$(find $PROJECT_ROOT/app/src/main/java/com/empathy/ai/di -name "*.kt" -type f 2>/dev/null | wc -l)
DI_LINES=$(find $PROJECT_ROOT/app/src/main/java/com/empathy/ai/di -name "*.kt" -type f -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}' || echo 0)

printf "%-15s %8s %10s %10s\n" "模块" "文件数" "行数" "占比"
if command -v bc >/dev/null 2>&1; then
    DOMAIN_PERCENT=$(echo "scale=1; $DOMAIN_LINES * 100 / $MAIN_KT_LINES" | bc -l)
    DATA_PERCENT=$(echo "scale=1; $DATA_LINES * 100 / $MAIN_KT_LINES" | bc -l)
    PRESENTATION_PERCENT=$(echo "scale=1; $PRESENTATION_LINES * 100 / $MAIN_KT_LINES" | bc -l)
    DI_PERCENT=$(echo "scale=1; $DI_LINES * 100 / $MAIN_KT_LINES" | bc -l)
else
    DOMAIN_PERCENT=$(awk "BEGIN {printf \"%.1f\", $DOMAIN_LINES * 100 / $MAIN_KT_LINES}")
    DATA_PERCENT=$(awk "BEGIN {printf \"%.1f\", $DATA_LINES * 100 / $MAIN_KT_LINES}")
    PRESENTATION_PERCENT=$(awk "BEGIN {printf \"%.1f\", $PRESENTATION_LINES * 100 / $MAIN_KT_LINES}")
    DI_PERCENT=$(awk "BEGIN {printf \"%.1f\", $DI_LINES * 100 / $MAIN_KT_LINES}")
fi
printf "%-15s %8s %10s %10s\n" "Domain层" "$DOMAIN_FILES" "$DOMAIN_LINES" "${DOMAIN_PERCENT}%"
printf "%-15s %8s %10s %10s\n" "Data层" "$DATA_FILES" "$DATA_LINES" "${DATA_PERCENT}%"
printf "%-15s %8s %10s %10s\n" "Presentation层" "$PRESENTATION_FILES" "$PRESENTATION_LINES" "${PRESENTATION_PERCENT}%"
printf "%-15s %8s %10s %10s\n" "DI层" "$DI_FILES" "$DI_LINES" "${DI_PERCENT}%"
echo ""

# 5. 项目总体规模
echo "📊 5. 项目总体规模"
echo "-------------------------------------"

TOTAL_FILES=$((TOTAL_KT_FILES + XML_FILES + MD_FILES))
TOTAL_LINES=$((TOTAL_KT_LINES + XML_LINES + MD_LINES))

echo "📁 总文件数: $TOTAL_FILES"
echo "📝 总代码行数: $TOTAL_LINES"
echo ""

# 换算成万行
WAN_LINES=$((TOTAL_LINES / 10000))
LEFT_LINES=$((TOTAL_LINES % 10000))

echo "📈 项目规模: 约 $WAN_LINES.$LEFT_LINES 万行代码"
echo ""

# 6. 代码质量指标
echo "📊 6. 代码质量指标"
echo "-------------------------------------"

if [ $MAIN_KT_LINES -gt 0 ]; then
    if command -v bc >/dev/null 2>&1; then
        TEST_COVERAGE=$(echo "scale=1; $TEST_KT_LINES * 100 / $MAIN_KT_LINES" | bc -l)
    else
        TEST_COVERAGE=$(awk "BEGIN {printf \"%.1f\", $TEST_KT_LINES * 100 / $MAIN_KT_LINES}")
    fi
    echo "🧪 测试覆盖率: ${TEST_COVERAGE}% (测试代码/源代码)"
else
    echo "🧪 测试覆盖率: 无法计算"
fi

echo "📋 文档化程度: $MD_FILES 个文档"
echo "🏗️  架构层数: 4层 (Domain/Data/Presentation/DI)"
echo ""

echo "====================================="
echo "✅ 统计完成"
echo "====================================="