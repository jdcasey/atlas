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
import java.util.List;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class FilteringTraversal
    extends AbstractFilteringTraversal
{

    private final List<ProjectRelationship<?, ?>> captured = new ArrayList<ProjectRelationship<?, ?>>();

    private final boolean doCapture;

    public FilteringTraversal( final ProjectRelationshipFilter filter )
    {
        this( filter, false );
    }

    public FilteringTraversal( final ProjectRelationshipFilter filter, final boolean doCapture )
    {
        super( filter );
        this.doCapture = doCapture;
    }

    public List<ProjectRelationship<?, ?>> getCapturedRelationships()
    {
        return captured;
    }

    public List<ProjectVersionRef> getCapturedProjects( final boolean unique )
    {
        final List<ProjectVersionRef> refs = new ArrayList<ProjectVersionRef>();
        for ( final ProjectRelationship<?, ?> rel : captured )
        {
            final ProjectVersionRef d = rel.getDeclaring();
            final ProjectVersionRef t = rel.getTarget()
                                           .asProjectVersionRef();

            if ( !unique || !refs.contains( d ) )
            {
                refs.add( d );
            }

            if ( !unique || !refs.contains( t ) )
            {
                refs.add( t );
            }
        }

        return refs;
    }

    @Override
    protected boolean shouldTraverseEdge( final ProjectRelationship<?, ?> relationship,
                                          final List<ProjectRelationship<?, ?>> path )
    {
        if ( doCapture )
        {
            captured.add( relationship );
        }
        return true;
    }

}
