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
package org.commonjava.maven.atlas.graph;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships;
import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.traverse.RelationshipGraphTraversal;
import org.commonjava.maven.atlas.graph.traverse.TraversalType;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.*;

import static org.apache.commons.lang.StringUtils.join;

public final class RelationshipGraph
        implements Closeable
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String GROUP_ID = "groupId";

    private static final String ARTIFACT_ID = "artifactId";

    private static final String VERSION = "version";

    private List<RelationshipGraphListener> listeners;

    private final ViewParams params;

    private RelationshipGraphConnection connection;

    // if we didn't have a user, we wouldn't have constructed this thing!
    private int userCount = 1;

    RelationshipGraph( final ViewParams params, final RelationshipGraphConnection driver )
    {
        this.params = params;
        this.connection = driver;

        getConnectionInternal().registerView( params );
    }

    public ViewParams getParams()
    {
        return params;
    }

    public void storeProjectError( final ProjectVersionRef ref, final Throwable error )
            throws RelationshipGraphException
    {
        getConnectionInternal().addProjectError( ref, String.format( "%s\n%s", error.getMessage(),
                                                                     join( error.getStackTrace(), "\n  " ) ) );

        for ( final RelationshipGraphListener listener : listeners )
        {
            listener.projectError( this, ref, error );
        }
    }

    public String getProjectError( final ProjectVersionRef ref )
    {
        return getConnectionInternal().getProjectError( ref );
    }

    public boolean hasProjectError( final ProjectVersionRef ref )
    {
        return getConnectionInternal().hasProjectError( ref );
    }

    public void clearProjectError( final ProjectVersionRef ref )
            throws RelationshipGraphException
    {
        getConnectionInternal().clearProjectError( ref );
    }

    public Set<ProjectRelationship<?>> storeRelationships( final ProjectRelationship<?>... relationships )
            throws RelationshipGraphException
    {
        final List<ProjectRelationship<?>> rels = Arrays.asList( relationships );

        for ( final RelationshipGraphListener listener : listeners )
        {
            listener.storing( this, rels );
        }

        final Set<ProjectRelationship<?>> rejected = getConnectionInternal().addRelationships( relationships );

        for ( final RelationshipGraphListener listener : listeners )
        {
            listener.stored( this, rels, rejected );
        }

        return rejected;
    }

    public Set<ProjectRelationship<?>> storeRelationships(
            final Collection<? extends ProjectRelationship<?>> relationships )
            throws RelationshipGraphException
    {
        for ( final RelationshipGraphListener listener : listeners )
        {
            listener.storing( this, relationships );
        }

        final Set<ProjectRelationship<?>> rejected = getConnectionInternal().addRelationships(
                relationships.toArray( new ProjectRelationship<?>[relationships.size()] ) );

        for ( final RelationshipGraphListener listener : listeners )
        {
            listener.stored( this, relationships, rejected );
        }

        return rejected;
    }

    public void addListener( final RelationshipGraphListener listener )
    {
        if ( listeners == null )
        {
            listeners = new ArrayList<RelationshipGraphListener>();
        }

        listeners.add( listener );
    }

    public void removeListener( final RelationshipGraphListener listener )
    {
        if ( listeners != null )
        {
            listeners.remove( listener );
        }
    }

    synchronized void incrementGraphOwnership()
    {
        userCount++;
        logger.info( "User count incremented to: {} for: {}", userCount, params );
    }

    public synchronized void forceClose()
            throws RelationshipGraphException
    {
        logger.info( "Closing: {}", params );

        if ( listeners != null )
        {
            for ( final RelationshipGraphListener listener : listeners )
            {
                listener.closing( this );
            }
        }

        connection = null;

        if ( listeners != null )
        {
            for ( final RelationshipGraphListener listener : listeners )
            {
                listener.closed( this );
            }
        }
    }

    @Override
    public synchronized void close()
            throws IOException
    {
        userCount--;
        logger.info( "User count decremented to: {} for: {}", userCount, params );

        if ( userCount < 1 )
        {
            try
            {
                forceClose();
            }
            catch ( final RelationshipGraphException e )
            {
                throw new IOException( "Failed to close graph.", e );
            }
        }
        else
        {
            logger.info( "NOT closing; there are other users registered!" );
        }
    }

    // +++ IMPORTED FROM EProjectWeb...

    public Set<ProjectRelationship<?>> getAllRelationships()
    {
        return new HashSet<ProjectRelationship<?>>( getConnectionInternal().getAllRelationships( params ) );
    }

    public boolean isComplete()
    {
        return !getConnectionInternal().hasMissingProjects( params );
    }

    public boolean isConcrete()
    {
        return !getConnectionInternal().hasVariableProjects( params );
    }

    public Set<ProjectVersionRef> getIncompleteSubgraphs()
    {
        return Collections.unmodifiableSet( getConnectionInternal().getMissingProjects( params ) );
    }

    public Set<ProjectVersionRef> getVariableSubgraphs()
    {
        return Collections.unmodifiableSet( getConnectionInternal().getVariableProjects( params ) );
    }

    public Set<ProjectRelationship<?>> add( final EProjectDirectRelationships rels )
            throws RelationshipGraphException
    {
        return addAll( rels.getAllRelationships() );
    }

    public boolean add( final ProjectRelationship<?> rel )
            throws RelationshipGraphException
    {
        if ( rel == null )
        {
            return false;
        }

        return getConnectionInternal().addRelationships( rel ).isEmpty();
    }

    public <T extends ProjectRelationship<?>> Set<T> addAll( final Collection<T> rels )
            throws RelationshipGraphException
    {
        if ( rels == null )
        {
            return null;
        }

        final Set<T> result = new HashSet<T>( rels );

        final Set<ProjectRelationship<?>> rejected =
                getConnectionInternal().addRelationships( rels.toArray( new ProjectRelationship<?>[rels.size()] ) );
        result.removeAll( rejected );

        if ( !result.isEmpty() )
        {
            getConnectionInternal().recomputeIncompleteSubgraphs();
        }

        return result;
    }

    public <T extends ProjectRelationship<?>> Set<T> addAll( final T... rels )
            throws RelationshipGraphException
    {
        if ( rels == null )
        {
            return null;
        }

        final Set<T> result = new HashSet<T>();
        for ( final T rel : rels )
        {
            if ( add( rel ) )
            {
                result.add( rel );
            }
        }

        getConnectionInternal().recomputeIncompleteSubgraphs();

        return result;
    }

    public void traverse( final ProjectVersionRef start, final RelationshipGraphTraversal traversal,
                          final TraversalType type )
            throws RelationshipGraphException
    {
        getConnectionInternal().traverse( traversal, start, this, type );
    }

    public void traverse( final ProjectVersionRef start, final RelationshipGraphTraversal traversal )
            throws RelationshipGraphException
    {
        traverse( start, traversal, TraversalType.breadth_first );
    }

    public void traverse( final RelationshipGraphTraversal traversal, final TraversalType type )
            throws RelationshipGraphException
    {
        for ( final ProjectVersionRef root : params.getRoots() )
        {
            getConnectionInternal().traverse( traversal, root, this, type );
        }
    }

    public void traverse( final RelationshipGraphTraversal traversal )
            throws RelationshipGraphException
    {
        traverse( traversal, TraversalType.breadth_first );
    }

    public Set<ProjectRelationship<?>> getUserRelationships( final ProjectVersionRef ref )
    {
        if ( !getConnectionInternal().containsProject( params, ref ) )
        {
            return Collections.emptySet();
        }

        return new HashSet<ProjectRelationship<?>>( getConnectionInternal().getRelationshipsTargeting( params, ref ) );
    }

    public Set<ProjectRelationship<?>> getDirectRelationships( final ProjectVersionRef ref )
    {
        if ( !getConnectionInternal().containsProject( params, ref ) )
        {
            return Collections.emptySet();
        }

        return new HashSet<ProjectRelationship<?>>( getConnectionInternal().getRelationshipsDeclaredBy( params, ref ) );
    }

    public Set<ProjectVersionRef> getRoots()
    {
        return params.getRoots();
    }

    public Set<ProjectRelationship<?>> getExactAllRelationships()
    {
        return getAllRelationships();
    }

    public boolean isCycleParticipant( final ProjectVersionRef ref )
    {
        return getConnectionInternal().isCycleParticipant( params, ref );
    }

    public boolean isCycleParticipant( final ProjectRelationship<?> rel )
    {
        return getConnectionInternal().isCycleParticipant( params, rel );
    }

    public void addCycle( final EProjectCycle cycle )
            throws RelationshipGraphException
    {
        getConnectionInternal().addCycle( cycle );
    }

    public Set<EProjectCycle> getCycles()
    {
        return getConnectionInternal().getCycles( params );
    }

    public Set<ProjectRelationship<?>> getRelationshipsTargeting( final ProjectVersionRef ref )
    {
        final Collection<? extends ProjectRelationship<?>> rels =
                getConnectionInternal().getRelationshipsTargeting( params, ref.asProjectVersionRef() );
        if ( rels == null )
        {
            return Collections.emptySet();
        }

        return new HashSet<ProjectRelationship<?>>( rels );
    }

    public RelationshipGraphConnection getDatabase()
    {
        return connection;
    }

    public Set<ProjectVersionRef> getAllProjects()
    {
        return getConnectionInternal().getAllProjects( params );
    }

    public void addMetadata( final ProjectVersionRef ref, final String name, final String value )
            throws RelationshipGraphException
    {
        getConnectionInternal().addMetadata( ref, name, value );
    }

    public void addMetadata( final ProjectVersionRef ref, final Map<String, String> metadata )
            throws RelationshipGraphException
    {
        getConnectionInternal().setMetadata( ref, metadata );
    }

    public Set<ProjectVersionRef> getProjectsWithMetadata( final String key )
    {
        return getConnectionInternal().getProjectsWithMetadata( params, key );
    }

    public void reindex()
            throws RelationshipGraphException
    {
        getConnectionInternal().reindex();
    }

    public void reindex( final ProjectVersionRef ref )
            throws RelationshipGraphConnectionException
    {
        getConnectionInternal().reindex( ref );
    }

    public Set<List<ProjectRelationship<?>>> getPathsTo( final ProjectVersionRef... projectVersionRefs )
    {
        return getConnectionInternal().getAllPathsTo( params, projectVersionRefs );
    }

    public boolean introducesCycle( final ProjectRelationship<?> rel )
    {
        return getConnectionInternal().introducesCycle( params, rel );
    }

    public void addDisconnectedProject( final ProjectVersionRef ref )
            throws RelationshipGraphException
    {
        getConnectionInternal().addDisconnectedProject( ref );
    }

    public boolean isMissing( final ProjectVersionRef ref )
    {
        return getConnectionInternal().isMissing( params, ref );
    }

    public Set<URI> getSources()
    {
        return params.getActiveSources();
        //        final Set<ProjectRelationship<?>> rels = getAllRelationships();
        //        final Set<URI> sources = new HashSet<URI>();
        //        for ( final ProjectRelationship<?> rel : rels )
        //        {
        //            sources.addAll( rel.getSources() );
        //        }
        //
        //        return sources;
    }

    public GraphMutator getMutator()
    {
        return params.getMutator();
    }

    public ProjectRelationshipFilter getFilter()
    {
        return params.getFilter();
    }

    public ViewParams addActivePomLocation( final URI location )
    {
        return params.addActivePomLocation( location );
    }

    public ViewParams addActivePomLocations( final Collection<URI> locations )
    {
        return params.addActivePomLocations( locations );
    }

    public ViewParams addActivePomLocations( final URI... locations )
    {
        return params.addActivePomLocations( locations );
    }

    public ViewParams addActiveSources( final Collection<URI> sources )
    {
        return params.addActiveSources( sources );
    }

    public ViewParams addActiveSources( final URI... sources )
    {
        return params.addActiveSources( sources );
    }

    public ViewParams addActiveSource( final URI source )
    {
        return params.addActiveSource( source );
    }

    public long getLastAccess()
    {
        return params.getLastAccess();
    }

    public String setProperty( final String key, final String value )
    {
        return params.setProperty( key, value );
    }

    public String getProperty( final String key )
    {
        return params.getProperty( key );
    }

    public String getProperty( final String key, final String def )
    {
        return params.getProperty( key, def );
    }

    public final String getWorkspaceId()
    {
        return params.getWorkspaceId();
    }

    public final Set<URI> getActivePomLocations()
    {
        return params.getActivePomLocations();
    }

    public final Set<URI> getActiveSources()
    {
        return params.getActiveSources();
    }

    public final ProjectVersionRef getSelection( final ProjectRef ref )
    {
        return params.getSelection( ref );
    }

    // --- IMPORTED FROM EProjectWeb

    // +++ IMPORTED FROM EGraphManager
    public boolean containsGraph( final ProjectVersionRef ref )
    {
        return getConnectionInternal().containsProject( params, ref );
    }

    public Set<ProjectRelationship<?>> findDirectRelationshipsTo( final ProjectVersionRef to,
                                                                  final boolean includeManagedInfo,
                                                                  final RelationshipType... types )
    {
        return getConnectionInternal().getDirectRelationshipsTo( params, to, includeManagedInfo, true, types );
    }

    public Set<ProjectRelationship<?>> findDirectRelationshipsTo( final ProjectVersionRef to,
                                                                  final boolean includeManagedInfo,
                                                                  final boolean includeConcreteInfo,
                                                                  final RelationshipType... types )
    {
        return getConnectionInternal().getDirectRelationshipsTo( params, to, includeManagedInfo, includeConcreteInfo,
                                                                 types );
    }

    public Set<ProjectRelationship<?>> findDirectRelationshipsFrom( final ProjectVersionRef source,
                                                                    final boolean managed,
                                                                    final RelationshipType... types )
    {
        return getConnectionInternal().getDirectRelationshipsFrom( params, source, managed, true, types );
    }

    public Set<ProjectRelationship<?>> findDirectRelationshipsFrom( final ProjectVersionRef source,
                                                                    final boolean managed, final boolean concrete,
                                                                    final RelationshipType... types )
    {
        return getConnectionInternal().getDirectRelationshipsFrom( params, source, managed, concrete, types );
    }

    public Set<ProjectVersionRef> getAllIncompleteSubgraphs()
    {
        return getConnectionInternal().getMissingProjects( params );
    }

    public Set<ProjectVersionRef> getAllVariableSubgraphs()
    {
        return getConnectionInternal().getVariableProjects( params );
    }

    public Map<String, String> getMetadata( final ProjectVersionRef ref )
    {
        final Map<String, String> result = new HashMap<String, String>();
        final Map<String, String> metadata = getConnectionInternal().getMetadata( ref );
        if ( metadata != null )
        {
            result.putAll( metadata );
        }

        result.put( GROUP_ID, ref.getGroupId() );
        result.put( ARTIFACT_ID, ref.getArtifactId() );
        result.put( VERSION, ref.getVersionString() );

        return result;
    }

    public Map<String, String> getMetadata( final ProjectVersionRef ref, final Set<String> keys )
    {
        final Map<String, String> result = new HashMap<String, String>();
        final Map<String, String> metadata = getConnectionInternal().getMetadata( ref, keys );
        if ( metadata != null )
        {
            result.putAll( metadata );
        }

        if ( keys.contains( GROUP_ID ) )
        {
            result.put( GROUP_ID, ref.getGroupId() );
        }

        if ( keys.contains( ARTIFACT_ID ) )
        {
            result.put( ARTIFACT_ID, ref.getArtifactId() );
        }

        if ( keys.contains( VERSION ) )
        {
            result.put( VERSION, ref.getVersionString() );
        }

        return result;
    }

    public Map<Map<String, String>, Set<ProjectVersionRef>> collateByMetadata( final Set<ProjectVersionRef> refs,
                                                                               final Set<String> keys )
    {
        final Map<Map<String, String>, Set<ProjectVersionRef>> result =
                new HashMap<Map<String, String>, Set<ProjectVersionRef>>();
        for ( final ProjectVersionRef ref : refs )
        {
            final Map<String, String> metadata = getMetadata( ref, keys );
            Set<ProjectVersionRef> collated = result.get( metadata );
            if ( collated == null )
            {
                collated = new HashSet<ProjectVersionRef>();
                result.put( metadata, collated );
            }

            collated.add( ref );
        }

        return result;
    }

    public void setMetadata( final ProjectVersionRef project, final Map<String, String> metadata )
            throws RelationshipGraphException
    {
        getConnectionInternal().setMetadata( project, metadata );
    }

    public void deleteRelationshipsDeclaredBy( final ProjectVersionRef ref )
            throws RelationshipGraphException
    {
        getConnectionInternal().deleteRelationshipsDeclaredBy( ref );
    }

    public Set<ProjectVersionRef> getProjectsMatching( final ProjectRef projectRef )
    {
        return getConnectionInternal().getProjectsMatching( params, projectRef );
    }

    public Map<GraphPath<?>, GraphPathInfo> getPathMapTargeting( final Set<ProjectVersionRef> refs )
    {
        return getConnectionInternal().getPathMapTargeting( params, refs );
    }

    public ProjectVersionRef getPathTargetRef( final GraphPath<?> path )
    {
        return getConnectionInternal().getPathTargetRef( path );
    }

    public GraphPath<?> createPath( final GraphPath<?> parentPath, final ProjectRelationship<?> relationship )
    {
        return getConnectionInternal().createPath( parentPath, relationship );
    }

    public GraphPath<?> createPath( final ProjectRelationship<?>... relationships )
    {
        return getConnectionInternal().createPath( relationships );
    }

    public List<ProjectVersionRef> getPathRefs( final GraphPath<?> path )
    {
        return getConnectionInternal().getPathRefs( params, path );
    }

    RelationshipGraphConnection getConnection()
    {
        return connection;
    }

    // --- IMPORTED FROM EGraphManager

    @Override
    public String toString()
    {
        return "RelationshipGraph [params=" + params + "]";
    }

    public void printStats()
    {
        getConnectionInternal().printStats();
    }

    public Map<ProjectVersionRef, String> getAllProjectErrors()
    {
        final Map<ProjectVersionRef, String> errors = new HashMap<ProjectVersionRef, String>();
        final Set<ProjectVersionRef> projects = getConnectionInternal().getAllProjects( params );
        for ( final ProjectVersionRef ref : projects )
        {
            final String error = getConnectionInternal().getProjectError( ref );
            if ( error != null )
            {
                errors.put( ref, error );
            }
        }

        return errors;
    }

    public synchronized boolean isOpen()
    {
        return connection != null;
    }

    private synchronized RelationshipGraphConnection getConnectionInternal()
    {
        if ( connection == null )
        {
            throw new IllegalStateException( "Relationship graph has been closed!" );
        }

        return connection;
    }

    public Collection<? extends ProjectRelationship<?>> getRelationshipsDeclaring( final ProjectVersionRef root )
    {
        return getConnectionInternal().getRelationshipsDeclaredBy( params, root );
    }

}
