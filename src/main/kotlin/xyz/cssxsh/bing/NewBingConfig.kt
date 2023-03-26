package xyz.cssxsh.bing


public interface NewBingConfig {
    public val proxy: String
    public val mirror: String get() = ""
    public val doh: String
    public val ipv6: Boolean
    public val timeout: Long
    public val cookie: String
    public val device: String
    public val language: String

    /**
     * * deepleo
     * * enable_debug_commands
     * * disable_emoji_spoken_text
     * * enablemm
     */
    public val options: List<String>

    /**
     * * Chat
     * * InternalSearchQuery
     * * InternalSearchResult
     * * Disengaged
     * * InternalLoaderMessage
     * * RenderCardRequest
     * * AdsQuery
     * * SemanticSerp
     * * GenerateContentQuery
     * * SearchQuery
     */
    public val allowed: List<String>

    /**
     * TODO
     */
    public val sliceIds: List<String> get() = emptyList()
}