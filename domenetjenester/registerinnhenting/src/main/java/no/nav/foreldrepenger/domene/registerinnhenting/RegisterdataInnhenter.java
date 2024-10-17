package no.nav.foreldrepenger.domene.registerinnhenting;

import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.ARBEIDSFORHOLD;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.INNTEKT_BEREGNINGSGRUNNLAG;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.INNTEKT_PENSJONSGIVENDE;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.INNTEKT_SAMMENLIGNINGSGRUNNLAG;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.LIGNET_NÆRING;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.YTELSE;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.abakus.iaygrunnlag.request.InnhentRegisterdataRequest;
import no.nav.abakus.iaygrunnlag.request.RegisterdataType;
import no.nav.abakus.vedtak.ytelse.Kildesystem;
import no.nav.abakus.vedtak.ytelse.Ytelser;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerInnleggelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.abakus.AbakusTjeneste;
import no.nav.foreldrepenger.domene.abakus.mapping.KodeverkMapper;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.medlem.medl2.Medlemskapsperiode;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningInnhenter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;

@ApplicationScoped
public class RegisterdataInnhenter {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterdataInnhenter.class);
    private static final Set<RegisterdataType> FØRSTEGANGSSØKNAD_FP_SVP = Set.of(
        YTELSE,
        ARBEIDSFORHOLD,
        INNTEKT_PENSJONSGIVENDE,
        LIGNET_NÆRING,
        INNTEKT_BEREGNINGSGRUNNLAG,
        INNTEKT_SAMMENLIGNINGSGRUNNLAG
    );
    private static final Set<RegisterdataType> REVURDERING_FP_SVP = Set.of(
        YTELSE,
        ARBEIDSFORHOLD,
        INNTEKT_PENSJONSGIVENDE,
        INNTEKT_BEREGNINGSGRUNNLAG,
        INNTEKT_SAMMENLIGNINGSGRUNNLAG
    );

    private PersonopplysningInnhenter personopplysningInnhenter;
    private MedlemTjeneste medlemTjeneste;
    private PersonopplysningRepository personopplysningRepository;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private MedlemskapRepository medlemskapRepository;
    private PleiepengerRepository pleiepengerRepository;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;
    private AbakusTjeneste abakusTjeneste;
    private StønadsperioderInnhenter stønadsperioderInnhenter;

    RegisterdataInnhenter() {
        // for CDI proxy
    }

    @Inject
    public RegisterdataInnhenter(PersonopplysningInnhenter personopplysningInnhenter,
                                 MedlemTjeneste medlemTjeneste,
                                 BehandlingRepository behandlingRepository,
                                 BehandlingGrunnlagRepositoryProvider grunnlagRepositoryProvider,
                                 FamilieHendelseTjeneste familieHendelseTjeneste,
                                 MedlemskapRepository medlemskapRepository,
                                 OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste,
                                 AbakusTjeneste abakusTjeneste,
                                 StønadsperioderInnhenter stønadsperioderInnhenter) {
        this.personopplysningInnhenter = personopplysningInnhenter;
        this.medlemTjeneste = medlemTjeneste;
        this.personopplysningRepository = grunnlagRepositoryProvider.getPersonopplysningRepository();
        this.behandlingRepository = behandlingRepository;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.medlemskapRepository = medlemskapRepository;
        this.pleiepengerRepository = grunnlagRepositoryProvider.getPleiepengerRepository();
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
        this.abakusTjeneste = abakusTjeneste;
        this.stønadsperioderInnhenter = stønadsperioderInnhenter;
    }

    private Optional<AktørId> finnAnnenPart(Long behandlingId) {
        return personopplysningRepository.hentOppgittAnnenPartHvisEksisterer(behandlingId)
            .map(OppgittAnnenPartEntitet::getAktørId);
    }

    public void innhentPersonopplysninger(Behandling behandling) {
        var fødselsIntervall = familieHendelseTjeneste.forventetFødselsIntervaller(BehandlingReferanse.fra(behandling));
        var filtrertFødselFREG = personopplysningInnhenter.innhentAlleFødteForIntervaller(behandling.getFagsakYtelseType(), behandling.getAktørId(), fødselsIntervall);
        innhentPersoninformasjon(behandling, filtrertFødselFREG);
        innhentFamiliehendelse(behandling.getId(), filtrertFødselFREG);
        innhentPleiepenger(behandling, filtrertFødselFREG);
        // Logikk avhengig av familiehendelse som bør være innhentet
        stønadsperioderInnhenter.innhentNesteSak(behandling);
    }

    private void innhentPersoninformasjon(Behandling behandling, List<FødtBarnInfo> filtrertFødselFREG) {
        var søker = behandling.getNavBruker().getAktørId();
        var annenPart = finnAnnenPart(behandling.getId());
        var opplysningsperioden = opplysningsPeriodeTjeneste.beregnTilOgMedIdag(behandling.getId(), behandling.getFagsakYtelseType());

        var informasjonBuilder = personopplysningRepository.opprettBuilderForRegisterdata(behandling.getId());
        informasjonBuilder.tilbakestill(behandling.getAktørId(), annenPart);
        personopplysningInnhenter.innhentPersonopplysninger(behandling.getFagsakYtelseType(), informasjonBuilder, søker, annenPart, opplysningsperioden, filtrertFødselFREG);
        personopplysningRepository.lagre(behandling.getId(), informasjonBuilder);
    }

    private void innhentFamiliehendelse(Long behandlingId, List<FødtBarnInfo> filtrertFødselFREG) {
        familieHendelseTjeneste.oppdaterFødselPåGrunnlag(behandlingId, filtrertFødselFREG);
    }

    private void innhentPleiepenger(Behandling behandling, List<FødtBarnInfo> filtrertFødselFREG) {
        var bekreftetFødt = filtrertFødselFREG.stream().map(FødtBarnInfo::ident).filter(Objects::nonNull).collect(Collectors.toSet());

        if (bekreftetFødt.isEmpty() || !FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) return;
        var tidligstFødt = filtrertFødselFREG.stream().map(FødtBarnInfo::fødselsdato).min(Comparator.naturalOrder()).orElseGet(LocalDate::now);

        var request = AbakusTjeneste.lagRequestForHentVedtakFom(behandling.getAktørId(), tidligstFødt.minusWeeks(25),
            Set.of(Ytelser.PLEIEPENGER_SYKT_BARN));

        var potensielleVedtak = abakusTjeneste.hentVedtakForAktørId(request).stream()
            .map(y -> (YtelseV1)y)
            .filter(y -> Kildesystem.K9SAK.equals(y.getKildesystem()))
            .filter(y -> Ytelser.PLEIEPENGER_SYKT_BARN.equals(y.getYtelse()))
            .filter(y -> y.getTilleggsopplysninger() != null && !y.getTilleggsopplysninger().isBlank())
            .toList();

        LOG.info("PSB innhent behandling {} tidligst født {} antall potensielle vedtak {}", behandling.getId(), tidligstFødt, potensielleVedtak.size());

        var aktivtGrunnlag = pleiepengerRepository.hentGrunnlag(behandling.getId());
        var eventuellePleiepenger = mapTilPleiepengerGrunnlagData(behandling, aktivtGrunnlag, potensielleVedtak, bekreftetFødt);
        eventuellePleiepenger.ifPresent(pp -> {
            LOG.info("PSB innhent behandling {} lagrer grunnlag", behandling.getId());
            pleiepengerRepository.lagrePerioder(behandling.getId(), pp);
        });
    }

    public void innhentMedlemskapsOpplysning(Behandling behandling) {
        var behandlingId = behandling.getId();

        // Innhent medl for søker
        var medlemskapsperioder = innhentMedlemskapsopplysninger(behandling);
        medlemskapRepository.lagreMedlemskapRegisterOpplysninger(behandlingId, medlemskapsperioder);
    }

    private List<MedlemskapPerioderEntitet> innhentMedlemskapsopplysninger(Behandling behandling) {
        var opplysningsperiode = opplysningsPeriodeTjeneste.beregn(behandling.getId(), behandling.getFagsakYtelseType());

        return medlemTjeneste.finnMedlemskapPerioder(behandling.getAktørId(), opplysningsperiode.getFomDato(), opplysningsperiode.getTomDato()).stream()
            .map(this::lagMedlemskapPeriode)
            .toList();
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
        LOG.info("Trigger innhenting i abakus for behandling med id={} og uuid={}", behandling.getId(), behandling.getUuid());
        var behandlingUuid = behandling.getUuid();
        var saksnummer = behandling.getFagsak().getSaksnummer().getVerdi();
        var ytelseType = KodeverkMapper.fraFagsakYtelseType(fagsakYtelseType);
        var opplysningsperiode = opplysningsPeriodeTjeneste.beregn(behandling.getId(), fagsakYtelseType);
        var periode = new Periode(opplysningsperiode.getFomDato(), opplysningsperiode.getTomDato());
        var aktør = new AktørIdPersonident(behandling.getAktørId().getId());
        var informasjonsElementer = utledBasertPå(behandlingType);

        var request = new InnhentRegisterdataRequest(saksnummer, behandlingUuid, ytelseType, periode, aktør, informasjonsElementer);

        var næringsperiode = opplysningsPeriodeTjeneste.beregnForNæringPGI(behandling.getId(), fagsakYtelseType);
        LOG.info("ABAKUS NÆRINGSPERIODE {} OPPLYSNINGSPERIODE {} behandling id={}", næringsperiode, opplysningsperiode, behandling.getId());
        request.setOpplysningsperiodeSkattegrunnlag(new Periode(næringsperiode.getFomDato(), næringsperiode.getTomDato()));

        abakusTjeneste.innhentRegisterdata(request);
    }

    private Set<RegisterdataType> utledBasertPå(BehandlingType behandlingType) {
        return BehandlingType.FØRSTEGANGSSØKNAD.equals(behandlingType) ? FØRSTEGANGSSØKNAD_FP_SVP : REVURDERING_FP_SVP;
    }

    private Optional<PleiepengerPerioderEntitet.Builder> mapTilPleiepengerGrunnlagData(Behandling behandling, Optional<PleiepengerGrunnlagEntitet> aktivtGrunnlag,
                                                                                       List<YtelseV1> vedtakene, Set<PersonIdent> aktuelleBarn) {
        var ppBuilder = new PleiepengerPerioderEntitet.Builder();
        for (var vedtak : vedtakene) {
            var oversatt = PleipengerOversetter.oversettTilleggsopplysninger(vedtak.getTilleggsopplysninger());
            if (oversatt != null) LOG.info("PSB innhent behandling {} vedtak aktuelt barn {} med perioder {}",
                behandling.getId(), gjelderAktuelleBarn(oversatt, aktuelleBarn), oversatt.innleggelsesPerioder());
            if (oversatt != null && oversatt.innleggelsesPerioder() != null && gjelderAktuelleBarn(oversatt, aktuelleBarn)) {
                oversatt.innleggelsesPerioder().stream()
                    .filter(ip -> ip.tom() != null)
                    .map(ip -> new PleiepengerInnleggelseEntitet.Builder()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(ip.fom(), ip.tom()))
                        .medPleiepengerSaksnummer(new Saksnummer(vedtak.getSaksnummer()))
                        .medPleietrengendeAktørId(oversatt.pleietrengende()))
                    .forEach(ppBuilder::leggTil);
            }
        }
        if (ppBuilder.harPerioder() || aktivtGrunnlag.flatMap(PleiepengerGrunnlagEntitet::getPerioderMedInnleggelse).isPresent()) {
            LOG.info("PSB innhent behandling {} har perioder {}", behandling.getId(), ppBuilder.harPerioder());
            return Optional.of(ppBuilder);
        }
        return Optional.empty();
    }

    private boolean gjelderAktuelleBarn(PleipengerOversetter.PleiepengerOpplysninger pleiepenger, Set<PersonIdent> aktuelleBarn) {
        return personopplysningInnhenter.hentPersonIdentForAktør(pleiepenger.pleietrengende())
            .filter(aktuelleBarn::contains)
            .isPresent();
    }

}
