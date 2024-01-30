package no.nav.foreldrepenger.dokumentbestiller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
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
                                        DokumentMalType dokumentMal,
                                        DokumentMalType opprinneligDokumentMal) {

        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setBehandling(behandling);
        historikkinnslag.setAktør(historikkAktør);
        historikkinnslag.setType(HistorikkinnslagType.BREV_BESTILT);

        new HistorikkInnslagTekstBuilder().medHendelse(HistorikkinnslagType.BREV_BESTILT)
            .medBegrunnelse(utledBegrunnelse(dokumentMal, opprinneligDokumentMal))
            .build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }

    private String utledBegrunnelse(DokumentMalType dokumentMal, DokumentMalType opprinneligDokumentMal) {
        if (DokumentMalType.FRITEKSTBREV.equals(dokumentMal) && opprinneligDokumentMal != null) {
            return opprinneligDokumentMal.getNavn() + " (" + dokumentMal.getNavn() + ")";
        }
        return dokumentMal.getNavn();
    }
}
