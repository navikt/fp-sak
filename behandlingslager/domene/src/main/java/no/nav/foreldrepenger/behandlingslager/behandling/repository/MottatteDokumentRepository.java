package no.nav.foreldrepenger.behandlingslager.behandling.repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class MottatteDokumentRepository {

    private EntityManager entityManager;

    private static final String PARAM_KEY = "param";

    public MottatteDokumentRepository() {
        // for CDI proxy
    }

    @Inject
    public MottatteDokumentRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public MottattDokument lagre(MottattDokument mottattDokument) {
        entityManager.persist(mottattDokument);
        entityManager.flush();

        return mottattDokument;
    }

    public Optional<MottattDokument> hentMottattDokument(long mottattDokumentId) {
        TypedQuery<MottattDokument> query = entityManager.createQuery(
            "select m from MottattDokument m where m.id = :param", MottattDokument.class)
            .setParameter(PARAM_KEY, mottattDokumentId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Returnerer liste av MottattDokument.
     * NB: Kan returnere samme dokument flere ganger dersom de har ulike eks. mottatt_dato, journalføringsenhet (dersom byttet enhet). Er derfor
     * ikke å anbefale å bruke.
     */
    public List<MottattDokument> hentMottatteDokument(long behandlingId) {
        String strQueryTemplate = "select m from MottattDokument m where m.behandlingId = :param";
        return entityManager.createQuery(
            strQueryTemplate, MottattDokument.class)
            .setParameter(PARAM_KEY, behandlingId)
            .getResultList();
    }

    /**
     * Returnerer liste av MottattDokument.
     * NB: Kan returnere samme dokument flere ganger dersom de har ulike eks. mottatt_dato, journalføringsenhet (dersom byttet enhet). Er derfor
     * ikke å anbefale å bruke.
     */
    public List<MottattDokument> hentMottattDokument(JournalpostId journalpostId) {
        var query = entityManager.createQuery( "select m from MottattDokument m where m.journalpostId = :param", MottattDokument.class)
            .setParameter(PARAM_KEY, journalpostId);
        return query.getResultList();
    }

    /**
     * Returnerer liste av MottattDokument.
     * NB: Kan returnere samme dokument flere ganger dersom de har ulike eks. mottatt_dato, journalføringsenhet (dersom byttet enhet). Er derfor
     * ikke å anbefale å bruke.
     */
    public List<MottattDokument> hentMottatteDokumentMedFagsakId(long fagsakId) {
        String strQueryTemplate = "select m from MottattDokument m where m.fagsakId = :param";
        return entityManager.createQuery(
            strQueryTemplate, MottattDokument.class)
            .setParameter(PARAM_KEY, fagsakId)
            .getResultList();
    }

    /**
     * Returnerer liste av MottattDokument.
     * NB: Kan returnere samme dokument flere ganger dersom de har ulike eks. mottatt_dato, journalføringsenhet (dersom byttet enhet). Er derfor
     * ikke å anbefale å bruke.
     */
    public List<MottattDokument> hentMottatteDokumentMedForsendelseId(UUID forsendelseId) {
        String strQueryTemplate = "select m from MottattDokument m where m.forsendelseId = :param";
        return entityManager.createQuery(
            strQueryTemplate, MottattDokument.class)
            .setParameter(PARAM_KEY, forsendelseId)
            .getResultList();
    }

    public void oppdaterMedBehandling(MottattDokument mottattDokument, long behandlingId) {
        entityManager.createQuery(
            "update MottattDokument set behandlingId = :param WHERE id = :dokumentId")
            .setParameter("dokumentId", mottattDokument.getId())
            .setParameter(PARAM_KEY, behandlingId)
            .executeUpdate();
    }

    public void oppdaterMedKanalreferanse(MottattDokument mottattDokument, String kanalreferanse) {
        entityManager.createQuery(
            "update MottattDokument set kanalreferanse = :param WHERE id = :dokumentId")
            .setParameter("dokumentId", mottattDokument.getId())
            .setParameter(PARAM_KEY, kanalreferanse)
            .executeUpdate();
    }
}
