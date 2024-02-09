package no.nav.foreldrepenger.dokumentbestiller.formidling;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.dokumentbestiller.BrevBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
public class DokumentBestilt {
    private HistorikkRepository historikkRepository;

    public DokumentBestilt() {
        // for cdi proxy
    }

    @Inject
    public DokumentBestilt(HistorikkRepository historikkRepository) {
        this.historikkRepository = historikkRepository;
    }

    public void opprettHistorikkinnslag(HistorikkAktør historikkAktør,
                                        Behandling behandling,
                                        BrevBestilling bestilling) {

        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setBehandling(behandling);
        historikkinnslag.setAktør(historikkAktør);
        historikkinnslag.setType(HistorikkinnslagType.BREV_BESTILT);

        new HistorikkInnslagTekstBuilder().medHendelse(HistorikkinnslagType.BREV_BESTILT)
            .medBegrunnelse(utledBegrunnelse(bestilling.dokumentMal(), bestilling.journalførSom()))
            .build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }

    private String utledBegrunnelse(DokumentMalType dokumentMal, DokumentMalType journalførSom) {
        if (DokumentMalType.FRITEKSTBREV.equals(dokumentMal)) {
            Objects.requireNonNull(journalførSom, "journalførSom må være satt om FRITEKST brev brukes.");
            return journalførSom.getNavn() + " (" + dokumentMal.getNavn() + ")";
        }
        return dokumentMal.getNavn();
    }
}
