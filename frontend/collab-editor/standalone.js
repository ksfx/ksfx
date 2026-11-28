// The general Markdown editor for everything outside the live wiki (issues, the agents' file
// browser, ...): the same Milkdown editor and toolbar as the wiki, alone (local Yjs document, no
// session), plus what a lone editor can offer that a live one cannot:
// - a Markdown source toggle (WYSIWYG <-> raw text),
// - YAML front matter (--- at the very top, common in agent files) kept apart in its own text
//   field and put back unchanged - WYSIWYG would turn it into a rule and a heading.
import * as Y from 'yjs';
import { Awareness } from 'y-protocols/awareness';

const BUTTONS = [
    ['h1', 'H1', 'Heading 1'], ['h2', 'H2', 'Heading 2'], ['h3', 'H3', 'Heading 3'], ['paragraph', '¶', 'Normal text'], null,
    ['bold', '<i class="fa fa-bold"></i>', 'Bold'], ['italic', '<i class="fa fa-italic"></i>', 'Italic'],
    ['strike', '<i class="fa fa-strikethrough"></i>', 'Strikethrough'], ['code', '<i class="fa fa-code"></i>', 'Inline code'],
    ['link', '<i class="fa fa-link"></i>', 'Link'], null,
    ['bullet', '<i class="fa fa-list-ul"></i>', 'Bullet list'], ['ordered', '<i class="fa fa-list-ol"></i>', 'Numbered list'],
    ['quote', '<i class="fa fa-quote-left"></i>', 'Quote'], ['codeblock', '<i class="fa fa-file-code-o"></i>', 'Code block'],
    ['table', '<i class="fa fa-table"></i>', 'Table'], ['image', '<i class="fa fa-picture-o"></i>', 'Image (or paste / drop one)'],
    ['hr', '—', 'Horizontal line']
];

const FRONT_MATTER = /^---[ \t]*\n[\s\S]*?\n---[ \t]*(?:\n|$)/;

/**
 * Builds the editor into `container` and resolves to { getMarkdown, focus }.
 * options: initialMarkdown, uploadFile(file) -> Promise<url> (images; omit to hide the button),
 * minHeight (px), sourceToggle (default true), startInSource (default false).
 */
export async function mountStandalone(container, options, createEditor) {
    const opts = Object.assign({ initialMarkdown: '', minHeight: 260, sourceToggle: true, startInSource: false }, options || {});
    let markdown = opts.initialMarkdown || '';
    let frontMatter = '';
    const match = markdown.match(FRONT_MATTER);

    if (match) {
        frontMatter = match[0].replace(/\n?$/, '\n');
        markdown = markdown.slice(match[0].length).replace(/^\n/, '');
    }

    container.classList.add('ksfx-md-editor');
    container.innerHTML = '';

    const toolbar = document.createElement('div');
    toolbar.className = 'ksfx-md-toolbar';

    BUTTONS.forEach((b) => {
        if (!b) {
            const sep = document.createElement('span');
            sep.className = 'ksfx-md-sep';
            toolbar.appendChild(sep);
            return;
        }

        if (b[0] === 'image' && !opts.uploadFile) {
            return;
        }

        const button = document.createElement('button');
        button.type = 'button';
        button.dataset.cmd = b[0];
        button.title = b[2];
        button.innerHTML = b[1];
        toolbar.appendChild(button);
    });

    let sourceButton = null;

    if (opts.sourceToggle) {
        sourceButton = document.createElement('button');
        sourceButton.type = 'button';
        sourceButton.className = 'ksfx-md-source-toggle';
        sourceButton.title = 'Edit the Markdown source';
        sourceButton.textContent = 'Markdown';
        toolbar.appendChild(sourceButton);
    }

    const imageInput = document.createElement('input');
    imageInput.type = 'file';
    imageInput.accept = 'image/*';
    imageInput.hidden = true;
    toolbar.appendChild(imageInput);

    let frontMatterField = null;

    if (frontMatter) {
        const wrap = document.createElement('div');
        wrap.className = 'ksfx-md-frontmatter';
        const label = document.createElement('div');
        label.className = 'ksfx-md-frontmatter-label';
        label.textContent = 'Front matter (YAML) - kept exactly as written';
        frontMatterField = document.createElement('textarea');
        frontMatterField.spellcheck = false;
        frontMatterField.value = frontMatter.replace(/\n$/, '');
        frontMatterField.rows = Math.min(10, frontMatterField.value.split('\n').length);
        wrap.appendChild(label);
        wrap.appendChild(frontMatterField);
        container.appendChild(toolbar);
        container.appendChild(wrap);
    } else {
        container.appendChild(toolbar);
    }

    const prose = document.createElement('div');
    prose.className = 'ksfx-md-prose-mount';
    prose.style.minHeight = opts.minHeight + 'px';
    container.appendChild(prose);

    const source = document.createElement('textarea');
    source.className = 'ksfx-md-source';
    source.spellcheck = false;
    source.hidden = true;
    source.style.minHeight = opts.minHeight + 'px';
    container.appendChild(source);

    const ydoc = new Y.Doc();
    const editor = await createEditor({ el: prose, ydoc, awareness: new Awareness(ydoc), initialMarkdown: markdown, uploadFile: opts.uploadFile });
    let inSource = false;

    function setSourceMode(on) {
        if (on === inSource) {
            return;
        }

        if (on) {
            source.value = editor.getMarkdown();
        } else {
            editor.replaceAll(source.value);
        }

        inSource = on;
        source.hidden = !on;
        prose.hidden = on;
        toolbar.querySelectorAll('button[data-cmd]').forEach((b) => { b.disabled = on; });

        if (sourceButton) {
            sourceButton.classList.toggle('active', on);
            sourceButton.title = on ? 'Back to the visual editor' : 'Edit the Markdown source';
        }
    }

    toolbar.addEventListener('mousedown', (event) => {
        const button = event.target.closest('button');

        if (!button || button.disabled) {
            return;
        }

        event.preventDefault();

        if (button === sourceButton) {
            setSourceMode(!inSource);
        } else if (button.dataset.cmd === 'image') {
            imageInput.click();
        } else if (button.dataset.cmd) {
            editor.command(button.dataset.cmd);
        }
    });

    imageInput.addEventListener('change', () => {
        const file = imageInput.files[0];
        imageInput.value = '';

        if (file) {
            editor.insertImageFile(file).catch(() => { /* the caller's uploadFile reports it */ });
        }
    });

    if (opts.startInSource) {
        setSourceMode(true);
    }

    return {
        getMarkdown() {
            const body = inSource ? source.value : editor.getMarkdown();
            const head = frontMatterField && frontMatterField.value.trim() ? frontMatterField.value.replace(/\n*$/, '\n') + '\n' : '';
            return head + body;
        },
        focus() {
            (inSource ? source : prose.querySelector('.ksfx-collab-prose') || prose).focus();
        }
    };
}
