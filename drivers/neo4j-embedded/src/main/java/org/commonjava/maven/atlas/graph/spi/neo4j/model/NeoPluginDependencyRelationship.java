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
import org.commonjava.maven.atlas.graph.rel.PluginDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.neo4j.graphdb.Relationship;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.Set;

public final class NeoPluginDependencyRelationship
    extends AbstractNeoProjectRelationship<NeoPluginDependencyRelationship, PluginDependencyRelationship, ArtifactRef>
    implements Serializable, PluginDependencyRelationship
{

    private static final long serialVersionUID = 1L;

    public NeoPluginDependencyRelationship( final Relationship rel )
    {
        super( rel, RelationshipType.PLUGIN_DEP );
    }

    @Override
    public final ProjectRef getPlugin()
    {
        return new NeoProjectRef( rel, Conversions.PLUGIN_GROUP_ID, Conversions.PLUGIN_ARTIFACT_ID );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( isManaged() ? 1231 : 1237 );
        result = prime * result + ( ( getPlugin() == null ) ? 0 : getPlugin().hashCode() );
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
        final NeoPluginDependencyRelationship other = (NeoPluginDependencyRelationship) obj;
        if ( isManaged() != other.isManaged() )
        {
            return false;
        }

        ProjectRef plugin = getPlugin();
        ProjectRef otherPlugin = other.getPlugin();
        if ( plugin == null )
        {
            if ( otherPlugin != null )
            {
                return false;
            }
        }
        else if ( !plugin.equals( otherPlugin ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "PluginDependencyRelationship [%s -> (%s) => %s (managed=%s, index=%s)]", getDeclaring(),
                              getPlugin(), getTarget(), isManaged(), getIndex() );
    }

    @Override
    public ArtifactRef getTarget()
    {
        return target == null ? new NeoArtifactRef( rel.getEndNode() ) : target;
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return getTarget();
    }

    @Override
    public PluginDependencyRelationship selectDeclaring( final ProjectVersionRef ref )
    {
        return new NeoPluginDependencyRelationship( rel ).cloneDirtyState( this ).withDeclaring( ref );
    }

    @Override
    public PluginDependencyRelationship selectTarget( final ProjectVersionRef ref )
    {
        ArtifactRef t = getTarget();
        return new NeoPluginDependencyRelationship( rel ).cloneDirtyState( this ).withTarget( NeoIdentityUtils.newNeoArtifactRef( ref, t ) );
    }

    @Override
    public PluginDependencyRelationship addSource( URI source )
    {
        Set<URI> sources = getSources();
        if ( sources.add( source ) )
        {
            return new NeoPluginDependencyRelationship( rel ).cloneDirtyState( this ).withSources( sources );
        }

        return this;
    }

    @Override
    public PluginDependencyRelationship addSources( Collection<URI> sources )
    {
        Set<URI> srcs = getSources();
        boolean changed = false;
        for ( URI src: sources )
        {
            changed = srcs.add( src ) || changed;
        }

        if ( changed )
        {
            return new NeoPluginDependencyRelationship( rel ).cloneDirtyState( this ).withSources( srcs );
        }

        return this;
    }

}
