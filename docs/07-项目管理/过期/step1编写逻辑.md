---
date_modified: 2025-12-02 20:13:37
---

### 第一步：Phase 1 (基础骨架) —— 只做 10%

先创建一个“容器”来存放你的代码。

你不可能在记事本里写 Java/Kotlin，你需要 IDE (Android Studio) 的环境支持。

- **你需要做的：**
    
    1. **新建项目：** Android Studio -> New Project -> Empty Activity.
        
    2. **建目录：** 对照我们的架构图，把 `domain/model`, `domain/repository`, `domain/usecase` 这些文件夹建好。
        
    3. **配依赖 (Gradle)：** 把 Hilt, Room, Retrofit, Coroutines 的库加进去。
        
- **不要做的：**
    
    - 不要现在去写悬浮窗服务（FloatingWindowService）。
        
    - 不要现在去写无障碍服务（AccessibilityService）。
        
    - _原因：这两块涉及 Android 系统权限和 UI 绘制，不仅繁琐，而且容易让你在还没写核心逻辑前就因为“窗口不显示”而受挫。_
        

### 第二步：编写业务层代码 (Domain Layer) —— 完成 40%

**这是你最擅长的部分，也是我们刚刚讨论的核心（数据结构+接口+UseCase）。**

- **为什么先写这个？**
    
    - **核心稳定：** 这是 App 的灵魂。这层写好了，无论你后面换什么数据库、换什么 UI，核心逻辑都不用动。
        
    - **编译即验证：** 你可以马上写完 `AnalyzeChatUseCase`，然后写一个简单的 Unit Test 跑通它。这对于后台开发者来说，**反馈循环（Feedback Loop）** 最快，最有成就感。
        
    - **C++ 类比：** 这就像先把 `.h` 头文件（接口）和的核心算法库（UseCase）写好。
        

### 第三步：编写数据层代码 (Data Layer) —— 完成 30%

**把接口变成具体的实现。**

- **你需要做的：**
    
    - 写 Room 的 `Entity` 和 `Dao` 实现 `ContactRepository`。
        
    - 写 Retrofit 的 `Service` 实现 `AiRepository`。
        
    - **最后配置 Hilt：** 把这些实现类注入到上面的 UseCase 里。
        

### 第四步：Phase 1 (Android 服务) —— 最后 20%

**这时候你的“大脑”和“手脚”都好了，最后再把它们装进 Android 的壳子里。**

- **你需要做的：**
    
    - 这时候再去写 `ScreenFetcher` (Accessibility) 和 `FloatingWindow`。
        
    - 把 UI 上的按钮点击事件，绑定到你已经写好并测试过的 `UseCase` 上。
        

---

### 总结建议

**现在的最佳行动：执行“第一步”和“第二步”。**

因为没有 Android Project 这个工程外壳，你的 Kotlin 代码没法编译，IDE 也不会给你语法提示。

**我们现在的任务清单 (Action Item)：**

1. 打开 Android Studio，**新建项目**。
    
2. 配置 `build.gradle.kts` (引入 Hilt/Room/Retrofit)。
    
3. 创建 `domain` 包结构。
    
4. **直接开始写 `ContactProfile.kt`, `AiRepository.kt`, `AnalyzeChatUseCase.kt`**。
    

