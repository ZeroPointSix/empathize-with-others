# 归档的高级功能模块

**归档时间**：2025-12-12  
**原因**：Git 回退后存在大量编译错误，暂时归档以快速恢复核心功能

## 归档的模块

### 1. 学习引擎 (learning/)
- `AdaptiveMappingStrategy.kt` - 自适应映射策略
- `FieldMappingLearningEngine.kt` - 字段映射学习引擎
- `LearningDataStore.kt` - 学习数据存储
- `ParsingPatternAnalyzer.kt` - 解析模式分析器

**功能**：AI 响应解析的自适应学习和优化

### 2. 优化管理 (optimization/)
- `AdaptivePerformanceOptimizer.kt` - 自适应性能优化器
- `DynamicLoadBalancer.kt` - 动态负载均衡器
- `OptimizationConfigManager.kt` - 优化配置管理器
- `OptimizationManager.kt` - 优化管理器
- `ResourceMonitor.kt` - 资源监控器

**功能**：性能优化和资源管理

### 3. 监控系统 (monitoring/)
- `AiResponseParserMetrics.kt` - 指标收集
- `HealthCheckSystem.kt` - 健康检查系统
- `MetricsRepository.kt` - 指标仓库

**功能**：系统监控和指标收集

### 4. 集成管理 (integration/)
- `AiResponseParserIntegrationManager.kt` - 集成管理器
- `AiResponseParserUsageExample.kt` - 使用示例

**功能**：模块集成和使用示例

### 5. 性能改进 (improvement/)
- `OptimizationRecommendationOptimizer.kt` - 优化推荐引擎
- `PerformanceRegressionDetector.kt` - 性能回归检测

**功能**：性能分析和优化建议

### 6. 资源管理 (resource/)
- `CacheManager.kt` - 缓存管理器
- `GarbageCollectionOptimizer.kt` - GC 优化器
- `MemoryOptimizer.kt` - 内存优化器
- `ResourcePool.kt` - 资源池

**功能**：内存和资源优化

### 7. 可观测性 (observability/)
- `DetailedLogger.kt` - 详细日志
- `DiagnosticCollector.kt` - 诊断收集器
- `ObservabilityManager.kt` - 可观测性管理器
- `ParsingTracer.kt` - 解析追踪器

**功能**：系统可观测性和诊断

### 8. 解析器高级功能 (parser/)
- `SmartFieldMapper.kt` - 智能字段映射器
- `ResponseParserFacade.kt` - 解析器门面
- `MultiLevelFallbackHandler.kt` - 多级回退处理器
- `AiResponseParserFactory.kt` - 解析器工厂
- `EnhancedStrategyBasedAiResponseParser.kt` - 增强策略解析器
- `StrategyBasedAiResponseParser.kt` - 策略解析器

**功能**：高级解析策略和回退机制

## 保留的核心功能

以下核心功能**保持可用**：

✅ AI 配置管理（AiProviderRepository, AiConfigViewModel）  
✅ AI 分析功能（AiRepositoryImpl, AnalyzeChatUseCase）  
✅ 联系人管理（ContactRepository, ContactViewModel）  
✅ 数据库层（AppDatabase, Entity, DAO）  
✅ 基础解析器（EnhancedJsonCleaner）  
✅ 设置功能（SettingsViewModel, SettingsScreen）  
✅ 悬浮窗功能（FloatingWindowService）

## 恢复计划

后续根据需要逐步恢复高级功能：

1. **评估优先级**：确定哪些高级功能是必需的
2. **逐个修复**：从依赖最少的模块开始修复
3. **测试验证**：每个模块恢复后进行充分测试
4. **文档更新**：更新相关文档和使用指南

## 编译错误统计

- **总错误数**：约 300+
- **主要错误类型**：
  - 未解析的引用
  - 类型不匹配
  - 参数缺失
  - 接口不一致

详细分析见：`.kiro/specs/compilation-errors-analysis.md`
