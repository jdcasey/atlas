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
package org.commonjava.maven.atlas.graph.spi;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.traverse.RelationshipGraphTraversal;
import org.commonjava.maven.atlas.graph.traverse.TraversalType;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public interface RelationshipGraphConnection
{

    /* 
     * #########################
     * Mutations are viewless
     * #########################
     */

    boolean addCycle( EProjectCycle cycle )
        throws RelationshipGraphConnectionException;

    void addDisconnectedProject( ProjectVersionRef ref )
        throws RelationshipGraphConnectionException;

    void addMetadata( ProjectVersionRef ref, String key, String value )
        throws RelationshipGraphConnectionException;

    void setMetadata( ProjectVersionRef ref, Map<String, String> metadata )
        throws RelationshipGraphConnectionException;

    void deleteRelationshipsDeclaredBy( ProjectVersionRef root )
        throws RelationshipGraphConnectionException;

    /**
     * Add the given relationships. Skip/return those that introduce cycles.
     * 
     * @return The set of relationships that were NOT added because they introduce cycles. NEVER null, but maybe empty.
     */
    Set<ProjectRelationship<?>> addRelationships( ProjectRelationship<?>... rel )
        throws RelationshipGraphConnectionException;

    void addProjectError( ProjectVersionRef ref, String error )
        throws RelationshipGraphConnectionException;

    void clearProjectError( ProjectVersionRef ref )
        throws RelationshipGraphConnectionException;

    void recomputeIncompleteSubgraphs()
        throws RelationshipGraphConnectionException;

    void reindex()
        throws RelationshipGraphConnectionException;

    void reindex( final ProjectVersionRef ref )
        throws RelationshipGraphConnectionException;

    /* 
     * ################################################
     * Queries require a view
     * ---
     * View param is first to support vararg methods
     * ################################################
     */

    String getProjectError( ProjectVersionRef ref );

    Collection<? extends ProjectRelationship<?>> getRelationshipsDeclaredBy( ViewParams params, ProjectVersionRef root );

    Collection<? extends ProjectRelationship<?>> getRelationshipsTargeting( ViewParams params, ProjectVersionRef root );

    Collection<ProjectRelationship<?>> getAllRelationships( ViewParams params );

    Set<List<ProjectRelationship<?>>> getAllPathsTo( ViewParams params, ProjectVersionRef... projectVersionRefs );

    boolean introducesCycle( ViewParams params, ProjectRelationship<?> rel );

    Set<ProjectVersionRef> getAllProjects( ViewParams params );

    void traverse( RelationshipGraphTraversal traversal, ProjectVersionRef root, RelationshipGraph graph,
                   TraversalType type )
        throws RelationshipGraphConnectionException;

    boolean containsProject( ViewParams params, ProjectVersionRef ref );

    boolean containsRelationship( ViewParams params, ProjectRelationship<?> rel );

    boolean isMissing( ViewParams params, ProjectVersionRef project );

    boolean hasMissingProjects( ViewParams params );

    boolean hasProjectError( ProjectVersionRef ref );

    Set<ProjectVersionRef> getMissingProjects( ViewParams params );

    boolean hasVariableProjects( ViewParams params );

    Set<ProjectVersionRef> getVariableProjects( ViewParams params );

    Set<EProjectCycle> getCycles( ViewParams params );

    boolean isCycleParticipant( ViewParams params, ProjectRelationship<?> rel );

    boolean isCycleParticipant( ViewParams params, ProjectVersionRef ref );

    Map<String, String> getMetadata( ProjectVersionRef ref );

    Map<String, String> getMetadata( ProjectVersionRef ref, Set<String> keys );

    Set<ProjectVersionRef> getProjectsWithMetadata( ViewParams params, String key );

    @Deprecated
    Set<ProjectRelationship<?>> getDirectRelationshipsFrom( ViewParams params, ProjectVersionRef from,
                                                            boolean includeManagedInfo, RelationshipType... types );

    @Deprecated
    Set<ProjectRelationship<?>> getDirectRelationshipsTo( ViewParams params, ProjectVersionRef to,
                                                          boolean includeManagedInfo, RelationshipType... types );

    Set<ProjectRelationship<?>> getDirectRelationshipsFrom( ViewParams params, ProjectVersionRef from,
                                                            boolean includeManagedInfo, boolean includeConcreteInfo,
                                                            RelationshipType... types );

    Set<ProjectRelationship<?>> getDirectRelationshipsTo( ViewParams params, ProjectVersionRef to,
                                                          boolean includeManagedInfo, boolean includeConcreteInfo,
                                                          RelationshipType... types );

    Set<ProjectVersionRef> getProjectsMatching( ViewParams params, ProjectRef projectRef );

    void printStats();

    ProjectVersionRef getManagedTargetFor( ProjectVersionRef target, GraphPath<?> path, RelationshipType type );

    GraphPath<?> createPath( ProjectRelationship<?>... relationships );

    GraphPath<?> createPath( GraphPath<?> parent, ProjectRelationship<?> relationship );

    boolean registerView( ViewParams params );

    void registerViewSelection( ViewParams params, ProjectRef ref, ProjectVersionRef projectVersionRef );

    Map<GraphPath<?>, GraphPathInfo> getPathMapTargeting( ViewParams params, Set<ProjectVersionRef> refs );

    ProjectVersionRef getPathTargetRef( GraphPath<?> path );

    List<ProjectVersionRef> getPathRefs( ViewParams params, GraphPath<?> path );

    void close()
        throws RelationshipGraphConnectionException;

    boolean isClosed();

    String getWorkspaceId();

}
