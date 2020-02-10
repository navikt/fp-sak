package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class OppgaveBehandlingKoblingRepository {

    private EntityManager entityManager;

    OppgaveBehandlingKoblingRepository() {
        // for CDI proxy
    }

    @Inject
    public OppgaveBehandlingKoblingRepository(@VLPersistenceUnit EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    /**
     * Lagrer kobling til GSAK oppgave for behandling. Sørger for at samtidige oppdateringer på samme Behandling, ikke kan gjøres samtidig.
     *
     * @see BehandlingLås
     */
    public Long lagre(OppgaveBehandlingKobling oppgaveBehandlingKobling) {
        entityManager.persist(oppgaveBehandlingKobling);
        entityManager.flush();
        return oppgaveBehandlingKobling.getId();
    }

    
    public Optional<OppgaveBehandlingKobling> hentOppgaveBehandlingKobling(String oppgaveId) {
        TypedQuery<OppgaveBehandlingKobling> query = entityManager.createQuery("from OppgaveBehandlingKobling where oppgave_id=:oppgaveId", //$NON-NLS-1$
            OppgaveBehandlingKobling.class);
        query.setParameter("oppgaveId", oppgaveId); //$NON-NLS-1$
        return HibernateVerktøy.hentUniktResultat(query);
    }

    
    public List<OppgaveBehandlingKobling> hentOppgaverRelatertTilBehandling(Long behandlingId) {
        TypedQuery<OppgaveBehandlingKobling> query = entityManager.createQuery("from OppgaveBehandlingKobling where behandling_id=:behandlingId", //$NON-NLS-1$
            OppgaveBehandlingKobling.class);
        query.setParameter("behandlingId", behandlingId); //$NON-NLS-1$
        return query.getResultList();
    }

    
    public List<OppgaveBehandlingKobling> hentUferdigeOppgaverOpprettetTidsrom(LocalDate fom, LocalDate tom, Set<OppgaveÅrsak> oppgaveTyper) {
        TypedQuery<OppgaveBehandlingKobling> query = entityManager.
            createQuery("from OppgaveBehandlingKobling where ferdigstilt=:ferdig and opprettet_tid >= :fom and opprettet_tid <= :tom and oppgaveÅrsak in :aarsaker", //$NON-NLS-1$
            OppgaveBehandlingKobling.class);
        query.setParameter("fom", fom.atStartOfDay()); //$NON-NLS-1$
        query.setParameter("tom", tom.plusDays(1).atStartOfDay().minusMinutes(1)); //$NON-NLS-1$
        query.setParameter("aarsaker", oppgaveTyper); //$NON-NLS-1$
        query.setParameter("ferdig", Boolean.FALSE); //$NON-NLS-1$
        return query.getResultList();
    }
}
