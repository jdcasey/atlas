package org.commonjava.maven.atlas.graph.rel;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.io.Serializable;

/**
 * Created by jdcasey on 8/24/15.
 */
public interface PluginRelationship
        extends ProjectRelationship<PluginRelationship, ProjectVersionRef>,Serializable
{
    boolean isReporting();

    @Override
    PluginRelationship cloneFor( ProjectVersionRef projectRef );

    @Override
    ArtifactRef getTargetArtifact();

    @Override
    PluginRelationship selectDeclaring( ProjectVersionRef ref );

    @Override
    PluginRelationship selectTarget( ProjectVersionRef ref );
}
