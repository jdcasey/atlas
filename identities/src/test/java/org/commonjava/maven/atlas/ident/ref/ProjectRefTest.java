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

import org.junit.Test;

public class ProjectRefTest
{

    @Test
    public void matchesTotalWildcardGroupId()
    {
        final ProjectRef pattern = new SimpleProjectRef( "*", "foo" );
        final ProjectRef test = new SimpleProjectRef( "org.bar", "foo" );

        assertThat( pattern.matches( test ), equalTo( true ) );
    }

    @Test
    public void matchesTotalWildcardArtifactId()
    {
        final ProjectRef pattern = new SimpleProjectRef( "org.bar", "*" );
        final ProjectRef test = new SimpleProjectRef( "org.bar", "foo" );

        assertThat( pattern.matches( test ), equalTo( true ) );
    }

    @Test
    public void matchesTerminatingWildcardGroupId()
    {
        final ProjectRef pattern = new SimpleProjectRef( "org.*", "foo" );
        final ProjectRef test = new SimpleProjectRef( "org.bar", "foo" );

        assertThat( pattern.matches( test ), equalTo( true ) );
    }

    @Test
    public void matchesTerminatingWildcardArtifactId()
    {
        final ProjectRef pattern = new SimpleProjectRef( "org.bar", "fo*" );
        final ProjectRef test = new SimpleProjectRef( "org.bar", "foo" );

        assertThat( pattern.matches( test ), equalTo( true ) );
    }

    @Test
    public void matchesEmbeddedWildcardGroupId()
    {
        final ProjectRef pattern = new SimpleProjectRef( "org.*r", "foo" );
        final ProjectRef test = new SimpleProjectRef( "org.bar", "foo" );

        assertThat( pattern.matches( test ), equalTo( true ) );
    }

    @Test
    public void matchesEmbeddedWildcardArtifactId()
    {
        final ProjectRef pattern = new SimpleProjectRef( "org.bar", "f*o" );
        final ProjectRef test = new SimpleProjectRef( "org.bar", "foo" );

        assertThat( pattern.matches( test ), equalTo( true ) );
    }

}
