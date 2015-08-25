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
import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.commonjava.maven.atlas.graph.rel.SimpleDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.SimpleParentRelationship;
import org.commonjava.maven.atlas.graph.traverse.AncestryTraversal;
import org.commonjava.maven.atlas.graph.traverse.TraversalType;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.atlas.tck.graph.AbstractSPI_TCK;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class RelationshipGraph_StoreAndVerifyInView_TCK
    extends AbstractSPI_TCK
{

    @Test
    public void run()
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
                new SimpleParentRelationship(src, gav),
                new SimpleDependencyRelationship(src, gav, d1.asArtifactRef("jar",
                        null), DependencyScope.compile, 0, true),
                new SimpleDependencyRelationship(src, gav, d2.asArtifactRef("jar",
                        null), DependencyScope.compile, 1, true));
        /* @formatter:on */

        graph.containsGraph( gav );
    }
}
