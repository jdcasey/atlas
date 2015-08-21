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
package org.commonjava.maven.atlas.graph.model;

import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.filterTerminalParents;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.BomRelationship;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;

public class EProjectDirectRelationships
    implements EProjectRelationshipCollection, Serializable
{

    private static final long serialVersionUID = 1L;

    private final URI source;

    private final ProjectVersionRef ref;

    private final List<BomRelationship> boms;

    private final List<DependencyRelationship> dependencies;

    private final List<DependencyRelationship> managedDependencies;

    private final List<PluginRelationship> plugins;

    private final List<PluginRelationship> managedPlugins;

    private final List<ExtensionRelationship> extensions;

    private final ParentRelationship parent;

    private final Map<PluginRelationship, List<PluginDependencyRelationship>> pluginDependencies;

    public EProjectDirectRelationships( final URI source,
                                        final ProjectVersionRef ref,
                                        final ParentRelationship parent,
                                        final List<BomRelationship> boms,
                                        final List<DependencyRelationship> dependencies, final List<PluginRelationship> plugins,
                                        final List<DependencyRelationship> managedDependencies, final List<PluginRelationship> managedPlugins,
                                        final List<ExtensionRelationship> extensions,
                                        final Map<PluginRelationship, List<PluginDependencyRelationship>> pluginDependencies )
    {
        this.source = source;
        this.ref = ref;
        this.parent = parent;
        this.boms = Collections.unmodifiableList( boms );
        this.dependencies = Collections.unmodifiableList( dependencies );
        this.plugins = Collections.unmodifiableList( plugins );
        this.managedDependencies = Collections.unmodifiableList( managedDependencies );
        this.managedPlugins = Collections.unmodifiableList( managedPlugins );
        this.extensions = Collections.unmodifiableList( extensions );

        final HashMap<PluginRelationship, List<PluginDependencyRelationship>> pdrels =
            new HashMap<PluginRelationship, List<PluginDependencyRelationship>>();

        for ( final Map.Entry<PluginRelationship, List<PluginDependencyRelationship>> entry : pluginDependencies.entrySet() )
        {
            pdrels.put( entry.getKey(), Collections.unmodifiableList( entry.getValue() ) );
        }

        this.pluginDependencies = Collections.unmodifiableMap( pdrels );
    }

    public final ProjectVersionRef getProjectRef()
    {
        return ref;
    }

    public final List<DependencyRelationship> getDependencies()
    {
        return dependencies;
    }

    public final List<DependencyRelationship> getManagedDependencies()
    {
        return managedDependencies;
    }

    public final List<PluginRelationship> getPlugins()
    {
        return plugins;
    }

    public final List<PluginRelationship> getManagedPlugins()
    {
        return managedPlugins;
    }

    public final List<ExtensionRelationship> getExtensions()
    {
        return extensions;
    }

    public final ParentRelationship getParent()
    {
        return parent;
    }

    public final Map<PluginRelationship, List<PluginDependencyRelationship>> getPluginDependencies()
    {
        return pluginDependencies;
    }

    public final List<PluginDependencyRelationship> getPluginDependencies( final ProjectVersionRef plugin, final boolean managed )
    {
        final PluginRelationship pr = new PluginRelationship( source, getProjectRef(), plugin, 0, managed );
        return pluginDependencies.get( pr );
    }

    public final List<BomRelationship> getBoms()
    {
        return boms;
    }

    @Override
    public Set<ProjectRelationship<?>> getAllRelationships()
    {
        final Set<ProjectRelationship<?>> rels = getExactAllRelationships();
        filterTerminalParents( rels );

        return rels;
    }

    @Override
    public Set<ProjectRelationship<?>> getExactAllRelationships()
    {
        final Set<ProjectRelationship<?>> result = new HashSet<ProjectRelationship<?>>();
        if ( parent != null )
        {
            result.add( parent );
        }

        result.addAll( boms );
        result.addAll( dependencies );
        result.addAll( managedDependencies );
        result.addAll( plugins );
        result.addAll( managedPlugins );
        result.addAll( extensions );

        for ( final List<PluginDependencyRelationship> pluginRels : pluginDependencies.values() )
        {
            result.addAll( pluginRels );
        }

        return result;
    }

    public static final class Builder
    {
        private final URI source;

        private final ProjectVersionRef ref;

        private final List<BomRelationship> boms = new ArrayList<BomRelationship>();

        private final List<DependencyRelationship> dependencies = new ArrayList<DependencyRelationship>();

        private final List<DependencyRelationship> managedDependencies = new ArrayList<DependencyRelationship>();

        private final List<PluginRelationship> plugins = new ArrayList<PluginRelationship>();

        private final List<PluginRelationship> managedPlugins = new ArrayList<PluginRelationship>();

        private final List<ExtensionRelationship> extensions = new ArrayList<ExtensionRelationship>();

        private ParentRelationship parent;

        private final Map<PluginRelationship, List<PluginDependencyRelationship>> pluginDependencies =
            new HashMap<PluginRelationship, List<PluginDependencyRelationship>>();

        public Builder( final URI source, final ProjectVersionRef projectRef, final String... activeProfiles )
        {
            this.source = source;
            this.ref = projectRef;
        }

        public EProjectDirectRelationships build()
        {
            if ( parent == null )
            {
                parent = new ParentRelationship( source, ref );
            }

            return new EProjectDirectRelationships( source, ref, parent, boms, dependencies, plugins,
                                                    managedDependencies, managedPlugins, extensions,
                                                    pluginDependencies );
        }

        public Builder withParent( final ProjectVersionRef parent )
        {
            this.parent = new ParentRelationship( source, ref, parent );
            return this;
        }

        public Builder withParent( final ParentRelationship parent )
        {
            this.parent = adjustDeclaring( parent );

            return this;
        }

        @SuppressWarnings( "unchecked" )
        private <C extends ProjectRelationship<?>> C adjustDeclaring( final C rel )
        {
            if ( !ref
                     .equals( rel.getDeclaring() ) )
            {
                return (C) rel.cloneFor( ref );
            }

            return rel;
        }

        public Builder withBoms( final BomRelationship... boms )
        {
            return withBoms( Arrays.asList( boms ) );
        }

        public Builder withBoms( final Collection<BomRelationship> boms )
        {
            for ( BomRelationship bom : boms )
            {
                bom = adjustDeclaring( bom );
                this.boms.add( bom );
            }

            return this;
        }

        public Builder withDependencies( final DependencyRelationship... deps )
        {
            return withDependencies( Arrays.asList( deps ) );
        }

        public Builder withDependencies( final Collection<DependencyRelationship> deps )
        {
            for ( DependencyRelationship dep : deps )
            {
                dep = adjustDeclaring( dep );
                if ( dep.isManaged() )
                {
                    if ( !managedDependencies.contains( dep ) )
                    {
                        managedDependencies.add( dep );
                    }
                }
                else
                {
                    if ( !dependencies.contains( dep ) )
                    {
                        dependencies.add( dep );
                    }
                }
            }

            return this;
        }

        public Builder withPlugins( final PluginRelationship... plugins )
        {
            return withPlugins( Arrays.asList( plugins ) );
        }

        public Builder withPlugins( final Collection<PluginRelationship> plugins )
        {
            for ( PluginRelationship plugin : plugins )
            {
                plugin = adjustDeclaring( plugin );
                if ( plugin.isManaged() )
                {
                    if ( !managedPlugins.contains( plugin ) )
                    {
                        managedPlugins.add( plugin );
                    }
                }
                else
                {
                    if ( !this.plugins.contains( plugin ) )
                    {
                        this.plugins.add( plugin );
                    }
                }
            }

            return this;
        }

        public Builder withPluginDependencies( final PluginDependencyRelationship... pluginDeps )
        {
            return withPluginDependencies( Arrays.asList( pluginDeps ) );
        }

        public Builder withPluginDependencies( final Collection<PluginDependencyRelationship> pluginDeps )
        {
            for ( PluginDependencyRelationship rel : pluginDeps )
            {
                rel = adjustDeclaring( rel );
                final ProjectRef pluginRef = rel.getPlugin();

                PluginRelationship pr = null;
                if ( rel.isManaged() )
                {
                    for ( final PluginRelationship pluginRel : managedPlugins )
                    {
                        if ( pluginRef.equals( pluginRel.getTarget() ) )
                        {
                            pr = pluginRel;
                            break;
                        }
                    }
                }
                else
                {
                    for ( final PluginRelationship pluginRel : plugins )
                    {
                        if ( pluginRef.equals( pluginRel.getTarget() ) )
                        {
                            pr = pluginRel;
                            break;
                        }
                    }
                }

                if ( pr == null )
                {
                    throw new IllegalArgumentException( "Orphaned plugin-level dependency found: " + rel
                        + ". Make sure you load plugin relationships BEFORE attempting to load plugin-dependency-relationships." );
                }

                List<PluginDependencyRelationship> pdrs = pluginDependencies.get( pr );
                if ( pdrs == null )
                {
                    pdrs = new ArrayList<PluginDependencyRelationship>();
                    pluginDependencies.put( pr, pdrs );
                }

                if ( !pdrs.contains( rel ) )
                {
                    pdrs.add( rel );
                }
            }

            return this;
        }

        public Builder withExtensions( final ExtensionRelationship... exts )
        {
            return withExtensions( Arrays.asList( exts ) );
        }

        public Builder withExtensions( final Collection<ExtensionRelationship> exts )
        {
            for ( final ExtensionRelationship ext : exts )
            {
                if ( !extensions.contains( ext ) )
                {
                    extensions.add( adjustDeclaring( ext ) );
                }
            }

            return this;
        }

        public Builder withRelationships( final Collection<ProjectRelationship<?>> relationships )
        {
            final Set<PluginDependencyRelationship> pluginDepRels = new HashSet<PluginDependencyRelationship>();
            for ( ProjectRelationship<?> rel : relationships )
            {
                rel = adjustDeclaring( rel );
                switch ( rel.getType() )
                {
                    case BOM:
                    {
                        withBoms( (BomRelationship) rel );
                        break;
                    }
                    case DEPENDENCY:
                    {
                        withDependencies( (DependencyRelationship) rel );

                        break;
                    }
                    case PLUGIN:
                    {
                        withPlugins( (PluginRelationship) rel );

                        break;
                    }
                    case EXTENSION:
                    {
                        withExtensions( (ExtensionRelationship) rel );
                        break;
                    }
                    case PLUGIN_DEP:
                    {
                        // load all plugin relationships first.
                        pluginDepRels.add( (PluginDependencyRelationship) rel );
                        break;
                    }
                    case PARENT:
                    {
                        withParent( (ParentRelationship) rel );
                        break;
                    }
                }
            }

            withPluginDependencies( pluginDepRels );

            return this;
        }

        public int getNextPluginIndex( final boolean managed )
        {
            return managed ? managedPlugins.size() : plugins.size();
        }

        public int getNextPluginDependencyIndex( final ProjectVersionRef plugin, final boolean managed )
        {
            final List<PluginDependencyRelationship> list =
                pluginDependencies.get( new PluginRelationship( source, ref, plugin, 0, managed ) );
            return list == null ? 0 : list.size();
        }

        public int getNextDependencyIndex( final boolean managed )
        {
            return managed ? managedDependencies.size() : dependencies.size();
        }

        public int getNextExtensionIndex()
        {
            return extensions.size();
        }

        public Builder withDependency( final ProjectVersionRef ref, final String type, final String classifier, final DependencyScope scope,
                                       final boolean managed )
        {
            withDependencies( new DependencyRelationship( source, ref, new SimpleArtifactRef( ref, type, classifier, false ),
                                                          scope,
                                                          getNextDependencyIndex( managed ), managed ) );

            return this;
        }

        public Builder withPlugin( final ProjectVersionRef ref, final boolean managed )
        {
            withPlugins( new PluginRelationship( source, ref, ref, getNextPluginIndex( managed ), managed ) );

            return this;
        }

        public Builder withExtension( final ProjectVersionRef ref )
        {
            withExtensions( new ExtensionRelationship( source, RelationshipUtils.POM_ROOT_URI, ref, ref,
                                                       getNextExtensionIndex() ) );
            return this;
        }

        public ProjectVersionRef getProjectRef()
        {
            return ref;
        }

    }

}
