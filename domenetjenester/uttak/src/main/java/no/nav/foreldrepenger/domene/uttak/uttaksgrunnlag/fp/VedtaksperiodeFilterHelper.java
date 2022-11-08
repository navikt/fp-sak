package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;

public class VedtaksperiodeFilterHelper {

    private VedtaksperiodeFilterHelper() {
        //
    }

    public static List<OppgittPeriodeEntitet> opprettOppgittePerioderKunInnvilget(UttakResultatEntitet uttakResultatFraForrigeBehandling, LocalDate perioderFom) {
        return uttakResultatFraForrigeBehandling.getGjeldendePerioder()
            .getPerioder()
            .stream()
            .filter(UttakResultatPeriodeEntitet::isInnvilget)
            .filter(p -> !p.getTom().isBefore(perioderFom))
            .filter(p -> p.getPeriodeSøknad().isPresent())
            .filter(p -> !p.getTidsperiode().erHelg())
            .map(VedtaksperiodeFilterHelper::konverter)
            .toList();
    }

    private static OppgittPeriodeEntitet konverter(UttakResultatPeriodeEntitet up) {
        var samtidigUttaksprosent = VedtaksperioderHelper.finnSamtidigUttaksprosent(up).orElse(null);
        var builder = OppgittPeriodeBuilder.ny()
            .medPeriode(up.getTidsperiode().getFomDato(), up.getTidsperiode().getTomDato())
            .medPeriodeType(finnPeriodetype(up))
            .medSamtidigUttak(SamtidigUttaksprosent.erSamtidigUttak(samtidigUttaksprosent))
            .medSamtidigUttaksprosent(samtidigUttaksprosent)
            .medFlerbarnsdager(up.isFlerbarnsdager())
            .medGraderingAktivitetType(VedtaksperioderHelper.finnGradertAktivitetType(up))
            .medPeriodeKilde(FordelingPeriodeKilde.TIDLIGERE_VEDTAK);

        VedtaksperioderHelper.finnMorsAktivitet(up).ifPresent(builder::medMorsAktivitet);
        VedtaksperioderHelper.finnDokumentasjonVurdering(up).ifPresent(builder::medDokumentasjonVurdering);
        VedtaksperioderHelper.finnGraderingArbeidsprosent(up).ifPresent(builder::medArbeidsprosent);
        VedtaksperioderHelper.finnUtsettelsesÅrsak(up).ifPresent(builder::medÅrsak);
        VedtaksperioderHelper.finnGradertArbeidsgiver(up).ifPresent(builder::medArbeidsgiver);
        VedtaksperioderHelper.finnOppholdsÅrsak(up).ifPresent(builder::medÅrsak);
        VedtaksperioderHelper.finnOverføringÅrsak(up).ifPresent(builder::medÅrsak);
        builder.medMottattDato(up.getPeriodeSøknad().orElseThrow().getMottattDato());
        builder.medTidligstMottattDato(up.getPeriodeSøknad().orElseThrow().getTidligstMottattDato().orElse(null));

        return builder.build();
    }

    private static UttakPeriodeType finnPeriodetype(UttakResultatPeriodeEntitet uttakResultatPeriode) {
        //Oppholdsperiode har ingen aktiviteter
        if (uttakResultatPeriode.isOpphold()) {
            return UttakPeriodeType.ANNET;
        }
        var harTrekkdager = uttakResultatPeriode.getAktiviteter().stream()
            .map(UttakResultatPeriodeAktivitetEntitet::getTrekkdager)
            .anyMatch(Trekkdager::merEnn0);
        // Utsettelse kommer normalt uten konto, mens det ofte er satt i UR, selvc uten trekkdager
        if (uttakResultatPeriode.isUtsettelse() && !harTrekkdager) {
            return UttakPeriodeType.UDEFINERT;
        }
        var stønadskontoType = uttakResultatPeriode.getAktiviteter()
            .stream()
            .max(Comparator.comparing(aktivitet -> aktivitet.getTrekkdager().decimalValue()))
            .map(UttakResultatPeriodeAktivitetEntitet::getTrekkonto);
        if (stønadskontoType.isPresent()) {
            return UttakEnumMapper.mapTilYf(stønadskontoType.get());
        }
        throw new IllegalStateException("Uttaksperiode mangler stønadskonto");
    }
}
