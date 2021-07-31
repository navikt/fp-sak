package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.hibernate.jpa.QueryHints;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class OppgaveBehandlingKoblingRepository {

    private EntityManager entityManager;

    OppgaveBehandlingKoblingRepository() {
        // for CDI proxy
    }

    @Inject
    public OppgaveBehandlingKoblingRepository( EntityManager entityManager) {
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
        var query = entityManager.createQuery("from OppgaveBehandlingKobling where oppgave_id=:oppgaveId", //$NON-NLS-1$
            OppgaveBehandlingKobling.class);
        query.setParameter("oppgaveId", oppgaveId); //$NON-NLS-1$
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public Optional<OppgaveBehandlingKobling> hentOppgaveBehandlingKobling(Long behandlingId, String oppgaveId) {
        var query = entityManager
            .createQuery("from OppgaveBehandlingKobling where behandling_id=:behandlingId and oppgave_id=:oppgaveId", //$NON-NLS-1$
            OppgaveBehandlingKobling.class);
        query.setParameter("oppgaveId", oppgaveId); //$NON-NLS-1$
        query.setParameter("behandlingId", behandlingId); //$NON-NLS-1$
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public Optional<OppgaveBehandlingKobling> hentOppgaveBehandlingKobling(String oppgaveId, Saksnummer saksnummer) {
        var query = entityManager
            .createQuery("from OppgaveBehandlingKobling where oppgave_id=:oppgaveId and saksnummer=:saksnummer", //$NON-NLS-1$
            OppgaveBehandlingKobling.class);
        query.setParameter("oppgaveId", oppgaveId); //$NON-NLS-1$
        query.setParameter("saksnummer", saksnummer); //$NON-NLS-1$
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public List<OppgaveBehandlingKobling> hentOppgaverRelatertTilBehandling(Long behandlingId) {
        var query = entityManager.createQuery("from OppgaveBehandlingKobling where behandling_id=:behandlingId", //$NON-NLS-1$
            OppgaveBehandlingKobling.class);
        query.setParameter("behandlingId", behandlingId); //$NON-NLS-1$
        return query.getResultList();
    }


    public List<OppgaveBehandlingKobling> hentUferdigeOppgaverOpprettetTidsrom(LocalDate fom, LocalDate tom, Set<OppgaveÅrsak> oppgaveTyper) {
        var query = entityManager.
            createQuery("from OppgaveBehandlingKobling where ferdigstilt=:ferdig and opprettet_tid >= :fom and opprettet_tid <= :tom and oppgaveÅrsak in :aarsaker ",
                OppgaveBehandlingKobling.class)
            .setHint(QueryHints.HINT_READONLY, "true")
            .setParameter("fom", fom.atStartOfDay())
            .setParameter("tom", tom.plusDays(1).atStartOfDay().minusMinutes(1))
            .setParameter("aarsaker", oppgaveTyper)
            .setParameter("ferdig", Boolean.FALSE); //$NON-NLS-1$
        return query.getResultList();
    }
}
