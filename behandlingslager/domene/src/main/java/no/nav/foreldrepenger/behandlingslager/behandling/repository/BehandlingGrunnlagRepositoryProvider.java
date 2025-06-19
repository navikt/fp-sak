package no.nav.foreldrepenger.behandlingslager.behandling.repository;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravArbeidRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValgRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;

/**
 * Provider for å enklere å kunne hente ut ulike behandlingsgrunnlagrepository uten for mange injection points.
 */
@ApplicationScoped
public class BehandlingGrunnlagRepositoryProvider {

    private EntityManager entityManager;
    private PersonopplysningRepository personopplysningRepository;
    private MedlemskapRepository medlemskapRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private PleiepengerRepository pleiepengerRepository;
    private UføretrygdRepository uføretrygdRepository;
    private SøknadRepository søknadRepository;
    private NesteSakRepository nesteSakRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private ArbeidsforholdValgRepository arbeidsforholdValgRepository;
    private AktivitetskravArbeidRepository aktivitetskravArbeidRepository;
    private EøsUttakRepository eøsUttakRepository;

    BehandlingGrunnlagRepositoryProvider() {
        // for CDI proxy
    }

    @Inject
    public BehandlingGrunnlagRepositoryProvider(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;

        // Generelle grunnlag
        this.familieHendelseRepository = new FamilieHendelseRepository(entityManager);
        this.medlemskapRepository = new MedlemskapRepository(entityManager);
        this.personopplysningRepository = new PersonopplysningRepository(entityManager);
        this.pleiepengerRepository = new PleiepengerRepository(entityManager);
        this.uføretrygdRepository = new UføretrygdRepository(entityManager);
        this.søknadRepository = new SøknadRepository(entityManager, new BehandlingRepository(entityManager));
        this.nesteSakRepository = new NesteSakRepository(entityManager);
        this.arbeidsforholdValgRepository = new ArbeidsforholdValgRepository(entityManager);
        this.aktivitetskravArbeidRepository = new AktivitetskravArbeidRepository(entityManager);

        // Ytelsespesifikke grunnlag
        this.ytelsesFordelingRepository = new YtelsesFordelingRepository(entityManager);
        this.svangerskapspengerRepository = new SvangerskapspengerRepository(entityManager);
        this.eøsUttakRepository = new EøsUttakRepository(entityManager);
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public PersonopplysningRepository getPersonopplysningRepository() {
        return personopplysningRepository;
    }

    public MedlemskapRepository getMedlemskapRepository() {
        return medlemskapRepository;
    }

    public FamilieHendelseRepository getFamilieHendelseRepository() {
        return familieHendelseRepository;
    }

    public PleiepengerRepository getPleiepengerRepository() {
        return pleiepengerRepository;
    }

    public UføretrygdRepository getUføretrygdRepository() {
        return uføretrygdRepository;
    }

    public SøknadRepository getSøknadRepository() {
        return søknadRepository;
    }

    public YtelsesFordelingRepository getYtelsesFordelingRepository() {
        return ytelsesFordelingRepository;
    }

    public SvangerskapspengerRepository getSvangerskapspengerRepository() {
        return svangerskapspengerRepository;
    }

    public AktivitetskravArbeidRepository getAktivitetskravArbeidRepository() {
        return aktivitetskravArbeidRepository;
    }

    public NesteSakRepository getNesteSakRepository() {
        return nesteSakRepository;
    }

    public ArbeidsforholdValgRepository getArbeidsforholdValgRepository() {
        return arbeidsforholdValgRepository;
    }

    public EøsUttakRepository getEøsUttakRepository() {
        return eøsUttakRepository;
    }
}
