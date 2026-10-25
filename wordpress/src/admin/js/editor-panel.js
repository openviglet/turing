/**
 * The block editor's "Search" panel: whether this post is in the Turing index, why not,
 * "Exclude from search" and "Reindex now".
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
            }, __('Reindex now', 'viglet-dumont'))
        );
    }

    wp.plugins.registerPlugin('viglet-dumont-search', { render: SearchPanel });
})(window.wp);
