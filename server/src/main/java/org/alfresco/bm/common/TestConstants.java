/*
 * Copyright (C) 2005-2013 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.bm.common;

/**
 * Define commonly-used constants
 * 
 * @author Derek Hulley
 * @since 2.0
 */
public final class TestConstants
{
    /*
     * Spring paths
     */
    public static final String PATH_APP_CONTEXT = "classpath:config/spring/app-context.xml";
    public static final String PATH_TEST_SERVICES_CONTEXT = "classpath:config/spring/test-services-context.xml";
    public static final String PATH_TEST_COMMON_CONTEXT = "classpath:config/spring/test-common-context.xml";
    public static final String PATH_TEST_CONTEXT = "classpath:config/spring/test-context.xml";
    
    /*
     * General
     */
    
    public static final String PROP_SYSTEM_CAPABILITIES = "system.capabilities";
    public static final String PROP_TEST_RUN_MONITOR_PERIOD = "test.run.monitor-period";
    
    public static final String CAPABILITY_JAVA6 = "java6";
    public static final String CAPABILITY_JAVA7 = "java7";
    public static final String CAPABILITY_SELENIUM = "selenium";
    
    public static final String TEST_NAME_REGEX = "^[a-zA-Z][a-zA-Z0-9_]*$";
    public static final String RUN_NAME_REGEX = "[a-zA-Z0-9_]*$";
    public static final String PROP_NAME_REGEX = "^[a-zA-Z][a-zA-Z0-9\\.\\-\\_]*$";
    
    public static final String PROP_APP_CONTEXT_PATH = "app.contextPath";
    public static final String PROP_APP_DIR = "app.dir";
    public static final String PROP_APP_RELEASE = "app.release";
    public static final String PROP_APP_DESCRIPTION = "app.description";
    public static final String PROP_APP_SCHEMA = "app.schema";
    public static final String PROP_APP_INHERITANCE = "app.inheritance";
    
    public static final Integer VERSION_ZERO = Integer.valueOf(0);
    public static final String MASK = "******";
    public static final String SEPARATOR = "/";
    public static final String DOT = ".";
    
    /*
     * Mongo properties
     */
    public static final String MONGO_PREFIX = "mongodb://";
    public static final String PROP_MONGO_CONFIG_HOST       = "mongo.config.host";
    public static final String PROP_MONGO_CONFIG_DATABASE   = "mongo.config.database";
    public static final String PROP_MONGO_CONFIG_USERNAME   = "mongo.config.username";
    public static final String PROP_MONGO_CONFIG_PASSWORD   = "mongo.config.password";
    public static final String PROP_MONGO_CONFIG_URI        = "mongo.config.uri";
    public static final String PROP_MONGO_TEST_HOST         = "mongo.test.host";
    public static final String PROP_MONGO_TEST_DATABASE     = "mongo.test.database";
    public static final String PROP_MONGO_TEST_USERNAME     = "mongo.test.username";
    public static final String PROP_MONGO_TEST_PASSWORD     = "mongo.test.password";
    public static final String PROP_MONGO_TEST_URI          = "mongo.test.uri";

    /*
     * Common test run properties
     */
    public static final String PROP_DRIVER_ID = "driverId";
    public static final String PROP_TEST = "test";
    public static final String PROP_TEST_RUN = "testRun";
    public static final String PROP_TEST_RUN_ID = "testRunId";
    public static final String PROP_TEST_RUN_FQN = "testRunFqn";
    
    /*
     * DB field names
     */
    
    public static final String FIELD_ID = "_id";
    public static final String FIELD_RELEASE = "release";
    public static final String FIELD_SCHEMA = "schema";
    public static final String FIELD_PROPERTIES = "properties";
    public static final String FIELD_VALUE = "value";
    public static final String FIELD_IP_ADDRESS = "ipAddress";
    public static final String FIELD_CONTEXT_PATH = "contextPath";
    public static final String FIELD_HOSTNAME = "hostname";
    public static final String FIELD_CAPABILITIES = "capabilities";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_ORIGIN = "origin";
    public static final String FIELD_DEFAULT = "default";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_HIDE = "hide";
    public static final String FIELD_MASK = "mask";
    public static final String FIELD_SYSTEM = "system";
    public static final String FIELD_PING = "ping";
    public static final String FIELD_TIME = "time";
    public static final String FIELD_EXPIRES = "expires";
    public static final String FIELD_TEST = "test";
    public static final String FIELD_RUN = "run";
    public static final String FIELD_DRIVERS = "drivers";
    
    /** @since 2.1.2 */
    public static final String FIELD_CIPHER = "chipher";
    public static final String FIELD_MESSAGE = "msg";
    public static final String FIELD_RESULT = "result";
    
    
    /*
     * Service field names
     */
    
    public static final String FIELD_STATE = "state";
    public static final String FIELD_SCHEDULED = "scheduled";
    public static final String FIELD_STARTED = "started";
    public static final String FIELD_STOPPED = "stopped";
    public static final String FIELD_COMPLETED = "completed";
    public static final String FIELD_DURATION = "duration";
    public static final String FIELD_RESULTS_SUCCESS = "resultsSuccess";
    public static final String FIELD_RESULTS_FAIL = "resultsFail";
    public static final String FIELD_RESULTS_TOTAL = "resultsTotal";
    public static final String FIELD_SUCCESS_RATE = "successRate";
    public static final String FIELD_PROGRESS = "progress";
}
