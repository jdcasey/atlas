package org.commonjava.maven.atlas.ident.ref;

/**
 * Created by jdcasey on 8/21/15.
 */
public interface VersionlessArtifactRef
        extends ProjectRef
{
    String getType();

    String getClassifier();

    TypeAndClassifier getTypeAndClassifier();

    boolean isOptional();

    @Override
    VersionlessArtifactRef asVersionlessPomArtifact();

    @Override
    VersionlessArtifactRef asVersionlessJarArtifact();

    @Override
    VersionlessArtifactRef asVersionlessArtifactRef( String type, String classifier );

    @Override
    VersionlessArtifactRef asVersionlessArtifactRef( String type, String classifier, boolean optional );

    @Override
    VersionlessArtifactRef asVersionlessArtifactRef( TypeAndClassifier tc );

    @Override
    VersionlessArtifactRef asVersionlessArtifactRef( TypeAndClassifier tc, boolean optional );
}
