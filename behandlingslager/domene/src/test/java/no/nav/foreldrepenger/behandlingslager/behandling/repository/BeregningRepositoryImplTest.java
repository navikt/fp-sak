package no.nav.foreldrepenger.behandlingslager.behandling.repository;


import java.time.LocalDate;

import javax.persistence.EntityManager;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.exception.TekniskException;

public class BeregningRepositoryImplTest {

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private LegacyESBeregningRepository repository = new LegacyESBeregningRepository(repositoryRule.getEntityManager());

    @Test(expected = TekniskException.class)
    public void skal_kaste_feil_dersom_eksakt_sats_ikke_kan_identifiseres() {
        BeregningSats sats = new BeregningSats(BeregningSatsType.ENGANG, DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(1)), 123L);
        BeregningSats satsSomOverlapperEksisterendeSats = new BeregningSats(BeregningSatsType.ENGANG, DatoIntervallEntitet.fraOgMed(LocalDate.now().minusDays(1)), 123L);
        EntityManager entityManager = repositoryRule.getEntityManager();
        entityManager.persist(sats);
        entityManager.persist(satsSomOverlapperEksisterendeSats);
        entityManager.flush();

        repository.finnEksaktSats(BeregningSatsType.ENGANG, LocalDate.now());
    }
}
