package org.commonjava.maven.atlas.tck.graph.traverse.buildorder;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.rel.SimpleDependencyRelationship;
import org.commonjava.maven.atlas.graph.traverse.BuildOrderTraversal;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 8/24/15.
 */
public class SimpleDependencyBuildOrderTCK
    extends AbstractBuildOrderTCK
{
    @Test
    public void run()
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
        graph.storeRelationships( new SimpleDependencyRelationship( source, c, new SimpleArtifactRef( b, null, null, false ), null, 0, false ),
                                  new SimpleDependencyRelationship( source, b, new SimpleArtifactRef( a, null, null, false ), null, 0, false ) );
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

}
