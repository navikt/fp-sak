package no.nav.foreldrepenger.behandlingslager.fagsak;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;
import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import no.nav.foreldrepenger.behandlingslager.TraverseEntityGraphFactory;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.diff.DiffEntity;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLås;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class FagsakRelasjonRepository {

    private EntityManager entityManager;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private FagsakLåsRepository fagsakLåsRepository;

    protected FagsakRelasjonRepository() {
        // for CDI proxy
    }

    @Inject
    public FagsakRelasjonRepository( EntityManager entityManager, YtelsesFordelingRepository ytelsesFordelingRepository,
                                    FagsakLåsRepository fagsakLåsRepository) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.fagsakLåsRepository = fagsakLåsRepository;
    }

    public FagsakRelasjon finnRelasjonFor(Fagsak fagsak) {
        var query = entityManager.createQuery("from FagsakRelasjon where (fagsakNrEn=:fagsak or fagsakNrTo=:fagsak) AND aktiv = true",
            FagsakRelasjon.class);
        query.setParameter("fagsak", fagsak); // NOSONAR $NON-NLS-1$
        return hentEksaktResultat(query);
    }

    public FagsakRelasjon finnRelasjonFor(Saksnummer saksnummer) {
        var query = finnRelasjonQueryHvisEksisterer(saksnummer);
        return (FagsakRelasjon) query.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public Optional<FagsakRelasjon> finnRelasjonHvisEksisterer(Saksnummer saksnummer) {
        var query = finnRelasjonQueryHvisEksisterer(saksnummer);
        List<FagsakRelasjon> resultatListe = query.getResultList();
        if (resultatListe.size() > 1) {
            throw new IllegalStateException("Fant mer enn en FagsakRelasjon for saksnummer: " + saksnummer);
        }
        return resultatListe.size() == 1 ? Optional.of(resultatListe.get(0)) : Optional.empty();
    }

    private Query finnRelasjonQueryHvisEksisterer(Saksnummer saksnummer) {
        var query = entityManager.createNativeQuery(
            "SELECT fr.* FROM FAGSAK_RELASJON fr" +
                " INNER JOIN FAGSAK fagsak ON fagsak.id IN (fr.fagsak_en_id, fr.fagsak_to_id)" +
                " WHERE fr.AKTIV = 'J'" +
                " AND fagsak.saksnummer = :saksnummer",
            FagsakRelasjon.class);
        query.setParameter("saksnummer", saksnummer.getVerdi()); // NOSONAR $NON-NLS-1$
        return query;
    }

    public Optional<FagsakRelasjon> finnRelasjonForHvisEksisterer(Fagsak fagsak) {
        var query = entityManager.createQuery("from FagsakRelasjon where (fagsakNrEn=:fagsak or fagsakNrTo=:fagsak) AND aktiv = true",
            FagsakRelasjon.class);
        query.setParameter("fagsak", fagsak); // NOSONAR $NON-NLS-1$
        return hentUniktResultat(query);
    }

    public Optional<FagsakRelasjon> finnRelasjonForHvisEksisterer(long fagsakId) {
        var query = entityManager.createQuery("from FagsakRelasjon where (fagsakNrEn.id=:fagsak or fagsakNrTo.id=:fagsak) AND aktiv = true",
            FagsakRelasjon.class);
        query.setParameter("fagsak", fagsakId); // NOSONAR $NON-NLS-1$
        return hentUniktResultat(query);
    }

    public void lagre(Fagsak fagsak, Long behandlingId, Stønadskontoberegning stønadskontoberegning) {
        Objects.requireNonNull(stønadskontoberegning, "stønadskontoberegning");

        final var fagsak1Lås = fagsakLåsRepository.taLås(fagsak.getId());
        FagsakLås fagsak2Lås = null;
        final var fagsakRelasjon = hentEllerOpprett(fagsak, behandlingId);
        final var fagsakNrTo = fagsakRelasjon.getFagsakNrTo();
        if (fagsakNrTo.isPresent()) {
            fagsak2Lås = fagsakLåsRepository.taLås(fagsakNrTo.get().getId());
        }
        final var forskjellige = differ().areDifferent(fagsakRelasjon.getStønadskontoberegning().orElse(null), stønadskontoberegning);
        if (forskjellige) {
            deaktiverEksisterendeRelasjon(fagsakRelasjon);
            entityManager.persist(stønadskontoberegning);
            for (var stønadskonto : stønadskontoberegning.getStønadskontoer()) {
                entityManager.persist(stønadskonto);
            }
            final var nyFagsakRelasjon = new FagsakRelasjon(fagsakRelasjon.getFagsakNrEn(),
                fagsakRelasjon.getFagsakNrTo().orElse(null),
                stønadskontoberegning,
                fagsakRelasjon.getOverstyrtStønadskontoberegning().orElse(null),
                fagsakRelasjon.getDekningsgrad(),
                fagsakRelasjon.getOverstyrtDekningsgrad().orElse(null),
                fagsakRelasjon.getAvsluttningsdato());

            entityManager.persist(nyFagsakRelasjon);
        }
        fagsakLåsRepository.oppdaterLåsVersjon(fagsak1Lås);
        if (fagsak2Lås != null) {
            fagsakLåsRepository.oppdaterLåsVersjon(fagsak2Lås);
        }
        entityManager.flush();
    }

    private FagsakRelasjon hentEllerOpprett(Fagsak fagsak, Long behandlingId) {
        final var optionalFagsakRelasjon = finnRelasjonForHvisEksisterer(fagsak);
        if (optionalFagsakRelasjon.isEmpty()) {
            opprettRelasjon(fagsak, getDekningsgrad(behandlingId));
        }
        return finnRelasjonFor(fagsak);
    }

    private Dekningsgrad getDekningsgrad(Long behandlingId) {
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(behandlingId);
        return Dekningsgrad.grad(ytelseFordelingAggregat.getOppgittDekningsgrad().getDekningsgrad());
    }

    public FagsakRelasjon opprettRelasjon(Fagsak fagsak, Dekningsgrad dekningsgrad) {
        Objects.requireNonNull(fagsak, "fagsak"); // NOSONAR $NON-NLS-1$
        final var fagsakLås = fagsakLåsRepository.taLås(fagsak);
        return opprettRelasjon(fagsak, dekningsgrad, fagsakLås);
    }

    private FagsakRelasjon opprettRelasjon(Fagsak fagsak, Dekningsgrad dekningsgrad, FagsakLås fagsakLås) {
        Objects.requireNonNull(fagsak, "fagsak"); // NOSONAR $NON-NLS-1$

        final var nyFagsakRelasjon = new FagsakRelasjon(fagsak, null, null,
            null, dekningsgrad, null, null);

        entityManager.persist(nyFagsakRelasjon);
        fagsakLåsRepository.oppdaterLåsVersjon(fagsakLås);
        entityManager.flush();
        return nyFagsakRelasjon;
    }

    public FagsakRelasjon overstyrDekningsgrad(Fagsak fagsak, Dekningsgrad overstyrtVerdi) {
        Objects.requireNonNull(overstyrtVerdi);
        return oppdaterRelasjon(fagsak, finnRelasjonFor(fagsak).getDekningsgrad(), overstyrtVerdi);
    }

    private FagsakRelasjon oppdaterRelasjon(Fagsak fagsak, Dekningsgrad dekningsgrad, Dekningsgrad overstyrtDekningsgrad) {
        Objects.requireNonNull(fagsak, "fagsak"); // NOSONAR $NON-NLS-1$
        var eksisterendeFagsakRelasjon = finnRelasjonFor(fagsak);
        if (eksisterendeFagsakRelasjon.getDekningsgrad().equals(overstyrtDekningsgrad) || dekningsgrad.equals(overstyrtDekningsgrad)) {
            return eksisterendeFagsakRelasjon;
        }
        final var fagsakLås = fagsakLåsRepository.taLås(fagsak);
        deaktiverEksisterendeRelasjon(eksisterendeFagsakRelasjon);
        final var nyFagsakRelasjon = new FagsakRelasjon(eksisterendeFagsakRelasjon.getFagsakNrEn(),
            eksisterendeFagsakRelasjon.getFagsakNrTo().orElse(null),
            eksisterendeFagsakRelasjon.getStønadskontoberegning().orElse(null),
            // Hvis dekningsgrad blir oversyrt så skal overstyrt stønadskontoberegning allerede være null
            null,
            dekningsgrad,
            overstyrtDekningsgrad,
            eksisterendeFagsakRelasjon.getAvsluttningsdato());
        entityManager.persist(nyFagsakRelasjon);
        fagsakLåsRepository.oppdaterLåsVersjon(fagsakLås);
        entityManager.flush();
        return nyFagsakRelasjon;
    }

    public Optional<FagsakRelasjon> opprettEllerOppdaterRelasjon(Fagsak fagsak, Optional<FagsakRelasjon> fagsakRelasjon, Dekningsgrad dekningsgrad) {
        if (fagsakRelasjon.isPresent()) {
            if (!dekningsgrad.equals(fagsakRelasjon.get().getDekningsgrad())) {
                final var fagsakLås = fagsakLåsRepository.taLås(fagsak.getId());
                deaktiverEksisterendeRelasjon(fagsakRelasjon.get());
                return Optional.of(opprettRelasjon(fagsak, dekningsgrad, fagsakLås));
            }
        } else {
            final var fagsakLås = fagsakLåsRepository.taLås(fagsak.getId());
            return Optional.of(opprettRelasjon(fagsak, dekningsgrad, fagsakLås));
        }
        return Optional.empty();
    }

    public Optional<FagsakRelasjon> kobleFagsaker(Fagsak fagsakEn, Fagsak fagsakTo, Behandling behandlingEn) {
        Objects.requireNonNull(fagsakEn, "fagsakEn");
        Objects.requireNonNull(fagsakTo, "fagsakTo");
        final var fagsak1Lås = fagsakLåsRepository.taLås(fagsakEn.getId());
        final var fagsak2Lås = fagsakLåsRepository.taLås(fagsakTo.getId());

        if (!fagsakEn.getYtelseType().equals(fagsakTo.getYtelseType())) {
            var msg = String.format(
                "Kan ikke koble sammen saker med forskjellig ytelse type. Prøver å koble sammen fagsakene %s (%s) og %s (%s).",
                fagsakEn.getId(), fagsakEn.getYtelseType(), fagsakTo.getId(), fagsakTo.getYtelseType());
            throw new TekniskException("FP-983410", msg);
        }
        if (fagsakEn.getId().equals(fagsakTo.getId())) {
            var msg = String.format("Prøver å koble fagsak med saksnummer %s sammen med seg selv",
                fagsakEn.getSaksnummer());
            throw new TekniskException("FP-831923", msg);
        }
        if (fagsakEn.getAktørId().equals(fagsakTo.getAktørId())) {
            var msg = String.format(
                "Kan ikke koble sammen to saker med identisk aktørid. Prøver å koble sammen fagsakene %s og %s, aktør %s.",
                fagsakEn.getSaksnummer(), fagsakTo.getSaksnummer(), fagsakEn.getAktørId());
            throw new TekniskException("FP-102432", msg);
        }

        final var fagsakRelasjon1 = finnRelasjonForHvisEksisterer(fagsakEn);
        fagsakRelasjon1.ifPresent(this::deaktiverEksisterendeRelasjon);
        final var fagsakRelasjon2 = finnRelasjonForHvisEksisterer(fagsakTo);
        fagsakRelasjon2.ifPresent(this::deaktiverEksisterendeRelasjon);

        final var nyFagsakRelasjon = new FagsakRelasjon(fagsakEn,
            fagsakTo,
            fagsakRelasjon1.flatMap(FagsakRelasjon::getStønadskontoberegning).orElse(null),
            fagsakRelasjon1.flatMap(FagsakRelasjon::getOverstyrtStønadskontoberegning).orElse(null),
            fagsakRelasjon1.map(FagsakRelasjon::getDekningsgrad)
                .orElseGet(() -> getDekningsgrad(behandlingEn.getId())),
            fagsakRelasjon1.flatMap(FagsakRelasjon::getOverstyrtDekningsgrad).orElse(null),
            fagsakRelasjon1.map(FagsakRelasjon::getAvsluttningsdato).orElse(null));

        entityManager.persist(nyFagsakRelasjon);
        fagsakLåsRepository.oppdaterLåsVersjon(fagsak1Lås);
        fagsakLåsRepository.oppdaterLåsVersjon(fagsak2Lås);
        entityManager.flush();
        return Optional.of(nyFagsakRelasjon);
    }

    public Optional<FagsakRelasjon> fraKobleFagsaker(Fagsak fagsakEn, Fagsak fagsakTo) {
        Objects.requireNonNull(fagsakEn, "fagsakEn");
        Objects.requireNonNull(fagsakTo, "fagsakTo");
        final var fagsak1Lås = fagsakLåsRepository.taLås(fagsakEn.getId());
        final var fagsak2Lås = fagsakLåsRepository.taLås(fagsakTo.getId());

        final var fagsakRelasjon = finnRelasjonForHvisEksisterer(fagsakEn).orElse(null);

        if (fagsakRelasjon == null || fagsakRelasjon.getFagsakNrTo().isEmpty()
            || !fagsakRelasjon.getFagsakNrEn().getId().equals(fagsakEn.getId())
            || !fagsakRelasjon.getFagsakNrTo().get().getId().equals(fagsakTo.getId())) {
            var msg = String.format("Fagsakene %s og %s er ikke koblet.", fagsakEn.getSaksnummer(),
                fagsakTo.getSaksnummer());
            throw new TekniskException("FP-102433", msg);
        }

        deaktiverEksisterendeRelasjon(fagsakRelasjon);

        fjernFagsak2(fagsakRelasjon, fagsak1Lås);
        return Optional.of(opprettRelasjon(fagsakTo, fagsakRelasjon.getDekningsgrad(), fagsak2Lås));
    }

    private FagsakRelasjon fjernFagsak2(FagsakRelasjon fagsakRelasjon, FagsakLås fagsakLås) {
        Objects.requireNonNull(fagsakRelasjon, "fagsak"); // NOSONAR $NON-NLS-1$

        final var nyFagsakRelasjon = new FagsakRelasjon(fagsakRelasjon.getFagsakNrEn(),
            null,
            fagsakRelasjon.getStønadskontoberegning().orElse(null),
            fagsakRelasjon.getOverstyrtStønadskontoberegning().orElse(null),
            fagsakRelasjon.getDekningsgrad(),
            fagsakRelasjon.getOverstyrtDekningsgrad().orElse(null),
            fagsakRelasjon.getAvsluttningsdato());
        entityManager.persist(nyFagsakRelasjon);
        fagsakLåsRepository.oppdaterLåsVersjon(fagsakLås);
        entityManager.flush();
        return nyFagsakRelasjon;
    }

    public FagsakRelasjon nullstillOverstyrtDekningsgrad(Fagsak fagsak) {
        var fagsakRelasjon = finnRelasjonFor(fagsak);
        oppdaterRelasjon(fagsak, fagsakRelasjon.getDekningsgrad(), null);
        return fagsakRelasjon;
    }

    public Optional<FagsakRelasjon> nullstillOverstyrtStønadskontoberegning(Fagsak fagsak) {
        final var fagsak1Lås = fagsakLåsRepository.taLås(fagsak);
        FagsakLås fagsak2Lås = null;
        final var fagsakRelasjon = finnRelasjonFor(fagsak);
        final var fagsakNrTo = fagsakRelasjon.getFagsakNrTo();
        if (fagsakNrTo.isPresent()) {
            fagsak2Lås = fagsakLåsRepository.taLås(fagsakNrTo.get().getId());
        }
        deaktiverEksisterendeRelasjon(fagsakRelasjon);

        final var nyFagsakRelasjon = new FagsakRelasjon(fagsakRelasjon.getFagsakNrEn(),
            fagsakRelasjon.getFagsakNrTo().orElse(null),
            fagsakRelasjon.getStønadskontoberegning().orElse(null),
            null,
            fagsakRelasjon.getDekningsgrad(),
            fagsakRelasjon.getOverstyrtDekningsgrad().orElse(null),
            fagsakRelasjon.getAvsluttningsdato());

        entityManager.persist(nyFagsakRelasjon);

        fagsakLåsRepository.oppdaterLåsVersjon(fagsak1Lås);
        if (fagsak2Lås != null) {
            fagsakLåsRepository.oppdaterLåsVersjon(fagsak2Lås);
        }
        entityManager.flush();
        return Optional.of(nyFagsakRelasjon);
    }

    public void overstyrStønadskontoberegning(Fagsak fagsak, Long behandlingId, Stønadskontoberegning stønadskontoberegning) {
        Objects.requireNonNull(stønadskontoberegning, "stønadskontoberegning");

        final var fagsak1Lås = fagsakLåsRepository.taLås(fagsak);
        FagsakLås fagsak2Lås = null;
        final var fagsakRelasjon = hentEllerOpprett(fagsak, behandlingId);
        final var fagsakNrTo = fagsakRelasjon.getFagsakNrTo();
        if (fagsakNrTo.isPresent()) {
            fagsak2Lås = fagsakLåsRepository.taLås(fagsakNrTo.get().getId());
        }
        deaktiverEksisterendeRelasjon(fagsakRelasjon);
        entityManager.persist(stønadskontoberegning);
        for (var stønadskonto : stønadskontoberegning.getStønadskontoer()) {
            entityManager.persist(stønadskonto);
        }

        final var nyFagsakRelasjon = new FagsakRelasjon(fagsakRelasjon.getFagsakNrEn(),
            fagsakRelasjon.getFagsakNrTo().orElse(null),
            fagsakRelasjon.getStønadskontoberegning().orElse(null),
            stønadskontoberegning,
            fagsakRelasjon.getDekningsgrad(),
            fagsakRelasjon.getOverstyrtDekningsgrad().orElse(null),
            fagsakRelasjon.getAvsluttningsdato());

        entityManager.persist(nyFagsakRelasjon);

        fagsakLåsRepository.oppdaterLåsVersjon(fagsak1Lås);
        if (fagsak2Lås != null) {
            fagsakLåsRepository.oppdaterLåsVersjon(fagsak2Lås);
        }
        entityManager.flush();
    }

    private void deaktiverEksisterendeRelasjon(FagsakRelasjon relasjon) {
        relasjon.setAktiv(false);
        entityManager.persist(relasjon);
        entityManager.flush();
    }

    private DiffEntity differ() {
        var traverser = TraverseEntityGraphFactory.build(true, Fagsak.class);
        return new DiffEntity(traverser);
    }

    @SuppressWarnings("unused")
    public Optional<FagsakRelasjon> oppdaterMedAvsluttningsdato(FagsakRelasjon relasjon, LocalDate avsluttningsdato, FagsakRelasjonLås lås, Optional<FagsakLås> fagsak1Lås, Optional<FagsakLås> fagsak2Lås) {
        Objects.requireNonNull(avsluttningsdato, "avsluttningsdato");

        if (relasjon.getAvsluttningsdato() != null && relasjon.getAvsluttningsdato().equals(avsluttningsdato)) {
            return Optional.empty();
        }
        deaktiverEksisterendeRelasjon(relasjon);

        final var nyFagsakRelasjon = new FagsakRelasjon(relasjon.getFagsakNrEn(),
            relasjon.getFagsakNrTo().orElse(null),
            relasjon.getStønadskontoberegning().orElse(null),
            relasjon.getOverstyrtStønadskontoberegning().orElse(null),
            relasjon.getDekningsgrad(),
            relasjon.getOverstyrtDekningsgrad().orElse(null),
            avsluttningsdato);

        entityManager.persist(nyFagsakRelasjon);

        fagsak1Lås.ifPresent(fagsakLåsRepository::oppdaterLåsVersjon);
        fagsak2Lås.ifPresent(fagsakLåsRepository::oppdaterLåsVersjon);
        entityManager.flush();
        return Optional.of(nyFagsakRelasjon);
    }

    public List<Fagsak> finnFagsakerForAvsluttning(LocalDate dato) {
        // La saker som er unde behandling være i fred
        var query = entityManager.createQuery("select f from Fagsak f " +
                "inner join FagsakRelasjon fr on (f.id in (fr.fagsakNrEn, fr.fagsakNrTo) and fr.aktiv=true) " +
                "where f.fagsakStatus = :lopende and fr.avsluttningsdato < :datogrense",
            Fagsak.class)
            .setParameter("datogrense", Optional.ofNullable(dato).orElseGet(LocalDate::now))
            .setParameter("lopende", FagsakStatus.LØPENDE.getKode()); // NOSONAR $NON-NLS-1$

        return query.getResultList();
    }
}
