import SwiftRs
import Tauri
import UIKit
import WebKit
import ObjectiveC
import Foundation
import UniformTypeIdentifiers

class ShareArgs: Decodable {
  let path: String
  let mime: String?
  let group: String?
}

struct FileInfo: Codable {
    let name: String
    let size: Int
    let mime: String
    let data: Data
}

class SharePlugin: Plugin {
	
	private func getPath(fileURL: String) -> String? {
			// Remove "/private" prefix if it exists
			var filePath = "";
			if fileURL.hasPrefix("/private") {
				filePath = fileURL.replacingOccurrences(of: "/private", with: "")
			} else {
				filePath = fileURL;
			}
			
			
			let homeDirectory = NSHomeDirectory();
			// Replace sandbox path with relative path
			var relativePath = filePath.replacingOccurrences(of: homeDirectory, with: "")
			if relativePath.hasPrefix("/") {
					relativePath = String(relativePath.dropFirst())
			}
			
			let path = URL(fileURLWithPath: homeDirectory).appendingPathComponent(relativePath).path
			
			NSLog("[shareFile]: Relative Path \(path)")
			
			// Check if the file exists
			if !FileManager.default.fileExists(atPath: path) {
					NSLog("[shareFile]: File does not exist \(path)")
					return nil
			}
			
			return path
	}
	private func shareFileByUrl(fileURL: String) {
			
			DispatchQueue.main.async {
					
					// Prepare content for sharing
					var activityItems = [Any]()
					
					if let path = self.getPath(fileURL: fileURL) {
						let relativeURL = URL(fileURLWithPath: path)
						activityItems.append(relativeURL)
					} else {
						NSLog("[shareFile]: Error: Unable to resolve path for file.")
						return
					}
					
					// Initialize UIActivityViewController
					let activityVC = UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
					
					// Present the activity view controller
					if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
					   let rootVC = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController {
						rootVC.present(activityVC, animated: true, completion: nil)
					} else {
						NSLog("[shareFile]: Error: rootVC is nil")
					}
			}
	}
	
	private func fetchAndDeleteFilesInAppGroupDirectory(group: String,folder: String) -> [FileInfo] {
	    var fileInfoArray: [FileInfo] = []
	    
	    // get App Group dir
	    guard let sharedContainerURL = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: group) else {
	        NSLog("Error: Unable to access App Group container")
	        return fileInfoArray
	    }
	    
	    let ztmTempDirectory = sharedContainerURL.appendingPathComponent(folder)
	    
	    do {
	        // Traverse files
	        let fileURLs = try FileManager.default.contentsOfDirectory(at: ztmTempDirectory, includingPropertiesForKeys: nil, options: [])
	        
	        for fileURL in fileURLs {
	            do {
	                // Read file data
									let fileData = try Data(contentsOf: fileURL)
									
									// Get file name
									let fileName = fileURL.lastPathComponent
									
									// Get file size
									let fileSize = try fileURL.resourceValues(forKeys: [.fileSizeKey]).fileSize ?? 0
									
									// Get MIME type
									var mime = "application/octet-stream"
									if #available(iOS 14.0, *), let utType = UTType(filenameExtension: fileURL.pathExtension) {
											mime = utType.preferredMIMEType ?? "application/octet-stream"
									}
									
									// Append file information to array
									let fileInfo = FileInfo(name: fileName, size: fileSize, mime: mime, data: fileData)
									fileInfoArray.append(fileInfo)
	                
	                try FileManager.default.removeItem(at: fileURL)
	                NSLog("File \(fileURL.lastPathComponent) read and deleted successfully")
	                
	            } catch {
	                NSLog("Error reading or deleting file at \(fileURL): \(error.localizedDescription)")
	            }
	        }
	        
	    } catch {
	        NSLog("Error accessing files in directory \(ztmTempDirectory): \(error.localizedDescription)")
	    }
	    
	    return fileInfoArray
	}
	
	private func fetchAndDeleteFilesPathInAppGroupDirectory(group: String,folder: String) -> [String] {
	    var filePathsArray: [String] = []
	    
	    // get App Group dir
	    guard let sharedContainerURL = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: group) else {
	        NSLog("Error: Unable to access App Group container")
	        return filePathsArray
	    }
	    
	    let ztmTempDirectory = sharedContainerURL.appendingPathComponent(folder)
	    
	    do {
	        // Traverse files
	        let fileURLs = try FileManager.default.contentsOfDirectory(at: ztmTempDirectory, includingPropertiesForKeys: nil, options: [])
	        
	        for fileURL in fileURLs {
	            filePathsArray.append(fileURL.path)
	        }
	        
	    } catch {
	        NSLog("Error accessing files in directory \(ztmTempDirectory): \(error.localizedDescription)")
	    }
	    
	    return filePathsArray
	}
  @objc public func shareFile(_ invoke: Invoke) throws {
    let args = try invoke.parseArgs(ShareArgs.self)
		self.shareFileByUrl(fileURL: args.path)
		NSLog("[shareFile]: \(args.path)")
		invoke.resolve(true)
  }
	
	@objc public func getSharedFiles(_ invoke: Invoke) throws {
	  let args = try invoke.parseArgs(ShareArgs.self)
		invoke.resolve(self.fetchAndDeleteFilesInAppGroupDirectory(group: (args.group ?? ""), folder:args.path))
	}
	@objc public func getSharedFilesPath(_ invoke: Invoke) throws {
	  let args = try invoke.parseArgs(ShareArgs.self)
		invoke.resolve(self.fetchAndDeleteFilesPathInAppGroupDirectory(group: (args.group ?? ""), folder:args.path))
	}
	
}

@_cdecl("init_plugin_share")
func initPlugin() -> Plugin {
  return SharePlugin()
}
