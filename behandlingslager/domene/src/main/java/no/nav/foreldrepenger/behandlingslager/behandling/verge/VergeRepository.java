package no.nav.foreldrepenger.behandlingslager.behandling.verge;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class VergeRepository {
    private EntityManager entityManager;
    private BehandlingLåsRepository behandlingLåsRepository;

    VergeRepository() {
        // for CDI proxy
    }

    @Inject
    public VergeRepository( EntityManager entityManager, BehandlingLåsRepository behandlingLåsRepository) {
        this.entityManager = entityManager;
        this.behandlingLåsRepository = behandlingLåsRepository;
    }

    public Optional<VergeAggregat> hentAggregat(Long behandlingId) {
        return hentVerge(getAktivtBehandlingsgrunnlag(behandlingId));
    }

    public void lagreOgFlush(Long behandlingId, VergeBuilder vergeBuilder) {
        Objects.requireNonNull(behandlingId, "behandlingId"); //NOSONAR //$NON-NLS-1$
        var verge = vergeBuilder.build();
        var grunnlag = new VergeGrunnlagEntitet(behandlingId, verge);
        lagreOgFlush(behandlingId, grunnlag);
    }

    public void fjernVergeFraEksisterendeGrunnlag(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); //NOSONAR //$NON-NLS-1$
        var vergeAggregat = hentAggregat(behandlingId);
        if (vergeAggregat.isPresent()) {
            var lås = behandlingLåsRepository.taLås(behandlingId);
            settAktivFalseOgPersisterTidligereGrunnlag(behandlingId);
            lagreGrunnlag(behandlingId, new VergeGrunnlagEntitet(behandlingId, null));
            verifiserBehandlingLås(lås);
            entityManager.flush();
        }
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        var vergeAggregat = hentAggregat(gammelBehandlingId);
        if (vergeAggregat.isPresent()) {
            var vergeGrunnlagEntitet = new VergeGrunnlagEntitet(nyBehandlingId, vergeAggregat.get().getVerge().orElse(null));
            lagreOgFlush(nyBehandlingId, vergeGrunnlagEntitet);
        }
    }

    private void lagreOgFlush(Long behandlingId, VergeGrunnlagEntitet nyttGrunnlag) {
        Objects.requireNonNull(behandlingId, "behandlingId"); //NOSONAR //$NON-NLS-1$
        if (nyttGrunnlag == null) {
            return;
        }
        var lås = behandlingLåsRepository.taLås(behandlingId);

        settAktivFalseOgPersisterTidligereGrunnlag(behandlingId);
        lagreVerge(nyttGrunnlag.getVerge());
        lagreGrunnlag(behandlingId, nyttGrunnlag);

        verifiserBehandlingLås(lås);
        entityManager.flush();
    }

    private void settAktivFalseOgPersisterTidligereGrunnlag(Long behandlingId) {
        var tidligereGrunnlag = getAktivtBehandlingsgrunnlag(behandlingId);
        if (tidligereGrunnlag.isPresent()) {
            var grunnlag = tidligereGrunnlag.get();
            grunnlag.setAktiv(false);
            entityManager.persist(grunnlag);
            entityManager.flush();
        }
    }

    private void lagreVerge(VergeEntitet verge) {
        if (verge.getVergeOrganisasjon().isPresent()) {
            entityManager.persist(verge.getVergeOrganisasjon().get());
        }
        entityManager.persist(verge);
    }

    private void lagreGrunnlag(Long behandlingId, VergeGrunnlagEntitet nyttGrunnlag) {
        nyttGrunnlag.setBehandling(behandlingId);
        entityManager.persist(nyttGrunnlag);
    }

    private void verifiserBehandlingLås(BehandlingLås lås) {
        behandlingLåsRepository.oppdaterLåsVersjon(lås);
    }

    private Optional<VergeAggregat> hentVerge(Optional<VergeGrunnlagEntitet> optGrunnlag) {
        if (optGrunnlag.isPresent()) {
            var grunnlag = optGrunnlag.get();
            var vergeAggregat = grunnlag.tilAggregat();
            return Optional.of(vergeAggregat);
        }
        return Optional.empty();
    }

    private Optional<VergeGrunnlagEntitet> getAktivtBehandlingsgrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT vg FROM VergeGrunnlag vg WHERE vg.behandlingId = :behandling_id AND vg.aktiv = 'J'", //$NON-NLS-1$
            VergeGrunnlagEntitet.class);

        query.setParameter("behandling_id", behandlingId); //$NON-NLS-1$
        return HibernateVerktøy.hentUniktResultat(query);
    }
}
