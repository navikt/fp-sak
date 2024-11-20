package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntekt;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.OppgittEgenNæring;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ApplicationScoped
public class AksjonspunktutlederForVurderOppgittOpptjening {

    private static final List<AksjonspunktResultat> INGEN_AKSJONSPUNKTER = emptyList();
    private static final Logger LOG = LoggerFactory.getLogger(AksjonspunktutlederForVurderOppgittOpptjening.class);
    private static final int FORVENTET_FERDIGLIGNET_MÅNED = 7; // Analyse viser økning i ferdiglignet næring i juli.

    private OpptjeningRepository opptjeningRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private VirksomhetTjeneste virksomhetTjeneste;

    AksjonspunktutlederForVurderOppgittOpptjening() {
        // CDI
    }

    @Inject
    public AksjonspunktutlederForVurderOppgittOpptjening(OpptjeningRepository opptjeningRepository,
            InntektArbeidYtelseTjeneste iayTjeneste,
            VirksomhetTjeneste virksomhetTjeneste) {
        this.opptjeningRepository = opptjeningRepository;
        this.iayTjeneste = iayTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
    }

    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {

        var behandlingId = param.getBehandlingId();

        var inntektArbeidYtelseGrunnlagOptional = iayTjeneste.finnGrunnlag(behandlingId);
        var fastsattOpptjeningOptional = opptjeningRepository.finnOpptjening(behandlingId);
        if (inntektArbeidYtelseGrunnlagOptional.isEmpty() || fastsattOpptjeningOptional.isEmpty()) {
            return INGEN_AKSJONSPUNKTER;
        }
        var oppgittOpptjening = inntektArbeidYtelseGrunnlagOptional.get().getGjeldendeOppgittOpptjening().orElse(null);
        var opptjeningPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fastsattOpptjeningOptional.get().getFom(),
                fastsattOpptjeningOptional.get().getTom());

        if (harBrukerOppgittPerioderMed(oppgittOpptjening, opptjeningPeriode, Collections.singletonList(ArbeidType.FRILANSER)) == JA) {
            LOG.info("Utleder AP 5051 fra oppgitt eller bekreftet frilans: behandlingId={}", behandlingId);
            return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
        }

        if (harBrukerOppgittPerioderMed(oppgittOpptjening, opptjeningPeriode, finnRelevanteKoder()) == JA) {
            LOG.info("Utleder AP 5051 fra oppgitt opptjening");
            return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
        }

        if (harBrukerOppgittArbeidsforholdMed(ArbeidType.UTENLANDSK_ARBEIDSFORHOLD, opptjeningPeriode, oppgittOpptjening) == JA) {
            LOG.info("Utleder AP 5051 fra utlandsk arbeidsforhold");
            return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
        }

        if (harBrukerOppgittÅVæreSelvstendigNæringsdrivende(oppgittOpptjening, opptjeningPeriode) == JA) {
            var aktørId = param.getAktørId();
            if (manglerFerdiglignetNæringsinntekt(aktørId, oppgittOpptjening, inntektArbeidYtelseGrunnlagOptional.get(), param.getSkjæringstidspunkt()) == JA) {
                LOG.info("Utleder AP 5051 fra oppgitt næringsdrift");
                return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
            }
        }
        return INGEN_AKSJONSPUNKTER;
    }

    private List<ArbeidType> finnRelevanteKoder() {
        return Stream.of(ArbeidType.values())
                .filter(ArbeidType::erAnnenOpptjening)
                .toList();
    }

    private Utfall harBrukerOppgittArbeidsforholdMed(ArbeidType annenOpptjeningType, DatoIntervallEntitet opptjeningPeriode,
            OppgittOpptjening oppgittOpptjening) {
        if (oppgittOpptjening == null) {
            return NEI;
        }

        for (var oppgittArbeidsforhold : oppgittOpptjening.getOppgittArbeidsforhold()) {
            if (oppgittArbeidsforhold.getArbeidType().equals(annenOpptjeningType)
                    && opptjeningPeriode.overlapper(oppgittArbeidsforhold.getPeriode())) {
                return JA;
            }
        }
        return NEI;
    }

    private Utfall harBrukerOppgittPerioderMed(OppgittOpptjening oppgittOpptjening, DatoIntervallEntitet opptjeningPeriode,
            List<ArbeidType> annenOpptjeningType) {
        if (oppgittOpptjening == null) {
            return NEI;
        }

        for (var annenAktivitet : oppgittOpptjening.getAnnenAktivitet()) {
            if (annenOpptjeningType.contains(annenAktivitet.getArbeidType()) && opptjeningPeriode.overlapper(annenAktivitet.getPeriode())) {
                return JA;
            }
        }
        return NEI;
    }

    private Utfall harBrukerOppgittÅVæreSelvstendigNæringsdrivende(OppgittOpptjening oppgittOpptjening, DatoIntervallEntitet opptjeningPeriode) {
        if (oppgittOpptjening == null) {
            return NEI;
        }

        for (var egenNæring : oppgittOpptjening.getEgenNæring()) {
            if (opptjeningPeriode.overlapper(egenNæring.getPeriode())) {
                return JA;
            }
        }
        return NEI;
    }

    private Utfall manglerFerdiglignetNæringsinntekt(AktørId aktørId, OppgittOpptjening oppgittOpptjening,
            InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag, Skjæringstidspunkt skjæringstidspunkt) {
        // Før forventningsdato er kun et fåtall ferdiglignet og mans sjekker 2 år tilbake, deretter sjekker man kun fjoråret eller stp-året dersom lenge etter
        var stp = skjæringstidspunkt.getUtledetSkjæringstidspunkt();
        var stpFerdiglignetFrist = stp.plusYears(1).withMonth(FORVENTET_FERDIGLIGNET_MÅNED).withDayOfMonth(1).plusMonths(3);
        Set<Integer> forventetFerdiglignet = new LinkedHashSet<>();
        if (LocalDate.now().isAfter(stpFerdiglignetFrist)) {
            forventetFerdiglignet.add(stp.getYear());
        } else {
            if (LocalDate.now().isBefore(stp.withMonth(FORVENTET_FERDIGLIGNET_MÅNED).withDayOfMonth(1))) {
                forventetFerdiglignet.add(stp.minusYears(2).getYear());
            }
            forventetFerdiglignet.add(stp.minusYears(1).getYear());
        }

        return inneholderSisteFerdiglignendeÅrNæringsinntekt(aktørId, inntektArbeidYtelseGrunnlag, forventetFerdiglignet, skjæringstidspunkt) == NEI
            && erDetRegistrertNæringEtterSisteFerdiglignendeÅr(oppgittOpptjening, forventetFerdiglignet) == NEI ? JA : NEI;
    }

    private Utfall inneholderSisteFerdiglignendeÅrNæringsinntekt(AktørId aktørId,
            InntektArbeidYtelseGrunnlag grunnlag,
            Set<Integer> forventetFerdiglignet, Skjæringstidspunkt skjæringstidspunkt) {
        var stp = skjæringstidspunkt.getUtledetSkjæringstidspunkt();
        var filter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId));
        if (filter.isEmpty()) {
            return NEI;
        }

        return filter.før(stp).filterBeregnetSkatt().filter(InntektspostType.SELVSTENDIG_NÆRINGSDRIVENDE, InntektspostType.NÆRING_FISKE_FANGST_FAMBARNEHAGE)
                .anyMatchFilter(harInntektI(forventetFerdiglignet)) ? JA : NEI;
    }

    private BiPredicate<Inntekt, Inntektspost> harInntektI(Set<Integer> forventetFerdiglignet) {
        return (inntekt, inntektspost) -> forventetFerdiglignet.contains(inntektspost.getPeriode().getTomDato().getYear())
            && inntektspost.getBeløp().getVerdi().compareTo(BigDecimal.ZERO) != 0;
    }

    private Utfall erDetRegistrertNæringEtterSisteFerdiglignendeÅr(OppgittOpptjening oppgittOpptjening, Set<Integer> forventetFerdiglignet) {
        if (oppgittOpptjening == null) {
            return NEI;
        }

        var registrertEtter = forventetFerdiglignet.stream().max(Comparator.naturalOrder()).orElseThrow();
        return oppgittOpptjening.getEgenNæring().stream()
                .anyMatch(egenNæring -> erRegistrertNæring(egenNæring, registrertEtter)) ? JA : NEI;
    }

    private boolean erRegistrertNæring(OppgittEgenNæring eg, int sistFerdiglignetÅr) {
        if (eg.getOrgnr() == null) {
            return false;
        }
        // Virksomhetsinformasjonen er ikke hentet, henter vi den fra ereg.
        // Innført etter feil som oppstod i https://jira.adeo.no/browse/TFP-1484
        var virksomhet = virksomhetTjeneste.finnOrganisasjon(eg.getOrgnr())
            .orElseGet(() -> virksomhetTjeneste.hentOrganisasjon(eg.getOrgnr()));
        return virksomhet.getRegistrert().getYear() > sistFerdiglignetÅr;
    }

    boolean girAksjonspunktForOppgittNæring(Long behandlingId, AktørId aktørId, InntektArbeidYtelseGrunnlag iayg,
            Skjæringstidspunkt skjæringstidspunkt) {
        var fastsattOpptjeningOptional = opptjeningRepository.finnOpptjening(behandlingId);
        if (fastsattOpptjeningOptional.isEmpty()) {
            return false;
        }
        var oppgittOpptjening = iayg.getGjeldendeOppgittOpptjening().orElse(null);
        var opptjeningPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fastsattOpptjeningOptional.get().getFom(),
            fastsattOpptjeningOptional.get().getTom());

        return harBrukerOppgittÅVæreSelvstendigNæringsdrivende(oppgittOpptjening, opptjeningPeriode) == JA
            && manglerFerdiglignetNæringsinntekt(aktørId, oppgittOpptjening, iayg, skjæringstidspunkt) == JA;
    }
}
