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
import org.commonjava.maven.atlas.graph.rel.BomRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.neo4j.graphdb.Relationship;

import java.net.URI;
import java.util.Collection;
import java.util.Set;

/** <b>NOTE:</b> BOM relationships are actually marked as concrete.
 * This may be somewhat counter-intuitive, but they are structural (like a parent POM).
 * Therefore, managed isn't correct (despite Maven's unfortunate choice for location).
 */
public class NeoBomRelationship
        extends AbstractNeoProjectRelationship<NeoBomRelationship, BomRelationship, ProjectVersionRef>
        implements BomRelationship
{

    private static final long serialVersionUID = 1L;

    public NeoBomRelationship( final Relationship rel )
    {
        super( rel, RelationshipType.BOM );
    }

    @Override
    public BomRelationship selectDeclaring( final ProjectVersionRef ref )
    {
        return new NeoBomRelationship( rel ).cloneDirtyState( this ).withDeclaring( ref );
    }

    @Override
    public BomRelationship selectTarget( final ProjectVersionRef ref )
    {
        return new NeoBomRelationship( rel ).cloneDirtyState( this ).withTarget( ref );
    }

    @Override
    public BomRelationship addSource( URI source )
    {
        Set<URI> sources = getSources();
        if ( sources.add( source ) )
        {
            return new NeoBomRelationship( rel ).cloneDirtyState( this ).withSources( sources );
        }

        return this;
    }

    @Override
    public BomRelationship addSources( Collection<URI> sources )
    {
        Set<URI> srcs = getSources();
        boolean changed = false;
        for ( URI src : sources )
        {
            changed = srcs.add( src ) || changed;
        }

        if ( changed )
        {
            return new NeoBomRelationship( rel ).cloneDirtyState( this ).withSources( srcs );
        }

        return this;
    }

    @Override
    public ProjectVersionRef getTarget()
    {
        return target == null ? new NeoProjectVersionRef( rel.getEndNode() ) : target;
    }

    @Override
    public String toString()
    {
        return String.format( "BomRelationship [%s => %s, rel=%d]", getDeclaring(), getTarget(), rel.getId() );
    }
}
