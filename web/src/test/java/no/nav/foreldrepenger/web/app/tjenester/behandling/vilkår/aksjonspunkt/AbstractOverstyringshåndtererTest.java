package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto.OverstyringFødselsvilkåretDto;

@CdiDbAwareTest
public class AbstractOverstyringshåndtererTest {

    private static final String IKKE_OK = "ikke født likevel";

    @Inject
    private AksjonspunktApplikasjonTjeneste aksjonspunktApplikasjonTjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Test
    public void skal_reaktivere_inaktivt_aksjonspunkt() {
        Behandling behandling = ScenarioMorSøkerEngangsstønad.forFødsel().lagre(repositoryProvider);
        Aksjonspunkt ap = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.OVERSTYRING_AV_FØDSELSVILKÅRET);
        AksjonspunktTestSupport.setTilUtført(ap, "OK");

        OverstyringAksjonspunktDto dto = new OverstyringFødselsvilkåretDto(false, IKKE_OK, Avslagsårsak.MANGLENDE_DOKUMENTASJON.getKode());

        aksjonspunktApplikasjonTjeneste.overstyrAksjonspunkter(Set.of(dto), behandling.getId());

        assertThat(behandling.getAksjonspunktFor(AksjonspunktDefinisjon.OVERSTYRING_AV_FØDSELSVILKÅRET).getBegrunnelse()).isEqualTo(IKKE_OK);
    }

}
