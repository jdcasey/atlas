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

import java.util.List;

import org.commonjava.maven.atlas.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.graph.filter.NoneFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFilteringTraversal
    extends AbstractTraversal
{

    private final ProjectRelationshipFilter rootFilter;

    protected AbstractFilteringTraversal()
    {
        rootFilter = AnyFilter.INSTANCE;
    }

    protected AbstractFilteringTraversal( final ProjectRelationshipFilter filter )
    {
        rootFilter = filter;
    }

    protected abstract boolean shouldTraverseEdge( ProjectRelationship<?, ?> relationship,
                                                   List<ProjectRelationship<?, ?>> path );

    protected void edgeTraversalFinished( final ProjectRelationship<?, ?> relationship,
                                          final List<ProjectRelationship<?, ?>> path )
    {
    }

    public final ProjectRelationshipFilter getRootFilter()
    {
        return rootFilter;
    }

    @Override
    public final void edgeTraversed( final ProjectRelationship<?, ?> relationship, final List<ProjectRelationship<?, ?>> path )
    {
        edgeTraversalFinished( relationship, path );
    }

    @Override
    public final boolean traverseEdge( final ProjectRelationship<?, ?> relationship,
                                       final List<ProjectRelationship<?, ?>> path )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        if ( !preCheck( relationship, path ) )
        {
            logger.debug("DON'T traverse: {}", relationship );
            return false;
        }

        //        seen.add( relationship );

        final boolean ok = shouldTraverseEdge( relationship, path );
        logger.debug( "Traverse: {}?\n{}", relationship, ok );

        return ok;
    }

    @Override
    public boolean preCheck( final ProjectRelationship<?, ?> relationship, final List<ProjectRelationship<?, ?>> path )
    {
        boolean result = true;

        final ProjectRelationshipFilter filter = constructFilter( path );
        if ( result && filter != null && !filter.accept( relationship ) )
        {
            result = false;
        }

        return result;
    }

    private ProjectRelationshipFilter constructFilter( final List<ProjectRelationship<?, ?>> path )
    {
        if ( rootFilter == null )
        {
            return null;
        }

        ProjectRelationshipFilter filter = rootFilter;
        for ( final ProjectRelationship<?, ?> rel : path )
        {
            if ( !filter.accept( rel ) )
            {
                return NoneFilter.INSTANCE;
            }
            else
            {
                filter = filter.getChildFilter( rel );
            }
        }

        return filter;
    }

}
