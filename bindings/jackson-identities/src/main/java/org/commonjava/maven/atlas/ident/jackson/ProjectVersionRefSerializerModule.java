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
package org.commonjava.maven.atlas.ident.jackson;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.KeyDeserializer;
import org.commonjava.maven.atlas.ident.ref.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class ProjectVersionRefSerializerModule
                extends SimpleModule
{

    private static final long serialVersionUID = 1L;

    public ProjectVersionRefSerializerModule()
    {
        super( "ProjectRef (with variants) Serializer" );

        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Registering ProjectRef serializer/deserialer" );
        // ProjectRef
        addSerializer( ProjectRef.class, new ProjectRefSerializer<ProjectRef>( ProjectRef.class, false ) );
        addKeySerializer( ProjectRef.class, new ProjectRefSerializer<ProjectRef>( ProjectRef.class, true ) );

        addDeserializer( ProjectRef.class, new ProjectRefDeserializer<ProjectRef>( ProjectRef.class ) );
        addKeyDeserializer( ProjectRef.class, new ProjectRefKeyDeserializer<ProjectRef>( ProjectRef.class ) );

        logger.debug( "Registering ProjectVersionRef serializer/deserialer" );
        // ProjectVersionRef
        addSerializer( ProjectVersionRef.class,
                       new ProjectRefSerializer<ProjectVersionRef>( ProjectVersionRef.class, false ) );
        addKeySerializer( ProjectVersionRef.class,
                          new ProjectRefSerializer<ProjectVersionRef>( ProjectVersionRef.class, true ) );

        addDeserializer( ProjectVersionRef.class,
                         new ProjectRefDeserializer<ProjectVersionRef>( ProjectVersionRef.class ) );
        addKeyDeserializer( ProjectVersionRef.class,
                            new ProjectRefKeyDeserializer<ProjectVersionRef>( ProjectVersionRef.class ) );

        logger.debug( "Registering SimpleArtifactRef serializer/deserialer" );
        // SimpleArtifactRef
        addSerializer( ArtifactRef.class, new ProjectRefSerializer<ArtifactRef>( ArtifactRef.class, false ) );
        addKeySerializer( ArtifactRef.class, new ProjectRefSerializer<ArtifactRef>( ArtifactRef.class, true ) );

        addDeserializer( ArtifactRef.class, new ProjectRefDeserializer<ArtifactRef>( ArtifactRef.class ) );
        addKeyDeserializer( ArtifactRef.class, new ProjectRefKeyDeserializer<ArtifactRef>( ArtifactRef.class ) );

        logger.debug( "Registering VersionlessArtifactRef serializer/deserialer" );
        // VersionlessArtifactRef
        addSerializer( VersionlessArtifactRef.class,
                       new ProjectRefSerializer<VersionlessArtifactRef>( VersionlessArtifactRef.class, false ) );
        addKeySerializer( VersionlessArtifactRef.class,
                          new ProjectRefSerializer<VersionlessArtifactRef>( VersionlessArtifactRef.class, true ) );

        addDeserializer( VersionlessArtifactRef.class,
                         new ProjectRefDeserializer<VersionlessArtifactRef>( VersionlessArtifactRef.class ) );
        addKeyDeserializer( VersionlessArtifactRef.class,
                            new ProjectRefKeyDeserializer<VersionlessArtifactRef>( VersionlessArtifactRef.class ) );
    }

    @Override
    public int hashCode()
    {
        return getClass().getSimpleName().hashCode() + 17;
    }

    @Override
    public boolean equals( final Object other )
    {
        return getClass().equals( other.getClass() );
    }

    private <T extends ProjectRef> T parse( final String value, final Class<T> type )
                    throws IOException
    {
        Class<?> realType = null;
        if ( ProjectRef.class.equals( type ) )
        {
            realType = SimpleProjectRef.class;
        }
        else if ( ProjectVersionRef.class.equals( type ) )
        {
            realType = SimpleProjectVersionRef.class;
        }
        else if ( ArtifactRef.class.equals( type ) )
        {
            realType = SimpleArtifactRef.class;
        }
        else if ( VersionlessArtifactRef.class.equals( type ) )
        {
            realType = SimpleVersionlessArtifactRef.class;
        }

        if ( realType == null )
        {
            throw new IOException( "Cannot find concrete class for type: " + type.getSimpleName() );
        }

        try
        {
            final Method parseMethod = realType.getMethod( "parse", String.class );
            return type.cast( parseMethod.invoke( null, value ) );
        }
        catch ( final NoSuchMethodException e )
        {
            throw new IOException( "Failed to lookup/invoke parse() method on " + type.getSimpleName(), e );
        }
        catch ( final IllegalAccessException e )
        {
            throw new IOException( "Failed to lookup/invoke parse() method on " + type.getSimpleName(), e );
        }
        catch ( final InvocationTargetException e )
        {
            throw new IOException( "Failed to lookup/invoke parse() method on " + type.getSimpleName(), e );
        }
    }

    private static final class ProjectRefSerializer<T extends ProjectRef>
                    extends StdSerializer<T>
    {
        private boolean keySer;

        ProjectRefSerializer( final Class<T> refCls, boolean keySer )
        {
            super( refCls );
            this.keySer = keySer;
        }

        @Override
        public void serialize( final T src, final JsonGenerator generator, final SerializerProvider provider )
                        throws IOException, JsonGenerationException
        {
            if ( keySer )
            {
                generator.writeFieldName( src.toString() );
            }
            else
            {
                generator.writeString( src.toString() );
            }
        }
    }

    private class ProjectRefDeserializer<T extends ProjectRef>
                    extends StdDeserializer<T>
    {
        private static final long serialVersionUID = 1L;

        private final Class<T> refCls;

        ProjectRefDeserializer( final Class<T> refCls )
        {
            super( refCls );
            this.refCls = refCls;
        }

        @Override
        public T deserialize( final JsonParser jp, final DeserializationContext ctxt )
                        throws IOException, JsonProcessingException
        {
            return parse( jp.getText(), refCls );
        }
    }

    public class ProjectRefKeyDeserializer<T extends ProjectRef>
                    extends KeyDeserializer
    {
        private static final long serialVersionUID = 1L;

        private final Class<T> refCls;

        public ProjectRefKeyDeserializer( final Class<T> type )
        {
            this.refCls = type;
        }

        @Override
        public Object deserializeKey( String key, DeserializationContext ctxt )
                        throws IOException, JsonProcessingException
        {
            return parse( key, refCls );
        }
    }

}
