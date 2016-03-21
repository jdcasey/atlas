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

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.junit.Test;

import static org.commonjava.maven.atlas.graph.rel.RelationshipConstants.POM_ROOT_URI;
import static org.commonjava.maven.atlas.graph.rel.RelationshipUtils.dependency;
import static org.commonjava.maven.atlas.ident.util.IdentityUtils.projectVersion;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class RelationshipComparatorTest
{

    /**
     * Tests the transitivity of the compare mathod, i.e. if A < B and B < C, then also A < C.
     * The test creates 4 projects (W, X, Y and Z) and 3 relationships:
     * <pre>
     * A = W depends on X
     * B = X depends on Y
     * C = Y depends on Z
     * </pre>
     *
     * Then it confirms that A < B and B < C. The last step is to test if A < C.
     */
    @Test
    public void testCompareTransitivity()
    {
        final ProjectVersionRef nodeW = projectVersion( "w", "w", "1.0" );
        final ProjectVersionRef nodeX = projectVersion( "x", "x", "1.0" );
        final ProjectVersionRef nodeY = projectVersion( "y", "y", "1.0" );
        final ProjectVersionRef nodeZ = projectVersion( "z", "z", "1.0" );

        DependencyRelationship relA = dependency( POM_ROOT_URI, nodeW, nodeX, 0, false, false );
        DependencyRelationship relB = dependency( POM_ROOT_URI, nodeX, nodeY, 0, false, false );
        DependencyRelationship relC = dependency( POM_ROOT_URI, nodeY, nodeZ, 0, false, false );

        assertThat( "relA < relB must be true to perform the test",
                    RelationshipComparator.INSTANCE.compare( relA, relB ), equalTo( -1 ) );
        assertThat( "relB < relC must be true to perform the test",
                    RelationshipComparator.INSTANCE.compare( relB, relC ), equalTo( -1 ) );

        int compare = RelationshipComparator.INSTANCE.compare( relA, relC );
        compare = ((Float) Math.signum( compare )).intValue();
        assertThat( "relA < relC must be true because relA < relB and relB < relC, but it is not",
                    compare, equalTo( -1 ) );
    }

}
