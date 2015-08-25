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

import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;

public class PluginOnlyFilter
    extends AbstractTypedFilter
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public PluginOnlyFilter()
    {
        this( false, true );
    }

    public PluginOnlyFilter( final boolean includeManaged, final boolean includeConcrete )
    {
        super( RelationshipType.PLUGIN, false, includeManaged, includeConcrete );
    }

    @Override
    public boolean doAccept( final ProjectRelationship<?, ?> rel )
    {
        final PluginRelationship pr = (PluginRelationship) rel;
        if ( includeManagedRelationships() && pr.isManaged() )
        {
            return true;
        }
        else if ( includeConcreteRelationships() && !pr.isManaged() )
        {
            return true;
        }

        return false;
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?, ?> parent )
    {
        return NoneFilter.INSTANCE;
    }

}
