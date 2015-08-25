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
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class DependencyTreeRelationshipPrinter
    implements StructureRelationshipPrinter
{

    private final Set<ProjectVersionRef> missing;

    public DependencyTreeRelationshipPrinter()
    {
        missing = null;
    }

    public DependencyTreeRelationshipPrinter( final Set<ProjectVersionRef> missing )
    {
        this.missing = missing;
    }

    @Override
    public void print( final ProjectRelationship<?, ?> relationship, final ProjectVersionRef selectedTarget,
                       final PrintWriter writer, final Map<String, Set<ProjectVersionRef>> labels, final int depth,
                       final String indent )
    {
        indent( writer, depth, indent );

        final RelationshipType type = relationship.getType();

        final ProjectVersionRef originalTarget = relationship.getTarget()
                                                             .asProjectVersionRef();

        ProjectVersionRef target = null;
        ArtifactRef targetArtifact = relationship.getTargetArtifact();

        if ( selectedTarget == null )
        {
            target = originalTarget;
        }
        else
        {
            target = selectedTarget;
            targetArtifact = selectedTarget.asArtifactRef( targetArtifact.getTypeAndClassifier() );
        }

        final Set<String> localLabels = new HashSet<String>();

        String suffix = null;
        if ( type == RelationshipType.DEPENDENCY )
        {
            final DependencyRelationship dr = (DependencyRelationship) relationship;
            suffix = ":" + dr.getScope()
                             .name();

            if ( dr.getTargetArtifact()
                   .isOptional() )
            {
                localLabels.add( "OPTIONAL" );
            }

            //            writer.print( " [idx: " )
            //                   .append( relationship.getIndex() )
            //                   .append( ']' );
        }
        else
        {
            localLabels.add( type.name() );
        }

        printProjectVersionRef( targetArtifact, writer, suffix, labels, localLabels );

        if ( !target.equals( originalTarget ) )
        {
            writer.print( " [was: " );
            writer.print( originalTarget );
            writer.print( "]" );
        }

        if ( missing != null && missing.contains( target ) )
        {
            writer.print( '\n' );
            indent( writer, depth + 1, indent );
            writer.print( "???" );
        }
    }

    @Override
    public void printProjectVersionRef( final ProjectVersionRef targetArtifact, final PrintWriter writer,
                                        final String targetSuffix, final Map<String, Set<ProjectVersionRef>> labels,
                                        final Set<String> localLabels )
    {
        // the original could be an artifact ref!
        final ProjectVersionRef target = targetArtifact.asProjectVersionRef();

        writer.print( targetArtifact );
        if ( targetSuffix != null )
        {
            writer.print( targetSuffix );
        }

        boolean hasLabel = false;
        if ( localLabels != null && !localLabels.isEmpty() )
        {
            hasLabel = true;
            writer.print( " (" );

            boolean first = true;
            for ( final String label : localLabels )
            {
                if ( first )
                {
                    first = false;
                }
                else
                {
                    writer.print( ", " );
                }

                writer.print( label );
            }
        }

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

    private void indent( final PrintWriter writer, final int depth, final String indent )
    {
        for ( int i = 0; i < depth; i++ )
        {
            writer.print( indent );
        }
    }
}
