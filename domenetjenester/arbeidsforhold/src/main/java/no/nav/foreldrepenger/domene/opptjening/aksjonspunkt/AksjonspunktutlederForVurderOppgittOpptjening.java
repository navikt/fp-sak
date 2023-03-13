package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
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
public class AksjonspunktutlederForVurderOppgittOpptjening implements AksjonspunktUtleder {

    private static final List<AksjonspunktResultat> INGEN_AKSJONSPUNKTER = emptyList();
    private static final Logger LOG = LoggerFactory.getLogger(AksjonspunktutlederForVurderOppgittOpptjening.class);

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

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {

        var behandlingId = param.getBehandlingId();

        var inntektArbeidYtelseGrunnlagOptional = iayTjeneste.finnGrunnlag(behandlingId);
        var fastsattOpptjeningOptional = opptjeningRepository.finnOpptjening(behandlingId);
        if (inntektArbeidYtelseGrunnlagOptional.isEmpty() || fastsattOpptjeningOptional.isEmpty()) {
            return INGEN_AKSJONSPUNKTER;
        }
        var oppgittOpptjening = inntektArbeidYtelseGrunnlagOptional.get().getOppgittOpptjening().orElse(null);
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
            if (manglerFerdiglignetNæringsinntekt(aktørId, oppgittOpptjening, inntektArbeidYtelseGrunnlagOptional.get(), opptjeningPeriode,
                    param.getSkjæringstidspunkt()) == JA) {
                LOG.info("Utleder AP 5051 fra oppgitt næringsdrift");
                return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
            }
        }
        return INGEN_AKSJONSPUNKTER;
    }

    private List<ArbeidType> finnRelevanteKoder() {
        return List.of(ArbeidType.values())
                .stream()
                .filter(ArbeidType::erAnnenOpptjening)
                .collect(Collectors.toList());
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
            InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag, DatoIntervallEntitet opptjeningPeriode,
            Skjæringstidspunkt skjæringstidspunkt) {
        // Det siste ferdiglignede år vil alltid være året før behandlingstidspunktet
        // Bruker LocalDate.now() her etter avklaring med funksjonell.
        var sistFerdiglignetÅr = LocalDate.now().minusYears(1L).getYear();
        if (inneholderSisteFerdiglignendeÅrNæringsinntekt(aktørId, inntektArbeidYtelseGrunnlag, sistFerdiglignetÅr, opptjeningPeriode,
                skjæringstidspunkt) == NEI) {
            if (erDetRegistrertNæringEtterSisteFerdiglignendeÅr(oppgittOpptjening, sistFerdiglignetÅr) == NEI) {
                return JA;
            }
        }
        return NEI;
    }

    private Utfall inneholderSisteFerdiglignendeÅrNæringsinntekt(AktørId aktørId,
            InntektArbeidYtelseGrunnlag grunnlag,
            int sistFerdiglignetÅr,
            DatoIntervallEntitet opptjeningPeriode,
            Skjæringstidspunkt skjæringstidspunkt) {
        var stp = skjæringstidspunkt.getUtledetSkjæringstidspunkt();
        var filter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId));
        if (filter.isEmpty()) {
            return NEI;
        }

        var stpFilter = (opptjeningPeriode.getTomDato().getYear() >= sistFerdiglignetÅr) ? filter.før(stp) : filter.etter(stp);

        return stpFilter.filterBeregnetSkatt().filter(InntektspostType.SELVSTENDIG_NÆRINGSDRIVENDE, InntektspostType.NÆRING_FISKE_FANGST_FAMBARNEHAGE)
                .anyMatchFilter(harInntektI(sistFerdiglignetÅr)) ? JA : NEI;
    }

    private BiPredicate<Inntekt, Inntektspost> harInntektI(int sistFerdiglignetÅr) {
        return (inntekt, inntektspost) -> (inntektspost.getPeriode().getTomDato().getYear() == sistFerdiglignetÅr) &&
                (inntektspost.getBeløp().getVerdi().compareTo(BigDecimal.ZERO) != 0);
    }

    private Utfall erDetRegistrertNæringEtterSisteFerdiglignendeÅr(OppgittOpptjening oppgittOpptjening, int sistFerdiglignetÅr) {
        if (oppgittOpptjening == null) {
            return NEI;
        }

        return oppgittOpptjening.getEgenNæring().stream()
                .anyMatch(egenNæring -> erRegistrertNæring(egenNæring, sistFerdiglignetÅr)) ? JA : NEI;
    }

    private boolean erRegistrertNæring(OppgittEgenNæring eg, int sistFerdiglignetÅr) {
        if (eg.getOrgnr() == null) {
            return false;
        }
        var lagretVirksomhet = virksomhetTjeneste.finnOrganisasjon(eg.getOrgnr());
        if (lagretVirksomhet.isPresent()) {
            return lagretVirksomhet.get().getRegistrert().getYear() > sistFerdiglignetÅr;
        }
        // Virksomhetsinformasjonen er ikke hentet, henter vi den fra ereg. Innført
        // etter feil som oppstod i https://jira.adeo.no/browse/TFP-1484
        var hentetVirksomhet = virksomhetTjeneste.hentOrganisasjon(eg.getOrgnr());
        return hentetVirksomhet.getRegistrert().getYear() > sistFerdiglignetÅr;
    }

    boolean girAksjonspunktForOppgittNæring(Long behandlingId, AktørId aktørId, InntektArbeidYtelseGrunnlag iayg,
            Skjæringstidspunkt skjæringstidspunkt) {
        var fastsattOpptjeningOptional = opptjeningRepository.finnOpptjening(behandlingId);
        if (fastsattOpptjeningOptional.isEmpty()) {
            return false;
        }
        var oppgittOpptjening = iayg.getOppgittOpptjening().orElse(null);
        var opptjeningPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fastsattOpptjeningOptional.get().getFom(),
                fastsattOpptjeningOptional.get().getTom());

        return (harBrukerOppgittÅVæreSelvstendigNæringsdrivende(oppgittOpptjening, opptjeningPeriode) == JA) &&
                (manglerFerdiglignetNæringsinntekt(aktørId, oppgittOpptjening, iayg, opptjeningPeriode,
                        skjæringstidspunkt) == JA);
    }
}
