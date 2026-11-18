// KSFX collaborative wiki editor bundle (branch multiwiki): Milkdown (WYSIWYG, Markdown-native,
// ProseMirror) + Yjs (CRDT) + y-protocols awareness, exposed as window.KsfxCollab. The transport
// (long-polling against WikiCollabController) lives in wiki-collab.js, not here - this bundle only
// builds editors and offers the merge helper. Build: npm run build (esbuild, one IIFE file).
import { Editor, rootCtx, editorViewCtx, editorViewOptionsCtx, remarkStringifyOptionsCtx } from '@milkdown/kit/core';
import {
    commonmark, toggleStrongCommand, toggleEmphasisCommand, toggleInlineCodeCommand, wrapInHeadingCommand,
    wrapInBulletListCommand, wrapInOrderedListCommand, wrapInBlockquoteCommand, createCodeBlockCommand,
    insertHrCommand, turnIntoTextCommand, toggleLinkCommand
} from '@milkdown/kit/preset/commonmark';
import { gfm, insertTableCommand, toggleStrikethroughCommand } from '@milkdown/kit/preset/gfm';
import { listener, listenerCtx } from '@milkdown/kit/plugin/listener';
import { clipboard } from '@milkdown/kit/plugin/clipboard';
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
 * Creates a collaborative editor in `el`, bound to `ydoc` and `awareness`. `initialMarkdown` is
 * only applied when this client was told to initialize the shared document (exactly one client per
 * session - see WikiCollabService), otherwise the content arrives as Yjs updates.
 */
async function createEditor({ el, ydoc, awareness, initialMarkdown, onChange }) {
    const editor = await Editor.make()
        .config((ctx) => {
            ctx.set(rootCtx, el);
            ctx.update(editorViewOptionsCtx, (prev) => ({ ...prev, attributes: { class: 'ksfx-collab-prose', spellcheck: 'true' } }));
            // Write Markdown the way the wiki (Toast UI) and the agents do - "-" bullets, "*"
            // emphasis - so saving through this editor does not rewrite untouched lines.
            ctx.update(remarkStringifyOptionsCtx, (prev) => ({ ...prev, bullet: '-', emphasis: '*', strong: '*', rule: '-', fence: '`' }));
            ctx.get(listenerCtx).markdownUpdated((_ctx, markdown, prevMarkdown) => {
                if (onChange && markdown !== prevMarkdown) {
                    onChange(markdown);
                }
            });
        })
        .use(commonmark)
        .use(gfm)
        .use(listener)
        .use(clipboard)
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
        getMarkdown: () => editor.action(getMarkdown()),
        // No "flush": a flush builds a new editor state and would cut the Yjs binding - a plain
        // transaction goes through y-prosemirror to everybody else.
        replaceAll: (markdown) => editor.action(replaceAll(markdown)),
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
    createEditor, mergeMarkdown, toBase64, fromBase64
};
