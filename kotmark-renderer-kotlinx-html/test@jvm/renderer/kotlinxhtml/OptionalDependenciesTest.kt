package io.heapy.kotmark.renderer.kotlinxhtml

import io.heapy.kotmark.ext.highlight.HighlightExtension
import io.heapy.kotmark.ext.math.MathExtension
import io.heapy.kotmark.parser.Parser
import java.net.URLClassLoader
import java.lang.reflect.Proxy
import kotlinx.html.TagConsumer
import kotlin.test.*

/** Tests the actual production outputs with a classloader containing no unselected extensions. */
class OptionalDependenciesTest {
    @Test
    fun plainRendererLoadsWithoutAnyExtensions() {
        isolated().use { loader ->
            assertFailsWith<ClassNotFoundException> { loader.loadClass("io.heapy.kotmark.ext.math.MathExtension") }
            assertFailsWith<ClassNotFoundException> { loader.loadClass("io.heapy.kotmark.ext.highlight.HighlightExtension") }
            assertNotNull(loader.getResource("io/heapy/kotmark/renderer/kotlinxhtml/adapters/MathAdapterKt.class"))
            assertEquals("<p>plain</p>", render(loader, "plain"))
        }
    }

    @Test
    fun eachAdapterWorksWithoutTheOtherExtensions() {
        isolated(MathExtension::class.java).use { loader ->
            assertFailsWith<ClassNotFoundException> { loader.loadClass("io.heapy.kotmark.ext.highlight.HighlightExtension") }
            assertEquals("<p><span class=\"math math-inline\">x</span></p>",
                render(loader, "\$x\$", "io.heapy.kotmark.ext.math.MathExtension", "MathAdapterKt", "mathAdapter"))
        }
        isolated(HighlightExtension::class.java).use { loader ->
            assertFailsWith<ClassNotFoundException> { loader.loadClass("io.heapy.kotmark.ext.math.MathExtension") }
            assertEquals("<p><mark>x</mark></p>",
                render(loader, "==x==", "io.heapy.kotmark.ext.highlight.HighlightExtension", "HighlightAdapterKt", "highlightAdapter"))
        }
    }

    private fun isolated(vararg extensions: Class<*>): URLClassLoader {
        val classes = listOf(KotlinxHtmlRenderer::class.java, Parser::class.java, TagConsumer::class.java, Unit::class.java) + extensions
        return URLClassLoader(classes.map { it.protectionDomain.codeSource.location }.distinct().toTypedArray(), ClassLoader.getPlatformClassLoader())
    }

    private fun render(loader: ClassLoader, source: String, extensionClass: String? = null, adapterFile: String? = null, adapterFunction: String? = null): String {
        val function = loader.loadClass("kotlin.jvm.functions.Function1")
        val unit = loader.loadClass("kotlin.Unit").getField("INSTANCE").get(null)
        fun configure(body: (Any) -> Unit): Any = Proxy.newProxyInstance(loader, arrayOf(function)) { _, method, args ->
            check(method.name == "invoke")
            body(args[0])
            unit
        }
        val parser = loader.loadClass("io.heapy.kotmark.parser.ParserKt").getMethod("parser", function).invoke(null, configure { config ->
            if (extensionClass != null) {
                val extension = loader.loadClass(extensionClass).getMethod("create").invoke(null)
                config.javaClass.getMethod("extensions", Iterable::class.java).invoke(config, listOf(extension))
            }
        })
        val document = parser.javaClass.getMethod("parse", String::class.java).invoke(parser, source)
        val renderer = loader.loadClass("io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlRendererKt")
            .getMethod("kotlinxHtmlRenderer", function).invoke(null, configure { config ->
                if (adapterFile != null) {
                    val adapter = loader.loadClass("io.heapy.kotmark.renderer.kotlinxhtml.adapters.$adapterFile")
                        .getMethod(adapterFunction).invoke(null)
                    config.javaClass.getMethod("adapters", Iterable::class.java).invoke(config, listOf(adapter))
                }
            })
        return renderer.javaClass.getMethod("render", loader.loadClass("io.heapy.kotmark.node.Node")).invoke(renderer, document) as String
    }
}
