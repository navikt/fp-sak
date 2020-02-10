package no.nav.foreldrepenger.web.app.tjenester.kodeverk.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.kodeverk.KodeverkRepository;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

public class HentKodeverkTjenesteImplTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private KodeverkRepository repo = new KodeverkRepository(repoRule.getEntityManager());
    private no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste enhetsTjeneste = new BehandlendeEnhetTjeneste();

    @Test
    public void skal_filtere_arbeidtyper() {
        var kodeverk = new HentKodeverkTjeneste(repo, enhetsTjeneste);

        var resultat = kodeverk.hentGruppertKodeliste();
        var arbeidType = resultat.get("ArbeidType");

        assertThat(arbeidType).hasSize(6);
    }
}
