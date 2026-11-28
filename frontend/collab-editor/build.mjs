import * as esbuild from 'esbuild';
await esbuild.build({
  entryPoints: ['collab-entry.js'],
  bundle: true, minify: true, format: 'iife', target: ['es2019'],
  outfile: 'dist/collab-editor.min.js', loader: { '.css': 'css' },
  legalComments: 'linked', logLevel: 'info'
});
