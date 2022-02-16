package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdInntektsmeldingToggleTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "KO_ARB_IM")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
class KontrollerArbeidsforholdInntektsmeldingStegImpl implements KontrollerArbeidsforholdInntektsmeldingSteg {

    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private AksjonspunktUtlederForArbeidsforholdInntektsmelding utleder;

    KontrollerArbeidsforholdInntektsmeldingStegImpl() {
        // for CDI proxy
    }

    @Inject
    KontrollerArbeidsforholdInntektsmeldingStegImpl(BehandlingRepository behandlingRepository,
                                                    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                    AksjonspunktUtlederForArbeidsforholdInntektsmelding utleder) {
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.utleder = utleder;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        if (!ArbeidsforholdInntektsmeldingToggleTjeneste.erTogglePå()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
        List<AksjonspunktResultat> aksjonspuntker = new ArrayList<>(utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref)));
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspuntker);
    }
}
