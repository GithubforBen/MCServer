/*
 * The admin website.
 *
 * The page knows nothing about the modules the server offers: it asks for the list, builds the navigation
 * from it and looks up a renderer per module id. A module without a renderer still shows up and gets a
 * generic view, and a new renderer can be added with McAdmin.registerPanel(id, fn) - also from a script
 * that is loaded afterwards.
 */
(function () {
    'use strict';

    var state = {
        csrf: null,
        username: null,
        graceSeconds: 3,
        modules: [],
        activeModule: null,
        refreshTimer: null,
        onLeavePanel: null
    };

    var panels = {};

    /* ------------------------------------------------------------------ helpers */

    function $(id) {
        return document.getElementById(id);
    }

    /**
     * Builds an element. Text is always set through textContent, so nothing a server or a player typed can
     * turn into markup.
     */
    function el(tag, options, children) {
        var node = document.createElement(tag);
        options = options || {};
        if (options.className) node.className = options.className;
        if (options.text !== undefined) node.textContent = options.text;
        if (options.type) node.type = options.type;
        if (options.value !== undefined) node.value = options.value;
        if (options.placeholder) node.placeholder = options.placeholder;
        if (options.id) node.id = options.id;
        if (options.disabled) node.disabled = true;
        if (options.onClick) node.addEventListener('click', options.onClick);
        (children || []).forEach(function (child) {
            if (child) node.appendChild(child);
        });
        return node;
    }

    function clear(node) {
        while (node.firstChild) node.removeChild(node.firstChild);
    }

    var toastTimer = null;

    /**
     * Strips the leading marks the server puts on its messages. They exist because the same strings are
     * shown in the game chat, where a tick or a cross is the only formatting there is; on a page that
     * already colours a message by its outcome they are noise.
     */
    function plain(message) {
        return String(message === undefined || message === null ? '' : message)
            .replace(/^[\u2713\u2714\u274c\u2192\u26a0\ufe0f\s]+/, '')
            .trim();
    }

    function toast(message, kind) {
        var node = $('toast');
        node.textContent = plain(message);
        node.className = 'toast ' + (kind || '');
        if (toastTimer) clearTimeout(toastTimer);
        toastTimer = setTimeout(function () {
            node.className = 'toast hidden';
        }, 4000);
    }

    function ApiError(message, status, data) {
        this.message = message;
        this.status = status;
        this.data = data || {};
    }

    ApiError.prototype = Object.create(Error.prototype);

    /**
     * Talks to the api. Changing requests carry the csrf token, so a cookie alone is not enough to act on
     * behalf of a logged in admin.
     */
    function api(path, options) {
        options = options || {};
        var method = options.method || 'GET';
        var init = {method: method, headers: {}, credentials: 'same-origin'};
        if (options.body !== undefined) {
            init.headers['Content-Type'] = 'application/json';
            init.body = JSON.stringify(options.body);
        }
        if (state.csrf && method !== 'GET') init.headers['X-CSRF-Token'] = state.csrf;

        return fetch(path, init).then(function (response) {
            return response.json().catch(function () {
                return {};
            }).then(function (data) {
                if (response.status === 401 && !options.allowUnauthorized) {
                    showLogin(data.error || 'Die Sitzung ist abgelaufen.');
                    throw new ApiError(data.error || 'Nicht angemeldet.', 401, data);
                }
                if (!response.ok) {
                    throw new ApiError(data.error || ('Fehler ' + response.status), response.status, data);
                }
                return data;
            });
        });
    }

    /* ------------------------------------------------------------------ login */

    var graceTimer = null;

    function showLogin(message) {
        stopRefresh();
        if (state.onLeavePanel) {
            state.onLeavePanel();
            state.onLeavePanel = null;
        }
        state.csrf = null;
        state.username = null;
        $('app-view').classList.add('hidden');
        $('login-view').classList.remove('hidden');
        if (message) setLoginMessage(message, '');
        $('login-password').value = '';
        $('login-token').value = '';
    }

    function setLoginMessage(text, kind) {
        var node = $('login-message');
        node.textContent = plain(text);
        node.className = 'message ' + (kind || '');
        node.classList.toggle('hidden', !text);
    }

    /**
     * Blocks the button for the grace period the server enforces after every attempt, and counts it down so
     * the wait is visible instead of looking like the page hung.
     */
    function startGraceCountdown(seconds) {
        if (graceTimer) clearInterval(graceTimer);
        var remaining = Math.max(0, Math.ceil(seconds));
        var button = $('login-submit');
        if (remaining <= 0) {
            button.disabled = false;
            button.textContent = 'Anmelden';
            return;
        }
        button.disabled = true;
        button.textContent = 'Grace Period ' + remaining + 's';
        graceTimer = setInterval(function () {
            remaining -= 1;
            if (remaining <= 0) {
                clearInterval(graceTimer);
                graceTimer = null;
                button.disabled = false;
                button.textContent = 'Anmelden';
                return;
            }
            button.textContent = 'Grace Period ' + remaining + 's';
        }, 1000);
    }

    function onLoginSubmit(event) {
        event.preventDefault();
        var button = $('login-submit');
        if (button.disabled) return;

        var username = $('login-username').value.trim();
        var password = $('login-password').value;
        var token = $('login-token').value.trim();

        if (!token) {
            setLoginMessage('Bitte den Google Authenticator Code eingeben - ohne ihn wird das Passwort nicht geprüft.', '');
            return;
        }

        button.disabled = true;
        button.textContent = 'Wird geprüft ...';
        setLoginMessage('Code und Passwort werden geprüft. Die Antwort kommt nach der Grace Period von '
            + state.graceSeconds + ' Sekunden.', 'wait');

        api('/api/login', {
            method: 'POST',
            allowUnauthorized: true,
            body: {username: username, password: password, token: token}
        }).then(function (data) {
            state.csrf = data.csrfToken;
            state.username = data.username;
            state.graceSeconds = data.graceSeconds || state.graceSeconds;
            button.disabled = false;
            button.textContent = 'Anmelden';
            setLoginMessage('', '');
            $('login-password').value = '';
            $('login-token').value = '';
            showApp();
        }).catch(function (error) {
            setLoginMessage(error.message || 'Die Anmeldung ist fehlgeschlagen.', '');
            $('login-token').value = '';
            // the server refuses further attempts until the grace period of this one is over
            startGraceCountdown(error.data && error.data.retryAfter ? error.data.retryAfter : 0);
        });
    }

    /* ------------------------------------------------------------------ frame */

    function showApp() {
        $('login-view').classList.add('hidden');
        $('app-view').classList.remove('hidden');
        $('current-user').textContent = state.username || '';
        loadModules();
    }

    /**
     * Writes the network's own name into the head of the bank and onto the login card. It is configurable
     * because a placeholder is the one thing on the page nobody but the operator can fix.
     */
    function applyBrand(name) {
        if (!name) return;
        state.brand = name;
        var brand = $('brand');
        clear(brand);
        brand.appendChild(document.createTextNode(name + ' '));
        brand.appendChild(el('em', {text: '/ Admin'}));
        $('login-brand').textContent = name;
        document.title = name + ' Administration';
    }

    function loadModules() {
        api('/api/modules').then(function (data) {
            state.modules = data.modules || [];
            renderNav();
            var first = state.modules.length ? state.modules[0].id : null;
            selectModule(state.activeModule && findModule(state.activeModule) ? state.activeModule : first);
        }).catch(function (error) {
            toast(error.message, 'error');
        });
    }

    function findModule(id) {
        return state.modules.filter(function (module) {
            return module.id === id;
        })[0];
    }

    function renderNav() {
        var nav = $('nav');
        clear(nav);
        state.modules.forEach(function (module) {
            nav.appendChild(el('button', {
                text: module.title,
                type: 'button',
                className: module.id === state.activeModule ? 'active' : '',
                onClick: function () {
                    selectModule(module.id);
                }
            }));
        });
    }

    function selectModule(id) {
        if (!id) return;
        if (state.onLeavePanel) {
            state.onLeavePanel();
            state.onLeavePanel = null;
        }
        state.activeModule = id;
        renderNav();
        stopRefresh();
        var panel = $('panel');
        clear(panel);
        panel.appendChild(buildStrip());
        var module = findModule(id) || {id: id, title: id, description: ''};
        var renderer = panels[id] || renderUnknown;
        renderer(panel, module);
    }

    /* ------------------------------------------------------------------ status strip */

    /**
     * The three readings that belong above every panel: who is playing, what is running and how well the
     * worst server is keeping up.
     *
     * Each tally fills itself and survives its source being unreachable - a launcher that cannot answer
     * leaves a dash rather than taking the whole page with it.
     */
    function buildStrip() {
        var players = tally('Spieler online');
        var servers = tally('Server laufen');
        var tps = tally('Langsamster Server');
        var strip = el('div', {className: 'strip'}, [players.node, servers.node, tps.node]);

        api('/api/players').then(function (data) {
            var list = data.players || [];
            players.set(String(list.length), list.length ? 'nominal' : '');

            var loads = data.servers || [];
            if (!loads.length) {
                tps.set('–', '');
                return;
            }
            var worst = loads.reduce(function (a, b) {
                return b.tps < a.tps ? b : a;
            });
            // twenty ticks is the ceiling; below eighteen a server is visibly behind
            tps.set(worst.tps.toFixed(1), worst.tps >= 18 ? 'nominal' : (worst.tps >= 10 ? 'caution' : 'alarm'));
            tps.label(worst.name.toLowerCase());
        }).catch(function () {
            players.set('–', '');
            tps.set('–', '');
        });

        api('/api/servers').then(function (data) {
            var all = (data.servers || []);
            var up = all.filter(function (server) {
                return server.online;
            }).length;
            servers.set(String(up), up === all.length ? 'nominal' : 'caution', ' / ' + all.length);
        }).catch(function () {
            servers.set('–', '');
        });

        return strip;
    }

    /**
     * One reading of the status strip.
     *
     * @param labelText what it counts
     */
    function tally(labelText) {
        var num = el('span', {className: 'tally-num', text: '–'});
        var lab = el('span', {className: 'tally-lab', text: labelText});
        var node = el('div', {className: 'tally'}, [num, lab]);
        return {
            node: node,
            set: function (value, state, suffix) {
                clear(num);
                num.appendChild(document.createTextNode(value));
                if (suffix) num.appendChild(el('small', {text: suffix}));
                node.className = 'tally' + (state ? ' state-' + state : '');
            },
            label: function (text) {
                lab.textContent = labelText + ' · ' + text;
            }
        };
    }

    /**
     * What a module without its own renderer gets: its name and whatever its api returns.
     */
    function renderUnknown(panel, module) {
        var output = el('pre', {text: 'Lade ...'});
        panel.appendChild(el('section', {className: 'card'}, [
            el('h2', {text: module.title}),
            el('p', {className: 'muted', text: module.description
                    || 'Für dieses Modul gibt es noch keine eigene Ansicht.'}),
            output
        ]));
        api('/api/' + module.id).then(function (data) {
            output.textContent = JSON.stringify(data, null, 2);
        }).catch(function (error) {
            output.textContent = error.message;
        });
    }

    function stopRefresh() {
        if (state.refreshTimer) {
            clearInterval(state.refreshTimer);
            state.refreshTimer = null;
        }
    }

    function autoRefresh(fn, seconds) {
        stopRefresh();
        state.refreshTimer = setInterval(fn, seconds * 1000);
    }

    /* ------------------------------------------------------------------ panel: servers */

    panels['servers'] = function (panel, module) {
        var rows = el('div', {className: 'rows'});
        var status = el('p', {className: 'muted', text: 'Lade ...'});

        var nameInput = el('input', {type: 'text', placeholder: 'z.B. SOMMERFEST'});
        var templateSelect = el('select');
        var memoryInput = el('input', {type: 'number', placeholder: 'MB'});
        var createButton = el('button', {text: 'Server erstellen', type: 'button'});

        createButton.addEventListener('click', function () {
            var name = nameInput.value.trim();
            if (!name) {
                toast('Bitte einen Namen eingeben.', 'error');
                return;
            }
            var body = {name: name, template: templateSelect.value};
            if (memoryInput.value) body.memory = parseInt(memoryInput.value, 10);
            createButton.disabled = true;
            api('/api/servers', {method: 'POST', body: body}).then(function (data) {
                toast(data.message, 'ok');
                nameInput.value = '';
                memoryInput.value = '';
                refresh();
            }).catch(function (error) {
                toast(error.message, 'error');
            }).finally(function () {
                createButton.disabled = false;
            });
        });

        panel.appendChild(el('section', {className: 'card'}, [
            el('h2', {text: module.title}),
            el('p', {className: 'muted', text: module.description}),
            status,
            rows
        ]));

        panel.appendChild(el('section', {className: 'card'}, [
            el('h3', {text: 'Neuen Server erstellen'}),
            el('div', {className: 'inline-form'}, [
                el('div', {className: 'field'}, [el('label', {text: 'Name'}), nameInput]),
                el('div', {className: 'field'}, [el('label', {text: 'Template'}), templateSelect]),
                el('div', {className: 'field'}, [el('label', {text: 'Speicher (MB)'}), memoryInput]),
                createButton
            ])
        ]));

        api('/api/servers/templates').then(function (data) {
            (data.templates || []).forEach(function (template) {
                var option = el('option', {text: template.name + ' (' + template.defaultMemory + ' MB)'});
                option.value = template.name;
                templateSelect.appendChild(option);
            });
        }).catch(function () {
            /* the list of templates is a convenience - the panel works without it */
        });

        /**
         * Sends start, stop or restart for one server and refreshes the list afterwards.
         */
        function act(name, action, button) {
            button.disabled = true;
            api('/api/servers/' + encodeURIComponent(name) + '/' + action, {method: 'POST'})
                .then(function (data) {
                    toast(data.message, 'ok');
                    refresh();
                }).catch(function (error) {
                    toast(error.message, 'error');
                    button.disabled = false;
                });
        }

        function refresh() {
            api('/api/servers').then(function (data) {
                var servers = data.servers || [];
                status.textContent = servers.length
                    ? servers.filter(function (server) {
                        return server.online;
                    }).length + ' von ' + servers.length + ' Servern laufen.'
                    : 'Es ist noch kein Server angelegt.';
                clear(rows);
                servers.forEach(function (server) {
                    rows.appendChild(renderServerRow(server));
                });
            }).catch(function (error) {
                status.textContent = error.message;
            });
        }

        function renderServerRow(server) {
            var running = server.online || server.starting;
            var stateText = server.online ? 'läuft' : (server.starting ? 'startet ...' : 'aus');
            var dotClass = server.online ? 'online' : (server.starting ? 'starting' : 'offline');

            var meta = [];
            if (server.port && server.port > 0) meta.push('Port ' + server.port);
            if (server.memory) meta.push(server.memory + ' MB');
            if (server.template) meta.push(server.template);

            var actions = el('div', {className: 'actions'});
            if (running) {
                var stopButton = el('button', {text: 'Ausschalten', type: 'button', className: 'small danger'});
                stopButton.addEventListener('click', function () {
                    act(server.name, 'stop', stopButton);
                });
                var restartButton = el('button', {text: 'Neustart', type: 'button', className: 'small secondary'});
                restartButton.addEventListener('click', function () {
                    act(server.name, 'restart', restartButton);
                });
                actions.appendChild(stopButton);
                actions.appendChild(restartButton);
            } else {
                var startButton = el('button', {text: 'Anschalten', type: 'button', className: 'small'});
                startButton.addEventListener('click', function () {
                    act(server.name, 'start', startButton);
                });
                actions.appendChild(startButton);
            }

            return el('div', {className: 'row state-' + (server.online ? 'nominal'
                    : (server.starting ? 'caution' : 'alarm'))}, [
                el('div', {className: 'grow'}, [
                    el('div', {className: 'name', text: server.name}),
                    el('div', {className: 'meta', text: meta.join(' · ')})
                ]),
                el('span', {className: 'status'}, [
                    el('span', {className: 'dot ' + dotClass}),
                    el('span', {text: stateText})
                ]),
                actions
            ]);
        }

        refresh();
        autoRefresh(refresh, 5);
    };

    /* ------------------------------------------------------------------ panel: paying players */

    panels['paying-players'] = function (panel, module) {
        var rows = el('div', {className: 'rows'});
        var status = el('p', {className: 'muted', text: 'Lade ...'});
        var input = el('input', {type: 'text', placeholder: 'Minecraft-Name oder UUID'});
        var addButton = el('button', {text: 'Hinzufügen', type: 'button'});

        function add() {
            var value = input.value.trim();
            if (!value) {
                toast('Bitte einen Namen oder eine UUID eingeben.', 'error');
                return;
            }
            var body = value.indexOf('-') > 0 && value.length > 30 ? {uuid: value} : {name: value};
            addButton.disabled = true;
            api('/api/paying-players', {method: 'POST', body: body}).then(function (data) {
                toast(data.message, 'ok');
                input.value = '';
                refresh();
            }).catch(function (error) {
                toast(error.message, 'error');
            }).finally(function () {
                addButton.disabled = false;
            });
        }

        addButton.addEventListener('click', add);
        input.addEventListener('keydown', function (event) {
            if (event.key === 'Enter') add();
        });

        panel.appendChild(el('section', {className: 'card'}, [
            el('h2', {text: module.title}),
            el('p', {className: 'muted', text: module.description}),
            el('div', {className: 'inline-form'}, [
                el('div', {className: 'field'}, [el('label', {text: 'Spieler'}), input]),
                addButton
            ])
        ]));

        panel.appendChild(el('section', {className: 'card'}, [
            el('h3', {text: 'Zahlende Spieler'}),
            status,
            rows
        ]));

        function refresh() {
            api('/api/paying-players').then(function (data) {
                var players = data.players || [];
                status.textContent = players.length
                    ? players.length + ' Spieler zahlen für den Server.'
                    : 'Es zahlt noch niemand.';
                clear(rows);
                players.forEach(function (player) {
                    var removeButton = el('button', {
                        text: 'Entfernen', type: 'button', className: 'small danger'
                    });
                    removeButton.addEventListener('click', function () {
                        removeButton.disabled = true;
                        api('/api/paying-players/' + encodeURIComponent(player.uuid), {method: 'DELETE'})
                            .then(function (result) {
                                toast(result.message, 'ok');
                                refresh();
                            }).catch(function (error) {
                                toast(error.message, 'error');
                                removeButton.disabled = false;
                            });
                    });
                    rows.appendChild(el('div', {className: 'row'}, [
                        el('div', {className: 'grow'}, [
                            el('div', {className: 'name', text: player.name}),
                            el('div', {className: 'meta', text: player.uuid})
                        ]),
                        el('div', {className: 'actions'}, [removeButton])
                    ]));
                });
            }).catch(function (error) {
                status.textContent = error.message;
            });
        }

        refresh();
    };

    /* ------------------------------------------------------------------ panel: console */

    panels['console'] = function (panel, module) {
        var serverSelect = el('select');
        var commandInput = el('input', {type: 'text', placeholder: 'z.B. say Hallo'});
        var sendButton = el('button', {text: 'Senden', type: 'button'});
        var output = el('div', {className: 'console-output'});
        var statusDot = el('span', {className: 'dot down'});
        var statusText = el('span', {text: 'nicht verbunden'});
        var followBox = el('input', {type: 'checkbox'});
        followBox.checked = true;

        var socket = null;
        var reconnectTimer = null;
        var closedByUs = false;

        /* Keeps the view pinned to the newest line unless the user scrolled up to read something. */
        function atBottom() {
            return output.scrollHeight - output.scrollTop - output.clientHeight < 40;
        }

        function appendLine(text, kind) {
            var follow = followBox.checked && atBottom();
            output.appendChild(el('div', {className: 'line ' + (kind || ''), text: text}));
            // a long running server produces a lot of output - keep the dom from growing without bound
            while (output.childElementCount > 2000) output.removeChild(output.firstChild);
            if (follow) output.scrollTop = output.scrollHeight;
        }

        /* Colours the obvious severities without pretending to parse every log format. */
        function kindOf(line) {
            if (/\b(ERROR|SEVERE|FATAL|Exception)\b/.test(line)) return 'error';
            if (/\bWARN(ING)?\b/.test(line)) return 'warn';
            return '';
        }

        function setStatus(text, live) {
            statusText.textContent = text;
            statusDot.className = 'dot ' + (live ? 'live' : 'down');
        }

        function disconnect() {
            closedByUs = true;
            if (reconnectTimer) {
                clearTimeout(reconnectTimer);
                reconnectTimer = null;
            }
            if (socket) {
                socket.close();
                socket = null;
            }
            setStatus('nicht verbunden', false);
        }

        function connect(server) {
            disconnect();
            closedByUs = false;
            clear(output);
            if (!server) {
                setStatus('kein Server ausgewählt', false);
                return;
            }
            setStatus('verbinde ...', false);
            var scheme = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
            var url = scheme + window.location.host + '/api/console/stream?server=' + encodeURIComponent(server);
            var current;
            try {
                current = new WebSocket(url);
            } catch (error) {
                setStatus('Verbindung nicht möglich', false);
                return;
            }
            socket = current;

            current.onopen = function () {
                setStatus('live', true);
            };
            current.onmessage = function (event) {
                var message;
                try {
                    message = JSON.parse(event.data);
                } catch (error) {
                    return;
                }
                if (message.type === 'history') {
                    clear(output);
                    (message.lines || []).forEach(function (line) {
                        appendLine(line, kindOf(line));
                    });
                    output.scrollTop = output.scrollHeight;
                } else if (message.type === 'line') {
                    appendLine(message.line, kindOf(message.line));
                } else if (message.type === 'error') {
                    appendLine('[' + message.message + ']', 'system');
                }
            };
            current.onclose = function () {
                if (socket !== current) return;
                socket = null;
                if (closedByUs) return;
                setStatus('Verbindung verloren - neuer Versuch in 3s', false);
                appendLine('[Verbindung zum Server verloren]', 'system');
                reconnectTimer = setTimeout(function () {
                    if (state.activeModule === 'console') connect(serverSelect.value);
                }, 3000);
            };
            current.onerror = function () {
                setStatus('Verbindungsfehler', false);
            };
        }

        function send() {
            var command = commandInput.value.trim();
            if (!serverSelect.value || !command) {
                toast('Bitte Server und Befehl angeben.', 'error');
                return;
            }
            sendButton.disabled = true;
            api('/api/console', {method: 'POST', body: {server: serverSelect.value, command: command}})
                .then(function () {
                    commandInput.value = '';
                }).catch(function (error) {
                    toast(error.message, 'error');
                }).finally(function () {
                    sendButton.disabled = false;
                });
        }

        sendButton.addEventListener('click', send);
        commandInput.addEventListener('keydown', function (event) {
            if (event.key === 'Enter') send();
        });
        serverSelect.addEventListener('change', function () {
            connect(serverSelect.value);
        });

        panel.appendChild(el('section', {className: 'card'}, [
            el('h2', {text: module.title}),
            el('p', {className: 'muted', text: module.description}),
            el('div', {className: 'console-toolbar'}, [
                el('div', {className: 'field'}, [el('label', {text: 'Server'}), serverSelect]),
                el('span', {className: 'console-status'}, [statusDot, statusText]),
                el('label', {className: 'checkbox'}, [followBox, el('span', {text: 'automatisch scrollen'})]),
                el('button', {
                    text: 'Leeren', type: 'button', className: 'secondary small',
                    onClick: function () {
                        clear(output);
                    }
                })
            ]),
            output
        ]));

        panel.appendChild(el('section', {className: 'card'}, [
            el('h3', {text: 'Befehl senden'}),
            el('div', {className: 'inline-form'}, [
                el('div', {className: 'field'}, [el('label', {text: 'Befehl'}), commandInput]),
                sendButton
            ])
        ]));

        api('/api/servers').then(function (data) {
            var running = (data.servers || []).filter(function (server) {
                return server.online || server.starting;
            });
            running.forEach(function (server) {
                var option = el('option', {text: server.name});
                option.value = server.name;
                serverSelect.appendChild(option);
            });
            if (running.length) {
                connect(running[0].name);
            } else {
                setStatus('kein Server läuft', false);
            }
        }).catch(function (error) {
            toast(error.message, 'error');
        });

        // leaving the panel has to close the socket, or every visit would open another one
        state.onLeavePanel = disconnect;
    };

    /* ------------------------------------------------------------------ start */

    function boot() {
        $('login-form').addEventListener('submit', onLoginSubmit);
        $('logout').addEventListener('click', function () {
            api('/api/logout', {method: 'POST'}).catch(function () {
                /* the session is gone either way */
            }).then(function () {
                showLogin('Abgemeldet.');
            });
        });

        api('/api/session', {allowUnauthorized: true}).then(function (data) {
            state.graceSeconds = data.graceSeconds || state.graceSeconds;
            applyBrand(data.brand);
            if (data.authenticated) {
                state.csrf = data.csrfToken;
                state.username = data.username;
                showApp();
            } else {
                showLogin('');
            }
        }).catch(function () {
            showLogin('');
        });
    }

    /* The hook other scripts use to add a panel for a module they registered on the server. */
    window.McAdmin = {
        registerPanel: function (id, renderer) {
            panels[id] = renderer;
        },
        api: api,
        el: el,
        clear: clear,
        toast: toast,
        autoRefresh: autoRefresh,
        stopRefresh: stopRefresh
    };

    document.addEventListener('DOMContentLoaded', boot);
})();
