package nu.validator.saxtree;

public final class PrefixMapping {
    private final String prefix;
    private final String uri;
    /**
     * @param prefix
     * @param uri
     */
    public PrefixMapping(final String prefix, final String uri) {
        this.prefix = prefix;
        this.uri = uri;
    }
    /**
     * Returns the prefix.
     * 
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }
    /**
     * Returns the uri.
     * 
     * @return the uri
     */
    public String getUri() {
        return uri;
    }
}
