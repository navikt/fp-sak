package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.HåndterePermisjoner;
import no.nav.foreldrepenger.domene.arbeidsforhold.IAYGrunnlagDiff;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdInntektsmeldingToggleTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.SakInntektsmeldinger;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;

@ApplicationScoped
@GrunnlagRef(GrunnlagRef.IAY_GRUNNLAG)
class StartpunktUtlederInntektArbeidYtelse implements StartpunktUtleder {

    private String klassenavn = this.getClass().getSimpleName();
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private StartpunktUtlederInntektsmelding startpunktUtlederInntektsmelding;
    private VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste; // Denne bør kunne ryddes bort når saker ikke lenger stopper i 5080
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;
    private static final Logger LOG = LoggerFactory.getLogger(StartpunktUtlederInntektArbeidYtelse.class);

    public StartpunktUtlederInntektArbeidYtelse() {
        // For CDI
    }

    @Inject
    StartpunktUtlederInntektArbeidYtelse(InntektArbeidYtelseTjeneste iayTjeneste,
                                         // NOSONAR - ingen enkel måte å unngå mange parametere her
                                         BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                         BehandlingRepositoryProvider repositoryProvider,
                                         StartpunktUtlederInntektsmelding startpunktUtlederInntektsmelding,
                                         VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste,
                                         ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste,
                                         ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.startpunktUtlederInntektsmelding = startpunktUtlederInntektsmelding;
        this.vurderArbeidsforholdTjeneste = vurderArbeidsforholdTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.arbeidsforholdAdministrasjonTjeneste = arbeidsforholdAdministrasjonTjeneste;
        this.arbeidsforholdInntektsmeldingMangelTjeneste = arbeidsforholdInntektsmeldingMangelTjeneste;
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
        var defaultStartpunktForRegisterEndringer = BehandlingType.FØRSTEGANGSSØKNAD.equals(ref.behandlingType()) ? StartpunktType.OPPTJENING : StartpunktType.UDEFINERT;

        var grunnlag1 = iayTjeneste.hentGrunnlagPåId(ref.behandlingId(), grunnlagId1);
        var grunnlag2 = iayTjeneste.hentGrunnlagPåId(ref.behandlingId(), grunnlagId2);

        var skjæringstidspunkt = ref.getUtledetSkjæringstidspunkt();

        var iayGrunnlagDiff = new IAYGrunnlagDiff(grunnlag1, grunnlag2);
        var erAktørArbeidEndretForSøker = iayGrunnlagDiff.erEndringPåAktørArbeidForAktør(skjæringstidspunkt, ref.aktørId());
        var erAktørInntektEndretForSøker = iayGrunnlagDiff.erEndringPåAktørInntektForAktør(skjæringstidspunkt, ref.aktørId());
        var erInntektsmeldingEndret = iayGrunnlagDiff.erEndringPåInntektsmelding();

        var saksnummer = ref.saksnummer();
        var aktørYtelseEndringForSøker = iayGrunnlagDiff.endringPåAktørYtelseForAktør(saksnummer, skjæringstidspunkt, ref.aktørId());

        if (vurderArbeidsforhold) {
            var skalTaStillingTilEndringerIArbeidsforhold = skalTaStillingTilEndringerIArbeidsforhold(ref);

            var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.behandlingId()); // TODO burde ikke være nødvendig (bør velge grunnlagId1, grunnlagId2)
            var sakInntektsmeldinger = skalTaStillingTilEndringerIArbeidsforhold ? iayTjeneste.hentInntektsmeldinger(ref.saksnummer()) : null /* ikke hent opp */;

            //Må rydde opp eventuelle tidligere aksjonspunkt
            var erPåkrevdManuelleAvklaringer = trengsManuelleAvklaringer(ref, skalTaStillingTilEndringerIArbeidsforhold, iayGrunnlag,
                sakInntektsmeldinger);
            var måVurderePermisjonUtenSluttdato = sjekkOmMåVurderePermisjonerUtenSluttdato(ref, iayGrunnlag);

            if (erPåkrevdManuelleAvklaringer) {
                leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.KONTROLLER_ARBEIDSFORHOLD, "manuell vurdering av arbeidsforhold");
            } else {
                ryddOppAksjonspunktForInntektsmeldingHvisEksisterer(ref);
            }
            if (måVurderePermisjonUtenSluttdato){
                loggAdvarselHvisAksjonspunktForPermisjonUtenSluttdatIkkeEksisterer(ref);
            } else {
                ryddOppAksjonspunktForPermisjonUtenSluttdatoHvisEksisterer(ref);
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
                arbeidsforholdAdministrasjonTjeneste.fjernOverstyringerGjortAvSaksbehandler(ref.behandlingId(), ref.aktørId());
            }
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, startpunktIM, "inntektsmelding");
        }

        return startpunkter;
    }

    private boolean trengsManuelleAvklaringer(BehandlingReferanse ref,
                                                   boolean skalTaStillingTilEndringerIArbeidsforhold,
                                                   InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                   SakInntektsmeldinger sakInntektsmeldinger) {
        var trengerAvklaringI5080 = !vurderArbeidsforholdTjeneste.vurder(ref, iayGrunnlag, sakInntektsmeldinger, skalTaStillingTilEndringerIArbeidsforhold).isEmpty();
        var trengerAvklaringI5085 = ArbeidsforholdInntektsmeldingToggleTjeneste.erTogglePå() &&
        !arbeidsforholdInntektsmeldingMangelTjeneste.utledManglerPåArbeidsforholdInntektsmelding(ref).isEmpty();
        return trengerAvklaringI5080 || trengerAvklaringI5085;
    }

    private boolean sjekkOmMåVurderePermisjonerUtenSluttdato(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        return !HåndterePermisjoner.finnArbForholdMedPermisjonUtenSluttdatoMangel(ref, inntektArbeidYtelseGrunnlag).isEmpty();
    }

    private boolean skalTaStillingTilEndringerIArbeidsforhold(BehandlingReferanse behandlingReferanse) {
        var behandling = behandlingRepository.hentBehandling(behandlingReferanse.behandlingId());
        return Objects.equals(behandlingReferanse.behandlingType(), BehandlingType.REVURDERING)
            || behandling.harSattStartpunkt();
    }

    /*
    Kontroller arbeidsforhold skal ikke lenger være aktiv hvis tilstanden i saken ikke tilsier det
    Setter dermed aksjonspunktet til utført hvis det står til opprettet.
     */
    private void ryddOppAksjonspunktForInntektsmeldingHvisEksisterer(BehandlingReferanse behandlingReferanse) {
        var behandling = behandlingRepository.hentBehandling(behandlingReferanse.behandlingId());
        var aksjonspunkter = behandling.getAksjonspunkter().stream()
            .filter(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD)
                || ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING))
            .filter(Aksjonspunkt::erÅpentAksjonspunkt)
            .collect(Collectors.toList());

        avbrytAksjonspunkter(behandling, aksjonspunkter);
    }

    private void ryddOppAksjonspunktForPermisjonUtenSluttdatoHvisEksisterer(BehandlingReferanse behandlingReferanse) {
        var behandling = behandlingRepository.hentBehandling(behandlingReferanse.behandlingId());
        var aksjonspunkter = behandling.getAksjonspunkter().stream()
            .filter(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDER_PERMISJON_UTEN_SLUTTDATO))
            .filter(Aksjonspunkt::erÅpentAksjonspunkt)
            .collect(Collectors.toList());

        avbrytAksjonspunkter(behandling, aksjonspunkter);
    }

    private void avbrytAksjonspunkter(Behandling behandling, List<Aksjonspunkt> aksjonspunkter) {
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(), aksjonspunkter);
    }

    //sjekker om dette scenarioet i det hele tatt oppstår før vi eventuelt legger inn håndtering av det
    private void loggAdvarselHvisAksjonspunktForPermisjonUtenSluttdatIkkeEksisterer(BehandlingReferanse behandlingReferanse) {
        var behandling = behandlingRepository.hentBehandling(behandlingReferanse.behandlingId());
        if (!behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_PERMISJON_UTEN_SLUTTDATO)) {
            LOG.warn("BehandlingId {} har arbeidsforhold med permisjon uten sluttdato, men har ikke åpent aksjonspunkt av type VURDER_PERMISJON_UTEN_SLUTTDATO", behandling.getId());
        }
    }

    private void leggTilStartpunkt(List<StartpunktType> startpunkter, UUID grunnlagId1, UUID grunnlagId2, StartpunktType startpunkt, String endringLoggtekst) {
        startpunkter.add(startpunkt);
        FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(klassenavn, startpunkt, endringLoggtekst, grunnlagId1, grunnlagId2);
    }

}
