/**
 * Viglet Dumont Admin JavaScript
 */
(function ($) {
    'use strict';

    var api = {
        url: vigletDumont.restUrl,
        nonce: vigletDumont.nonce,
        strings: vigletDumont.strings,

        request: function (method, endpoint, data) {
            return $.ajax({
                url: this.url + endpoint,
                method: method,
                data: data ? JSON.stringify(data) : undefined,
                contentType: 'application/json',
                beforeSend: function (xhr) {
                    xhr.setRequestHeader('X-WP-Nonce', api.nonce);
                }
            });
        }
    };

    // Tab switching
    function initTabs() {
        var $tabs = $('.viglet-dumont-tab');
        var $panels = $('.viglet-dumont-panel');
        var $save = $('#viglet-dumont-save');

        $tabs.on('click', function (e) {
            e.preventDefault();
            var tab = $(this).data('tab');

            $tabs.removeClass('active');
            $(this).addClass('active');

            $panels.hide();
            $('#panel-' + tab).show();

            // Hide save button on actions tab
            if (tab === 'actions') {
                $save.hide();
                loadStats();
            } else {
                $save.show();
            }

            // Update URL hash
            window.location.hash = tab;
        });

        // Restore tab from hash
        var hash = window.location.hash.replace('#', '');
        if (hash) {
            $tabs.filter('[data-tab="' + hash + '"]').trigger('click');
        }
    }

    // Status badge helper
    function statusBadge(type, message) {
        return '<span class="viglet-dumont-status viglet-dumont-status--' + type + '">' +
            (type === 'loading' ? '<span class="viglet-dumont-spinner"></span>' : '') +
            message + '</span>';
    }

    // Test connection
    function initPing() {
        $('#viglet-dumont-ping, #viglet-dumont-action-ping').on('click', function () {
            var $result = $(this).attr('id') === 'viglet-dumont-ping'
                ? $('#viglet-dumont-ping-result')
                : $('#viglet-dumont-action-ping-result');

            $result.html(statusBadge('loading', api.strings.pinging));

            api.request('GET', 'ping').done(function (data) {
                $result.html(statusBadge(
                    data.success ? 'success' : 'error',
                    data.message
                ));
            }).fail(function (xhr) {
                var msg = xhr.responseJSON && xhr.responseJSON.message
                    ? xhr.responseJSON.message
                    : api.strings.error;
                $result.html(statusBadge('error', msg));
            });
        });
    }

    // Batch indexing
    function initIndexing() {
        var isIndexing = false;

        $('#viglet-dumont-index-all').on('click', function () {
            if (isIndexing) return;
            isIndexing = true;

            var $btn = $(this).prop('disabled', true);
            var postType = $('#viglet-dumont-index-type').val();
            var $progress = $('#viglet-dumont-index-progress').show().removeClass('complete');
            var $fill = $progress.find('.viglet-dumont-progress__fill');
            var $text = $progress.find('.viglet-dumont-progress__text');
            var $msg = $progress.find('.viglet-dumont-progress__message');

            function indexBatch(offset) {
                api.request('POST', 'index-batch', {
                    offset: offset,
                    post_type: postType
                }).done(function (data) {
                    $fill.css('width', data.percent + '%');
                    $text.text(data.percent + '%');
                    $msg.text(data.message);

                    if (!data.done) {
                        indexBatch(data.offset);
                    } else {
                        $progress.addClass('complete');
                        $btn.prop('disabled', false);
                        isIndexing = false;
                        loadStats();
                    }
                }).fail(function (xhr) {
                    var msg = xhr.responseJSON && xhr.responseJSON.message
                        ? xhr.responseJSON.message
                        : api.strings.error;
                    $msg.text(msg);
                    $fill.css({ width: '100%', background: 'var(--vd-error)' });
                    $text.text(api.strings.error);
                    $btn.prop('disabled', false);
                    isIndexing = false;
                });
            }

            indexBatch(0);
        });
    }

    // Delete all
    function initDeleteAll() {
        $('#viglet-dumont-delete-all').on('click', function () {
            if (!confirm(api.strings.confirm_del)) return;

            var $result = $('#viglet-dumont-delete-result');
            var $btn = $(this).prop('disabled', true);

            $result.html(statusBadge('loading', api.strings.deleting));

            api.request('POST', 'delete-all').done(function (data) {
                $result.html(statusBadge(
                    data.success ? 'success' : 'error',
                    data.success ? api.strings.deleted : api.strings.error
                ));
                $btn.prop('disabled', false);
            }).fail(function () {
                $result.html(statusBadge('error', api.strings.error));
                $btn.prop('disabled', false);
            });
        });
    }

    // Load content stats
    function loadStats() {
        var $grid = $('#viglet-dumont-stats-grid');

        api.request('GET', 'stats').done(function (data) {
            var html = '';
            var total = 0;

            Object.keys(data).forEach(function (key) {
                if (key === 'batch_size') return;
                total += data[key].total;
                html += '<div class="viglet-dumont-stat">' +
                    '<span class="viglet-dumont-stat__number">' + data[key].total + '</span>' +
                    '<span class="viglet-dumont-stat__label">' + data[key].label + '</span>' +
                    '</div>';
            });

            html = '<div class="viglet-dumont-stat">' +
                '<span class="viglet-dumont-stat__number">' + total + '</span>' +
                '<span class="viglet-dumont-stat__label">Total</span>' +
                '</div>' + html;

            $grid.html(html);
        }).fail(function () {
            $grid.html('<p>' + api.strings.error + '</p>');
        });
    }

    // Initialize
    $(document).ready(function () {
        initTabs();
        initPing();
        initIndexing();
        initDeleteAll();
    });

})(jQuery);
