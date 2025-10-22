package no.nav.foreldrepenger.inngangsvilkaar.adopsjon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;

class AdopsjonsvilkårForeldrepengerTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;

    private PersonopplysningTjeneste personopplysningTjeneste;
    private AdopsjonsvilkårOversetter oversetter;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        personopplysningTjeneste = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository(),
            BehandlingEventPubliserer.NULL_EVENT_PUB);
        oversetter = new AdopsjonsvilkårOversetter(repositoryProvider, personopplysningTjeneste);
    }

    @Test
    void skal_gi_avslag_barn_adopteres_er_over_15_år_på_overtakelsesdato() {
        var behandling = settOppAdopsjonBehandlingFor(16, false, NavBrukerKjønn.KVINNE, false, LocalDate.of(2018, 1, 1));

        var data = new InngangsvilkårForeldrepengerAdopsjon(oversetter).vurderVilkår(lagRef(behandling));

        assertThat(data.vilkårType()).isEqualTo(VilkårType.ADOPSJONSVILKARET_FORELDREPENGER);
        assertThat(data.utfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(data.vilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1004);
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

    @Test
    void skal_gi_innvilgelse_dersom_stønadsperiode_for_annen_forelder_ikke_er_brukt_opp() {
        var omsorgsovertakelsedato = LocalDate.of(2018, 5, 1);

        var oversetter = new AdopsjonsvilkårOversetter(repositoryProvider, personopplysningTjeneste);

        var behandling = settOppAdopsjonBehandlingFor(
            10, true, NavBrukerKjønn.KVINNE, false, omsorgsovertakelsedato);

        var data = new InngangsvilkårForeldrepengerAdopsjon(oversetter).vurderVilkår(lagRef(behandling));

        var jsonNode = StandardJsonConfig.fromJsonAsTree(data.regelInput());
        var ektefellesBarn = jsonNode.get("ektefellesBarn").asText();

        assertThat(data.vilkårType()).isEqualTo(VilkårType.ADOPSJONSVILKARET_FORELDREPENGER);
        assertThat(data.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(data.vilkårUtfallMerknad()).isNull();
        assertThat(data.regelInput()).isNotEmpty();
        assertThat(ektefellesBarn).isEqualTo("true");
    }

    @Test
    void skal_gi_innvilgelse_dersom_kvinne_adopterer_barn_10år_som_ikke_tilhører_ektefelle_eller_samboer() {
        var behandling = settOppAdopsjonBehandlingFor(
            10, false, NavBrukerKjønn.KVINNE, false, LocalDate.of(2018, 1, 1));

        var data = new InngangsvilkårForeldrepengerAdopsjon(oversetter).vurderVilkår(lagRef(behandling));

        assertThat(data.vilkårType()).isEqualTo(VilkårType.ADOPSJONSVILKARET_FORELDREPENGER);
        System.out.println(data.vilkårUtfallMerknad());
        assertThat(data.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(data.vilkårUtfallMerknad()).isNull();
    }

    @Test
    void skal_gi_innvilgelse_dersom_mann_alene_adopterer_barn_10år_som_ikke_tilhører_ektefelle_eller_samboer() {
        var behandling = settOppAdopsjonBehandlingFor(
            10, false, NavBrukerKjønn.MANN, true, LocalDate.of(2018, 1, 1));

        var data = new InngangsvilkårForeldrepengerAdopsjon(oversetter).vurderVilkår(lagRef(behandling));

        assertThat(data.vilkårType()).isEqualTo(VilkårType.ADOPSJONSVILKARET_FORELDREPENGER);
        assertThat(data.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(data.vilkårUtfallMerknad()).isNull();
    }

    private Behandling settOppAdopsjonBehandlingFor(int alder, boolean ektefellesBarn, NavBrukerKjønn kjønn,
                                                    boolean adoptererAlene, LocalDate omsorgsovertakelsedato) {

        var scenario = kjønn.equals(NavBrukerKjønn.KVINNE) ? ScenarioMorSøkerEngangsstønad.forAdopsjon()
            : ScenarioFarSøkerEngangsstønad.forAdopsjon();

        leggTilSøker(scenario, kjønn);
        scenario.medSøknadHendelse()
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(omsorgsovertakelsedato)
                .medErEktefellesBarn(ektefellesBarn)
                .medAdoptererAlene(adoptererAlene))
            .leggTilBarn(omsorgsovertakelsedato.minusYears(alder));
        scenario.medBekreftetHendelse()
            .medAdopsjon(scenario.medBekreftetHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(omsorgsovertakelsedato)
                .medErEktefellesBarn(ektefellesBarn)
                .medAdoptererAlene(adoptererAlene))
            .leggTilBarn(omsorgsovertakelsedato.minusYears(alder));
        return scenario.lagre(repositoryProvider);
    }

    private void leggTilSøker(AbstractTestScenario<?> scenario, NavBrukerKjønn kjønn) {
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();
        var søker = builderForRegisteropplysninger
            .medPersonas()
            .voksenPerson(søkerAktørId, SivilstandType.UOPPGITT, kjønn)
            .build();
        scenario.medRegisterOpplysninger(søker);
    }
}
