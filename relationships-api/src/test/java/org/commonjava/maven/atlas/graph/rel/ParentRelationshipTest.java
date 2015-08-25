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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.net.URISyntaxException;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.VersionUtils;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class ParentRelationshipTest
{

    @Rule
    public TestName naming = new TestName();

    private URI testURI()
        throws URISyntaxException
    {
        return new URI( "test:repo:" + naming.getMethodName() );
    }

    @Test
    public void cloneToDifferentProject()
        throws InvalidVersionSpecificationException, URISyntaxException
    {
        final ProjectVersionRef projectRef =
            new SimpleProjectVersionRef( "org.foo", "foobar", VersionUtils.createSingleVersion( "1.0" ) );

        final ProjectVersionRef project2Ref =
            new SimpleProjectVersionRef( "org.foo", "footoo", VersionUtils.createSingleVersion( "1.0" ) );

        final ProjectVersionRef parentRef =
            new SimpleProjectVersionRef( "org.foo", "foobar-parent", VersionUtils.createSingleVersion( "1" ) );

        final URI source = testURI();
        final ParentRelationship pr = new SimpleParentRelationship( source, projectRef, parentRef );
        final ParentRelationship pr2 = (ParentRelationship) pr.cloneFor( project2Ref );

        assertThat( pr.getDeclaring(), equalTo( projectRef ) );
        assertThat( pr2.getDeclaring(), equalTo( project2Ref ) );
        assertThat( pr.getTarget(), equalTo( parentRef ) );
        assertThat( pr2.getTarget(), equalTo( parentRef ) );
    }

}
