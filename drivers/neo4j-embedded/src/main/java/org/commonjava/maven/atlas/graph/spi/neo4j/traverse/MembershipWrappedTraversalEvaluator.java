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

import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toProjectRelationship;

import java.util.Collections;
import java.util.List;
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
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.commonjava.maven.atlas.graph.traverse.RelationshipGraphTraversal;
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

public class MembershipWrappedTraversalEvaluator<STATE>
    implements Evaluator, PathExpander<STATE>
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Set<Long> rootIds;

    private final RelationshipGraphTraversal traversal;

    private boolean reversedExpander;

    private final ViewParams view;

    private ConversionCache cache = new ConversionCache();

    private final GraphAdmin admin;

    private final Node viewNode;

    private final GraphRelType[] types;

    private final RelationshipGraphConnection connection;

    public MembershipWrappedTraversalEvaluator( final Set<Long> rootIds, final RelationshipGraphTraversal traversal,
                                                final RelationshipGraphConnection connection, final ViewParams view,
                                                final Node viewNode, final GraphAdmin admin,
                                                final GraphRelType... types )
    {
        this.rootIds = rootIds;
        this.traversal = traversal;
        this.connection = connection;
        this.view = view;
        this.viewNode = viewNode;
        this.admin = admin;
        this.types = types;
    }

    private MembershipWrappedTraversalEvaluator( final MembershipWrappedTraversalEvaluator<STATE> ev, final boolean reversedExpander )
    {
        this.rootIds = ev.rootIds;
        this.traversal = ev.traversal;
        this.connection = ev.connection;
        this.view = ev.view;
        this.admin = ev.admin;
        this.viewNode = ev.viewNode;
        this.types = ev.types;
        this.reversedExpander = reversedExpander;
    }

    public void setConversionCache( final ConversionCache cache )
    {
        this.cache = cache;
    }

    @Override
    public Evaluation evaluate( final Path path )
    {
        final Relationship rel = path.lastRelationship();
        if ( rel == null )
        {
            //            logger.info( "MISSING last-relationship: {}. exclude and continue", rel );
            return Evaluation.EXCLUDE_AND_CONTINUE;
        }

        final Set<Long> roots = rootIds;
        if ( roots == null || roots.isEmpty() || roots.contains( path.startNode()
                                                                     .getId() ) )
        {
            final ProjectRelationship<?, ?> lastRel = Conversions.toProjectRelationship( rel, cache );

            final List<ProjectRelationship<?, ?>> relPath = Conversions.convertToRelationships( path.relationships(), cache );
            if ( relPath.indexOf( lastRel ) == relPath.size() - 1 )
            {
                //                logger.warn( "\n\n\n\n\nREMOVING last-relationship: {} from path!\n\n\n\n\n" );
                relPath.remove( relPath.size() - 1 );
            }

            if ( traversal.preCheck( lastRel, relPath ) )
            {
                logger.debug( "Include-and-continue: {}, {}", relPath, lastRel );
                return Evaluation.INCLUDE_AND_CONTINUE;
            }
            else
            {
                logger.debug( "exclude-and-prune: {}, {}", relPath, lastRel );
            }
        }

        //        logger.info( "exclude and prune" );
        return Evaluation.EXCLUDE_AND_PRUNE;
    }

    @Override
    public Iterable<Relationship> expand( final Path path, final BranchState<STATE> state )
    {
        final Node node = path.endNode();
        //        logger.info( "START expansion for: {}", path );

        // TODO: Is node(0) appropriate to see??
        final Set<Long> roots = rootIds;
        if ( node.getId() != 0 && roots != null && roots.isEmpty() && !roots.contains( path.startNode()
                                                                                           .getId() ) )
        {
            //            logger.info( "{} not in membership. Skipping expansion.", node );
            return Collections.emptySet();
        }

        final Neo4jGraphPath graphPath = new Neo4jGraphPath( path );
        GraphPathInfo pathInfo = new GraphPathInfo( connection, view );
        for ( final Long rid : graphPath )
        {
            final Relationship r = admin.getRelationship( rid );
            pathInfo = pathInfo.getChildPathInfo( toProjectRelationship( r, cache ) );
        }

        GraphRelType[] childTypes = types;
        final ProjectRelationshipFilter filter = pathInfo.getFilter();
        if ( filter != null )
        {
            childTypes = TraversalUtils.getGraphRelTypes( filter );
        }

        final Iterable<Relationship> rs =
            node.getRelationships( reversedExpander ? Direction.INCOMING : Direction.OUTGOING, childTypes );

        if ( rs == null )
        {
            //            logger.info( "No relationships from end-node: {}", node );
            return Collections.emptySet();
        }

        // sort the child relationships to make the traversal deterministic
        final Set<Relationship> result = new TreeSet<Relationship>( new AtlasRelIndexComparator() );

        final List<ProjectRelationship<?, ?>> rels = getPathRelationships( path );

        //        logger.info( "For: {} Determining which of {} child relationships to expand traversal into for: {}\n{}", traversal.getClass()
        //                                                                                                                          .getName(), path.length(),
        //                     path.endNode()
        //                         .hasProperty( GAV ) ? path.endNode()
        //                                                   .getProperty( GAV ) : "Unknown", new JoinString( "\n  ", Thread.currentThread()
        //                                                                                                                  .getStackTrace() ) );

        for ( Relationship r : rs )
        {
            if ( Conversions.getBooleanProperty( Conversions.CYCLES_INJECTED, r, false ) )
            {
                continue;
            }

            final Relationship selected = admin.select( r, view, viewNode, pathInfo, graphPath );

            // if no selection happened and r is a selection-only relationship, skip it.
            if ( ( selected == null || selected == r ) && admin.isSelection( r, viewNode ) )
            {
                continue;
            }

            if ( selected != null )
            {
                r = selected;
            }
            //            logger.info( "Attempting to expand: {}", r );

            final ProjectRelationship<?, ?> projectRel = Conversions.toProjectRelationship( r, cache );

            logger.debug( "Pre-checking relationship {} for expansion using filter: {}", projectRel, traversal );
            if ( traversal.preCheck( projectRel, rels ) )
            {
                logger.debug( "Adding for expansion: {}", projectRel );
                result.add( r );
            }
            else
            {
                logger.debug( "Skipping for expansion: {}", projectRel );
            }
        }

        logger.debug( "Expanding for {} relationships.", result.size() );
        return result;
    }

    private List<ProjectRelationship<?, ?>> getPathRelationships( final Path path )
    {
        List<ProjectRelationship<?, ?>> rels;
        final Iterable<Relationship> rs = path.relationships();
        if ( rs == null )
        {
            //            logger.info( "Constructing empty relationship list for filter." );
            rels = Collections.emptyList();
        }
        else
        {
            rels = Conversions.convertToRelationships( rs, cache );
            //            logger.info( "Got relationship list {} entries long for filter", rels.size() );
        }

        if ( reversedExpander )
        {
            //            logger.info( "Reversing relationship list for filter." );
            Collections.reverse( rels );
        }

        return rels;
    }

    @Override
    public PathExpander<STATE> reverse()
    {
        return new MembershipWrappedTraversalEvaluator<STATE>( this, true );
    }

}
