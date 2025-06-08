package com.groovy.lsp.workspace.dependency;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.jspecify.annotations.Nullable;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves dependencies for Maven projects using Maven Resolver API.
 */
public class MavenDependencyResolver implements DependencyResolver {
    private static final Logger logger = LoggerFactory.getLogger(MavenDependencyResolver.class);

    private final Path workspaceRoot;
    @Nullable private RepositorySystem repositorySystem;
    @Nullable private RepositorySystemSession session;
    @Nullable private List<RemoteRepository> repositories;

    public MavenDependencyResolver(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
        initializeMavenResolver();
    }

    @Override
    public boolean canHandle(Path workspaceRoot) {
        return Files.exists(workspaceRoot.resolve("pom.xml"));
    }

    @Override
    public BuildSystem getBuildSystem() {
        return BuildSystem.MAVEN;
    }

    @Override
    public List<Path> resolveDependencies() {
        Path pomPath = workspaceRoot.resolve("pom.xml");
        if (!Files.exists(pomPath)) {
            logger.warn("No pom.xml found at: {}", workspaceRoot);
            return Collections.emptyList();
        }

        if (repositorySystem == null || session == null || repositories == null) {
            logger.error("Maven resolver not properly initialized");
            return Collections.emptyList();
        }

        try {
            // Parse pom.xml
            Model model = parsePomFile(pomPath);
            if (model == null) {
                return Collections.emptyList();
            }

            // Resolve dependencies
            List<Path> dependencies = new ArrayList<>();

            // Create collect request
            CollectRequest collectRequest = new CollectRequest();

            // Add project dependencies
            for (Dependency dep : model.getDependencies()) {
                if (!"test".equals(dep.getScope())) {
                    Artifact artifact =
                            new DefaultArtifact(
                                    dep.getGroupId(),
                                    dep.getArtifactId(),
                                    dep.getClassifier(),
                                    dep.getType() != null ? dep.getType() : "jar",
                                    dep.getVersion());
                    collectRequest.addDependency(
                            new org.eclipse.aether.graph.Dependency(artifact, dep.getScope()));
                }
            }

            // Set repositories
            collectRequest.setRepositories(repositories);

            // Create dependency request with runtime scope filter
            DependencyFilter classpathFilter =
                    DependencyFilterUtils.classpathFilter(
                            JavaScopes.COMPILE, JavaScopes.RUNTIME, JavaScopes.PROVIDED);

            DependencyRequest dependencyRequest =
                    new DependencyRequest(collectRequest, classpathFilter);

            // Resolve dependencies
            DependencyResult result =
                    repositorySystem.resolveDependencies(session, dependencyRequest);

            // Extract paths from resolved artifacts
            for (ArtifactResult artifactResult : result.getArtifactResults()) {
                if (artifactResult.isResolved() && artifactResult.getArtifact() != null) {
                    File file = artifactResult.getArtifact().getFile();
                    if (file != null && file.exists()) {
                        dependencies.add(file.toPath());
                        logger.debug("Resolved dependency: {}", file);
                    }
                }
            }

            return new ArrayList<>(dependencies);

        } catch (Exception e) {
            logger.error("Failed to resolve Maven dependencies", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Path> getSourceDirectories() {
        List<Path> sourceDirs = new ArrayList<>();

        // Standard Maven layout
        addIfExists(sourceDirs, "src/main/groovy");
        addIfExists(sourceDirs, "src/main/java");
        addIfExists(sourceDirs, "src/test/groovy");
        addIfExists(sourceDirs, "src/test/java");

        // TODO: Parse pom.xml to find custom source directories

        return sourceDirs;
    }

    private void initializeMavenResolver() {
        try {
            // Create service locator - need to use MavenRepositorySystemUtils
            repositorySystem = newRepositorySystem();

            // Create session
            DefaultRepositorySystemSession systemSession = MavenRepositorySystemUtils.newSession();

            // Set local repository
            String userHome = System.getProperty("user.home");
            Path localRepoPath = Path.of(userHome, ".m2", "repository");
            LocalRepository localRepo = new LocalRepository(localRepoPath.toFile());
            systemSession.setLocalRepositoryManager(
                    repositorySystem.newLocalRepositoryManager(systemSession, localRepo));

            // Configure session
            systemSession.setOffline(false);
            systemSession.setChecksumPolicy("warn");
            systemSession.setUpdatePolicy("daily");

            session = systemSession;

            // Setup repositories
            repositories = new ArrayList<>();

            // Add Maven Central
            RemoteRepository central =
                    new RemoteRepository.Builder(
                                    "central", "default", "https://repo.maven.apache.org/maven2/")
                            .build();
            repositories.add(central);

            logger.info("Maven resolver initialized with local repository: {}", localRepoPath);

        } catch (Exception e) {
            logger.error("Failed to initialize Maven resolver", e);
        }
    }

    private static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        return locator.getService(RepositorySystem.class);
    }

    @Nullable
    private Model parsePomFile(Path pomPath) {
        try (var reader = Files.newBufferedReader(pomPath, StandardCharsets.UTF_8)) {
            MavenXpp3Reader pomReader = new MavenXpp3Reader();
            Model model = pomReader.read(reader);

            // Handle parent POM if present
            if (model.getParent() != null) {
                // For simplicity, we're not resolving parent POMs in this implementation
                // In a production system, you would need to resolve and merge parent POMs
                logger.debug(
                        "Parent POM detected but not resolved: {}:{}:{}",
                        model.getParent().getGroupId(),
                        model.getParent().getArtifactId(),
                        model.getParent().getVersion());
            }

            return model;
        } catch (Exception e) {
            logger.error("Failed to parse pom.xml at: {}", pomPath, e);
            return null;
        }
    }

    private void addIfExists(List<Path> list, String relativePath) {
        Path path = workspaceRoot.resolve(relativePath);
        if (Files.exists(path) && Files.isDirectory(path)) {
            list.add(path);
        }
    }
}
