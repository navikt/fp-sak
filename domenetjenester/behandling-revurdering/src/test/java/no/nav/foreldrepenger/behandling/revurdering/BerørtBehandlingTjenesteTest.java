package no.nav.foreldrepenger.behandling.revurdering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ExtendWith(JpaExtension.class)
class BerørtBehandlingTjenesteTest {

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
        tjeneste = new BerørtBehandlingTjeneste(saldoTjeneste, uttakInputTjeneste,
            foreldrepengerUttakTjeneste,
            new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository()));
    }

    @Test
    void berørt_behandling_skal_ikke_opprette_berørt() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultOppgittDekningsgrad().lagre(repositoryProvider);
        var behandlingAnnenpart = ScenarioFarSøkerForeldrepenger.forFødsel().medDefaultOppgittDekningsgrad().lagre(repositoryProvider);

        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandling), null, null,
            new ForeldrepengerGrunnlag().medErBerørtBehandling(true));
        when(uttakInputTjeneste.lagInput(behandling.getId())).thenReturn(uttakInput);

        var resultat = skalBerørtOpprettes(behandling, behandlingAnnenpart);

        assertThat(resultat).isFalse();
    }

    @Test
    void annenpart_uten_uttak_skal_ikke_opprette_berørt() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultOppgittDekningsgrad().lagre(repositoryProvider);
        var behandlingAnnenpart = ScenarioFarSøkerForeldrepenger.forFødsel().medDefaultOppgittDekningsgrad().lagre(repositoryProvider);

        lagUttakInput(behandling);

        lagreUttak(behandlingAnnenpart, null);

        var resultat = skalBerørtOpprettes(behandling, behandlingAnnenpart);

        assertThat(resultat).isFalse();
    }

    @Test
    void behandling_uten_uttak_skal_ikke_opprette_berørt() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultOppgittDekningsgrad()
            .medFødselAdopsjonsdato(LocalDate.now())
            .lagre(repositoryProvider);
        var behandlingAnnenpart = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medDefaultOppgittDekningsgrad()
            .medFødselAdopsjonsdato(LocalDate.now())
            .lagre(repositoryProvider);

        lagUttakInput(behandling);

        var annenpartsUttak = new ForeldrepengerUttak(List.of(
            new ForeldrepengerUttakPeriode.Builder()
                .medTidsperiode(LocalDate.now(), LocalDate.now().plusWeeks(10))
                .medResultatÅrsak(PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER)
                .build()));
        lagreUttak(behandlingAnnenpart, annenpartsUttak);
        lagreUttak(behandling, null);

        var resultat = skalBerørtOpprettes(behandling, behandlingAnnenpart);

        assertThat(resultat).isFalse();
    }

    @Test
    void behandling_med_negativ_saldo_skal_opprette_berørt() {
        var basedato = LocalDate.now();

        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultOppgittDekningsgrad()
            .medFødselAdopsjonsdato(basedato.minusWeeks(7))
            .lagre(repositoryProvider);
        var behandlingAnnenpart = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medDefaultOppgittDekningsgrad()
            .medFødselAdopsjonsdato(basedato.minusWeeks(7))
            .lagre(repositoryProvider);

        var morsMK = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(basedato.minusWeeks(7), basedato.minusWeeks(1).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var morsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(basedato, basedato.plusWeeks(20).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FELLESPERIODE)))
            .build();
        var farsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(basedato.plusWeeks(20), basedato.plusWeeks(33).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsMK, morsPeriode));
        lagreUttak(behandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsPeriode));
        lagreUttak(behandlingAnnenpart, farUttak);

        var uttakInput = lagUttakInput(behandling);

        when(saldoTjeneste.erNegativSaldoPåNoenKonto(uttakInput)).thenReturn(true);

        var resultat = skalBerørtOpprettes(behandling, behandlingAnnenpart);

        assertThat(resultat).isTrue();
    }

    private UttakInput lagUttakInput(Behandling behandling) {
        var fødselsdato = repositoryProvider.getFamilieHendelseRepository()
            .hentAggregat(behandling.getId())
            .getGjeldendeVersjon()
            .getFødselsdato().orElse(null);
        var fpGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1)));
        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandling), null, null, fpGrunnlag);
        when(uttakInputTjeneste.lagInput(behandling.getId())).thenReturn(uttakInput);
        return uttakInput;
    }

    private UttakInput lagUttakInputSammenhengendeUttak(Behandling behandling) {
        var fødselsdato = repositoryProvider.getFamilieHendelseRepository()
            .hentAggregat(behandling.getId())
            .getGjeldendeVersjon()
            .getFødselsdato().orElse(null);
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(fødselsdato)
            .medUtenMinsterett(true)
            .medKreverSammenhengendeUttak(true);
        var fpGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1)));
        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandling), stp.build(), null, fpGrunnlag);
        when(uttakInputTjeneste.lagInput(behandling.getId())).thenReturn(uttakInput);
        return uttakInput;
    }

    private UttakInput lagUttakInputUtenMinsterett(Behandling behandling) {
        var fødselsdato = repositoryProvider.getFamilieHendelseRepository()
            .hentAggregat(behandling.getId())
            .getGjeldendeVersjon()
            .getFødselsdato().orElse(null);
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(fødselsdato)
            .medUtenMinsterett(true);
        var fpGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(FamilieHendelse.forFødsel(null, fødselsdato, List.of(new Barn()), 1)));
        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandling), stp.build(), null, fpGrunnlag);
        when(uttakInputTjeneste.lagInput(behandling.getId())).thenReturn(uttakInput);
        return uttakInput;
    }

    @Test
    void behandling_med_endret_stønadskonto_skal_opprette_berørt() {
        var basedato = LocalDate.now();

        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultOppgittDekningsgrad()
            .medFødselAdopsjonsdato(basedato.minusWeeks(7))
            .lagre(repositoryProvider);
        var behandlingAnnenpart = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medDefaultOppgittDekningsgrad()
            .medFødselAdopsjonsdato(basedato.minusWeeks(7))
            .lagre(repositoryProvider);

        var morsMK = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(basedato.minusWeeks(7), basedato.minusWeeks(1).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var morsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(basedato, basedato.plusWeeks(20).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FELLESPERIODE)))
            .build();
        var morsBeregning = Map.of(StønadskontoType.MØDREKVOTE, 75, StønadskontoType.FEDREKVOTE, 75, StønadskontoType.FELLESPERIODE, 80);
        var morUttak = new ForeldrepengerUttak(List.of(morsMK, morsPeriode), null, morsBeregning);
        lagreUttak(behandling, morUttak);

        var farsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(basedato.plusWeeks(20), basedato.plusWeeks(33).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FORELDREPENGER)))
            .build();
        var farsBeregning = Map.of(StønadskontoType.FORELDREPENGER, 230);
        var farUttak = new ForeldrepengerUttak(List.of(farsPeriode), null, farsBeregning);
        lagreUttak(behandlingAnnenpart, farUttak);

        lagUttakInput(behandling);

        var br = Behandlingsresultat.builder().buildFor(behandling);

        var resultat = tjeneste.skalBerørtBehandlingOpprettes(br, behandling, behandlingAnnenpart.getId());

        assertThat(resultat).isPresent();
    }

    @Test
    void ikke_berørt_behandling_når_far_har_uttak_i_mellom_mors_uttaksperioder() {
        var startDatoMor = LocalDate.of(2020, 1, 1);
        var startDatoFar = startDatoMor.plusMonths(3);

        var farBehandling = opprettBehandlingFar(startDatoMor);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultOppgittDekningsgrad().lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoFar.minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var farsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoFar, startDatoFar.plusWeeks(10))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var morsAndrePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(farsPeriode.getTom().plusDays(1), farsPeriode.getTom().plusWeeks(5))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsFørstePeriode, morsAndrePeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsPeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isFalse();
    }

    @Test
    void ikke_berørt_behandling_når_far_har_overlapp_med_oppholdsperiode_i_mors_uttak() {
        var startDatoMor = LocalDate.of(2020, 1, 1);
        var startDatoFar = startDatoMor.plusMonths(3);

        var farBehandling = opprettBehandlingFar(startDatoMor);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultOppgittDekningsgrad().lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoFar.minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var morsOppholdsperiode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(morsFørstePeriode.getTom().plusDays(1), startDatoFar)
            .medOppholdÅrsak(OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .build();
        var farsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(morsOppholdsperiode.getFom(), morsOppholdsperiode.getTom())
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var morsAndrePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(farsPeriode.getTom().plusDays(1), farsPeriode.getTom().plusWeeks(5))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsFørstePeriode, morsOppholdsperiode, morsAndrePeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsPeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isFalse();
    }

    @Test
    void ikke_berørt_behandling_når_far_har_oppholdsperiode() {
        var startDatoMor = LocalDate.of(2020, 1, 1);
        var startDatoFar = startDatoMor.plusMonths(3);

        var farBehandling = opprettBehandlingFar(startDatoMor);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultOppgittDekningsgrad().lagre(repositoryProvider);

        lagUttakInputSammenhengendeUttak(farBehandling);

        var morsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoFar.minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var opphold = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoFar, startDatoFar.plusWeeks(2))
            .medOppholdÅrsak(OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .build();
        var farsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(opphold.getTom().plusDays(1), opphold.getTom().plusWeeks(10))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsPeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(opphold, farsPeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isFalse();
    }

    @Test
    void ikke_berørt_behandling_når_eneste_overlapp_er_oppholdsperioder() {
        var startDatoMor = LocalDate.of(2020, 1, 1);

        var mødrekvote1 = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoMor.plusWeeks(10))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var oppholdMor1 = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(mødrekvote1.getTom().plusDays(1), mødrekvote1.getTom().plusWeeks(1))
            .medOppholdÅrsak(OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .build();
        var mødrekvote2 = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(oppholdMor1.getTom().plusDays(1), oppholdMor1.getTom().plusWeeks(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var oppholdMor2 = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(mødrekvote2.getTom().plusDays(1), mødrekvote2.getTom().plusWeeks(1))
            .medOppholdÅrsak(OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .build();
        var mødrekvote3 = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(oppholdMor2.getTom().plusDays(1), oppholdMor2.getTom().plusWeeks(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();

        var fedrekvote1 = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(oppholdMor1.getFom(), oppholdMor1.getTom())
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var oppholdFar1 = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(mødrekvote2.getFom(), mødrekvote2.getTom())
            .medOppholdÅrsak(OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .build();
        //Overlappende oppholdsperioder på far og mor
        var oppholdFar2 = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(oppholdMor2.getFom(), oppholdMor2.getTom())
            .medOppholdÅrsak(OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .build();

        var farBehandling = opprettBehandlingFar(startDatoMor);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultOppgittDekningsgrad().lagre(repositoryProvider);

        lagUttakInputSammenhengendeUttak(farBehandling);

        var morUttak = new ForeldrepengerUttak(List.of(mødrekvote1, oppholdMor1, mødrekvote2, oppholdMor2, mødrekvote3));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(fedrekvote1, oppholdFar1, oppholdFar2));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isFalse();
    }

    @Test
    void ikke_berørt_når_det_ikke_er_hull_første_6_ukene() {
        var startDatoMor = LocalDate.of(2021, 9, 27);
        var startDatoFar = LocalDate.of(2022, 1, 10);

        var fødselsdato = LocalDate.of(2021, 9, 26);
        var farBehandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medDefaultOppgittDekningsgrad()
            .medFødselAdopsjonsdato(fødselsdato)
            .medAvklarteUttakDatoer(avklarteDatoer(startDatoFar))
            .lagre(repositoryProvider);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultOppgittDekningsgrad()
            .lagre(repositoryProvider);

        lagUttakInputSammenhengendeUttak(farBehandling);

        var morUtsettelse = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, LocalDate.of(2021, 10, 15))
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medUtsettelseType(UttakUtsettelseType.BARN_INNLAGT)
            .medResultatÅrsak(PeriodeResultatÅrsak.UTSETTELSE_GYLDIG_PGA_BARN_INNLAGT)
            .medAktiviteter(List.of(aktivitetUtenTrekkdagerOgUtbetaling(UttakPeriodeType.UDEFINERT)))
            .build();
        var morPleiepengerUtenInnleggelse = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(LocalDate.of(2021, 10, 18), LocalDate.of(2021, 10, 22))
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.BARNETS_INNLEGGELSE_IKKE_OPPFYLT)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var morMødrekvote = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(LocalDate.of(2021, 10, 23), LocalDate.of(2021, 11, 6))
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var morMødrekvote2 = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(LocalDate.of(2021, 11, 7), LocalDate.of(2022, 1, 7))
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var farsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(morMødrekvote.getTom().plusDays(1), morMødrekvote.getTom().plusWeeks(10))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morUtsettelse, morPleiepengerUtenInnleggelse, morMødrekvote, morMødrekvote2));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsPeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isFalse();
    }

    @Test
    void berørt_behandling_ved_overlapp_mellom_partene() {
        var startDatoMor = LocalDate.of(2020, 1, 1);

        var farBehandling = opprettBehandlingFar(startDatoMor);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultOppgittDekningsgrad().lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoMor.plusWeeks(15))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var farsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(morsPeriode.getFom().plusWeeks(1), morsPeriode.getTom().minusWeeks(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsPeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsPeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isTrue();
    }

    @Test
    void berørt_behandling_ved_overlapp_mellom_partene_der_annenpart_har_innvilget_utsettelse() {
        var startDatoMor = LocalDate.of(2020, 1, 1);

        var farBehandling = opprettBehandlingFar(startDatoMor);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultOppgittDekningsgrad().lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoMor.plusWeeks(15))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medUtsettelseType(UttakUtsettelseType.FERIE)
            .medAktiviteter(List.of(new ForeldrepengerUttakPeriodeAktivitet.Builder()
                .medArbeidsprosent(BigDecimal.ZERO)
                .medTrekkdager(Trekkdager.ZERO)
                .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
                .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS))
                .build()))
            .build();
        var farsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(morsPeriode.getFom().plusWeeks(1), morsPeriode.getTom().minusWeeks(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsPeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsPeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isTrue();
    }

    @Test
    void ikke_berørt_behandling_ved_overlapp_mellom_partene_før_endringsdato() {
        var startDatoMor = LocalDate.of(2020, 1, 1);
        var endringsdato = startDatoMor.plusMonths(3);

        var farBehandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medDefaultOppgittDekningsgrad()
            .medAvklarteUttakDatoer(avklarteDatoer(endringsdato))
            .medFødselAdopsjonsdato(startDatoMor)
            .lagre(repositoryProvider);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultOppgittDekningsgrad()
            .medFødselAdopsjonsdato(startDatoMor).lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, endringsdato.minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var farsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(morsFørstePeriode.getFom(), morsFørstePeriode.getTom())
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
            .build();
        //Bare fars periode etter endringsdato
        var farsAndrePerioder = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(endringsdato, endringsdato.plusWeeks(2))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsFørstePeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsFørstePeriode, farsAndrePerioder));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isFalse();
    }

    @Test
    void ikke_berørt_behandling_dersom_overlapp_kun_i_forbindelse_med_fødsel() {
        var startDatoMor = VirkedagUtil.fomVirkedag(LocalDate.now());

        var farBehandling = opprettBehandlingFar(startDatoMor);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultOppgittDekningsgrad()
            .medFødselAdopsjonsdato(startDatoMor)
            .lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoMor.plusWeeks(15).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var farsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoMor.plusWeeks(1).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
            .medSamtidigUttak(true)
            .build();
        var farsAndrePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor.plusWeeks(15), startDatoMor.plusWeeks(17).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsPeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsFørstePeriode, farsAndrePeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isFalse();
    }

    @Test
    void berørt_behandling_dersom_overlapp_kun_i_forbindelse_med_fødsel_men_ikke_samtidig_uttak() {
        var startDatoMor = VirkedagUtil.fomVirkedag(LocalDate.now());

        var farBehandling = opprettBehandlingFar(startDatoMor);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultOppgittDekningsgrad()
            .medFødselAdopsjonsdato(startDatoMor)
            .lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoMor.plusWeeks(15).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var farsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoMor.plusWeeks(1).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var farsAndrePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor.plusWeeks(15), startDatoMor.plusWeeks(17).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsPeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsFørstePeriode, farsAndrePeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isTrue();
    }

    @Test
    void berørt_behandling_dersom_overlapp_kun_i_forbindelse_med_fødsel_men_ikke_FAB() {
        var startDatoMor = VirkedagUtil.fomVirkedag(LocalDate.of(2021, 11, 1));

        var farBehandling = opprettBehandlingFar(startDatoMor);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultOppgittDekningsgrad().medFødselAdopsjonsdato(startDatoMor).lagre(repositoryProvider);

        lagUttakInputUtenMinsterett(farBehandling);

        var morsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoMor.plusWeeks(15).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var farsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoMor.plusWeeks(1).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
            .medSamtidigUttak(true)
            .build();
        var farsAndrePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor.plusWeeks(15), startDatoMor.plusWeeks(17).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsPeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsFørstePeriode, farsAndrePeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isTrue();
    }

    @Test
    void berørt_behandling_dersom_overlapp_i_forbindelse_med_fødsel_og_senere() {
        var startDatoMor = VirkedagUtil.fomVirkedag(LocalDate.now());

        var farBehandling = opprettBehandlingFar(startDatoMor);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultOppgittDekningsgrad().medFødselAdopsjonsdato(startDatoMor).lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoMor.plusWeeks(15).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var farsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoMor.plusWeeks(1).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
            .medSamtidigUttak(true)
            .build();
        var farsAndrePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor.plusWeeks(14), startDatoMor.plusWeeks(17).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsPeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsFørstePeriode, farsAndrePeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isTrue();
    }

    @Test
    void berørt_behandling_hvis_et_avslag_fører_til_hull_felles_plan() {
        var startDatoMor = LocalDate.of(2020, 1, 1);

        var farBehandling = opprettBehandlingFar(startDatoMor);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultOppgittDekningsgrad().lagre(repositoryProvider);


        lagUttakInputSammenhengendeUttak(farBehandling);

        var morsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoMor.plusWeeks(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        //Denne perioden er avslått og skaper derfor hull som må fylles av mor
        var farsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(morsFørstePeriode.getTom().plusDays(1), morsFørstePeriode.getTom().plusWeeks(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.IKKE_STØNADSDAGER_IGJEN)
            .medAktiviteter(List.of(aktivitetUtenTrekkdagerOgUtbetaling(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var morsAndrePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(farsFørstePeriode.getTom().plusDays(1), farsFørstePeriode.getTom().plusWeeks(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsFørstePeriode, morsAndrePeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsFørstePeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isTrue();
    }

    @Test
    void sjekk_at_ingen_berørt_behandling_fri_utsettelse() {
        var fødselsdato = LocalDate.of(2022, 6, 6);
        var friPeriodeStart = fødselsdato.plusWeeks(7);
        var farStart = fødselsdato.plusWeeks(10);
        var morPeriode2 = fødselsdato.plusWeeks(15);

        var farBehandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medAvklarteUttakDatoer(avklarteDatoer(farStart))
            .medFødselAdopsjonsdato(fødselsdato)
            .medDefaultOppgittDekningsgrad()
            .lagre(repositoryProvider);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(fødselsdato)
            .medDefaultOppgittDekningsgrad()
            .lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(fødselsdato, friPeriodeStart.minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var morsFørstePeriodeDel2 = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(friPeriodeStart, friPeriodeStart.plusWeeks(2).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        // Manglende periode og hull som ikke trengs fylles
        var farsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(farStart, farStart.plusWeeks(4).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(aktivitetUtenTrekkdagerOgUtbetaling(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var morsAndrePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(morPeriode2, morPeriode2.plusWeeks(7).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsFørstePeriode, morsFørstePeriodeDel2, morsAndrePeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsFørstePeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isFalse();
    }

    @Test
    void sjekk_at_berørt_behandling_ved_hull_første_seks_uker() {
        var fødselsdato = LocalDate.of(2022, 6, 6);
        var farStart = fødselsdato.plusWeeks(10);
        var morPeriode2 = fødselsdato.plusWeeks(15);

        var farBehandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medAvklarteUttakDatoer(avklarteDatoer(farStart))
            .medFødselAdopsjonsdato(fødselsdato)
            .medDefaultOppgittDekningsgrad()
            .lagre(repositoryProvider);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(fødselsdato)
            .medDefaultOppgittDekningsgrad()
            .lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(fødselsdato, fødselsdato.plusWeeks(1).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        // Manglende periode og hull som må fylles
        var morsFørstePeriodeDel2 = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(fødselsdato.plusWeeks(2), farStart.minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        // Manglende periode og hull som ikke trengs fylles
        var farsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(farStart, farStart.plusWeeks(4).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(aktivitetUtenTrekkdagerOgUtbetaling(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var morsAndrePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(morPeriode2, morPeriode2.plusWeeks(7).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsFørstePeriode, morsFørstePeriodeDel2, morsAndrePeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsFørstePeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isTrue();
    }

    @Test
    void sjekk_at_berørt_behandling_sammenhengende_uttak() {
        var fødselsdato = LocalDate.of(2020, 1, 1);
        var friPeriodeStart = fødselsdato.plusWeeks(7);
        var farStart = fødselsdato.plusWeeks(10);
        var morPeriode2 = fødselsdato.plusWeeks(15);

        var farBehandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medAvklarteUttakDatoer(avklarteDatoer(farStart))
            .medFødselAdopsjonsdato(fødselsdato)
            .medDefaultOppgittDekningsgrad()
            .lagre(repositoryProvider);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(fødselsdato)
            .medDefaultOppgittDekningsgrad()
            .lagre(repositoryProvider);

        lagUttakInputSammenhengendeUttak(farBehandling);

        var morsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(fødselsdato, friPeriodeStart.minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var morsFørstePeriodeDel2 = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(friPeriodeStart, friPeriodeStart.plusWeeks(2).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        //Denne perioden er avslått og skaper derfor hull som må fylles av mor
        var farsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(farStart, farStart.plusWeeks(4).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(aktivitetUtenTrekkdagerOgUtbetaling(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var morsAndrePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(morPeriode2, morPeriode2.plusWeeks(7).minusDays(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsFørstePeriode, morsFørstePeriodeDel2, morsAndrePeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsFørstePeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isTrue();
    }

    @Test
    void berørt_behandling_hvis_tomt_uttak_fører_til_hull_i_tidsperiode_forbeholdt_mor() {
        var startDatoMor = LocalDate.of(2021, 12, 13);

        var scenarioAnnenpart = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultOppgittDekningsgrad();
        //Annulleringsbehandling for far
        var farBehandling = farAnnulleringsbehandling(startDatoMor);
        var morBehandling = scenarioAnnenpart.lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoMor.plusWeeks(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var morsAndrePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(morsFørstePeriode.getTom().plusWeeks(2), morsFørstePeriode.getTom().plusWeeks(10))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsFørstePeriode, morsAndrePeriode));
        lagreUttak(morBehandling, morUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isTrue();
    }

    private Behandling farAnnulleringsbehandling(LocalDate startDatoMor) {
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medOpprinneligEndringsdato(null)
            .build();
        return opprettBehandlingFar(startDatoMor, avklarteUttakDatoer);
    }

    @Test
    void ikke_berørt_behandling_hvis_tomt_uttak_fører_til_hull_etter_tidsperiode_forbeholdt_mor() {
        var startDatoMor = LocalDate.of(2021, 12, 13);

        //Annulleringsbehandling for far
        var farBehandling = farAnnulleringsbehandling(startDatoMor);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultOppgittDekningsgrad()
            .lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoMor.plusWeeks(6))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var morsAndrePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(morsFørstePeriode.getTom().plusWeeks(10), morsFørstePeriode.getTom().plusWeeks(20))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsFørstePeriode, morsAndrePeriode));
        lagreUttak(morBehandling, morUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isFalse();
    }

    private Behandling opprettBehandlingFar(LocalDate startDatoMor) {
        return opprettBehandlingFar(startDatoMor, avklarteDatoer(startDatoMor));
    }

    private Behandling opprettBehandlingFar(LocalDate startDatoMor, AvklarteUttakDatoerEntitet avklarteUttakDatoer) {
        return ScenarioFarSøkerForeldrepenger.forFødsel()
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medFødselAdopsjonsdato(startDatoMor)
            .medDefaultOppgittDekningsgrad()
            .lagre(repositoryProvider);
    }

    @Test
    void ikke_berørt_behandling_hvis_bruker_går_tom_for_dager_på_slutten_av_felles_uttaksplan() {
        var startDatoMor = LocalDate.of(2020, 1, 1);

        var farBehandling = opprettBehandlingFar(startDatoMor);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultOppgittDekningsgrad().lagre(repositoryProvider);

        lagUttakInputSammenhengendeUttak(farBehandling);

        var morsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoMor.plusWeeks(7))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var farsFørstePeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(morsFørstePeriode.getTom().plusDays(1), morsFørstePeriode.getTom().plusWeeks(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.IKKE_STØNADSDAGER_IGJEN)
            .medAktiviteter(List.of(aktivitetUtenTrekkdagerOgUtbetaling(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsFørstePeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsFørstePeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isFalse();
    }

    @Test
    void ikke_berørt_behandling_ved_overlapp_mellom_partene_når_br_konsekvens_er_ingen_endring() {
        var startDatoMor = LocalDate.of(2020, 1, 1);

        var farBehandling = opprettBehandlingFar(startDatoMor);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultOppgittDekningsgrad().lagre(repositoryProvider);

        var brMedIngenEndring = Behandlingsresultat.builderEndreEksisterende(getBehandlingsresultat(farBehandling.getId()))
            .leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.INGEN_ENDRING)
            .build();
        getBehandlingsresultatRepository().lagre(farBehandling.getId(), brMedIngenEndring);

        lagUttakInput(farBehandling);

        var morsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoMor.plusWeeks(15))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(new SamtidigUttaksprosent(50))
            .build();
        var farsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(morsPeriode.getFom().plusWeeks(1), morsPeriode.getTom().minusWeeks(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
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

    @Test
    void ikke_berørt_behandling_ved_overlapp_mellom_partene_men_behandlingen_er_henlagt() {
        var startDatoMor = LocalDate.of(2020, 1, 1);

        var farBehandling = opprettBehandlingFar(startDatoMor);
        var morBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoMor.plusWeeks(15))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var farsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(morsPeriode.getFom().plusWeeks(1), morsPeriode.getTom().minusWeeks(1))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.FEDREKVOTE)))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsPeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsPeriode));
        lagreUttak(farBehandling, farUttak);
        var behandlingsresultat = getBehandlingsresultat(farBehandling.getId());
        var henlagtBehandlingsresultat = Behandlingsresultat.builderEndreEksisterende(behandlingsresultat)
            .medBehandlingResultatType(BehandlingResultatType.HENLAGT_FEILOPPRETTET)
            .build();
        getBehandlingsresultatRepository().lagre(henlagtBehandlingsresultat.getBehandlingId(), henlagtBehandlingsresultat);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isFalse();
    }

    @Test
    void ikke_berørt_behandling_ved_overlapp_mellom_partene_der_bruker_har_avslått_utsettelse() {
        var startDatoMor = LocalDate.of(2020, 1, 1);

        var scenarioAnnenpart = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultOppgittDekningsgrad();
        var farBehandling = opprettBehandlingFar(startDatoMor);
        var morBehandling = scenarioAnnenpart.lagre(repositoryProvider);

        lagUttakInput(farBehandling);

        var morsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(startDatoMor, startDatoMor.plusWeeks(15))
            .medResultatÅrsak(PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(uttakPeriodeAktivitet(UttakPeriodeType.MØDREKVOTE)))
            .build();
        var farsPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(morsPeriode.getFom(), morsPeriode.getTom())
            .medResultatÅrsak(PeriodeResultatÅrsak.IKKE_STØNADSDAGER_IGJEN)
            .medResultatType(PeriodeResultatType.AVSLÅTT)
            .medUtsettelseType(UttakUtsettelseType.ARBEID)
            .medAktiviteter(List.of(new ForeldrepengerUttakPeriodeAktivitet.Builder()
                .medArbeidsprosent(BigDecimal.ZERO)
                .medTrekkdager(Trekkdager.ZERO)
                .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
                .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS))
                .build()))
            .build();
        var morUttak = new ForeldrepengerUttak(List.of(morsPeriode));
        lagreUttak(morBehandling, morUttak);

        var farUttak = new ForeldrepengerUttak(List.of(farsPeriode));
        lagreUttak(farBehandling, farUttak);

        var resultat = skalBerørtOpprettes(farBehandling, morBehandling);
        assertThat(resultat).isFalse();
    }

    private boolean skalBerørtOpprettes(Behandling behandling, Behandling annenpartsBehandling) {
        return tjeneste.skalBerørtBehandlingOpprettes(getBehandlingsresultat(behandling.getId()), behandling,
            annenpartsBehandling.getId()).isPresent();
    }

    private BehandlingsresultatRepository getBehandlingsresultatRepository() {
        return repositoryProvider.getBehandlingsresultatRepository();
    }

    private void lagreUttak(Behandling behandling, ForeldrepengerUttak uttak) {
        when(foreldrepengerUttakTjeneste.hentHvisEksisterer(behandling.getId())).thenReturn(
            Optional.ofNullable(uttak));
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return getBehandlingsresultatRepository().hent(behandlingId);
    }

    private AvklarteUttakDatoerEntitet avklarteDatoer(LocalDate endringsdato) {
        return new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(endringsdato).build();
    }

    private ForeldrepengerUttakPeriodeAktivitet uttakPeriodeAktivitet(UttakPeriodeType stønadskontoType) {
        return new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medArbeidsprosent(BigDecimal.ZERO)
            .medTrekkdager(new Trekkdager(10))
            .medTrekkonto(stønadskontoType)
            .medUtbetalingsgrad(new Utbetalingsgrad(100))
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS))
            .build();
    }

    private ForeldrepengerUttakPeriodeAktivitet aktivitetUtenTrekkdagerOgUtbetaling(UttakPeriodeType stønadskontoType) {
        return new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medArbeidsprosent(BigDecimal.ZERO)
            .medTrekkdager(Trekkdager.ZERO)
            .medTrekkonto(stønadskontoType)
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS))
            .build();
    }
}
