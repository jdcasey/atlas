package org.commonjava.maven.atlas.graph.spi.neo4j.update;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphAdmin;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.ConversionCache;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.CyclePath;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.AbstractTraverseVisitor;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.AtlasCollector;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CycleCacheUpdater
    extends AbstractTraverseVisitor
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final ConversionCache cache;

    private final Node viewNode;

    final Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();

    private final GraphView view;

    public CycleCacheUpdater( final GraphView view, final Node viewNode, final ConversionCache cache )
    {
        this.view = view;
        this.viewNode = viewNode;
        this.cache = cache;
    }

    @Override
    public void cycleDetected( final CyclePath cyclicPath, final Relationship injector )
    {
        if ( cyclicPath.length() < 1 )
        {
            logger.debug( "No paths in cycle!" );
            return;
        }

        addCycleInternal( cyclicPath, injector );
    }

    public void addCycle( final CyclePath cyclicPath, final Relationship injector )
    {
        if ( cyclicPath.length() < 1 )
        {
            logger.debug( "No paths in cycle!" );
            return;
        }

        addCycleInternal( cyclicPath, injector );
    }

    private void addCycleInternal( final CyclePath cyclicPath, final Relationship injector )
    {
        logger.debug( "Adding cycle: {} (via: {})", cyclicPath, injector );
        Conversions.storeCachedCyclePath( cyclicPath, viewNode );
    }

    public static CyclePath getTerminatingCycle( final Path path )
    {
        final Logger logger = LoggerFactory.getLogger( CycleCacheUpdater.class );
        logger.debug( "Looking for terminating cycle in: {}", path );

        final List<Long> rids = new ArrayList<Long>();
        final List<Long> starts = new ArrayList<Long>();
        for ( final Relationship pathR : path.relationships() )
        {
            rids.add( pathR.getId() );

            final long sid = pathR.getStartNode()
                                  .getId();

            final long eid = pathR.getEndNode()
                                  .getId();

            final int idx = starts.indexOf( eid );
            if ( idx > -1 )
            {
                final CyclePath cp = new CyclePath( rids.subList( idx, rids.size() ) );
                logger.debug( "Detected cycle: {}", cp );

                return cp;
            }

            starts.add( sid );
        }

        logger.debug( "No cycle detected" );

        return null;
    }

    public static CyclePath getTerminatingCycle( final Neo4jGraphPath graphPath, final GraphAdmin admin )
    {
        final Logger logger = LoggerFactory.getLogger( CycleCacheUpdater.class );
        logger.debug( "Looking for terminating cycle in: {}", graphPath );

        final Map<Long, Long> startNodesToRids = new HashMap<Long, Long>();
        final long[] rids = graphPath.getRelationshipIds();
        Long startRid = null;
        for ( final long rid : rids )
        {
            final Relationship r = admin.getRelationship( rid );

            final long eid = r.getEndNode()
                              .getId();

            startRid = startNodesToRids.get( eid );
            if ( startRid != null )
            {
                break;
            }

            startNodesToRids.put( r.getStartNode()
                                   .getId(), r.getId() );
        }

        if ( startRid != null )
        {
            int i = 0;
            for ( ; i < rids.length; i++ )
            {
                if ( rids[i] == startRid )
                {
                    break;
                }
            }

            final long[] cycle = new long[rids.length - i];
            System.arraycopy( rids, i, cycle, 0, cycle.length );

            final CyclePath cp = new CyclePath( cycle );
            logger.debug( "Detected cycle: {}", cp );
            return cp;
        }

        logger.debug( "No cycle detected" );

        return null;
    }

    @Override
    public void configure( final AtlasCollector<?> collector )
    {
        collector.setConversionCache( cache );
    }

    public int getCycleCount()
    {
        return cycles.size();
    }

    public Set<EProjectCycle> getCycles()
    {
        return cycles;
    }

    @Override
    public void traverseComplete( final AtlasCollector<?> collector )
    {
        logger.debug( "Clearing PENDING cycle-detection for: {} of view: {}", viewNode, view.getShortId() );
        Conversions.setCycleDetectionPending( viewNode, false );
    }

}