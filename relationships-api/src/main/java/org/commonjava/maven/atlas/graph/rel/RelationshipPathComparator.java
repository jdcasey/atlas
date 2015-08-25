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
package org.commonjava.maven.atlas.graph.rel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

public class RelationshipPathComparator
    implements Comparator<List<ProjectRelationship<?, ?>>>
{

    public static RelationshipPathComparator INSTANCE = new RelationshipPathComparator();

    private final RelationshipComparator comp = RelationshipComparator.INSTANCE;

    private RelationshipPathComparator()
    {
    }

    @Override
    public int compare( final List<ProjectRelationship<?, ?>> one, final List<ProjectRelationship<?, ?>> two )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug("Comparing:\nOne: {}\nTwo: {}", one, two);

        final int commonLen = Math.min( one.size(), two.size() );

        if ( one.size() > commonLen )
        {
            return 1;
        }
        else if ( two.size() > commonLen )
        {
            return -1;
        }

        for ( int i = 0; i < commonLen; i++ )
        {
            final int result = compareRelTypes( one.get( i ), two.get( i ) );
            if ( result != 0 )
            {
                return result;
            }
        }

        for ( int i = 0; i < commonLen; i++ )
        {
            final int result = compareRels( one.get( i ), two.get( i ) );
            if ( result != 0 )
            {
                return result;
            }
        }

        return 0;
    }

    private int compareRels( final ProjectRelationship<?, ?> one, final ProjectRelationship<?, ?> two )
    {
        return comp.compare( one, two );
    }

    private int compareRelTypes( final ProjectRelationship<?, ?> one, final ProjectRelationship<?, ?> two )
    {
        return one.getType()
                  .ordinal() - two.getType()
                                  .ordinal();
    }

}
