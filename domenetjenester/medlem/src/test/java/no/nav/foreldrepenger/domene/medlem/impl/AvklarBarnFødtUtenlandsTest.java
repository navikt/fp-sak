package no.nav.foreldrepenger.domene.medlem.impl;


import static no.nav.foreldrepenger.domene.medlem.impl.MedlemResultat.AVKLAR_OM_ER_BOSATT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

class AvklarBarnFødtUtenlandsTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider provider;

    private AvklarBarnFødtUtenlands tjeneste;

    @BeforeEach
    public void setUp() {
        provider = new BehandlingRepositoryProvider(getEntityManager());
        tjeneste = new AvklarBarnFødtUtenlands(provider);
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_om_søker_har_oppholdt_seg_i_Norge_de_siste_12_måneder() {
        //Arrange
        var fødselsdato = LocalDate.now().minusDays(5L);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato);

        var behandling = scenario.lagre(provider);

        // Act
        var medlemResultat = tjeneste.utled(behandling.getId(), fødselsdato);

        //Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_om_det_ikke_er_søkt_på_bakgrunn_av_fødsel() {
        //Arrange
        var termindato = LocalDate.now().minusDays(5L); // Oppgir termindato, dvs. søknad ikke basert på fødsel
        var scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel();
        scenario.medDefaultOppgittTilknytning();
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medUtstedtDato(LocalDate.now())
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN"));

        var oppholdStart = termindato.minusDays(2L);
        var oppholdSlutt = termindato.plusDays(2L);

        var danmark = lagUtlandsopphold(oppholdStart, oppholdSlutt);
        scenario.medOppgittTilknytning().leggTilOpphold(danmark);

        var behandling = scenario.lagre(provider);

        // Act
        var medlemResultat = tjeneste.utled(behandling.getId(), termindato);

        //Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_om_søkers_barn_er_født_i_Norge() {
        //Arrange
        var fødselsdato = LocalDate.now().minusDays(2L);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultOppgittTilknytning();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato);

        var oppholdStart = fødselsdato.minusDays(20L);
        var oppholdSlutt = fødselsdato.minusDays(5L);

        var danmark = lagUtlandsopphold(oppholdStart, oppholdSlutt);
        scenario.medOppgittTilknytning().leggTilOpphold(danmark);

        var behandling = scenario.lagre(provider);

        // Act
        var medlemResultat = tjeneste.utled(behandling.getId(), fødselsdato);

        //Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    void skal_opprette_aksjonspunkt_om_søkers_barn_fra_søknad_er_født_i_utlandet() {
        //Arrange
        var fødselsdato = LocalDate.now().minusDays(5L);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultOppgittTilknytning();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato);

        var oppholdStart = fødselsdato.minusDays(2L);
        var oppholdSlutt = fødselsdato.plusDays(2L);

        var danmark = lagUtlandsopphold(oppholdStart, oppholdSlutt);
        scenario.medOppgittTilknytning().leggTilOpphold(danmark);

        var behandling = scenario.lagre(provider);

        // Act
        var medlemResultat = tjeneste.utled(behandling.getId(), fødselsdato);

        //Assert
        assertThat(medlemResultat).contains(AVKLAR_OM_ER_BOSATT);
    }

    @Test
    void skal_opprette_aksjonspunkt_om_søkers_barn_fra_tps_er_født_i_utlandet() {
        //Arrange
        var fødselsdato = LocalDate.now().minusDays(5L);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultOppgittTilknytning();
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato);

        var oppholdStart = fødselsdato.minusDays(2L);
        var oppholdSlutt = fødselsdato.plusDays(2L);

        var danmark = lagUtlandsopphold(oppholdStart, oppholdSlutt);
        scenario.medOppgittTilknytning().leggTilOpphold(danmark);

        var behandling = scenario.lagre(provider);

        // Act
        var medlemResultat = tjeneste.utled(behandling.getId(), fødselsdato);

        //Assert
        assertThat(medlemResultat).contains(AVKLAR_OM_ER_BOSATT);
    }

    private MedlemskapOppgittLandOppholdEntitet lagUtlandsopphold(LocalDate oppholdStart, LocalDate oppholdSlutt) {
        return new MedlemskapOppgittLandOppholdEntitet.Builder()
            .erTidligereOpphold(false)
            .medLand(Landkoder.SWE)
            .medPeriode(oppholdStart, oppholdSlutt)
            .build();
    }
}
