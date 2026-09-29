/*
 * The public rules page. It shares the styles of the admin interface, but none of its script: whoever opens
 * it is a player, not an admin.
 */
(function () {
    'use strict';

    function $(id) {
        return document.getElementById(id);
    }

    function show(text, kind) {
        var message = $('rules-message');
        message.textContent = text;
        message.className = 'message ' + (kind || '');
    }

    function request(path, body) {
        var init = {credentials: 'omit'};
        if (body) {
            init.method = 'POST';
            init.headers = {'Content-Type': 'application/json'};
            init.body = JSON.stringify(body);
        }
        return fetch(path, init).then(function (response) {
            return response.json().catch(function () {
                return {};
            }).then(function (data) {
                if (!response.ok) throw new Error(data.error || ('Fehler ' + response.status));
                return data;
            });
        });
    }

    function boot() {
        var form = $('rules-form');
        var submit = $('rules-submit');

        request('/api/public/rules').then(function (data) {
            $('rules-brand').textContent = 'Regeln - ' + data.brand;
            document.title = 'Regeln - ' + data.brand;
            $('rules-text').textContent = data.rules;
            if (!data.selfService) {
                submit.disabled = true;
                show('Selbst eintragen ist gerade ausgeschaltet. Bitte wende dich an einen Admin.', 'wait');
            }
        }).catch(function (error) {
            $('rules-text').textContent = '';
            show(error.message);
        });

        form.addEventListener('submit', function (event) {
            event.preventDefault();
            if (!$('rules-accepted').checked) {
                show('Bitte zuerst die Regeln akzeptieren.');
                return;
            }
            submit.disabled = true;
            show('Prüfe den Namen bei Mojang ...', 'wait');
            request('/api/public/whitelist', {name: $('rules-name').value.trim(), accepted: true})
                .then(function (data) {
                    show(data.message, 'ok');
                    $('rules-name').value = '';
                }).catch(function (error) {
                    show(error.message);
                }).finally(function () {
                    submit.disabled = false;
                });
        });
    }

    document.addEventListener('DOMContentLoaded', boot);
})();
