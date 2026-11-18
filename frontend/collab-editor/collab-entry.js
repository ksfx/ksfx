// KSFX collaborative wiki editor bundle (branch multiwiki): Milkdown (WYSIWYG, Markdown-native,
// ProseMirror) + Yjs (CRDT) + y-protocols awareness, exposed as window.KsfxCollab. The transport
// (long-polling against WikiCollabController) lives in wiki-collab.js, not here - this bundle only
// builds editors and offers the merge helper. Build: npm run build (esbuild, one IIFE file).
import { Editor, rootCtx, editorViewCtx, editorViewOptionsCtx, remarkStringifyOptionsCtx } from '@milkdown/kit/core';
import {
    commonmark, toggleStrongCommand, toggleEmphasisCommand, toggleInlineCodeCommand, wrapInHeadingCommand,
    wrapInBulletListCommand, wrapInOrderedListCommand, wrapInBlockquoteCommand, createCodeBlockCommand,
    insertHrCommand, turnIntoTextCommand, toggleLinkCommand, insertImageCommand
} from '@milkdown/kit/preset/commonmark';
import { gfm, insertTableCommand, toggleStrikethroughCommand, remarkGFMPlugin } from '@milkdown/kit/preset/gfm';
import { listener, listenerCtx } from '@milkdown/kit/plugin/listener';
import { clipboard } from '@milkdown/kit/plugin/clipboard';
import { upload, uploadConfig } from '@milkdown/kit/plugin/upload';
import { callCommand, getMarkdown, replaceAll } from '@milkdown/kit/utils';
import { collab, collabServiceCtx } from '@milkdown/plugin-collab';
import * as Y from 'yjs';
import { Awareness, encodeAwarenessUpdate, applyAwarenessUpdate, removeAwarenessStates } from 'y-protocols/awareness';
import { diff3Merge } from 'node-diff3';

import '@milkdown/kit/prose/view/style/prosemirror.css';
import '@milkdown/kit/prose/tables/style/tables.css';

const COMMANDS = {
    bold: () => [toggleStrongCommand.key],
    italic: () => [toggleEmphasisCommand.key],
    strike: () => [toggleStrikethroughCommand.key],
    code: () => [toggleInlineCodeCommand.key],
    h1: () => [wrapInHeadingCommand.key, 1],
    h2: () => [wrapInHeadingCommand.key, 2],
    h3: () => [wrapInHeadingCommand.key, 3],
    paragraph: () => [turnIntoTextCommand.key],
    bullet: () => [wrapInBulletListCommand.key],
    ordered: () => [wrapInOrderedListCommand.key],
    quote: () => [wrapInBlockquoteCommand.key],
    codeblock: () => [createCodeBlockCommand.key],
    hr: () => [insertHrCommand.key],
    table: () => [insertTableCommand.key],
    link: () => {
        const href = window.prompt('Link URL:');
        return href ? [toggleLinkCommand.key, { href }] : null;
    }
};

/**
 * Milkdown does not know the wiki's own syntax, so its Markdown output needs two corrections:
 * - [[wikilinks]] come out escaped (\[\[Setup]] - remark guards "[" in plain text), which the
 *   wiki would no longer resolve: the escapes inside a [[...]] are removed again;
 * - bare URLs come out as <https://...> autolinks: back to the bare form the wiki and the agents
 *   write (GFM links them either way), so saving does not rewrite untouched lines;
 * - table delimiters come out as ":-" (left-aligned, the default anyway): back to "---".
 * Remaining harmless differences: remark escapes some characters in plain text (2\*3, \[x]) - they
 * render exactly the same.
 */
function normalizeMarkdown(markdown) {
    return markdown
        .replace(/\\\[\\\[((?:[^\]\n]|\\\])+?)\\?\]\\?\]/g, (match, inner) => '[[' + inner.replace(/\\([\\`*_{}\[\]()#+\-.!|~<>])/g, '$1') + ']]')
        .replace(/<(https?:\/\/[^>\s]+)>/g, '$1')
        // Table delimiter rows: Milkdown marks every column left-aligned (":-"); left is the default,
        // so write the plain "---" the wiki and the agents use.
        .replace(/^\|(?: *:?-+:? *\|)+ *$/gm, (row) => row.replace(/(\| *):-( *)(?=\|)/g, '$1---$2'));
}

/**
 * Creates a collaborative editor in `el`, bound to `ydoc` and `awareness`. `initialMarkdown` is
 * only applied when this client was told to initialize the shared document (exactly one client per
 * session - see WikiCollabService), otherwise the content arrives as Yjs updates.
 */
async function createEditor({ el, ydoc, awareness, initialMarkdown, onChange, uploadFile }) {
    const editor = await Editor.make()
        .config((ctx) => {
            ctx.set(rootCtx, el);
            ctx.update(editorViewOptionsCtx, (prev) => ({ ...prev, attributes: { class: 'ksfx-collab-prose', spellcheck: 'true' } }));
            // Write Markdown the way the wiki (Toast UI) and the agents do - "-" bullets, "*"
            // emphasis - so saving through this editor does not rewrite untouched lines.
            ctx.update(remarkStringifyOptionsCtx, (prev) => ({ ...prev, bullet: '-', emphasis: '*', strong: '*', rule: '-', fence: '`' }));
            // Compact tables like Toast UI / the agents write them - no column padding.
            ctx.set(remarkGFMPlugin.options.key, { tablePipeAlign: false });
            // Pasted / dropped images go to the server first (uploadFile -> URL); only the image node
            // with that URL enters the document - so a live session shares a reference, never the file.
            ctx.update(uploadConfig.key, (prev) => ({
                ...prev,
                uploader: async (files, schema) => {
                    const nodes = [];

                    for (let i = 0; i < files.length; i++) {
                        const file = files.item(i);

                        if (file && file.type.startsWith('image/') && uploadFile) {
                            const src = await uploadFile(file);
                            nodes.push(schema.nodes.image.createAndFill({ src, alt: imageAlt(file) }));
                        }
                    }

                    return nodes;
                }
            }));
            ctx.get(listenerCtx).markdownUpdated((_ctx, markdown, prevMarkdown) => {
                if (onChange && markdown !== prevMarkdown) {
                    onChange(normalizeMarkdown(markdown));
                }
            });
        })
        .use(commonmark)
        .use(gfm)
        .use(listener)
        .use(clipboard)
        .use(upload)
        .use(collab)
        .create();

    editor.action((ctx) => {
        const service = ctx.get(collabServiceCtx).bindDoc(ydoc).setAwareness(awareness);

        if (initialMarkdown !== null && initialMarkdown !== undefined) {
            service.applyTemplate(initialMarkdown, (remoteNode) => remoteNode.textContent.length === 0);
        }

        service.connect();
    });

    return {
        getMarkdown: () => normalizeMarkdown(editor.action(getMarkdown())),
        // No "flush": a flush builds a new editor state and would cut the Yjs binding - a plain
        // transaction goes through y-prosemirror to everybody else.
        replaceAll: (markdown) => editor.action(replaceAll(markdown)),
        // The toolbar's image button: upload, then insert at the cursor.
        insertImageFile: async (file) => {
            const src = await uploadFile(file);
            editor.action(callCommand(insertImageCommand.key, { src, alt: imageAlt(file) }));
        },
        command: (name) => {
            const make = COMMANDS[name];
            const args = make ? make() : null;

            if (args) {
                editor.action(callCommand(...args));
                editor.action((ctx) => ctx.get(editorViewCtx).focus());
            }
        },
        destroy: () => editor.destroy()
    };
}

function imageAlt(file) {
    return (file.name || 'image').replace(/\.[a-z0-9]+$/i, '');
}

/**
 * Line-based three-way merge: what changed from `base` to `theirs` (an agent's write) is applied on
 * top of `ours` (the live document). Conflicting hunks keep `ours` - the agent's full version is
 * in the page history anyway. Returns { markdown, conflicts }.
 */
function mergeMarkdown(base, ours, theirs) {
    const regions = diff3Merge(ours.split('\n'), base.split('\n'), theirs.split('\n'));
    const lines = [];
    let conflicts = 0;

    for (const region of regions) {
        if (region.ok) {
            lines.push(...region.ok);
        } else {
            conflicts++;
            lines.push(...region.conflict.a);
        }
    }

    return { markdown: lines.join('\n'), conflicts };
}

function toBase64(bytes) {
    let binary = '';
    for (let i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
    return btoa(binary);
}

function fromBase64(text) {
    const binary = atob(text);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
    return bytes;
}

window.KsfxCollab = {
    Y, Awareness, encodeAwarenessUpdate, applyAwarenessUpdate, removeAwarenessStates,
    createEditor, mergeMarkdown, normalizeMarkdown, toBase64, fromBase64
};
