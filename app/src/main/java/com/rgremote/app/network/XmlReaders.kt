package com.rgremote.app.network

import com.rgremote.app.domain.ActiveApp
import com.rgremote.app.domain.HdmiPort
import com.rgremote.app.domain.RokuApp
import com.rgremote.app.domain.RokuDeviceInfo
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource

object RokuXmlReaders {
    fun deviceInfo(xml: String): RokuDeviceInfo {
        val root = parse(xml).documentElement
        return RokuDeviceInfo(
            friendlyName = root.text("friendly-device-name") ?: root.text("user-device-name"),
            serialNumber = root.text("serial-number"),
            powerMode = root.text("power-mode"),
            modelName = root.text("model-name"),
            modelNumber = root.text("model-number"),
            networkType = root.text("network-type"),
            isTv = root.booleanText("is-tv"),
            supportsTvPowerControl = root.booleanText("supports-tv-power-control"),
            supportsAudioVolumeControl = root.booleanText("supports-audio-volume-control"),
            wifiMac = root.text("wifi-mac"),
            ethernetMac = root.text("ethernet-mac")
        )
    }

    fun activeApp(xml: String): ActiveApp? {
        val apps = parse(xml).getElementsByTagName("app")
        if (apps.length == 0) return null
        val app = apps.item(0) as? Element ?: return null
        val id = app.getAttribute("id").takeIf { it.isNotBlank() } ?: return null
        return ActiveApp(
            id = id,
            name = app.textContent?.trim().orEmpty().ifBlank { id },
            inferredHdmiPort = HdmiPort.fromActiveAppId(id)
        )
    }

    fun apps(xml: String): List<RokuApp> {
        val nodes = parse(xml).getElementsByTagName("app")
        return buildList {
            for (index in 0 until nodes.length) {
                val node = nodes.item(index) as? Element ?: continue
                val id = node.getAttribute("id").takeIf { it.isNotBlank() } ?: continue
                add(
                    RokuApp(
                        id = id,
                        name = node.textContent?.trim().orEmpty().ifBlank { id },
                        type = node.getAttribute("type").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }

    private fun parse(xml: String) =
        DocumentBuilderFactory.newInstance().apply {
            runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
            runCatching { isXIncludeAware = false }
            isExpandEntityReferences = false
        }
            .newDocumentBuilder()
            .parse(InputSource(StringReader(xml)))

    private fun Element.text(tag: String): String? =
        getElementsByTagName(tag)
            .item(0)
            ?.textContent
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    private fun Element.booleanText(tag: String): Boolean? =
        when (text(tag)?.lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }
}
