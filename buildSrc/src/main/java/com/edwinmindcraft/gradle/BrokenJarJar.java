/*
 * ForgeGradle
 * Copyright (C) 2018 Forge Development LLC
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package com.edwinmindcraft.gradle;

import net.minecraftforge.gradle.userdev.dependency.DefaultDependencyFilter;
import net.minecraftforge.gradle.userdev.dependency.DependencyFilter;
import net.minecraftforge.gradle.userdev.jarjar.JarJarProjectExtension;
import net.minecraftforge.gradle.userdev.manifest.DefaultInheritManifest;
import net.minecraftforge.gradle.userdev.manifest.InheritManifest;
import net.minecraftforge.gradle.userdev.util.DeobfuscatingVersionUtils;
import net.minecraftforge.jarjar.metadata.*;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.gradle.api.Action;
import org.gradle.api.artifacts.*;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.bundling.Jar;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * This is a copy of FG's JarJar class, that can support subprojects in very simplistic ways.
 * Subprojects must be non-transitive, have both pins and ranges defined
 */
@SuppressWarnings("unused")
public abstract class BrokenJarJar extends Jar {
    private final List<Configuration> configurations;
    private transient DependencyFilter dependencyFilter;

    private FileCollection sourceSetsClassesDirs;

    private final ConfigurableFileCollection includedDependencies = this.getProject().files(new Callable<FileCollection>() {

        @Override
        public FileCollection call() {
            return BrokenJarJar.this.getProject().files(
                    BrokenJarJar.this.getResolvedDependencies().stream().flatMap(d -> d.getAllModuleArtifacts().stream()).map(ResolvedArtifact::getFile).toArray()
            );
        }
    });

    private final ConfigurableFileCollection metadata = this.getProject().files((Callable<FileCollection>) () -> {
        this.writeMetadata();
        return this.getProject().files(this.getJarJarMetadataPath().toFile());
    });

    private final CopySpec jarJarCopySpec;

    public BrokenJarJar() {
        super();
        this.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE); //As opposed to shadow, we do not filter out our entries early!, So we need to handle them accordingly.
        this.dependencyFilter = new DefaultDependencyFilter(this.getProject());
        this.setManifest(new DefaultInheritManifest(this.getServices().get(FileResolver.class)));
        this.configurations = new ArrayList<>();

        this.jarJarCopySpec = this.getMainSpec().addChild();
        this.jarJarCopySpec.into("META-INF/jarjar");
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    FileCollection getSourceSetsClassesDirs() {
        if (this.sourceSetsClassesDirs == null) {
            ConfigurableFileCollection allClassesDirs = this.getProject().getObjects().fileCollection();
            this.sourceSetsClassesDirs = allClassesDirs.filter(File::isDirectory);
        }
        return this.sourceSetsClassesDirs;
    }

    @Override
    public InheritManifest getManifest() {
        return (InheritManifest) super.getManifest();
    }

    @TaskAction
    protected void copy() {
        this.jarJarCopySpec.from(this.getIncludedDependencies());
        this.jarJarCopySpec.from(this.getMetadata());
        super.copy();
    }

    @Classpath
    public FileCollection getIncludedDependencies() {
        return this.includedDependencies;
    }

    @Internal
    public Set<ResolvedDependency> getResolvedDependencies() {
        return this.configurations.stream().flatMap(config -> config.getAllDependencies().stream())
                .map(this::getResolvedDependency)
                .filter(this.dependencyFilter::isIncluded)
                .collect(Collectors.toSet());
    }

    @Classpath
    public FileCollection getMetadata() {
        return this.metadata;
    }

    public BrokenJarJar dependencies(Action<DependencyFilter> c) {
        c.execute(this.dependencyFilter);
        return this;
    }

    @Classpath
    @org.gradle.api.tasks.Optional
    public List<Configuration> getConfigurations() {
        return this.configurations;
    }

    public void setConfigurations(List<Configuration> configurations) {
        this.configurations.clear();
        this.configurations.addAll(configurations);
    }

    @Internal
    public DependencyFilter getDependencyFilter() {
        return this.dependencyFilter;
    }

    public void setDependencyFilter(DependencyFilter filter) {
        this.dependencyFilter = filter;
    }

    public void configuration(@Nullable final Configuration configuration) {
        if (configuration == null) {
            return;
        }

        this.configurations.add(configuration);
    }

    public void fromRuntimeConfiguration() {
        final Configuration runtimeConfiguration = this.getProject().getConfigurations().findByName("runtimeClasspath");
        if (runtimeConfiguration != null) {
            this.configuration(runtimeConfiguration);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void writeMetadata() {
        final Path metadataPath = this.getJarJarMetadataPath();

        try {
            metadataPath.toFile().getParentFile().mkdirs();
            Files.deleteIfExists(metadataPath);
            Files.write(metadataPath, MetadataIOHandler.toLines(this.createMetadata()), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JarJar dependency metadata to disk.", e);
        }
    }

    private Path getJarJarMetadataPath() {
        return this.getProject().getBuildDir().toPath().resolve("jarjar").resolve(this.getName()).resolve("metadata.json");
    }

    private Metadata createMetadata() {
        return new Metadata(
                this.configurations.stream().flatMap(config -> config.getAllDependencies().stream())
                        .map(this::createDependencyMetadata)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList())
        );
    }

    private Optional<ContainedJarMetadata> createDependencyMetadata(final Dependency dependency) {
        if (!this.dependencyFilter.isIncluded(new DependencyFilter.ArtifactIdentifier(dependency.getGroup(), dependency.getName(), dependency.getVersion()))) {
            return Optional.empty();
        }

        if (!this.isValidVersionRange(Objects.requireNonNull(this.getVersionRangeFrom(dependency)))) {
            throw this.createInvalidVersionRangeException(dependency, null);
        }

        final ResolvedDependency resolvedDependency = this.getResolvedDependency(dependency);
        if (!this.dependencyFilter.isIncluded(resolvedDependency)) {
            //Skipping this file since the dependency filter does not want this to be included at all!
            return Optional.empty();
        }

        try {
            return Optional.of(new ContainedJarMetadata(
                    new ContainedJarIdentifier(dependency.getGroup(), dependency.getName()),
                    new ContainedVersion(
                            VersionRange.createFromVersionSpec(this.getVersionRangeFrom(dependency)),
                            new DefaultArtifactVersion(DeobfuscatingVersionUtils.adaptDeobfuscatedVersion(resolvedDependency.getModuleVersion()))
                    ),
                    "META-INF/jarjar/" + resolvedDependency.getAllModuleArtifacts().iterator().next().getFile().getName(),
                    this.isObfuscated(dependency)
            ));
        } catch (InvalidVersionSpecificationException e) {
            throw this.createInvalidVersionRangeException(dependency, e);
        }
    }

    private RuntimeException createInvalidVersionRangeException(final Dependency dependency, final Throwable cause) {
        return new RuntimeException("The given version specification is invalid: " + this.getVersionRangeFrom(dependency)
                                    + ". If you used gradle based range versioning like 2.+, convert this to a maven compatible format: [2.0,3.0).", cause);
    }

    private String getVersionRangeFrom(final Dependency dependency) {
        final Optional<String> attributeVersion = this.getProject().getExtensions().getByType(JarJarProjectExtension.class).getRange(dependency);

        return attributeVersion.map(DeobfuscatingVersionUtils::adaptDeobfuscatedVersion).orElseGet(() -> DeobfuscatingVersionUtils.adaptDeobfuscatedVersion(Objects.requireNonNull(dependency.getVersion())));
    }

    private String getVersionFrom(final Dependency dependency, final ResolvedDependency resolvedDependency) {
        final Optional<String> attributeVersion = this.getProject().getExtensions().getByType(JarJarProjectExtension.class).getPin(dependency);

        return attributeVersion.map(DeobfuscatingVersionUtils::adaptDeobfuscatedVersion).orElseGet(() -> DeobfuscatingVersionUtils.adaptDeobfuscatedVersion(Objects.requireNonNull(resolvedDependency.getModuleVersion())));
    }

    private String getVersionFrom(final Dependency dependency) {
        final Optional<String> attributeVersion = this.getProject().getExtensions().getByType(JarJarProjectExtension.class).getPin(dependency);

        return attributeVersion.map(DeobfuscatingVersionUtils::adaptDeobfuscatedVersion).orElseGet(() -> DeobfuscatingVersionUtils.adaptDeobfuscatedVersion(Objects.requireNonNull(dependency.getVersion())));
    }

    private ResolvedDependency getResolvedDependency(final Dependency dependency) {
        Dependency toResolve = dependency.copy();
        if (toResolve instanceof ExternalModuleDependency)
            ((ExternalModuleDependency) toResolve).version(constraint -> constraint.strictly(this.getVersionFrom(dependency)));

        final Set<ResolvedDependency> deps = this.getProject().getConfigurations().detachedConfiguration(toResolve).getResolvedConfiguration().getFirstLevelModuleDependencies();
        if (deps.isEmpty()) {
            throw new IllegalArgumentException(String.format("Failed to resolve: %s", toResolve));
        }

        return deps.iterator().next();
    }

    private boolean isObfuscated(final Dependency dependency) {
        return Objects.requireNonNull(dependency.getVersion()).contains("_mapped_");
    }

    private boolean isValidVersionRange(final String range) {
        try {
            final VersionRange data = VersionRange.createFromVersionSpec(range);
            return data.hasRestrictions() && data.getRecommendedVersion() == null && !range.contains("+");
        } catch (InvalidVersionSpecificationException e) {
            return false;
        }
    }
}
