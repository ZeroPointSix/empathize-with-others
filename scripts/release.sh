#!/bin/bash
# ============================================================
# 版本发布脚本 (Linux/Mac)
# 
# 用法:
#   ./release.sh                    - 使用默认阶段(dev)发布
#   ./release.sh --stage=beta       - 指定发布阶段
#   ./release.sh --dry-run          - 预览模式
#   ./release.sh --force            - 强制更新
#   ./release.sh --help             - 显示帮助
#
# @see TDD-00024 图标和版本号自动更新技术设计
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 默认参数
STAGE="dev"
DRY_RUN=""
FORCE=""

# 显示帮助
show_help() {
    echo ""
    echo "版本发布脚本 - 自动更新版本号和图标"
    echo ""
    echo "用法:"
    echo "  ./release.sh [选项]"
    echo ""
    echo "选项:"
    echo "  --stage=STAGE    发布阶段: dev, test, beta, production (默认: dev)"
    echo "  --dry-run        预览模式，不实际执行更新"
    echo "  --force          强制更新，忽略未提交的更改"
    echo "  --help, -h       显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  ./release.sh                       使用默认阶段(dev)发布"
    echo "  ./release.sh --stage=beta          发布beta版本"
    echo "  ./release.sh --stage=production    发布正式版本"
    echo "  ./release.sh --dry-run             预览版本变更"
    echo ""
}

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        --stage=*)
            STAGE="${1#*=}"
            shift
            ;;
        --dry-run)
            DRY_RUN="--dry-run"
            shift
            ;;
        --force)
            FORCE="--force"
            shift
            ;;
        --help|-h)
            show_help
            exit 0
            ;;
        *)
            print_error "未知参数: $1"
            show_help
            exit 1
            ;;
    esac
done

# 验证发布阶段
case $STAGE in
    dev|test|beta|production)
        ;;
    *)
        print_error "无效的发布阶段: $STAGE"
        echo "有效值: dev, test, beta, production"
        exit 1
        ;;
esac

# 显示配置
echo ""
echo "============================================================"
echo "                    版本发布脚本"
echo "============================================================"
echo ""
echo "发布阶段: $STAGE"
[[ -n "$DRY_RUN" ]] && echo "预览模式: 是"
[[ -n "$FORCE" ]] && echo "强制模式: 是"
echo ""

# 确认执行（非预览模式）
if [[ -z "$DRY_RUN" ]]; then
    print_warning "此操作将更新版本号和图标文件"
    echo ""
    read -p "确认继续? (y/N): " CONFIRM
    if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
        echo "操作已取消"
        exit 0
    fi
    echo ""
fi

# 检查Git状态
print_info "[1/4] 检查Git状态..."
if ! git status --porcelain > /dev/null 2>&1; then
    print_error "无法获取Git状态，请确保在Git仓库中运行"
    exit 1
fi

# 检查未提交的更改
if [[ -z "$FORCE" ]]; then
    if [[ -n $(git status --porcelain) ]]; then
        print_warning "存在未提交的更改"
        echo "请先提交或暂存更改，或使用 --force 参数强制执行"
        exit 1
    fi
fi

# 分析Git提交
print_info "[2/4] 分析Git提交..."
./gradlew analyzeCommits --no-daemon

# 构建参数
GRADLE_PARAMS="--stage=$STAGE $DRY_RUN $FORCE"

# 执行版本更新
echo ""
print_info "[3/4] 更新版本号和图标..."
./gradlew updateVersionAndIcon $GRADLE_PARAMS --no-daemon

# 显示结果
echo ""
print_info "[4/4] 显示当前版本..."
./gradlew showCurrentVersion --no-daemon

echo ""
echo "============================================================"
if [[ -n "$DRY_RUN" ]]; then
    echo "                    预览完成"
else
    print_success "                   发布完成!"
fi
echo "============================================================"
echo ""

# 提示后续操作
if [[ -z "$DRY_RUN" ]]; then
    echo "后续操作:"
    echo "  1. 检查更改: git diff"
    echo "  2. 提交更改: git add -A && git commit -m \"chore: 更新版本号\""
    echo "  3. 推送更改: git push"
    if [[ "$STAGE" == "production" ]]; then
        echo "  4. 创建标签: git tag -a v版本号 -m \"Release v版本号\""
        echo "  5. 推送标签: git push --tags"
    fi
    echo ""
fi

exit 0
