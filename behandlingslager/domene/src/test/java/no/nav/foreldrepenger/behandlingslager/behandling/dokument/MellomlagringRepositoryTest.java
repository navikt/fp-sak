package no.nav.foreldrepenger.behandlingslager.behandling.dokument;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

class MellomlagringRepositoryTest extends EntityManagerAwareTest {

    private MellomlagringRepository mellomlagringRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        mellomlagringRepository = new MellomlagringRepository(entityManager);
    }

    @Test
    void mellomlagring_lagre_og_hent() {
        var behandling = opprettBehandling();

        var mellomlagring = MellomlagringEntitet.Builder.ny()
            .medBehandlingId(behandling.getId())
            .medType(MellomlagringType.VARSEL_REVURDERING)
            .medInnhold("<p>Hei</p>")
            .build();
        mellomlagringRepository.lagreOgFlush(mellomlagring);

        var funnet = mellomlagringRepository.hentMellomlagring(behandling.getId(), MellomlagringType.VARSEL_REVURDERING);
        assertThat(funnet).isPresent();
        assertThat(funnet.get().getInnhold()).isEqualTo("<p>Hei</p>");
    }

    @Test
    void mellomlagring_hent_returnerer_empty_for_ukjent_type() {
        var behandling = opprettBehandling();

        var funnet = mellomlagringRepository.hentMellomlagring(behandling.getId(), MellomlagringType.VARSEL_REVURDERING);
        assertThat(funnet).isEmpty();
    }

    @Test
    void mellomlagring_fjern_én_type() {
        var behandling = opprettBehandling();

        mellomlagringRepository.lagreOgFlush(MellomlagringEntitet.Builder.ny()
            .medBehandlingId(behandling.getId())
            .medType(MellomlagringType.VARSEL_REVURDERING)
            .medInnhold("<p>Varsel</p>")
            .build());
        mellomlagringRepository.lagreOgFlush(MellomlagringEntitet.Builder.ny()
            .medBehandlingId(behandling.getId())
            .medType(MellomlagringType.INNHENT_OPPLYSNINGER)
            .medInnhold("<p>Innhent</p>")
            .build());

        mellomlagringRepository.fjernMellomlagring(behandling.getId(), MellomlagringType.VARSEL_REVURDERING);

        assertThat(mellomlagringRepository.hentMellomlagring(behandling.getId(), MellomlagringType.VARSEL_REVURDERING)).isEmpty();
        assertThat(mellomlagringRepository.hentMellomlagring(behandling.getId(), MellomlagringType.INNHENT_OPPLYSNINGER)).isPresent();
    }

    @Test
    void mellomlagring_fjern_alle() {
        var behandling = opprettBehandling();

        mellomlagringRepository.lagreOgFlush(MellomlagringEntitet.Builder.ny()
            .medBehandlingId(behandling.getId())
            .medType(MellomlagringType.VARSEL_REVURDERING)
            .medInnhold("<p>Varsel</p>")
            .build());
        mellomlagringRepository.lagreOgFlush(MellomlagringEntitet.Builder.ny()
            .medBehandlingId(behandling.getId())
            .medType(MellomlagringType.INNHENT_OPPLYSNINGER)
            .medInnhold("<p>Innhent</p>")
            .build());

        mellomlagringRepository.fjernAlleMellomlagringer(behandling.getId());

        assertThat(mellomlagringRepository.hentMellomlagring(behandling.getId(), MellomlagringType.VARSEL_REVURDERING)).isEmpty();
        assertThat(mellomlagringRepository.hentMellomlagring(behandling.getId(), MellomlagringType.INNHENT_OPPLYSNINGER)).isEmpty();
    }

    @Test
    void mellomlagring_oppdaterer_eksisterende_ved_lagre() {
        var behandling = opprettBehandling();

        var mellomlagring = MellomlagringEntitet.Builder.ny()
            .medBehandlingId(behandling.getId())
            .medType(MellomlagringType.VARSEL_REVURDERING)
            .medInnhold("<p>Opprinnelig</p>")
            .build();
        mellomlagringRepository.lagreOgFlush(mellomlagring);

        mellomlagring.setInnhold("<p>Oppdatert</p>");
        mellomlagringRepository.lagreOgFlush(mellomlagring);

        var funnet = mellomlagringRepository.hentMellomlagring(behandling.getId(), MellomlagringType.VARSEL_REVURDERING);
        assertThat(funnet).isPresent();
        assertThat(funnet.get().getInnhold()).isEqualTo("<p>Oppdatert</p>");
    }

    private Behandling opprettBehandling() {
        var behandlingBuilder = new BasicBehandlingBuilder(getEntityManager());
        var behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        var resultat = Behandlingsresultat.builder().build();
        behandlingBuilder.lagreBehandlingsresultat(behandling.getId(), resultat);
        return behandling;
    }
}
