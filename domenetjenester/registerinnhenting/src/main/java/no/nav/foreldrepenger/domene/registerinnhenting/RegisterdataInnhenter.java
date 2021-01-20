package no.nav.foreldrepenger.domene.registerinnhenting;

import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.ARBEIDSFORHOLD;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.INNTEKT_BEREGNINGSGRUNNLAG;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.INNTEKT_PENSJONSGIVENDE;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.INNTEKT_SAMMENLIGNINGSGRUNNLAG;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.LIGNET_NÆRING;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.YTELSE;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.abakus.iaygrunnlag.request.InnhentRegisterdataRequest;
import no.nav.abakus.iaygrunnlag.request.RegisterdataType;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.abakus.logg.AbakusInnhentingGrunnlagLogg;
import no.nav.foreldrepenger.behandlingslager.abakus.logg.AbakusInnhentingGrunnlagLoggRepository;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.abakus.AbakusTjeneste;
import no.nav.foreldrepenger.domene.abakus.mapping.KodeverkMapper;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.medlem.api.Medlemskapsperiode;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningInnhenter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
public class RegisterdataInnhenter {

    private static final Logger log = LoggerFactory.getLogger(RegisterdataInnhenter.class);
    private static final Set<RegisterdataType> FØRSTEGANGSSØKNAD_FP_SVP = Set.of(
        YTELSE,
        ARBEIDSFORHOLD,
        INNTEKT_PENSJONSGIVENDE,
        LIGNET_NÆRING,
        INNTEKT_BEREGNINGSGRUNNLAG,
        INNTEKT_SAMMENLIGNINGSGRUNNLAG
    );
    private static final Set<RegisterdataType> FØRSTEGANGSSØKNAD_ES = Set.of(
        YTELSE,
        ARBEIDSFORHOLD,
        INNTEKT_PENSJONSGIVENDE,
        LIGNET_NÆRING
    );
    private static final Set<RegisterdataType> REVURDERING_FP_SVP = Set.of(
        YTELSE,
        ARBEIDSFORHOLD,
        INNTEKT_PENSJONSGIVENDE,
        INNTEKT_BEREGNINGSGRUNNLAG,
        INNTEKT_SAMMENLIGNINGSGRUNNLAG
    );

    private static final Set<RegisterdataType> REVURDERING_ES = Set.of(
        YTELSE,
        ARBEIDSFORHOLD,
        INNTEKT_PENSJONSGIVENDE
    );

    private PersonopplysningInnhenter personopplysningInnhenter;
    private MedlemTjeneste medlemTjeneste;
    private PersonopplysningRepository personopplysningRepository;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private MedlemskapRepository medlemskapRepository;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;
    private AbakusTjeneste abakusTjeneste;
    private AbakusInnhentingGrunnlagLoggRepository loggRepository;

    RegisterdataInnhenter() {
        // for CDI proxy
    }

    @Inject
    public RegisterdataInnhenter(PersonopplysningInnhenter personopplysningInnhenter,
                                 MedlemTjeneste medlemTjeneste,
                                 BehandlingRepositoryProvider repositoryProvider,
                                 FamilieHendelseTjeneste familieHendelseTjeneste,
                                 MedlemskapRepository medlemskapRepository,
                                 OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste,
                                 AbakusTjeneste abakusTjeneste,
                                 AbakusInnhentingGrunnlagLoggRepository loggRepository) {
        this.personopplysningInnhenter = personopplysningInnhenter;
        this.medlemTjeneste = medlemTjeneste;
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.medlemskapRepository = medlemskapRepository;
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
        this.abakusTjeneste = abakusTjeneste;
        this.loggRepository = loggRepository;
    }

    private Optional<AktørId> finnAnnenPart(Long behandlingId) {
        return personopplysningRepository.hentPersonopplysningerHvisEksisterer(behandlingId)
            .flatMap(PersonopplysningGrunnlagEntitet::getOppgittAnnenPart)
            .map(OppgittAnnenPartEntitet::getAktørId);
    }

    public void innhentPersonopplysninger(Behandling behandling) {
        innhentPersoninformasjon(behandling);
        innhentFamiliehendelse(behandling);
    }

    public void innhentPersoninformasjon(Behandling behandling) {
        AktørId søker = behandling.getNavBruker().getAktørId();
        var annenPart = finnAnnenPart(behandling.getId());
        final Interval opplysningsperioden = opplysningsPeriodeTjeneste.beregnTilOgMedIdag(behandling.getId(), behandling.getFagsakYtelseType());
        var fødselsIntervall = familieHendelseTjeneste.forventetFødselsIntervaller(BehandlingReferanse.fra(behandling));

        final PersonInformasjonBuilder informasjonBuilder = personopplysningRepository.opprettBuilderForRegisterdata(behandling.getId());
        informasjonBuilder.tilbakestill(behandling.getAktørId(), annenPart);
        personopplysningInnhenter.innhentPersonopplysninger(informasjonBuilder, søker, annenPart, opplysningsperioden, fødselsIntervall);
        personopplysningRepository.lagre(behandling.getId(), informasjonBuilder);
    }

    private void innhentFamiliehendelse(Behandling behandling) {
        List<LocalDateInterval> intervaller = familieHendelseTjeneste.forventetFødselsIntervaller(BehandlingReferanse.fra(behandling));
        List<FødtBarnInfo> fødselRegistrertTps = personopplysningInnhenter.innhentAlleFødteForIntervaller(behandling.getAktørId(), intervaller);
        familieHendelseTjeneste.oppdaterFødselPåGrunnlag(behandling, fødselRegistrertTps);
    }

    public void innhentMedlemskapsOpplysning(Behandling behandling) {
        Long behandlingId = behandling.getId();

        // Innhent medl for søker
        List<MedlemskapPerioderEntitet> medlemskapsperioder = innhentMedlemskapsopplysninger(behandling);
        medlemskapRepository.lagreMedlemskapRegisterOpplysninger(behandlingId, medlemskapsperioder);
    }

    private List<MedlemskapPerioderEntitet> innhentMedlemskapsopplysninger(Behandling behandling) {
        final Interval opplysningsperiode = opplysningsPeriodeTjeneste.beregn(behandling.getId(), behandling.getFagsakYtelseType());
        var fom = LocalDateTime.ofInstant(opplysningsperiode.getStart(), ZoneId.systemDefault()).toLocalDate();
        var tom = LocalDateTime.ofInstant(opplysningsperiode.getEnd(), ZoneId.systemDefault()).toLocalDate();

        return medlemTjeneste.finnMedlemskapPerioder(behandling.getAktørId(), fom, tom).stream()
            .map(this::lagMedlemskapPeriode)
            .collect(Collectors.toList());
    }

    private MedlemskapPerioderEntitet lagMedlemskapPeriode(Medlemskapsperiode medlemskapsperiode) {
        return new MedlemskapPerioderBuilder()
            .medPeriode(medlemskapsperiode.getFom(), medlemskapsperiode.getTom())
            .medBeslutningsdato(medlemskapsperiode.getDatoBesluttet())
            .medErMedlem(medlemskapsperiode.isErMedlem())
            .medLovvalgLand(medlemskapsperiode.getLovvalgsland())
            .medStudieLand(medlemskapsperiode.getStudieland())
            .medDekningType(medlemskapsperiode.getTrygdedekning())
            .medKildeType(medlemskapsperiode.getKilde())
            .medMedlemskapType(medlemskapsperiode.getLovvalg())
            .medMedlId(medlemskapsperiode.getMedlId())
            .build();
    }

    public void oppdaterSistOppdatertTidspunkt(Behandling behandling) {
        behandlingRepository.oppdaterSistOppdatertTidspunkt(behandling, LocalDateTime.now());
    }

    public void innhentIAYIAbakus(Behandling behandling) {
        doInnhentIAYIAbakus(behandling, behandling.getType(), behandling.getFagsakYtelseType());
    }

    public void innhentFullIAYIAbakus(Behandling behandling) {
        doInnhentIAYIAbakus(behandling, BehandlingType.FØRSTEGANGSSØKNAD, behandling.getFagsakYtelseType());
    }

    private void doInnhentIAYIAbakus(Behandling behandling, BehandlingType behandlingType, FagsakYtelseType fagsakYtelseType) {
        log.info("Trigger innhenting i abakus for behandling med id={} og uuid={}", behandling.getId(), behandling.getUuid());
        final var opplysningsperiode = opplysningsPeriodeTjeneste.beregn(behandling.getId(), fagsakYtelseType);
        var informasjonsElementer = utledBasertPå(behandlingType, fagsakYtelseType);
        final InnhentRegisterdataRequest innhentRegisterdataRequest = new InnhentRegisterdataRequest(behandling.getFagsak().getSaksnummer().getVerdi(),
            behandling.getUuid(),
            KodeverkMapper.fraFagsakYtelseType(fagsakYtelseType),
            new Periode(LocalDate.ofInstant(opplysningsperiode.getStart(), ZoneId.systemDefault()), LocalDate.ofInstant(opplysningsperiode.getEnd(), ZoneId.systemDefault())),
            new AktørIdPersonident(behandling.getAktørId().getId()),
            informasjonsElementer);

        final var uuidDto = abakusTjeneste.innhentRegisterdata(innhentRegisterdataRequest);
        log.info("Nytt aktivt grunnlag for behandling={} i abakus har uuid={}", behandling.getUuid(), uuidDto.toUuidReferanse());
        loggRepository.lagre(new AbakusInnhentingGrunnlagLogg(behandling.getId(), uuidDto.toUuidReferanse()));
    }

    private Set<RegisterdataType> utledBasertPå(BehandlingType behandlingType, FagsakYtelseType fagsakYtelseType) {
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandlingType)) {
            return FagsakYtelseType.ENGANGSTØNAD.equals(fagsakYtelseType) ? FØRSTEGANGSSØKNAD_ES : FØRSTEGANGSSØKNAD_FP_SVP;
        }
        return FagsakYtelseType.ENGANGSTØNAD.equals(fagsakYtelseType) ? REVURDERING_ES : REVURDERING_FP_SVP;
    }
}
