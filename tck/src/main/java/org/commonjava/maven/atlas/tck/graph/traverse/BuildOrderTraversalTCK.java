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
package org.commonjava.maven.atlas.tck.graph.traverse;

import static org.apache.commons.lang.StringUtils.join;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.traverse.BuildOrderTraversal;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.atlas.tck.graph.AbstractSPI_TCK;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public abstract class BuildOrderTraversalTCK
    extends AbstractSPI_TCK
{

    @Test
    public void simpleDependencyBuildOrder()
        throws Exception
    {
        final ProjectVersionRef c = new SimpleProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new SimpleProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new SimpleProjectVersionRef( "group.id", "a", "1" );

        final Map<ProjectVersionRef, ProjectVersionRef> relativeOrder =
            new HashMap<ProjectVersionRef, ProjectVersionRef>();
        relativeOrder.put( c, b );
        relativeOrder.put( b, a );

        final URI source = sourceURI();
        final RelationshipGraph graph = simpleGraph( c );

        /* @formatter:off */
        graph.storeRelationships( new DependencyRelationship( source, c, new SimpleArtifactRef( b, null, null, false ), null, 0, false ),
                                  new DependencyRelationship( source, b, new SimpleArtifactRef( a, null, null, false ), null, 0, false ) );
        /* @formatter:on */

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 2 ) );

        logger.info( "Starting build-order traversal" );
        final BuildOrderTraversal bo = new BuildOrderTraversal( new DependencyFilter( DependencyScope.test ) );
        graph.traverse( bo );

        final BuildOrder buildOrderObj = bo.getBuildOrder();
        final List<ProjectRef> buildOrder = buildOrderObj.getOrder();

        assertRelativeOrder( relativeOrder, buildOrder );
    }

    @Test
    //@Ignore
    public void simpleDependencyBuildOrder_includeDepParent()
        throws Exception

    {
        final ProjectVersionRef c = new SimpleProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new SimpleProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new SimpleProjectVersionRef( "group.id", "a", "1" );
        final ProjectVersionRef p = new SimpleProjectVersionRef( "group.id", "b-parent", "1001" );

        final Map<ProjectVersionRef, ProjectVersionRef> relativeOrder =
            new HashMap<ProjectVersionRef, ProjectVersionRef>();
        relativeOrder.put( c, b );
        relativeOrder.put( b, a );
        relativeOrder.put( b, p );

        final URI source = sourceURI();
        final RelationshipGraph graph = simpleGraph( c );

        /* @formatter:off */
        graph.storeRelationships( new ParentRelationship( source, c ), 
                                  new ParentRelationship( source, b, p ),
                                  new DependencyRelationship( source, c, b.asJarArtifact(), null, 0, false ),
                                  new DependencyRelationship( source, b, a.asJarArtifact(), null, 0, false )
        );
        /* @formatter:on */

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 3 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal( new DependencyFilter( DependencyScope.test ) );
        graph.traverse( bo );

        final BuildOrder buildOrderObj = bo.getBuildOrder();
        final List<ProjectRef> buildOrder = buildOrderObj.getOrder();

        LoggerFactory.getLogger( getClass() )
                     .info( "Build order: {}", buildOrder );

        assertThat( buildOrder.size(), equalTo( 4 ) );

        assertRelativeOrder( relativeOrder, buildOrder );
    }

    @Test
    //@Ignore
    public void simpleDependencyBuildOrder_IgnorePluginPath()
        throws Exception
    {
        final ProjectVersionRef c = new SimpleProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new SimpleProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new SimpleProjectVersionRef( "group.id", "a", "1" );
        final ProjectVersionRef pa = new SimpleProjectVersionRef( "plugin.id", "p-a", "1" );
        final ProjectVersionRef pb = new SimpleProjectVersionRef( "plugin.id", "p-b", "2" );

        final Map<ProjectVersionRef, ProjectVersionRef> relativeOrder =
            new HashMap<ProjectVersionRef, ProjectVersionRef>();
        relativeOrder.put( c, b );
        relativeOrder.put( b, a );

        final URI source = sourceURI();
        final RelationshipGraph graph = simpleGraph( c );

        /* @formatter:off */
        graph.storeRelationships( new ParentRelationship( source, c ),
                                  new DependencyRelationship( source, c, b.asJarArtifact(), null, 0, false ),
                                  new PluginRelationship( source, c, pb, 0, false ),
                                  new DependencyRelationship( source, pb, pa.asJarArtifact(), null, 0, false ),
                                  new DependencyRelationship( source, b, a.asJarArtifact(), null, 0, false )
        );
        /* @formatter:on */

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 4 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal( new DependencyFilter( DependencyScope.test ) );
        graph.traverse( bo );

        final BuildOrder buildOrderObj = bo.getBuildOrder();
        final List<ProjectRef> buildOrder = buildOrderObj.getOrder();

        assertRelativeOrder( relativeOrder, buildOrder );
    }

    @Test
    //@Ignore
    public void simpleDependencyBuildOrder_runtimeDepsOnly()
        throws Exception
    {
        final ProjectVersionRef e = new SimpleProjectVersionRef( "group.id", "e", "5" );
        final ProjectVersionRef d = new SimpleProjectVersionRef( "group.id", "d", "4" );
        final ProjectVersionRef c = new SimpleProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new SimpleProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new SimpleProjectVersionRef( "group.id", "a", "1" );
        final ProjectVersionRef pa = new SimpleProjectVersionRef( "plugin.id", "p-a", "1" );
        final ProjectVersionRef pb = new SimpleProjectVersionRef( "plugin.id", "p-b", "2" );

        final Map<ProjectVersionRef, ProjectVersionRef> relativeOrder =
            new HashMap<ProjectVersionRef, ProjectVersionRef>();
        relativeOrder.put( c, b );
        relativeOrder.put( b, a );

        final URI source = sourceURI();
        final RelationshipGraph graph = simpleGraph( c );

        /* @formatter:off */
        graph.storeRelationships( new ParentRelationship( source, c ),
                                  new DependencyRelationship( source, c, b.asJarArtifact(), null, 0, false ),
                                  new DependencyRelationship( source, c, d.asJarArtifact(), DependencyScope.test, 1, false ),
                                  new PluginRelationship( source, c, pb, 0, false ),
                                  new DependencyRelationship( source, b, a.asJarArtifact(), DependencyScope.runtime, 0, false ),
                                  new DependencyRelationship( source, d, e.asJarArtifact(), DependencyScope.runtime, 0, false ),
                                  new DependencyRelationship( source, pb, pa.asJarArtifact(), null, 0, false )
        );
        /* @formatter:on */

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 6 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal( new DependencyFilter( DependencyScope.runtime ) );
        graph.traverse( bo );

        final BuildOrder buildOrderObj = bo.getBuildOrder();
        final List<ProjectRef> buildOrder = buildOrderObj.getOrder();

        assertThat( buildOrder.size(), equalTo( 3 ) );

        assertRelativeOrder( relativeOrder, buildOrder );
    }

    @Test
    //@Ignore
    public void simpleDependencyBuildOrder_ignoreExcluded()
        throws Exception
    {
        final ProjectVersionRef d = new SimpleProjectVersionRef( "group.id", "d", "4" );
        final ProjectVersionRef c = new SimpleProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new SimpleProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new SimpleProjectVersionRef( "group.id", "a", "1" );

        final Map<ProjectVersionRef, ProjectVersionRef> relativeOrder =
            new HashMap<ProjectVersionRef, ProjectVersionRef>();
        relativeOrder.put( c, b );
        relativeOrder.put( b, a );

        final URI source = sourceURI();
        final RelationshipGraph graph = simpleGraph( c );

        /* @formatter:off */
        graph.storeRelationships( new ParentRelationship( source, c ),
                                  new DependencyRelationship( source, c, b.asJarArtifact(), null, 0, false, d ),
                                  new DependencyRelationship( source, b, a.asJarArtifact(), DependencyScope.runtime, 0, false ),
                                  new DependencyRelationship( source, b, d.asJarArtifact(), DependencyScope.runtime, 1, false )
        );
        /* @formatter:on */

        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 3 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal( new DependencyFilter( DependencyScope.runtime ) );
        graph.traverse( bo );

        final BuildOrder buildOrderObj = bo.getBuildOrder();
        final List<ProjectRef> buildOrder = buildOrderObj.getOrder();

        logger.info( "Build order: {}", buildOrder );
        assertThat( buildOrder.size(), equalTo( 3 ) );

        assertRelativeOrder( relativeOrder, buildOrder );
    }

    @Test
    //@Ignore
    public void simpleEverythingBuildOrder()
        throws Exception
    {
        final ProjectVersionRef c = new SimpleProjectVersionRef( "group.id", "c", "3" );
        final ProjectVersionRef b = new SimpleProjectVersionRef( "group.id", "b", "2" );
        final ProjectVersionRef a = new SimpleProjectVersionRef( "group.id", "a", "1" );
        final ProjectVersionRef pa = new SimpleProjectVersionRef( "plugin.dep.id", "p-a", "1" );
        final ProjectVersionRef pb = new SimpleProjectVersionRef( "plugin.id", "p-b", "2" );

        final Map<ProjectVersionRef, ProjectVersionRef> relativeOrder =
            new HashMap<ProjectVersionRef, ProjectVersionRef>();
        relativeOrder.put( c, b );
        relativeOrder.put( b, a );
        relativeOrder.put( c, pb );
        relativeOrder.put( pb, pa );

        final URI source = sourceURI();
        final RelationshipGraph graph = simpleGraph( c );

        /* @formatter:off */
        graph.storeRelationships( new ParentRelationship( source, c ),
                                  new DependencyRelationship( source, c, b.asJarArtifact(), null, 0, false ),
                                  new PluginRelationship( source, c, pb, 0, false ),
                                  new DependencyRelationship( source, b, a.asJarArtifact(), DependencyScope.runtime, 0, false ),
                                  new DependencyRelationship( source, pb, pa.asJarArtifact(), null, 0, false )
        );
        /* @formatter:on */

        System.out.println( "Got relationships:\n\n  " + join( graph.getAllRelationships(), "\n  " ) );
        assertThat( graph.getAllRelationships()
                         .size(), equalTo( 4 ) );

        final BuildOrderTraversal bo = new BuildOrderTraversal();
        graph.traverse( bo );

        final BuildOrder buildOrderObj = bo.getBuildOrder();
        final List<ProjectRef> buildOrder = buildOrderObj.getOrder();

        System.out.printf( "Build order: %s\n", buildOrder );

        assertRelativeOrder( relativeOrder, buildOrder );
    }

    private void assertRelativeOrder( final Map<ProjectVersionRef, ProjectVersionRef> relativeOrder,
                                      final List<ProjectRef> buildOrder )
    {
        for ( final Map.Entry<ProjectVersionRef, ProjectVersionRef> entry : relativeOrder.entrySet() )
        {
            final ProjectRef k = entry.getKey()
                                      .asProjectRef();
            final ProjectRef v = entry.getValue()
                                      .asProjectRef();

            final int kidx = buildOrder.indexOf( k );
            final int vidx = buildOrder.indexOf( v );

            if ( kidx < 0 )
            {
                fail( "Cannot find: " + k + " in build order: " + buildOrder );
            }

            if ( vidx < 0 )
            {
                fail( "Cannot find: " + v + " in build order: " + buildOrder );
            }

            if ( vidx >= kidx )
            {
                fail( "prerequisite project: " + v + " of: " + k + " appears AFTER it in the build order: "
                    + buildOrder );
            }
        }
    }

}
