package no.nav.foreldrepenger.datavarehus.domene;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

@ApplicationScoped
public class DatavarehusRepository {

    private EntityManager entityManager;

    DatavarehusRepository() {
        // for CDI proxy
    }

    @Inject
    public DatavarehusRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    private static Optional<VedtakUtbetalingDvh> optionalFirst(List<VedtakUtbetalingDvh> liste) {
        return liste.isEmpty() ? Optional.empty() : Optional.of(liste.get(0));
    }

    public long lagre(FagsakDvh fagsakDvh) {
        entityManager.persist(fagsakDvh);
        return fagsakDvh.getId();
    }

    public long lagre(BehandlingDvh behandlingDvh) {
        entityManager.persist(behandlingDvh);
        return behandlingDvh.getId();
    }

    public long lagre(BehandlingStegDvh behandlingStegDvh) {
        entityManager.persist(behandlingStegDvh);
        return behandlingStegDvh.getId();
    }

    public long lagre(AksjonspunktDvh aksjonspunktDvh) {
        entityManager.persist(aksjonspunktDvh);
        return aksjonspunktDvh.getId();
    }

    public long lagre(KontrollDvh kontrollDvh) {
        entityManager.persist(kontrollDvh);
        return kontrollDvh.getId();
    }

    public long lagre(BehandlingVedtakDvh behandlingVedtakDvh) {
        entityManager.persist(behandlingVedtakDvh);
        return behandlingVedtakDvh.getId();
    }

    public long lagre(VedtakUtbetalingDvh vedtakUtbetalingDvh) {
        entityManager.persist(vedtakUtbetalingDvh);
        return vedtakUtbetalingDvh.getId();
    }

    public long oppdater(Long eksisterendeBehandlingId, Long eksisterendeVedtakId, String nyVedtakXml) {
        var eksisterende = finn(eksisterendeBehandlingId, eksisterendeVedtakId);
        if (eksisterende.isPresent()) {
            return
                oppdater(eksisterende.get(), nyVedtakXml);
        }
        throw new IllegalStateException(String.format("Finner ikke eksiterende dvh vedtak utbetaling for behandling %s og vedtak %s", eksisterendeBehandlingId, eksisterendeVedtakId));
    }

    public List<Long> hentVedtakBehandlinger(LocalDateTime fom, LocalDateTime tom) {
        var query = entityManager.createQuery("from VedtakUtbetalingDvh where funksjonellTid >= :fom " +
            "AND funksjonellTid <= :tom", VedtakUtbetalingDvh.class);
        query.setParameter("fom", fom);
        query.setParameter("tom", tom);

        return query.getResultList().stream().map(VedtakUtbetalingDvh::getBehandlingId).toList();
    }

    public List<Long> hentVedtakBehandlinger(Long behandlingid) {
        var query = entityManager.createQuery("from VedtakUtbetalingDvh where behandlingId = :bid ", VedtakUtbetalingDvh.class);
        query.setParameter("bid", behandlingid);
        return query.getResultList().stream().map(VedtakUtbetalingDvh::getBehandlingId).toList();
    }

    public long oppdater(VedtakUtbetalingDvh vedtakUtbetalingDvh, String nyVedtakXml) {
        vedtakUtbetalingDvh.setXmlClob(nyVedtakXml);
        vedtakUtbetalingDvh.setTransTid();
        var merge = entityManager.merge(vedtakUtbetalingDvh);
        return merge.getId();
    }

    public Optional<VedtakUtbetalingDvh> finn(Long behandlingId, Long vedtakId) {
        var query = entityManager.createQuery("from VedtakUtbetalingDvh where behandlingId = :behandlingId " +
            "AND vedtakId = :vedtakId order by id", VedtakUtbetalingDvh.class);
        query.setParameter("behandlingId", behandlingId);
        query.setParameter("vedtakId", vedtakId);
        return optionalFirst(query.getResultList());
    }

    public long lagre(KlageFormkravDvh formkrav) {
        entityManager.persist(formkrav);
        return formkrav.getId();
    }

    public long lagre(KlageVurderingResultatDvh klageVurderingResultat) {
        entityManager.persist(klageVurderingResultat);
        return klageVurderingResultat.getId();
    }

    public long lagre(AnkeVurderingResultatDvh ankeVurderingResultat) {
        entityManager.persist(ankeVurderingResultat);
        return ankeVurderingResultat.getId();

    }

    public long lagre( FagsakRelasjonDvh fagsakRelasjonDvh ) {
        entityManager.persist(fagsakRelasjonDvh);
        entityManager.flush();
        return fagsakRelasjonDvh.getId();
    }

    public Map<String, AksjonspunktDefDvh> hentAksjonspunktDefinisjoner() {
        var query = entityManager.createQuery("from AksjonspunktDefDvh", AksjonspunktDefDvh.class);

        return query.getResultList().stream().collect(Collectors.toMap(AksjonspunktDefDvh::getAksjonspunktDef, a -> a));
    }

    public void lagre( AksjonspunktDefDvh aksjonspunktDefDvh ) {
        entityManager.persist(aksjonspunktDefDvh);
        entityManager.flush();
    }

}
