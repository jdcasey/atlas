package org.commonjava.maven.atlas.graph.rel;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.io.Serializable;
import java.net.URI;

/**
 * Created by jdcasey on 8/24/15.
 */
public interface ParentRelationship
        extends ProjectRelationship<ParentRelationship, ProjectVersionRef>,Serializable
{
    @Override
    ArtifactRef getTargetArtifact();

    boolean isTerminus();

    @Override
    ParentRelationship selectDeclaring( ProjectVersionRef ref );

    @Override
    ParentRelationship selectTarget( ProjectVersionRef ref );

    @Override
    ParentRelationship cloneFor( ProjectVersionRef declaring );
}
