/**
 * The block editor's "Search" panel: whether this post is in the Turing index, why not,
 * "Exclude from search", "Reindex now", and an Assist section (D171) that finds what the site
 * already has near the post.
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

    function SearchPanel() {
        var post = useSelect(function (select) {
            var editor = select('core/editor');
            return {
                id: editor.getCurrentPostId(),
                meta: editor.getEditedPostAttribute('meta') || {},
                saving: editor.isSavingPost() && !editor.isAutosavingPost()
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
            el(Assist, { postId: post.id })
        );
    }

    wp.plugins.registerPlugin('viglet-dumont-search', { render: SearchPanel });
})(window.wp);
