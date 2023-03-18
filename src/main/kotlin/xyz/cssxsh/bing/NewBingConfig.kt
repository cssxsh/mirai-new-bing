package xyz.cssxsh.bing


public interface NewBingConfig {
    public val proxy: String
    public val doh: String
    public val ipv6: Boolean
    public val timeout: Long
    public val cookie: String
    public val device: String

    public val options: List<String>
    public val allowed: List<String>
}