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
package org.commonjava.maven.atlas.ident.ref;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Reference to a whole project (or module, in terms of Maven builds). This reference is not specific to a release of the project (see {@link SimpleProjectVersionRef}).
 * 
 * @author jdcasey
 */
public class SimpleProjectRef
    implements ProjectRef
{

    private static final long serialVersionUID = 1L;

    // NEVER null
    private final String groupId;

    // NEVER null
    private final String artifactId;

    public SimpleProjectRef( final String groupId, final String artifactId )
    {
        if ( isEmpty( groupId ) || isEmpty( artifactId ) )
        {
            throw new InvalidRefException( "ProjectId must contain non-empty groupId AND artifactId. (Given: '"
                + groupId + "':'" + artifactId + "')" );
        }

        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public static ProjectRef parse( final String ga )
    {
        final String[] parts = ga.split( ":" );
        if ( parts.length < 2 || isEmpty( parts[0] ) || isEmpty( parts[1] ) )
        {
            throw new InvalidRefException( "ProjectRef must contain non-empty groupId AND artifactId. (Given: '" + ga
                + "')" );
        }

        return new SimpleProjectRef( parts[0], parts[1] );
    }

    @Override
    public final String getGroupId()
    {
        return groupId;
    }

    @Override
    public final String getArtifactId()
    {
        return artifactId;
    }

    @Override
    public ProjectRef asProjectRef()
    {
        return SimpleProjectRef.class.equals( getClass() ) ? this : new SimpleProjectRef( getGroupId(), getArtifactId() );
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
        return new SimpleVersionlessArtifactRef( this, type, classifier, optional );
    }

    @Override
    public VersionlessArtifactRef asVersionlessArtifactRef( final TypeAndClassifier tc )
    {
        return asVersionlessArtifactRef( tc, false );
    }

    @Override
    public VersionlessArtifactRef asVersionlessArtifactRef( final TypeAndClassifier tc, final boolean optional )
    {
        return new SimpleVersionlessArtifactRef( this, tc, optional );
    }

    @Override
    public String toString()
    {
        return String.format( "%s:%s", groupId, artifactId );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + artifactId.hashCode();
        result = prime * result + groupId.hashCode();
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
        if ( !artifactId.equals( other.getArtifactId() ) )
        {
            return false;
        }
        return groupId.equals( other.getGroupId() );
    }

    public int compareTo( final ProjectRef o )
    {
        int comp = groupId.compareTo( o.getGroupId() );
        if ( comp == 0 )
        {
            comp = artifactId.compareTo( o.getArtifactId() );
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

}
