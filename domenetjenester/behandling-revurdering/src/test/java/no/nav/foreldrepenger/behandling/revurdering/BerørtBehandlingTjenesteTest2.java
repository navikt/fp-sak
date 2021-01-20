package no.nav.foreldrepenger.behandling.revurdering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
class BerørtBehandlingTjenesteTest2 {

    private BehandlingRepositoryProvider repositoryProvider;
    private BerørtBehandlingTjeneste tjeneste;

    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private StønadskontoSaldoTjeneste saldoTjeneste;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        foreldrepengerUttakTjeneste = mock(ForeldrepengerUttakTjeneste.class);
        uttakInputTjeneste = mock(UttakInputTjeneste.class);
        saldoTjeneste = mock(StønadskontoSaldoTjeneste.class);
        tjeneste = new BerørtBehandlingTjeneste(saldoTjeneste, repositoryProvider, uttakInputTjeneste,
            foreldrepengerUttakTjeneste,
            new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository()));
    }

    @Test
    public void tapende_behandling_skal_ikke_opprette_berørt() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var scenarioAnnenpart = ScenarioFarSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingAnnenpart = scenarioAnnenpart.lagre(repositoryProvider);

        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandling), null,
            new ForeldrepengerGrunnlag().medErTapendeBehandling(true));
        when(uttakInputTjeneste.lagInput(behandling.getId())).thenReturn(uttakInput);

        var resultat = skalBerørtOpprettes(behandling, behandlingAnnenpart);

        assertThat(resultat).isFalse();
    }

    @Test
    public void annenpart_uten_uttak_skal_ikke_opprette_berørt() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var scenarioAnnenpart = ScenarioFarSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingAnnenpart = scenarioAnnenpart.lagre(repositoryProvider);

        lagUttakInput(behandling);

        lagreUttak(behandlingAnnenpart, null);

        var resultat = skalBerørtOpprettes(behandling, behandlingAnnenpart);

        assertThat(resultat).isFalse();
    }

    @Test
    public void behandling_uten_uttak_skal_ikke_opprette_berørt() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var scenarioAnnenpart = ScenarioFarSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingAnnenpart = scenarioAnnenpart.lagre(repositoryProvider);

        lagUttakInput(behandling);

        var annenpartsUttak = new ForeldrepengerUttak(List.of(
            new ForeldrepengerUttakPeriode.Builder().medTidsperiode(LocalDate.now(), LocalDate.now().plusWeeks(10))
                .medResultatÅrsak(InnvilgetÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER)
                .build()));
        lagreUttak(behandlingAnnenpart, annenpartsUttak);
        lagreUttak(behandling, null);

        var resultat = skalBerørtOpprettes(behandling, behandlingAnnenpart);

        assertThat(resultat).isFalse();
    }

    @Test
    public void behandling_med_negativ_saldo_skal_opprette_berørt() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var scenarioAnnenpart = ScenarioFarSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingAnnenpart = scenarioAnnenpart.lagre(repositoryProvider);

        var uttakInput = lagUttakInput(behandling);

        when(saldoTjeneste.erNegativSaldoPåNoenKonto(uttakInput)).thenReturn(true);

        var resultat = skalBerørtOpprettes(behandling, behandlingAnnenpart);

        assertThat(resultat).isTrue();
    }

    private UttakInput lagUttakInput(Behandling behandling) {
        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandling), null, new ForeldrepengerGrunnlag());
        when(uttakInputTjeneste.lagInput(behandling.getId())).thenReturn(uttakInput);
        return uttakInput;
    }

    @Test
    public void behandling_med_endret_stønadskonto_skal_opprette_berørt() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var scenarioAnnenpart = ScenarioFarSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingAnnenpart = scenarioAnnenpart.lagre(repositoryProvider);

        lagUttakInput(behandling);

        var br = Behandlingsresultat.builder().medEndretStønadskonto(true).buildFor(behandling);

        var resultat = tjeneste.skalBerørtBehandlingOpprettesNy(br, behandling.getId(), behandlingAnnenpart.getId());

        assertThat(resultat).isTrue();
    }

    @Test
    public void ikke_berørt_behandling_når_far_har_uttak_i_mellom_mors_uttaksperioder() {
        var startDatoMor = LocalDate.of(2020, 1, 1);
        var startDatoFar = startDatoMor.plusMonths(3);

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel().medAvklarteUttakDatoer(avklarteDatoer(startDatoFar));
        var scenarioAnnenpart = ScenarioMorSøkerForeldrepenger.forFødsel();
        var farBehandling = scenario.lagre(repositoryProvider);
        var morBehandling = scenarioAnnenpart.lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsFørstePeriode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(startDatoMor,
            startDatoFar.minusDays(1))
            .medResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(StønadskontoType.MØDREKVOTE)))
            .build();
        var farsPeriode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(startDatoFar,
            startDatoFar.plusWeeks(10))
            .medResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(StønadskontoType.FEDREKVOTE)))
            .build();
        var morsAndrePeriode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(farsPeriode.getTom().plusDays(1),
            farsPeriode.getTom().plusWeeks(5))
            .medResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(StønadskontoType.MØDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsFørstePeriode, morsAndrePeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsPeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isFalse();
    }

    @Test
    public void ikke_berørt_behandling_når_far_har_overlapp_med_oppholdsperiode_i_mors_uttak() {
        var startDatoMor = LocalDate.of(2020, 1, 1);
        var startDatoFar = startDatoMor.plusMonths(3);

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel().medAvklarteUttakDatoer(avklarteDatoer(startDatoFar));
        var scenarioAnnenpart = ScenarioMorSøkerForeldrepenger.forFødsel();
        var farBehandling = scenario.lagre(repositoryProvider);
        var morBehandling = scenarioAnnenpart.lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsFørstePeriode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(startDatoMor,
            startDatoFar.minusDays(1))
            .medResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(StønadskontoType.MØDREKVOTE)))
            .build();
        var morsOppholdsperiode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(
            morsFørstePeriode.getTom().plusDays(1), startDatoFar).medResultatÅrsak(PeriodeResultatÅrsak.UKJENT).build();
        var farsPeriode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(morsOppholdsperiode.getFom(),
            morsOppholdsperiode.getTom())
            .medResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(StønadskontoType.FEDREKVOTE)))
            .build();
        var morsAndrePeriode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(farsPeriode.getTom().plusDays(1),
            farsPeriode.getTom().plusWeeks(5))
            .medResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(StønadskontoType.MØDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsFørstePeriode, morsOppholdsperiode, morsAndrePeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsPeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isFalse();
    }

    @Test
    public void berørt_behandling_ved_overlapp_mellom_partene() {
        var startDatoMor = LocalDate.of(2020, 1, 1);

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel().medAvklarteUttakDatoer(avklarteDatoer(startDatoMor));
        var scenarioAnnenpart = ScenarioMorSøkerForeldrepenger.forFødsel();
        var farBehandling = scenario.lagre(repositoryProvider);
        var morBehandling = scenarioAnnenpart.lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsPeriode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(startDatoMor,
            startDatoMor.plusWeeks(15))
            .medResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(StønadskontoType.MØDREKVOTE)))
            .build();
        var farsPeriode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(morsPeriode.getFom().plusWeeks(1),
            morsPeriode.getTom().minusWeeks(1))
            .medResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(StønadskontoType.FEDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsPeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsPeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isTrue();
    }

    @Test
    public void berørt_behandling_ved_overlapp_mellom_partene_der_annenpart_har_innvilget_utsettelse() {
        var startDatoMor = LocalDate.of(2020, 1, 1);

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel().medAvklarteUttakDatoer(avklarteDatoer(startDatoMor));
        var scenarioAnnenpart = ScenarioMorSøkerForeldrepenger.forFødsel();
        var farBehandling = scenario.lagre(repositoryProvider);
        var morBehandling = scenarioAnnenpart.lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsPeriode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(startDatoMor,
            startDatoMor.plusWeeks(15))
            .medResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medUtsettelseType(UttakUtsettelseType.FERIE)
            .medAktiviteter(List.of(new ForeldrepengerUttakPeriodeAktivitet.Builder().medArbeidsprosent(BigDecimal.ZERO)
                .medTrekkdager(Trekkdager.ZERO)
                .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
                .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS))
                .build()))
            .build();
        var farsPeriode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(morsPeriode.getFom().plusWeeks(1),
            morsPeriode.getTom().minusWeeks(1))
            .medResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(StønadskontoType.FEDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsPeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsPeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isTrue();
    }

    @Test
    public void ikke_berørt_behandling_ved_overlapp_mellom_partene_før_endringsdato() {
        var startDatoMor = LocalDate.of(2020, 1, 1);
        var endringsdato = startDatoMor.plusMonths(3);

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel().medAvklarteUttakDatoer(avklarteDatoer(endringsdato));
        var scenarioAnnenpart = ScenarioMorSøkerForeldrepenger.forFødsel();
        var farBehandling = scenario.lagre(repositoryProvider);
        var morBehandling = scenarioAnnenpart.lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsFørstePeriode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(startDatoMor,
            endringsdato.minusDays(1))
            .medResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(StønadskontoType.MØDREKVOTE)))
            .build();
        var farsFørstePeriode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(morsFørstePeriode.getFom(),
            morsFørstePeriode.getTom())
            .medResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(StønadskontoType.FEDREKVOTE)))
            .build();
        //Bare fars periode etter endringsdato
        var farsAndrePerioder = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(endringsdato,
            endringsdato.plusWeeks(2))
            .medResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(StønadskontoType.FEDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsFørstePeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsFørstePeriode, farsAndrePerioder));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isFalse();
    }

    @Test
    public void berørt_behandling_hvis_et_avslag_fører_til_hull_felles_plan() {
        var startDatoMor = LocalDate.of(2020, 1, 1);

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel().medAvklarteUttakDatoer(avklarteDatoer(startDatoMor));
        var scenarioAnnenpart = ScenarioMorSøkerForeldrepenger.forFødsel();
        var farBehandling = scenario.lagre(repositoryProvider);
        var morBehandling = scenarioAnnenpart.lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsFørstePeriode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(startDatoMor,
            startDatoMor.plusWeeks(1))
            .medResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(StønadskontoType.MØDREKVOTE)))
            .build();
        //Denne perioden er avslått og skaper derfor hull som må fylles av mor
        var farsFørstePeriode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(
            morsFørstePeriode.getTom().plusDays(1), morsFørstePeriode.getTom().plusWeeks(1))
            .medResultatÅrsak(IkkeOppfyltÅrsak.IKKE_STØNADSDAGER_IGJEN)
            .medAktiviteter(List.of(avslåttUttakPeriodeAktivitet(StønadskontoType.FEDREKVOTE)))
            .build();
        var morsAndrePeriode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(
            farsFørstePeriode.getTom().plusDays(1), farsFørstePeriode.getTom().plusWeeks(1))
            .medResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(StønadskontoType.MØDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsFørstePeriode, morsAndrePeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsFørstePeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isTrue();
    }

    @Test
    public void ikke_berørt_behandling_hvis_bruker_går_tom_for_dager_på_slutten_av_felles_uttaksplan() {
        var startDatoMor = LocalDate.of(2020, 1, 1);

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel().medAvklarteUttakDatoer(avklarteDatoer(startDatoMor));
        var scenarioAnnenpart = ScenarioMorSøkerForeldrepenger.forFødsel();
        var farBehandling = scenario.lagre(repositoryProvider);
        var morBehandling = scenarioAnnenpart.lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsFørstePeriode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(startDatoMor,
            startDatoMor.plusWeeks(1))
            .medResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(StønadskontoType.MØDREKVOTE)))
            .build();
        var farsFørstePeriode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(
            morsFørstePeriode.getTom().plusDays(1), morsFørstePeriode.getTom().plusWeeks(1))
            .medResultatÅrsak(IkkeOppfyltÅrsak.IKKE_STØNADSDAGER_IGJEN)
            .medAktiviteter(List.of(avslåttUttakPeriodeAktivitet(StønadskontoType.FEDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsFørstePeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsFørstePeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isFalse();
    }

    @Test
    public void ikke_berørt_behandling_ved_overlapp_mellom_partene_når_br_konsekvens_er_ingen_endring() {
        var startDatoMor = LocalDate.of(2020, 1, 1);

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel().medAvklarteUttakDatoer(avklarteDatoer(startDatoMor));
        var scenarioAnnenpart = ScenarioMorSøkerForeldrepenger.forFødsel();
        var farBehandling = scenario.lagre(repositoryProvider);
        var morBehandling = scenarioAnnenpart.lagre(repositoryProvider);

        var brMedIngenEndring = Behandlingsresultat.builderEndreEksisterende(getBehandlingsresultat(farBehandling.getId()))
            .leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.INGEN_ENDRING)
            .build();
        getBehandlingsresultatRepository().lagre(farBehandling.getId(), brMedIngenEndring);

        lagUttakInput(farBehandling);

        var morsPeriode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(startDatoMor,
            startDatoMor.plusWeeks(15))
            .medResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(StønadskontoType.MØDREKVOTE)))
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(new SamtidigUttaksprosent(50))
            .build();
        var farsPeriode = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(morsPeriode.getFom().plusWeeks(1),
            morsPeriode.getTom().minusWeeks(1))
            .medResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(StønadskontoType.FEDREKVOTE)))
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(new SamtidigUttaksprosent(50))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsPeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsPeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isFalse();
    }

    private boolean skalBerørtOpprettes(Behandling behandling, Behandling annenpartsBehandling) {
        return tjeneste.skalBerørtBehandlingOpprettesNy(getBehandlingsresultat(behandling.getId()), behandling.getId(),
            annenpartsBehandling.getId());
    }

    private BehandlingsresultatRepository getBehandlingsresultatRepository() {
        return repositoryProvider.getBehandlingsresultatRepository();
    }

    private void lagreUttak(Behandling behandling, ForeldrepengerUttak uttak) {
        when(foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(behandling.getId())).thenReturn(
            Optional.ofNullable(uttak));
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return getBehandlingsresultatRepository().hent(behandlingId);
    }

    private AvklarteUttakDatoerEntitet avklarteDatoer(LocalDate endringsdato) {
        return new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(endringsdato).build();
    }

    private ForeldrepengerUttakPeriodeAktivitet uttakPeriodeAktivitet(StønadskontoType stønadskontoType) {
        return new ForeldrepengerUttakPeriodeAktivitet.Builder().medArbeidsprosent(BigDecimal.ZERO)
            .medTrekkdager(new Trekkdager(10))
            .medTrekkonto(stønadskontoType)
            .medUtbetalingsgrad(new Utbetalingsgrad(100))
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS))
            .build();
    }

    private ForeldrepengerUttakPeriodeAktivitet avslåttUttakPeriodeAktivitet(StønadskontoType stønadskontoType) {
        return new ForeldrepengerUttakPeriodeAktivitet.Builder().medArbeidsprosent(BigDecimal.ZERO)
            .medTrekkdager(Trekkdager.ZERO)
            .medTrekkonto(stønadskontoType)
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS))
            .build();
    }
}
