package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.HåndterePermisjoner;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.AktørInntekt;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvistAndel;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

/**
 * Tjeneste som skal utlede om en arbeidsgiver er inaktiv eller ikke,
 * for å filtrere ut unødvendige arbeidsforhold fra å kreve inntektsmelding.
 */
public class InaktiveArbeidsforholdUtleder {
    private static final Set<RelatertYtelseType> YTELSER_SOM_IKKE_PÅVIRKER_IM = Set.of(RelatertYtelseType.ARBEIDSAVKLARINGSPENGER, RelatertYtelseType.DAGPENGER);
    private static final int AKTIVE_MÅNEDER_FØR_STP = 4;
    private static final int NYOPPSTARTEDE_ARBEIDSFORHOLD_ALDER_I_MND = 4;

    private InaktiveArbeidsforholdUtleder() {
    }

    public static Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> finnKunAktive(Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevdeInntektsmeldinger,
                                                                                Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                                                BehandlingReferanse referanse, Skjæringstidspunkt stp) {
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> kunAktiveArbeidsforhold = new HashMap<>();

        var utledetStp = stp.getUtledetSkjæringstidspunkt();
        påkrevdeInntektsmeldinger.forEach((key, value) -> {
            var erInaktivt = erInaktivt(key, inntektArbeidYtelseGrunnlag, referanse.aktørId(), utledetStp,
                referanse.saksnummer());
            if (!erInaktivt) {
                List<InternArbeidsforholdRef> aktiveArbeidsforholdsRef = new ArrayList<>();
                //Sjekker om hvert arbeidsforhold under virksomheten har registrert permisjon som overlapper skjæringstidspunkt. Fjerne de som har det
                value.forEach(internArbeidsforholdRef-> {
                    if (!erIPermisjonPåStp(key, internArbeidsforholdRef, inntektArbeidYtelseGrunnlag, referanse.aktørId(), utledetStp)) {
                        aktiveArbeidsforholdsRef.add(internArbeidsforholdRef);
                    }
                });
                if (!aktiveArbeidsforholdsRef.isEmpty()) {
                    kunAktiveArbeidsforhold.put(key, new HashSet<>(aktiveArbeidsforholdsRef));
                }
            }
        });
        return kunAktiveArbeidsforhold;
    }


    public static boolean erInaktivt(Arbeidsgiver arbeidsgiverSomSjekkes,
                                     Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                     AktørId søkerAktørId,
                                     LocalDate stp,
                                     Saksnummer saksnummer) {
        if (inntektArbeidYtelseGrunnlag.isEmpty()) {
            return false;
        }
        if (erNyoppstartet(arbeidsgiverSomSjekkes, inntektArbeidYtelseGrunnlag.get(), søkerAktørId, stp)) {
            return false;
        }
        if (harMottattIMFraAG(arbeidsgiverSomSjekkes, inntektArbeidYtelseGrunnlag.get())) {
            return false;
        }
        if (harHattYtelseForArbeidsgiver(arbeidsgiverSomSjekkes, inntektArbeidYtelseGrunnlag.get(), søkerAktørId, stp, saksnummer)) {
            return false;
        }
        return !harHattInntektIPeriode(arbeidsgiverSomSjekkes, inntektArbeidYtelseGrunnlag.get(), søkerAktørId, stp);
    }

    private static boolean erIPermisjonPåStp(Arbeidsgiver arbeidsgiver,
                                             InternArbeidsforholdRef ref,
                                             Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                             AktørId søkerAktørId,
                                             LocalDate stp) {
        return inntektArbeidYtelseGrunnlag.map(iayg -> iayg.getAktørArbeidFraRegister(søkerAktørId)
            .map(AktørArbeid::hentAlleYrkesaktiviteter).orElse(Collections.emptyList()).stream()
            .filter(yrkesaktivitet -> yrkesaktivitet.getArbeidsgiver() != null && yrkesaktivitet.getArbeidsgiver().equals(arbeidsgiver))
            .filter(yrkesaktivitet -> yrkesaktivitet.getArbeidsforholdRef() != null && yrkesaktivitet.getArbeidsforholdRef().equals(ref))
            .anyMatch(yrkesAktivitet -> HåndterePermisjoner.harRelevantPermisjonSomOverlapperSkjæringstidspunkt(yrkesAktivitet, stp))).orElse(false);
    }

    private static boolean harMottattIMFraAG(Arbeidsgiver arbeidsgiverSomSjekkes, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        return inntektArbeidYtelseGrunnlag.getInntektsmeldinger()
            .map(im -> !im.getInntektsmeldingerFor(arbeidsgiverSomSjekkes).isEmpty())
            .orElse(false);
    }

    private static boolean harHattYtelseForArbeidsgiver(Arbeidsgiver arbeidsgiver,
                                                        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag,
                                                        AktørId søkerAktørId,
                                                        LocalDate stp,
                                                        Saksnummer saksnummer) {
        var periodeViDefinererSomAkivt = DatoIntervallEntitet.fraOgMedTilOgMed(stp.minusMonths(AKTIVE_MÅNEDER_FØR_STP), stp);
        var ytelser = inntektArbeidYtelseGrunnlag.getAktørYtelseFraRegister(søkerAktørId)
            .map(AktørYtelse::getAlleYtelser)
            .orElse(Collections.emptyList());
        return ytelser.stream()
            .filter(yt-> !YTELSER_SOM_IKKE_PÅVIRKER_IM.contains(yt.getRelatertYtelseType()))
            .filter(yt -> !saksnummer.equals(yt.getSaksnummer())) // Bryr oss ikke om tidligere vedtak på saken vi nå behandler
            .anyMatch(yt -> harYtelseForArbeidsforholdIPeriode(yt, periodeViDefinererSomAkivt, arbeidsgiver));
    }

    private static boolean harHattInntektIPeriode(Arbeidsgiver arbeidsgiver, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag, AktørId søkerAktørId, LocalDate stp) {
        var periodeViDefinererSomAkivt = DatoIntervallEntitet.fraOgMedTilOgMed(stp.minusMonths(AKTIVE_MÅNEDER_FØR_STP), stp);
        var inntekter = inntektArbeidYtelseGrunnlag.getAktørInntektFraRegister(søkerAktørId)
            .map(AktørInntekt::getInntekt)
            .orElse(Collections.emptyList());
        return inntekter.stream()
            .filter(innt -> innt.getInntektsKilde().equals(InntektsKilde.INNTEKT_BEREGNING))
            .filter(innt -> innt.getArbeidsgiver() != null && innt.getArbeidsgiver().equals(arbeidsgiver))
            .anyMatch(innt -> harInntektIPeriode(innt.getAlleInntektsposter(), periodeViDefinererSomAkivt));
    }

    private static boolean erNyoppstartet(Arbeidsgiver arbeidsgiver, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag, AktørId søkerAktørId, LocalDate stp) {
        var alleArbeidsforholdHosAG = inntektArbeidYtelseGrunnlag.getAktørArbeidFraRegister(søkerAktørId)
            .map(AktørArbeid::hentAlleYrkesaktiviteter)
            .orElse(Collections.emptyList())
            .stream()
            .filter(arb -> arb.getArbeidsgiver() != null && arb.getArbeidsgiver().equals(arbeidsgiver))
            .toList();
        if (alleArbeidsforholdHosAG.isEmpty()) {
            return false;
        }
        return alleArbeidsforholdHosAG.stream()
            .noneMatch(arb -> erEldreEnnGrense(arb, stp));
    }

    private static boolean erEldreEnnGrense(Yrkesaktivitet arb, LocalDate stp) {
        var fomDefinertSomNyoppstartet = stp.minusMonths(NYOPPSTARTEDE_ARBEIDSFORHOLD_ALDER_I_MND);
        return arb.getAlleAktivitetsAvtaler().stream()
            .filter(AktivitetsAvtale::erAnsettelsesPeriode)
            .anyMatch(aa -> aa.getPeriode().getFomDato().isBefore(fomDefinertSomNyoppstartet));
    }

    private static boolean harInntektIPeriode(Collection<Inntektspost> alleInntektsposter, DatoIntervallEntitet periodeViDefinererSomAkivt) {
        return alleInntektsposter.stream()
            .filter(post -> post.getPeriode().overlapper(periodeViDefinererSomAkivt))
            .anyMatch(post -> !post.getBeløp().erNullEllerNulltall());
    }

    private static boolean harYtelseForArbeidsforholdIPeriode(Ytelse ytelse, DatoIntervallEntitet periode, Arbeidsgiver arbeidsgiver) {
        var overlappendeAnvisninger = ytelse.getYtelseAnvist()
            .stream()
            .filter(ya -> periode.inkluderer(ya.getAnvistFOM()) || periode.inkluderer(ya.getAnvistTOM()))
            .toList();
        if (overlappendeAnvisninger.isEmpty()) {
            return false;
        }

        var allePerioderHarAnvisteAndeler = overlappendeAnvisninger.stream().noneMatch(anv -> anv.getYtelseAnvistAndeler() == null || anv.getYtelseAnvistAndeler().isEmpty());

        if (!allePerioderHarAnvisteAndeler) {
            // Har ikke datagrunnlag for å kunne se hva ytelsen er basert på, antar at arbeidsforhold er aktivt
            return true;
        }

        return overlappendeAnvisninger.stream()
            .anyMatch(a -> harAndelHosArbeidsgiverMedUtbetaling(a, arbeidsgiver));
    }

    private static boolean harAndelHosArbeidsgiverMedUtbetaling(YtelseAnvist anvisning, Arbeidsgiver arbeidsgiver) {
        return anvisning.getYtelseAnvistAndeler().stream()
            .filter(andel -> matcherArbeidsgiver(arbeidsgiver, andel))
            .anyMatch(andel -> andel.getDagsats() != null && !andel.getDagsats().erNullEllerNulltall());
    }

    private static Boolean matcherArbeidsgiver(Arbeidsgiver arbeidsgiver, YtelseAnvistAndel andel) {
        return andel.getArbeidsgiver()
            .filter(ytelseAG -> ytelseAG.equals(arbeidsgiver) || Nødnummer.erNødnummer(ytelseAG.getIdentifikator()))
            .isPresent();
    }
}
