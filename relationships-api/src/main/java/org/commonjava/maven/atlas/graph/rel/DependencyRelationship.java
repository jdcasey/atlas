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
package org.commonjava.maven.atlas.graph.rel;

import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.version.SingleVersion;

import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class DependencyRelationship
    extends AbstractProjectRelationship<ArtifactRef>
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    private final DependencyScope scope;

    private final Set<ProjectRef> excludes;

    public DependencyRelationship( final URI source, final ProjectVersionRef declaring, final ArtifactRef target,
                                   final DependencyScope scope, final int index, final boolean managed,
                                   final ProjectRef... excludes )
    {
        super( source, RelationshipType.DEPENDENCY, declaring, target, index, managed );
        this.scope = scope == null ? DependencyScope.compile : scope;
        this.excludes = new HashSet<ProjectRef>( Arrays.asList( excludes ) );
    }

    public DependencyRelationship( final URI source, final URI pomLocation, final ProjectVersionRef declaring,
                                   final ArtifactRef target, final DependencyScope scope, final int index,
                                   final boolean managed, final ProjectRef... excludes )
    {
        super( source, pomLocation, RelationshipType.DEPENDENCY, declaring, target, index, managed );
        this.scope = scope == null ? DependencyScope.compile : scope;
        this.excludes = new HashSet<ProjectRef>( Arrays.asList( excludes ) );
    }

    public DependencyRelationship( final Collection<URI> sources, final ProjectVersionRef declaring,
                                   final ArtifactRef target, final DependencyScope scope, final int index,
                                   final boolean managed, final ProjectRef... excludes )
    {
        super( sources, RelationshipType.DEPENDENCY, declaring, target, index, managed );
        this.scope = scope == null ? DependencyScope.compile : scope;
        this.excludes = new HashSet<ProjectRef>( Arrays.asList( excludes ) );
    }

    public DependencyRelationship( final Collection<URI> sources, final URI pomLocation,
                                   final ProjectVersionRef declaring, final ArtifactRef target,
                                   final DependencyScope scope, final int index, final boolean managed,
                                   final ProjectRef... excludes )
    {
        super( sources, pomLocation, RelationshipType.DEPENDENCY, declaring, target, index, managed );
        this.scope = scope == null ? DependencyScope.compile : scope;
        this.excludes = new HashSet<ProjectRef>( Arrays.asList( excludes ) );
    }

    public final DependencyScope getScope()
    {
        return scope;
    }

    @Override
    public synchronized ProjectRelationship<ArtifactRef> cloneFor( final ProjectVersionRef projectRef )
    {
        return new DependencyRelationship( getSources(), getPomLocation(), projectRef, getTarget(), scope, getIndex(),
                                           isManaged() );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( isManaged() ? 1231 : 1237 );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( !super.equals( obj ) )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final DependencyRelationship other = (DependencyRelationship) obj;
        return isManaged() == other.isManaged();
    }

    @Override
    public String toString()
    {
        return String.format( "DependencyRelationship [%s => %s (managed=%s, scope=%s, index=%s)]", getDeclaring(),
                              getTarget(), isManaged(), scope, getIndex() );
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return getTarget();
    }

    public Set<ProjectRef> getExcludes()
    {
        return excludes;
    }

    @Override
    public ProjectRelationship<ArtifactRef> selectDeclaring( final SingleVersion version )
    {
        return selectDeclaring( version, false );
    }

    @Override
    public ProjectRelationship<ArtifactRef> selectDeclaring( final SingleVersion version, final boolean force )
    {
        final ProjectVersionRef d = getDeclaring().selectVersion( version, force );
        final ArtifactRef t = getTarget();

        Set<ProjectRef> var = getExcludes();
        return new DependencyRelationship( getSources(), getPomLocation(), d, t, getScope(), getIndex(), isManaged(),
                                           var.toArray( new ProjectRef[var.size()] ) );
    }

    @Override
    public ProjectRelationship<ArtifactRef> selectTarget( final SingleVersion version )
    {
        return selectTarget( version, false );
    }

    @Override
    public ProjectRelationship<ArtifactRef> selectTarget( final SingleVersion version, final boolean force )
    {
        final ProjectVersionRef d = getDeclaring();
        ArtifactRef t = getTarget();
        t = new SimpleArtifactRef( t.selectVersion( version, force ), t.getType(), t.getClassifier(), t.isOptional() );

        Set<ProjectRef> var = getExcludes();
        return new DependencyRelationship( getSources(), getPomLocation(), d, t, getScope(), getIndex(), isManaged(),
                                           var.toArray( new ProjectRef[var.size()] ) );
    }

    @Override
    public ProjectRelationship<ArtifactRef> selectDeclaring( final ProjectVersionRef ref )
    {
        final ArtifactRef t = getTarget();

        Set<ProjectRef> var = getExcludes();
        return new DependencyRelationship( getSources(), getPomLocation(), ref, t, getScope(), getIndex(), isManaged(),
                                           var.toArray( new ProjectRef[var.size()] ) );
    }

    @Override
    public ProjectRelationship<ArtifactRef> selectTarget( final ProjectVersionRef ref )
    {
        final ProjectVersionRef d = getDeclaring();
        ArtifactRef t = getTarget();
        t =
            (ArtifactRef) ( ( ref instanceof ArtifactRef ) ? ref : new SimpleArtifactRef( ref, t.getType(),
                                                                                    t.getClassifier(), t.isOptional() ) );

        Set<ProjectRef> var = getExcludes();
        return new DependencyRelationship( getSources(), getPomLocation(), d, t, getScope(), getIndex(), isManaged(),
                                           var.toArray( new ProjectRef[var.size()] ) );
    }

    public boolean isBOM()
    {
        return DependencyScope._import == getScope() && "pom".equals( getTargetArtifact().getType() );
    }

}
