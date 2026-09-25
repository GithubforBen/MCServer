/*
 * The tickets: a list on the left, the conversation of the chosen one below it.
 *
 * The same tickets as on discord and in the game - an answer written here reaches the player there, and the
 * admins see it in the ticket's discord thread.
 */
(function () {
    'use strict';

    var api = McAdmin.api;
    var el = McAdmin.el;
    var clear = McAdmin.clear;
    var toast = McAdmin.toast;
    var autoRefresh = McAdmin.autoRefresh;

    var STATE_CLASS = {OPEN: 'state-alarm', IN_PROGRESS: 'state-caution', CLOSED: 'state-nominal'};

    function when(millis) {
        return new Date(millis).toLocaleString('de-DE', {
            day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit'
        });
    }

    McAdmin.registerPanel('tickets', function (panel, module) {
        var status = el('p', {className: 'muted', text: 'Lade ...'});
        var filter = el('select');
        [['active', 'Offen und in Bearbeitung'], ['OPEN', 'Nur offen'], ['CLOSED', 'Geschlossen'], ['all', 'Alle']]
            .forEach(function (option) {
                var node = el('option', {text: option[1], value: option[0]});
                filter.appendChild(node);
            });
        var rows = el('div', {className: 'rows'});
        var detail = el('section', {className: 'card hidden'});
        var selected = null;
        /** What the list looked like last time, so a refresh only redraws when something changed. */
        var lastList = '';

        panel.appendChild(el('section', {className: 'card'}, [
            el('h2', {text: module.title}),
            el('p', {className: 'muted', text: module.description}),
            el('div', {className: 'inline-form'}, [
                el('div', {className: 'field'}, [el('label', {text: 'Anzeigen'}), filter])
            ]),
            status
        ]));
        panel.appendChild(el('section', {className: 'card'}, [el('h3', {text: 'Tickets'}), rows]));
        panel.appendChild(detail);

        filter.addEventListener('change', function () {
            lastList = '';
            refresh();
        });

        function shown(ticket) {
            var value = filter.value;
            if (value === 'all') return true;
            if (value === 'active') return ticket.status !== 'CLOSED';
            return ticket.status === value;
        }

        function refresh() {
            api('/api/tickets').then(function (data) {
                var tickets = data.tickets || [];
                var open = tickets.filter(function (t) {
                    return t.status === 'OPEN';
                }).length;
                status.textContent = tickets.length + ' Tickets, ' + open + ' davon warten auf einen Admin.';
                var visible = tickets.filter(shown);
                var fingerprint = JSON.stringify(visible);
                if (fingerprint !== lastList) {
                    lastList = fingerprint;
                    drawList(visible);
                }
                if (selected !== null) load(selected, true);
            }).catch(function (error) {
                status.textContent = error.message;
            });
        }

        function drawList(tickets) {
            clear(rows);
            if (!tickets.length) {
                rows.appendChild(el('p', {className: 'muted', text: 'Keine Tickets.'}));
                return;
            }
            tickets.forEach(function (ticket) {
                var open = el('button', {text: 'Öffnen', type: 'button', className: 'small'});
                open.addEventListener('click', function () {
                    load(ticket.id, false);
                });
                var meta = ticket.type + ' · ' + ticket.statusTitle + ' · von ' + ticket.author
                    + ' · ' + ticket.source + ' · ' + ticket.messages + ' Nachrichten · ' + when(ticket.updatedAt)
                    + (ticket.assignee ? ' · bearbeitet von ' + ticket.assignee : '');
                rows.appendChild(el('div', {className: 'row ' + (STATE_CLASS[ticket.status] || '')}, [
                    el('div', {className: 'grow'}, [
                        el('div', {className: 'name', text: '#' + ticket.id + ' · ' + ticket.title}),
                        el('div', {className: 'meta', text: meta})
                    ]),
                    el('div', {className: 'actions'}, [open])
                ]));
            });
        }

        /** The text an admin is writing, kept across refreshes. */
        var draft = {};
        /** What the open ticket looked like last time, so typing is only interrupted by a real change. */
        var lastDetail = '';

        function load(id, quiet) {
            api('/api/tickets/' + encodeURIComponent(id)).then(function (ticket) {
                selected = ticket.id;
                var fingerprint = JSON.stringify(ticket);
                if (quiet && fingerprint === lastDetail) return;
                lastDetail = fingerprint;
                drawDetail(ticket);
                if (!quiet) detail.scrollIntoView({behavior: 'smooth'});
            }).catch(function (error) {
                if (!quiet) toast(error.message, 'error');
                selected = null;
                detail.classList.add('hidden');
            });
        }

        function post(path, body, button) {
            button.disabled = true;
            return api(path, {method: 'POST', body: body}).then(function (data) {
                toast(data.message, 'ok');
                lastList = '';
                refresh();
                return data;
            }).catch(function (error) {
                toast(error.message, 'error');
                throw error;
            }).finally(function () {
                button.disabled = false;
            });
        }

        function drawDetail(ticket) {
            // keep what is being typed, and where the cursor is
            var old = detail.querySelector('textarea');
            var hadFocus = old && document.activeElement === old;
            if (old) draft[ticket.id] = old.value;
            clear(detail);
            detail.classList.remove('hidden');

            var base = '/api/tickets/' + ticket.id;
            detail.appendChild(el('h3', {text: '#' + ticket.id + ' · ' + ticket.title}));
            var reach = [];
            if (ticket.linkedDiscord) reach.push('Discord');
            if (ticket.linkedMinecraft) reach.push('im Spiel');
            detail.appendChild(el('p', {
                className: 'muted',
                text: ticket.type + ' · ' + ticket.statusTitle + ' · von ' + ticket.author
                    + (ticket.assignee ? ' · bearbeitet von ' + ticket.assignee : '')
                    + ' · Antworten erreichen den Spieler ' + (reach.length ? reach.join(' und ') : 'nirgends')
            }));
            if (ticket.context) {
                detail.appendChild(el('p', {className: 'muted', text: 'Bezieht sich auf: ' + ticket.context}));
            }

            var conversation = el('div', {className: 'rows'});
            (ticket.messages || []).forEach(function (message) {
                conversation.appendChild(el('div', {className: 'row ' + (message.staff ? 'state-caution' : '')}, [
                    el('div', {className: 'grow'}, [
                        el('div', {
                            className: 'meta',
                            text: message.author + (message.staff ? ' (Admin)' : '') + ' · ' + message.source
                                + ' · ' + when(message.at)
                        }),
                        el('div', {className: 'message-text', text: message.text})
                    ])
                ]));
            });
            detail.appendChild(conversation);

            var text = el('textarea', {placeholder: 'Antwort an ' + ticket.author});
            text.rows = 4;
            text.maxLength = 1500;
            text.style.width = '100%';
            text.value = draft[ticket.id] || '';
            var send = el('button', {text: 'Antworten', type: 'button'});
            send.addEventListener('click', function () {
                var value = text.value.trim();
                if (!value) {
                    toast('Die Antwort ist leer.', 'error');
                    return;
                }
                post(base + '/reply', {text: value}, send).then(function () {
                    draft[ticket.id] = '';
                    text.value = '';
                }).catch(function () {
                });
            });

            var actions = [send];
            var claim = el('button', {text: 'Übernehmen', type: 'button', className: 'small'});
            claim.addEventListener('click', function () {
                post(base + '/claim', {}, claim).catch(function () {
                });
            });
            actions.push(claim);
            var toggle = ticket.status === 'CLOSED'
                ? el('button', {text: 'Wieder öffnen', type: 'button', className: 'small'})
                : el('button', {text: 'Schließen', type: 'button', className: 'small danger'});
            toggle.addEventListener('click', function () {
                post(base + '/status', {status: ticket.status === 'CLOSED' ? 'OPEN' : 'CLOSED'}, toggle)
                    .catch(function () {
                    });
            });
            actions.push(toggle);

            detail.appendChild(el('div', {className: 'field'}, [el('label', {text: 'Antwort'}), text]));
            detail.appendChild(el('div', {className: 'actions'}, actions));
            if (hadFocus) text.focus();
        }

        refresh();
        autoRefresh(refresh, 10);
    });
})();
