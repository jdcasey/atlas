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
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.neo4j.graphdb.Relationship;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.Set;

public final class NeoPluginRelationship
    extends AbstractNeoProjectRelationship<NeoPluginRelationship, PluginRelationship, ProjectVersionRef>
    implements Serializable, PluginRelationship
{

    private static final long serialVersionUID = 1L;

    public NeoPluginRelationship( final Relationship rel )
    {
        super( rel, RelationshipType.PLUGIN );
    }

    @Override
    public final boolean isReporting()
    {
        return Conversions.getBooleanProperty( Conversions.IS_REPORTING_PLUGIN, rel );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( isManaged() ? 1231 : 1237 );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( !super.equals( obj ) )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final PluginRelationship other = (PluginRelationship) obj;
        return isManaged() == other.isManaged();
    }

    @Override
    public String toString()
    {
        return String.format( "PluginRelationship [%s => %s (managed=%s, index=%s)]", getDeclaring(), getTarget(),
                              isManaged(), getIndex() );
    }

    @Override
    public ProjectVersionRef getTarget()
    {
        return target == null ? new NeoProjectVersionRef( rel.getEndNode() ) : target;
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return new SimpleArtifactRef( getTarget(), "maven-plugin", null, false );
    }

    @Override
    public PluginRelationship selectDeclaring( final ProjectVersionRef ref )
    {
        return new NeoPluginRelationship( rel ).cloneDirtyState( this ).withDeclaring( ref );
    }

    @Override
    public PluginRelationship selectTarget( final ProjectVersionRef ref )
    {
        return new NeoPluginRelationship( rel ).cloneDirtyState( this ).withTarget( ref );
    }

    @Override
    public PluginRelationship addSource( URI source )
    {
        Set<URI> sources = getSources();
        if ( sources.add( source ) )
        {
            return new NeoPluginRelationship( rel ).cloneDirtyState( this ).withSources( sources );
        }

        return this;
    }

    @Override
    public PluginRelationship addSources( Collection<URI> sources )
    {
        Set<URI> srcs = getSources();
        boolean changed = false;
        for ( URI src: sources )
        {
            changed = srcs.add( src ) || changed;
        }

        if ( changed )
        {
            return new NeoPluginRelationship( rel ).cloneDirtyState( this ).withSources( srcs );
        }

        return this;
    }

}
