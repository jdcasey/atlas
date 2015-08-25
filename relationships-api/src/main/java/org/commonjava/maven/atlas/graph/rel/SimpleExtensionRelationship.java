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

import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;

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
                                        final ProjectVersionRef target, final int index )
    {
        super( source, pomLocation, RelationshipType.EXTENSION, declaring, target, index );
    }

    public SimpleExtensionRelationship( final Collection<URI> sources, final URI pomLocation,
                                        final ProjectVersionRef declaring, final ProjectVersionRef target,
                                        final int index )
    {
        super( sources, pomLocation, RelationshipType.EXTENSION, declaring, target, index );
    }

    public SimpleExtensionRelationship( final URI source, final ProjectVersionRef declaring,
                                        final ProjectVersionRef target, final int index )
    {
        super( source, RelationshipUtils.POM_ROOT_URI, RelationshipType.EXTENSION, declaring, target, index );
    }

    public SimpleExtensionRelationship( final Collection<URI> sources, final ProjectVersionRef declaring,
                                        final ProjectVersionRef target, final int index )
    {
        super( sources, RelationshipUtils.POM_ROOT_URI, RelationshipType.EXTENSION, declaring, target, index );
    }

    @Override
    public String toString()
    {
        return String.format( "ExtensionRelationship [%s => %s (index=%s)]", getDeclaring(), getTarget(), getIndex() );
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return new SimpleArtifactRef( getTarget(), null, null, false );
    }

    @Override
    public ExtensionRelationship selectDeclaring( final ProjectVersionRef ref )
    {
        final ProjectVersionRef t = getTarget();

        return new SimpleExtensionRelationship( getSources(), getPomLocation(), ref, t, getIndex() );
    }

    @Override
    public ExtensionRelationship selectTarget( final ProjectVersionRef ref )
    {
        final ProjectVersionRef d = getDeclaring();

        return new SimpleExtensionRelationship( getSources(), getPomLocation(), d, ref, getIndex() );
    }

    @Override
    public ExtensionRelationship cloneFor( final ProjectVersionRef declaring )
    {
        return new SimpleExtensionRelationship( getSources(), getPomLocation(), declaring, getTarget(), getIndex() );
    }

    @Override
    public ExtensionRelationship addSource( URI source )
    {
        Set<URI> srcs = getSources();
        srcs.add( source );
        return new SimpleExtensionRelationship( srcs, getPomLocation(), getDeclaring(), getTarget(), getIndex() );
    }

    @Override
    public ExtensionRelationship addSources( Collection<URI> sources )
    {
        Set<URI> srcs = getSources();
        srcs.addAll( sources );
        return new SimpleExtensionRelationship( srcs, getPomLocation(), getDeclaring(), getTarget(), getIndex() );
    }
}
