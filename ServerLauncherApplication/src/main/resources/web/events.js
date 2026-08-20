/*
 * The event calendar.
 *
 * The game shows events as an inventory; here they are time spans, because that is what a browser is good
 * at and what makes overlapping events obvious at a glance. The timeline is drawn from the earliest start
 * to the latest end, so it stays readable whether the events span an hour or a fortnight.
 */
(function () {
    'use strict';

    var api = McAdmin.api;
    var el = McAdmin.el;
    var clear = McAdmin.clear;
    var toast = McAdmin.toast;
    var autoRefresh = McAdmin.autoRefresh;

    /**
     * Writes a timestamp the way the rest of the interface does.
     */
    function when(millis) {
        var date = new Date(millis);
        return date.toLocaleString('de-DE', {
            day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit'
        });
    }

    /**
     * Turns a span into something readable, the same wording the game uses.
     */
    function span(millis) {
        if (millis <= 0) return 'jetzt';
        var minutes = Math.floor(millis / 60000);
        var hours = Math.floor(minutes / 60);
        var days = Math.floor(hours / 24);
        if (days > 0) return days + ' Tage ' + (hours % 24) + ' Std';
        if (hours > 0) return hours + ' Std ' + (minutes % 60) + ' Min';
        return minutes + ' Min';
    }

    /**
     * The window the timeline covers: from the earliest start to the latest end, with now included so a
     * calendar full of past events still shows where the present is.
     */
    function bounds(events) {
        var now = Date.now();
        var from = now;
        var to = now;
        events.forEach(function (event) {
            from = Math.min(from, event.startsAt);
            to = Math.max(to, event.endsAt);
        });
        if (to <= from) to = from + 3600000;
        return {from: from, to: to, now: now};
    }

    function percent(value, range) {
        return ((value - range.from) / (range.to - range.from)) * 100;
    }

    McAdmin.registerPanel('events', function (panel, module) {
        var status = el('p', {className: 'muted', text: 'Lade ...'});
        var timelineHost = el('div', {className: 'rows'});
        var listHost = el('div', {className: 'rows'});

        panel.appendChild(el('section', {className: 'card'}, [
            el('h2', {text: module.title}),
            el('p', {className: 'muted', text: module.description}),
            status
        ]));
        panel.appendChild(el('section', {className: 'card'}, [
            el('h3', {text: 'Zeitraum'}),
            timelineHost
        ]));
        panel.appendChild(el('section', {className: 'card'}, [
            el('h3', {text: 'Alle Events'}),
            listHost
        ]));

        /* ------------------------------------------------------------------ creating */

        var nameInput = el('input', {type: 'text', placeholder: 'Name des Events'});
        var descriptionInput = el('input', {type: 'text', placeholder: 'Beschreibung (optional)'});
        var typeSelect = el('select');
        var startInput = el('input', {type: 'datetime-local'});
        var endInput = el('input', {type: 'datetime-local'});
        var createButton = el('button', {text: 'Event anlegen', type: 'button'});

        createButton.addEventListener('click', function () {
            if (!nameInput.value.trim()) {
                toast('Es fehlt der Name des Events.', 'error');
                return;
            }
            if (!startInput.value || !endInput.value) {
                toast('Anfang und Ende müssen gesetzt sein.', 'error');
                return;
            }
            var startsAt = new Date(startInput.value).getTime();
            var endsAt = new Date(endInput.value).getTime();
            if (endsAt <= startsAt) {
                toast('Das Event endet vor seinem Anfang.', 'error');
                return;
            }
            createButton.disabled = true;
            api('/api/events', {
                method: 'POST',
                // milliseconds do not survive a json int on the server, so they travel as strings
                body: {
                    name: nameInput.value.trim(),
                    description: descriptionInput.value.trim(),
                    type: typeSelect.value,
                    startsAt: String(startsAt),
                    endsAt: String(endsAt)
                }
            }).then(function (data) {
                toast(data.message, 'ok');
                nameInput.value = '';
                descriptionInput.value = '';
                refresh();
            }).catch(function (error) {
                toast(error.message, 'error');
            }).then(function () {
                createButton.disabled = false;
            });
        });

        panel.appendChild(el('section', {className: 'card'}, [
            el('h3', {text: 'Neues Event'}),
            el('div', {className: 'inline-form'}, [
                el('div', {className: 'field'}, [el('label', {text: 'Name'}), nameInput]),
                el('div', {className: 'field'}, [el('label', {text: 'Typ'}), typeSelect]),
                el('div', {className: 'field'}, [el('label', {text: 'Anfang'}), startInput]),
                el('div', {className: 'field'}, [el('label', {text: 'Ende'}), endInput]),
                el('div', {className: 'field'}, [el('label', {text: 'Beschreibung'}), descriptionInput]),
                createButton
            ])
        ]));

        api('/api/events/types').then(function (data) {
            (data.types || []).forEach(function (type) {
                var option = el('option', {
                    text: type.title + (type.hasMechanics ? ' (mit Mechanik)' : '')
                });
                option.value = type.name;
                typeSelect.appendChild(option);
            });
        }).catch(function () {
            /* the list of types is a convenience - the form works without it */
        });

        /* ------------------------------------------------------------------ drawing */

        /**
         * One event as a bar on the timeline. The bar is positioned by percentage, so the same markup
         * works whether the calendar spans an hour or a fortnight.
         */
        function renderBar(event, range) {
            var left = Math.max(0, percent(event.startsAt, range));
            var right = Math.min(100, percent(event.endsAt, range));
            var state = stateClass(event);
            var bar = el('div', {className: state ? 'timeline-bar state-' + state : 'timeline-bar'});
            bar.style.marginLeft = left + '%';
            bar.style.width = Math.max(1.5, right - left) + '%';

            var nowMark = el('div', {className: 'timeline-now'});
            nowMark.style.left = percent(range.now, range) + '%';

            return el('div', {className: rowClass(event)}, [
                el('div', {className: 'grow'}, [
                    el('div', {className: 'name', text: event.name}),
                    el('div', {className: 'timeline'}, [bar, nowMark]),
                    el('div', {className: 'meta', text: when(event.startsAt) + ' – ' + when(event.endsAt)})
                ])
            ]);
        }

        /**
         * Colour follows state, the way the rest of the interface does it. An event that is simply over
         * gets no colour at all, which is what lets it sink into the background.
         */
        function stateClass(event) {
            if (event.cancelled) return 'alarm';
            if (event.state === 'RUNNING') return 'nominal';
            if (event.state === 'PLANNED') return 'caution';
            return '';
        }

        /**
         * @return the class list for a row, leaving the state off when there is none
         */
        function rowClass(event) {
            var state = stateClass(event);
            return state ? 'row state-' + state : 'row';
        }

        /**
         * One event as a row with its buttons.
         */
        function renderRow(event) {
            var note;
            if (event.cancelled) {
                note = 'Abgesagt';
            } else if (event.state === 'RUNNING') {
                note = 'Läuft noch ' + span(event.endsAt - Date.now());
            } else if (event.state === 'PLANNED') {
                note = 'Startet in ' + span(event.startsAt - Date.now());
            } else {
                note = 'Vorbei';
            }

            var cancelButton = el('button', {
                text: event.cancelled ? 'Wieder aktivieren' : 'Absagen',
                type: 'button',
                className: 'small secondary'
            });
            cancelButton.addEventListener('click', function () {
                cancelButton.disabled = true;
                api('/api/events/' + encodeURIComponent(event.id) + '/cancel', {method: 'POST'})
                    .then(function (data) {
                        toast(data.message, 'ok');
                        refresh();
                    }).catch(function (error) {
                        toast(error.message, 'error');
                        cancelButton.disabled = false;
                    });
            });

            var deleteButton = el('button', {text: 'Löschen', type: 'button', className: 'small danger'});
            deleteButton.addEventListener('click', function () {
                deleteButton.disabled = true;
                api('/api/events/' + encodeURIComponent(event.id), {method: 'DELETE'})
                    .then(function (data) {
                        toast(data.message, 'ok');
                        refresh();
                    }).catch(function (error) {
                        toast(error.message, 'error');
                        deleteButton.disabled = false;
                    });
            });

            var actions = el('div', {className: 'actions'});
            if (event.state !== 'FINISHED') actions.appendChild(cancelButton);
            actions.appendChild(deleteButton);

            return el('div', {className: rowClass(event)}, [
                el('div', {className: 'grow'}, [
                    el('div', {className: 'name', text: event.name}),
                    el('div', {className: 'meta', text: event.typeTitle + ' · ' + note}),
                    event.description
                        ? el('div', {className: 'meta', text: event.description})
                        : null
                ]),
                actions
            ]);
        }

        function refresh() {
            return api('/api/events').then(function (data) {
                var events = data.events || [];
                clear(timelineHost);
                clear(listHost);
                if (!events.length) {
                    status.textContent = 'Es ist noch kein Event angelegt.';
                    return;
                }
                var running = events.filter(function (event) {
                    return event.state === 'RUNNING' && !event.cancelled;
                }).length;
                status.textContent = events.length + ' Events, davon laufen ' + running + '.';
                var range = bounds(events);
                events.forEach(function (event) {
                    timelineHost.appendChild(renderBar(event, range));
                    listHost.appendChild(renderRow(event));
                });
            }).catch(function (error) {
                status.textContent = error.message;
            });
        }

        refresh();
        autoRefresh(refresh, 10);
    });
})();
