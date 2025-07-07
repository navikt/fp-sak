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
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.Endringstype;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.hendelser.freg.DødfødselForretningshendelse;
import no.nav.foreldrepenger.mottak.hendelser.saksvelger.DødfødselForretningshendelseSaksvelger;

@ExtendWith(MockitoExtension.class)
class DødfødselForretningshendelseSaksvelgerTest {

    @Mock
    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private FagsakRepository fagsakRepository;

    @Mock
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    @Mock
    private BehandlingRepository behandlingRepository;

    @Mock
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    private DødfødselForretningshendelseSaksvelger saksvelger;

    @BeforeEach
    void before() {
        when(repositoryProvider.getFagsakRepository()).thenReturn(fagsakRepository);
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        saksvelger = new DødfødselForretningshendelseSaksvelger(repositoryProvider, familieHendelseTjeneste, historikkinnslagTjeneste);
    }

    @Test
    void skal_velge_sak_som_er_åpen_foreldrepengesak() {
        // Arrange
        var aktørId = AktørId.dummy();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.of(behandling));
        when(familieHendelseTjeneste.erHendelseDatoRelevantForBehandling(any(), any())).thenReturn(Boolean.TRUE);

        var hendelse = new DødfødselForretningshendelse(singletonList(aktørId), LocalDate.now(), Endringstype.OPPRETTET);

        // Act
        var behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1)
            .containsKey(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL)).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL).get(0)).isEqualTo(fagsak);
    }

    @Test
    void skal_ikke_velge_sak_som_er_avsluttet_foreldrepengesak() {
        // Arrange
        var aktørId = AktørId.dummy();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        fagsak.setAvsluttet();
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));

        var hendelse = new DødfødselForretningshendelse(singletonList(aktørId), LocalDate.now(), Endringstype.OPPRETTET);

        // Act
        var behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1)
            .containsKey(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL)).isEmpty();
    }

    @Test
    void skal_ikke_velge_engangsstønadsak() {
        // Arrange
        var aktørId = AktørId.dummy();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, null);
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));

        var hendelse = new DødfødselForretningshendelse(singletonList(aktørId), LocalDate.now(), Endringstype.OPPRETTET);

        // Act
        var behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1)
            .containsKey(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL)).isEmpty();
    }

    @Test
    void annullert_dødfødselshendelse_skal_treffe_åpen_foreldrepengesak() {
        // Arrange
        var aktørId = AktørId.dummy();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));

        var hendelse = new DødfødselForretningshendelse(singletonList(aktørId), LocalDate.now(), Endringstype.ANNULLERT);

        // Act
        var behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1)
            .containsKey(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL)).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL).get(0)).isEqualTo(fagsak);
        verify(historikkinnslagTjeneste, times(1)).opprettHistorikkinnslagForEndringshendelse(eq(fagsak), anyString());
    }

    @Test
    void korrigert_dødfødselshendelse_skal_treffe_åpen_foreldrepengesak() {
        // Arrange
        var aktørId = AktørId.dummy();
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.of(behandling));
        when(familieHendelseTjeneste.erHendelseDatoRelevantForBehandling(any(), any())).thenReturn(Boolean.TRUE);

        var hendelse = new DødfødselForretningshendelse(singletonList(aktørId), LocalDate.now(), Endringstype.KORRIGERT);

        // Act
        var behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1)
            .containsKey(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL)).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL).get(0)).isEqualTo(fagsak);
        verify(historikkinnslagTjeneste, times(1)).opprettHistorikkinnslagForEndringshendelse(eq(fagsak), anyString());
    }

}
