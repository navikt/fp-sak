package no.nav.foreldrepenger.behandlingslager.behandling.repository;


import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.exception.TekniskException;

public class LegacyESBeregningRepositoryTest extends EntityManagerAwareTest {

    @Test
    public void skal_kaste_feil_dersom_eksakt_sats_ikke_kan_identifiseres() {
        var entityManager = getEntityManager();
        var repository = new LegacyESBeregningRepository(entityManager);

        var sats = new BeregningSats(BeregningSatsType.ENGANG, DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(1)), 123L);
        var satsSomOverlapperEksisterendeSats = new BeregningSats(BeregningSatsType.ENGANG, DatoIntervallEntitet.fraOgMed(LocalDate.now().minusDays(1)), 123L);
        entityManager.persist(sats);
        entityManager.persist(satsSomOverlapperEksisterendeSats);
        entityManager.flush();

        assertThrows(TekniskException.class, () -> repository.finnEksaktSats(BeregningSatsType.ENGANG, LocalDate.now()));
    }
}
