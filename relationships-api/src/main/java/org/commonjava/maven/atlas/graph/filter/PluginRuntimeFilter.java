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

import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.rel.SimplePluginRelationship;
import org.commonjava.maven.atlas.ident.DependencyScope;

public class PluginRuntimeFilter
    implements ProjectRelationshipFilter
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public PluginRuntimeFilter()
    {
    }

    @Override
    public boolean accept( final ProjectRelationship<?, ?> rel )
    {
        return ( rel instanceof SimplePluginRelationship ) && !( (PluginRelationship) rel ).isManaged();
    }

    // TODO: Optimize to minimize new instance creation...
    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?, ?> parent )
    {
        ProjectRelationshipFilter child;
        if ( parent instanceof SimplePluginRelationship )
        {
            final PluginRelationship plugin = (PluginRelationship) parent;

            child =
                new OrFilter( new DependencyFilter( DependencyScope.runtime ),
                              new PluginDependencyFilter( plugin, true, true ), ParentFilter.EXCLUDE_TERMINAL_PARENTS );
        }
        else
        {
            child = NoneFilter.INSTANCE;
        }

        return child;
    }

    @Override
    public String getLongId()
    {
        return "PLUGIN-RUNTIME";
    }

    @Override
    public String toString()
    {
        return getLongId();
    }

    @Override
    public String getCondensedId()
    {
        return getLongId();
    }

    @Override
    public boolean includeManagedRelationships()
    {
        return false;
    }

    @Override
    public boolean includeConcreteRelationships()
    {
        return true;
    }

    @Override
    public Set<RelationshipType> getAllowedTypes()
    {
        final Set<RelationshipType> result = new HashSet<RelationshipType>();
        result.add( RelationshipType.PLUGIN );

        // for descendants
        result.add( RelationshipType.PARENT );
        result.add( RelationshipType.DEPENDENCY );
        result.add( RelationshipType.PLUGIN_DEP );

        return result;
    }

}
