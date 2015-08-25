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

import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.filterTerminalParents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipComparator;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class EProjectCycle
    implements Iterable<ProjectRelationship<?, ?>>, EProjectRelationshipCollection
{

    private static final long serialVersionUID = 1L;

    private List<ProjectRelationship<?, ?>> relationships = new ArrayList<ProjectRelationship<?, ?>>();

    public static final class Builder
    {
        private final List<ProjectRelationship<?, ?>> participants;

        public Builder( final ProjectRelationship<?, ?>... rels )
        {
            participants = new ArrayList<ProjectRelationship<?, ?>>( Arrays.asList( rels ) );
        }

        public Builder( final List<ProjectRelationship<?, ?>> rels )
        {
            participants = new ArrayList<ProjectRelationship<?, ?>>( rels );
        }

        public Builder( final Builder builder )
        {
            participants = new ArrayList<ProjectRelationship<?, ?>>( builder.participants );
        }

        public Builder( final Builder builder, final int start )
        {
            participants = new ArrayList<ProjectRelationship<?, ?>>( builder.participants );
            for ( int i = 0; i < start; i++ )
            {
                participants.remove( 0 );
            }
        }

        public Builder with( final ProjectRelationship<?, ?> rel )
        {
            participants.add( rel );
            return this;
        }

        public Builder withoutLast()
        {
            participants.remove( participants.size() - 1 );
            return this;
        }

        public EProjectCycle build()
        {
            return new EProjectCycle( participants );
        }

        public int indexOf( final ProjectVersionRef ref )
        {
            return build().indexOf( ref );
        }

        public int indexOf( final ProjectRelationship<?, ?> rel )
        {
            return build().indexOf( rel );
        }

        public boolean contains( final ProjectVersionRef ref )
        {
            return build().contains( ref );
        }

        public boolean contains( final ProjectRelationship<?, ?> rel )
        {
            return build().contains( rel );
        }
    }

    public EProjectCycle(){}

    public EProjectCycle( final List<ProjectRelationship<?, ?>> cycle )
    {
        this.relationships = new ArrayList<ProjectRelationship<?, ?>>( cycle );
    }

    public boolean contains( final ProjectRelationship<?, ?> rel )
    {
        return relationships.contains( rel );
    }

    public boolean contains( final ProjectVersionRef ref )
    {
        for ( final ProjectRelationship<?, ?> rel : relationships )
        {
            if ( rel.getDeclaring()
                    .equals( ref ) )
            {
                return true;
            }
        }

        return false;
    }

    public int indexOf( final ProjectRelationship<?, ?> rel )
    {
        return relationships.indexOf( rel );
    }

    public int indexOf( final ProjectVersionRef ref )
    {
        int targetIdx = -1;
        for ( int i = 0; i < relationships.size(); i++ )
        {
            final ProjectRelationship<?, ?> rel = relationships.get( i );
            if ( rel.getDeclaring()
                    .equals( ref ) )
            {
                return i;
            }

            // if we find the ref we're after as a TARGET, log it.
            // if, at the end, we haven't found it as a DECLARING ref, return
            // the index of the relationship that lists it as a target.
            if ( targetIdx < 0 && rel.getTarget()
                                     .asProjectVersionRef()
                                     .equals( ref ) )
            {
                targetIdx = i;
            }
        }

        return targetIdx;
    }

    @Override
    public Iterator<ProjectRelationship<?, ?>> iterator()
    {
        return relationships.iterator();
    }

    @Override
    public Collection<ProjectRelationship<?, ?>> getAllRelationships()
    {
        final Collection<ProjectRelationship<?, ?>> rels = getExactAllRelationships();
        filterTerminalParents( rels );

        return rels;
    }

    @Override
    public Collection<ProjectRelationship<?, ?>> getExactAllRelationships()
    {
        return new ArrayList<ProjectRelationship<?, ?>>( relationships );
    }

    public Set<ProjectVersionRef> getAllParticipatingProjects()
    {
        final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>();
        for ( final ProjectRelationship<?, ?> rel : relationships )
        {
            refs.add( rel.getDeclaring() );
            refs.add( rel.getTarget()
                         .asProjectVersionRef() );
        }

        return refs;
    }

    public List<ProjectRelationship<?, ?>> getRelationships()
    {
        return relationships;
    }

    public void setRelationships( final List<ProjectRelationship<?, ?>> relationships )
    {
        this.relationships = relationships;
    }

    @Override
    public String toString()
    {
        return String.format( "Project cycle: [%s]", join( relationships, " -> " ) );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;

        final List<ProjectRelationship<?, ?>> sorted = new ArrayList<ProjectRelationship<?, ?>>( relationships );
        Collections.sort( sorted, RelationshipComparator.INSTANCE );

        result = prime * result + sorted.hashCode();
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
        final EProjectCycle other = (EProjectCycle) obj;
        if ( relationships == null )
        {
            if ( other.relationships != null )
            {
                return false;
            }
        }
        else
        {

            final Set<ProjectRelationship<?, ?>> cycle = new HashSet<ProjectRelationship<?, ?>>( this.relationships );
            final Set<ProjectRelationship<?, ?>> otherCycle = new HashSet<ProjectRelationship<?, ?>>( other.relationships );

            for ( final ProjectRelationship<?, ?> rel : cycle )
            {
                if ( !otherCycle.contains( rel ) )
                {
                    return false;
                }
            }
        }

        return true;
    }

}
