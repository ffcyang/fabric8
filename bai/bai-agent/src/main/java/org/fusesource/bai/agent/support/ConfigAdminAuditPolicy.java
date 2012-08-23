/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.bai.agent.support;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 */
public class ConfigAdminAuditPolicy extends DefaultAuditPolicy {
    private static final transient Logger LOG = LoggerFactory.getLogger(ConfigAdminAuditPolicy.class);

    public final String KEY_CAMEL_CONTEXT_EXCLUDE = "camelContext.exclude";

    private String configPid = "org.fusesource.bai.agent";
    private ConfigurationAdmin configurationAdmin;

    public void init() throws Exception {
        if (configurationAdmin != null) {
            Configuration config = configurationAdmin.getConfiguration(configPid);
            if (config != null) {
                Dictionary properties = config.getProperties();
                if (properties == null) {
                    // there are no configuration properties yet
                    properties = new Hashtable();
                }
                updated(properties);
            } else {
                LOG.warn("ConfigurationAdmin Configuration for " + configPid);
            }
        }
    }

    public void updated(Dictionary dict) throws ConfigurationException {
        String pattern = getOrElse(dict, KEY_CAMEL_CONTEXT_EXCLUDE, DEFAULT_EXCLUDE_CAMEL_CONTEXT_FILTER);
        LOG.info("Setting the camelContext exclude pattern to " + pattern);
        setExcludeCamelContextPattern(pattern);

        // TODO load the other properties here!!!
    }

    public static String getOrElse(Dictionary dict, String key, String defaultValue) {
        Object value = dict.get(key);
        if (value == null) {
            return defaultValue;
        } else {
            return value.toString();
        }
    }


    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public String getConfigPid() {
        return configPid;
    }

    public void setConfigPid(String configPid) {
        this.configPid = configPid;
    }
}
