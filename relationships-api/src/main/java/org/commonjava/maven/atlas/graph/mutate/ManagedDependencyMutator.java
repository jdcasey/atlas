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
package org.commonjava.maven.atlas.graph.mutate;

import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class ManagedDependencyMutator
    extends VersionManagerMutator
    implements GraphMutator
{

    private static final long serialVersionUID = 1L;

    @Override
    public ProjectRelationship<?, ?> selectFor( final ProjectRelationship<?, ?> rel, final GraphPath<?> path,
                                             final RelationshipGraphConnection connection, final ViewParams params )
    {
        if ( rel.getType() != RelationshipType.DEPENDENCY ) // TODO: BOM types??
        {
            //            logger.debug( "No selections for relationships of type: {}", rel.getType() );
            return rel;
        }

        ProjectRelationship<?, ?> mutated = super.selectFor( rel, path, connection, params );
        if ( mutated == null || mutated == rel )
        {
            final ProjectVersionRef managed =
                connection
                                                  .getManagedTargetFor( rel.getTarget(), path, RelationshipType.DEPENDENCY );
            if ( managed != null )
            {
                mutated = rel.selectTarget( managed );
            }
        }

        return mutated == null ? rel : mutated;
    }
}
