package no.nav.foreldrepenger.domene.arbeidsforhold;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.Ambasade;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.FORENKLET_OPPGJØRSORDNING;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.MARITIMT_ARBEIDSFORHOLD;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.ORDINÆRT_ARBEIDSFORHOLD;

public class InntektsmeldingUtenArbeidsforholdTjeneste {
    private static final Integer MND_FØR_STP_INNTEKT_ER_RELEVANT = 6;
    private static final Set<ArbeidType> ARBEIDSFORHOLD_TYPER = Stream.of(ORDINÆRT_ARBEIDSFORHOLD,
            FORENKLET_OPPGJØRSORDNING, MARITIMT_ARBEIDSFORHOLD)
        .collect(Collectors.toSet());

    private InntektsmeldingUtenArbeidsforholdTjeneste() {
        // Skjuler default konstrukør
    }

    public static Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> utledManglendeArbeidsforhold(InntektArbeidYtelseGrunnlag grunnlag,
                                                                                               AktørId aktørId, LocalDate utledetStp) {
        final var inntektsmeldinger = grunnlag.getInntektsmeldinger();
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> resultat = new HashMap<>();
        if (inntektsmeldinger.isPresent()) {
            final var aggregat = inntektsmeldinger.get();
            for (var inntektsmelding : aggregat.getInntektsmeldingerSomSkalBrukes()) {
                if (måVurderes(grunnlag, inntektsmelding, aktørId, utledetStp)) {
                    final var arbeidsgiver = inntektsmelding.getArbeidsgiver();
                    final var arbeidsforholdRefs = trekkUtRef(inntektsmelding);
                    resultat.put(arbeidsgiver, arbeidsforholdRefs);
                }
            }
        }
        return resultat;
    }

    private static boolean måVurderes(InntektArbeidYtelseGrunnlag grunnlag,
                                      Inntektsmelding inntektsmelding,
                                      AktørId aktørId,
                                      LocalDate utledetStp) {
        var erRegistrertSomFrilans = gjelderFrilans(aktørId, grunnlag, inntektsmelding);
        if (erRegistrertSomFrilans) {
            // Arbeidsgiver sender av og til inntektsmeldinger på frilansforhold.
            // Dette er ikke riktig praksis, og saksbehandlingen ignorerer disse så vi trenger ikke lage arbeidsforhold på de.
            return false;
        }
        var harRapportertInntektHosArbeidsgiver = harRapportertInntekt(new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId)), utledetStp, inntektsmelding.getArbeidsgiver());
        var harIngenArbeidsforholdHosArbeidsgiver = !harArbeidsforholdIRegistreHosArbeidsgiver(aktørId, grunnlag, inntektsmelding.getArbeidsgiver());
        var erAmbasadeUtenArbeidsforhold = erArbeidsgiverAmbasade(inntektsmelding.getArbeidsgiver()) && harIngenArbeidsforholdHosArbeidsgiver;
        var finnesInntektUtenArbeidsforhold = harRapportertInntektHosArbeidsgiver && harIngenArbeidsforholdHosArbeidsgiver;
        return finnesInntektUtenArbeidsforhold || erFiskerUtenAktivtArbeid(aktørId, utledetStp, grunnlag, inntektsmelding) || erAmbasadeUtenArbeidsforhold;
    }

    private static boolean erArbeidsgiverAmbasade(Arbeidsgiver arbeidsgiver) {
        return arbeidsgiver != null && arbeidsgiver.getErVirksomhet() && Ambasade.erAmbasade(arbeidsgiver.getOrgnr());
    }

    private static boolean gjelderFrilans(AktørId aktørId, InntektArbeidYtelseGrunnlag grunnlag, Inntektsmelding inntektsmelding) {
        var filter = new YrkesaktivitetFilter(grunnlag.getAktørArbeidFraRegister(aktørId)
            .map(AktørArbeid::hentAlleYrkesaktiviteter)
            .orElse(Collections.emptyList()));
        return filter.getFrilansOppdrag().stream()
            .anyMatch(ya -> ya.gjelderFor(inntektsmelding.getArbeidsgiver(), inntektsmelding.getArbeidsforholdRef()));
    }

    private static boolean harRapportertInntekt(InntektFilter inntektFilter, LocalDate utledetStp, Arbeidsgiver arbeidsgiver) {
        var periodeViSerEtterInntekt = DatoIntervallEntitet.fraOgMedTilOgMed(utledetStp.minusMonths(MND_FØR_STP_INNTEKT_ER_RELEVANT).withDayOfMonth(1), utledetStp);
        return inntektFilter.getAlleInntektBeregningsgrunnlag().stream()
            .filter(intk -> Objects.equals(intk.getArbeidsgiver(), arbeidsgiver))
            .anyMatch(intk -> harInntektIPeriode(periodeViSerEtterInntekt, intk.getAlleInntektsposter()));
    }

    private static boolean harInntektIPeriode(DatoIntervallEntitet periodeViSerEtterInntekt, Collection<Inntektspost> alleInntektsposter) {
        return alleInntektsposter.stream().anyMatch(post -> post.getPeriode().overlapper(periodeViSerEtterInntekt) && !post.getBeløp().erNullEllerNulltall());
    }

    private static boolean erFiskerUtenAktivtArbeid(AktørId aktørId,
                                                    LocalDate utledetStp,
                                                    InntektArbeidYtelseGrunnlag grunnlag,
                                                    Inntektsmelding inntektsmelding) {
        return harOppgittFiske(grunnlag) && harIngenAktiveArbeidsforhold(aktørId, utledetStp, inntektsmelding, grunnlag);
    }

    private static boolean harIngenAktiveArbeidsforhold(AktørId aktørId,
                                                        LocalDate utledetStp,
                                                        Inntektsmelding inntektsmelding,
                                                        InntektArbeidYtelseGrunnlag grunnlag) {
        var filter = new YrkesaktivitetFilter(grunnlag.getAktørArbeidFraRegister(aktørId)
            .map(AktørArbeid::hentAlleYrkesaktiviteter)
            .orElse(Collections.emptyList()));
        return filter.getYrkesaktiviteter().stream()
            .filter(ya -> gjelderInntektsmeldingFor(inntektsmelding.getArbeidsgiver(), ya))
            .noneMatch(ya -> ya.getAlleAktivitetsAvtaler().stream()
                .filter(AktivitetsAvtale::erAnsettelsesPeriode).anyMatch(aa -> aa.getPeriode().inkluderer(utledetStp)));
    }

    private static boolean harOppgittFiske(InntektArbeidYtelseGrunnlag grunnlag) {
        return grunnlag.getOppgittOpptjening().stream().anyMatch(
            oppgittOpptjening -> oppgittOpptjening.getEgenNæring().stream().anyMatch(en -> en.getVirksomhetType().equals(VirksomhetType.FISKE)));
    }

    private static boolean gjelderInntektsmeldingFor(Arbeidsgiver arbeidsgiver, Yrkesaktivitet yr) {
        return ARBEIDSFORHOLD_TYPER.contains(yr.getArbeidType())
            && yr.getArbeidsgiver().equals(arbeidsgiver);
    }

    private static boolean harArbeidsforholdIRegistreHosArbeidsgiver(AktørId aktørId, InntektArbeidYtelseGrunnlag grunnlag,
                                                                     Arbeidsgiver arbeidsgiverFraIM) {
        var filter = new YrkesaktivitetFilter(grunnlag.getAktørArbeidFraRegister(aktørId)
            .map(AktørArbeid::hentAlleYrkesaktiviteter)
            .orElse(Collections.emptyList()));
        return filter.getYrkesaktiviteter().stream()
            .anyMatch(yr -> gjelderInntektsmeldingFor(arbeidsgiverFraIM, yr));
    }

    private static Set<InternArbeidsforholdRef> trekkUtRef(Inntektsmelding inntektsmelding) {
        if (inntektsmelding.gjelderForEtSpesifiktArbeidsforhold()) {
            return Stream.of(inntektsmelding.getArbeidsforholdRef()).collect(Collectors.toSet());
        }
        return Stream.of(InternArbeidsforholdRef.nullRef()).collect(Collectors.toSet());
    }
}
