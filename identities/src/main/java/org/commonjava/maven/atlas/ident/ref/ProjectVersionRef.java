package org.commonjava.maven.atlas.ident.ref;

import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.maven.atlas.ident.version.VersionSpec;

import java.io.Serializable;

/**
 * Created by jdcasey on 8/21/15.
 */
public interface ProjectVersionRef
        extends ProjectRef, VersionedRef<ProjectVersionRef>,Serializable
{
    ProjectVersionRef asProjectVersionRef();

    ArtifactRef asPomArtifact();

    ArtifactRef asJarArtifact();

    ArtifactRef asArtifactRef( String type, String classifier );

    ArtifactRef asArtifactRef( String type, String classifier, boolean optional );

    ArtifactRef asArtifactRef( TypeAndClassifier tc );

    ArtifactRef asArtifactRef( TypeAndClassifier tc, boolean optional );

    VersionSpec getVersionSpecRaw();

    String getVersionStringRaw();

    @Override
    boolean isRelease();

    @Override
    boolean isSpecificVersion();

    @Override
    boolean matchesVersion( SingleVersion version );

    @Override
    ProjectVersionRef selectVersion( String version );

    @Override
    ProjectVersionRef selectVersion( String version, boolean force );

    @Override
    ProjectVersionRef selectVersion( SingleVersion version );

    @Override
    ProjectVersionRef selectVersion( SingleVersion version, boolean force );

    ProjectVersionRef newRef( String groupId, String artifactId, SingleVersion version );

    @Override
    VersionSpec getVersionSpec();

    boolean versionlessEquals( ProjectVersionRef other );

    @Override
    boolean isCompound();

    @Override
    boolean isSnapshot();

    @Override
    String getVersionString();

    boolean isVariableVersion();
}
