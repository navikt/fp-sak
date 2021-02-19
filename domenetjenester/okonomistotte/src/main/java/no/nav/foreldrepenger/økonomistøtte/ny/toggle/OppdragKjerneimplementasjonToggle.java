package no.nav.foreldrepenger.økonomistøtte.ny.toggle;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.util.env.Environment;

@ApplicationScoped
public class OppdragKjerneimplementasjonToggle {

    //TODO legg inn saksnumre i denne lista for å lansere ny impl for utvalgte saker
    public static final Set<Saksnummer> LANSERT_I_PROD = Set.of();

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
        return !Environment.current().isProd() && LANSERT_I_PROD.contains(behandling.getFagsak().getSaksnummer());
    }
}
