package no.nav.foreldrepenger.inngangsvilkaar.adopsjon;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;

public class AdopsjonsvilkårEngangsstønadTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private InngangsvilkårOversetter oversetter;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
            new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));
        var personopplysningTjeneste = new PersonopplysningTjeneste(
            repositoryProvider.getPersonopplysningRepository());
        oversetter = new InngangsvilkårOversetter(repositoryProvider,
            personopplysningTjeneste, new YtelseMaksdatoTjeneste(repositoryProvider, new RelatertBehandlingTjeneste(repositoryProvider)),
            iayTjeneste, null);
    }

    @Test
    public void skal_gi_avslag_barn_adopteres_er_over_15_år_på_overtakelsesdato() {
        var behandling = settOppAdopsjonBehandlingForMor(16, false, NavBrukerKjønn.KVINNE, false);

        var data = new InngangsvilkårEngangsstønadAdopsjon(oversetter).vurderVilkår(lagRef(behandling));

        assertThat(data.getVilkårType()).isEqualTo(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(data.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1004);
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
    }

    @Test
    public void skal_gi_avslag_dersom_adoptert_barn_tilhører_ektefelle_eller_samboer() throws JsonProcessingException, IOException {
        var behandling = settOppAdopsjonBehandlingForMor(10, true, NavBrukerKjønn.KVINNE, false);

        var data = new InngangsvilkårEngangsstønadAdopsjon(oversetter).vurderVilkår(lagRef(behandling));

        var om = new ObjectMapper();
        var jsonNode = om.readTree(data.getRegelInput());
        var ektefellesBarn = jsonNode.get("ektefellesBarn").asText();

        assertThat(data.getVilkårType()).isEqualTo(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(data.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1005);
        assertThat(data.getRegelInput()).isNotEmpty();
        assertThat(ektefellesBarn).isEqualTo("true");
    }

    @Test
    public void skal_gi_avslag_dersom_mann_ikke_adopterer_alene() {
        var behandling = settOppAdopsjonBehandlingForMor(10, false, NavBrukerKjønn.MANN, false);

        var data = new InngangsvilkårEngangsstønadAdopsjon(oversetter).vurderVilkår(lagRef(behandling));

        assertThat(data.getVilkårType()).isEqualTo(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(data.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1006);
    }

    @Test
    public void skal_gi_innvilgelse_dersom_kvinne_adopterer_barn_10år_som_ikke_tilhører_ektefelle_eller_samboer() {
        var behandling = settOppAdopsjonBehandlingForMor(10, false, NavBrukerKjønn.KVINNE, false);

        var data = new InngangsvilkårEngangsstønadAdopsjon(oversetter).vurderVilkår(lagRef(behandling));

        assertThat(data.getVilkårType()).isEqualTo(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD);
        System.out.println(data.getVilkårUtfallMerknad());
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(data.getVilkårUtfallMerknad()).isNull();
    }

    @Test
    public void skal_gi_innvilgelse_dersom_mann_alene_adopterer_barn_10år_som_ikke_tilhører_ektefelle_eller_samboer() {
        var behandling = settOppAdopsjonBehandlingForMor(10, false, NavBrukerKjønn.MANN, true);

        var data = new InngangsvilkårEngangsstønadAdopsjon(oversetter).vurderVilkår(lagRef(behandling));

        assertThat(data.getVilkårType()).isEqualTo(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(data.getVilkårUtfallMerknad()).isNull();
    }

    private Behandling settOppAdopsjonBehandlingForMor(int alder, boolean ektefellesBarn, NavBrukerKjønn kjønn, boolean adoptererAlene) {
        var omsorgsovertakelsedato = LocalDate.of(2018, 1, 1);

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
            .voksenPerson(søkerAktørId, SivilstandType.UOPPGITT, kjønn, Region.UDEFINERT)
            .build();
        scenario.medRegisterOpplysninger(søker);
    }

}
