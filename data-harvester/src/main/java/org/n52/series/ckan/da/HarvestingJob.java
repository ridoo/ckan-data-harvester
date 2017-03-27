/*
 * Copyright (C) 2015-2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public License
 * version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package org.n52.series.ckan.da;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.n52.io.task.ScheduledJob;
import org.quartz.InterruptableJob;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HarvestingJob extends ScheduledJob implements InterruptableJob {

    private final static Logger LOGGER = LoggerFactory.getLogger(HarvestingJob.class);

    /**
     * Autowired is required because quartz always creates a new instance of the job.
     *
     * See Job Instances in the documentation:
     *
     * www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/tutorial-lesson-03.html
     */
    @Autowired
    private CkanHarvestingService harvestingService;

    private String configFile;

    private boolean enabled = true;

    private HarvestingConfig readJobConfig(String configFile) {
        try (InputStream taskConfig = getClass().getResourceAsStream(configFile)) {
            ObjectMapper om = new ObjectMapper();
            return om.readValue(taskConfig, HarvestingConfig.class);
        }
        catch (IOException e) {
            LOGGER.error("Could not load {}. Using empty config.", configFile, e);
            return new HarvestingConfig();
        }
    }

    @Override
    public JobDetail createJobDetails() {
        return JobBuilder.newJob(HarvestingJob.class)
                .withIdentity(getJobName())
                .withDescription(getJobDescription())
                .usingJobData("configFile", configFile)
                .build();
    }


    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if ( !enabled) {
            return;
        }
        LOGGER.info("Start ckan harvesting task ...");
        final JobDetail details = context.getJobDetail();
        JobDataMap jobDataMap = details.getJobDataMap();
        HarvestingConfig harvestingConfig = readJobConfig((String) jobDataMap.get("configFile"));
        try {
            String outputPath = getOutputFolder(harvestingConfig.getOutputPath());
            LOGGER.debug("Download resources to {}", outputPath);

            harvestingService.setResourceDownloadBaseFolder(outputPath);
            harvestingService.harvestDatasets();
            harvestingService.harvestResources();
        } catch (Exception e) {
            LOGGER.error("could not harvest resources! ", e);
        }
        LOGGER.info("Done harvesting ckan.");
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        this.enabled = false;
        LOGGER.info("Render task successfully shutted down.");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    private String getOutputFolder(String outputPath) throws URISyntaxException {
        URL resource = getClass().getResource("/");
        Path baseFolder = Paths.get(resource.toURI());
        File outputDir = baseFolder.resolve(outputPath).toFile();
        if ( !outputDir.exists()) {
            outputDir.mkdirs();
        }
        return outputDir.getAbsolutePath();
    }

    public CkanHarvestingService getHarvestingService() {
        return harvestingService;
    }

    public void setHarvestingService(CkanHarvestingService harvestingService) {
        this.harvestingService = harvestingService;
    }

}
