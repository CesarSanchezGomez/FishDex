package com.cesarcosmico.fishdex;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Loads the connection-pool and the MySQL/MariaDB drivers at runtime via Paper's library loader instead
 * of shading them, so the plugin jar stays tiny. Paper caches libraries under {@code <server>/libraries/};
 * only the first launch needs network access.
 *
 * <p>The SQLite JDBC driver is bundled with Paper, so it is not resolved here. {@code slf4j-api} is
 * excluded — the server already provides it; a second copy would leave Hikari logging to a no-op
 * provider.</p>
 */
public final class FishDexLoader implements PluginLoader {

    // Keep the HikariCP version in sync with gradle.properties; the JDBC drivers are runtime-only.
    private static final List<String> LIBRARIES = List.of(
            "com.zaxxer:HikariCP:7.0.2",
            "org.mariadb.jdbc:mariadb-java-client:3.5.8",
            "com.mysql:mysql-connector-j:9.7.0");
    private static final List<Exclusion> EXCLUDE_SLF4J =
            List.of(new Exclusion("org.slf4j", "slf4j-api", "*", "*"));

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addRepository(new RemoteRepository.Builder(
                "central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build());
        for (String coordinates : LIBRARIES) {
            resolver.addDependency(new Dependency(new DefaultArtifact(coordinates), null, false, EXCLUDE_SLF4J));
        }
        classpathBuilder.addLibrary(resolver);
    }
}
