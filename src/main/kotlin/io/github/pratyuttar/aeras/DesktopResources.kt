package io.github.pratyuttar.aeras

import com.junkfood.seal.ResourceIndex

object DesktopResources {
    /** Classpath location of the drawable with the given R.drawable id. */
    fun drawablePath(id: Int): String {
        val file = ResourceIndex.drawableFiles.getOrNull(id - ResourceIndex.DRAWABLE_BASE)
        return "aeras/drawable/" + (file ?: "sample.jpg")
    }
}
