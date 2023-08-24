package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.steg.uttak.fp.UttakStegBeregnStønadskontoTjeneste.BeregningingAvStønadskontoResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.*;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.BeregnStønadskontoerTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static no.nav.foreldrepenger.behandling.steg.uttak.fp.UttakStegImplTest.avsluttMedVedtak;
import static org.assertj.core.api.Assertions.assertThat;

class UttakStegBeregnStønadskontoTjenesteTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private UttakStegBeregnStønadskontoTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        var uttakRepositoryProvider = new UttakRepositoryProvider(getEntityManager());
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(uttakRepositoryProvider.getFagsakRelasjonRepository(),
                null, uttakRepositoryProvider.getFagsakRepository());
        var uttakTjeneste = new ForeldrepengerUttakTjeneste(uttakRepositoryProvider.getFpUttakRepository());
        var beregnStønadskontoerTjeneste = new BeregnStønadskontoerTjeneste(uttakRepositoryProvider, fagsakRelasjonTjeneste, uttakTjeneste);
        var dekningsgradTjeneste = new DekningsgradTjeneste(fagsakRelasjonTjeneste, uttakRepositoryProvider.getBehandlingsresultatRepository());
        tjeneste = new UttakStegBeregnStønadskontoTjeneste(uttakRepositoryProvider, beregnStønadskontoerTjeneste, dekningsgradTjeneste,
                uttakTjeneste);
    }

    @Test
    void skal_beregne_hvis_vedtak_uten_uttak() {
        var førsteScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var førsteBehandling = førsteScenario.lagre(repositoryProvider);
        opprettStønadskontoer(førsteBehandling);
        avsluttMedVedtak(førsteBehandling, repositoryProvider);

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOriginalBehandling(førsteBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                .medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        revurderingScenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var revurdering = revurderingScenario.lagre(repositoryProvider);

        var ytelsespesifiktGrunnlag = familieHendelser(FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(), 1));
        var input = new UttakInput(BehandlingReferanse.fra(revurdering), null, ytelsespesifiktGrunnlag);
        var resultat = tjeneste.beregnStønadskontoer(input);

        assertThat(resultat).isEqualTo(BeregningingAvStønadskontoResultat.BEREGNET);
    }

    @Test
    void skal_beregne_hvis_vedtak_har_uttak_der_alle_periodene_er_avslått() {
        var førsteScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var førsteBehandling = førsteScenario.lagre(repositoryProvider);
        opprettStønadskontoer(førsteBehandling);
        lagreUttak(førsteBehandling, avslåttUttak());
        avsluttMedVedtak(førsteBehandling, repositoryProvider);

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOriginalBehandling(førsteBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                .medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        revurderingScenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var revurdering = revurderingScenario.lagre(repositoryProvider);

        var ytelsespesifiktGrunnlag = familieHendelser(FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(), 1));
        var input = new UttakInput(BehandlingReferanse.fra(revurdering), null, ytelsespesifiktGrunnlag);
        var resultat = tjeneste.beregnStønadskontoer(input);

        assertThat(resultat).isEqualTo(BeregningingAvStønadskontoResultat.BEREGNET);
    }

    @Test
    void skal_ikke_beregne_hvis_vedtak_har_uttak_der_en_periode_er_innvilget_og_en_avslått() {
        var førsteScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var førsteBehandling = førsteScenario.lagre(repositoryProvider);
        opprettStønadskontoer(førsteBehandling);
        var uttak = avslåttUttak();
        var periode = new UttakResultatPeriodeEntitet.Builder(uttak.getPerioder().get(0).getFom().minusWeeks(1),
                uttak.getPerioder().get(0).getFom().minusDays(1))
                        .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
                        .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(periode,
                new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build())
                        .medTrekkonto(StønadskontoType.FELLESPERIODE)
                        .medTrekkdager(new Trekkdager(5))
                        .medUtbetalingsgrad(Utbetalingsgrad.TEN)
                        .medArbeidsprosent(BigDecimal.ZERO)
                        .build();
        uttak.leggTilPeriode(periode);
        lagreUttak(førsteBehandling, uttak);
        avsluttMedVedtak(førsteBehandling, repositoryProvider);

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOriginalBehandling(førsteBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                .medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        revurderingScenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var revurdering = revurderingScenario.lagre(repositoryProvider);

        var ytelsespesifiktGrunnlag = familieHendelser(FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(), 1))
                .medAnnenpart(new Annenpart(førsteBehandling.getId(), LocalDateTime.now()));
        var input = new UttakInput(BehandlingReferanse.fra(revurdering), null, ytelsespesifiktGrunnlag);
        var resultat = tjeneste.beregnStønadskontoer(input);

        assertThat(resultat).isEqualTo(BeregningingAvStønadskontoResultat.INGEN_BEREGNING);
    }

    @Test
    void skal_ikke_beregne_hvis_annenpart_vedtak_har_uttak_innvilget() {
        var morScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var morBehandling = morScenario.lagre(repositoryProvider);
        opprettStønadskontoer(morBehandling);
        var uttak = innvilgetUttak();
        lagreUttak(morBehandling, uttak);
        avsluttMedVedtak(morBehandling, repositoryProvider);

        var farScenario = ScenarioFarSøkerForeldrepenger.forFødsel()
                .medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        farScenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var farBehandling = farScenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morBehandling.getFagsak(), farBehandling.getFagsak(), morBehandling);

        var ytelsespesifiktGrunnlag = familieHendelser(FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(), 1))
                .medAnnenpart(new Annenpart(morBehandling.getId(), LocalDateTime.now()));
        var input = new UttakInput(BehandlingReferanse.fra(farBehandling), null, ytelsespesifiktGrunnlag);
        var resultat = tjeneste.beregnStønadskontoer(input);

        assertThat(resultat).isEqualTo(BeregningingAvStønadskontoResultat.INGEN_BEREGNING);
    }

    private ForeldrepengerGrunnlag familieHendelser(FamilieHendelse søknadFamilieHendelse) {
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(søknadFamilieHendelse);
        return new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser);
    }

    @Test
    void skal_beregne_hvis_annenpart_vedtak_har_uten_uttak() {
        var morScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var morBehandling = morScenario.lagre(repositoryProvider);
        opprettStønadskontoer(morBehandling);
        avsluttMedVedtak(morBehandling, repositoryProvider);

        var farScenario = ScenarioFarSøkerForeldrepenger.forFødsel()
                .medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        farScenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var farBehandling = farScenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morBehandling.getFagsak(), farBehandling.getFagsak(), morBehandling);

        var ytelsespesifiktGrunnlag = familieHendelser(FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(), 1));
        var input = new UttakInput(BehandlingReferanse.fra(farBehandling), null, ytelsespesifiktGrunnlag);
        var resultat = tjeneste.beregnStønadskontoer(input);

        assertThat(resultat).isEqualTo(BeregningingAvStønadskontoResultat.BEREGNET);
    }

    @Test
    void skal_beregne_hvis_annenpart_vedtak_har_uttak_avslått() {
        var morScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var morBehandling = morScenario.lagre(repositoryProvider);
        opprettStønadskontoer(morBehandling);
        lagreUttak(morBehandling, avslåttUttak());
        avsluttMedVedtak(morBehandling, repositoryProvider);

        var farScenario = ScenarioFarSøkerForeldrepenger.forFødsel()
                .medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        farScenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var farBehandling = farScenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morBehandling.getFagsak(), farBehandling.getFagsak(), morBehandling);

        var ytelsespesifiktGrunnlag = familieHendelser(FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(), 1));
        var input = new UttakInput(BehandlingReferanse.fra(farBehandling), null, ytelsespesifiktGrunnlag);
        var resultat = tjeneste.beregnStønadskontoer(input);

        assertThat(resultat).isEqualTo(BeregningingAvStønadskontoResultat.BEREGNET);
    }

    private void lagreUttak(Behandling førsteBehandling, UttakResultatPerioderEntitet opprinneligPerioder) {
        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(førsteBehandling.getId(), opprinneligPerioder);
    }

    private UttakResultatPerioderEntitet innvilgetUttak() {
        var uttak = new UttakResultatPerioderEntitet();
        var periode = new UttakResultatPeriodeEntitet.Builder(LocalDate.now(),
                LocalDate.now().plusWeeks(1))
                        .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
                        .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(periode,
                new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build())
                        .medTrekkonto(StønadskontoType.FELLESPERIODE)
                        .medTrekkdager(new Trekkdager(5))
                        .medUtbetalingsgrad(Utbetalingsgrad.TEN)
                        .medArbeidsprosent(BigDecimal.ZERO)
                        .build();
        uttak.leggTilPeriode(periode);
        return uttak;
    }

    private UttakResultatPerioderEntitet avslåttUttak() {
        var uttak = new UttakResultatPerioderEntitet();
        var periode = new UttakResultatPeriodeEntitet.Builder(LocalDate.now(), LocalDate.now().plusWeeks(1))
                .medResultatType(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.BARNET_ER_DØD)
                .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(periode,
                new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build())
                        .medTrekkonto(StønadskontoType.FELLESPERIODE)
                        .medTrekkdager(Trekkdager.ZERO)
                        .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
                        .medArbeidsprosent(BigDecimal.ZERO)
                        .build();
        uttak.leggTilPeriode(periode);
        return uttak;
    }

    private void opprettStønadskontoer(Behandling førsteBehandling) {
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(førsteBehandling.getFagsak(), Dekningsgrad._100);
        repositoryProvider.getFagsakRelasjonRepository().lagre(førsteBehandling.getFagsak(), førsteBehandling.getId(), Stønadskontoberegning.builder()
                .medStønadskonto(new Stønadskonto.Builder().medMaxDager(10).medStønadskontoType(StønadskontoType.FELLESPERIODE).build())
                .medRegelEvaluering(" ")
                .medRegelInput(" ")
                .build());
    }
}
