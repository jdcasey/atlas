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
package org.commonjava.maven.atlas.graph.spi.neo4j;

import java.util.Map;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.neo4j.cypher.javacompat.ExecutionResult;

public interface Neo4JGraphConnection
    extends RelationshipGraphConnection
{

    ExecutionResult executeFrom( String cypher, ProjectVersionRef... roots )
        throws RelationshipGraphConnectionException;

    ExecutionResult executeFrom( String cypher, ProjectRelationship<?, ?> rootRel )
        throws RelationshipGraphConnectionException;

    ExecutionResult execute( String cypher )
        throws RelationshipGraphConnectionException;

    ExecutionResult executeFrom( String cypher, Map<String, Object> params, ProjectVersionRef... roots )
        throws RelationshipGraphConnectionException;

    ExecutionResult executeFrom( String cypher, Map<String, Object> params, ProjectRelationship<?, ?> rootRel )
        throws RelationshipGraphConnectionException;

    ExecutionResult execute( String cypher, Map<String, Object> params )
        throws RelationshipGraphConnectionException;

    //    Node getNode( ProjectVersionRef ref )
    //        throws GraphDriverException;
    //
    //    Relationship getRelationship( ProjectRelationship<?> rel )
    //        throws GraphDriverException;

}
