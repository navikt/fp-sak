package no.nav.foreldrepenger.inngangsvilkaar.fødsel;

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
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingsgrunnlagKodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
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

public class FødselsvilkårMorTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
        new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));
    private BasisPersonopplysningTjeneste personopplysningTjeneste = new BasisPersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository(), Mockito.mock(BehandlingsgrunnlagKodeverkRepository.class));
    private InngangsvilkårOversetter oversetter = new InngangsvilkårOversetter(repositoryProvider,
        personopplysningTjeneste, new YtelseMaksdatoTjeneste(repositoryProvider,
            new RelatertBehandlingTjeneste(repositoryProvider)),
        iayTjeneste,
        Period.parse("P18W3D"));

    @Test
    public void skal_vurdere_vilkår_som_ikke_oppfylt_når_søker_ikke_er_kvinne() throws JsonProcessingException, IOException {
        // Arrange
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());

        leggTilSøker(scenario, NavBrukerKjønn.MANN);

        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        VilkårData data = new InngangsvilkårFødselMor(oversetter).vurderVilkår(lagRef(behandling));

        ObjectMapper om = new ObjectMapper();
        JsonNode jsonNode = om.readTree(data.getRegelInput());
        String soekersKjonn = jsonNode.get("soekersKjonn").asText();

        // Assert
        assertThat(data.getVilkårType()).isEqualTo(VilkårType.FØDSELSVILKÅRET_MOR);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(data.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1003);
        assertThat(data.getRegelInput()).isNotEmpty();
        assertThat(soekersKjonn).isEqualTo("MANN");
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

    @Test
    public void skal_vurdere_vilkår_som_oppfylt_når_søker_er_mor_og_fødsel_bekreftet() {
        // Arrange
        Behandling behandling = lagBehandlingMedMorEllerMedmor(RelasjonsRolleType.MORA);

        // Act
        VilkårData data = new InngangsvilkårFødselMor(oversetter).vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(data.getVilkårType()).isEqualTo(VilkårType.FØDSELSVILKÅRET_MOR);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
    }

    @Test
    public void skal_vurdere_vilkår_som_ikke_oppfylt_når_søker_ikke_er_mor_og_fødsel_bekreftet() {
        // Arrange
        Behandling behandling = lagBehandlingMedMorEllerMedmor(RelasjonsRolleType.FARA);

        // Act
        VilkårData data = new InngangsvilkårFødselMor(oversetter).vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(data.getVilkårType()).isEqualTo(VilkårType.FØDSELSVILKÅRET_MOR);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(data.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1002);
    }

    @Test
    public void skal_vurdere_vilkår_som_ikke_oppfylt_når_fødsel_ikke_bekreftet_termindato_ikke_passert_22_uker() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().plusWeeks(18).plusDays(4))
                .medNavnPå("LEGEN MIN")
                .medUtstedtDato(LocalDate.now()));
        scenario.medSøknadDato(LocalDate.now().minusDays(2))
            .medOverstyrtHendelse()
            .medTerminbekreftelse(scenario.medOverstyrtHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().plusWeeks(18).plusDays(4))
                .medNavnPå("LEGEN MIN")
                .medUtstedtDato(LocalDate.now()));

        leggTilSøker(scenario, NavBrukerKjønn.KVINNE);
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        VilkårData data = new InngangsvilkårFødselMor(oversetter).vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(data.getVilkårType()).isEqualTo(VilkårType.FØDSELSVILKÅRET_MOR);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(data.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1001);
    }

    @Test
    public void skal_vurdere_vilkår_som_ikke_oppfylt_når_fødsel_bekreftet_termindato_ikke_passert_22_uker() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().plusWeeks(18).plusDays(2))
                .medNavnPå("LEGEN MIN")
                .medUtstedtDato(LocalDate.now().minusDays(2)));
        scenario.medSøknadDato(LocalDate.now().minusDays(2))
            .medOverstyrtHendelse()
            .medTerminbekreftelse(scenario.medOverstyrtHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().plusWeeks(18).plusDays(2))
                .medNavnPå("LEGEN MIN")
                .medUtstedtDato(LocalDate.now().minusDays(2)));

        leggTilSøker(scenario, NavBrukerKjønn.KVINNE);
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        VilkårData data = new InngangsvilkårFødselMor(oversetter).vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(data.getVilkårType()).isEqualTo(VilkårType.FØDSELSVILKÅRET_MOR);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(data.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1019);
    }

    @Test
    public void skal_vurdere_vilkår_som_oppfylt_når_fødsel_ikke_bekreftet_termindato_passert_22_uker() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().minusDays(24))
                .medNavnPå("LEGEN MIN")
                .medUtstedtDato(LocalDate.now()));
        scenario.medSøknadDato(LocalDate.now().minusMonths(5))
            .medOverstyrtHendelse()
            .medTerminbekreftelse(scenario.medOverstyrtHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().minusDays(24))
                .medNavnPå("LEGEN MIN")
                .medUtstedtDato(LocalDate.now()));
        leggTilSøker(scenario, NavBrukerKjønn.KVINNE);
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        VilkårData data = new InngangsvilkårFødselMor(oversetter).vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(data.getVilkårType()).isEqualTo(VilkårType.FØDSELSVILKÅRET_MOR);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
    }

    @Test
    public void skal_vurdere_vilkår_som_ikke_oppfylt_dersom_fødsel_burde_vært_inntruffet() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().minusDays(26))
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("LEGE LEGESEN"));
        scenario.medOverstyrtHendelse()
            .medTerminbekreftelse(scenario.medOverstyrtHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().minusDays(26))
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("LEGE LEGESEN"));
        leggTilSøker(scenario, NavBrukerKjønn.KVINNE);
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        VilkårData data = new InngangsvilkårFødselMor(oversetter).vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(data.getVilkårType()).isEqualTo(VilkårType.FØDSELSVILKÅRET_MOR);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(data.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1026);
    }

    @Test
    public void skal_vurdere_vilkår_som_ikke_oppfylt_dersom_fødsel_med_0_barn() {
        // Arrange
        LocalDate fødselsdato = LocalDate.now();
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato).medAntallBarn(0);
        scenario.medBekreftetHendelse().medFødselsDato(fødselsdato).medAntallBarn(0);
        scenario.medBrukerKjønn(NavBrukerKjønn.KVINNE);
        leggTilSøker(scenario, NavBrukerKjønn.KVINNE);
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        VilkårData data = new InngangsvilkårFødselMor(oversetter).vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(data.getVilkårType()).isEqualTo(VilkårType.FØDSELSVILKÅRET_MOR);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(data.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1026);
    }

    private Behandling lagBehandlingMedMorEllerMedmor(RelasjonsRolleType rolle) {
        // Setup basis scenario
        LocalDate fødselsdato = LocalDate.now();
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato).medAntallBarn(1);
        scenario.medBekreftetHendelse().medFødselsDato(fødselsdato).medAntallBarn(1);
        scenario.medBrukerKjønn(NavBrukerKjønn.KVINNE);

        Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        AktørId barnAktørId = AktørId.dummy();
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();

        PersonInformasjon fødtBarn = builderForRegisteropplysninger
            .medPersonas()
            .fødtBarn(barnAktørId, fødselsdato)
            .relasjonTil(søkerAktørId, rolle, true)
            .build();

        PersonInformasjon søker = builderForRegisteropplysninger
            .medPersonas()
            .kvinne(søkerAktørId, SivilstandType.GIFT, Region.NORDEN)
            .statsborgerskap(Landkoder.NOR)
            .relasjonTil(barnAktørId, RelasjonsRolleType.BARN, true)
            .build();
        scenario.medRegisterOpplysninger(søker);
        scenario.medRegisterOpplysninger(fødtBarn);

        return scenario.lagre(repositoryProvider);
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
    }

}
