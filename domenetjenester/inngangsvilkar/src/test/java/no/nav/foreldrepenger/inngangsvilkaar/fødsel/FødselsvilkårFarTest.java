package no.nav.foreldrepenger.inngangsvilkaar.fødsel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
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
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.MinsterettBehandling2022;

class FødselsvilkårFarTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private MinsterettBehandling2022 mockMinsterett;

    private FødselsvilkårOversetter oversetter;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        var personopplysningTjeneste = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());
        oversetter = new FødselsvilkårOversetter(repositoryProvider, personopplysningTjeneste, Period.parse("P6M"));
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        var ytelseMaksdatoTjeneste = new YtelseMaksdatoTjeneste(new RelatertBehandlingTjeneste(repositoryProvider, fagsakRelasjonTjeneste), repositoryProvider.getFpUttakRepository(),
            fagsakRelasjonTjeneste);
        mockMinsterett = mock(MinsterettBehandling2022.class);
        when(mockMinsterett.utenMinsterett(any())).thenReturn(false);
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, ytelseMaksdatoTjeneste, mockMinsterett);
    }

    @Test // FP_VK 11.2 Vilkårsutfall oppfylt
    public void skal_vurdere_vilkår_som_oppfylt_når_søker_er_far_og_fødsel_bekreftet() {
        // Arrange
        var behandling = lagBehandlingMedFarEllerMedmor(RelasjonsRolleType.FARA, NavBrukerKjønn.MANN, true, false, true);

        // Act
        var data = new InngangsvilkårFødselFar(oversetter, skjæringstidspunktTjeneste).vurderVilkår(lagRef(behandling));

        var jsonNode = StandardJsonConfig.fromJsonAsTree(data.regelInput());
        var soekersKjonn = jsonNode.get("soekersKjonn").asText();

        // Assert
        assertThat(data.vilkårType()).isEqualTo(VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR);
        assertThat(data.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(data.regelInput()).isNotEmpty();
        assertThat(soekersKjonn).isEqualTo("MANN");
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

    @Test // FP_VK 11.2 Vilkårsutfall oppfylt
    public void skal_vurdere_vilkår_som_oppfylt_når_søker_er_medmor_og_fødsel_bekreftet() {
        // Arrange
        var behandling = lagBehandlingMedFarEllerMedmor(RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE, true, false, true);

        // Act
        var data = new InngangsvilkårFødselFar(oversetter, skjæringstidspunktTjeneste).vurderVilkår(lagRef(behandling));

        var jsonNode = StandardJsonConfig.fromJsonAsTree(data.regelInput());
        var soekersKjonn = jsonNode.get("soekersKjonn").asText();

        // Assert
        assertThat(data.vilkårType()).isEqualTo(VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR);
        assertThat(data.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(data.regelInput()).isNotEmpty();
        assertThat(soekersKjonn).isEqualTo("KVINNE");
    }

    @Test // FP_VK 11.4 Vilkårsutfall ikke oppfylt
    public void skal_vurdere_vilkår_som_ikke_oppfylt_når_søker_er_medmor_og_fødsel_ikke_bekreftet_og_søkt_om_termin_og_mor_frisk() {
        // Arrange
        var behandling = lagBehandlingMedFarEllerMedmor(RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE, false, false, false,
            LocalDate.of(2020,1,1));
        when(mockMinsterett.utenMinsterett(any())).thenReturn(true);
        // Act
        var data = new InngangsvilkårFødselFar(oversetter, skjæringstidspunktTjeneste).vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(data.vilkårType()).isEqualTo(VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR);
        assertThat(data.utfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(data.vilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1028);
    }

    @Test // FP_VK 11.4 Vilkårsutfall ikke oppfylt
    public void skal_vurdere_vilkår_som_oppfylt_når_søker_er_medmor_og_fødsel_ikke_bekreftet_og_søkt_om_termin_uansett_mors_helse() {
        // Arrange
        var behandling = lagBehandlingMedFarEllerMedmor(RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE, false, false, false);
        when(mockMinsterett.utenMinsterett(any())).thenReturn(false);
        // Act
        var data = new InngangsvilkårFødselFar(oversetter, skjæringstidspunktTjeneste).vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(data.vilkårType()).isEqualTo(VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR);
        assertThat(data.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
    }

    @Test // FP_VK 11.4 Vilkårsutfall oppfylt
    public void skal_vurdere_vilkår_som_oppfylt_når_søker_er_far_og_fødsel_ikke_bekreftet_og_søkt_om_termin_og_mor_syk() {
        // Arrange
        var behandling = lagBehandlingMedFarEllerMedmor(RelasjonsRolleType.FARA, NavBrukerKjønn.MANN, false, true, false);

        // Act
        var data = new InngangsvilkårFødselFar(oversetter, skjæringstidspunktTjeneste).vurderVilkår(lagRef(behandling));

        // Assert
        assertThat(data.vilkårType()).isEqualTo(VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR);
        assertThat(data.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
    }

    private Behandling lagBehandlingMedFarEllerMedmor(RelasjonsRolleType rolle, NavBrukerKjønn kjønn, boolean fødselErBekreftet,
                                                      boolean morErSykVedFødsel, boolean erFødsel) {
        return lagBehandlingMedFarEllerMedmor(rolle, kjønn, fødselErBekreftet, morErSykVedFødsel, erFødsel, LocalDate.now());
    }

    private Behandling lagBehandlingMedFarEllerMedmor(RelasjonsRolleType rolle, NavBrukerKjønn kjønn, boolean fødselErBekreftet,
                                                      boolean morErSykVedFødsel, boolean erFødsel, LocalDate fødselsdato) {
        // Setup basis scenario
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        if (erFødsel) {
            scenario.medSøknadHendelse()
                .medFødselsDato(fødselsdato)
                .medAntallBarn(1)
                .medErMorForSykVedFødsel(morErSykVedFødsel);
        } else {
            scenario.medSøknadHendelse()
                .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                    .medTermindato(fødselsdato)
                    .medUtstedtDato(fødselsdato)
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
                    .medTermindato(fødselsdato)
                    .medUtstedtDato(fødselsdato)
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
            .voksenPerson(søkerAktørId, SivilstandType.GIFT, kjønn)
            .statsborgerskap(Landkoder.NOR)
            .relasjonTil(barnAktørId, RelasjonsRolleType.BARN, null)
            .build();

        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(fødselsdato).build());
        scenario.medRegisterOpplysninger(søker);
        scenario.medRegisterOpplysninger(fødtBarn);

        return scenario.lagre(repositoryProvider);
    }
}
