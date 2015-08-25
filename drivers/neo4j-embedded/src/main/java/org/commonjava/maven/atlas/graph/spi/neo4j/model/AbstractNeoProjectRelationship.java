package org.commonjava.maven.atlas.graph.spi.neo4j.model;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.neo4j.graphdb.Relationship;

import java.net.URI;
import java.util.Collection;
import java.util.Set;

/**
 * Created by jdcasey on 8/24/15.
 */
public abstract class AbstractNeoProjectRelationship<R extends AbstractNeoProjectRelationship<R, I, T>, I extends ProjectRelationship<I, T>, T extends ProjectVersionRef>
    implements ProjectRelationship<I, T>
{
    protected Relationship rel;

    private RelationshipType type;

    protected ProjectVersionRef declaring;

    private boolean dirty;

    protected T target;

    protected Set<URI> sources;

    protected AbstractNeoProjectRelationship( Relationship rel, RelationshipType type )
    {
        this.rel = rel;
        this.type = type;
    }

    protected R cloneDirtyState( R old )
    {
        this.dirty = old.isDirty();
        this.declaring = old.declaring;
        this.target = old.target;
        this.sources = old.sources;
        return (R) this;
    }

    protected R withDeclaring( ProjectVersionRef declaring )
    {
        this.declaring = declaring;
        this.dirty = true;
        return (R) this;
    }

    protected R withTarget( T target )
    {
        this.target = target;
        this.dirty = true;
        return (R) this;
    }

    protected R withSources( Set<URI> sources )
    {
        this.sources = sources;
        this.dirty = true;
        return (R) this;
    }

    public boolean isDirty()
    {
        return dirty;
    }

    @Override
    public synchronized I cloneFor( final ProjectVersionRef projectRef )
    {
        return selectDeclaring( projectRef );
    }

    @Override
    public int getIndex()
    {
        return Conversions.getIntegerProperty(Conversions.INDEX, rel);
    }

    @Override
    public RelationshipType getType()
    {
        return type;
    }

    @Override
    public ProjectVersionRef getDeclaring()
    {
        return declaring == null ? new NeoProjectVersionRef( rel.getStartNode() ) : declaring;
    }

    @Override
    public ArtifactRef getTargetArtifact()
    {
        return getTarget().asPomArtifact();
    }

    @Override
    public boolean isManaged()
    {
        return Conversions.getBooleanProperty( Conversions.IS_MANAGED, rel );
    }

    @Override
    public Set<URI> getSources()
    {
        return sources == null ? Conversions.getURISetProperty( Conversions.SOURCE_URI, rel, RelationshipUtils.UNKNOWN_SOURCE_URI ) : sources;
    }

    @Override
    public URI getPomLocation()
    {
        return Conversions.getURIProperty( Conversions.POM_LOCATION_URI, rel, RelationshipUtils.POM_ROOT_URI );
    }
}
