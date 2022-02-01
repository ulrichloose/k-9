package app.k9mail.html.cleaner

import org.jsoup.nodes.Document
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Whitelist as AllowList

internal class BodyCleaner {
    private val cleaner: Cleaner
    private val allowedBodyAttributes = setOf(
        "id", "class", "dir", "lang", "style",
        "alink", "background", "bgcolor", "link", "text", "vlink"
    )

    init {
        val allowList = AllowList.relaxed()
            .addTags("font", "hr", "ins", "del", "center", "map", "area", "title")
            .addAttributes("font", "color", "face", "size")
            .addAttributes(
                "table", "align", "background", "bgcolor", "border", "cellpadding", "cellspacing",
                "width"
            )
            .addAttributes("tr", "align", "background", "bgcolor", "valign")
            .addAttributes(
                "th",
                "align", "background", "bgcolor", "colspan", "headers", "height", "nowrap", "rowspan", "scope",
                "sorted", "valign", "width"
            )
            .addAttributes(
                "td",
                "align", "background", "bgcolor", "colspan", "headers", "height", "nowrap", "rowspan", "scope",
                "valign", "width"
            )
            .addAttributes("map", "name")
            .addAttributes("area", "shape", "coords", "href", "alt")
            .addProtocols("area", "href", "http", "https")
            .addAttributes("img", "usemap")
            .addAttributes(":all", "class", "style", "id", "dir")
            .addProtocols("img", "src", "http", "https", "cid", "data")
            .addProtocols("a", "href", "tel", "sip", "bitcoin", "ethereum", "rtsp")

        cleaner = Cleaner(allowList)
    }

    fun clean(dirtyDocument: Document): Document {
        val cleanedDocument = cleaner.clean(dirtyDocument)
        copyBodyAttributes(dirtyDocument, cleanedDocument)
        return cleanedDocument
    }

    private fun copyBodyAttributes(dirtyDocument: Document, cleanedDocument: Document) {
        val cleanedBody = cleanedDocument.body()
        for (attribute in dirtyDocument.body().attributes()) {
            if (attribute.key !in allowedBodyAttributes) continue

            if (attribute.hasDeclaredValue()) {
                cleanedBody.attr(attribute.key, attribute.value)
            } else {
                cleanedBody.attr(attribute.key, true)
            }
        }
    }
}
