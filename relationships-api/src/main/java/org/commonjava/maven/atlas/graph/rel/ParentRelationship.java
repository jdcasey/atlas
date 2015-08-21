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
import org.commonjava.maven.atlas.ident.version.SingleVersion;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

public final class ParentRelationship
    extends AbstractProjectRelationship<ProjectVersionRef>
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    public static final URI TERMINAL_PARENT_SOURCE_URI;
    static
    {
        final String uri = "atlas:terminal-parent";
        try
        {
            TERMINAL_PARENT_SOURCE_URI = new URI( uri );
        }
        catch ( final URISyntaxException e )
        {
            throw new IllegalStateException( "Terminal-parent source URI constant is invalid: " + uri, e );
        }

    }

    /**
     * Ancestry terminus. This is to signify that the declaring project has NO parent relationship.
     */
    public ParentRelationship( final ProjectVersionRef declaring )
    {
        super( TERMINAL_PARENT_SOURCE_URI, RelationshipType.PARENT, declaring, declaring, 0 );
    }

    /**
     * Ancestry terminus. This is to signify that the declaring project has NO parent relationship.
     * This form is deprecated.
     * @see ParentRelationship#ParentRelationship(ProjectVersionRef)
     */
    @Deprecated
    public ParentRelationship( final URI unused, final ProjectVersionRef declaring )
    {
        super( TERMINAL_PARENT_SOURCE_URI, RelationshipType.PARENT, declaring, declaring, 0 );
    }

    public ParentRelationship( final URI source, final ProjectVersionRef declaring, final ProjectVersionRef target )
    {
        super( source, RelationshipType.PARENT, declaring, target, 0 );
    }

    public ParentRelationship( final Collection<URI> sources, final ProjectVersionRef declaring,
                               final ProjectVersionRef target )
    {
        super( sources, RelationshipType.PARENT, declaring, target, 0 );
    }

    @Override
    public String toString()
    {
        return String.format( "ParentRelationship [%s => %s]", getDeclaring(), getTarget() );
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return new SimpleArtifactRef( getTarget(), "pom", null, false );
    }

    public boolean isTerminus()
    {
        return getDeclaring().equals( getTarget() );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectDeclaring( final SingleVersion version )
    {
        return selectDeclaring( version, false );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectDeclaring( final SingleVersion version, final boolean force )
    {
        ProjectVersionRef d = getDeclaring();
        final ProjectVersionRef t = getTarget();
        final boolean self = d.equals( t );

        d = d.selectVersion( version, force );

        return new ParentRelationship( getSources(), d, self ? d : t );
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
        ProjectVersionRef t = getTarget();
        final boolean self = d.equals( t );

        t = t.selectVersion( version, force );

        return new ParentRelationship( getSources(), self ? t : d, t );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectDeclaring( final ProjectVersionRef ref )
    {
        final ProjectVersionRef t = getTarget();

        return new ParentRelationship( getSources(), ref, t );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> selectTarget( final ProjectVersionRef ref )
    {
        final ProjectVersionRef d = getDeclaring();
        return new ParentRelationship( getSources(), d, ref );
    }

    @Override
    public ProjectRelationship<ProjectVersionRef> cloneFor( final ProjectVersionRef declaring )
    {
        return new ParentRelationship( getSources(), declaring, getTarget() );
    }

}
