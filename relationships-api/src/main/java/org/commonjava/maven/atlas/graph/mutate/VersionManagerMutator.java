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
package org.commonjava.maven.atlas.graph.mutate;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class VersionManagerMutator
    implements GraphMutator
{

    private static final long serialVersionUID = 1L;

    private transient String longId;

    private transient String shortId;

    @Override
    public ProjectRelationship<?, ?> selectFor( final ProjectRelationship<?, ?> rel, final GraphPath<?> path,
                                             final RelationshipGraphConnection connection, final ViewParams params )
    {
        final ProjectRef target = rel.getTarget()
                                     .asProjectRef();

        if ( params != null )
        {
            final ProjectVersionRef ref = params.getSelection( target );
            if ( ref != null )
            {
                return rel.selectTarget( ref );
            }
        }

        return rel;
    }

    @Override
    public GraphMutator getMutatorFor( final ProjectRelationship<?, ?> rel, final RelationshipGraphConnection connection,
                                       final ViewParams params )
    {
        return this;
    }

    @Override
    public int hashCode()
    {
        return getClass().hashCode() + 1;
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
        return getClass() == obj.getClass();
    }

    @Override
    public String getLongId()
    {
        if ( longId == null )
        {
            final StringBuilder sb = new StringBuilder();
            final String abbreviatedPackage = getClass().getPackage()
                                                        .getName()
                                                        .replaceAll( "([a-zA-Z])[a-zA-Z]+", "$1" );

            sb.append( abbreviatedPackage )
              .append( '.' )
              .append( getClass().getSimpleName() );

            longId = sb.toString();
        }

        return longId;
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
    public String toString()
    {
        return getLongId();
    }

}
