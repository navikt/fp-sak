package no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class TilbakekrevingRepository {

    private EntityManager entityManager;

    TilbakekrevingRepository() {
        // For CDI proxy
    }

    @Inject
    public TilbakekrevingRepository( EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<TilbakekrevingValg> hent(Long behandlingId) {
        return hentEntitet(behandlingId).stream().map(this::map).findFirst();
    }

    private Optional<TilbakekrevingValgEntitet> hentEntitet(long behandlingId) {
        var resultList = entityManager
            .createQuery("from TilbakekrevingValgEntitet where behandlingId=:behandlingId and aktiv=:aktiv", TilbakekrevingValgEntitet.class)
            .setParameter("behandlingId", behandlingId)
            .setParameter("aktiv", true)
            .getResultList();

        if (resultList.size() > 1) {
            throw new IllegalStateException(
                "Skal bare kunne finne en aktiv " + TilbakekrevingValg.class.getSimpleName() + " , men fikk flere for behandling " + behandlingId);
        }
        return resultList.isEmpty()
            ? Optional.empty()
            : Optional.of(resultList.get(0));
    }

    private TilbakekrevingValg map(TilbakekrevingValgEntitet entitet) {
        return new TilbakekrevingValg(entitet.erVilkarOppfylt(), entitet.erGrunnTilReduksjon(), entitet.getTilbakekrevningsVidereBehandling(),
            entitet.getVarseltekst());
    }

    public void lagre(Behandling behandling, TilbakekrevingValg valg) {
        deaktiverEksisterendeTilbakekrevingValg(behandling);

        var nyEntitet = TilbakekrevingValgEntitet.builder()
            .medBehandling(behandling)
            .medVilkarOppfylt(valg.getErTilbakekrevingVilkårOppfylt())
            .medGrunnTilReduksjon(valg.getGrunnerTilReduksjon())
            .medTilbakekrevningsVidereBehandling(valg.getVidereBehandling())
            .medVarseltekst(valg.getVarseltekst())
            .build();

        entityManager.persist(nyEntitet);
        entityManager.flush();
    }

    public void deaktiverEksisterendeTilbakekrevingValg(Behandling behandling) {
        var eksisterende = hentEntitet(behandling.getId());
        if (eksisterende.isPresent()) {
            var eksisterendeEntitet = eksisterende.get();
            eksisterendeEntitet.deaktiver();
            entityManager.persist(eksisterendeEntitet);
        }
    }

    public void lagre(Behandling behandling, boolean avslåttInntrekk) {
        Objects.requireNonNull(behandling, "behandling");
        deaktiverEksisterendeTilbakekrevingInntrekk(behandling);

        var inntrekkEntitet = new TilbakekrevingInntrekkEntitet.Builder()
            .medBehandling(behandling)
            .medAvslåttInntrekk(avslåttInntrekk)
            .build();

        entityManager.persist(inntrekkEntitet);
        entityManager.flush();
    }

    public Optional<TilbakekrevingInntrekkEntitet> hentTilbakekrevingInntrekk(Long behandlingId) {
        var query = entityManager
            .createQuery("from TilbakekrevingInntrekkEntitet ti where ti.behandlingId =:behandlingId and aktiv =: aktiv", TilbakekrevingInntrekkEntitet.class)
            .setParameter("behandlingId", behandlingId)
            .setParameter("aktiv", true);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void deaktiverEksisterendeTilbakekrevingInntrekk(Behandling behandling) {
        var tilbakekrevingInntrekkEntitet = hentTilbakekrevingInntrekk(behandling.getId());
        if (tilbakekrevingInntrekkEntitet.isPresent()) {
            var eksisterendeInntrekk = tilbakekrevingInntrekkEntitet.get();
            eksisterendeInntrekk.deaktiver();
            entityManager.persist(eksisterendeInntrekk);
        }
    }
}
