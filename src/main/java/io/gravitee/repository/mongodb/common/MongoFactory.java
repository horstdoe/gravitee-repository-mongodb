/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.mongodb.common;

import com.mongodb.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 * @author Guillaume GILLON (guillaume.gillon@outlook.com)
 */
public class MongoFactory implements FactoryBean<Mongo> {

    private final Logger logger = LoggerFactory.getLogger(MongoFactory.class);

    @Autowired
    private Environment environment;

    private final String propertyPrefix;

    public MongoFactory(String propertyPrefix) {
        this.propertyPrefix = propertyPrefix + ".mongodb.";
    }
    
    private MongoClientOptions.Builder builder() {
        MongoClientOptions.Builder builder = MongoClientOptions.builder();

        String writeConcern = readPropertyValue(propertyPrefix + "writeConcern", String.class, "1");
        Boolean journal = readPropertyValue(propertyPrefix + "journal", Boolean.class);
        Integer wtimeout = readPropertyValue(propertyPrefix + "wtimeout", Integer.class, 0);
        Integer connectionsPerHost = readPropertyValue(propertyPrefix + "connectionsPerHost", Integer.class);
        Integer connectTimeout = readPropertyValue(propertyPrefix + "connectTimeout", Integer.class, 1000);
        Integer maxWaitTime = readPropertyValue(propertyPrefix + "maxWaitTime", Integer.class);
        Integer socketTimeout = readPropertyValue(propertyPrefix + "socketTimeout", Integer.class, 1000);
        Boolean socketKeepAlive = readPropertyValue(propertyPrefix + "socketKeepAlive", Boolean.class);
        Integer maxConnectionLifeTime = readPropertyValue(propertyPrefix + "maxConnectionLifeTime", Integer.class);
        Integer maxConnectionIdleTime = readPropertyValue(propertyPrefix + "maxConnectionIdleTime", Integer.class);

        // We do not want to wait for a server
        Integer serverSelectionTimeout = readPropertyValue(propertyPrefix + "serverSelectionTimeout", Integer.class, 1000);
        Integer minHeartbeatFrequency = readPropertyValue(propertyPrefix + "minHeartbeatFrequency", Integer.class);
        String description = readPropertyValue(propertyPrefix + "description", String.class, "gravitee.io");
        Integer heartbeatConnectTimeout = readPropertyValue(propertyPrefix + "heartbeatConnectTimeout", Integer.class, 1000);
        Integer heartbeatFrequency = readPropertyValue(propertyPrefix + "heartbeatFrequency", Integer.class);
        Integer heartbeatSocketTimeout = readPropertyValue(propertyPrefix + "heartbeatSocketTimeout", Integer.class);
        Integer localThreshold = readPropertyValue(propertyPrefix + "localThreshold", Integer.class);
        Integer minConnectionsPerHost = readPropertyValue(propertyPrefix + "minConnectionsPerHost", Integer.class);
        Boolean sslEnabled = readPropertyValue(propertyPrefix + "sslEnabled", Boolean.class);
        String keystore = readPropertyValue(propertyPrefix + "keystore", String.class);
        String keystorePassword = readPropertyValue(propertyPrefix + "keystorePassword", String.class);
        String keyPassword = readPropertyValue(propertyPrefix + "keyPassword", String.class);
        Integer threadsAllowedToBlockForConnectionMultiplier = readPropertyValue(propertyPrefix + "threadsAllowedToBlockForConnectionMultiplier", Integer.class);
        Boolean cursorFinalizerEnabled = readPropertyValue(propertyPrefix + "cursorFinalizerEnabled", Boolean.class);

        String readPreference = readPropertyValue(propertyPrefix + "readPreference", String.class);
        String readPreferenceTags = readPropertyValue(propertyPrefix + "readPreferenceTags", String.class);

        if (connectionsPerHost != null)
            builder.connectionsPerHost(connectionsPerHost);
        if (maxWaitTime != null)
            builder.maxWaitTime(maxWaitTime);
        if (connectTimeout != null)
            builder.connectTimeout(connectTimeout);
        if (socketTimeout != null)
            builder.socketTimeout(socketTimeout);
        if (socketKeepAlive != null)
            builder.socketKeepAlive(socketKeepAlive);
        if (maxConnectionLifeTime != null)
            builder.maxConnectionLifeTime(maxConnectionLifeTime);
        if (maxConnectionIdleTime != null)
            builder.maxConnectionIdleTime(maxConnectionIdleTime);
        if (minHeartbeatFrequency != null)
            builder.minHeartbeatFrequency(minHeartbeatFrequency);
        if (description != null)
            builder.description(description);
        if (heartbeatConnectTimeout != null)
            builder.heartbeatConnectTimeout(heartbeatConnectTimeout);
        if (heartbeatFrequency != null)
            builder.heartbeatFrequency(heartbeatFrequency);
        if (heartbeatSocketTimeout != null)
            builder.heartbeatSocketTimeout(heartbeatSocketTimeout);
        if (localThreshold != null)
            builder.localThreshold(localThreshold);
        if (minConnectionsPerHost != null)
            builder.minConnectionsPerHost(minConnectionsPerHost);
        if (sslEnabled != null)
            builder.sslEnabled(sslEnabled);
        if (keystore != null) {
            try {
                SSLContext ctx = SSLContext.getInstance("TLS");
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(new FileInputStream(keystore), keystorePassword.toCharArray());
                keyManagerFactory.init(ks, keyPassword.toCharArray());
                ctx.init(keyManagerFactory.getKeyManagers(), null, null);
                builder.sslContext(ctx);
            } catch (Exception e) {
                logger.error(e.getCause().toString());
                throw new IllegalStateException("Error creating the keystore for mongodb", e);
            }
        }
        if (threadsAllowedToBlockForConnectionMultiplier != null)
            builder.threadsAllowedToBlockForConnectionMultiplier(threadsAllowedToBlockForConnectionMultiplier);
        if (cursorFinalizerEnabled != null)
            builder.cursorFinalizerEnabled(cursorFinalizerEnabled);
        if (serverSelectionTimeout != null)
            builder.serverSelectionTimeout(serverSelectionTimeout);

        if (readPreference != null)  {
            TagSet tagSet = null;
            ReadPreference readPrefObj = null;

            if(readPreferenceTags != null) {
                tagSet = buildTagSet(readPreferenceTags);
            }

            switch (readPreference) {
                case "nearest":
                    readPrefObj = tagSet != null ? ReadPreference.nearest(tagSet) : ReadPreference.nearest(); break;
                case "primary":
                    readPrefObj = ReadPreference.primary(); break;
                case "primaryPreferred":
                    readPrefObj = ReadPreference.primaryPreferred(); break;
                case "secondary":
                    readPrefObj = tagSet != null ? ReadPreference.secondary(tagSet) : ReadPreference.secondary(); break;
                case "secondaryPreferred":
                    readPrefObj = tagSet != null ? ReadPreference.secondaryPreferred(tagSet) : ReadPreference.secondaryPreferred(); break;
            }

            builder.readPreference(readPrefObj);
        }
        WriteConcern wc;
        if (StringUtils.isNumeric(writeConcern)) {
            wc = new WriteConcern(Integer.valueOf(writeConcern));
        } else {
            Assert.isTrue(writeConcern.equals("majority"), "writeConcern must be numeric or equals to 'majority'");
            wc = new WriteConcern(writeConcern);
        }
        builder.writeConcern(wc.withJournal(journal).withWTimeout(wtimeout, TimeUnit.MILLISECONDS));

        return builder;
    }

    @Override
    public Mongo getObject() throws Exception {
        MongoClientOptions.Builder builder = builder();

        // Trying to get the MongoClientURI if uri property is defined
        String uri = readPropertyValue(propertyPrefix + "uri");

        if (uri != null && ! uri.isEmpty()) {
            // The builder can be configured with default options, which may be overridden by options specified in
            // the URI string.
            return new MongoClient(
                    new MongoClientURI(uri, builder));
        } else {
            String username = readPropertyValue(propertyPrefix + "username");
            String password = readPropertyValue(propertyPrefix + "password");

            MongoCredential credential = null;
            if (username != null || password != null) {
                String authSource = readPropertyValue(propertyPrefix + "authSource", String.class, "gravitee");
                credential = MongoCredential.createCredential(username, authSource, password.toCharArray());
            }


            List<ServerAddress> seeds;
            int serversCount = getServersCount();

            if (serversCount == 0) {
                String host = readPropertyValue(propertyPrefix + "host", String.class, "localhost");
                int port = readPropertyValue(propertyPrefix + "port", int.class, 27017);
                seeds = Collections.singletonList(new ServerAddress(host, port));
            } else {
                seeds = new ArrayList<>(serversCount);
                for (int i = 0; i < serversCount; i++) {
                    seeds.add(buildServerAddress(i));
                }
            }

            MongoClientOptions options = builder.build();
            if (credential == null) {
                return new MongoClient(seeds, options);
            }

            return new MongoClient(seeds, credential, options);
        }
    }

    private int getServersCount() {
        logger.debug("Looking for MongoDB server configuration...");

        boolean found = true;
        int idx = 0;

        while (found) {
            String serverHost = environment.getProperty(propertyPrefix + "servers[" + (idx++) + "].host");
            found = (serverHost != null);
        }

        return --idx;
    }

    private ServerAddress buildServerAddress(int idx) {
        String host = environment.getProperty(propertyPrefix + "servers[" + idx + "].host");
        int port = readPropertyValue(propertyPrefix + "servers[" + idx + "].port", int.class, 27017);

        return new ServerAddress(host, port);
    }

    private TagSet buildTagSet(String readPreferenceTags)  {
        List<Tag> tags = Pattern.compile(",").splitAsStream(readPreferenceTags)
                .map((String::trim))
                .map((tag) -> {
                    String[] tagString = tag.split(":");
                    return new Tag(tagString[0].trim(), tagString[1].trim());
                }).collect(Collectors.toList());

        if(tags.size() >  1) {
            return new TagSet(tags);
        } else {
            return new TagSet(tags.get(0));
        }
    }

    private String readPropertyValue(String propertyName) {
        return readPropertyValue(propertyName, String.class, null);
    }

    private <T> T readPropertyValue(String propertyName, Class<T> propertyType) {
        return readPropertyValue(propertyName, propertyType, null);
    }

    private <T> T readPropertyValue(String propertyName, Class<T> propertyType, T defaultValue) {
        T value = environment.getProperty(propertyName, propertyType, defaultValue);
        logger.debug("Read property {}: {}", propertyName, value);
        return value;
    }

    @Override
    public Class<?> getObjectType() {
        return Mongo.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
