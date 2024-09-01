package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;


class EndringsdatoBerørtUtlederTest {

    @Test
    void skal_ikke_opprette_berørt_hvis_eneste_overlapp_er_msp_med_innvilget_periode() {

        var fødselsdato = LocalDate.of(2022, 10, 20);
        var mødrekvote = new ForeldrepengerUttakPeriode.Builder().medAktiviteter(List.of(
                new ForeldrepengerUttakPeriodeAktivitet.Builder().medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS))
                    .medTrekkdager(new Trekkdager(75))
                    .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
                    .medTrekkonto(UttakPeriodeType.MØDREKVOTE)
                    .medArbeidsprosent(BigDecimal.ZERO)
                    .build()))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medTidsperiode(fødselsdato, fødselsdato.plusWeeks(15).minusDays(1))
            .build();
        var avlsåttMsp = new ForeldrepengerUttakPeriode.Builder().medAktiviteter(List.of(
                new ForeldrepengerUttakPeriodeAktivitet.Builder().medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS))
                    .medTrekkdager(new Trekkdager(1))
                    .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
                    .medTrekkonto(UttakPeriodeType.FELLESPERIODE)
                    .medArbeidsprosent(BigDecimal.ZERO)
                    .build()))
            .medResultatÅrsak(PeriodeResultatÅrsak.HULL_MELLOM_FORELDRENES_PERIODER)
            .medResultatType(PeriodeResultatType.AVSLÅTT)
            .medErFraSøknad(false)
            .medTidsperiode(fødselsdato.plusWeeks(15), fødselsdato.plusWeeks(15).plusDays(1))
            .build();
        var morsPerioder = List.of(mødrekvote, avlsåttMsp);
        var berørtUttak = new ForeldrepengerUttak(morsPerioder);
        var utløsendeYfa = lagYtelseFordelingAggregat(avlsåttMsp.getFom());
        var fedrekvote = new ForeldrepengerUttakPeriode.Builder().medAktiviteter(List.of(
                new ForeldrepengerUttakPeriodeAktivitet.Builder().medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS))
                    .medTrekkdager(new Trekkdager(1))
                    .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
                    .medTrekkonto(UttakPeriodeType.FEDREKVOTE)
                    .medArbeidsprosent(BigDecimal.ZERO)
                    .build()))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medTidsperiode(fødselsdato.plusWeeks(15), fødselsdato.plusWeeks(15).plusDays(1))
            .build();
        var utløsendeUttak = new ForeldrepengerUttak(List.of(fedrekvote));
        var repositoryProvider = new UttakRepositoryStubProvider();
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        var behandling = scenario
            .lagre(repositoryProvider);
        var resultat = EndringsdatoBerørtUtleder.utledEndringsdatoForBerørtBehandling(utløsendeUttak, utløsendeYfa,
            false, Optional.of(berørtUttak), getUttakInput(behandling, fødselsdato), "");

        assertThat(resultat).isEmpty();
    }

    private static UttakInput getUttakInput(Behandling behandling, LocalDate fødselsdato) {
        return new UttakInput(BehandlingReferanse.fra(behandling), null, null, new ForeldrepengerGrunnlag().medFamilieHendelser(
            new FamilieHendelser().medBekreftetHendelse(FamilieHendelse.forFødsel(null, fødselsdato, List.of(), 1))));
    }

    private Behandlingsresultat lagBehandlingsresultat() {
        return Behandlingsresultat.builder().build();
    }

    private static YtelseFordelingAggregat lagYtelseFordelingAggregat(LocalDate endringsdato) {
        return YtelseFordelingAggregat.oppdatere(Optional.empty())
            .medAvklarteDatoer(new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(endringsdato).build())
            .medSakskompleksDekningsgrad(Dekningsgrad._100)
            .build();
    }

}
