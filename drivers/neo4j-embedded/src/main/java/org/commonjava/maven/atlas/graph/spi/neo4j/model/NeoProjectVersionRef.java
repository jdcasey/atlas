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
package org.commonjava.maven.atlas.graph.spi.neo4j.model;

import static org.commonjava.maven.atlas.graph.spi.neo4j.model.NeoIdentityUtils.getStringProperty;

import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.ident.ref.*;
import org.commonjava.maven.atlas.ident.util.VersionUtils;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.maven.atlas.ident.version.VersionSpec;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Reference to a particular release of a project (or module, in terms of Maven builds). A release may contain many artifacts (see {@link NeoArtifactRef}).
 *
 * @see {@link NeoProjectRef}
 * @see {@link NeoArtifactRef}
 *
 * @author jdcasey
 */
public class NeoProjectVersionRef
        extends NeoProjectRef
        implements ProjectVersionRef
{

    private static final long serialVersionUID = 1L;

    // NEVER null
    private VersionSpec versionSpec;

    private String versionString;

    public NeoProjectVersionRef( final Node node )
    {
        super( node );
    }

    public NeoProjectVersionRef( final ProjectRef ref, final String versionSpec )
            throws InvalidVersionSpecificationException
    {
        this( ref.getGroupId(), ref.getArtifactId(), versionSpec );
    }

    NeoProjectVersionRef( final String groupId, final String artifactId, final VersionSpec versionSpec,
                          final String versionString )
    {
        super( groupId, artifactId );
        if ( versionSpec == null && versionString == null )
        {
            throw new InvalidRefException(
                    "Version spec AND string cannot both be null for '" + groupId + ":" + artifactId + "'" );
        }

        this.versionString = versionString;
        this.versionSpec = versionSpec;
    }

    public NeoProjectVersionRef( final String groupId, final String artifactId, final VersionSpec versionSpec )
    {
        this( groupId, artifactId, versionSpec, null );
    }

    public NeoProjectVersionRef( final String groupId, final String artifactId, final String versionString )
            throws InvalidVersionSpecificationException
    {
        this( groupId, artifactId, null, versionString );
    }

    public NeoProjectVersionRef( ProjectVersionRef ref )
    {
        super( ref );
        if ( container == null )
        {
            this.versionString = ref.getVersionStringRaw();
            this.versionSpec = ref.getVersionSpecRaw();
        }
        else if ( ref instanceof NeoProjectVersionRef )
        {
            this.versionString = ((NeoProjectVersionRef)ref).versionString;
            this.versionSpec = ((NeoProjectVersionRef)ref).versionSpec;
        }
    }

    public static ProjectVersionRef parse( final String gav )
    {
        final String[] parts = gav.split( ":" );
        if ( parts.length < 3 || isEmpty( parts[0] ) || isEmpty( parts[1] ) || isEmpty( parts[2] ) )
        {
            throw new InvalidRefException(
                    "ProjectVersionRef must contain non-empty groupId, artifactId, AND version. (Given: '" + gav
                            + "')" );
        }

        return new NeoProjectVersionRef( parts[0], parts[1], parts[2] );
    }

    @Override
    public ProjectVersionRef asProjectVersionRef()
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug("Trying to create new ProjectVersionRef from: {}", getClass().getSimpleName());

        return NeoProjectVersionRef.class.equals( getClass() ) ?
                this :
                new NeoProjectVersionRef( this );
    }

    @Override
    public ArtifactRef asPomArtifact()
    {
        return asArtifactRef( "pom", null, false );
    }

    @Override
    public ArtifactRef asJarArtifact()
    {
        return asArtifactRef( "jar", null, false );
    }

    @Override
    public ArtifactRef asArtifactRef( final String type, final String classifier )
    {
        return asArtifactRef( type, classifier, false );
    }

    @Override
    public ArtifactRef asArtifactRef( final String type, final String classifier, final boolean optional )
    {
        return new NeoArtifactRef( this, type, classifier, optional );
    }

    @Override
    public ArtifactRef asArtifactRef( final TypeAndClassifier tc )
    {
        return asArtifactRef( tc, false );
    }

    @Override
    public ArtifactRef asArtifactRef( final TypeAndClassifier tc, final boolean optional )
    {
        NeoTypeAndClassifier ntc = ( tc instanceof NeoTypeAndClassifier ) ?
                (NeoTypeAndClassifier) tc :
                new NeoTypeAndClassifier( tc.getType(), tc.getClassifier() );

        return new NeoArtifactRef( this, ntc, optional );
    }

    @Override
    public VersionSpec getVersionSpecRaw()
    {
        return versionSpec;
    }

    @Override
    public String getVersionStringRaw()
    {
        String v = getStringProperty( container, Conversions.VERSION, versionString, null );
        return v;
    }

    @Override
    public boolean isRelease()
    {
        return getVersionSpec().isRelease();
    }

    @Override
    public boolean isSpecificVersion()
    {
        return getVersionSpec().isSingle();
    }

    @Override
    public boolean matchesVersion( final SingleVersion version )
    {
        return getVersionSpec().contains( version );
    }

    @Override
    public NeoProjectVersionRef selectVersion( final String version )
    {
        final SingleVersion single = VersionUtils.createSingleVersion( version );
        return selectVersion( single, false );
    }

    @Override
    public NeoProjectVersionRef selectVersion( final String version, final boolean force )
    {
        final SingleVersion single = VersionUtils.createSingleVersion( version );
        return selectVersion( single, force );
    }

    @Override
    public NeoProjectVersionRef selectVersion( final SingleVersion version )
    {
        return selectVersion( version, false );
    }

    @Override
    public NeoProjectVersionRef selectVersion( final SingleVersion version, final boolean force )
    {
        final VersionSpec versionSpec = getVersionSpec();
        if ( versionSpec.equals( version ) )
        {
            return this;
        }

        if ( !force && !versionSpec.contains( version ) )
        {
            throw new IllegalArgumentException(
                    "Specified version: " + version.renderStandard() + " is not contained in spec: "
                            + versionSpec.renderStandard() );
        }

        return newRef( getGroupId(), getArtifactId(), version );
    }

    @Override
    public NeoProjectVersionRef newRef( final String groupId, final String artifactId, final SingleVersion version )
    {
        return new NeoProjectVersionRef( groupId, artifactId, version );
    }

    @Override
    public VersionSpec getVersionSpec()
    {
        if ( versionSpec == null )
        {
            versionSpec = VersionUtils.createFromSpec( getVersionStringRaw() );
        }
        return versionSpec;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( getVersionString() == null ) ? 0 : getVersionString().hashCode() );
        return result;
    }

    public boolean versionlessEquals( final ProjectVersionRef other )
    {
        return this == other || super.equals( other );
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
        if ( !( obj instanceof ProjectVersionRef ) )
        {
            return false;
        }

        final ProjectVersionRef other = (ProjectVersionRef) obj;
        boolean result = true;
        try
        {
            if ( getVersionSpec() == null )
            {
                if ( other.getVersionSpec() != null )
                {
                    result = false;
                }
            }
            else if ( !getVersionSpec().equals( other.getVersionSpec() ) )
            {
                result = false;
            }
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            if ( getVersionString() == null )
            {
                if ( other.getVersionString() != null )
                {
                    result = false;
                }
            }
            else if ( !getVersionString().equals( other.getVersionString() ) )
            {
                result = false;
            }
        }

        return result;
    }

    @Override
    public String toString()
    {
        String msg = String.format( "%s:%s:%s", getGroupId(), getArtifactId(), getVersionString() );
        return msg;
    }

    @Override
    public boolean isCompound()
    {
        return !getVersionSpec().isSingle();
    }

    @Override
    public boolean isSnapshot()
    {
        return getVersionSpec().isSnapshot();
    }

    @Override
    public String getVersionString()
    {
        if ( versionString == null )
        {
            String v = getVersionStringRaw();
            if ( v == null )
            {
                return versionSpec.renderStandard();
            }

            return v;
        }

        return versionString;
    }

    @Override
    public boolean isVariableVersion()
    {
        return isCompound() || ( isSpecificVersion() && ( (SingleVersion) getVersionSpec() ).isLocalSnapshot() );
    }

    @Override
    public int compareTo( final ProjectRef o )
    {
        int comp = super.compareTo( o );
        if ( comp == 0 && ( o instanceof ProjectVersionRef ) )
        {
            final ProjectVersionRef or = (ProjectVersionRef) o;
            comp = getVersionString().compareTo( or.getVersionString() );
        }

        return comp;
    }

    @Override
    public boolean isDirty()
    {
        return super.isDirty() || versionString != null;
    }
}
