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

public final class SimpleExtensionRelationship
    extends AbstractSimpleProjectRelationship<ExtensionRelationship, ProjectVersionRef>
    implements Serializable, ExtensionRelationship
{

    private static final long serialVersionUID = 1L;

    public SimpleExtensionRelationship( final URI source, final URI pomLocation, final ProjectVersionRef declaring,
                                        final ProjectVersionRef target, final int index, final boolean inherited )
    {
        super( source, pomLocation, RelationshipType.EXTENSION, declaring, target, index, inherited, false );
    }

    public SimpleExtensionRelationship( final Collection<URI> sources, final URI pomLocation,
                                        final ProjectVersionRef declaring, final ProjectVersionRef target,
                                        final int index, final boolean inherited )
    {
        super( sources, pomLocation, RelationshipType.EXTENSION, declaring, target, index, inherited, false );
    }

    public SimpleExtensionRelationship( final URI source, final ProjectVersionRef declaring,
                                        final ProjectVersionRef target, final int index,
                                        final boolean inherited )
    {
        super( source, RelationshipConstants.POM_ROOT_URI, RelationshipType.EXTENSION, declaring, target, index, inherited, false );
    }

    public SimpleExtensionRelationship( final Collection<URI> sources, final ProjectVersionRef declaring,
                                        final ProjectVersionRef target, final int index,
                                        final boolean inherited )
    {
        super( sources, RelationshipConstants.POM_ROOT_URI, RelationshipType.EXTENSION, declaring, target, index, inherited, false );
    }

    public SimpleExtensionRelationship( final ExtensionRelationship relationship )
    {
        super( relationship );
    }

    @Override
    public String toString()
    {
        return String.format( "ExtensionRelationship [%s => %s (index=%s)]", getDeclaring(), getTarget(), getIndex() );
    }

    @Override
    protected ProjectVersionRef cloneTarget( final ProjectVersionRef target )
    {
        return new SimpleProjectVersionRef( target );
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return new SimpleArtifactRef( getTarget(), null, null );
    }

    @Override
    public ExtensionRelationship selectDeclaring( final ProjectVersionRef ref )
    {
        final ProjectVersionRef t = getTarget();

        // TODO
        return new SimpleExtensionRelationship( getSources(), getPomLocation(), ref, t, getIndex(), isInherited() );
    }

    @Override
    public ExtensionRelationship selectTarget( final ProjectVersionRef ref )
    {
        final ProjectVersionRef d = getDeclaring();

        return new SimpleExtensionRelationship( getSources(), getPomLocation(), d, ref, getIndex(), isInherited() );
    }

    @Override
    public ExtensionRelationship cloneFor( final ProjectVersionRef declaring )
    {
        return new SimpleExtensionRelationship( getSources(), getPomLocation(), declaring, getTarget(), getIndex(), isInherited() );
    }

    @Override
    public ExtensionRelationship addSource( final URI source )
    {
        Set<URI> srcs = getSources();
        srcs.add( source );
        return new SimpleExtensionRelationship( srcs, getPomLocation(), getDeclaring(), getTarget(), getIndex(), isInherited() );
    }

    @Override
    public ExtensionRelationship addSources( final Collection<URI> sources )
    {
        Set<URI> srcs = getSources();
        srcs.addAll( sources );
        return new SimpleExtensionRelationship( srcs, getPomLocation(), getDeclaring(), getTarget(), getIndex(), isInherited() );
    }
}
