package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatDokRegelEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.VedtaksperioderHelper;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.TrekkdagerUtregningUtil;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Periode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.SamtidigUttaksprosent;

public final class FastsettePerioderRevurderingUtil {

    private FastsettePerioderRevurderingUtil() {
    }

    public static List<UttakResultatPeriodeEntitet> perioderFørDato(UttakResultatEntitet opprinneligUttak, LocalDate endringsdato) {
        var opprinneligePerioder = opprinneligUttak.getGjeldendePerioder().getPerioder();

        List<UttakResultatPeriodeEntitet> perioderFør = new ArrayList<>();

        for (var periode : opprinneligePerioder) {
            if (!VedtaksperioderHelper.avslåttPgaAvTaptPeriodeTilAnnenpart(periode)) {
                if (periode.getTom().isBefore(endringsdato)) {
                    perioderFør.add(kopierPeriode(periode));
                } else if (periode.getTidsperiode().inkluderer(endringsdato) && !periode.getFom().isEqual(endringsdato)) {
                    perioderFør.add(splittPeriode(periode, endringsdato));
                }
            }
        }

        return perioderFør.stream()
            .filter(p -> !helgUtenTrekkdager(p))
            .toList();
    }

    private static boolean helgUtenTrekkdager(UttakResultatPeriodeEntitet p) {
        //Helg med trekkdager skal ikke oppstå i praksis
        return p.getTidsperiode().erHelg() && !harTrekkdager(p);
    }

    private static boolean harTrekkdager(UttakResultatPeriodeEntitet p) {
        return p.getAktiviteter().stream().anyMatch(a -> a.getTrekkdager().merEnn0());
    }

    public static UttakResultatPerioderEntitet kopier(UttakResultatPerioderEntitet perioder) {
        var nyePerioder = new UttakResultatPerioderEntitet();
        for (var periode : perioder.getPerioder()) {
            nyePerioder.leggTilPeriode(kopierPeriode(periode));
        }
        return perioder;
    }

    private static UttakResultatPeriodeEntitet splittPeriode(UttakResultatPeriodeEntitet periode, LocalDate endringsdato) {
        var nyPeriode = kopierPeriode(periode, endringsdato.minusDays(1));
        for (var aktivitet : periode.getAktiviteter()) {
            nyPeriode.leggTilAktivitet(kopierAktivitet(aktivitet, nyPeriode, regnUtTrekkdager(aktivitet, nyPeriode.getTom())));
        }
        return nyPeriode;
    }

    private static UttakResultatPeriodeEntitet kopierPeriode(UttakResultatPeriodeEntitet periode) {
        var nyPeriode = kopierPeriode(periode, periode.getTom());
        for (var aktivitet : periode.getAktiviteter()) {
            nyPeriode.leggTilAktivitet(kopierAktivitet(aktivitet, nyPeriode, aktivitet.getTrekkdager()));
        }
        return nyPeriode;
    }

    private static UttakResultatPeriodeEntitet kopierPeriode(UttakResultatPeriodeEntitet periode, LocalDate nyTom) {
        var builder = new UttakResultatPeriodeEntitet.Builder(periode.getFom(), nyTom)
            .medResultatType(periode.getResultatType(), periode.getResultatÅrsak())
            .medGraderingInnvilget(periode.isGraderingInnvilget())
            .medUtsettelseType(periode.getUtsettelseType())
            .medOppholdÅrsak(periode.getOppholdÅrsak())
            .medOverføringÅrsak(periode.getOverføringÅrsak())
            .medSamtidigUttak(periode.isSamtidigUttak())
            .medSamtidigUttaksprosent(periode.getSamtidigUttaksprosent())
            .medFlerbarnsdager(periode.isFlerbarnsdager())
            .medGraderingAvslagÅrsak(periode.getGraderingAvslagÅrsak())
            .medManueltBehandlet(periode.isManueltBehandlet())
            .medManuellBehandlingÅrsak(periode.getManuellBehandlingÅrsak())
            .medBegrunnelse(periode.getBegrunnelse());
        if (periode.getDokRegel() != null) {
            builder.medDokRegel(kopierDokRegel(periode.getDokRegel()));
        }
        periode.getPeriodeSøknad().ifPresent(builder::medPeriodeSoknad);
        return builder.build();
    }

    private static UttakResultatDokRegelEntitet kopierDokRegel(UttakResultatDokRegelEntitet dokRegel) {
        UttakResultatDokRegelEntitet.Builder nyDokRegel;
        if (dokRegel.isTilManuellBehandling()) {
            nyDokRegel = UttakResultatDokRegelEntitet.medManuellBehandling(dokRegel.getManuellBehandlingÅrsak());
        } else {
            nyDokRegel = UttakResultatDokRegelEntitet.utenManuellBehandling();
        }
        return nyDokRegel
            .medRegelEvaluering(dokRegel.getRegelEvaluering())
            .medRegelInput(dokRegel.getRegelInput())
            .medRegelVersjon(dokRegel.getRegelVersjon())
            .build();
    }

    private static UttakResultatPeriodeAktivitetEntitet kopierAktivitet(UttakResultatPeriodeAktivitetEntitet aktivitet,
                                                                        UttakResultatPeriodeEntitet periode,
                                                                        Trekkdager nyeTrekkdager) {
        return new UttakResultatPeriodeAktivitetEntitet.Builder(periode, aktivitet.getUttakAktivitet())
            .medArbeidsprosent(aktivitet.getArbeidsprosent())
            .medTrekkdager(nyeTrekkdager)
            .medTrekkonto(aktivitet.getTrekkonto())
            .medErSøktGradering(aktivitet.isSøktGradering())
            .medUtbetalingsgrad(aktivitet.getUtbetalingsgrad())
            .build();
    }

    private static Trekkdager regnUtTrekkdager(UttakResultatPeriodeAktivitetEntitet aktivitet, LocalDate tom) {
        if (aktivitet.getTrekkdager().merEnn0()) {
            var samtidigUttaksprosent = samtidigUttaksprosent(aktivitet);
            return new Trekkdager(TrekkdagerUtregningUtil.trekkdagerFor(new Periode(aktivitet.getFom(), tom),
                aktivitet.isGraderingInnvilget(), aktivitet.getArbeidsprosent(), samtidigUttaksprosent).decimalValue());
        }
        return new Trekkdager(BigDecimal.ZERO);
    }

    private static SamtidigUttaksprosent samtidigUttaksprosent(UttakResultatPeriodeAktivitetEntitet aktivitet) {
        var samtidigUttaksprosent = aktivitet.getPeriode().getSamtidigUttaksprosent();
        return samtidigUttaksprosent == null ? null : new SamtidigUttaksprosent(samtidigUttaksprosent.decimalValue());
    }
}
