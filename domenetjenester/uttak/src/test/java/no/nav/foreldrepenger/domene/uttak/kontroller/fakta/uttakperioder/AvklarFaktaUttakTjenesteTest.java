package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class AvklarFaktaUttakTjenesteTest extends EntityManagerAwareTest {

    private UttakRepositoryProvider repositoryProvider;
    private AvklarFaktaUttakPerioderTjeneste tjeneste;

    @BeforeEach
    public void setup() {
        repositoryProvider = new UttakRepositoryProvider(getEntityManager());
        tjeneste = new AvklarFaktaUttakPerioderTjeneste(repositoryProvider.getYtelsesFordelingRepository());
    }

    private Behandling opprettBehandlingForMorMedSøktePerioder(List<OppgittPeriodeEntitet> perioder) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(true, true, false);
        scenario.medOppgittRettighet(rettighet);

        scenario.medFordeling(new OppgittFordelingEntitet(perioder, true));
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void utsettelseFerieErTilpassetInntektsmeldingen() {
        LocalDate fom = LocalDate.of(2018, 4, 18);
        LocalDate tom = fom.plusWeeks(1);
        OppgittPeriodeEntitet periode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(UtsettelseÅrsak.FERIE)
            .medBegrunnelse("bla bla")
            .medPeriode(fom, tom)
            .medVurdering(UttakPeriodeVurderingType.PERIODE_OK_ENDRET)
            .build();

        UttakPeriodeEditDistance uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isTrue();
    }

    @Test
    public void utsettelseFerieKanIkkeAvklares() {
        LocalDate fom = LocalDate.of(2018, 4, 18);
        LocalDate tom = fom.plusWeeks(1);
        OppgittPeriodeEntitet periode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(UtsettelseÅrsak.FERIE)
            .medBegrunnelse("bla bla")
            .medPeriode(fom, tom)
            .medVurdering(UttakPeriodeVurderingType.PERIODE_KAN_IKKE_AVKLARES)
            .build();

        UttakPeriodeEditDistance uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isFalse();
    }

    @Test
    public void utsettelseArbeidErVurdertOkAvSaksbehandler() {
        LocalDate fom = LocalDate.of(2018, 4, 18);
        LocalDate tom = fom.plusWeeks(1);
        OppgittPeriodeEntitet periode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medPeriode(fom, tom)
            .medBegrunnelse("bla bla")
            .medVurdering(UttakPeriodeVurderingType.PERIODE_OK)
            .build();

        UttakPeriodeEditDistance uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isTrue();
    }

    @Test
    public void utsettelseArbeidErAutomatiskVurdertOkDokumentertAvInntektsmelding() {
        LocalDate fom = LocalDate.of(2018, 4, 18);
        LocalDate tom = fom.plusWeeks(1);
        OppgittPeriodeEntitet periode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medPeriode(fom, tom)
            .medVurdering(UttakPeriodeVurderingType.PERIODE_IKKE_VURDERT)
            .build();

        UttakPeriodeEditDistance uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isNull();
    }

    @Test
    public void utsettelseArbeidKanIkkeAvklares() {
        LocalDate fom = LocalDate.of(2018, 4, 18);
        LocalDate tom = fom.plusWeeks(1);
        OppgittPeriodeEntitet periode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medPeriode(fom, tom)
            .medBegrunnelse("bla bla")
            .medVurdering(UttakPeriodeVurderingType.PERIODE_KAN_IKKE_AVKLARES)
            .build();

        UttakPeriodeEditDistance uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isFalse();
    }

    @Test
    public void graderingErTilpassetInntektsmeldingen() {
        LocalDate fom = LocalDate.of(2018, 4, 18);
        LocalDate tom = fom.plusWeeks(1);
        OppgittPeriodeEntitet periode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medArbeidsprosent(new BigDecimal(50))
            .medPeriode(fom, tom)
            .medBegrunnelse("bla bla")
            .medVurdering(UttakPeriodeVurderingType.PERIODE_OK_ENDRET)
            .build();

        UttakPeriodeEditDistance uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isTrue();
    }

    @Test
    public void graderingKanIkkeAvklares() {
        LocalDate fom = LocalDate.of(2018, 4, 18);
        LocalDate tom = fom.plusWeeks(1);
        OppgittPeriodeEntitet periode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medArbeidsprosent(new BigDecimal(50))
            .medPeriode(fom, tom)
            .medBegrunnelse("bla bla")
            .medVurdering(UttakPeriodeVurderingType.PERIODE_KAN_IKKE_AVKLARES)
            .build();

        UttakPeriodeEditDistance uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isFalse();
    }

    @Test
    public void utsettelseSykSøkerDokumentert() {
        LocalDate fom = LocalDate.of(2018, 4, 18);
        LocalDate tom = fom.plusWeeks(1);
        OppgittPeriodeEntitet periode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriode(fom, tom)
            .medVurdering(UttakPeriodeVurderingType.PERIODE_OK_ENDRET)
            .medBegrunnelse("bla bla")
            .build();

        UttakPeriodeEditDistance uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isTrue();
    }

    @Test
    public void utsettelseSykSøkerIkkeDokumentert() {
        LocalDate fom = LocalDate.of(2018, 4, 18);
        LocalDate tom = fom.plusWeeks(1);
        OppgittPeriodeEntitet periode1 = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriode(fom, tom)
            .medVurdering(UttakPeriodeVurderingType.PERIODE_IKKE_VURDERT)
            .medBegrunnelse("bla bla")
            .build();

        // Ingen dokumenterte perioder
        UttakPeriodeEditDistance uttakPeriodeEditDistance = tjeneste.mapPeriode(periode1);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isFalse();

        OppgittPeriodeEntitet periode2 = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FELLESPERIODE)
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
        LocalDate fom = LocalDate.of(2018, 4, 18);
        LocalDate tom = fom.plusWeeks(1);
        OppgittPeriodeEntitet periode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medÅrsak(UtsettelseÅrsak.INSTITUSJON_SØKER)
            .medPeriode(fom, tom)
            .medVurdering(UttakPeriodeVurderingType.PERIODE_OK)
            .medBegrunnelse("bla bla")
            .build();

        UttakPeriodeEditDistance uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isTrue();

    }

    @Test
    public void utsettelseInnlagtBarnDokumentert() {
        LocalDate fom = LocalDate.of(2018, 4, 18);
        LocalDate tom = fom.plusWeeks(1);
        OppgittPeriodeEntitet periode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medÅrsak(UtsettelseÅrsak.INSTITUSJON_BARN)
            .medPeriode(fom, tom)
            .medBegrunnelse("bla bla")
            .medVurdering(UttakPeriodeVurderingType.PERIODE_OK)
            .build();

        UttakPeriodeEditDistance uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isTrue();
    }

    @Test
    public void mapperPeriodeSomIkkeErUtsettelseEllerGradering() {
        LocalDate fom = LocalDate.of(2018, 4, 18);
        LocalDate tom = fom.plusWeeks(1);
        OppgittPeriodeEntitet periode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(fom, tom)
            .build();

        UttakPeriodeEditDistance uttakPeriodeEditDistance = tjeneste.mapPeriode(periode);
        assertThat(uttakPeriodeEditDistance.getPeriode()).isEqualTo(periode);
        assertThat(uttakPeriodeEditDistance.isPeriodeDokumentert()).isNull();
    }

    @Test
    public void finnesOverlappendePerioder() {
        LocalDate fom = LocalDate.of(2018, 4, 18);
        LocalDate tom = fom.plusWeeks(1);

        OppgittPeriodeEntitet førstePeriode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(fom, tom)
            .build();

        OppgittPeriodeEntitet andrePeriode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(tom, tom.plusDays(1))
            .build();
        Behandling behandling = opprettBehandlingForMorMedSøktePerioder(List.of(førstePeriode, andrePeriode));
        assertThat(tjeneste.finnesOverlappendePerioder(behandling.getId())).isTrue();
    }

    @Test
    public void finnesIkkeOverlappendePerioder() {
        LocalDate fom = LocalDate.of(2018, 4, 18);
        LocalDate tom = fom.plusWeeks(1);

        OppgittPeriodeEntitet førstePeriode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(fom, tom)
            .build();

        OppgittPeriodeEntitet andrePeriode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(tom.plusDays(1), tom.plusWeeks(1))
            .build();
        Behandling behandling = opprettBehandlingForMorMedSøktePerioder(List.of(førstePeriode, andrePeriode));
        assertThat(tjeneste.finnesOverlappendePerioder(behandling.getId())).isFalse();
    }

}
