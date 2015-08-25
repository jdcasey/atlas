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

import static org.apache.commons.lang.StringUtils.join;

import java.util.Arrays;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;

public class DependencyOnlyFilter
    extends AbstractTypedFilter
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    // if unspecified, include all dependencies.
    private final DependencyScope[] scopes;

    private final boolean useImpliedScope;

    public DependencyOnlyFilter()
    {
        this( false, true, true, DependencyScope.test );
    }

    public DependencyOnlyFilter( final DependencyScope... scopes )
    {
        this( false, true, true, scopes );
    }

    public DependencyOnlyFilter( final boolean includeManaged, final boolean includeConcrete,
                                 final boolean useImpliedScope, final DependencyScope... scopes )
    {
        super( RelationshipType.DEPENDENCY, false, includeManaged, includeConcrete );

        this.scopes = scopes;
        this.useImpliedScope = useImpliedScope;
    }

    @Override
    public boolean doAccept( final ProjectRelationship<?, ?> rel )
    {
        if ( this.scopes == null || this.scopes.length < 1 )
        {
            return true;
        }

        final DependencyRelationship dr = (DependencyRelationship) rel;
        final DependencyScope scope = dr.getScope();
        for ( final DependencyScope s : this.scopes )
        {
            if ( scope == s || ( useImpliedScope && s.implies( scope ) ) )
            {
                if ( ( dr.isManaged() && includeManagedRelationships() )
                    || ( !dr.isManaged() && includeConcreteRelationships() ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?, ?> parent )
    {
        return NoneFilter.INSTANCE;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode( scopes );
        result = prime * result + ( useImpliedScope ? 1231 : 1237 );
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
        final DependencyOnlyFilter other = (DependencyOnlyFilter) obj;
        if ( !Arrays.equals( scopes, other.scopes ) )
        {
            return false;
        }
        return useImpliedScope == other.useImpliedScope;
    }

    @Override
    protected void renderIdAttributes( final StringBuilder sb )
    {
        sb.append( ",scopes:{" )
          .append( join( scopes, "," ) )
          .append( ",implied-scopes: " )
          .append( useImpliedScope );
    }

}
