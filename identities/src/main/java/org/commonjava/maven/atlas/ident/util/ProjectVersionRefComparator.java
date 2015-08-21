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

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.VersionSpec;

public class ProjectVersionRefComparator
    implements Comparator<ProjectVersionRef>
{

    @Override
    public int compare( final ProjectVersionRef f, final ProjectVersionRef s )
    {
        final int comp = f.compareTo( s ); // this compares groupId and artifactId ONLY
        if ( comp == 0 )
        {
            final VersionSpec fs = f.getVersionSpec();
            final VersionSpec ss = s.getVersionSpec();

            return fs.compareTo( ss );
        }

        return comp;
    }

}
