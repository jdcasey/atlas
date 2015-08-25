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
import org.commonjava.maven.atlas.ident.ref.InvalidRefException;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.TypeAndClassifier;
import org.commonjava.maven.atlas.ident.ref.VersionlessArtifactRef;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Reference to a whole project (or module, in terms of Maven builds). This reference is not specific to a release of the project (see {@link NeoProjectVersionRef}).
 *
 * @author jdcasey
 */
public class NeoProjectRef
    implements ProjectRef
{

    private static final long serialVersionUID = 1L;

    // NEVER null
    private String groupId;

    // NEVER null
    private String artifactId;

    protected PropertyContainer container;

    private String groupIdProperty = Conversions.GROUP_ID;

    private String artifactIdProperty = Conversions.ARTIFACT_ID;

    public NeoProjectRef(ProjectRef ref)
    {
        if ( ref instanceof NeoProjectRef)
        {
            container = ((NeoProjectRef)ref).container;
        }
        else
        {
            groupId = ref.getGroupId();
            artifactId = ref.getArtifactId();
        }
    }

    public NeoProjectRef( final String groupId, final String artifactId )
    {
        if ( isEmpty( groupId ) || isEmpty( artifactId ) )
        {
            throw new InvalidRefException( "ProjectId must contain non-empty groupId AND artifactId. (Given: '"
                + groupId + "':'" + artifactId + "')" );
        }

        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public NeoProjectRef( Node container )
    {
        this.container = container;
        if ( isEmpty( getGroupId() ) || isEmpty( getArtifactId() ) )
        {
            throw new InvalidRefException( "ProjectId must contain non-empty groupId AND artifactId. (Given: '"
                                                   + getGroupId() + "':'" + getArtifactId() + "', db-node=" + container.getId() + ")" );
        }
    }

    public NeoProjectRef( PropertyContainer container, String groupIdProperty, String artifactIdProperty )
    {
        this.container = container;
        this.groupIdProperty = groupIdProperty;
        this.artifactIdProperty = artifactIdProperty;
    }

    public static ProjectRef parse( final String ga )
    {
        final String[] parts = ga.split( ":" );
        if ( parts.length < 2 || isEmpty( parts[0] ) || isEmpty( parts[1] ) )
        {
            throw new InvalidRefException( "ProjectRef must contain non-empty groupId AND artifactId. (Given: '" + ga
                + "')" );
        }

        return new NeoProjectRef( parts[0], parts[1] );
    }

    @Override
    public final String getGroupId()
    {
        return getStringProperty( container, groupIdProperty, groupId, null );
    }

    @Override
    public final String getArtifactId()
    {
        return getStringProperty( container, artifactIdProperty, artifactId, null );
    }

    @Override
    public ProjectRef asProjectRef()
    {
        return NeoProjectRef.class.equals( getClass() ) ? this : new NeoProjectRef( getGroupId(), getArtifactId() );
    }

    @Override
    public VersionlessArtifactRef asVersionlessPomArtifact()
    {
        return asVersionlessArtifactRef( "pom", null, false );
    }

    @Override
    public VersionlessArtifactRef asVersionlessJarArtifact()
    {
        return asVersionlessArtifactRef( "jar", null, false );
    }

    @Override
    public VersionlessArtifactRef asVersionlessArtifactRef( final String type, final String classifier )
    {
        return asVersionlessArtifactRef( type, classifier, false );
    }

    @Override
    public VersionlessArtifactRef asVersionlessArtifactRef( final String type, final String classifier,
                                                            final boolean optional )
    {
        return new NeoVersionlessArtifactRef( this, type, classifier, optional );
    }

    @Override
    public VersionlessArtifactRef asVersionlessArtifactRef( final TypeAndClassifier tc )
    {
        return asVersionlessArtifactRef( tc, false );
    }

    @Override
    public VersionlessArtifactRef asVersionlessArtifactRef( final TypeAndClassifier tc, final boolean optional )
    {
        return new NeoVersionlessArtifactRef( this, tc, optional );
    }

    @Override
    public String toString()
    {
        return String.format( "%s:%s", getArtifactId(), getGroupId() );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + getArtifactId().hashCode();
        result = prime * result + getGroupId().hashCode();
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
        if ( !(obj instanceof ProjectRef) )
        {
            return false;
        }
        final ProjectRef other = (ProjectRef) obj;
        if ( !getArtifactId().equals( other.getArtifactId() ) )
        {
            return false;
        }
        return getGroupId().equals( other.getGroupId() );
    }

    public int compareTo( final ProjectRef o )
    {
        int comp = getGroupId().compareTo( o.getGroupId() );
        if ( comp == 0 )
        {
            comp = getArtifactId().compareTo( o.getArtifactId() );
        }

        return comp;
    }

    @Override
    public boolean matches( final ProjectRef ref )
    {
        if ( equals( ref ) )
        {
            return true;
        }

        final String gidPattern = toWildcard( getGroupId() );
        if ( !ref.getGroupId()
                 .matches( gidPattern ) )
        {
            return false;
        }

        final String aidPattern = toWildcard( getArtifactId() );
        return ref.getArtifactId().matches( aidPattern );

    }

    private String toWildcard( final String val )
    {
        return val.replaceAll( "\\.", "\\." )
                  .replaceAll( "\\*", ".*" );
    }

    public PropertyContainer getContainer()
    {
        return container;
    }

    public boolean isDirty()
    {
        return groupId != null || artifactId != null;
    }
}
