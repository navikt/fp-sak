package no.nav.foreldrepenger.inngangsvilkaar.adopsjon;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingsgrunnlagKodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon.Builder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;

public class AdopsjonsvilkårEngangsstønadTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
        new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));
    private BasisPersonopplysningTjeneste personopplysningTjeneste = new BasisPersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository(), Mockito.mock(BehandlingsgrunnlagKodeverkRepository.class));
    private InngangsvilkårOversetter oversetter = new InngangsvilkårOversetter(repositoryProvider,
        personopplysningTjeneste, new YtelseMaksdatoTjeneste(repositoryProvider, new RelatertBehandlingTjeneste(repositoryProvider)),
        iayTjeneste, null);

    @Test
    public void skal_gi_avslag_barn_adopteres_er_over_15_år_på_overtakelsesdato() {
        Behandling behandling = settOppAdopsjonBehandlingForMor(16, false, NavBrukerKjønn.KVINNE, false);

        VilkårData data = new InngangsvilkårEngangsstønadAdopsjon(oversetter).vurderVilkår(lagRef(behandling));

        assertThat(data.getVilkårType()).isEqualTo(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(data.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1004);
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
    }

    @Test
    public void skal_gi_avslag_dersom_adoptert_barn_tilhører_ektefelle_eller_samboer() throws JsonProcessingException, IOException {
        Behandling behandling = settOppAdopsjonBehandlingForMor(10, true, NavBrukerKjønn.KVINNE, false);

        VilkårData data = new InngangsvilkårEngangsstønadAdopsjon(oversetter).vurderVilkår(lagRef(behandling));

        ObjectMapper om = new ObjectMapper();
        JsonNode jsonNode = om.readTree(data.getRegelInput());
        String ektefellesBarn = jsonNode.get("ektefellesBarn").asText();

        assertThat(data.getVilkårType()).isEqualTo(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(data.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1005);
        assertThat(data.getRegelInput()).isNotEmpty();
        assertThat(ektefellesBarn).isEqualTo("true");
    }

    @Test
    public void skal_gi_avslag_dersom_mann_ikke_adopterer_alene() {
        Behandling behandling = settOppAdopsjonBehandlingForMor(10, false, NavBrukerKjønn.MANN, false);

        VilkårData data = new InngangsvilkårEngangsstønadAdopsjon(oversetter).vurderVilkår(lagRef(behandling));

        assertThat(data.getVilkårType()).isEqualTo(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(data.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1006);
    }

    @Test
    public void skal_gi_innvilgelse_dersom_kvinne_adopterer_barn_10år_som_ikke_tilhører_ektefelle_eller_samboer() {
        Behandling behandling = settOppAdopsjonBehandlingForMor(10, false, NavBrukerKjønn.KVINNE, false);

        VilkårData data = new InngangsvilkårEngangsstønadAdopsjon(oversetter).vurderVilkår(lagRef(behandling));

        assertThat(data.getVilkårType()).isEqualTo(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD);
        System.out.println(data.getVilkårUtfallMerknad());
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(data.getVilkårUtfallMerknad()).isNull();
    }

    @Test
    public void skal_gi_innvilgelse_dersom_mann_alene_adopterer_barn_10år_som_ikke_tilhører_ektefelle_eller_samboer() {
        Behandling behandling = settOppAdopsjonBehandlingForMor(10, false, NavBrukerKjønn.MANN, true);

        VilkårData data = new InngangsvilkårEngangsstønadAdopsjon(oversetter).vurderVilkår(lagRef(behandling));

        assertThat(data.getVilkårType()).isEqualTo(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(data.getVilkårUtfallMerknad()).isNull();
    }

    private Behandling settOppAdopsjonBehandlingForMor(int alder, boolean ektefellesBarn, NavBrukerKjønn kjønn, boolean adoptererAlene) {
        LocalDate omsorgsovertakelsedato = LocalDate.of(2018, 1, 1);

        AbstractTestScenario<?> scenario = kjønn.equals(NavBrukerKjønn.KVINNE) ? ScenarioMorSøkerEngangsstønad.forAdopsjon()
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
        Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        PersonInformasjon søker = builderForRegisteropplysninger
            .medPersonas()
            .voksenPerson(søkerAktørId, SivilstandType.UOPPGITT, kjønn, Region.UDEFINERT)
            .build();
        scenario.medRegisterOpplysninger(søker);
    }

}
