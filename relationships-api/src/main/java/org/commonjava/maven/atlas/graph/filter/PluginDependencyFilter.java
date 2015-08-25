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
package org.commonjava.maven.atlas.graph.filter;

import org.commonjava.maven.atlas.graph.rel.PluginDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;

// TODO: Do we need to consider excludes in the direct plugin-level dependency?
public class PluginDependencyFilter
    extends AbstractTypedFilter
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private final ProjectRef plugin;

    public PluginDependencyFilter( final PluginRelationship plugin )
    {
        this( plugin, false, true );
    }

    public PluginDependencyFilter( final PluginRelationship plugin, final boolean includeManaged,
                                   final boolean includeConcrete )
    {
        super( RelationshipType.PLUGIN_DEP, RelationshipType.DEPENDENCY, includeManaged, includeConcrete );

        this.plugin = plugin == null ? null : plugin.getTarget()
                                                    .asProjectRef();
    }

    public PluginDependencyFilter()
    {
        this( null, false, true );
    }

    public PluginDependencyFilter( final boolean includeManaged, final boolean includeConcrete )
    {
        this( null, includeManaged, includeConcrete );
    }

    @Override
    public boolean doAccept( final ProjectRelationship<?, ?> rel )
    {
        final PluginDependencyRelationship pdr = (PluginDependencyRelationship) rel;
        if ( plugin == null || plugin.equals( pdr.getPlugin() ) )
        {
            if ( includeManagedRelationships() && pdr.isManaged() )
            {
                return true;
            }
            else if ( includeConcreteRelationships() && !pdr.isManaged() )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?, ?> parent )
    {
        return new DependencyFilter( DependencyScope.runtime );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
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
        final PluginDependencyFilter other = (PluginDependencyFilter) obj;
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
    protected void renderIdAttributes( final StringBuilder sb )
    {
        sb.append( ",plugin:" )
          .append( plugin );
    }

}
