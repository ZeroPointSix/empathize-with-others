# Claude Code Skill 诊断报告

> 生成时间: {{TIMESTAMP}}
> 诊断版本: v1.0.0

---

## 📊 诊断摘要

| 项目 | 结果 |
|------|------|
| **总体状态** | {{STATUS}} |
| **发现问题数** | {{ISSUE_COUNT}} |
| **已修复问题数** | {{FIXED_COUNT}} |
| **待处理问题数** | {{PENDING_COUNT}} |

---

## 🔍 检查结果详情

### 1. 全环境检查

**Claude Code 版本**: {{CLAUDE_VERSION}}
**当前用户**: {{CURRENT_USER}}
**主目录**: {{HOME_DIR}}
**权限状态**: {{PERMISSION_STATUS}}
**网络连接**: {{NETWORK_STATUS}}

---

### 2. 技能文件位置

**找到的技能目录**:
```
{{FOUND_DIRECTORIES}}
```

**找到的技能文件**:
```
{{FOUND_FILES}}
```

---

### 3. 文件验证结果

#### YAML 头部检查
| 文件 | YAML 格式 | name | description | 编码 | 权限 |
|------|----------|------|-------------|------|------|
| {{FILE_1}} | {{YAML_STATUS}} | {{NAME_STATUS}} | {{DESC_STATUS}} | {{ENCODING}} | {{PERMS}} |
| {{FILE_2}} | {{YAML_STATUS}} | {{NAME_STATUS}} | {{DESC_STATUS}} | {{ENCODING}} | {{PERMS}} |

#### 目录结构检查
| 目录 | 权限 | 结构 | 状态 |
|------|------|------|------|
| {{DIR_1}} | {{DIR_PERMS}} | {{STRUCTURE}} | {{DIR_STATUS}} |
| {{DIR_2}} | {{DIR_PERMS}} | {{STRUCTURE}} | {{DIR_STATUS}} |

---

## 🐛 发现的问题

### 🔴 Critical (严重)

1. **{{CRITICAL_ISSUE_1}}**
   - 位置: `{{LOCATION}}`
   - 原因: {{REASON}}
   - 修复方案: {{FIX}}
   - ✅ 已修复 / ⏳ 待处理

### 🟠 Important (重要)

1. **{{IMPORTANT_ISSUE_1}}**
   - 位置: `{{LOCATION}}`
   - 原因: {{REASON}}
   - 修复方案: {{FIX}}
   - ✅ 已修复 / ⏳ 待处理

### 🟡 Minor (轻微)

1. **{{MINOR_ISSUE_1}}**
   - 位置: `{{LOCATION}}`
   - 建议: {{SUGGESTION}}
   - ✅ 已修复 / ⏳ 待处理

---

## 🔧 修复操作记录

### 已执行的修复操作

1. **{{FIX_1_TITLE}}**
   ```bash
   {{COMMAND}}
   ```
   结果: {{RESULT}}

2. **{{FIX_2_TITLE}}**
   ```bash
   {{COMMAND}}
   ```
   结果: {{RESULT}}

---

## 📝 修复清单

### 问题汇总

| # | 问题 | 严重级别 | 状态 | 修复方法 |
|---|------|---------|------|---------|
| 1 | {{ISSUE_1}} | Critical | ✅ 已修复 | {{METHOD}} |
| 2 | {{ISSUE_2}} | Important | ⏳ 待处理 | {{METHOD}} |
| 3 | {{ISSUE_3}} | Minor | ✅ 已修复 | {{METHOD}} |

### 待办事项

- [ ] {{TODO_1}}
- [ ] {{TODO_2}}
- [ ] {{TODO_3}}

---

## 🎯 建议

### 立即行动
{{IMMEDIATE_ACTIONS}}

### 后续优化
{{OPTIMIZATION_SUGGESTIONS}}

### 预防措施
{{PREVENTION_MEASURES}}

---

## 📚 参考资料

- 官方文档: https://support.claude.com/en/articles/12512198-how-to-create-custom-skills
- Skill 规范: `/skills/README.md`
- 配置示例: `/skills/skill-rules.json`

---

**报告生成者**: Claude Code Skill Diagnostic Agent
**下次检查建议**: {{NEXT_CHECK_DATE}}
