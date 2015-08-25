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

public class SimpleTypeAndClassifier
    implements TypeAndClassifier
{
    private static final long serialVersionUID = 1L;

    private final String type;

    private final String classifier;

    public SimpleTypeAndClassifier( final String type, final String classifier )
    {
        this.type = type == null ? "jar" : type;
        this.classifier = isEmpty( classifier ) ? null : classifier;
    }

    public SimpleTypeAndClassifier( final String type )
    {
        this( type, null );
    }

    public SimpleTypeAndClassifier()
    {
        this( null, null );
    }

    @Override
    public String getType()
    {
        return type == null ? "jar" : type;
    }

    @Override
    public String getClassifier()
    {
        return classifier;
    }

    @Override
    public String toString()
    {
        return String.format( "%s%s", type, ( classifier == null ? "" : ":" + classifier ) );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( classifier == null ) ? 0 : classifier.hashCode() );
        result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
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
        if ( !(obj instanceof TypeAndClassifier) )
        {
            return false;
        }
        final TypeAndClassifier other = (TypeAndClassifier) obj;
        if ( classifier == null )
        {
            if ( other.getClassifier() != null )
            {
                return false;
            }
        }
        else if ( !classifier.equals( other.getClassifier() ) )
        {
            return false;
        }
        if ( type == null )
        {
            if ( other.getType() != null )
            {
                return false;
            }
        }
        else if ( !type.equals( other.getType() ) )
        {
            return false;
        }
        return true;
    }

}
