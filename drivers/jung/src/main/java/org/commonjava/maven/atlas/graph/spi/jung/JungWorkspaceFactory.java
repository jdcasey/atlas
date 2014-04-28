/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.atlas.graph.spi.jung;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.spi.GraphWorkspaceFactory;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;

public class JungWorkspaceFactory
    implements GraphWorkspaceFactory
{

    private final Map<String, GraphWorkspace> workspaces = new HashMap<String, GraphWorkspace>();

    @Override
    public GraphWorkspace createWorkspace( final String id, final GraphWorkspaceConfiguration config )
        throws RelationshipGraphConnectionException
    {
        if ( workspaces.containsKey( id ) )
        {
            throw new RelationshipGraphConnectionException( "Workspace already exists: %s. Cannot create workspace.", id );
        }

        final GraphWorkspace ws = new GraphWorkspace( id, new JungEGraphDriver( config ) );
        workspaces.put( ws.getId(), ws );
        return ws;
    }

    @Override
    public GraphWorkspace createWorkspace( final GraphWorkspaceConfiguration config )
        throws RelationshipGraphConnectionException
    {
        final GraphWorkspace ws = new GraphWorkspace( Long.toString( System.currentTimeMillis() ), new JungEGraphDriver( config ) );
        workspaces.put( ws.getId(), ws );
        return ws;
    }

    @Override
    public boolean deleteWorkspace( final String id )
    {
        return workspaces.remove( id ) != null;
    }

    @Override
    public void storeWorkspace( final GraphWorkspace workspace )
        throws RelationshipGraphConnectionException
    {
        // currently just stored in memory...
    }

    @Override
    public GraphWorkspace loadWorkspace( final String id )
        throws RelationshipGraphConnectionException
    {
        return workspaces.get( id );
    }

    @Override
    public Set<GraphWorkspace> loadAllWorkspaces( final Set<String> excluded )
    {
        final Set<GraphWorkspace> result = new HashSet<GraphWorkspace>( workspaces.values() );
        for ( final Iterator<GraphWorkspace> it = result.iterator(); it.hasNext(); )
        {
            final GraphWorkspace ws = it.next();
            if ( excluded.contains( ws.getId() ) )
            {
                it.remove();
            }
        }

        return result;
    }

}
