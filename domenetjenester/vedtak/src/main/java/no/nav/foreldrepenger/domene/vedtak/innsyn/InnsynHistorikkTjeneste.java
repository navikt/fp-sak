package no.nav.foreldrepenger.domene.vedtak.innsyn;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

/** Lag historikk innslag ved innsyn. */
@ApplicationScoped
public class InnsynHistorikkTjeneste {
    private HistorikkRepository historikkRepository;

    InnsynHistorikkTjeneste() {
        // For CDI
    }

    @Inject
    public InnsynHistorikkTjeneste(HistorikkRepository historikkRepository) {
        this.historikkRepository = historikkRepository;
    }

    void opprettHistorikkinnslag(Behandling behandling, HistorikkAktør historikkAktør) {
        Historikkinnslag innslag = new Historikkinnslag();
        innslag.setType(HistorikkinnslagType.INNSYN_OPPR);
        innslag.setAktør(historikkAktør);
        HistorikkInnslagTekstBuilder historiebygger = new HistorikkInnslagTekstBuilder()
                .medHendelse(HistorikkinnslagType.INNSYN_OPPR)
                .medBegrunnelse("Krav om innsyn mottatt " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        historiebygger.build(innslag);

        innslag.setBehandling(behandling);
        historikkRepository.lagre(innslag);
    }

}
