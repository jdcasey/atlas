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
package org.commonjava.maven.atlas.graph.traverse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.OrFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

/**
 * Dependency graph traversal ({@link ProjectNetTraversal}) implementation used
 * to determine the paths between two GAVs or sets of GAVs in a graph.
 *
 * @author jdcasey
 * @author pkocandr
 */
public class PathsTraversal
extends AbstractTraversal
{

    private final ProjectRelationshipFilter rootFilter;

    private final Set<? extends ProjectRef> to;

    private final Map<ProjectRef, OrFilter> cache = new HashMap<ProjectRef, OrFilter>();

    private final Set<List<ProjectRelationship<?, ?>>> paths = new HashSet<List<ProjectRelationship<?, ?>>>();

    public PathsTraversal( final ProjectRelationshipFilter filter, final Set<? extends ProjectRef> toGas )
    {
        this.rootFilter = filter;
        this.to = toGas;
    }

    @Override
    public boolean preCheck( final ProjectRelationship<?, ?> relationship, final List<ProjectRelationship<?, ?>> path )
    {
        final ProjectVersionRef declaring = relationship.getDeclaring().asProjectVersionRef();
        if ( path.isEmpty() || path.get( path.size() - 1 ).getTarget().asProjectVersionRef().equals( declaring ) )
        {
            final ProjectRef dRef = declaring.asProjectRef();

            ProjectRelationshipFilter filter = cache.get( dRef );
            if ( filter == null )
            {
                filter = rootFilter;
            }

            if ( filter.accept( relationship ) )
            {
                final ProjectRef tRef = relationship.getTarget()
                        .asProjectRef();

                final ProjectRelationshipFilter child = filter.getChildFilter( relationship );
                final OrFilter f = cache.get( tRef );
                if ( f == null )
                {
                    if ( child instanceof OrFilter )
                    {
                        cache.put( tRef, (OrFilter) child );
                    }
                    else
                    {
                        cache.put( tRef, new OrFilter( child ) );
                    }
                }
                else
                {
                    final Set<ProjectRelationshipFilter> filters =
                            new HashSet<ProjectRelationshipFilter>( f.getFilters() );
                    if ( child instanceof OrFilter )
                    {
                        final List<? extends ProjectRelationshipFilter> childFilters =
                            ( (OrFilter) child ).getFilters();
                        if ( !filters.containsAll( childFilters ) )
                        {
                            filters.addAll( childFilters );
                            cache.put( tRef, new OrFilter( filters ) );
                        }
                    }
                    else
                    {
                        if ( !filters.contains( child ) )
                        {
                            filters.add( child );
                            cache.put( tRef, new OrFilter( filters ) );
                        }
                    }
                }

                if ( to.contains( tRef ) )
                {
                    final List<ProjectRelationship<?, ?>> realPath = new ArrayList<ProjectRelationship<?, ?>>( path );
                    realPath.add( relationship );
                    paths.add( realPath );
                }

                return true;
            }
        }

        return false;
    }

    public Set<List<ProjectRelationship<?, ?>>> getDiscoveredPaths()
    {
        return paths;
    }

//    @Override
//    public boolean traverseEdge( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path )
//    {
//        return false;
//    }

}
