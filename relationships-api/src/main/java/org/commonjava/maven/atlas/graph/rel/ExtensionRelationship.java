package org.commonjava.maven.atlas.graph.rel;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.io.Serializable;

/**
 * Created by jdcasey on 8/24/15.
 */
public interface ExtensionRelationship
        extends ProjectRelationship<ExtensionRelationship, ProjectVersionRef>,Serializable
{
    @Override
    ArtifactRef getTargetArtifact();

    @Override
    ExtensionRelationship selectDeclaring( ProjectVersionRef ref );

    @Override
    ExtensionRelationship selectTarget( ProjectVersionRef ref );

    @Override
    ExtensionRelationship cloneFor( ProjectVersionRef declaring );
}
