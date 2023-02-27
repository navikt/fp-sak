package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public final class SammenlignFordeling {

    private SammenlignFordeling() {
    }

    public static boolean erLikeFordelinger(OppgittFordelingEntitet fordelingEntitet1, OppgittFordelingEntitet fordelingEntitet2) {
        if (fordelingEntitet1 == null && fordelingEntitet2 == null) {
            return true;
        }
        if (fordelingEntitet1 == null || fordelingEntitet2 == null ||
            !Objects.equals(fordelingEntitet1.ønskerJustertVedFødsel(), fordelingEntitet2.ønskerJustertVedFødsel()) ||
            !Objects.equals(fordelingEntitet1.getErAnnenForelderInformert(), fordelingEntitet2.getErAnnenForelderInformert())) {
            return false;
        }

        // Tidslinje og tidligste/seneste dato fra ny søknad
        var segmenter1 = fordelingEntitet1.getPerioder().stream().map(SammenlignFordeling::segmentForOppgittPeriode).toList();
        var tidslinjeSammenlign1 =  new LocalDateTimeline<>(segmenter1);
        var segmenter2 = fordelingEntitet2.getPerioder().stream().map(SammenlignFordeling::segmentForOppgittPeriode).toList();
        var tidslinjeSammenlign2 =  new LocalDateTimeline<>(segmenter2);

        // Finner segmenter der de to tidslinjene (søknad vs vedtakFomTidligsteDatoSøknad) er ulike
        var ulike = tidslinjeSammenlign1.combine(tidslinjeSammenlign2, (i, l, r) -> new LocalDateSegment<>(i, !Objects.equals(l ,r)), LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .filterValue(v -> v);

        // Sjekk om finnes segment som er ulike
        return ulike.isEmpty();
    }

    private static LocalDateSegment<SammenligningPeriodeForOppgitt> segmentForOppgittPeriode(OppgittPeriodeEntitet periode) {
        var fom = VirkedagUtil.lørdagSøndagTilMandag(periode.getFom());
        var tom = VirkedagUtil.fredagLørdagTilSøndag(periode.getTom());
        if (fom.isAfter(tom)) {
            fom = periode.getFom();
        }
        return new LocalDateSegment<>(fom, tom, new SammenligningPeriodeForOppgitt(periode));
    }

    private record SammenligningPeriodeForOppgitt(Årsak årsak, UttakPeriodeType periodeType, SamtidigUttaksprosent samtidigUttaksprosent, SammenligningGraderingForOppgitt gradering, boolean flerbarnsdager, MorsAktivitet morsAktivitet) {
        SammenligningPeriodeForOppgitt(OppgittPeriodeEntitet periode) {
            this(periode.getÅrsak(), periode.getPeriodeType(), periode.getSamtidigUttaksprosent(), periode.isGradert() ? new SammenligningGraderingForOppgitt(periode) : null, periode.isFlerbarnsdager(), periode.getMorsAktivitet());
        }
    }

    private record SammenligningGraderingForOppgitt(GraderingAktivitetType gradertAktivitet, Stillingsprosent arbeidsprosent, Arbeidsgiver arbeidsgiver) {
        SammenligningGraderingForOppgitt(OppgittPeriodeEntitet periode) {
            this(periode.getGraderingAktivitetType(), periode.getArbeidsprosentSomStillingsprosent(), periode.getArbeidsgiver());
        }
    }

}
