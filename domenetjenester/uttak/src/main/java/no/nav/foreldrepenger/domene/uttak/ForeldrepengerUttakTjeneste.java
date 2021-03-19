package no.nav.foreldrepenger.domene.uttak;

import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
public class ForeldrepengerUttakTjeneste {

    private FpUttakRepository fpUttakRepository;

    @Inject
    public ForeldrepengerUttakTjeneste(FpUttakRepository fpUttakRepository) {
        this.fpUttakRepository = fpUttakRepository;
    }

    ForeldrepengerUttakTjeneste() {
        //CDI
    }

    public Optional<ForeldrepengerUttak> hentUttakHvisEksisterer(long behandlingId) {
        return fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId).map(entitet -> map(entitet));
    }

    public ForeldrepengerUttak hentUttak(long behandlingId) {
        return hentUttakHvisEksisterer(behandlingId).orElseThrow();
    }

    public static ForeldrepengerUttak map(UttakResultatEntitet entitet) {
        var opprinneligPerioder = entitet.getOpprinneligPerioder().getPerioder().stream()
            .map(p -> map(p))
            .collect(Collectors.toList());
        var overstyrtPerioder = entitet.getOverstyrtPerioder() == null ? null : entitet.getOverstyrtPerioder().getPerioder().stream()
            .map(p -> map(p))
            .collect(Collectors.toList());

        return new ForeldrepengerUttak(opprinneligPerioder, overstyrtPerioder);
    }

    private static ForeldrepengerUttakPeriode map(UttakResultatPeriodeEntitet entitet) {
        var aktiviteter = entitet.getAktiviteter().stream()
            .map(a -> map(a))
            .collect(Collectors.toList());
        var periodeBuilder = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(new LocalDateInterval(entitet.getFom(), entitet.getTom()))
            .medAktiviteter(aktiviteter)
            .medBegrunnelse(entitet.getBegrunnelse())
            .medResultatType(entitet.getResultatType())
            .medResultatÅrsak(entitet.getResultatÅrsak())
            .medFlerbarnsdager(entitet.isFlerbarnsdager())
            .medUtsettelseType(entitet.getUtsettelseType())
            .medOppholdÅrsak(entitet.getOppholdÅrsak())
            .medOverføringÅrsak(entitet.getOverføringÅrsak())
            .medSamtidigUttak(entitet.isSamtidigUttak())
            .medGraderingInnvilget(entitet.isGraderingInnvilget())
            .medGraderingAvslagÅrsak(entitet.getGraderingAvslagÅrsak())
            .medSamtidigUttaksprosent(entitet.getSamtidigUttaksprosent())
            .medManuellBehandlingÅrsak(entitet.getManuellBehandlingÅrsak())
            .medSøktKonto(entitet.getPeriodeSøknad().map(se -> se.getUttakPeriodeType()).orElse(null))
            .medMottattDato(entitet.getPeriodeSøknad().map(se -> se.getMottattDato()).orElse(null))
            .medOpprinneligSendtTilManuellBehandling(entitet.opprinneligSendtTilManuellBehandling())
            .medManueltBehandlet(entitet.isManueltBehandlet());
        return periodeBuilder.build();
    }

    private static ForeldrepengerUttakPeriodeAktivitet map(UttakResultatPeriodeAktivitetEntitet periodeAktivitet) {
        var uttakAktivitet = new ForeldrepengerUttakAktivitet(periodeAktivitet.getUttakArbeidType(), periodeAktivitet.getArbeidsgiver(),
            periodeAktivitet.getArbeidsforholdRef());
        return new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medArbeidsprosent(periodeAktivitet.getArbeidsprosent())
            .medTrekkonto(periodeAktivitet.getTrekkonto())
            .medTrekkdager(periodeAktivitet.getTrekkdager())
            .medUtbetalingsgrad(periodeAktivitet.getUtbetalingsgrad())
            .medAktivitet(uttakAktivitet)
            .medSøktGraderingForAktivitetIPeriode(periodeAktivitet.isSøktGradering())
            .build();
    }
}
