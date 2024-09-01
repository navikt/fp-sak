package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt.AksjonspunktUtlederForArbeidsforholdInntektsmelding;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

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
        var ref = BehandlingReferanse.fra(behandling);

        arbeidsforholdInntektsmeldingMangelTjeneste.ryddVekkUgyldigeValg(ref, skjæringstidspunkter);
        arbeidsforholdInntektsmeldingMangelTjeneste.ryddVekkUgyldigeArbeidsforholdoverstyringer(ref);

        List<AksjonspunktResultat> aksjonspuntker = new ArrayList<>(utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref, skjæringstidspunkter)));
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspuntker);
    }
}
