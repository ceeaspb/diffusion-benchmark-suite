/*
 * Copyright 2013, 2014 Push Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.pushtechnology.benchmarks.experiments;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Properties;
import java.util.logging.Level;

import com.pushtechnology.benchmarks.util.PropertiesUtil;
import com.pushtechnology.diffusion.api.Logs;

/**
 * A main class for all experiments.
 * <P>
 * Gets the experiment and the settings file from the command line arguments.
 * Uses the settings file, system properties and experiment settings class for
 * other configuration.
 * <P>
 * Reflection is used to create instances of the settings class and the
 * experiment class.
 * 
 * @author nitsanw
 * 
 */
public final class ExperimentRunner {
    /**
     * Never create me...
     */
    private ExperimentRunner() {
    }

    /**
     * Entry point for experiments.
     * 
     * @param args expect experiment class name and settings file
     */
    @SuppressWarnings("deprecation")
    public static void main(final String[] args) {

        Runnable experiment =null;
        try {
            // This depends on the system properties not on the settings file
            // Try putting this in the client.vm.args
            if (Boolean.getBoolean("verbose")) {
                Logs.setLevel(Level.FINEST);
            } else {
                Logs.setLevel(Level.INFO);
            }

            final CommonExperimentSettings settings =
                    getSettingsObject(args[0], args[1]);

            experiment = getExperimentObject(args[0], settings);
            experiment.run();
            System.exit(0);
        } catch (final Throwable t) {
            // Could be a problem with logging
            // Do not log this error, print it
            System.err.println(System.currentTimeMillis()+" ERROR An exception has been caught at the top"
                    + " level. Unable to complete experiment "+ args[0]);
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Get the settings object to use for the experiment. It also writes the
     * properties used back to the settings file.
     * 
     * @param experimentClassName Name of the experiment class.
     * @param settingsFile The name of the settings file.
     * @return The settings object.
     * @throws IOException Problem writing the settings to a file.
     * @throws ReflectiveOperationException Problem with the reflection used to
     *             get the experiment settings.
     */
    @SuppressWarnings("deprecation")
    private static CommonExperimentSettings getSettingsObject(
            final String experimentClassName,
            final String settingsFile) throws IOException,
            ReflectiveOperationException {
        // Set up the experiment class and settings class
        Class<?> settingsClass;
        try {
            // Use an experiment specific settings class
            final String settingsClassName = experimentClassName + "$Settings";
            settingsClass = Class.forName(settingsClassName);
            Logs.info("Using " + settingsClassName + " settings class...");
        } catch (final ClassNotFoundException e) {
            // Use the default settings class
            Logs.info("Using default settings class...");
            settingsClass = CommonExperimentSettings.class;
        }

        // Load the settings file and obtain an instance of the settings class
        final Properties experimentProperties =
                PropertiesUtil.load(settingsFile);
        final CommonExperimentSettings settings =
                (CommonExperimentSettings) settingsClass.getConstructor(
                        Properties.class).newInstance(
                        experimentProperties);

        // Update the settings file with the experiment settings
        final FileOutputStream out = new FileOutputStream(settingsFile);
        experimentProperties.store(out, "");
        out.close();

        return settings;
    }

    /**
     * Get the experiment object.
     * 
     * @param experimentClassName The name of the experiment.
     * @param settings The settings object to use in the experiment.
     * @return The experiment object.
     * @throws ReflectiveOperationException Problem with the reflection used to
     *             get the experiment object.
     */
    private static Runnable getExperimentObject(
            final String experimentClassName,
            final CommonExperimentSettings settings)
            throws ReflectiveOperationException {
        final Class<?> experimentClass = Class.forName(experimentClassName);
        final Constructor<?> constructor =
                experimentClass.getConstructor(settings.getClass());
        return (Runnable) constructor.newInstance(settings);
    }
}
