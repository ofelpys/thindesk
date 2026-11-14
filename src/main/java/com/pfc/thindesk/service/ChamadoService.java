package com.pfc.thindesk.service;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.repository.ChamadoRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChamadoService {

    @Autowired
    private ChamadoRepository chamadoRepository;

    public Chamado criarChamado(Chamado chamado) {
        String status = chamado.getStatus();

        if (status == null || status.isBlank()) {
            status = "ABERTO";
        }
        validarStatus(status);

        Instant agora = Instant.now();
        chamado.setId(null);
        chamado.setStatus(status);
        chamado.setAbertoEm(agora);
        chamado.setConcluidoEm("CONCLUIDO".equals(status) ? agora : null);

        return chamadoRepository.save(chamado);
    }

    public List<Chamado> listarChamados() {
        return chamadoRepository.findAll();
    }

    public Chamado buscarPorId(String id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Chamado não encontrado"));
    }

    public Chamado atualizarChamado(String id, Chamado chamadoAtualizado) {
        validarStatus(chamadoAtualizado.getStatus());

        Chamado chamado = chamadoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Chamado não encontrado"));

        String novoStatus = chamadoAtualizado.getStatus();

        if ("CONCLUIDO".equals(novoStatus)
                && (!"CONCLUIDO".equals(chamado.getStatus())
                || chamado.getConcluidoEm() == null)) {
            chamado.setConcluidoEm(Instant.now());
        } else if (!"CONCLUIDO".equals(novoStatus)) {
            chamado.setConcluidoEm(null);
        }

        chamado.setDescricao(chamadoAtualizado.getDescricao());
        chamado.setStatus(novoStatus);
        chamado.setTipo(chamadoAtualizado.getTipo());
        chamado.setTecnico(chamadoAtualizado.getTecnico());
        chamado.setUsuario(chamadoAtualizado.getUsuario());

        if (chamadoAtualizado.getConsideracoesTecnicas() != null) {
            chamado.setConsideracoesTecnicas(
                    chamadoAtualizado.getConsideracoesTecnicas());
        }

        return chamadoRepository.save(chamado);
    }

    private void validarStatus(String status) {
        if (!"ABERTO".equals(status)
                && !"EM_ANDAMENTO".equals(status)
                && !"CONCLUIDO".equals(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Status inválido");
        }
    }
}