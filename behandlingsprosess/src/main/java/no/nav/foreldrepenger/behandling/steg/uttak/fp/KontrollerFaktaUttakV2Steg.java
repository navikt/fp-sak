package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.uttak.UttakSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.fakta.dokumentasjon.VurderUttakDokumentasjonAksjonspunktUtleder;

@BehandlingStegRef(BehandlingStegType.KONTROLLER_FAKTA_UTTAK_V2)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class KontrollerFaktaUttakV2Steg implements UttakSteg {

    private BehandlingRepository behandlingRepository;
    private VurderUttakDokumentasjonAksjonspunktUtleder aksjonspunktUtleder;
    private UttakInputTjeneste uttakInputTjeneste;

    @Inject
    public KontrollerFaktaUttakV2Steg(BehandlingRepository behandlingRepository,
                                      VurderUttakDokumentasjonAksjonspunktUtleder aksjonspunktUtleder,
                                      UttakInputTjeneste uttakInputTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.aksjonspunktUtleder = aksjonspunktUtleder;
        this.uttakInputTjeneste = uttakInputTjeneste;
    }

    KontrollerFaktaUttakV2Steg() {
        //CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var uttakInput = uttakInputTjeneste.lagInput(kontekst.getBehandlingId());
        var ap = aksjonspunktUtleder.utledAksjonspunkterFor(uttakInput);
        return BehandleStegResultat.utførtMedAksjonspunkter(ap.map(ad -> List.of(ad)).orElse(List.of()));
    }
}
