package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto.OverstyringFødselsvilkåretDto;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class AbstractOverstyringshåndtererTest {

    private static final String IKKE_OK = "ikke født likevel";

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private EntityManager em = repoRule.getEntityManager();
    @Inject
    private AksjonspunktApplikasjonTjeneste aksjonspunktApplikasjonTjeneste;


    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(em);

    private AksjonspunktTestSupport aksjonspunktRepository = new AksjonspunktTestSupport();

    @Test
    public void skal_reaktivere_inaktivt_aksjonspunkt() throws Exception {
        Behandling behandling = ScenarioMorSøkerEngangsstønad.forFødsel().lagre(repositoryProvider);
        Aksjonspunkt ap = aksjonspunktRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.OVERSTYRING_AV_FØDSELSVILKÅRET);
        aksjonspunktRepository.setTilUtført(ap, "OK");

        OverstyringAksjonspunktDto dto = new OverstyringFødselsvilkåretDto(false, IKKE_OK, Avslagsårsak.MANGLENDE_DOKUMENTASJON.getKode());

        aksjonspunktApplikasjonTjeneste.overstyrAksjonspunkter(Set.of(dto), behandling.getId());

        assertThat(behandling.getAksjonspunktFor(AksjonspunktDefinisjon.OVERSTYRING_AV_FØDSELSVILKÅRET).getBegrunnelse()).isEqualTo(IKKE_OK);
    }

}
