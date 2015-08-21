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

import static org.commonjava.maven.atlas.ident.DependencyScope.compile;
import static org.commonjava.maven.atlas.ident.util.IdentityUtils.projectVersion;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.List;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.traverse.TransitiveDependencyTraversal;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.tck.graph.AbstractSPI_TCK;
import org.junit.Test;

public abstract class TransitiveDependencyTraversalTCK
    extends AbstractSPI_TCK
{

    @Test
    public void collectDependencyOfDependency()
        throws Exception
    {
        final URI source = sourceURI();
        final ProjectVersionRef root = projectVersion( "group.id", "my-project", "1.0" );
        final ProjectVersionRef d1 = projectVersion( "other.group", "dep-L1", "1.0.1" );
        final ProjectVersionRef d2 = projectVersion( "foo", "dep-L2", "1.1.1" );

        final RelationshipGraph graph = simpleGraph( root );
        
        /* @formatter:off */
        graph.storeRelationships( new ParentRelationship( source, root ),
                                  new DependencyRelationship( source, root, d1.asJarArtifact(), compile, 0, false ),
                                  new DependencyRelationship( source, d1, d2.asJarArtifact(), compile, 0, false )
        );
        /* @formatter:on */

        final TransitiveDependencyTraversal depTraversal = new TransitiveDependencyTraversal();
        graph.traverse( depTraversal );

        final List<ArtifactRef> artifacts = depTraversal.getArtifacts();

        assertThat( artifacts.size(), equalTo( 2 ) );

        int idx = 0;

        ArtifactRef ref = artifacts.get( idx++ );
        assertThat( ref.getArtifactId(), equalTo( "dep-L1" ) );

        ref = artifacts.get( idx++ );
        assertThat( ref.getArtifactId(), equalTo( "dep-L2" ) );
    }

    @Test
    public void collectDependencyOfDependencyWithExternalDepManagement()
        throws Exception
    {
        final URI source = sourceURI();
        final ProjectVersionRef root = projectVersion( "group.id", "my-project", "1.0" );
        final ProjectVersionRef d1 = projectVersion( "other.group", "dep-L1", "1.0.1" );
        final ProjectVersionRef d2 = projectVersion( "foo", "dep-L2", "1.1.1" );
        final ProjectVersionRef d3 = projectVersion( "foo", "dep-L2", "1.1.2" );

        RelationshipGraph graph = simpleGraph( root );

        /* @formatter:off */
        graph.storeRelationships( new ParentRelationship( source, root ),
                                  new DependencyRelationship( source, root, d1.asJarArtifact(), compile, 0, false ),
                                  new DependencyRelationship( source, d1, d2.asJarArtifact(), compile, 0, false )
        );
        /* @formatter:on */

        graph = graphFactory().open( new ViewParams.Builder( graph.getParams() ).withSelection( d2.asProjectRef(), d3 )
                                                                                .build(), false );

        //        graph.getView()
        //             .selectVersion( d2.asProjectRef(), d3 );

        final TransitiveDependencyTraversal depTraversal = new TransitiveDependencyTraversal();
        graph.traverse( depTraversal );

        final List<ArtifactRef> artifacts = depTraversal.getArtifacts();

        assertThat( artifacts.size(), equalTo( 2 ) );

        int idx = 0;

        ArtifactRef ref = artifacts.get( idx++ );
        assertThat( ref.getArtifactId(), equalTo( "dep-L1" ) );

        ref = artifacts.get( idx++ );
        assertThat( ref.getArtifactId(), equalTo( "dep-L2" ) );
        assertThat( ref.getVersionString(), equalTo( d3.getVersionString() ) );
    }

    @Test
    public void preferDirectDependency()
        throws Exception
    {
        final URI source = sourceURI();

        final ProjectVersionRef root = projectVersion( "group.id", "my-project", "1.0" );
        final ProjectVersionRef d1 = projectVersion( "other.group", "dep-L1", "1.0.1" );
        final ProjectVersionRef d2a = projectVersion( "foo", "dep-L2", "1.1.1" );
        final ProjectVersionRef d2b = projectVersion( "foo", "dep-L2", "1.0" );
        
        final RelationshipGraph graph = simpleGraph( root );

        /* @formatter:off */
        graph.storeRelationships( new ParentRelationship( source, root ),
                                  new DependencyRelationship( source, root, d1.asJarArtifact(), compile, 0, false ),
                                  new DependencyRelationship( source, root, d2a.asJarArtifact(), compile, 1, false ),
                                  new DependencyRelationship( source, d1, d2b.asJarArtifact(), compile, 0, false )
        );
        /* @formatter:on */

        final TransitiveDependencyTraversal depTraversal = new TransitiveDependencyTraversal();
        graph.traverse( depTraversal );

        final List<ArtifactRef> artifacts = depTraversal.getArtifacts();

        assertThat( artifacts.size(), equalTo( 2 ) );

        int idx = 0;

        ArtifactRef ref = artifacts.get( idx++ );
        assertThat( ref.getArtifactId(), equalTo( "dep-L1" ) );

        ref = artifacts.get( idx++ );
        assertThat( ref.getArtifactId(), equalTo( "dep-L2" ) );
        assertThat( ref.getVersionSpec()
                       .renderStandard(), equalTo( "1.1.1" ) );
    }

    @Test
    public void preferLocalDirectDepOverDirectDepInParent()
        throws Exception
    {
        final URI source = sourceURI();

        final ProjectVersionRef root = projectVersion( "group.id", "my-project", "1.0" );
        final ProjectVersionRef parent = projectVersion( "group.id", "parent", "1" );
        final ProjectVersionRef d1a = projectVersion( "other.group", "dep-L1", "1.1.1" );
        final ProjectVersionRef d1b = projectVersion( "other.group", "dep-L1", "1.0" );

        final RelationshipGraph graph = simpleGraph( root );

        /* @formatter:off */
        graph.storeRelationships( new ParentRelationship( source, root, parent ),
                                  new DependencyRelationship( source, root, d1a.asJarArtifact(), compile, 0, false ),
                                  new DependencyRelationship( source, parent, d1b.asJarArtifact(), compile, 0, false )
        );
        /* @formatter:on */

        final TransitiveDependencyTraversal depTraversal = new TransitiveDependencyTraversal();
        graph.traverse( depTraversal );

        final List<ArtifactRef> artifacts = depTraversal.getArtifacts();

        assertThat( artifacts.size(), equalTo( 1 ) );

        int idx = 0;

        final ArtifactRef ref = artifacts.get( idx++ );
        assertThat( ref.getArtifactId(), equalTo( "dep-L1" ) );

        assertThat( ref.getVersionSpec()
                       .renderStandard(), equalTo( "1.1.1" ) );
    }

}
