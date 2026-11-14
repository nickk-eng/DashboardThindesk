(() => {
    'use strict';
    const root = document.getElementById('dashboard');
    if (!root) return;
    const form = document.getElementById('dashboard-filters');
    const message = document.getElementById('dashboard-message');
    const body = document.getElementById('dashboard-body');
    const refresh = document.getElementById('dashboard-refresh');
    const previous = document.getElementById('dashboard-prev');
    const next = document.getElementById('dashboard-next');
    const charts = {};
    const colors = ['#343a40', '#198754', '#0d6efd', '#ffc107', '#6f42c1', '#dc3545', '#6c757d'];
    const number = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 });
    let page = 0;
    let total = 0;
    let controller;
    let activeFilters = new URLSearchParams();
    const size = 20;

    function notify(text, style = 'info') {
        message.textContent = text;
        message.className = `alert alert-${style}`;
        message.hidden = !text;
    }

    async function get(path, params, signal) {
        const response = await fetch(`${root.dataset.api}/${path}?${params}`, {
            credentials: 'same-origin', headers: { Accept: 'application/json' }, signal
        });
        if (response.status === 401) throw new Error('Sua sessão expirou. Acesse a página de login novamente.');
        if (!response.headers.get('content-type')?.includes('application/json')) {
            throw new Error('O servidor não retornou os dados esperados. Entre novamente no sistema.');
        }
        const result = await response.json();
        if (!response.ok || !result.sucesso) throw new Error(result.erro?.mensagem || 'Não foi possível carregar os dados.');
        return result.dados;
    }

    function chart(id, type, labels, values, horizontal = false) {
        if (charts[id]) charts[id].destroy();
        charts[id] = new Chart(document.getElementById(id), {
            type,
            data: { labels, datasets: [{ label: 'Chamados', data: values,
                backgroundColor: type === 'line' ? '#343a4020' : colors,
                borderColor: type === 'line' ? '#343a40' : '#fff', borderWidth: 2, tension: 0.2, fill: type === 'line' }] },
            options: { responsive: true, maintainAspectRatio: false, indexAxis: horizontal ? 'y' : 'x',
                plugins: { legend: { display: type === 'doughnut', position: 'bottom' } },
                ...(type !== 'doughnut' ? { scales: { [horizontal ? 'x' : 'y']: { beginAtZero: true, ticks: { precision: 0 } } } } : {}) }
        });
    }

    function table(result) {
        total = result.total;
        const rows = document.getElementById('dashboard-rows');
        rows.replaceChildren();
        for (const item of result.itens) {
            const row = document.createElement('tr');
            const date = item.dataAbertura ? new Date(item.dataAbertura).toLocaleString('pt-BR', { timeZone: 'America/Sao_Paulo' }) : 'Sem data';
            const values = [item.descricao, date, item.status, item.tipo,
                [item.tecnico, item.usuario].filter(Boolean).join(' / '), item.clienteId, item.consideracoesTecnicas];
            values.forEach((value, index) => {
                const cell = document.createElement('td');
                if (index === 2) {
                    const badge = document.createElement('span');
                    badge.className = 'badge text-bg-secondary';
                    badge.textContent = value || 'Não informado';
                    cell.append(badge);
                } else cell.textContent = value || 'Não informado';
                row.append(cell);
            });
            rows.append(row);
        }
        if (!result.itens.length) {
            const cell = document.createElement('td');
            cell.colSpan = 7;
            cell.textContent = 'Nenhum chamado nesta página.';
            const row = document.createElement('tr');
            row.append(cell);
            rows.append(row);
        }
        document.getElementById('dashboard-page').textContent = `Página ${page + 1} de ${Math.max(1, Math.ceil(total / size))} · ${number.format(total)} chamados`;
    }

    async function load(all = true, params = activeFilters, requestedPage = page) {
        controller?.abort();
        controller = new AbortController();
        const current = controller;
        refresh.disabled = true;
        previous.disabled = true;
        next.disabled = true;
        body.setAttribute('aria-busy', 'true');
        notify('Carregando dados…');
        const listParams = new URLSearchParams(params);
        listParams.set('pagina', requestedPage);
        listParams.set('tamanho', size);
        try {
            const requests = [get('chamados', listParams, current.signal)];
            if (all) for (const endpoint of ['indicadores', 'por-periodo', 'situacao', 'tipos']) requests.push(get(endpoint, params, current.signal));
            const [list, indicators, evolution, status, types] = await Promise.all(requests);
            page = requestedPage;
            activeFilters = new URLSearchParams(params);
            table(list);
            if (all) {
                for (const key of ['total', 'abertos', 'emAndamento', 'resolvidos', 'taxaResolucao', 'comConsideracoesTecnicas']) {
                    document.getElementById(`kpi-${key}`).textContent = number.format(indicators[key]) + (key === 'taxaResolucao' ? '%' : '');
                }
                document.getElementById('dashboard-quality').textContent = `${indicators.semDataAbertura} sem data de abertura · ${indicators.outros} em outros status. Datas ausentes não entram na evolução temporal.`;
                document.getElementById('periodo-summary').textContent = `${evolution.periodos.length} períodos com registros · ${evolution.semData} chamados sem data.`;
                for (const [id, data] of [['situacao', status], ['tipos', types]]) {
                    document.getElementById(`${id}-summary`).textContent = data.map(item => `${item.nome}: ${number.format(item.quantidade)} (${number.format(item.percentual)}%)`).join(' · ') || 'Sem dados.';
                }
                if (typeof Chart !== 'undefined') {
                    chart('chart-periodo', 'line', evolution.periodos.map(item => item.periodo), evolution.periodos.map(item => item.quantidade));
                    chart('chart-situacao', 'doughnut', status.map(item => item.nome), status.map(item => item.quantidade));
                    chart('chart-tipos', 'bar', types.map(item => item.nome), types.map(item => item.quantidade), true);
                }
            }
            document.getElementById('dashboard-updated').textContent = `Atualizado às ${new Date().toLocaleTimeString('pt-BR')}`;
            notify(total === 0 ? 'Nenhum chamado corresponde aos filtros selecionados.' :
                typeof Chart === 'undefined' ? 'Dados carregados. Os gráficos estão indisponíveis; consulte os resumos e a tabela.' : '', total === 0 ? 'info' : 'warning');
        } catch (error) {
            if (error.name !== 'AbortError') notify(`${error.message} Os dados já exibidos podem estar desatualizados.`, 'danger');
        } finally {
            if (current === controller) {
                body.setAttribute('aria-busy', 'false');
                refresh.disabled = false;
                previous.disabled = page === 0;
                next.disabled = (page + 1) * size >= total;
            }
        }
    }

    form.addEventListener('submit', event => {
        event.preventDefault();
        const params = new URLSearchParams();
        for (const [key, value] of new FormData(form)) if (value.trim()) params.set(key, value.trim());
        if (params.get('dataInicio') && params.get('dataFim') && params.get('dataInicio') > params.get('dataFim')) {
            notify('A data inicial deve ser anterior ou igual à data final.', 'warning');
            return;
        }
        load(true, params, 0);
    });
    form.addEventListener('reset', () => { setTimeout(() => form.requestSubmit(), 0); });
    previous.addEventListener('click', () => { if (page > 0) load(false, activeFilters, page - 1); });
    next.addEventListener('click', () => { if ((page + 1) * size < total) load(false, activeFilters, page + 1); });
    form.requestSubmit();
})();
