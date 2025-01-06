package com.loganasherjones.mac;

import java.io.IOException;

/**
 * An interface to set the classpath for subprocesses.
 * <p>
 * This interface allows users to inject their own classpath weirdness
 * directly into the processes spawned by mini accumulo cluster. This
 * is mostly useful when you want to inject your own iterators into
 * the accumulo processes, but for some reason the jars are not already
 * on your classpath.
 * </p>
 *
 * @author loganasherjones
 * @since 1.10.4
 */
public interface ClasspathLoader {

    /**
     * Returns a classpath string fit for the -cp flag to java.
     *
     * @return Classpath string fit for the -cp flag.
     * @throws IOException If something goes wrong loading the classpath.
     * @since 1.10.4
     */
    String getClasspath() throws IOException;
}
