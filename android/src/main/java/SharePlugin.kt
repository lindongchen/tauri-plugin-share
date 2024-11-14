package com.plugin.share

import android.app.Activity
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin
import app.tauri.plugin.Invoke
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import com.google.gson.JsonArray
import com.google.gson.Gson

@InvokeArg
class ShareOptions {
    var path: String = ""
    var mime: String = "text/plain"
    var group: String = ""
}

@InvokeArg
class ShareFile {
    var name: String = ""
    var size: Long = 0
    var mime: String = ""
    var data: ByteArray =  ByteArray(1024)
}
@TauriPlugin
class SharePlugin(private val activity: Activity): Plugin(activity) {

	@Command
	fun shareFile(invoke: Invoke) {
		val args = invoke.parseArgs(ShareOptions::class.java)

		val context = activity.applicationContext
	 
		val file = File(args.path)
        println("SharePlugin getSharedFiles: $args.path")
		val uri = FileProvider.getUriForFile(
			context,
			"${context.packageName}.fileprovider",
			file
		)
		val intent = Intent(Intent.ACTION_SEND)
		intent.setType(args.mime);
		intent.putExtra(Intent.EXTRA_STREAM, uri);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent)
	}

	@Command
	fun getSharedFilesPath(invoke: Invoke) {
		val args = invoke.parseArgs(ShareOptions::class.java)
		val context = activity.applicationContext
		val appDir = context.filesDir
        val sharedPath = File(appDir, args.path)

		val paths = ArrayList<String>()
		sharedPath.listFiles()?.forEach { file ->
        	println("SharePlugin getSharedFilesPath: $file")
            paths.add(file.absolutePath)
        }
		val ret = JSObject()
		ret.put("paths", paths.joinToString(";"))
		invoke.resolve(ret)
        
	}

	@Command
	fun getSharedFiles(invoke: Invoke) {
		val args = invoke.parseArgs(ShareOptions::class.java)
		val context = activity.applicationContext
		val appDir = context.filesDir
        val sharedPath = File(appDir, args.path)

		val files = ArrayList<ShareFile>()
		sharedPath.listFiles()?.forEach { file ->
			val path: Path = Paths.get(file.absolutePath)
			val mimeType = Files.probeContentType(path)
			val fileData: ByteArray = Files.readAllBytes(path)
			val fileObj = ShareFile()
			fileObj.name = file.name
			fileObj.size = file.length()
			fileObj.mime = mimeType
			fileObj.data = fileData
            files.add(fileObj)

			file.delete()
        }
        println("SharePlugin getSharedFiles: $files")
		val gson = Gson()
		val jsonArray = gson.toJsonTree(files).asJsonArray
		val ret = JSObject()
		ret.put("files", jsonArray)
		invoke.resolve(ret)
        
	}
}
