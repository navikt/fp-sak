package no.nav.foreldrepenger.domene.uttak.fakta.omsorg;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.domene.uttak.PersonopplysningerForUttak;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

class BrukerHarAleneomsorgAksjonspunktUtlederTest {

    private UttakRepositoryProvider repositoryProvider;

    private BrukerHarAleneomsorgAksjonspunktUtleder aksjonspunktUtleder;
    private PersonopplysningerForUttak personopplysninger;

    @BeforeEach
    void setUp() {
        repositoryProvider = new UttakRepositoryStubProvider();
        personopplysninger = mock(PersonopplysningerForUttak.class);
        aksjonspunktUtleder = new BrukerHarAleneomsorgAksjonspunktUtleder(repositoryProvider, personopplysninger);
    }

    @Test
    void ingen_aksjonspunkter_dersom_søker_oppgitt_ikke_ha_aleneomsorg() {
        var behandling = behandlingMedOppgittRettighet(true, false);
        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    void aksjonspunkter_dersom_søker_oppgitt_ha_aleneomsorg_men_oppgitt_annenForeldre_og_ha_samme_address_som_bruker() {
        var behandling = behandlingMedOppgittRettighet(false, true);
        var ref = BehandlingReferanse.fra(behandling);
        when(personopplysninger.annenpartHarSammeBosted(ref)).thenReturn(true);
        when(personopplysninger.harOppgittAnnenpartMedNorskID(ref)).thenReturn(true);

        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunktResultater).containsExactly(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG);
    }

    private Behandling behandlingMedOppgittRettighet(boolean annenpartRett, boolean aleneomsorg) {
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now()).build();
        return ScenarioMorSøkerForeldrepenger.forFødsel()
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medOppgittRettighet(oppgittRettighet(annenpartRett, aleneomsorg))
            .lagre(repositoryProvider);
    }

    @Test
    void aksjonspunkter_dersom_bruker_ikke_oppgitt_annenForeldre_men_er_gift_og_ha_samme_address_som_bruker() {
        var behandling = behandlingMedOppgittRettighet(false, true);
        when(personopplysninger.ektefelleHarSammeBosted(BehandlingReferanse.fra(behandling))).thenReturn(true);
        when(personopplysninger.harOppgittAnnenpartMedNorskID(BehandlingReferanse.fra(behandling))).thenReturn(false);

        var aksjonspunktResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunktResultater).containsExactly(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG);
    }

    private UttakInput lagInput(Behandling behandling) {
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        return new UttakInput(BehandlingReferanse.fra(behandling, skjæringstidspunkt), null, null);
    }

    private OppgittRettighetEntitet oppgittRettighet(Boolean harAnnenForeldreRett, Boolean harAleneomsorgForBarnet) {
        return new OppgittRettighetEntitet(harAnnenForeldreRett, harAleneomsorgForBarnet, false, false, false);
    }

}
