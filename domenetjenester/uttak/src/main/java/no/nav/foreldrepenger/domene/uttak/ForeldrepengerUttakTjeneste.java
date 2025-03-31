package no.nav.foreldrepenger.domene.uttak;

import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeSøknadEntitet;
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

    public Optional<ForeldrepengerUttak> hentHvisEksisterer(long behandlingId) {
        return fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId).map(ForeldrepengerUttakTjeneste::map);
    }

    public ForeldrepengerUttak hent(long behandlingId) {
        return hentHvisEksisterer(behandlingId).orElseThrow();
    }

    private static ForeldrepengerUttak map(UttakResultatEntitet entitet) {
        var opprinneligPerioder = entitet.getOpprinneligPerioder().getPerioder().stream()
            .map(ForeldrepengerUttakTjeneste::map)
            .toList();
        var overstyrtPerioder = entitet.getOverstyrtPerioder() == null ? null : entitet.getOverstyrtPerioder().getPerioder().stream()
            .map(ForeldrepengerUttakTjeneste::map)
            .toList();
        var kontoutregning = Optional.ofNullable(entitet.getStønadskontoberegning()).map(Stønadskontoberegning::getStønadskontoutregning).orElse(Map.of());
        return new ForeldrepengerUttak(opprinneligPerioder, overstyrtPerioder, kontoutregning);
    }

    private static ForeldrepengerUttakPeriode map(UttakResultatPeriodeEntitet entitet) {
        var aktiviteter = entitet.getAktiviteter().stream()
            .map(ForeldrepengerUttakTjeneste::map)
            .toList();
        var mottattDato = entitet.getPeriodeSøknad().map(UttakResultatPeriodeSøknadEntitet::getMottattDato).orElse(null);
        var tidligsMottatt = entitet.getPeriodeSøknad().flatMap(UttakResultatPeriodeSøknadEntitet::getTidligstMottattDato).orElse(null);

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
            .medSøktKonto(entitet.getPeriodeSøknad().map(UttakResultatPeriodeSøknadEntitet::getUttakPeriodeType).orElse(null))
            .medMottattDato(mottattDato)
            .medTidligstMottattDato(tidligsMottatt)
            .medMorsAktivitet(entitet.getPeriodeSøknad().map(UttakResultatPeriodeSøknadEntitet::getMorsAktivitet).orElse(MorsAktivitet.UDEFINERT))
            .medErFraSøknad(entitet.getPeriodeSøknad().isPresent())
            .medDokumentasjonVurdering(entitet.getPeriodeSøknad()
                .map(UttakResultatPeriodeSøknadEntitet::getDokumentasjonVurdering)
                .orElse(null))
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
