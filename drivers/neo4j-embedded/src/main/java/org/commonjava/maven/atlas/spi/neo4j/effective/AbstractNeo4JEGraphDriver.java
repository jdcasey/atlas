/*******************************************************************************
 * Copyright (C) 2013 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.atlas.spi.neo4j.effective;

import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.atlas.spi.neo4j.effective.traverse.TraversalUtils.getGraphRelTypes;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.GA;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.GAV;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.RELATIONSHIP_ID;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.SOURCE_URI;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.addToURIListProperty;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.cloneRelationshipProperties;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.convertToProjects;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.convertToRelationships;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.getInjectedCycles;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.getMetadataMap;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.getSelections;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.getStringProperty;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.id;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.isCloneFor;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.isConnected;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.markConnected;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.markCycleInjection;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.markDeselected;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.markSelection;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.markSelectionOnly;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.removeSelectionAnnotationsFor;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.setMetadata;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.toNodeProperties;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.toProjectRelationship;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.toProjectVersionRef;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.toRelationshipProperties;
import static org.commonjava.maven.atlas.spi.neo4j.io.Conversions.toSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.effective.EProjectCycle;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.GraphView;
import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.traverse.AbstractFilteringTraversal;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;
import org.apache.maven.graph.effective.traverse.TraversalType;
import org.apache.maven.graph.effective.workspace.GraphWorkspace;
import org.apache.maven.graph.effective.workspace.GraphWorkspaceConfiguration;
import org.apache.maven.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.spi.neo4j.effective.traverse.AtlasCollector;
import org.commonjava.maven.atlas.spi.neo4j.effective.traverse.ConnectingPathsCollector;
import org.commonjava.maven.atlas.spi.neo4j.effective.traverse.EndNodesCollector;
import org.commonjava.maven.atlas.spi.neo4j.effective.traverse.MembershipWrappedTraversalEvaluator;
import org.commonjava.maven.atlas.spi.neo4j.effective.traverse.RootedNodesCollector;
import org.commonjava.maven.atlas.spi.neo4j.effective.traverse.RootedRelationshipsCollector;
import org.commonjava.maven.atlas.spi.neo4j.effective.traverse.TraversalUtils;
import org.commonjava.maven.atlas.spi.neo4j.io.Conversions;
import org.commonjava.util.logging.Logger;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

public abstract class AbstractNeo4JEGraphDriver
    implements Runnable, Neo4JEGraphDriver
{

    private final Logger logger = new Logger( getClass() );

    private static final String ALL_RELATIONSHIPS = "all_relationships";

    private static final String BY_GAV_IDX = "by_gav";

    private static final String BY_GA_IDX = "by_ga";

    private static final String CYCLE_INJECTION_IDX = "cycle_injections";

    private static final String VARIABLE_NODES_IDX = "variable_nodes";

    private static final String MISSING_NODES_IDX = "missing_nodes";

    private static final String METADATA_INDEX_PREFIX = "has_metadata_";

    //    private static final String GRAPH_ATLAS_TYPES_CLAUSE = join( GraphRelType.atlasRelationshipTypes(), "|" );

    /* @formatter:off */
//    private static final String CYPHER_SELECTION_RETRIEVAL = String.format(
//        "CYPHER 1.8 START a=node({roots}) " 
//            + "\nMATCH p1=(a)-[:%s*1..]->(s), " 
//            + "\n    p2=(a)-[:%s*1..]->(v) "
//            + "\nWITH v, s, last(relationships(p1)) as r1, last(relationships(p2)) as r2 "
//            + "\nWHERE v.%s = s.%s "
//            + "\n    AND v.%s = s.%s "
//            + "\n    AND has(r1.%s) "
//            + "\n    AND any(x in r1.%s "
//            + "\n        WHERE x IN {roots}) "
//            + "\n    AND has(r2.%s) "
//            + "\n    AND any(x in r2.%s "
//            + "\n          WHERE x IN {roots}) "
//            + "\nRETURN r1,r2,v,s",
//        GRAPH_ATLAS_TYPES_CLAUSE, GRAPH_ATLAS_TYPES_CLAUSE, 
//        Conversions.GROUP_ID, Conversions.GROUP_ID, 
//        Conversions.ARTIFACT_ID, Conversions.ARTIFACT_ID, 
//        Conversions.SELECTED_FOR, Conversions.SELECTED_FOR, 
//        Conversions.DESELECTED_FOR, Conversions.DESELECTED_FOR
//    );
    
    /* @formatter:on */

    private GraphDatabaseService graph;

    private final boolean useShutdownHook;

    private ExecutionEngine queryEngine;

    protected AbstractNeo4JEGraphDriver( final GraphDatabaseService graph, final boolean useShutdownHook )
    {
        this.graph = graph;
        this.useShutdownHook = useShutdownHook;

        printGraphStats();

        if ( useShutdownHook )
        {
            Runtime.getRuntime()
                   .addShutdownHook( new Thread( this ) );
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

    private void printGraphStats()
    {
        final Logger logger = new Logger( getClass() );
        logger.info( "Loaded approximately %d nodes.", graph.index()
                                                            .forNodes( BY_GAV_IDX )
                                                            .query( GAV, "*" )
                                                            .size() );

        logger.info( "Loaded approximately %d relationships.", graph.index()
                                                                    .forRelationships( ALL_RELATIONSHIPS )
                                                                    .query( RELATIONSHIP_ID, "*" )
                                                                    .size() );
    }

    @Override
    public Collection<? extends ProjectRelationship<?>> getRelationshipsDeclaredBy( final GraphView view,
                                                                                    final ProjectVersionRef ref )
    {
        checkClosed();

        if ( ref == null )
        {
            return null;
        }

        final Index<Node> index = graph.index()
                                       .forNodes( BY_GAV_IDX );
        final IndexHits<Node> hits = index.get( GAV, ref.toString() );

        if ( hits.hasNext() )
        {
            final Node node = hits.next();
            final Iterable<Relationship> relationships = node.getRelationships( Direction.OUTGOING );
            return convertToRelationships( relationships );
        }

        return null;
    }

    private void checkClosed()
    {
        if ( graph == null )
        {
            throw new IllegalStateException( "Graph database has been closed!" );
        }
    }

    @Override
    public Collection<? extends ProjectRelationship<?>> getRelationshipsTargeting( final GraphView view,
                                                                                   final ProjectVersionRef ref )
    {
        checkClosed();

        final Index<Node> index = graph.index()
                                       .forNodes( BY_GAV_IDX );
        final IndexHits<Node> hits = index.get( GAV, ref.toString() );

        if ( hits.hasNext() )
        {
            final Node node = hits.next();
            final Iterable<Relationship> relationships = node.getRelationships( Direction.INCOMING );
            return convertToRelationships( relationships );
        }

        return null;
    }

    @Override
    public Collection<ProjectRelationship<?>> getAllRelationships( final GraphView view )
    {
        final Set<Node> roots = getRoots( view );
        if ( roots != null && !roots.isEmpty() )
        {
            final Set<ProjectRelationship<?>> rels = new HashSet<ProjectRelationship<?>>();

            final RootedRelationshipsCollector checker = new RootedRelationshipsCollector( roots, view, false );

            collectAtlasRelationships( view, checker, roots );

            for ( final Relationship r : checker )
            {
                rels.add( toProjectRelationship( r ) );
            }

            for ( final ProjectRelationship<?> rel : new HashSet<ProjectRelationship<?>>( rels ) )
            {
                logger.debug( "Checking for self-referential parent: %s", rel );
                if ( rel.getType() == RelationshipType.PARENT && rel.getDeclaring()
                                                                    .equals( rel.getTarget()
                                                                                .asProjectVersionRef() ) )
                {
                    logger.debug( "Removing self-referential parent: %s", rel );
                    rels.remove( rel );
                }
            }

            logger.debug( "returning %d relationships: %s", rels.size(), rels );
            return rels;
        }
        else
        {
            final IndexHits<Relationship> hits = graph.index()
                                                      .forRelationships( ALL_RELATIONSHIPS )
                                                      .query( RELATIONSHIP_ID, "*" );
            return convertToRelationships( hits );
        }
    }

    @Override
    public Set<List<ProjectRelationship<?>>> getAllPathsTo( final GraphView view, final ProjectVersionRef... refs )
    {
        // NOTE: using global lookup here to avoid checking for paths, which we're going to collect below.
        final Set<Node> nodes = new HashSet<Node>( refs.length );
        for ( final ProjectVersionRef ref : refs )
        {
            final Node n = getNode( ref );
            if ( n != null )
            {
                nodes.add( n );
            }
        }

        if ( nodes.isEmpty() )
        {
            return null;
        }

        final Set<Node> roots = getRoots( view );
        final ConnectingPathsCollector checker = new ConnectingPathsCollector( roots, nodes, view, false );

        collectAtlasRelationships( view, checker, roots );

        final Set<Path> paths = checker.getFoundPaths();
        final Set<List<ProjectRelationship<?>>> result = new HashSet<List<ProjectRelationship<?>>>();
        for ( final Path path : paths )
        {
            result.add( convertToRelationships( path.relationships() ) );
        }

        return result;
    }

    @Override
    public Set<ProjectRelationship<?>> addRelationships( final ProjectRelationship<?>... rels )
    {
        checkClosed();

        Transaction tx = graph.beginTx();
        final Set<ProjectRelationship<?>> skipped = new HashSet<ProjectRelationship<?>>();

        try
        {
            for ( final ProjectRelationship<?> rel : rels )
            {
                logger.debug( "Adding relationship: %s", rel );

                final Index<Node> index = graph.index()
                                               .forNodes( BY_GAV_IDX );

                final ProjectVersionRef declaring = rel.getDeclaring();
                final ProjectVersionRef target = rel.getTarget()
                                                    .asProjectVersionRef();

                final long[] ids = new long[2];
                int i = 0;
                for ( final ProjectVersionRef ref : new ProjectVersionRef[] { declaring, target } )
                {
                    final IndexHits<Node> hits = index.get( GAV, ref.toString() );
                    if ( !hits.hasNext() )
                    {
                        final Node node = newProjectNode( ref );
                        logger.debug( "Created project node: %s with id: %d", ref, node.getId() );
                        ids[i] = node.getId();
                    }
                    else
                    {
                        ids[i] = hits.next()
                                     .getId();

                        logger.debug( "Using existing project node: %s", ids[i] );
                    }

                    i++;
                }

                final RelationshipIndex relIdx = graph.index()
                                                      .forRelationships( ALL_RELATIONSHIPS );

                final String relId = id( rel );
                final IndexHits<Relationship> relHits = relIdx.get( RELATIONSHIP_ID, relId );
                if ( relHits.size() < 1 )
                {
                    final Node from = graph.getNodeById( ids[0] );

                    if ( ids[0] != ids[1] )
                    {
                        final Node to = graph.getNodeById( ids[1] );

                        logger.debug( "Creating graph relationship for: %s between node: %d and node: %d", rel, ids[0],
                                      ids[1] );

                        final GraphRelType grt = GraphRelType.map( rel.getType(), rel.isManaged() );

                        final Relationship relationship = from.createRelationshipTo( to, grt );

                        logger.debug( "New relationship is: %s with type: %s", relationship, grt );

                        toRelationshipProperties( rel, relationship );
                        relIdx.add( relationship, RELATIONSHIP_ID, relId );
                    }

                    graph.index()
                         .forNodes( MISSING_NODES_IDX )
                         .remove( from );
                    markConnected( from, true );
                }
                else
                {
                    final Relationship r = relHits.next();

                    addToURIListProperty( rel.getSources(), SOURCE_URI, r );
                    markSelectionOnly( r, false );
                }
            }

            logger.debug( "Committing graph transaction." );
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        tx = graph.beginTx();
        try
        {
            logger.debug( "Analyzing for new cycles..." );
            for ( final ProjectRelationship<?> rel : rels )
            {
                if ( skipped.contains( rel ) )
                {
                    logger.debug( "SKIP: %s", rel );
                    continue;
                }

                final Relationship r = getRelationship( rel );
                if ( r == null || markCycle( rel, r ) )
                {
                    logger.debug( "Relationship %s not stored, or cycle introduced by: %s", r, rel );
                    skipped.add( rel );
                }
            }

            tx.success();
        }
        finally
        {
            tx.finish();
        }

        return skipped;
    }

    public boolean markCycle( final ProjectRelationship<?> rel, final Relationship relationship )
    {
        //        if ( roots == null || roots.isEmpty() )
        //        {
        //            logger.info( "NOT marking cycles for global graph, where there are no root nodes." );
        //            return false;
        //        }

        final Set<Path> cycles = getIntroducedCycles( GraphView.GLOBAL, rel );
        if ( cycles != null && !cycles.isEmpty() )
        {
            markCycleInjection( relationship, cycles );

            final String relId = id( rel );
            graph.index()
                 .forRelationships( CYCLE_INJECTION_IDX )
                 .add( relationship, RELATIONSHIP_ID, relId );

            return true;
        }

        return false;
    }

    @Override
    public boolean introducesCycle( final GraphView view, final ProjectRelationship<?> rel )
    {
        return !getIntroducedCycles( view, rel ).isEmpty();
    }

    private Set<Path> getIntroducedCycles( final GraphView view, final ProjectRelationship<?> rel )
    {
        //        logger.info( "\n\n\n\nCHECKING FOR CYCLES INTRODUCED BY: %s\n\n\n\n", rel );

        final Node from = getNode( rel.getDeclaring() );
        final Node to = getNode( rel.getTarget()
                                    .asProjectVersionRef() );
        if ( from == null || to == null )
        {
            return Collections.emptySet();
        }

        final ConnectingPathsCollector checker = new ConnectingPathsCollector( to, from, view, false );
        collectAtlasRelationships( view, checker, Collections.singleton( to ) );

        return checker.getFoundPaths();
        //
        //
        //        final Map<String, Object> params = new HashMap<String, Object>();
        //        params.put( "to", to.getId() );
        //        //        params.put( "from", from.getId() );
        //
        //        //        ExecutionResult result = execute( CYPHER_CYCLE_DETECTION_EXISTING, params );
        //        //        for ( final Map<String, Object> record : result )
        //        //        {
        //        //            final Path p = (Path) record.get( "path" );
        //        //            final Set<Long> cycle = new HashSet<Long>();
        //        //            for ( final Relationship r : p.relationships() )
        //        //            {
        //        //                cycle.add( r.getId() );
        //        //            }
        //        //
        //        //            logger.warn( "\n\n\n\nCYCLE DETECTED!\n\nCycle: %s\n\n\n\n", convertToRelationships( p.relationships() ) );
        //        //            cycles.add( cycle );
        //        //        }
        //
        //        params.put( "from", from.getId() );
        //
        //        final ExecutionResult result = execute( CYPHER_CYCLE_DETECTION_NEW, params );
        //        for ( final Map<String, Object> record : result )
        //        {
        //            final Path p = (Path) record.get( "path" );
        //            final Set<Long> cycle = new LinkedHashSet<Long>();
        //            for ( final Relationship r : p.relationships() )
        //            {
        //                cycle.add( r.getId() );
        //            }
        //
        //            logger.warn( "\n\n\n\nCYCLE DETECTED!\n\nCycle: %s\n\n\n\n", convertToRelationships( p.relationships() ) );
        //            cycles.add( cycle );
        //        }
        //
        //        logger.info( "\n\n\n\n%d CYCLES via: %s\n\n\n\n", cycles.size(), rel );
        //        return cycles;
    }

    private Node newProjectNode( final ProjectVersionRef ref )
    {
        final Node node = graph.createNode();
        toNodeProperties( ref, node, false );

        final String gav = ref.toString();

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
            logger.info( "Adding %s to variable-nodes index.", ref );
            graph.index()
                 .forNodes( VARIABLE_NODES_IDX )
                 .add( node, GAV, gav );
        }

        return node;
    }

    @Override
    public Set<ProjectVersionRef> getAllProjects( final GraphView view )
    {
        final Set<Node> roots = getRoots( view );
        Iterable<Node> nodes = null;
        if ( roots != null && !roots.isEmpty() )
        {
            final RootedNodesCollector agg = new RootedNodesCollector( roots, view, false );
            collectAtlasRelationships( view, agg, roots );
            nodes = agg;
        }
        else
        {
            final IndexHits<Node> hits = graph.index()
                                              .forNodes( BY_GAV_IDX )
                                              .query( GAV, "*" );
            nodes = hits;
        }

        return new HashSet<ProjectVersionRef>( convertToProjects( nodes ) );
    }

    private Set<Node> getAllProjectNodes( final GraphView view )
    {
        final Set<Node> roots = getRoots( view );
        if ( roots != null && !roots.isEmpty() )
        {
            final RootedNodesCollector agg = new RootedNodesCollector( roots, view, false );
            collectAtlasRelationships( view, agg, roots );
            return agg.getFoundNodes();
        }
        else
        {
            final IndexHits<Node> hits = graph.index()
                                              .forNodes( BY_GAV_IDX )
                                              .query( GAV, "*" );
            return toSet( hits );
        }
    }

    @Override
    public void traverse( final GraphView view, final ProjectNetTraversal traversal, final EProjectNet net,
                          final ProjectVersionRef root )
        throws GraphDriverException
    {
        printCaller( "TRAVERSE" );

        final Node rootNode = getNode( root );
        if ( rootNode == null )
        {
            //            logger.debug( "Root node not found! (root: %s)", root );
            return;
        }

        final Set<GraphRelType> relTypes = getRelTypes( traversal );

        for ( int i = 0; i < traversal.getRequiredPasses(); i++ )
        {
            //            logger.debug( "PASS: %d", i );

            TraversalDescription description = Traversal.traversal( Uniqueness.RELATIONSHIP_GLOBAL )
                                                        .sort( new PathComparator() );

            for ( final GraphRelType grt : relTypes )
            {
                description.relationships( grt, Direction.OUTGOING );
            }

            if ( traversal.getType( i ) == TraversalType.breadth_first )
            {
                description = description.breadthFirst();
            }
            else
            {
                description = description.depthFirst();
            }

            //            logger.debug( "starting traverse of: %s", net );
            traversal.startTraverse( i, net );

            final Set<Long> rootIds = getRootIds( view );

            @SuppressWarnings( { "rawtypes", "unchecked" } )
            final MembershipWrappedTraversalEvaluator checker =
                new MembershipWrappedTraversalEvaluator( rootIds, traversal, i );

            description = description.expand( checker )
                                     .evaluator( checker );

            final Traverser traverser = description.traverse( rootNode );
            for ( final Path path : traverser )
            {
                //                logger.debug( "traversing: %s", path );
                final List<ProjectRelationship<?>> rels = convertToRelationships( path.relationships() );
                if ( rels.isEmpty() )
                {
                    //                    logger.debug( "Skipping path with 0 relationships..." );
                    continue;
                }

                //                logger.debug( "traversing path with: %d relationships...", rels.size() );
                final ProjectRelationship<?> rel = rels.remove( rels.size() - 1 );

                if ( traversal.traverseEdge( rel, rels, i ) )
                {
                    traversal.edgeTraversed( rel, rels, i );
                }
            }

            traversal.endTraverse( i, net );

            checker.printStats();
        }
    }

    private Set<GraphRelType> getRelTypes( final ProjectNetTraversal traversal )
    {
        final Set<GraphRelType> relTypes = new HashSet<GraphRelType>();
        if ( traversal instanceof AbstractFilteringTraversal )
        {
            final ProjectRelationshipFilter rootFilter = ( (AbstractFilteringTraversal) traversal ).getRootFilter();
            relTypes.addAll( getGraphRelTypes( rootFilter ) );
        }
        else
        {
            relTypes.addAll( Arrays.asList( GraphRelType.values() ) );
        }

        return relTypes;
    }

    private void printCaller( final String label )
    {
        //        logger.debug( "\n\n\n\n%s called from:\n\n%s\n\n\n\n", label, join( new Throwable().getStackTrace(), "\n" ) );
    }

    @Override
    public boolean containsProject( final GraphView view, final ProjectVersionRef ref )
    {
        return getNode( view, ref ) != null;
    }

    @Override
    public boolean containsRelationship( final GraphView view, final ProjectRelationship<?> rel )
    {
        return getRelationship( rel ) != null;
    }

    @Override
    public Node getNode( final ProjectVersionRef ref )
    {
        return getNode( null, ref );
    }

    public Node getNode( final GraphView view, final ProjectVersionRef ref )
    {
        checkClosed();

        final Index<Node> idx = graph.index()
                                     .forNodes( BY_GAV_IDX );

        final IndexHits<Node> hits = idx.get( GAV, ref.asProjectVersionRef()
                                                      .toString() );

        final Node node = hits.hasNext() ? hits.next() : null;

        //        logger.debug( "Query result for node: %s is: %s\nChecking for path to root(s): %s", ref, node,
        //                      join( roots, "|" ) );

        if ( view != null && !hasPathTo( view, node ) )
        {
            return null;
        }

        return node;
    }

    private boolean hasPathTo( final GraphView view, final Node node )
    {
        if ( node == null )
        {
            return false;
        }

        final GraphView v = view == null ? GraphView.GLOBAL : view;

        final Set<Node> roots = getRoots( v );
        if ( roots == null || roots.isEmpty() )
        {
            return true;
        }

        if ( roots.contains( node.getId() ) )
        {
            return true;
        }

        logger.debug( "Checking for path between roots: %s and target node: %s", join( roots, "," ), node.getId() );
        final EndNodesCollector checker = new EndNodesCollector( roots, Collections.singleton( node ), view, false );

        collectAtlasRelationships( v, checker, roots );
        return checker.hasFoundNodes();
    }

    @Override
    public Relationship getRelationship( final ProjectRelationship<?> rel )
    {
        return getRelationship( id( rel ) );
    }

    Relationship getRelationship( final String relId )
    {
        checkClosed();

        final RelationshipIndex idx = graph.index()
                                           .forRelationships( ALL_RELATIONSHIPS );

        final IndexHits<Relationship> hits = idx.get( RELATIONSHIP_ID, relId );

        return hits.hasNext() ? hits.next() : null;
    }

    @Override
    public synchronized void close()
        throws IOException
    {
        if ( graph != null )
        {
            try
            {
                graph.shutdown();
                graph = null;
            }
            catch ( final Exception e )
            {
                throw new IOException( "Failed to shutdown: " + e.getMessage(), e );
            }
        }
    }

    @Override
    public void run()
    {
        try
        {
            close();
        }
        catch ( final IOException e )
        {
            new Logger( getClass() ).error( "Failed to shutdown graph database. Reason: %s", e, e.getMessage() );
        }
    }

    @SuppressWarnings( "unused" )
    private boolean isMissing( final Node node )
    {
        return !isConnected( node );
    }

    @Override
    public boolean isMissing( final GraphView view, final ProjectVersionRef ref )
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( BY_GAV_IDX )
                                          .get( GAV, ref.toString() );

        if ( hits.size() > 0 )
        {
            return !isConnected( hits.next() );
        }

        return false;
    }

    @Override
    public boolean hasMissingProjects( final GraphView view )
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( MISSING_NODES_IDX )
                                          .query( GAV, "*" );

        return hasIndexedProjects( view, hits );
    }

    @Override
    public Set<ProjectVersionRef> getMissingProjects( final GraphView view )
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( MISSING_NODES_IDX )
                                          .query( GAV, "*" );

        return getIndexedProjects( view, hits );
        //        return getAllFlaggedProjects( CONNECTED, false );
    }

    private Set<ProjectVersionRef> getIndexedProjects( final GraphView view, final Iterable<Node> hits )
    {
        final Set<Node> nodes = toSet( hits );
        final Set<Node> roots = getRoots( view );
        final EndNodesCollector checker = new EndNodesCollector( roots, nodes, view, false );

        collectAtlasRelationships( view, checker, roots );

        final Set<Node> found = checker.getFoundNodes();
        //        logger.info( "Found %d nodes: %s", found.size(), found );

        final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>();
        for ( final Node node : found )
        {
            refs.add( toProjectVersionRef( node ) );
        }

        return refs;
    }

    private boolean hasIndexedProjects( final GraphView view, final Iterable<Node> hits )
    {
        final Set<Node> nodes = toSet( hits );
        final Set<Node> roots = getRoots( view );
        final EndNodesCollector checker = new EndNodesCollector( roots, nodes, view, true );

        collectAtlasRelationships( view, checker, roots );

        return checker.hasFoundNodes();
    }

    private Set<Node> getRoots( final GraphView view )
    {
        final Set<ProjectVersionRef> rootRefs = view.getRoots();
        if ( rootRefs == null || rootRefs.isEmpty() )
        {
            return null;
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

    private Set<Long> getRootIds( final GraphView view )
    {
        final Set<ProjectVersionRef> rootRefs = view.getRoots();
        if ( rootRefs == null || rootRefs.isEmpty() )
        {
            return null;
        }

        final Set<Long> ids = new HashSet<Long>( rootRefs.size() );
        for ( final ProjectVersionRef ref : rootRefs )
        {
            final Node n = getNode( view, ref );
            if ( n != null )
            {
                ids.add( n.getId() );
            }
        }

        return ids;
    }

    private void collectAtlasRelationships( final GraphView view, final AtlasCollector<?> checker, final Set<Node> from )
    {
        if ( from == null || from.isEmpty() )
        {
            throw new UnsupportedOperationException(
                                                     "Cannot collect atlas nodes/relationships via traversal without at least one 'from' node!" );
        }

        //        logger.info( "Traversing for aggregation using: %s from roots: %s", checker.getClass()
        //                                                                                   .getName(), from );

        TraversalDescription description = Traversal.traversal( Uniqueness.RELATIONSHIP_GLOBAL );
        //                                                    .sort( new PathComparator() );

        final Set<GraphRelType> relTypes = getGraphRelTypes( view.getFilter() );
        for ( final GraphRelType grt : relTypes )
        {
            description.relationships( grt, Direction.OUTGOING );
        }

        description = description.breadthFirst();

        description = description.expand( checker )
                                 .evaluator( checker );

        final Traverser traverser = description.traverse( from.toArray( new Node[] {} ) );
        for ( @SuppressWarnings( "unused" )
        final Path path : traverser )
        {
            //            logger.info( "Aggregating path: %s", path );
            // Don't need this, but we need to iterate the traverser.
        }
    }

    @Override
    public boolean hasVariableProjects( final GraphView view )
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( VARIABLE_NODES_IDX )
                                          .query( GAV, "*" );

        return hasIndexedProjects( view, hits );
    }

    @Override
    public Set<ProjectVersionRef> getVariableProjects( final GraphView view )
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( VARIABLE_NODES_IDX )
                                          .query( GAV, "*" );

        //        logger.info( "Getting variable projects" );
        return getIndexedProjects( view, hits );
        //        return getAllFlaggedProjects( VARIABLE, true );
    }

    @Override
    public boolean addCycle( final EProjectCycle cycle )
    {
        // NOP, auto-detected.
        return false;
    }

    @Override
    public Set<EProjectCycle> getCycles( final GraphView view )
    {
        printCaller( "GET-CYCLES" );

        final IndexHits<Relationship> hits = graph.index()
                                                  .forRelationships( CYCLE_INJECTION_IDX )
                                                  .query( RELATIONSHIP_ID, "*" );

        final Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();
        for ( final Relationship hit : hits )
        {
            if ( hasPathTo( view, hit.getStartNode() ) )
            {
                final Set<Set<Long>> cycleIds = getInjectedCycles( hit );
                nextCycle: for ( final Set<Long> cycle : cycleIds )
                {
                    final List<ProjectRelationship<?>> rels = new ArrayList<ProjectRelationship<?>>();
                    for ( final Long relId : cycle )
                    {
                        final Relationship r = graph.getRelationshipById( relId );
                        if ( r == null )
                        {
                            continue nextCycle;
                        }

                        rels.add( toProjectRelationship( r ) );
                    }

                    cycles.add( new EProjectCycle( rels ) );
                }
            }
        }

        if ( view.getFilter() != null )
        {
            nextCycle: for ( final Iterator<EProjectCycle> it = cycles.iterator(); it.hasNext(); )
            {
                final EProjectCycle eProjectCycle = it.next();
                ProjectRelationshipFilter f = view.getFilter();
                for ( final ProjectRelationship<?> rel : eProjectCycle )
                {
                    if ( !f.accept( rel ) )
                    {
                        it.remove();
                        continue nextCycle;
                    }

                    f = f.getChildFilter( rel );
                }
            }
        }

        return cycles;

        //        final Map<String, Object> params = new HashMap<String, Object>();
        //        params.put( "roots", ( roots == null || roots.isEmpty() ? "*" : roots ) );
        //
        //        final ExecutionResult result = execute( CYPHER_CYCLE_RETRIEVAL, params );
        //
        //        final Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();
        //        nextPath: for ( final Map<String, Object> record : result )
        //        {
        //            final Path p = (Path) record.get( "path" );
        //            final Node terminus = p.lastRelationship()
        //                                   .getEndNode();
        //
        //            final List<ProjectRelationship<?>> rels = new ArrayList<ProjectRelationship<?>>();
        //            boolean logging = false;
        //
        //            ProjectRelationshipFilter f = filter;
        //            for ( final Relationship r : p.relationships() )
        //            {
        //                if ( r.getStartNode()
        //                      .equals( terminus ) )
        //                {
        //                    logging = true;
        //                }
        //
        //                if ( logging )
        //                {
        //                    final ProjectRelationship<?> rel = toProjectRelationship( r );
        //                    if ( f != null )
        //                    {
        //                        if ( !f.accept( rel ) )
        //                        {
        //                            continue nextPath;
        //                        }
        //                        else
        //                        {
        //                            f = f.getChildFilter( rel );
        //                        }
        //                    }
        //
        //                    rels.add( rel );
        //                }
        //            }
        //
        //            if ( !rels.isEmpty() )
        //            {
        //                cycles.add( new EProjectCycle( rels ) );
        //            }
        //        }
        //
        //        return cycles;
    }

    @Override
    public boolean isCycleParticipant( final GraphView view, final ProjectRelationship<?> rel )
    {
        for ( final EProjectCycle cycle : getCycles( view ) )
        {
            if ( cycle.contains( rel ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isCycleParticipant( final GraphView view, final ProjectVersionRef ref )
    {
        for ( final EProjectCycle cycle : getCycles( view ) )
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

            setMetadata( key, value, node );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public void addMetadata( final ProjectVersionRef ref, final Map<String, String> metadata )
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

            setMetadata( metadata, node );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public ExecutionResult executeFrom( final String cypher, final ProjectVersionRef... roots )
        throws GraphDriverException
    {
        return executeFrom( cypher, null, roots );
    }

    @Override
    public ExecutionResult executeFrom( final String cypher, final Map<String, Object> params,
                                        final ProjectVersionRef... roots )
        throws GraphDriverException
    {
        if ( cypher.startsWith( "START" ) )
        {
            throw new GraphDriverException(
                                            "Leave off the START clause when supplying ProjectVersionRef instances as query roots:\n'%s'",
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
    public ExecutionResult executeFrom( final String cypher, final ProjectRelationship<?> rootRel )
        throws GraphDriverException
    {
        return executeFrom( cypher, null, rootRel );
    }

    @Override
    public ExecutionResult executeFrom( final String cypher, final Map<String, Object> params,
                                        final ProjectRelationship<?> rootRel )
        throws GraphDriverException
    {
        if ( cypher.startsWith( "START" ) )
        {
            throw new GraphDriverException(
                                            "Leave off the START clause when supplying ProjectRelationship instances as query roots:\n'%s'",
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

        logger.debug( "Running query:\n\n%s\n\nWith params:\n\n%s\n\n", cypher, params );

        final String query = cypher.replaceAll( "(\\s)\\s+", "$1" );

        final ExecutionResult result =
            params == null ? queryEngine.execute( query ) : queryEngine.execute( query, params );

        //        logger.info( "Execution plan:\n%s", result.executionPlanDescription() );

        return result;
    }

    private synchronized void checkExecutionEngine()
    {
        if ( queryEngine == null )
        {
            queryEngine = new ExecutionEngine( graph );
        }
    }

    @Override
    public void reindex()
        throws GraphDriverException
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Iterable<Node> nodes = getAllProjectNodes( GraphView.GLOBAL );
            for ( final Node node : nodes )
            {
                final String gav = getStringProperty( GAV, node );
                if ( gav == null )
                {
                    continue;
                }

                final Map<String, String> md = getMetadataMap( node );
                if ( md == null || md.isEmpty() )
                {
                    continue;
                }

                for ( final String key : md.keySet() )
                {
                    graph.index()
                         .forNodes( METADATA_INDEX_PREFIX + key )
                         .add( node, GAV, gav );
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
    public Set<ProjectVersionRef> getProjectsWithMetadata( final GraphView view, final String key )
    {
        final IndexHits<Node> nodes = graph.index()
                                           .forNodes( METADATA_INDEX_PREFIX + key )
                                           .query( GAV, "*" );

        final Set<Node> connected = new HashSet<Node>();
        for ( final Node node : nodes )
        {
            // TODO: What about disconnected nodes discovered as part of the graph??
            if ( hasPathTo( view, node ) )
            {
                connected.add( node );
            }
        }

        return new HashSet<ProjectVersionRef>( convertToProjects( connected ) );
    }

    @Override
    public void selectVersionFor( final ProjectVersionRef variable, final SingleVersion select, final String sessionId )
    {
        logger.debug( "\n\n\n\nSELECT: %s for: %s\n\n\n\n", select, variable );
        if ( !select.isConcrete() )
        {
            logger.warn( "Cannot select non-concrete version! Attempted to select: %s", select );
            return;
        }

        //        if ( variable.isRelease() )
        //        {
        //            logger.warn( "Cannot select version if target is already a concrete version! Attempted to select for: %s",
        //                         variable );
        //
        //            return;
        //        }

        final Node node = getNode( variable );
        final Iterable<Relationship> rels = node.getRelationships( Direction.INCOMING );

        final long sid = getWorkspaceNodeId( sessionId );
        for ( final Relationship r : rels )
        {
            final ProjectRelationship<?> rel = toProjectRelationship( r );
            final ProjectRelationship<?> sel = rel.selectTarget( select );

            selectRelationship( sid, r, sel );
        }
    }

    private Relationship selectRelationship( final long sessionId, final Relationship from,
                                             final ProjectRelationship<?> toPR )
    {
        //        logger.info( "\n\n\n\nSELECT: %s\n\n\n\n", toPR );
        Relationship to = null;
        Transaction tx = null;
        try
        {
            final Node sessionNode = graph.getNodeById( sessionId );

            final RelationshipIndex relIdx = graph.index()
                                                  .forRelationships( ALL_RELATIONSHIPS );

            final String toId = id( toPR );

            Node fromNode = from.getStartNode();

            tx = graph.beginTx();

            //            logger.info( "\n\nLooking for relationship with id: %s", toId );
            final IndexHits<Relationship> hits = relIdx.get( RELATIONSHIP_ID, toId );
            if ( hits.size() < 1 )
            {
                Node toNode = getNode( toPR.getTarget()
                                           .asProjectVersionRef() );

                if ( toNode == null )
                {
                    toNode = newProjectNode( toPR.getTarget()
                                                 .asProjectVersionRef() );

                    //                    logger.info( "Created node %s for selected project version: %s", toNode, toPR.getTarget()
                    //                                                                                                 .asProjectVersionRef() );
                }

                //                logger.info( "\n\nCreating relationship for selected: %s from node: %s to node: %s\n\n", toPR,
                //                             fromNode, toNode );

                to = fromNode.createRelationshipTo( toNode, from.getType() );
                markSelectionOnly( to, true );

                cloneRelationshipProperties( from, to );
                relIdx.add( to, RELATIONSHIP_ID, toId );

                markConnected( fromNode, true );
            }
            else
            {
                to = hits.next();
                //                logger.info( "Got relationship: %s", to );
                fromNode = to.getStartNode();
            }

            markSelection( from, to, sessionNode );

            tx.success();
        }
        finally
        {
            if ( tx != null )
            {
                tx.finish();
            }
        }

        //        logger.info( "SELECTION DONE; returning: %s", to );

        return to;
    }

    @Override
    public boolean clearSelectedVersionsFor( final String sessionId )
    {
        Transaction tx = null;
        try
        {
            tx = graph.beginTx();

            final boolean result = clearSelectedVersions( getWorkspaceNodeId( sessionId ), tx );

            tx.success();

            return result;
        }
        finally
        {
            tx.finish();
        }
    }

    private boolean clearSelectedVersions( final long sessionId, final Transaction tx )
    {
        final Node node = graph.getNodeById( sessionId );
        if ( node == null )
        {
            return false;
        }

        final Map<Long, Long> selections = getSelections( node );

        final Set<Long> deleted = new HashSet<Long>();
        for ( final Entry<Long, Long> entry : selections.entrySet() )
        {
            final Long fromId = entry.getKey();
            final Relationship from = graph.getRelationshipById( fromId );

            final Long toId = entry.getValue();
            final Relationship to = graph.getRelationshipById( toId );

            logger.debug( "Clearing selection:\nSelected: %s\nVariable: %s", to.getEndNode()
                                                                               .getProperty( Conversions.GAV ),
                          from.getEndNode()
                              .getProperty( Conversions.GAV ) );

            if ( deleted.contains( toId ) || deleted.contains( fromId ) )
            {
                logger.debug( "Selected- or Variable-relationship already deleted:\n  selected: %s\n    deleted? %s\n  variable: %s\n    deleted? %s.\nContinuing to next mapping.",
                              to, deleted.contains( toId ), from, deleted.contains( fromId ) );
                continue;
            }

            // TODO: If something else starts making use of this relationship, we need to clear this flag!!
            if ( isCloneFor( to, from ) )
            {
                logger.debug( "Deleting cloned relationship from previous selection operation: %s", to );

                deleted.add( toId );
                to.delete();
            }

            removeSelectionAnnotationsFor( from, node );
            if ( !deleted.contains( toId ) )
            {
                logger.debug( "Removing selection annotations for previously selected: %s", to );

                markDeselected( to, node );
                //                        removeSelectionAnnotationsFor( info.sr, root );
            }
        }

        return true;
    }

    @Override
    public void addDisconnectedProject( final ProjectVersionRef ref )
    {
        if ( !containsProject( GraphView.GLOBAL, ref ) )
        {
            final Transaction tx = graph.beginTx();
            try
            {
                newProjectNode( ref );

                tx.success();
            }
            finally
            {
                tx.finish();
            }
        }
    }

    @Override
    public Set<ProjectRelationship<?>> getDirectRelationshipsFrom( final GraphView view, final ProjectVersionRef from,
                                                                   final boolean includeManagedInfo,
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
            grts.add( GraphRelType.map( relType, false ) );
            if ( includeManagedInfo )
            {
                grts.add( GraphRelType.map( relType, true ) );
            }
        }

        final Iterable<Relationship> relationships =
            node.getRelationships( Direction.OUTGOING, grts.toArray( new GraphRelType[grts.size()] ) );

        if ( relationships != null )
        {
            final Set<ProjectRelationship<?>> result = new HashSet<ProjectRelationship<?>>();
            for ( final Relationship r : relationships )
            {
                if ( TraversalUtils.acceptedInView( r, view ) )
                {
                    final ProjectRelationship<?> rel = toProjectRelationship( r );
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
    public Set<ProjectRelationship<?>> getDirectRelationshipsTo( final GraphView view, final ProjectVersionRef to,
                                                                 final boolean includeManagedInfo,
                                                                 final RelationshipType... types )
    {
        logger.debug( "Finding relationships targeting: %s (filter: %s, managed: %s, types: %s)", to, view.getFilter(),
                      includeManagedInfo, Arrays.asList( types ) );
        final Node node = getNode( to );
        if ( node == null )
        {
            return null;
        }

        final Set<GraphRelType> grts = new HashSet<GraphRelType>( types.length * 2 );
        for ( final RelationshipType relType : types )
        {
            grts.add( GraphRelType.map( relType, false ) );
            if ( includeManagedInfo )
            {
                grts.add( GraphRelType.map( relType, true ) );
            }
        }

        logger.debug( "Using graph-relationship types: %s", grts );

        final Iterable<Relationship> relationships =
            node.getRelationships( Direction.INCOMING, grts.toArray( new GraphRelType[grts.size()] ) );

        if ( relationships != null )
        {
            final Set<ProjectRelationship<?>> result = new HashSet<ProjectRelationship<?>>();
            for ( final Relationship r : relationships )
            {
                logger.debug( "Examining relationship: %s", r );
                if ( TraversalUtils.acceptedInView( r, view ) )
                {
                    final ProjectRelationship<?> rel = toProjectRelationship( r );
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
    public Set<ProjectVersionRef> getProjectsMatching( final ProjectRef projectRef, final GraphView eProjectNetView )
    {
        final IndexHits<Node> hits = graph.index()
                                          .forNodes( BY_GA_IDX )
                                          .query( GA, projectRef.toString() );
        return new HashSet<ProjectVersionRef>( convertToProjects( hits ) );
    }

    @Override
    public GraphWorkspace createWorkspace( final GraphWorkspaceConfiguration config )
        throws GraphDriverException
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Node wsNode = graph.createNode();
            final GraphWorkspace ws = new GraphWorkspace( Long.toString( wsNode.getId() ), config );

            Conversions.toNodeProperties( ws, wsNode );

            tx.success();

            return ws;
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public boolean deleteWorkspace( final String id )
    {
        Transaction tx = null;
        try
        {
            tx = graph.beginTx();

            final long nid = getWorkspaceNodeId( id );
            final Node wsNode = graph.getNodeById( nid );
            if ( wsNode == null )
            {
                return false;
            }

            if ( clearSelectedVersions( nid, tx ) )
            {
                wsNode.delete();
            }

            tx.success();
        }
        finally
        {
            tx.finish();
        }

        return true;
    }

    private long getWorkspaceNodeId( final String id )
    {
        return Long.parseLong( id );
    }

    @Override
    public void storeWorkspace( final GraphWorkspace workspace )
        throws GraphDriverException
    {
        final Transaction tx = graph.beginTx();
        try
        {
            final Node wsNode = graph.getNodeById( getWorkspaceNodeId( workspace.getId() ) );
            Conversions.toNodeProperties( workspace, wsNode );

            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    @Override
    public GraphWorkspace loadWorkspace( final String id )
        throws GraphDriverException
    {
        final long nid = getWorkspaceNodeId( id );
        final Node node = graph.getNodeById( nid );
        if ( node == null )
        {
            return null;
        }

        return Conversions.toWorkspace( node );
    }

}
