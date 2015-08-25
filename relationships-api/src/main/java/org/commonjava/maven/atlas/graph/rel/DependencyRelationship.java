package org.commonjava.maven.atlas.graph.rel;

import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.io.Serializable;
import java.util.Set;

/**
 * Created by jdcasey on 8/24/15.
 */
public interface DependencyRelationship
        extends ProjectRelationship<DependencyRelationship, ArtifactRef>,Serializable
{
    DependencyScope getScope();

    @Override
    DependencyRelationship cloneFor( ProjectVersionRef projectRef );

    @Override
    ArtifactRef getTargetArtifact();

    Set<ProjectRef> getExcludes();

    @Override
    DependencyRelationship selectDeclaring( ProjectVersionRef ref );

    @Override
    DependencyRelationship selectTarget( ProjectVersionRef ref );

    boolean isBOM();
}
