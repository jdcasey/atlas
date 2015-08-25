package org.commonjava.maven.atlas.graph.spi.neo4j.fixture;

import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionFactory;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jConnectionFactory;
import org.commonjava.maven.atlas.tck.graph.testutil.TCKDriver;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

/**
 * Created by jdcasey on 8/24/15.
 */
public class NeoTCKDriver
        implements TCKDriver
{
    private TemporaryFolder temp;

    private FileNeo4jConnectionFactory factory;

    @Override
    public void setup( TemporaryFolder temp )
            throws Exception
    {
        this.temp = temp;
    }

    @Override
    public RelationshipGraphConnectionFactory getConnectionFactory()
            throws Exception
    {
        if ( factory == null )
        {
            factory = new FileNeo4jConnectionFactory( temp.newFolder( "db", ".dir" ), false );
        }

        return factory;
    }

    @Override
    public void close()
            throws IOException
    {
        if ( factory != null )
        {
            try
            {
                factory.close();
            }
            catch ( RelationshipGraphConnectionException e )
            {
                throw new IOException( "Failed to shutdown graph connection factory: " + e.getMessage(), e );
            }
        }
    }
}
