package com.groovy.lsp.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * Groovyプロジェクトの共通設定を提供するコンベンションプラグイン
 * 
 * このプラグインは以下の機能を提供します：
 * - Groovyバージョンの競合解決
 * - Groovyコンパイルオプションの設定
 * - Apache Groovy vs Codehaus Groovyの競合解決
 */
class GroovyConventionPlugin implements Plugin<Project> {
    
    @Override
    void apply(Project project) {
        // Groovy pluginを適用
        project.plugins.apply('groovy')
        
        configureGroovyCompileOptions(project)
        configureGroovyVersionResolution(project)
    }
    
    /**
     * Groovyコンパイルオプションを設定
     */
    private void configureGroovyCompileOptions(Project project) {
        project.tasks.withType(org.gradle.api.tasks.compile.GroovyCompile) {
            options.encoding = 'UTF-8'
            groovyOptions.parameters = true
        }
    }
    
    /**
     * Groovyバージョンの競合を解決
     */
    private void configureGroovyVersionResolution(Project project) {
        project.configurations.all { Configuration config ->
            config.resolutionStrategy { strategy ->
                // libs.versions.tomlからGroovyバージョンを取得
                def groovyVersion = project.extensions
                    .getByName('libs')
                    .versions
                    .groovy
                    .get()
                
                // Apache Groovyモジュールのバージョンを統一
                def groovyModules = [
                    'groovy',
                    'groovy-json',
                    'groovy-xml',
                    'groovy-nio',
                    'groovy-templates',
                    'groovy-all',
                    'groovy-ant',
                    'groovy-groovydoc',
                    'groovy-docgenerator',
                    'groovy-sql',
                    'groovy-test',
                    'groovy-swing',
                    'groovy-console',
                    'groovy-dateutil',
                    'groovy-datetime',
                    'groovy-macro',
                    'groovy-jsr223',
                    'groovy-servlet',
                    'groovy-groovysh',
                    'groovy-testng'
                ]
                
                groovyModules.each { module ->
                    strategy.force "org.apache.groovy:${module}:${groovyVersion}"
                }
                
                // Codehaus Groovy -> Apache Groovy への自動変換
                strategy.eachDependency { details ->
                    if (details.requested.group == 'org.codehaus.groovy') {
                        def moduleName = details.requested.name
                        details.useTarget("org.apache.groovy:${moduleName}:${groovyVersion}")
                        details.because('Standardizing on Apache Groovy')
                    }
                }
                
                // Capability競合の解決
                config.resolutionStrategy.capabilitiesResolution {
                    // Groovy本体の競合解決
                    withCapability('org.codehaus.groovy:groovy') {
                        selectHighestVersion()
                    }
                    withCapability('org.apache.groovy:groovy') {
                        select(candidates.find { it.version == groovyVersion })
                    }
                }
            }
        }
    }
}