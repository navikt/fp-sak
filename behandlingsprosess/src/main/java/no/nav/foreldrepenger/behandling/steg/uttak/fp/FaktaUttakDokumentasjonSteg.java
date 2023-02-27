package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDER_UTTAK_DOKUMENTASJON;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.uttak.UttakSteg;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.VurderUttakDokumentasjonAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

@BehandlingStegRef(BehandlingStegType.FAKTA_UTTAK_DOKUMENTASJON)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class FaktaUttakDokumentasjonSteg implements UttakSteg {

    private VurderUttakDokumentasjonAksjonspunktUtleder vurderUttakDokumentasjonAksjonspunktUtleder;
    private UttakInputTjeneste uttakInputTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public FaktaUttakDokumentasjonSteg(VurderUttakDokumentasjonAksjonspunktUtleder vurderUttakDokumentasjonAksjonspunktUtleder,
                                       UttakInputTjeneste uttakInputTjeneste,
                                       BehandlingRepository behandlingRepository) {
        this.vurderUttakDokumentasjonAksjonspunktUtleder = vurderUttakDokumentasjonAksjonspunktUtleder;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    FaktaUttakDokumentasjonSteg() {
        //CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var uttakInput = uttakInputTjeneste.lagInput(behandling);
        var utledetAp = vurderUttakDokumentasjonAksjonspunktUtleder.utledAksjonspunktFor(uttakInput);
        if (utledetAp) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(VURDER_UTTAK_DOKUMENTASJON));
        }
        if (behandling.harAvbruttAksjonspunktMedType(VURDER_UTTAK_DOKUMENTASJON) && harDokVurderingBehov(uttakInput)) {
            var aksjonspunktResultat = AksjonspunktResultat.opprettAvbruttTilUtførtForAksjonspunkt(VURDER_UTTAK_DOKUMENTASJON);
            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(aksjonspunktResultat));
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean harDokVurderingBehov(UttakInput uttakInput) {
        return !vurderUttakDokumentasjonAksjonspunktUtleder.utledDokumentasjonVurderingBehov(uttakInput).isEmpty();
    }
}
