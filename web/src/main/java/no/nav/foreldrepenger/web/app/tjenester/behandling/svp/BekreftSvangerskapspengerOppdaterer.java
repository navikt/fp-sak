package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import static no.nav.foreldrepenger.web.app.tjenester.behandling.svp.SvangerskapsTjenesteFeil.kanIkkeFinneSvangerskapspengerGrunnlagForBehandling;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.svp.SvangerskapsTjenesteFeil.kanIkkeFinneTerminbekreftelsePåSvangerskapspengerSøknad;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpAvklartOpphold;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.registerinnhenting.StønadsperioderInnhenter;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftSvangerskapspengerDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftSvangerskapspengerOppdaterer implements AksjonspunktOppdaterer<BekreftSvangerskapspengerDto> {

    private static final DateTimeFormatter DATO_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private HistorikkTjenesteAdapter historikkAdapter;
    private FamilieHendelseRepository familieHendelseRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private StønadsperioderInnhenter stønadsperioderInnhenter;
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste;
    private BehandlingRepository behandlingRepository;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;

    BekreftSvangerskapspengerOppdaterer() {
        //CDI
    }

    @Inject
    public BekreftSvangerskapspengerOppdaterer(HistorikkTjenesteAdapter historikkAdapter,
                                               BehandlingGrunnlagRepositoryProvider repositoryProvider,
                                               InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                               StønadsperioderInnhenter stønadsperioderInnhenter,
                                               ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste,
                                               BehandlingRepository behandlingRepository,
                                               OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste) {
        this.svangerskapspengerRepository = repositoryProvider.getSvangerskapspengerRepository();
        this.historikkAdapter = historikkAdapter;
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.stønadsperioderInnhenter = stønadsperioderInnhenter;
        this.arbeidsforholdAdministrasjonTjeneste = arbeidsforholdAdministrasjonTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(BekreftSvangerskapspengerDto dto, AksjonspunktOppdaterParameter param) {

        verifiserUnikeDatoer(dto);
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        var forrigeFikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandling.getId(), param.getRef().fagsakYtelseType());
        var termindatoEndret = oppdaterFamiliehendelse(dto, behandling);
        var tilretteleggingEndret = oppdaterTilrettelegging(dto, behandling);
        oppdaterPermisjonVedBehov(dto, param);

        if (termindatoEndret || tilretteleggingEndret) {
            var begrunnelse = dto.getBegrunnelse();
            historikkAdapter.tekstBuilder()
                .medBegrunnelse(begrunnelse, param.erBegrunnelseEndret())
                .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_SVP_INNGANG);
            var sistefikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandling.getId(), param.getRef().fagsakYtelseType());
            if (Objects.equals(forrigeFikspunkt, sistefikspunkt)) {
                return OppdateringResultat.utenTransisjon().medTotrinn().build();
            } else {
                return OppdateringResultat.utenTransisjon().medTotrinn().medOppdaterGrunnlag().build();
            }
        }

        return OppdateringResultat.utenTransisjon().build();
    }

    private List<SvpTilretteleggingEntitet> hentGjeldendeTilrettelegginger(Behandling behandling) {
        return  svangerskapspengerRepository.hentGrunnlag(behandling.getId())
            .orElseThrow( () -> kanIkkeFinneSvangerskapspengerGrunnlagForBehandling(behandling.getId()))
            .getGjeldendeVersjon()
            .getTilretteleggingListe();
    }

    private void oppdaterPermisjonVedBehov(BekreftSvangerskapspengerDto dto, AksjonspunktOppdaterParameter param) {
        var infoBuilder = arbeidsforholdAdministrasjonTjeneste.opprettBuilderFor(param.getBehandlingId());
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId());

        var yrkesfilter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(),
            iayGrunnlag.getAktørArbeidFraRegister(param.getAktørId()));
        var oppdaterePermisjonBuildere = dto.getBekreftetSvpArbeidsforholdList()
            .stream()
            .filter(this::harGjortPermisjonsvalg)
            .map(aktivitet -> lagPermisjonsoppdatering(aktivitet, infoBuilder, yrkesfilter))
            .flatMap(Collection::stream)
            .toList();
        if (!oppdaterePermisjonBuildere.isEmpty()) {
            oppdaterePermisjonBuildere.forEach(infoBuilder::leggTil);
            arbeidsforholdAdministrasjonTjeneste.lagreOverstyring(param.getBehandlingId(), infoBuilder);
        }
    }

    private boolean harGjortPermisjonsvalg(SvpArbeidsforholdDto aktivitet) {
        return aktivitet.getVelferdspermisjoner().stream()
            .anyMatch(p -> PermisjonsbeskrivelseType.VELFERDSPERMISJONER.contains(p.getType()) && p.getErGyldig() != null);
    }

    private List<ArbeidsforholdOverstyringBuilder> lagPermisjonsoppdatering(SvpArbeidsforholdDto aktivitet,
                                                                            ArbeidsforholdInformasjonBuilder infoBuilder,
                                                                            YrkesaktivitetFilter yrkesfilter) {
        var yrkesaktiviteter = yrkesfilter.getYrkesaktiviteter()
            .stream()
            .filter(ya -> ya.getArbeidsgiver() != null && ya.getArbeidsgiver().getIdentifikator().equals(aktivitet.getArbeidsgiverReferanse()))
            .filter(ya -> ya.getArbeidsforholdRef().gjelderFor(InternArbeidsforholdRef.ref(aktivitet.getInternArbeidsforholdReferanse())))
            .toList();
        if (yrkesaktiviteter.isEmpty()) {
            var feilmelding = String.format("Forventet minst 1 matchende yrkesaktivitet , fant 0. Gjelder aktivitet med arbeidsgiverIdent %s og arbeidsforholdId %s",
                aktivitet.getArbeidsgiverReferanse(), aktivitet.getInternArbeidsforholdReferanse());
            throw new IllegalStateException(feilmelding);
        }
        List<ArbeidsforholdOverstyringBuilder> listeMedEndredeArbeidsforhold = new ArrayList<>();
        yrkesaktiviteter.forEach(yrkesaktivitet -> {
            var vurdertPermisjon = finnPermisjonSomErVurdert(aktivitet.getVelferdspermisjoner(), yrkesaktivitet);
            if (vurdertPermisjon.isPresent()) {
                var permFraIAY = finnMatchendePermisjonIIAY(yrkesaktivitet, vurdertPermisjon.get()).orElseThrow();
                infoBuilder.fjernOverstyringVedrørende(yrkesaktivitet.getArbeidsgiver(), yrkesaktivitet.getArbeidsforholdRef());
                var yaBuilder = infoBuilder.getOverstyringBuilderFor(yrkesaktivitet.getArbeidsgiver(), yrkesaktivitet.getArbeidsforholdRef());
                yaBuilder.medBekreftetPermisjon(new BekreftetPermisjon(vurdertPermisjon.get().getPermisjonFom(), permFraIAY.getTilOgMed(), finnStatus(vurdertPermisjon.get())))
                    .medHandling(ArbeidsforholdHandlingType.BRUK);
                listeMedEndredeArbeidsforhold.add(yaBuilder);

            }
        });
        return listeMedEndredeArbeidsforhold;
    }

    private BekreftetPermisjonStatus finnStatus(VelferdspermisjonDto vp) {
        return Boolean.TRUE.equals(vp.getErGyldig()) ? BekreftetPermisjonStatus.BRUK_PERMISJON : BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON;
    }

    private Optional<VelferdspermisjonDto> finnPermisjonSomErVurdert(List<VelferdspermisjonDto> velferdspermisjoner, Yrkesaktivitet yrkesaktivitet) {
        return velferdspermisjoner.stream()
            .filter(bekreftetPerm -> finnMatchendePermisjonIIAY(yrkesaktivitet, bekreftetPerm).isPresent())
            .filter(bekreftetPerm -> bekreftetPerm.getErGyldig() != null)
            .findFirst();
    }

    private static Optional<Permisjon> finnMatchendePermisjonIIAY(Yrkesaktivitet yrkesaktivitet, VelferdspermisjonDto bekreftetPerm) {
        return yrkesaktivitet.getPermisjon()
            .stream()
            .filter(yaPerm -> yaPerm.getFraOgMed().equals(bekreftetPerm.getPermisjonFom())
                && yaPerm.getProsentsats().getVerdi().compareTo(bekreftetPerm.getPermisjonsprosent()) == 0
                && Objects.equals(yaPerm.getPermisjonsbeskrivelseType(), bekreftetPerm.getType()))
            .findFirst();
    }

    private void verifiserUnikeDatoer(BekreftSvangerskapspengerDto dto) {
        var bekreftedeArbeidsforhold = dto.getBekreftetSvpArbeidsforholdList();
        for (var arbeidsforhold : bekreftedeArbeidsforhold) {
            if (arbeidsforhold.getSkalBrukes()) {
                var harUliktAntallTilretteleggingDatoer = arbeidsforhold.getTilretteleggingDatoer().stream()
                    .map(SvpTilretteleggingDatoDto::getFom)
                    .collect(Collectors.toSet())
                    .size() != arbeidsforhold.getTilretteleggingDatoer().size();
                if (harUliktAntallTilretteleggingDatoer) {
                    throw new FunksjonellException("FP-682318", "Forskjellige tilretteleggingstyper i ett arbeidsforhold "
                        + "kan ikke løpe fra samme dato", "Avklar riktige FOM datoer for alle tilrettelegginger");
                }
            }
        }
    }

    boolean oppdaterTilrettelegging(BekreftSvangerskapspengerDto dto, Behandling behandling) {
        var bekreftedeArbeidsforholdDtoer = dto.getBekreftetSvpArbeidsforholdList();
        var eksisterendeTilrettelegginger = hentGjeldendeTilrettelegginger(behandling);

        var nyeTilrettelegginger = new ArrayList<SvpTilretteleggingEntitet>();

        var harSaksbehandlerGjortEndringer = bekreftedeArbeidsforholdDtoer.stream().anyMatch(svpArbeidsforholdDto -> tilretteleggingErEndret(svpArbeidsforholdDto, eksisterendeTilrettelegginger));

        if (harSaksbehandlerGjortEndringer) {
            bekreftedeArbeidsforholdDtoer.forEach(
                svpArbeidsforholdDto ->  {
                    var tilrettelegging = mapTilretteleggingOgLagHistorikk(svpArbeidsforholdDto, eksisterendeTilrettelegginger);
                    nyeTilrettelegginger.add(tilrettelegging);
                }
            );

            if (nyeTilrettelegginger.size() != eksisterendeTilrettelegginger.size()) {
                throw new TekniskException("FP-564312", "Antall overstyrte arbeidsforhold for svangerskapspenger stemmer "
                    + "ikke overens med arbeidsforhold fra søknaden: " + behandling.getId());
            }

            svangerskapspengerRepository.lagreOverstyrtGrunnlag(behandling, nyeTilrettelegginger);

            //behov fra dato er stp i saken tidlig i prosessen, og benyttes som start dato når det sjekkes om det finnes en relevant neste sak. Dersom denne endres må utledning av neste sak gjøres på nytt.
            var erFørsteBehovFraDatoEndret = endretFørsteBehovFra(bekreftedeArbeidsforholdDtoer, eksisterendeTilrettelegginger);
            if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType()) && erFørsteBehovFraDatoEndret) {
                stønadsperioderInnhenter.innhentNesteSak(behandling);
            }
        }
        return harSaksbehandlerGjortEndringer;
    }

    private boolean endretFørsteBehovFra(List<SvpArbeidsforholdDto> bekreftedeArbeidsforholdDtoer, List<SvpTilretteleggingEntitet> tilretteleggingerUfiltrert) {
        var førsteBehovFraDatoNy = bekreftedeArbeidsforholdDtoer.stream()
            .map(SvpArbeidsforholdDto::getTilretteleggingBehovFom)
            .min(Comparator.naturalOrder()).orElse(Tid.TIDENES_ENDE);
        var førsteBehovFraDatoGammel = tilretteleggingerUfiltrert.stream()
            .map(SvpTilretteleggingEntitet::getBehovForTilretteleggingFom)
            .min(Comparator.naturalOrder()).orElse(Tid.TIDENES_ENDE);

        return !førsteBehovFraDatoGammel.isEqual(førsteBehovFraDatoNy);
    }

    private boolean tilretteleggingErEndret(SvpArbeidsforholdDto arbeidsforholdDto,
                                            List<SvpTilretteleggingEntitet> eksisterendeTilrettelegingerListe) {
        var eksisterendeTilrettelegging = hentEksisterendeTilrettelegging(eksisterendeTilrettelegingerListe, arbeidsforholdDto.getTilretteleggingId());
        var nyTilrettelegging = mapNyTilrettelegging(arbeidsforholdDto, eksisterendeTilrettelegging);

        return  erTilretteleggingEndret(eksisterendeTilrettelegging, nyTilrettelegging);
    }

    private boolean erTilretteleggingEndret(SvpTilretteleggingEntitet eksisterendeTilrettelegging, SvpTilretteleggingEntitet nyTilrettelegging) {
        var nyFomsSortert = sorterFoms(nyTilrettelegging.getTilretteleggingFOMListe());
        var eksFomsSortert = sorterFoms(eksisterendeTilrettelegging.getTilretteleggingFOMListe());
        var nyOppholdSortert = sorterOpphold(nyTilrettelegging.getAvklarteOpphold());
        var eksOppholdSortert = sorterOpphold(eksisterendeTilrettelegging.getAvklarteOpphold());

       return nyTilrettelegging.getSkalBrukes() != eksisterendeTilrettelegging.getSkalBrukes()
            || !nyFomsSortert.equals(eksFomsSortert)
            || !Objects.equals(eksisterendeTilrettelegging.getBehovForTilretteleggingFom(), nyTilrettelegging.getBehovForTilretteleggingFom())
            || !nyOppholdSortert.equals(eksOppholdSortert);
    }

    private List<TilretteleggingFOM> sorterFoms(List<TilretteleggingFOM> tilretteleggingFOM) {
        return tilretteleggingFOM.stream()
            .sorted(Comparator.comparing(TilretteleggingFOM::getFomDato))
            .toList();
    }

    private List<SvpAvklartOpphold> sorterOpphold(List<SvpAvklartOpphold> tilretteleggingFOM) {
        return tilretteleggingFOM.stream()
            .sorted(Comparator.comparing(SvpAvklartOpphold::getFom))
            .toList();
    }


    private SvpTilretteleggingEntitet mapTilretteleggingOgLagHistorikk(SvpArbeidsforholdDto arbeidsforholdDto,
                                                                       List<SvpTilretteleggingEntitet> eksisterendeTilrettelegingerListe) {
        var eksisterendeTilrettelegging = hentEksisterendeTilrettelegging(eksisterendeTilrettelegingerListe, arbeidsforholdDto.getTilretteleggingId());
        var nyTilrettelegging = mapNyTilrettelegging(arbeidsforholdDto, eksisterendeTilrettelegging);

        if (erTilretteleggingEndret(eksisterendeTilrettelegging, nyTilrettelegging)) {
            opprettHistorikkInnslagForEndringer(eksisterendeTilrettelegging, nyTilrettelegging);
            return nyTilrettelegging;
        }

        return eksisterendeTilrettelegging;
    }

    private SvpTilretteleggingEntitet hentEksisterendeTilrettelegging(List<SvpTilretteleggingEntitet> eksisterendeTilrettelegingerListe, Long tilretteleggingId) {
        return eksisterendeTilrettelegingerListe.stream()
            .filter(a -> a.getId().equals(tilretteleggingId))
            .findFirst()
            .orElseThrow(() -> new TekniskException("FP-572361",
                "Finner ikke eksisterende tilrettelegging på svangerskapspengergrunnlag med identifikator: "
                    + tilretteleggingId));
    }

    private SvpTilretteleggingEntitet mapNyTilrettelegging(SvpArbeidsforholdDto arbeidsforholdDto, SvpTilretteleggingEntitet eksisterendeTilrettelegging) {
        var nyTilretteleggingEntitetBuilder = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(arbeidsforholdDto.getTilretteleggingBehovFom())
            .medArbeidType(eksisterendeTilrettelegging.getArbeidType())
            .medArbeidsgiver(eksisterendeTilrettelegging.getArbeidsgiver().orElse(null))
            .medBegrunnelse(arbeidsforholdDto.getBegrunnelse())
            .medOpplysningerOmRisikofaktorer(eksisterendeTilrettelegging.getOpplysningerOmRisikofaktorer().orElse(null))
            .medOpplysningerOmTilretteleggingstiltak(eksisterendeTilrettelegging.getOpplysningerOmTilretteleggingstiltak().orElse(null))
            .medKopiertFraTidligereBehandling(eksisterendeTilrettelegging.getKopiertFraTidligereBehandling())
            .medMottattTidspunkt(eksisterendeTilrettelegging.getMottattTidspunkt())
            .medInternArbeidsforholdRef(InternArbeidsforholdRef.ref(arbeidsforholdDto.getInternArbeidsforholdReferanse()))
            .medSkalBrukes(arbeidsforholdDto.getSkalBrukes());

        //nye tilrettelegging-fra-datoer per arbeidsforhold
        for (var datoDto : arbeidsforholdDto.getTilretteleggingDatoer()) {
            if (arbeidsforholdDto.getSkalBrukes() && delvisTilretteleggingUtenStillingsprosentOgIkkeOverstyrt(datoDto)) {
                throw new FunksjonellException("FP-128763", "Verken arbeidsprosent eller overstyrt utbetalingsgrad opgitt ved delvis tilrettelegging",
                    "Fyll ut enten arbeidsprosent eller endre oppgitt utbetalingsgrad");
            }

            var nyTilretteleggingFOM= new TilretteleggingFOM.Builder()
                .medTilretteleggingType(datoDto.getType())
                .medFomDato(datoDto.getFom())
                .medStillingsprosent(datoDto.getStillingsprosent())
                .medOverstyrtUtbetalingsgrad(datoDto.getOverstyrtUtbetalingsgrad())
                .medTidligstMottattDato(utledTidligstMotattFraEks(datoDto, eksisterendeTilrettelegging))
                .medKilde(datoDto.getKilde())
                .build();

            nyTilretteleggingEntitetBuilder.medTilretteleggingFom(nyTilretteleggingFOM);
        }

        if (arbeidsforholdDto.getAvklarteOppholdPerioder() != null) {
            arbeidsforholdDto.getAvklarteOppholdPerioder().stream()
                .filter(oppholdDto -> !oppholdDto.forVisning()) //Vi viser opphold fra IM - disse skal ikke lagres i grunnlaget
                .forEach(oppholdDto -> {
                    var kilde = switch (oppholdDto.oppholdKilde()) {
                        case SØKNAD -> SvpOppholdKilde.SØKNAD;
                        case REGISTRERT_AV_SAKSBEHANDLER -> SvpOppholdKilde.REGISTRERT_AV_SAKSBEHANDLER;
                        case INNTEKTSMELDING -> throw new IllegalStateException("Kan ikke lagre som saksbehandlet oppholdsperioder fra inntektsmelding");
                        case null -> SvpOppholdKilde.REGISTRERT_AV_SAKSBEHANDLER;
                    };
                    var nyttAvklartOpphold = SvpAvklartOpphold.Builder.nytt()
                        .medOppholdPeriode(oppholdDto.fom(), oppholdDto.tom())
                        .medOppholdÅrsak(oppholdDto.oppholdÅrsak())
                        .medKilde(kilde)
                        .build();
                    nyTilretteleggingEntitetBuilder.medAvklartOpphold(nyttAvklartOpphold);
            });
        }
        return nyTilretteleggingEntitetBuilder.build();
    }

    private LocalDate utledTidligstMotattFraEks(SvpTilretteleggingDatoDto datoDto, SvpTilretteleggingEntitet eksisterendeTilretteleggingEntitet) {
        var eksisterendeTilrettleggingFoms = eksisterendeTilretteleggingEntitet.getTilretteleggingFOMListe();
        var tidligstMotattDato = eksisterendeTilrettleggingFoms.stream()
            .map(TilretteleggingFOM::getTidligstMotattDato)
            .filter(Objects::nonNull)
            .min(LocalDate::compareTo)
            .orElse(eksisterendeTilretteleggingEntitet.getMottattTidspunkt().toLocalDate());

        return  eksisterendeTilrettleggingFoms.stream()
            .filter(eksFraDato -> datoDto.getFom().isEqual(eksFraDato.getFomDato()))
            .findFirst()
            .map(TilretteleggingFOM::getTidligstMotattDato)
            .orElse(tidligstMotattDato);
    }

    private boolean delvisTilretteleggingUtenStillingsprosentOgIkkeOverstyrt(SvpTilretteleggingDatoDto dto) {
        return dto.getType().equals(TilretteleggingType.DELVIS_TILRETTELEGGING) && dto.getStillingsprosent() == null && dto.getOverstyrtUtbetalingsgrad() == null;
    }

    private void opprettHistorikkInnslagForEndringer(SvpTilretteleggingEntitet eksisterendeTilrettelegging,
                                                     SvpTilretteleggingEntitet nyTilrettelegging) {
        var fjernet = "fjernet";

        historikkAdapter.tekstBuilder().medSkjermlenke(SkjermlenkeType.PUNKT_FOR_SVP_INNGANG);

        //Tilrettelegging fra datoer
        var eksisterendeFoms = eksisterendeTilrettelegging.getTilretteleggingFOMListe();
        var nyeFoms = nyTilrettelegging.getTilretteleggingFOMListe();
        var fjernetListe = eksisterendeFoms.stream().filter(fom -> !nyeFoms.contains(fom)).toList();
        var lagtTilListe = nyeFoms.stream().filter(fom -> !eksisterendeFoms.contains(fom)).toList();

        fjernetListe.forEach(fomFjernet -> historikkAdapter.tekstBuilder().medEndretFelt(finnHistorikkFeltType(fomFjernet.getType()), formaterForHistorikk(fomFjernet), fjernet));
        lagtTilListe.forEach(fomLagtTil -> historikkAdapter.tekstBuilder().medEndretFelt(finnHistorikkFeltType(fomLagtTil.getType()), null, formaterForHistorikk(fomLagtTil)));

        //Oppholdsperioder
        var eksisterendeOpphold = eksisterendeTilrettelegging.getAvklarteOpphold();
        var nyeOpphold = nyTilrettelegging.getAvklarteOpphold();

        // frontend sender ikke oppdateringer på kilde per nå
        BiPredicate<SvpAvklartOpphold, SvpAvklartOpphold> equalssjekkUtenKilde = (a, b) ->
            Objects.equals(a.getOppholdÅrsak(), b.getOppholdÅrsak())
                && Objects.equals(a.getFom(), b.getFom())
                && Objects.equals(a.getTom(), b.getTom());

        var fjernetOppholdListe = eksisterendeOpphold.stream()
            .filter(eksisterende -> nyeOpphold.stream()
                .noneMatch(nyttOpphold -> equalssjekkUtenKilde.test(nyttOpphold, eksisterende)))
            .toList();
        var lagtTilOppholdListe = nyeOpphold.stream()
            .filter(nyttOpphold -> eksisterendeOpphold.stream()
                .noneMatch(eksisterende -> equalssjekkUtenKilde.test(eksisterende, nyttOpphold)))
            .toList();
        // TODO: vurder ren equalssjekk og sette kilde i frontend ved oppdateringer
        lagtTilOppholdListe.forEach(nyttOpphold -> nyttOpphold.setKilde(SvpOppholdKilde.REGISTRERT_AV_SAKSBEHANDLER));

        lagtTilOppholdListe.forEach(lagtTilOpphold -> historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.SVP_OPPHOLD_PERIODE, null,
                "nytt opphold " + formatterOppholdDetaljerForHistorikk(lagtTilOpphold)));
        fjernetOppholdListe.forEach(fjernetOpphold -> historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.SVP_OPPHOLD_PERIODE, formatterOppholdDetaljerForHistorikk(fjernetOpphold), fjernet));

        var erEndretBehovFom =  !Objects.equals(eksisterendeTilrettelegging.getBehovForTilretteleggingFom(), nyTilrettelegging.getBehovForTilretteleggingFom());
        if (erEndretBehovFom) {
            historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.TILRETTELEGGING_BEHOV_FOM, eksisterendeTilrettelegging.getBehovForTilretteleggingFom(), nyTilrettelegging.getBehovForTilretteleggingFom());
        }

        var erEndretSkalBrukes = eksisterendeTilrettelegging.getSkalBrukes() != nyTilrettelegging.getSkalBrukes();

        if (erEndretSkalBrukes) {
            historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.TILRETTELEGGING_SKAL_BRUKES, eksisterendeTilrettelegging.getSkalBrukes() ? "JA" : "NEI", nyTilrettelegging.getSkalBrukes() ? "JA" : "NEI");
        }
    }

    private String formatterOppholdDetaljerForHistorikk(SvpAvklartOpphold opphold) {
        var formattert = String.format("%s – %s, årsak: %s", DATO_FORMATTER.format(opphold.getFom()), DATO_FORMATTER.format(opphold.getTom()),
            opphold.getOppholdÅrsak());
        if (opphold.getKilde() == SvpOppholdKilde.SØKNAD) {
            formattert = formattert + ", kilde: " + SvpOppholdKilde.SØKNAD;
        }
        return formattert;
    }

    private String formaterForHistorikk(TilretteleggingFOM fom) {
        var historikk = new StringBuilder().append(fom.getType().getNavn()).append(" fom: ").append(fom.getFomDato());
        if (TilretteleggingType.DELVIS_TILRETTELEGGING.equals(fom.getType())) {
            historikk.append(", Stillingsprosent: ").append(fom.getStillingsprosent());
        }
        if (fom.getOverstyrtUtbetalingsgrad() != null) {
            historikk.append(", Overstyrt utbetalingsgrad: ").append(fom.getOverstyrtUtbetalingsgrad());
        }
        return historikk.toString();
    }

    private HistorikkEndretFeltType finnHistorikkFeltType(TilretteleggingType tilretteleggingType) {
        if (TilretteleggingType.HEL_TILRETTELEGGING.equals(tilretteleggingType)) {
            return HistorikkEndretFeltType.HEL_TILRETTELEGGING_FOM;
        }
        if (TilretteleggingType.DELVIS_TILRETTELEGGING.equals(tilretteleggingType)) {
            return HistorikkEndretFeltType.DELVIS_TILRETTELEGGING_FOM;
        }
        if (TilretteleggingType.INGEN_TILRETTELEGGING.equals(tilretteleggingType)) {
            return HistorikkEndretFeltType.SLUTTE_ARBEID_FOM;
        }
        throw new IllegalStateException("Ukjent Tilrettelegingstype");
    }

    private boolean oppdaterFamiliehendelse(BekreftSvangerskapspengerDto dto, Behandling behandling) {
        var behandlingId = behandling.getId();
        var grunnlag = familieHendelseRepository.hentAggregat(behandlingId);

        var termindatoOppdatert = oppdaterTermindato(dto, behandling, grunnlag);
        var fødselsdatoOppdatert = oppdaterFødselsdato(dto, behandling, grunnlag);

        return termindatoOppdatert || fødselsdatoOppdatert;
    }

    private boolean oppdaterFødselsdato(BekreftSvangerskapspengerDto dto, Behandling behandling, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        var orginalFødselsdato = getFødselsdato(familieHendelseGrunnlag);

        var erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.FODSELSDATO, orginalFødselsdato.orElse(null), dto.getFødselsdato());

        if (erEndret) {
            var oppdatertOverstyrtHendelse = familieHendelseRepository.opprettBuilderFor(behandling.getId());
            oppdatertOverstyrtHendelse.tilbakestillBarn();
            if (dto.getFødselsdato() != null) {
                oppdatertOverstyrtHendelse.medFødselsDato(dto.getFødselsdato()).medAntallBarn(1);
            }

            familieHendelseRepository.lagreOverstyrtHendelse(behandling.getId(), oppdatertOverstyrtHendelse);
        }
        return erEndret;
    }

    private Optional<LocalDate> getFødselsdato(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        return familieHendelseGrunnlag.getGjeldendeVersjon().getFødselsdato();
    }

    private boolean oppdaterTermindato(BekreftSvangerskapspengerDto dto, Behandling behandling, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {

        var orginalTermindato = getTermindato(familieHendelseGrunnlag, behandling);
        var erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.TERMINDATO, orginalTermindato, dto.getTermindato());

        if (erEndret) {
            var oppdatertOverstyrtHendelse = familieHendelseRepository.opprettBuilderFor(behandling.getId());
            oppdatertOverstyrtHendelse
                .medTerminbekreftelse(oppdatertOverstyrtHendelse.getTerminbekreftelseBuilder()
                    .medTermindato(dto.getTermindato()));
            familieHendelseRepository.lagreOverstyrtHendelse(behandling.getId(), oppdatertOverstyrtHendelse);
        }
        return erEndret;
    }

    private LocalDate getTermindato(FamilieHendelseGrunnlagEntitet grunnlag, Behandling behandling) {
        return getGjeldendeTerminbekreftelse(grunnlag, behandling).getTermindato();
    }

    private TerminbekreftelseEntitet getGjeldendeTerminbekreftelse(FamilieHendelseGrunnlagEntitet grunnlag, Behandling behandling) {
        return grunnlag.getGjeldendeTerminbekreftelse()
            .orElseThrow(() -> kanIkkeFinneTerminbekreftelsePåSvangerskapspengerSøknad(behandling.getId()));
    }

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType, LocalDate original, LocalDate bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType,
                original != null ? HistorikkInnslagTekstBuilder.formatString(original) : "Ingen verdi",
                bekreftet != null ? HistorikkInnslagTekstBuilder.formatString(bekreftet) : "Ingen verdi");
            return true;
        }
        return false;
    }

}
