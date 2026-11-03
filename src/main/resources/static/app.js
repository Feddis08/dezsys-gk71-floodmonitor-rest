/* Hochwasser-Webclient: jQuery, öffentliche GET-Endpunkte, JSON und XML. */
(function ($) {
  'use strict';
  const base = '/api/v1/stations';
  const fields = [
    ['stationId', 'Stations-ID'], ['timestamp', 'Zeitpunkt'],
    ['waterLevel', 'Wasserstand (cm)'], ['flowRate', 'Durchfluss (m³/s)'],
    ['rainfall', 'Niederschlag (mm/h)'], ['temperature', 'Temperatur (°C)'],
    ['batteryLevel', 'Batterie (%)'], ['status', 'Status'], ['warningLevel', 'Warnstufe']
  ];
  let stations = [], epoch = 0, pending = [], timer = null;
  const value = x => x === null || x === undefined || x === '' ? '–' : String(x);
  const error = (id, message) => $(id).text(message || '').prop('hidden', !message);

  function xmlObject(node) {
    if (node.getAttributeNS('http://www.w3.org/2001/XMLSchema-instance', 'nil') === 'true') return null;
    const children = Array.from(node.children);
    if (!children.length) return node.textContent.trim();
    const result = {};
    children.forEach(child => {
      const name = child.localName;
      const item = xmlObject(child);
      if (Object.prototype.hasOwnProperty.call(result, name)) {
        if (!Array.isArray(result[name])) result[name] = [result[name]];
        result[name].push(item);
      } else result[name] = item;
    });
    return result;
  }

  // Handle plural roots, Jackson List/item wrappers and nested wrappers.
  function listOf(data, kind) {
    if (data === null || data === undefined || data === '') return [];
    if (Array.isArray(data)) return data.flatMap(item => listOf(item, kind));
    if (typeof data !== 'object') throw new Error('Unerwartete Antwortstruktur.');
    const identifyingKey = kind === 'station' ? 'id' : 'timestamp';
    if (Object.prototype.hasOwnProperty.call(data, identifyingKey)) return [data];
    for (const key of [kind, kind + 's', 'item', 'items', 'List', 'list', 'ArrayList']) {
      if (Object.prototype.hasOwnProperty.call(data, key)) return listOf(data[key], kind);
    }
    const keys = Object.keys(data);
    if (!keys.length) return [];
    if (keys.length === 1) return listOf(data[keys[0]], kind);
    throw new Error('Unerwartete Antwortstruktur: ' + keys.join(', '));
  }

  function parse(body, format, kind) {
    if (!body || !body.trim()) return [];
    let data;
    if (format === 'xml') {
      const doc = $.parseXML(body);
      if (!doc || doc.getElementsByTagName('parsererror').length) throw new Error('Ungültiges XML.');
      data = xmlObject(doc.documentElement);
    } else data = JSON.parse(body);
    return listOf(data, kind);
  }

  function responseError(xhr) {
    let detail = '';
    const body = (xhr.responseText || '').trim();
    if (body) {
      try {
        const data = body.startsWith('<') ? xmlObject($.parseXML(body).documentElement) : JSON.parse(body);
        if (typeof data === 'object' && data) {
          detail = [data.title, data.detail || data.message || data.error].filter(x => typeof x === 'string').join(': ');
        } else if (typeof data === 'string') detail = data;
      } catch (_) { /* Plain text is useful; never render server markup. */ }
      if (!detail && !/^\s*<!doctype html|^\s*<html/i.test(body)) detail = body.slice(0, 400);
    }
    const status = xhr.status ? 'HTTP ' + xhr.status : 'Netzwerkfehler';
    if (!detail && xhr.status === 404) detail = 'Keine Daten für diese Station vorhanden.';
    if (!detail && xhr.status === 0) detail = 'Server nicht erreichbar. Verbindung und Serverstart prüfen.';
    return status + (detail ? ': ' + detail : ': Anfrage fehlgeschlagen.');
  }

  function invalidate() {
    ++epoch; // Invalidate before abort: abort callbacks run synchronously.
    const old = pending;
    pending = [];
    old.forEach(xhr => xhr.abort());
    $('#loading').prop('hidden', true);
    return epoch;
  }

  function request(url, params, kind, token, success, failure) {
    const format = $('#format').val();
    const xhr = $.ajax({ url: url, method: 'GET', data: params,
      headers: { Accept: format === 'xml' ? 'application/xml' : 'application/json' },
      dataType: 'text', timeout: 15000, cache: false });
    pending.push(xhr);
    $('#loading').prop('hidden', false);
    xhr.done(function (body) {
      if (token !== epoch) return;
      try { success(parse(body, format, kind)); }
      catch (e) { failure('Antwort kann nicht als ' + format.toUpperCase() + ' gelesen werden: ' + e.message); }
    }).fail(function (response, state) {
      if (token === epoch && state !== 'abort') failure(responseError(response));
    }).always(function () {
      if (token !== epoch) return;
      pending = pending.filter(item => item !== xhr);
      $('#loading').prop('hidden', pending.length === 0);
      if (!pending.length) $('#updated').text('Letzter Abruf beendet: ' + new Date().toLocaleString('de-AT') + ' (Fehler siehe oben).');
    });
  }

  function warningBadge(level) {
    const normalized = String(level || 'UNKNOWN').toUpperCase();
    const known = ['NORMAL', 'WARNING', 'CRITICAL', 'UNKNOWN'].includes(normalized);
    return $('<span>').addClass('badge ' + (known ? normalized.toLowerCase() : 'unknown')).text(known ? normalized : 'UNKNOWN (' + normalized + ')');
  }

  function displayed(field, item) {
    if (field === 'warningLevel') return warningBadge(item[field]);
    if (field === 'timestamp' && item[field]) {
      const date = new Date(item[field]);
      if (!Number.isNaN(date.getTime())) return date.toLocaleString('de-AT');
    }
    if (['waterLevel', 'flowRate', 'rainfall', 'temperature', 'batteryLevel'].includes(field) && item[field] !== null && item[field] !== '' && Number.isFinite(Number(item[field]))) {
      return Number(item[field]).toLocaleString('de-AT', { maximumFractionDigits: 2 });
    }
    return value(item[field]);
  }

  function renderLatest(items) {
    const panel = $('#latest').empty();
    if (!items.length) {
      panel.prop('hidden', true);
      $('#latest-message').text('Für diese Station ist noch keine Messung vorhanden.');
      return;
    }
    fields.forEach(([key, label]) => {
      panel.append($('<dt>').text(label));
      const cell = $('<dd>').attr('title', key === 'timestamp' ? value(items[0][key]) : '');
      const content = displayed(key, items[0]);
      if (key === 'warningLevel') cell.append(content); else cell.text(content);
      panel.append(cell);
    });
    panel.prop('hidden', false);
    $('#latest-message').text('');
  }

  function renderHistory(items) {
    const body = $('#history').empty();
    items.forEach(item => {
      const row = $('<tr>');
      fields.forEach(([key]) => {
        const cell = $('<td>').attr('title', key === 'timestamp' ? value(item[key]) : '');
        const content = displayed(key, item);
        if (key === 'warningLevel') cell.append(content); else cell.text(content);
        row.append(cell);
      });
      body.append(row);
    });
    $('#history-message').text(items.length ? items.length + ' Messung(en).' : 'Keine Messungen für die gewählten Filter vorhanden.');
  }

  function filterParams() {
    const params = {};
    for (const key of ['from', 'to']) {
      const raw = $('#' + key).val();
      if (raw) {
        const date = new Date(raw);
        if (Number.isNaN(date.getTime())) throw new Error('Bitte gültige Zeitangaben eingeben.');
        params[key] = date.toISOString();
      }
    }
    if (params.from && params.to && params.from > params.to) throw new Error('„Von“ darf nicht nach „Bis“ liegen.');
    const limit = Number($('#limit').val());
    if (!Number.isInteger(limit) || limit < 1 || limit > 1000) throw new Error('Maximale Zeilen: ganze Zahl von 1 bis 1000 eingeben.');
    params.limit = limit;
    if ($('#warning').val()) params.warningLevel = $('#warning').val();
    return params;
  }

  function stationDetails() {
    const station = stations.find(s => String(s.id) === $('#station').val());
    if (!station) return;
    const active = station.isActive === true || station.isActive === 'true';
    const location = typeof station.location === 'object' && station.location ? JSON.stringify(station.location) : value(station.location);
    $('#station-details').text('ID: ' + value(station.id) + ' · Name: ' + value(station.name) + ' · Fluss: ' + value(station.river) + ' · Aktiv: ' + (active ? 'Ja' : 'Nein') + ' · Standort: ' + location);
  }

  function refreshMeasurements() {
    const token = invalidate();
    error('#filter-error', '');
    error('#latest-error', '');
    error('#history-error', '');
    $('#latest').empty().prop('hidden', true);
    $('#history').empty();
    $('#latest-message').text('Noch keine Messung geladen.');
    $('#history-message').text('Noch keine Historie geladen.');
    const id = $('#station').val();
    if (!id) return;
    stationDetails();
    let params;
    try { params = filterParams(); }
    catch (e) { error('#filter-error', e.message); return; }
    const endpoint = base + '/' + encodeURIComponent(id) + '/measurements';
    $('#latest-message').text('Letzte Messung wird geladen …');
    $('#history-message').text('Historie wird geladen …');
    request(endpoint + '/latest', {}, 'measurement', token, renderLatest, message => {
      $('#latest-message').text('Keine letzte Messung verfügbar.'); error('#latest-error', message);
    });
    request(endpoint, params, 'measurement', token, renderHistory, message => {
      $('#history-message').text('Historie konnte nicht geladen werden.'); error('#history-error', message);
    });
  }

  function loadStations() {
    const token = invalidate();
    const selected = $('#station').val();
    error('#station-error', '');
    error('#latest-error', '');
    error('#history-error', '');
    $('#latest').empty().prop('hidden', true);
    $('#history').empty();
    $('#latest-message').text('Warte auf Stationsliste …');
    $('#history-message').text('Warte auf Stationsliste …');
    $('#station-message').text('Stationen werden geladen …');
    $('#station').prop('disabled', true);
    request(base, {}, 'station', token, items => {
      stations = items;
      const select = $('#station').empty();
      items.forEach(station => select.append($('<option>').val(String(station.id)).text(value(station.name) + ' — ' + value(station.river) + ' (ID ' + value(station.id) + ')')));
      if (items.length) {
        if (items.some(station => String(station.id) === selected)) select.val(selected);
        select.prop('disabled', false);
        $('#station-message').text(items.length + ' Station(en).');
        // Start a new generation. The completed station request cannot hide its loading state.
        refreshMeasurements();
      } else {
        select.append($('<option>').val('').text('Keine Stationen vorhanden'));
        $('#station-details').text('Keine Station ausgewählt.');
        $('#station-message').text('Die API liefert noch keine Stationen. „Jetzt aktualisieren“ versucht es erneut.');
        $('#latest').empty().prop('hidden', true); $('#history').empty();
        $('#latest-message').text('Ohne Station ist keine Messung verfügbar.');
        $('#history-message').text('Ohne Station ist keine Historie verfügbar.');
      }
    }, message => {
      stations = [];
      $('#station').empty().append($('<option>').val('').text('Stationen nicht verfügbar'));
      $('#station-details').text('Keine Station ausgewählt.');
      $('#station-message').text('„Jetzt aktualisieren“ versucht den Abruf erneut.');
      $('#latest').empty().prop('hidden', true); $('#history').empty();
      $('#latest-message').text('Ohne Station ist keine Messung verfügbar.');
      $('#history-message').text('Ohne Station ist keine Historie verfügbar.');
      error('#station-error', message);
    });
  }

  function restartTimer() {
    clearInterval(timer);
    timer = null;
    error('#settings-error', '');
    const seconds = Number($('#interval').val());
    if (!Number.isInteger(seconds) || seconds < 1 || seconds > 3600) {
      error('#settings-error', 'Intervall: ganze Zahl von 1 bis 3600 Sekunden eingeben.'); return;
    }
    if ($('#automatic').prop('checked')) timer = setInterval(function () {
      // Polling never overlaps a running request; user changes abort and supersede it.
      if (!pending.length) {
        if (stations.length) refreshMeasurements(); else loadStations();
      }
    }, seconds * 1000);
  }

  $(function () {
    $('#timezone').text('Zeitzone der Eingaben und Anzeige: ' + Intl.DateTimeFormat().resolvedOptions().timeZone + '. Die ursprünglichen ISO-Zeitpunkte werden beim Darüberfahren angezeigt.');
    $('#station').on('change', refreshMeasurements);
    $('#format').on('change', loadStations);
    $('#filters').on('submit', function (event) { event.preventDefault(); refreshMeasurements(); });
    $('#from, #to, #warning, #limit').on('change', refreshMeasurements);
    // Invalidate immediately while typing, not only when an input loses focus.
    $('#from, #to, #limit').on('input', function () {
      invalidate();
      $('#latest').empty().prop('hidden', true); $('#history').empty();
      $('#latest-message').text('Filter geändert. Bitte Filter anwenden.');
      $('#history-message').text('Filter geändert. Bitte Filter anwenden.');
    });
    $('#refresh').on('click', function () { if (stations.length) refreshMeasurements(); else loadStations(); });
    $('#automatic, #interval').on('change', restartTimer);
    loadStations();
    restartTimer();
  });
})(jQuery);
