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
package org.commonjava.maven.atlas.tck.graph;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.ServiceLoader;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionFactory;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.tck.graph.testutil.TCKDriver;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSPI_TCK
{

    @Rule
    public TestName naming = new TestName();

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private TCKDriver driver;

    @Before
    public void before()
            throws Exception
    {
        driver = ServiceLoader.load( TCKDriver.class ).iterator().next();
        driver.setup( temp );
    }

    @After
    public void after()
            throws IOException
    {
        if ( driver != null )
        {
            driver.close();
        }
    }

    protected URI sourceURI()
        throws URISyntaxException
    {
        return new URI( "test:repo:" + naming.getMethodName() );
    }

    protected RelationshipGraph simpleGraph( final ProjectVersionRef... roots )
        throws Exception
    {
        final ViewParams params = new ViewParams( newWorkspaceId(), roots );
        params.addActiveSource( sourceURI() );

        return graphFactory().open( params, true );
    }

    protected String newWorkspaceId()
    {
        return "Test-" + System.currentTimeMillis();
    }

    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    private RelationshipGraphFactory graphFactory;

    protected final RelationshipGraphConnectionFactory connectionFactory()
            throws Exception
    {
        return driver.getConnectionFactory();
    }

    protected final RelationshipGraphFactory graphFactory()
        throws Exception
    {
        if ( graphFactory == null )
        {
            graphFactory = new RelationshipGraphFactory( driver.getConnectionFactory() );
        }

        return graphFactory;
    }

    protected final RelationshipGraph openGraph( final ViewParams params, final boolean create )
        throws Exception
    {
        final RelationshipGraph graph =
            graphFactory().open( new ViewParams.Builder( params ).withActiveSources( Collections.singleton( RelationshipUtils.ANY_SOURCE_URI ) )
                                                                 .build(), create );
        return graph;
    }

    private long start;

    @Before
    public void printStart()
    {
        start = System.currentTimeMillis();
        System.out.printf( "***START [%s#%s] (%s)\n\n", naming.getClass(), naming.getMethodName(), new Date().toString() );
    }

    @After
    public void printEnd()
        throws Exception
    {
        System.out.printf( "\n\n***END [%s#%s] - %sms (%s)\n", naming.getClass(), naming.getMethodName(), ( System.currentTimeMillis() - start ),
                           new Date().toString() );

        if ( graphFactory != null )
        {
            graphFactory.close();
        }

        connectionFactory().close();
    }

}
