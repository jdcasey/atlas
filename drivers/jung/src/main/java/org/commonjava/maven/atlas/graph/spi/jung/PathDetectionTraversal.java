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
package org.commonjava.maven.atlas.graph.spi.jung;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;
import org.commonjava.maven.atlas.graph.spi.jung.model.JungGraphPath;
import org.commonjava.maven.atlas.graph.traverse.AbstractTraversal;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;

final class PathDetectionTraversal
    extends AbstractTraversal
{
    //        private final Logger logger = new Logger( getClass() );

    private final Set<ProjectVersionRef> to;

    private final Map<JungGraphPath, GraphPathInfo> pathMap = new HashMap<JungGraphPath, GraphPathInfo>();

    private final Set<JungGraphPath> paths = new HashSet<JungGraphPath>();

    private final ViewParams params;

    private final RelationshipGraphConnection connection;

    PathDetectionTraversal( final RelationshipGraphConnection connection, final ViewParams params,
                            final ProjectVersionRef[] refs )
    {
        this.connection = connection;
        this.params = params;
        this.to = new HashSet<ProjectVersionRef>( Arrays.asList( refs ) );
    }

    public PathDetectionTraversal( final RelationshipGraphConnection connection, final ViewParams params,
                                   final Set<ProjectVersionRef> refs )
    {
        this.connection = connection;
        this.params = params;
        this.to = refs;
    }

    public Map<JungGraphPath, GraphPathInfo> getPathMap()
    {
        return pathMap;
    }

    public Set<JungGraphPath> getPaths()
    {
        return paths;
    }

    @Override
    public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path )
    {
        JungGraphPath jpath;
        GraphPathInfo pathInfo;
        if ( path.isEmpty() )
        {
            jpath = new JungGraphPath( relationship.getDeclaring() );
            pathInfo = new GraphPathInfo( connection, params );
        }
        else
        {
            jpath = new JungGraphPath( path );
            pathInfo = pathMap.get( jpath );
        }

        if ( pathInfo == null )
        {
            return false;
        }

        final ProjectRelationship<?> selected = pathInfo.selectRelationship( relationship, jpath );
        if ( selected == null )
        {
            return false;
        }

        jpath = new JungGraphPath( jpath, selected );
        pathInfo = pathInfo.getChildPathInfo( relationship );

        pathMap.put( jpath, pathInfo );

        final ProjectVersionRef target = selected.getTarget()
                                                 .asProjectVersionRef();

        // logger.info( "Checking path: %s to see if target: %s is in endpoints: %s", join( path, "," ), target, join( to, ", " ) );
        boolean found = false;
        for ( final ProjectVersionRef t : to )
        {
            if ( t.equals( target ) )
            {
                paths.add( jpath );
                // logger.info( "+= %s", join( path, ", " ) );
                found = true;
            }
        }

        return !found;
    }
}
