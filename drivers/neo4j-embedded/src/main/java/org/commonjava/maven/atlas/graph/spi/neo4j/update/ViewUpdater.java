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
package org.commonjava.maven.atlas.graph.spi.neo4j.update;

import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.NID;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.RID;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphAdmin;
import org.commonjava.maven.atlas.graph.spi.neo4j.ViewIndexes;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.ConversionCache;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.CyclePath;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.AbstractTraverseVisitor;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.AtlasCollector;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewUpdater
    extends AbstractTraverseVisitor
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Node viewNode;

    private final ConversionCache cache;

    private final GraphAdmin admin;

    private final CycleCacheUpdater cycleUpdater;

    private final ViewIndexes indexes;

    private Node stopNode;

    public ViewUpdater( final ViewParams view, final Node viewNode, final ViewIndexes indexes,
                        final ConversionCache cache, final GraphAdmin admin )
    {
        this.viewNode = viewNode;
        this.indexes = indexes;
        this.cache = cache;
        this.admin = admin;
        this.cycleUpdater = new CycleCacheUpdater( view, viewNode, admin, cache );
    }

    public ViewUpdater( final Node stopNode, final ViewParams view, final Node viewNode, final ViewIndexes indexes,
                        final ConversionCache cache,
                        final GraphAdmin admin )
    {
        this.stopNode = stopNode;
        this.viewNode = viewNode;
        this.indexes = indexes;
        this.cache = cache;
        this.admin = admin;
        this.cycleUpdater = new CycleCacheUpdater( view, viewNode, admin, cache );
    }

    public void cacheRoots( final Set<Node> roots )
    {
        final Transaction tx = admin.beginTransaction();
        try
        {
            final Index<Node> cachedNodes = indexes.getCachedNodes();
            for ( final Node node : roots )
            {
                cachedNodes.add( node, NID, node.getId() );
            }
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    public boolean processAddedRelationships( final Map<Long, ProjectRelationship<?, ?>> createdRelationshipsMap )
    {
        for ( final Entry<Long, ProjectRelationship<?, ?>> entry : createdRelationshipsMap.entrySet() )
        {
            final Long rid = entry.getKey();
            final Relationship add = admin.getRelationship( rid );

            // TODO: WTF is the point of this??
            //            indexes.getSelections()
            //                   .remove( add );
            //

            final Transaction tx = admin.beginTransaction();
            try
            {
                logger.debug( "Checking node cache for: {}", add.getStartNode() );
                final IndexHits<Node> hits = indexes.getCachedNodes()
                                                    .get( NID, add.getStartNode()
                                                                  .getId() );
                if ( hits.hasNext() )
                {
                    Conversions.setMembershipDetectionPending( viewNode, true );
                    Conversions.setCycleDetectionPending( viewNode, true );

                    tx.success();
                    return true;
                }
            }
            finally
            {
                tx.finish();
            }
        }

        return false;
    }

    @Override
    public void includingChild( final Relationship child, final Neo4jGraphPath childPath, final GraphPathInfo childPathInfo, final Path parentPath )
    {
        cachePath( childPath, childPathInfo );
    }

    @Override
    public void configure( final AtlasCollector<?> collector )
    {
        collector.setConversionCache( cache );
        cycleUpdater.configure( collector );
    }

    private void cachePath( final Neo4jGraphPath path, final GraphPathInfo pathInfo )
    {
        final CyclePath cyclePath = CycleCacheUpdater.getTerminatingCycle( path, admin );
        if ( cyclePath != null )
        {
            //            logger.info( "CYCLE: {}", cyclePath );

            final Relationship injector = admin.getRelationship( path.getLastRelationshipId() );
            cycleUpdater.addCycle( cyclePath, injector );

            return;
        }

        final Transaction tx = admin.beginTransaction();
        try
        {
            logger.debug( "Caching path: {}", path );

            final RelationshipIndex cachedRels = indexes.getCachedRelationships();
            final Index<Node> cachedNodes = indexes.getCachedNodes();

            final Set<Long> nodes = new HashSet<Long>();
            for ( final Long relId : path )
            {
                final Relationship r = admin.getRelationship( relId );

                logger.debug( "rel-membership += " + relId );
                cachedRels.add( r, RID, relId );

                final long startId = r.getStartNode()
                                      .getId();
                if ( nodes.add( startId ) )
                {
                    logger.debug( "node-membership += " + startId );
                    cachedNodes.add( r.getStartNode(), NID, startId );
                }

                final long endId = r.getEndNode()
                                    .getId();
                if ( nodes.add( endId ) )
                {
                    logger.debug( "node-membership += " + endId );
                    cachedNodes.add( r.getEndNode(), NID, endId );
                }
            }
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public void cycleDetected( final CyclePath path, final Relationship injector )
    {
        cycleUpdater.cycleDetected( path, injector );
    }

    @Override
    public boolean includeChildren( final Path path, final Neo4jGraphPath graphPath, final GraphPathInfo pathInfo )
    {
        return !( stopNode != null && path.endNode().getId() == stopNode.getId() );

    }

    @Override
    public void traverseComplete( final AtlasCollector<?> collector )
    {
        if ( stopNode == null )
        {
            final Transaction tx = admin.beginTransaction();
            try
            {
                // we did a complete traversal.
                Conversions.setMembershipDetectionPending( viewNode, false );
                tx.success();
            }
            finally
            {
                tx.finish();
            }

            cycleUpdater.traverseComplete( collector );
        }
    }

}
