# Tauri share file and get shared plugin

## Share file
```typescript
import { shareFile } from 'tauri-plugin-share'
shareFile(path, mime)
```
- or
```typescript
import { invoke } from '@tauri-apps/api/core'
invoke('plugin:share|share_file', {
	path, // ios: /private/var...  | android: /storage/emulated/0/Android...
	mime // application/pdf | application/zip ....
})
```

## From others app shared
- ios: First, you need to implement a Share Extension target on xcode, and store the file in the app group workspace, after completing these, use the plugin to get and reomve files shared by other applications.
- android: Mainly used keep pace with ios, so it doesn't depend on plugins, it depends on the specific implementation of the sharing goal. I'm used to putting them under $DOCUMENT/temp
- path: [ios] a custom folder under app group dir | [android] a custom folder under $DOCUMENT dir,  eg: temp
- group: [ios] your Share Extension Target's app group id | [android] empty, eg:group.com.xxxx.xxx
### Get files path from other app shared 
```typescript
import { getSharedFilesPath } from 'tauri-plugin-share'
getSharedFilesPath(group, path)
```
- or
```typescript
import { invoke } from '@tauri-apps/api/core'
invoke('plugin:share|get_shared_files_path', { group, path })
```

### Get and remove files data from other app shared 
```typescript
import { getSharedFiles } from 'tauri-plugin-share'
getSharedFiles(group, path)
```
- or
```typescript
import { invoke } from '@tauri-apps/api/core'
invoke('plugin:share|get_shared_files', { group, path })
```