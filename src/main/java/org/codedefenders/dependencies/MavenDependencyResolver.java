package org.codedefenders.dependencies;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.util.graph.visitor.CloningDependencyVisitor;
import org.eclipse.aether.util.graph.visitor.PreorderDependencyNodeConsumerVisitor;

/**
 * Resolves and installs Maven artifacts with their transitive dependencies.
 * @see <a href="https://maven.apache.org/repositories/artifacts.html">Maven Artifacts</a>
 * @see <a href="https://maven.apache.org/resolver/maven-resolver-api/index.html">Maven Resolver Docs</a>
 * @see <a href="https://maven.apache.org/resolver/maven-resolver-api/apidocs/index.html">Maven Resolver JavaDocs</a>
 * @see <a href="https://maven.apache.org/resolver/apidocs/org/eclipse/aether/util/package-summary.html">
 *     Maven Resolver Utils JavaDocs</a>
 */
public class MavenDependencyResolver implements AutoCloseable {
    protected final Path localRepoBase;
    protected final String javaVersion;
    protected final RepositorySystem system;
    protected final CloseableSession session;

    /**
     * @param localRepoBase Base directory of the local Maven repository. Artifacts are installed there.
     * @param javaVersion Java version to search for when resolving artifacts.
     */
    public MavenDependencyResolver(Path localRepoBase, String javaVersion) {
        this.localRepoBase = localRepoBase;
        this.javaVersion = javaVersion;
        this.system = new RepositorySystemSupplier().get();

        Map<String, String> systemProperties = new HashMap<>();
        systemProperties.put("java.version", javaVersion);
        this.session = system.createSessionBuilder()
                .setSystemProperties(systemProperties)
                .setDependencySelector(new DependencySelector() {
                    @Override
                    public boolean selectDependency(Dependency dependency) {
                        return !dependency.isOptional() && dependency.getScope().equals("runtime");
                    }

                    @Override
                    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
                        return this;
                    }
                })
                .withLocalRepositoryBaseDirectories(localRepoBase)
                .build();
    }

    /**
     * Resolves and installs a collection of artifact specs and their transitive dependencies.
     * @return A collection of artifacts representing the artifacts and their dependencies.
     *     All artifacts point to an installed JAR file in the local repo.
     */
    public MavenDependencies resolveDependencies(Collection<Artifact> artifacts) throws MavenDependencyResolverException {
        Set<Artifact> resolvedDependencies = new HashSet<>();

        // CollectRequest finds the transitive dependencies and installs their POMs.
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRepositories(getRemoteRepos());
        collectRequest.setDependencies(
            artifacts.stream().map(artifact -> new Dependency(artifact, "runtime", false)).toList());
        CollectResult collectResult;
        try {
            collectResult = system.collectDependencies(session, collectRequest);
        } catch (DependencyCollectionException e) {
            throw new MavenDependencyResolverException(e);
        }

        DependencyNode root = setJarTarget(collectResult.getRoot());

        // DependencyRequest resolves and installs each artifact in the dependency tree.
        DependencyRequest dependencyRequest = new DependencyRequest();
        dependencyRequest.setRoot(root);
        try {
            system.resolveDependencies(session, dependencyRequest);
        } catch (DependencyResolutionException e) {
            throw new MavenDependencyResolverException(e);
        }

        // Gather resolved dependencies.
        var visitor = new PreorderDependencyNodeConsumerVisitor(node -> {
            var artifact = node.getArtifact();
            if (artifact != null) {
                resolvedDependencies.add(artifact);
            }
        });
        root.accept(visitor);

        return new MavenDependencies(resolvedDependencies);
    }

    protected List<RemoteRepository> getRemoteRepos() {
        return List.of(
            new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build()
        );
    }

    /**
     * Parses Maven artifact specs of the form
     * {@code <groupId>:<artifactId>:<version>}.
     */
    public static Artifact parseArtifactSpec(String spec) {
        var split = spec.split(":");
        if (split.length != 3) {
            throw new IllegalArgumentException(String.format("Invalid artifact spec: '%s'", spec));
        }
        String groupId = split[0];
        String artifactId = split[1];
        String version = split[2];
        return new DefaultArtifact(groupId, artifactId, null, version);
    }

    /**
     * Formats Maven artifact specs of the form
     * {@code <groupId>:<artifactId>:<version>}.
     */
    public static String formatArtifactSpec(Artifact spec) {
        return String.format("%s:%s:%s",
            spec.getGroupId(),
            spec.getArtifactId(),
            spec.getVersion()
        );
    }

    /**
     * Sets each node in a dependency tree to target the JAR artifact.
     */
    private DependencyNode setJarTarget(DependencyNode node) {
        var transformer = new CloningDependencyVisitor() {
            @Override
            protected DependencyNode clone(DependencyNode node) {
                var clone = super.clone(node);
                if (clone.getArtifact() != null) {
                    var oldArtifact = clone.getArtifact();
                    var newArtifact = new DefaultArtifact(
                        oldArtifact.getGroupId(),
                        oldArtifact.getArtifactId(),
                        "jar",
                        oldArtifact.getVersion());
                    clone.setArtifact(newArtifact);
                }
                return clone;
            }
        };
        node.accept(transformer);
        return transformer.getRootNode();
    }

    public void close() {
        session.close();
        system.close();
    }

    /**
     * Result class that represents the resolved dependencies.
     */
    public record MavenDependencies(Collection<Artifact> dependencies) {
        public String getClasspath() {
            return dependencies.stream()
                    .map(Artifact::getPath)
                    .map(Path::toString)
                    .collect(Collectors.joining(Character.toString(File.pathSeparatorChar)));
        }
    }

    public class MavenDependencyResolverException extends Exception {
        public MavenDependencyResolverException(Throwable cause) {
            super(cause);
        }
    }
}
