package no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling;

import java.util.Objects;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.KodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetRepository;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@Dependent
public class IAYRepositoryProvider {

    private EntityManager entityManager;
    private FagsakRepository fagsakRepository;
    private KodeverkRepository kodeverkRepository;
    private PersonopplysningRepository personopplysningRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private SøknadRepository søknadRepository;
    private LegacyESBeregningRepository beregningRepository;
    private VirksomhetRepository virksomhetRepository;
    private OpptjeningRepository opptjeningRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;

    @Inject
    public IAYRepositoryProvider(@VLPersistenceUnit EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;

        // kodeverk
        this.kodeverkRepository = new KodeverkRepository(entityManager);

        // behandling repositories
        this.behandlingRepository = new BehandlingRepository(entityManager);
        this.fagsakRepository = new FagsakRepository(entityManager);

        // behandling aggregater
        this.familieHendelseRepository = new FamilieHendelseRepository(entityManager);
        this.opptjeningRepository = new OpptjeningRepository(entityManager, this.behandlingRepository);
        this.personopplysningRepository = new PersonopplysningRepository(entityManager);
        this.søknadRepository = new SøknadRepository(entityManager, this.behandlingRepository);

        // inntekt arbeid ytelser
        this.virksomhetRepository = new VirksomhetRepository();

        // behandling resultat aggregater
        this.beregningRepository = new LegacyESBeregningRepository(entityManager, getBehandlingRepository());
        this.beregningsresultatRepository = new BeregningsresultatRepository(entityManager);

        // behandling støtte repositories
        this.mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);

    }

    public BehandlingRepository getBehandlingRepository() {
        return behandlingRepository;
    }

    public FagsakRepository getFagsakRepository() {
        // bridge metode før sammenkobling medBehandling
        return fagsakRepository;
    }

    public KodeverkRepository getKodeverkRepository() {
        return kodeverkRepository;
    }

    public MottatteDokumentRepository getMottatteDokumentRepository() {
        return mottatteDokumentRepository;
    }

    public OpptjeningRepository getOpptjeningRepository() {
        return opptjeningRepository;
    }

    public SøknadRepository getSøknadRepository() {
        return søknadRepository;
    }

    public VirksomhetRepository getVirksomhetRepository() {
        return virksomhetRepository;
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    LegacyESBeregningRepository getBeregningRepository() {
        return beregningRepository;
    }

    public BeregningsresultatRepository getBeregningsresultatRepository() {
        return beregningsresultatRepository;
    }

    public FamilieHendelseRepository getFamilieGrunnlagRepository() {
        return familieHendelseRepository;
    }

    InntektArbeidYtelseTjeneste getInntektArbeidYtelseTjeneste() {
        return new AbakusInMemoryInntektArbeidYtelseTjeneste();
    }

    PersonopplysningRepository getPersonopplysningRepository() {
        return personopplysningRepository;
    }
}
