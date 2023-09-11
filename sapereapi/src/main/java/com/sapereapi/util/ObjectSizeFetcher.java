package com.sapereapi.util;

import java.lang.instrument.Instrumentation;

import eu.sapere.middleware.log.AbstractLogger;

public class ObjectSizeFetcher {
    private static volatile Instrumentation globalInstrumentation;

    public static void premain(final String agentArgs, final Instrumentation inst) {
        globalInstrumentation = inst;
    }

    public static long getObjectSize(final Object object) {
        if (globalInstrumentation == null) {
            throw new IllegalStateException("Agent not initialized.");
        }
        return globalInstrumentation.getObjectSize(object);
    }

    public static void printObjectSize(Object object, AbstractLogger logger) {
        logger.info("Object type: " + object.getClass() + ", size: " + getObjectSize(object) + " bytes");
    }
}