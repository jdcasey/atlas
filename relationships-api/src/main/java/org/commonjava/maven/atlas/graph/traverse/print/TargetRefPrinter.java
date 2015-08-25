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
package org.commonjava.maven.atlas.graph.traverse.print;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public final class TargetRefPrinter
    implements StructureRelationshipPrinter
{

    @Override
    public void print( final ProjectRelationship<?, ?> relationship, final ProjectVersionRef selectedTarget,
                       final PrintWriter writer, final Map<String, Set<ProjectVersionRef>> labels, final int depth,
                       final String indent )
    {
        for ( int i = 0; i < depth; i++ )
        {
            writer.print( indent );
        }

        final ProjectVersionRef originalTarget = relationship.getTarget()
                                                             .asProjectVersionRef();
        final ProjectVersionRef target = selectedTarget == null ? originalTarget : selectedTarget;

        printProjectVersionRef( target, writer, null, labels, null );

        if ( target != originalTarget )
        {
            writer.print( " [was: " );
            writer.print( originalTarget );
            writer.print( "]" );
        }
    }

    @Override
    public void printProjectVersionRef( final ProjectVersionRef target, final PrintWriter writer,
                                        final String targetSuffix, final Map<String, Set<ProjectVersionRef>> labels,
                                        final Set<String> localLabels )
    {
        writer.print( target );
        if ( targetSuffix != null )
        {
            writer.print( targetSuffix );
        }

        boolean hasLabel = false;
        for ( final Entry<String, Set<ProjectVersionRef>> entry : labels.entrySet() )
        {
            final String label = entry.getKey();
            final Set<ProjectVersionRef> refs = entry.getValue();

            if ( refs.contains( target ) )
            {
                if ( !hasLabel )
                {
                    hasLabel = true;
                    writer.print( " (" );
                }
                else
                {
                    writer.print( ", " );
                }

                writer.print( label );
            }

        }

        if ( hasLabel )
        {
            writer.print( ')' );
        }
    }

}
