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
package org.commonjava.maven.atlas.tck.graph.traverse.transdep;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.rel.SimpleDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.SimpleParentRelationship;
import org.commonjava.maven.atlas.graph.traverse.TransitiveDependencyTraversal;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.tck.graph.AbstractSPI_TCK;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;
import java.util.List;

import static org.commonjava.maven.atlas.ident.DependencyScope.compile;
import static org.commonjava.maven.atlas.ident.util.IdentityUtils.projectVersion;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TransitiveDependencyTraversal_PreferDirectTCK
    extends AbstractSPI_TCK
{

    @Test
    public void run()
        throws Exception
    {
        final URI source = sourceURI();

        final ProjectVersionRef root = projectVersion( "group.id", "my-project", "1.0" );
        final ProjectVersionRef d1 = projectVersion( "other.group", "dep-L1", "1.0.1" );
        final ProjectVersionRef d2a = projectVersion( "foo", "dep-L2", "1.1.1" );
        final ProjectVersionRef d2b = projectVersion( "foo", "dep-L2", "1.0" );
        
        final RelationshipGraph graph = simpleGraph( root );

        /* @formatter:off */
        graph.storeRelationships( new SimpleParentRelationship( source, root ),
                                  new SimpleDependencyRelationship( source, root, d1.asJarArtifact(), compile, 0, false ),
                                  new SimpleDependencyRelationship( source, root, d2a.asJarArtifact(), compile, 1, false ),
                                  new SimpleDependencyRelationship( source, d1, d2b.asJarArtifact(), compile, 0, false )
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

}
