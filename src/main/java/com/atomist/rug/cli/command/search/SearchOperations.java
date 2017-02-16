package com.atomist.rug.cli.command.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.springframework.util.StringUtils;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.utils.HttpClientFactory;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

public class SearchOperations {
    
    public List<Operation> collectResults(String endpoint, String search, String type,
            Properties tags, Settings settings) {

        if (!endpoint.endsWith(Constants.CATALOG_PATH)) {
            if (!endpoint.endsWith("/")) {
                endpoint = endpoint + "/";
            }
            endpoint = endpoint + Constants.CATALOG_PATH;
        }

        HttpClient client = HttpClientFactory.httpClient(endpoint);
        HttpPost post = new HttpPost(endpoint);
        HttpClientFactory.authorizationHeader(post, settings.getToken());
        HttpClientFactory.body(post, getSearchQuery(search, type, tags));

        Optional<Operations> operations = HttpClientFactory.executeAndRead(client, post,
                new TypeReference<Operations>() {});
        
        if (operations.isPresent()) {
            return operations.get().operations();
        }
        else {
            return Collections.emptyList();
        }
    }

    private String getSearchQuery(String search, String type, Properties tags) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"queries\": [{ ");
        List<String> queries = new ArrayList<>();
        if (search != null && search.length() > 0) {
            queries.add(String.format("\"search\": \"%s\"", search));
        }
        if (tags.size() > 0) {
            queries.add(String.format("\"tags\": [%s]", toCommaSeperatedList(tags)));
        }
        if (type != null) {
            queries.add(String.format("\"operation\": { \"type\": \"%s\" }", type));
        }
        sb.append(StringUtils.collectionToCommaDelimitedString(queries));
        sb.append(" }]}");
        return sb.toString();
    }

    private String toCommaSeperatedList(Properties tags) {
        if (tags.isEmpty()) {
            return "";
        }
        else {
            return StringUtils.collectionToCommaDelimitedString(
                    tags.keySet().stream().map(k -> "\"" + k + "\"").collect(Collectors.toList()));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Operations {
        @JsonProperty
        private List<Operation> operations;

        public List<Operation> operations() {
            return operations;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Operation {
        @JsonProperty
        private String type;
        @JsonProperty
        private String name;
        @JsonProperty
        private Archive archive;

        public String name() {
            return name;
        }

        public String type() {
            return type;
        }

        public Archive archive() {
            return archive;
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Archive {
        @JsonProperty
        private String group;
        @JsonProperty
        private String artifact;
        @JsonProperty
        private Version version;

        public String group() {
            return group;
        }

        public String artifact() {
            return artifact;
        }

        public Version version() {
            return version;
        }

        public String key() {
            return String.format("%s:%s:%s", group, artifact, version.value);
        }
    }

    public static class Version {
        @JsonProperty
        private String value;

        public String value() {
            return value;
        }
    }
}   