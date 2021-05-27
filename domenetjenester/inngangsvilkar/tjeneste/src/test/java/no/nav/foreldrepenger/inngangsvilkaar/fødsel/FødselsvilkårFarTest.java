package no.nav.foreldrepenger.inngangsvilkaar.fødsel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.UtsettelseCore2021;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktUtils;

public class FødselsvilkårFarTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private final SkjæringstidspunktUtils stputil = new SkjæringstidspunktUtils(Period.parse("P10M"),
        Period.parse("P6M"), Period.parse("P1Y"), Period.parse("P6M"));
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    private InngangsvilkårOversetter oversetter;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        var personopplysningTjeneste = new PersonopplysningTjeneste(
            repositoryProvider.getPersonopplysningRepository());
        oversetter = new InngangsvilkårOversetter(repositoryProvider,
            personopplysningTjeneste, new YtelseMaksdatoTjeneste(repositoryProvider, new RelatertBehandlingTjeneste(repositoryProvider)),
            iayTjeneste, Period.parse("P6M"));
        var ytelseMaksdatoTjeneste = new YtelseMaksdatoTjeneste(repositoryProvider,
            new RelatertBehandlingTjeneste(repositoryProvider));
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, ytelseMaksdatoTjeneste, stputil, mock(UtsettelseCore2021.class));
    }

    @Test // FP_VK 11.2 Vilkårsutfall oppfylt
    public void skal_vurdere_vilkår_som_oppfylt_når_søker_er_far_og_fødsel_bekreftet() throws IOException {
        // Arrange
        var behandling = lagBehandlingMedFarEllerMedmor(RelasjonsRolleType.FARA, NavBrukerKjønn.MANN, true, false, true);

        // Act
        var data = new InngangsvilkårFødselFar(oversetter).vurderVilkår(lagRef(behandling));

        var jsonNode = StandardJsonConfig.fromJsonAsTree(data.getRegelInput());
        var soekersKjonn = jsonNode.get("soekersKjonn").asText();

        // Assert
        assertThat(data.getVilkårType()).isEqualTo(VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(data.getRegelInput()).isNotEmpty();
        assertThat(soekersKjonn).isEqualTo("MANN");
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
    }

    @Test // FP_VK 11.2 Vilkårsutfall oppfylt
    public void skal_vurdere_vilkår_som_oppfylt_når_søker_er_medmor_og_fødsel_bekreftet() throws IOException {
        // Arrange
        var behandling = lagBehandlingMedFarEllerMedmor(RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE, true, false, true);

        // Act
        var data = new InngangsvilkårFødselFar(oversetter).vurderVilkår(lagRef(behandling));

        var jsonNode = StandardJsonConfig.fromJsonAsTree(data.getRegelInput());
        var soekersKjonn = jsonNode.get("soekersKjonn").asText();

        // Assert
        assertThat(data.getVilkårType()).isEqualTo(VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(data.getRegelInput()).isNotEmpty();
        assertThat(soekersKjonn).isEqualTo("KVINNE");
    }

    @Test // FP_VK 11.4 Vilkårsutfall ikke oppfylt
    public void skal_vurdere_vilkår_som_ikke_oppfylt_når_søker_er_medmor_og_fødsel_ikke_bekreftet_og_søkt_om_termin_og_mor_frisk() {
        // Arrange
        var behandling = lagBehandlingMedFarEllerMedmor(RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE, false, false, false);

        // Act
        var data = new InngangsvilkårFødselFar(oversetter).vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(data.getVilkårType()).isEqualTo(VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(data.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1028);
    }

    @Test // FP_VK 11.4 Vilkårsutfall oppfylt
    public void skal_vurdere_vilkår_som_oppfylt_når_søker_er_far_og_fødsel_ikke_bekreftet_og_søkt_om_termin_og_mor_syk() {
        // Arrange
        var behandling = lagBehandlingMedFarEllerMedmor(RelasjonsRolleType.FARA, NavBrukerKjønn.MANN, false, true, false);

        // Act
        var data = new InngangsvilkårFødselFar(oversetter).vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(data.getVilkårType()).isEqualTo(VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR);
        assertThat(data.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
    }

    private Behandling lagBehandlingMedFarEllerMedmor(RelasjonsRolleType rolle, NavBrukerKjønn kjønn, boolean fødselErBekreftet,
                                                      boolean morErSykVedFødsel, boolean erFødsel) {
        // Setup basis scenario
        var fødselsdato = LocalDate.now();
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        if (erFødsel) {
            scenario.medSøknadHendelse()
                .medFødselsDato(fødselsdato)
                .medAntallBarn(1)
                .medErMorForSykVedFødsel(morErSykVedFødsel);
        } else {
            scenario.medSøknadHendelse()
                .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                    .medTermindato(LocalDate.now())
                    .medUtstedtDato(LocalDate.now())
                    .medNavnPå("LEGEN min"))
                .medAntallBarn(1)
                .medErMorForSykVedFødsel(morErSykVedFødsel);
        }
        scenario.medBrukerKjønn(NavBrukerKjønn.MANN);

        // Legg til om fødsel er bekreftet eller om mor er syk ved fødsel
        if (fødselErBekreftet) {
            scenario.medBekreftetHendelse().medFødselsDato(fødselsdato).medAntallBarn(1);
        }
        if (morErSykVedFødsel) {
            scenario.medOverstyrtHendelse().medErMorForSykVedFødsel(true).medAntallBarn(1)
                .medTerminbekreftelse(scenario.medOverstyrtHendelse().getTerminbekreftelseBuilder()
                    .medTermindato(LocalDate.now())
                    .medUtstedtDato(LocalDate.now())
                    .medNavnPå("LEGEN min"));
        }

        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var barnAktørId = AktørId.dummy();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();

        var fødtBarn = builderForRegisteropplysninger
            .medPersonas()
            .fødtBarn(barnAktørId, fødselsdato)
            .relasjonTil(søkerAktørId, rolle, null)
            .build();

        var søker = builderForRegisteropplysninger
            .medPersonas()
            .voksenPerson(søkerAktørId, SivilstandType.GIFT, kjønn, Region.NORDEN)
            .statsborgerskap(Landkoder.NOR)
            .relasjonTil(barnAktørId, RelasjonsRolleType.BARN, null)
            .build();

        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(fødselsdato).build());
        scenario.medRegisterOpplysninger(søker);
        scenario.medRegisterOpplysninger(fødtBarn);

        return scenario.lagre(repositoryProvider);
    }
}
