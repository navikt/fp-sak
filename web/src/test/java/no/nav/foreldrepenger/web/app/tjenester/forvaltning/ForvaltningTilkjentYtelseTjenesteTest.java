package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.mottak.hendelser.JsonMapper;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.MapBeregningsresultatTilEndringsmodell;

@CdiDbAwareTest
public class ForvaltningTilkjentYtelseTjenesteTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private BeregningsresultatRepository beregningsresultatRepository;
    private ForvaltningTilkjentYtelseTjeneste forvaltningTilkjentYtelseTjeneste;


    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
        forvaltningTilkjentYtelseTjeneste = new ForvaltningTilkjentYtelseTjeneste(beregningsresultatRepository);

    }

    @Test
    public void test() {
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        BeregningsresultatEntitet beregningsresultatEntitet = opprettBeregningsresultat();
        beregningsresultatRepository.lagre(behandling, beregningsresultatEntitet);

        Response response = forvaltningTilkjentYtelseTjeneste.hentTilkjentYtelse(new ForvaltningBehandlingIdDto(behandling.getId().toString()));

    }



    private BeregningsresultatEntitet opprettBeregningsresultat() {
        return BeregningsresultatEntitet.builder()
            .medRegelInput("regelInput")
            .medRegelSporing("regelSporing")
            .build();
    }



}
