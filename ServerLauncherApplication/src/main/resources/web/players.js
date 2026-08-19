/*
 * The player manager and the CoreProtect view.
 *
 * Both are registered through the public McAdmin hook rather than being built into app.js - the same way
 * anybody adding a module to the server would add its panel here.
 *
 * There are no minecraft textures in this interface. Shipping the item atlas is neither legal nor small, so
 * a slot shows the item's name, its count and a stable colour derived from the material. That reads well
 * enough to spot what is in an inventory at a glance and needs nothing from outside the page.
 */
(function () {
    'use strict';

    var api = McAdmin.api;
    var el = McAdmin.el;
    var clear = McAdmin.clear;
    var toast = McAdmin.toast;

    /** How many hue classes style.css defines for item slots. */
    var HUE_COUNT = 12;

    /* ------------------------------------------------------------------ item helpers */

    /**
     * Turns MATERIAL_NAMES into readable text.
     */
    function prettyMaterial(name) {
        if (!name) return '';
        return name.toLowerCase().split('_').map(function (part) {
            return part.charAt(0).toUpperCase() + part.slice(1);
        }).join(' ');
    }

    /**
     * A stable colour per material, so the same item always looks the same without any texture.
     */
    function hueClassOf(name) {
        var hash = 0;
        for (var i = 0; i < name.length; i++) {
            hash = ((hash << 5) - hash + name.charCodeAt(i)) | 0;
        }
        return 'hue-' + (Math.abs(hash) % HUE_COUNT);
    }

    /**
     * Draws one inventory slot.
     *
     * @param item    what is in it, or null for an empty slot
     * @param slot    the slot number, used when the slot is clicked
     * @param onClick what to do when it is clicked
     * @param label   an optional label for special slots like armour
     */
    function renderSlot(item, slot, onClick, label, containerKey) {
        var node = el('button', {type: 'button', className: 'slot'});
        node.dataset.slot = String(slot);
        if (containerKey) makeDraggable(node, containerKey, slot, item);
        if (!item) {
            node.classList.add('empty');
            if (label) node.appendChild(el('span', {className: 'slot-label', text: label}));
        } else {
            node.classList.add(hueClassOf(item.material));
            if (item.enchantments && item.enchantments.length) node.classList.add('enchanted');
            node.appendChild(el('span', {className: 'slot-name', text: prettyMaterial(item.material)}));
            if (item.amount > 1) {
                node.appendChild(el('span', {className: 'slot-amount', text: String(item.amount)}));
            }
            var tooltip = prettyMaterial(item.material);
            if (item.displayName) tooltip += '\n"' + item.displayName + '"';
            if (item.enchantments && item.enchantments.length) {
                tooltip += '\n' + item.enchantments.join(', ');
            }
            if (item.damage) tooltip += '\nSchaden: ' + item.damage + '/' + item.maxDurability;
            node.title = tooltip;
        }
        node.addEventListener('click', function () {
            onClick(slot, item, node);
        });
        return node;
    }

    /**
     * Builds a row of slots.
     */
    function renderRow(items, from, count, onClick, labels, containerKey) {
        var row = el('div', {className: 'slot-row'});
        for (var i = 0; i < count; i++) {
            var slot = from + i;
            row.appendChild(renderSlot(items[slot] || null, slot, onClick,
                labels ? labels[i] : null, containerKey));
        }
        return row;
    }

    /* ------------------------------------------------------------------ drag and drop */

    /*
     * Every open container registers itself here, which is what lets an item be dragged from a player's
     * inventory into the admin stash: the drop only has to look the source container up by its key.
     */
    var containers = {};

    function resetContainers() {
        containers = {};
    }

    function registerContainer(container) {
        containers[container.key] = container;
    }

    /** The payload of a drag, kept next to the DOM one because dataTransfer is unreadable during dragover. */
    var dragging = null;

    /**
     * Makes a slot draggable when it holds something, and a drop target either way.
     */
    function makeDraggable(node, containerKey, slot, item) {
        if (item) {
            node.draggable = true;
            node.addEventListener('dragstart', function (event) {
                dragging = {key: containerKey, slot: slot};
                event.dataTransfer.effectAllowed = 'move';
                // firefox refuses to start a drag without any data set
                event.dataTransfer.setData('text/plain', containerKey + ':' + slot);
                node.classList.add('dragging');
            });
            node.addEventListener('dragend', function () {
                dragging = null;
                node.classList.remove('dragging');
                document.querySelectorAll('.slot.drop-target').forEach(function (other) {
                    other.classList.remove('drop-target');
                });
            });
        }
        node.addEventListener('dragover', function (event) {
            if (!dragging) return;
            event.preventDefault();
            event.dataTransfer.dropEffect = 'move';
            node.classList.add('drop-target');
        });
        node.addEventListener('dragleave', function () {
            node.classList.remove('drop-target');
        });
        node.addEventListener('drop', function (event) {
            event.preventDefault();
            node.classList.remove('drop-target');
            if (!dragging) return;
            moveItem(dragging, {key: containerKey, slot: slot});
            dragging = null;
        });
    }

    /**
     * Moves an item from one slot to another, swapping with whatever is already there. Both containers are
     * only changed in the browser - nothing is written until the save bar is used.
     */
    function moveItem(from, to) {
        var source = containers[from.key];
        var target = containers[to.key];
        if (!source || !target) return;
        if (from.key === to.key && from.slot === to.slot) return;

        var moved = source.items[from.slot];
        if (!moved) return;
        var displaced = target.items[to.slot] || null;

        // a slot that does not exist in the target - armour into a chest, say - would silently vanish
        if (to.slot >= target.size) {
            toast('Dieser Slot gehört nicht zum Ziel.', 'error');
            return;
        }

        target.items[to.slot] = Object.assign({}, moved, {slot: to.slot});
        source.items[from.slot] = displaced ? Object.assign({}, displaced, {slot: from.slot}) : null;

        source.dirty = true;
        target.dirty = true;
        source.redraw();
        if (target !== source) target.redraw();
        updateSaveBar();
    }

    /* ------------------------------------------------------------------ saving */

    var saveBar = null;

    function setSaveBar(node) {
        saveBar = node;
        updateSaveBar();
    }

    function dirtyContainers() {
        return Object.keys(containers).map(function (key) {
            return containers[key];
        }).filter(function (container) {
            return container.dirty;
        });
    }

    function updateSaveBar() {
        if (!saveBar) return;
        var dirty = dirtyContainers();
        saveBar.classList.toggle('hidden', dirty.length === 0);
        var label = saveBar.querySelector('.save-label');
        if (label) {
            label.textContent = dirty.length === 0 ? '' : 'Ungespeichert: '
                + dirty.map(function (container) { return container.title; }).join(', ');
        }
    }

    /**
     * Writes every changed container, one after another.
     *
     * There is no way to make this one operation: the player inventory lives inside a game server and the
     * stash on the launcher, so they are two different writes. Player inventories go first - if one of them
     * is refused nothing has been written anywhere yet and the browser still holds every change, so the
     * admin can simply try again. Doing it the other way round could leave an item in the stash *and* on
     * the player, which is the one outcome worth avoiding.
     */
    function saveAll(button) {
        var pending = dirtyContainers().sort(function (a, b) {
            return (a.kind === 'stash' ? 1 : 0) - (b.kind === 'stash' ? 1 : 0);
        });
        if (!pending.length) return;
        button.disabled = true;

        function step(index) {
            if (index >= pending.length) {
                button.disabled = false;
                toast('Alles gespeichert.', 'ok');
                updateSaveBar();
                return;
            }
            var container = pending[index];
            container.save().then(function (message) {
                container.dirty = false;
                container.redraw();
                updateSaveBar();
                toast(message, 'ok');
                step(index + 1);
            }).catch(function (error) {
                button.disabled = false;
                updateSaveBar();
                var left = pending.slice(index).map(function (c) { return c.title; }).join(', ');
                toast(container.title + ': ' + error.message
                    + ' - noch offen: ' + left + '. Die Änderungen sind noch im Browser.', 'error');
            });
        }

        step(0);
    }

    /* ------------------------------------------------------------------ item editor */

    /**
     * The little form that opens when a slot is clicked. Only material and amount can be changed; anything
     * else the item carries is kept by sending its original bytes back untouched.
     */
    function openSlotEditor(container, slot, item, state, redraw) {
        var existing = document.getElementById('slot-editor');
        if (existing) existing.remove();

        var materialInput = el('input', {
            type: 'text', id: 'slot-material', placeholder: 'z.B. DIAMOND_SWORD',
            value: item ? item.material : ''
        });
        materialInput.setAttribute('list', 'material-options');
        var amountInput = el('input', {
            type: 'number', id: 'slot-amount', value: item ? String(item.amount) : '1'
        });
        amountInput.min = '1';
        amountInput.max = '99';

        var applyButton = el('button', {text: 'Übernehmen', type: 'button', className: 'small'});
        var clearButton = el('button', {text: 'Slot leeren', type: 'button', className: 'small danger'});
        var cancelButton = el('button', {text: 'Abbrechen', type: 'button', className: 'small secondary'});

        applyButton.addEventListener('click', function () {
            var material = materialInput.value.trim().toUpperCase();
            if (!material) {
                toast('Bitte ein Material angeben.', 'error');
                return;
            }
            var amount = Math.max(1, parseInt(amountInput.value, 10) || 1);
            var previous = state.items[slot];
            state.items[slot] = {
                slot: slot,
                material: material,
                amount: amount,
                // keep the original bytes only while the material stays the same, so enchantments survive
                raw: previous && previous.material === material ? previous.raw : null,
                displayName: previous && previous.material === material ? previous.displayName : null,
                enchantments: previous && previous.material === material ? previous.enchantments : [],
                damage: previous && previous.material === material ? previous.damage : 0,
                maxDurability: previous ? previous.maxDurability : 0
            };
            state.dirty = true;
            editor.remove();
            redraw();
        });

        clearButton.addEventListener('click', function () {
            state.items[slot] = null;
            state.dirty = true;
            editor.remove();
            redraw();
        });

        cancelButton.addEventListener('click', function () {
            editor.remove();
        });

        var editor = el('div', {className: 'slot-editor card', id: 'slot-editor'}, [
            el('h4', {text: 'Slot ' + slot}),
            el('div', {className: 'inline-form'}, [
                el('div', {className: 'field'}, [el('label', {text: 'Material'}), materialInput]),
                el('div', {className: 'field narrow'}, [el('label', {text: 'Anzahl'}), amountInput])
            ]),
            item && item.enchantments && item.enchantments.length
                ? el('p', {className: 'hint', text: 'Verzauberungen: ' + item.enchantments.join(', ')
                    + ' - bleiben erhalten, solange das Material gleich bleibt.'})
                : null,
            el('div', {className: 'actions'}, [applyButton, clearButton, cancelButton])
        ]);
        container.appendChild(editor);
        materialInput.focus();
    }

    /* ------------------------------------------------------------------ container view */

    /**
     * Draws a whole container and its save button.
     *
     * @param host      where to draw
     * @param inventory what came back from the server
     * @param uuid      whose container it is
     */
    function renderContainer(host, inventory, uuid) {
        var key = 'player:' + uuid + ':' + inventory.kind + ':' + (inventory.containerId || '');
        var items = [];
        for (var i = 0; i < inventory.size; i++) items[i] = null;
        inventory.items.forEach(function (item) {
            if (item.slot >= 0 && item.slot < inventory.size) items[item.slot] = item;
        });

        var grid = el('div', {className: 'inventory'});
        var editorHost = el('div');

        var container = {
            key: key,
            title: inventory.title + ' von ' + inventory.playerName,
            kind: inventory.kind,
            size: inventory.size,
            items: items,
            dirty: false,
            redraw: redraw,
            save: save
        };
        registerContainer(container);

        function onSlotClick(slot) {
            openSlotEditor(editorHost, slot, container.items[slot], container, function () {
                redraw();
                updateSaveBar();
            });
        }

        function redraw() {
            clear(grid);
            if (inventory.kind === 'INVENTORY') {
                // the layout minecraft itself uses: storage on top, hotbar below, gear to the side
                grid.appendChild(el('p', {className: 'grid-caption', text: 'Inventar'}));
                grid.appendChild(renderRow(container.items, 9, 9, onSlotClick, null, key));
                grid.appendChild(renderRow(container.items, 18, 9, onSlotClick, null, key));
                grid.appendChild(renderRow(container.items, 27, 9, onSlotClick, null, key));
                grid.appendChild(el('p', {className: 'grid-caption', text: 'Hotbar'}));
                grid.appendChild(renderRow(container.items, 0, 9, onSlotClick, null, key));
                grid.appendChild(el('p', {className: 'grid-caption', text: 'Rüstung & Nebenhand'}));
                grid.appendChild(renderRow(container.items, 36, 4, onSlotClick,
                    ['Schuhe', 'Hose', 'Brust', 'Helm'], key));
                grid.appendChild(renderRow(container.items, 40, 1, onSlotClick, ['Nebenhand'], key));
            } else {
                var rows = Math.ceil(inventory.size / 9);
                for (var r = 0; r < rows; r++) {
                    grid.appendChild(renderRow(container.items, r * 9,
                        Math.min(9, inventory.size - r * 9), onSlotClick, null, key));
                }
            }
        }

        /**
         * Sends this container back to the game server the player is on.
         */
        function save() {
            var payload = {
                kind: inventory.kind,
                containerId: inventory.containerId,
                size: container.size,
                items: container.items.filter(Boolean).map(function (item, index) {
                    return item;
                }).map(function (item) {
                    return {slot: item.slot, material: item.material, amount: item.amount, raw: item.raw};
                })
            };
            // the slot each item sits in is the array index, not whatever it carried when it was read
            payload.items = [];
            container.items.forEach(function (item, slot) {
                if (!item) return;
                payload.items.push({slot: slot, material: item.material, amount: item.amount, raw: item.raw});
            });
            return api('/api/players/' + encodeURIComponent(uuid) + '/inventory',
                {method: 'POST', body: payload}).then(function (data) {
                return data.message;
            });
        }

        clear(host);
        host.appendChild(el('div', {className: 'container-head'}, [
            el('h3', {text: inventory.title}),
            el('span', {className: 'spacer'}),
            el('span', {className: 'muted hint-drag', text: 'Items lassen sich ziehen'})
        ]));
        host.appendChild(grid);
        host.appendChild(editorHost);
        redraw();
        updateSaveBar();
    }

    /* ------------------------------------------------------------------ the admin stash */

    /**
     * Draws the admin stash: the container items are dragged into, and the same chest /admin opens in game.
     *
     * @param host  where to draw it
     * @param onLoad called once it is there, so a caller can show a save bar
     */
    function renderStash(host, onLoad) {
        clear(host);
        host.appendChild(el('p', {className: 'muted', text: 'Ablage wird geladen ...'}));
        api('/api/stash').then(function (stash) {
            var items = [];
            for (var i = 0; i < stash.size; i++) items[i] = null;
            stash.items.forEach(function (item) {
                if (item.slot >= 0 && item.slot < stash.size) items[item.slot] = item;
            });

            var grid = el('div', {className: 'inventory'});
            var editorHost = el('div');
            var container = {
                key: 'stash',
                title: 'Admin-Ablage',
                kind: 'stash',
                size: stash.size,
                items: items,
                revision: stash.revision,
                dirty: false,
                redraw: redraw,
                save: save
            };
            registerContainer(container);

            function onSlotClick(slot) {
                openSlotEditor(editorHost, slot, container.items[slot], container, function () {
                    redraw();
                    updateSaveBar();
                });
            }

            function redraw() {
                clear(grid);
                var rows = Math.ceil(container.size / 9);
                for (var r = 0; r < rows; r++) {
                    grid.appendChild(renderRow(container.items, r * 9,
                        Math.min(9, container.size - r * 9), onSlotClick, null, 'stash'));
                }
            }

            function save() {
                var payload = {size: container.size, revision: container.revision, items: []};
                container.items.forEach(function (item, slot) {
                    if (!item) return;
                    payload.items.push({slot: slot, material: item.material, amount: item.amount, raw: item.raw});
                });
                return api('/api/stash', {method: 'POST', body: payload}).then(function (data) {
                    container.revision = data.revision;
                    return data.message;
                });
            }

            clear(host);
            host.appendChild(el('div', {className: 'container-head'}, [
                el('h3', {text: 'Admin-Ablage'}),
                el('span', {className: 'spacer'}),
                el('span', {className: 'muted hint-drag', text: 'Im Spiel mit /admin zu öffnen'})
            ]));
            host.appendChild(grid);
            host.appendChild(editorHost);
            redraw();
            updateSaveBar();
            if (onLoad) onLoad(container);
        }).catch(function (error) {
            clear(host);
            host.appendChild(el('p', {className: 'message', text: error.message}));
        });
    }

    /**
     * The bar that appears as soon as anything was moved.
     */
    function buildSaveBar() {
        var button = el('button', {text: 'Änderungen speichern', type: 'button'});
        var bar = el('div', {className: 'save-bar hidden'}, [
            el('span', {className: 'save-label'}),
            el('span', {className: 'spacer'}),
            button
        ]);
        button.addEventListener('click', function () {
            saveAll(button);
        });
        setSaveBar(bar);
        return bar;
    }

    /* ------------------------------------------------------------------ player panel */

    McAdmin.registerPanel('players', function (panel, module) {
        resetContainers();
        var listHost = el('div', {className: 'rows'});
        var status = el('p', {className: 'muted', text: 'Lade ...'});
        var search = el('input', {type: 'search', placeholder: 'Nach Name suchen ...'});
        var detailHost = el('div');
        var allPlayers = [];
        var selected = null;

        // the datalist the item editor completes materials from
        var materialOptions = el('datalist', {id: 'material-options'});
        panel.appendChild(materialOptions);
        api('/api/materials').then(function (data) {
            (data.materials || []).forEach(function (material) {
                var option = el('option');
                option.value = material.name;
                materialOptions.appendChild(option);
            });
        }).catch(function () {
            /* the editor still works by typing the name out */
        });

        search.addEventListener('input', function () {
            renderList();
        });

        panel.appendChild(el('section', {className: 'card'}, [
            el('h2', {text: module.title}),
            el('p', {className: 'muted', text: module.description}),
            el('div', {className: 'inline-form'}, [
                el('div', {className: 'field'}, [el('label', {text: 'Suche'}), search])
            ]),
            status,
            listHost
        ]));
        panel.appendChild(detailHost);

        function renderList() {
            var needle = search.value.trim().toLowerCase();
            clear(listHost);
            var shown = allPlayers.filter(function (player) {
                return !needle || (player.name || '').toLowerCase().indexOf(needle) >= 0;
            });
            if (!shown.length) {
                listHost.appendChild(el('p', {className: 'muted',
                    text: allPlayers.length ? 'Kein Spieler passt zur Suche.' : 'Gerade ist niemand online.'}));
                return;
            }
            shown.forEach(function (player) {
                var badges = el('div', {className: 'badges'});
                if (player.paying) badges.appendChild(el('span', {className: 'badge pay', text: 'zahlt'}));
                if (player.op) badges.appendChild(el('span', {className: 'badge op', text: 'OP'}));
                badges.appendChild(el('span', {className: 'badge', text: player.gameMode}));

                var openButton = el('button', {text: 'Verwalten', type: 'button', className: 'small'});
                openButton.addEventListener('click', function () {
                    openPlayer(player);
                });

                listHost.appendChild(el('div', {className: 'row'}, [
                    el('div', {className: 'grow'}, [
                        el('div', {className: 'name', text: player.name}),
                        el('div', {className: 'meta',
                            text: player.server + ' · ' + player.world
                                + ' · ' + player.x + '/' + player.y + '/' + player.z})
                    ]),
                    el('span', {className: 'status'}, [
                        el('span', {className: 'dot online'}),
                        el('span', {text: Math.round(player.health) + '/' + Math.round(player.maxHealth) + ' HP'})
                    ]),
                    badges,
                    el('div', {className: 'actions'}, [openButton])
                ]));
            });
        }

        function refresh() {
            api('/api/players').then(function (data) {
                allPlayers = data.players || [];
                status.textContent = allPlayers.length
                    ? allPlayers.length + ' Spieler online.'
                    : 'Gerade ist niemand online.';
                renderList();
                if (selected) {
                    var still = allPlayers.filter(function (p) { return p.uuid === selected.uuid; })[0];
                    if (!still) {
                        clear(detailHost);
                        selected = null;
                    }
                }
            }).catch(function (error) {
                status.textContent = error.message;
            });
        }

        /**
         * Opens the detail view of one player: what to do to them, and their containers.
         */
        function openPlayer(player) {
            selected = player;
            // the previous player's containers must not stay droppable behind the new view
            resetContainers();
            clear(detailHost);

            var actionResult = el('span', {className: 'muted'});

            function act(action, argument, button) {
                if (button) button.disabled = true;
                api('/api/players/' + encodeURIComponent(player.uuid) + '/action',
                    {method: 'POST', body: {action: action, argument: argument}})
                    .then(function (data) {
                        toast(data.message, 'ok');
                        actionResult.textContent = data.message;
                        refresh();
                    }).catch(function (error) {
                        toast(error.message, 'error');
                    }).finally(function () {
                        if (button) button.disabled = false;
                    });
            }

            function actionButton(label, action, argument, className) {
                var button = el('button', {
                    text: label, type: 'button', className: 'small ' + (className || 'secondary')
                });
                button.addEventListener('click', function () {
                    act(action, argument, button);
                });
                return button;
            }

            var gamemodeSelect = el('select');
            ['SURVIVAL', 'CREATIVE', 'ADVENTURE', 'SPECTATOR'].forEach(function (mode) {
                var option = el('option', {text: mode});
                option.value = mode;
                if (mode === player.gameMode) option.selected = true;
                gamemodeSelect.appendChild(option);
            });
            gamemodeSelect.addEventListener('change', function () {
                act('SET_GAMEMODE', gamemodeSelect.value, null);
            });

            var messageInput = el('input', {type: 'text', placeholder: 'Nachricht an den Spieler'});
            var messageButton = el('button', {text: 'Senden', type: 'button', className: 'small'});
            messageButton.addEventListener('click', function () {
                if (!messageInput.value.trim()) return;
                act('SEND_MESSAGE', messageInput.value.trim(), messageButton);
                messageInput.value = '';
            });

            detailHost.appendChild(el('section', {className: 'card'}, [
                el('div', {className: 'container-head'}, [
                    el('h2', {text: player.name}),
                    el('span', {className: 'spacer'}),
                    el('span', {className: 'muted', text: player.uuid})
                ]),
                el('div', {className: 'stats'}, [
                    stat('Server', player.server),
                    stat('Welt', player.world),
                    stat('Position', player.x + ' / ' + player.y + ' / ' + player.z),
                    stat('Leben', Math.round(player.health) + ' / ' + Math.round(player.maxHealth)),
                    stat('Hunger', String(player.foodLevel)),
                    stat('Level', String(player.level)),
                    stat('Sichtweite', player.viewDistance + ' Chunks'),
                    stat('Zahlt', player.paying ? 'ja' : 'nein')
                ]),
                el('div', {className: 'actions wrap'}, [
                    actionButton('Heilen', 'HEAL', null),
                    actionButton('Sättigen', 'FEED', null),
                    actionButton('Zum Spawn', 'TELEPORT_TO_SPAWN', null),
                    actionButton('Inventar leeren', 'CLEAR_INVENTORY', null, 'danger'),
                    actionButton('Kicken', 'KICK', 'Vom Admin gekickt.', 'danger'),
                    actionButton(player.op ? 'OP entziehen' : 'OP geben', 'SET_OP',
                        player.op ? 'false' : 'true'),
                    gamemodeSelect
                ]),
                el('div', {className: 'inline-form'}, [
                    el('div', {className: 'field'}, [el('label', {text: 'Nachricht'}), messageInput]),
                    messageButton
                ]),
                actionResult
            ]));

            // the containers, one tab each
            var tabs = el('div', {className: 'tabs'});
            var containerHost = el('div');
            var sources = [
                {id: 'INVENTORY', label: 'Inventar', container: null},
                {id: 'ENDER_CHEST', label: 'Enderchest', container: null}
            ];
            (player.backpacks || []).forEach(function (backpack) {
                sources.push({id: 'BACKPACK', label: backpack.title, container: backpack.id});
            });

            function loadContainer(source, button) {
                Array.prototype.forEach.call(tabs.children, function (child) {
                    child.classList.remove('active');
                });
                button.classList.add('active');
                clear(containerHost);
                containerHost.appendChild(el('p', {className: 'muted', text: 'Lade ...'}));
                var url = '/api/players/' + encodeURIComponent(player.uuid) + '/inventory?kind=' + source.id
                    + (source.container ? '&container=' + encodeURIComponent(source.container) : '');
                api(url).then(function (data) {
                    renderContainer(containerHost, data, player.uuid);
                }).catch(function (error) {
                    clear(containerHost);
                    containerHost.appendChild(el('p', {className: 'message', text: error.message}));
                });
            }

            sources.forEach(function (source, index) {
                var button = el('button', {text: source.label, type: 'button'});
                button.addEventListener('click', function () {
                    loadContainer(source, button);
                });
                tabs.appendChild(button);
                if (index === 0) setTimeout(function () { loadContainer(source, button); }, 0);
            });

            if (!(player.backpacks || []).length) {
                tabs.appendChild(el('span', {className: 'hint inline',
                    text: 'Keine Backpacks - es ist kein Backpack-System installiert.'}));
            }

            var stashHost = el('div');
            detailHost.appendChild(el('section', {className: 'card'}, [tabs, containerHost]));
            detailHost.appendChild(el('section', {className: 'card'}, [stashHost]));
            detailHost.appendChild(buildSaveBar());
            renderStash(stashHost);
            detailHost.scrollIntoView({behavior: 'smooth', block: 'start'});
        }

        function stat(label, value) {
            return el('div', {className: 'stat'}, [
                el('span', {className: 'stat-label', text: label}),
                el('span', {className: 'stat-value', text: value})
            ]);
        }

        refresh();
        McAdmin.autoRefresh(refresh, 10);
    });

    /* ------------------------------------------------------------------ stash panel */

    McAdmin.registerPanel('stash', function (panel, module) {
        resetContainers();
        var stashHost = el('div');
        panel.appendChild(el('section', {className: 'card'}, [
            el('h2', {text: module.title}),
            el('p', {className: 'muted', text: module.description}),
            el('p', {className: 'hint',
                text: 'Items aus einem Spielerinventar landen hier, wenn du sie im Spieler-Panel '
                    + 'herüberziehst. Slots lassen sich auch hier per Drag & Drop umsortieren.'})
        ]));
        panel.appendChild(el('section', {className: 'card'}, [stashHost]));
        panel.appendChild(buildSaveBar());
        renderStash(stashHost);
    });

    /* ------------------------------------------------------------------ coreprotect panel */

    McAdmin.registerPanel('coreprotect', function (panel, module) {
        var serverSelect = el('select');
        var kindSelect = el('select');
        var userInput = el('input', {type: 'text', placeholder: 'Spielername (optional)'});
        var timeSelect = el('select');
        var worldInput = el('input', {type: 'text', placeholder: 'z.B. world (optional)'});
        var xInput = el('input', {type: 'number', placeholder: 'X'});
        var yInput = el('input', {type: 'number', placeholder: 'Y'});
        var zInput = el('input', {type: 'number', placeholder: 'Z'});
        var radiusInput = el('input', {type: 'number', placeholder: 'Radius', value: '10'});
        var limitInput = el('input', {type: 'number', placeholder: 'Limit', value: '100'});
        var searchButton = el('button', {text: 'Suchen', type: 'button'});
        var status = el('p', {className: 'muted', text: ''});
        var resultHost = el('div');

        [['1 Stunde', 3600], ['6 Stunden', 21600], ['1 Tag', 86400],
            ['3 Tage', 259200], ['1 Woche', 604800], ['30 Tage', 2592000]].forEach(function (entry) {
            var option = el('option', {text: entry[0]});
            option.value = String(entry[1]);
            timeSelect.appendChild(option);
        });
        timeSelect.value = '86400';

        api('/api/coreprotect').then(function (data) {
            (data.servers || []).forEach(function (name) {
                var option = el('option', {text: name});
                option.value = name;
                serverSelect.appendChild(option);
            });
            if (!(data.servers || []).length) {
                status.textContent = 'Es läuft gerade kein Paper-Server, der CoreProtect haben könnte.';
            }
            (data.kinds || []).forEach(function (kind) {
                var option = el('option', {text: kind.label});
                option.value = kind.id;
                kindSelect.appendChild(option);
            });
        }).catch(function (error) {
            status.textContent = error.message;
        });

        searchButton.addEventListener('click', function () {
            if (!serverSelect.value) {
                toast('Bitte einen Server wählen.', 'error');
                return;
            }
            var body = {
                server: serverSelect.value,
                kind: kindSelect.value,
                user: userInput.value.trim(),
                timeSeconds: parseInt(timeSelect.value, 10),
                limit: Math.max(1, parseInt(limitInput.value, 10) || 100)
            };
            if (worldInput.value.trim()) {
                body.world = worldInput.value.trim();
                body.x = parseInt(xInput.value, 10) || 0;
                body.y = parseInt(yInput.value, 10) || 0;
                body.z = parseInt(zInput.value, 10) || 0;
                body.radius = parseInt(radiusInput.value, 10) || 0;
            }
            searchButton.disabled = true;
            status.textContent = 'Suche ...';
            api('/api/coreprotect/lookup', {method: 'POST', body: body}).then(function (data) {
                renderResults(data.entries || []);
            }).catch(function (error) {
                status.textContent = error.message;
                clear(resultHost);
            }).finally(function () {
                searchButton.disabled = false;
            });
        });

        function renderResults(entries) {
            clear(resultHost);
            status.textContent = entries.length
                ? entries.length + ' Einträge gefunden.'
                : 'Nichts gefunden.';
            if (!entries.length) return;

            var table = el('table', {className: 'log-table'});
            var head = el('tr');
            ['Zeit', 'Spieler', 'Aktion', 'Objekt', 'Ort', 'Details'].forEach(function (title) {
                head.appendChild(el('th', {text: title}));
            });
            table.appendChild(el('thead', {}, [head]));

            var body = el('tbody');
            entries.forEach(function (entry) {
                var when = new Date(entry.timestamp);
                var row = el('tr');
                if (entry.rolledBack) row.classList.add('rolled-back');
                row.appendChild(el('td', {text: when.toLocaleString('de-DE')}));
                row.appendChild(el('td', {text: entry.player || '-'}));
                row.appendChild(el('td', {text: entry.action || '-'}));
                row.appendChild(el('td', {text: entry.target ? prettyMaterial(entry.target) : '-'}));
                row.appendChild(el('td', {
                    text: entry.world ? entry.world + ' ' + entry.x + '/' + entry.y + '/' + entry.z : '-'
                }));
                row.appendChild(el('td', {text: entry.detail || ''}));
                body.appendChild(row);
            });
            table.appendChild(body);
            resultHost.appendChild(el('div', {className: 'table-scroll'}, [table]));
        }

        panel.appendChild(el('section', {className: 'card'}, [
            el('h2', {text: module.title}),
            el('p', {className: 'muted', text: module.description}),
            el('div', {className: 'inline-form'}, [
                el('div', {className: 'field'}, [el('label', {text: 'Server'}), serverSelect]),
                el('div', {className: 'field wide'}, [el('label', {text: 'Art'}), kindSelect]),
                el('div', {className: 'field'}, [el('label', {text: 'Spieler'}), userInput]),
                el('div', {className: 'field'}, [el('label', {text: 'Zeitraum'}), timeSelect]),
                el('div', {className: 'field narrow'}, [el('label', {text: 'Limit'}), limitInput])
            ]),
            el('div', {className: 'inline-form'}, [
                el('div', {className: 'field'}, [el('label', {text: 'Welt'}), worldInput]),
                el('div', {className: 'field narrow'}, [el('label', {text: 'X'}), xInput]),
                el('div', {className: 'field narrow'}, [el('label', {text: 'Y'}), yInput]),
                el('div', {className: 'field narrow'}, [el('label', {text: 'Z'}), zInput]),
                el('div', {className: 'field narrow'}, [el('label', {text: 'Radius'}), radiusInput]),
                searchButton
            ]),
            el('p', {className: 'hint',
                text: 'Eine Block-Abfrage braucht eine Position. Ohne Welt sucht CoreProtect überall.'}),
            status
        ]));
        panel.appendChild(resultHost);
    });
})();
