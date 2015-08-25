/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.atlas.graph.spi.neo4j.model;

import org.commonjava.maven.atlas.graph.rel.AbstractSimpleProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.neo4j.graphdb.Relationship;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.Set;

public final class NeoParentRelationship
    extends AbstractNeoProjectRelationship<NeoParentRelationship, ParentRelationship, ProjectVersionRef>
    implements Serializable, ParentRelationship
{

    private static final long serialVersionUID = 1L;

    /**
     * Ancestry terminus. This is to signify that the declaring project has NO parent relationship.
     */
    public NeoParentRelationship( final Relationship rel )
    {
        super( rel, RelationshipType.PARENT );
    }

    @Override
    public String toString()
    {
        return String.format( "ParentRelationship [%s => %s]", getDeclaring(), getTarget() );
    }

    @Override
    public ProjectVersionRef getTarget()
    {
        return target == null ? new NeoProjectVersionRef( rel.getEndNode() ) : target;
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return getTarget().asPomArtifact();
    }

    @Override
    public boolean isTerminus()
    {
        if ( declaring != null || target != null )
        {
            return getDeclaring().equals( getTarget() );
        }

        return rel.getStartNode().getId() == rel.getEndNode().getId();
    }

    @Override
    public ParentRelationship selectDeclaring( final ProjectVersionRef ref )
    {
        return new NeoParentRelationship( rel ).cloneDirtyState( this ).withDeclaring( ref );
    }

    @Override
    public ParentRelationship selectTarget( final ProjectVersionRef ref )
    {
        return new NeoParentRelationship( rel ).cloneDirtyState( this ).withTarget( ref );
    }

    @Override
    public ParentRelationship addSource( URI source )
    {
        Set<URI> sources = getSources();
        if ( sources.add( source ) )
        {
            return new NeoParentRelationship( rel ).cloneDirtyState( this ).withSources( sources );
        }

        return this;
    }

    @Override
    public ParentRelationship addSources( Collection<URI> sources )
    {
        Set<URI> srcs = getSources();
        boolean changed = false;
        for ( URI src: sources )
        {
            changed = srcs.add( src ) || changed;
        }

        if ( changed )
        {
            return new NeoParentRelationship( rel ).cloneDirtyState( this ).withSources( srcs );
        }

        return this;
    }

}
