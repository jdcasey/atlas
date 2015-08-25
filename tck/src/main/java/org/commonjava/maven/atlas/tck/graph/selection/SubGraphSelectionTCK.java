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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Set;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.rel.SimpleDependencyRelationship;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.atlas.tck.graph.AbstractSPI_TCK;
import org.junit.Ignore;
import org.junit.Test;

public class SubGraphSelectionTCK
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
        final ProjectVersionRef sel2 = new SimpleProjectVersionRef( varD2, "1.0-20130314.161200-1" );

        assertThat( varDep.isVariableVersion(), equalTo( true ) );

        final URI source = sourceURI();
        final RelationshipGraph graph = simpleGraph( project );

        /* @formatter:off */
        graph.storeRelationships( new SimpleDependencyRelationship( source, project, new SimpleArtifactRef( varDep, null, null, false ), null, 0, false ),
                                  new SimpleDependencyRelationship( source, varDep,  new SimpleArtifactRef( varD2,  null, null, false ), null, 0, false ) );
        /* @formatter:on */

        Set<ProjectVersionRef> variables = graph.getVariableSubgraphs();
        System.out.println( "Before selection here are the variable nodes: " + variables );
        assertThat( variables.contains( varDep ), equalTo( true ) );

        final RelationshipGraph graph2 =
            graphFactory().open(
                    new ViewParams.Builder( graph.getParams() ).withSelection( varDep.asProjectRef(), selected )
                                                               .withSelection( varD2.asProjectRef(), sel2 )
                                                               .build(), false );

        //        final ProjectVersionRef selDep = view.selectVersion( varDep.asProjectRef(), selected );
        //        assertThat( selDep.asProjectRef(), equalTo( varDep.asProjectRef() ) );

        variables = graph2.getVariableSubgraphs();
        System.out.println( "After selection here are the variable nodes: " + variables );
        assertThat( variables.isEmpty(), equalTo( true ) );

        final Set<ProjectVersionRef> incomplete = graph2.getIncompleteSubgraphs();
        System.out.println( "Checking missing subgraphs for: " + selected );
        assertThat( incomplete.contains( selected ), equalTo( true ) );
    }
}
