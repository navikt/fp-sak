package no.nav.foreldrepenger.behandlingslager.behandling.opptjening;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.hibernate.jpa.QueryHints;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
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
        Objects.requireNonNull(em, "em"); //$NON-NLS-1$
        Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.em = em;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = new BehandlingsresultatRepository(em);
    }

    private VilkårResultat validerRiktigBehandling(Behandling behandling) {
        Objects.requireNonNull(behandling, "behandling"); //$NON-NLS-1$
        Behandlingsresultat behandlingresultat = getBehandlingsresultat(behandling.getId());
        if (behandlingresultat == null) {
            throw new IllegalArgumentException(
                "Utvikler-feil: kan ikke sette opptjening før Behandlingresultat er lagd for Behandling:" + behandling.getId()); //$NON-NLS-1$
        }
        VilkårResultat vilkårResultat = behandlingresultat.getVilkårResultat();
        if (vilkårResultat == null) {
            throw new IllegalArgumentException(
                "Utvikler-feil: kan ikke sette opptjening før VilkårResultat er lagd for Behandling:" + behandling.getId()); //$NON-NLS-1$
        }
        if (!behandling.getId().equals(vilkårResultat.getOriginalBehandlingId())) {
            throw new IllegalArgumentException(
                "Utvikler-feil: kan ikke sette opptjening på vilkårResultat fra tidligere behandling. behanlding= " + behandling.getId() //$NON-NLS-1$
                    + ", original=" + vilkårResultat); //$NON-NLS-1$
        }
        return vilkårResultat;
    }

    /**
     * Finn gjeldende opptjening for denne behandlingen.
     */
    public Optional<Opptjening> finnOpptjening(Long behandlingId) {
        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(behandlingId);

        if (behandlingsresultat == null) {
            return Optional.empty();
        } else {

            VilkårResultat vilkårResultat = behandlingsresultat.getVilkårResultat();
            if (vilkårResultat == null) {
                return Optional.empty();
            }

            return finnOpptjening(vilkårResultat);
        }
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
    }

    public Optional<Opptjening> finnOpptjening(VilkårResultat vilkårResultat) {

        return hentTidligereOpptjening(vilkårResultat.getId(), true);
    }

    private Optional<Opptjening> hentTidligereOpptjening(Long vilkårResultatId, boolean readOnly) {
        // slår opp med HQL istedf. å traverse grafen
        TypedQuery<Opptjening> query = em.createQuery("from Opptjening o where o.vilkårResultat.id=:id and o.aktiv = 'J'", Opptjening.class); //$NON-NLS-1$
        query.setParameter("id", vilkårResultatId); //$NON-NLS-1$

        if (readOnly) {
            // returneres read-only, kan kun legge til nye ved skriving uten å oppdatere
            query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        }
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private Optional<Opptjening> deaktivereTidligereOpptjening(Long vilkårResultatId, boolean readOnly) {
        Optional<Opptjening> opptjening = hentTidligereOpptjening(vilkårResultatId, readOnly);
        if (opptjening.isPresent()) {
            Query query = em.createNativeQuery("UPDATE OPPTJENING SET AKTIV = 'N' WHERE ID=:id"); //$NON-NLS-1$
            query.setParameter("id", opptjening.get().getId()); //$NON-NLS-1$
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

        Function<Opptjening, Opptjening> oppdateringsfunksjon = (tidligereOpptjening) -> {
            // lager ny opptjening alltid ved ny opptjeningsperiode.
            Opptjening nyOpptjening = new Opptjening(opptjeningFom, opptjeningTom);
            if (skalBevareResultat) {
                Set<OpptjeningAktivitet> kopiListe = duplikatSjekk(tidligereOpptjening.getOpptjeningAktivitet());
                nyOpptjening.setOpptjentPeriode(tidligereOpptjening.getOpptjentPeriode());
                nyOpptjening.setOpptjeningAktivitet(kopiListe);
            }
            return nyOpptjening;
        };

        return lagre(behandling, oppdateringsfunksjon);
    }

    public void deaktiverOpptjening(Behandling behandling) {
        VilkårResultat vilkårResultat = validerRiktigBehandling(behandling);

        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        deaktivereTidligereOpptjening(vilkårResultat.getId(), false);
        em.flush();

        behandlingRepository.verifiserBehandlingLås(behandlingLås);
    }

    private Opptjening lagre(Behandling behandling, Function<Opptjening, Opptjening> oppdateringsfunksjon) {
        Long behandlingId = behandling.getId();
        VilkårResultat vilkårResultat = validerRiktigBehandling(behandling);

        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);


        Opptjening tidligereOpptjening = null;
        Opptjening opptjening;
        Optional<Opptjening> optTidligereOpptjening = deaktivereTidligereOpptjening(vilkårResultat.getId(), false);
        if (optTidligereOpptjening.isPresent()) {
            tidligereOpptjening = optTidligereOpptjening.get();
        }
        opptjening = oppdateringsfunksjon.apply(tidligereOpptjening);

        opptjening.setVilkårResultat(getBehandlingsresultat(behandlingId).getVilkårResultat());

        em.persist(opptjening);
        em.flush();

        behandlingRepository.verifiserBehandlingLås(behandlingLås);

        return opptjening;

    }

    /** Opptjening* Lagre opptjeningresultat (opptjent periode og aktiviteter).*/
    public Opptjening lagreOpptjeningResultat(Behandling behandling, Period opptjentPeriode,
                                              Collection<OpptjeningAktivitet> opptjeningAktiviteter) {

        Set<OpptjeningAktivitet> kopiListe = duplikatSjekk(opptjeningAktiviteter);

        Function<Opptjening, Opptjening> oppdateringsfunksjon = (tidligereOpptjening) -> {
            Opptjening ny = new Opptjening(tidligereOpptjening);
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
        Long orgBehandlingId = origBehandling.getId();
        Opptjening origOpptjening = hentTidligereOpptjening(getBehandlingsresultat(orgBehandlingId).getVilkårResultat().getId(), true)
            .orElseThrow(() -> new IllegalStateException("Original behandling har ikke opptjening."));

        lagreOpptjeningsperiode(nyBehandling, origOpptjening.getFom(), origOpptjening.getTom(), false);
        lagreOpptjeningResultat(nyBehandling, origOpptjening.getOpptjentPeriode(), origOpptjening.getOpptjeningAktivitet());
    }

    private Set<OpptjeningAktivitet> duplikatSjekk(Collection<OpptjeningAktivitet> opptjeningAktiviteter) {
        // ta kopi av alle aktiviteter for å være sikker på at gamle ikke skrives inn.
        if (opptjeningAktiviteter == null) {
            return Collections.emptySet();
        }
        Set<OpptjeningAktivitet> kopiListe = opptjeningAktiviteter.stream().map(oa -> new OpptjeningAktivitet(oa)).collect(Collectors.toCollection(LinkedHashSet::new));

        if (opptjeningAktiviteter.size() > kopiListe.size()) {
            // har duplikater!!
            Set<OpptjeningAktivitet> duplikater = opptjeningAktiviteter.stream()
                .filter(oa -> Collections.frequency(opptjeningAktiviteter, oa) > 1)
                .collect(Collectors.toCollection(LinkedHashSet::new));

            throw new IllegalArgumentException(
                "Utvikler-feil: har duplikate opptjeningsaktiviteter: [" + duplikater + "] i input: " + opptjeningAktiviteter); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return kopiListe;
    }

}
