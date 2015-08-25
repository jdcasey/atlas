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
package org.commonjava.maven.atlas.tck.graph.traverse.ancestry;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Set;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.rel.*;
import org.commonjava.maven.atlas.graph.traverse.AncestryTraversal;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.atlas.tck.graph.AbstractSPI_TCK;
import org.junit.Ignore;
import org.junit.Test;

public class AncestryTraversal_IgnoreNonParentRels_TCK
    extends AbstractSPI_TCK
{

    @Test
    public void run()
        throws Exception
    {
        final ProjectVersionRef myRef = new SimpleProjectVersionRef( "my.group", "my-artifact", "1.0" );
        final ProjectVersionRef parentRef = new SimpleProjectVersionRef( "my.group", "my-dad", "1" );
        final ProjectVersionRef grandRef = new SimpleProjectVersionRef( "other.group", "grandpa", "20120821" );

        final URI source = sourceURI();
        final RelationshipGraph graph = simpleGraph( myRef );

        /* @formatter:off */
        graph.storeRelationships( new SimpleParentRelationship( source, myRef, parentRef ),
              new SimpleDependencyRelationship( source, myRef, new SimpleArtifactRef( new SimpleProjectVersionRef( "some.group", "foo", "1.0"   ), null, null, false ), null, 0, false ),
              new SimpleDependencyRelationship( source, myRef, new SimpleArtifactRef( new SimpleProjectVersionRef( "some.group", "bar", "1.2.1" ), null, null, false ), null, 1, false ),
              new SimplePluginRelationship( source, myRef, new SimpleProjectVersionRef( "org.apache.maven.plugins", "maven-compiler-plugin", "2.5.1" ), 0, false ),
              new SimplePluginRelationship( source, myRef, new SimpleProjectVersionRef( "org.apache.maven.plugins","maven-jar-plugin", "2.2" ), 1, false ),
              new SimpleExtensionRelationship( source, myRef, new SimpleProjectVersionRef( "org.apache.maven.plugins", "maven-compiler-plugin", "2.5.1" ), 0 ),
              new SimpleParentRelationship( source, parentRef, grandRef ),
              new SimpleDependencyRelationship( source, parentRef, new SimpleProjectVersionRef( "other.group", "utils", "3-1" ).asJarArtifact(), null, 0, false )
        );
        /* @formatter:on */

        final AncestryTraversal ancestry = new AncestryTraversal();
        graph.traverse( ancestry );

        final List<ProjectVersionRef> ancestorRefs = ancestry.getAncestry();

        assertThat( ancestorRefs.size(), equalTo( 3 ) );

        int idx = 0;
        ProjectVersionRef ref = ancestorRefs.get( idx++ );

        assertThat( ref.getGroupId(), equalTo( "my.group" ) );
        assertThat( ref.getArtifactId(), equalTo( "my-artifact" ) );

        ref = ancestorRefs.get( idx++ );

        assertThat( ref.getGroupId(), equalTo( "my.group" ) );
        assertThat( ref.getArtifactId(), equalTo( "my-dad" ) );

        ref = ancestorRefs.get( idx++ );

        assertThat( ref.getGroupId(), equalTo( "other.group" ) );
        assertThat( ref.getArtifactId(), equalTo( "grandpa" ) );

    }

}
