package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tjeneste som skal utlede om en arbeidsgiver er inaktiv eller ikke,
 * for å filtrere ut unødvendige arbeidsforhold fra å kreve inntektsmelding.
 */
public class InaktiveArbeidsforholdUtleder {
    private static final Logger LOG = LoggerFactory.getLogger(InaktiveArbeidsforholdUtleder.class);
    private static final Set<RelatertYtelseType> YTELSER_SOM_IKKE_PÅVIRKER_IM = Set.of(RelatertYtelseType.ARBEIDSAVKLARINGSPENGER, RelatertYtelseType.DAGPENGER);
    private static final int AKTIVE_MÅNEDER_FØR_STP = 4;
    private static final int NYOPPSTARTEDE_ARBEIDSFORHOLD_ALDER_I_MND = 4;

    private InaktiveArbeidsforholdUtleder() {
    }

    public static Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> finnKunAktive(Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> påkrevdeInntektsmeldinger,
                                                                                Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                                                BehandlingReferanse referanse, Skjæringstidspunkt stp) {
        var utledetStp = stp.getUtledetSkjæringstidspunkt();

        var aktiveArbeidsforhold = påkrevdeInntektsmeldinger.entrySet().stream()
            .filter(e -> !erInaktivt(e.getKey(), inntektArbeidYtelseGrunnlag, referanse.aktørId(), utledetStp, referanse.saksnummer()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (aktiveArbeidsforhold.isEmpty()) {
            LOG.info("INAKTIV_ARB_UTLEDER: Alle arbeidsforhold var inaktive, returnerer tom liste");
            return aktiveArbeidsforhold;
        }

        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> aktiveArbeidsforholdUtenPermisjon = new HashMap<>();
        //Sjekker om hvert arbeidsforhold under virksomheten har registrert permisjon som overlapper skjæringstidspunkt. Fjerne de som har det
        aktiveArbeidsforhold.forEach((key, value) -> {
            var utenPermisjon = value.stream()
                .filter(ref -> !erIPermisjonPåStp(key, ref, inntektArbeidYtelseGrunnlag, referanse.aktørId(), utledetStp))
                .collect(Collectors.toSet());
            if (!utenPermisjon.isEmpty()) {
                aktiveArbeidsforholdUtenPermisjon.put(key, utenPermisjon);
            }
        });
        if (aktiveArbeidsforholdUtenPermisjon.isEmpty()) {
            LOG.info("INAKTIV_ARB_UTLEDER: Finnes ingen aktive arbeidsforhold uten permisjon, returnerer istedenfor aktive arbeidsforhold med permisjon");
            return aktiveArbeidsforhold;
        }
        return aktiveArbeidsforholdUtenPermisjon;
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
            LOG.info("INAKTIV_ARB_UTLEDER: Arbeidsforhold hos {} er nyoppstartet (< {} mnd før stp {}), regnes som aktivt", arbeidsgiverSomSjekkes, NYOPPSTARTEDE_ARBEIDSFORHOLD_ALDER_I_MND, stp);
            return false;
        }
        if (harMottattIMFraAG(arbeidsgiverSomSjekkes, inntektArbeidYtelseGrunnlag.get())) {
            LOG.info("INAKTIV_ARB_UTLEDER: Har mottatt inntektsmelding fra {}, regnes som aktivt", arbeidsgiverSomSjekkes);
            return false;
        }
        if (harHattYtelseForArbeidsgiver(arbeidsgiverSomSjekkes, inntektArbeidYtelseGrunnlag.get(), søkerAktørId, stp, saksnummer)) {
            LOG.info("INAKTIV_ARB_UTLEDER: Har hatt ytelse for arbeidsgiver {} i perioden {} mnd før stp {}, regnes som aktivt", arbeidsgiverSomSjekkes, AKTIVE_MÅNEDER_FØR_STP, stp);
            return false;
        }
        var harInntekt = harHattInntektIPeriode(arbeidsgiverSomSjekkes, inntektArbeidYtelseGrunnlag.get(), søkerAktørId, stp);
        if (!harInntekt) {
            LOG.info("INAKTIV_ARB_UTLEDER: Arbeidsforhold hos {} regnes som inaktivt: ikke nyoppstartet, ingen IM, ingen ytelse, ingen inntekt siste {} mnd før stp {}", arbeidsgiverSomSjekkes, AKTIVE_MÅNEDER_FØR_STP, stp);
        }
        return !harInntekt;
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
            .filter(ya -> periode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(ya.getAnvistFOM(), ya.getAnvistTOM())))
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
