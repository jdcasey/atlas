package org.commonjava.maven.atlas.tck.graph.testutil;

import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionFactory;
import org.junit.rules.TemporaryFolder;

import java.io.Closeable;

/**
 * Created by jdcasey on 8/24/15.
 */
public interface TCKDriver extends Closeable
{

    void setup( TemporaryFolder temp )
            throws Exception;

    RelationshipGraphConnectionFactory getConnectionFactory()
            throws Exception;
}
