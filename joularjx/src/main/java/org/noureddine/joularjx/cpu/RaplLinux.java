/*
 * Copyright (c) 2021-2024, Adel Noureddine, Université de Pau et des Pays de l'Adour.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the
 * GNU General Public License v3.0 only (GPL-3.0-only)
 * which accompanies this distribution, and is available at
 * https://www.gnu.org/licenses/gpl-3.0.en.html
 *
 * Author : Adel Noureddine
 */

package org.noureddine.joularjx.cpu;

import org.noureddine.joularjx.utils.JoularJXLogging;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RaplLinux implements Cpu {

    private static final Logger logger = JoularJXLogging.getLogger();

    static final String RAPL_PSYS = "/sys/class/powercap/intel-rapl/intel-rapl:1/energy_uj";

    static final String RAPL_PKG = "/sys/class/powercap/intel-rapl/intel-rapl:0/energy_uj";

    static final String RAPL_DRAM = "/sys/class/powercap/intel-rapl/intel-rapl:0/intel-rapl:0:2/energy_uj";

    static final String RAPL_PSYS_MAX = "/sys/class/powercap/intel-rapl/intel-rapl:1/max_energy_range_uj";

    static final String RAPL_PKG_MAX = "/sys/class/powercap/intel-rapl/intel-rapl:0/max_energy_range_uj";

    static final String RAPL_DRAM_MAX = "/sys/class/powercap/intel-rapl/intel-rapl:0/intel-rapl:0:2/max_energy_range_uj";

    /**
     * RAPL files existing on the current system. All files in this list will be used for reading the
     * energy values.
     */
    private final List<Path> raplFilesToRead = new ArrayList<>(3);

    /**
     * RAPL max values files existing on the current system. All files in this list will be used for reading the energy values.
     */
    private final List<Path> maxRaplFilesToRead = new ArrayList<>(3);

    /**
     * Filesystem where the RAPL files are located.
     */
    private final FileSystem fileSystem;

    /**
     * Create a new energy measurement via RAPL. The files will be read from the default filesystem.
     */
    public RaplLinux() {
        this(FileSystems.getDefault());
    }

    /**
     * Create a new energy measurement via RAPL. The files will be read from the passed filesystem.
     *
     * @param fileSystem The filesystem to use for reading the RAPL files
     */
    RaplLinux(final FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    /**
     * Check which RAPL files are available on the system to read the energy values from.
     */
    @Override
    public void initialize() {
        final Path psysFile = fileSystem.getPath(RAPL_PSYS);
        final Path psysMaxFile = fileSystem.getPath(RAPL_PSYS_MAX);
        if (Files.exists(psysFile)) {
            // psys exists, so use this for energy readings
            if (Files.isReadable(psysFile)) {
                raplFilesToRead.add(psysFile);
                if (Files.isReadable(psysMaxFile)) {
                    maxRaplFilesToRead.add(psysMaxFile);
                } else {
                    logger.log(Level.SEVERE, "Failed to get read" + psysMaxFile + " file. Exiting... Did you run JoularJX with elevated privileges (sudo)?");
                    System.exit(1);
                }
            } else {
                logger.log(Level.SEVERE, "Failed to get RAPL energy readings from" + psysFile + " file. Exiting... Did you run JoularJX with elevated privileges (sudo)?");
                System.exit(1);
            }
        } else {
            // No psys supported, then check for pkg and dram
            final Path pkgFile = fileSystem.getPath(RAPL_PKG);
            final Path pkgMaxFile = fileSystem.getPath(RAPL_PKG_MAX);
            if (Files.exists(pkgFile)) {
                if (Files.isReadable(pkgFile)) {
                    raplFilesToRead.add(pkgFile);
                    if (Files.isReadable(pkgMaxFile)) {
                        maxRaplFilesToRead.add(pkgMaxFile);
                    } else {
                        logger.log(Level.SEVERE, "Failed to get read" + pkgMaxFile + " file. Exiting... Did you run JoularJX with elevated privileges (sudo)?");
                        System.exit(1);
                    }
                } else {
                    logger.log(Level.SEVERE, "Failed to get RAPL energy readings from" + pkgFile + " file. Exiting... Did you run JoularJX with elevated privileges (sudo)?");
                    System.exit(1);
                }

                final Path dramFile = fileSystem.getPath(RAPL_DRAM);
                final Path dramMaxFile = fileSystem.getPath(RAPL_DRAM_MAX);
                if (Files.exists(dramFile)) {
                    if (Files.isReadable(dramFile)) {
                        raplFilesToRead.add(dramFile);
                        if (Files.isReadable(dramMaxFile)) {
                            maxRaplFilesToRead.add(dramMaxFile);
                        } else {
                            logger.log(Level.SEVERE, "Failed to get read" + dramMaxFile + " file. Exiting... Did you run JoularJX with elevated privileges (sudo)?");
                            System.exit(1);
                        }
                    } else {
                        logger.log(Level.WARNING, "Failed to get RAPL energy readings from" + dramFile + " file. Continuing without it.");
                    }
                }
            }
        }

        if (raplFilesToRead.isEmpty()) {
            logger.log(Level.SEVERE, "Found no RAPL files to read the energy measurement from. Exit ...");
            System.exit(1);
        }
    }

    /**
     * Get energy readings from RAPL through powercap
     * Calculates the best energy reading as supported by CPU (psys, or pkg+dram, or pkg)
     * @return Energy readings from RAPL
     */
    @Override
    public double getCurrentPower(final double cpuLoad) {
        double energyData = 0.0;

        for (final Path raplFile : raplFilesToRead) {
            try {
                energyData += Double.parseDouble(Files.readString(raplFile));
            } catch (IOException exception) {
                logger.throwing(getClass().getName(), "getCurrentPower", exception);
            }
        }

        // Divide by 1 million to convert microJoules to Joules
        return energyData / 1000000;
    }

    /**
     * Get max energy value of RAPL interface through powercap
     * @return Maximum energy value of RAPL interface
     */
    @Override
    public double getMaxPower(final double cpuLoad) {
        double energyData = 0.0;

        for (final Path raplFile : maxRaplFilesToRead) {
            try {
                energyData += Double.parseDouble(Files.readString(raplFile));
            } catch (IOException exception) {
                logger.throwing(getClass().getName(), "getMaxPower", exception);
            }
        }

        // Divide by 1 million to convert microJoules to Joules
        return energyData / 1000000;
    }

    /**
     * Returns the
     *
     * @return Energy readings from RAPL
     */
    @Override
    public double getInitialPower() {
        return getCurrentPower(0);
    }

    @Override
    public void close() {
        // Nothing to do for RAPL Linux
    }
}