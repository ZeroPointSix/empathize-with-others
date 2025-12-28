---
description: 对指定代码库执行完整的深度分析流程，从问题细化到最终分析报告生成
argument-hint: [代码分析主题或问题]
allowed-tools: Task, Glob, Grep, Read, Write, LSP, TodoWrite, Bash
---

# Code Deep Research

Execute comprehensive deep code analysis on the given codebase using the 7-phase analysis methodology and Graph of Thoughts framework.

## Analysis Topic

$ARGUMENTS

## Code Analysis Workflow

This command will execute the following steps:

### Step 1: Question Refinement
Use the **code-question-refiner** skill to ask clarifying questions and generate a structured analysis prompt.

### Step 2: Analysis Planning
Break down the analysis topic into 3-7 dimensions and create a detailed execution plan:
- Architecture & Design
- Code Quality & Maintainability
- Dependencies & Coupling
- Performance & Scalability
- Security & Vulnerabilities
- Testing & Coverage
- Technical Debt & Code Smells

### Step 3: Multi-Agent Code Analysis
Deploy multiple parallel analysis agents to gather information from different aspects:
- **Architecture Analysis Agents (2-3 agents)**: Design patterns, layering, module boundaries
- **Code Quality Agents (2-3 agents)**: Complexity, maintainability, code smells
- **Dependency Analysis Agents (1-2 agents)**: Third-party libraries, internal dependencies
- **Performance Analysis Agents (1-2 agents)**: Algorithms, data structures, bottlenecks
- **Security Analysis Agents (1 agent)**: Vulnerabilities, input validation
- **Testing Analysis Agents (1 agent)**: Test coverage, test quality

### Step 4: Code Triangulation
Compare findings across multiple agents and validate claims using code references with file:line format.

### Step 5: Knowledge Synthesis
Use the **code-synthesizer** skill to combine findings into a coherent report with inline code references.

### Step 6: Quality Assurance
Verify all findings have accurate, complete code references and validate severity assessments.

### Step 7: Output Generation
Generate structured analysis outputs in the `CODE_ANALYSIS/[project]/` directory:
- README.md
- executive_summary.md
- full_analysis_report.md
- architecture/ (design patterns, module structure, dependency graph)
- code_quality/ (complexity, code smells, maintainability)
- findings/ (critical issues, warnings, recommendations)
- visualizations/ (diagram descriptions)
- appendices/ (methodology, file list, limitations)

## Code Reference Requirements

Ensure every finding includes:
1. File path and line number(s)
2. Function, class, or module name
3. Code snippet (if relevant)
4. Impact assessment (severity/priority)
5. Recommended action

**Example Citation**:
```markdown
The authentication module has a potential SQL injection vulnerability in `src/auth/login.py:45-52`:
[High Severity] The `user_input` variable is directly concatenated into the SQL query without parameterization.
Recommendation: Use parameterized queries or prepared statements.
```

## Analysis Dimensions

This command can analyze multiple dimensions:

- **Architecture**: Design patterns, layering, module boundaries, coupling, separation of concerns
- **Code Quality**: Complexity, maintainability, readability, naming conventions, code smells
- **Dependencies**: Third-party libraries, internal dependencies, circular dependencies
- **Performance**: Bottlenecks, algorithms, data structures, database queries
- **Security**: Vulnerabilities, sensitive data handling, authentication/authorization
- **Testing**: Test coverage, test quality, test architecture
- **Technical Debt**: Code smells, anti-patterns, TODO/FIXME comments, deprecated code

Begin the code deep analysis process now.
