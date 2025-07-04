package no.nav.foreldrepenger.familiehendelse.omsorg;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdateringTransisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.OmsorgsvilkårAksjonspunktOppdaterer;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.Foreldreansvarsvilkår1AksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.Foreldreansvarsvilkår2AksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.OmsorgsvilkårAksjonspunktDto;

class OmsorgsvilkårOppdatererTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private HistorikkinnslagRepository historikkinnslagRepository;

    @BeforeEach
    void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        historikkinnslagRepository = repositoryProvider.getHistorikkinnslagRepository();
    }

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_omsorgsvilkår() {
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();
        // Act
        var dto = new OmsorgsvilkårAksjonspunktDto("begrunnelse", true, "-");
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        var omsorgsvilkarOppdaterer = new OmsorgsvilkårAksjonspunktOppdaterer.OmsorgsvilkårOppdaterer(historikkinnslagRepository, repositoryProvider.getBehandlingRepository());
        var resultat = omsorgsvilkarOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto,
            aksjonspunkt));

        // Assert
        assertThat(aksjonspunkt.isToTrinnsBehandling()).isTrue();
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil()).hasSize(1);
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil().get(0).getVilkårType()).isEqualTo(VilkårType.OMSORGSVILKÅRET);
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil().get(0).getVilkårUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        var historikk = historikkinnslagRepository.hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikk.getLinjer()).hasSize(2);
        assertThat(historikk.getSkjermlenke()).isEqualTo(SkjermlenkeType.PUNKT_FOR_OMSORG);
        assertThat(historikk.getLinjer().get(0).getTekst()).contains("Omsorgsvilkåret", VilkårUtfallType.OPPFYLT.getNavn());
        assertThat(historikk.getLinjer().get(1).getTekst()).contains(dto.getBegrunnelse());
    }

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_foreldreansvar_andre_ledd_vilkår() {
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_2_LEDD,
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();
        // Act
        var dto = new Foreldreansvarsvilkår1AksjonspunktDto("begrunnelse", true, "-");
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        var oppdaterer = new OmsorgsvilkårAksjonspunktOppdaterer.Foreldreansvarsvilkår1Oppdaterer(historikkinnslagRepository, repositoryProvider.getBehandlingRepository());
        var resultat = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil()).hasSize(1);
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil().get(0).getVilkårType()).isEqualTo(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD);
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil().get(0).getVilkårUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        var historikk = historikkinnslagRepository.hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikk.getLinjer()).hasSize(2);
        assertThat(historikk.getSkjermlenke()).isEqualTo(SkjermlenkeType.PUNKT_FOR_FORELDREANSVAR);
        assertThat(historikk.getLinjer().get(0).getTekst()).contains("Foreldreansvarsvilkåret", VilkårUtfallType.OPPFYLT.getNavn());
        assertThat(historikk.getLinjer().get(1).getTekst()).contains(dto.getBegrunnelse());
    }

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_foreldreansvar_fjerde_ledd_vilkår() {
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_4_LEDD,
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();
        // Act
        var dto = new Foreldreansvarsvilkår2AksjonspunktDto("begrunnelse", false, Avslagsårsak.IKKE_FORELDREANSVAR_ALENE_ETTER_BARNELOVA.getKode());
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        var oppdaterer = new OmsorgsvilkårAksjonspunktOppdaterer.Foreldreansvarsvilkår2Oppdaterer(historikkinnslagRepository, repositoryProvider.getBehandlingRepository());
        var resultat = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        assertThat(resultat.kreverTotrinnsKontroll()).isFalse();
        assertThat(resultat.getTransisjon()).isEqualTo(AksjonspunktOppdateringTransisjon.AVSLAG_VILKÅR);
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil()).hasSize(1);
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil().get(0).getVilkårType()).isEqualTo(VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD);
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil().get(0).getVilkårUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil().get(0).getAvslagsårsak()).isEqualTo(Avslagsårsak.IKKE_FORELDREANSVAR_ALENE_ETTER_BARNELOVA);

        var historikk = historikkinnslagRepository.hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikk.getLinjer()).hasSize(2);
        assertThat(historikk.getSkjermlenke()).isEqualTo(SkjermlenkeType.PUNKT_FOR_FORELDREANSVAR);
        assertThat(historikk.getLinjer().get(0).getTekst()).contains("Foreldreansvarsvilkåret", VilkårUtfallType.IKKE_OPPFYLT.getNavn());
        assertThat(historikk.getLinjer().get(1).getTekst()).contains(dto.getBegrunnelse());
    }

}
