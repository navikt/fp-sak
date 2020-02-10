package no.nav.foreldrepenger.behandling.klage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

/** Lag historikk innslag ved klage. */
@ApplicationScoped
public class KlageHistorikkTjeneste {
    private HistorikkRepository historikkRepository;

    KlageHistorikkTjeneste() {
        // For CDI
    }

    @Inject
    public KlageHistorikkTjeneste(HistorikkRepository historikkRepository) {
        this.historikkRepository = historikkRepository;
    }

    public void opprettHistorikkinnslag(Behandling klageBehandling) {
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.SØKER);
        historikkinnslag.setType(HistorikkinnslagType.BEH_STARTET);
        historikkinnslag.setBehandlingId(klageBehandling.getId());
        historikkinnslag.setFagsakId(klageBehandling.getFagsakId());

        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(BehandlingType.KLAGE.equals(klageBehandling.getType()) ? HistorikkinnslagType.KLAGEBEH_STARTET : HistorikkinnslagType.BEH_STARTET);
        builder.build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
    }
}
