package org.commonjava.maven.atlas.graph.rel;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by jdcasey on 3/16/16.
 */
public class RelationshipConstants
{
    public static final URI UNKNOWN_SOURCE_URI;

    public static final URI POM_ROOT_URI;

    public static URI ANY_SOURCE_URI;

    public static URI TERMINAL_PARENT_SOURCE_URI;

    static
    {
        final String uri = "atlas:terminal-parent";
        try
        {
            TERMINAL_PARENT_SOURCE_URI = new URI( uri );
        }
        catch ( final URISyntaxException e )
        {
            throw new IllegalStateException( "Terminal-parent source URI constant is invalid: " + uri, e );
        }

        try
        {
            ANY_SOURCE_URI = new URI( "any:any" );
        }
        catch ( final URISyntaxException e )
        {
            throw new IllegalStateException( "Cannot construct any-source URI: 'any:any'" );
        }

        try
        {
            UNKNOWN_SOURCE_URI = new URI( "unknown:unknown" );
        }
        catch ( final URISyntaxException e )
        {
            throw new IllegalStateException( "Cannot construct unknown-source URI: 'unknown:unknown'" );
        }

        try
        {
            POM_ROOT_URI = new URI( "pom:root" );
        }
        catch ( final URISyntaxException e )
        {
            throw new IllegalStateException( "Cannot construct pom-root URI: 'pom:root'" );
        }
    }

}
