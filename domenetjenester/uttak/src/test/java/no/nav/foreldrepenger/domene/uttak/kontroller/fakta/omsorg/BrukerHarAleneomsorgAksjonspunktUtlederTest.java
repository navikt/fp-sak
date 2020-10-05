package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.omsorg;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class BrukerHarAleneomsorgAksjonspunktUtlederTest extends EntityManagerAwareTest {

    private static final AktørId AKTØR_ID_MOR = AktørId.dummy();
    private static final AktørId AKTØR_ID_FAR = AktørId.dummy();

    private final ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR);
    private UttakRepositoryProvider repositoryProvider;

    private BrukerHarAleneomsorgAksjonspunktUtleder aksjonspunktUtleder;

    @BeforeEach
    public void oppsett() {
        var entityManager = getEntityManager();
        repositoryProvider = new UttakRepositoryProvider(entityManager);
        var personopplysningTjeneste = new PersonopplysningTjeneste(new PersonopplysningRepository(entityManager));
        aksjonspunktUtleder = new BrukerHarAleneomsorgAksjonspunktUtleder(repositoryProvider, personopplysningTjeneste);

        // default scenario
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now()).build());

    }

    @Test
    public void ingen_aksjonspunkter_dersom_søker_oppgitt_ikke_ha_aleneomsorg() {

        prepScenarioMedSøkerOgEktefelle(RelasjonsRolleType.UDEFINERT, null);
        prepScenarioMedSøknadHendelseOgRettighet(true, false);

        Behandling behandling = scenario.lagre(repositoryProvider);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void aksjonspunkter_dersom_søker_oppgitt_ha_aleneomsorg_men_oppgitt_annenForeldre_og_ha_samme_address_som_bruker_i_tps() {

        prepScenarioMedSøkerOgEktefelle(RelasjonsRolleType.UDEFINERT, true);
        prepScenarioMedSøknadHendelseOgRettighet(false, true);
        scenario.medSøknadAnnenPart().medAktørId(AKTØR_ID_FAR);

        Behandling behandling = scenario.lagre(repositoryProvider);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunktResultater).containsExactly(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG);
    }

    @Test
    public void aksjonspunkter_dersom_bruker_ikke_oppgitt_annenForeldre_men_er_gift_og_ha_samme_address_som_bruker_i_tps() {

        prepScenarioMedSøkerOgEktefelle(RelasjonsRolleType.EKTE, true);
        prepScenarioMedSøknadHendelseOgRettighet(false, true);

        Behandling behandling = scenario.lagre(repositoryProvider);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunktResultater).containsExactly(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG);
    }

    private UttakInput lagInput(Behandling behandling) {
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        return new UttakInput(BehandlingReferanse.fra(behandling, skjæringstidspunkt), null, null);
    }

    private void prepScenarioMedSøkerOgEktefelle(RelasjonsRolleType rolle, Boolean sammeBosted) {
        PersonInformasjon.Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();

        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        AktørId partnerAktørId = AKTØR_ID_FAR;
        PersonInformasjon gift = builderForRegisteropplysninger
            .medPersonas()
            .mann(partnerAktørId, SivilstandType.GIFT)
            .relasjonTil(søkerAktørId, rolle, sammeBosted)
            .build();
        scenario.medRegisterOpplysninger(gift);

        PersonInformasjon søker = builderForRegisteropplysninger
            .medPersonas()
            .kvinne(søkerAktørId, SivilstandType.GIFT)
            .relasjonTil(partnerAktørId, rolle, sammeBosted)
            .build();

        scenario.medRegisterOpplysninger(søker);
    }

    private void prepScenarioMedSøknadHendelseOgRettighet(Boolean harAnnenForeldreRett, Boolean harAleneomsorgForBarnet) {
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(harAnnenForeldreRett, true, harAleneomsorgForBarnet);
        scenario.medOppgittRettighet(rettighet);
    }

}
