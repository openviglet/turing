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

    // Index All (D331): the button only starts a reindex that WP-Cron runs, so this page can
    // close at any moment. While it stays open it shows how the run goes, read every few seconds.
    function initIndexing() {
        var $btn = $('#viglet-dumont-index-all');
        var $progress = $('#viglet-dumont-index-progress');
        var $fill = $progress.find('.viglet-dumont-progress__fill');
        var $text = $progress.find('.viglet-dumont-progress__text');
        var $msg = $progress.find('.viglet-dumont-progress__message');
        var timer = null;
        var started = '';

        function format(text, values) {
            var i = 0;
            return text.replace(/%(\d+\$)?d/g, function (match, position) {
                var value = position ? values[parseInt(position, 10) - 1] : values[i++];
                return String(value);
            });
        }

        function healthLink() {
            return $('<a>').attr('href', api.strings.health_url).text(api.strings.health);
        }

        function show(report) {
            var run = report.running;
            if (run) {
                var percent = run.total > 0 ? Math.round((run.offset / run.total) * 100) : 100;
                $progress.show().removeClass('complete');
                $fill.css({ width: percent + '%', background: '' });
                $text.text(percent + '%');
                $msg.empty().append(
                    document.createTextNode(format(api.strings.running, [run.offset, run.total, run.failed]) + ' '),
                    healthLink()
                );
                $btn.prop('disabled', true);
                return true;
            }

            $btn.prop('disabled', false);
            var last = report.last;
            // Only the run this page started or found running says how it ended.
            if (last && started && last.id === started) {
                $progress.show().addClass('complete');
                $fill.css({ width: '100%', background: last.state === 'done' ? '' : 'var(--vd-error)' });
                $text.text(last.state === 'done' ? api.strings.complete : api.strings.error);
                $msg.text(format(api.strings['ended_' + last.state] || api.strings.ended_done, [last.sent, last.failed]));
                loadStats();
            }
            return false;
        }

        function poll() {
            api.request('GET', 'reindex').done(function (report) {
                if (report.running) {
                    started = report.running.id;
                }
                if (show(report)) {
                    timer = window.setTimeout(poll, 3000);
                } else {
                    timer = null;
                }
            });
        }

        $btn.on('click', function () {
            if (timer) return;
            $btn.prop('disabled', true);

            api.request('POST', 'reindex', {
                type: $('#viglet-dumont-index-type').val()
            }).done(function (report) {
                started = report.running ? report.running.id : '';
                show(report);
                timer = window.setTimeout(poll, 3000);
            }).fail(function (xhr) {
                var report = xhr.responseJSON || {};
                // One is running already: follow that one.
                if (report.running) {
                    started = report.running.id;
                    show(report);
                    timer = window.setTimeout(poll, 3000);
                    return;
                }
                $progress.show();
                $msg.text(report.message || api.strings.error);
                $fill.css({ width: '100%', background: 'var(--vd-error)' });
                $text.text(api.strings.error);
                $btn.prop('disabled', false);
            });
        });

        // A reindex started earlier, from here or anywhere, shows as soon as the page opens.
        poll();
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
                // Only post types are {total, label}; batch_size and queue ride along.
                var entry = data[key];
                if (!entry || typeof entry !== 'object' || typeof entry.total !== 'number') return;
                total += entry.total;
                html += '<div class="viglet-dumont-stat">' +
                    '<span class="viglet-dumont-stat__number">' + data[key].total + '</span>' +
                    '<span class="viglet-dumont-stat__label">' + data[key].label + '</span>' +
                    '</div>';
            });

            html = '<div class="viglet-dumont-stat">' +
                '<span class="viglet-dumont-stat__number">' + total + '</span>' +
                '<span class="viglet-dumont-stat__label">' + api.strings.total + '</span>' +
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
