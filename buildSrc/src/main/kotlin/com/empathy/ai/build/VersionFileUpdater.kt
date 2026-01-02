package com.empathy.ai.build

import java.io.File

/**
 * 版本文件更新器接口
 * 支持不同类型项目的版本号同步
 * 
 * @see TDD-00024 4.5.1 VersionFileUpdater接口
 */
interface VersionFileUpdater {
    /** 更新器名称 */
    val name: String
    
    /** 支持的文件模式 */
    val supportedFiles: List<String>
    
    /**
     * 检查是否支持指定文件
     * @param file 目标文件
     * @return 是否支持
     */
    fun supports(file: File): Boolean
    
    /**
     * 更新版本号
     * @param file 目标文件
     * @param version 新版本号
     * @return 是否更新成功
     */
    fun updateVersion(file: File, version: SemanticVersion): Boolean
    
    /**
     * 读取当前版本号
     * @param file 目标文件
     * @return 当前版本号，如果无法读取则返回null
     */
    fun readVersion(file: File): SemanticVersion?
}
