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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;

public abstract class AbstractAggregatingFilter
    implements ProjectRelationshipFilter, Iterable<ProjectRelationshipFilter>
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private final List<? extends ProjectRelationshipFilter> filters;

    private transient String longId;

    private transient String shortId;

    protected AbstractAggregatingFilter( final Collection<? extends ProjectRelationshipFilter> filters )
    {
        this.filters = new ArrayList<ProjectRelationshipFilter>( filters );
    }

    protected AbstractAggregatingFilter( final ProjectRelationshipFilter... filters )
    {
        this.filters = new ArrayList<ProjectRelationshipFilter>( Arrays.asList( filters ) );
    }

    public final List<? extends ProjectRelationshipFilter> getFilters()
    {
        return filters;
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?, ?> parent )
    {
        final Set<ProjectRelationshipFilter> childFilters = new LinkedHashSet<ProjectRelationshipFilter>();
        for ( final ProjectRelationshipFilter filter : getFilters() )
        {
            //            if ( filter.accept( parent ) )
            //            {
            childFilters.add( filter.getChildFilter( parent ) );
            //            }
        }

        if ( filtersEqual( childFilters ) )
        {
            return this;
        }

        // TODO: Optimize to ensure we're only creating a new instance when it's critical to...
        return newChildFilter( childFilters );
    }

    protected abstract AbstractAggregatingFilter newChildFilter( Collection<ProjectRelationshipFilter> childFilters );

    @Override
    public Iterator<ProjectRelationshipFilter> iterator()
    {
        return new ArrayList<ProjectRelationshipFilter>( filters ).iterator();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( filters == null ) ? 0 : filters.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final AbstractAggregatingFilter other = (AbstractAggregatingFilter) obj;

        return filtersEqual( other.filters );
    }

    protected final boolean filtersEqual( final Collection<? extends ProjectRelationshipFilter> otherFilters )
    {
        if ( filters == null )
        {
            if ( otherFilters != null )
            {
                return false;
            }
        }
        else if ( otherFilters != null )
        {
            //            if ( orderMatters )
            //            {
            if ( otherFilters instanceof List )
            {
                return filters.equals( otherFilters );
            }
            else
            {
                List<ProjectRelationshipFilter> otherFiltersList = 
                        new ArrayList<ProjectRelationshipFilter>( otherFilters );
                return filters.equals( otherFiltersList );
            }
            //            }
            //            else
            //            {
            //                for ( final ProjectRelationshipFilter filter : filters )
            //                {
            //                    if ( !otherFilters.contains( filter ) )
            //                    {
            //                        return false;
            //                    }
            //                }
            //
            //                for ( final ProjectRelationshipFilter filter : otherFilters )
            //                {
            //                    if ( !filters.contains( filter ) )
            //                    {
            //                        return false;
            //                    }
            //                }
            //            }
        }

        return true;
    }

    @Override
    public String getLongId()
    {
        if ( longId == null )
        {
            final StringBuilder sb = new StringBuilder();
            final List<? extends ProjectRelationshipFilter> filters = getFilters();
            final String abbreviatedPackage = getClass().getPackage()
                                                        .getName()
                                                        .replaceAll( "([a-zA-Z])[a-zA-Z]+", "$1" );

            sb.append( abbreviatedPackage )
              .append( '.' )
              .append( getClass().getSimpleName() )
              .append( '(' );

            boolean first = true;
            for ( final ProjectRelationshipFilter filter : filters )
            {
                if ( !first )
                {
                    sb.append( ',' );
                }

                first = false;
                sb.append( filter.getLongId() );
            }
            sb.append( ')' );

            longId = sb.toString();
        }

        return longId;
    }

    @Override
    public String toString()
    {
        return getLongId();
    }

    @Override
    public String getCondensedId()
    {
        if ( shortId == null )
        {
            shortId = DigestUtils.shaHex( getLongId() );
        }

        return shortId;
    }

    @Override
    public boolean includeManagedRelationships()
    {
        for ( final ProjectRelationshipFilter filter : filters )
        {
            if ( filter.includeManagedRelationships() )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean includeConcreteRelationships()
    {
        for ( final ProjectRelationshipFilter filter : filters )
        {
            if ( filter.includeConcreteRelationships() )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public Set<RelationshipType> getAllowedTypes()
    {
        final Set<RelationshipType> result = new HashSet<RelationshipType>();
        for ( final ProjectRelationshipFilter filter : filters )
        {
            result.addAll( filter.getAllowedTypes() );
        }

        return result;
    }
}
