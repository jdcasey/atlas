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
package org.commonjava.maven.atlas.ident.ref;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.atlas.ident.version.VersionSpec;
import org.junit.Test;

public class ProjectVersionRefTest
{

    @Test
    public void constructWithStringVersionAndRenderStandardSpecMatches()
        throws InvalidVersionSpecificationException
    {
        final String ver = "2.1.1.Final";
        final ProjectVersionRef ref = new SimpleProjectVersionRef( "g", "a", ver );
        final VersionSpec spec = ref.getVersionSpec();

        assertThat( spec.renderStandard(), equalTo( ver ) );
    }

    @Test
    public void hashCodeEquality()
        throws InvalidVersionSpecificationException
    {
        final ProjectVersionRef ref1 = new SimpleProjectVersionRef( "org.foo", "bar", "1.1.1-baz-1" );
        final ProjectVersionRef ref2 = new SimpleProjectVersionRef( "org.foo", "bar", "1.1.1-baz-1" );

        assertThat( ref1.hashCode(), equalTo( ref2.hashCode() ) );
    }

    @Test
    public void objectEquality()
        throws InvalidVersionSpecificationException
    {
        final ProjectVersionRef ref1 = new SimpleProjectVersionRef( "org.foo", "bar", "1.1.1-baz-1" );
        final ProjectVersionRef ref2 = new SimpleProjectVersionRef( "org.foo", "bar", "1.1.1-baz-1" );

        assertThat( ref1, equalTo( ref2 ) );
    }

    @Test
    public void addTwoIdenticalRefsToASetAndVerifyThatOnlyOneIsAdded()
        throws InvalidVersionSpecificationException
    {
        final ProjectVersionRef ref1 = new SimpleProjectVersionRef( "org.foo", "bar", "1.1.1-baz-1" );
        final ProjectVersionRef ref2 = new SimpleProjectVersionRef( "org.foo", "bar", "1.1.1-baz-1" );

        assertThat( ref1, equalTo( ref2 ) );

        final Set<ProjectVersionRef> set = new HashSet<ProjectVersionRef>();
        assertThat( set.add( ref1 ), equalTo( true ) );
        assertThat( set.add( ref2 ), equalTo( false ) );
    }

    @Test
    public void addTwoIdenticalCompoundRefsToASetAndVerifyThatOnlyOneIsAdded()
        throws InvalidVersionSpecificationException
    {
        final ProjectVersionRef ref1 = new SimpleProjectVersionRef( "org.foo", "bar", "[1.1.1-baz-1,1.1.1-baz-2]" );
        final ProjectVersionRef ref2 = new SimpleProjectVersionRef( "org.foo", "bar", "[1.1.1-baz-1,1.1.1-baz-2]" );

        assertThat( ref1, equalTo( ref2 ) );

        final Set<ProjectVersionRef> set = new HashSet<ProjectVersionRef>();
        assertThat( set.add( ref1 ), equalTo( true ) );
        assertThat( set.add( ref2 ), equalTo( false ) );
    }

}
