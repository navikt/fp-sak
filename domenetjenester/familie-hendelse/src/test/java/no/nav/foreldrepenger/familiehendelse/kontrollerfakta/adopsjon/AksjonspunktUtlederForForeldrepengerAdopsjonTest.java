package no.nav.foreldrepenger.familiehendelse.kontrollerfakta.adopsjon;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_ADOPSJONSDOKUMENTAJON;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_OM_ADOPSJON_GJELDER_EKTEFELLES_BARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

@ExtendWith(MockitoExtension.class)
class AksjonspunktUtlederForForeldrepengerAdopsjonTest {

    @Mock
    private FamilieHendelseRepository familieHendelseRepositoryMock;

    private AksjonspunktUtlederForForeldrepengerAdopsjon utleder;

    @BeforeEach
    public void setUp() {
        var morSøkerAdopsjonScenario = ScenarioMorSøkerForeldrepenger.forAdopsjon();

        var familieHendelseTjeneste = new FamilieHendelseTjeneste(null,
            familieHendelseRepositoryMock);
        utleder = new AksjonspunktUtlederForForeldrepengerAdopsjon(familieHendelseTjeneste);

        var familieHendelseBuilder = morSøkerAdopsjonScenario.medSøknadHendelse();

        morSøkerAdopsjonScenario.medSøknadHendelse().medAdopsjon(morSøkerAdopsjonScenario.medSøknadHendelse().getAdopsjonBuilder());
        var familieHendelseAggregat = FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty())
            .medSøknadVersjon(familieHendelseBuilder).build();
        when(familieHendelseRepositoryMock.hentAggregat(any())).thenReturn(familieHendelseAggregat);
    }

    @Test
    void skal_utlede_aksjonspunkt_basert_på_fakta_om_fp_til_mor() {
        var fagsakMock = mock(Fagsak.class);
        var behandlingMock = mock(Behandling.class);
        when(behandlingMock.getFagsak()).thenReturn(fagsakMock);

        var aksjonspunkter = aksjonspunktForFakta(behandlingMock);

        assertThat(aksjonspunkter).hasSize(2);
        assertThat(aksjonspunkter.stream().map(AksjonspunktResultat::getAksjonspunktDefinisjon).toList())
            .containsExactlyInAnyOrder(AVKLAR_OM_ADOPSJON_GJELDER_EKTEFELLES_BARN, AVKLAR_ADOPSJONSDOKUMENTAJON);
    }

    @Test
    void skal_utlede_aksjonspunkt_basert_på_fakta_om_fp_til_far() {
        var fagsakMock = mock(Fagsak.class);
        var behandlingMock = mock(Behandling.class);
        when(behandlingMock.getFagsak()).thenReturn(fagsakMock);
        var aksjonspunkter = aksjonspunktForFakta(behandlingMock);

        assertThat(aksjonspunkter).hasSize(2);
        assertThat(aksjonspunkter.stream().map(AksjonspunktResultat::getAksjonspunktDefinisjon).toList())
            .containsExactlyInAnyOrder(AVKLAR_OM_ADOPSJON_GJELDER_EKTEFELLES_BARN, AVKLAR_ADOPSJONSDOKUMENTAJON);
    }

    @Test
    void skal_utlede_aksjonspunkt_basert_på_fakta_om_fp_til_medmor() {
        var fagsakMock = mock(Fagsak.class);
        var behandlingMock = mock(Behandling.class);
        when(behandlingMock.getFagsak()).thenReturn(fagsakMock);
        var aksjonspunkter = aksjonspunktForFakta(behandlingMock);

        assertThat(aksjonspunkter).hasSize(2);
        assertThat(aksjonspunkter.stream().map(AksjonspunktResultat::getAksjonspunktDefinisjon).toList())
            .containsExactlyInAnyOrder(AVKLAR_OM_ADOPSJON_GJELDER_EKTEFELLES_BARN, AVKLAR_ADOPSJONSDOKUMENTAJON);
    }

    private AksjonspunktUtlederInput lagInput(Behandling behandling) {
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling), null);
    }

    private List<AksjonspunktResultat> aksjonspunktForFakta(Behandling behandling) {
        return utleder.utledAksjonspunkterFor(lagInput(behandling));
    }

}
