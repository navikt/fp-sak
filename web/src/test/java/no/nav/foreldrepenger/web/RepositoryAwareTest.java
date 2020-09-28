package no.nav.foreldrepenger.web;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

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
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
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
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        hendelsemottakRepository = new HendelsemottakRepository(getEntityManager());
        beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(getEntityManager());
        svangerskapspengerRepository = new SvangerskapspengerRepository(getEntityManager());
        familieHendelseRepository = new FamilieHendelseRepository(getEntityManager());
        behandlingRepository = new BehandlingRepository(getEntityManager());
        ytelsesfordelingRepository = new YtelsesFordelingRepository(getEntityManager());
        personopplysningRepository = new PersonopplysningRepository(getEntityManager());
        medlemskapRepository = new MedlemskapRepository(getEntityManager());
        ytelsesFordelingRepository = new YtelsesFordelingRepository(getEntityManager());
        opptjeningIUtlandDokStatusRepository = new OpptjeningIUtlandDokStatusRepository(getEntityManager());
        vergeRepository = new VergeRepository(getEntityManager(), behandlingLåsRepository);
        behandlingLåsRepository = new BehandlingLåsRepository(getEntityManager());
        fagsakRepository = new FagsakRepository(getEntityManager());
        medlemskapVilkårPeriodeRepository = new MedlemskapVilkårPeriodeRepository(getEntityManager());
        historikkRepository = new HistorikkRepository(getEntityManager());
        søknadRepository = new SøknadRepository(getEntityManager(), behandlingRepository);
        fagsakRelasjonRepository = new FagsakRelasjonRepository(getEntityManager(), ytelsesFordelingRepository, fagsakLåsRepository);
        fpUttakRepository = new FpUttakRepository(getEntityManager());
        uttaksperiodegrenseRepository = new UttaksperiodegrenseRepository(getEntityManager());
        behandlingVedtakRepository = new BehandlingVedtakRepository(getEntityManager());
        opptjeningRepository = new OpptjeningRepository(getEntityManager(), behandlingRepository);
        beregningsresultatRepository = new BeregningsresultatRepository(getEntityManager());
        mottatteDokumentRepository = new MottatteDokumentRepository(getEntityManager());
        behandlingRevurderingRepository = new BehandlingRevurderingRepository(getEntityManager(), behandlingRepository, fagsakRelasjonRepository,
                søknadRepository, behandlingLåsRepository);
        behandlingsresultatRepository = new BehandlingsresultatRepository(getEntityManager());
        svangerskapspengerUttakResultatRepository = new SvangerskapspengerUttakResultatRepository(getEntityManager());
        fagsakLåsRepository = new FagsakLåsRepository(getEntityManager());
        prosessTaskRepository = new ProsessTaskRepositoryImpl(getEntityManager(), null, null);
    }

}
