/*
 * The event calendar.
 *
 * The game shows events as an inventory; here they are time spans, because that is what a browser is good
 * at and what makes overlapping events obvious at a glance. The timeline is drawn from the earliest start
 * to the latest end, so it stays readable whether the events span an hour or a fortnight.
 *
 * An event is edited here as a whole - name, times, the settings of its kind and its rewards - and saved in
 * one go.
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
        // the editor sits above the lists and is left alone by the refresh, so a form being filled in is
        // never redrawn under somebody's cursor
        var editorHost = el('section', {className: 'card hidden'});
        panel.appendChild(editorHost);
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
                // straight on to settings and rewards, which is what comes next for almost every event
                refresh().then(function () {
                    var created = findEvent(data.id);
                    if (created) openEditor(created);
                });
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
            if (!event.applied) {
                actions.appendChild(el('button', {
                    text: 'Bearbeiten', type: 'button', className: 'small',
                    onClick: function () {
                        openEditor(event);
                    }
                }));
            }
            if (event.state !== 'FINISHED') actions.appendChild(cancelButton);
            actions.appendChild(deleteButton);

            return el('div', {className: rowClass(event)}, [
                el('div', {className: 'grow'}, [
                    el('div', {className: 'name', text: event.name}),
                    el('div', {className: 'meta', text: event.typeTitle + ' · ' + note}),
                    event.description
                        ? el('div', {className: 'meta', text: event.description})
                        : null,
                    event.rewards && event.rewards.length
                        ? el('div', {className: 'meta', text: 'Belohnungen: ' + event.rewards.map(function (reward) {
                            return reward.label;
                        }).join(', ')})
                        : null
                ]),
                actions
            ]);
        }

        /* ------------------------------------------------------------------ editing */

        /**
         * Who shares a placing, said per kind of event - this is the question every admin asks before they
         * set the first prize of a team event.
         */
        var PLACING_NOTES = {
            BEDWARS: 'Platzierung nach Teams: das beste Team ist Platz 1, das zweitbeste Platz 2 usw. '
                + 'Alle Spieler eines Teams teilen sich den Platz - jeder bekommt die volle Belohnung.',
            UHC_BOSSES: 'Platzierung nach Läufen, schnellster zuerst. Alle in einem Lauf teilen sich dessen '
                + 'Platz - jeder bekommt die volle Belohnung. Läuft dieselbe Gruppe mehrmals, zählt ihr bester Lauf.',
            UHC_DRAGON: 'Platzierung nach Läufen, schnellster zuerst. Alle in einem Lauf teilen sich dessen '
                + 'Platz - jeder bekommt die volle Belohnung. Läuft dieselbe Gruppe mehrmals, zählt ihr bester Lauf.',
            HUNGER_GAMES: 'Jeder spielt für sich: Platz 1 ist, wer als Letzter übrig bleibt.',
            POKER: 'Jeder spielt für sich: gewertet wird der Gewinn, wer die Mindestwerte nicht schafft, '
                + 'ist nicht platziert.'
        };

        var WHO = [
            {value: 'PLACE', label: 'Platzierung'},
            {value: 'KILLS', label: 'Kills', kills: true},
            {value: 'PARTICIPATION', label: 'Teilnahme'}
        ];

        /**
         * A timestamp as a datetime-local input wants it, in the browser's own time zone.
         */
        function toLocalInput(millis) {
            var date = new Date(millis);
            function pad(n) {
                return (n < 10 ? '0' : '') + n;
            }
            return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate())
                + 'T' + pad(date.getHours()) + ':' + pad(date.getMinutes());
        }

        /**
         * Who gets a reward, in the words the game uses: "#1", "Platz 4-10", "ab Platz 10", "ab 5 Kills".
         */
        function describeWho(reward) {
            if (reward.who === 'PARTICIPATION') return 'Teilnahme';
            if (reward.who === 'KILLS') return 'ab ' + reward.kills + (reward.kills === 1 ? ' Kill' : ' Kills');
            if (!reward.to) return reward.from <= 1 ? 'Alle Plätze' : 'ab Platz ' + reward.from;
            if (reward.from === reward.to) return '#' + reward.from;
            return 'Platz ' + reward.from + '-' + reward.to;
        }

        /**
         * @return the first single placing that has no reward yet, so a new reward starts somewhere useful
         */
        function nextFreePlace(rewards) {
            var place = 1;
            while (rewards.some(function (reward) {
                return reward.who === 'PLACE' && reward.from === place && reward.to === place;
            })) {
                place++;
            }
            return place;
        }

        function numberInput(value, min, placeholder) {
            var input = el('input', {type: 'number', value: value === null ? '' : String(value)});
            if (min !== undefined) input.min = String(min);
            if (placeholder) input.placeholder = placeholder;
            return input;
        }

        function field(labelText, input, className) {
            return el('div', {className: className ? 'field ' + className : 'field'}, [
                el('label', {text: labelText}), input
            ]);
        }

        var materialList = null;

        /**
         * The item names the reward form completes from. Asked for once - the list comes from a game server
         * and does not change while it runs. Without one the names can still be typed out.
         */
        function materialOptions() {
            if (materialList) return materialList;
            materialList = el('datalist', {id: 'event-material-options'});
            panel.appendChild(materialList);
            api('/api/materials').then(function (data) {
                (data.materials || []).forEach(function (material) {
                    var option = el('option');
                    option.value = material.name;
                    materialList.appendChild(option);
                });
            }).catch(function () {
                /* typing the name out still works */
            });
            return materialList;
        }

        function closeEditor() {
            clear(editorHost);
            editorHost.className = 'card hidden';
        }

        /**
         * Opens the form for one event. It works on a copy: nothing is sent until "Speichern", and then all
         * of it at once, together with the revision it was opened on - if somebody changed the event in the
         * meantime, the server says so instead of one of the two changes quietly getting lost.
         */
        function openEditor(event) {
            materialOptions();
            clear(editorHost);
            editorHost.className = 'card';

            var rewards = (event.rewards || []).map(function (reward) {
                return {
                    who: reward.who,
                    from: reward.from,
                    to: reward.to,
                    kills: reward.kills,
                    money: reward.money,
                    items: (reward.items || []).map(function (item) {
                        return {material: item.material, amount: item.amount};
                    })
                };
            });

            /* --- the basics */
            var nameInput = el('input', {type: 'text', value: event.name});
            var descriptionInput = el('input', {type: 'text', value: event.description || ''});
            var startInput = el('input', {type: 'datetime-local', value: toLocalInput(event.startsAt)});
            var endInput = el('input', {type: 'datetime-local', value: toLocalInput(event.endsAt)});
            // what the inputs showed when the form opened: an input only has minutes, so an untouched time
            // is sent back exactly as it was instead of losing its seconds
            var startShown = startInput.value;
            var endShown = endInput.value;
            if (event.started) startInput.disabled = true;

            /* --- the settings */
            var settingInputs = [];
            var settingFields = (event.fields || []).map(function (setting) {
                var input;
                if (setting.kind === 'toggle') {
                    input = el('input', {type: 'checkbox'});
                    input.checked = setting.value === 'true';
                    settingInputs.push({key: setting.key, read: function () {
                        return input.checked ? 'true' : 'false';
                    }});
                    return el('div', {className: 'field'}, [
                        el('label', {className: 'checkbox'}, [input, document.createTextNode(' ' + setting.title)]),
                        setting.description.length
                            ? el('p', {className: 'hint', text: setting.description.join(' ')})
                            : null
                    ]);
                }
                input = el('select');
                setting.options.forEach(function (choice) {
                    var option = el('option', {text: choice.label});
                    option.value = choice.value;
                    input.appendChild(option);
                });
                input.value = setting.value;
                settingInputs.push({key: setting.key, read: function () {
                    return input.value;
                }});
                return el('div', {className: 'field'}, [
                    el('label', {text: setting.title}),
                    input,
                    setting.description.length
                        ? el('p', {className: 'hint', text: setting.description.join(' ')})
                        : null
                ]);
            });

            /* --- the rewards */
            var rewardsHost = el('div', {className: 'stack'});

            function renderRewards() {
                clear(rewardsHost);
                if (!rewards.length) {
                    rewardsHost.appendChild(el('p', {className: 'muted', text: 'Noch keine Belohnungen.'}));
                }
                rewards.forEach(function (reward, index) {
                    rewardsHost.appendChild(renderReward(reward, index));
                });
            }

            function renderReward(reward, index) {
                var title = el('h4', {text: (index + 1) + '. ' + describeWho(reward)});
                function retitle() {
                    title.textContent = (index + 1) + '. ' + describeWho(reward);
                }

                var whoSelect = el('select');
                WHO.forEach(function (choice) {
                    if (choice.kills && !event.countsKills && reward.who !== 'KILLS') return;
                    var option = el('option', {text: choice.label});
                    option.value = choice.value;
                    whoSelect.appendChild(option);
                });
                whoSelect.value = reward.who;
                whoSelect.addEventListener('change', function () {
                    reward.who = whoSelect.value;
                    if (reward.who === 'PLACE' && !reward.from) {
                        reward.from = 1;
                        reward.to = 1;
                    }
                    if (reward.who === 'KILLS' && !reward.kills) reward.kills = 1;
                    renderRewards();
                });

                var whoFields = [field('Wer bekommt sie?', whoSelect)];
                if (reward.who === 'PLACE') {
                    var fromInput = numberInput(reward.from, 1);
                    var toInput = numberInput(reward.to ? reward.to : null, 1, 'bis zum letzten');
                    fromInput.addEventListener('input', function () {
                        reward.from = parseInt(fromInput.value, 10) || 0;
                        retitle();
                    });
                    toInput.addEventListener('input', function () {
                        // empty means down to the last placing, which the server stores as zero
                        reward.to = toInput.value === '' ? 0 : (parseInt(toInput.value, 10) || 0);
                        retitle();
                    });
                    whoFields.push(field('Von Platz', fromInput, 'narrow'));
                    whoFields.push(field('Bis Platz', toInput, 'narrow'));
                } else if (reward.who === 'KILLS') {
                    var killsInput = numberInput(reward.kills, 1);
                    killsInput.addEventListener('input', function () {
                        reward.kills = parseInt(killsInput.value, 10) || 0;
                        retitle();
                    });
                    whoFields.push(field('Ab Kills', killsInput, 'narrow'));
                }

                var moneyInput = numberInput(reward.money, 0);
                moneyInput.addEventListener('input', function () {
                    reward.money = parseInt(moneyInput.value, 10) || 0;
                });
                whoFields.push(field('Bits', moneyInput, 'narrow'));

                var itemRows = reward.items.map(function (item, itemIndex) {
                    var materialInput = el('input', {type: 'text', value: item.material, placeholder: 'z.B. DIAMOND'});
                    materialInput.setAttribute('list', 'event-material-options');
                    materialInput.addEventListener('input', function () {
                        item.material = materialInput.value.trim().toUpperCase();
                    });
                    var amountInput = numberInput(item.amount, 1);
                    amountInput.addEventListener('input', function () {
                        item.amount = parseInt(amountInput.value, 10) || 0;
                    });
                    return el('div', {className: 'inline-form'}, [
                        field('Item', materialInput),
                        field('Anzahl', amountInput, 'narrow'),
                        el('button', {
                            text: 'Entfernen', type: 'button', className: 'small secondary',
                            onClick: function () {
                                reward.items.splice(itemIndex, 1);
                                renderRewards();
                            }
                        })
                    ]);
                });

                return el('div', {className: 'reward-card'}, [
                    title,
                    el('div', {className: 'inline-form'}, whoFields),
                    el('div', {className: 'stack'}, itemRows),
                    el('div', {className: 'actions wrap'}, [
                        el('button', {
                            text: 'Item hinzufügen', type: 'button', className: 'small secondary',
                            onClick: function () {
                                reward.items.push({material: '', amount: 1});
                                renderRewards();
                            }
                        }),
                        el('button', {
                            text: 'Belohnung entfernen', type: 'button', className: 'small danger',
                            onClick: function () {
                                rewards.splice(index, 1);
                                renderRewards();
                            }
                        })
                    ])
                ]);
            }

            var addRewardButton = el('button', {
                text: 'Neue Belohnung', type: 'button', className: 'small',
                onClick: function () {
                    var place = nextFreePlace(rewards);
                    rewards.push({who: 'PLACE', from: place, to: place, kills: 1, money: 0, items: []});
                    renderRewards();
                }
            });

            /* --- saving */
            var saveButton = el('button', {text: 'Speichern', type: 'button'});
            saveButton.addEventListener('click', function () {
                if (!nameInput.value.trim()) {
                    toast('Es fehlt der Name des Events.', 'error');
                    return;
                }
                if (!startInput.value || !endInput.value) {
                    toast('Anfang und Ende müssen gesetzt sein.', 'error');
                    return;
                }
                var startsAt = startInput.value === startShown
                    ? event.startsAt : new Date(startInput.value).getTime();
                var endsAt = endInput.value === endShown ? event.endsAt : new Date(endInput.value).getTime();
                if (endsAt <= startsAt) {
                    toast('Das Event endet vor seinem Anfang.', 'error');
                    return;
                }
                var settings = {};
                settingInputs.forEach(function (input) {
                    settings[input.key] = input.read();
                });
                var body = {
                    revision: event.revision,
                    name: nameInput.value.trim(),
                    description: descriptionInput.value.trim(),
                    startsAt: String(startsAt),
                    endsAt: String(endsAt),
                    settings: settings
                };
                // an event that ranks nobody has no rewards to send - leaving them out keeps it that way
                if (event.ranked) {
                    body.rewards = rewards.map(function (reward) {
                        return {
                            who: reward.who,
                            from: reward.from,
                            to: reward.to,
                            kills: reward.kills,
                            money: reward.money,
                            items: reward.items.filter(function (item) {
                                return item.material;
                            })
                        };
                    });
                }
                saveButton.disabled = true;
                api('/api/events/' + encodeURIComponent(event.id), {method: 'POST', body: body})
                    .then(function (data) {
                        toast(data.message, 'ok');
                        closeEditor();
                        refresh();
                    }).catch(function (error) {
                        toast(error.message, 'error');
                        saveButton.disabled = false;
                    });
            });

            var rewardsSection;
            if (event.ranked) {
                renderRewards();
                rewardsSection = el('div', {className: 'stack'}, [
                    el('h3', {text: 'Belohnungen'}),
                    el('p', {className: 'hint', text: 'Jede Belohnung, die auf jemanden passt, wird ausgezahlt - '
                        + 'wer Platz 1 und 5 Kills hat, bekommt beides. Ausgezahlt wird, wenn das Event endet; '
                        + 'wer offline ist, bekommt es beim nächsten Join.'}),
                    PLACING_NOTES[event.type] ? el('p', {className: 'hint', text: PLACING_NOTES[event.type]}) : null,
                    rewardsHost,
                    el('div', {className: 'actions'}, [addRewardButton])
                ]);
            } else {
                rewardsSection = el('p', {
                    className: 'hint',
                    text: event.typeTitle + ' wertet niemanden - hier gibt es keine Belohnungen.'
                });
            }

            [
                el('h2', {text: 'Bearbeiten: ' + event.name}),
                el('p', {className: 'muted', text: event.typeTitle + (event.started
                    ? ' · hat schon angefangen, der Anfang bleibt, wie er ist'
                    : '')}),
                el('div', {className: 'inline-form'}, [
                    field('Name', nameInput),
                    field('Anfang', startInput, 'time'),
                    field('Ende', endInput, 'time'),
                    field('Beschreibung', descriptionInput, 'wide')
                ]),
                settingFields.length ? el('h3', {text: 'Einstellungen'}) : null,
                settingFields.length ? el('div', {className: 'inline-form'}, settingFields) : null,
                rewardsSection,
                el('div', {className: 'actions'}, [
                    saveButton,
                    el('button', {text: 'Abbrechen', type: 'button', className: 'secondary', onClick: closeEditor})
                ])
            ].forEach(function (node) {
                if (node) editorHost.appendChild(node);
            });
            editorHost.scrollIntoView({behavior: 'smooth', block: 'start'});
        }

        var latest = [];

        function findEvent(id) {
            for (var i = 0; i < latest.length; i++) {
                if (latest[i].id === id) return latest[i];
            }
            return null;
        }

        function refresh() {
            return api('/api/events').then(function (data) {
                var events = data.events || [];
                latest = events;
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
