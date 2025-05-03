package no.nav.foreldrepenger.behandling.steg.innhentsaksopplysninger;

import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunktMedFrist;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.impl.EtterlysInntektsmeldingTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ExtendWith(MockitoExtension.class)
class InnhentRegisteropplysningerResterendeOppgaverStegImplTest {

    @Mock
    private DokumentBestillerTjeneste dokumentBestillerTjenesteMock;
    @Mock
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    @Mock
    private PersonopplysningTjeneste personopplysningTjeneste;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Mock
    private Kompletthetsjekker kompletthetsjekker;

    private BehandlingRepositoryProvider repositoryProvider;
    private InnhentRegisteropplysningerResterendeOppgaverStegImpl steg;

    @BeforeEach
    void setUp() {
        repositoryProvider = ScenarioMorSøkerForeldrepenger.forFødsel().mockBehandlingRepositoryProvider();
        var etterlysInntektsmeldingTjeneste = new EtterlysInntektsmeldingTjeneste(
            dokumentBestillerTjenesteMock,
            dokumentBehandlingTjeneste
        );
        steg = new InnhentRegisteropplysningerResterendeOppgaverStegImpl(
            repositoryProvider.getBehandlingRepository(),
            mock(FagsakTjeneste.class),
            personopplysningTjeneste,
            mock(FamilieHendelseTjeneste.class),
            mock(BehandlendeEnhetTjeneste.class),
            kompletthetsjekker,
            mock(FagsakEgenskapRepository.class),
            skjæringstidspunktTjeneste,
            etterlysInntektsmeldingTjeneste
        );
    }

    @Test
    void skal_ikke_etterlyse_IM_hvis_kompletthetsjekk_er_oppfylt() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        when(kompletthetsjekker.vurderEtterlysningInntektsmelding(any(), any())).thenReturn(KompletthetResultat.oppfylt());
        mockPersonopplysningKall(behandling);
        mockSkjæringstidspunkt(LocalDate.now().plusWeeks(2));

        // Act
        var resultat = steg.utførSteg(new BehandlingskontrollKontekst(behandling, new BehandlingLås(behandling.getId())));

        // Assert
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
        verify(dokumentBestillerTjenesteMock, never()).bestillDokument(any(DokumentBestilling.class));
    }

    @Test
    void skal_ikke_etterlyse_IM_hvis_behandlingårsak_er_relatert_til_død() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        var revudering = lagRevurdering(behandling, BehandlingÅrsakType.RE_HENDELSE_DØD_BARN);

        mockPersonopplysningKall(revudering);
        mockSkjæringstidspunkt(LocalDate.now().plusWeeks(2));

        // Act
        var resultat = steg.utførSteg(new BehandlingskontrollKontekst(revudering, new BehandlingLås(revudering.getId())));

        // Assert
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
        verify(dokumentBestillerTjenesteMock, never()).bestillDokument(any(DokumentBestilling.class));
    }

    @Test
    void skal_ikke_etterlyse_IM_hvis_stp_er_mer_enn_6_uker_siden() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        mockPersonopplysningKall(behandling);
        mockSkjæringstidspunkt(LocalDate.now().minusWeeks(6).minusDays(1));

        // Act
        var resultat = steg.utførSteg(new BehandlingskontrollKontekst(behandling, new BehandlingLås(behandling.getId())));

        // Assert
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
        verify(dokumentBestillerTjenesteMock, never()).bestillDokument(any(DokumentBestilling.class));
        verify(kompletthetsjekker, never()).vurderEtterlysningInntektsmelding(any(), any());
    }


    @Test
    void skal_ikke_sende_brev_når_etterlysning_sendt() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        when(kompletthetsjekker.vurderEtterlysningInntektsmelding(any(), any())).thenReturn(KompletthetResultat.fristUtløpt());
        when(dokumentBehandlingTjeneste.erDokumentBestilt(any(), any())).thenReturn(true);
        mockPersonopplysningKall(behandling);
        mockSkjæringstidspunkt(LocalDate.now().plusWeeks(2));

        // Act
        var resultat = steg.utførSteg(new BehandlingskontrollKontekst(behandling, new BehandlingLås(behandling.getId())));


        // Assert
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
        verify(dokumentBestillerTjenesteMock, never()).bestillDokument(any(DokumentBestilling.class));
    }

    @Test
    void skal_etterlyse_IM_hvis_kompletthetsjekk_er_ikke_oppfylt_og_ventefrist_er_passert() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        var ventefrist = LocalDateTime.now().plusWeeks(1);
        when(kompletthetsjekker.vurderEtterlysningInntektsmelding(any(), any())).thenReturn(KompletthetResultat.ikkeOppfylt(ventefrist, Venteårsak.VENT_OPDT_INNTEKTSMELDING));
        when(dokumentBehandlingTjeneste.erDokumentBestilt(any(), any())).thenReturn(false);
        mockSkjæringstidspunkt(LocalDate.now().plusWeeks(2));

        // Act
        var resultat = steg.utførSteg(new BehandlingskontrollKontekst(behandling, new BehandlingLås(behandling.getId())));

        // Assert
        assertThat(resultat.getAksjonspunktResultater()).containsExactly(opprettForAksjonspunktMedFrist(AUTO_VENT_ETTERLYST_INNTEKTSMELDING, Venteårsak.VENT_OPDT_INNTEKTSMELDING, ventefrist));
        verify(dokumentBestillerTjenesteMock, times(1)).bestillDokument(any(DokumentBestilling.class));
    }

    @Test
    void skal_etterlyse_IM_hvis_kompletthetsjekk_er_ikke_oppfylt_og_ventefrist_er_utgått() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        when(kompletthetsjekker.vurderEtterlysningInntektsmelding(any(), any())).thenReturn(KompletthetResultat.fristUtløpt());
        when(dokumentBehandlingTjeneste.erDokumentBestilt(any(), any())).thenReturn(false);
        mockPersonopplysningKall(behandling);
        mockSkjæringstidspunkt(LocalDate.now().plusWeeks(2));

        // Act
        var resultat = steg.utførSteg(new BehandlingskontrollKontekst(behandling, new BehandlingLås(behandling.getId())));


        // Assert
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
        assertThat(resultat.getAksjonspunktResultater()).isEmpty();
        verify(dokumentBestillerTjenesteMock, times(1)).bestillDokument(any(DokumentBestilling.class));
    }

    private void mockPersonopplysningKall(Behandling behandling) {
        when(personopplysningTjeneste.hentPersonopplysninger(any())).thenReturn(opprettPersonopplysningAggregatForPerson(behandling.getAktørId()));
    }

    private void mockSkjæringstidspunkt(LocalDate utledetSkjæringstidspunkt) {
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(utledetSkjæringstidspunkt).build());
    }

    private PersonopplysningerAggregat opprettPersonopplysningAggregatForPerson(AktørId aktørId) {
        var builder = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder.leggTil(builder.getPersonopplysningBuilder(aktørId).medFødselsdato(LocalDate.now().minusYears(25)));
        var entitet = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder).build();
        return new PersonopplysningerAggregat(entitet, aktørId);
    }

    private Behandling lagRevurdering(Behandling originalBehandling, BehandlingÅrsakType årsak) {
        var revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medKopiAvForrigeBehandlingsresultat()
            .medBehandlingÅrsak(
                BehandlingÅrsak.builder(årsak)
                    .medManueltOpprettet(true)
                    .medOriginalBehandlingId(originalBehandling.getId()))
            .build();
        repositoryProvider.getBehandlingRepository().lagre(revurdering, repositoryProvider.getBehandlingRepository().taSkriveLås(revurdering));
        return revurdering;
    }
}
