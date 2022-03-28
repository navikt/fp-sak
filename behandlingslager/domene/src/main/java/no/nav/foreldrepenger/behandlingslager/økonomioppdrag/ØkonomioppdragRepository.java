package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class ØkonomioppdragRepository {

    private static final String SAKSNR = "saksnr";

    private EntityManager entityManager;

    ØkonomioppdragRepository() {
        // for CDI proxy
    }

    @Inject
    public ØkonomioppdragRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Oppdragskontroll hentOppdragskontroll(long oppdragskontrollId) {
        var query = entityManager.createQuery(
            "from Oppdragskontroll where id=:oppdragskontrollId", Oppdragskontroll.class); //$NON-NLS-1$
        query.setParameter("oppdragskontrollId", oppdragskontrollId); //$NON-NLS-1$
        return hentEksaktResultat(query);
    }

    public List<Oppdrag110> hentOppdrag110ForPeriodeOgFagområde(LocalDate fomDato, LocalDate tomDato, KodeFagområde fagområde) {
        Objects.requireNonNull(fomDato, "fomDato");
        Objects.requireNonNull(tomDato, "tomDato");
        Objects.requireNonNull(fagområde, "fagområde");
        if (fomDato.isAfter(tomDato)) {
            throw new IllegalArgumentException("Datointervall for økonomioppdrag må inneholde minst 1 dag");
        }

        var resultList = entityManager
            .createQuery("""
                select o110 from Oppdrag110 as o110
                where o110.opprettetTidspunkt >= :fomTidspunkt
                    and o110.opprettetTidspunkt < :tilTidspunkt
                    and o110.kodeFagomrade = :fagomrade
                order by o110.opprettetTidspunkt, o110.nøkkelAvstemming
                    """,
                Oppdrag110.class) //$NON-NLS-1$
            .setParameter("fomTidspunkt", fomDato.atStartOfDay()) //$NON-NLS-1$
            .setParameter("tilTidspunkt", tomDato.plusDays(1).atStartOfDay()) //$NON-NLS-1$
            .setParameter("fagomrade", fagområde)
            .getResultList(); //$NON-NLS-1$

        return resultList;
    }

    public Oppdrag110 hentOppdragUtenKvittering(long fagsystemId, long behandlingId) {
        Objects.requireNonNull(fagsystemId, "fagsystemId");
        Objects.requireNonNull(behandlingId, "behandlingId");
        var typedQuery = entityManager
            .createQuery("""
                    select o110 from Oppdrag110 as o110
                    where o110.oppdragskontroll.behandlingId = :behandlingId
                        and o110.fagsystemId = :fagsystemId
                        and o110.oppdragKvittering is empty
                    """, Oppdrag110.class) //$NON-NLS-1$
            .setParameter("fagsystemId", fagsystemId)
            .setParameter("behandlingId", behandlingId)
            ;
        return hentEksaktResultat(typedQuery);
    }

    public Optional<Oppdragskontroll> finnOppdragForBehandling(long behandlingId) {
        var resultList = entityManager.createQuery(
            "from Oppdragskontroll where behandlingId = :behandlingId", Oppdragskontroll.class)//$NON-NLS-1$
            .setParameter("behandlingId", behandlingId)
            .getResultList();

        if (resultList.size() > 1) {
            throw new IllegalStateException("Utviklerfeil: Finnes mer enn en Oppdragskontroll for behandling " + behandlingId);
        }
        return resultList.isEmpty() ? Optional.empty() : Optional.of(resultList.get(0));
    }


    public List<Oppdragskontroll> finnAlleOppdragForSak(Saksnummer saksnr) {
        return entityManager.createQuery(
            "from Oppdragskontroll where saksnummer = :saksnr", Oppdragskontroll.class)//$NON-NLS-1$
            .setParameter(SAKSNR, saksnr)
            .getResultList();
    }


    public long lagre(Oppdragskontroll oppdragskontroll) {
        entityManager.persist(oppdragskontroll);
        oppdragskontroll.getOppdrag110Liste().forEach(this::lagre);
        entityManager.flush();
        return oppdragskontroll.getId();
    }

    private void lagre(Oppdrag110 oppdrag110) {
        entityManager.persist(oppdrag110);
        oppdrag110.getOmpostering116().ifPresent(this::lagre);
        oppdrag110.getOppdragslinje150Liste().forEach(this::lagre);
    }

    private void lagre(Ompostering116 ompostering116) {
        if (ompostering116 != null) {
            entityManager.persist(ompostering116);
        }
    }

    private void lagre(Oppdragslinje150 oppdragslinje150) {
        entityManager.persist(oppdragslinje150);
        lagre(oppdragslinje150.getRefusjonsinfo156());
    }

    private void lagre(Refusjonsinfo156 refusjonsinfo156) {
        if (null != refusjonsinfo156) {
            entityManager.persist(refusjonsinfo156);
        }
    }

    public void lagre(OppdragKvittering oppdragKvittering) {
        entityManager.persist(oppdragKvittering);
        entityManager.flush();
    }
}
