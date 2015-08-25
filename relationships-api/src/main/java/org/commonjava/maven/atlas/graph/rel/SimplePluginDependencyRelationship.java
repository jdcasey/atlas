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
package org.commonjava.maven.atlas.graph.rel;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.Set;

public final class SimplePluginDependencyRelationship
    extends AbstractSimpleProjectRelationship<PluginDependencyRelationship, ArtifactRef>
    implements Serializable, PluginDependencyRelationship
{

    private static final long serialVersionUID = 1L;

    private final ProjectRef plugin;

    public SimplePluginDependencyRelationship( final URI source, final ProjectVersionRef declaring,
                                               final ProjectRef plugin, final ArtifactRef target, final int index,
                                               final boolean managed )
    {
        super( source, RelationshipType.PLUGIN_DEP, declaring, target, index, managed );
        this.plugin = plugin;
    }

    public SimplePluginDependencyRelationship( final URI source, final URI pomLocation,
                                               final ProjectVersionRef declaring, final ProjectRef plugin,
                                               final ArtifactRef target, final int index, final boolean managed )
    {
        super( source, pomLocation, RelationshipType.PLUGIN_DEP, declaring, target, index, managed );
        this.plugin = plugin;
    }

    public SimplePluginDependencyRelationship( final Collection<URI> sources, final URI pomLocation,
                                               final ProjectVersionRef declaring, final ProjectRef plugin,
                                               final ArtifactRef target, final int index, final boolean managed )
    {
        super( sources, pomLocation, RelationshipType.PLUGIN_DEP, declaring, target, index, managed );
        this.plugin = plugin;
    }

    @Override
    public final ProjectRef getPlugin()
    {
        return plugin;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( isManaged() ? 1231 : 1237 );
        result = prime * result + ( ( plugin == null ) ? 0 : plugin.hashCode() );
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
        final SimplePluginDependencyRelationship other = (SimplePluginDependencyRelationship) obj;
        if ( isManaged() != other.isManaged() )
        {
            return false;
        }
        if ( plugin == null )
        {
            if ( other.plugin != null )
            {
                return false;
            }
        }
        else if ( !plugin.equals( other.plugin ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "PluginDependencyRelationship [%s -> (%s) => %s (managed=%s, index=%s)]", getDeclaring(),
                              plugin, getTarget(), isManaged(), getIndex() );
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return getTarget();
    }

    @Override
    public PluginDependencyRelationship selectDeclaring( final ProjectVersionRef ref )
    {
        final ArtifactRef t = getTarget();

        return new SimplePluginDependencyRelationship( getSources(), getPomLocation(), ref, getPlugin(), t, getIndex(),
                                                 isManaged() );
    }

    @Override
    public PluginDependencyRelationship selectTarget( final ProjectVersionRef ref )
    {
        final ProjectVersionRef d = getDeclaring();
        ArtifactRef t = getTarget();
        t =
            (ArtifactRef) ( ( ref instanceof ArtifactRef ) ? ref : new SimpleArtifactRef( ref, t.getType(),
                                                                                    t.getClassifier(), t.isOptional() ) );

        return new SimplePluginDependencyRelationship( getSources(), getPomLocation(), d, getPlugin(), t, getIndex(),
                                                 isManaged() );
    }

    @Override
    public synchronized PluginDependencyRelationship cloneFor( final ProjectVersionRef projectRef )
    {
        return new SimplePluginDependencyRelationship( getSources(), getPomLocation(), projectRef, plugin, getTarget(),
                                                       getIndex(), isManaged() );
    }

    @Override
    public PluginDependencyRelationship addSource( URI source )
    {
        Set<URI> srcs = getSources();
        srcs.add( source );
        return new SimplePluginDependencyRelationship( srcs, getPomLocation(), getDeclaring(), plugin, getTarget(),
                                                       getIndex(), isManaged() );
    }

    @Override
    public PluginDependencyRelationship addSources( Collection<URI> sources )
    {
        Set<URI> srcs = getSources();
        srcs.addAll( sources );
        return new SimplePluginDependencyRelationship( srcs, getPomLocation(), getDeclaring(), plugin, getTarget(),
                                                       getIndex(), isManaged() );
    }
}
