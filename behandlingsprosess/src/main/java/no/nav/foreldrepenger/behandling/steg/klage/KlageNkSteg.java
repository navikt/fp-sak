package no.nav.foreldrepenger.behandling.steg.klage;

import static java.util.Collections.singletonList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegKoder;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

@BehandlingStegRef(kode = BehandlingStegKoder.KLAGE_NK_KODE)
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
            var aksjonspunktDefinisjons = singletonList(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NK);
            return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjons);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg,
            BehandlingStegType sisteSteg) {
        klageRepository.settKlageGodkjentHosMedunderskriver(kontekst.getBehandlingId(), KlageVurdertAv.NK, false);
    }

}
