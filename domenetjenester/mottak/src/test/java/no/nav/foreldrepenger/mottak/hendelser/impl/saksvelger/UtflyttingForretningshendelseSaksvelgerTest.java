package no.nav.foreldrepenger.mottak.hendelser.impl.saksvelger;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.Endringstype;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.hendelser.freg.UtflyttingForretningshendelse;
import no.nav.foreldrepenger.mottak.hendelser.saksvelger.UtflyttingForretningshendelseSaksvelger;

@ExtendWith(MockitoExtension.class)
class UtflyttingForretningshendelseSaksvelgerTest {

    public static final LocalDate UTFLYTTINGSDATO = LocalDate.now();

    @Mock
    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private FagsakRepository fagsakRepository;

    @Mock
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    @Mock
    private BehandlingRepository behandlingRepository;

    @Mock
    private BeregningsresultatRepository beregningsresultatRepository;

    @Mock
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    private UtflyttingForretningshendelseSaksvelger saksvelger;

    @BeforeEach
    void before() {
        when(repositoryProvider.getFagsakRepository()).thenReturn(fagsakRepository);
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        lenient().when(repositoryProvider.getBeregningsresultatRepository()).thenReturn(beregningsresultatRepository);
        saksvelger = new UtflyttingForretningshendelseSaksvelger(repositoryProvider, familieHendelseTjeneste, historikkinnslagTjeneste);
    }

    @Test
    void skal_velge_sak_som_er_åpen_foreldrepengesak() {
        // Arrange
        var aktørId = AktørId.dummy();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        when(behandlingRepository.harÅpenOrdinærYtelseBehandlingerForFagsakId(any())).thenReturn(true);
        var hendelse = new UtflyttingForretningshendelse(singletonList(aktørId), UTFLYTTINGSDATO, Endringstype.OPPRETTET);

        // Act
        var behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1).containsKey(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING)).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING).get(0)).isEqualTo(fagsak);
    }


    @Test
    void skal_velge_sak_som_er_avsluttet_engangsstønadsak_med_innvilget_vedtak() {
        // Arrange
        var aktørId = AktørId.dummy();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, null);
        fagsak.setAvsluttet();
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.finnSisteInnvilgetBehandling(any())).thenReturn(Optional.of(behandling));
        when(familieHendelseTjeneste.erHendelseDatoRelevantForBehandling(any(), any())).thenReturn(Boolean.TRUE);
        var hendelse = new UtflyttingForretningshendelse(singletonList(aktørId), UTFLYTTINGSDATO, Endringstype.OPPRETTET);

        // Act
        var behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1).containsKey(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING)).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING).get(0)).isEqualTo(fagsak);
    }

    @Test
    void skal_ikke_velge_sak_som_er_avsluttet_engangsstønadsak_med_avslått_vedtak() {
        // Arrange
        var aktørId = AktørId.dummy();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, null);
        fagsak.setAvsluttet();
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        when(behandlingRepository.finnSisteInnvilgetBehandling(any())).thenReturn(Optional.empty());

        var hendelse = new UtflyttingForretningshendelse(singletonList(aktørId), UTFLYTTINGSDATO, Endringstype.OPPRETTET);

        // Act
        var behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1).containsKey(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING)).isEmpty();
    }

    @Test
    void skal_ikke_velge_sak_som_er_avsluttet_engangsstønadsak_med_tidligere_barn() {
        // Arrange
        var aktørId = AktørId.dummy();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, null);
        fagsak.setAvsluttet();
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.finnSisteInnvilgetBehandling(any())).thenReturn(Optional.of(behandling));
        when(familieHendelseTjeneste.erHendelseDatoRelevantForBehandling(any(), any())).thenReturn(Boolean.FALSE);
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));

        var hendelse = new UtflyttingForretningshendelse(singletonList(aktørId), UTFLYTTINGSDATO, Endringstype.OPPRETTET);

        // Act
        var behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING)).isEmpty();
    }

    @Test
    void skal_velge_foreldrepengersak_der_utflyttingsdatoen_er_før_tilkjent_ytelse_sluttdato() {
        // Arrange
        var aktørId = AktørId.dummy();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(any())).thenReturn(Optional.of(behandling));
        when(beregningsresultatRepository.hentUtbetBeregningsresultat(any())).thenReturn(
            Optional.of(opprettBeregningsresultatPerioder(UTFLYTTINGSDATO.plusDays(2))));
        var hendelse = new UtflyttingForretningshendelse(singletonList(aktørId), UTFLYTTINGSDATO, Endringstype.OPPRETTET);

        // Act
        var behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1).containsKey(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING)).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING).get(0)).isEqualTo(fagsak);
    }

    @Test
    void skal_velge_svangerskapspengersak_der_utflyttingsdatoen_er_lik_tilkjent_ytelse_sluttdato() {
        // Arrange
        var aktørId = AktørId.dummy();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.SVANGERSKAPSPENGER, null);
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(any())).thenReturn(Optional.of(behandling));
        when(beregningsresultatRepository.hentUtbetBeregningsresultat(any())).thenReturn(
            Optional.of(opprettBeregningsresultatPerioder(UTFLYTTINGSDATO)));
        var hendelse = new UtflyttingForretningshendelse(singletonList(aktørId), UTFLYTTINGSDATO, Endringstype.OPPRETTET);

        // Act
        var behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1).containsKey(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING)).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING).get(0)).isEqualTo(fagsak);
    }

    @Test
    void skal_ikke_velge_foreldrepengersak_der_utflyttingsdatoen_er_etter_tilkjent_ytelse_sluttdato() {
        // Arrange
        var aktørId = AktørId.dummy();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(any())).thenReturn(Optional.of(behandling));
        when(beregningsresultatRepository.hentUtbetBeregningsresultat(any())).thenReturn(
            Optional.of(opprettBeregningsresultatPerioder(UTFLYTTINGSDATO.minusDays(2))));
        var hendelse = new UtflyttingForretningshendelse(singletonList(aktørId), UTFLYTTINGSDATO, Endringstype.OPPRETTET);

        // Act
        var behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1).containsKey(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING)).isEmpty();
    }

    @Test
    void annullert_utflyttingshendelse_skal_ikke_treffe_foreldrepengesak() {
        // Arrange
        var aktørId = AktørId.dummy();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        var hendelse = new UtflyttingForretningshendelse(singletonList(aktørId), null, Endringstype.ANNULLERT);

        // Act
        var behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING)).isEmpty();
        verifyNoInteractions(historikkinnslagTjeneste);
    }


    private BeregningsresultatEntitet opprettBeregningsresultatPerioder(LocalDate sluttDato) {
        var beregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("").medRegelSporing("").build();
        BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(sluttDato.minusDays(10), sluttDato).build(beregningsresultat);
        return beregningsresultat;
    }
}
