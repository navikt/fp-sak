package no.nav.foreldrepenger.domene.uttak;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;

/**
 * Provider for å enklere å kunne hente ut ulike repository uten for mange injection points.
 */
@ApplicationScoped
public class UttakRepositoryProvider {

    private EntityManager entityManager;
    private FagsakRepository fagsakRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;

    private FpUttakRepository fpUttakRepository;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;

    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;

    UttakRepositoryProvider() {
        // for CDI proxy
    }

    @Inject
    public UttakRepositoryProvider( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;

        // fp spesifikke behandling aggregater
        this.ytelsesFordelingRepository = new YtelsesFordelingRepository(entityManager);

        // behandling repositories
        this.fagsakRepository = new FagsakRepository(entityManager);
        this.fagsakRelasjonRepository = new FagsakRelasjonRepository(entityManager, ytelsesFordelingRepository,
            new FagsakLåsRepository(entityManager));

        // behandling aggregater
        this.fpUttakRepository = new FpUttakRepository(entityManager);
        this.uttaksperiodegrenseRepository = new UttaksperiodegrenseRepository(entityManager);
        this.behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);

        this.svangerskapspengerUttakResultatRepository = new SvangerskapspengerUttakResultatRepository(entityManager);
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public FagsakRepository getFagsakRepository() {
        // bridge metode før sammenkobling medBehandling
        return fagsakRepository;
    }

    public FagsakRelasjonRepository getFagsakRelasjonRepository() {
        return fagsakRelasjonRepository;
    }

    public FpUttakRepository getFpUttakRepository() {
        return fpUttakRepository;
    }

    public YtelsesFordelingRepository getYtelsesFordelingRepository() {
        return ytelsesFordelingRepository;
    }

    public BehandlingsresultatRepository getBehandlingsresultatRepository() {
        return behandlingsresultatRepository;
    }

    public SvangerskapspengerUttakResultatRepository getSvangerskapspengerUttakResultatRepository() {
        return svangerskapspengerUttakResultatRepository;
    }

    public UttaksperiodegrenseRepository getUttaksperiodegrenseRepository() {
        return uttaksperiodegrenseRepository;
    }
}
