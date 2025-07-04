package no.nav.foreldrepenger.skjæringstidspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.FamilieHendelseDato;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;

@ExtendWith(MockitoExtension.class)
class StønadsperiodeTjenesteTest {

    @Mock
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BeregningsresultatRepository beregningsresultatRepository;
    @Mock
    private FpUttakRepository fpUttakRepository;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    private StønadsperiodeTjeneste stønadsperiodeTjeneste;

    @BeforeEach
    void setup() {
        stønadsperiodeTjeneste = new StønadsperiodeTjeneste(fagsakRelasjonTjeneste, behandlingRepository,
            fpUttakRepository, beregningsresultatRepository, skjæringstidspunktTjeneste);
    }

    @Test
    void skal_finne_stønadsperiode_svp() {
        // Arrange
        var skjæringsdato = LocalDate.now().minusDays(10);

        var førstegangScenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse()
            .medTerminbekreftelse(førstegangScenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(skjæringsdato.plusMonths(3)).medUtstedtDato(skjæringsdato).medNavnPå("Dr Dankel"));
        førstegangScenario.medBehandlingsresultat(new Behandlingsresultat.Builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        var behandling = førstegangScenario.lagMocked();
        behandling.avsluttBehandling();

        var tilkjent = lagBeregningsresultat(skjæringsdato, skjæringsdato.plusMonths(2).minusWeeks(3).minusDays(1));
        when(beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId())).thenReturn(Optional.of(tilkjent));

        // Act/Assert
        var periodeB = stønadsperiodeTjeneste.stønadsperiode(behandling);
        assertThat(periodeB).hasValueSatisfying(b -> {
            assertThat(b.getFomDato()).isEqualTo(skjæringsdato);
            assertThat(b.getTomDato()).isEqualTo(skjæringsdato.plusMonths(2).minusWeeks(3).minusDays(1));
        });

        // Act/Assert
        var periodeF =  stønadsperiodeTjeneste.stønadsperiode(behandling);
        assertThat(periodeF).isEqualTo(periodeB);
    }

    @Test
    void skal_returnere_innvilget_periode_ved_enkeltperson_sak() {
        // Arrange
        var skjæringsdato = VirkedagUtil.fomVirkedag(LocalDate.now().minusDays(30));
        var bekreftetfødselsdato = skjæringsdato.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse()
            .medTerminbekreftelse(førstegangScenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(bekreftetfødselsdato));
        førstegangScenario.medBehandlingsresultat(new Behandlingsresultat.Builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        var repoProvider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();
        behandling.avsluttBehandling();

        var ur = new UttakResultatEntitet.Builder(repoProvider.getBehandlingsresultatRepository().hent(behandling.getId()))
            .medOpprinneligPerioder(new UttakResultatPerioderEntitet()
                .leggTilPeriode(new UttakResultatPeriodeEntitet.Builder(skjæringsdato, skjæringsdato.plusWeeks(9).minusDays(1))
                        .medResultatType(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.SØKNADSFRIST).build())
                .leggTilPeriode(new UttakResultatPeriodeEntitet.Builder(skjæringsdato.plusWeeks(9), skjæringsdato.plusWeeks(15).minusDays(1))
                    .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE).build())
                .leggTilPeriode(new UttakResultatPeriodeEntitet.Builder(skjæringsdato.plusWeeks(15), skjæringsdato.plusWeeks(20).minusDays(1))
                    .medResultatType(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.IKKE_STØNADSDAGER_IGJEN).build())
            );
        when(fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(ur.build()));
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId())).thenReturn(Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(skjæringsdato).build());

        // Act/Assert
        var periode = stønadsperiodeTjeneste.stønadsperiode(behandling);
        assertThat(periode).hasValueSatisfying(b -> {
            assertThat(b.getFomDato()).isEqualTo(skjæringsdato);
            assertThat(b.getTomDato()).isEqualTo(skjæringsdato.plusWeeks(15).minusDays(1));
        });

        // Act/Assert
        var periodeF =  stønadsperiodeTjeneste.stønadsperiode(behandling);
        assertThat(periodeF).isEqualTo(periode);

    }

    @Test
    void skal_returnere_stp_periode_ved_tidlig_fødsel_og_ikke_søkt_fra_start() {
        // Arrange
        var skjæringsdato = VirkedagUtil.fomVirkedag(LocalDate.now().minusDays(30));
        var bekreftetfødselsdato = skjæringsdato.plusWeeks(2);
        var termindato = skjæringsdato.plusWeeks(2);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse()
            .medTerminbekreftelse(førstegangScenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato));
        førstegangScenario.medBehandlingsresultat(new Behandlingsresultat.Builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        var repoProvider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();
        behandling.avsluttBehandling();

        var ur = new UttakResultatEntitet.Builder(repoProvider.getBehandlingsresultatRepository().hent(behandling.getId()))
            .medOpprinneligPerioder(new UttakResultatPerioderEntitet()
                .leggTilPeriode(new UttakResultatPeriodeEntitet.Builder(skjæringsdato.plusWeeks(1), termindato.minusDays(1))
                    .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.INNVILGET_FORELDREPENGER_FØR_FØDSEL).build())
                .leggTilPeriode(new UttakResultatPeriodeEntitet.Builder(termindato, skjæringsdato.plusWeeks(15).minusDays(1))
                    .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE).build())
                .leggTilPeriode(new UttakResultatPeriodeEntitet.Builder(skjæringsdato.plusWeeks(15), skjæringsdato.plusWeeks(20).minusDays(1))
                    .medResultatType(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.IKKE_STØNADSDAGER_IGJEN).build())
            );
        when(fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(ur.build()));
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId())).thenReturn(Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(skjæringsdato).build());

        // Act/Assert
        var periode = stønadsperiodeTjeneste.stønadsperiode(behandling);
        assertThat(periode).hasValueSatisfying(b -> {
            assertThat(b.getFomDato()).isEqualTo(skjæringsdato);
            assertThat(b.getTomDato()).isEqualTo(skjæringsdato.plusWeeks(15).minusDays(1));
        });

        // Act/Assert
        var periodeF =  stønadsperiodeTjeneste.stønadsperiode(behandling);
        assertThat(periodeF).isEqualTo(periode);

    }

    @Test
    void skal_returnere_første_uttak_ved_adopsjon_og_senere_start() {
        // Arrange
        var skjæringsdato = VirkedagUtil.fomVirkedag(LocalDate.now().minusDays(30));
        var uttaksdato = skjæringsdato.plusWeeks(2);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse()
            .medAdopsjon(førstegangScenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(skjæringsdato));
        førstegangScenario.medBehandlingsresultat(new Behandlingsresultat.Builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        var repoProvider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();
        behandling.avsluttBehandling();

        var ur = new UttakResultatEntitet.Builder(repoProvider.getBehandlingsresultatRepository().hent(behandling.getId()))
            .medOpprinneligPerioder(new UttakResultatPerioderEntitet()
                .leggTilPeriode(new UttakResultatPeriodeEntitet.Builder(uttaksdato, uttaksdato.plusWeeks(15).minusDays(1))
                    .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE).build())
                .leggTilPeriode(new UttakResultatPeriodeEntitet.Builder(uttaksdato.plusWeeks(15), uttaksdato.plusWeeks(20).minusDays(1))
                    .medResultatType(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.IKKE_STØNADSDAGER_IGJEN).build())
            );
        when(fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(ur.build()));
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()))
            .thenReturn(Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(skjæringsdato)
                .medFamilieHendelseDato(FamilieHendelseDato.forAdopsjonOmsorg(skjæringsdato)).build());

        // Act/Assert
        var periode = stønadsperiodeTjeneste.stønadsperiode(behandling);
        assertThat(periode).hasValueSatisfying(b -> {
            assertThat(b.getFomDato()).isEqualTo(uttaksdato);
            assertThat(b.getTomDato()).isEqualTo(uttaksdato.plusWeeks(15).minusDays(1));
        });

        // Act/Assert
        var periodeF =  stønadsperiodeTjeneste.stønadsperiode(behandling);
        assertThat(periodeF).isEqualTo(periode);

    }


    @Test
    void skal_returnere_hele_innvilget_periode_ved_toperson_sak() {
        // Arrange
        var skjæringsdato = VirkedagUtil.fomVirkedag(LocalDate.now().minusDays(30));
        var bekreftetfødselsdato = skjæringsdato.plusWeeks(3);

        var førstegangScenarioFar = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenarioFar.medBekreftetHendelse().medFødselsDato(bekreftetfødselsdato);
        førstegangScenarioFar.medBehandlingsresultat(new Behandlingsresultat.Builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        var repoProviderFar = førstegangScenarioFar.mockBehandlingRepositoryProvider();
        var behandlingFar = førstegangScenarioFar.lagMocked();
        behandlingFar.avsluttBehandling();
        var førstegangScenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenarioMor.medBekreftetHendelse().medFødselsDato(bekreftetfødselsdato);
        førstegangScenarioMor.medBehandlingsresultat(new Behandlingsresultat.Builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        var repoProvider = førstegangScenarioMor.mockBehandlingRepositoryProvider();
        var behandlingMor = førstegangScenarioMor.lagMocked();
        behandlingMor.avsluttBehandling();

        var relasjon = mock(FagsakRelasjon.class);
        when(relasjon.getRelatertFagsakFraId(behandlingMor.getFagsakId())).thenReturn(Optional.of(behandlingFar.getFagsak()));
        when(relasjon.getRelatertFagsakFraId(behandlingFar.getFagsakId())).thenReturn(Optional.of(behandlingMor.getFagsak()));
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(anyLong())).thenReturn(Optional.of(relasjon));

        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandlingMor.getFagsakId())).thenReturn(Optional.of(behandlingMor));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandlingFar.getFagsakId())).thenReturn(Optional.of(behandlingFar));

        var urMor = new UttakResultatEntitet.Builder(repoProvider.getBehandlingsresultatRepository().hent(behandlingMor.getId()))
            .medOpprinneligPerioder(new UttakResultatPerioderEntitet()
                .leggTilPeriode(new UttakResultatPeriodeEntitet.Builder(skjæringsdato, skjæringsdato.plusWeeks(4).minusDays(1))
                    .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UTSETTELSE_GYLDIG_PGA_BARN_INNLAGT).medUtsettelseType(UttakUtsettelseType.BARN_INNLAGT).build())
                .leggTilPeriode(new UttakResultatPeriodeEntitet.Builder(skjæringsdato.plusWeeks(4), skjæringsdato.plusWeeks(19).minusDays(1))
                    .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE).build())
                .leggTilPeriode(new UttakResultatPeriodeEntitet.Builder(skjæringsdato.plusWeeks(25), skjæringsdato.plusWeeks(41).minusDays(1))
                    .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER).build())
            );
        when(fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingMor.getId())).thenReturn(Optional.of(urMor.build()));
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingMor.getId())).thenReturn(Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(skjæringsdato).build());
        var urFar = new UttakResultatEntitet.Builder(repoProviderFar.getBehandlingsresultatRepository().hent(behandlingFar.getId()))
            .medOpprinneligPerioder(new UttakResultatPerioderEntitet()
                .leggTilPeriode(new UttakResultatPeriodeEntitet.Builder(skjæringsdato.plusWeeks(19), skjæringsdato.plusWeeks(25).minusDays(1))
                    .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE).build())
                .leggTilPeriode(new UttakResultatPeriodeEntitet.Builder(skjæringsdato.plusWeeks(41), skjæringsdato.plusWeeks(50).minusDays(1))
                    .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE).build())
                .leggTilPeriode(new UttakResultatPeriodeEntitet.Builder(skjæringsdato.plusWeeks(50), skjæringsdato.plusWeeks(55).minusDays(1))
                    .medResultatType(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.IKKE_STØNADSDAGER_IGJEN).build())
            );
        when(fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingFar.getId())).thenReturn(Optional.of(urFar.build()));

        // Act/Assert
        var periode = stønadsperiodeTjeneste.stønadsperiode(behandlingFar);
        assertThat(periode).hasValueSatisfying(b -> {
            assertThat(b.getFomDato()).isEqualTo(skjæringsdato);
            assertThat(b.getTomDato()).isEqualTo(skjæringsdato.plusWeeks(50).minusDays(1));
        });


        // Act/Assert
        var periodeSak =  stønadsperiodeTjeneste.stønadsperiode(behandlingMor);
        assertThat(periodeSak).isEqualTo(periode);

    }


    private BeregningsresultatEntitet lagBeregningsresultat(LocalDate periodeFom,
                                                            LocalDate periodeTom) {
        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("sporing")
            .build();
        var beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(periodeFom, periodeTom)
            .build(beregningsresultat);
        BeregningsresultatAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats(1000)
            .medDagsatsFraBg(1000)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(beregningsresultatPeriode);
        return beregningsresultat;
    }


}
