/*
 * The item editor, the one way items are made on this website.
 *
 * Event prizes, a slot in a player's inventory and the admin stash all build their items with it, so an item
 * means the same thing wherever it is made: a material, an amount, and optionally a name, lore,
 * enchantments, attribute modifiers, unbreakable and wear. What it works on is the json form of an ItemSpec
 * on the server.
 *
 * Every name - material, enchantment, attribute - is picked from a search field with suggestions. They come
 * from the catalog a game server reported, which the launcher keeps, so they work while no server runs too.
 * Without any catalog the fields still take a name typed out.
 */
(function () {
    'use strict';

    var api = McAdmin.api;
    var el = McAdmin.el;
    var clear = McAdmin.clear;

    /* ------------------------------------------------------------------ names */

    /**
     * The german names of the vanilla enchantments. The key is always shown next to it, so an enchantment
     * this list does not know - or a newer one - is still recognisable.
     */
    var ENCHANTMENT_NAMES = {
        protection: 'Schutz', fire_protection: 'Feuerschutz', feather_falling: 'Federfall',
        blast_protection: 'Explosionsschutz', projectile_protection: 'Schusssicher', respiration: 'Atmung',
        aqua_affinity: 'Wasseraffinität', thorns: 'Dornen', depth_strider: 'Wasserläufer',
        frost_walker: 'Eisläufer', binding_curse: 'Fluch der Bindung', soul_speed: 'Seelenläufer',
        swift_sneak: 'Huschen', sharpness: 'Schärfe', smite: 'Bann', bane_of_arthropods: 'Nemesis der Gliederfüßer',
        knockback: 'Rückstoß', fire_aspect: 'Verbrennung', looting: 'Plünderung', sweeping_edge: 'Schwungkraft',
        efficiency: 'Effizienz', silk_touch: 'Behutsamkeit', unbreaking: 'Haltbarkeit', fortune: 'Glück',
        power: 'Stärke', punch: 'Schlag', flame: 'Flamme', infinity: 'Unendlichkeit',
        luck_of_the_sea: 'Glück des Meeres', lure: 'Köder', loyalty: 'Treue', impaling: 'Harpune',
        riptide: 'Sog', channeling: 'Entladung', multishot: 'Mehrfachschuss', quick_charge: 'Schnellladen',
        piercing: 'Durchschuss', mending: 'Reparatur', vanishing_curse: 'Fluch des Verschwindens',
        density: 'Dichte', breach: 'Durchbruch', wind_burst: 'Windstoß'
    };

    /** Descriptive german labels for the attributes, again shown next to the key. */
    var ATTRIBUTE_NAMES = {
        attack_damage: 'Angriffsschaden', attack_speed: 'Angriffsgeschwindigkeit', attack_knockback: 'Angriffsrückstoß',
        armor: 'Rüstung', armor_toughness: 'Rüstungshärte', max_health: 'Maximale Gesundheit',
        max_absorption: 'Maximale Absorption', movement_speed: 'Bewegungsgeschwindigkeit',
        knockback_resistance: 'Rückstoßresistenz', luck: 'Glück', scale: 'Größe', step_height: 'Schritthöhe',
        jump_strength: 'Sprungkraft', gravity: 'Schwerkraft', safe_fall_distance: 'Sichere Fallhöhe',
        fall_damage_multiplier: 'Fallschaden-Faktor', block_interaction_range: 'Reichweite für Blöcke',
        entity_interaction_range: 'Reichweite für Kreaturen', block_break_speed: 'Abbaugeschwindigkeit',
        mining_efficiency: 'Abbau-Effizienz', submerged_mining_speed: 'Abbaugeschwindigkeit unter Wasser',
        sneaking_speed: 'Schleichgeschwindigkeit', sweeping_damage_ratio: 'Anteil des Schwungschadens',
        oxygen_bonus: 'Sauerstoff-Bonus', water_movement_efficiency: 'Bewegung im Wasser',
        movement_efficiency: 'Bewegung auf schwerem Grund', burning_time: 'Brenndauer',
        explosion_knockback_resistance: 'Explosions-Rückstoßresistenz', follow_range: 'Verfolgungsreichweite',
        flying_speed: 'Fluggeschwindigkeit', camera_distance: 'Kameraabstand', waypoint_receive_range:
            'Wegpunkt-Empfangsreichweite', waypoint_transmit_range: 'Wegpunkt-Sendereichweite'
    };

    var OPERATIONS = [
        {value: 'ADD_NUMBER', label: '+ Wert'},
        {value: 'ADD_SCALAR', label: '+ Anteil vom Grundwert'},
        {value: 'MULTIPLY_SCALAR_1', label: '× (1 + Wert)'}
    ];

    var SLOTS = [
        {value: 'any', label: 'Überall'},
        {value: 'mainhand', label: 'Haupthand'},
        {value: 'offhand', label: 'Nebenhand'},
        {value: 'hand', label: 'Eine Hand'},
        {value: 'head', label: 'Kopf'},
        {value: 'chest', label: 'Brust'},
        {value: 'legs', label: 'Beine'},
        {value: 'feet', label: 'Füße'},
        {value: 'armor', label: 'Rüstung (alle)'},
        {value: 'body', label: 'Körper (Tiere)'}
    ];

    /** The limits the server checks, so the form can say so before a save is refused. */
    var LIMITS = {name: 120, loreLines: 16, loreLine: 120, enchantLevel: 255, attributeAmount: 2048};

    var ROMAN = ['', 'I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X'];

    function shortKey(key) {
        key = String(key || '');
        return key.indexOf('minecraft:') === 0 ? key.slice('minecraft:'.length) : key;
    }

    /**
     * MATERIAL_NAMES and key_names as readable words.
     */
    function pretty(name) {
        if (!name) return '';
        return shortKey(name).toLowerCase().split(/[_.]/).map(function (part) {
            return part.charAt(0).toUpperCase() + part.slice(1);
        }).join(' ');
    }

    function enchantmentName(key) {
        return ENCHANTMENT_NAMES[shortKey(key)] || pretty(key);
    }

    function attributeName(key) {
        return ATTRIBUTE_NAMES[shortKey(key)] || pretty(key);
    }

    function level(n) {
        return ROMAN[n] || String(n);
    }

    /** A material as somebody typed it, the way bukkit spells it. */
    function normaliseMaterial(text) {
        var value = String(text || '').trim().toUpperCase().replace(/\s+/g, '_');
        if (value.indexOf('MINECRAFT:') === 0) value = value.slice('MINECRAFT:'.length);
        return value;
    }

    /** A registry key as somebody typed it, with its namespace. */
    function normaliseKey(text) {
        var value = String(text || '').trim().toLowerCase().replace(/\s+/g, '_');
        if (!value) return '';
        return value.indexOf(':') >= 0 ? value : 'minecraft:' + value;
    }

    function stripCodes(text) {
        return String(text || '').replace(/[&§]#[0-9a-fA-F]{6}/g, '').replace(/[&§][0-9a-fk-orA-FK-OR]/g, '');
    }

    /* ------------------------------------------------------------------ the catalog */

    var catalogPromise = null;

    /**
     * What items can be made of, asked for once per page. Never rejects: without a catalog the editor
     * simply has no suggestions.
     */
    function catalog() {
        if (catalogPromise) return catalogPromise;
        catalogPromise = api('/api/items/catalog').then(function (data) {
            var materials = data.materials || [];
            var byName = {};
            materials.forEach(function (material) {
                byName[material.name] = material;
            });
            var enchantments = data.enchantments || [];
            var enchantmentByKey = {};
            enchantments.forEach(function (enchantment) {
                enchantmentByKey[enchantment.key] = enchantment;
            });
            return {
                source: data.source || 'none',
                materials: materials,
                material: function (name) {
                    return byName[name] || null;
                },
                enchantments: enchantments,
                enchantment: function (key) {
                    return enchantmentByKey[key] || null;
                },
                attributes: data.attributes || []
            };
        }).catch(function () {
            // asked again next time, the network may be back by then
            catalogPromise = null;
            return {
                source: 'none', materials: [], enchantments: [], attributes: [],
                material: function () { return null; },
                enchantment: function () { return null; }
            };
        });
        return catalogPromise;
    }

    /* ------------------------------------------------------------------ search with suggestions */

    /** Makes text comparable: lower case, spaces and underscores alike, no namespace. */
    function fold(text) {
        return shortKey(String(text || '').toLowerCase()).replace(/[_\s.]+/g, ' ').trim();
    }

    /**
     * How well an entry fits what was typed, lower is better, -1 for not at all. Every word typed has to
     * appear somewhere; an entry that starts with it beats one that only contains it.
     */
    function rank(entry, words) {
        if (!words.length) return 2;
        var haystacks = [fold(entry.value), fold(entry.label)];
        var best = -1;
        haystacks.forEach(function (hay) {
            if (!hay) return;
            var all = words.every(function (word) {
                return hay.indexOf(word) >= 0;
            });
            if (!all) return;
            var joined = words.join(' ');
            var score;
            if (hay === joined) score = 0;
            else if (hay.indexOf(joined) === 0) score = 1;
            else if ((' ' + hay).indexOf(' ' + words[0]) >= 0) score = 2;
            else score = 3;
            if (best < 0 || score < best) best = score;
        });
        return best;
    }

    var comboCounter = 0;

    /**
     * A text field that suggests while typing - by name in german or by its minecraft key, whichever
     * somebody knows. Arrow keys move through the list, enter takes one, escape closes it. What is typed
     * stays valid input even when it matches no suggestion, so a name the catalog does not know can still
     * be entered.
     *
     * @param options.value       what it starts with
     * @param options.placeholder what it says while empty
     * @param options.entries     function returning [{value, label, hint, preferred}] - asked on every key,
     *                            so it can depend on other fields
     * @param options.onChange    called with the value whenever it changes
     * @param options.display     optional function turning a value into what the field shows
     * @param options.parse       optional function turning what was typed into a value
     */
    function combo(options) {
        var id = 'combo-' + (++comboCounter);
        var input = el('input', {type: 'text', placeholder: options.placeholder || ''});
        input.setAttribute('role', 'combobox');
        input.setAttribute('aria-autocomplete', 'list');
        input.setAttribute('aria-expanded', 'false');
        input.setAttribute('aria-controls', id);
        input.autocomplete = 'off';
        input.spellcheck = false;
        var list = el('ul', {className: 'combo-list hidden', id: id});
        list.setAttribute('role', 'listbox');
        var wrap = el('div', {className: 'combo'}, [input, list]);

        var value = options.value || '';
        var display = options.display || function (v) { return v; };
        var parse = options.parse || function (text) { return text.trim(); };
        input.value = display(value);

        var shown = [];
        var active = -1;
        var typed = false;

        function set(next, fromTyping) {
            value = next;
            if (!fromTyping) input.value = display(value);
            if (options.onChange) options.onChange(value);
        }

        function close() {
            list.classList.add('hidden');
            input.setAttribute('aria-expanded', 'false');
            active = -1;
        }

        /** The real options only, without the divider rows. */
        function optionNodes() {
            return Array.prototype.filter.call(list.children, function (node) {
                return node.classList.contains('combo-option');
            });
        }

        function highlight(index) {
            var nodes = optionNodes();
            nodes.forEach(function (node, i) {
                node.classList.toggle('active', i === index);
                node.setAttribute('aria-selected', i === index ? 'true' : 'false');
            });
            active = index;
            if (index >= 0 && nodes[index]) {
                nodes[index].scrollIntoView({block: 'nearest'});
                input.setAttribute('aria-activedescendant', nodes[index].id);
            } else {
                input.removeAttribute('aria-activedescendant');
            }
        }

        function open() {
            // right after focusing, everything is offered - the filter starts with the first key pressed
            var words = typed ? fold(input.value).split(' ').filter(Boolean) : [];
            var scored = [];
            (options.entries() || []).forEach(function (entry) {
                var score = rank(entry, words);
                if (score < 0) return;
                scored.push({entry: entry, score: score + (entry.preferred ? 0 : 10)});
            });
            scored.sort(function (a, b) {
                return a.score - b.score || String(a.entry.label).localeCompare(String(b.entry.label), 'de');
            });
            shown = scored.slice(0, 60).map(function (item) {
                return item.entry;
            });
            clear(list);
            if (!shown.length) {
                close();
                return;
            }
            var lastPreferred = null;
            shown.forEach(function (entry, index) {
                if (lastPreferred === true && !entry.preferred) {
                    list.appendChild(el('li', {className: 'combo-divider', text: 'Weitere'}));
                }
                lastPreferred = !!entry.preferred;
                var option = el('li', {className: 'combo-option', id: id + '-' + index}, [
                    el('span', {className: 'combo-label', text: entry.label}),
                    entry.hint ? el('span', {className: 'combo-hint', text: entry.hint}) : null
                ]);
                option.setAttribute('role', 'option');
                // mousedown rather than click: a click comes after the blur that closes the list
                option.addEventListener('mousedown', function (event) {
                    event.preventDefault();
                    choose(index);
                });
                list.appendChild(option);
            });
            list.classList.remove('hidden');
            input.setAttribute('aria-expanded', 'true');
            highlight(words.length ? 0 : -1);
        }

        function choose(index) {
            var entry = shown[index];
            if (!entry) return;
            set(entry.value, false);
            close();
        }

        input.addEventListener('input', function () {
            typed = true;
            set(parse(input.value), true);
            open();
        });
        input.addEventListener('focus', function () {
            typed = false;
            input.select();
            open();
        });
        input.addEventListener('blur', function () {
            close();
            // what was typed is shown the way it will be stored
            input.value = display(value);
        });
        input.addEventListener('keydown', function (event) {
            var open_ = !list.classList.contains('hidden');
            if (event.key === 'ArrowDown') {
                event.preventDefault();
                if (!open_) open();
                else highlight(Math.min(optionNodes().length - 1, active + 1));
            } else if (event.key === 'ArrowUp') {
                event.preventDefault();
                if (open_) highlight(Math.max(0, active - 1));
            } else if (event.key === 'Enter') {
                if (open_ && active >= 0) {
                    event.preventDefault();
                    choose(active);
                }
            } else if (event.key === 'Escape') {
                if (open_) {
                    event.preventDefault();
                    event.stopPropagation();
                    close();
                }
            }
        });

        return {
            node: wrap,
            input: input,
            value: function () {
                return value;
            },
            set: function (next) {
                value = next;
                input.value = display(value);
            }
        };
    }

    /**
     * A search field for a material, with the materials of the catalog as suggestions.
     *
     * @param value    what it starts with
     * @param onChange called with the material name
     * @param options  optional: {blocksOnly: true} suggests only what can be placed
     */
    function materialInput(value, onChange, options) {
        options = options || {};
        var known = [];
        catalog().then(function (data) {
            known = data.materials.filter(function (material) {
                return !options.blocksOnly || material.block;
            }).map(function (material) {
                var hint = [material.block ? 'Block' : 'Item'];
                if (material.maxDurability) hint.push(material.maxDurability + ' Haltbarkeit');
                else if (material.maxStack !== 64) hint.push('stapelt bis ' + material.maxStack);
                return {
                    value: material.name,
                    label: pretty(material.name),
                    hint: material.name + ' · ' + hint.join(' · '),
                    preferred: true
                };
            });
        });
        return combo({
            value: value,
            placeholder: options.placeholder || 'Suchen, z.B. Diamant­schwert oder DIAMOND_SWORD',
            entries: function () {
                return known;
            },
            parse: normaliseMaterial,
            onChange: onChange
        });
    }

    /* ------------------------------------------------------------------ describing */

    function isPlain(spec) {
        return !spec.name && !(spec.lore || []).length && !(spec.enchantments || []).length
            && !(spec.attributes || []).length && !spec.unbreakable && !spec.damage;
    }

    /** Two items that are the same apart from their amount. */
    function sameItem(a, b) {
        function key(spec) {
            if (!spec) return '';
            var copy = clean(spec);
            copy.amount = 0;
            return JSON.stringify(copy);
        }
        return key(a) === key(b);
    }

    /**
     * An item reduced to what the server stores: names normalised, empty rows and defaults dropped, in a
     * fixed key order so two descriptions of the same item compare equal.
     */
    function clean(spec) {
        spec = spec || {};
        var out = {material: normaliseMaterial(spec.material), amount: parseInt(spec.amount, 10) || 0};
        var name = spec.name ? String(spec.name) : '';
        if (name.trim()) out.name = name;
        var lore = (spec.lore || []).map(function (line) {
            return String(line);
        });
        while (lore.length && !lore[lore.length - 1].trim()) lore.pop();
        if (lore.length) out.lore = lore;
        var enchantments = (spec.enchantments || []).filter(function (enchantment) {
            return enchantment.key && String(enchantment.key).trim();
        }).map(function (enchantment) {
            return {key: normaliseKey(enchantment.key), level: parseInt(enchantment.level, 10) || 0};
        });
        if (enchantments.length) out.enchantments = enchantments;
        var attributes = (spec.attributes || []).filter(function (attribute) {
            return attribute.attribute && String(attribute.attribute).trim();
        }).map(function (attribute) {
            var amount = typeof attribute.amount === 'number' ? attribute.amount : parseFloat(attribute.amount);
            return {
                attribute: normaliseKey(attribute.attribute),
                amount: isFinite(amount) ? amount : 0,
                operation: attribute.operation || 'ADD_NUMBER',
                slot: attribute.slot || 'any'
            };
        });
        if (attributes.length) out.attributes = attributes;
        if (spec.unbreakable) out.unbreakable = true;
        var damage = parseInt(spec.damage, 10) || 0;
        if (damage > 0) out.damage = damage;
        return out;
    }

    /**
     * @return the item in one line: "3x Diamond Sword „Excalibur“ · Schärfe V, Haltbarkeit III · unzerstörbar"
     */
    function describe(spec) {
        if (!spec || !spec.material) return 'Kein Item';
        var text = (spec.amount || 1) + 'x ' + pretty(spec.material);
        if (spec.name) text += ' „' + stripCodes(spec.name) + '“';
        var extras = [];
        var enchants = (spec.enchantments || []).map(function (enchantment) {
            return enchantmentName(enchantment.key) + ' ' + level(enchantment.level);
        });
        if (enchants.length) extras.push(enchants.join(', '));
        var attributes = (spec.attributes || []).map(function (attribute) {
            var amount = Number(attribute.amount);
            var sign = amount >= 0 ? '+' : '';
            var suffix = attribute.operation && attribute.operation !== 'ADD_NUMBER' ? '×' : '';
            return attributeName(attribute.attribute) + ' ' + sign + amount + suffix;
        });
        if (attributes.length) extras.push(attributes.join(', '));
        if (spec.unbreakable) extras.push('unzerstörbar');
        if (spec.damage) extras.push(spec.damage + ' Verschleiß');
        var loreLines = (spec.lore || []).length;
        if (loreLines) extras.push(loreLines + (loreLines === 1 ? ' Zeile Text' : ' Zeilen Text'));
        return extras.length ? text + ' · ' + extras.join(' · ') : text;
    }

    /* ------------------------------------------------------------------ minecraft text preview */

    var COLOURS = {
        '0': '#000000', '1': '#0000AA', '2': '#00AA00', '3': '#00AAAA', '4': '#AA0000', '5': '#AA00AA',
        '6': '#FFAA00', '7': '#AAAAAA', '8': '#555555', '9': '#5555FF', a: '#55FF55', b: '#55FFFF',
        c: '#FF5555', d: '#FF55FF', e: '#FFFF55', f: '#FFFFFF'
    };

    /**
     * Draws text with & colour codes the way the game will show it. These are the item's own colours - the
     * one place on the page where a hue is content rather than a state.
     */
    function renderText(host, text, base) {
        clear(host);
        var colour = base;
        var bold = false, italic = false, underline = false, strike = false;
        var pattern = /[&§](#[0-9a-fA-F]{6}|[0-9a-fk-orA-FK-OR])/g;
        var last = 0;
        var match;

        function flush(part) {
            if (!part) return;
            var span = el('span', {text: part});
            span.style.color = colour;
            if (bold) span.style.fontWeight = '700';
            if (italic) span.style.fontStyle = 'italic';
            var lines = [];
            if (underline) lines.push('underline');
            if (strike) lines.push('line-through');
            if (lines.length) span.style.textDecoration = lines.join(' ');
            host.appendChild(span);
        }

        while ((match = pattern.exec(text)) !== null) {
            flush(text.slice(last, match.index));
            var code = match[1].toLowerCase();
            if (code.charAt(0) === '#') {
                colour = code;
                bold = italic = underline = strike = false;
            } else if (COLOURS[code]) {
                colour = COLOURS[code];
                bold = italic = underline = strike = false;
            } else if (code === 'l') bold = true;
            else if (code === 'o') italic = true;
            else if (code === 'n') underline = true;
            else if (code === 'm') strike = true;
            else if (code === 'r') {
                colour = base;
                bold = italic = underline = strike = false;
            }
            last = pattern.lastIndex;
        }
        flush(text.slice(last));
    }

    /* ------------------------------------------------------------------ the editor */

    function field(labelText, input, className) {
        return el('div', {className: className ? 'field ' + className : 'field'}, [
            el('label', {text: labelText}), input
        ]);
    }

    function numberInput(value, min, max, step) {
        var input = el('input', {type: 'number', value: value === null || value === undefined ? '' : String(value)});
        if (min !== undefined && min !== null) input.min = String(min);
        if (max !== undefined && max !== null) input.max = String(max);
        if (step) input.step = String(step);
        return input;
    }

    function select(options, value) {
        var node = el('select');
        options.forEach(function (choice) {
            var option = el('option', {text: choice.label});
            option.value = choice.value;
            node.appendChild(option);
        });
        node.value = value;
        return node;
    }

    /**
     * The whole item editor.
     *
     * @param spec             the item to start from, json as the server sends it - it is copied, never changed
     * @param options.maxAmount the most of it there may be
     * @param options.onChange called with the cleaned item after every change
     * @param options.summary  false to leave out the one-line summary, when the caller shows one itself
     * @return {node, value(), problem()} - value() is the item as the server wants it, problem() what the
     *         server would refuse, or null
     */
    function editor(spec, options) {
        options = options || {};
        var maxAmount = options.maxAmount || 64;
        var state = JSON.parse(JSON.stringify(spec || {}));
        state.material = normaliseMaterial(state.material);
        state.amount = state.amount || 1;
        state.lore = state.lore || [];
        state.enchantments = state.enchantments || [];
        state.attributes = state.attributes || [];
        var known = null;

        var root = el('div', {className: 'item-editor'});
        var summary = el('p', {className: options.summary === false ? 'item-summary hidden' : 'item-summary'});

        function changed() {
            summary.textContent = describe(clean(state));
            refreshDurability();
            if (options.onChange) options.onChange(clean(state));
        }

        /* --- material and amount */
        var material = materialInput(state.material, function (value) {
            state.material = value;
            renderEnchantments();
            changed();
        });
        var amount = numberInput(state.amount, 1, maxAmount);
        amount.addEventListener('input', function () {
            state.amount = parseInt(amount.value, 10) || 0;
            changed();
        });
        var sourceNote = el('p', {className: 'hint hidden'});

        /* --- name and lore */
        var nameInput = el('input', {type: 'text', value: state.name || '', placeholder: 'Standardname'});
        nameInput.maxLength = LIMITS.name;
        var namePreview = el('div', {className: 'item-preview'});
        var loreInput = el('textarea', {placeholder: 'Eine Zeile pro Zeile, optional'});
        loreInput.rows = 3;
        loreInput.value = state.lore.join('\n');
        var lorePreview = el('div', {className: 'item-preview lore'});

        function refreshPreview() {
            namePreview.classList.toggle('hidden', !state.name);
            if (state.name) renderText(namePreview, state.name, '#FFFFFF');
            clear(lorePreview);
            lorePreview.classList.toggle('hidden', !state.lore.join('').trim());
            state.lore.forEach(function (line) {
                var row = el('div');
                renderText(row, line, '#AA00AA');
                lorePreview.appendChild(row);
            });
        }

        nameInput.addEventListener('input', function () {
            state.name = nameInput.value;
            refreshPreview();
            changed();
        });
        loreInput.addEventListener('input', function () {
            state.lore = loreInput.value.split('\n');
            refreshPreview();
            changed();
        });

        /* --- enchantments */
        var enchantHost = el('div', {className: 'stack'});

        function enchantmentEntries() {
            if (!known) return [];
            return known.enchantments.map(function (enchantment) {
                var fits = enchantment.materials.indexOf(state.material) >= 0;
                return {
                    value: enchantment.key,
                    label: enchantmentName(enchantment.key),
                    hint: shortKey(enchantment.key) + ' · bis ' + level(enchantment.maxLevel),
                    preferred: fits
                };
            });
        }

        function renderEnchantments() {
            clear(enchantHost);
            state.enchantments.forEach(function (enchantment, index) {
                var levelInput = numberInput(enchantment.level, 1, LIMITS.enchantLevel);
                var note = el('span', {className: 'hint'});

                function refreshNote() {
                    var info = known && known.enchantment(normaliseKey(enchantment.key));
                    var notes = [];
                    if (info && enchantment.level > info.maxLevel) notes.push('über dem normalen Maximum ' + level(info.maxLevel));
                    if (info && state.material && info.materials.length
                        && info.materials.indexOf(state.material) < 0) notes.push('wirkt auf diesem Item normal nicht');
                    if (enchantment.key && known && known.enchantments.length && !info) notes.push('unbekannt');
                    note.textContent = notes.join(' · ');
                }

                var key = combo({
                    value: enchantment.key,
                    placeholder: 'z.B. Schärfe oder sharpness',
                    entries: enchantmentEntries,
                    display: function (value) {
                        return value ? enchantmentName(value) + ' (' + shortKey(value) + ')' : '';
                    },
                    parse: function (text) {
                        // "Schärfe (sharpness)" back to its key, and a german name typed out as well
                        var inBrackets = /\(([^)]+)\)\s*$/.exec(text);
                        if (inBrackets) return normaliseKey(inBrackets[1]);
                        var folded = fold(text);
                        for (var k in ENCHANTMENT_NAMES) {
                            if (fold(ENCHANTMENT_NAMES[k]) === folded) return 'minecraft:' + k;
                        }
                        return normaliseKey(text);
                    },
                    onChange: function (value) {
                        // typing passes through half words first, so "fresh" means: no real enchantment yet
                        var fresh = !(known && known.enchantment(enchantment.key));
                        enchantment.key = value;
                        var info = known && known.enchantment(value);
                        // a freshly picked enchantment starts at its usual maximum, which is what people want
                        if (fresh && info && (!enchantment.level || enchantment.level === 1)) {
                            enchantment.level = info.maxLevel;
                            levelInput.value = String(info.maxLevel);
                        }
                        refreshNote();
                        changed();
                    }
                });
                levelInput.addEventListener('input', function () {
                    enchantment.level = parseInt(levelInput.value, 10) || 0;
                    refreshNote();
                    changed();
                });
                refreshNote();
                enchantHost.appendChild(el('div', {className: 'inline-form'}, [
                    field('Verzauberung', key.node, 'wide'),
                    field('Stufe', levelInput, 'narrow'),
                    el('button', {
                        text: 'Entfernen', type: 'button', className: 'small secondary',
                        onClick: function () {
                            state.enchantments.splice(index, 1);
                            renderEnchantments();
                            changed();
                        }
                    }),
                    note
                ]));
            });
        }

        var addEnchantment = el('button', {
            text: 'Verzauberung hinzufügen', type: 'button', className: 'small secondary',
            onClick: function () {
                state.enchantments.push({key: '', level: 1});
                renderEnchantments();
                var inputs = enchantHost.querySelectorAll('.combo input');
                if (inputs.length) inputs[inputs.length - 1].focus();
            }
        });

        /* --- attributes */
        var attributeHost = el('div', {className: 'stack'});

        function attributeEntries() {
            if (!known) return [];
            return known.attributes.map(function (key) {
                return {value: key, label: attributeName(key), hint: shortKey(key), preferred: true};
            });
        }

        function renderAttributes() {
            clear(attributeHost);
            state.attributes.forEach(function (attribute, index) {
                var key = combo({
                    value: attribute.attribute,
                    placeholder: 'z.B. Angriffsschaden',
                    entries: attributeEntries,
                    display: function (value) {
                        return value ? attributeName(value) + ' (' + shortKey(value) + ')' : '';
                    },
                    parse: function (text) {
                        var inBrackets = /\(([^)]+)\)\s*$/.exec(text);
                        if (inBrackets) return normaliseKey(inBrackets[1]);
                        var folded = fold(text);
                        for (var k in ATTRIBUTE_NAMES) {
                            if (fold(ATTRIBUTE_NAMES[k]) === folded) return 'minecraft:' + k;
                        }
                        return normaliseKey(text);
                    },
                    onChange: function (value) {
                        attribute.attribute = value;
                        changed();
                    }
                });
                var amountInput = numberInput(attribute.amount, -LIMITS.attributeAmount, LIMITS.attributeAmount, 'any');
                amountInput.addEventListener('input', function () {
                    attribute.amount = amountInput.value;
                    changed();
                });
                var operation = select(OPERATIONS, attribute.operation || 'ADD_NUMBER');
                operation.addEventListener('change', function () {
                    attribute.operation = operation.value;
                    changed();
                });
                var slot = select(SLOTS, attribute.slot || 'any');
                slot.addEventListener('change', function () {
                    attribute.slot = slot.value;
                    changed();
                });
                attributeHost.appendChild(el('div', {className: 'inline-form'}, [
                    field('Attribut', key.node, 'wide'),
                    field('Wert', amountInput, 'narrow'),
                    field('Rechnung', operation),
                    field('Wirkt in', slot),
                    el('button', {
                        text: 'Entfernen', type: 'button', className: 'small secondary',
                        onClick: function () {
                            state.attributes.splice(index, 1);
                            renderAttributes();
                            changed();
                        }
                    })
                ]));
            });
        }

        var addAttribute = el('button', {
            text: 'Attribut hinzufügen', type: 'button', className: 'small secondary',
            onClick: function () {
                state.attributes.push({attribute: '', amount: 1, operation: 'ADD_NUMBER', slot: 'any'});
                renderAttributes();
                var inputs = attributeHost.querySelectorAll('.combo input');
                if (inputs.length) inputs[inputs.length - 1].focus();
            }
        });

        /* --- unbreakable and wear */
        var unbreakable = el('input', {type: 'checkbox'});
        unbreakable.checked = !!state.unbreakable;
        unbreakable.addEventListener('change', function () {
            state.unbreakable = unbreakable.checked;
            changed();
        });
        var damageInput = numberInput(state.damage || 0, 0);
        var damageField = field('Verschleiß', damageInput, 'narrow');
        var damageNote = el('span', {className: 'hint'});
        damageInput.addEventListener('input', function () {
            state.damage = parseInt(damageInput.value, 10) || 0;
            changed();
        });

        function refreshDurability() {
            var info = known && known.material(state.material);
            var max = info ? info.maxDurability : 0;
            // without a catalog the field stays, there is no telling whether the item wears
            var wears = !known || !known.materials.length || max > 0;
            damageField.classList.toggle('hidden', !wears && !state.damage);
            damageInput.max = max ? String(max - 1) : '';
            damageNote.textContent = max ? '0 = neu, höchstens ' + (max - 1) + ' von ' + max : '';
        }

        /* --- layout */
        var hasExtras = !isPlain(clean(state));
        var details = el('details', {className: 'item-details'}, [
            el('summary', {text: 'Name, Verzauberungen, Attribute …'}),
            el('div', {className: 'stack'}, [
                el('div', {className: 'inline-form'}, [
                    field('Name', nameInput, 'wide'),
                    namePreview
                ]),
                el('p', {className: 'hint', text: 'Farben und Formatierung mit &: &6 Gold, &c Rot, &l fett, '
                    + '&#ff8800 eigene Farbe. Ohne Angabe ist der Text nicht kursiv.'}),
                field('Beschreibung (Lore)', loreInput),
                lorePreview,
                el('h4', {text: 'Verzauberungen'}),
                enchantHost,
                el('div', {className: 'actions'}, [addEnchantment]),
                el('h4', {text: 'Attribute'}),
                el('p', {className: 'hint', text: 'Sobald ein Item eigene Attribute hat, gelten die '
                    + 'Standardwerte des Materials nicht mehr - ein Schwert mit +2 Rüstung macht dann nur noch 1 Schaden. '
                    + 'Angriffsschaden also mit angeben.'}),
                attributeHost,
                el('div', {className: 'actions'}, [addAttribute]),
                el('div', {className: 'inline-form'}, [
                    el('div', {className: 'field'}, [
                        el('label', {className: 'checkbox'}, [unbreakable, document.createTextNode(' Unzerstörbar')])
                    ]),
                    damageField,
                    damageNote
                ])
            ])
        ]);
        details.open = hasExtras || !!options.open;

        root.appendChild(el('div', {className: 'inline-form'}, [
            field('Material', material.node, 'wide'),
            field('Anzahl', amount, 'narrow')
        ]));
        root.appendChild(sourceNote);
        root.appendChild(details);
        root.appendChild(summary);

        renderEnchantments();
        renderAttributes();
        refreshPreview();
        summary.textContent = describe(clean(state));

        catalog().then(function (data) {
            known = data;
            if (data.source === 'stored') {
                sourceNote.textContent = 'Vorschläge vom letzten Mal, als ein Spielserver lief.';
                sourceNote.classList.remove('hidden');
            } else if (data.source === 'none') {
                sourceNote.textContent = 'Noch keine Vorschläge - die kommen, sobald ein Spielserver läuft. '
                    + 'Namen lassen sich trotzdem eintippen (z.B. DIAMOND_SWORD, sharpness).';
                sourceNote.classList.remove('hidden');
            }
            renderEnchantments();
            refreshDurability();
        });
        refreshDurability();

        return {
            node: root,
            value: function () {
                return clean(state);
            },
            problem: function () {
                return problem(clean(state), maxAmount, known);
            },
            focus: function () {
                material.input.focus();
            }
        };
    }

    /**
     * What the server would refuse, said before anything is sent. The server checks again - this is only
     * so the admin hears it next to the field instead of after a round trip.
     */
    function problem(spec, maxAmount, known) {
        if (!spec.material) return 'Es fehlt das Material.';
        if (known && known.materials.length && !known.material(spec.material)) {
            return '„' + spec.material + '“ ist kein Minecraft-Item.';
        }
        if (spec.amount < 1 || spec.amount > maxAmount) return 'Die Anzahl muss zwischen 1 und ' + maxAmount + ' liegen.';
        if (spec.name && spec.name.length > LIMITS.name) return 'Der Name ist zu lang.';
        if ((spec.lore || []).length > LIMITS.loreLines) return 'Höchstens ' + LIMITS.loreLines + ' Zeilen Lore.';
        var tooLong = (spec.lore || []).some(function (line) {
            return line.length > LIMITS.loreLine;
        });
        if (tooLong) return 'Eine Lore-Zeile ist länger als ' + LIMITS.loreLine + ' Zeichen.';
        var badLevel = (spec.enchantments || []).filter(function (enchantment) {
            return enchantment.level < 1 || enchantment.level > LIMITS.enchantLevel;
        })[0];
        if (badLevel) return 'Die Stufe von ' + enchantmentName(badLevel.key) + ' muss zwischen 1 und 255 liegen.';
        var badAmount = (spec.attributes || []).filter(function (attribute) {
            return Math.abs(attribute.amount) > LIMITS.attributeAmount;
        })[0];
        if (badAmount) return 'Der Wert von ' + attributeName(badAmount.attribute) + ' ist zu groß.';
        return null;
    }

    window.McItems = {
        catalog: catalog,
        combo: combo,
        materialInput: materialInput,
        editor: editor,
        describe: describe,
        clean: clean,
        isPlain: isPlain,
        sameItem: sameItem,
        pretty: pretty,
        enchantmentName: enchantmentName
    };
})();
