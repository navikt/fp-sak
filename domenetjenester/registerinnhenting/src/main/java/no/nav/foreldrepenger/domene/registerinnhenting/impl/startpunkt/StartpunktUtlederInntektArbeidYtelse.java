package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.arbeidsforhold.IAYGrunnlagDiff;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;

@ApplicationScoped
@GrunnlagRef("InntektArbeidYtelseGrunnlag")
class StartpunktUtlederInntektArbeidYtelse implements StartpunktUtleder {

    private String klassenavn = this.getClass().getSimpleName();
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private StartpunktUtlederInntektsmelding startpunktUtlederInntektsmelding;
    private VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste;
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    public StartpunktUtlederInntektArbeidYtelse() {
        // For CDI
    }

    @Inject
    StartpunktUtlederInntektArbeidYtelse(InntektArbeidYtelseTjeneste iayTjeneste, // NOSONAR - ingen enkel måte å unngå mange parametere her
                                         BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                         BehandlingRepositoryProvider repositoryProvider,
                                         StartpunktUtlederInntektsmelding startpunktUtlederInntektsmelding,
                                         VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste,
                                         ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.startpunktUtlederInntektsmelding = startpunktUtlederInntektsmelding;
        this.vurderArbeidsforholdTjeneste = vurderArbeidsforholdTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.arbeidsforholdAdministrasjonTjeneste = arbeidsforholdAdministrasjonTjeneste;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        return hentAlleStartpunktForInntektArbeidYtelse(ref, true, (UUID) grunnlagId1, (UUID) grunnlagId2).stream()
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    @Override
    public StartpunktType utledInitieltStartpunktRevurdering(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        return hentAlleStartpunktForInntektArbeidYtelse(ref, false, (UUID) grunnlagId1, (UUID) grunnlagId2).stream()
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    private List<StartpunktType> hentAlleStartpunktForInntektArbeidYtelse(BehandlingReferanse ref, boolean vurderArbeidsforhold,
                                                                          UUID grunnlagId1, UUID grunnlagId2) {
        List<StartpunktType> startpunkter = new ArrayList<>();
        // Revurderinger skal normalt begynne i uttak.
        var defaultStartpunktForRegisterEndringer = BehandlingType.FØRSTEGANGSSØKNAD.equals(ref.getBehandlingType()) ? StartpunktType.OPPTJENING : StartpunktType.UDEFINERT;

        var grunnlag1 = iayTjeneste.hentGrunnlagPåId(ref.getBehandlingId(), grunnlagId1);
        var grunnlag2 = iayTjeneste.hentGrunnlagPåId(ref.getBehandlingId(), grunnlagId2);

        var skjæringstidspunkt = ref.getUtledetSkjæringstidspunkt();

        var iayGrunnlagDiff = new IAYGrunnlagDiff(grunnlag1, grunnlag2);
        var erAktørArbeidEndretForSøker = iayGrunnlagDiff.erEndringPåAktørArbeidForAktør(skjæringstidspunkt, ref.getAktørId());
        var erAktørInntektEndretForSøker = iayGrunnlagDiff.erEndringPåAktørInntektForAktør(skjæringstidspunkt, ref.getAktørId());
        var erInntektsmeldingEndret = iayGrunnlagDiff.erEndringPåInntektsmelding();

        var saksnummer = ref.getSaksnummer();
        var aktørYtelseEndringForSøker = iayGrunnlagDiff.endringPåAktørYtelseForAktør(saksnummer, skjæringstidspunkt, ref.getAktørId());

        if (vurderArbeidsforhold) {
            var skalTaStillingTilEndringerIArbeidsforhold = skalTaStillingTilEndringerIArbeidsforhold(ref);

            var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.getBehandlingId()); // TODO burde ikke være nødvendig (bør velge grunnlagId1, grunnlagId2)
            var sakInntektsmeldinger = skalTaStillingTilEndringerIArbeidsforhold ? iayTjeneste.hentInntektsmeldinger(ref.getSaksnummer()) : null /* ikke hent opp */;

            var erPåkrevdManuelleAvklaringer = !vurderArbeidsforholdTjeneste.vurder(ref, iayGrunnlag, sakInntektsmeldinger, skalTaStillingTilEndringerIArbeidsforhold).isEmpty();

            if (erPåkrevdManuelleAvklaringer) {
                leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.KONTROLLER_ARBEIDSFORHOLD, "manuell vurdering av arbeidsforhold");
            } else {
                ryddOppAksjonspunktHvisEksisterer(ref);
            }
        }
        if (erAktørArbeidEndretForSøker) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, defaultStartpunktForRegisterEndringer, "aktørarbeid");
        }
        if (aktørYtelseEndringForSøker.erEksklusiveYtelserEndret()) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.SØKERS_RELASJON_TIL_BARNET, "aktør ytelse");
        }
        if (aktørYtelseEndringForSøker.erAndreYtelserEndret()) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, defaultStartpunktForRegisterEndringer, "aktør ytelse andre tema");
        }
        if (erAktørInntektEndretForSøker) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, defaultStartpunktForRegisterEndringer, "aktør inntekt");
        }
        if (erInntektsmeldingEndret) {
            var startpunktIM = startpunktUtlederInntektsmelding.utledStartpunkt(ref, grunnlag1, grunnlag2);
            if (StartpunktType.KONTROLLER_ARBEIDSFORHOLD.equals(startpunktIM)) {
                arbeidsforholdAdministrasjonTjeneste.fjernOverstyringerGjortAvSaksbehandler(ref.getBehandlingId(), ref.getAktørId());
            }
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, startpunktIM, "inntektsmelding");
        }

        return startpunkter;
    }

    private boolean skalTaStillingTilEndringerIArbeidsforhold(BehandlingReferanse behandlingReferanse) {
        var behandling = behandlingRepository.hentBehandling(behandlingReferanse.getBehandlingId());
        return Objects.equals(behandlingReferanse.getBehandlingType(), BehandlingType.REVURDERING)
            || behandling.harSattStartpunkt();
    }

    /*
    Kontroller arbeidsforhold skal ikke lenger være aktiv hvis tilstanden i saken ikke tilsier det
    Setter dermed aksjonspunktet til utført hvis det står til opprettet.
     */
    private void ryddOppAksjonspunktHvisEksisterer(BehandlingReferanse behandlingReferanse) {
        var behandling = behandlingRepository.hentBehandling(behandlingReferanse.getId());
        var aksjonspunkter = behandling.getAksjonspunkter().stream()
            .filter(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD))
            .filter(Aksjonspunkt::erÅpentAksjonspunkt)
            .collect(Collectors.toList());

        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(), aksjonspunkter);
    }

    private void leggTilStartpunkt(List<StartpunktType> startpunkter, UUID grunnlagId1, UUID grunnlagId2, StartpunktType startpunkt, String endringLoggtekst) {
        startpunkter.add(startpunkt);
        FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(klassenavn, startpunkt, endringLoggtekst, grunnlagId1, grunnlagId2);
    }

}
