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
package org.commonjava.maven.atlas.graph.rel;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.commonjava.maven.atlas.graph.rel.RelationshipUtils.dependency;
import static org.commonjava.maven.atlas.ident.util.IdentityUtils.projectVersion;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class RelationshipPathComparatorTest
{

    @Rule
    public TestName naming = new TestName();

    private URI testURI()
        throws URISyntaxException
    {
        return new URI( "test:repo:" + naming.getMethodName() );
    }

    @Test
    public void sortParentDependencyPathAheadOfDirectDependency()
        throws InvalidVersionSpecificationException, URISyntaxException
    {
        final List<List<ProjectRelationship<?, ?>>> paths = new ArrayList<List<ProjectRelationship<?, ?>>>();

        List<ProjectRelationship<?, ?>> rels = new ArrayList<ProjectRelationship<?, ?>>();

        final ProjectVersionRef root = projectVersion( "group.id", "my-artifact", "1.0" );

        final ProjectVersionRef dep = projectVersion( "org.group", "dep-1", "1.0" );

        final URI source = testURI();
        rels.add( dependency( source, root, dep, 0, false, false ) );
        rels.add( dependency( source, dep, projectVersion( "org.foo", "bar", "1.0" ), 0, false, false ) );

        paths.add( rels );

        rels = new ArrayList<ProjectRelationship<?, ?>>();

        final ProjectVersionRef parent = projectVersion( "group.id", "parent", "1" );

        rels.add( new SimpleParentRelationship( source, root, parent ) );
        rels.add( dependency( source, parent, "org.foo", "bar", "1.1.1", 0, false, false ) );

        paths.add( rels );

        Collections.sort( paths, RelationshipPathComparator.INSTANCE );

        final List<ProjectRelationship<?, ?>> result = paths.get( 0 );
        final ProjectRelationship<?, ?> firstResult = result.get( 0 );

        assertThat( ( firstResult instanceof SimpleParentRelationship ), equalTo( true ) );
    }

}
