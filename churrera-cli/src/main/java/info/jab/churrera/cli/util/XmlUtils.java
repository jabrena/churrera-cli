package info.jab.churrera.cli.util;

/**
 * Utility class for general XML operations.
 * Provides methods for XML escaping and value extraction.
 */
public final class XmlUtils {

    private XmlUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Escapes special XML characters in a string.
     *
     * @param text the text to escape
     * @return the escaped text, or empty string if text is null
     */
    public static String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Unescapes XML entities in a string.
     *
     * @param text the text to unescape
     * @return the unescaped text
     */
    public static String unescapeXml(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("&apos;", "'")
                .replace("&quot;", "\"")
                .replace("&gt;", ">")
                .replace("&lt;", "<")
                .replace("&amp;", "&");
    }

    /**
     * Extracts a required XML value from an XML string.
     *
     * @param xml the XML string to parse
     * @param tagName the name of the XML tag to extract
     * @return the unescaped value between the tags
     * @throws IllegalArgumentException if the tag is not found
     */
    public static String extractXmlValue(String xml, String tagName) {
        String startTag = "<" + tagName + ">";
        String endTag = "</" + tagName + ">";
        int startIndex = xml.indexOf(startTag);
        if (startIndex == -1) {
            throw new IllegalArgumentException("Start tag not found: " + startTag);
        }
        int start = startIndex + startTag.length();
        int end = xml.indexOf(endTag, start);
        if (end == -1) {
            throw new IllegalArgumentException("End tag not found: " + endTag);
        }
        String value = xml.substring(start, end);
        return unescapeXml(value);
    }

    /**
     * Extracts an optional XML value from an XML string.
     *
     * @param xml the XML string to parse
     * @param tagName the name of the XML tag to extract
     * @return the unescaped value between the tags, or null if the tag is not found
     */
    public static String extractXmlValueOptional(String xml, String tagName) {
        String startTag = "<" + tagName + ">";
        String endTag = "</" + tagName + ">";
        int startIndex = xml.indexOf(startTag);
        if (startIndex == -1) {
            return null; // Tag not found, return null
        }
        int start = startIndex + startTag.length();
        int end = xml.indexOf(endTag, start);
        if (end == -1) {
            return null; // End tag not found, return null
        }
        String value = xml.substring(start, end);
        return unescapeXml(value);
    }
}

