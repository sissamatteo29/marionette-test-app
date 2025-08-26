package com.outfit.processor.__marionette;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import java.io.InputStream;

public class BehaviourRegistry {

    // For each class, maintain a map between method name (original) and current
    // variant of execution
    private static Map<String, Map<String, String>> behaviourRegistry = new HashMap<>();

    static {
        try {
            System.out.println("Initializing BehaviourRegistry...");
            
            // Try to load from classpath first
            InputStream configStream = BehaviourRegistry.class.getClassLoader()
                .getResourceAsStream("marionette.xml");
            
            if (configStream == null) {
                System.err.println("Cannot find marionette.xml in classpath, registry will remain empty");
            }
            
            System.out.println("Found marionette.xml in classpath, parsing...");
            IMarionetteConfigLoaderFactory loaderFactory = new XMLMarionetteConfigLoaderFactory();
            MarionetteParser marionetteParser = new MarionetteParser("marionette.xml", loaderFactory);
            MarionetteConfig marionetteConfig = marionetteParser.parseConfig();
            for (MarionetteClassConfig classConfig : marionetteConfig.marionetteClasses) {
                // Obtain class name
                Path pathToClass = Paths.get(classConfig.originalClass.path);
                // Load default behavrious for each class
                for (BehaviourConfig behaviour : classConfig.originalClass.behaviours) {
                    BehaviourRegistry.addMethod(pathToClass.toString(), behaviour.name, behaviour.id);
                }
            }

            System.out.println("BehaviourRegistry initialized successfully with " + behaviourRegistry.size() + " classes");
        } catch (Exception e) {
           System.err.println("Failed to initialize BehaviourRegistry: " + e.getMessage());
            e.printStackTrace();
            // Don't rethrow - let the class load with empty registry
        }
    }

    public static void addMethod(String className, String methodName, String originalBehaviourId) {
        behaviourRegistry.computeIfAbsent(className, key -> new HashMap<>()).put(methodName, originalBehaviourId);
        System.out.println("Current behaviour registry: " + behaviourRegistry);
    }

    public static String getBehaviourId(String className, String methodName) {
        return behaviourRegistry.get(className).get(methodName);
    }

    public static void setBehaviour(String className, String methodName, String behaviourId) {
        if (behaviourRegistry.containsKey(className)) {
            Map<String, String> methodMap = behaviourRegistry.get(className);
            if (methodMap.containsKey(methodName)) {
                methodMap.put(methodName, behaviourId);
            } else {
                System.out.println("Sorry no method named like that in the class");
            }
        } else {
            System.out.println("Sorry, no class named like that");
        }
    }
}
