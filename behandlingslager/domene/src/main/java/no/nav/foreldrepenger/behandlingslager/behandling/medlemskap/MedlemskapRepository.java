package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.util.Collection;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.TraverseEntityGraphFactory;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.diff.DiffEntity;
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
        return getAktivtBehandlingsgrunnlag(behandlingId).map(this::hentMedlemskap);
    }

    public Optional<VurdertMedlemskapPeriodeEntitet> hentVurdertLøpendeMedlemskap(Long behandlingId) {
        return hentMedlemskap(behandlingId).flatMap(MedlemskapAggregat::getVurderingLøpendeMedlemskap);
    }

    public VurdertMedlemskapPeriodeEntitet.Builder hentBuilderFor(Long behandlingId) {
        var vurdertMedlemskapPeriode = hentVurdertLøpendeMedlemskap(behandlingId);

        return new VurdertMedlemskapPeriodeEntitet.Builder(vurdertMedlemskapPeriode);
    }

    /** Kopierer grunnlag fra en tidligere behandling.  Endrer ikke aggregater, en skaper nye referanser til disse. */
    public void kopierGrunnlagFraEksisterendeBehandling(Long eksisterendeBehandlingId, Long nyBehandlingId) {
        var nyLås = taLås(nyBehandlingId);
        var eksisterendeGrunnlag = getAktivtBehandlingsgrunnlag(eksisterendeBehandlingId);

        var nyttGrunnlag = MedlemskapBehandlingsgrunnlagEntitet.fra(eksisterendeGrunnlag, nyBehandlingId);

        lagreOgFlush(nyBehandlingId, Optional.empty(), nyttGrunnlag);
        oppdaterLås(nyLås);
    }

    public void kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(Long eksisterendeBehandlingId, Long nyBehandlingId) {
        var nyLås = taLås(nyBehandlingId);
        var eksisterendeGrunnlag = getAktivtBehandlingsgrunnlag(eksisterendeBehandlingId);

        var nyttGrunnlag = MedlemskapBehandlingsgrunnlagEntitet.forRevurdering(eksisterendeGrunnlag,
            nyBehandlingId);

        lagreOgFlush(nyBehandlingId, Optional.empty(), nyttGrunnlag);
        oppdaterLås(nyLås);
    }

    /** Lagre registrert opplysninger om medlemskap (fra MEDL). Merk at implementasjonen står fritt til å ta en kopi av oppgitte data.*/
    public void lagreMedlemskapRegisterOpplysninger(Long behandlingId, Collection<MedlemskapPerioderEntitet> registrertMedlemskap) {
        var lås = taLås(behandlingId);
        var gr = getAktivtBehandlingsgrunnlag(behandlingId);
        var data = kopierOgLagreHvisEndret(gr, registrertMedlemskap);
        var nyttGrunnlag = MedlemskapBehandlingsgrunnlagEntitet.fra(gr, behandlingId, data);
        lagreOgFlush(behandlingId, gr, nyttGrunnlag);
        oppdaterLås(lås);
    }

    protected BehandlingLås taLås(Long behandlingId) {
        return behandlingLåsRepository.taLås(behandlingId);
    }

    public void lagreMedlemskapRegistrert(MedlemskapRegistrertEntitet ny) {
        entityManager.persist(ny);
        entityManager.flush();
    }

    /** Lagre vurderte opplysninger om meldemskap slik det har blitt gjort av Saksbehandler eller av systemet automatisk. Merk at implementasjonen står fritt til å ta en kopi av oppgitte data.*/
    public void lagreMedlemskapVurdering(Long behandlingId, VurdertMedlemskap vurdertMedlemskap) {
        var lås = taLås(behandlingId);
        var gr = getAktivtBehandlingsgrunnlag(behandlingId);
        var data = kopierOgLagreHvisEndret(gr, vurdertMedlemskap);
        var nyttGrunnlag = MedlemskapBehandlingsgrunnlagEntitet.fra(gr, behandlingId, data);
        lagreOgFlush(behandlingId, gr, nyttGrunnlag);
        oppdaterLås(lås);
    }

    protected void lagreOgFlush(Long behandlingId,
                                Optional<MedlemskapBehandlingsgrunnlagEntitet> tidligereGrunnlagOpt,
                                MedlemskapBehandlingsgrunnlagEntitet nyttGrunnlag) {


        if (tidligereGrunnlagOpt.isPresent()) {
            var tidligereGrunnlag = tidligereGrunnlagOpt.get();
            var erForskjellig = medlemskapAggregatDiffer(false).areDifferent(tidligereGrunnlag, nyttGrunnlag);
            if (erForskjellig) {
                tidligereGrunnlag.setAktiv(false);
                entityManager.persist(tidligereGrunnlag);
                entityManager.flush();
                entityManager.persist(nyttGrunnlag);
            } else {
                return;
            }

        } else {
            entityManager.persist(nyttGrunnlag);
        }

        entityManager.flush();
    }

    /** Lagre oppgitte opplysninger om oppgitt tilknytning slik det kan brukes i vurdering av Medlemskap. Merk at implementasjonen står fritt til å ta en kopi av oppgitte data.*/
    public void lagreOppgittTilkytning(Long behandlingId, MedlemskapOppgittTilknytningEntitet oppgittTilknytning) {
        var lås = taLås(behandlingId);
        var gr = getAktivtBehandlingsgrunnlag(behandlingId);
        var data = kopierHvisEndretOgLagre(gr, oppgittTilknytning);
        var nyttGrunnlag = MedlemskapBehandlingsgrunnlagEntitet.fra(gr, behandlingId, data);
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
        var gr = getAktivtBehandlingsgrunnlag(behandlingId);
        var nyttGrunnlag = MedlemskapBehandlingsgrunnlagEntitet.fra(gr, behandlingId,
            (VurdertMedlemskapEntitet) null);
        lagreOgFlush(behandlingId, gr, nyttGrunnlag);
        entityManager.flush();
    }

    protected void oppdaterLås(BehandlingLås lås) {
        behandlingLåsRepository.oppdaterLåsVersjon(lås);
    }

    public void lagreVurdertMedlemskap(VurdertMedlemskapEntitet ny) {
        entityManager.persist(ny);
        entityManager.flush();
    }

    public void lagreOppgittTilknytning(MedlemskapOppgittTilknytningEntitet ny) {
        entityManager.persist(ny);
        entityManager.flush();
    }

    private MedlemskapAggregat hentMedlemskap(MedlemskapBehandlingsgrunnlagEntitet grunnlag) {
        return grunnlag.tilAggregat();
    }

    private MedlemskapOppgittTilknytningEntitet kopierHvisEndretOgLagre(
                                                                        Optional<MedlemskapBehandlingsgrunnlagEntitet> gr,
                                                                        MedlemskapOppgittTilknytningEntitet oppgittTilknytning) {

        var ny = new MedlemskapOppgittTilknytningEntitet(oppgittTilknytning);
        if (gr.isPresent()) {
            var eksisterende = gr.get().getOppgittTilknytning();
            var erForskjellig = medlemskapAggregatDiffer(false).areDifferent(eksisterende, ny);
            if (erForskjellig) {
                lagreOppgittTilknytning(ny);
                return ny;
            }
            return eksisterende;
        }
        lagreOppgittTilknytning(ny);
        return ny;
    }

    private MedlemskapRegistrertEntitet kopierOgLagreHvisEndret(Optional<MedlemskapBehandlingsgrunnlagEntitet> gr,
                                                                Collection<MedlemskapPerioderEntitet> registrertMedlemskapPerioder) {
        var ny = new MedlemskapRegistrertEntitet(registrertMedlemskapPerioder);

        if (gr.isPresent()) {
            var eksisterende = gr.get().getRegisterMedlemskap();
            var erForskjellig = medlemskapAggregatDiffer(false).areDifferent(eksisterende, ny);
            if (erForskjellig) {
                lagreMedlemskapRegistrert(ny);
                return ny;
            }
            return eksisterende;
        }
        lagreMedlemskapRegistrert(ny);
        return ny;
    }

    private VurdertMedlemskapEntitet kopierOgLagreHvisEndret(Optional<MedlemskapBehandlingsgrunnlagEntitet> gr,
                                                             VurdertMedlemskap vurdertMedlemskap) {

        var ny = new VurdertMedlemskapEntitet(vurdertMedlemskap);
        if (gr.isPresent()) {
            var eksisterende = gr.get().getVurderingMedlemskapSkjæringstidspunktet();
            var erForskjellig = medlemskapAggregatDiffer(false).areDifferent(eksisterende, ny);
            if (erForskjellig) {
                lagreVurdertMedlemskap(ny);
                return ny;
            }
            return eksisterende;
        }
        lagreVurdertMedlemskap(ny);
        return ny;

    }

    private VurdertMedlemskapPeriodeEntitet kopierOgLagreHvisEndret(Optional<MedlemskapBehandlingsgrunnlagEntitet> gr,
                                                                    VurdertMedlemskapPeriodeEntitet løpendeMedlemskap) {
        var ny = new VurdertMedlemskapPeriodeEntitet(løpendeMedlemskap);

        if (gr.isPresent()) {
            var eksisterende = gr.get().getVurderingLøpendeMedlemskap();
            var erForskjellig = medlemskapAggregatDiffer(false).areDifferent(eksisterende, ny);
            if (erForskjellig) {
                lagreVurdertLøpendeMedlemskap(ny);
                return ny;
            }
            return eksisterende;
        }
        lagreVurdertLøpendeMedlemskap(ny);
        return ny;
    }

    private void lagreVurdertLøpendeMedlemskap(VurdertMedlemskapPeriodeEntitet ny) {
        entityManager.persist(ny);
        ny.getPerioder().forEach(vurdertLøpendeMedlemskap -> entityManager.persist(vurdertLøpendeMedlemskap));
    }

    private DiffEntity medlemskapAggregatDiffer(boolean medOnlyCheckTrackedFields) {
        var traverser = TraverseEntityGraphFactory.build(medOnlyCheckTrackedFields);
        return new DiffEntity(traverser);
    }

    protected Optional<MedlemskapBehandlingsgrunnlagEntitet> getAktivtBehandlingsgrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "SELECT mbg FROM MedlemskapBehandlingsgrunnlag mbg WHERE mbg.behandlingId = :behandling_id AND mbg.aktiv = true",
            MedlemskapBehandlingsgrunnlagEntitet.class)
                .setParameter("behandling_id", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    protected Optional<MedlemskapBehandlingsgrunnlagEntitet> getInitiellVersjonAvBehandlingsgrunnlag(Long behandlingId) {
        // må også sortere på id da opprettetTidspunkt kun er til nærmeste millisekund og ikke satt fra db.
        var query = entityManager.createQuery(
            "SELECT mbg FROM MedlemskapBehandlingsgrunnlag mbg WHERE mbg.behandlingId = :behandling_id ORDER BY mbg.opprettetTidspunkt, mbg.id",
            MedlemskapBehandlingsgrunnlagEntitet.class)
                .setParameter("behandling_id", behandlingId)
                .setMaxResults(1);

        return query.getResultStream().findFirst();
    }

    /** Henter førsteversjon av aggregat for Medlemskap, hvis det eksisterer. */
    public Optional<MedlemskapAggregat> hentFørsteVersjonAvMedlemskap(Long behandlingId) {
        return getInitiellVersjonAvBehandlingsgrunnlag(behandlingId).map(this::hentMedlemskap);
    }

    public Optional<Long> hentIdPåAktivMedlemskap(Long behandlingId) {
        return getAktivtBehandlingsgrunnlag(behandlingId)
                .map(MedlemskapBehandlingsgrunnlagEntitet::getId);
    }

    public MedlemskapAggregat hentMedlemskapPåId(Long aggregatId) {
        var optGrunnlag = hentGrunnlagPåId(
            aggregatId);
        return optGrunnlag.isPresent() ? optGrunnlag.get().tilAggregat() : null;
    }

    /** Lagre vurderte opplysninger om løpende meldemskap slik det har blitt gjort av Saksbehandler eller av systemet automatisk. Merk at implementasjonen står fritt til å ta en kopi av oppgitte data.*/
    public void lagreLøpendeMedlemskapVurdering(Long behandlingId, VurdertMedlemskapPeriodeEntitet løpendeMedlemskap) {
        var lås = taLås(behandlingId);
        var gr = getAktivtBehandlingsgrunnlag(behandlingId);
        var data = kopierOgLagreHvisEndret(gr, løpendeMedlemskap);
        var nyttGrunnlag = MedlemskapBehandlingsgrunnlagEntitet.fra(gr, behandlingId, data);
        lagreOgFlush(behandlingId, gr, nyttGrunnlag);
        oppdaterLås(lås);
    }

    public Optional<MedlemskapBehandlingsgrunnlagEntitet> hentGrunnlagPåId(Long grunnlagId) {
        var query = entityManager.createQuery(
            "SELECT mbg FROM MedlemskapBehandlingsgrunnlag mbg WHERE mbg.id = :grunnlagId",
            MedlemskapBehandlingsgrunnlagEntitet.class)
                .setParameter("grunnlagId", grunnlagId);
        return query.getResultStream().findFirst();
    }
}
