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
package org.commonjava.maven.atlas.graph.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.maven.atlas.graph.rel.*;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.jackson.ProjectVersionRefSerializerModule;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 8/21/15.
 */
public class ProjectRelationshipSerializerModuleTest
{

    private ObjectMapper mapper;

    @Before
    public void before()
    {
        mapper = new ObjectMapper();
        mapper.registerModules( new ProjectVersionRefSerializerModule(), new ProjectRelationshipSerializerModule() );
    }

    @Test
    public void roundTrip_TerminalParentRelationship()
            throws Exception
    {
        ParentRelationship rel = new SimpleParentRelationship( new SimpleProjectVersionRef( "org.foo", "bar", "1" ) );

        String json = mapper.writeValueAsString( rel );
        System.out.println( json );

        ProjectRelationship<?, ?> result = mapper.readValue( json, ProjectRelationship.class );

        assertThat( (ParentRelationship) result, equalTo( rel ) );
    }

    @Test
    public void roundTrip_ParentRelationship()
            throws Exception
    {
        ParentRelationship rel = new SimpleParentRelationship( URI.create( "some:test:location" ),
                                                         new SimpleProjectVersionRef( "org.foo", "bar", "1" ),
                                                         new SimpleProjectVersionRef( "org.foo", "parent", "1001" ) );

        String json = mapper.writeValueAsString( rel );
        System.out.println( json );

        ProjectRelationship<?, ?> result = mapper.readValue( json, ProjectRelationship.class );

        assertThat( (ParentRelationship) result, equalTo( rel ) );
    }

    @Test
    public void roundTrip_SimpleConcreteDependency()
            throws Exception
    {
        DependencyRelationship rel =
                new SimpleDependencyRelationship( URI.create( "some:test:location" ), RelationshipUtils.POM_ROOT_URI,
                                            new SimpleProjectVersionRef( "org.foo", "bar", "1" ),
                                            new SimpleProjectVersionRef( "org.foo", "dep", "1.1" ).asJarArtifact(),
                                            DependencyScope.compile, 0, false );

        String json = mapper.writeValueAsString( rel );
        System.out.println( json );

        ProjectRelationship<?, ?> result = mapper.readValue( json, ProjectRelationship.class );

        assertThat( (DependencyRelationship) result, equalTo( rel ) );
    }
}
