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

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.maven.atlas.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.io.Serializable;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;

import static org.apache.commons.lang.StringUtils.join;

/**
 * <p>
 * View of a graph database that may include a set of traversal root-nodes, 
 * a {@link ProjectRelationshipFilter}, and a {@link GraphMutator}. This view
 * also supports selection of a GAV to override either all references to a GA, or
 * references to a specific GAV. 
 * </p>
 * 
 * <p>
 * <b>NOTE(1):</b> Selections currently cannot be overridden or 
 * cleared once set, and root GAVs cannot be overridden with a selection.
 * </p>
 * 
 * <p>
 * <b>NOTE(2):</b> If root nodes are unspecified, then neither filter nor mutator can be used!
 * </p>
 * 
 * @author jdcasey
 */
public class ViewParams
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    private static final Set<URI> DEFAULT_SOURCES =
        Collections.unmodifiableSet( Collections.singleton( RelationshipUtils.UNKNOWN_SOURCE_URI ) );

    private static final Set<URI> DEFAULT_POM_LOCATIONS =
        Collections.unmodifiableSet( Collections.singleton( RelationshipUtils.POM_ROOT_URI ) );

    private final Set<ProjectVersionRef> roots = new HashSet<ProjectVersionRef>();

    private final ProjectRelationshipFilter filter;

    private final GraphMutator mutator;

    private Set<URI> activePomLocations;

    private Set<URI> activeSources;

    private long lastAccess = System.currentTimeMillis();

    private Map<String, String> properties;

    private final Map<ProjectRef, ProjectVersionRef> selections;

    private transient String longId;

    private transient String shortId;

    private final String workspaceId;

    public ViewParams( final String workspaceId, final ProjectRelationshipFilter filter, final GraphMutator mutator,
                       final Collection<ProjectVersionRef> roots )
    {
        this( workspaceId, filter, mutator, null, roots );
    }

    public ViewParams( final String workspaceId, final ProjectRelationshipFilter filter, final GraphMutator mutator,
                       final ProjectVersionRef... roots )
    {
        this( workspaceId, filter, mutator, null, roots );
    }

    public ViewParams( final String workspaceId, final Collection<ProjectVersionRef> roots )
    {
        this( workspaceId, null, null, null, roots );
    }

    public ViewParams( final String workspaceId, final ProjectVersionRef... roots )
    {
        this( workspaceId, null, null, null, roots );
    }

    public ViewParams( final String workspaceId, final ProjectRelationshipFilter filter, final GraphMutator mutator,
                       final Map<ProjectRef, ProjectVersionRef> selections, final Collection<ProjectVersionRef> roots )
    {
        this.workspaceId = workspaceId;
        this.filter = filter;
        this.selections = selections;
        this.roots.addAll( roots );
        this.mutator = mutator;
    }

    public ViewParams( final String workspaceId, final ProjectRelationshipFilter filter, final GraphMutator mutator,
                       final Map<ProjectRef, ProjectVersionRef> selections, final ProjectVersionRef... roots )
    {
        this.workspaceId = workspaceId;
        this.filter = filter;
        this.selections = selections;
        this.roots.addAll( Arrays.asList( roots ) );
        this.mutator = mutator;
    }

    public ViewParams( final String workspaceId, final Map<ProjectRef, ProjectVersionRef> selections,
                       final Collection<ProjectVersionRef> roots )
    {
        this( workspaceId, null, null, selections, roots );
    }

    public ViewParams( final String workspaceId, final Map<ProjectRef, ProjectVersionRef> selections,
                       final ProjectVersionRef... roots )
    {
        this( workspaceId, null, null, selections, roots );
    }

    public ViewParams( final String workspaceId, final ProjectRelationshipFilter filter, final GraphMutator mutator,
                       final Set<URI> activePomLocations, final Set<URI> activeSources,
                       final Map<ProjectRef, ProjectVersionRef> selections, final Map<String, String> properties,
                       final Collection<ProjectVersionRef> roots )
    {
        this.workspaceId = workspaceId;
        this.filter = filter;
        this.mutator = mutator;
        this.activePomLocations = activePomLocations;
        this.activeSources = activeSources;
        this.selections = selections;
        this.properties = properties;
        this.roots.addAll( roots );
    }

    public GraphMutator getMutator()
    {
        return mutator == null ? new ManagedDependencyMutator() : mutator;
    }

    public ProjectRelationshipFilter getFilter()
    {
        return filter == null ? AnyFilter.INSTANCE : filter;
    }

    public Set<ProjectVersionRef> getRoots()
    {
        return roots;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( filter == null ) ? 0 : filter.hashCode() );
        result = prime * result + ( ( roots == null ) ? 0 : roots.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final ViewParams other = (ViewParams) obj;
        return getShortId().equals( other.getShortId() );
    }

    @Override
    public String toString()
    {
        return getLongId() + " (shortId: " + getShortId() + ")";
    }

    public String getLongId()
    {
        if ( longId == null )
        {
            final StringBuilder sb = new StringBuilder();
            final String abbreviatedPackage = getClass().getPackage()
                                                        .getName()
                                                        .replaceAll( "([a-zA-Z])[a-zA-Z]+", "$1" );

            sb.append( abbreviatedPackage )
              .append( '.' )
              .append( getClass().getSimpleName() )
              .append( "(roots:" )
              .append( join( roots, "," ) )
              .append( ",filter:" )
              .append( filter == null ? "none" : filter.getLongId() )
              .append( ",mutator:" )
              .append( mutator == null ? "none" : mutator.getLongId() )
              .append( ",selections:\n  " )
              .append( renderSelections() );

            sb.append( ")" );

            longId = sb.toString();
        }

        return longId;
    }

    public String getShortId()
    {
        if ( shortId == null )
        {
            shortId = DigestUtils.shaHex( getLongId() );
        }

        return shortId;
    }

    public void touch()
    {
        lastAccess = System.currentTimeMillis();
    }

    public ViewParams addActivePomLocation( final URI location )
    {
        if ( activePomLocations == null )
        {
            activePomLocations = new HashSet<URI>();
        }

        activePomLocations.add( location );
        return this;
    }

    public ViewParams addActivePomLocations( final Collection<URI> locations )
    {
        if ( activePomLocations == null )
        {
            activePomLocations = new HashSet<URI>();
        }

        activePomLocations.addAll( locations );
        return this;
    }

    public ViewParams addActivePomLocations( final URI... locations )
    {
        if ( activePomLocations == null )
        {
            activePomLocations = new HashSet<URI>();
        }

        activePomLocations.addAll( Arrays.asList( locations ) );
        return this;
    }

    public ViewParams addActiveSources( final Collection<URI> sources )
    {
        if ( activeSources == null )
        {
            activeSources = new HashSet<URI>();
        }

        activeSources.addAll( sources );
        return this;
    }

    public ViewParams addActiveSources( final URI... sources )
    {
        if ( activeSources == null )
        {
            activeSources = new HashSet<URI>();
        }

        activeSources.addAll( Arrays.asList( sources ) );
        return this;
    }

    public ViewParams addActiveSource( final URI source )
    {
        if ( activeSources == null )
        {
            activeSources = new HashSet<URI>();
        }

        activeSources.add( source );
        return this;
    }

    public long getLastAccess()
    {
        return lastAccess;
    }

    public String setProperty( final String key, final String value )
    {
        if ( properties == null )
        {
            properties = new HashMap<String, String>();
        }

        return properties.put( key, value );
    }

    public String removeProperty( final String key )
    {
        return properties == null ? null : properties.remove( key );
    }

    public String getProperty( final String key )
    {
        return properties == null ? null : properties.get( key );
    }

    public String getProperty( final String key, final String def )
    {
        final String val = properties == null ? null : properties.get( key );
        return val == null ? def : val;
    }

    public final String getWorkspaceId()
    {
        return workspaceId;
    }

    public final Set<URI> getActivePomLocations()
    {
        return activePomLocations == null ? DEFAULT_POM_LOCATIONS : activePomLocations;
    }

    public final Set<URI> getActiveSources()
    {
        return activeSources == null ? DEFAULT_SOURCES : activeSources;
    }

    public final Iterable<URI> activePomLocations()
    {
        return activePomLocations;
    }

    public final Iterable<URI> activeSources()
    {
        return activeSources;
    }

    public final Map<ProjectRef, ProjectVersionRef> getSelections()
    {
        return selections == null ? Collections.<ProjectRef, ProjectVersionRef> emptyMap() : selections;
    }

    public final ProjectVersionRef getSelection( final ProjectRef ref )
    {
        if ( selections == null )
        {
            return null;
        }

        ProjectVersionRef selected = selections.get( ref );
        if ( selected == null )
        {
            selected = selections.get( ref.asProjectRef() );
        }

        return selected;
    }

    public boolean hasSelection( final ProjectVersionRef ref )
    {
        return selections != null && ( selections.containsKey( ref ) || selections.containsKey( ref.asProjectRef() ) );
    }

    public String renderSelections()
    {
        if ( selections == null )
        {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        for ( final Entry<ProjectRef, ProjectVersionRef> entry : selections.entrySet() )
        {
            final ProjectRef key = entry.getKey();
            final ProjectVersionRef value = entry.getValue();

            sb.append( "\n  " )
              .append( key )
              .append( " => " )
              .append( value );
        }

        return sb.toString();
    }

    public boolean hasSelections()
    {
        return selections != null && !selections.isEmpty();
    }

    public static final class Builder
    {
        private final Set<ProjectVersionRef> roots;

        private ProjectRelationshipFilter filter;

        private GraphMutator mutator;

        private final Set<URI> activePomLocations;

        private final Set<URI> activeSources;

        private final Map<String, String> properties;

        private final Map<ProjectRef, ProjectVersionRef> selections;

        private final String workspaceId;

        public Builder( final String workspaceId, final ProjectVersionRef... roots )
        {
            this.workspaceId = workspaceId;
            this.roots = new HashSet<ProjectVersionRef>( Arrays.asList( roots ) );
            this.activePomLocations = new HashSet<URI>();
            this.activeSources = new HashSet<URI>();
            this.properties = new HashMap<String, String>();
            this.selections = new HashMap<ProjectRef, ProjectVersionRef>();
            this.filter = AnyFilter.INSTANCE;
            this.mutator = new ManagedDependencyMutator();
        }

        public Builder( final String workspaceId, final Collection<ProjectVersionRef> roots )
        {
            this.workspaceId = workspaceId;
            this.roots = new HashSet<ProjectVersionRef>( roots );
            this.activePomLocations = new HashSet<URI>();
            this.activeSources = new HashSet<URI>();
            this.properties = new HashMap<String, String>();
            this.selections = new HashMap<ProjectRef, ProjectVersionRef>();
            this.filter = AnyFilter.INSTANCE;
            this.mutator = new ManagedDependencyMutator();
        }

        public Builder( final ViewParams params )
        {
            this.workspaceId = params.getWorkspaceId();
            this.roots =
                params.roots == null ? new HashSet<ProjectVersionRef>() : new HashSet<ProjectVersionRef>( params.roots );
            this.filter = params.filter;
            this.mutator = params.mutator;
            this.activePomLocations =
                params.activePomLocations == null ? new HashSet<URI>() : new HashSet<URI>( params.activePomLocations );
            this.activeSources =
                params.activeSources == null ? new HashSet<URI>() : new HashSet<URI>( params.activeSources );
            this.properties =
                params.properties == null ? new HashMap<String, String>()
                                : new HashMap<String, String>( params.properties );
            this.selections =
                params.selections == null ? new HashMap<ProjectRef, ProjectVersionRef>()
                                : new HashMap<ProjectRef, ProjectVersionRef>( params.selections );
        }

        public Builder withRoots( final ProjectVersionRef... roots )
        {
            this.roots.clear();
            this.roots.addAll( Arrays.asList( roots ) );
            return this;
        }

        public Builder withFilter( final ProjectRelationshipFilter filter )
        {
            this.filter = filter;
            return this;
        }

        public Builder withMutator( final GraphMutator mutator )
        {
            this.mutator = mutator;
            return this;
        }

        /**
         * Adds selected versions into selections map. To work properly keys
         * must be instances of {@link ProjectRef} or of the same class as the
         * one used in {@link ViewParams#getSelection(ProjectRef)} and
         * {@link ViewParams#hasSelection(ProjectVersionRef)}.
         * 
         * @param selections
         *            map of {@link ProjectRef} to {@link ProjectVersionRef},
         *            can be {@code null} indicating that no selections were
         *            done
         * @return this instance for method chaining
         */
        public Builder withSelections( final Map<ProjectRef, ProjectVersionRef> selections )
        {
            this.selections.clear();
            if ( selections != null )
            {
                this.selections.putAll( selections );
            }
            return this;
        }

        public Builder withProperties( final Map<String, String> properties )
        {
            this.properties.clear();
            this.properties.putAll( properties );
            return this;
        }

        public Builder withActivePomLocations( final Set<URI> activePomLocations )
        {
            this.activePomLocations.clear();
            this.activePomLocations.addAll( activePomLocations );
            return this;
        }

        public Builder withActiveSources( final Set<URI> activeSources )
        {
            this.activeSources.clear();
            this.activeSources.addAll( activeSources );
            return this;
        }

        public ViewParams build()
        {
            return new ViewParams( workspaceId, filter, mutator, activePomLocations, activeSources, selections,
                                   properties, roots );
        }

        public Builder withSelection( final ProjectRef target, final ProjectVersionRef selection )
        {
            selections.put( target, selection );
            return this;
        }

        public Builder withRoots( final Collection<ProjectVersionRef> roots )
        {
            this.roots.clear();
            this.roots.addAll( roots );
            return this;
        }

    }

}
