/**
 * The block editor's "Search" panel: whether this post is in the Turing index, why not,
 * "Exclude from search", "Reindex now", and an Assist section (D171) that finds what the site
 * already has near the post and drafts its excerpt.
 *
 * Written against the wp.* globals WordPress already loads, so the plugin needs no JS build.
 */
(function (wp) {
    'use strict';

    var el = wp.element.createElement;
    var useState = wp.element.useState;
    var useEffect = wp.element.useEffect;
    var useSelect = wp.data.useSelect;
    var useDispatch = wp.data.useDispatch;
    var __ = wp.i18n.__;
    var sprintf = wp.i18n.sprintf;
    var components = wp.components;
    // PluginDocumentSettingPanel moved from wp.editPost to wp.editor in WordPress 6.6.
    var Panel = (wp.editor && wp.editor.PluginDocumentSettingPanel) || wp.editPost.PluginDocumentSettingPanel;
    var EXCLUDE_META = '_viglet_dumont_exclude';

    /**
     * A link to a suggested post, as a paragraph after the selected block (or at the end):
     * the author keeps it, moves it or deletes it like any other text.
     */
    function addLink(link) {
        var editor = wp.data.select('core/block-editor');
        var selected = editor.getSelectedBlockClientId();
        var root = selected ? editor.getBlockRootClientId(selected) : undefined;
        var index = selected ? editor.getBlockIndex(selected) + 1 : undefined;
        var content = '<a href="' + wp.escapeHtml.escapeAttribute(link.url) + '">' +
            wp.escapeHtml.escapeHTML(link.title) + '</a>';
        wp.data.dispatch('core/block-editor').insertBlocks(
            wp.blocks.createBlock('core/paragraph', { content: content }),
            index,
            root
        );
    }

    /**
     * What the site already has near this post (D171), on demand: posts that may already cover
     * it, and posts to link to. Read-only: nothing changes unless the author adds a link.
     */
    function Assist(props) {
        var resultHook = useState(null);
        var result = resultHook[0];
        var setResult = resultHook[1];
        var busyHook = useState(false);
        var busy = busyHook[0];
        var setBusy = busyHook[1];

        function find() {
            setBusy(true);
            wp.apiFetch({ path: '/viglet-dumont/v1/post/' + props.postId + '/assist' })
                .then(setResult)
                .catch(function (error) {
                    setResult({ error: error.message || String(error), covered: [], links: [] });
                })
                .finally(function () {
                    setBusy(false);
                });
        }

        function list(items, withAdd) {
            // The panel loads no stylesheet of its own: the little layout it needs is inline.
            return el('ul', { className: 'viglet-dumont-assist__list', style: { margin: '0 0 12px' } }, items.map(function (item) {
                return el('li', { key: item.url, style: { display: 'flex', justifyContent: 'space-between', gap: '8px' } },
                    el('a', { href: item.url, target: '_blank', rel: 'noreferrer' }, item.title),
                    withAdd ? el(components.Button, {
                        variant: 'link',
                        style: { flexShrink: 0 },
                        onClick: function () { addLink(item); },
                        'aria-label': __('Add a link to', 'viglet-dumont') + ' ' + item.title
                    }, __('Add a link', 'viglet-dumont')) : null
                );
            }));
        }

        return el('div', { className: 'viglet-dumont-assist', style: { marginTop: '16px' } },
            el('h3', null, __('Assist', 'viglet-dumont')),
            result && result.error ? el('p', null, result.error) : null,
            result && !result.error ? el('div', { 'aria-live': 'polite' },
                el('h4', null, __('Already covered?', 'viglet-dumont')),
                result.covered.length
                    ? list(result.covered, false)
                    : el('p', null, __('Nothing on the site looks like this post.', 'viglet-dumont')),
                el('h4', null, __('Link ideas', 'viglet-dumont')),
                result.links.length
                    ? list(result.links, true)
                    : el('p', null, __('No related posts yet.', 'viglet-dumont')),
                'title' === result.basis
                    ? el('p', { className: 'viglet-dumont-assist__note' },
                        __('Compared by title: once this post is indexed, Turing compares the whole text.', 'viglet-dumont'))
                    : null
            ) : null,
            el(components.Button, {
                variant: 'secondary',
                isBusy: busy,
                disabled: busy || !props.postId,
                onClick: find
            }, result ? __('Look again', 'viglet-dumont') : __('Find related posts', 'viglet-dumont'))
        );
    }

    /**
     * An excerpt drafted by Turing from the saved post (D171). "Use" puts it in the excerpt
     * field like typing would; the post changes only when the author saves it.
     */
    function ExcerptDraft(props) {
        var editPost = useDispatch('core/editor').editPost;
        var draftHook = useState(null);
        var draft = draftHook[0];
        var setDraft = draftHook[1];
        var busyHook = useState(false);
        var busy = busyHook[0];
        var setBusy = busyHook[1];
        var usedHook = useState(false);
        var used = usedHook[0];
        var setUsed = usedHook[1];

        function write(again) {
            setBusy(true);
            setUsed(false);
            wp.apiFetch({
                path: '/viglet-dumont/v1/post/' + props.postId + '/excerpt',
                method: 'POST',
                data: { again: !!again }
            })
                .then(setDraft)
                .catch(function (error) {
                    setDraft({ excerpt: '', error: error.message || String(error) });
                })
                .finally(function () {
                    setBusy(false);
                });
        }

        return el('div', { className: 'viglet-dumont-assist__excerpt' },
            el('h4', null, __('Excerpt', 'viglet-dumont')),
            draft && draft.error ? el('p', null, draft.error) : null,
            draft && draft.excerpt ? el('div', { 'aria-live': 'polite' },
                el('blockquote', { className: 'viglet-dumont-assist__draft', style: { margin: '0 0 8px', fontStyle: 'italic' } }, draft.excerpt),
                used
                    ? el('p', null, __('In the excerpt field. Save the post to keep it.', 'viglet-dumont'))
                    : el(components.Button, {
                        variant: 'primary',
                        onClick: function () {
                            editPost({ excerpt: draft.excerpt });
                            setUsed(true);
                        }
                    }, __('Use this excerpt', 'viglet-dumont'))
            ) : null,
            el('p', null, el(components.Button, {
                variant: 'secondary',
                isBusy: busy,
                disabled: busy || !props.postId,
                onClick: function () { write(!!draft); }
            }, draft && draft.excerpt ? __('Draft another', 'viglet-dumont') : __('Draft an excerpt', 'viglet-dumont'))),
            el('p', { className: 'viglet-dumont-assist__note' },
                __('Written by Turing’s language model from the saved post. Nothing changes until you use it and save.', 'viglet-dumont'))
        );
    }

    /**
     * How the saved post suits each audience persona of the site's agent in Turing (D171): a
     * score with its reasons. Shown only on a site whose agent has personas.
     */
    function AudienceFit(props) {
        var personasHook = useState([]);
        var personas = personasHook[0];
        var setPersonas = personasHook[1];
        var resultHook = useState(null);
        var result = resultHook[0];
        var setResult = resultHook[1];
        var busyHook = useState(false);
        var busy = busyHook[0];
        var setBusy = busyHook[1];

        useEffect(function () {
            if (props.postId) {
                wp.apiFetch({ path: '/viglet-dumont/v1/post/' + props.postId + '/fit' })
                    .then(function (answer) { setPersonas(answer.personas || []); })
                    .catch(function () { setPersonas([]); });
            }
        }, [props.postId]);

        if (!personas.length) {
            return null;
        }

        function check() {
            setBusy(true);
            wp.apiFetch({ path: '/viglet-dumont/v1/post/' + props.postId + '/fit', method: 'POST' })
                .then(setResult)
                .catch(function (error) {
                    setResult({ fits: [], error: error.message || String(error) });
                })
                .finally(function () {
                    setBusy(false);
                });
        }

        return el('div', { className: 'viglet-dumont-assist__fit' },
            el('h4', null, __('Fits the audience?', 'viglet-dumont')),
            result && result.error ? el('p', null, result.error) : null,
            result && result.fits.length ? el('ul', { className: 'viglet-dumont-assist__list', 'aria-live': 'polite', style: { margin: '0 0 12px' } },
                result.fits.map(function (fit) {
                    return el('li', { key: fit.persona, style: { marginBottom: '8px' } },
                        el('strong', null, sprintf(
                            /* translators: 1: an audience persona's name, 2: how well the post suits it, 0 to 100. */
                            __('%1$s: %2$d%%', 'viglet-dumont'),
                            fit.persona,
                            fit.score
                        )),
                        fit.summary ? el('br') : null,
                        fit.summary,
                        fit.misfits.length ? el('ul', null, fit.misfits.map(function (misfit, i) {
                            return el('li', { key: i },
                                el('em', null, misfit.reason), ': “', misfit.span, '”',
                                misfit.suggestion ? el('br') : null,
                                misfit.suggestion ? sprintf(
                                    /* translators: %s: a rewrite Turing suggests for a passage. */
                                    __('Try: %s', 'viglet-dumont'),
                                    misfit.suggestion
                                ) : null
                            );
                        })) : null
                    );
                })) : null,
            el('p', null, el(components.Button, {
                variant: 'secondary',
                isBusy: busy,
                disabled: busy,
                onClick: check
            }, result ? __('Check again', 'viglet-dumont') : __('Check audience fit', 'viglet-dumont'))),
            el('p', { className: 'viglet-dumont-assist__note' },
                sprintf(
                    /* translators: %s: the audience personas, comma separated. */
                    __('Judged by Turing against %s, from the saved post.', 'viglet-dumont'),
                    personas.map(function (p) { return p.name; }).join(', ')
                ))
        );
    }

    function SearchPanel() {
        var post = useSelect(function (select) {
            var editor = select('core/editor');
            return {
                id: editor.getCurrentPostId(),
                meta: editor.getEditedPostAttribute('meta') || {},
                saving: editor.isSavingPost() && !editor.isAutosavingPost(),
                canTune: !!select('core').canUser('update', 'settings')
            };
        }, []);
        var editPost = useDispatch('core/editor').editPost;
        var stateHook = useState(null);
        var state = stateHook[0];
        var setState = stateHook[1];
        var busyHook = useState(false);
        var busy = busyHook[0];
        var setBusy = busyHook[1];

        function load() {
            wp.apiFetch({ path: '/viglet-dumont/v1/post/' + post.id + '/state' })
                .then(setState)
                .catch(function (error) {
                    setState({ state: 'failed', label: __('Unknown', 'viglet-dumont'), detail: error.message || '' });
                });
        }

        // Read the state on open and again once each save finishes.
        useEffect(function () {
            if (post.id && !post.saving) {
                load();
            }
        }, [post.id, post.saving]);

        function reindex() {
            setBusy(true);
            wp.apiFetch({ path: '/viglet-dumont/v1/post/' + post.id + '/reindex', method: 'POST' })
                .then(setState)
                .catch(function (error) {
                    setState({ state: 'failed', label: __('Failed', 'viglet-dumont'), detail: error.message || '' });
                })
                .finally(function () {
                    setBusy(false);
                });
        }

        return el(
            Panel,
            { name: 'viglet-dumont-search', title: __('Search', 'viglet-dumont'), className: 'viglet-dumont-search-panel' },
            el('p', { 'aria-live': 'polite' },
                el('strong', null, state ? state.label : __('Checking…', 'viglet-dumont')),
                state && state.detail ? el('br') : null,
                state && state.detail ? el('span', { className: 'viglet-dumont-search-panel__detail' }, state.detail) : null
            ),
            el(components.ToggleControl, {
                label: __('Exclude from search', 'viglet-dumont'),
                help: __('Takes effect when the post is saved.', 'viglet-dumont'),
                checked: !!post.meta[EXCLUDE_META],
                onChange: function (value) {
                    var meta = {};
                    meta[EXCLUDE_META] = value;
                    editPost({ meta: meta });
                }
            }),
            el(components.Button, {
                variant: 'secondary',
                isBusy: busy,
                disabled: busy || !post.id,
                onClick: reindex
            }, __('Reindex now', 'viglet-dumont')),
            // Search tuning (D172) opens with this post chosen; only an admin may tune search.
            post.canTune && post.id ? el('p', null, el(components.Button, {
                variant: 'link',
                href: 'admin.php?page=viglet-dumont-tuning&promote=' + post.id
            }, __('Promote for a search…', 'viglet-dumont'))) : null,
            el(Assist, { postId: post.id }),
            el(ExcerptDraft, { postId: post.id }),
            el(AudienceFit, { postId: post.id })
        );
    }

    wp.plugins.registerPlugin('viglet-dumont-search', { render: SearchPanel });
})(window.wp);
