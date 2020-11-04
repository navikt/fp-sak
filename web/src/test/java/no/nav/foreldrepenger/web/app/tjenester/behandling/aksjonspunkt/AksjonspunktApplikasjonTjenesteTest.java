package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Collections;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftTerminbekreftelseAksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.OmsorgsvilkårAksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt.AvklarSaksopplysningerDto;

@CdiDbAwareTest
public class AksjonspunktApplikasjonTjenesteTest {

    private static final String BEGRUNNELSE = "begrunnelse";
    private static final LocalDate TERMINDATO = LocalDate.now().plusDays(40);
    private static final LocalDate UTSTEDTDATO = LocalDate.now().minusDays(7);

    @Inject
    private AksjonspunktApplikasjonTjeneste aksjonspunktApplikasjonTjeneste;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    private AbstractTestScenario<?> lagScenarioMedAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultSøknadTerminbekreftelse();
        scenario.leggTilAksjonspunkt(aksjonspunktDefinisjon, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        return scenario;
    }

    @Test
    public void skal_sette_aksjonspunkt_til_utført_og_lagre_behandling() {
        // Arrange
        // Bruker BekreftTerminbekreftelseAksjonspunktDto som konkret case
        var scenario = lagScenarioMedAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE);
        var behandling = scenario.lagre(repositoryProvider);

        var dto = new BekreftTerminbekreftelseAksjonspunktDto(BEGRUNNELSE, TERMINDATO, UTSTEDTDATO, 1);

        // Act
        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto), behandling.getId());

        // Assert
        Behandling oppdatertBehandling = behandlingRepository.hentBehandling(behandling.getId());
        Assertions.assertThat(oppdatertBehandling.getAksjonspunkter()).first().matches(a -> a.erUtført());

    }

    @Test
    public void skal_håndtere_aksjonspunkt_for_omsorgsvilkåret() {
        AbstractTestScenario<?> scenario = lagScenarioMedAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET);
        Behandling behandling = scenario.lagre(repositoryProvider);
        OmsorgsvilkårAksjonspunktDto dto = new OmsorgsvilkårAksjonspunktDto(BEGRUNNELSE, false,
                Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_O.getKode());

        // Act
        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto), behandling.getId());

        // Assert
        assertThat(behandling.getBehandlingsresultat().getVilkårResultat()).isNotNull();
        assertThat(behandling.getBehandlingsresultat().getVilkårResultat().getVilkårene()).hasSize(1);
        assertThat(behandling.getBehandlingsresultat().getVilkårResultat().getVilkårene().iterator().next().getAvslagsårsak())
                .isEqualTo(Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_O);
        assertThat(behandling.getBehandlingStegTilstand().get().getBehandlingSteg()).isEqualTo(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT);
    }

    @Test
    public void skal_sette_ansvarlig_saksbehandler() {
        // Arrange
        // Bruker BekreftTerminbekreftelseAksjonspunktDto som konkret case
        AksjonspunktApplikasjonTjeneste aksjonspunktApplikasjonTjenesteImpl = aksjonspunktApplikasjonTjeneste;
        AbstractTestScenario<?> scenario = lagScenarioMedAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE);
        Behandling behandling = scenario.lagre(repositoryProvider);
        Behandling behandlingSpy = spy(behandling);

        BekreftTerminbekreftelseAksjonspunktDto dto = new BekreftTerminbekreftelseAksjonspunktDto(BEGRUNNELSE, TERMINDATO, UTSTEDTDATO, 1);

        // Act
        aksjonspunktApplikasjonTjenesteImpl.setAnsvarligSaksbehandler(singletonList(dto), behandlingSpy);

        // Assert
        verify(behandlingSpy, times(1)).setAnsvarligSaksbehandler(any());
    }

    @Test
    public void skal_ikke_sette_ansvarlig_saksbehandler_hvis_bekreftet_aksjonspunkt_er_fatter_vedtak() {
        // Arrange
        // Bruker BekreftTerminbekreftelseAksjonspunktDto som konkret case
        AksjonspunktApplikasjonTjeneste aksjonspunktApplikasjonTjenesteImpl = aksjonspunktApplikasjonTjeneste;
        AbstractTestScenario<?> scenario = lagScenarioMedAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE);
        Behandling behandling = scenario.lagre(repositoryProvider);
        Behandling behandlingSpy = spy(behandling);

        FatterVedtakAksjonspunktDto dto = new FatterVedtakAksjonspunktDto(BEGRUNNELSE, Collections.emptyList());

        // Act
        aksjonspunktApplikasjonTjenesteImpl.setAnsvarligSaksbehandler(singletonList(dto), behandlingSpy);

        // Assert
        verify(behandlingSpy, never()).setAnsvarligSaksbehandler(any());
    }

    @Test
    public void skal_sette_totrinn_når_revurdering_ap_medfører_endring_i_grunnlag() {
        // Arrange
        Behandling førstegangsbehandling = opprettFørstegangsbehandlingMedAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FAKTA_FOR_PERSONSTATUS);
        AksjonspunktTestSupport.setTilUtført(førstegangsbehandling.getAksjonspunkter().iterator().next(), BEGRUNNELSE);
        Behandling revurdering = opprettRevurderingsbehandlingMedAksjonspunkt(førstegangsbehandling,
                AksjonspunktDefinisjon.AVKLAR_FAKTA_FOR_PERSONSTATUS);
        AvklarSaksopplysningerDto dto = new AvklarSaksopplysningerDto(BEGRUNNELSE, "UTVA", true);

        // Act
        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto), revurdering.getId());

        // Assert
        Behandling oppdatertBehandling = behandlingRepository.hentBehandling(revurdering.getId());
        Aksjonspunkt aksjonspunkt = oppdatertBehandling.getAksjonspunkter().iterator().next();
        assertThat(aksjonspunkt.isToTrinnsBehandling()).isTrue();
    }

    @Test
    public void skal_hoppe_til_uttak_ved_avslag_vilkår() {
        // Arrange
        Behandling førstegangsbehandling = opprettFørstegangsbehandlingMedAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET);
        AksjonspunktTestSupport.setTilUtført(førstegangsbehandling.getAksjonspunkter().iterator().next(), BEGRUNNELSE);
        Behandling revurdering = opprettRevurderingsbehandlingMedAksjonspunkt(førstegangsbehandling,
                AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET);
        OmsorgsvilkårAksjonspunktDto dto = new OmsorgsvilkårAksjonspunktDto(BEGRUNNELSE, false,
                Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_O.getKode());

        // Act
        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto), revurdering.getId());

        // Assert
        Behandling oppdatertBehandling = behandlingRepository.hentBehandling(revurdering.getId());
        assertThat(oppdatertBehandling.getBehandlingStegTilstand().get().getBehandlingSteg())
                .isEqualTo(BehandlingStegType.SØKNADSFRIST_FORELDREPENGER);
        assertThat(oppdatertBehandling.getBehandlingsresultat().getVilkårResultat()).isNotNull();
        assertThat(oppdatertBehandling.getBehandlingsresultat().getVilkårResultat().getVilkårene()).hasSize(1);
        assertThat(oppdatertBehandling.getBehandlingsresultat().getVilkårResultat().getVilkårene().iterator().next().getAvslagsårsak())
                .isEqualTo(Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_O);
    }

    private Behandling opprettFørstegangsbehandlingMedAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        var førstegangsscenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        førstegangsscenario.medSøknad().medMottattDato(LocalDate.now());
        førstegangsscenario.medSøknadHendelse()
                .medAntallBarn(1)
                .medTerminbekreftelse(førstegangsscenario.medSøknadHendelse()
                        .getTerminbekreftelseBuilder()
                        .medTermindato(TERMINDATO)
                        .medUtstedtDato(UTSTEDTDATO));

        førstegangsscenario.leggTilAksjonspunkt(aksjonspunktDefinisjon, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        Behandling behandling = førstegangsscenario.lagre(repositoryProvider);
        repositoryProvider.getYtelsesFordelingRepository().lagre(behandling.getId(), avklarteDatoer()); // HACK
        return behandling;
    }

    private AvklarteUttakDatoerEntitet avklarteDatoer() {
        return new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now()).build();
    }

    private Behandling opprettRevurderingsbehandlingMedAksjonspunkt(Behandling førstegangsbehandling, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        Long behandlingId = førstegangsbehandling.getId();
        avsluttBehandlingOgFagsak(førstegangsbehandling);

        var revurderingsscenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
                .medBehandlingType(BehandlingType.REVURDERING);
        revurderingsscenario.medSøknad().medMottattDato(LocalDate.now());
        revurderingsscenario.leggTilAksjonspunkt(aksjonspunktDefinisjon, BehandlingStegType.KONTROLLER_FAKTA);

        Behandling revurdering = revurderingsscenario.lagre(repositoryProvider);

        Long revurderingId = revurdering.getId();
        repositoryProvider.getFamilieHendelseRepository().kopierGrunnlagFraEksisterendeBehandling(behandlingId, revurderingId);
        repositoryProvider.getYtelsesFordelingRepository().lagre(revurderingId, avklarteDatoer()); // HACK
        return revurdering;
    }

    private void avsluttBehandlingOgFagsak(Behandling behandling) {
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.LØPENDE);
    }

}
