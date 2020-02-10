package no.nav.foreldrepenger.behandling.anke.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

/** Lag historikk innslag ved anke. */
@ApplicationScoped
public class AnkeHistorikkTjeneste {

    private HistorikkRepository historikkRepository;

    AnkeHistorikkTjeneste() {
        // For CDI
    }

    @Inject
    public AnkeHistorikkTjeneste(HistorikkRepository historikkRepository) {
        this.historikkRepository = historikkRepository;
    }

    public void opprettHistorikkinnslag(Behandling ankeBehandling) {
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.SØKER);
        historikkinnslag.setType(HistorikkinnslagType.BEH_STARTET);
        historikkinnslag.setBehandlingId(ankeBehandling.getId());
        historikkinnslag.setFagsakId(ankeBehandling.getFagsakId());

        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(BehandlingType.ANKE.equals(ankeBehandling.getType()) ? HistorikkinnslagType.ANKEBEH_STARTET : HistorikkinnslagType.BEH_STARTET);
        builder.build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
    }
}
