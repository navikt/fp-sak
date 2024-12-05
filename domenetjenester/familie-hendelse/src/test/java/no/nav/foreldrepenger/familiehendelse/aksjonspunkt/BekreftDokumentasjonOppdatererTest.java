package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftDokumentertDatoAksjonspunktDto;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;

class BekreftDokumentasjonOppdatererTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
    }

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_dokumentert_adopsjonsdato() {
        // Arrange
        var opprinneligOvertakelsesdato = LocalDate.now();
        var bekreftetOvertakelsesdato = opprinneligOvertakelsesdato.plusDays(1);
        var opprinneligFødselsdato = LocalDate.now().plusDays(30);
        var bekreftetFødselsdato = opprinneligFødselsdato.plusDays(1);

        // Behandling
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.ADOPTERER_ALENE);
        scenario.medSøknadHendelse()
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(opprinneligOvertakelsesdato))
            .leggTilBarn(new UidentifisertBarnEntitet(opprinneligFødselsdato));
        scenario.medBekreftetHendelse()
            .medAdopsjon(scenario.medBekreftetHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(opprinneligOvertakelsesdato))
            .leggTilBarn(opprinneligFødselsdato);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_ADOPSJONSDOKUMENTAJON, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();

        // Dto
        Map<Integer, LocalDate> bekreftedeFødselsdatoer = new HashMap<>();
        bekreftedeFødselsdatoer.put(1, bekreftetFødselsdato);
        var dto = new BekreftDokumentertDatoAksjonspunktDto("begrunnelse", bekreftetOvertakelsesdato, bekreftedeFødselsdatoer);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        // Act
        new BekreftDokumentasjonOppdaterer(familieHendelseTjeneste, mock(OpplysningsPeriodeTjeneste.class),
            repositoryProvider.getHistorikkinnslag2Repository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        var historikkInnslag = repositoryProvider.getHistorikkinnslag2Repository().hent(behandling.getId());

        // Assert
        assertThat(historikkInnslag).hasSize(1);
        var linjer = historikkInnslag.getFirst().getLinjer();
        assertThat(linjer).hasSize(3);

        assertThat(linjer.getFirst().getTekst()).contains("Omsorgsovertakelsesdato", format(opprinneligOvertakelsesdato),
            format(bekreftetOvertakelsesdato));
        assertThat(linjer.get(1).getTekst()).contains("Fødselsdato", format(opprinneligFødselsdato), format(bekreftetFødselsdato));
        assertThat(linjer.get(2).getTekst()).contains(dto.getBegrunnelse());
    }
}
