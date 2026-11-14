package com.pfc.thindesk.service;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.repository.ChamadoRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DashboardService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Autowired
    private ChamadoRepository chamadoRepository;

    public List<Chamado> listarChamados(
            LocalDate inicio, LocalDate fim, String status, String tipo) {
        List<Chamado> dados = filtrar(inicio, fim, status, tipo, false);
        dados.sort(Comparator.comparing(Chamado::getAbertoEm).reversed());
        return dados;
    }

    public Map<String, Object> resumo(
            LocalDate inicio, LocalDate fim, String status, String tipo) {
        List<Chamado> chamados = filtrar(inicio, fim, status, tipo, false);

        long somaMinutos = 0;
        long chamadosComTempo = 0;

        for (Chamado chamado : chamados) {
            if ("CONCLUIDO".equals(chamado.getStatus())
                    && chamado.getAbertoEm() != null
                    && chamado.getConcluidoEm() != null
                    && !chamado.getConcluidoEm().isBefore(chamado.getAbertoEm())) {
                somaMinutos += Duration.between(
                        chamado.getAbertoEm(), chamado.getConcluidoEm()).toMinutes();
                chamadosComTempo++;
            }
        }

        long media = chamadosComTempo == 0
                ? 0
                : Math.round((double) somaMinutos / chamadosComTempo);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("totalChamados", chamados.size());
        resultado.put("abertos", contarStatus(chamados, "ABERTO"));
        resultado.put("emAndamento", contarStatus(chamados, "EM_ANDAMENTO"));
        resultado.put("concluidos", contarStatus(chamados, "CONCLUIDO"));
        resultado.put("tempoMedioAtendimentoMinutos", media);
        return resultado;
    }

    public List<Map<String, Object>> atendidosPorDia(
            LocalDate inicio, LocalDate fim, String status, String tipo) {
        Map<LocalDate, Long> contagem = new TreeMap<>();

        for (Chamado chamado : filtrar(inicio, fim, status, tipo, true)) {
            if ("CONCLUIDO".equals(chamado.getStatus())) {
                LocalDate dia = chamado.getConcluidoEm()
                        .atZone(FUSO).toLocalDate();
                contagem.merge(dia, 1L, Long::sum);
            }
        }

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> item : contagem.entrySet()) {
            resultado.add(linha("data", item.getKey().toString(), item.getValue()));
        }
        return resultado;
    }

    public List<Map<String, Object>> situacao(
            LocalDate inicio, LocalDate fim, String status, String tipo) {
        List<Chamado> chamados = filtrar(inicio, fim, status, tipo, false);
        List<Map<String, Object>> resultado = new ArrayList<>();

        resultado.add(linha("status", "ABERTO", contarStatus(chamados, "ABERTO")));
        resultado.add(linha("status", "EM_ANDAMENTO",
                contarStatus(chamados, "EM_ANDAMENTO")));
        resultado.add(linha("status", "CONCLUIDO",
                contarStatus(chamados, "CONCLUIDO")));

        return resultado;
    }

    public List<Map<String, Object>> tiposMaisAtendidos(
            LocalDate inicio, LocalDate fim, String status, String tipo) {
        Map<String, Long> contagem = new HashMap<>();

        for (Chamado chamado : filtrar(inicio, fim, status, tipo, true)) {
            if ("CONCLUIDO".equals(chamado.getStatus())) {
                String nome = chamado.getTipo();
                if (nome == null || nome.isBlank()) {
                    nome = "Sem tipo";
                }
                contagem.merge(nome.trim(), 1L, Long::sum);
            }
        }

        List<Map.Entry<String, Long>> ordenados =
                new ArrayList<>(contagem.entrySet());
        ordenados.sort((primeiro, segundo) -> {
            int porQuantidade = Long.compare(
                    segundo.getValue(), primeiro.getValue());
            return porQuantidade != 0
                    ? porQuantidade
                    : primeiro.getKey().compareToIgnoreCase(segundo.getKey());
        });

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Map.Entry<String, Long> item : ordenados) {
            resultado.add(linha("tipo", item.getKey(), item.getValue()));
        }
        return resultado;
    }

    private List<Chamado> filtrar(LocalDate inicio, LocalDate fim,
                                  String status, String tipo, boolean porConclusao) {
        if ((inicio == null) != (fim == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Informe inicio e fim juntos");
        }

        if (inicio == null) {
            fim = LocalDate.now(FUSO);
            inicio = fim.minusDays(29);
        }

        if (inicio.isAfter(fim)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Periodo invalido");
        }

        if (status != null && !status.isBlank()
                && !"ABERTO".equals(status)
                && !"EM_ANDAMENTO".equals(status)
                && !"CONCLUIDO".equals(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Status invalido");
        }

        Instant comeco = inicio.atStartOfDay(FUSO).toInstant();
        Instant fimExclusivo = fim.plusDays(1)
                .atStartOfDay(FUSO).toInstant();

        List<Chamado> resultado = new ArrayList<>();
        for (Chamado chamado : chamadoRepository.findAll()) {
            Instant data = porConclusao
                    ? chamado.getConcluidoEm()
                    : chamado.getAbertoEm();

            if (data == null || data.isBefore(comeco)
                    || !data.isBefore(fimExclusivo)) {
                continue;
            }
            if (status != null && !status.isBlank()
                    && !status.equals(chamado.getStatus())) {
                continue;
            }
            if (tipo != null && !tipo.isBlank()
                    && (chamado.getTipo() == null
                    || !tipo.trim().equalsIgnoreCase(
                    chamado.getTipo().trim()))) {
                continue;
            }
            resultado.add(chamado);
        }
        return resultado;
    }

    private long contarStatus(List<Chamado> chamados, String status) {
        return chamados.stream()
                .filter(chamado -> status.equals(chamado.getStatus()))
                .count();
    }

    private Map<String, Object> linha(
            String campo, String valor, long quantidade) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put(campo, valor);
        item.put("quantidade", quantidade);
        return item;
    }
}
