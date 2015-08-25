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
package org.commonjava.maven.atlas.graph.spi.jung;

import static org.apache.commons.lang.StringUtils.join;

import java.util.ArrayList;
import java.util.List;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.AbstractTraversal;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CycleDetectionTraversal
    extends AbstractTraversal
{
    private final List<EProjectCycle> cycles = new ArrayList<EProjectCycle>();

    private final ProjectRelationship<?, ?> rel;

    CycleDetectionTraversal( final ProjectRelationship<?, ?> rel )
    {
        this.rel = rel;
    }

    public List<EProjectCycle> getCycles()
    {
        return cycles;
    }

    @Override
    public boolean preCheck( final ProjectRelationship<?, ?> relationship, final List<ProjectRelationship<?, ?>> path )
    {
        if ( rel.getDeclaring()
                .equals( rel.getTarget()
                            .asProjectVersionRef() ) )
        {
            return false;
        }

        final Logger logger = LoggerFactory.getLogger( getClass() );

        logger.debug( "Checking for cycle: {}\n\nPath: {}\n\n", relationship, new JoinString( "\n", path ) );

        final ProjectVersionRef from = rel.getDeclaring();
        if ( from.equals( relationship.getTarget()
                                      .asProjectVersionRef() ) )
        {
            final List<ProjectRelationship<?, ?>> cycle = new ArrayList<ProjectRelationship<?, ?>>( path );
            cycle.add( rel );

            cycles.add( new EProjectCycle( cycle ) );

            logger.warn( "CYCLE: {}", join( cycle, ", " ) );
            return false;
        }

        return true;
    }
}
