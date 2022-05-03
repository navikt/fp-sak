package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public class SøknadsperiodeDokKontrollererFamArbBalanseTest {

    private static final LocalDate FOM = LocalDate.of(2022, 1, 13);

    @Test
    public void farEllerMedmorSøktOmUttakRundtFødsel() {
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medSamtidigUttak(true)
            .medPeriode(FOM, FOM.plusWeeks(2).minusDays(3))
            .build();

        var fødselsDatoTilTidligOppstart = FOM;
        var kontrollerer = new SøknadsperiodeDokKontrollerer(List.of(), fødselsDatoTilTidligOppstart,
            new UtsettelseDokKontrollererFrittUttak(fødselsDatoTilTidligOppstart), List.of(),
            Optional.of(new LocalDateInterval(FOM, FOM.plusWeeks(2).minusDays(1))));

        var kontrollerFaktaPeriode = kontrollerer.kontrollerSøknadsperiode(oppgittPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
        assertThat(kontrollerFaktaPeriode.isTidligOppstart()).isFalse();
    }

    @Test
    public void farEllerMedmorSøktOmUttakRundtFødselForLangPeriode() {
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medSamtidigUttak(true)
            .medPeriode(FOM, FOM.plusWeeks(3))
            .build();

        var fødselsDatoTilTidligOppstart = FOM;
        var kontrollerer = new SøknadsperiodeDokKontrollerer(List.of(), fødselsDatoTilTidligOppstart,
            new UtsettelseDokKontrollererFrittUttak(fødselsDatoTilTidligOppstart), List.of(),
            Optional.of(new LocalDateInterval(FOM, FOM.plusWeeks(2).minusDays(1))));

        var kontrollerFaktaPeriode = kontrollerer.kontrollerSøknadsperiode(oppgittPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isFalse();
        assertThat(kontrollerFaktaPeriode.isTidligOppstart()).isTrue();
    }

    @Test
    public void bfhrSøktOmUttakRundtFødselMorTrengerHjelp() {
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMorsAktivitet(MorsAktivitet.TRENGER_HJELP)
            .medPeriode(FOM, FOM.plusWeeks(2).minusDays(3))
            .build();

        var fødselsDatoTilTidligOppstart = FOM;
        var kontrollerer = new SøknadsperiodeDokKontrollerer(List.of(), fødselsDatoTilTidligOppstart,
            new UtsettelseDokKontrollererFrittUttak(fødselsDatoTilTidligOppstart), List.of(),
            Optional.of(new LocalDateInterval(FOM, FOM.plusWeeks(2).minusDays(1))));

        var kontrollerFaktaPeriode = kontrollerer.kontrollerSøknadsperiode(oppgittPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isFalse();
        assertThat(kontrollerFaktaPeriode.isTidligOppstart()).isTrue();
    }

    @Test
    public void bfhrSøktOmUttakRundtFødselMorUfør() {
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMorsAktivitet(MorsAktivitet.UFØRE)
            .medPeriode(FOM, FOM.plusWeeks(2).minusDays(3))
            .build();

        var fødselsDatoTilTidligOppstart = FOM;
        var kontrollerer = new SøknadsperiodeDokKontrollerer(List.of(), fødselsDatoTilTidligOppstart,
            new UtsettelseDokKontrollererFrittUttak(fødselsDatoTilTidligOppstart), List.of(),
            Optional.of(new LocalDateInterval(FOM, FOM.plusWeeks(2).minusDays(1))));

        var kontrollerFaktaPeriode = kontrollerer.kontrollerSøknadsperiode(oppgittPeriode);
        assertThat(kontrollerFaktaPeriode.erBekreftet()).isTrue();
        assertThat(kontrollerFaktaPeriode.isTidligOppstart()).isFalse();
    }

}
