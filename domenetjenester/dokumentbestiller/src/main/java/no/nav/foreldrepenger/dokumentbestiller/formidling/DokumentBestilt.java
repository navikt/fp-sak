package no.nav.foreldrepenger.dokumentbestiller.formidling;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

@ApplicationScoped
public class DokumentBestilt {
    private Historikkinnslag2Repository historikkinnslag2Repository;

    public DokumentBestilt() {
        // for cdi proxy
    }

    @Inject
    public DokumentBestilt(Historikkinnslag2Repository historikkinnslag2Repository) {
        this.historikkinnslag2Repository = historikkinnslag2Repository;
    }

    public void opprettHistorikkinnslag(HistorikkAktør historikkAktør,
                                        Behandling behandling,
                                        DokumentBestilling bestilling) {
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medAktør(historikkAktør)
            .medTittel("Brev bestilt")
            .addLinje(utledBegrunnelse(bestilling.dokumentMal(), bestilling.journalførSom()))
            .build();
        historikkinnslag2Repository.lagre(historikkinnslag);
    }

    private String utledBegrunnelse(DokumentMalType dokumentMal, DokumentMalType journalførSom) {
        if (DokumentMalType.FRITEKSTBREV.equals(dokumentMal)) {
            Objects.requireNonNull(journalførSom, "journalførSom må være satt om FRITEKST brev brukes.");
            return journalførSom.getNavn() + " (" + dokumentMal.getNavn() + ")";
        }
        return dokumentMal.getNavn();
    }
}
