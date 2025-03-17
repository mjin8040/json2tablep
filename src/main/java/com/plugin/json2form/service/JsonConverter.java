package com.plugin.json2form.service;

import java.util.*;

public class JsonConverter {
    private final boolean buildTopToBottom;
    private final String tableOpeningTag;
    
    public JsonConverter(BuildDirection buildDirection, Map<String, String> tableAttributes) {
        this.buildTopToBottom = buildDirection == BuildDirection.TOP_TO_BOTTOM;
        this.tableOpeningTag = "<table" + dictToHtmlAttributes(tableAttributes) + ">";
    }
    
    public String convert(Map<String, Object> jsonInput) {
        StringBuilder html = new StringBuilder(tableOpeningTag);
        
        if (buildTopToBottom) {
            html.append(markupHeaderRow(jsonInput.keySet()));
            html.append("<tr>");
            for (var entry : jsonInput.entrySet()) {
                html.append(markup(entry.getValue(), entry.getKey()));
            }
            html.append("</tr>");
            html.append("</table>");
            return html.toString();
        } else {
            for (Map.Entry<String, Object> entry : jsonInput.entrySet()) {
                html.append("<tr><th>").append(markup(entry.getKey(), "")).append("</th>");
                markup(entry.getValue(), "");
                html.append("</tr>");
            }
        }
        
        html.append("</table>");
        return html.toString();
    }
    
    private String markup(Object entry, String key) {
        return switch (entry) {
            case null -> markupCell("null", key);
            case List list -> markup(list, key);
            case Map map -> //noinspection unchecked
                    markup((Map<String, Object>)map);
            default -> markupCell(entry, key);
        };
    }

    private String markupCell(Object value, String key) {
        return String.format("<td mkey='%s'>%s</td>", key, value);
    }

    private String markup(Map<String, Object> map) {
        StringBuilder html = new StringBuilder("<div class=\"list-wrapper\">");
        html.append("<table>");
        html.append(markupHeaderRow(map.keySet()));
        html.append("<tr>");
        for (String key : map.keySet()) {
            html.append(markup(map.get(key), key));
        }
        html.append("</tr>");
        html.append("</table>");
        return wrapExpandable(html.toString());
    }

    private String markup(List<?> entry, String key) {
        List<?> list = entry;
        Set<String> commonHeaders = getCommonHeaders(entry);
        if (commonHeaders != null) {
            return clubListEntries(list, commonHeaders);
        }
        // list doesn't have common headers so just display the list
        StringBuilder html = new StringBuilder(tableOpeningTag);
        html.append("<tr>");
        for (Object o : list) {
            html.append(markup(o, key));
        }
        html.append("</tr>");
        html.append("</table>");
        return wrapExpandable(html.toString());
    }

    private String clubListEntries(List<?> listOfDicts, Set<String> columnHeaders) {
        StringBuilder html = new StringBuilder(tableOpeningTag);
        html.append(markupHeaderRow(columnHeaders));

        for (Object item : listOfDicts) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dict = (Map<String, Object>) item;
            html.append("<tr>");
            for (String columnHeader : columnHeaders) {
                html.append(markup(dict.get(columnHeader), columnHeader));
            }
            html.append("</tr>");
        }

        html.append("</table>");
        return wrapExpandable(html.toString());
    }

    private Set<String> getCommonHeaders(List<?> listOfDicts) {
        Set<String> headers;
        if (listOfDicts.isEmpty()) {
            return null; // no common headers
        }
        if (listOfDicts.getFirst() instanceof Map map) {
           headers = map.keySet();
        } else {
           return null; // cannot determine common headers
        }

        for (int i = 1; i < listOfDicts.size(); i++) {
            Object item = listOfDicts.get(i);
            if (!(item instanceof Map)) {
                return null; // cannot determine common headers if entries are not dictionaries
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> dict = (Map<String, Object>) item;
            if (!headers.equals(dict.keySet())) {
                return null; // there are additional or missing headers
            }
        }

        return headers;
    }

    private String markupHeaderRow(Collection<String> headers) {
        return "<tr><th>" + String.join("</th><th>", headers) + "</th></tr>";
    }

    private static String dictToHtmlAttributes(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            result.append(String.format(" %s=\"%s\"", entry.getKey(), entry.getValue()));
        }
        return result.toString();
    }

    private String wrapExpandable(String cellContent) {
        String toggleContent = wrapWithToggle(cellContent);
        String cellClass = " class=\"has-children\"";
        return String.format("<td%s>%s</td>", cellClass, toggleContent);
    }

    private String wrapWithToggle(String content) {
        return String.format(
                "<div class=\"cell-content\">" +
                        "<span class=\"toggle-btn expanded\" onclick=\"toggleNode(this)\"></span>" +
                        "<div class=\"content-wrapper\">%s</div>" +
                        "</div>",
                content
        );
    }
} 