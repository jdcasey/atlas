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
package org.commonjava.maven.atlas.tck.graph.cycle;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.rel.SimpleDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.SimplePluginRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.atlas.tck.graph.AbstractSPI_TCK;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.join;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class CycleDetection_RootDependentTCK
    extends AbstractSPI_TCK
{

    @Test
    public void run()
        throws Exception
    {
        final URI source = sourceURI();

        final ProjectVersionRef a = new SimpleProjectVersionRef( "project", "A", "1.0" );
        final ProjectVersionRef b = new SimpleProjectVersionRef( "project", "B", "1.0" );
        final ProjectVersionRef c = new SimpleProjectVersionRef( "project", "C", "1.0" );

        final ProjectVersionRef d = new SimpleProjectVersionRef( "project", "D", "1.0" );
        final ProjectVersionRef e = new SimpleProjectVersionRef( "project", "E", "1.0" );

        final RelationshipGraph graph = simpleGraph( a );

        /* @formatter:off */
        // a --> b --> c --> a
        // d --> e --> c --> a --> b --> c
        graph.storeRelationships( new SimpleDependencyRelationship( source, a, new SimpleArtifactRef( b, null, null, false ), null, 0, false ),
                                         new SimpleDependencyRelationship( source, b,  new SimpleArtifactRef( c,  null, null, false ), null, 0, false ),
                                         new SimpleDependencyRelationship( source, c,  new SimpleArtifactRef( a,  null, null, false ), null, 0, false ),
                                         new SimpleDependencyRelationship( source, d, new SimpleArtifactRef( e, null, null, false ), null, 0, false ),
                                         new SimpleDependencyRelationship( source, e,  new SimpleArtifactRef( c,  null, null, false ), null, 0, false ) );
        /* @formatter:on */

        final RelationshipGraph graph2 =
            graphFactory().open( new ViewParams.Builder( graph.getParams() ).withRoots( d )
                                                                            .build(), false );

        final Set<EProjectCycle> cycles1 = graph.getCycles();
        System.out.println( "Graph 1 Cycles:\n\n" + join( cycles1, "\n" ) );

        final Set<EProjectCycle> cycles2 = graph2.getCycles();
        System.out.println( "Graph 2 Cycles:\n\n" + join( cycles2, "\n" ) );

        assertThat( cycles1.size(), equalTo( 1 ) );
        assertThat( cycles2.size(), equalTo( 1 ) );

        final Set<Set<EProjectCycle>> cycleSets = new HashSet<Set<EProjectCycle>>();
        cycleSets.add( cycles1 );
        cycleSets.add( cycles2 );

        int i = 0;
        for ( final Set<EProjectCycle> cycles : cycleSets )
        {
            int j = 0;
            for ( final EProjectCycle cycle : cycles )
            {
                final Set<ProjectVersionRef> refs = cycle.getAllParticipatingProjects();
                assertThat( i + ", " + j + " missing A", refs.contains( a ), equalTo( true ) );
                assertThat( i + ", " + j + " missing B", refs.contains( b ), equalTo( true ) );
                assertThat( i + ", " + j + " missing C", refs.contains( c ), equalTo( true ) );
                j++;
            }
            i++;
        }
    }

}
