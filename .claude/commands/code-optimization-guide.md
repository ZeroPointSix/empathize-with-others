---
description: 分析代码库的性能瓶颈和优化机会，生成优化建议和路线图
argument-hint: [代码库路径或项目名称]
allowed-tools: Task, Glob, Grep, Read, Write, LSP, TodoWrite
---

# Code Optimization Guide

Execute performance and optimization analysis on the codebase.

## Analysis Scope

$ARGUMENTS

## Optimization Analysis Workflow

This command will:

1. **Identify Performance Bottlenecks**
   - Inefficient algorithms (O(n²) or worse)
   - Nested loops and iterations
   - N+1 query problems
   - Unnecessary database calls
   - Memory leaks or excessive allocations

2. **Analyze Data Structures**
   - Appropriate data structure usage
   - Collection size considerations
   - Caching opportunities
   - Index usage (for database operations)

3. **Evaluate I/O Operations**
   - File I/O efficiency
   - Network call optimization
   - Batch processing opportunities
   - Async/await usage

4. **Review Concurrency**
   - Thread safety issues
   - Lock contention
   - Parallel processing opportunities
   - Async pattern usage

5. **Assess Scalability**
   - Horizontal scaling potential
   - Vertical scaling opportunities
   - Caching strategies
   - Load balancing considerations

## Output

Generate optimization guide in `CODE_ANALYSIS/[project]/findings/`:
- performance_bottlenecks.md
- optimization_opportunities.md
- quick_wins.md
- long_term_improvements.md
- optimization_roadmap.md
