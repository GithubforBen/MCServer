/*
 * The whitelist: the rules players accept on the public page, the switches, and who is on it that way.
 */
(function () {
    'use strict';

    var api = McAdmin.api;
    var el = McAdmin.el;
    var clear = McAdmin.clear;
    var toast = McAdmin.toast;

    function when(millis) {
        return new Date(millis).toLocaleString('de-DE', {
            day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
        });
    }

    function checkbox(text, checked) {
        var input = el('input', {type: 'checkbox'});
        input.checked = checked;
        return {input: input, label: el('label', {className: 'checkbox'}, [input, document.createTextNode(' ' + text)])};
    }

    McAdmin.registerPanel('whitelist', function (panel, module) {
        var state = el('p', {className: 'message hidden'});
        var link = el('a', {className: 'secret'});
        link.target = '_blank';
        link.rel = 'noopener';
        var enforced = checkbox('Whitelist durchsetzen - nur wer darauf steht, kommt auf die Server', false);
        var selfService = checkbox('Spieler dürfen sich über die Regeln-Seite selbst eintragen', true);
        var url = el('input', {type: 'text'});
        var rules = el('textarea');
        rules.rows = 10;
        var save = el('button', {text: 'Speichern', type: 'button'});
        var count = el('p', {className: 'muted'});
        var rows = el('div', {className: 'rows'});
        var admins = el('p', {className: 'hint'});
        var loaded = null;

        panel.appendChild(el('section', {className: 'card'}, [
            el('h2', {text: module.title}),
            el('p', {className: 'muted', text: module.description}),
            state,
            el('label', {text: 'Regeln-Seite für Spieler'}),
            link
        ]));
        panel.appendChild(el('section', {className: 'card'}, [
            el('h3', {text: 'Einstellungen'}),
            enforced.label,
            selfService.label,
            el('div', {className: 'inline-form'}, [
                el('div', {className: 'field wide'}, [el('label', {text: 'Öffentlicher Link (leer = automatisch)'}), url])
            ]),
            el('p', {className: 'hint', text: 'Der Link steht in der Nachricht, die ein Spieler bekommt, wenn er nicht auf '
                    + 'der Whitelist steht. Läuft die Website hinter einer Domain, hier die Adresse eintragen.'}),
            el('div', {className: 'field'}, [el('label', {text: 'Regeln'}), rules]),
            el('p', {className: 'hint', text: 'Wer die Regeln vor einer Änderung akzeptiert hat, bleibt auf der Whitelist '
                    + 'und wird in der Liste mit "alte Regeln" markiert.'}),
            el('div', {className: 'actions'}, [save])
        ]));
        panel.appendChild(el('section', {className: 'card'}, [
            el('h3', {text: 'Selbst eingetragen'}),
            count,
            rows,
            admins
        ]));

        save.addEventListener('click', function () {
            // switching it on sends everybody away who is not on the list, so that takes a second click
            if (enforced.input.checked && loaded && !loaded.enforced && save.dataset.armed !== 'yes') {
                save.dataset.armed = 'yes';
                save.textContent = 'Wirklich durchsetzen?';
                setTimeout(function () {
                    save.dataset.armed = '';
                    save.textContent = 'Speichern';
                }, 4000);
                return;
            }
            save.dataset.armed = '';
            save.textContent = 'Speichern';
            save.disabled = true;
            api('/api/whitelist/settings', {method: 'POST', body: {
                enforced: enforced.input.checked,
                selfService: selfService.input.checked,
                publicUrl: url.value.trim(),
                rules: rules.value
            }}).then(function (data) {
                toast(data.message, 'ok');
                refresh();
            }).catch(function (error) {
                toast(error.message, 'error');
            }).finally(function () {
                save.disabled = false;
            });
        });

        function refresh() {
            api('/api/whitelist').then(function (data) {
                loaded = data;
                state.textContent = data.enforced
                    ? 'Die Whitelist ist aktiv: auf die Server kommt nur, wer darauf steht (und die Ops).'
                    : 'Die Whitelist ist aus: jeder kann joinen. Eintragen geht trotzdem schon, damit die Liste '
                        + 'voll ist, bevor sie eingeschaltet wird.';
                state.className = 'message ' + (data.enforced ? 'ok' : 'wait');
                link.textContent = data.rulesUrl;
                link.href = data.rulesUrl;
                enforced.input.checked = data.enforced;
                selfService.input.checked = data.selfService;
                url.value = data.publicUrl;
                url.placeholder = data.rulesUrl;
                rules.value = data.rules;
                drawPlayers(data);
            }).catch(function (error) {
                toast(error.message, 'error');
            });
        }

        function drawPlayers(data) {
            var players = data.players || [];
            count.textContent = players.length
                ? players.length + ' Spieler haben die Regeln akzeptiert.'
                : 'Noch hat sich niemand selbst eingetragen.';
            admins.textContent = data.adminNames.length
                ? 'Außerdem von Admins eingetragen (unter Einstellungen): ' + data.adminNames.join(', ')
                : '';
            clear(rows);
            players.forEach(function (player) {
                var remove = el('button', {text: 'Entfernen', type: 'button', className: 'small danger'});
                remove.addEventListener('click', function () {
                    if (remove.dataset.armed !== 'yes') {
                        remove.dataset.armed = 'yes';
                        remove.textContent = 'Wirklich?';
                        return;
                    }
                    remove.disabled = true;
                    api('/api/whitelist/' + encodeURIComponent(player.uuid), {method: 'DELETE'}).then(function (result) {
                        toast(result.message, 'ok');
                        refresh();
                    }).catch(function (error) {
                        toast(error.message, 'error');
                        remove.disabled = false;
                    });
                });
                rows.appendChild(el('div', {className: 'row'}, [
                    el('div', {className: 'grow'}, [
                        el('div', {className: 'name', text: player.name}),
                        el('div', {className: 'meta', text: 'akzeptiert am ' + when(player.acceptedAt) + ' · ' + player.uuid})
                    ]),
                    player.currentRules ? null : el('span', {className: 'badge', text: 'alte Regeln'}),
                    el('div', {className: 'actions'}, [remove])
                ]));
            });
        }

        refresh();
    });
})();
