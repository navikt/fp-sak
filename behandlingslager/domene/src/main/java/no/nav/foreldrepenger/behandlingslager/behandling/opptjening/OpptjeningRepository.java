package no.nav.foreldrepenger.behandlingslager.behandling.opptjening;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hibernate.jpa.HibernateHints;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class OpptjeningRepository {

    private EntityManager em;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    public OpptjeningRepository() {
        // for CDI proxy
    }

    @Inject
    public OpptjeningRepository( EntityManager em, BehandlingRepository behandlingRepository) {
        Objects.requireNonNull(em, "em");
        Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.em = em;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = new BehandlingsresultatRepository(em);
    }

    private VilkårResultat validerRiktigBehandling(Behandling behandling) {
        Objects.requireNonNull(behandling, "behandling");
        var behandlingresultat = getBehandlingsresultat(behandling.getId());
        if (behandlingresultat == null) {
            throw new IllegalArgumentException(
                "Utvikler-feil: kan ikke sette opptjening før Behandlingresultat er lagd for Behandling:" + behandling.getId());
        }
        var vilkårResultat = behandlingresultat.getVilkårResultat();
        if (vilkårResultat == null) {
            throw new IllegalArgumentException(
                "Utvikler-feil: kan ikke sette opptjening før VilkårResultat er lagd for Behandling:" + behandling.getId());
        }
        if (!behandling.getId().equals(vilkårResultat.getOriginalBehandlingId())) {
            throw new IllegalArgumentException(
                "Utvikler-feil: kan ikke sette opptjening på vilkårResultat fra tidligere behandling. behanlding= " + behandling.getId()
                    + ", original=" + vilkårResultat);
        }
        return vilkårResultat;
    }

    /**
     * Finn gjeldende opptjening for denne behandlingen.
     */
    public Optional<Opptjening> finnOpptjening(Long behandlingId) {
        return Optional.ofNullable(getBehandlingsresultat(behandlingId))
            .map(Behandlingsresultat::getVilkårResultat)
            .flatMap(this::finnOpptjening);
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
    }

    public Optional<Opptjening> finnOpptjening(VilkårResultat vilkårResultat) {

        return hentTidligereOpptjening(vilkårResultat.getId(), true);
    }

    private Optional<Opptjening> hentTidligereOpptjening(Long vilkårResultatId, boolean readOnly) {
        // slår opp med HQL istedf. å traverse grafen
        var query = em.createQuery("from Opptjening o where o.vilkårResultat.id=:id and o.aktiv = true", Opptjening.class);
        query.setParameter("id", vilkårResultatId);

        if (readOnly) {
            // returneres read-only, kan kun legge til nye ved skriving uten å oppdatere
            query.setHint(HibernateHints.HINT_READ_ONLY, "true");
        }
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private Optional<Opptjening> deaktivereTidligereOpptjening(Long vilkårResultatId, boolean readOnly) {
        var opptjening = hentTidligereOpptjening(vilkårResultatId, readOnly);
        if (opptjening.isPresent()) {
            var query = em.createNativeQuery("UPDATE OPPTJENING SET AKTIV = 'N' WHERE ID=:id");
            query.setParameter("id", opptjening.get().getId());
            query.executeUpdate();
            em.flush();
            return opptjening;
        }
        return opptjening;
    }

    /**
     * Lagre Opptjeningsperiode (fom, tom) for en gitt behandling.
     */
    public Opptjening lagreOpptjeningsperiode(Behandling behandling, LocalDate opptjeningFom, LocalDate opptjeningTom, boolean skalBevareResultat) {

        UnaryOperator<Opptjening> oppdateringsfunksjon = tidligereOpptjening -> {
            // lager ny opptjening alltid ved ny opptjeningsperiode.
            var nyOpptjening = new Opptjening(opptjeningFom, opptjeningTom);
            if (skalBevareResultat) {
                var kopiListe = duplikatSjekk(tidligereOpptjening.getOpptjeningAktivitet());
                nyOpptjening.setOpptjentPeriode(tidligereOpptjening.getOpptjentPeriode());
                nyOpptjening.setOpptjeningAktivitet(kopiListe);
            }
            return nyOpptjening;
        };

        return lagre(behandling, oppdateringsfunksjon);
    }

    public void deaktiverOpptjening(Behandling behandling) {
        var vilkårResultat = validerRiktigBehandling(behandling);

        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        deaktivereTidligereOpptjening(vilkårResultat.getId(), false);
        em.flush();

        behandlingRepository.verifiserBehandlingLås(behandlingLås);
    }

    private Opptjening lagre(Behandling behandling, UnaryOperator<Opptjening> oppdateringsfunksjon) {
        var behandlingId = behandling.getId();
        var vilkårResultat = validerRiktigBehandling(behandling);

        var behandlingLås = behandlingRepository.taSkriveLås(behandling);


        Opptjening tidligereOpptjening = null;
        Opptjening opptjening;
        var optTidligereOpptjening = deaktivereTidligereOpptjening(vilkårResultat.getId(), false);
        if (optTidligereOpptjening.isPresent()) {
            tidligereOpptjening = optTidligereOpptjening.get();
        }
        opptjening = oppdateringsfunksjon.apply(tidligereOpptjening);

        opptjening.setVilkårResultat(Optional.ofNullable(getBehandlingsresultat(behandlingId)).orElseThrow().getVilkårResultat());

        em.persist(opptjening);
        em.flush();

        behandlingRepository.verifiserBehandlingLås(behandlingLås);

        return opptjening;

    }

    /** Opptjening* Lagre opptjeningresultat (opptjent periode og aktiviteter).*/
    public Opptjening lagreOpptjeningResultat(Behandling behandling, Period opptjentPeriode,
                                              Collection<OpptjeningAktivitet> opptjeningAktiviteter) {

        var kopiListe = duplikatSjekk(opptjeningAktiviteter);

        UnaryOperator<Opptjening> oppdateringsfunksjon = tidligereOpptjening -> {
            var ny = new Opptjening(tidligereOpptjening);
            ny.setOpptjeningAktivitet(kopiListe);
            ny.setOpptjentPeriode(opptjentPeriode);
            return ny;
        };

        return lagre(behandling, oppdateringsfunksjon);
    }

    /**
     * Kopier over grunnlag til ny behandling
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Behandling origBehandling, Behandling nyBehandling) {
        // Opptjening er ikke koblet til Behandling gjennom aggregatreferanse. Må derfor kopieres som deep copy
        var orgBehandlingId = origBehandling.getId();
        var origVilkårResultatId = Optional.ofNullable(getBehandlingsresultat(orgBehandlingId)).orElseThrow().getVilkårResultat().getId();
        var origOpptjening = hentTidligereOpptjening(origVilkårResultatId, true)
            .orElseThrow(() -> new IllegalStateException("Original behandling har ikke opptjening."));

        lagreOpptjeningsperiode(nyBehandling, origOpptjening.getFom(), origOpptjening.getTom(), false);
        lagreOpptjeningResultat(nyBehandling, origOpptjening.getOpptjentPeriode(), origOpptjening.getOpptjeningAktivitet());
    }

    private Set<OpptjeningAktivitet> duplikatSjekk(Collection<OpptjeningAktivitet> opptjeningAktiviteter) {
        // ta kopi av alle aktiviteter for å være sikker på at gamle ikke skrives inn.
        if (opptjeningAktiviteter == null) {
            return Collections.emptySet();
        }
        Set<OpptjeningAktivitet> kopiListe = opptjeningAktiviteter.stream().map(OpptjeningAktivitet::new).collect(Collectors.toCollection(LinkedHashSet::new));

        if (opptjeningAktiviteter.size() > kopiListe.size()) {
            // har duplikater!!
            Set<OpptjeningAktivitet> duplikater = opptjeningAktiviteter.stream()
                .filter(oa -> Collections.frequency(opptjeningAktiviteter, oa) > 1)
                .collect(Collectors.toCollection(LinkedHashSet::new));

            throw new IllegalArgumentException(
                "Utvikler-feil: har duplikate opptjeningsaktiviteter: [" + duplikater + "] i input: " + opptjeningAktiviteter);
        }
        return kopiListe;
    }

}
