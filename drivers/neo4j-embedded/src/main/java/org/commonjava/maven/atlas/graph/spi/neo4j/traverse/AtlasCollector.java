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
package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.GAV;
import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toProjectRelationship;
import static org.commonjava.maven.atlas.graph.spi.neo4j.traverse.TraversalUtils.accepted;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphAdmin;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphRelType;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.ConversionCache;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.CyclePath;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.NeoProjectVersionRef;
import org.commonjava.maven.atlas.graph.spi.neo4j.update.CycleCacheUpdater;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AtlasCollector<STATE>
    implements Evaluator, PathExpander<STATE>
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private Direction direction = Direction.OUTGOING;

    private final Set<Node> startNodes;

    private ViewParams view;

    private ConversionCache cache = new ConversionCache();

    private TraverseVisitor visitor;

    private GraphAdmin admin;

    private boolean useSelections = true;

    private Node viewNode;

    private GraphRelType[] types;

    private RelationshipGraphConnection connection;

    public AtlasCollector( final TraverseVisitor visitor, final Node start,
                           final RelationshipGraphConnection connection, final ViewParams view, final Node viewNode,
                           final GraphAdmin admin, final GraphRelType... types )
    {
        this( visitor, Collections.singleton( start ), connection, view, viewNode, admin );
        this.types = types;
    }

    public AtlasCollector( final TraverseVisitor visitor, final Set<Node> startNodes,
                           final RelationshipGraphConnection connection, final ViewParams view, final Node viewNode,
                           final GraphAdmin admin, final GraphRelType... types )
    {
        this.visitor = visitor;
        this.connection = connection;
        this.viewNode = viewNode;
        this.admin = admin;
        this.types = types;
        visitor.configure( this );

        this.startNodes = startNodes;

        this.view = view;
    }

    public AtlasCollector( final TraverseVisitor visitor, final Set<Node> startNodes,
                           final RelationshipGraphConnection connection, final ViewParams view, final Node viewNode,
                           final GraphAdmin admin, final GraphRelType[] types, final Direction direction )
    {
        this( visitor, startNodes, connection, view, viewNode, admin, types );
        this.direction = direction;
    }

    public void setUseSelections( final boolean useSelections )
    {
        this.useSelections = useSelections;
    }

    public void setConversionCache( final ConversionCache cache )
    {
        this.cache = cache;
    }

    @Override
    @SuppressWarnings( "rawtypes" )
    public final Iterable<Relationship> expand( final Path path, final BranchState state )
    {
        if ( !visitor.isEnabledFor( path ) )
        {
            logger.debug( "Disabled, NOT expanding: {}", path );
            return Collections.emptySet();
        }

        if ( !startNodes.isEmpty() )
        {
            final Node startNode = path.startNode();
            if ( !startNodes.contains( startNode ) )
            {
                logger.debug( "Rejecting path; it does not start with one of our roots:\n\t{}", path );
                return Collections.emptySet();
            }
        }

        final Neo4jGraphPath graphPath = new Neo4jGraphPath( path );

        GraphPathInfo pathInfo = new GraphPathInfo( connection, view );
        // if we're here, we're pre-cleared to blindly construct this pathInfo (see child iteration below)
        for ( final Long rid : graphPath )
        {
            final Relationship r = admin.getRelationship( rid );
            pathInfo = pathInfo.getChildPathInfo( toProjectRelationship( r, cache ) );
        }

        logger.debug( "For {}, using pathInfo: {}", graphPath, pathInfo );

        final CyclePath cyclePath = CycleCacheUpdater.getTerminatingCycle( path );
        if ( cyclePath != null )
        {
            final Relationship injector = path.lastRelationship();
            logger.debug( "Detected cycle in progress for path: {} at relationship: {}\n  Cycle path is: {}", path,
                          injector, cyclePath );

            visitor.cycleDetected( cyclePath, injector );
        }

        //        logger.debug( "Checking hasSeen for graphPath: {} with pathInfo: {} (actual path: {})", graphPath, pathInfo, path );
        //        if ( visitor.hasSeen( graphPath, pathInfo ) )
        //        {
        //            logger.debug( "Already seen: {} (path: {})", graphPath, path );
        //            return Collections.emptySet();
        //        }
        // split this so we register both the seen and the cycle.
        /*else*/if ( cyclePath != null )
        {
            return Collections.emptySet();
        }

        if ( returnChildren( path, graphPath, pathInfo ) )
        {

            //            final ProjectRelationshipFilter nextFilter = pathInfo.getFilter();
            //            log( "Implementation says return the children of: {}\n  lastRel={}\n  nextFilter={}\n\n",
            //                 path.endNode()
            //                     .hasProperty( GAV ) ? path.endNode()
            //                                               .getProperty( GAV ) : "Unknown", path.lastRelationship(), nextFilter );

            final Set<Relationship> nextRelationships = new TreeSet<Relationship>( new AtlasRelIndexComparator() );

            GraphRelType[] childTypes = types;
            final ProjectRelationshipFilter filter = pathInfo.getFilter();
            if ( filter != null )
            {
                childTypes = TraversalUtils.getGraphRelTypes( filter );
            }

            logger.debug( "Getting relationships from node: {} with type in [{}] and direction: {} (path: {})",
                          path.endNode(), new JoinString( ", ", childTypes ), direction, path );

            final Iterable<Relationship> relationships = path.endNode()
                                                             .getRelationships( direction, childTypes );

            //            logger.info( "{} Determining which of {} child relationships to expand traversal into for: {}\n{}", getClass().getName(), path.length(),
            //                         path.endNode()
            //                             .hasProperty( GAV ) ? path.endNode()
            //                                                       .getProperty( GAV ) : "Unknown", new JoinString( "\n  ", Thread.currentThread()
            //                                                                                                                      .getStackTrace() ) );

            for ( Relationship r : relationships )
            {
                logger.debug( "Analyzing child relationship for traversal potential: {}", r );

                if ( useSelections )
                {
                    final Relationship selected = admin.select( r, view, viewNode, pathInfo, graphPath );
                    if ( selected == null )
                    {
                        logger.debug( "selection failed for: {} at {}. Likely, this is filter rejection from: {}", r,
                                      graphPath, pathInfo );
                        continue;
                    }

                    // if no selection happened and r is a selection-only relationship, skip it.
                    if ( selected == r && admin.isSelection( r, viewNode ) )
                    {
                        logger.debug( "{} is NOT the result of selection, yet it is marked as a selection relationship. Path: {}",
                                      r, path );
                        continue;
                    }

                    if ( !accepted( selected, view, cache ) )
                    {
                        logger.debug( "{} NOT accepted, likely due to incompatible POM location or source URI. Path: {}",
                                      r, path );
                        continue;
                    }

                    if ( selected != null )
                    {
                        r = selected;
                    }

                    logger.debug( "After selection, using child relationship: {}", r );
                }

                final ProjectRelationship<?, ?> rel = toProjectRelationship( r, cache );

                final Neo4jGraphPath nextPath = new Neo4jGraphPath( graphPath, r );
                final GraphPathInfo nextPathInfo = pathInfo.getChildPathInfo( rel );

                logger.debug( "Including child: {} with next-path: {} and childPathInfo: {} from parent path: {}", r,
                              nextPath, nextPathInfo, path );
                visitor.includingChild( r, nextPath, nextPathInfo, path );

                logger.debug( "+= {}", wrap( r ) );
                nextRelationships.add( r );
            }

            return nextRelationships;
        }

        logger.debug( "children not being returned for: {}", path );
        return Collections.emptySet();
    }

    public boolean returnChildren( final Path path, final Neo4jGraphPath graphPath, final GraphPathInfo pathInfo )
    {
        // if there's a GraphPathInfo mapped for this path, then it was accepted during expansion.
        return visitor.includeChildren( path, graphPath, pathInfo );
    }

    private Object wrap( final Relationship r )
    {
        return new Object()
        {
            @Override
            public String toString()
            {
                return r + " " + String.valueOf( toProjectRelationship( r, cache ) );
            }
        };
    }

    @Override
    public final Evaluation evaluate( final Path path )
    {
        return Evaluation.INCLUDE_AND_CONTINUE;
    }

    @Override
    public PathExpander<STATE> reverse()
    {
        final AtlasCollector<STATE> collector =
            new AtlasCollector<STATE>( visitor, startNodes, connection, view, viewNode, admin, types,
                                       direction.reverse() );
        collector.setConversionCache( cache );
        collector.setUseSelections( useSelections );

        return collector;
    }
}
