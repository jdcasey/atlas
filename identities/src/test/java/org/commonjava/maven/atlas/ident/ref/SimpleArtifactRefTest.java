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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class SimpleArtifactRefTest
{

    @Test( expected = InvalidRefException.class )
    public void failParsingGA()
    {
        SimpleArtifactRef.parse( "org.foo:bar" );
    }

    @Test
    public void parseGAVIntoPOM()
    {
        final String g = "org.foo";
        final String a = "bar";
        final String v = "1.0";

        final ArtifactRef ar = SimpleArtifactRef.parse( String.format( "%s:%s:%s", g, a, v ) );

        assertThat( ar.getGroupId(), equalTo( g ) );
        assertThat( ar.getArtifactId(), equalTo( a ) );
        assertThat( ar.getVersionString(), equalTo( v ) );
        assertThat( ar.getType(), equalTo( "pom" ) );
        assertThat( ar.getClassifier(), nullValue() );
    }

    @Test
    public void parseGATV()
    {
        final String g = "org.foo";
        final String a = "bar";
        final String v = "1.0";
        final String t = "zip";

        final ArtifactRef ar = SimpleArtifactRef.parse( String.format( "%s:%s:%s:%s", g, a, t, v ) );

        assertThat( ar.getGroupId(), equalTo( g ) );
        assertThat( ar.getArtifactId(), equalTo( a ) );
        assertThat( ar.getVersionString(), equalTo( v ) );
        assertThat( ar.getType(), equalTo( t ) );
        assertThat( ar.getClassifier(), nullValue() );
    }

    @Test
    public void parseGATVC()
    {
        final String g = "org.foo";
        final String a = "bar";
        final String v = "1.0";
        final String t = "zip";
        final String c = "sources";

        final ArtifactRef ar = SimpleArtifactRef.parse( String.format( "%s:%s:%s:%s:%s", g, a, t, v, c ) );

        assertThat( ar.getGroupId(), equalTo( g ) );
        assertThat( ar.getArtifactId(), equalTo( a ) );
        assertThat( ar.getVersionString(), equalTo( v ) );
        assertThat( ar.getType(), equalTo( t ) );
        assertThat( ar.getClassifier(), equalTo( c ) );
    }

}
