package no.nav.foreldrepenger.økonomistøtte.ny.toggle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import net.bytebuddy.asm.Advice;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.vedtak.util.env.Environment;

@ApplicationScoped
public class OppdragKjerneimplementasjonToggle {

    private BehandlingRepository behandlingRepository;

    OppdragKjerneimplementasjonToggle() {
        //cdi proxy
    }

    @Inject
    public OppdragKjerneimplementasjonToggle(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    public boolean brukNyImpl(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var fagsakOpprettetTid = behandling.getFagsak().getOpprettetTidspunkt();
        return !Environment.current().isProd() || fagsakOpprettetTid.isAfter(LocalDateTime.of(LocalDate.of(2021, 2, 1), LocalTime.MIDNIGHT));
    }
}
