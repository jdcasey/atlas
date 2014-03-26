package org.commonjava.maven.atlas.graph.spi.neo4j.update;

import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.NID;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.RID;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.model.GraphView;
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

    private final GraphAdmin maint;

    private final CycleCacheUpdater cycleUpdater;

    private final ViewIndexes indexes;

    private Node stopNode;

    public ViewUpdater( final GraphView view, final Node viewNode, final ViewIndexes indexes, final ConversionCache cache, final GraphAdmin maint )
    {
        this.viewNode = viewNode;
        this.indexes = indexes;
        this.cache = cache;
        this.maint = maint;
        this.cycleUpdater = new CycleCacheUpdater( view, viewNode, cache );
    }

    public ViewUpdater( final Node stopNode, final GraphView view, final Node viewNode, final ViewIndexes indexes, final ConversionCache cache,
                        final GraphAdmin maint )
    {
        this.stopNode = stopNode;
        this.viewNode = viewNode;
        this.indexes = indexes;
        this.cache = cache;
        this.maint = maint;
        this.cycleUpdater = new CycleCacheUpdater( view, viewNode, cache );
    }

    public void cacheRoots( final Set<Node> roots )
    {
        final Index<Node> cachedNodes = indexes.getCachedNodes();
        for ( final Node node : roots )
        {
            cachedNodes.add( node, NID, node.getId() );
        }
    }

    public boolean processAddedRelationships( final Map<Long, ProjectRelationship<?>> createdRelationshipsMap )
    {
        for ( final Entry<Long, ProjectRelationship<?>> entry : createdRelationshipsMap.entrySet() )
        {
            final Long rid = entry.getKey();
            final Relationship add = maint.getRelationship( rid );

            indexes.getSelections()
                   .remove( add );

            logger.debug( "Checking node cache for: {}", add.getStartNode() );
            final IndexHits<Node> hits = indexes.getCachedNodes()
                                                .get( NID, add.getStartNode()
                                                              .getId() );
            if ( hits.hasNext() )
            {
                Conversions.setMembershipDetectionPending( viewNode, true );
                Conversions.setCycleDetectionPending( viewNode, true );
                return true;
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
        final CyclePath cyclePath = CycleCacheUpdater.getTerminatingCycle( path, maint );
        if ( cyclePath != null )
        {
            logger.info( "CYCLE: {}", cyclePath );

            final Relationship injector = maint.getRelationship( path.getLastRelationshipId() );
            cycleUpdater.addCycle( cyclePath, injector );

            return;
        }

        logger.debug( "Caching path: {}", path );

        final RelationshipIndex cachedRels = indexes.getCachedRelationships();
        final Index<Node> cachedNodes = indexes.getCachedNodes();

        final Set<Long> nodes = new HashSet<Long>();
        for ( final Long relId : path )
        {
            final Relationship r = maint.getRelationship( relId );

            cachedRels.add( r, RID, relId );

            final long startId = r.getStartNode()
                                  .getId();
            if ( nodes.add( startId ) )
            {
                cachedNodes.add( r.getStartNode(), NID, startId );
            }

            final long endId = r.getEndNode()
                                .getId();
            if ( nodes.add( endId ) )
            {
                cachedNodes.add( r.getEndNode(), NID, endId );
            }
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
        if ( stopNode != null && path.endNode()
                                     .getId() == stopNode.getId() )
        {
            return false;
        }

        return true;
    }

    @Override
    public void traverseComplete( final AtlasCollector<?> collector )
    {
        if ( stopNode == null )
        {
            // we did a complete traversal.
            Conversions.setMembershipDetectionPending( viewNode, false );
            cycleUpdater.traverseComplete( collector );
        }
    }

}