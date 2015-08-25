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

import java.util.Collections;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;

public class NoneFilter
    implements ProjectRelationshipFilter
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static final NoneFilter INSTANCE = new NoneFilter();

    private NoneFilter()
    {
    }

    @Override
    public boolean accept( final ProjectRelationship<?, ?> rel )
    {
        return false;
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?, ?> parent )
    {
        return this;
    }

    @Override
    public String getLongId()
    {
        return "NONE";
    }

    @Override
    public String toString()
    {
        return getLongId();
    }

    @Override
    public boolean equals( final Object obj )
    {
        return obj instanceof NoneFilter;
    }

    @Override
    public int hashCode()
    {
        return NoneFilter.class.hashCode() + 1;
    }

    @Override
    public String getCondensedId()
    {
        return getLongId();
    }

    @Override
    public boolean includeManagedRelationships()
    {
        return false;
    }

    @Override
    public boolean includeConcreteRelationships()
    {
        return false;
    }

    @Override
    public Set<RelationshipType> getAllowedTypes()
    {
        return Collections.emptySet();
    }

}
