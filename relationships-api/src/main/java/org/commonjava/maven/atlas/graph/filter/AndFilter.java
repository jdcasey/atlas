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

import java.util.Collection;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

public class AndFilter
    extends AbstractAggregatingFilter
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public AndFilter( final Collection<? extends ProjectRelationshipFilter> filters )
    {
        super( filters );
    }

    public <T extends ProjectRelationshipFilter> AndFilter( final T... filters )
    {
        super( filters );
    }

    @Override
    public boolean accept( final ProjectRelationship<?, ?> rel )
    {
        boolean accepted = true;
        for ( final ProjectRelationshipFilter filter : getFilters() )
        {
            accepted = accepted && filter.accept( rel );
            if ( !accepted )
            {
                break;
            }
        }

        return accepted;
    }

    @Override
    protected AbstractAggregatingFilter newChildFilter( final Collection<ProjectRelationshipFilter> childFilters )
    {
        if ( !filtersEqual( childFilters ) )
        {
            return new AndFilter( childFilters );
        }

        return this;
    }

}
