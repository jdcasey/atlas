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
package org.commonjava.maven.atlas.graph.filter;

import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;

public class ParentFilter
    extends AbstractTypedFilter
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static final ParentFilter EXCLUDE_TERMINAL_PARENTS = new ParentFilter( false );

    public static final ParentFilter INCLUDE_TERMINAL_PARENTS = new ParentFilter( true );

    private final boolean allowTerminalParent;

    private ParentFilter( final boolean allowTerminalParent )
    {
        super( RelationshipType.PARENT, true, false, true );
        this.allowTerminalParent = allowTerminalParent;
    }

    @Override
    public boolean doAccept( final ProjectRelationship<?, ?> rel )
    {
        return allowTerminalParent || !( (ParentRelationship) rel ).isTerminus();

    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?, ?> parent )
    {
        return this;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( allowTerminalParent ? 1231 : 1237 );
        return result;
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
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final ParentFilter other = (ParentFilter) obj;
        return allowTerminalParent == other.allowTerminalParent;
    }

    @Override
    protected void renderIdAttributes( final StringBuilder sb )
    {
        sb.append( ",terminalParent:" )
          .append( allowTerminalParent );
    }

}
