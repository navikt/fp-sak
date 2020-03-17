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
import javax.persistence.TypedQuery;

import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class DatavarehusRepository {

    private EntityManager entityManager;

    DatavarehusRepository() {
        // for CDI proxy
    }

    @Inject
    public DatavarehusRepository(@VLPersistenceUnit EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
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
        Optional<VedtakUtbetalingDvh> eksisterende = finn(eksisterendeBehandlingId, eksisterendeVedtakId);
        if (eksisterende.isPresent()) {
            return
                oppdater(eksisterende.get(), nyVedtakXml);
        }
        throw new IllegalStateException(String.format("Finner ikke eksiterende dvh vedtak utbetaling for behandling %s og vedtak %s", eksisterendeBehandlingId, eksisterendeVedtakId));
    }

    public List<Long> hentVedtakBehandlinger(LocalDateTime fom, LocalDateTime tom) {
        TypedQuery<VedtakUtbetalingDvh> query = entityManager.createQuery("from VedtakUtbetalingDvh where funksjonellTid >= :fom " +
            "AND funksjonellTid <= :tom", VedtakUtbetalingDvh.class);
        query.setParameter("fom", fom); // NOSONAR $NON-NLS-1$
        query.setParameter("tom", tom); // NOSONAR $NON-NLS-1$

        return query.getResultList().stream().map(VedtakUtbetalingDvh::getBehandlingId).collect(Collectors.toList());
    }

    public List<Long> hentVedtakBehandlinger(Long behandlingid) {
        TypedQuery<VedtakUtbetalingDvh> query = entityManager.createQuery("from VedtakUtbetalingDvh where behandlingId = :bid ", VedtakUtbetalingDvh.class);
        query.setParameter("bid", behandlingid); // NOSONAR $NON-NLS-1$
        return query.getResultList().stream().map(VedtakUtbetalingDvh::getBehandlingId).collect(Collectors.toList());
    }

    public long oppdater(VedtakUtbetalingDvh vedtakUtbetalingDvh, String nyVedtakXml) {
        vedtakUtbetalingDvh.setXmlClob(nyVedtakXml);
        vedtakUtbetalingDvh.setTransTid();
        VedtakUtbetalingDvh merge = entityManager.merge(vedtakUtbetalingDvh);
        return merge.getId();
    }

    public Optional<VedtakUtbetalingDvh> finn(Long behandlingId, Long vedtakId) {
        TypedQuery<VedtakUtbetalingDvh> query = entityManager.createQuery("from VedtakUtbetalingDvh where behandlingId = :behandlingId " +
            "AND vedtakId = :vedtakId order by id", VedtakUtbetalingDvh.class);
        query.setParameter("behandlingId", behandlingId); // NOSONAR $NON-NLS-1$
        query.setParameter("vedtakId", vedtakId); // NOSONAR $NON-NLS-1$
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
        TypedQuery<AksjonspunktDefDvh> query = entityManager.createQuery("from AksjonspunktDefDvh", AksjonspunktDefDvh.class);

        return query.getResultList().stream().collect(Collectors.toMap(AksjonspunktDefDvh::getAksjonspunktDef, a -> a));
    }

    public void lagre( AksjonspunktDefDvh aksjonspunktDefDvh ) {
        entityManager.persist(aksjonspunktDefDvh);
        entityManager.flush();
    }

}
