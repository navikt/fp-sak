package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

public class AvklarFaktaUttakTjenesteTest {

    private UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();
    private AvklarFaktaUttakPerioderTjeneste tjeneste = new AvklarFaktaUttakPerioderTjeneste(repositoryProvider.getYtelsesFordelingRepository());

    private Behandling opprettBehandlingForMorMedSøktePerioder(List<OppgittPeriodeEntitet> perioder) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var rettighet = new OppgittRettighetEntitet(true, false, false);
        scenario.medOppgittRettighet(rettighet);

        scenario.medFordeling(new OppgittFordelingEntitet(perioder, true));
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void utsettelseFerieErTilpassetInntektsmeldingen() {
        var fom = LocalDate.of(2018, 4, 18);
        var tom = fom.plusWeeks(1);
        var periode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(UtsettelseÅrsak.FERIE)
            .medBegrunnelse("bla bla")
            .medPeriode(fom, tom)
            .medVurdering(UttakPeriodeVurderingType.PERIODE_OK_ENDRET)
            .build();

        var uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isTrue();
    }

    @Test
    public void utsettelseFerieKanIkkeAvklares() {
        var fom = LocalDate.of(2018, 4, 18);
        var tom = fom.plusWeeks(1);
        var periode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(UtsettelseÅrsak.FERIE)
            .medBegrunnelse("bla bla")
            .medPeriode(fom, tom)
            .medVurdering(UttakPeriodeVurderingType.PERIODE_KAN_IKKE_AVKLARES)
            .build();

        var uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isFalse();
    }

    @Test
    public void utsettelseArbeidErVurdertOkAvSaksbehandler() {
        var fom = LocalDate.of(2018, 4, 18);
        var tom = fom.plusWeeks(1);
        var periode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medPeriode(fom, tom)
            .medBegrunnelse("bla bla")
            .medVurdering(UttakPeriodeVurderingType.PERIODE_OK)
            .build();

        var uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isTrue();
    }

    @Test
    public void utsettelseArbeidErAutomatiskVurdertOkDokumentertAvInntektsmelding() {
        var fom = LocalDate.of(2018, 4, 18);
        var tom = fom.plusWeeks(1);
        var periode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medPeriode(fom, tom)
            .medVurdering(UttakPeriodeVurderingType.PERIODE_IKKE_VURDERT)
            .build();

        var uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isNull();
    }

    @Test
    public void utsettelseArbeidKanIkkeAvklares() {
        var fom = LocalDate.of(2018, 4, 18);
        var tom = fom.plusWeeks(1);
        var periode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medPeriode(fom, tom)
            .medBegrunnelse("bla bla")
            .medVurdering(UttakPeriodeVurderingType.PERIODE_KAN_IKKE_AVKLARES)
            .build();

        var uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isFalse();
    }

    @Test
    public void graderingErTilpassetInntektsmeldingen() {
        var fom = LocalDate.of(2018, 4, 18);
        var tom = fom.plusWeeks(1);
        var periode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medArbeidsprosent(new BigDecimal(50))
            .medPeriode(fom, tom)
            .medBegrunnelse("bla bla")
            .medVurdering(UttakPeriodeVurderingType.PERIODE_OK_ENDRET)
            .build();

        var uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isTrue();
    }

    @Test
    public void graderingKanIkkeAvklares() {
        var fom = LocalDate.of(2018, 4, 18);
        var tom = fom.plusWeeks(1);
        var periode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medArbeidsprosent(new BigDecimal(50))
            .medPeriode(fom, tom)
            .medBegrunnelse("bla bla")
            .medVurdering(UttakPeriodeVurderingType.PERIODE_KAN_IKKE_AVKLARES)
            .build();

        var uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isFalse();
    }

    @Test
    public void utsettelseSykSøkerDokumentert() {
        var fom = LocalDate.of(2018, 4, 18);
        var tom = fom.plusWeeks(1);
        var periode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriode(fom, tom)
            .medVurdering(UttakPeriodeVurderingType.PERIODE_OK_ENDRET)
            .medBegrunnelse("bla bla")
            .build();

        var uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isTrue();
    }

    @Test
    public void utsettelseSykSøkerIkkeDokumentert() {
        var fom = LocalDate.of(2018, 4, 18);
        var tom = fom.plusWeeks(1);
        var periode1 = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriode(fom, tom)
            .medVurdering(UttakPeriodeVurderingType.PERIODE_IKKE_VURDERT)
            .medBegrunnelse("bla bla")
            .build();

        // Ingen dokumenterte perioder
        var uttakPeriodeEditDistance = tjeneste.mapPeriode(periode1);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isFalse();

        var periode2 = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriode(fom, tom)
            .medVurdering(UttakPeriodeVurderingType.PERIODE_OK)
            .medBegrunnelse("bla bla")
            .build();

        uttakPeriodeEditDistance = tjeneste.mapPeriode(periode2);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isTrue();
    }

    @Test
    public void utsettelseInnlagtSøkerDokumentert() {
        var fom = LocalDate.of(2018, 4, 18);
        var tom = fom.plusWeeks(1);
        var periode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medÅrsak(UtsettelseÅrsak.INSTITUSJON_SØKER)
            .medPeriode(fom, tom)
            .medVurdering(UttakPeriodeVurderingType.PERIODE_OK)
            .medBegrunnelse("bla bla")
            .build();

        var uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isTrue();

    }

    @Test
    public void utsettelseInnlagtBarnDokumentert() {
        var fom = LocalDate.of(2018, 4, 18);
        var tom = fom.plusWeeks(1);
        var periode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medÅrsak(UtsettelseÅrsak.INSTITUSJON_BARN)
            .medPeriode(fom, tom)
            .medBegrunnelse("bla bla")
            .medVurdering(UttakPeriodeVurderingType.PERIODE_OK)
            .build();

        var uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isTrue();
    }

    @Test
    public void mapperPeriodeSomIkkeErUtsettelseEllerGradering() {
        var fom = LocalDate.of(2018, 4, 18);
        var tom = fom.plusWeeks(1);
        var periode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(fom, tom)
            .build();

        var uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.getPeriode()).isEqualTo(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isNull();
    }

    @Test
    public void finnesOverlappendePerioder() {
        var fom = LocalDate.of(2018, 4, 18);
        var tom = fom.plusWeeks(1);

        var førstePeriode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(fom, tom)
            .build();

        var andrePeriode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(tom, tom.plusDays(1))
            .build();
        var behandling = opprettBehandlingForMorMedSøktePerioder(List.of(førstePeriode, andrePeriode));
        assertThat(tjeneste.finnesOverlappendePerioder(behandling.getId())).isTrue();
    }

    @Test
    public void finnesIkkeOverlappendePerioder() {
        var fom = LocalDate.of(2018, 4, 18);
        var tom = fom.plusWeeks(1);

        var førstePeriode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(fom, tom)
            .build();

        var andrePeriode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(tom.plusDays(1), tom.plusWeeks(1))
            .build();
        var behandling = opprettBehandlingForMorMedSøktePerioder(List.of(førstePeriode, andrePeriode));
        assertThat(tjeneste.finnesOverlappendePerioder(behandling.getId())).isFalse();
    }

}
