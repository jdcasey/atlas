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

import java.net.URI;
import java.util.Collection;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.SingleVersion;

/** <b>NOTE:</b> BOM relationships are actually marked as concrete.
 * This may be somewhat counter-intuitive, but they are structural (like a parent POM).
 * Therefore, managed isn't correct (despite Maven's unfortunate choice for location).
 */
public class BomRelationship
    extends AbstractProjectRelationship<ProjectVersionRef>
{

    private static final long serialVersionUID = 1L;

    public BomRelationship( final Collection<URI> sources, final ProjectVersionRef d, final ProjectVersionRef t,
                            final int index )
    {
        // BOMs are actually marked as concrete...somewhat counter-intuitive, 
        // but they're structural, so managed isn't quite correct (despite 
        // Maven's unfortunate choice for location).
        super( sources, RelationshipType.BOM, d, t, index, false );
    }

    public BomRelationship( final URI source, final ProjectVersionRef d, final ProjectVersionRef t, final int index )
    {
        // BOMs are actually marked as concrete...somewhat counter-intuitive, 
        // but they're structural, so managed isn't quite correct (despite 
        // Maven's unfortunate choice for location).
        super( source, RelationshipType.BOM, d, t, index, false );
    }

    public BomRelationship( final Collection<URI> sources, final URI pomLocation, final ProjectVersionRef d,
                            final ProjectVersionRef t, final int index )
    {
        // BOMs are actually marked as concrete...somewhat counter-intuitive, 
        // but they're structural, so managed isn't quite correct (despite 
        // Maven's unfortunate choice for location).
        super( sources, pomLocation, RelationshipType.BOM, d, t, index, false );
    }

    public BomRelationship( final URI source, final URI pomLocation, final ProjectVersionRef d,
                            final ProjectVersionRef t, final int index )
    {
        // BOMs are actually marked as concrete...somewhat counter-intuitive, 
        // but they're structural, so managed isn't quite correct (despite 
        // Maven's unfortunate choice for location).
        super( source, pomLocation, RelationshipType.BOM, d, t, index, false );
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return getTarget().asPomArtifact();
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectDeclaring( final SingleVersion version )
    {
        return selectDeclaring( version, false );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectDeclaring( final SingleVersion version, final boolean force )
    {
        final ProjectVersionRef d = getDeclaring().selectVersion( version, force );
        final ProjectVersionRef t = getTarget();

        return new BomRelationship( getSources(), d, t, getIndex() );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectTarget( final SingleVersion version )
    {
        return selectTarget( version, false );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectTarget( final SingleVersion version, final boolean force )
    {
        final ProjectVersionRef d = getDeclaring();
        final ProjectVersionRef t = getTarget().selectVersion( version, force );

        return new BomRelationship( getSources(), d, t, getIndex() );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectDeclaring( final ProjectVersionRef ref )
    {
        final ProjectVersionRef t = getTarget();

        return new BomRelationship( getSources(), ref, t, getIndex() );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectTarget( final ProjectVersionRef ref )
    {
        final ProjectVersionRef d = getDeclaring();

        return new BomRelationship( getSources(), d, ref, getIndex() );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> cloneFor( final ProjectVersionRef declaring )
    {
        return new BomRelationship( getSources(), getPomLocation(), declaring, getTarget(), getIndex() );
    }

    @Override
    public String toString()
    {
        return String.format( "BomRelationship [%s => %s]", getDeclaring(), getTarget() );
    }
}
