package no.nav.foreldrepenger.behandlingslager.behandling.repository;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapsvilkårVurderingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;

/**
 * Provider for å enklere å kunne hente ut ulike repository uten for mange injection points.
 *
 * NB: Behandlingsgrunnlag skal ikke ligge her - men i BehandlingGrunnlagRepositoryProvider
 * Forøvrig - inject enkeltvis heller enn å bruke denne
 */
@ApplicationScoped
public class BehandlingRepositoryProvider {

    private EntityManager entityManager;
    private BehandlingLåsRepository behandlingLåsRepository;
    private FagsakRepository fagsakRepository;
    private PersonopplysningRepository personopplysningRepository;
    private MedlemskapRepository medlemskapRepository;
    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;
    private MedlemskapsvilkårVurderingRepository medlemskapsvilkårVurderingRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private HistorikkRepository historikkRepository;
    private SøknadRepository søknadRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FpUttakRepository fpUttakRepository;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private OpptjeningRepository opptjeningRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;

    private BehandlingRepository behandlingRepository;
    private FagsakLåsRepository fagsakLåsRepository;

    BehandlingRepositoryProvider() {
        // for CDI proxy
    }

    @Inject
    public BehandlingRepositoryProvider(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;

        // behandling repositories
        this.behandlingRepository = new BehandlingRepository(entityManager);
        this.behandlingLåsRepository = new BehandlingLåsRepository(entityManager);
        this.fagsakRepository = new FagsakRepository(entityManager);
        this.fagsakLåsRepository = new FagsakLåsRepository(entityManager);

        // behandling grunnlagaggregater - flyttes til BehandlingGrunnlagRepositoryProvider
        this.personopplysningRepository = new PersonopplysningRepository(entityManager);
        this.søknadRepository = new SøknadRepository(entityManager, this.behandlingRepository);
        this.familieHendelseRepository = new FamilieHendelseRepository(entityManager);
        this.medlemskapRepository = new MedlemskapRepository(entityManager);
        this.ytelsesFordelingRepository = new YtelsesFordelingRepository(entityManager);

        // behandling mellomresultat
        this.medlemskapVilkårPeriodeRepository = new MedlemskapVilkårPeriodeRepository(entityManager);
        this.opptjeningRepository = new OpptjeningRepository(entityManager, this.behandlingRepository);
        this.fpUttakRepository = new FpUttakRepository(entityManager);
        this.uttaksperiodegrenseRepository = new UttaksperiodegrenseRepository(entityManager);
        this.behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);
        this.medlemskapsvilkårVurderingRepository = new MedlemskapsvilkårVurderingRepository(entityManager, behandlingsresultatRepository);

        // behandling resultat aggregater
        this.beregningsresultatRepository = new BeregningsresultatRepository(entityManager);

        // behandling støtte repositories
        this.mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);
        this.historikkRepository = new HistorikkRepository(entityManager);
        this.behandlingVedtakRepository = new BehandlingVedtakRepository(entityManager);

        this.svangerskapspengerUttakResultatRepository = new SvangerskapspengerUttakResultatRepository(entityManager);

        // Må gjøres til slutt pga deps
        this.fagsakRelasjonRepository = new FagsakRelasjonRepository(entityManager, fagsakLåsRepository);


        // ********
        // VIS HENSYN - IKKE FORSØPLE MER FLERE REPOS HER. DET SKAPER AVHENGIGHETER. TA GJERNE NOEN UT HVIS DU SER DETTE.
        // ********
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public BehandlingRepository getBehandlingRepository() {
        return behandlingRepository;
    }

    public PersonopplysningRepository getPersonopplysningRepository() {
        return personopplysningRepository;
    }

    public MedlemskapRepository getMedlemskapRepository() {
        return medlemskapRepository;
    }

    public MedlemskapVilkårPeriodeRepository getMedlemskapVilkårPeriodeRepository() {
        return medlemskapVilkårPeriodeRepository;
    }

    public MedlemskapsvilkårVurderingRepository getMedlemskapsvilkårVurderingRepository() {
        return medlemskapsvilkårVurderingRepository;
    }

    public BehandlingLåsRepository getBehandlingLåsRepository() {
        return behandlingLåsRepository;
    }

    public FagsakRepository getFagsakRepository() {
        // bridge metode før sammenkobling medBehandling
        return fagsakRepository;
    }

    public FamilieHendelseRepository getFamilieHendelseRepository() {
        return familieHendelseRepository;
    }

    public HistorikkRepository getHistorikkRepository() {
        return historikkRepository;
    }

    public SøknadRepository getSøknadRepository() {
        return søknadRepository;
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

    public BehandlingVedtakRepository getBehandlingVedtakRepository() {
        return behandlingVedtakRepository;
    }

    public OpptjeningRepository getOpptjeningRepository() {
        return opptjeningRepository;
    }

    public BeregningsresultatRepository getBeregningsresultatRepository() {
        return beregningsresultatRepository;
    }

    public MottatteDokumentRepository getMottatteDokumentRepository() {
        return mottatteDokumentRepository;
    }

    public FagsakLåsRepository getFagsakLåsRepository() {
        return fagsakLåsRepository;
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
