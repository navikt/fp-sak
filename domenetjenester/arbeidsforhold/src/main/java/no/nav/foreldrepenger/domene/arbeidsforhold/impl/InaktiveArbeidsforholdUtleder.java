package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.Inntekt;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelse;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class InaktiveArbeidsforholdUtleder {
    private static final Set<String> NØDNUMRE = Set.of("971278420", "971278439", "971248106", "971373032", "871400172");
    private static final int AKTIVE_MÅNEDER_FØR_STP = 10;
    private static final Logger LOG = LoggerFactory.getLogger(InaktiveArbeidsforholdUtleder.class);

    public static boolean utled(Arbeidsgiver arbeidsgiver, LocalDate stp, Collection<Ytelse> ytelser, Collection<Inntekt> inntekter) {
        var periodeViDefinererSomAkivt = DatoIntervallEntitet.fraOgMedTilOgMed(stp.minusMonths(AKTIVE_MÅNEDER_FØR_STP), stp);
        var harHattYtelseDennePerioden = ytelser.stream()
            .filter(yt -> harYtelseIPeriode(yt, periodeViDefinererSomAkivt))
            .anyMatch(yt -> erYtelseForAG(yt, arbeidsgiver));
        var harHattInntektDennePerioden = inntekter.stream()
            .filter(innt -> innt.getInntektsKilde().equals(InntektsKilde.INNTEKT_BEREGNING))
            .filter(innt -> innt.getArbeidsgiver() != null && innt.getArbeidsgiver().equals(arbeidsgiver))
            .anyMatch(innt -> harInntektIPeriode(innt.getAlleInntektsposter(), periodeViDefinererSomAkivt));
        return harHattYtelseDennePerioden || harHattInntektDennePerioden;
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
