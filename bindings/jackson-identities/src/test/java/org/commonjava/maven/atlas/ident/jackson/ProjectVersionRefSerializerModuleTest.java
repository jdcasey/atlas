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
package org.commonjava.maven.atlas.ident.jackson;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProjectVersionRefSerializerModuleTest
{

    private ObjectMapper mapper;

    @Before
    public void setup()
    {
        mapper = new ObjectMapper();
        mapper.registerModule( new ProjectVersionRefSerializerModule() );
    }

    @Test
    public void projectRefRoundTrip()
        throws Exception
    {
        final ProjectRef pr = new SimpleProjectRef( "org.foo", "bar" );
        final String json = mapper.writeValueAsString( pr );

        final ProjectRef result = mapper.readValue( json, ProjectRef.class );

        assertThat( result, equalTo( pr ) );
    }

    @Test
    public void mapWithProjectRefKeyRoundTrip()
        throws Exception
    {
        final ProjectRef pr = new SimpleProjectRef( "org.foo", "bar" );
        final String value = "this is the value";

        final Map<ProjectRef, String> map = new HashMap<ProjectRef, String>();
        map.put( pr, value );

        final String json = mapper.writeValueAsString( map );

        final Map<ProjectRef, String> result = mapper.readValue( json, new TypeReference<Map<ProjectRef, String>>()
        {
        } );

        assertThat( result.get( pr ), equalTo( value ) );
    }

}
