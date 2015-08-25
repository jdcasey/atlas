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

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;

public abstract class AbstractTraversal
    implements RelationshipGraphTraversal
{


    @Override
    public void startTraverse( final RelationshipGraph graph )
        throws RelationshipGraphConnectionException
    {
    }

    @Override
    public void endTraverse( final RelationshipGraph graph )
        throws RelationshipGraphConnectionException
    {
    }

    @Override
    public void edgeTraversed( final ProjectRelationship<?, ?> relationship, final List<ProjectRelationship<?, ?>> path )
    {
    }

    @Override
    public boolean traverseEdge( final ProjectRelationship<?, ?> relationship, final List<ProjectRelationship<?, ?>> path )
    {
        return preCheck( relationship, path );
    }
}
