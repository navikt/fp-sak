package no.nav.foreldrepenger.domene.registerinnhenting;

import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.ARBEIDSFORHOLD;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.INNTEKT_BEREGNINGSGRUNNLAG;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.INNTEKT_PENSJONSGIVENDE;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.INNTEKT_SAMMENLIGNINGSGRUNNLAG;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.LIGNET_NÆRING;
import static no.nav.abakus.iaygrunnlag.request.RegisterdataType.YTELSE;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
import no.nav.foreldrepenger.behandlingslager.abakus.logg.AbakusInnhentingGrunnlagLogg;
import no.nav.foreldrepenger.behandlingslager.abakus.logg.AbakusInnhentingGrunnlagLoggRepository;
import no.nav.foreldrepenger.behandlingslager.aktør.Adresseinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.Familierelasjon;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.AdressePeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.PersonstatusPeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.StatsborgerskapPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.MapRegionLandkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.kodeverk.KodeverkRepository;
import no.nav.foreldrepenger.domene.abakus.AbakusTjeneste;
import no.nav.foreldrepenger.domene.abakus.mapping.KodeverkMapper;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.medlem.api.Medlemskapsperiode;
import no.nav.foreldrepenger.domene.person.tps.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.person.tps.TpsFødselUtil;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.SaksopplysningerFeil;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.konfig.Tid;

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

    private PersoninfoAdapter personinfoAdapter;
    private MedlemTjeneste medlemTjeneste;
    private PersonopplysningRepository personopplysningRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private BehandlingRepository behandlingRepository;
    private KodeverkRepository kodeverkRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private MedlemskapRepository medlemskapRepository;
    private SøknadRepository søknadRepository;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;
    private AbakusTjeneste abakusTjeneste;
    private AbakusInnhentingGrunnlagLoggRepository loggRepository;
    private Period etterkontrollTidsromFørSøknadsdato;
    private Period etterkontrollTidsromEtterTermindato;

    RegisterdataInnhenter() {
        // for CDI proxy
    }

    /**
     * @param etterkontrollTidsromFørSøknadsdato - Periode før søknadsdato hvor det skal etterkontrolleres barn er født
     * @param etterkontrollTidsromEtterTermindato - Periode etter termindato hvor det skal etterkontrolleres barn er født
     */
    @Inject
    public RegisterdataInnhenter(PersoninfoAdapter personinfoAdapter, // NOSONAR - krever mange parametere
                                 MedlemTjeneste medlemTjeneste,
                                 BehandlingRepositoryProvider repositoryProvider,
                                 KodeverkRepository kodeverkRepository,
                                 FamilieHendelseTjeneste familieHendelseTjeneste,
                                 MedlemskapRepository medlemskapRepository,
                                 OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste,
                                 AbakusTjeneste abakusTjeneste,
                                 AbakusInnhentingGrunnlagLoggRepository loggRepository,
                                 @KonfigVerdi(value = "etterkontroll.førsøknad.periode", defaultVerdi = "P1W") Period etterkontrollTidsromFørSøknadsdato,
                                 @KonfigVerdi(value = "etterkontroll.ettertermin.periode", defaultVerdi = "P4W") Period etterkontrollTidsromEtterTermindato) {
        this.personinfoAdapter = personinfoAdapter;
        this.medlemTjeneste = medlemTjeneste;
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.medlemskapRepository = medlemskapRepository;
        this.kodeverkRepository = kodeverkRepository;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
        this.abakusTjeneste = abakusTjeneste;
        this.loggRepository = loggRepository;
        this.etterkontrollTidsromFørSøknadsdato = etterkontrollTidsromFørSøknadsdato;
        this.etterkontrollTidsromEtterTermindato = etterkontrollTidsromEtterTermindato;
    }

    public Personinfo innhentSaksopplysningerForSøker(AktørId søkerAktørId) {
        return personinfoAdapter.innhentSaksopplysningerForSøker(søkerAktørId);
    }

    public Optional<Personinfo> innhentSaksopplysningerForMedSøker(Long behandlingId) {
        Optional<AktørId> annenPart = finnAnnenPart(behandlingId);

        return annenPart.flatMap(
            aktørId -> personinfoAdapter.innhentSaksopplysningerForEktefelle(Optional.of(aktørId)));
    }

    private Optional<AktørId> finnAnnenPart(Long behandlingId) {
        final Optional<OppgittAnnenPartEntitet> oppgittAnnenPart = personopplysningRepository.hentPersonopplysningerHvisEksisterer(behandlingId)
            .flatMap(PersonopplysningGrunnlagEntitet::getOppgittAnnenPart);
        Optional<AktørId> funnetAktørId = oppgittAnnenPart.map(OppgittAnnenPartEntitet::getAktørId);
        return funnetAktørId;
    }

    public Personinfo innhentPersonopplysninger(Behandling behandling) {
        // Innhent data fra TPS for søker
        AktørId søkerAktørId = behandling.getNavBruker().getAktørId();
        Personinfo søkerInfo = innhentSaksopplysningerForSøker(søkerAktørId);

        if (søkerInfo == null) {
            throw SaksopplysningerFeil.FACTORY.feilVedOppslagITPS(søkerAktørId.toString())
                .toException();
        }

        // Innhent øvrige data fra TPS
        Optional<Personinfo> medsøkerInfo = innhentSaksopplysningerForMedSøker(behandling.getId());
        innhentPersonopplysninger(behandling, søkerInfo, medsøkerInfo);
        innhentFamiliehendelse(behandling);
        return søkerInfo;
    }

    public void innhentPersonopplysninger(Behandling behandling, Personinfo søkerInfo,
                                          Optional<Personinfo> medsøkerInfo) {

        final PersonInformasjonBuilder personInformasjonBuilder = byggPersonopplysningMedRelasjoner(søkerInfo, medsøkerInfo, behandling);

        personopplysningRepository.lagre(behandling.getId(), personInformasjonBuilder);
    }

    private void innhentFamiliehendelse(Behandling behandling) {
        FamilieHendelseGrunnlagEntitet aggregat = familieHendelseRepository.hentAggregat(behandling.getId());
        List<FødtBarnInfo> fødselRegistrertTps = personinfoAdapter.innhentAlleFødteForBehandling(behandling, aggregat);
        familieHendelseTjeneste.oppdaterFødselPåGrunnlag(behandling, fødselRegistrertTps);
    }

    public void innhentMedlemskapsOpplysning(Behandling behandling) {
        Long behandlingId = behandling.getId();

        // Innhent medl for søker
        List<MedlemskapPerioderEntitet> medlemskapsperioder = innhentMedlemskapsopplysninger(behandling);
        medlemskapRepository.lagreMedlemskapRegisterOpplysninger(behandlingId, medlemskapsperioder);
    }

    private PersonInformasjonBuilder byggPersonopplysningMedRelasjoner(Personinfo søkerPersonInfo,
                                                                       Optional<Personinfo> annenPartInfo,
                                                                       Behandling behandling) {

        final PersonInformasjonBuilder informasjonBuilder = personopplysningRepository.opprettBuilderForRegisterdata(behandling.getId());
        informasjonBuilder.tilbakestill(behandling.getAktørId(), annenPartInfo.map(Personinfo::getAktørId));

        // Historikk for søker
        final Interval opplysningsperioden = opplysningsPeriodeTjeneste.beregnTilOgMedIdag(behandling.getId(), behandling.getFagsakYtelseType());
        final Personhistorikkinfo personhistorikkinfo = personinfoAdapter.innhentPersonopplysningerHistorikk(søkerPersonInfo.getAktørId(), opplysningsperioden);
        if (personhistorikkinfo != null) {
            mapAdresser(personhistorikkinfo.getAdressehistorikk(), informasjonBuilder, søkerPersonInfo);
            mapStatsborgerskap(personhistorikkinfo.getStatsborgerskaphistorikk(), informasjonBuilder, søkerPersonInfo);
            mapPersonstatus(personhistorikkinfo.getPersonstatushistorikk(), informasjonBuilder, søkerPersonInfo);
        }

        mapTilPersonopplysning(søkerPersonInfo, informasjonBuilder, true, false, behandling);
        // Ektefelle
        leggTilEktefelle(søkerPersonInfo, informasjonBuilder, behandling);

        // Medsøker (annen part). kan være samme person som Ektefelle
        annenPartInfo.ifPresent(annenPart -> leggTilMedsøkerAnnenPart(søkerPersonInfo, annenPart, informasjonBuilder, behandling));

        return informasjonBuilder;
    }

    private void mapPersonstatus(List<PersonstatusPeriode> personstatushistorikk, PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        for (PersonstatusPeriode personstatus : personstatushistorikk) {
            final PersonstatusType status = personstatus.getPersonstatus();
            final DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(
                brukFødselsdatoHvisEtter(personstatus.getGyldighetsperiode().getFom(), personinfo.getFødselsdato()),
                personstatus.getGyldighetsperiode().getTom());
            final PersonInformasjonBuilder.PersonstatusBuilder builder = informasjonBuilder.getPersonstatusBuilder(personinfo.getAktørId(), periode);
            builder.medPeriode(periode)
                .medPersonstatus(status);
            informasjonBuilder.leggTil(builder);
        }
    }

    private void mapStatsborgerskap(List<StatsborgerskapPeriode> statsborgerskaphistorikk, PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        for (StatsborgerskapPeriode statsborgerskap : statsborgerskaphistorikk) {
            final Landkoder landkode = kodeverkRepository.finn(Landkoder.class, statsborgerskap.getStatsborgerskap().getLandkode());

            Region region = MapRegionLandkoder.mapLandkode(statsborgerskap.getStatsborgerskap().getLandkode());

            final DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(brukFødselsdatoHvisEtter(
                statsborgerskap.getGyldighetsperiode().getFom(), personinfo.getFødselsdato()), statsborgerskap.getGyldighetsperiode().getTom());
            final PersonInformasjonBuilder.StatsborgerskapBuilder builder = informasjonBuilder.getStatsborgerskapBuilder(personinfo.getAktørId(), periode,
                landkode, region);
            builder.medPeriode(periode)
                .medStatsborgerskap(landkode);
            builder.medRegion(region);

            informasjonBuilder.leggTil(builder);
        }
    }

    private void mapAdresser(List<AdressePeriode> adressehistorikk, PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        AktørId aktørId = personinfo.getAktørId();
        for (AdressePeriode adresse : adressehistorikk) {
            final DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(
                brukFødselsdatoHvisEtter(adresse.getGyldighetsperiode().getFom(), personinfo.getFødselsdato()), adresse.getGyldighetsperiode().getTom());
            final PersonInformasjonBuilder.AdresseBuilder adresseBuilder = informasjonBuilder.getAdresseBuilder(aktørId, periode,
                adresse.getAdresse().getAdresseType());
            adresseBuilder.medPeriode(periode)
                .medAdresselinje1(adresse.getAdresse().getAdresselinje1())
                .medAdresselinje2(adresse.getAdresse().getAdresselinje2())
                .medAdresselinje3(adresse.getAdresse().getAdresselinje3())
                .medAdresselinje4(adresse.getAdresse().getAdresselinje4())
                .medLand(adresse.getAdresse().getLand())
                .medPostnummer(adresse.getAdresse().getPostnummer())
                .medPoststed(adresse.getAdresse().getPoststed());
            informasjonBuilder.leggTil(adresseBuilder);
        }
    }

    private LocalDate brukFødselsdatoHvisEtter(LocalDate dato, LocalDate fødseldato) {
        if (dato.isBefore(fødseldato)) {
            return fødseldato;
        }
        return dato;
    }

    private void mapTilPersonopplysning(Personinfo personinfo, PersonInformasjonBuilder informasjonBuilder, boolean skalHenteBarnRelasjoner,
                                        boolean erIkkeSøker, Behandling behandling) {
        mapInfoTilEntitet(personinfo, informasjonBuilder, erIkkeSøker);

        if (skalHenteBarnRelasjoner) {
            List<Personinfo> barna = hentBarnRelatertTil(personinfo, behandling);
            barna.forEach(barn -> {
                mapInfoTilEntitet(barn, informasjonBuilder, true);
                mapRelasjon(personinfo, barn, Collections.singletonList(RelasjonsRolleType.BARN), informasjonBuilder);
                mapRelasjon(barn, personinfo, utledRelasjonsrolleTilBarn(personinfo, barn), informasjonBuilder);
            });
        }
    }

    private List<RelasjonsRolleType> utledRelasjonsrolleTilBarn(Personinfo personinfo, Personinfo barn) {
        if (barn == null) {
            return Collections.emptyList();
        }
        return barn.getFamilierelasjoner().stream()
            .filter(fr -> fr.getPersonIdent().equals(personinfo.getPersonIdent()))
            .map(rel -> utledRelasjonsrolleTilBarn(personinfo.getKjønn(), rel.getRelasjonsrolle()))
            .collect(Collectors.toList());
    }

    private void mapRelasjon(Personinfo fra, Personinfo til, List<RelasjonsRolleType> roller, PersonInformasjonBuilder informasjonBuilder) {
        if (til == null) {
            return;
        }
        for (RelasjonsRolleType rolle : roller) {
            final PersonInformasjonBuilder.RelasjonBuilder builder = informasjonBuilder.getRelasjonBuilder(fra.getAktørId(), til.getAktørId(), rolle);
            builder.harSammeBosted(utledSammeBosted(fra, til, rolle));
            informasjonBuilder.leggTil(builder);
        }
    }

    private boolean utledSammeBosted(Personinfo personinfo, Personinfo barn, RelasjonsRolleType rolle) {
        final Optional<Boolean> sammeBosted = personinfo.getFamilierelasjoner().stream()
            .filter(fr -> fr.getRelasjonsrolle().equals(rolle) && fr.getPersonIdent().equals(barn.getPersonIdent()))
            .findAny()
            .map(Familierelasjon::getHarSammeBosted);
        return sammeBosted.orElse(false);
    }

    private void mapInfoTilEntitet(Personinfo personinfo, PersonInformasjonBuilder informasjonBuilder, boolean lagreIHistoriskeTabeller) {
        final DatoIntervallEntitet periode = getPeriode(personinfo.getFødselsdato(), Tid.TIDENES_ENDE);
        final PersonInformasjonBuilder.PersonopplysningBuilder builder = informasjonBuilder.getPersonopplysningBuilder(personinfo.getAktørId());
        builder.medKjønn(personinfo.getKjønn())
            .medFødselsdato(personinfo.getFødselsdato())
            .medNavn(personinfo.getNavn())
            .medDødsdato(personinfo.getDødsdato())
            .medSivilstand(personinfo.getSivilstandType())
            .medRegion(personinfo.getRegion());
        informasjonBuilder.leggTil(builder);

        if (lagreIHistoriskeTabeller || informasjonBuilder.harIkkeFåttStatsborgerskapHistorikk(personinfo.getAktørId())) {
            final PersonInformasjonBuilder.StatsborgerskapBuilder statsborgerskapBuilder = informasjonBuilder.getStatsborgerskapBuilder(personinfo.getAktørId(),
                periode, personinfo.getLandkode(), personinfo.getRegion());
            informasjonBuilder.leggTil(statsborgerskapBuilder);
        }

        if (lagreIHistoriskeTabeller || informasjonBuilder.harIkkeFåttAdresseHistorikk(personinfo.getAktørId())) {
            for (Adresseinfo adresse : personinfo.getAdresseInfoList()) {
                final PersonInformasjonBuilder.AdresseBuilder adresseBuilder = informasjonBuilder.getAdresseBuilder(personinfo.getAktørId(),
                    periode, adresse.getGjeldendePostadresseType());
                informasjonBuilder.leggTil(adresseBuilder
                    .medAdresselinje1(adresse.getAdresselinje1())
                    .medAdresselinje2(adresse.getAdresselinje2())
                    .medAdresselinje3(adresse.getAdresselinje3())
                    .medPostnummer(adresse.getPostNr())
                    .medPoststed(adresse.getPoststed())
                    .medLand(adresse.getLand())
                    .medAdresseType(adresse.getGjeldendePostadresseType())
                    .medPeriode(periode));
            }
        }

        if (lagreIHistoriskeTabeller || informasjonBuilder.harIkkeFåttPersonstatusHistorikk(personinfo.getAktørId())) {
            final PersonInformasjonBuilder.PersonstatusBuilder personstatusBuilder = informasjonBuilder.getPersonstatusBuilder(personinfo.getAktørId(),
                periode).medPersonstatus(personinfo.getPersonstatus());
            informasjonBuilder.leggTil(personstatusBuilder);
        }
    }

    private DatoIntervallEntitet getPeriode(LocalDate fom, LocalDate tom) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom != null ? tom : Tid.TIDENES_ENDE);
    }

    private void leggTilMedsøkerAnnenPart(Personinfo søkerPersonInfo, Personinfo annenPart, PersonInformasjonBuilder informasjonBuilder,
                                          Behandling behandling) {
        // Medsøker - kan være samme person som ektefelle
        List<Personinfo> fellesBarn = finnFellesBarn(annenPart, søkerPersonInfo, behandling);

        mapTilPersonopplysning(annenPart, informasjonBuilder, false, true, behandling);
        for (Personinfo barn : fellesBarn) {
            final Personinfo til = personinfoAdapter.innhentSaksopplysninger(barn.getPersonIdent()).orElse(null);
            mapRelasjon(annenPart, til, Collections.singletonList(RelasjonsRolleType.BARN), informasjonBuilder);
            mapRelasjon(til, annenPart, utledRelasjonsrolleTilBarn(annenPart, til), informasjonBuilder); // NOSONAR
        }
    }

    private void leggTilEktefelle(Personinfo søkerPersonInfo, PersonInformasjonBuilder informasjonBuilder, Behandling behandling) {
        // Ektefelle
        final List<Familierelasjon> familierelasjoner = søkerPersonInfo.getFamilierelasjoner()
            .stream()
            .filter(f -> f.getRelasjonsrolle().equals(RelasjonsRolleType.EKTE) ||
                f.getRelasjonsrolle().equals(RelasjonsRolleType.REGISTRERT_PARTNER) ||
                f.getRelasjonsrolle().equals(RelasjonsRolleType.SAMBOER))
            .collect(Collectors.toList());
        for (Familierelasjon familierelasjon : familierelasjoner) {
            Optional<Personinfo> ektefelleInfo = personinfoAdapter.innhentSaksopplysninger(familierelasjon.getPersonIdent());
            if (ektefelleInfo.isPresent()) {
                final Personinfo personinfo = ektefelleInfo.get();
                mapTilPersonopplysning(personinfo, informasjonBuilder, false, true, behandling);
                mapRelasjon(søkerPersonInfo, personinfo, Collections.singletonList(familierelasjon.getRelasjonsrolle()), informasjonBuilder);
                mapRelasjon(personinfo, søkerPersonInfo, Collections.singletonList(familierelasjon.getRelasjonsrolle()), informasjonBuilder);
            }
        }
    }

    private List<Personinfo> hentBarnRelatertTil(Personinfo personinfo, Behandling behandling) {
        List<Personinfo> relaterteBarn = hentAlleRelaterteBarn(personinfo);
        SøknadEntitet søknad = søknadRepository.hentFørstegangsSøknad(behandling);
        FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = familieHendelseRepository.hentAggregat(behandling.getId());

        DatoIntervallEntitet forventetFødselIntervall = TpsFødselUtil.forventetFødselIntervall(familieHendelseGrunnlag,
            etterkontrollTidsromFørSøknadsdato, etterkontrollTidsromEtterTermindato, søknad);

        return relaterteBarn.stream().filter(b -> forventetFødselIntervall.inkluderer(b.getFødselsdato())).collect(Collectors.toList());
    }

    private List<Personinfo> hentAlleRelaterteBarn(Personinfo søkerPersonInfo) {
        return søkerPersonInfo.getFamilierelasjoner()
            .stream()
            .filter(r -> r.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .map(r -> personinfoAdapter.innhentSaksopplysningerForBarn(r.getPersonIdent()).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private RelasjonsRolleType utledRelasjonsrolleTilBarn(NavBrukerKjønn kjønn, RelasjonsRolleType rolle) {
        if (kjønn.equals(NavBrukerKjønn.KVINNE) && rolle.equals(RelasjonsRolleType.FARA)) {
            return RelasjonsRolleType.MEDMOR;
        }

        return NavBrukerKjønn.KVINNE.equals(kjønn) ? RelasjonsRolleType.MORA : RelasjonsRolleType.FARA;
    }

    private List<Personinfo> finnFellesBarn(Personinfo annenPart, Personinfo førstePart, Behandling behandling) {
        List<PersonIdent> fnrAnnenPartsBarn = annenPart.getFamilierelasjoner().stream()
            .filter(f -> f.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .map(Familierelasjon::getPersonIdent)
            .collect(Collectors.toList());

        final List<Personinfo> barnPersonInfo = hentBarnRelatertTil(førstePart, behandling);

        return barnPersonInfo.stream()
            .filter(barn -> fnrAnnenPartsBarn.contains(barn.getPersonIdent()))
            .collect(Collectors.toList());
    }

    private List<MedlemskapPerioderEntitet> innhentMedlemskapsopplysninger(Behandling behandling) {
        final Interval opplysningsperiode = opplysningsPeriodeTjeneste.beregn(behandling.getId(), behandling.getFagsakYtelseType());
        var fom = LocalDateTime.ofInstant(opplysningsperiode.getStart(), ZoneId.systemDefault()).toLocalDate();
        var tom = LocalDateTime.ofInstant(opplysningsperiode.getEnd(), ZoneId.systemDefault()).toLocalDate();
        List<Medlemskapsperiode> medlemskapsperioder = medlemTjeneste.finnMedlemskapPerioder(behandling.getAktørId(), fom, tom);
        ArrayList<MedlemskapPerioderEntitet> resultat = new ArrayList<>();
        for (Medlemskapsperiode medlemskapsperiode : medlemskapsperioder) {
            resultat.add(lagMedlemskapPeriode(medlemskapsperiode));
        }
        return resultat;
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
