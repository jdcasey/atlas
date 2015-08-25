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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.SimpleDependencyRelationship;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class ListPrinter
{

    //    private final Logger logger = new Logger( getClass() );

    private final StructureRelationshipPrinter relationshipPrinter;

    //    private final Set<ProjectRef> seen = new HashSet<ProjectRef>();

    public ListPrinter()
    {
        this.relationshipPrinter = new TargetRefPrinter();
    }

    public ListPrinter( final StructureRelationshipPrinter relationshipPrinter )
    {
        this.relationshipPrinter = relationshipPrinter;
    }

    public void printStructure( final ProjectVersionRef from,
                                final Map<ProjectVersionRef, List<ProjectRelationship<?, ?>>> links,
                                final Map<String, Set<ProjectVersionRef>> labels, PrintWriter writer )
    {
        printStructure( from, links, null, null, labels, writer );
    }

    public void printStructure( final ProjectVersionRef from,
                                final Map<ProjectVersionRef, List<ProjectRelationship<?, ?>>> links, final String header,
                                final String footer, final Map<String, Set<ProjectVersionRef>> labels,
                                PrintWriter writer )
    {
        if ( header != null )
        {
            writer.print( header );
        }

        writer.print( "  " );
        relationshipPrinter.printProjectVersionRef( from, writer, null, labels, null );
        //      writer.print( from );

        final Set<String> lines = new LinkedHashSet<String>();

        printLinks( from, lines, links, labels, new HashSet<ProjectRef>(), new Stack<ProjectVersionRef>() );

        final List<String> sorted = new ArrayList<String>( lines );
        Collections.sort( sorted );

        for ( final String line : sorted )
        {
            writer.print( "\n  " );
            writer.print( line );
        }
        writer.print( "\n" );

        if ( footer != null )
        {
            writer.print( footer );
        }
    }

    private void printLinks( final ProjectVersionRef from, final Set<String> lines,
                             final Map<ProjectVersionRef, List<ProjectRelationship<?, ?>>> links,
                             final Map<String, Set<ProjectVersionRef>> labels, final Set<ProjectRef> excluded,
                             final Stack<ProjectVersionRef> inPath )
    {
        inPath.push( from );

        final List<ProjectRelationship<?, ?>> outbound = links.get( from );
        if ( outbound != null )
        {
            StringWriter sw;
            for ( final ProjectRelationship<?, ?> out : outbound )
            {
                sw = new StringWriter();

                ProjectVersionRef outRef = out.getTarget()
                                              .asProjectVersionRef();
                if ( inPath.contains( outRef ) )
                {
                    continue;
                }

                if ( excluded.contains( outRef ) )
                {
                    continue;
                }
                // TODO: Reinstate transitive collapse IF we can find a way to make output consistent.
                //                else if ( !seen.add( out.getTarget()
                //                                        .asProjectRef() ) )
                //                {
                //                    return;
                //                }

                relationshipPrinter.print( out, null, new PrintWriter( sw ), labels, 0, "" );
                lines.add( sw.toString() );

                if ( !from.equals( out.getTarget()
                                      .asProjectRef() ) )
                {
                    Set<ProjectRef> newExcluded = null;
                    if ( out instanceof SimpleDependencyRelationship )
                    {
                        final Set<ProjectRef> excludes = ( (DependencyRelationship) out ).getExcludes();
                        if ( excludes != null && !excludes.isEmpty() )
                        {
                            newExcluded = new HashSet<ProjectRef>();
                            for ( final ProjectRef ref : excludes )
                            {
                                if ( !RelationshipUtils.isExcluded( ref, excluded ) )
                                {
                                    newExcluded.add( ref );
                                    excluded.add( ref );
                                }
                            }
                        }
                    }

                    printLinks( out.getTarget()
                                   .asProjectVersionRef(), lines, links, labels, excluded, inPath );

                    if ( newExcluded != null && !newExcluded.isEmpty() )
                    {
                        excluded.removeAll( newExcluded );
                    }
                }
            }
        }

        inPath.pop();
    }

}
