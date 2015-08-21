package org.commonjava.maven.atlas.ident.ref;

import java.io.Serializable;

/**
 * Created by jdcasey on 8/21/15.
 */
public interface ProjectRef
        extends Serializable, Comparable<ProjectRef>
{
    String getGroupId();

    String getArtifactId();

    ProjectRef asProjectRef();

    VersionlessArtifactRef asVersionlessPomArtifact();

    VersionlessArtifactRef asVersionlessJarArtifact();

    VersionlessArtifactRef asVersionlessArtifactRef( String type, String classifier );

    VersionlessArtifactRef asVersionlessArtifactRef( String type, String classifier, boolean optional );

    VersionlessArtifactRef asVersionlessArtifactRef( TypeAndClassifier tc );

    VersionlessArtifactRef asVersionlessArtifactRef( TypeAndClassifier tc, boolean optional );

    boolean matches( ProjectRef ref );
}
