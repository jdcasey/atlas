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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.List;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.traverse.RelationshipGraphTraversal;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.junit.Test;

public abstract class RelationshipGraphFactoryTCK
    extends AbstractSPI_TCK
{

    @Test
    public void openStoreDeleteAndReopen()
        throws Exception
    {
        final ProjectVersionRef r = new SimpleProjectVersionRef( "org.test", "root", "1" );
        final ProjectVersionRef p = new SimpleProjectVersionRef( "org.test", "parent", "1.0" );
        final ProjectVersionRef c = new SimpleProjectVersionRef( "org.test", "child", "1.0" );

        final URI source = sourceURI();

        final String wsid = newWorkspaceId();

        final RelationshipGraph child = openGraph( new ViewParams( wsid, c ), true );

        child.storeRelationships( new ParentRelationship( source, c, p ) );

        openGraph( new ViewParams( wsid, p ), true ).storeRelationships( new ParentRelationship( source, p, r ) );

        RelationshipGraph graph = openGraph( new ViewParams( wsid, r ), true );
        graph.storeRelationships( new ParentRelationship( source, r ) );

        //        final Thread t = new Thread( new DelayTraverseRunnable( graph ) );
        //        t.setDaemon( true );
        //        t.start();

        try
        {
            graphFactory().deleteWorkspace( wsid );

            graph = openGraph( new ViewParams( wsid, c ), true );
            assertThat( graph, notNullValue() );

            graph.storeRelationships( new ParentRelationship( source, c, p ) );
        }
        finally
        {
            //            t.interrupt();
        }
    }

    public static final class DelayTraverseRunnable
        implements Runnable
    {
        private final RelationshipGraph graph;

        DelayTraverseRunnable( final RelationshipGraph graph )
        {
            this.graph = graph;
        }

        @Override
        public void run()
        {
            try
            {
                graph.traverse( new RelationshipGraphTraversal()
                {
                    @Override
                    public boolean traverseEdge( final ProjectRelationship<?> relationship,
                                                 final List<ProjectRelationship<?>> path )
                    {
                        try
                        {
                            Thread.sleep( 2000 );
                        }
                        catch ( final InterruptedException e )
                        {
                            return false;
                        }
                        return true;
                    }

                    @Override
                    public void startTraverse( final RelationshipGraph graph )
                        throws RelationshipGraphConnectionException
                    {
                    }

                    @Override
                    public boolean preCheck( final ProjectRelationship<?> relationship,
                                             final List<ProjectRelationship<?>> path )
                    {
                        return true;
                    }

                    @Override
                    public void endTraverse( final RelationshipGraph graph )
                        throws RelationshipGraphConnectionException
                    {
                    }

                    @Override
                    public void edgeTraversed( final ProjectRelationship<?> relationship,
                                               final List<ProjectRelationship<?>> path )
                    {
                    }
                } );
            }
            catch ( final RelationshipGraphException e )
            {
                e.printStackTrace();
            }
        }

    }

}
