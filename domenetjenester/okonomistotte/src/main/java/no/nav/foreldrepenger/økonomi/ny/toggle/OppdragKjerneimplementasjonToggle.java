package no.nav.foreldrepenger.økonomi.ny.toggle;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.util.env.Environment;

@ApplicationScoped
public class OppdragKjerneimplementasjonToggle {

    //TODO legg inn saksnumre i denne lista for å lansere ny impl for utvalgte saker
    public static final Set<Saksnummer> LANSERT_I_PROD = Set.of();

    OppdragKjerneimplementasjonToggle() {
        //cdi proxy
    }

    public boolean brukNyImpl(Long behandlingId) {
        return false;
    }
}
