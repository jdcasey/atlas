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
package org.commonjava.maven.atlas.graph.spi.neo4j;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.rel.*;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.ConversionCache;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.AbstractNeoProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.CyclePath;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.commonjava.maven.atlas.graph.spi.neo4j.traverse.*;
import org.commonjava.maven.atlas.graph.spi.neo4j.update.CycleCacheUpdater;
import org.commonjava.maven.atlas.graph.spi.neo4j.update.ViewUpdater;
import org.commonjava.maven.atlas.graph.traverse.RelationshipGraphTraversal;
import org.commonjava.maven.atlas.graph.traverse.TraversalType;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.*;
import static org.commonjava.maven.atlas.graph.spi.neo4j.traverse.TraversalUtils.getGraphRelTypes;

public class FileNeo4JGraphConnection
    implements Runnable, Neo4JGraphConnection
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    //    private static final int ADD_BATCHSIZE = 50;

    private static final String ALL_RELATIONSHIPS = "all_relationships";

    private static final String BY_GAV_IDX = "by_gav";

    private static final String BY_GA_IDX = "by_ga";

    private static final String CONFIG_NODES_IDX = "config_nodes";

    private static final String VARIABLE_NODES_IDX = "variable_nodes";

    private static final String MISSING_NODES_IDX = "missing_nodes";

    private static final String METADATA_INDEX_PREFIX = "has_metadata_";

    private static final String MANAGED_GA = "managed-ga";

    private static final String MANAGED_KEY = "mkey";

    private static final String BASE_CONFIG_NODE = "_base";

    private static final String MKEY_FORMAT = "%d/%s/%s:%s";

    //    private static final String GRAPH_ATLAS_TYPES_CLAUSE = join( GraphRelType.atlasRelationshipTypes(), "|" );

    /* @formatter:off */
//    private static final String CYPHER_SELECTION_RETRIEVAL = String.format(
//        "CYPHER 1.8 START a=node({roots}) " 
//            + "\nMATCH p1=(a)-[:{}*1..]->(s), " 
//            + "\n    p2=(a)-[:{}*1..]->(v) "
//            + "\nWITH v, s, last(relationships(p1)) as r1, last(relationships(p2)) as r2 "
//            + "\nWHERE v.{} = s.{} "
//            + "\n    AND v.{} = s.{} "
//            + "\n    AND has(r1.{}) "
//            + "\n    AND any(x in r1.{} "
//            + "\n        WHERE x IN {roots}) "
//            + "\n    AND has(r2.{}) "
//            + "\n    AND any(x in r2.{} "
//            + "\n          WHERE x IN {roots}) "
//            + "\nRETURN r1,r2,v,s",
//        GRAPH_ATLAS_TYPES_CLAUSE, GRAPH_ATLAS_TYPES_CLAUSE, 
//        Conversions.GROUP_ID, Conversions.GROUP_ID, 
//        Conversions.ARTIFACT_ID, Conversions.ARTIFACT_ID, 
//        Conversions.SELECTED_FOR, Conversions.SELECTED_FOR, 
//        Conversions.DESELECTED_FOR, Conversions.DESELECTED_FOR
//    );
    
    /* @formatter:on */

    private boolean closed = false;

    private GraphDatabaseService graph;

    private final boolean useShutdownHook;

    private ExecutionEngine queryEngine;

    private Node configNode;

    private final GraphAdminImpl adminAccess;

    private final String workspaceId;

    private final FileNeo4jConnectionFactory factory;

    private final File dbDir;

    FileNeo4JGraphConnection( final String workspaceId, final File dbDir, final boolean useShutdownHook,
                              final FileNeo4jConnectionFactory factory )
    {
        this.workspaceId = workspaceId;
        this.dbDir = dbDir;
        this.factory = factory;
        this.adminAccess = new GraphAdminImpl( this );

        this.graph = new GraphDatabaseFactory().newEmbeddedDatabase( dbDir.getAbsolutePath() );
        this.useShutdownHook = useShutdownHook;

        printStats();

        if ( useShutdownHook )
        {
            Runtime.getRuntime()
                   .addShutdownHook( new Thread( this ) );
        }

        final Transaction tx = graph.beginTx();
        try
        {
            graph.createNode();

            long id = -1;
            final IndexHits<Node> hits = graph.index()
                                              .forNodes( CONFIG_NODES_IDX )
                                              .get( CONFIG_ID, BASE_CONFIG_NODE );
            if ( hits.hasNext() )
            {
                configNode = hits.next();
                id = configNode.getId();
            }

            if ( id < 0 )
            {
                configNode = graph.createNode();
                id = configNode.getId();

                graph.index()
                     .forNodes( CONFIG_NODES_IDX )
                     .add( configNode, CONFIG_ID, BASE_CONFIG_NODE );

            }

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    protected GraphDatabaseService getGraph()
    {
        return graph;
    }

    protected boolean isUseShutdownHook()
    {
        return useShutdownHook;
    }

    @Override
    public void printStats()
    {
        final StringBuilder stats = new StringBuilder();
        stats.append( "Graph in: " )
             .append( dbDir );
        stats.append( "\ncontains " )
             .append( graph.index()
                           .forNodes( BY_GAV_IDX )
                           .query( GAV, "*" )
                           .size() )
             .append( " nodes." );

        stats.append( "\ncontains " )
             .append( graph.index()
                           .forRelationships( ALL_RELATIONSHIPS )
                           .query( RELATIONSHIP_ID, "*" )
                           .size() )
             .append( " relationships." );

        logger.info( stats.toString() );
    }

    @Override
    public Collection<? extends ProjectRelationship<?, ?>> getRelationshipsDeclaredBy( final ViewParams params,
                                                                                    final ProjectVersionRef ref )
    {
        checkClosed();

        if ( ref == null )
        {
            return null;
        }

        final Index<Node> index = graph.index()
                                       .forNodes( BY_GAV_IDX );
        final IndexHits<Node> hits = index.get( GAV, ref.asProjectVersionRef()
                                                        .toString() );

        if ( hits.hasNext() )
        {
            final Node node = hits.next();
            final Iterable<Relationship> relationships = node.getRelationships( Direction.OUTGOING );
            return convertToRelationships( relationships, new ConversionCache() );
        }

        return null;
    }

    private synchronized void checkClosed()
    {
        if ( closed || graph == null )
        {
            throw new IllegalStateException( "Graph database has been closed!" );
        }
    }

    @Override
    public Collection<? extends ProjectRelationship<?, ?>> getRelationshipsTargeting( final ViewParams params,
                                                                                   final ProjectVersionRef ref )
    {
        checkClosed();

        final ConversionCache cache = new ConversionCache();
        if ( registerView( params ) )
        {
            final Node node = getNode( ref );

            final Iterable<Relationship> relationships = node.getRelationships( Direction.INCOMING );
            final StringBuilder sb = new StringBuilder();
            for ( final Relationship r : relationships )
            {
                if ( sb.length() > 0 )
                {
                    sb.append( ' ' );
                }

                sb.append( r.getId() );
            }

            final RelationshipIndex cachedRels = new ViewIndexes( graph.index(), params ).getCachedRelationships();
            final IndexHits<Relationship> hits = cachedRels.query( RID, sb.toString() );

            final Set<ProjectRelationship<?, ?>> result = new HashSet<ProjectRelationship<?, ?>>();

            while ( hits.hasNext() )
            {
                final Relationship r = hits.next();
                final ProjectRelationship<?, ?> rel = toProjectRelationship( r, cache );
                result.add( rel );
            }

            return result;
        }

        final Index<Node> index = graph.index()
                                       .forNodes( BY_GAV_IDX );
        final IndexHits<Node> hits = index.get( GAV, ref.asProjectVersionRef()
                                                        .toString() );

        if ( hits.hasNext() )
        {
            final Node node = hits.next();
            // FIXME: What if this params has a filter or mutator?? Without a root, that would be very strange...
            final Iterable<Relationship> relationships = node.getRelationships( Direction.INCOMING );
            return convertToRelationships( relationships, cache );
        }

        return null;
    }

    @Override
    public Set<ProjectVersionRef> getAllProjects( final ViewParams params )
    {
        checkClosed();

        logger.debug( "Getting all-projects for: {}", params );
        final ConversionCache cache = new ConversionCache();
        if ( registerView( params ) )
        {
            final Index<Node> cachedNodes = new ViewIndexes( graph.index(), params ).getCachedNodes();

            final IndexHits<Node> nodeHits = cachedNodes.query( NID, "*" );
            final Set<ProjectVersionRef> nodes = new HashSet<ProjectVersionRef>();
            while ( nodeHits.hasNext() )
            {
                nodes.add( toProjectVersionRef( nodeHits.next(), cache ) );
            }

            return nodes;
        }

        // FIXME: What if this params has a filter or mutator?? Without a root, that would be very strange...
        return new HashSet<ProjectVersionRef>( convertToProjects( graph.index()
                                                                       .forNodes( BY_GAV_IDX )
                                                                       .query( GAV, "*" ), cache ) );
    }

    private void updateView( final ViewParams params, final ConversionCache cache )
    {
        if ( params.getRoots() == null || params.getRoots()
                                                .isEmpty() )
        {
            logger.debug( "paramss without roots are never updated." );
            return;
        }

        final Node paramsNode = getViewNode( params );

        logger.debug( "Checking whether {} ({} / {}) is in need of update.", params.getShortId(), paramsNode, params );
        final ViewIndexes indexes = new ViewIndexes( graph.index(), params );

        if ( Conversions.isMembershipDetectionPending( paramsNode ) )
        {
            logger.debug( "Traversing graph to update params membership: {} ({})", params.getShortId(), params );
            final Set<Node> roots = getRoots( params );
            if ( roots.isEmpty() )
            {
                logger.debug( "{}: No root nodes found.", params.getShortId() );
                return;
            }

            final ViewUpdater updater = new ViewUpdater( params, paramsNode, indexes, cache, adminAccess );
            collectAtlasRelationships( params, updater, roots, false, Uniqueness.RELATIONSHIP_GLOBAL );

            logger.debug( "Traverse complete for update of params: {}", params.getShortId() );
        }
        else
        {
            logger.debug( "{}: no update pending.", params.getShortId() );
        }
    }

    @Override
    public Collection<ProjectRelationship<?, ?>> getAllRelationships( final ViewParams params )
    {
        checkClosed();

        final ConversionCache cache = new ConversionCache();
        if ( registerView( params ) )
        {
            final RelationshipIndex cachedRels = new ViewIndexes( graph.index(), params ).getCachedRelationships();

            final IndexHits<Relationship> relHits = cachedRels.query( RID, "*" );
            final Set<ProjectRelationship<?, ?>> rels = new HashSet<ProjectRelationship<?, ?>>();
            while ( relHits.hasNext() )
            {
                rels.add( toProjectRelationship( relHits.next(), cache ) );
            }

            return rels;
        }

        final IndexHits<Relationship> hits = graph.index()
                                                  .forRelationships( ALL_RELATIONSHIPS )
                                                  .query( RELATIONSHIP_ID, "*" );
        synchronized ( hits )
        {
            return convertToRelationships( hits, cache );
        }
    }

    @Override
    public Map<GraphPath<?>, GraphPathInfo> getPathMapTargeting( final ViewParams params,
                                                                 final Set<ProjectVersionRef> refs )
    {
        checkClosed();
        if ( !registerView( params ) )
        {
            throw new IllegalArgumentException(
                                                "You must specify at least one root GAV in order to retrieve path-related info." );
        }

        final Set<Node> endNodes = getNodes( refs );

        final ConversionCache cache = new ConversionCache();
        final PathCollectingVisitor visitor = new PathCollectingVisitor( endNodes, cache );
        collectAtlasRelationships( params, visitor, getRoots( params ), false, Uniqueness.RELATIONSHIP_GLOBAL );

        final Map<GraphPath<?>, GraphPathInfo> result = new HashMap<GraphPath<?>, GraphPathInfo>();
        for ( final Neo4jGraphPath path : visitor )
        {
            GraphPathInfo info = new GraphPathInfo( this, params );
            for ( final Long rid : path )
            {
                final Relationship r = graph.getRelationshipById( rid );
                info = info.getChildPathInfo( toProjectRelationship( r, cache ) );
            }

            result.put( path, info );
        }

        return result;
    }

    @Override
    public ProjectVersionRef getPathTargetRef( final GraphPath<?> path )
    {
        if ( path == null )
        {
            return null;
        }

        if ( !( path instanceof Neo4jGraphPath ) )
        {
            throw new IllegalArgumentException( "GraphPath instances must be of type Neo4jGraphPath. Was: "
                + path.getClass()
                      .getName() );
        }

        final long rid = ( (Neo4jGraphPath) path ).getLastRelationshipId();
        if ( rid < 0 )
        {
            return null;
        }

        final Relationship rel = graph.getRelationshipById( rid );
        final Node target = rel.getEndNode();

        return toProjectVersionRef( target, null );
    }

    @Override
    public Set<List<ProjectRelationship<?, ?>>> getAllPathsTo( final ViewParams params, final ProjectVersionRef... refs )
    {
        checkClosed();
        if ( !registerView( params ) )
        {
            throw new IllegalArgumentException(
                                                "You must specify at least one root GAV in order to retrieve path-related info." );
        }

        final Set<Node> endNodes = getNodes( refs );

        final ConversionCache cache = new ConversionCache();
        final PathCollectingVisitor visitor = new PathCollectingVisitor( endNodes, cache );
        collectAtlasRelationships( params, visitor, getRoots( params ), false, Uniqueness.RELATIONSHIP_GLOBAL );

        final Set<List<ProjectRelationship<?, ?>>> result = new HashSet<List<ProjectRelationship<?, ?>>>();
        for ( final Neo4jGraphPath path : visitor )
        {
            result.add( convertToRelationships( path, adminAccess, cache ) );
        }

        return result;
    }

    @Override
    public synchronized Set<ProjectRelationship<?, ?>> addRelationships( final ProjectRelationship<?, ?>... rels )
    {
        final Map<Long, ProjectRelationship<?, ?>> createdRelationshipsMap = addRelationshipsInternal( rels );

        logger.info( "Updating all-projects caches with {} new entries", createdRelationshipsMap.size() );
        updateCaches( createdRelationshipsMap );

        // FIXME: We're delaying cycle detection, so there will NEVER be rejected relationships...
        final Set<ProjectRelationship<?, ?>> skipped = Collections.emptySet();
        logger.debug( "Cycle injection detected for: {}", skipped );
        logger.info( "Returning {} rejected relationships.", skipped.size() );

        //        printGraphStats();

        return skipped;
    }

    private Map<Long, ProjectRelationship<?, ?>> addRelationshipsInternal( final ProjectRelationship<?, ?>... rels )
    {
        checkClosed();

        final ConversionCache cache = new ConversionCache();
        final Map<Long, ProjectRelationship<?, ?>> createdRelationshipsMap = new HashMap<Long, ProjectRelationship<?, ?>>();

        final List<ProjectRelationship<?, ?>> sorted = new ArrayList<ProjectRelationship<?, ?>>( Arrays.asList( rels ) );
        Collections.sort( sorted, RelationshipComparator.INSTANCE );

        final Transaction tx = graph.beginTx();
        try
        {
            //            int txBatchCount = 0;
            nextRel: for ( final ProjectRelationship<?, ?> rel : sorted )
            {
                if ( (rel instanceof AbstractNeoProjectRelationship ) && !( (AbstractNeoProjectRelationship) rel ).isDirty())
                {
                    logger.debug("Clean Neo4j-backed relationship: {} NOT being added.", rel );
                    continue;
                }

                logger.debug( "Checking relationship: {}", rel );

                final Index<Node> index = graph.index()
                                               .forNodes( BY_GAV_IDX );

                final ProjectVersionRef declaring = rel.getDeclaring();
                final ProjectVersionRef target = rel.getTarget()
                                                    .asProjectVersionRef();

                final Node[] nodes = new Node[2];
                int i = 0;
                for ( final ProjectVersionRef ref : new ProjectVersionRef[] { declaring, target } )
                {
                    final IndexHits<Node> hits = index.get( GAV, ref.asProjectVersionRef()
                                                                    .toString() );
                    if ( !hits.hasNext() )
                    {
                        logger.debug( "Creating new node for: {} to support addition of relationship: {}", ref, rel );
                        try
                        {
                            final Node node = newProjectNode( ref );
                            logger.debug( "Node: {} created for: {}", node, ref );
                            nodes[i] = node;
                        }
                        catch ( final InvalidVersionSpecificationException e )
                        {
                            // FIXME: This means we're discarding a rejected relationship without passing it back...NOT GOOD
                            // However, some code assumes rejects are cycles...also not good.
                            logger.error( String.format( "Failed to create node for project ref: %s. Reason: %s", ref,
                                                         e.getMessage() ), e );
                            continue nextRel;
                        }
                    }
                    else
                    {
                        nodes[i] = hits.next();

                        logger.debug( "Using existing project node: {} for: {}", nodes[i], ref.asProjectVersionRef() );
                    }

                    i++;
                }

                final RelationshipIndex relIdx = graph.index()
                                                      .forRelationships( ALL_RELATIONSHIPS );

                final String relId = id( rel );
                Relationship relationship = getRelationship( relId );
                if ( relationship == null )
                {
                    final Node from = nodes[0];

                    logger.debug( "Removing missing/incomplete flag from: {} ({})", from, declaring );
                    graph.index()
                         .forNodes( MISSING_NODES_IDX )
                         .remove( from );

                    markConnected( from, true );

                    if ( from.getId() != nodes[1].getId() )
                    {
                        final Node to = nodes[1];

                        logger.debug( "Creating graph relationship for: {} between node: {} and node: {}", rel, from,
                                      to );

                        final GraphRelType grt = GraphRelType.map( rel.getType(), rel.isManaged() );

                        relationship = from.createRelationshipTo( to, grt );

                        // now, we set an index on the relationship of where it is in the range of ALL atlas relationships 
                        // for this node. ProjectRelationship<?>.getIndex() only gives the index for that TYPE, so we can't
                        // use it. The next value will be stored on the from node for the next go.
                        int nodeRelIdx = Conversions.getIntegerProperty( Conversions.ATLAS_RELATIONSHIP_COUNT, from, 0 );

                        relationship.setProperty( Conversions.ATLAS_RELATIONSHIP_INDEX, nodeRelIdx );
                        from.setProperty( Conversions.ATLAS_RELATIONSHIP_COUNT, ++nodeRelIdx );

                        logger.debug( "New relationship is: {} with type: {}", relationship, grt );

                        toRelationshipProperties( rel, relationship );
                        relIdx.add( relationship, RELATIONSHIP_ID, relId );

                        if ( rel.isManaged() )
                        {
                            graph.index()
                                 .forRelationships( MANAGED_GA )
                                 .add( relationship,
                                       MANAGED_KEY,
                                       String.format( MKEY_FORMAT, relationship.getStartNode()
                                                                               .getId(), rel.getType()
                                                                                            .name(), rel.getTarget()
                                                                                                        .getGroupId(),
                                                      rel.getTarget()
                                                         .getArtifactId() ) );
                        }

                        logger.debug( "+= {} ({})", relationship, rel );
                    }
                    else
                    {
                        logger.info( "Self-referential relationship: {}. Skipping", rel );
                        continue;
                    }

                    if ( !( rel instanceof SimpleParentRelationship ) || !( (ParentRelationship) rel ).isTerminus() )
                    {
                        createdRelationshipsMap.put( relationship.getId(), rel );
                    }
                }
                else
                {
                    logger.debug( "== {} ({})", relationship, new RelToString( relationship, cache ) );

                    addToURISetProperty( rel.getSources(), SOURCE_URI, relationship );
                }

                // We don't really need transaction here, I don't think...
                // but we're forced to use one to update the graph.
                // So, to find a balance between memory consumed by a transaction 
                // and speed (creating/committing txns takes time), we'll batch them.
                //                txBatchCount++;
                //                if ( txBatchCount >= ADD_BATCHSIZE )
                //                {
                //                    logger.info( "Storing batch of {} relationships.", txBatchCount + 1 );
                //                    tx.success();
                //                    tx = graph.beginTx();
                //                    txBatchCount = 0;
                //                }
            }

            //            logger.info( "Storing final batch of {} relationships.", txBatchCount + 1 );
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        return createdRelationshipsMap;
    }

    @Override
    public boolean introducesCycle( final ViewParams params, final ProjectRelationship<?, ?> rel )
    {
        checkClosed();

        final ProjectVersionRef to = rel.getDeclaring();
        final ProjectVersionRef from = rel.getTarget()
                                          .asProjectVersionRef();

        final Node toNode = getNode( to );
        final Node fromNode = getNode( from );

        if ( toNode == null || fromNode == null )
        {
            return false;
        }

        logger.debug( "Checking for existence of path from: {} to: {} in global database", fromNode, toNode );
        final PathExistenceVisitor collector = new PathExistenceVisitor( toNode );
        collectAtlasRelationships( params, collector, Collections.singleton( fromNode ), false,
                                   Uniqueness.RELATIONSHIP_GLOBAL );

        return collector.isFound();
    }

    private Node newProjectNode( final ProjectVersionRef ref )
    {
        final Node node = graph.createNode();

        toNodeProperties( ref, node, false );

        final String gav = ref.asProjectVersionRef()
                              .toString();

        graph.index()
             .forNodes( BY_GAV_IDX )
             .add( node, GAV, gav );

        graph.index()
             .forNodes( BY_GA_IDX )
             .add( node, GA, ref.asProjectRef()
                                .toString() );

        graph.index()
             .forNodes( MISSING_NODES_IDX )
             .add( node, GAV, gav );

        if ( ref.isVariableVersion() )
        {
            //            logger.info( "Adding {} to variable-nodes index.", ref );
            graph.index()
                 .forNodes( VARIABLE_NODES_IDX )
                 .add( node, GAV, gav );
        }

        //        logger.info( "Created project node: {} with id: {}", ref, node.getId() );
        return node;
    }

    private Relationship select( final Relationship old, final ViewParams params, final Node paramsNode,
                                 final GraphPathInfo pathInfo, final Neo4jGraphPath path )
    {
        final ViewIndexes indexes = new ViewIndexes( graph.index(), params );

        final long targetRid = Conversions.getDeselectionTarget( old.getId(), paramsNode );
        if ( targetRid > -1 )
        {
            return graph.getRelationshipById( targetRid );
        }

        final ProjectRelationship<?, ?> oldRel = toProjectRelationship( old, null );
        if ( oldRel == null )
        {
            return null;
        }

        logger.debug( "Selecting mutated relationship for: {} with pathInfo: {}", oldRel, pathInfo );
        final ProjectRelationship<?, ?> selected = pathInfo == null ? oldRel : pathInfo.selectRelationship( oldRel, path );

        if ( selected == null )
        {
            return null;
        }

        if ( selected != oldRel )
        {
            //                logger.info( "Checking for existing DB relationship for: {}", selected );
            final String selId = id( selected );
            Relationship result = getRelationship( selId );
            if ( result != null )
            {
                return result;
            }

            logger.debug( "Creating ad-hoc db relationship for selection: {} (replacing: {})", selected, oldRel );

            @SuppressWarnings( "unused" )
            final Map<Long, ProjectRelationship<?, ?>> added = addRelationshipsInternal( selected );

//            final Transaction tx = graph.beginTx();
            try
            {
                result = getRelationship( selId );
                if ( result != null )
                {
                    logger.debug( "Adding relationship {} to selections index", result );
                    Conversions.setSelection( old.getId(), result.getId(), paramsNode );

                    // Does this imply that a whole subgraph from oldRel needs to be removed from the cache??
                    // No, because that would only happen if a new selection were added to the params, which would trigger a registerViewSelection() call...
                    logger.debug( "Adding node {} to membership cache for {}", result.getEndNode()
                                                                                     .getId(), params.getShortId() );
                    indexes.getCachedNodes()
                           .add( result.getEndNode(), NID, result.getEndNode()
                                                                 .getId() );
                }

//                tx.success();

                return result;
            }
            finally
            {
//                tx.finish();
            }
        }

        return old;
    }

    //    private Set<ProjectVersionRef> getProjectsRootedAt( final ViewParams params, final Set<Node> roots )
    //    {
    //        Iterable<Node> nodes = null;
    //        if ( roots != null && !roots.isEmpty() )
    //        {
    //            final RootedNodesCollector agg = new RootedNodesCollector( roots, params, false );
    //            collectAtlasRelationships( params, agg, roots, false );
    //            nodes = agg;
    //        }
    //        else
    //        {
    //            final IndexHits<Node> hits = graph.index()
    //                                              .forNodes( BY_GAV_IDX )
    //                                              .query( GAV, "*" );
    //            nodes = hits;
    //        }
    //
    //        return new HashSet<ProjectVersionRef>( convertToProjects( nodes ) );
    //    }

    @Override
    public void traverse( final RelationshipGraphTraversal traversal, final ProjectVersionRef root,
                          final RelationshipGraph graph, final TraversalType type )
        throws RelationshipGraphConnectionException
    {
        final Node rootNode = getNode( root );
        if ( rootNode == null )
        {
            //            logger.debug( "Root node not found! (root: {})", root );
            return;
        }

        //            logger.debug( "PASS: {}", i );

        // NOTE: Changing this means some cases of morphing filters/mutators may NOT report correct results.
        //            TraversalDescription description = Traversal.traversal( Uniqueness.RELATIONSHIP_PATH )
        TraversalDescription description = Traversal.traversal( Uniqueness.RELATIONSHIP_GLOBAL )
                                                    .sort( PathComparator.INSTANCE );

        final ViewParams params = graph.getParams();
        final GraphRelType[] relTypes = getGraphRelTypes( params.getFilter() );
        for ( final GraphRelType grt : relTypes )
        {
            description.relationships( grt, Direction.OUTGOING );
        }

        if ( type == TraversalType.breadth_first )
        {
            description = description.breadthFirst();
        }
        else
        {
            description = description.depthFirst();
        }

        //            logger.debug( "starting traverse of: {}", net );
        traversal.startTraverse( graph );

        final ConversionCache cache = new ConversionCache();

        final Node paramsNode = getViewNode( params );

        @SuppressWarnings( { "rawtypes", "unchecked" } )
        final MembershipWrappedTraversalEvaluator checker =
            new MembershipWrappedTraversalEvaluator( Collections.singleton( rootNode.getId() ), traversal, this,
                                                     params, paramsNode, adminAccess, relTypes );

        checker.setConversionCache( cache );

        description = description.expand( checker )
                                 .evaluator( checker );

        Transaction tx = this.graph.beginTx();
        try
        {
            final Traverser traverser = description.traverse( rootNode );
            for ( final Path path : traverser )
            {
                if ( path.lastRelationship() == null )
                {
                    continue;
                }

                final List<ProjectRelationship<?, ?>> rels = convertToRelationships( path.relationships(), cache );
                logger.debug( "traversing path: {}", rels );
                for ( final ProjectRelationship<?, ?> rel : rels )
                {
                    logger.debug( "traverse: {}", rel );
                    if ( traversal.traverseEdge( rel, rels ) )
                    {
                        logger.debug( "traversed: {}", rel );
                        traversal.edgeTraversed( rel, rels );
                    }
                }
            }

            tx.success();
        }
        finally
        {
            tx.finish();
        }

        traversal.endTraverse( graph );
    }

    @Override
    public boolean containsProject( final ViewParams params, final ProjectVersionRef ref )
    {
        checkClosed();

        final IndexHits<Node> missing = graph.index()
                                             .forNodes( MISSING_NODES_IDX )
                                             .get( GAV, ref.asProjectVersionRef()
                                                           .toString() );
        if ( missing.size() > 0 )
        {
            return false;
        }

        final Node node = getNode( ref );
        if ( node == null )
        {
            return false;
        }

        if ( registerView( params ) )
        {
            final Index<Node> cachedNodes = new ViewIndexes( graph.index(), params ).getCachedNodes();

            final IndexHits<Node> nodeHits = cachedNodes.get( NID, node.getId() );
            return nodeHits.hasNext();
        }
        else
        {
            return getNode( ref ) != null;
        }
    }

    @Override
    public boolean containsRelationship( final ViewParams params, final ProjectRelationship<?, ?> rel )
    {
        checkClosed();

        final Relationship relationship = getRelationship( rel );
        if ( relationship == null )
        {
            return false;
        }

        if ( registerView( params ) )
        {
            return new ViewIndexes( graph.index(), params ).getCachedRelationships()
                                                           .get( RID, relationship.getId() )
                                                           .hasNext();
        }
        else
        {
            return true;
        }
    }

    private Set<Node> getNodes( final Set<ProjectVersionRef> refs )
    {
        final Set<Node> nodes = new HashSet<Node>( refs.size() );
        for ( final ProjectVersionRef ref : refs )
        {
            final Node node = getNode( ref );
            if ( node != null )
            {
                nodes.add( node );
            }
        }

        return nodes;
    }

    private Set<Node> getNodes( final ProjectVersionRef... refs )
    {
        final Set<Node> nodes = new HashSet<Node>( refs.length );
        for ( final ProjectVersionRef ref : refs )
        {
            final Node node = getNode( ref );
            if ( node != null )
            {
                nodes.add( node );
            }
        }

        return nodes;
    }

    protected Node getNode( final ProjectVersionRef ref )
    {
        checkClosed();

        final Index<Node> idx = graph.index()
                                     .forNodes( BY_GAV_IDX );

        final IndexHits<Node> hits = idx.get( GAV, ref.asProjectVersionRef()
                                                      .toString() );

        if ( hits.size() < 1 )
        {
            return null;
        }

        final Node node = hits.next();

        return node;
    }

    protected Relationship getRelationship( final ProjectRelationship<?, ?> rel )
    {
        return getRelationship( id( rel ) );
    }

    Relationship getRelationship( final String relId )
    {
        checkClosed();

        final RelationshipIndex idx = graph.index()
                                           .forRelationships( ALL_RELATIONSHIPS );

        final IndexHits<Relationship> hits = idx.get( RELATIONSHIP_ID, relId );
        synchronized ( hits )
        {
            return hits.hasNext() ? hits.next() : null;
        }
    }

    private static final int SHUTDOWN_WAIT = 5;

    @Override
    public synchronized void close()
        throws RelationshipGraphConnectionException
    {
        closed = true;

        factory.connectionClosing( workspaceId );

        if ( graph != null )
        {
            try
            {
                logger.info( "Shutting down graph..." );
                printStats();

                graph.shutdown();

                logger.info( "Waiting for shutdown..." );
                if ( graph.isAvailable( 1000 * SHUTDOWN_WAIT ) )
                {
                    throw new RelationshipGraphConnectionException( "Failed to shutdown graph: %s.", dbDir );
                }

                graph = null;

                logger.info( "...graph shutdown complete." );
            }
            catch ( final Exception e )
            {
                throw new RelationshipGraphConnectionException( "Failed to shutdown: " + e.getMessage(), e );
            }
        }

    }

    @Override
    public boolean isClosed()
    {
        return closed;
    }

    @Override
    public void run()
    {
        try
        {
            close();
        }
        catch ( final RelationshipGraphConnectionException e )
        {
            //            new Logger( getClass() ).debug( "Failed to shutdown graph database. Reason: {}", e, e.getMessage() );
        }
    }

    @SuppressWarnings( "unused" )
    private boolean isMissing( final Node node )
    {
        return !isConnected( node );
    }

    @Override
    public boolean isMissing( final ViewParams params, final ProjectVersionRef ref )
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( MISSING_NODES_IDX )
                                          .get( GAV, ref.asProjectVersionRef()
                                                        .toString() );

        return hits.size() > 0;
        //        final IndexHits<Node> hits = graph.index()
        //                                          .forNodes( BY_GAV_IDX )
        //                                          .get( GAV, ref.asProjectVersionRef().toString() );
        //
        //        if ( hits.size() > 0 )
        //        {
        //            return !isConnected( hits.next() );
        //        }
        //
        //        return false;
    }

    @Override
    public boolean hasMissingProjects( final ViewParams params )
    {
        return hasIndexedProjects( params, MISSING_NODES_IDX );
    }

    @Override
    public Set<ProjectVersionRef> getMissingProjects( final ViewParams params )
    {
        logger.debug( "Getting missing projects for: {}", params.getShortId() );
        return getIndexedProjects( params, MISSING_NODES_IDX );
    }

    private Set<ProjectVersionRef> getIndexedProjects( final ViewParams params, final String indexName )
    {
        checkClosed();

        final IndexHits<Node> hits = graph.index()
                                          .forNodes( indexName )
                                          .query( GAV, "*" );

        final Set<ProjectVersionRef> result = new HashSet<ProjectVersionRef>();

        final ConversionCache cache = new ConversionCache();
        if ( registerView( params ) )
        {
            final Index<Node> cachedNodes = new ViewIndexes( graph.index(), params ).getCachedNodes();

            for ( final Node node : hits )
            {
                logger.debug( "Checking for membership: {} ({})", node, node.getProperty( GAV ) );
                final IndexHits<Node> cacheHits = cachedNodes.get( NID, node.getId() );
                if ( cacheHits.hasNext() )
                {
                    logger.debug( "Including: {}", node );
                    result.add( toProjectVersionRef( node, cache ) );
                }
            }
        }
        else
        {
            for ( final Node node : hits )
            {
                logger.debug( "Including: {}", node );
                result.add( toProjectVersionRef( node, cache ) );
            }
        }

        return result;
    }

    private boolean hasIndexedProjects( final ViewParams params, final String indexName )
    {
        checkClosed();

        final IndexHits<Node> hits = graph.index()
                                          .forNodes( indexName )
                                          .query( GAV, "*" );

        if ( registerView( params ) )
        {
            final Index<Node> cachedNodes = new ViewIndexes( graph.index(), params ).getCachedNodes();

            for ( final Node node : hits )
            {
                final IndexHits<Node> cacheHits = cachedNodes.get( NID, node.getId() );
                if ( cacheHits.hasNext() )
                {
                    return true;
                }
            }
        }
        else
        {
            if ( hits.hasNext() )
            {
                return true;
            }
        }

        return false;
    }

    private Set<Node> getRoots( final ViewParams params )
    {
        return getRoots( params, true );
    }

    private Set<Node> getRoots( final ViewParams params, final boolean defaultToAll )
    {
        final Set<ProjectVersionRef> rootRefs = params.getRoots();
        if ( ( rootRefs == null || rootRefs.isEmpty() ) && defaultToAll )
        {
            return Collections.emptySet();
            //            final Set<Node> connectedNodes = Conversions.toSet( graph.index()
            //                                                                     .forNodes( BY_GAV_IDX )
            //                                                                     .query( GAV, "*" ) );
            //            connectedNodes.removeAll( Conversions.toSet( graph.index()
            //                                                              .forNodes( MISSING_NODES_IDX )
            //                                                              .query( GAV, "*" ) ) );
            //            connectedNodes.removeAll( Conversions.toSet( graph.index()
            //                                                              .forNodes( VARIABLE_NODES_IDX )
            //                                                              .query( GAV, "*" ) ) );
            //
            //            return connectedNodes;
        }

        final Set<Node> nodes = new HashSet<Node>( rootRefs.size() );
        for ( final ProjectVersionRef ref : rootRefs )
        {
            final Node n = getNode( ref );
            if ( n != null )
            {
                nodes.add( n );
            }
        }

        return nodes;
    }

    private void collectAtlasRelationships( final ViewParams params, final TraverseVisitor visitor,
                                            final Set<Node> start, final boolean sorted, final Uniqueness uniqueness )
    {
        if ( start == null || start.isEmpty() )
        {
            throw new UnsupportedOperationException(
                                                     "Cannot collect atlas nodes/relationships via traversal without at least one 'from' node!" );
        }

        //        logger.info( "Traversing for aggregation using: {} from roots: {}", checker.getClass()
        //                                                                                   .getName(), from );

        TraversalDescription description = Traversal.traversal( uniqueness );
        if ( sorted )
        {
            description = description.sort( PathComparator.INSTANCE );
        }

        final GraphRelType[] relTypes = getGraphRelTypes( params.getFilter() );
        for ( final GraphRelType grt : relTypes )
        {
            description.relationships( grt, Direction.OUTGOING );
        }

        description = description.breadthFirst();

        final Node paramsNode = getViewNode( params );

        final AtlasCollector<Object> checker =
            new AtlasCollector<Object>( visitor, start, this, params, paramsNode, adminAccess, relTypes );

        description = description.expand( checker )
                                 .evaluator( checker );

        Transaction tx = graph.beginTx();
        try
        {
            final Traverser traverser = description.traverse( start.toArray( new Node[start.size()] ) );
            for ( @SuppressWarnings( "unused" )
            final Path path : traverser )
            {
                //            logger.info( "Aggregating path: {}", path );
                // Don't need this, but we need to iterate the traverser.
            }
            tx.success();
        }
        finally
        {
            visitor.traverseComplete( checker );
            tx.finish();
        }
    }

    @Override
    public boolean hasVariableProjects( final ViewParams params )
    {
        return hasIndexedProjects( params, VARIABLE_NODES_IDX );
    }

    @Override
    public Set<ProjectVersionRef> getVariableProjects( final ViewParams params )
    {
        logger.debug( "Getting variable projects for: {}", params.getShortId() );
        return getIndexedProjects( params, VARIABLE_NODES_IDX );
    }

    @Override
    public boolean addCycle( final EProjectCycle cycle )
    {
        // NOP, auto-detected.
        return false;
    }

    @Override
    public Set<EProjectCycle> getCycles( final ViewParams params )
    {
        checkClosed();

        final ViewIndexes indexes = new ViewIndexes( graph.index(), params );

        if ( !registerView( params ) )
        {
            logger.warn( "Skipping cycle detection on {}. View doesn't declare a root GAV, and this is prohibitively expensive! (params info: {})",
                         params.getShortId(), params );
            return null;
        }

        final ConversionCache cache = new ConversionCache();

        final Transaction tx = graph.beginTx();
        Node paramsNode;
        try
        {
            paramsNode = getViewNode( params );

            if ( Conversions.isCycleDetectionPending( paramsNode ) )
            {
                logger.debug( "Cycle-detection is pending for: {}", params.getShortId() );

                Set<Node> nodes;
                //                if ( global )
                //                {
                //                    // FIXME: This seems to be VERY expensive
                //                    nodes = Conversions.toSet( graph.index()
                //                                                    .forNodes( BY_GAV_IDX )
                //                                                    .query( GAV, "*" ) );
                //                }
                //                else
                //                {
                nodes = Conversions.toSet( indexes.getCachedNodes()
                                                  .query( NID, "*" ) );
                //                }

                logger.info( "Traversing graph to find cycles for params {}", params.getShortId() );
                final CycleCacheUpdater cycleUpdater = new CycleCacheUpdater( params, paramsNode, adminAccess, cache );
                // NOTE: Changing this means some cases of morphing filters/mutators may NOT report correct results.
                //                collectAtlasRelationships( params, cycleUpdater, nodes, false, global ? Uniqueness.RELATIONSHIP_GLOBAL : Uniqueness.RELATIONSHIP_PATH );
                collectAtlasRelationships( params, cycleUpdater, nodes, false, Uniqueness.RELATIONSHIP_GLOBAL );

                final int cycleCount = cycleUpdater.getCycleCount();
                logger.info( "Registered {} cycles in params {}'s cycle cache.", cycleCount, params.getShortId() );

                return cycleUpdater.getCycles();
            }

            tx.success();
        }
        finally
        {
            tx.finish();
        }

        final Set<CyclePath> cyclePaths = Conversions.getCachedCyclePaths( paramsNode );
        logger.debug( "Retrieved the following cached cycle paths:\n  {}", new JoinString( "\n  ", cyclePaths ) );

        //        final IndexHits<Relationship> hits = graph.index()
        //                                                  .forRelationships( CYCLE_INJECTION_IDX )
        //                                                  .query( RELATIONSHIP_ID, "*" );
        //
        //        final Map<Node, Relationship> targetNodes = new HashMap<Node, Relationship>();
        //        for ( final Relationship hit : hits )
        //        {
        //            targetNodes.put( hit.getStartNode(), hit );
        //        }
        //
        //        final Set<Path> paths = getPathsTo( params, targetNodes.keySet() );

        final Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();
        for ( final CyclePath cyclicPath : cyclePaths )
        {
            final List<ProjectRelationship<?, ?>> cycle = new ArrayList<ProjectRelationship<?, ?>>( cyclicPath.length() + 1 );
            for ( final long id : cyclicPath.getRelationshipIds() )
            {
                ProjectRelationship<?, ?> rel = cache.getRelationship( id );
                if ( rel == null )
                {
                    final Relationship r = graph.getRelationshipById( id );
                    rel = toProjectRelationship( r, cache );
                }
                cycle.add( rel );
            }

            logger.debug( "[cache] CYCLES += {}", cycle );
            cycles.add( new EProjectCycle( cycle ) );
        }

        return cycles;
    }

    @Override
    public boolean isCycleParticipant( final ViewParams params, final ProjectRelationship<?, ?> rel )
    {
        for ( final EProjectCycle cycle : getCycles( params ) )
        {
            if ( cycle.contains( rel ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isCycleParticipant( final ViewParams params, final ProjectVersionRef ref )
    {
        for ( final EProjectCycle cycle : getCycles( params ) )
        {
            if ( cycle.contains( ref ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public void recomputeIncompleteSubgraphs()
    {
        // NOP, handled automatically.
    }

    @Override
    public Map<String, String> getMetadata( final ProjectVersionRef ref )
    {
        final Node node = getNode( ref );
        if ( node == null )
        {
            return null;
        }

        return getMetadataMap( node );
    }

    @Override
    public Map<String, String> getMetadata( final ProjectVersionRef ref, final Set<String> keys )
    {
        final Node node = getNode( ref );
        if ( node == null )
        {
            return null;
        }

        return getMetadataMap( node, keys );
    }

    @Override
    public void addMetadata( final ProjectVersionRef ref, final String key, final String value )
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Node node = getNode( ref );
            if ( node == null )
            {
                tx.failure();
                return;
            }

            Conversions.setMetadata( key, value, node );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public void setMetadata( final ProjectVersionRef ref, final Map<String, String> metadata )
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Node node = getNode( ref );
            if ( node == null )
            {
                tx.failure();
                return;
            }

            Conversions.setMetadata( metadata, node );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public ExecutionResult executeFrom( final String cypher, final ProjectVersionRef... roots )
        throws RelationshipGraphConnectionException
    {
        return executeFrom( cypher, null, roots );
    }

    @Override
    public ExecutionResult executeFrom( final String cypher, final Map<String, Object> params,
                                        final ProjectVersionRef... roots )
        throws RelationshipGraphConnectionException
    {
        if ( cypher.startsWith( "START" ) )
        {
            throw new RelationshipGraphConnectionException(
                                                            "Leave off the START clause when supplying ProjectVersionRef instances as query roots:\n'{}'",
                                                            cypher );
        }

        final StringBuilder sb = new StringBuilder();
        for ( final ProjectVersionRef root : roots )
        {
            final Node node = getNode( root );
            if ( node != null )
            {
                if ( sb.length() > 0 )
                {
                    sb.append( ", " );
                }
                sb.append( node.getId() );
            }
        }

        if ( sb.length() < 1 )
        {
            sb.append( "*" );
        }

        return execute( String.format( "START n=node(%s) %s", sb, cypher ), params );
    }

    @Override
    public ExecutionResult executeFrom( final String cypher, final ProjectRelationship<?, ?> rootRel )
        throws RelationshipGraphConnectionException
    {
        return executeFrom( cypher, null, rootRel );
    }

    @Override
    public ExecutionResult executeFrom( final String cypher, final Map<String, Object> params,
                                        final ProjectRelationship<?, ?> rootRel )
        throws RelationshipGraphConnectionException
    {
        if ( cypher.startsWith( "START" ) )
        {
            throw new RelationshipGraphConnectionException(
                                                            "Leave off the START clause when supplying ProjectRelationship instances as query roots:\n'{}'",
                                                            cypher );
        }

        String id = "*";
        if ( rootRel != null )
        {
            final Relationship r = getRelationship( rootRel );
            if ( r != null )
            {
                id = Long.toString( r.getId() );
            }
        }

        return execute( String.format( "START r=relationship(%s) %s", id, cypher ), params );
    }

    @Override
    public ExecutionResult execute( final String cypher )
    {
        return execute( cypher, null );
    }

    @Override
    public ExecutionResult execute( final String cypher, final Map<String, Object> params )
    {
        checkExecutionEngine();

        logger.debug( "Running query:\n\n{}\n\nWith params:\n\n{}\n\n", cypher, params );

        final String query = cypher.replaceAll( "(\\s)\\s+", "$1" );

        final ExecutionResult result =
            params == null ? queryEngine.execute( query ) : queryEngine.execute( query, params );

        //        logger.info( "Execution plan:\n{}", result.executionPlanDescription() );

        return result;
    }

    private void checkExecutionEngine()
    {
        if ( queryEngine == null )
        {
            queryEngine = new ExecutionEngine( graph );
        }
    }

    @Override
    public void reindex()
        throws RelationshipGraphConnectionException
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final IndexHits<Node> nodes = graph.index()
                                               .forNodes( BY_GAV_IDX )
                                               .query( GAV, "*" );
            for ( final Node node : nodes )
            {
                reindexNode( node );
            }

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public void reindex( final ProjectVersionRef ref )
    {
        final Node node = getNode( ref );
        if ( node == null )
        {
            return;
        }

        final Transaction tx = graph.beginTx();
        try
        {
            reindexNode( node );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    private void reindexNode( final Node node )
    {
        final String gav = getStringProperty( GAV, node );
        if ( gav == null )
        {
            return;
        }

        final Map<String, String> md = getMetadataMap( node );
        if ( md == null || md.isEmpty() )
        {
            return;
        }

        for ( final String key : md.keySet() )
        {
            graph.index()
                 .forNodes( METADATA_INDEX_PREFIX + key )
                 .add( node, GAV, gav );
        }
    }

    @Override
    public Set<ProjectVersionRef> getProjectsWithMetadata( final ViewParams params, final String key )
    {
        checkClosed();

        final IndexHits<Node> nodes = graph.index()
                                           .forNodes( METADATA_INDEX_PREFIX + key )
                                           .query( GAV, "*" );

        final ConversionCache cache = new ConversionCache();
        if ( registerView( params ) )
        {
            final Index<Node> cachedNodes = new ViewIndexes( graph.index(), params ).getCachedNodes();

            final Set<ProjectVersionRef> result = new HashSet<ProjectVersionRef>();
            for ( final Node node : nodes )
            {
                if ( cachedNodes.get( NID, node.getId() )
                                .hasNext() )
                {
                    result.add( toProjectVersionRef( node, cache ) );
                }
            }

            return result;
        }
        else
        {
            return new HashSet<ProjectVersionRef>( convertToProjects( nodes, cache ) );
        }
    }

    @Override
    public void addDisconnectedProject( final ProjectVersionRef ref )
    {
        if ( !containsProject( null, ref ) )
        {
            final Transaction tx = graph.beginTx();
            try
            {
                logger.debug( "Creating new node to account for disconnected project: {}", ref );
                newProjectNode( ref );

                tx.success();
            }
            finally
            {
                tx.finish();
            }
        }
    }

    @Deprecated
    @Override
    public Set<ProjectRelationship<?, ?>> getDirectRelationshipsFrom( final ViewParams params,
                                                                   final ProjectVersionRef from,
                                                                   final boolean includeManagedInfo,
                                                                   final RelationshipType... types )
    {
        return getDirectRelationshipsFrom( params, from, includeManagedInfo, true, types );
    }

    @Override
    public Set<ProjectRelationship<?, ?>> getDirectRelationshipsFrom( final ViewParams params,
                                                                   final ProjectVersionRef from,
                                                                   final boolean includeManagedInfo,
                                                                   final boolean includeConcreteInfo,
                                                                   final RelationshipType... types )
    {
        final Node node = getNode( from );
        if ( node == null )
        {
            return null;
        }

        final Set<GraphRelType> grts = new HashSet<GraphRelType>( types.length * 2 );
        for ( final RelationshipType relType : types )
        {
            if ( includeConcreteInfo )
            {
                grts.add( GraphRelType.map( relType, false ) );
            }

            if ( includeManagedInfo )
            {
                grts.add( GraphRelType.map( relType, true ) );
            }
        }

        final Iterable<Relationship> relationships =
            node.getRelationships( Direction.OUTGOING, grts.toArray( new GraphRelType[grts.size()] ) );

        if ( relationships != null )
        {
            final Set<ProjectRelationship<?, ?>> result = new HashSet<ProjectRelationship<?, ?>>();

            final ConversionCache cache = new ConversionCache();
            for ( final Relationship r : relationships )
            {
                if ( TraversalUtils.acceptedInView( r, params, cache ) )
                {
                    final ProjectRelationship<?, ?> rel = toProjectRelationship( r, cache );
                    if ( rel != null )
                    {
                        result.add( rel );
                    }
                }
            }

            return result;
        }

        return null;
    }

    @Override
    public Set<ProjectRelationship<?, ?>> getDirectRelationshipsTo( final ViewParams params, final ProjectVersionRef to,
                                                                 final boolean includeManagedInfo,
                                                                 final boolean includeConcreteInfo,
                                                                 final RelationshipType... types )
    {
        logger.debug( "Finding relationships targeting: {} (filter: {}, managed: {}, types: {})", to,
                      params.getFilter(), includeManagedInfo, Arrays.asList( types ) );
        final Node node = getNode( to );
        if ( node == null )
        {
            return null;
        }

        final Set<GraphRelType> grts = new HashSet<GraphRelType>( types.length * 2 );
        for ( final RelationshipType relType : types )
        {
            if ( includeConcreteInfo )
            {
                final GraphRelType graphType = GraphRelType.map( relType, false );
                if ( graphType != null )
                {
                    grts.add( graphType );
                }
            }

            if ( includeManagedInfo )
            {
                final GraphRelType graphType = GraphRelType.map( relType, true );
                if ( graphType != null )
                {
                    grts.add( graphType );
                }
            }
        }

        logger.debug( "Using graph-relationship types: {}", grts );

        final Iterable<Relationship> relationships =
            node.getRelationships( Direction.INCOMING, grts.toArray( new GraphRelType[grts.size()] ) );

        final ConversionCache cache = new ConversionCache();
        if ( relationships != null )
        {
            final Set<ProjectRelationship<?, ?>> result = new HashSet<ProjectRelationship<?, ?>>();
            for ( final Relationship r : relationships )
            {
                logger.debug( "Examining relationship: {}", r );
                if ( TraversalUtils.acceptedInView( r, params, cache ) )
                {
                    final ProjectRelationship<?, ?> rel = toProjectRelationship( r, cache );
                    if ( rel != null )
                    {
                        result.add( rel );
                    }
                }
            }

            return result;
        }

        return null;
    }

    @Override
    public Set<ProjectVersionRef> getProjectsMatching( final ViewParams params, final ProjectRef projectRef )
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( BY_GA_IDX )
                                          .query( GA, projectRef.asProjectRef()
                                                                .toString() );
        return new HashSet<ProjectVersionRef>( convertToProjects( hits, new ConversionCache() ) );
    }

    @Override
    public void deleteRelationshipsDeclaredBy( final ProjectVersionRef ref )
        throws RelationshipGraphConnectionException
    {
        checkClosed();

        if ( ref == null )
        {
            return;
        }

        final Index<Node> index = graph.index()
                                       .forNodes( BY_GAV_IDX );

        final String gav = ref.asProjectVersionRef()
                              .toString();

        final IndexHits<Node> hits = index.get( GAV, gav );

        if ( hits.hasNext() )
        {
            final Transaction tx = graph.beginTx();
            try
            {
                final Node node = hits.next();
                final Iterable<Relationship> relationships = node.getRelationships( Direction.OUTGOING );
                if ( relationships != null )
                {
                    for ( final Relationship r : relationships )
                    {
                        r.delete();
                    }
                }

                graph.index()
                     .forNodes( MISSING_NODES_IDX )
                     .add( node, GAV, gav );

                tx.success();
            }
            finally
            {
                tx.finish();
            }
        }
    }

    @Override
    public ProjectVersionRef getManagedTargetFor( final ProjectVersionRef target, final GraphPath<?> path,
                                                  final RelationshipType type )
    {
        if ( path == null )
        {
            return null;
        }

        if ( !( path instanceof Neo4jGraphPath ) )
        {
            throw new IllegalArgumentException( "GraphPath instances must be of type Neo4jGraphPath. Was: "
                + path.getClass()
                      .getName() );
        }

        final RelationshipIndex idx = graph.index()
                                           .forRelationships( MANAGED_GA );

        //        logger.info( "Searching for managed override of: {} in: {}", target, path );
        final Neo4jGraphPath neopath = (Neo4jGraphPath) path;

        final ConversionCache cache = new ConversionCache();
        for ( final Long id : neopath )
        {
            final Relationship r = graph.getRelationshipById( id );

            final String mkey =
                String.format( MKEY_FORMAT, r.getStartNode()
                                             .getId(), type.name(), target.getGroupId(), target.getArtifactId() );

            //            logger.info( "Searching for m-key: {}", mkey );

            final IndexHits<Relationship> hits = idx.get( MANAGED_KEY, mkey );
            if ( hits != null && hits.hasNext() )
            {
                final Relationship hit = hits.next();
                final ProjectVersionRef ref = toProjectVersionRef( hit.getEndNode(), cache );

                logger.debug( "[MUTATION] {} => {} (via: {})", target, ref, new RelToString( hit, cache ) );

                return ref;
            }

            //            final Node node = graph.getNodeById( id );
            //            final Iterable<Relationship> relationships = node.getRelationships( Direction.OUTGOING, GraphRelType.map( type, true ) );
            //            if ( relationships != null )
            //            {
            //                for ( final Relationship r : relationships )
            //                {
            //                    if ( r.hasProperty( GROUP_ID ) && r.getProperty( GROUP_ID )
            //                                                       .equals( target.getGroupId() ) && r.hasProperty( ARTIFACT_ID )
            //                        && r.getProperty( ARTIFACT_ID )
            //                            .equals( target.getArtifactId() ) )
            //                    {
            //                        return toProjectVersionRef( r.getEndNode() );
            //                    }
            //                }
            //            }
        }

        return null;
    }

    @Override
    public List<ProjectVersionRef> getPathRefs( final ViewParams params, final GraphPath<?> path )
    {
        if ( path != null && !( path instanceof Neo4jGraphPath ) )
        {
            throw new IllegalArgumentException( "Cannot get refs for: " + path
                + ". This is not a Neo4jGraphPathKey instance!" );
        }

        final ConversionCache cache = new ConversionCache();

        final Neo4jGraphPath gp = (Neo4jGraphPath) path;
        final List<ProjectRelationship<?, ?>> rels = convertToRelationships( gp, adminAccess, cache );
        final List<ProjectVersionRef> refs = new ArrayList<ProjectVersionRef>( rels.size() + 2 );
        for ( final ProjectRelationship<?, ?> rel : rels )
        {
            if ( refs.isEmpty() )
            {
                refs.add( rel.getDeclaring() );
            }

            refs.add( rel.getTarget()
                         .asProjectVersionRef() );
        }

        if ( refs.isEmpty() )
        {
            final Node node = graph.getNodeById( gp.getStartNodeId() );
            final ProjectVersionRef ref = toProjectVersionRef( node, cache );
            if ( ref != null )
            {
                refs.add( ref );
            }
        }

        return refs;
    }

    @Override
    public GraphPath<?> createPath( final ProjectRelationship<?, ?>... rels )
    {
        if ( rels.length < 1 )
        {
            return null;
        }

        final Relationship[] rs = new Relationship[rels.length];
        for ( int i = 0; i < rels.length; i++ )
        {
            final Relationship r = getRelationship( rels[i] );
            if ( r == null )
            {
                return null;
            }

            rs[i] = r;
        }

        return new Neo4jGraphPath( rs );
    }

    @Override
    public GraphPath<?> createPath( final GraphPath<?> parent, final ProjectRelationship<?, ?> rel )
    {
        if ( parent != null && !( parent instanceof Neo4jGraphPath ) )
        {
            throw new IllegalArgumentException( "Cannot get child path-key for: " + parent
                + ". This is not a Neo4jGraphPathKey instance!" );
        }

        Relationship r = getRelationship( rel );
        if ( r == null )
        {
            final Transaction tx = graph.beginTx();
            try
            {
                logger.debug( "Creating new node to account for missing project referenced in path: {}", r );

                addRelationshipsInternal( rel );

                // FIXME: Restore cycle detection somehow...
                //                    if ( rejected != null && !rejected.isEmpty() )
                //                    {
                //                        tx.failure();
                //                        throw new IllegalArgumentException( "Cannot create missing relationship for: " + rel + ". It creates a relationship cycle." );
                //                    }

                r = getRelationship( rel );

                tx.success();
                if ( r == null )
                {
                    return null;
                }
            }
            finally
            {
                tx.finish();
            }
        }

        return new Neo4jGraphPath( (Neo4jGraphPath) parent, r );
    }

    @Override
    public boolean registerView( final ViewParams params )
    {
        checkClosed();

        if ( params == null )
        {
            return false;
        }

        if ( params.getRoots() == null || params.getRoots()
                                                .isEmpty() )
        {
            logger.info( "Cannot track membership in params! It has no root GAVs.\nView: {} (short id: {})",
                         params.getLongId(), params.getShortId() );
            return false;
        }

        updateView( params, new ConversionCache() );

        return true;
    }

    private Node getViewNode( final ViewParams params )
    {
        //        if ( params.equals( globalView ) )
        //        {
        //            return configNode;
        //        }

        final Index<Node> confIdx = graph.index()
                                         .forNodes( CONFIG_NODES_IDX );

        final IndexHits<Node> hits = confIdx.get( VIEW_ID, params.getShortId() );
        if ( hits.hasNext() )
        {
            logger.debug( "View already registered: {} (short id: {})", params.getLongId(), params.getShortId() );
            return hits.next();
        }
        else
        {
            logger.debug( "Registering new params: {} (short id: {})", params.getLongId(), params.getShortId() );

            final Transaction tx = graph.beginTx();
            try
            {
                final Node paramsNode;
                paramsNode = graph.createNode();
                Conversions.storeView( params, paramsNode );

                confIdx.add( paramsNode, VIEW_ID, params.getShortId() );

                logger.debug( "Setting cycle-detection PENDING for new paramsNode: {} of: {}", paramsNode,
                              params.getShortId() );
                Conversions.setCycleDetectionPending( paramsNode, true );
                Conversions.setMembershipDetectionPending( paramsNode, true );

                final ViewIndexes indexes = new ViewIndexes( graph.index(), params );
                final Index<Node> cachedNodes = indexes.getCachedNodes();

                for ( final ProjectVersionRef rootRef : params.getRoots() )
                {
                    Node rootNode = getNode( rootRef );
                    if ( rootNode == null )
                    {
                        logger.info( "Creating node for root: {}", rootRef );
                        rootNode = newProjectNode( rootRef );
                    }

                    cachedNodes.add( rootNode, NID, rootNode.getId() );
                }

                tx.success();

                return paramsNode;
            }
            finally
            {
                tx.finish();
            }
        }
    }

    @Override
    public void registerViewSelection( final ViewParams params, final ProjectRef ref,
                                       final ProjectVersionRef projectVersionRef )
    {
        checkClosed();
        if ( !registerView( params ) )
        {
            return;
        }

        IndexHits<Node> nodeHits;
        if ( ref instanceof ProjectVersionRef )
        {
            nodeHits = graph.index()
                            .forNodes( BY_GAV_IDX )
                            .get( GAV, ( (ProjectVersionRef) ref ).asProjectVersionRef()
                                                                  .toString() );
        }
        else
        {
            nodeHits = graph.index()
                            .forNodes( BY_GA_IDX )
                            .get( GA, ref.asProjectRef() );
        }

        final Set<Long> viaNodes = new HashSet<Long>();
        for ( final Node node : nodeHits )
        {
            viaNodes.add( node.getId() );
        }

        logger.debug( "Searching for sub-paths to de-select (via): {}", viaNodes );

        final Set<Node> toUncacheNode = new HashSet<Node>();
        final Set<Relationship> toUncache = new HashSet<Relationship>();
        final Set<Relationship> toUnselect = new HashSet<Relationship>();

        final Transaction tx = graph.beginTx();
        try
        {
            final SubPathsCollectingVisitor visitor = new SubPathsCollectingVisitor( viaNodes, adminAccess );
            collectAtlasRelationships( params, visitor, getRoots( params ), false, Uniqueness.RELATIONSHIP_GLOBAL );

            for ( final Neo4jGraphPath path : visitor )
            {
                boolean uncache = false;
                for ( final Long id : path )
                {
                    final Relationship r = graph.getRelationshipById( id );

                    // first relationship in the sub-path.
                    if ( !uncache )
                    {
                        logger.debug( "Uncaching subgraph: {}", r.getEndNode() );
                        logger.debug( "Uncaching: {}", r );
                        toUncache.add( r );
                        uncache = true;
                    }
                    else
                    {
                        logger.debug( "Uncaching: {}", r );
                        toUncacheNode.add( r.getStartNode() );
                        toUncacheNode.add( r.getEndNode() );
                        toUncache.add( r );
                    }
                }
            }

            final ViewIndexes indexes = new ViewIndexes( graph.index(), params );
            final Index<Node> nodes = indexes.getCachedNodes();
            for ( final Node uncache : toUncacheNode )
            {
                logger.debug( "Uncache: {}", uncache );
                nodes.remove( uncache );
            }

            final RelationshipIndex rels = indexes.getCachedRelationships();
            for ( final Relationship uncache : toUncache )
            {
                logger.debug( "Uncache: {}", uncache );
                rels.remove( uncache );
            }

            final Node paramsNode = getViewNode( params );
            for ( final Relationship unsel : toUnselect )
            {
                Conversions.removeSelectionByTarget( unsel.getId(), paramsNode );
            }

            Conversions.setMembershipDetectionPending( paramsNode, true );
            Conversions.setCycleDetectionPending( paramsNode, true );

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    private void updateCaches( final Map<Long, ProjectRelationship<?, ?>> newRelationships )
    {
        if ( newRelationships.isEmpty() )
        {
            return;
        }

        final Index<Node> confIdx = graph.index()
                                         .forNodes( CONFIG_NODES_IDX );

        final IndexHits<Node> hits = confIdx.query( VIEW_ID, "*" );

        final ConversionCache cache = new ConversionCache();

        Transaction tx = graph.beginTx();
        try
        {
            logger.debug( "Setting global cycle-detection as PENDING" );
            Conversions.setCycleDetectionPending( configNode, true );

            tx.success();
        }
        finally
        {
            tx.finish();
        }

        for ( final Node paramsNode : hits )
        {
            final ViewParams params = Conversions.retrieveView( paramsNode, cache, adminAccess );
            logger.debug( "Updating params: {} ({})", params.getShortId(), paramsNode );

            //            if ( params == null || params.getShortId()
            //                                         .equals( globalView.getShortId() ) || params.getRoots() == null
            //                || params.getRoots()
            //                                                                                                          .isEmpty() )
            //            {
            //                logger.debug( "nevermind; it's the global params." );
            //                continue;
            //            }

            if ( getRoots( params, false ).isEmpty() )
            {
                tx = graph.beginTx();
                try
                {
                    Conversions.setCycleDetectionPending( paramsNode, true );
                    //                    Conversions.setMembershipDetectionPending( paramsNode, true );
                    tx.success();
                    continue;
                }
                finally
                {
                    tx.finish();
                }
            }

            final ViewIndexes vi = new ViewIndexes( graph.index(), params );
            final ViewUpdater vu = new ViewUpdater( params, paramsNode, vi, cache, adminAccess );
            vu.cacheRoots( getRoots( params, false ) );
            if ( vu.processAddedRelationships( newRelationships ) )
            {
                logger.debug( "{} ({}) marked for update.", params.getShortId(), paramsNode );
            }
            else
            {
                logger.debug( "{} ({}) NOT marked for update.", params.getShortId(), paramsNode );
            }
        }
    }

    private static class GraphAdminImpl
        implements GraphAdmin
    {

        private final FileNeo4JGraphConnection driver;

        GraphAdminImpl( final FileNeo4JGraphConnection driver )
        {
            this.driver = driver;
        }

        @Override
        public FileNeo4JGraphConnection getDriver()
        {
            return driver;
        }

        @Override
        public Relationship getRelationship( final long rid )
        {
            return driver.graph.getRelationshipById( rid );
        }

        @Override
        public Relationship select( final Relationship r, final ViewParams params, final Node paramsNode,
                                    final GraphPathInfo paramsPathInfo, final Neo4jGraphPath paramsPath )
        {
            return driver.select( r, params, paramsNode, paramsPathInfo, paramsPath );
        }

        @Override
        public RelationshipIndex getRelationshipIndex( final String name )
        {
            return driver.graph.index()
                               .forRelationships( name );
        }

        @Override
        public Index<Node> getNodeIndex( final String name )
        {
            return driver.graph.index()
                               .forNodes( name );
        }

        @Override
        public Transaction beginTransaction()
        {
            return driver.graph.beginTx();
        }

        @Override
        public boolean isSelection( final Relationship r, final Node paramsNode )
        {
            return Conversions.getDeselectionTarget( r.getId(), paramsNode ) > -1;
        }

    }

    @Override
    public String getWorkspaceId()
    {
        return workspaceId;
    }

    @Override
    public void addProjectError( final ProjectVersionRef ref, final String error )
        throws RelationshipGraphConnectionException
    {
        final Transaction tx = graph.beginTx();
        try
        {
            Node node = getNode( ref );
            if ( node == null )
            {
                node = newProjectNode( ref );
            }
            
            Conversions.storeError( node, error );
            
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public String getProjectError( final ProjectVersionRef ref )
    {
        final Node node = getNode( ref );
        if ( node == null )
        {
            return null;
        }

        return Conversions.getError( node );
    }

    @Override
    public boolean hasProjectError( final ProjectVersionRef ref )
    {
        final Node node = getNode( ref );
        if ( node == null )
        {
            return false;
        }

        return node.hasProperty( Conversions.PROJECT_ERROR );
    }

    @Override
    public void clearProjectError( final ProjectVersionRef ref )
        throws RelationshipGraphConnectionException
    {
        final Node node = getNode( ref );
        if ( node == null || !node.hasProperty( Conversions.PROJECT_ERROR ) )
        {
            return;
        }

        node.removeProperty( Conversions.PROJECT_ERROR );
    }

    @Override
    public Set<ProjectRelationship<?, ?>> getDirectRelationshipsTo( final ViewParams params, final ProjectVersionRef to,
                                                                 final boolean includeManagedInfo,
                                                                 final RelationshipType... types )
    {
        return getDirectRelationshipsFrom( params, to, includeManagedInfo, true, types );
    }

    public synchronized boolean isOpen()
    {
        return !closed;
    }

}
