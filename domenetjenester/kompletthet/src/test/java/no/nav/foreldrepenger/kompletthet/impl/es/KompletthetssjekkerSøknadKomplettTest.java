package no.nav.foreldrepenger.kompletthet.impl.es;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetsjekkerOld;

/**
 * Test for kompletthetssjekk for engangsstønad
 */
class KompletthetssjekkerSøknadKomplettTest {

    private KompletthetsjekkerOld testObjekt;

    @Test
    void ikke_elektronisk_reg_søknad_skal_behandles_som_komplett_ved_adopsjon_og_mangler_vedlegg() {
        var behandling = lagMocketBehandling(false, false, true, LocalDate.now(), false);
        var resultat = testObjekt.erForsendelsesgrunnlagKomplett(lagRef(behandling));
        assertThat(resultat).isTrue();
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        return BehandlingReferanse.fra(behandling);
    }

    @Test
    void ikke_elektronisk_reg_søknad_skal_behandles_som_komplett_ved_adopsjon_og_mangler_ikke_vedlegg() {
        var behandling = lagMocketBehandling(false, false, false, LocalDate.now(), false);
        var resultat = testObjekt.erForsendelsesgrunnlagKomplett(lagRef(behandling));
        assertThat(resultat).isTrue();
    }

    @Test
    void ikke_elektronisk_reg_søknad_skal_behandles_som_komplett_ved_fødsel_og_mangler_vedlegg() {
        var behandling = lagMocketBehandling(false, true, true, LocalDate.now(), true);
        var resultat = testObjekt.erForsendelsesgrunnlagKomplett(lagRef(behandling));
        assertThat(resultat).isTrue();
    }

    @Test
    void ikke_elektronisk_reg_søknad_skal_behandles_som_komplett_ved_fødsel_og_mangler_ikke_vedlegg() {
        var behandling = lagMocketBehandling(false, true, false, LocalDate.now(), true);
        var resultat = testObjekt.erForsendelsesgrunnlagKomplett(lagRef(behandling));
        assertThat(resultat).isTrue();
    }

    @Test
    void elektronisk_reg_søknad_skal_behandles_som_ikke_komplett_ved_adopsjon_og_manglende_vedlegg() {
        var behandling = lagMocketBehandling(true, false, true, LocalDate.now(), false);
        var resultat = testObjekt.erForsendelsesgrunnlagKomplett(lagRef(behandling));
        assertThat(resultat).isFalse();
    }

    @Test
    void elektronisk_reg_søknad_skal_behandles_som_komplett_ved_fødsel_og_manglende_vedlegg_hvis_bekrefet_i_TPS() {
        var behandling = lagMocketBehandling(true, true, true, LocalDate.now(), true);
        var resultat = testObjekt.erForsendelsesgrunnlagKomplett(lagRef(behandling));
        assertThat(resultat).isTrue();
    }

    @Test
    void elektronisk_reg_søknad_skal_behandles_som_komplett_ved_fødsel_og_barn_finnes_i_pdl_og_mangler_ikke_vedlegg() {
        var behandling = lagMocketBehandling(true, true, false, LocalDate.now(), true);
        var resultat = testObjekt.erForsendelsesgrunnlagKomplett(lagRef(behandling));
        assertThat(resultat).isTrue();
    }

    @Test
    void elektronisk_reg_søknad_skal_behandles_som_være_komplett_ved_fødsel_og_barn_finnes_ikke_i_pdl_og_mangler_ikke_vedlegg() {
        var behandling = lagMocketBehandling(true, true, false, LocalDate.now(), false);
        var resultat = testObjekt.erForsendelsesgrunnlagKomplett(lagRef(behandling));
        assertThat(resultat).isTrue();
    }

    private Behandling lagMocketBehandling(boolean elektroniskRegistrert, boolean gjelderFødsel, boolean manglerVedlegg,
                                           LocalDate fødselsdatoBarn, boolean bekreftetViaPDL) {

        var scenario = lagScenario(gjelderFødsel, elektroniskRegistrert);
        leggVedRegisteropplysninger(fødselsdatoBarn, bekreftetViaPDL, scenario);

        return lagBehandling(manglerVedlegg, scenario);
    }

    private void leggVedRegisteropplysninger(LocalDate fødselsdatoBarn, boolean bekreftetViaPDL, AbstractTestScenario<?> scenario) {
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();

        var barnAktørId = AktørId.dummy();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();

        var fødtBarn = builderForRegisteropplysninger
            .medPersonas()
            .fødtBarn(barnAktørId, fødselsdatoBarn)
            .relasjonTil(søkerAktørId, RelasjonsRolleType.MORA, false)
            .build();

        if (bekreftetViaPDL) {
            scenario.medRegisterOpplysninger(fødtBarn);
        }

        var søker = builderForRegisteropplysninger
            .medPersonas()
            .kvinne(søkerAktørId, SivilstandType.GIFT)
            .statsborgerskap(Landkoder.NOR)
            .relasjonTil(barnAktørId, RelasjonsRolleType.BARN, false)
            .build();
        scenario.medRegisterOpplysninger(søker);
    }

    private Behandling lagBehandling(boolean manglerVedlegg, AbstractTestScenario<?> scenario) {
        var behandling = scenario.lagMocked();

        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        var personopplysningTjeneste = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());

        testObjekt = spy(new KompletthetsjekkerImpl(repositoryProvider, null, personopplysningTjeneste));

        if (!manglerVedlegg) {
            Mockito.doReturn(emptyList())
                .when(testObjekt).utledAlleManglendeVedleggForForsendelse(any());
        } else {
            Mockito.doReturn(Collections.singletonList(new ManglendeVedlegg(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL)))
                .when(testObjekt).utledAlleManglendeVedleggForForsendelse(any());
        }
        return behandling;
    }

    private AbstractTestScenario<?> lagScenario(boolean gjelderFødsel, boolean elektroniskRegistrert) {
        var fødselsdato = LocalDate.now();

        AbstractTestScenario<?> scenario;
        if (!gjelderFødsel) {
            scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
            scenario.medSøknadHendelse()
                .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
                    .medOmsorgsovertakelseDato(LocalDate.now()));
        } else {
            scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
            scenario.medSøknadHendelse().medFødselsDato(fødselsdato);
        }

        scenario.medSøknad()
            .medElektroniskRegistrert(elektroniskRegistrert);
        return scenario;
    }
}
