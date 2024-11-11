import { readFileSync } from 'fs'
import { join } from 'path'
import { cwd } from 'process'
import typescript from '@rollup/plugin-typescript'
import { nodeResolve } from '@rollup/plugin-node-resolve'
import terser from '@rollup/plugin-terser'

const pkg = JSON.parse(readFileSync(join(cwd(), 'package.json'), 'utf8'))
const pName = "__TAURI_PLUGIN_SHARE__";
const sName = "share";
export default [{
  input: 'guest-js/index.ts',
  output: [
    {
      file: pkg.exports.import,
      format: 'esm'
    },
    {
      file: pkg.exports.require,
      format: 'cjs'
    }
  ],
  plugins: [
    typescript({
      declaration: true,
      declarationDir: `./${pkg.exports.import.split('/')[0]}`
    })
  ],
  external: [
    /^@tauri-apps\/api/,
    ...Object.keys(pkg.dependencies || {}),
    ...Object.keys(pkg.peerDependencies || {})
  ]
},{
	input: './guest-js/index.ts',
	output: {
		format: "iife",
		name: pName,
		banner: "if ('__TAURI__' in window) {",
		footer: `Object.defineProperty(window.__TAURI__, '${sName}', { value: ${pName} }) }`,
		file: "api-iife.js",
	},
	plugins: [typescript(), terser(), nodeResolve()],
	onwarn: (warning) => {
		throw Object.assign(new Error(), warning);
	},
}]
