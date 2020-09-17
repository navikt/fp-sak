package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFilter;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.tilganger.TilgangerTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftSvangerskapspengerDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftSvangerskapspengerOppdaterer implements AksjonspunktOppdaterer<BekreftSvangerskapspengerDto> {

    private SvangerskapspengerRepository svangerskapspengerRepository;
    private HistorikkTjenesteAdapter historikkAdapter;
    private BehandlingRepositoryProvider repositoryProvider;
    private FamilieHendelseRepository familieHendelseRepository;
    private TilgangerTjeneste tilgangerTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Inject
    public BekreftSvangerskapspengerOppdaterer(SvangerskapspengerRepository svangerskapspengerRepository,
                                               HistorikkTjenesteAdapter historikkAdapter,
                                               BehandlingRepositoryProvider repositoryProvider,
                                               FamilieHendelseRepository familieHendelseRepository,
                                               TilgangerTjeneste tilgangerTjeneste,
                                               InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.historikkAdapter = historikkAdapter;
        this.repositoryProvider = repositoryProvider;
        this.familieHendelseRepository = familieHendelseRepository;
        this.tilgangerTjeneste = tilgangerTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(BekreftSvangerskapspengerDto dto, AksjonspunktOppdaterParameter param) {

        verifiserUnikeDatoer(dto);

        var behandling = param.getBehandling();
        boolean termindatoEndret = oppdaterFamiliehendelse(dto, behandling);
        boolean tilretteleggingEndret = oppdaterTilrettelegging(dto, behandling);

        oppdaterPermisjon(dto, param, behandling);

        if (termindatoEndret || tilretteleggingEndret) {
            var begrunnelse = dto.getBegrunnelse();
            boolean erBegrunnelseEndret = param.erBegrunnelseEndret();
            historikkAdapter.tekstBuilder()
                .medBegrunnelse(begrunnelse, erBegrunnelseEndret)
                .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_SVP_INNGANG);
        }

        return OppdateringResultat.utenTransisjon().medTotrinnHvis(termindatoEndret || tilretteleggingEndret).build();

    }

    private void oppdaterPermisjon(BekreftSvangerskapspengerDto dto, AksjonspunktOppdaterParameter param, Behandling behandling) {
        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
        InntektArbeidYtelseAggregatBuilder saksbehandlet = InntektArbeidYtelseAggregatBuilder.oppdatere(finnAggregat(iayGrunnlag), VersjonType.SAKSBEHANDLET);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = saksbehandlet.getAktørArbeidBuilder(param.getAktørId());
        dto.getBekreftetSvpArbeidsforholdList().forEach(arbeidsforhold -> oppdaterPermisjonForArbeid(param, iayGrunnlag, aktørArbeidBuilder, arbeidsforhold));
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);
    }

    private void oppdaterPermisjonForArbeid(AksjonspunktOppdaterParameter param,
                                            InntektArbeidYtelseGrunnlag iayGrunnlag,
                                            InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder,
                                            SvpArbeidsforholdDto arbeidsforhold) {
        Optional<Yrkesaktivitet> yrkesaktivitet = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(param.getAktørId()))
            .getYrkesaktiviteter().stream()
            .filter(ya -> ya.getArbeidsgiver() != null && ya.getArbeidsgiver().getIdentifikator().equals(arbeidsforhold.getArbeidsgiverIdent()))
            .filter(ya -> ya.getArbeidsforholdRef().gjelderFor(InternArbeidsforholdRef.ref(arbeidsforhold.getInternArbeidsforholdReferanse())))
            .findFirst();
        List<VelferdspermisjonDto> velferdspermisjoner = arbeidsforhold.getVelferdspermisjoner();
        if (yrkesaktivitet.isPresent() && velferdspermisjoner != null) {
            List<Permisjon> inkludertePermisjoner = finnGyldigePermisjoner(yrkesaktivitet.get(), velferdspermisjoner);
            YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder
                .getYrkesaktivitetBuilderForNøkkelAvType(Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(yrkesaktivitet.get().getArbeidsforholdRef(), yrkesaktivitet.get().getArbeidsgiver()), yrkesaktivitet.get().getArbeidType())
                .tilbakestillPermisjon();
            inkludertePermisjoner.forEach(yrkesaktivitetBuilder::leggTilPermisjon);
        }
    }

    private List<Permisjon> finnGyldigePermisjoner(Yrkesaktivitet yrkesaktivitet, List<VelferdspermisjonDto> velferdspermisjoner) {
        return yrkesaktivitet.getPermisjon().stream()
                    .filter(p -> !p.getPermisjonsbeskrivelseType().equals(PermisjonsbeskrivelseType.VELFERDSPERMISJON)
                        || velferdspermisjoner.stream().noneMatch(vp -> vp.getPermisjonFom().isEqual(p.getFraOgMed()) && vp.getPermisjonsprosent().compareTo(p.getProsentsats().getVerdi()) == 0 && !vp.getErGyldig()))
                    .collect(Collectors.toList());
    }

    private Optional<InntektArbeidYtelseAggregat> finnAggregat(InntektArbeidYtelseGrunnlag iayGrunnlag) {
        Optional<InntektArbeidYtelseAggregat> saksbehandletVersjon = iayGrunnlag.getSaksbehandletVersjon();
        if (saksbehandletVersjon.isPresent()) {
            return saksbehandletVersjon;
        }
        return iayGrunnlag.getRegisterVersjon();
    }

    private void verifiserUnikeDatoer(BekreftSvangerskapspengerDto dto) {
        List<SvpArbeidsforholdDto> bekreftedeArbeidsforhold = dto.getBekreftetSvpArbeidsforholdList();
        for (var arbeidsforhold : bekreftedeArbeidsforhold) {
            if (arbeidsforhold.getSkalBrukes()) {
                boolean harUliktAntallTilretteleggingDatoer = arbeidsforhold.getTilretteleggingDatoer().stream()
                    .map(SvpTilretteleggingDatoDto::getFom)
                    .collect(Collectors.toSet())
                    .size() != arbeidsforhold.getTilretteleggingDatoer().size();
                if (harUliktAntallTilretteleggingDatoer) {
                    throw SvangerskapsTjenesteFeil.FACTORY.kanIkkeHaLikeDatoerPåEttArbeidsforhold().toException();
                }
            }
        }
    }

    private boolean oppdaterTilrettelegging(BekreftSvangerskapspengerDto dto, Behandling behandling) {

        var svpGrunnlag = svangerskapspengerRepository.hentGrunnlag(behandling.getId()).orElseThrow(() -> {
            throw SvangerskapsTjenesteFeil.FACTORY.kanIkkeFinneSvangerskapspengerGrunnlagForBehandling(behandling.getId()).toException();
        });
        var oppdatertTilrettelegging = new ArrayList<SvpTilretteleggingEntitet>();
        var bekreftedeArbeidsforholdDtoer = dto.getBekreftetSvpArbeidsforholdList();
        var tilretteleggingerUfiltrert = new TilretteleggingFilter(svpGrunnlag).getAktuelleTilretteleggingerUfiltrert();

        boolean erEndret = false;
        for (SvpArbeidsforholdDto arbeidsforholdDto : bekreftedeArbeidsforholdDtoer) {
            if (mapTilretteleggingHvisEndret(arbeidsforholdDto, tilretteleggingerUfiltrert, oppdatertTilrettelegging)) {
                erEndret = true;
            }
        }

        if (oppdatertTilrettelegging.size() != tilretteleggingerUfiltrert.size()) {
            throw SvangerskapsTjenesteFeil.FACTORY.overstyrteArbeidsforholdStemmerIkkeOverensMedSøknadsgrunnlag(behandling.getId()).toException();
        }

        if (erEndret) {
            svangerskapspengerRepository.lagreOverstyrtGrunnlag(behandling, oppdatertTilrettelegging);
        }

        return erEndret;

    }

    private boolean mapTilretteleggingHvisEndret(SvpArbeidsforholdDto arbeidsforholdDto,
                                                 List<SvpTilretteleggingEntitet> aktuelleTilrettelegingerListe,
                                                 List<SvpTilretteleggingEntitet> oppdatertTilretteleggingListe) {

        var tilretteleggingId = arbeidsforholdDto.getTilretteleggingId();
        var eksisterendeTilretteleggingEntitet = aktuelleTilrettelegingerListe.stream()
            .filter(a -> a.getId().equals(tilretteleggingId))
            .findFirst()
            .orElseThrow(() -> {
                throw SvangerskapsTjenesteFeil.FACTORY.kanIkkFinneTilretteleggingForSvangerskapspenger(tilretteleggingId).toException();
            });

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
                throw SvangerskapsTjenesteFeil.FACTORY.manglerStillingsprosentForDelvisTilrettelegging().toException();
            }
            //Sjekk om overstyring av utbetalingsgrad er lovlig
            if (datoDto.getOverstyrtUtbetalingsgrad() != null && !sjekkOmOverstyringErLovlig()) {
                throw SvangerskapsTjenesteFeil.FACTORY.ingenTilgangTilOverstyringAvUtbetalingsgrad().toException();
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
        boolean erEndret = oppdaterVedEndretTilretteleggingFOM(eksisterendeTilretteleggingEntitet, overstyrtTilretteleggingEntitet);

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
        return innloggetBruker.getKanOverstyre();
    }


    private boolean oppdaterVedEndretTilretteleggingFOM(SvpTilretteleggingEntitet eksisterendeTilrettelegging,
                                                        SvpTilretteleggingEntitet overstyrtTilrettelegging) {

        List<TilretteleggingFOM> fjernet = eksisterendeTilrettelegging.getTilretteleggingFOMListe().stream()
            .filter(fom -> !overstyrtTilrettelegging.getTilretteleggingFOMListe().contains(fom))
            .collect(Collectors.toList());
        List<TilretteleggingFOM> lagtTil = overstyrtTilrettelegging.getTilretteleggingFOMListe().stream()
            .filter(fom -> !eksisterendeTilrettelegging.getTilretteleggingFOMListe().contains(fom))
            .collect(Collectors.toList());

        if (fjernet.isEmpty() && lagtTil.isEmpty()) {
            return false;
        }

        for (TilretteleggingFOM fomFjernet : fjernet) {
            historikkAdapter.tekstBuilder().medEndretFelt(finnHistorikkFeltType(fomFjernet.getType()), formaterForHistorikk(fomFjernet), "fjernet");
        }
        for (TilretteleggingFOM fomLagtTil : lagtTil) {
            historikkAdapter.tekstBuilder().medEndretFelt(finnHistorikkFeltType(fomLagtTil.getType()), null, formaterForHistorikk(fomLagtTil));
        }

        return true;

    }

    private String formaterForHistorikk(TilretteleggingFOM fom) {
        StringBuilder historikk = new StringBuilder().append(fom.getType().getNavn()).append(" fom: ").append(fom.getFomDato());
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
        Long behandlingId = behandling.getId();
        final FamilieHendelseGrunnlagEntitet grunnlag = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandlingId);

        boolean termindatoOppdatert = oppdaterTermindato(dto, behandling, grunnlag);
        boolean fødselsdatoOppdatert = oppdaterFødselsdato(dto, behandling, grunnlag);

        return termindatoOppdatert || fødselsdatoOppdatert;
    }

    private boolean oppdaterFødselsdato(BekreftSvangerskapspengerDto dto, Behandling behandling, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        Optional<LocalDate> orginalFødselsdato = getFødselsdato(familieHendelseGrunnlag);

        boolean erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.FODSELSDATO, orginalFødselsdato.orElse(null), dto.getFødselsdato());

        if (erEndret) {
            final FamilieHendelseBuilder oppdatertOverstyrtHendelse = familieHendelseRepository.opprettBuilderFor(behandling);
            oppdatertOverstyrtHendelse.tilbakestillBarn();
            if (dto.getFødselsdato() != null) {
                oppdatertOverstyrtHendelse.medFødselsDato(dto.getFødselsdato());
            }

            familieHendelseRepository.lagreOverstyrtHendelse(behandling, oppdatertOverstyrtHendelse);
        }
        return erEndret;
    }

    private Optional<LocalDate> getFødselsdato(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        return familieHendelseGrunnlag.getGjeldendeVersjon().getFødselsdato();
    }

    private boolean oppdaterTermindato(BekreftSvangerskapspengerDto dto, Behandling behandling, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {

        LocalDate orginalTermindato = getTermindato(familieHendelseGrunnlag, behandling);
        boolean erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.TERMINDATO, orginalTermindato, dto.getTermindato());

        if (erEndret) {
            final FamilieHendelseBuilder oppdatertOverstyrtHendelse = familieHendelseRepository.opprettBuilderFor(behandling);
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
            .orElseThrow(() -> SvangerskapsTjenesteFeil.FACTORY.kanIkkeFinneTerminbekreftelsePåSvangerskapspengerSøknad(behandling.getId()).toException());
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
