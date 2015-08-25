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
package org.commonjava.maven.atlas.graph.traverse;

import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.filter.OrFilter;
import org.commonjava.maven.atlas.graph.filter.ParentFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.*;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleVersionlessArtifactRef;
import org.commonjava.maven.atlas.ident.ref.VersionlessArtifactRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TransitiveDependencyTraversal
    extends AbstractFilteringTraversal
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Map<VersionlessArtifactRef, ArtifactRef> artifacts = new HashMap<VersionlessArtifactRef, ArtifactRef>();

    private final Map<VersionlessArtifactRef, Integer> seenArtifacts = new HashMap<VersionlessArtifactRef, Integer>();

    public TransitiveDependencyTraversal()
    {
        this( DependencyScope.runtime );
    }

    public TransitiveDependencyTraversal( final ProjectRelationshipFilter filter )
    {
        super( filter );
    }

    public TransitiveDependencyTraversal( final DependencyScope scope )
    {
        super( new OrFilter( new DependencyFilter( scope ), ParentFilter.EXCLUDE_TERMINAL_PARENTS ) );
    }

    public List<ArtifactRef> getArtifacts()
    {
        return Collections.unmodifiableList( new ArrayList<ArtifactRef>( artifacts.values() ) );
    }

    @Override
    public boolean shouldTraverseEdge( final ProjectRelationship<?, ?> relationship,
                                       final List<ProjectRelationship<?, ?>> path )
    {
        boolean result = false;
        if ( relationship instanceof DependencyRelationship )
        {
            final ArtifactRef target = (ArtifactRef) relationship.getTarget();
            final VersionlessArtifactRef versionlessTarget = new SimpleVersionlessArtifactRef( target );

            final Integer distance = seenArtifacts.get( versionlessTarget );
            logger.debug( "Checking for seen versionless GA[TC]: {}\nStored distance: {}\nPath distance: {}", versionlessTarget, distance, path.size() );
            if ( distance == null || distance > path.size() )
            {
                logger.debug( "Adding: {} ({})", target, versionlessTarget );
                artifacts.put( versionlessTarget, target );
                seenArtifacts.put( versionlessTarget, path.size() );
                result = true;
            }
        }
        else if ( relationship instanceof ParentRelationship )
        {
            result = true;
        }

        return result;
    }
}
