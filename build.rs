const COMMANDS: &[&str] = &["share_file","get_shared_files","get_shared_files_path"];

fn main() {
  tauri_plugin::Builder::new(COMMANDS)
		.global_api_script_path("./api-iife.js")
    .android_path("android")
    .ios_path("ios")
    .build();
}
