package com.groovy.lsp.groovy.core.api;

import java.util.List;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.jmolecules.ddd.annotation.Factory;

/**
 * Service interface for creating Groovy compiler configurations.
 * Provides factory methods for different compilation scenarios.
 *
 * This is a factory service that creates properly configured
 * CompilerConfiguration instances for various use cases.
 */
@Factory
public interface CompilerConfigurationService {

    /**
     * Creates a default compiler configuration optimized for LSP.
     *
     * @return the default compiler configuration
     */
    CompilerConfiguration createDefaultConfiguration();

    /**
     * Creates a compiler configuration with specified classpath.
     *
     * @param classpath list of classpath entries
     * @return the configured compiler configuration
     */
    CompilerConfiguration createConfigurationWithClasspath(List<String> classpath);

    /**
     * Creates a configuration optimized for script compilation.
     *
     * @return the script compiler configuration
     */
    CompilerConfiguration createScriptConfiguration();

    /**
     * Creates a configuration with type checking enabled.
     *
     * @param staticTypeChecking whether to enable static type checking
     * @return the type checking compiler configuration
     */
    CompilerConfiguration createTypeCheckingConfiguration(boolean staticTypeChecking);
}
