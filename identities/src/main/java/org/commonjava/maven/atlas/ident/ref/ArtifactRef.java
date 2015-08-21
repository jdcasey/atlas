package org.commonjava.maven.atlas.ident.ref;

import java.io.Serializable;

/**
 * Created by jdcasey on 8/21/15.
 */
public interface ArtifactRef
        extends ProjectVersionRef, Serializable, Comparable<ProjectRef>, VersionedRef<ProjectVersionRef>
{
    String getType();

    String getClassifier();

    TypeAndClassifier getTypeAndClassifier();

    boolean isOptional();

    boolean versionlessEquals( ProjectVersionRef other );
}
