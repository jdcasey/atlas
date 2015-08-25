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

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.rel.SimpleExtensionRelationship;
import org.commonjava.maven.atlas.ident.DependencyScope;

// TODO: Do we need to consider excludes in the extensions?
public class ExtensionFilter
    extends AbstractTypedFilter
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public ExtensionFilter()
    {
        super( RelationshipType.EXTENSION, RelationshipType.DEPENDENCY, false, true );
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?, ?> parent )
    {
        if ( parent instanceof SimpleExtensionRelationship )
        {
            return new OrFilter( new DependencyFilter( DependencyScope.runtime ), ParentFilter.EXCLUDE_TERMINAL_PARENTS );
        }
        else
        {
            return NoneFilter.INSTANCE;
        }
    }

}
