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

import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.ident.ref.TypeAndClassifier;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class NeoTypeAndClassifier
    implements TypeAndClassifier
{
    private static final long serialVersionUID = 1L;

    private String type;

    private String classifier;

    private Relationship rel;

    private Node node;

    public NeoTypeAndClassifier( final String type, final String classifier )
    {
        this.type = type == null ? "jar" : type;
        this.classifier = isEmpty( classifier ) ? null : classifier;
    }

    public NeoTypeAndClassifier( final String type )
    {
        this( type, null );
    }

    public NeoTypeAndClassifier()
    {
        this( null, null );
    }

    public NeoTypeAndClassifier( Relationship rel )
    {
        this.rel = rel;
    }

    public NeoTypeAndClassifier( Node node )
    {
        this.node = node;
    }

    @Override
    public String getType()
    {
        String t;
        if ( rel == null )
        {
            t = type;
        }
        else
        {
            t = Conversions.getStringProperty( Conversions.TYPE, rel );
        }

        return t == null ? "jar" : t;
    }

    @Override
    public String getClassifier()
    {
        if ( rel == null )
        {
            return classifier;
        }
        else
        {
            return Conversions.getStringProperty( Conversions.CLASSIFIER, rel );
        }
    }

    @Override
    public String toString()
    {
        return String.format( "%s%s", type, ( classifier == null ? "" : ":" + classifier ) );
    }

    @Override
    // FIXME: Expensive!!
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( getClassifier() == null ) ? 0 : getClassifier().hashCode() );
        result = prime * result + ( ( getType() == null ) ? 0 : getType().hashCode() );
        return result;
    }

    @Override
    // FIXME: Expensive!!
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
        if ( !(obj instanceof TypeAndClassifier) )
        {
            return false;
        }
        final TypeAndClassifier other = (TypeAndClassifier) obj;
        if ( getClassifier() == null )
        {
            if ( other.getClassifier() != null )
            {
                return false;
            }
        }
        else if ( !getClassifier().equals( other.getClassifier() ) )
        {
            return false;
        }
        if ( getType() == null )
        {
            if ( other.getType() != null )
            {
                return false;
            }
        }
        else if ( !getType().equals( other.getType() ) )
        {
            return false;
        }
        return true;
    }

    public boolean isDirty()
    {
        return type != null || classifier != null;
    }

}
