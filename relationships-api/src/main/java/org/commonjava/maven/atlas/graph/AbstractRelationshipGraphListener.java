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
package org.commonjava.maven.atlas.graph;

import java.util.Collection;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public abstract class AbstractRelationshipGraphListener
    implements RelationshipGraphListener
{

    protected AbstractRelationshipGraphListener()
    {
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals( Object other );

    @Override
    public void storing( final RelationshipGraph graph, final Collection<? extends ProjectRelationship<?, ?>> relationships )
        throws RelationshipGraphException
    {
        // NOP
    }

    @Override
    public void stored( final RelationshipGraph graph,
                        final Collection<? extends ProjectRelationship<?, ?>> relationships,
                        final Collection<ProjectRelationship<?, ?>> rejected )
        throws RelationshipGraphException
    {
        // NOP
    }

    @Override
    public void projectError( final RelationshipGraph graph, final ProjectVersionRef ref,
                              final Throwable error )
        throws RelationshipGraphException
    {
        // NOP
    }

    @Override
    public void closing( final RelationshipGraph graph )
        throws RelationshipGraphException
    {
        // NOP
    }

    @Override
    public void closed( final RelationshipGraph graph )
        throws RelationshipGraphException
    {
        // NOP
    }

}
