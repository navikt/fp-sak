package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.AktørInntekt;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelse;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class InaktiveArbeidsforholdUtleder {
    private static final Set<String> NØDNUMRE = Set.of("971278420", "971278439", "971248106", "971373032", "871400172");
    private static final Set<RelatertYtelseType> YTELSER_SOM_IKKE_PÅVIRKER_IM = Set.of(RelatertYtelseType.PLEIEPENGER_NÆRSTÅENDE, RelatertYtelseType.ARBEIDSAVKLARINGSPENGER, RelatertYtelseType.DAGPENGER, RelatertYtelseType.OMSORGSPENGER);
    private static final int AKTIVE_MÅNEDER_FØR_STP = 10;
    private static final int NYOPPSTARTEDE_ARBEIDSFORHOLD_ALDER_I_MND = 4;

    private static final Logger LOG = LoggerFactory.getLogger(InaktiveArbeidsforholdUtleder.class);

    public static boolean erInaktivt(Arbeidsgiver arbeidsgiverSomSjekkes, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag, BehandlingReferanse referanse) {
        if (erNyoppstartet(arbeidsgiverSomSjekkes, inntektArbeidYtelseGrunnlag, referanse)) {
            return false;
        }
        if (HarMottattIMFraAG(arbeidsgiverSomSjekkes, inntektArbeidYtelseGrunnlag)) {
            return false;
        }
        boolean harHattYtelse = harHattYtelseForArbeidsgiver(arbeidsgiverSomSjekkes, inntektArbeidYtelseGrunnlag, referanse);
        boolean harHattInntekt = harHattInntektIPeriode(arbeidsgiverSomSjekkes, inntektArbeidYtelseGrunnlag, referanse);
        return harHattYtelse || harHattInntekt;
    }

    private static boolean HarMottattIMFraAG(Arbeidsgiver arbeidsgiverSomSjekkes, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag) {
        return inntektArbeidYtelseGrunnlag
            .flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger)
            .map(im -> im.getInntektsmeldingerFor(arbeidsgiverSomSjekkes).size() > 0)
            .orElse(false);
    }

    private static boolean harHattYtelseForArbeidsgiver(Arbeidsgiver arbeidsgiver, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag, BehandlingReferanse referanse) {
        var periodeViDefinererSomAkivt = DatoIntervallEntitet.fraOgMedTilOgMed(referanse.getUtledetSkjæringstidspunkt().minusMonths(AKTIVE_MÅNEDER_FØR_STP), referanse.getUtledetSkjæringstidspunkt());
        Collection<Ytelse> ytelser = inntektArbeidYtelseGrunnlag
            .flatMap(iayg -> iayg.getAktørYtelseFraRegister(referanse.getAktørId()))
            .map(AktørYtelse::getAlleYtelser)
            .orElse(Collections.emptyList());
        return ytelser.stream()
            .filter(yt-> !YTELSER_SOM_IKKE_PÅVIRKER_IM.contains(yt.getRelatertYtelseType()))
            .filter(yt -> harYtelseIPeriode(yt, periodeViDefinererSomAkivt))
            .anyMatch(yt -> erYtelseForAG(yt, arbeidsgiver));
    }

    private static boolean harHattInntektIPeriode(Arbeidsgiver arbeidsgiver, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag, BehandlingReferanse referanse) {
        var periodeViDefinererSomAkivt = DatoIntervallEntitet.fraOgMedTilOgMed(referanse.getUtledetSkjæringstidspunkt().minusMonths(AKTIVE_MÅNEDER_FØR_STP), referanse.getUtledetSkjæringstidspunkt());
        var inntekter = inntektArbeidYtelseGrunnlag
            .flatMap(iayg -> iayg.getAktørInntektFraRegister(referanse.getAktørId()))
            .map(AktørInntekt::getInntekt)
            .orElse(Collections.emptyList());
        return inntekter.stream()
            .filter(innt -> innt.getInntektsKilde().equals(InntektsKilde.INNTEKT_BEREGNING))
            .filter(innt -> innt.getArbeidsgiver() != null && innt.getArbeidsgiver().equals(arbeidsgiver))
            .anyMatch(innt -> harInntektIPeriode(innt.getAlleInntektsposter(), periodeViDefinererSomAkivt));
    }

    private static boolean erNyoppstartet(Arbeidsgiver arbeidsgiver, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag, BehandlingReferanse referanse) {
        var alleArbeidsforhold = inntektArbeidYtelseGrunnlag.
            flatMap(iayg -> iayg.getAktørArbeidFraRegister(referanse.getAktørId()))
            .map(AktørArbeid::hentAlleYrkesaktiviteter).orElse(Collections.emptyList());
        return alleArbeidsforhold.stream()
            .filter(arb -> arb.getArbeidsgiver() != null && arb.getArbeidsgiver().equals(arbeidsgiver))
            .anyMatch(arb -> erEldreEnnGrense(arb, referanse.getUtledetSkjæringstidspunkt()));
    }

    private static boolean erEldreEnnGrense(Yrkesaktivitet arb, LocalDate stp) {
        LocalDate fomDefinertSomNyoppstartet = stp.minusMonths(NYOPPSTARTEDE_ARBEIDSFORHOLD_ALDER_I_MND);
        return arb.getAlleAktivitetsAvtaler().stream()
            .filter(AktivitetsAvtale::erAnsettelsesPeriode)
            .anyMatch(aa -> aa.getPeriode().getFomDato().isBefore(fomDefinertSomNyoppstartet));
    }

    private static boolean harInntektIPeriode(Collection<Inntektspost> alleInntektsposter, DatoIntervallEntitet periodeViDefinererSomAkivt) {
        return alleInntektsposter.stream()
            .filter(post -> post.getPeriode().overlapper(periodeViDefinererSomAkivt))
            .anyMatch(post -> !post.getBeløp().erNullEllerNulltall());
    }

    private static boolean erYtelseForAG(Ytelse ytelse, Arbeidsgiver arbeidsgiver) {
        var ytelsestørrelser = ytelse.getYtelseGrunnlag()
            .map(YtelseGrunnlag::getYtelseStørrelse)
            .orElse(Collections.emptyList());
        if (ytelsestørrelser.isEmpty()) {
            LOG.info("Mottok ingen ytelsestørrelse for ytelse, defaulter til true");
            return true;
        }
        return ytelsestørrelser.stream().anyMatch(ys -> kanMatcheAG(arbeidsgiver, ys));
    }

    private static boolean kanMatcheAG(Arbeidsgiver arbeidsgiver, YtelseStørrelse ys) {
        // Hvis orgnr mangler kan det være at det gjelder for arbeidsgiver, så hvis det mangler returnerer vi true for sikkerhets skyld
        boolean matcherOrgnr = ys.getOrgnr().map(org -> org.equals(arbeidsgiver.getIdentifikator())).orElse(true);
        if (matcherOrgnr) {
            return true;
        }
        return NØDNUMRE.contains(arbeidsgiver.getIdentifikator());
    }

    private static boolean harYtelseIPeriode(Ytelse ytelse, DatoIntervallEntitet periode) {
        if (ytelse.getYtelseAnvist().isEmpty()) {
            return ytelse.getPeriode().overlapper(periode);
        }
        return ytelse.getYtelseAnvist().stream()
            .anyMatch(ya -> periode.inkluderer(ya.getAnvistFOM()) || periode.inkluderer(ya.getAnvistTOM()));
    }
}
