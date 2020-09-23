package no.nav.foreldrepenger.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class RepositoryAwareTest extends EntityManagerAwareTest {

    protected BehandlingRepositoryProvider repositoryProvider;
    protected SvangerskapspengerRepository svangerskapspengerRepository;
    protected FamilieHendelseRepository familieHendelseRepository;

    @BeforeEach
    public void beforeEach() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        svangerskapspengerRepository = repositoryProvider.getSvangerskapspengerRepository();
        familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

}
