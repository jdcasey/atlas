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

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.SingleVersion;

public interface ProjectRelationship<R extends ProjectRelationship<R, T>, T extends ProjectVersionRef>
    extends Serializable
{

    int getIndex();

    RelationshipType getType();

    ProjectVersionRef getDeclaring();

    T getTarget();

    ArtifactRef getTargetArtifact();

    R cloneFor( final ProjectVersionRef projectRef );

    R selectDeclaring( ProjectVersionRef ref );

    R selectTarget( ProjectVersionRef ref );

    boolean isManaged();

    Set<URI> getSources();

    R addSource( URI source );

    R addSources( Collection<URI> sources );

    URI getPomLocation();

}
