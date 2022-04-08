package no.nav.foreldrepenger.domene.vedtak.intern.fp;


import static no.nav.foreldrepenger.domene.vedtak.intern.fp.FpFagsakRelasjonAvslutningsdatoOppdaterer.KLAGEFRIST_I_UKER_VED_DØD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.fp.MaksDatoUttakTjenesteImpl;
import no.nav.foreldrepenger.domene.vedtak.intern.FagsakRelasjonAvslutningsdatoOppdaterer;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregning;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Stønadskontotype;
import no.nav.foreldrepenger.regler.uttak.konfig.Parametertype;
import no.nav.foreldrepenger.regler.uttak.konfig.StandardKonfigurasjon;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FpFagsakRelasjonAvslutningsdatoOppdatererTest {

    private FagsakRelasjonAvslutningsdatoOppdaterer fagsakRelasjonAvslutningsdatoOppdaterer;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private FamilieHendelseRepository familieHendelseRepository;

    @Mock
    private FpUttakRepository fpUttakRepository;

    @Mock
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;

    @Mock
    private SaldoUtregning saldoUtregning;

    @Mock
    private UttakInputTjeneste uttakInputTjeneste;

    private Fagsak fagsak;
    private Behandling behandling;

    @BeforeEach
    public void setUp() {
        when(stønadskontoSaldoTjeneste.finnSaldoUtregning(any(UttakInput.class))).thenReturn(saldoUtregning);

        var maksDatoUttakTjeneste = new MaksDatoUttakTjenesteImpl(fpUttakRepository,
            stønadskontoSaldoTjeneste);

        repositoryProvider = mock(BehandlingRepositoryProvider.class);
        var fagsakLåsRepository = mock(FagsakLåsRepository.class);
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(repositoryProvider.getBehandlingsresultatRepository()).thenReturn(behandlingsresultatRepository);
        when(repositoryProvider.getFagsakLåsRepository()).thenReturn(fagsakLåsRepository);
        when(repositoryProvider.getFamilieHendelseRepository()).thenReturn(familieHendelseRepository);

        fagsakRelasjonAvslutningsdatoOppdaterer = new FpFagsakRelasjonAvslutningsdatoOppdaterer(repositoryProvider,
            stønadskontoSaldoTjeneste, uttakInputTjeneste, maksDatoUttakTjeneste, fagsakRelasjonTjeneste);

        behandling = lagBehandling();
        fagsak = behandling.getFagsak();
        var stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(LocalDate.now())
            .medFørsteUttaksdato(VirkedagUtil.fomVirkedag(LocalDate.now()))
            .medKreverSammenhengendeUttak(false);

        when(uttakInputTjeneste.lagInput(any(Behandling.class))).thenReturn(
            new UttakInput(BehandlingReferanse.fra(behandling, stp.build()), null, new ForeldrepengerGrunnlag()));
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
    }

    @Test
    public void testAvsluttningsdatoVedAvslag() {
        // Arrange
        var fagsakRelasjon = mock(FagsakRelasjon.class);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)).thenReturn(Optional.of(fagsakRelasjon));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(
            Optional.of(behandling));
        when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())).thenReturn(
            lagBehandlingsresultat(behandling, BehandlingResultatType.AVSLÅTT,
                KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN));
        when(fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(Optional.empty());

        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.empty());

        // Act
        fagsakRelasjonAvslutningsdatoOppdaterer.oppdaterFagsakRelasjonAvsluttningsdato(fagsakRelasjon, fagsak.getId(),
            null, Optional.empty(), Optional.empty());

        // Assert
        verify(fagsakRelasjonTjeneste).oppdaterMedAvsluttningsdato(fagsakRelasjon, LocalDate.now().plusDays(1), null,
            Optional.empty(), Optional.empty());
    }

    @Test
    public void testAvsluttningsdatoVedAvslagPgaDød() {
        // Arrange
        var fødselsdato = LocalDate.now().minusDays(5);
        var fagsakRelasjon = mock(FagsakRelasjon.class);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)).thenReturn(Optional.of(fagsakRelasjon));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(
            Optional.of(behandling));
        when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())).thenReturn(
            lagBehandlingsresultat(behandling, BehandlingResultatType.AVSLÅTT,
                KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN));
        when(fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(
            lagUttakResultat(fødselsdato.minusDays(5), fødselsdato.plusWeeks(6).minusDays(1)));

        var familieHendelseGrunnlag = mock(FamilieHendelseGrunnlagEntitet.class);
        var familieHendelse = mock(FamilieHendelseEntitet.class);
        when(familieHendelse.getFødselsdato()).thenReturn(Optional.of(fødselsdato));
        when(familieHendelseGrunnlag.getGjeldendeVersjon()).thenReturn(familieHendelse);

        List<UidentifisertBarn> barna = new ArrayList<>();
        UidentifisertBarn dødtbarn = new UidentifisertBarnEntitet(1, fødselsdato, fødselsdato);
        barna.add(dødtbarn);

        when(familieHendelse.getBarna()).thenReturn(barna);
        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(
            Optional.of(familieHendelseGrunnlag));

        //        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.empty());

        // Act
        fagsakRelasjonAvslutningsdatoOppdaterer.oppdaterFagsakRelasjonAvsluttningsdato(fagsakRelasjon, fagsak.getId(),
            null, Optional.empty(), Optional.empty());

        // Assert
        var forventetAvslutning = fødselsdato.plusWeeks(
            StandardKonfigurasjon.KONFIGURASJON.getParameter(Parametertype.UTTAK_ETTER_BARN_DØDT_UKER, LocalDate.now()))
            .plusWeeks(KLAGEFRIST_I_UKER_VED_DØD);
        verify(fagsakRelasjonTjeneste).oppdaterMedAvsluttningsdato(fagsakRelasjon, forventetAvslutning, null,
            Optional.empty(), Optional.empty());
    }

    @Test
    public void testOppbruktStønadsdager() {
        // Arrange
        var fagsakRelasjon = mock(FagsakRelasjon.class);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)).thenReturn(Optional.of(fagsakRelasjon));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(
            Optional.of(behandling));
        when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())).thenReturn(
            lagBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET, KonsekvensForYtelsen.UDEFINERT));

        var periodeStartDato = LocalDate.now().minusDays(10);
        var periodeAvsluttetDato = LocalDate.now().plusDays(10);
        when(fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(
            lagUttakResultat(periodeStartDato, periodeAvsluttetDato));

        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.empty());
        when(saldoUtregning.saldo(any(Stønadskontotype.class))).thenReturn(0);
        when(stønadskontoSaldoTjeneste.finnStønadRest(any(UttakInput.class))).thenReturn(0);

        // Act
        fagsakRelasjonAvslutningsdatoOppdaterer.oppdaterFagsakRelasjonAvsluttningsdato(fagsakRelasjon, fagsak.getId(),
            null, Optional.empty(), Optional.empty());

        // Assert
        verify(fagsakRelasjonTjeneste).oppdaterMedAvsluttningsdato(fagsakRelasjon,
            VirkedagUtil.tomVirkedag(periodeAvsluttetDato).plusDays(1), null, Optional.empty(), Optional.empty());
    }

    @Test
    public void testStønadsdagerIgjen() {
        // Arrange
        var fagsakRelasjon = mock(FagsakRelasjon.class);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)).thenReturn(Optional.of(fagsakRelasjon));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(
            Optional.of(behandling));
        when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())).thenReturn(
            lagBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET, KonsekvensForYtelsen.UDEFINERT));

        var dato = LocalDate.now();
        var fødselsDato = dato.minusDays(5);
        var periodeStartDato = dato.minusDays(10);
        var periodeAvsluttetDato = dato.plusDays(10);
        when(fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(
            lagUttakResultat(periodeStartDato, periodeAvsluttetDato));

        var familieHendelseGrunnlag = mock(FamilieHendelseGrunnlagEntitet.class);
        var familieHendelse = mock(FamilieHendelseEntitet.class);
        when(familieHendelse.getSkjæringstidspunkt()).thenReturn(fødselsDato);
        when(familieHendelseGrunnlag.getGjeldendeVersjon()).thenReturn(familieHendelse);
        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(
            Optional.of(familieHendelseGrunnlag));

        var totalRest = 3;
        when(saldoUtregning.saldo(any(Stønadskontotype.class))).thenReturn(1);
        when(stønadskontoSaldoTjeneste.finnStønadRest(any(UttakInput.class))).thenReturn(
            totalRest); //summen for de tre stønadskotoene

        // Act
        fagsakRelasjonAvslutningsdatoOppdaterer.oppdaterFagsakRelasjonAvsluttningsdato(fagsakRelasjon, fagsak.getId(),
            null, Optional.empty(), Optional.empty());

        var avslutningsdato = fødselsDato.plusYears(3);
        // Assert
        verify(fagsakRelasjonTjeneste).oppdaterMedAvsluttningsdato(fagsakRelasjon, avslutningsdato, null,
            Optional.empty(), Optional.empty());
    }

    private Behandling lagBehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        scenario.medBehandlingType(BehandlingType.REVURDERING);
        return scenario.lagMocked();
    }

    private Optional<Behandlingsresultat> lagBehandlingsresultat(Behandling behandling,
                                                                 BehandlingResultatType behandlingResultatType,
                                                                 KonsekvensForYtelsen konsekvensForYtelsen) {
        return Optional.of(Behandlingsresultat.builder()
            .medBehandlingResultatType(behandlingResultatType)
            .leggTilKonsekvensForYtelsen(konsekvensForYtelsen)
            .buildFor(behandling));
    }

    private Optional<UttakResultatEntitet> lagUttakResultat(LocalDate fom, LocalDate tom) {
        var periode = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(PeriodeResultatType.INNVILGET,
            PeriodeResultatÅrsak.UKJENT).build();
        var perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(periode);
        var behandlingsresultat = new Behandlingsresultat.Builder().build();

        return Optional.of(
            new UttakResultatEntitet.Builder(behandlingsresultat).medOpprinneligPerioder(perioder).build());
    }
}
