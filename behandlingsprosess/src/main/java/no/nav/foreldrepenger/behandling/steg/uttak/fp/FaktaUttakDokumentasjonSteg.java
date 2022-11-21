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
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.fakta.v2.VurderUttakDokumentasjonAksjonspunktUtleder;

@BehandlingStegRef(BehandlingStegType.FAKTA_UTTAK_DOKUMENTASJON)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class FaktaUttakDokumentasjonSteg implements UttakSteg {

    private VurderUttakDokumentasjonAksjonspunktUtleder vurderUttakDokumentasjonAksjonspunktUtleder;
    private UttakInputTjeneste uttakInputTjeneste;

    @Inject
    public FaktaUttakDokumentasjonSteg(VurderUttakDokumentasjonAksjonspunktUtleder vurderUttakDokumentasjonAksjonspunktUtleder,
                                       UttakInputTjeneste uttakInputTjeneste) {
        this.vurderUttakDokumentasjonAksjonspunktUtleder = vurderUttakDokumentasjonAksjonspunktUtleder;
        this.uttakInputTjeneste = uttakInputTjeneste;
    }

    FaktaUttakDokumentasjonSteg() {
        //CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var uttakInput = uttakInputTjeneste.lagInput(kontekst.getBehandlingId());
        var dokumentasjonAP = vurderUttakDokumentasjonAksjonspunktUtleder.utledAksjonspunkterFor(uttakInput);
        return BehandleStegResultat.utførtMedAksjonspunkter(dokumentasjonAP.map(ad -> List.of(ad)).orElse(List.of()));
    }
}
