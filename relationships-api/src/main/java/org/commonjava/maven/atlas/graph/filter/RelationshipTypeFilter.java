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

import java.util.Collection;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;

public class RelationshipTypeFilter
    extends AbstractTypedFilter
{

    private static final long serialVersionUID = 1L;

    public RelationshipTypeFilter( final Collection<RelationshipType> types, final boolean includeManagedInfo,
                                   final boolean includeConcreteInfo )
    {
        super( types, types, includeManagedInfo, includeManagedInfo );
    }

    public RelationshipTypeFilter( final Collection<RelationshipType> types,
                                   final Collection<RelationshipType> descendantTypes,
                                   final boolean includeManagedInfo, final boolean includeConcreteInfo )
    {
        super( types, descendantTypes, includeManagedInfo, includeConcreteInfo );
    }

    public RelationshipTypeFilter( final RelationshipType type, final boolean hasDescendants,
                                   final boolean includeManagedInfo, final boolean includeConcreteInfo )
    {
        super( type, hasDescendants, includeManagedInfo, includeConcreteInfo );
    }

    public RelationshipTypeFilter( final RelationshipType type, final Collection<RelationshipType> descendantTypes,
                                   final boolean includeManagedInfo, final boolean includeConcreteInfo )
    {
        super( type, descendantTypes, includeManagedInfo, includeConcreteInfo );
    }

    public RelationshipTypeFilter( final RelationshipType type, final RelationshipType descendantType,
                                   final boolean includeManagedInfo, final boolean includeConcreteInfo )
    {
        super( type, descendantType, includeManagedInfo, includeConcreteInfo );
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?, ?> parent )
    {
        final Set<RelationshipType> descendantTypes = getDescendantRelationshipTypes();
        final Set<RelationshipType> allowedTypes = getAllowedTypes();
        if ( !allowedTypes.equals( descendantTypes ) )
        {
            return new RelationshipTypeFilter( descendantTypes, includeManagedRelationships(),
                                               includeConcreteRelationships() );
        }

        return this;
    }

}
