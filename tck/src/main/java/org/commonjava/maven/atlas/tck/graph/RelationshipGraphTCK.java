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
package org.commonjava.maven.atlas.tck.graph;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.AncestryTraversal;
import org.commonjava.maven.atlas.graph.traverse.TraversalType;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public abstract class RelationshipGraphTCK
    extends AbstractSPI_TCK
{

    @Test
    public void createPath_ReturnNullWhenTargetVersionIsAnExpression()
        throws Exception
    {
        final ProjectVersionRef from = new SimpleProjectVersionRef( "org.from", "project", "1.0" );
        final ProjectVersionRef to = new SimpleProjectVersionRef( "org.to", "artifact", "${version.target}" );

        final URI src = new URI( "test:source-uri" );
        final ProjectRelationship<?> rel =
            new DependencyRelationship( src, from, to.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false );

        final RelationshipGraph graph = simpleGraph( from );
        final GraphPath<?> path = graph.createPath( rel );

        assertThat( path, nullValue() );
    }

    @Test
    public void storeBOMThenVerifyBomGAVPresentInView()
        throws Exception
    {
        final URI src = sourceURI();
        final ProjectVersionRef gav = new SimpleProjectVersionRef( "g", "a", "v" );

        final ProjectVersionRef d1 = new SimpleProjectVersionRef( "g", "d1", "1" );
        final ProjectVersionRef d2 = new SimpleProjectVersionRef( "g", "d2", "2" );

        final RelationshipGraph graph =
            openGraph( new ViewParams( newWorkspaceId(), new DependencyFilter(), new ManagedDependencyMutator(), gav ),
                       true );

        /* @formatter:off */
        graph.storeRelationships(
                new ParentRelationship(src, gav),
                new DependencyRelationship(src, gav, d1.asArtifactRef("jar",
                        null), DependencyScope.compile, 0, true),
                new DependencyRelationship(src, gav, d2.asArtifactRef("jar",
                        null), DependencyScope.compile, 1, true));
        /* @formatter:on */

        graph.containsGraph( gav );
    }

    @Test
    public void connectThreeGraphsWithParentInterrelationships()
        throws Exception
    {
        final ProjectVersionRef r = new SimpleProjectVersionRef( "org.test", "root", "1" );
        final ProjectVersionRef p = new SimpleProjectVersionRef( "org.test", "parent", "1.0" );
        final ProjectVersionRef c = new SimpleProjectVersionRef( "org.test", "child", "1.0" );

        final URI source = sourceURI();

        final String wsid = newWorkspaceId();

        openGraph( new ViewParams( wsid, r ), true ).storeRelationships( new ParentRelationship( source, r ) );

        openGraph( new ViewParams( wsid, p ), true ).storeRelationships( new ParentRelationship( source, p, r ) );

        final RelationshipGraph child = openGraph( new ViewParams( wsid, c ), true );

        child.storeRelationships( new ParentRelationship( source, c, p ) );

        System.out.println( "Incomplete subgraphs: " + child.getIncompleteSubgraphs() );
        System.out.flush();
        assertThat( child.isComplete(), equalTo( true ) );

        final AncestryTraversal ancestryTraversal = new AncestryTraversal();
        child.traverse( ancestryTraversal, TraversalType.depth_first );

        final List<ProjectVersionRef> ancestry = ancestryTraversal.getAncestry();
        LoggerFactory.getLogger( getClass() )
                     .info( "Ancestry: {}", ancestry );

        assertThat( ancestry, notNullValue() );
        assertThat( ancestry.size(), equalTo( 3 ) );

        final Iterator<ProjectVersionRef> iterator = ancestry.iterator();
        assertThat( iterator.next(), equalTo( c ) );
        assertThat( iterator.next(), equalTo( p ) );
        assertThat( iterator.next(), equalTo( r ) );
    }

    @Test
    public void connectThreeGraphsWithParentInterrelationships_WrongOrder()
        throws Exception
    {
        final ProjectVersionRef r = new SimpleProjectVersionRef( "org.test", "root", "1" );
        final ProjectVersionRef p = new SimpleProjectVersionRef( "org.test", "parent", "1.0" );
        final ProjectVersionRef c = new SimpleProjectVersionRef( "org.test", "child", "1.0" );

        final URI source = sourceURI();

        final String wsid = newWorkspaceId();

        final RelationshipGraph child = openGraph( new ViewParams( wsid, c ), true );

        child.storeRelationships( new ParentRelationship( source, c, p ) );

        openGraph( new ViewParams( wsid, p ), true ).storeRelationships( new ParentRelationship( source, p, r ) );

        openGraph( new ViewParams( wsid, r ), true ).storeRelationships( new ParentRelationship( source, r ) );

        assertThat( child.isComplete(), equalTo( true ) );

        final AncestryTraversal ancestryTraversal = new AncestryTraversal();
        child.traverse( ancestryTraversal, TraversalType.depth_first );

        final List<ProjectVersionRef> ancestry = ancestryTraversal.getAncestry();
        LoggerFactory.getLogger( getClass() )
                     .info( "Ancestry: {}", ancestry );

        assertThat( ancestry, notNullValue() );
        assertThat( ancestry.size(), equalTo( 3 ) );

        final Iterator<ProjectVersionRef> iterator = ancestry.iterator();
        assertThat( iterator.next(), equalTo( c ) );
        assertThat( iterator.next(), equalTo( p ) );
        assertThat( iterator.next(), equalTo( r ) );
    }

}
