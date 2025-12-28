---
description: 分析代码库的技术债务，包括代码异味、反模式、待改进项等
argument-hint: [代码库路径或项目名称]
allowed-tools: Task, Glob, Grep, Read, Write, LSP, TodoWrite
---

# Code Technical Debt Analysis

Execute technical debt analysis on the codebase.

## Analysis Scope

$ARGUMENTS

## Technical Debt Analysis Workflow

This command will:

1. **Detect Code Smells**
   - Long methods/functions
   - God classes/modules
   - Duplicate code
   - Dead code (unused functions, variables)
   - Complex conditional logic

2. **Identify Anti-Patterns**
   - Singleton abuse
   - Spaghetti code
   - Golden hammer
   - Magic numbers and strings
   - Hard-coded values

3. **Analyze TODO/FIXME Comments**
   - Catalog all TODO, FIXME, HACK, XXX comments
   - Assess priority and impact
   - Categorize by type (bug, feature, refactor, optimization)

4. **Evaluate Code Quality Metrics**
   - Cyclomatic complexity
   - Maintainability index
   - Code duplication percentage
   - Test coverage gaps

5. **Assess Deprecated Code**
   - Identify deprecated functions/methods
   - Find usage of deprecated APIs
   - Check for outdated dependencies

## Output

Generate technical debt inventory in `CODE_ANALYSIS/[project]/findings/`:
- code_smells.md
- anti_patterns.md
- todo_inventory.md
- deprecated_code.md
- prioritized_backlog.md
