package com.plugin.share

import android.app.Activity
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.JSObject
import app.tauri.plugin.JSArray
import app.tauri.plugin.Plugin
import app.tauri.plugin.Invoke
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.json.JSONArray

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

		val paths: MutableList<String> = ArrayList()
		sharedPath.listFiles()?.forEach { file ->
			println("SharePlugin getSharedFilesPath: $file")
			paths.add(file.absolutePath)
		}
		val ret = JSObject()
		ret.put("paths", JSArray.from(paths.toTypedArray()))
		invoke.resolve(ret)
        
	}

	private fun fromU8Array(byteArray: ByteArray): JSONArray {
			val json = JSONArray()
			for (byte in byteArray) {
					json.put(byte)
			}
			return json
	}
	
	private fun recordToJson(record: ShareFile): JSObject {
		val json = JSObject()
		json.put("name", record.name)
		json.put("size", record.size)
		json.put("mime", record.mime)
		json.put("data", fromU8Array(record.data))
		return json
	}
	
	@Command
	fun getSharedFiles(invoke: Invoke) {
		val args = invoke.parseArgs(ShareOptions::class.java)
		val context = activity.applicationContext
		val appDir = context.filesDir
		val sharedPath = File(appDir, args.path)

		val files: MutableList<ShareFile> = ArrayList()
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
		val filesArray = Array(files.size) { index -> recordToJson(files[index]) }
		println("SharePlugin getSharedFiles: $files")
		val ret = JSObject()
		ret.put("files", JSArray.from(filesArray))
		invoke.resolve(ret)
        
	}
}
