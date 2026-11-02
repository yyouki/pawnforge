package com.youki.pawnforge

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File

class MainActivity : Activity() {

    private lateinit var editor: EditText
    private lateinit var lineNumbers: TextView
    private lateinit var logView: TextView
    private lateinit var statusView: TextView
    private lateinit var titleView: TextView
    private lateinit var compilerTab: TextView
    private lateinit var outputTab: TextView

    private var currentUri: Uri? = null
    private var workspaceTreeUri: Uri? = null
    private var stagedWorkspace: File? = null
    private var currentSourceFile: File? = null
    private var customIncludePaths = ""
    private var activeFileName = "gamemode.pwn"
    private var showingCompiler = true

    companion object {
        private const val REQUEST_OPEN_FILE = 1101
        private const val REQUEST_SAVE_FILE = 1102
        private const val REQUEST_OPEN_WORKSPACE = 1103
        private const val PREFS_NAME = "pawnforge_settings"

        private const val PAWN_SAMPLE = """#include <a_samp>

new PlayerCount = 0;

public OnGameModeInit()
{
    print("[GM] Gamemode berhasil di-load!");
    return 1;
}

public OnPlayerConnect(playerid)
{
    PlayerCount++;
    SendClientMessage(playerid, 0xFFFFFFFF, "Selamat datang!");
    return 1;
}
"""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.rgb(32, 45, 67)
        window.decorView.systemUiVisibility = 0

        // Restore the user-selected workspace and custom include directories.
        val preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        workspaceTreeUri = preferences.getString("workspace_tree_uri", null)?.let(Uri::parse)
        customIncludePaths = preferences.getString("custom_include_paths", "") ?: ""

        buildInterface()
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(205, 205, 205))
        }

        root.addView(buildTitleBar(), verticalParams(48))
        root.addView(buildMenuStrip(), verticalParams(34))
        root.addView(buildToolbar(), verticalParams(48))

        val editorArea = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
        }

        lineNumbers = TextView(this).apply {
            text = "1"
            typeface = Typeface.MONOSPACE
            textSize = 14f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.TOP or Gravity.RIGHT
            setPadding(dp(6), dp(9), dp(8), dp(8))
            setBackgroundColor(Color.rgb(235, 235, 235))
            minWidth = dp(40)
        }

        editor = EditText(this).apply {
            setText(PAWN_SAMPLE)
            typeface = Typeface.MONOSPACE
            textSize = 14f
            setTextColor(Color.rgb(25, 25, 25))
            setBackgroundColor(Color.WHITE)
            setPadding(dp(10), dp(8), dp(12), dp(10))
            gravity = Gravity.TOP or Gravity.START
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            isSingleLine = false
            setHorizontallyScrolling(true)
            setLineSpacing(0f, 1.12f)
        }

        editorArea.addView(
            lineNumbers,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        editorArea.addView(
            editor,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        )
        root.addView(editorArea, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        editor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateLineNumbers(s)
            }

            override fun afterTextChanged(editable: Editable?) {
                editable?.let { applySyntaxHighlighting(it) }
            }
        })
        updateLineNumbers(editor.text)
        applySyntaxHighlighting(editor.text)

        root.addView(buildPanelTabs(), verticalParams(36))

        val logScroll = ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
            isFillViewport = true
        }
        logView = TextView(this).apply {
            text = initialCompilerLog()
            typeface = Typeface.MONOSPACE
            textSize = 13f
            setTextColor(Color.rgb(28, 28, 28))
            setPadding(dp(12), dp(10), dp(12), dp(12))
            setTextIsSelectable(true)
        }
        logScroll.addView(logView)
        root.addView(logScroll, verticalParams(158))

        statusView = TextView(this).apply {
            text = "Line: 1     Col: 1     Ready."
            textSize = 12f
            setTextColor(Color.rgb(35, 35, 35))
            setPadding(dp(10), dp(5), dp(8), dp(5))
            setBackgroundColor(Color.rgb(226, 226, 226))
        }
        root.addView(statusView, verticalParams(28))
        root.addView(buildBottomNavigation(), verticalParams(58))

        setContentView(root)
    }

    private fun buildTitleBar(): View {
        val bar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), 0, dp(8), 0)
            setBackgroundColor(Color.rgb(10, 76, 174))
        }

        // Use the same vector mark as the Android launcher icon.
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.ic_pawnforge_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        bar.addView(logo, LinearLayout.LayoutParams(dp(34), dp(34)))

        titleView = TextView(this).apply {
            text = "PawnForge — [$activeFileName]"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        bar.addView(titleView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        return bar
    }

    private fun buildMenuStrip(): View {
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(Color.rgb(239, 239, 239))
        }
        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        listOf("File", "Edit", "Search", "Build", "Options", "Help").forEach { label ->
            val item = TextView(this).apply {
                text = label
                textSize = 15f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                setPadding(dp(13), 0, dp(13), 0)
                setOnClickListener { onMenuSelected(label) }
            }
            menu.addView(item, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
        }

        scroll.addView(menu)
        return scroll
    }

    private fun buildToolbar(): View {
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(Color.rgb(225, 225, 225))
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(3), dp(4), dp(3))
        }

        addToolbarButton(toolbar, "▯", 42) { newFile() }
        addToolbarButton(toolbar, "📂", 42) { openDocument() }
        addToolbarButton(toolbar, "▣", 42) { saveDocument() }
        addToolbarButton(toolbar, "↶", 42) { toast("Undo belum tersedia pada build awal.") }
        addToolbarButton(toolbar, "↷", 42) { toast("Redo belum tersedia pada build awal.") }
        addToolbarButton(toolbar, "▶ Compile", 112) { runCompile() }

        scroll.addView(toolbar)
        return scroll
    }

    private fun addToolbarButton(parent: LinearLayout, label: String, widthDp: Int, action: () -> Unit) {
        val button = TextView(this).apply {
            text = label
            textSize = if (label.length > 2) 14f else 22f
            typeface = if (label.length > 2) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setTextColor(Color.rgb(20, 20, 20))
            gravity = Gravity.CENTER
            background = beveledBackground(Color.rgb(248, 248, 248))
            setOnClickListener { action() }
        }
        val params = LinearLayout.LayoutParams(dp(widthDp), dp(40))
        params.setMargins(dp(2), 0, dp(2), 0)
        parent.addView(button, params)
    }

    private fun buildPanelTabs(): View {
        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.rgb(232, 232, 232))
            setPadding(dp(4), dp(2), dp(4), dp(2))
        }

        compilerTab = createTab("Compiler", true) {
            showingCompiler = true
            refreshPanel()
        }
        outputTab = createTab("Output", false) {
            showingCompiler = false
            refreshPanel()
        }

        tabs.addView(compilerTab, LinearLayout.LayoutParams(dp(92), ViewGroup.LayoutParams.MATCH_PARENT))
        tabs.addView(outputTab, LinearLayout.LayoutParams(dp(82), ViewGroup.LayoutParams.MATCH_PARENT))
        return tabs
    }

    private fun createTab(label: String, selected: Boolean, action: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            background = if (selected) beveledBackground(Color.rgb(194, 218, 248))
                else beveledBackground(Color.rgb(239, 239, 239))
            setOnClickListener { action() }
        }
    }

    private fun refreshPanel() {
        compilerTab.background = beveledBackground(
            if (showingCompiler) Color.rgb(194, 218, 248) else Color.rgb(239, 239, 239)
        )
        outputTab.background = beveledBackground(
            if (showingCompiler) Color.rgb(239, 239, 239) else Color.rgb(194, 218, 248)
        )
        logView.text = if (showingCompiler) initialCompilerLog()
        else "PawnForge Output\n\nNo build output yet.\nOpen a .pwn file and use Build > Compile."
    }

    private fun buildBottomNavigation(): View {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(32, 45, 67))
        }
        addBottomItem(nav, "⌂\nHome", true) {
            editor.requestFocus()
            toast("Editor aktif.")
        }
        addBottomItem(nav, "▱\nProjects", false) { showProjects() }
        addBottomItem(nav, "⚙\nSettings", false) { showSettings() }
        addBottomItem(nav, "ⓘ\nAbout", false) { showAbout() }
        return nav
    }

    private fun addBottomItem(parent: LinearLayout, label: String, selected: Boolean, action: () -> Unit) {
        val item = TextView(this).apply {
            text = label
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            if (selected) setBackgroundColor(Color.rgb(20, 62, 120))
            setOnClickListener { action() }
        }
        parent.addView(item, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
    }

    private fun onMenuSelected(label: String) {
        when (label) {
            "File" -> AlertDialog.Builder(this)
                .setTitle("File")
                .setItems(arrayOf("New", "Open", "Save", "Save As")) { _, which ->
                    when (which) {
                        0 -> newFile()
                        1 -> openDocument()
                        2 -> saveDocument()
                        3 -> saveDocumentAs()
                    }
                }.show()
            "Edit" -> toast("Gunakan keyboard Android untuk mengedit kode.")
            "Search" -> showSearchDialog()
            "Build" -> runCompile()
            "Options" -> showSettings()
            "Help" -> showAbout()
        }
    }

    private fun newFile() {
        activeFileName = "gamemode.pwn"
        currentUri = null
        currentSourceFile = null
        editor.setText(PAWN_SAMPLE)
        updateTitle()
        statusView.text = "New file created."
    }

    private fun openDocument() {
        // SAF lets the user choose a source file without broad storage permissions.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, REQUEST_OPEN_FILE)
    }

    private fun saveDocument() {
        val stagedFile = currentSourceFile
        if (stagedFile != null) {
            try {
                stagedFile.writeText(editor.text.toString(), Charsets.UTF_8)
                statusView.text = "Saved: $activeFileName"
                toast("Project file saved.")
            } catch (error: Exception) {
                toast("Save failed: ${error.message ?: "unknown error"}")
            }
            return
        }

        val uri = currentUri
        if (uri == null) saveDocumentAs() else writeToUri(uri)
    }

    private fun saveDocumentAs() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, activeFileName)
        }
        startActivityForResult(intent, REQUEST_SAVE_FILE)
    }

    @Deprecated("Uses Activity Result API compatibility for a dependency-free first build.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return

        when (requestCode) {
            REQUEST_OPEN_FILE -> {
                try {
                    val content = contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        ?: throw IllegalStateException("File could not be read.")

                    currentUri = uri
                    currentSourceFile = null
                    activeFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "untitled.pwn"
                    editor.setText(content)
                    updateTitle()
                    statusView.text = "Opened: $activeFileName"
                } catch (error: Exception) {
                    toast("Open failed: ${error.message ?: "unknown error"}")
                }
            }
            REQUEST_SAVE_FILE -> {
                currentUri = uri
                currentSourceFile = null
                writeToUri(uri)
            }
            REQUEST_OPEN_WORKSPACE -> {
                try {
                    // Keep access to the selected project folder after the app restarts.
                    val grantedFlags = data?.flags?.and(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    ) ?: (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    contentResolver.takePersistableUriPermission(uri, grantedFlags)
                    workspaceTreeUri = uri
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putString("workspace_tree_uri", uri.toString())
                        .apply()
                    statusView.text = "Workspace selected: ${workspaceLabel()}"
                    toast("Project folder selected.")
                } catch (error: Exception) {
                    toast("Workspace access failed: ${error.message ?: "unknown error"}")
                }
            }
        }
    }

    private fun writeToUri(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use {
                it.write(editor.text.toString())
            } ?: throw IllegalStateException("File could not be written.")

            activeFileName = uri.lastPathSegment?.substringAfterLast('/') ?: activeFileName
            updateTitle()
            statusView.text = "Saved: $activeFileName"
            toast("File saved.")
        } catch (error: Exception) {
            toast("Save failed: ${error.message ?: "unknown error"}")
        }
    }

    private fun runCompile() {
        showingCompiler = true
        refreshPanel()

        val root = stagedWorkspace
        if (root == null || !root.isDirectory) {
            logView.text = "No staged project workspace.\n\nOpen Projects > Choose project folder, then Prepare workspace for compiler."
            statusView.text = "Project workspace required."
            showProjects()
            return
        }

        val selected = currentSourceFile?.let { file ->
            runCatching {
                val canonicalRoot = root.canonicalPath
                val canonicalFile = file.canonicalPath
                file.isFile && canonicalFile.startsWith(canonicalRoot + File.separator)
            }.getOrDefault(false).let { inside -> if (inside) file else null }
        }

        if (selected != null) {
            compileStagedSource(root, selected)
        } else {
            showProjectSourcePicker(compileAfterSelection = true)
        }
    }

    private fun showProjectSourcePicker(compileAfterSelection: Boolean) {
        val root = stagedWorkspace
        if (root == null || !root.isDirectory) {
            toast("Prepare a project workspace first.")
            return
        }

        val sources = findPawnSources(root)
        if (sources.isEmpty()) {
            logView.text = "No .pwn, .pawn, or .p source files were found in this workspace."
            statusView.text = "No Pawn source files found."
            return
        }

        val labels = sources.map { file ->
            root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/')
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Pawn source")
            .setItems(labels) { _, which ->
                val selected = sources[which]
                openStagedSource(selected)
                if (compileAfterSelection) compileStagedSource(root, selected)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun findPawnSources(root: File): List<File> {
        val results = mutableListOf<File>()
        val excludedFolders = setOf(".git", "build", ".gradle", "output")

        fun visit(directory: File) {
            directory.listFiles()
                ?.sortedBy { it.name.lowercase() }
                ?.forEach { child ->
                    when {
                        child.isDirectory && child.name !in excludedFolders -> visit(child)
                        child.isFile && child.extension.lowercase() in setOf("pwn", "pawn", "p") ->
                            results.add(child)
                    }
                }
        }

        visit(root)
        return results.sortedBy { it.relativeTo(root).path.lowercase() }
    }

    private fun openStagedSource(sourceFile: File) {
        try {
            editor.setText(sourceFile.readText(Charsets.UTF_8))
            currentSourceFile = sourceFile
            currentUri = null
            activeFileName = sourceFile.name
            updateTitle()
            statusView.text = "Opened project source: $activeFileName"
        } catch (error: Exception) {
            toast("Could not open source: \${error.message ?: "unknown error"}")
        }
    }

    private fun compileStagedSource(root: File, sourceFile: File) {
        try {
            // Compile the editor buffer, not an older copy left on disk.
            if (currentSourceFile?.canonicalFile == sourceFile.canonicalFile) {
                sourceFile.writeText(editor.text.toString(), Charsets.UTF_8)
            }

            val builtInIncludes = ensureCompilerIncludes()
            val outputDirectory = File(root, "output")
            val outputFile = File(outputDirectory, sourceFile.nameWithoutExtension + ".amx")
            val request = PawnCompileRequest(
                sourceFile = sourceFile,
                workspaceRoot = root,
                outputFile = outputFile,
                customIncludePaths = customIncludePaths.lines(),
                extraIncludeDirectories = listOf(builtInIncludes),
                compilerFlags = listOf("-Z", "-O2")
            )
            val arguments = listOf("pawncc") + PawnCompilerCommand.buildArguments(request)
            val logFile = File(cacheDir, "pawnforge-compiler.log")
            val temporaryDirectory = File(cacheDir, "pawn-tmp")

            logView.text = buildString {
                appendLine("Pawn Community Compiler 3.10.10")
                appendLine("Source: \${sourceFile.relativeTo(root).path}")
                appendLine("Target: SA-MP / open.mp")
                appendLine("Output: \${outputFile.relativeTo(root).path}")
                appendLine()
                appendLine("Compiler arguments:")
                arguments.drop(1).forEach { appendLine("  $it") }
                appendLine()
                appendLine("Compiling...")
            }
            statusView.text = "Compiling $activeFileName..."

            // Run the native compiler off the UI thread; native output is captured to a log file.
            Thread {
                val result = runCatching {
                    PawnNativeCompiler.compile(
                        arguments = arguments,
                        logFile = logFile,
                        temporaryDirectory = temporaryDirectory,
                        workingDirectory = root,
                        outputFile = outputFile
                    )
                }

                runOnUiThread {
                    result.onSuccess { compileResult ->
                        val success = compileResult.exitCode == 0 && compileResult.outputFile.isFile
                        logView.text = buildString {
                            append(compileResult.output)
                            if (compileResult.output.isNotBlank() && !compileResult.output.endsWith("\n")) appendLine()
                            appendLine()
                            if (success) {
                                appendLine("BUILD SUCCEEDED")
                                appendLine("AMX: \${compileResult.outputFile.absolutePath}")
                            } else {
                                appendLine("BUILD FAILED")
                                appendLine("Compiler exit code: \${compileResult.exitCode}")
                                if (!compileResult.outputFile.exists()) appendLine("No AMX output file was produced.")
                            }
                        }
                        statusView.text = if (success) "Compile successful: \${outputFile.name}" else "Compile failed — inspect diagnostics."
                        if (success) toast("AMX generated: \${outputFile.name}")
                    }.onFailure { error ->
                        logView.text = "Compiler invocation failed.\n\${error.message ?: error.javaClass.simpleName}"
                        statusView.text = "Compiler invocation failed."
                        toast("Compile failed.")
                    }
                }
            }.start()
        } catch (error: Exception) {
            logView.text = "Could not start compiler.\n\${error.message ?: error.javaClass.simpleName}"
            statusView.text = "Compiler setup failed."
            toast("Compile setup failed.")
        }
    }

    private fun ensureCompilerIncludes(): File {
        val root = File(filesDir, "pawn-compiler")
        val includeDirectory = File(root, "include")
        if (includeDirectory.isDirectory && includeDirectory.list()?.isNotEmpty() == true) {
            return includeDirectory
        }

        fun copyAssetPath(assetPath: String, destination: File) {
            val children = assets.list(assetPath).orEmpty()
            if (children.isEmpty()) {
                destination.parentFile?.mkdirs()
                assets.open(assetPath).use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                }
                return
            }

            if (!destination.exists() && !destination.mkdirs()) {
                throw IllegalStateException("Could not create compiler include folder.")
            }
            children.forEach { child ->
                val childAsset = "$assetPath/$child"
                val childTarget = File(destination, child)
                val nested = assets.list(childAsset).orEmpty()
                if (nested.isNotEmpty()) {
                    copyAssetPath(childAsset, childTarget)
                } else {
                    childTarget.parentFile?.mkdirs()
                    assets.open(childAsset).use { input ->
                        childTarget.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }

        copyAssetPath("pawn-compiler/include", includeDirectory)
        if (!includeDirectory.isDirectory || includeDirectory.list()?.isEmpty() != false) {
            throw IllegalStateException("Bundled Pawn include files are missing from the APK.")
        }
        return includeDirectory
    }

    private fun showProjects() {
        AlertDialog.Builder(this)
            .setTitle("Projects — ${workspaceLabel()}")
            .setItems(arrayOf(
                "Choose project folder",
                "Prepare workspace for compiler",
                "Browse project .pwn files",
                "Open file from device",
                "Configure include directories"
            )) { _, which ->
                when (which) {
                    0 -> chooseWorkspaceFolder()
                    1 -> stageWorkspace()
                    2 -> showProjectSourcePicker(compileAfterSelection = false)
                    3 -> openDocument()
                    4 -> showIncludePathsDialog()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showSettings() {
        AlertDialog.Builder(this)
            .setTitle("Options")
            .setItems(arrayOf("Project workspace", "Include directories", "Compiler profile")) { _, which ->
                when (which) {
                    0 -> chooseWorkspaceFolder()
                    1 -> showIncludePathsDialog()
                    2 -> showCompilerProfile()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun stageWorkspace() {
        val selectedTree = workspaceTreeUri
        if (selectedTree == null) {
            toast("Choose a project folder first.")
            chooseWorkspaceFolder()
            return
        }

        statusView.text = "Preparing project workspace..."
        logView.text = "Copying project files while preserving folders..."

        // SAF copying is done off the UI thread so large gamemodes do not freeze the editor.
        Thread {
            val result = runCatching {
                WorkspaceStager(contentResolver, filesDir).stage(selectedTree)
            }

            runOnUiThread {
                result.onSuccess { stagedRoot ->
                    stagedWorkspace = stagedRoot
                    currentSourceFile = null
                    val directories = PawnIncludePaths.existingDirectories(
                        workspaceRoot = stagedRoot,
                        customRelativePaths = customIncludePaths.lines()
                    )
                    logView.text = buildString {
                        appendLine("Workspace prepared for Pawn compilation.")
                        appendLine("Project: ${stagedRoot.name}")
                        appendLine("Staged path: ${stagedRoot.absolutePath}")
                        appendLine()
                        appendLine("Resolved include search directories:")
                        directories.forEach { appendLine("  -i${it.absolutePath}") }
                        appendLine()
                        appendLine("Nested folders are preserved, including gamemode/.")
                        appendLine("Quoted includes resolve relative to their containing source file.")
                        appendLine("Angle-bracket includes use the -i directories above.")
                        appendLine()
                        appendLine("No compile was run: native Pawn Compiler is not integrated yet.")
                    }
                    statusView.text = "Workspace prepared: ${stagedRoot.name}"
                    toast("Workspace prepared.")
                }.onFailure { error ->
                    logView.text = "Workspace preparation failed. Check folder access and try again."
                    statusView.text = "Workspace preparation failed."
                    toast("Could not prepare workspace.")
                }
            }
        }.start()
    }

    private fun chooseWorkspaceFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
        }
        startActivityForResult(intent, REQUEST_OPEN_WORKSPACE)
    }

    private fun showIncludePathsDialog() {
        val input = EditText(this).apply {
            setText(customIncludePaths)
            hint = "One relative folder per line"
            minLines = 5
            maxLines = 10
            gravity = Gravity.TOP or Gravity.START
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        val help = TextView(this).apply {
            text = "Built-in paths: pawno/include, qawno/include, include, includes, gamemode, and project root (.).\nAdd custom folders relative to the project root, one per line."
            textSize = 13f
            setTextColor(Color.DKGRAY)
            setPadding(dp(12), dp(4), dp(12), dp(8))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(help)
            addView(input, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(150)
            ))
            setPadding(dp(16), dp(4), dp(16), 0)
        }

        AlertDialog.Builder(this)
            .setTitle("Pawn include directories")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                customIncludePaths = input.text.toString()
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString("custom_include_paths", customIncludePaths)
                    .apply()
                statusView.text = "Include path settings saved."
                toast("Include directories saved.")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCompilerProfile() {
        AlertDialog.Builder(this)
            .setTitle("Compiler profile")
            .setItems(arrayOf("SA-MP Legacy", "open.mp")) { _, which ->
                val profile = if (which == 0) "SA-MP Legacy" else "open.mp"
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString("compiler_profile", profile)
                    .apply()
                statusView.text = "Compiler profile: $profile"
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun configuredIncludePaths(): List<String> {
        val custom = PawnIncludePaths.sanitizeRelativePaths(
            customIncludePaths.lines()
        )
        return (PawnIncludePaths.defaultRelativeDirectories + custom).distinct()
    }

    private fun workspaceLabel(): String {
        val uri = workspaceTreeUri ?: return "not selected"
        return uri.lastPathSegment
            ?.substringAfterLast(':')
            ?.takeIf { it.isNotBlank() }
            ?: "selected folder"
    }

    private fun showSearchDialog() {
        val input = EditText(this).apply {
            hint = "Search in current file"
            setSingleLine(true)
        }
        AlertDialog.Builder(this)
            .setTitle("Search")
            .setView(input)
            .setPositiveButton("Find") { _, _ ->
                val query = input.text.toString()
                if (query.isNotBlank()) {
                    val index = editor.text.toString().indexOf(query, ignoreCase = true)
                    if (index >= 0) {
                        editor.requestFocus()
                        editor.setSelection(index, index + query.length)
                        statusView.text = "Found: $query"
                    } else {
                        toast("Text not found.")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle("PawnForge 0.1.0")
            .setMessage(
                "PawnForge is an Android Pawn editor inspired by Pawno/Qawno.\n\n" +
                    "This first build contains the editor interface and file handling. " +
                    "Compilation will become available after the Android Pawn compiler engine is integrated."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun updateTitle() {
        titleView.text = "PawnForge — [$activeFileName]"
    }

    private fun updateLineNumbers(text: CharSequence?) {
        val lineCount = (text?.count { it == '\n' } ?: 0) + 1
        lineNumbers.text = (1..lineCount).joinToString("\n")
    }

    private fun applySyntaxHighlighting(editable: Editable) {
        // Re-apply only color spans; the underlying source text and cursor remain unchanged.
        editable.getSpans(0, editable.length, ForegroundColorSpan::class.java)
            .forEach(editable::removeSpan)

        applyPattern(editable, Regex("\\b(public|stock|new|const|static|forward|native|return|if|else|for|while|switch|case|break|continue|sizeof|default)\\b"), Color.rgb(0, 45, 220))
        applyPattern(editable, Regex("\\b0[xX][0-9A-Fa-f]+|\\b[0-9]+\\b"), Color.rgb(190, 25, 35))
        applyPattern(editable, Regex("#[A-Za-z_]+"), Color.rgb(65, 45, 210))
        applyPattern(editable, Regex("[\\x22][^\\x22]*[\\x22]"), Color.rgb(20, 120, 40))
        applyPattern(editable, Regex("//[^\\n]*"), Color.rgb(120, 120, 120))
    }

    private fun applyPattern(editable: Editable, regex: Regex, color: Int) {
        regex.findAll(editable).forEach { match ->
            editable.setSpan(
                ForegroundColorSpan(color),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun initialCompilerLog(): String =
        "PawnForge Compiler Panel\n\n" +
            "Ready. Open a .pwn file and select Build > Compile.\n\n" +
            "Native Pawn compiler: bundled\n" +
            "Target: SA-MP / open.mp\n" +
            "Output format: .amx\n\n" +
            "Select a staged workspace and source file to compile offline."

    private fun beveledBackground(fillColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fillColor)
            setStroke(dp(1), Color.rgb(125, 125, 125))
            cornerRadius = dp(2).toFloat()
        }
    }

    private fun verticalParams(heightDp: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(heightDp)
        )

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
