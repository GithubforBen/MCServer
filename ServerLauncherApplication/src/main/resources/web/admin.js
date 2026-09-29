/*
 * The network as a whole, and the settings: /neustart from the browser, the memory of the machine, the
 * accounts of this website and what install.sh asks for.
 */
(function () {
    'use strict';

    var api = McAdmin.api;
    var el = McAdmin.el;
    var clear = McAdmin.clear;
    var toast = McAdmin.toast;
    var autoRefresh = McAdmin.autoRefresh;

    function stat(label, value) {
        return el('div', {className: 'stat'}, [
            el('span', {className: 'stat-label', text: label}),
            el('span', {className: 'stat-value', text: value})
        ]);
    }

    function field(label, input, className) {
        return el('div', {className: 'field' + (className ? ' ' + className : '')}, [el('label', {text: label}), input]);
    }

    function password(placeholder, autocomplete) {
        var input = el('input', {type: 'password', placeholder: placeholder || ''});
        input.autocomplete = autocomplete || 'current-password';
        return input;
    }

    function duration(seconds) {
        var days = Math.floor(seconds / 86400);
        var hours = Math.floor(seconds % 86400 / 3600);
        var minutes = Math.floor(seconds % 3600 / 60);
        if (days) return days + ' d ' + hours + ' h';
        if (hours) return hours + ' h ' + minutes + ' min';
        if (minutes) return minutes + ' min ' + (seconds % 60) + ' s';
        return seconds + ' s';
    }

    function mb(value) {
        return value >= 1024 ? (value / 1024).toFixed(1).replace('.', ',') + ' GB' : value + ' MB';
    }

    /**
     * A button that has to be pressed twice, for what can not be taken back. The first press only changes
     * its label; it falls back after a few seconds.
     */
    function armed(button, label, armedLabel, action) {
        var timer = null;
        button.addEventListener('click', function () {
            if (button.dataset.armed !== 'yes' && armedLabel(button)) {
                button.dataset.armed = 'yes';
                button.textContent = armedLabel(button);
                timer = setTimeout(function () {
                    button.dataset.armed = '';
                    button.textContent = label;
                }, 4000);
                return;
            }
            clearTimeout(timer);
            button.dataset.armed = '';
            button.textContent = label;
            action();
        });
    }

    /** Runs a request with the button disabled, and reports the answer. */
    function send(button, path, method, body, done) {
        button.disabled = true;
        return api(path, {method: method, body: body}).then(function (data) {
            if (data.message) toast(data.message, 'ok');
            if (done) done(data);
        }).catch(function (error) {
            toast(error.message, 'error');
        }).finally(function () {
            button.disabled = false;
        });
    }

    /** The QR code, the secret and the link for an authenticator app. */
    function authenticatorBox(data, intro) {
        var image = el('img', {className: 'qr'});
        image.src = data.qr;
        image.alt = 'QR-Code für Google Authenticator';
        return el('div', {className: 'qr-box'}, [
            image,
            el('div', {className: 'grow'}, [
                el('p', {text: intro}),
                el('label', {text: 'Schlüssel'}),
                el('div', {className: 'secret', text: data.secret})
            ])
        ]);
    }

    /* ------------------------------------------------------------------ panel: network */

    McAdmin.registerPanel('network', function (panel, module) {
        var stats = el('div', {className: 'stats'});
        var lastUpdate = el('p', {className: 'message hidden'});
        var plan = el('p', {className: 'message wait hidden'});

        var modeSelect = el('select');
        [['neustart', 'Neustart'], ['update', 'Update (git pull, bauen, neu starten)'], ['aus', 'Herunterfahren']]
            .forEach(function (option) {
                modeSelect.appendChild(el('option', {text: option[1], value: option[0]}));
            });
        var minutesInput = el('input', {type: 'number', value: '5'});
        minutesInput.min = '0';
        var scheduleButton = el('button', {text: 'Planen', type: 'button'});
        var cancelButton = el('button', {text: 'Absagen', type: 'button', className: 'secondary hidden'});

        armed(scheduleButton, 'Planen', function () {
            if (modeSelect.value === 'aus') return 'Wirklich herunterfahren?';
            if (Number(minutesInput.value) === 0) return 'Wirklich sofort?';
            return null;
        }, function () {
            send(scheduleButton, '/api/network/restart', 'POST',
                {mode: modeSelect.value, minutes: Number(minutesInput.value)}, refresh);
        });
        cancelButton.addEventListener('click', function () {
            send(cancelButton, '/api/network/restart/cancel', 'POST', {}, refresh);
        });

        panel.appendChild(el('section', {className: 'card'}, [
            el('h2', {text: module.title}),
            el('p', {className: 'muted', text: module.description}),
            stats,
            lastUpdate
        ]));
        panel.appendChild(el('section', {className: 'card'}, [
            el('h3', {text: 'Neustart'}),
            el('p', {className: 'muted', text: 'Wie /neustart im Spiel: Countdown für alle Spieler, dann wird jeder '
                    + 'Server gespeichert und gestoppt. Ein neuer Plan ersetzt den alten.'}),
            plan,
            el('div', {className: 'inline-form'}, [
                field('Art', modeSelect, 'wide'),
                field('Minuten', minutesInput, 'narrow'),
                scheduleButton,
                cancelButton
            ])
        ]));

        var memoryBar = el('div', {className: 'timeline-bar'});
        var memoryStats = el('div', {className: 'stats'});
        var advice = el('div', {className: 'rows'});
        var memoryCard = el('section', {className: 'card hidden'}, [
            el('h3', {text: 'Arbeitsspeicher'}),
            el('div', {className: 'timeline'}, [memoryBar]),
            memoryStats,
            advice
        ]);
        panel.appendChild(memoryCard);

        function refresh() {
            api('/api/network').then(function (data) {
                var version = data.version || {};
                clear(stats);
                stats.appendChild(stat('Stand', version.commit
                    ? version.commit + (version.branch ? ' (' + version.branch + ')' : '') : 'unbekannt'));
                stats.appendChild(stat('Letzter Commit', version.subject || '-'));
                stats.appendChild(stat('Launcher läuft seit', duration(data.uptimeSeconds || 0)));

                var restart = data.restart || {};
                lastUpdate.textContent = restart.lastUpdate ? 'Letztes Update: ' + restart.lastUpdate : '';
                lastUpdate.className = restart.lastUpdate
                    ? 'message ' + (/fehlgeschlagen|übersprungen/i.test(restart.lastUpdate) ? 'wait' : 'ok')
                    : 'message hidden';

                if (restart.scheduled) {
                    plan.textContent = restart.modeTitle + ' in ' + duration(restart.secondsLeft)
                        + ' - geplant von ' + restart.requestedBy + '.';
                    plan.className = 'message wait';
                    cancelButton.className = 'secondary';
                } else {
                    plan.className = 'message wait hidden';
                    cancelButton.className = 'secondary hidden';
                }
                minutesInput.max = String(data.maxMinutes || 1440);

                var memory = data.memory;
                memoryCard.className = memory ? 'card' : 'card hidden';
                if (!memory) return;
                var used = memory.budget ? Math.min(100, memory.allocated / memory.budget * 100) : 0;
                memoryBar.style.width = Math.max(1, used) + '%';
                memoryBar.className = 'timeline-bar state-' + (used > 90 ? 'alarm' : used > 70 ? 'caution' : 'nominal');
                clear(memoryStats);
                memoryStats.appendChild(stat('Maschine', mb(memory.machine)));
                memoryStats.appendChild(stat('Reserve', mb(memory.reserve)));
                memoryStats.appendChild(stat('Vergeben', mb(memory.allocated) + ' / ' + mb(memory.budget)));
                memoryStats.appendChild(stat('Frei', mb(memory.free)));
                memoryStats.appendChild(stat('Abgelehnt (7 Tage)', memory.refusedRecently + ' / ' + memory.refusedTotal));
                clear(advice);
                (memory.advice || []).forEach(function (entry) {
                    advice.appendChild(el('div', {className: 'row state-caution'}, [
                        el('div', {className: 'grow'}, [
                            el('div', {className: 'name', text: entry.server + ': ' + mb(entry.suggested) + ' würden reichen'}),
                            el('div', {className: 'meta', text: mb(entry.allocated) + ' zugewiesen, Spitze '
                                    + mb(entry.peak) + ' - macht ' + mb(entry.freed) + ' frei'})
                        ])
                    ]));
                });
            }).catch(function (error) {
                toast(error.message, 'error');
            });
        }

        refresh();
        autoRefresh(refresh, 5);
    });

    /* ------------------------------------------------------------------ panel: settings */

    McAdmin.registerPanel('settings', function (panel, module) {
        panel.appendChild(el('section', {className: 'card'}, [
            el('h2', {text: module.title}),
            el('p', {className: 'muted', text: module.description + ' Alles, was jemanden aussperren oder '
                    + 'hereinlassen kann, braucht zusätzlich dein Passwort - die Prüfung dauert ein paar Sekunden.'})
        ]));

        var passwordCard = el('section', {className: 'card'});
        var totpCard = el('section', {className: 'card'});
        var accountsCard = el('section', {className: 'card'});
        var networkCard = el('section', {className: 'card'});
        panel.appendChild(passwordCard);
        panel.appendChild(totpCard);
        panel.appendChild(accountsCard);
        panel.appendChild(networkCard);

        api('/api/settings').then(function (data) {
            renderPassword(data);
            renderTotp();
            renderAccounts(data);
            renderNetwork(data);
        }).catch(function (error) {
            toast(error.message, 'error');
        });

        function renderPassword(data) {
            var current = password('');
            var next = password('mindestens 12 Zeichen', 'new-password');
            var repeat = password('', 'new-password');
            var button = el('button', {text: 'Ändern', type: 'button'});
            button.addEventListener('click', function () {
                if (next.value.length < 12) return toast('Das neue Passwort braucht mindestens 12 Zeichen.', 'error');
                if (next.value !== repeat.value) return toast('Die beiden neuen Passwörter stimmen nicht überein.', 'error');
                send(button, '/api/account/password', 'POST', {current: current.value, next: next.value}, function () {
                    current.value = next.value = repeat.value = '';
                });
            });
            passwordCard.appendChild(el('h3', {text: 'Passwort von ' + data.username}));
            passwordCard.appendChild(el('div', {className: 'inline-form'}, [
                field('Aktuelles Passwort', current),
                field('Neues Passwort', next),
                field('Wiederholen', repeat),
                button
            ]));
        }

        function renderTotp() {
            var current = password('');
            var startButton = el('button', {text: 'Neuen Schlüssel erzeugen', type: 'button', className: 'secondary'});
            var pending = el('div', {className: 'hidden'});
            startButton.addEventListener('click', function () {
                send(startButton, '/api/account/totp', 'POST', {current: current.value}, function (data) {
                    current.value = '';
                    var code = el('input', {type: 'text', placeholder: '123456'});
                    code.inputMode = 'numeric';
                    code.autocomplete = 'one-time-code';
                    var confirm = el('button', {text: 'Bestätigen', type: 'button'});
                    confirm.addEventListener('click', function () {
                        send(confirm, '/api/account/totp/confirm', 'POST', {code: code.value}, function () {
                            clear(pending);
                            pending.className = 'hidden';
                        });
                    });
                    clear(pending);
                    pending.className = '';
                    pending.appendChild(authenticatorBox(data, 'Mit Google Authenticator scannen und danach den '
                        + 'Code aus dem neuen Eintrag eingeben. Erst dann gilt der neue Schlüssel - bis dahin der alte.'));
                    pending.appendChild(el('div', {className: 'inline-form'}, [field('Code', code, 'narrow'), confirm]));
                });
            });
            totpCard.appendChild(el('h3', {text: 'Google Authenticator'}));
            totpCard.appendChild(el('p', {className: 'muted', text: 'Neues Handy, oder der Schlüssel ist bekannt '
                    + 'geworden? Hier bekommt dein Account einen neuen.'}));
            totpCard.appendChild(el('div', {className: 'inline-form'}, [field('Dein Passwort', current), startButton]));
            totpCard.appendChild(pending);
        }

        function renderAccounts(data) {
            var rows = el('div', {className: 'rows'});
            var mine = password('für Anlegen und Löschen');
            var nameInput = el('input', {type: 'text', placeholder: 'z.B. moderator'});
            var createButton = el('button', {text: 'Anlegen', type: 'button'});
            var created = el('div', {className: 'hidden'});

            function draw(accounts) {
                clear(rows);
                accounts.forEach(function (name) {
                    var self = name.toLowerCase() === data.username.toLowerCase();
                    var actions = el('div', {className: 'actions'});
                    if (!self) {
                        var remove = el('button', {text: 'Löschen', type: 'button', className: 'small danger'});
                        armed(remove, 'Löschen', function () {
                            return 'Wirklich?';
                        }, function () {
                            send(remove, '/api/accounts/' + encodeURIComponent(name), 'DELETE', {current: mine.value},
                                function () {
                                    draw(accounts.filter(function (other) {
                                        return other !== name;
                                    }));
                                });
                        });
                        actions.appendChild(remove);
                    }
                    rows.appendChild(el('div', {className: 'row'}, [
                        el('div', {className: 'grow'}, [el('div', {className: 'name', text: name})]),
                        self ? el('span', {className: 'badge op', text: 'Du'}) : null,
                        actions
                    ]));
                });
            }

            createButton.addEventListener('click', function () {
                send(createButton, '/api/accounts', 'POST', {name: nameInput.value, current: mine.value}, function (result) {
                    nameInput.value = '';
                    data.accounts.push(result.username);
                    draw(data.accounts);
                    clear(created);
                    created.className = '';
                    created.appendChild(el('p', {className: 'message ok',
                        text: 'Diese Daten werden nur jetzt angezeigt - bitte sicher an ' + result.username + ' weitergeben.'}));
                    created.appendChild(el('div', {className: 'stats'}, [
                        stat('Benutzername', result.username),
                        stat('Passwort', result.password)
                    ]));
                    created.appendChild(authenticatorBox(result, 'Der neue Admin scannt diesen Code mit Google Authenticator.'));
                    var hide = el('button', {text: 'Ausblenden', type: 'button', className: 'secondary small'});
                    hide.addEventListener('click', function () {
                        clear(created);
                        created.className = 'hidden';
                    });
                    created.appendChild(hide);
                });
            });

            accountsCard.appendChild(el('h3', {text: 'Admin-Accounts'}));
            accountsCard.appendChild(rows);
            accountsCard.appendChild(el('div', {className: 'inline-form'}, [
                field('Neuer Account', nameInput),
                field('Dein Passwort', mine),
                createButton
            ]));
            accountsCard.appendChild(created);
            draw(data.accounts);
        }

        function renderNetwork(data) {
            function list(values) {
                var area = el('textarea');
                area.rows = 4;
                area.value = values.join('\n');
                return area;
            }

            function read(area) {
                return area.value.split(/[\s,]+/).filter(function (value) {
                    return value.length > 0;
                });
            }

            var owner = el('input', {type: 'text', value: data.owner});
            owner.inputMode = 'numeric';
            var brand = el('input', {type: 'text', value: data.brand});
            var ops = list(data.ops);
            var whitelist = list(data.whitelist);
            var autostart = list(data.autostart);
            var save = el('button', {text: 'Speichern', type: 'button'});
            save.addEventListener('click', function () {
                send(save, '/api/settings', 'POST', {
                    owner: owner.value,
                    brand: brand.value,
                    ops: read(ops),
                    whitelist: read(whitelist),
                    autostart: read(autostart)
                });
            });

            networkCard.appendChild(el('h3', {text: 'Netzwerk'}));
            networkCard.appendChild(el('div', {className: 'inline-form'}, [
                field('Discord-ID des Besitzers', owner),
                field('Name der Seite', brand)
            ]));
            networkCard.appendChild(el('div', {className: 'inline-form'}, [
                field('Operatoren', ops),
                field('Whitelist', whitelist),
                field('Autostart', autostart)
            ]));
            networkCard.appendChild(el('p', {className: 'hint', text: 'Ein Name pro Zeile. Ops und Whitelist gelten ab '
                    + 'dem nächsten Start eines Servers, Autostart ab dem nächsten Neustart des Netzwerks.'}));
            networkCard.appendChild(el('div', {className: 'actions'}, [save]));
        }
    });
})();
