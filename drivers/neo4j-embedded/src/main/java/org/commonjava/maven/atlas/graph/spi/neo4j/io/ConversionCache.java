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
package org.commonjava.maven.atlas.graph.spi.neo4j.io;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class ConversionCache
{

    private Map<Long, WeakReference<ProjectRelationship<?, ?>>> relationships;

    private Map<Long, WeakReference<ProjectVersionRef>> gavs;

    private Map<String, WeakReference<Object>> serializedObjects;

    public ProjectRelationship<?, ?> getRelationship( final Relationship rel )
    {
        return getRelationship( rel.getId() );
    }

    public ProjectRelationship<?, ?> getRelationship( final long rid )
    {
        if ( relationships == null )
        {
            return null;
        }

        final WeakReference<ProjectRelationship<?, ?>> reference = relationships.get( rid );
        if ( reference == null )
        {
            return null;
        }

        return reference.get();
    }

    public void cache( final Relationship rel, final ProjectRelationship<?, ?> r )
    {
        if ( relationships == null )
        {
            relationships = new HashMap<Long, WeakReference<ProjectRelationship<?, ?>>>();
        }

        relationships.put( rel.getId(), new WeakReference<ProjectRelationship<?, ?>>( r ) );
    }

    public ProjectVersionRef getProjectVersionRef( final Node node )
    {
        return getProjectVersionRef( node.getId() );
    }

    public ProjectVersionRef getProjectVersionRef( final long nid )
    {
        if ( gavs == null )
        {
            return null;
        }

        final WeakReference<ProjectVersionRef> reference = gavs.get( nid );
        if ( reference == null )
        {
            return null;
        }

        return reference.get();
    }

    public void cache( final Node node, final ProjectVersionRef ref )
    {
        if ( gavs == null )
        {
            gavs = new HashMap<Long, WeakReference<ProjectVersionRef>>();
        }

        gavs.put( node.getId(), new WeakReference<ProjectVersionRef>( ref ) );
    }

    public <T> T getSerializedObject( final byte[] data, final Class<T> type )
    {
        if ( serializedObjects != null )
        {
            final String key = DigestUtils.shaHex( data );
            final WeakReference<Object> reference = serializedObjects.get( key );
            if ( reference == null )
            {
                return null;
            }

            final Object value = reference.get();
            if ( value != null )
            {
                return type.cast( value );
            }
        }

        return null;
    }

    public void cache( final byte[] data, final Object value )
    {
        if ( serializedObjects == null )
        {
            serializedObjects = new HashMap<String, WeakReference<Object>>();
        }

        final String key = DigestUtils.shaHex( data );
        serializedObjects.put( key, new WeakReference<Object>( value ) );
    }
}
