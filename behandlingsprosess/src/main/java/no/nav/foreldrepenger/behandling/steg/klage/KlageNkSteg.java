package no.nav.foreldrepenger.behandling.steg.klage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

@BehandlingStegRef(BehandlingStegType.KLAGE_NK)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class KlageNkSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private KlageRepository klageRepository;

    public KlageNkSteg() {
        // For CDI proxy
    }

    @Inject
    public KlageNkSteg(BehandlingRepository behandlingRepository, KlageRepository klageRepository) {
        this.behandlingRepository = behandlingRepository;
        this.klageRepository = klageRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        var klageVurderingNFP = klageRepository.hentKlageVurderingResultat(behandling.getId(), KlageVurdertAv.NFP)
                .orElseThrow(() -> new IllegalStateException("Skal ha NFPs klagevurdering opprettet før dette steget"));

        if (KlageVurderingTjeneste.skalBehandlesAvKlageInstans(KlageVurdertAv.NFP, klageVurderingNFP.getKlageVurdering())
            && !klageVurderingNFP.getKlageResultat().erBehandletAvKabal()) {
            throw new IllegalStateException("Utviklerfeil: Skal ikke lenger gjennomføre BehandlingSteg KlageKA. Mangler Kabal-utfall");
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
