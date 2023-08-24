package no.nav.foreldrepenger.domene.vedtak.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;

import java.util.Set;

@ApplicationScoped
public class KlageAnkeVedtakTjeneste {

    private KlageRepository klageRepository;
    private AnkeRepository ankeRepository;

    KlageAnkeVedtakTjeneste() {
        // CDI
    }

    @Inject
    public KlageAnkeVedtakTjeneste(KlageRepository klageRepository,
                                   AnkeRepository ankeRepository) {
        this.klageRepository = klageRepository;
        this.ankeRepository = ankeRepository;
    }

    public static boolean behandlingErKlageEllerAnke(Behandling behandling) {
        return BehandlingType.ANKE.equals(behandling.getType()) || BehandlingType.KLAGE.equals(behandling.getType());
    }

    public boolean skalOversendesTrygdretten(Behandling behandling) {
        return BehandlingType.ANKE.equals(behandling.getType()) &&
            ankeRepository.hentAnkeVurderingResultat(behandling.getId())
                .map(AnkeVurderingResultatEntitet::getAnkeVurdering)
                .filter(Set.of(AnkeVurdering.ANKE_AVVIS, AnkeVurdering.ANKE_STADFESTE_YTELSESVEDTAK)::contains).isPresent();
    }

    public boolean erOversendtTrygdretten(Behandling behandling) {
        return BehandlingType.ANKE.equals(behandling.getType()) &&
            ankeRepository.hentAnkeVurderingResultat(behandling.getId()).map(AnkeVurderingResultatEntitet::getSendtTrygderettDato).isPresent();
    }

    public boolean harKjennelseTrygdretten(Behandling behandling) {
        return BehandlingType.ANKE.equals(behandling.getType()) &&
            ankeRepository.hentAnkeVurderingResultat(behandling.getId())
                .map(AnkeVurderingResultatEntitet::getTrygderettVurdering)
                .filter(v -> !AnkeVurdering.UDEFINERT.equals(v))
                .isPresent();
    }

    public boolean harSattOversendelseDato(Behandling behandling) {
        return BehandlingType.ANKE.equals(behandling.getType()) &&
            ankeRepository.hentAnkeVurderingResultat(behandling.getId())
                .map(AnkeVurderingResultatEntitet::getSendtTrygderettDato)
                .isPresent();
    }

    public boolean erKlageResultatHjemsendt(Behandling behandling) {
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            return klageRepository.hentGjeldendeKlageVurderingResultat(behandling)
                .map(KlageVurderingResultat::getKlageVurdering)
                .filter(KlageVurdering.HJEMSENDE_UTEN_Å_OPPHEVE::equals).isPresent();
        }
        return false;
    }

    public boolean erBehandletAvKabal(Behandling behandling) {
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            return klageRepository.hentKlageResultatHvisEksisterer(behandling.getId())
                .map(KlageResultatEntitet::erBehandletAvKabal).orElse(false);
        } else if (BehandlingType.ANKE.equals(behandling.getType())) {
            return ankeRepository.hentAnkeResultat(behandling.getId())
                .map(AnkeResultatEntitet::erBehandletAvKabal).orElse(false);
        }
        return false;
    }


}
