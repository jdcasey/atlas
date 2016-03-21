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

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.Set;

public final class SimpleParentRelationship
    extends AbstractSimpleProjectRelationship<ParentRelationship, ProjectVersionRef>
    implements Serializable, ParentRelationship
{

    private static final long serialVersionUID = 1L;

    /**
     * Ancestry terminus. This is to signify that the declaring project has NO parent relationship.
     */
    public SimpleParentRelationship( final ProjectVersionRef declaring )
    {
        super( RelationshipConstants.TERMINAL_PARENT_SOURCE_URI, RelationshipType.PARENT, declaring, declaring, 0, false, false );
    }

    /**
     * Ancestry terminus. This is to signify that the declaring project has NO parent relationship.
     * This form is deprecated.
     * @see SimpleParentRelationship(ProjectVersionRef)
     */
    @Deprecated
    public SimpleParentRelationship( final URI unused, final ProjectVersionRef declaring )
    {
        super( RelationshipConstants.TERMINAL_PARENT_SOURCE_URI, RelationshipType.PARENT, declaring, declaring, 0, false, false );
    }

    public SimpleParentRelationship( final URI source, final ProjectVersionRef declaring,
                                     final ProjectVersionRef target )
    {
        super( source, RelationshipType.PARENT, declaring, target, 0, false, false );
    }

    public SimpleParentRelationship( final Collection<URI> sources, final ProjectVersionRef declaring,
                                     final ProjectVersionRef target )
    {
        super( sources, RelationshipType.PARENT, declaring, target, 0, false, false );
    }

    public SimpleParentRelationship( final ParentRelationship relationship )
    {
        super( relationship );
    }

    @Override
    public String toString()
    {
        return String.format( "ParentRelationship [%s => %s]", getDeclaring(), getTarget() );
    }

    @Override
    protected ProjectVersionRef cloneTarget( final ProjectVersionRef target )
    {
        return new SimpleProjectVersionRef( target );
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return new SimpleArtifactRef( getTarget(), "pom", null );
    }

    @Override
    public boolean isTerminus()
    {
        return getDeclaring().equals( getTarget() );
    }

    @Override
    public ParentRelationship selectDeclaring( final ProjectVersionRef ref )
    {
        final ProjectVersionRef t = getTarget();

        return new SimpleParentRelationship( getSources(), ref, t );
    }

    @Override
    public ParentRelationship selectTarget( final ProjectVersionRef ref )
    {
        final ProjectVersionRef d = getDeclaring();
        return new SimpleParentRelationship( getSources(), d, ref );
    }

    @Override
    public ParentRelationship cloneFor( final ProjectVersionRef declaring )
    {
        return new SimpleParentRelationship( getSources(), declaring, getTarget() );
    }

    @Override
    public ParentRelationship addSource( final URI source )
    {
        Set<URI> srcs = getSources();
        srcs.add( source );
        return new SimpleParentRelationship( srcs, getDeclaring(), getTarget() );
    }

    @Override
    public ParentRelationship addSources( final Collection<URI> sources )
    {
        Set<URI> srcs = getSources();
        srcs.addAll( sources );
        return new SimpleParentRelationship( srcs, getDeclaring(), getTarget() );
    }
}
