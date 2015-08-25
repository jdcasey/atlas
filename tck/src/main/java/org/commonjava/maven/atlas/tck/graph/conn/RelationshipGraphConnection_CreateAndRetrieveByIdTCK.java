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
package org.commonjava.maven.atlas.tck.graph.conn;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Set;

import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;
import org.commonjava.maven.atlas.tck.graph.AbstractSPI_TCK;
import org.junit.Ignore;
import org.junit.Test;

public class RelationshipGraphConnection_CreateAndRetrieveByIdTCK
    extends AbstractSPI_TCK
{

    @Test
    public void run()
        throws Exception
    {
        final String wsid = newWorkspaceId();
        final RelationshipGraphConnection connection = connectionFactory().openConnection( wsid, true );

        System.out.println( "wsid: " + connection.getWorkspaceId() );

        assertThat( connection, notNullValue() );

        logger.info( "Created connection: {}", connection );

        final RelationshipGraphConnection result = connectionFactory().openConnection( wsid, false );

        logger.info( "Retrieved connection: {}", result );

        assertThat( result, notNullValue() );
        assertThat( result.getWorkspaceId(), equalTo( connection.getWorkspaceId() ) );
        assertThat( result.equals( connection ), equalTo( true ) );
    }

}
