package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import static no.nav.foreldrepenger.web.app.tjenester.behandling.svp.SvangerskapsTjenesteFeil.kanIkkeFinneSvangerskapspengerGrunnlagForBehandling;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.svp.SvangerskapsTjenesteFeil.kanIkkeFinneTerminbekreftelsePåSvangerskapspengerSøknad;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFilter;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.registerinnhenting.StønadsperioderInnhenter;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.tilganger.TilgangerTjeneste;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftSvangerskapspengerDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftSvangerskapspengerOppdaterer implements AksjonspunktOppdaterer<BekreftSvangerskapspengerDto> {

    private SvangerskapspengerRepository svangerskapspengerRepository;
    private HistorikkTjenesteAdapter historikkAdapter;
    private FamilieHendelseRepository familieHendelseRepository;
    private TilgangerTjeneste tilgangerTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private StønadsperioderInnhenter stønadsperioderInnhenter;

    BekreftSvangerskapspengerOppdaterer() {
        //CDI
    }

    @Inject
    public BekreftSvangerskapspengerOppdaterer(HistorikkTjenesteAdapter historikkAdapter,
                                               BehandlingGrunnlagRepositoryProvider repositoryProvider,
                                               TilgangerTjeneste tilgangerTjeneste,
                                               InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                               StønadsperioderInnhenter stønadsperioderInnhenter) {
        this.svangerskapspengerRepository = repositoryProvider.getSvangerskapspengerRepository();
        this.historikkAdapter = historikkAdapter;
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.tilgangerTjeneste = tilgangerTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.stønadsperioderInnhenter = stønadsperioderInnhenter;
    }

    @Override
    public OppdateringResultat oppdater(BekreftSvangerskapspengerDto dto, AksjonspunktOppdaterParameter param) {

        verifiserUnikeDatoer(dto);

        var behandling = param.getBehandling();
        var termindatoEndret = oppdaterFamiliehendelse(dto, behandling);
        var tilretteleggingEndret = oppdaterTilrettelegging(dto, behandling);

        oppdaterPermisjon(dto, param, behandling);

        if (termindatoEndret || tilretteleggingEndret) {
            var begrunnelse = dto.getBegrunnelse();
            historikkAdapter.tekstBuilder()
                .medBegrunnelse(begrunnelse, param.erBegrunnelseEndret())
                .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_SVP_INNGANG);
        }

        return OppdateringResultat.utenTransisjon().medTotrinnHvis(termindatoEndret || tilretteleggingEndret).build();

    }

    private void oppdaterPermisjon(BekreftSvangerskapspengerDto dto, AksjonspunktOppdaterParameter param, Behandling behandling) {
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
        var saksbehandlet = InntektArbeidYtelseAggregatBuilder.oppdatere(finnAggregat(iayGrunnlag), VersjonType.SAKSBEHANDLET);
        var aktørArbeidBuilder = saksbehandlet.getAktørArbeidBuilder(param.getAktørId());
        dto.getBekreftetSvpArbeidsforholdList().forEach(arbeidsforhold -> oppdaterPermisjonForArbeid(param, iayGrunnlag, aktørArbeidBuilder, arbeidsforhold));
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);
    }

    private void oppdaterPermisjonForArbeid(AksjonspunktOppdaterParameter param,
                                            InntektArbeidYtelseGrunnlag iayGrunnlag,
                                            InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder,
                                            SvpArbeidsforholdDto arbeidsforhold) {
        var yrkesaktiviteter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(),
            iayGrunnlag.getAktørArbeidFraRegister(param.getAktørId())).getYrkesaktiviteter()
            .stream()
            .filter(ya -> ya.getArbeidsgiver() != null && ya.getArbeidsgiver().getIdentifikator().equals(arbeidsforhold.getArbeidsgiverReferanse()))
            .filter(ya -> ya.getArbeidsforholdRef().gjelderFor(InternArbeidsforholdRef.ref(arbeidsforhold.getInternArbeidsforholdReferanse())))
            .toList();
        yrkesaktiviteter.forEach(yrkesaktivitet -> {
            var velferdspermisjoner = arbeidsforhold.getVelferdspermisjoner();
            if (velferdspermisjoner != null && harEndretPermisjonForYrkesaktivitet(yrkesaktivitet, velferdspermisjoner)) {
                var inkludertePermisjoner = finnGyldigePermisjoner(yrkesaktivitet, velferdspermisjoner);
                var yrkesaktivitetBuilder = aktørArbeidBuilder
                        .getYrkesaktivitetBuilderForNøkkelAvType(Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(yrkesaktivitet.getArbeidsforholdRef(), yrkesaktivitet.getArbeidsgiver()), yrkesaktivitet.getArbeidType())
                        .tilbakestillPermisjon();
                    inkludertePermisjoner.forEach(yrkesaktivitetBuilder::leggTilPermisjon);
            }
        });
    }

    private boolean harEndretPermisjonForYrkesaktivitet(Yrkesaktivitet yrkesaktivitet, List<VelferdspermisjonDto> velferdspermisjoner) {
        return yrkesaktivitet.getPermisjon().stream()
            .anyMatch(p -> PermisjonsbeskrivelseType.VELFERDSPERMISJONER.contains(p.getPermisjonsbeskrivelseType())
                && velferdspermisjoner.stream().anyMatch(vp -> vp.getPermisjonFom().isEqual(p.getFraOgMed()) && vp.getPermisjonsprosent().compareTo(p.getProsentsats().getVerdi()) == 0));
    }

    private List<Permisjon> finnGyldigePermisjoner(Yrkesaktivitet yrkesaktivitet, List<VelferdspermisjonDto> velferdspermisjoner) {
        return yrkesaktivitet.getPermisjon().stream()
                    .filter(p -> !PermisjonsbeskrivelseType.VELFERDSPERMISJONER.contains(p.getPermisjonsbeskrivelseType())
                        || velferdspermisjoner.stream().noneMatch(vp -> vp.getPermisjonFom().isEqual(p.getFraOgMed()) && vp.getPermisjonsprosent().compareTo(p.getProsentsats().getVerdi()) == 0 && !vp.getErGyldig()))
                    .collect(Collectors.toList());
    }

    private Optional<InntektArbeidYtelseAggregat> finnAggregat(InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var saksbehandletVersjon = iayGrunnlag.getSaksbehandletVersjon();
        if (saksbehandletVersjon.isPresent()) {
            return saksbehandletVersjon;
        }
        return iayGrunnlag.getRegisterVersjon();
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

        var svpGrunnlag = svangerskapspengerRepository.hentGrunnlag(behandling.getId()).orElseThrow(() -> {
            throw kanIkkeFinneSvangerskapspengerGrunnlagForBehandling(behandling.getId());
        });
        var oppdatertTilrettelegging = new ArrayList<SvpTilretteleggingEntitet>();
        var bekreftedeArbeidsforholdDtoer = dto.getBekreftetSvpArbeidsforholdList();
        var tilretteleggingerUfiltrert = new TilretteleggingFilter(svpGrunnlag).getAktuelleTilretteleggingerUfiltrert();

        var erMinsteBehovFraDatoEndret = endretMinsteBehovFra(bekreftedeArbeidsforholdDtoer, tilretteleggingerUfiltrert);

        var erEndret = false;
        for (var arbeidsforholdDto : bekreftedeArbeidsforholdDtoer) {
            if (mapTilretteleggingHvisEndret(arbeidsforholdDto, tilretteleggingerUfiltrert, oppdatertTilrettelegging)) {
                erEndret = true;
            }
        }

        if (oppdatertTilrettelegging.size() != tilretteleggingerUfiltrert.size()) {
            throw new TekniskException("FP-564312", "Antall overstyrte arbeidsforhold for svangerskapspenger stemmer "
                + "ikke overens med arbeidsforhold fra søknaden: " + behandling.getId());
        }

        if (erEndret) {
            svangerskapspengerRepository.lagreOverstyrtGrunnlag(behandling, oppdatertTilrettelegging);
            //behov fra dato er stp i saken tidlig i prosessen, og benyttes som start dato når det sjekkes om det finnes en relevant neste sak. Dersom denne endres må utledning av neste sak gjøres på nytt.
            if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType()) && erMinsteBehovFraDatoEndret) {
                stønadsperioderInnhenter.innhentNesteSak(behandling);
            }
        }
        return erEndret;
    }

    private boolean endretMinsteBehovFra(List<SvpArbeidsforholdDto> bekreftedeArbeidsforholdDtoer, List<SvpTilretteleggingEntitet> tilretteleggingerUfiltrert) {
        var minsteBehovFraDatoNy = bekreftedeArbeidsforholdDtoer.stream()
            .map(SvpArbeidsforholdDto::getTilretteleggingBehovFom)
            .min(Comparator.naturalOrder()).orElse(Tid.TIDENES_ENDE);
        var minsteBehovFraDatoGammel = tilretteleggingerUfiltrert.stream()
            .map(SvpTilretteleggingEntitet::getBehovForTilretteleggingFom)
            .min(Comparator.naturalOrder()).orElse(Tid.TIDENES_ENDE);

        return !minsteBehovFraDatoGammel.isEqual(minsteBehovFraDatoNy);
    }

    private boolean mapTilretteleggingHvisEndret(SvpArbeidsforholdDto arbeidsforholdDto,
                                                 List<SvpTilretteleggingEntitet> aktuelleTilrettelegingerListe,
                                                 List<SvpTilretteleggingEntitet> oppdatertTilretteleggingListe) {

        var tilretteleggingId = arbeidsforholdDto.getTilretteleggingId();
        var eksisterendeTilretteleggingEntitet = aktuelleTilrettelegingerListe.stream()
            .filter(a -> a.getId().equals(tilretteleggingId))
            .findFirst()
            .orElseThrow(() -> new TekniskException("FP-572361",
                "Finner ikke eksisterende tilrettelegging på svangerskapspengergrunnlag med identifikator: "
                    + tilretteleggingId));

        var nyTilretteleggingEntitetBuilder = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(arbeidsforholdDto.getTilretteleggingBehovFom())
            .medArbeidType(eksisterendeTilretteleggingEntitet.getArbeidType())
            .medArbeidsgiver(eksisterendeTilretteleggingEntitet.getArbeidsgiver().orElse(null))
            .medBegrunnelse(arbeidsforholdDto.getBegrunnelse())
            .medOpplysningerOmRisikofaktorer(eksisterendeTilretteleggingEntitet.getOpplysningerOmRisikofaktorer().orElse(null))
            .medOpplysningerOmTilretteleggingstiltak(eksisterendeTilretteleggingEntitet.getOpplysningerOmTilretteleggingstiltak().orElse(null))
            .medKopiertFraTidligereBehandling(eksisterendeTilretteleggingEntitet.getKopiertFraTidligereBehandling())
            .medMottattTidspunkt(eksisterendeTilretteleggingEntitet.getMottattTidspunkt())
            .medInternArbeidsforholdRef(InternArbeidsforholdRef.ref(arbeidsforholdDto.getInternArbeidsforholdReferanse()))
            .medSkalBrukes(arbeidsforholdDto.getSkalBrukes());

        for (var datoDto : arbeidsforholdDto.getTilretteleggingDatoer()) {
            if (arbeidsforholdDto.getSkalBrukes() && delvisTilretteleggingUtenStillingsprosent(datoDto)) {
                throw new FunksjonellException("FP-128763", "Manlger Stillingsprosent ved delvis tilrettelegging",
                    "Fyll ut stillingprosent");
            }
            //Sjekk om overstyring av utbetalingsgrad er lovlig
            if (datoDto.getOverstyrtUtbetalingsgrad() != null && !sjekkOmOverstyringErLovlig()) {
                throw new FunksjonellException("FP-682319", "Ansatt har ikke tilgang til å overstyre utbetalingsgrad.",
                    "Ansatt med overstyring rolle må utføre denne endringen.");
            }
            var tilretteleggingFOM = new TilretteleggingFOM.Builder()
                .medTilretteleggingType(datoDto.getType())
                .medFomDato(datoDto.getFom())
                .medStillingsprosent(datoDto.getStillingsprosent())
                .medOverstyrtUtbetalingsgrad(datoDto.getOverstyrtUtbetalingsgrad())
                .build();
            nyTilretteleggingEntitetBuilder.medTilretteleggingFom(tilretteleggingFOM);
        }

        historikkAdapter.tekstBuilder().medSkjermlenke(SkjermlenkeType.PUNKT_FOR_SVP_INNGANG);

        var overstyrtTilretteleggingEntitet = nyTilretteleggingEntitetBuilder.build();
        var erEndret = oppdaterHistorikkHvisTilretteleggingErEndret(eksisterendeTilretteleggingEntitet, overstyrtTilretteleggingEntitet);

        if (erEndret) {
            oppdatertTilretteleggingListe.add(overstyrtTilretteleggingEntitet);
        } else {
            oppdatertTilretteleggingListe.add(eksisterendeTilretteleggingEntitet);
        }

        return erEndret;
    }

    private boolean delvisTilretteleggingUtenStillingsprosent(SvpTilretteleggingDatoDto dto) {
        return dto.getType().equals(TilretteleggingType.DELVIS_TILRETTELEGGING) && dto.getStillingsprosent() == null;
    }

    private boolean sjekkOmOverstyringErLovlig() {
        var innloggetBruker = tilgangerTjeneste.innloggetBruker();
        return innloggetBruker.kanOverstyre();
    }


    private boolean oppdaterHistorikkHvisTilretteleggingErEndret(SvpTilretteleggingEntitet eksisterendeTilrettelegging,
                                                                 SvpTilretteleggingEntitet overstyrtTilrettelegging) {

        var fjernet = eksisterendeTilrettelegging.getTilretteleggingFOMListe().stream()
            .filter(fom -> !overstyrtTilrettelegging.getTilretteleggingFOMListe().contains(fom))
            .toList();
        var lagtTil = overstyrtTilrettelegging.getTilretteleggingFOMListe().stream()
            .filter(fom -> !eksisterendeTilrettelegging.getTilretteleggingFOMListe().contains(fom))
            .toList();

        for (var fomFjernet : fjernet) {
            historikkAdapter.tekstBuilder().medEndretFelt(finnHistorikkFeltType(fomFjernet.getType()), formaterForHistorikk(fomFjernet), "fjernet");
        }
        for (var fomLagtTil : lagtTil) {
            historikkAdapter.tekstBuilder().medEndretFelt(finnHistorikkFeltType(fomLagtTil.getType()), null, formaterForHistorikk(fomLagtTil));
        }

        var erEndretBehovFom =  !Objects.equals(eksisterendeTilrettelegging.getBehovForTilretteleggingFom(), overstyrtTilrettelegging.getBehovForTilretteleggingFom());
        if (erEndretBehovFom) {
            historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.TILRETTELEGGING_BEHOV_FOM, eksisterendeTilrettelegging.getBehovForTilretteleggingFom(), overstyrtTilrettelegging.getBehovForTilretteleggingFom());
        }

        var erEndretSkalBrukes = eksisterendeTilrettelegging.getSkalBrukes() != overstyrtTilrettelegging.getSkalBrukes();

        if (erEndretSkalBrukes) {
            historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.TILRETTELEGGING_SKAL_BRUKES, eksisterendeTilrettelegging.getSkalBrukes() ? "JA" : "NEI", overstyrtTilrettelegging.getSkalBrukes() ? "JA" : "NEI");
        }

        return !fjernet.isEmpty() || !lagtTil.isEmpty() || erEndretBehovFom || erEndretSkalBrukes;
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
            final var oppdatertOverstyrtHendelse = familieHendelseRepository.opprettBuilderFor(behandling);
            oppdatertOverstyrtHendelse.tilbakestillBarn();
            if (dto.getFødselsdato() != null) {
                oppdatertOverstyrtHendelse.medFødselsDato(dto.getFødselsdato()).medAntallBarn(1);
            }

            familieHendelseRepository.lagreOverstyrtHendelse(behandling, oppdatertOverstyrtHendelse);
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
            final var oppdatertOverstyrtHendelse = familieHendelseRepository.opprettBuilderFor(behandling);
            oppdatertOverstyrtHendelse
                .medTerminbekreftelse(oppdatertOverstyrtHendelse.getTerminbekreftelseBuilder()
                    .medTermindato(dto.getTermindato()));
            familieHendelseRepository.lagreOverstyrtHendelse(behandling, oppdatertOverstyrtHendelse);
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

    @Override
    public boolean skalReinnhenteRegisteropplysninger(Long behandlingId, LocalDate forrigeSkjæringstidspunkt) {
        return false;
    }
}
