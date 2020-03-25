package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.uttak.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatDokRegelEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.fp.VedtaksperioderHelper;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.TrekkdagerUtregningUtil;
import no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Periode;

public final class FastsettePerioderRevurderingUtil {

    private FastsettePerioderRevurderingUtil() {
    }


    public static List<UttakResultatPeriodeEntitet> perioderFørDato(UttakResultatEntitet opprinneligUttak, LocalDate endringsdato) {
        List<UttakResultatPeriodeEntitet> opprinneligePerioder = opprinneligUttak.getGjeldendePerioder().getPerioder();

        List<UttakResultatPeriodeEntitet> perioderFør = new ArrayList<>();

        for (UttakResultatPeriodeEntitet periode : opprinneligePerioder) {
            if (!VedtaksperioderHelper.avslåttPgaAvTaptPeriodeTilAnnenpart(periode)) {
                if (periode.getTom().isBefore(endringsdato)) {
                    perioderFør.add(kopierPeriode(periode));
                } else if (periode.getTidsperiode().inkluderer(endringsdato) && !periode.getFom().isEqual(endringsdato)) {
                    perioderFør.add(splittPeriode(periode, endringsdato));
                }
            }
        }

        return perioderFør;
    }

    private static UttakResultatPeriodeEntitet splittPeriode(UttakResultatPeriodeEntitet periode, LocalDate endringsdato) {
        UttakResultatPeriodeEntitet nyPeriode = kopierPeriode(periode, endringsdato.minusDays(1));
        for (UttakResultatPeriodeAktivitetEntitet aktivitet : periode.getAktiviteter()) {
            nyPeriode.leggTilAktivitet(kopierAktivitet(aktivitet, nyPeriode, regnUtTrekkdager(aktivitet, nyPeriode.getTom())));
        }
        return nyPeriode;
    }

    private static UttakResultatPeriodeEntitet kopierPeriode(UttakResultatPeriodeEntitet periode) {
        UttakResultatPeriodeEntitet nyPeriode = kopierPeriode(periode, periode.getTom());
        for (UttakResultatPeriodeAktivitetEntitet aktivitet : periode.getAktiviteter()) {
            nyPeriode.leggTilAktivitet(kopierAktivitet(aktivitet, nyPeriode, aktivitet.getTrekkdager()));
        }
        return nyPeriode;
    }

    private static UttakResultatPeriodeEntitet kopierPeriode(UttakResultatPeriodeEntitet periode, LocalDate nyTom) {
        UttakResultatPeriodeEntitet.Builder builder = new UttakResultatPeriodeEntitet.Builder(periode.getFom(), nyTom)
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
            .medBegrunnelse(periode.getBegrunnelse());
        if (periode.getDokRegel() != null) {
            builder.medDokRegel(kopierDokRegel(periode.getDokRegel()));
        }
        if (periode.getPeriodeSøknad().isPresent()) {
            builder.medPeriodeSoknad(periode.getPeriodeSøknad().get());
        }
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
            return new Trekkdager(TrekkdagerUtregningUtil.trekkdagerFor(new Periode(aktivitet.getFom(), tom),
                aktivitet.isGraderingInnvilget(), aktivitet.getArbeidsprosent(), aktivitet.getPeriode().getSamtidigUttaksprosent()).decimalValue());
        }
        return new Trekkdager(BigDecimal.ZERO);
    }
}
