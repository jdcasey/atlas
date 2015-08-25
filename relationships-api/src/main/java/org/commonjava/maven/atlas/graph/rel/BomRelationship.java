package org.commonjava.maven.atlas.graph.rel;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.io.Serializable;

/**
 * Created by jdcasey on 8/24/15.
 */
public interface BomRelationship
        extends ProjectRelationship<BomRelationship, ProjectVersionRef>,Serializable
{
    @Override
    ArtifactRef getTargetArtifact();

    @Override
    BomRelationship selectDeclaring( ProjectVersionRef ref );

    @Override
    BomRelationship selectTarget( ProjectVersionRef ref );

    @Override
    BomRelationship cloneFor( ProjectVersionRef declaring );
}
