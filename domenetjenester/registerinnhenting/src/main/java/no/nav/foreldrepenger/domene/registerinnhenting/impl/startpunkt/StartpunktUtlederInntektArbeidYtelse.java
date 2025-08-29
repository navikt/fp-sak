package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktkontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.HåndterePermisjoner;
import no.nav.foreldrepenger.domene.arbeidsforhold.IAYGrunnlagDiff;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;

@ApplicationScoped
@GrunnlagRef(GrunnlagRef.IAY_GRUNNLAG)
class StartpunktUtlederInntektArbeidYtelse implements StartpunktUtleder {

    private String klassenavn = this.getClass().getSimpleName();
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private StartpunktUtlederInntektsmelding startpunktUtlederInntektsmelding;
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste;
    private BehandlingRepository behandlingRepository;
    private AksjonspunktkontrollTjeneste aksjonspunktkontrollTjeneste;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;

    public StartpunktUtlederInntektArbeidYtelse() {
        // For CDI
    }

    @Inject
    StartpunktUtlederInntektArbeidYtelse(InntektArbeidYtelseTjeneste iayTjeneste,
                                         AksjonspunktkontrollTjeneste aksjonspunktkontrollTjeneste,
                                         BehandlingRepositoryProvider repositoryProvider,
                                         StartpunktUtlederInntektsmelding startpunktUtlederInntektsmelding,
                                         ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste,
                                         ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.startpunktUtlederInntektsmelding = startpunktUtlederInntektsmelding;
        this.aksjonspunktkontrollTjeneste = aksjonspunktkontrollTjeneste;
        this.arbeidsforholdAdministrasjonTjeneste = arbeidsforholdAdministrasjonTjeneste;
        this.arbeidsforholdInntektsmeldingMangelTjeneste = arbeidsforholdInntektsmeldingMangelTjeneste;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Skjæringstidspunkt stp, Object grunnlagId1, Object grunnlagId2) {
        return hentAlleStartpunktForInntektArbeidYtelse(ref, stp, true, (UUID) grunnlagId1, (UUID) grunnlagId2).stream()
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    @Override
    public Set<StartpunktType> utledInitieltStartpunktRevurdering(BehandlingReferanse ref, Skjæringstidspunkt stp, Object grunnlagId1, Object grunnlagId2) {
        return new HashSet<>(hentAlleStartpunktForInntektArbeidYtelse(ref, stp, false, (UUID) grunnlagId1, (UUID) grunnlagId2));
    }

    private List<StartpunktType> hentAlleStartpunktForInntektArbeidYtelse(BehandlingReferanse ref, Skjæringstidspunkt stp, boolean vurderArbeidsforhold,
                                                                          UUID grunnlagId1, UUID grunnlagId2) {
        List<StartpunktType> startpunkter = new ArrayList<>();
        // Revurderinger skal normalt begynne i uttak.
        var defaultStartpunktForRegisterEndringer = BehandlingType.FØRSTEGANGSSØKNAD.equals(ref.behandlingType()) ? StartpunktType.OPPTJENING : StartpunktType.UDEFINERT;

        var grunnlag1 = iayTjeneste.hentGrunnlagPåId(ref.behandlingId(), grunnlagId1);
        var grunnlag2 = iayTjeneste.hentGrunnlagPåId(ref.behandlingId(), grunnlagId2);

        var skjæringstidspunkt = stp.getUtledetSkjæringstidspunkt();

        var iayGrunnlagDiff = new IAYGrunnlagDiff(grunnlag1, grunnlag2);
        var erAktørArbeidEndretForSøker = iayGrunnlagDiff.erEndringPåAktørArbeidForAktør(skjæringstidspunkt, ref.aktørId());
        var erAktørInntektEndretForSøker = iayGrunnlagDiff.erEndringPåAktørInntektForAktør(skjæringstidspunkt, ref.aktørId());
        var erInntektsmeldingEndret = iayGrunnlagDiff.erEndringPåInntektsmelding();
        var erOppgittOptjeningEndret = iayGrunnlagDiff.erEndringPåOppgittOpptjening();

        var saksnummer = ref.saksnummer();

        // Endringer før STP, eller rundt STP der man har søkt både ES og FP
        var aktørYtelseEndringForSøker = iayGrunnlagDiff.endringPåAktørYtelseForAktør(saksnummer, skjæringstidspunkt, ref.aktørId());

        // Endringer etter STP for ytelser der vi normalt skal vike (innvilge utsettelse)
        var erEndringPleiepengerEtterStp = iayGrunnlagDiff.erEndringPleiepengerEtterStp(skjæringstidspunkt, ref.aktørId());

        if (vurderArbeidsforhold) {
            var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.behandlingId()); // TODO burde ikke være nødvendig (bør velge grunnlagId1, grunnlagId2)

            //Må rydde opp eventuelle tidligere aksjonspunkt
            var erPåkrevdManuelleAvklaringer = trengsManuelleAvklaringer(ref, stp);
            var måVurderePermisjonUtenSluttdato = sjekkOmMåVurderePermisjonerUtenSluttdato(ref, stp, iayGrunnlag);

            if (erPåkrevdManuelleAvklaringer) {
                leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.KONTROLLER_ARBEIDSFORHOLD, "manuell vurdering av arbeidsforhold");
            } else {
                ryddOppAksjonspunktForInntektsmeldingHvisEksisterer(ref);
            }
            if (måVurderePermisjonUtenSluttdato && !erPåkrevdManuelleAvklaringer){
                leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.KONTROLLER_ARBEIDSFORHOLD, "manuell vurdering av arbeidsforhold pga permisjon");
            } else {
                ryddOppAksjonspunktForPermisjonUtenSluttdatoHvisEksisterer(ref);
            }
        }
        if (erInntektsmeldingEndret) {
            var startpunktIM = startpunktUtlederInntektsmelding.utledStartpunkt(ref, stp, grunnlag1, grunnlag2);
            if (StartpunktType.KONTROLLER_ARBEIDSFORHOLD.equals(startpunktIM)) {
                arbeidsforholdAdministrasjonTjeneste.fjernOverstyringerGjortAvSaksbehandler(ref.behandlingId());
            }
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, startpunktIM, "inntektsmelding");
        }
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ref.fagsakYtelseType())) {
            return startpunkter;
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
        if (erEndringPleiepengerEtterStp) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.UTTAKSVILKÅR, "pleiepenger under uttaket");
        }
        if (erAktørInntektEndretForSøker) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, defaultStartpunktForRegisterEndringer, "aktør inntekt");
        }
        if (erOppgittOptjeningEndret) {
            leggTilStartpunkt(startpunkter, grunnlagId1, grunnlagId2, StartpunktType.OPPTJENING, "oppgitt opptjening");
        }

        return startpunkter;
    }

    private boolean trengsManuelleAvklaringer(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        return !arbeidsforholdInntektsmeldingMangelTjeneste.utledUavklarteManglerPåArbeidsforholdInntektsmelding(ref, stp).isEmpty();
    }

    private boolean sjekkOmMåVurderePermisjonerUtenSluttdato(BehandlingReferanse ref, Skjæringstidspunkt stp, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        return !HåndterePermisjoner.finnArbForholdMedPermisjonUtenSluttdatoMangel(ref, stp.getUtledetSkjæringstidspunkt(), inntektArbeidYtelseGrunnlag).isEmpty();
    }

    /*
    Kontroller arbeidsforhold skal ikke lenger være aktiv hvis tilstanden i saken ikke tilsier det
    Setter dermed aksjonspunktet til utført hvis det står til opprettet.
     */
    private void ryddOppAksjonspunktForInntektsmeldingHvisEksisterer(BehandlingReferanse behandlingReferanse) {
        var skriveLås = behandlingRepository.taSkriveLås(behandlingReferanse.behandlingId());
        var behandling = behandlingRepository.hentBehandling(behandlingReferanse.behandlingId());
        var aksjonspunkter = behandling.getAksjonspunkter().stream()
            .filter(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING))
            .filter(Aksjonspunkt::erÅpentAksjonspunkt)
            .toList();

        avbrytAksjonspunkter(behandling, skriveLås, aksjonspunkter);
    }

    private void ryddOppAksjonspunktForPermisjonUtenSluttdatoHvisEksisterer(BehandlingReferanse behandlingReferanse) {
        var skriveLås = behandlingRepository.taSkriveLås(behandlingReferanse.behandlingId());
        var behandling = behandlingRepository.hentBehandling(behandlingReferanse.behandlingId());
        var aksjonspunkter = behandling.getAksjonspunkter().stream()
            .filter(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDER_PERMISJON_UTEN_SLUTTDATO))
            .filter(Aksjonspunkt::erÅpentAksjonspunkt)
            .toList();

        avbrytAksjonspunkter(behandling, skriveLås, aksjonspunkter);
    }

    private void avbrytAksjonspunkter(Behandling behandling, BehandlingLås skriveLås, List<Aksjonspunkt> aksjonspunkter) {
        aksjonspunktkontrollTjeneste.lagreAksjonspunkterAvbrutt(behandling, skriveLås, aksjonspunkter);
    }

    private void leggTilStartpunkt(List<StartpunktType> startpunkter, UUID grunnlagId1, UUID grunnlagId2, StartpunktType startpunkt, String endringLoggtekst) {
        startpunkter.add(startpunkt);
        FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(klassenavn, startpunkt, endringLoggtekst, grunnlagId1, grunnlagId2);
    }

}
