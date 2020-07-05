package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.TraverseEntityGraphFactory;
import no.nav.foreldrepenger.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.diff.DiffEntity;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseGraph;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

/**
 * Dette er et Repository for håndtering av alle persistente endringer i en Medlemskap for søker.
 * <p>
 * Hent opp og lagre innhentende Medlemskap data, fra søknad, register (MEDL) eller som avklart av Saksbehandler.
 * Ved hver endring kopieres Medlemskap grafen (inklusiv oppgitt tilknytning og utenlandsopphold) som et felles
 * Aggregat (ref. Domain Driven Design - Aggregat pattern)
 * <p>
 * <p>
 * Merk: standard regler - et Grunnlag eies av en Behandling. Et Aggregat (Søkers Medlemskap graf) har en
 * selvstenig livssyklus og vil kopieres ved hver endring.
 * Ved multiple endringer i et grunnlat for en Behandling vil alltid kun et innslag i grunnlag være aktiv for angitt
 * Behandling.
 */
@ApplicationScoped
public class MedlemskapRepository {

    private EntityManager entityManager;
    private BehandlingLåsRepository behandlingLåsRepository;

    public MedlemskapRepository() {
        // FOR CDI
    }

    @Inject
    public MedlemskapRepository( EntityManager entityManager) {
        this.entityManager = entityManager;
        this.behandlingLåsRepository = new BehandlingLåsRepository(entityManager);
    }

    /** Henter et aggregat for Medlemskap, hvis det eksisterer, basert på kun behandlingId */
    public Optional<MedlemskapAggregat> hentMedlemskap(Long behandlingId) {
        Optional<MedlemskapBehandlingsgrunnlagEntitet> optGrunnlag = getAktivtBehandlingsgrunnlag(behandlingId);
        return hentMedlemskap(optGrunnlag);
    }

    /**
     * Hent kun {@link VurdertMedlemskap} fra Behandling.
     */
    public Optional<VurdertMedlemskap> hentVurdertMedlemskap(Long behandlingId) {
        Optional<MedlemskapAggregat> medlemskap = hentMedlemskap(behandlingId);
        if (medlemskap.isPresent()) {
            return medlemskap.get().getVurdertMedlemskap();
        } else {
            return Optional.empty();
        }
    }

    public Optional<VurdertMedlemskapPeriodeEntitet> hentVurdertLøpendeMedlemskap(Long behandlingId) {
        Optional<MedlemskapAggregat> medlemskap = hentMedlemskap(behandlingId);
        if (medlemskap.isPresent()) {
            return medlemskap.get().getVurderingLøpendeMedlemskap();
        } else {
            return Optional.empty();
        }
    }

    public VurdertMedlemskapPeriodeEntitet.Builder hentBuilderFor(Long behandlingId) {
        Optional<VurdertMedlemskapPeriodeEntitet> vurdertMedlemskapPeriode = hentVurdertLøpendeMedlemskap(behandlingId);

        return new VurdertMedlemskapPeriodeEntitet.Builder(vurdertMedlemskapPeriode);
    }

    /** Kopierer grunnlag fra en tidligere behandling.  Endrer ikke aggregater, en skaper nye referanser til disse. */
    public void kopierGrunnlagFraEksisterendeBehandling(Long eksisterendeBehandlingId, Long nyBehandlingId) {
        final BehandlingLås nyLås = taLås(nyBehandlingId);
        Optional<MedlemskapBehandlingsgrunnlagEntitet> eksisterendeGrunnlag = getAktivtBehandlingsgrunnlag(eksisterendeBehandlingId);

        MedlemskapBehandlingsgrunnlagEntitet nyttGrunnlag = MedlemskapBehandlingsgrunnlagEntitet.fra(eksisterendeGrunnlag, nyBehandlingId);

        lagreOgFlush(nyBehandlingId, Optional.empty(), nyttGrunnlag);
        oppdaterLås(nyLås);
    }

    public void kopierGrunnlagFraEksisterendeBehandlingForRevurdering(Long eksisterendeBehandlingId, Long nyBehandlingId) {
        final BehandlingLås nyLås = taLås(nyBehandlingId);
        Optional<MedlemskapBehandlingsgrunnlagEntitet> eksisterendeGrunnlag = getAktivtBehandlingsgrunnlag(eksisterendeBehandlingId);

        MedlemskapBehandlingsgrunnlagEntitet nyttGrunnlag = MedlemskapBehandlingsgrunnlagEntitet.forRevurdering(eksisterendeGrunnlag,
            nyBehandlingId);

        lagreOgFlush(nyBehandlingId, Optional.empty(), nyttGrunnlag);
        oppdaterLås(nyLås);
    }

    /** Lagre registrert opplysninger om medlemskap (fra MEDL). Merk at implementasjonen står fritt til å ta en kopi av oppgitte data.*/
    public void lagreMedlemskapRegisterOpplysninger(Long behandlingId, Collection<MedlemskapPerioderEntitet> registrertMedlemskap) {
        final BehandlingLås lås = taLås(behandlingId);
        Optional<MedlemskapBehandlingsgrunnlagEntitet> gr = getAktivtBehandlingsgrunnlag(behandlingId);
        MedlemskapRegistrertEntitet data = kopierOgLagreHvisEndret(gr, registrertMedlemskap);
        MedlemskapBehandlingsgrunnlagEntitet nyttGrunnlag = MedlemskapBehandlingsgrunnlagEntitet.fra(gr, behandlingId, data);
        lagreOgFlush(behandlingId, gr, nyttGrunnlag);
        oppdaterLås(lås);
    }

    protected BehandlingLås taLås(Long behandlingId) {
        return behandlingLåsRepository.taLås(behandlingId);
    }

    public void lagreMedlemskapRegistrert(MedlemskapRegistrertEntitet ny) {
        EntityManager em = getEntityManager();
        em.persist(ny);
        em.flush();
    }

    /** Lagre vurderte opplysninger om meldemskap slik det har blitt gjort av Saksbehandler eller av systemet automatisk. Merk at implementasjonen står fritt til å ta en kopi av oppgitte data.*/
    public void lagreMedlemskapVurdering(Long behandlingId, VurdertMedlemskap vurdertMedlemskap) {
        final BehandlingLås lås = taLås(behandlingId);
        Optional<MedlemskapBehandlingsgrunnlagEntitet> gr = getAktivtBehandlingsgrunnlag(behandlingId);
        VurdertMedlemskapEntitet data = kopierOgLagreHvisEndret(gr, vurdertMedlemskap);
        MedlemskapBehandlingsgrunnlagEntitet nyttGrunnlag = MedlemskapBehandlingsgrunnlagEntitet.fra(gr, behandlingId, data);
        lagreOgFlush(behandlingId, gr, nyttGrunnlag);
        oppdaterLås(lås);
    }

    protected void lagreOgFlush(@SuppressWarnings("unused") Long behandlingId,
                                Optional<MedlemskapBehandlingsgrunnlagEntitet> tidligereGrunnlagOpt,
                                MedlemskapBehandlingsgrunnlagEntitet nyttGrunnlag) {

        EntityManager em = getEntityManager();

        if (tidligereGrunnlagOpt.isPresent()) {
            MedlemskapBehandlingsgrunnlagEntitet tidligereGrunnlag = tidligereGrunnlagOpt.get();
            boolean erForskjellig = medlemskapAggregatDiffer(false).areDifferent(tidligereGrunnlag, nyttGrunnlag);
            if (erForskjellig) {
                tidligereGrunnlag.setAktiv(false);
                em.persist(tidligereGrunnlag);
                em.flush();
                em.persist(nyttGrunnlag);
            } else {
                return;
            }

        } else {
            em.persist(nyttGrunnlag);
        }

        em.flush();
    }

    /** Lagre oppgitte opplysninger om oppgitt tilknytning slik det kan brukes i vurdering av Medlemskap. Merk at implementasjonen står fritt til å ta en kopi av oppgitte data.*/
    public void lagreOppgittTilkytning(Long behandlingId, MedlemskapOppgittTilknytningEntitet oppgittTilknytning) {
        final BehandlingLås lås = taLås(behandlingId);
        Optional<MedlemskapBehandlingsgrunnlagEntitet> gr = getAktivtBehandlingsgrunnlag(behandlingId);
        MedlemskapOppgittTilknytningEntitet data = kopierHvisEndretOgLagre(gr, oppgittTilknytning);
        MedlemskapBehandlingsgrunnlagEntitet nyttGrunnlag = MedlemskapBehandlingsgrunnlagEntitet.fra(gr, behandlingId, data);
        lagreOgFlush(behandlingId, gr, nyttGrunnlag);
        oppdaterLås(lås);
    }

    /**
     * Slette avklart medlemskapsdata på en Behandling. Sørger for at samtidige oppdateringer på samme Behandling,
     * eller andre Behandlinger
     * på samme Fagsak ikke kan gjøres samtidig.
     *
     * @see BehandlingLås
     */
    public void slettAvklarteMedlemskapsdata(Long behandlingId, BehandlingLås lås) {
        oppdaterLås(lås);
        Optional<MedlemskapBehandlingsgrunnlagEntitet> gr = getAktivtBehandlingsgrunnlag(behandlingId);
        MedlemskapBehandlingsgrunnlagEntitet nyttGrunnlag = MedlemskapBehandlingsgrunnlagEntitet.fra(gr, behandlingId,
            (VurdertMedlemskapEntitet) null);
        lagreOgFlush(behandlingId, gr, nyttGrunnlag);
        getEntityManager().flush();
    }

    protected void oppdaterLås(BehandlingLås lås) {
        behandlingLåsRepository.oppdaterLåsVersjon(lås);
    }

    public void lagreVurdertMedlemskap(VurdertMedlemskapEntitet ny) {
        EntityManager em = getEntityManager();
        em.persist(ny);
        em.flush();
    }

    public void lagreOppgittTilknytning(MedlemskapOppgittTilknytningEntitet ny) {
        EntityManager em = getEntityManager();
        em.persist(ny);
        em.flush();
    }

    private EntityManager getEntityManager() {
        Objects.requireNonNull(entityManager, "entityManager ikke satt"); //$NON-NLS-1$
        return entityManager;
    }

    private Optional<MedlemskapAggregat> hentMedlemskap(Optional<MedlemskapBehandlingsgrunnlagEntitet> optGrunnlag) {
        if (optGrunnlag.isPresent()) {
            MedlemskapBehandlingsgrunnlagEntitet grunnlag = optGrunnlag.get();
            MedlemskapAggregat ma = grunnlag.tilAggregat();
            return Optional.of(ma);
        } else {
            return Optional.empty();
        }
    }

    private MedlemskapOppgittTilknytningEntitet kopierHvisEndretOgLagre(
                                                                        Optional<MedlemskapBehandlingsgrunnlagEntitet> gr, // NOSONAR
                                                                        MedlemskapOppgittTilknytningEntitet oppgittTilknytning) {

        MedlemskapOppgittTilknytningEntitet ny = new MedlemskapOppgittTilknytningEntitet(oppgittTilknytning);
        if (gr.isPresent()) {
            MedlemskapOppgittTilknytningEntitet eksisterende = gr.get().getOppgittTilknytning();
            boolean erForskjellig = medlemskapAggregatDiffer(false).areDifferent(eksisterende, ny);
            if (erForskjellig) {
                lagreOppgittTilknytning(ny);
                return ny;
            } else {
                return eksisterende;
            }
        } else {
            lagreOppgittTilknytning(ny);
            return ny;
        }
    }

    private MedlemskapRegistrertEntitet kopierOgLagreHvisEndret(Optional<MedlemskapBehandlingsgrunnlagEntitet> gr,
                                                                Collection<MedlemskapPerioderEntitet> registrertMedlemskapPerioder) {
        MedlemskapRegistrertEntitet ny = new MedlemskapRegistrertEntitet(registrertMedlemskapPerioder);

        if (gr.isPresent()) {
            MedlemskapRegistrertEntitet eksisterende = gr.get().getRegisterMedlemskap();
            boolean erForskjellig = medlemskapAggregatDiffer(false).areDifferent(eksisterende, ny);
            if (erForskjellig) {
                lagreMedlemskapRegistrert(ny);
                return ny;
            } else {
                return eksisterende;
            }
        } else {
            lagreMedlemskapRegistrert(ny);
            return ny;
        }
    }

    private VurdertMedlemskapEntitet kopierOgLagreHvisEndret(Optional<MedlemskapBehandlingsgrunnlagEntitet> gr,
                                                             VurdertMedlemskap vurdertMedlemskap) {

        VurdertMedlemskapEntitet ny = new VurdertMedlemskapEntitet(vurdertMedlemskap);
        if (gr.isPresent()) {
            VurdertMedlemskapEntitet eksisterende = gr.get().getVurderingMedlemskapSkjæringstidspunktet();
            boolean erForskjellig = medlemskapAggregatDiffer(false).areDifferent(eksisterende, ny);
            if (erForskjellig) {
                lagreVurdertMedlemskap(ny);
                return ny;
            } else {
                return eksisterende;
            }
        } else {
            lagreVurdertMedlemskap(ny);
            return ny;
        }

    }

    private VurdertMedlemskapPeriodeEntitet kopierOgLagreHvisEndret(Optional<MedlemskapBehandlingsgrunnlagEntitet> gr,
                                                                    VurdertMedlemskapPeriodeEntitet løpendeMedlemskap) {
        VurdertMedlemskapPeriodeEntitet ny = new VurdertMedlemskapPeriodeEntitet(løpendeMedlemskap);

        if (gr.isPresent()) {
            VurdertMedlemskapPeriodeEntitet eksisterende = gr.get().getVurderingLøpendeMedlemskap();
            boolean erForskjellig = medlemskapAggregatDiffer(false).areDifferent(eksisterende, ny);
            if (erForskjellig) {
                lagreVurdertLøpendeMedlemskap(ny);
                return ny;
            } else {
                return eksisterende;
            }
        } else {
            lagreVurdertLøpendeMedlemskap(ny);
            return ny;
        }
    }

    private void lagreVurdertLøpendeMedlemskap(VurdertMedlemskapPeriodeEntitet ny) {
        EntityManager em = getEntityManager();
        em.persist(ny);
        ny.getPerioder().forEach(vurdertLøpendeMedlemskap -> {
            VurdertLøpendeMedlemskapEntitet entitet = vurdertLøpendeMedlemskap;
            em.persist(entitet);
        });
    }

    private DiffEntity medlemskapAggregatDiffer(boolean medOnlyCheckTrackedFields) {
        TraverseGraph traverser = TraverseEntityGraphFactory.build(medOnlyCheckTrackedFields);
        return new DiffEntity(traverser);
    }

    protected Optional<MedlemskapBehandlingsgrunnlagEntitet> getAktivtBehandlingsgrunnlag(Long behandlingId) {
        TypedQuery<MedlemskapBehandlingsgrunnlagEntitet> query = getEntityManager().createQuery(
            "SELECT mbg FROM MedlemskapBehandlingsgrunnlag mbg WHERE mbg.behandlingId = :behandling_id AND mbg.aktiv = 'J'", //$NON-NLS-1$
            MedlemskapBehandlingsgrunnlagEntitet.class)
                .setParameter("behandling_id", behandlingId); //$NON-NLS-1$

        return HibernateVerktøy.hentUniktResultat(query);
    }

    protected Optional<MedlemskapBehandlingsgrunnlagEntitet> getInitilVersjonAvBehandlingsgrunnlag(Long behandlingId) {
        // må også sortere på id da opprettetTidspunkt kun er til nærmeste millisekund og ikke satt fra db.
        TypedQuery<MedlemskapBehandlingsgrunnlagEntitet> query = getEntityManager().createQuery(
            "SELECT mbg FROM MedlemskapBehandlingsgrunnlag mbg WHERE mbg.behandlingId = :behandling_id ORDER BY mbg.opprettetTidspunkt, mbg.id", //$NON-NLS-1$
            MedlemskapBehandlingsgrunnlagEntitet.class)
                .setParameter("behandling_id", behandlingId)
                .setMaxResults(1); // $NON-NLS-1$

        return query.getResultStream().findFirst();
    }

    /** Henter førsteversjon av aggregat for Medlemskap, hvis det eksisterer. */
    public Optional<MedlemskapAggregat> hentFørsteVersjonAvMedlemskap(Long behandlingId) {
        Optional<MedlemskapBehandlingsgrunnlagEntitet> optGrunnlag = getInitilVersjonAvBehandlingsgrunnlag(behandlingId);
        return hentMedlemskap(optGrunnlag);
    }

    public Optional<Long> hentIdPåAktivMedlemskap(Long behandlingId) {
        return getAktivtBehandlingsgrunnlag(behandlingId)
                .map(MedlemskapBehandlingsgrunnlagEntitet::getId);
    }

    public MedlemskapAggregat hentMedlemskapPåId(Long aggregatId) {
        Optional<MedlemskapBehandlingsgrunnlagEntitet> optGrunnlag = getVersjonAvMedlemskapGrunnlagPåId(
            aggregatId);
        return optGrunnlag.isPresent() ? optGrunnlag.get().tilAggregat() : null;
    }

    public DiffResult diffResultat(Long grunnlagId1, Long grunnlagId2, boolean onlyCheckTrackedFields) {
        MedlemskapBehandlingsgrunnlagEntitet grunnlag1 = getVersjonAvMedlemskapGrunnlagPåId(grunnlagId1)
                .orElseThrow(() -> new IllegalStateException("id1 ikke kjent"));
        MedlemskapBehandlingsgrunnlagEntitet grunnlag2 = getVersjonAvMedlemskapGrunnlagPåId(grunnlagId2)
                .orElseThrow(() -> new IllegalStateException("id2 ikke kjent"));
        return new RegisterdataDiffsjekker(onlyCheckTrackedFields).getDiffEntity().diff(grunnlag1, grunnlag2);
    }

    /** Lagre vurderte opplysninger om løpende meldemskap slik det har blitt gjort av Saksbehandler eller av systemet automatisk. Merk at implementasjonen står fritt til å ta en kopi av oppgitte data.*/
    public void lagreLøpendeMedlemskapVurdering(Long behandlingId, VurdertMedlemskapPeriodeEntitet løpendeMedlemskap) {
        final BehandlingLås lås = taLås(behandlingId);
        Optional<MedlemskapBehandlingsgrunnlagEntitet> gr = getAktivtBehandlingsgrunnlag(behandlingId);
        VurdertMedlemskapPeriodeEntitet data = kopierOgLagreHvisEndret(gr, løpendeMedlemskap);
        MedlemskapBehandlingsgrunnlagEntitet nyttGrunnlag = MedlemskapBehandlingsgrunnlagEntitet.fra(gr, behandlingId, data);
        lagreOgFlush(behandlingId, gr, nyttGrunnlag);
        oppdaterLås(lås);
    }

    private Optional<MedlemskapBehandlingsgrunnlagEntitet> getVersjonAvMedlemskapGrunnlagPåId(Long aggregatId) {
        TypedQuery<MedlemskapBehandlingsgrunnlagEntitet> query = getEntityManager().createQuery(
            "SELECT mbg FROM MedlemskapBehandlingsgrunnlag mbg WHERE mbg.id = :aggregatId", //$NON-NLS-1$
            MedlemskapBehandlingsgrunnlagEntitet.class)
                .setParameter("aggregatId", aggregatId);
        return query.getResultStream().findFirst();
    }
}
