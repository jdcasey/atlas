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
package org.commonjava.maven.atlas.graph.model;

import java.io.Serializable;

import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.graph.mutate.VersionManagerMutator;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;

public final class GraphPathInfo
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    private final ProjectRelationshipFilter filter;

    private final GraphMutator mutator;

    private transient RelationshipGraphConnection connection;

    private transient ViewParams params;

    public GraphPathInfo( final RelationshipGraphConnection connection, final ViewParams params )
    {
        this.connection = connection;
        this.params = params;
        filter = params.getFilter();
        mutator = params.getMutator() == null ? new VersionManagerMutator() : params.getMutator();
    }

    public GraphPathInfo( final ProjectRelationshipFilter filter, final GraphMutator mutator,
                          final RelationshipGraphConnection connection, final ViewParams params )
    {
        this.connection = connection;
        this.params = params;
        this.filter = filter;
        this.mutator = mutator;
    }

    public ProjectRelationshipFilter getFilter()
    {
        return filter;
    }

    public GraphMutator getMutator()
    {
        return mutator;
    }

    public ProjectRelationship<?, ?> selectRelationship( ProjectRelationship<?, ?> next, final GraphPath<?> path )
    {
        if ( filter != null && !filter.accept( next ) )
        {
            return null;
        }

        if ( mutator != null )
        {
            next = mutator.selectFor( next, path, connection, params );
        }

        return next;
    }

    public GraphPathInfo getChildPathInfo( final ProjectRelationship<?, ?> rel )
    {
        final ProjectRelationshipFilter nextFilter = filter == null ? null : filter.getChildFilter( rel );
        final GraphMutator nextMutator = mutator == null ? null : mutator.getMutatorFor( rel, connection, params );
        if ( nextFilter == filter && nextMutator == mutator )
        {
            return this;
        }

        return new GraphPathInfo( nextFilter, nextMutator, connection, params );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( filter == null ) ? 0 : filter.hashCode() );
        result = prime * result + ( ( mutator == null ) ? 0 : mutator.hashCode() );
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
        final GraphPathInfo other = (GraphPathInfo) obj;
        if ( filter == null )
        {
            if ( other.filter != null )
            {
                return false;
            }
        }
        else if ( !filter.equals( other.filter ) )
        {
            return false;
        }
        if ( mutator == null )
        {
            if ( other.mutator != null )
            {
                return false;
            }
        }
        else if ( !mutator.equals( other.mutator ) )
        {
            return false;
        }
        return true;
    }

    public String getKey()
    {
        return ( filter == null ? "none" : filter.getCondensedId() ) + "/"
            + ( mutator == null ? "none" : mutator.getCondensedId() );
    }

    public void reattach( final RelationshipGraphConnection connection, final ViewParams params )
    {
        this.connection = connection;
        this.params = params;
    }

    @Override
    public String toString()
    {
        return String.format( "GraphPathInfo [filter=%s, mutator=%s, view=%s]", filter, mutator, params.getShortId() );
    }

}
