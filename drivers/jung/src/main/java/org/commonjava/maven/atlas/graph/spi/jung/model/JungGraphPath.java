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
package org.commonjava.maven.atlas.graph.spi.jung.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class JungGraphPath
    implements GraphPath<ProjectRelationship<?, ?>>
{

    private final ProjectRelationship<?, ?>[] rels;

    private final ProjectVersionRef root;

    public JungGraphPath( final ProjectVersionRef root )
    {
        this.root = root;
        this.rels = new ProjectRelationship<?, ?>[] {};
    }

    public JungGraphPath( final ProjectRelationship<?, ?>... rels )
    {
        this.root = null;
        this.rels = rels;
    }

    public JungGraphPath( final JungGraphPath parent, final ProjectRelationship<?, ?> child )
    {
        this.root = null;
        if ( parent == null )
        {
            rels = new ProjectRelationship<?, ?>[] { child };
        }
        else
        {
            final int parentLen = parent.rels.length;
            this.rels = new ProjectRelationship<?, ?>[parentLen + 1];
            System.arraycopy( parent.rels, 0, this.rels, 0, parentLen );
            this.rels[parentLen] = child;
        }
    }

    public JungGraphPath( final List<ProjectRelationship<?, ?>> path )
    {
        this.root = null;
        this.rels = path.toArray( new ProjectRelationship<?, ?>[path.size()] );
    }

    public ProjectVersionRef getTargetGAV()
    {
        if ( root != null )
        {
            return root;
        }
        else if ( rels.length > 0 )
        {
            return rels[rels.length - 1].getTarget()
                                        .asProjectVersionRef();
        }

        return null;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode( rels );
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
        final JungGraphPath other = (JungGraphPath) obj;
        return Arrays.equals( rels, other.rels );
    }

    @Override
    public Iterator<ProjectRelationship<?, ?>> iterator()
    {
        return new Iterator<ProjectRelationship<?, ?>>()
        {
            private int next = 0;

            @Override
            public boolean hasNext()
            {
                return rels.length > next;
            }

            @Override
            public ProjectRelationship<?, ?> next()
            {
                return rels[next++];
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException( "Immutable array of GAV's. Remove not supported." );
            }
        };
    }

    public List<ProjectRelationship<?, ?>> getPathElements()
    {
        return rels.length == 0 ? Collections.<ProjectRelationship<?, ?>> emptyList() : Arrays.asList( rels );
    }

    public boolean hasCycle()
    {
        if ( rels.length < 1 )
        {
            return false;
        }

        final Set<ProjectVersionRef> declared = new HashSet<ProjectVersionRef>( rels.length );
        for ( final ProjectRelationship<?, ?> item : rels )
        {
            // NOTE: order is important here, in case it's a terminal parent relationship.
            if ( declared.contains( item.getTarget()
                                        .asProjectVersionRef() ) || !declared.add( item.getDeclaring() ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getKey()
    {
        return DigestUtils.shaHex( StringUtils.join( rels, "," ) );
    }

}
