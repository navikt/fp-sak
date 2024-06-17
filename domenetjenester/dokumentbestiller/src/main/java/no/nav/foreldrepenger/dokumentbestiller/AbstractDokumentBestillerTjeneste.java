package no.nav.foreldrepenger.dokumentbestiller;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;

public abstract class AbstractDokumentBestillerTjeneste {

    private KlageRepository klageRepository;

    AbstractDokumentBestillerTjeneste() {
    }

    protected AbstractDokumentBestillerTjeneste(KlageRepository klageRepository) {
        this.klageRepository = klageRepository;
    }

    protected KlageVurdering finnKlageVurdering(Behandling behandling) {
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            return klageRepository.hentGjeldendeKlageVurderingResultat(behandling).map(KlageVurderingResultat::getKlageVurdering).orElse(null);
        }
        return null;
    }

    protected static boolean foreldrepengerErEndret(BehandlingResultatType behandlingResultatType) {
        return BehandlingResultatType.FORELDREPENGER_ENDRET.equals(behandlingResultatType);
    }

    protected static boolean erKunEndringIFordelingAvYtelsen(List<KonsekvensForYtelsen> konsekvensForYtelsen) {
        return konsekvensForYtelsen.contains(KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN) && konsekvensForYtelsen.size() == 1;
    }
}
