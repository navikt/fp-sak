package no.nav.foreldrepenger.inngangsvilkaar.opptjening;


import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.testutilities.db.Repository;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class YtelseMaksdatoTjenesteImplTest {

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private final BehandlingVedtakRepository behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();

    private final Repository repository = repoRule.getRepository();
    private final RelatertBehandlingTjeneste relatertBehandlingTjeneste = new RelatertBehandlingTjeneste(repositoryProvider);
    private final YtelseMaksdatoTjeneste beregnMorsMaksdatoTjeneste = new YtelseMaksdatoTjeneste(repositoryProvider, relatertBehandlingTjeneste);


    @Test
    public void finnesIngenVedtattBehandlingForMorSkalReturnereOptionalEmpty() {
        Behandling behandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        Optional<LocalDate> morsMaksdato = beregnMorsMaksdatoTjeneste.beregnMorsMaksdato(behandling.getFagsak().getSaksnummer(), behandling.getRelasjonsRolleType());
        assertThat(morsMaksdato).isEmpty();
    }

    @Test
    public void beregnerMorsMaksdato() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling morsBehandling = scenario.lagre(repositoryProvider);
        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();

        LocalDate start = LocalDate.of(2018, 5, 14);

        // Uttak periode 1
        UttakResultatPeriodeEntitet uttakMødrekvote = new UttakResultatPeriodeEntitet.Builder(start, start.plusWeeks(6).minusDays(1))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();

        Arbeidsgiver arbeidsgiver = arbeidsgiver("1111");

        UttakAktivitetEntitet arbeidsforhold1 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
            .build();

        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødrekvote, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(30))
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.ZERO).build();

        perioder.leggTilPeriode(uttakMødrekvote);

        // Uttak periode 2
        UttakResultatPeriodeEntitet uttakFellesperiode = new UttakResultatPeriodeEntitet.Builder(start.plusWeeks(6), start.plusWeeks(10).minusDays(1))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();

        UttakResultatPeriodeAktivitetEntitet.builder(uttakFellesperiode, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(20))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.ZERO).build();

        perioder.leggTilPeriode(uttakFellesperiode);

        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(morsBehandling);
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        repository.lagre(behandlingsresultat);

        repositoryProvider.getUttakRepository().lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), perioder);

        morsBehandling.avsluttBehandling();
        repository.lagre(morsBehandling);

        final BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder().medVedtakstidspunkt(LocalDateTime.now()).medBehandlingsresultat(behandlingsresultat)
            .medVedtakResultatType(VedtakResultatType.INNVILGET).medAnsvarligSaksbehandler("mor vedtak").build();
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(morsBehandling));

        Behandling farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        opprettStønadskontoerForFarOgMor(morsBehandling);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak(), morsBehandling);

        repository.flushAndClear();

        // Act
        Optional<LocalDate> morsMaksdato = beregnMorsMaksdatoTjeneste.beregnMorsMaksdato(farsBehandling.getFagsak().getSaksnummer(), farsBehandling.getRelasjonsRolleType());

        // Assert
        assertThat(morsMaksdato).isPresent();
        assertThat(morsMaksdato.get()).isEqualTo(LocalDate.of(2018, 9, 28));

    }

    @Test
    public void beregnerMorsMaksdatoVedFlereArbeidsforholdOgGradering() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET);
        UttakResultatPerioderEntitet uttak = new UttakResultatPerioderEntitet();

        LocalDate start = LocalDate.of(2018, 5, 14);

        // Uttak periode 1
        UttakResultatPeriodeEntitet uttakMødrekvote = new UttakResultatPeriodeEntitet.Builder(start, start.plusWeeks(6).minusDays(1))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();

        Arbeidsgiver arbeidsgiver1 = arbeidsgiver("1111");

        UttakAktivitetEntitet arbeidsforhold1 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver1, InternArbeidsforholdRef.nyRef())
            .build();

        Arbeidsgiver arbeidsgiver2 = arbeidsgiver("2222");

        UttakAktivitetEntitet arbeidsforhold2 = new UttakAktivitetEntitet.Builder()
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

        Behandling morsBehandling = scenario.lagre(repositoryProvider);
        morsBehandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsBehandling, repositoryProvider.getBehandlingLåsRepository().taLås(morsBehandling.getId()));

        Behandling farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        opprettStønadskontoerForFarOgMor(morsBehandling);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak(), morsBehandling);

        // Act
        Optional<LocalDate> morsMaksdato = beregnMorsMaksdatoTjeneste.beregnMorsMaksdato(farsBehandling.getFagsak().getSaksnummer(), farsBehandling.getRelasjonsRolleType());

        // Assert
        assertThat(morsMaksdato).isPresent();
        assertThat(morsMaksdato.get()).isEqualTo(LocalDate.of(2018, 10, 19));
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling morsBehandling) {
        return morsBehandling.getBehandlingsresultat();
    }

    @Test
    public void skalHåndtereAtAlleMorsPerioderErAvslåttMed0Trekkdager() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling morsBehandling = scenario.lagre(repositoryProvider);
        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();

        LocalDate start = LocalDate.of(2018, 5, 14);

        // Uttak periode 1
        UttakResultatPeriodeEntitet uttakMødrekvote = new UttakResultatPeriodeEntitet.Builder(start, start.plusWeeks(6).minusDays(1))
            .medPeriodeResultat(PeriodeResultatType.AVSLÅTT, IkkeOppfyltÅrsak.BARNET_ER_DØD)
            .build();

        Arbeidsgiver arbeidsgiver = arbeidsgiver("1111");

        UttakAktivitetEntitet arbeidsforhold1 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
            .build();

        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødrekvote, arbeidsforhold1)
            .medTrekkdager(Trekkdager.ZERO)
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();

        perioder.leggTilPeriode(uttakMødrekvote);

        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(morsBehandling);
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        repository.lagre(behandlingsresultat);

        repositoryProvider.getUttakRepository().lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), perioder);

        morsBehandling.avsluttBehandling();
        repository.lagre(morsBehandling);

        Behandling farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        opprettStønadskontoerForFarOgMor(morsBehandling);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak(), morsBehandling);

        // Act
        Optional<LocalDate> morsMaksdato = beregnMorsMaksdatoTjeneste.beregnMorsMaksdato(farsBehandling.getFagsak().getSaksnummer(), farsBehandling.getRelasjonsRolleType());

        // Assert
        assertThat(morsMaksdato).isEmpty();

    }

    private Arbeidsgiver arbeidsgiver(String orgnr) {
        return Arbeidsgiver.virksomhet(orgnr);
    }

    private void opprettStønadskontoerForFarOgMor(Behandling behandling) {
        Stønadskonto foreldrepengerFørFødsel = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL)
            .medMaxDager(15)
            .build();
        Stønadskonto mødrekvote = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.MØDREKVOTE)
            .medMaxDager(50)
            .build();
        Stønadskonto fedrekvote = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FEDREKVOTE)
            .medMaxDager(50)
            .build();
        Stønadskonto fellesperiode = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FELLESPERIODE)
            .medMaxDager(50)
            .build();
        Stønadskontoberegning stønadskontoberegning = Stønadskontoberegning.builder()
            .medRegelEvaluering("evaluering")
            .medRegelInput("grunnlag")
            .medStønadskonto(mødrekvote).medStønadskonto(fedrekvote).medStønadskonto(fellesperiode).medStønadskonto(foreldrepengerFørFødsel).build();

        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), behandling.getId(), stønadskontoberegning);
    }


}
