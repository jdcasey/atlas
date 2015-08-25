package org.commonjava.maven.atlas.graph.rel;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.io.Serializable;

/**
 * Created by jdcasey on 8/24/15.
 */
public interface PluginDependencyRelationship
        extends ProjectRelationship<PluginDependencyRelationship, ArtifactRef>,Serializable
{
    ProjectRef getPlugin();

    @Override
    PluginDependencyRelationship cloneFor( ProjectVersionRef projectRef );

    @Override
    ArtifactRef getTargetArtifact();

    @Override
    PluginDependencyRelationship selectDeclaring( ProjectVersionRef ref );

    @Override
    PluginDependencyRelationship selectTarget( ProjectVersionRef ref );
}
