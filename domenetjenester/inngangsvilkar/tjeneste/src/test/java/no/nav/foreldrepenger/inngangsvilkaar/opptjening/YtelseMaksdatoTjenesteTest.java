package no.nav.foreldrepenger.inngangsvilkaar.opptjening;


import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class YtelseMaksdatoTjenesteTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private YtelseMaksdatoTjeneste beregnMorsMaksdatoTjeneste;


    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        var relatertBehandlingTjeneste = new RelatertBehandlingTjeneste(repositoryProvider);
        beregnMorsMaksdatoTjeneste = new YtelseMaksdatoTjeneste(repositoryProvider, relatertBehandlingTjeneste);
        behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
    }

    @Test
    public void finnesIngenVedtattBehandlingForMorSkalReturnereOptionalEmpty() {
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        var morsMaksdato = beregnMorsMaksdatoTjeneste.beregnMorsMaksdato(behandling.getFagsak().getSaksnummer(), behandling.getRelasjonsRolleType());
        assertThat(morsMaksdato).isEmpty();
    }

    @Test
    public void beregnerMorsMaksdato() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var morsBehandling = scenario.lagre(repositoryProvider);
        var perioder = new UttakResultatPerioderEntitet();

        var start = LocalDate.of(2018, 5, 14);

        // Uttak periode 1
        var uttakMødrekvote = new UttakResultatPeriodeEntitet.Builder(start, start.plusWeeks(6).minusDays(1))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();

        var arbeidsgiver = arbeidsgiver("1111");

        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
            .build();

        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødrekvote, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(30))
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.ZERO).build();

        perioder.leggTilPeriode(uttakMødrekvote);

        // Uttak periode 2
        var uttakFellesperiode = new UttakResultatPeriodeEntitet.Builder(start.plusWeeks(6), start.plusWeeks(10).minusDays(1))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();

        UttakResultatPeriodeAktivitetEntitet.builder(uttakFellesperiode, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(20))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.ZERO).build();

        perioder.leggTilPeriode(uttakFellesperiode);

        var behandlingsresultat = getBehandlingsresultat(morsBehandling);
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandlingsresultatRepository.lagre(morsBehandling.getId(), behandlingsresultat);

        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), perioder);

        morsBehandling.avsluttBehandling();
        lagre(morsBehandling);

        final var behandlingVedtak = BehandlingVedtak.builder().medVedtakstidspunkt(LocalDateTime.now()).medBehandlingsresultat(behandlingsresultat)
            .medVedtakResultatType(VedtakResultatType.INNVILGET).medAnsvarligSaksbehandler("mor vedtak").build();
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(morsBehandling));

        var farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        opprettStønadskontoerForFarOgMor(morsBehandling);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak(), morsBehandling);

        // Act
        var morsMaksdato = beregnMorsMaksdatoTjeneste.beregnMorsMaksdato(farsBehandling.getFagsak().getSaksnummer(), farsBehandling.getRelasjonsRolleType());

        // Assert
        assertThat(morsMaksdato).isPresent();
        assertThat(morsMaksdato.get()).isEqualTo(LocalDate.of(2018, 9, 28));

    }

    private void lagre(Behandling morsBehandling) {
        behandlingRepository.lagre(morsBehandling, behandlingRepository.taSkriveLås(morsBehandling.getId()));
    }

    @Test
    public void beregnerMorsMaksdatoVedFlereArbeidsforholdOgGradering() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET);
        var uttak = new UttakResultatPerioderEntitet();

        var start = LocalDate.of(2018, 5, 14);

        // Uttak periode 1
        var uttakMødrekvote = new UttakResultatPeriodeEntitet.Builder(start, start.plusWeeks(6).minusDays(1))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();

        var arbeidsgiver1 = arbeidsgiver("1111");

        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver1, InternArbeidsforholdRef.nyRef())
            .build();

        var arbeidsgiver2 = arbeidsgiver("2222");

        var arbeidsforhold2 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver2, InternArbeidsforholdRef.nyRef())
            .build();

        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødrekvote, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(30))
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.ZERO).build();

        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødrekvote, arbeidsforhold2)
            .medTrekkdager(new Trekkdager(15))
            .medArbeidsprosent(BigDecimal.valueOf(50))
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.ZERO).build();

        uttak.leggTilPeriode(uttakMødrekvote);
        scenario.medUttak(uttak);

        var morsBehandling = scenario.lagre(repositoryProvider);
        morsBehandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsBehandling, repositoryProvider.getBehandlingLåsRepository().taLås(morsBehandling.getId()));

        var farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        opprettStønadskontoerForFarOgMor(morsBehandling);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak(), morsBehandling);

        // Act
        var morsMaksdato = beregnMorsMaksdatoTjeneste.beregnMorsMaksdato(farsBehandling.getFagsak().getSaksnummer(), farsBehandling.getRelasjonsRolleType());

        // Assert
        assertThat(morsMaksdato).isPresent();
        assertThat(morsMaksdato.get()).isEqualTo(LocalDate.of(2018, 10, 19));
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling morsBehandling) {
        return morsBehandling.getBehandlingsresultat();
    }

    @Test
    public void skalHåndtereAtAlleMorsPerioderErAvslåttMed0Trekkdager() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var morsBehandling = scenario.lagre(repositoryProvider);
        var perioder = new UttakResultatPerioderEntitet();

        var start = LocalDate.of(2018, 5, 14);

        // Uttak periode 1
        var uttakMødrekvote = new UttakResultatPeriodeEntitet.Builder(start, start.plusWeeks(6).minusDays(1))
            .medResultatType(PeriodeResultatType.AVSLÅTT, IkkeOppfyltÅrsak.BARNET_ER_DØD)
            .build();

        var arbeidsgiver = arbeidsgiver("1111");

        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
            .build();

        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødrekvote, arbeidsforhold1)
            .medTrekkdager(Trekkdager.ZERO)
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();

        perioder.leggTilPeriode(uttakMødrekvote);

        var behandlingsresultat = getBehandlingsresultat(morsBehandling);
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandlingsresultatRepository.lagre(morsBehandling.getId(), behandlingsresultat);

        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), perioder);

        morsBehandling.avsluttBehandling();
        lagre(morsBehandling);

        var farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        opprettStønadskontoerForFarOgMor(morsBehandling);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak(), morsBehandling);

        // Act
        var morsMaksdato = beregnMorsMaksdatoTjeneste.beregnMorsMaksdato(farsBehandling.getFagsak().getSaksnummer(), farsBehandling.getRelasjonsRolleType());

        // Assert
        assertThat(morsMaksdato).isEmpty();

    }

    private Arbeidsgiver arbeidsgiver(String orgnr) {
        return Arbeidsgiver.virksomhet(orgnr);
    }

    private void opprettStønadskontoerForFarOgMor(Behandling behandling) {
        var foreldrepengerFørFødsel = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL)
            .medMaxDager(15)
            .build();
        var mødrekvote = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.MØDREKVOTE)
            .medMaxDager(50)
            .build();
        var fedrekvote = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FEDREKVOTE)
            .medMaxDager(50)
            .build();
        var fellesperiode = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FELLESPERIODE)
            .medMaxDager(50)
            .build();
        var stønadskontoberegning = Stønadskontoberegning.builder()
            .medRegelEvaluering("evaluering")
            .medRegelInput("grunnlag")
            .medStønadskonto(mødrekvote).medStønadskonto(fedrekvote).medStønadskonto(fellesperiode).medStønadskonto(foreldrepengerFørFødsel).build();

        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), behandling.getId(), stønadskontoberegning);
    }


}
