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

import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.rel.SimpleDependencyRelationship;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ScopeTransitivity;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyFilter
    extends AbstractTypedFilter
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private final DependencyScope scope;

    private final ScopeTransitivity scopeTransitivity;

    private final Set<ProjectRef> excludes = new HashSet<ProjectRef>();

    private final boolean useImpliedScopes;

    public DependencyFilter()
    {
        this( DependencyScope.test, ScopeTransitivity.maven, false, true, true, null );
    }

    public DependencyFilter( final DependencyScope scope )
    {
        this( scope, ScopeTransitivity.maven, false, true, true, null );
    }

    public DependencyFilter( final DependencyScope scope, final ScopeTransitivity scopeTransitivity,
                             final boolean includeManaged, final boolean includeConcrete, final Set<ProjectRef> excludes )
    {
        this( scope, scopeTransitivity, includeManaged, includeConcrete, true, excludes );
    }

    public DependencyFilter( final DependencyScope scope, final ScopeTransitivity scopeTransitivity,
                             final boolean includeManaged, final boolean includeConcrete,
                             final boolean useImpliedScopes, final Set<ProjectRef> excludes )
    {
        super( RelationshipType.DEPENDENCY, true, includeManaged, includeConcrete );
        this.useImpliedScopes = useImpliedScopes;
        this.scope = scope == null ? DependencyScope.test : scope;
        this.scopeTransitivity = scopeTransitivity;
        if ( excludes != null )
        {
            for ( final ProjectRef pr : excludes )
            {
                this.excludes.add( pr.asProjectRef() );
            }
        }
    }

    @Override
    public boolean doAccept( final ProjectRelationship<?, ?> rel )
    {
//        Logger logger = LoggerFactory.getLogger( getClass() );
//        logger.debug( "CHECK accept: {}", rel);
        final DependencyRelationship dr = (DependencyRelationship) rel;
        if ( RelationshipUtils.isExcluded( dr.getTarget(), excludes ) )
        {
//            logger.debug( "NO (excluded)" );
            return false;
        }

        if ( scope != null )
        {
            if ( useImpliedScopes && !scope.implies( dr.getScope() ) )
            {
//                logger.debug( "NO (wrong implied scope)" );
                return false;
            }
            else if ( !useImpliedScopes && scope != dr.getScope() )
            {
//                logger.debug( "NO (wrong direct scope)" );
                return false;
            }
        }

        if ( !includeManagedRelationships() && dr.isManaged() )
        {
//            logger.debug( "NO (excluding managed)" );
            return false;
        }
        else if ( !includeConcreteRelationships() && !dr.isManaged() )
        {
//            logger.debug( "NO (excluding concrete)" );
            return false;
        }

//        logger.debug( "YES" );
        return true;
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?, ?> parent )
    {
        DependencyRelationship dr = null;
        if ( ( parent instanceof DependencyRelationship ) )
        {
            dr = (DependencyRelationship) parent;
        }

        final DependencyScope nextScope = scopeTransitivity.getChildFor( scope );
        Set<ProjectRef> newExcludes = dr == null ? null : dr.getExcludes();
        if ( nextScope != scope || ( newExcludes != null && !newExcludes.isEmpty() ) )
        {
            if ( excludes != null )
            {
                final Set<ProjectRef> ex = new HashSet<ProjectRef>( excludes );

                if ( newExcludes != null && !newExcludes.isEmpty() )
                {
                    ex.addAll( newExcludes );
                }

                newExcludes = ex;
            }

            return new DependencyFilter( nextScope, scopeTransitivity, includeManagedRelationships(),
                                         includeConcreteRelationships(), useImpliedScopes, newExcludes );
        }

        return this;
    }

    public boolean isUseImpliedScopes()
    {
        return useImpliedScopes;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( excludes == null ) ? 0 : excludes.hashCode() );
        result = prime * result + ( ( scope == null ) ? 0 : scope.hashCode() );
        result = prime * result + ( ( scopeTransitivity == null ) ? 0 : scopeTransitivity.hashCode() );
        result = prime * result + ( useImpliedScopes ? 1231 : 1237 );
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
        final DependencyFilter other = (DependencyFilter) obj;
        if ( excludes == null )
        {
            if ( other.excludes != null )
            {
                return false;
            }
        }
        else if ( !excludes.equals( other.excludes ) )
        {
            return false;
        }
        if ( scope != other.scope )
        {
            return false;
        }
        if ( scopeTransitivity != other.scopeTransitivity )
        {
            return false;
        }
        return useImpliedScopes == other.useImpliedScopes;
    }

    @Override
    protected void renderIdAttributes( final StringBuilder sb )
    {
        sb.append( ", scope:" );
        sb.append( scope.realName() );
        sb.append( ", transitivity:" )
          .append( scopeTransitivity.name() );
        sb.append( ", implied-scopes:" )
          .append( isUseImpliedScopes() );

        if ( excludes != null && !excludes.isEmpty() )
        {
            sb.append( ", excludes:{" );
            boolean first = true;
            for ( final ProjectRef exclude : excludes )
            {
                if ( !first )
                {
                    sb.append( ',' );
                }

                first = false;
                sb.append( exclude );
            }

            sb.append( "}" );
        }
    }

}
