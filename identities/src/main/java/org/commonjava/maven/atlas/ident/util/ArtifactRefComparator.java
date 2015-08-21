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
package org.commonjava.maven.atlas.ident.util;

import java.util.Comparator;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;

public class ArtifactRefComparator
    implements Comparator<ArtifactRef>
{

    private final ProjectVersionRefComparator pvrComp = new ProjectVersionRefComparator();

    @Override
    public int compare( final ArtifactRef f, final ArtifactRef s )
    {
        int comp = pvrComp.compare( f, s ); // compare groupId, artifactId, and version spec.
        if ( comp == 0 )
        {
            final String fc = f.getClassifier();
            final String sc = s.getClassifier();

            if ( fc == null && sc == null )
            {
                comp = 0;
            }
            else if ( fc == null )
            {
                comp = 1;
            }
            else if ( sc == null )
            {
                comp = -1;
            }
            else
            {
                comp = fc.compareTo( sc );
            }
        }

        if ( comp == 0 )
        {
            final String ft = f.getType(); // never null
            final String st = s.getType(); // never null

            comp = ft.compareTo( st );
        }

        return comp;
    }

}
