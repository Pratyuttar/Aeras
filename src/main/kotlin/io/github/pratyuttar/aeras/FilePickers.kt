package io.github.pratyuttar.aeras

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager

/** Native file dialogs. Each call blocks until the dialog is closed and returns null on cancel. */
object FilePickers {
    @Volatile var ownerFrame: Frame? = null

    fun openFile(title: String = ""): File? {
        val dialog = FileDialog(ownerFrame, title, FileDialog.LOAD)
        dialog.isVisible = true
        val name = dialog.file ?: return null
        return File(dialog.directory, name)
    }

    fun saveFile(suggestedName: String = "", title: String = ""): File? {
        val dialog = FileDialog(ownerFrame, title, FileDialog.SAVE)
        if (suggestedName.isNotEmpty()) dialog.file = suggestedName
        dialog.isVisible = true
        val name = dialog.file ?: return null
        return File(dialog.directory, name)
    }

    fun chooseDirectory(initial: File? = null, title: String = ""): File? {
        runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }
        val chooser = JFileChooser(initial?.takeIf { it.exists() } ?: AppDirs.systemDownloads)
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        chooser.isAcceptAllFileFilterUsed = false
        if (title.isNotEmpty()) chooser.dialogTitle = title
        return if (chooser.showOpenDialog(ownerFrame) == JFileChooser.APPROVE_OPTION) chooser.selectedFile
        else null
    }
}
