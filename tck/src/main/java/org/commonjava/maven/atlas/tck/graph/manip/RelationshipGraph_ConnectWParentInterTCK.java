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
package org.commonjava.maven.atlas.tck.graph.manip;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.rel.SimpleParentRelationship;
import org.commonjava.maven.atlas.graph.traverse.AncestryTraversal;
import org.commonjava.maven.atlas.graph.traverse.TraversalType;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.atlas.tck.graph.AbstractSPI_TCK;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class RelationshipGraph_ConnectWParentInterTCK
    extends AbstractSPI_TCK
{

    @Test
    public void run()
        throws Exception
    {
        final ProjectVersionRef r = new SimpleProjectVersionRef( "org.test", "root", "1" );
        final ProjectVersionRef p = new SimpleProjectVersionRef( "org.test", "parent", "1.0" );
        final ProjectVersionRef c = new SimpleProjectVersionRef( "org.test", "child", "1.0" );

        final URI source = sourceURI();

        final String wsid = newWorkspaceId();

        openGraph( new ViewParams( wsid, r ), true ).storeRelationships( new SimpleParentRelationship( r ) );

        openGraph( new ViewParams( wsid, p ), true ).storeRelationships( new SimpleParentRelationship( source, p, r ) );

        final RelationshipGraph child = openGraph( new ViewParams( wsid, c ), true );

        child.storeRelationships( new SimpleParentRelationship( source, c, p ) );

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
}
