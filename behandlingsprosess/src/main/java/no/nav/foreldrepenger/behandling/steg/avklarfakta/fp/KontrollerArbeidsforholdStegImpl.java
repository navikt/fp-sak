package no.nav.foreldrepenger.behandling.steg.avklarfakta.fp;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerArbeidsforholdSteg;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerArbeidsforholdTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.StartpunktRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.registerinnhenting.KontrollerFaktaAksjonspunktUtleder;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "KOARB")
@BehandlingTypeRef
@FagsakYtelseTypeRef("FP")
@StartpunktRef("KONTROLLER_ARBEIDSFORHOLD")
@ApplicationScoped
class KontrollerArbeidsforholdStegImpl implements KontrollerArbeidsforholdSteg {

    private KontrollerFaktaAksjonspunktUtleder tjeneste;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    KontrollerArbeidsforholdStegImpl() {
        // for CDI proxy
    }

    @Inject
    KontrollerArbeidsforholdStegImpl(BehandlingRepository behandlingRepository,
                                     SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                     @StartpunktRef("KONTROLLER_ARBEIDSFORHOLD") KontrollerArbeidsforholdTjenesteImpl tjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.tjeneste = tjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
        List<AksjonspunktResultat> aksjonspunktResultat = tjeneste.utledAksjonspunkter(ref);
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultat);
    }
}
