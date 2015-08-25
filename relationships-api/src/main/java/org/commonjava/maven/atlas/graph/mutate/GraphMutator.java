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

import java.io.Serializable;

import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;

/**
 * Mechanism for selecting alternative relationships (especially by changing the
 * target artifact/project), usually making use of version-management information
 * injected by the user or accumulated during graph traversal.
 * 
 * Child instances allow for shifts in mutator logic according to encounters with 
 * different types of relationships. They also allow incorporation of new 
 * version-management information by embedding new instances of 
 * {@link VersionManager}.
 * 
 * @author jdcasey
 */
public interface GraphMutator
    extends Serializable
{

    /**
     * Alter the relationship to be traversed next, using whatever mutation logic
     * this class incorporates. For example, managed versions (a la &lt;dependencyManagement/&gt;)
     * 
     * @param rel The relationship to process.
     * @param path The path leading to this selection, which may contain management information, etc. 
     * @param connection The db connection for this graph
     * @param params The parameters for the context "view" of this operation
     * @return The alternative relationship, or the given one if no mutation 
     * takes place.
     */
    ProjectRelationship<?, ?> selectFor( ProjectRelationship<?, ?> rel, GraphPath<?> path,
                                      RelationshipGraphConnection connection, ViewParams params );

    /**
     * If necessary, create a new mutator instance to handle the next wave of 
     * relationships resulting from traversal of the given relationship.
     * 
     * @param rel The relationship that will be traversed next, for which mutator logic is needed.
     * @param connection The db connection for this graph
     * @param params The parameters for the context "view" of this operation
     * 
     * @return  This instance WHEREVER POSSIBLE, or a new mutator instance to 
     * encapsulate changing logic or metadata. NEVER Null. Decisions about 
     * whether to proceed should be handled via {@link ProjectRelationshipFilter}, 
     * not here.
     */
    GraphMutator getMutatorFor( ProjectRelationship<?, ?> rel, RelationshipGraphConnection connection, ViewParams params );

    /**
     * Retrieve a human-readable string that uniquely identifies the logic in this mutator, 
     * along with any state stored in this instance.
     */
    String getLongId();

    /**
     * Retrieve a condensed version of the human-readable identity given in {@link #getLongId()}.
     * If the human-readable identity is sufficiently short (eg. "ANY"), then no
     * hashing is required.
     */
    String getCondensedId();

}
