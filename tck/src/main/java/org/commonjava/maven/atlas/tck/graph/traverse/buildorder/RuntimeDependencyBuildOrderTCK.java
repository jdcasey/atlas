package org.commonjava.maven.atlas.tck.graph.traverse.buildorder;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.rel.SimpleDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.SimpleParentRelationship;
import org.commonjava.maven.atlas.graph.rel.SimplePluginRelationship;
import org.commonjava.maven.atlas.graph.traverse.BuildOrderTraversal;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
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
public class RuntimeDependencyBuildOrderTCK
    extends AbstractBuildOrderTCK
{

    @Test
    public void run()
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
        graph.storeRelationships( new SimpleParentRelationship( source, c ),
                                  new SimpleDependencyRelationship( source, c, b.asJarArtifact(), null, 0, false ),
                                  new SimpleDependencyRelationship( source, c, d.asJarArtifact(), DependencyScope.test, 1, false ),
                                  new SimplePluginRelationship( source, c, pb, 0, false ),
                                  new SimpleDependencyRelationship( source, b, a.asJarArtifact(), DependencyScope.runtime, 0, false ),
                                  new SimpleDependencyRelationship( source, d, e.asJarArtifact(), DependencyScope.runtime, 0, false ),
                                  new SimpleDependencyRelationship( source, pb, pa.asJarArtifact(), null, 0, false )
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

}
