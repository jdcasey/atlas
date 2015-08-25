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

import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;
import org.commonjava.maven.atlas.tck.graph.AbstractSPI_TCK;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class RelationshipGraphConnection_CreateAndVerifyInListingTCK
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

        final Set<String> all = connectionFactory().listWorkspaces();

        logger.info( "Retrieved all workspaces: {}", all );

        assertThat( all, notNullValue() );
        assertThat( all.size(), equalTo( 1 ) );
        assertThat( all.contains( connection.getWorkspaceId() ), equalTo( true ) );
    }

}
