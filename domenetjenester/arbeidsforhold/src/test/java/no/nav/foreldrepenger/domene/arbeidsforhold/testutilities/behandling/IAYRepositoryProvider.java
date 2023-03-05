package no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling;

import java.util.Objects;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;

@Dependent
public class IAYRepositoryProvider {

    private final FagsakRepository fagsakRepository;
    private final PersonopplysningRepository personopplysningRepository;
    private final FamilieHendelseRepository familieHendelseRepository;
    private final SøknadRepository søknadRepository;
    private final OpptjeningRepository opptjeningRepository;
    private final MottatteDokumentRepository mottatteDokumentRepository;
    private final BehandlingRepository behandlingRepository;
    private final BeregningsresultatRepository beregningsresultatRepository;

    @Inject
    public IAYRepositoryProvider(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");

        // behandling repositories
        this.behandlingRepository = new BehandlingRepository(entityManager);
        this.fagsakRepository = new FagsakRepository(entityManager);

        // behandling aggregater
        this.familieHendelseRepository = new FamilieHendelseRepository(entityManager);
        this.opptjeningRepository = new OpptjeningRepository(entityManager, this.behandlingRepository);
        this.personopplysningRepository = new PersonopplysningRepository(entityManager);
        this.søknadRepository = new SøknadRepository(entityManager, this.behandlingRepository);

        // behandling resultat aggregater
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

    public MottatteDokumentRepository getMottatteDokumentRepository() {
        return mottatteDokumentRepository;
    }

    public OpptjeningRepository getOpptjeningRepository() {
        return opptjeningRepository;
    }

    public SøknadRepository getSøknadRepository() {
        return søknadRepository;
    }

    public BeregningsresultatRepository getBeregningsresultatRepository() {
        return beregningsresultatRepository;
    }

    public FamilieHendelseRepository getFamilieHendelseRepository() {
        return familieHendelseRepository;
    }

    InntektArbeidYtelseTjeneste getInntektArbeidYtelseTjeneste() {
        return new AbakusInMemoryInntektArbeidYtelseTjeneste();
    }

    PersonopplysningRepository getPersonopplysningRepository() {
        return personopplysningRepository;
    }
}
