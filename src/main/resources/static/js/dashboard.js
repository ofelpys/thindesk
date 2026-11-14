const formulario = document.getElementById("filtros");
let consultaAtual = 0;

function porId(id) {
    return document.getElementById(id);
}

function criar(tag, texto = "", classe = "") {
    const elemento = document.createElement(tag);
    elemento.textContent = texto;
    if (classe) elemento.className = classe;
    return elemento;
}

function mostrarMensagem(texto, tipo) {
    const aviso = porId("mensagem");
    aviso.className = `alert alert-${tipo}`;
    aviso.textContent = texto;
}

function limparPainel() {
    ["totalChamados", "abertos", "emAndamento", "concluidos", "tempoMedio"]
        .forEach(id => porId(id).textContent = "—");

    ["graficoAtendidos", "situacao", "rankingTipos", "listaChamados"]
        .forEach(id => porId(id).replaceChildren());

    porId("atualizadoEm").textContent = "";
}

function montarConsulta() {
    const inicio = porId("inicio").value;
    const fim = porId("fim").value;

    if (Boolean(inicio) !== Boolean(fim)) {
        throw new Error("Preencha a data inicial e a data final juntas.");
    }
    if (inicio && inicio > fim) {
        throw new Error("A data inicial não pode ser posterior à data final.");
    }

    const parametros = new URLSearchParams();
    if (inicio) {
        parametros.set("inicio", inicio);
        parametros.set("fim", fim);
    }

    const status = porId("status").value;
    const tipo = porId("tipo").value.trim();
    if (status) parametros.set("status", status);
    if (tipo) parametros.set("tipo", tipo);

    return parametros.toString();
}

async function buscarJson(caminho, consulta) {
    const url = consulta ? `${caminho}?${consulta}` : caminho;
    let resposta;

    try {
        resposta = await fetch(url, {
            headers: { "Accept": "application/json" }
        });
    } catch {
        throw new Error("Não foi possível conectar à API.");
    }

    if (resposta.status === 400) {
        throw new Error("Filtros inválidos. Confira as datas e o status.");
    }
    if (!resposta.ok) {
        throw new Error(`Falha ao consultar a API (${resposta.status}).`);
    }

    const tipoResposta = resposta.headers.get("content-type") || "";
    if (!tipoResposta.includes("application/json")) {
        throw new Error("Sua sessão expirou. Faça login novamente.");
    }

    try {
        return await resposta.json();
    } catch {
        throw new Error("A API retornou uma resposta inválida.");
    }
}

function formatarData(valor) {
    if (!valor) return "—";
    const partes = valor.split("-");
    return partes.length === 3
        ? `${partes[2]}/${partes[1]}/${partes[0]}`
        : valor;
}

function formatarDataHora(valor) {
    if (!valor) return "—";
    const data = new Date(valor);
    if (Number.isNaN(data.getTime())) return "—";
    return data.toLocaleString("pt-BR", {
        timeZone: "America/Sao_Paulo"
    });
}

function renderizarResumo(dados) {
    porId("totalChamados").textContent = dados.totalChamados ?? 0;
    porId("abertos").textContent = dados.abertos ?? 0;
    porId("emAndamento").textContent = dados.emAndamento ?? 0;
    porId("concluidos").textContent = dados.concluidos ?? 0;
    porId("tempoMedio").textContent =
        dados.tempoMedioAtendimentoMinutos ?? 0;
}

function renderizarGrafico(dados) {
    const grafico = porId("graficoAtendidos");
    grafico.replaceChildren();

    if (dados.length === 0) {
        grafico.append(criar("p", "Nenhum chamado concluído no período.",
            "text-secondary"));
        return;
    }

    const maximo = Math.max(
        1, ...dados.map(item => Number(item.quantidade) || 0)
    );

    for (const item of dados) {
        const quantidade = Number(item.quantidade) || 0;
        const coluna = criar("div", "", "grafico-coluna");
        const valor = criar("span", quantidade, "grafico-valor");
        const barra = criar("div", "", "grafico-barra");
        const data = criar("span", formatarData(item.data), "grafico-data");

        barra.style.height = quantidade === 0
            ? "0px"
            : `${Math.max(8, Math.round(150 * quantidade / maximo))}px`;
        barra.setAttribute("aria-hidden", "true");
        coluna.title = `${data.textContent}: ${quantidade} chamado(s) concluído(s)`;
        coluna.append(valor, barra, data);
        grafico.append(coluna);
    }
}

function renderizarSituacao(dados) {
    const area = porId("situacao");
    area.replaceChildren();

    const nomes = {
        ABERTO: "Abertos",
        EM_ANDAMENTO: "Em andamento",
        CONCLUIDO: "Concluídos"
    };
    const cores = {
        ABERTO: "bg-danger",
        EM_ANDAMENTO: "bg-warning",
        CONCLUIDO: "bg-success"
    };
    const total = dados.reduce(
        (soma, item) => soma + Number(item.quantidade), 0
    );

    for (const item of dados) {
        const quantidade = Number(item.quantidade);
        const linha = criar("div", "", "d-flex justify-content-between small mb-1");
        linha.append(
            criar("span", nomes[item.status] || item.status),
            criar("strong", quantidade)
        );

        const trilho = criar("div", "", "progress");
        trilho.style.height = "10px";
        const barra = criar("div", "", `progress-bar ${cores[item.status] || "bg-primary"}`);
        barra.style.width = `${total === 0 ? 0 : quantidade * 100 / total}%`;
        barra.setAttribute("aria-hidden", "true");
        trilho.append(barra);

        const bloco = criar("div", "", "mb-3");
        bloco.append(linha, trilho);
        area.append(bloco);
    }
}

function renderizarRanking(dados) {
    const lista = porId("rankingTipos");
    lista.replaceChildren();
    lista.classList.toggle("list-group-numbered", dados.length > 0);

    if (dados.length === 0) {
        lista.append(criar("li", "Nenhum chamado concluído no período.",
            "list-group-item text-secondary"));
        return;
    }

    for (const item of dados) {
        const linha = criar("li", "",
            "list-group-item d-flex justify-content-between align-items-center");
        linha.append(
            criar("span", item.tipo),
            criar("span", item.quantidade, "badge text-bg-primary rounded-pill")
        );
        lista.append(linha);
    }
}

function renderizarChamados(dados) {
    const corpo = porId("listaChamados");
    corpo.replaceChildren();

    if (dados.length === 0) {
        const linha = criar("tr");
        const celula = criar("td", "Nenhum chamado encontrado no período.",
            "text-secondary");
        celula.colSpan = 7;
        linha.append(celula);
        corpo.append(linha);
        return;
    }

    for (const chamado of dados) {
        const linha = criar("tr");
        const valores = [
            formatarDataHora(chamado.abertoEm),
            chamado.descricao || "—",
            chamado.status || "—",
            chamado.tipo || "—",
            chamado.tecnico || "—",
            chamado.usuario || "—",
            chamado.consideracoesTecnicas || "—"
        ];

        for (const valor of valores) {
            linha.append(criar("td", valor));
        }
        corpo.append(linha);
    }
}

async function carregarDados() {
    const numero = ++consultaAtual;
    limparPainel();
    mostrarMensagem("Carregando dados...", "info");

    try {
        const consulta = montarConsulta();
        const [resumo, porDia, situacao, tipos, chamados] = await Promise.all([
            buscarJson("/api/dashboard/resumo", consulta),
            buscarJson("/api/dashboard/atendidos-por-dia", consulta),
            buscarJson("/api/dashboard/status", consulta),
            buscarJson("/api/dashboard/tipos-mais-atendidos", consulta),
            buscarJson("/api/chamados", consulta)
        ]);

        if (numero !== consultaAtual) return;

        renderizarResumo(resumo);
        renderizarGrafico(porDia);
        renderizarSituacao(situacao);
        renderizarRanking(tipos);
        renderizarChamados(chamados);

        const agora = new Date().toLocaleString("pt-BR", {
            timeZone: "America/Sao_Paulo"
        });
        porId("atualizadoEm").textContent = `Atualizado em ${agora}`;

        if (resumo.totalChamados === 0 && porDia.length === 0
            && tipos.length === 0) {
            mostrarMensagem("Nenhum dado encontrado para os filtros.", "warning");
        } else {
            porId("mensagem").classList.add("d-none");
        }
    } catch (erro) {
        if (numero === consultaAtual) {
            mostrarMensagem(erro.message || "Não foi possível carregar os dados.",
                "danger");
        }
    }
}

formulario.addEventListener("submit", evento => {
    evento.preventDefault();
    carregarDados();
});

porId("atualizar").addEventListener("click", carregarDados);

porId("limpar").addEventListener("click", evento => {
    evento.preventDefault();
    formulario.reset();
    carregarDados();
});

carregarDados();