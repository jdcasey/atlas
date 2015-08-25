package org.commonjava.maven.atlas.graph.spi.jung.fixture;

import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionFactory;
import org.commonjava.maven.atlas.graph.spi.jung.JungGraphConnectionFactory;
import org.commonjava.maven.atlas.tck.graph.testutil.TCKDriver;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

/**
 * Created by jdcasey on 8/24/15.
 */
public class JungTCKDriver
        implements TCKDriver
{
    private TemporaryFolder temp;

    private JungGraphConnectionFactory factory;

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
            factory = new JungGraphConnectionFactory();
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
