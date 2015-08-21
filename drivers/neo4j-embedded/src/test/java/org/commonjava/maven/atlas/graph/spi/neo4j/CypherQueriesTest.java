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

import java.net.URI;
import java.util.Map;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionFactory;
import org.commonjava.maven.atlas.graph.spi.neo4j.fixture.FileConnectionFixture;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.atlas.tck.graph.AbstractSPI_TCK;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.neo4j.cypher.javacompat.ExecutionResult;

public class CypherQueriesTest
    extends AbstractSPI_TCK
{

    @Rule
    public FileConnectionFixture fixture = new FileConnectionFixture();

    @Rule
    public TestName naming = new TestName();

    @Test
    public void projectsWithVariableFlag_PartialQuery()
        throws Exception
    {
        final ProjectVersionRef project = new SimpleProjectVersionRef( "org.my", "project", "1.0" );
        final ProjectVersionRef varDep = new SimpleProjectVersionRef( "org.other", "dep", "1.0-SNAPSHOT" );
        final ProjectVersionRef varD2 = new SimpleProjectVersionRef( "org.other", "dep2", "1.0-SNAPSHOT" );
        final ProjectVersionRef selected = new SimpleProjectVersionRef( varDep, "1.0-20130314.161200-1" );

        final URI source = sourceURI();
        RelationshipGraph graph = simpleGraph( project );

        /* @formatter:off */
        graph.storeRelationships(
            new DependencyRelationship( source, project, new SimpleArtifactRef( varDep, null, null, false ), null, 0, false ),
            new DependencyRelationship( source, varDep,  new SimpleArtifactRef( varD2,  null, null, false ), null, 0, false )
        );

        graph = graphFactory().open( new ViewParams.Builder( graph.getParams() ).withSelection( varDep.asProjectRef(), selected ).build(), false );

//        view.selectVersion( varDep, selected );
        
        final String cypher = "START a=node(1) "
            + "\nMATCH p=(a)-[:M_PLUGIN_DEP|C_PLUGIN|PARENT|EXTENSION|M_DEPENDENCY|M_PLUGIN|C_PLUGIN_DEP|C_DEPENDENCY*]->(n) "
            + "\nWHERE "
            + "\n  none( "
            + "\n    r in relationships(p) "
            + "\n      WHERE has(r._deselected_for) "
            + "\n  ) "
            + "\nRETURN n as node, p as path";
        /* @formatter:on */

        final FileNeo4JGraphConnection driver = (FileNeo4JGraphConnection) graph.getDatabase();

        final ExecutionResult result = driver.execute( cypher );
        int i = 0;
        for ( final Map<String, Object> record : result )
        {
            System.out.printf( "%d:  %s", i++, record );
        }
    }

    @Test
    //    @Ignore
    public void projectsWithVariableFlagQuery()
        throws Exception
    {
        final ProjectVersionRef project = new SimpleProjectVersionRef( "org.my", "project", "1.0" );
        final ProjectVersionRef varDep = new SimpleProjectVersionRef( "org.other", "dep", "1.0-SNAPSHOT" );
        final ProjectVersionRef varD2 = new SimpleProjectVersionRef( "org.other", "dep2", "1.0-SNAPSHOT" );
        final ProjectVersionRef selected = new SimpleProjectVersionRef( varDep, "1.0-20130314.161200-1" );

        final URI source = sourceURI();
        RelationshipGraph graph = simpleGraph( project );

        /* @formatter:off */
        graph.storeRelationships( 
            new DependencyRelationship( source, project, new SimpleArtifactRef( varDep, null, null, false ), null, 0, false ),
            new DependencyRelationship( source, varDep,  new SimpleArtifactRef( varD2,  null, null, false ), null, 0, false )
        );
        
        graph = graphFactory().open( new ViewParams.Builder( graph.getParams()).withSelection( varDep.asProjectRef(), selected ).build(), false );
        
        final String cypher = "START a=node(1) "
            + "\nMATCH p=(a)-[:M_PLUGIN_DEP|C_PLUGIN|PARENT|EXTENSION|M_DEPENDENCY|M_PLUGIN|C_PLUGIN_DEP|C_DEPENDENCY*]->(n) "
            + "\nWHERE "
            + "\n  none( "
            + "\n    r in relationships(p) "
            + "\n      WHERE has(r._deselected_for) "
            + "\n  ) "
            + "\n  AND has(n.gav) "
            + "\n  AND has(n.gav) "
            + "\n  AND n._variable! = true "
            + "\nRETURN n as node, p as path";
        /* @formatter:on */

        final FileNeo4JGraphConnection driver = (FileNeo4JGraphConnection) graph.getDatabase();

        final ExecutionResult result = driver.execute( cypher );
        int i = 0;
        for ( final Map<String, Object> record : result )
        {
            System.out.printf( "%d:  %s", i++, record );
        }
    }

    @Override
    protected RelationshipGraphConnectionFactory connectionFactory()
        throws Exception
    {
        return fixture.connectionFactory();
    }

}
