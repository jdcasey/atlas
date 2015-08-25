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
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class CycleDetection_BackToRootTCK
    extends AbstractSPI_TCK
{

    @Test
    public void run()
        throws Exception
    {
        final URI source = sourceURI();

        final ProjectVersionRef project = new SimpleProjectVersionRef( "org.my", "project", "1.0" );
        final ProjectVersionRef dep = new SimpleProjectVersionRef( "org.other", "dep", "1.0" );
        final ProjectVersionRef dep2 = new SimpleProjectVersionRef( "org.other", "dep2", "1.0" );

        final RelationshipGraph graph = simpleGraph( project );

        /* @formatter:off */
        graph.storeRelationships( new SimpleDependencyRelationship( source, project, dep.asJarArtifact(), null, 0, false ),
                                  new SimpleDependencyRelationship( source, dep,  dep2.asJarArtifact(), null, 0, false ),
                                  new SimpleDependencyRelationship( source, dep2,  project.asJarArtifact(), null, 0, false ) );
        /* @formatter:on */

        //        final EProjectGraph graph = getManager().getGraph( session, project );

        final Set<EProjectCycle> cycles = graph.getCycles();
        System.out.println( "Cycles:\n\n" + join( cycles, "\n" ) );
        assertThat( cycles.size(), equalTo( 1 ) );

        for ( final EProjectCycle cycle : cycles )
        {
            final Set<ProjectVersionRef> projects = cycle.getAllParticipatingProjects();
            assertThat( projects.contains( project ), equalTo( true ) );
            assertThat( projects.contains( dep ), equalTo( true ) );
            assertThat( projects.contains( dep2 ), equalTo( true ) );
        }
    }

}
