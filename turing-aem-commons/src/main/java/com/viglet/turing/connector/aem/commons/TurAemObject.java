/*
 *
 * Copyright (C) 2016-2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.connector.aem.commons;

import static com.viglet.turing.connector.aem.commons.TurAemConstants.ACTIVATE;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.CONTENT_FRAGMENT;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.CQ_LAST_MODIFIED;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.CQ_LAST_REPLICATED;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.CQ_LAST_REPLICATED_PUBLISH;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.CQ_LAST_REPLICATION_ACTION;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.CQ_LAST_REPLICATION_ACTION_PUBLISH;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.CQ_MODEL;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.CQ_TEMPLATE;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.DATA_FOLDER;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.DATE_FORMAT;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.DATE_JSON_FORMAT;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.EMPTY_VALUE;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.HTML;
import static com.viglet.turing.connector.aem.commons.TurAemConstants.JCR_CONTENT;
import static java.time.ZoneOffset.UTC;
import static org.apache.jackrabbit.JcrConstants.JCR_CREATED;
import static org.apache.jackrabbit.JcrConstants.JCR_LASTMODIFIED;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_TITLE;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.json.JSONObject;
import com.viglet.turing.connector.aem.commons.bean.TurAemEvent;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
public class TurAemObject {
    private Calendar lastModified;
    private Calendar createdDate;
    private Calendar publicationDate;
    private boolean contentFragment = false;
    private boolean delivered = false;
    private final String type;
    private final String path;
    private final String url;
    private final JSONObject jcrNode;
    private JSONObject jcrContentNode = new JSONObject();
    private String title;
    private String template;
    private String model;
    private Set<String> dependencies;
    private final Map<String, Object> attributes = new HashMap<>();
    public final SimpleDateFormat aemJsonDateFormat =
            new SimpleDateFormat(DATE_JSON_FORMAT, Locale.ENGLISH);


    public TurAemObject(String nodePath, JSONObject jcrNode) {
        this(nodePath, jcrNode, TurAemEvent.NONE);
    }

    public TurAemObject(String nodePath, JSONObject jcrNode, TurAemEvent event) {
        this.jcrNode = jcrNode;
        this.path = nodePath;
        this.url = nodePath + HTML;
        this.type = jcrNode.has(JCR_PRIMARYTYPE) ? jcrNode.getString(JCR_PRIMARYTYPE) : EMPTY_VALUE;
        this.dependencies = TurAemCommonsUtils.getDependencies(jcrNode);
        try {
            if (jcrNode.has(JCR_CONTENT)) {
                processJcrContent(jcrNode, event);
            }
            if (jcrNode.has(JCR_CREATED)) {
                processJcrCreated(jcrNode);
            }
        } catch (ParseException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void processJcrCreated(JSONObject jcrNode) throws ParseException {
        Calendar createdDateCalendar = Calendar.getInstance();
        createdDateCalendar.setTime(aemJsonDateFormat.parse(jcrNode.getString(JCR_CREATED)));
        this.createdDate = createdDateCalendar;
    }

    private void processJcrContent(JSONObject jcrNode, TurAemEvent event) throws ParseException {
        if (!jcrNode.has(JCR_CONTENT)) {
            log.warn("JCR content node not found for path: {}", this.path);
            return;
        }

        this.jcrContentNode = jcrNode.getJSONObject(JCR_CONTENT);

        handleEventOverrides(event);
        extractBasicProperties();
        extractDataFolder();
        extractDates();
    }

    private void handleEventOverrides(TurAemEvent event) {
        if (event != null) {
            switch (event) {
                case PUBLISHING -> {
                    log.info("Overriding publishing status for path: {}", this.path);
                    this.delivered = true;
                }
                case UNPUBLISHING -> {
                    log.info("Overriding unpublishing status for path: {}", this.path);
                    this.delivered = false;
                }
                default -> this.delivered = getJcrDelivered();
            }
        } else {
            this.delivered = getJcrDelivered();
        }
    }

    private void extractBasicProperties() {
        this.template = getJcrTemplate();
        this.title = getJcrTitle();
        this.contentFragment = isJcrContentFragment();
    }

    private void extractDataFolder() {
        getDataFolder(this.jcrContentNode);
    }

    private void extractDates() {
        this.lastModified = parseDate(this::getJcrLastModified, "last modified");
        this.publicationDate = parseDate(this::getJcrPublicationDate, "publication");
    }

    private Calendar parseDate(DateParser parser, String dateType) {
        try {
            return parser.parse();
        } catch (ParseException e) {
            log.error("Failed to parse {} date for path: {}", dateType, this.path, e);
            return null;
        }
    }

    @FunctionalInterface
    private interface DateParser {
        Calendar parse() throws ParseException;
    }

    private boolean isJcrContentFragment() {
        return TurAemCommonsUtils.hasProperty(this.jcrContentNode, CONTENT_FRAGMENT)
                ? this.jcrContentNode.getBoolean(CONTENT_FRAGMENT)
                : (this.contentFragment = false);
    }

    private boolean getJcrDelivered() {
        return isActivated(CQ_LAST_REPLICATION_ACTION)
                && isActivated(CQ_LAST_REPLICATION_ACTION_PUBLISH);
    }

    private String getJcrTemplate() {
        return jcrContentNode.has(CQ_TEMPLATE) ? this.jcrContentNode.getString(CQ_TEMPLATE)
                : EMPTY_VALUE;
    }

    private String getJcrTitle() {
        return jcrContentNode.has(JCR_TITLE) ? this.jcrContentNode.getString(JCR_TITLE)
                : EMPTY_VALUE;
    }

    private Calendar getJcrPublicationDate() throws ParseException {
        return getCalendar(CQ_LAST_REPLICATED_PUBLISH, CQ_LAST_REPLICATED);
    }

    private Calendar getCalendar(String cqLastReplicatedPublish, String cqLastReplicated)
            throws ParseException {
        Calendar calendar = Calendar.getInstance();
        if (this.jcrContentNode.has(cqLastReplicatedPublish)) {
            calendar.setTime(aemJsonDateFormat
                    .parse(this.jcrContentNode.getString(cqLastReplicatedPublish)));
        } else if (this.jcrContentNode.has(cqLastReplicated)) {
            calendar.setTime(
                    aemJsonDateFormat.parse(this.jcrContentNode.getString(cqLastReplicated)));
        }
        return calendar;
    }

    private Calendar getJcrLastModified() throws ParseException {
        return getCalendar(JCR_LASTMODIFIED, CQ_LAST_MODIFIED);
    }

    private boolean isActivated(String attribute) {
        return jcrContentNode.has(attribute)
                && this.jcrContentNode.getString(attribute).equals(ACTIVATE);
    }

    private void getDataFolder(JSONObject jcrContentNode) {
        if (TurAemCommonsUtils.hasProperty(jcrContentNode, DATA_FOLDER)) {
            JSONObject jcrDataRootNode = jcrContentNode.getJSONObject(DATA_FOLDER);
            if (TurAemCommonsUtils.hasProperty(jcrDataRootNode, CQ_MODEL)) {
                this.model = jcrDataRootNode.getString(CQ_MODEL);
            }
        }
    }

    public void setDataPath(String dataPath) {
        if (dataPath != null) {
            JSONObject dataJson = this.jcrContentNode;
            for (String node : dataPath.split("/")) {
                if (dataJson.has(node)) {
                    dataJson = dataJson.getJSONObject(node);
                } else {
                    return;
                }
            }
            JSONObject finalDataJson = dataJson;
            dataJson.keySet().stream().filter(key -> !key.endsWith("@LastModified"))
                    .forEach(key -> {
                        Object value = finalDataJson.get(key);
                        if (isDate(value.toString())) {
                            try {
                                TimeZone tz = TimeZone.getTimeZone(UTC);
                                DateFormat turingDateFormat = new SimpleDateFormat(DATE_FORMAT);
                                turingDateFormat.setTimeZone(tz);
                                this.attributes.put(key, turingDateFormat.format(
                                        aemJsonDateFormat.parse(value.toString()).getTime()));
                            } catch (ParseException e) {
                                log.error(e.getMessage(), e);
                            }
                        } else {
                            this.attributes.put(key, value);
                        }
                    });
        }
    }

    public boolean isDate(String dateStr) {
        try {
            aemJsonDateFormat.parse(dateStr);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
}
