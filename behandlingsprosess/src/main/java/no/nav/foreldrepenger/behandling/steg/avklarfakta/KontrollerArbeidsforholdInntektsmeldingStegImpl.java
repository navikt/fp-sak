package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt.AksjonspunktUtlederForArbeidsforholdInntektsmelding;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

import java.util.ArrayList;
import java.util.List;

@BehandlingStegRef(BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
class KontrollerArbeidsforholdInntektsmeldingStegImpl implements KontrollerArbeidsforholdInntektsmeldingSteg {

    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private AksjonspunktUtlederForArbeidsforholdInntektsmelding utleder;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;

    KontrollerArbeidsforholdInntektsmeldingStegImpl() {
        // for CDI proxy
    }

    @Inject
    KontrollerArbeidsforholdInntektsmeldingStegImpl(BehandlingRepository behandlingRepository,
                                                    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                    AksjonspunktUtlederForArbeidsforholdInntektsmelding utleder,
                                                    ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.utleder = utleder;
        this.arbeidsforholdInntektsmeldingMangelTjeneste = arbeidsforholdInntektsmeldingMangelTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);

        arbeidsforholdInntektsmeldingMangelTjeneste.ryddVekkUgyldigeValg(ref);
        arbeidsforholdInntektsmeldingMangelTjeneste.ryddVekkUgyldigeArbeidsforholdoverstyringer(ref);

        List<AksjonspunktResultat> aksjonspuntker = new ArrayList<>(utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref)));
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspuntker);
    }
}
