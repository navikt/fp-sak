package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeSøknadEntitet;

public class VedtaksperiodeMottattdatoHelper {

    private VedtaksperiodeMottattdatoHelper() {
        //
    }

    public static List<OppgittPeriodeEntitet> opprettOppgittePerioderSøknadverdier(UttakResultatEntitet uttakResultatFraForrigeBehandling, LocalDate perioderFom) {
        return lagVedtaksperioderSøknadverdier(uttakResultatFraForrigeBehandling, perioderFom);
    }

    private static List<OppgittPeriodeEntitet> lagVedtaksperioderSøknadverdier(UttakResultatEntitet uttakResultat, LocalDate perioderFom) {
        return uttakResultat.getGjeldendePerioder()
            .getPerioder()
            .stream()
            .filter(p -> !p.getTom().isBefore(perioderFom))
            .filter(periode -> periode.getPeriodeSøknad().isPresent())
            .map(VedtaksperiodeMottattdatoHelper::konverterSøknadverdier)
            .toList();
    }

    private static OppgittPeriodeEntitet konverterSøknadverdier(UttakResultatPeriodeEntitet up) {
        var samtidigUttaksprosent = up.getPeriodeSøknad().map(UttakResultatPeriodeSøknadEntitet::getSamtidigUttaksprosent).orElse(null);
        var builder = OppgittPeriodeBuilder.ny()
            .medPeriode(up.getTidsperiode().getFomDato(), up.getTidsperiode().getTomDato())
            .medPeriodeType(up.getPeriodeSøknad().map(UttakResultatPeriodeSøknadEntitet::getUttakPeriodeType).orElseGet(() -> VedtaksperioderHelper.finnPeriodetype(up)))
            .medSamtidigUttak(up.getPeriodeSøknad().map(UttakResultatPeriodeSøknadEntitet::isSamtidigUttak)
                .or(() -> Optional.ofNullable(samtidigUttaksprosent).map(SamtidigUttaksprosent::erSamtidigUttak)).orElse(false))
            .medSamtidigUttaksprosent(samtidigUttaksprosent)
            .medFlerbarnsdager(up.isFlerbarnsdager())
            .medGraderingAktivitetType(VedtaksperioderHelper.finnGradertAktivitetType(up))
            .medPeriodeKilde(FordelingPeriodeKilde.TIDLIGERE_VEDTAK);

        VedtaksperioderHelper.finnMorsAktivitet(up).ifPresent(builder::medMorsAktivitet);
        finnGraderingArbeidsprosentSøknad(up).ifPresent(builder::medArbeidsprosent);
        VedtaksperioderHelper.finnUtsettelsesÅrsak(up).ifPresent(builder::medÅrsak);
        VedtaksperioderHelper.finnGradertArbeidsgiver(up).ifPresent(builder::medArbeidsgiver);
        VedtaksperioderHelper.finnOppholdsÅrsak(up).ifPresent(builder::medÅrsak);
        VedtaksperioderHelper.finnOverføringÅrsak(up).ifPresent(builder::medÅrsak);
        builder.medMottattDato(up.getPeriodeSøknad().orElseThrow().getMottattDato());
        builder.medTidligstMottattDato(up.getPeriodeSøknad().orElseThrow().getTidligstMottattDato().orElse(null));

        return builder.build();
    }

    private static Optional<BigDecimal> finnGraderingArbeidsprosentSøknad(UttakResultatPeriodeEntitet up) {
        return up.getPeriodeSøknad().map(UttakResultatPeriodeSøknadEntitet::getGraderingArbeidsprosent);
    }
}
