package no.nav.foreldrepenger.web;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok.OpptjeningIUtlandDokStatusRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelsemottakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;


//TODO palfi fjern
public class RepositoryAwareTest extends EntityManagerAwareTest {

    protected BehandlingRepositoryProvider repositoryProvider;
    protected SvangerskapspengerRepository svangerskapspengerRepository;
    protected FamilieHendelseRepository familieHendelseRepository;
    protected BehandlingRepository behandlingRepository;
    protected YtelsesFordelingRepository ytelsesfordelingRepository;
    protected ProsessTaskRepository prosessTaskRepository;
    protected PersonopplysningRepository personopplysningRepository;
    protected MedlemskapRepository medlemskapRepository;
    protected YtelsesFordelingRepository ytelsesFordelingRepository;
    protected OpptjeningIUtlandDokStatusRepository opptjeningIUtlandDokStatusRepository;
    protected VergeRepository vergeRepository;
    protected EntityManager entityManager;
    protected BehandlingLåsRepository behandlingLåsRepository;
    protected FagsakRepository fagsakRepository;
    protected MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;
    protected HistorikkRepository historikkRepository;
    protected SøknadRepository søknadRepository;
    protected FagsakRelasjonRepository fagsakRelasjonRepository;
    protected FpUttakRepository fpUttakRepository;
    protected UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    protected BehandlingVedtakRepository behandlingVedtakRepository;
    protected OpptjeningRepository opptjeningRepository;
    protected BeregningsresultatRepository beregningsresultatRepository;
    protected MottatteDokumentRepository mottatteDokumentRepository;
    protected BehandlingRevurderingRepository behandlingRevurderingRepository;
    protected BehandlingsresultatRepository behandlingsresultatRepository;
    protected SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;
    protected FagsakLåsRepository fagsakLåsRepository;
    protected HendelsemottakRepository hendelsemottakRepository;
    protected BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    @BeforeEach
    public void beforeEach() {
        entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        hendelsemottakRepository = new HendelsemottakRepository(entityManager);
        beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(entityManager);
        svangerskapspengerRepository = new SvangerskapspengerRepository(entityManager);
        familieHendelseRepository = new FamilieHendelseRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        ytelsesfordelingRepository = new YtelsesFordelingRepository(entityManager);
        personopplysningRepository = new PersonopplysningRepository(entityManager);
        medlemskapRepository = new MedlemskapRepository(entityManager);
        ytelsesFordelingRepository = new YtelsesFordelingRepository(entityManager);
        opptjeningIUtlandDokStatusRepository = new OpptjeningIUtlandDokStatusRepository(entityManager);
        vergeRepository = new VergeRepository(entityManager, behandlingLåsRepository);
        behandlingLåsRepository = new BehandlingLåsRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        medlemskapVilkårPeriodeRepository = new MedlemskapVilkårPeriodeRepository(entityManager);
        historikkRepository = new HistorikkRepository(entityManager);
        søknadRepository = new SøknadRepository(entityManager, behandlingRepository);
        fagsakRelasjonRepository = new FagsakRelasjonRepository(entityManager, ytelsesFordelingRepository, fagsakLåsRepository);
        fpUttakRepository = new FpUttakRepository(entityManager);
        uttaksperiodegrenseRepository = new UttaksperiodegrenseRepository(entityManager);
        behandlingVedtakRepository = new BehandlingVedtakRepository(entityManager);
        opptjeningRepository = new OpptjeningRepository(entityManager, behandlingRepository);
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
        mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);
        behandlingRevurderingRepository = new BehandlingRevurderingRepository(entityManager, behandlingRepository, fagsakRelasjonRepository,
                søknadRepository, behandlingLåsRepository);
        behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);
        svangerskapspengerUttakResultatRepository = new SvangerskapspengerUttakResultatRepository(entityManager);
        fagsakLåsRepository = new FagsakLåsRepository(entityManager);
        prosessTaskRepository = new ProsessTaskRepositoryImpl(entityManager, null, null);
    }

}
