package no.nav.foreldrepenger.mottak.hendelser.impl.saksvelger;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
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
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.dødsfall.DødfødselForretningshendelse;
import no.nav.foreldrepenger.mottak.hendelser.saksvelger.DødfødselForretningshendelseSaksvelger;

public class DødfødselForretningshendelseSaksvelgerTest {

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

    private DødfødselForretningshendelseSaksvelger saksvelger;

    @Before
    public void before() {
        when(repositoryProvider.getFagsakRepository()).thenReturn(fagsakRepository);
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(repositoryProvider.getFamilieHendelseRepository()).thenReturn(familieHendelseRepository);
        saksvelger = new DødfødselForretningshendelseSaksvelger(repositoryProvider);
    }

    @Test
    public void skal_velge_sak_som_er_åpen_foreldrepengesak() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.of(behandling));
        FamilieHendelseGrunnlagEntitet fh = lagFamilieHendelseGrunnlag(LocalDate.now().plusMonths(1), Optional.empty());
        when(familieHendelseRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(fh));

        DødfødselForretningshendelse hendelse = new DødfødselForretningshendelse(singletonList(aktørId), LocalDate.now());

        // Act
        Map<BehandlingÅrsakType, List<Fagsak>> behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.keySet()).contains(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL)).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL).get(0)).isEqualTo(fagsak);
    }

    @Test
    public void skal_ikke_velge_sak_som_er_avsluttet_foreldrepengesak() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        fagsak.setAvsluttet();
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.of(behandling));

        DødfødselForretningshendelse hendelse = new DødfødselForretningshendelse(singletonList(aktørId), LocalDate.now());

        // Act
        Map<BehandlingÅrsakType, List<Fagsak>> behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.keySet()).contains(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL)).isEmpty();
    }

    @Test
    public void skal_ikke_velge_engangsstønadsak() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, null);
        when(fagsakRepository.hentForBruker(aktørId)).thenReturn(singletonList(fagsak));
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.of(behandling));

        DødfødselForretningshendelse hendelse = new DødfødselForretningshendelse(singletonList(aktørId), LocalDate.now());

        // Act
        Map<BehandlingÅrsakType, List<Fagsak>> behandlingÅrsakTypeListMap = saksvelger.finnRelaterteFagsaker(hendelse);

        // Assert
        assertThat(behandlingÅrsakTypeListMap).hasSize(1);
        assertThat(behandlingÅrsakTypeListMap.keySet()).contains(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL);
        assertThat(behandlingÅrsakTypeListMap.get(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL)).isEmpty();
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

}
