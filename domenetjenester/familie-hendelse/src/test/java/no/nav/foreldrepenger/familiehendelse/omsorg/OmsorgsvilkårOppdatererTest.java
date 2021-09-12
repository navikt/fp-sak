package no.nav.foreldrepenger.familiehendelse.omsorg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.OmsorgsvilkårAksjonspunktOppdaterer;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.OmsorgsvilkårAksjonspunktDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

public class OmsorgsvilkårOppdatererTest {
    @Mock
    private HistorikkInnslagTekstBuilder tekstBuilder;
    private Behandling behandling;
    private OmsorgsvilkårAksjonspunktOppdaterer.OmsorgsvilkårOppdaterer omsorgsvilkarOppdaterer;

    @BeforeEach
    public void setup() {
        // Behandling
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET,
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagMocked();

        behandling = scenario.getBehandling();

        tekstBuilder = new HistorikkInnslagTekstBuilder();
        omsorgsvilkarOppdaterer = new OmsorgsvilkårAksjonspunktOppdaterer.OmsorgsvilkårOppdaterer(mockHistorikkAdapter());
    }

    private HistorikkTjenesteAdapter mockHistorikkAdapter() {
        var mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }

    @Test
    public void skal_generere_historikkinnslag_ved_avklaring_av_omsorgsvilkår() {
        // Act
        var oppdatertOmsorgsvilkårOk = true;
        var dto = new OmsorgsvilkårAksjonspunktDto("begrunnelse", oppdatertOmsorgsvilkårOk, "avslagkode");
        var aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(dto.getAksjonspunktDefinisjon());
        omsorgsvilkarOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, null, dto));
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        var historikkInnslag = tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(historikkInnslag).hasSize(1);

        var del = historikkInnslag.get(0);
        var feltList = del.getEndredeFelt();
        assertThat(feltList).hasSize(1);
        assertThat(feltList.get(0)).satisfies(felt -> {
            assertThat(felt.getNavn()).isEqualTo(HistorikkEndretFeltType.OMSORGSVILKAR.getKode());
            assertThat(felt.getFraVerdi()).isNull();
            assertThat(felt.getTilVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.OPPFYLT.getKode());
        });
    }

}
