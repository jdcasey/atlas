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
package org.commonjava.maven.atlas.tck.graph.selection;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.rel.SimpleDependencyRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.atlas.tck.graph.AbstractSPI_TCK;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class SubGraphSelection_ContextualToViewTCK
    extends AbstractSPI_TCK
{

    @Test
    public void run()
        throws Exception
    {
        final ProjectVersionRef project = new SimpleProjectVersionRef( "org.my", "project", "1.0" );
        final ProjectVersionRef varDep = new SimpleProjectVersionRef( "org.other", "dep", "1.0-SNAPSHOT" );
        final ProjectVersionRef varD2 = new SimpleProjectVersionRef( "org.other", "dep2", "1.0-SNAPSHOT" );
        final ProjectVersionRef selected = new SimpleProjectVersionRef( varDep, "1.0-20130314.161200-1" );

        final URI source = sourceURI();
        final RelationshipGraph graph = simpleGraph( project );

        /* @formatter:off */
        graph.storeRelationships( new SimpleDependencyRelationship( source, project, new SimpleArtifactRef( varDep, null, null, false ), null, 0, false ),
                                  new SimpleDependencyRelationship( source, varDep,  new SimpleArtifactRef( varD2,  null, null, false ), null, 0, false ) );
        /* @formatter:on */

        Set<ProjectVersionRef> variables = graph.getVariableSubgraphs();
        assertThat( variables.contains( varDep ), equalTo( true ) );

        // Select a concrete version for the session associated with the FIRST graph.
        // Second graph session should remain unchanged.
        final RelationshipGraph graph2 =
            graphFactory().open( new ViewParams.Builder( graph.getParams() ).withSelection( varDep.asProjectRef(),
                                                                                            selected )
                                                                            .build(), false );

        assertThat( graph.getParams()
                         .getSelection( varDep ), nullValue() );
        assertThat( graph2.getParams()
                          .getSelection( varDep ), equalTo( selected ) );

        assertThat( selected.asProjectRef(), equalTo( varDep.asProjectRef() ) );

        assertThat( selected.asProjectVersionRef()
                            .equals( varDep.asProjectVersionRef() ), equalTo( false ) );

        variables = graph.getVariableSubgraphs();
        assertThat( variables.contains( varDep ), equalTo( true ) );

        variables = graph2.getVariableSubgraphs();
        assertThat( variables.contains( varDep ), equalTo( false ) );

        Set<ProjectVersionRef> incomplete = graph.getIncompleteSubgraphs();
        assertThat( incomplete.contains( selected ), equalTo( false ) );

        incomplete = graph2.getIncompleteSubgraphs();
        assertThat( incomplete.contains( selected ), equalTo( true ) );

    }

}
