package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt.AksjonspunktUtlederForArbeidsforholdInntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
class KontrollerArbeidsforholdInntektsmeldingStegImpl implements KontrollerArbeidsforholdInntektsmeldingSteg {
    private static final Logger LOG = LoggerFactory.getLogger(KontrollerArbeidsforholdInntektsmeldingStegImpl.class);

    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private AksjonspunktUtlederForArbeidsforholdInntektsmelding utleder;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    KontrollerArbeidsforholdInntektsmeldingStegImpl() {
        // for CDI proxy
    }

    @Inject
    KontrollerArbeidsforholdInntektsmeldingStegImpl(BehandlingRepository behandlingRepository,
                                                    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                    AksjonspunktUtlederForArbeidsforholdInntektsmelding utleder,
                                                    ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste,
                                                    InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.utleder = utleder;
        this.arbeidsforholdInntektsmeldingMangelTjeneste = arbeidsforholdInntektsmeldingMangelTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        try {
            var originalBehandlingId = behandling.getOriginalBehandlingId();
            if (originalBehandlingId.isPresent()) {
                var nyeInntektsmeldinger = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId)
                    .flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger)
                    .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
                    .orElse(Collections.emptyList());
                var gamleInntektsmeldinger = inntektArbeidYtelseTjeneste.finnGrunnlag(originalBehandlingId.get())
                    .flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger)
                    .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
                    .orElse(Collections.emptyList());
                var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();
                var saksnummer = behandling.getFagsak().getSaksnummer().getVerdi();
                var alleEndringerIRefusjon = LoggRefusjonsavvikTjeneste.finnAvvik(saksnummer, stp, nyeInntektsmeldinger, gamleInntektsmeldinger);
                var alleEndringerIopphørtRefusjon = LoggRefusjonsavvikTjeneste.finnEndringIOpphørsdato(saksnummer, stp, nyeInntektsmeldinger, gamleInntektsmeldinger);
                alleEndringerIRefusjon.forEach(endring -> LOG.info("Fant avvik i refusjon: {}", endring));
                alleEndringerIopphørtRefusjon.forEach( endringOpphør -> LOG.info("Fant endring i opphørsdato: {}", endringOpphør));

            }
        } catch (Exception e) {
            LOG.info("Feil i logging av refusjonsdiff: ", e);
        }

        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);

        arbeidsforholdInntektsmeldingMangelTjeneste.ryddVekkUgyldigeValg(ref);
        arbeidsforholdInntektsmeldingMangelTjeneste.ryddVekkUgyldigeArbeidsforholdoverstyringer(ref);

        List<AksjonspunktResultat> aksjonspuntker = new ArrayList<>(utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref)));
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspuntker);
    }
}
