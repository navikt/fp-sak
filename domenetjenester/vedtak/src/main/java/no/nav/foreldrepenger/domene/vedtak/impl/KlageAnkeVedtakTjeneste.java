package no.nav.foreldrepenger.domene.vedtak.impl;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;

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

    public boolean erVurdertVedKlageinstans(Behandling behandling) {
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            return klageRepository.hentGjeldendeKlageVurderingResultat(behandling)
                .map(KlageVurderingResultat::getKlageVurdertAv).filter(KlageVurdertAv.NK::equals).isPresent();
        }
        if (BehandlingType.ANKE.equals(behandling.getType())) {
            return ankeRepository.hentAnkeVurderingResultat(behandling.getId()).isPresent();
        }
        return false;
    }

    public boolean skalOversendesTrygdretten(Behandling behandling) {
        if (BehandlingType.ANKE.equals(behandling.getType())) {
            var ankeVurderingResultat = ankeRepository.hentAnkeVurderingResultat(behandling.getId());
            return ankeVurderingResultat.isPresent()
                && ankeVurderingResultat.filter(AnkeVurderingResultatEntitet::godkjentAvMedunderskriver).isPresent()
                && Set.of(AnkeVurdering.ANKE_AVVIS, AnkeVurdering.ANKE_STADFESTE_YTELSESVEDTAK).contains(ankeVurderingResultat.get().getAnkeVurdering());
        }
        return false;
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

    public boolean erGodkjentHosMedunderskriver(Behandling behandling) {
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            return klageRepository.hentGjeldendeKlageVurderingResultat(behandling)
                .filter(KlageVurderingResultat::isGodkjentAvMedunderskriver)
                .map(KlageVurderingResultat::getKlageVurdertAv)
                .filter(KlageVurdertAv.NK::equals).isPresent();
        }
        if (BehandlingType.ANKE.equals(behandling.getType())) {
            return ankeRepository.hentAnkeVurderingResultat(behandling.getId())
                .filter(AnkeVurderingResultatEntitet::godkjentAvMedunderskriver).isPresent();
        }
        return false;
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

    public void settGodkjentHosMedunderskriver(Behandling behandling) {
        if (BehandlingType.ANKE.equals(behandling.getType())) {
            ankeRepository.settAnkeGodkjentHosMedunderskriver(behandling.getId(), true);
        } else if (BehandlingType.KLAGE.equals(behandling.getType())) {
            var vurdertAv = behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_KA) ? KlageVurdertAv.NK : KlageVurdertAv.NFP;
            klageRepository.settKlageGodkjentHosMedunderskriver(behandling.getId(), vurdertAv, true);
        }
    }

}
