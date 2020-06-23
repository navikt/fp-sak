package no.nav.foreldrepenger.mottak.hendelser.impl.saksvelger;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.Endringstype;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.fødsel.FødselForretningshendelse;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.hendelser.saksvelger.FødselForretningshendelseSaksvelger;

public class FødselForretningshendelseSaksvelgerTest {

    public static final LocalDate FØDSELSDATO = LocalDate.now();
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private FagsakRepository fagsakRepository;

    @Mock
    private FamilieHendelseRepository familieHendelseRepository;

    @Mock
    private BehandlingRepository behandlingRepository;

    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Mock
    private BeregningsresultatRepository beregningsresultatRepository;

    @Mock
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    private FødselForretningshendelseSaksvelger saksvelger;

    @Before
    public void before() {
        when(repositoryProvider.getFagsakRepository()).thenReturn(fagsakRepository);
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(repositoryProvider.getBehandlingsresultatRepository()).thenReturn(behandlingsresultatRepository);
        when(repositoryProvider.getBeregningsresultatRepository()).thenReturn(beregningsresultatRepository);
        when(repositoryProvider.getFamilieHendelseRepository()).thenReturn(familieHendelseRepository);
        saksvelger = new FødselForretningshendelseSaksvelger(repositoryProvider, historikkinnslagTjeneste);
    }

    @Test
    public void skal_velge_sak_som_er_åpen_foreldrepengesak() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.of(behandling));
        FødselForretningshendelse hendelse = new FødselForretningshendelse(singletonList(aktørId), FØDSELSDATO, Endringstype.OPPRETTET);
        FamilieHendelseGrunnlagEntitet fh = lagFamilieHendelseGrunnlag(FØDSELSDATO, Optional.empty());
        when(familieHendelseRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(fh));

        // Act
        Map<BehandlingÅrsakType, List<Fagsak>> behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.keySet()).contains(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_FØDSEL).get(0)).isEqualTo(fagsak);
    }

    @Test
    public void skal_ikke_velge_sak_som_er_avsluttet_foreldrepengesak() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        fagsak.setAvsluttet();
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        FødselForretningshendelse hendelse = new FødselForretningshendelse(singletonList(aktørId), FØDSELSDATO, Endringstype.OPPRETTET);

        // Act
        Map<BehandlingÅrsakType, List<Fagsak>> behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.keySet()).contains(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)).isEmpty();
    }

    @Test
    public void skal_velge_sak_som_er_åpen_engangsstønadsak() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, null);
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.of(behandling));
        FamilieHendelseGrunnlagEntitet fh = lagFamilieHendelseGrunnlag(FØDSELSDATO, Optional.empty());
        when(familieHendelseRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(fh));
        FødselForretningshendelse hendelse = new FødselForretningshendelse(singletonList(aktørId), FØDSELSDATO, Endringstype.OPPRETTET);

        // Act
        Map<BehandlingÅrsakType, List<Fagsak>> behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.keySet()).contains(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_FØDSEL).get(0)).isEqualTo(fagsak);
    }

    @Test
    public void skal_velge_sak_som_er_avsluttet_engangsstønadsak_med_innvilget_vedtak() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, null);
        fagsak.setAvsluttet();
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.of(behandling));
        when(behandlingRepository.finnSisteInnvilgetBehandling(any())).thenReturn(Optional.of(behandling));
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat.Builder()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        when(behandlingsresultatRepository.hentHvisEksisterer(any())).thenReturn(Optional.of(behandlingsresultat));
        FamilieHendelseGrunnlagEntitet fh = lagFamilieHendelseGrunnlag(FØDSELSDATO, Optional.empty());
        when(familieHendelseRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(fh));
        FødselForretningshendelse hendelse = new FødselForretningshendelse(singletonList(aktørId), FØDSELSDATO, Endringstype.OPPRETTET);

        // Act
        Map<BehandlingÅrsakType, List<Fagsak>> behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.keySet()).contains(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_FØDSEL).get(0)).isEqualTo(fagsak);
    }

    @Test
    public void skal_ikke_velge_sak_som_er_avsluttet_engangsstønadsak_med_avslått_vedtak() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, null);
        fagsak.setAvsluttet();
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.of(behandling));
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat.Builder()
            .medBehandlingResultatType(BehandlingResultatType.AVSLÅTT).buildFor(behandling);
        when(behandlingsresultatRepository.hentHvisEksisterer(any())).thenReturn(Optional.of(behandlingsresultat));

        FødselForretningshendelse hendelse = new FødselForretningshendelse(singletonList(aktørId), FØDSELSDATO, Endringstype.OPPRETTET);

        // Act
        Map<BehandlingÅrsakType, List<Fagsak>> behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.keySet()).contains(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)).isEmpty();
    }

    @Test
    public void skal_ikke_velge_sak_som_er_avsluttet_engangsstønadsak_med_tidligere_barn() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, null);
        fagsak.setAvsluttet();
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.of(behandling));
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat.Builder()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        when(behandlingsresultatRepository.hentHvisEksisterer(any())).thenReturn(Optional.of(behandlingsresultat));
        LocalDate gammelFødsel = LocalDate.now().minusYears(2);
        FamilieHendelseGrunnlagEntitet fh = lagFamilieHendelseGrunnlag(gammelFødsel, Optional.of(gammelFødsel));
        when(familieHendelseRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(fh));

        FødselForretningshendelse hendelse = new FødselForretningshendelse(singletonList(aktørId), FØDSELSDATO, Endringstype.OPPRETTET);

        // Act
        Map<BehandlingÅrsakType, List<Fagsak>> behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)).isEmpty();
    }

    @Test
    public void skal_velge_svangerskapspengersak_der_fødselsdatoen_er_før_tilkjent_ytelse_sluttdato() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.SVANGERSKAPSPENGER, null);
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.of(behandling));
        when(behandlingRepository.finnSisteInnvilgetBehandling(any())).thenReturn(Optional.of(behandling));
        when(beregningsresultatRepository.hentBeregningsresultat(any()))
            .thenReturn(Optional.of(opprettBeregningsresultatPerioder(FØDSELSDATO.plusDays(2))));
        FamilieHendelseGrunnlagEntitet fh = lagFamilieHendelseGrunnlag(FØDSELSDATO, Optional.empty());
        when(familieHendelseRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(fh));
        FødselForretningshendelse hendelse = new FødselForretningshendelse(singletonList(aktørId), FØDSELSDATO, Endringstype.OPPRETTET);

        // Act
        Map<BehandlingÅrsakType, List<Fagsak>> behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.keySet()).contains(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_FØDSEL).get(0)).isEqualTo(fagsak);
    }

    @Test
    public void skal_velge_svangerskapspengersak_der_fødselsdatoen_er_lik_tilkjent_ytelse_sluttdato() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.SVANGERSKAPSPENGER, null);
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.of(behandling));
        when(behandlingRepository.finnSisteInnvilgetBehandling(any())).thenReturn(Optional.of(behandling));
        when(beregningsresultatRepository.hentBeregningsresultat(any()))
            .thenReturn(Optional.of(opprettBeregningsresultatPerioder(FØDSELSDATO)));
        FamilieHendelseGrunnlagEntitet fh = lagFamilieHendelseGrunnlag(FØDSELSDATO, Optional.empty());
        when(familieHendelseRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(fh));
        FødselForretningshendelse hendelse = new FødselForretningshendelse(singletonList(aktørId), FØDSELSDATO, Endringstype.OPPRETTET);

        // Act
        Map<BehandlingÅrsakType, List<Fagsak>> behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.keySet()).contains(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_FØDSEL).get(0)).isEqualTo(fagsak);
    }

    @Test
    public void skal_ikke_velge_svangerskapspengersak_der_fødselsdatoen_er_etter_tilkjent_ytelse_sluttdato() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.SVANGERSKAPSPENGER, null);
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.of(behandling));
        when(behandlingRepository.finnSisteInnvilgetBehandling(any())).thenReturn(Optional.of(behandling));
        when(beregningsresultatRepository.hentBeregningsresultat(any()))
            .thenReturn(Optional.of(opprettBeregningsresultatPerioder(FØDSELSDATO.minusDays(2))));
        FødselForretningshendelse hendelse = new FødselForretningshendelse(singletonList(aktørId), FØDSELSDATO, Endringstype.OPPRETTET);

        // Act
        Map<BehandlingÅrsakType, List<Fagsak>> behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.keySet()).contains(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)).hasSize(0);
    }

    @Test
    public void annullert_fødselshendelse_skal_treffe_åpen_foreldrepengesak() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.of(behandling));
        FødselForretningshendelse hendelse = new FødselForretningshendelse(singletonList(aktørId), FØDSELSDATO, Endringstype.ANNULLERT);
        FamilieHendelseGrunnlagEntitet fh = lagFamilieHendelseGrunnlag(FØDSELSDATO, Optional.empty());
        when(familieHendelseRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(fh));

        // Act
        Map<BehandlingÅrsakType, List<Fagsak>> behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.keySet()).contains(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_FØDSEL).get(0)).isEqualTo(fagsak);
        verify(historikkinnslagTjeneste, times(1)).opprettHistorikkinnslagForEndringshendelse(eq(fagsak), anyString());
    }

    @Test
    public void korrigert_fødselshendelse_skal_treffe_åpen_foreldrepengesak() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.of(behandling));
        FødselForretningshendelse hendelse = new FødselForretningshendelse(singletonList(aktørId), FØDSELSDATO, Endringstype.KORRIGERT);
        FamilieHendelseGrunnlagEntitet fh = lagFamilieHendelseGrunnlag(FØDSELSDATO, Optional.empty());
        when(familieHendelseRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(fh));

        // Act
        Map<BehandlingÅrsakType, List<Fagsak>> behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.keySet()).contains(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_FØDSEL).get(0)).isEqualTo(fagsak);
        verify(historikkinnslagTjeneste, times(1)).opprettHistorikkinnslagForEndringshendelse(eq(fagsak), anyString());
    }

    private FamilieHendelseGrunnlagEntitet lagFamilieHendelseGrunnlag(LocalDate termindato, Optional<LocalDate> registrertFødselDato) {
        FamilieHendelseGrunnlagBuilder grunnlagBuilder = FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty());
        FamilieHendelseBuilder søknadBuilder =  FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.SØKNAD);
        FamilieHendelseBuilder.TerminbekreftelseBuilder terminbekreftelseBuilder = søknadBuilder.getTerminbekreftelseBuilder()
            .medTermindato(termindato).medUtstedtDato(termindato.minusWeeks(1));
        grunnlagBuilder.medSøknadVersjon(søknadBuilder.medTerminbekreftelse(terminbekreftelseBuilder));
        registrertFødselDato.ifPresent(fdato -> {
            grunnlagBuilder.medBekreftetVersjon(FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET)
                .medTerminbekreftelse(terminbekreftelseBuilder).medFødselsDato(fdato).medAntallBarn(1).leggTilBarn(new UidentifisertBarnEntitet(fdato)));
        });
        return grunnlagBuilder.build();
    }

    private BeregningsresultatEntitet opprettBeregningsresultatPerioder(LocalDate sluttDato) {
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("")
            .medRegelSporing("")
            .build();
        BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(sluttDato.minusDays(10), sluttDato)
            .build(beregningsresultat);
        return beregningsresultat;
    }
}
