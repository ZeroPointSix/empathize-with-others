---
description: 对代码库进行架构层面的深度分析，包括设计模式、模块耦合、分层结构等
argument-hint: [代码库路径或项目名称]
allowed-tools: Task, Glob, Grep, Read, Write, LSP, TodoWrite
---

# Code Architecture Analysis

Execute focused architecture analysis on the codebase.

## Analysis Scope

$ARGUMENTS

## Architecture Analysis Workflow

This command will:

1. **Identify Architectural Patterns**
   - Recognize architectural styles (layered, hexagonal, microservices, etc.)
   - Pattern consistency assessment
   - Appropriateness evaluation

2. **Analyze Module Structure**
   - Package/directory organization
   - Module boundaries and responsibilities
   - Separation of concerns
   - Layering strategy

3. **Evaluate Coupling and Cohesion**
   - Tight coupling detection
   - Cohesion assessment
   - Circular dependency identification
   - Import/export pattern analysis

4. **Detect Design Patterns**
   - GoF pattern identification
   - Anti-pattern detection (god classes, singletons, spaghetti code)
   - Pattern quality assessment

5. **Identify Architectural Violations**
   - Architecture drift detection
   - Bypassed abstractions
   - Boundary violations

## Output

Generate structured architecture analysis in `CODE_ANALYSIS/[project]/architecture/`:
- design_patterns.md
- module_structure.md
- dependency_graph.md
- architectural_violations.md
- recommendations.md
