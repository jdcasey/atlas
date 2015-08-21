package org.commonjava.maven.atlas.ident.ref;

import java.io.Serializable;

/**
 * Created by jdcasey on 8/21/15.
 */
public interface TypeAndClassifier
        extends Serializable
{
    String getType();

    String getClassifier();
}
