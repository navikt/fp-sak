package no.nav.foreldrepenger.web.app.tjenester.formidling;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.web.app.rest.ResourceLinks.get;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.Uttak;
import no.nav.foreldrepenger.domene.uttak.UttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.UtregnetStønadskontoTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidsforhold.InntektArbeidYtelseRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.BeregningsresultatRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.SkjæringstidspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn.InnsynRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.KlageRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.SøknadRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.UttakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.verge.VergeTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling.YtelsefordelingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.brev.BrevRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.dokument.DokumentRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.app.FagsakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.familiehendelse.FamiliehendelseRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.formidling.arbeidsforholdInntektsmelding.ArbeidsforholdInntektsmeldingFormidlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag.BeregningsgrunnlagFormidlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.formidling.rest.dto.BehandlingsresultatDto;
import no.nav.foreldrepenger.web.app.tjenester.formidling.rest.dto.BrevGrunnlagResponseDto;
import no.nav.foreldrepenger.web.app.tjenester.formidling.rest.dto.FagsakDto;
import no.nav.foreldrepenger.web.app.tjenester.formidling.rest.dto.VergeDto;
import no.nav.foreldrepenger.web.app.tjenester.formidling.rest.FormidlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.formidling.rest.kodeverk.KonsekvensForYtelsen;
import no.nav.foreldrepenger.web.app.tjenester.formidling.tilkjentytelse.TilkjentYtelseFormidlingRestTjeneste;

@ApplicationScoped
public class BrevGrunnlagTjeneste {

    private BeregningTjeneste beregningTjeneste;
    private UttakTjeneste uttakTjeneste;
    private UtregnetStønadskontoTjeneste utregnetStønadskontoTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private SøknadRepository søknadRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;
    private MedlemTjeneste medlemTjeneste;
    private VergeTjeneste vergeTjeneste;
    private FagsakTjeneste fagsakTjeneste;

    BrevGrunnlagTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BrevGrunnlagTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                BeregningTjeneste beregningTjeneste,
                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                BehandlingDokumentRepository behandlingDokumentRepository,
                                RelatertBehandlingTjeneste relatertBehandlingTjeneste,
                                UttakTjeneste uttakTjeneste,
                                DekningsgradTjeneste dekningsgradTjeneste,
                                UtregnetStønadskontoTjeneste utregnetStønadskontoTjeneste,
                                MedlemTjeneste medlemTjeneste,
                                VergeTjeneste vergeTjeneste,
                                FagsakTjeneste fagsakTjeneste) {
        this.beregningTjeneste = beregningTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.utregnetStønadskontoTjeneste = utregnetStønadskontoTjeneste;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
        this.medlemTjeneste = medlemTjeneste;
        this.vergeTjeneste = vergeTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
    }

    public BrevGrunnlagResponseDto toDto(Behandling behandling) {
        var dto = new BrevGrunnlagResponseDto();
        settBehandlingInfoForBrev(behandling, dto);
        settFagsakInfoForBrev(behandling.getSaksnummer(), dto);
        settVergeInfoForBrev(behandling, dto);

        if (BehandlingType.INNSYN.equals(behandling.getType())) {
            return utvideBehandlingDtoForInnsyn(behandling, dto);
        }
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            return utvideBehandlingDtoKlage(behandling, dto);
        }
        return utvideBehandlingDto(behandling, dto);
    }

    private void settBehandlingInfoForBrev(Behandling behandling, BrevGrunnlagResponseDto dto) {
        setStandardfelter(behandling, dto);
        dto.setSpråkkode(getSpråkkode(behandling));
        var behandlingsresultat = getBehandlingsresultat(behandling.getId());
        dto.setVilkår(!erAktivPapirsøknad(behandling) ? hentVilkårTyper(behandlingsresultat) : List.of());
        dto.setBehandlingsresultat(lagBehandlingsresultatDto(behandling, behandlingsresultat).orElse(null));
    }

    public static List<VilkårType> hentVilkårTyper(Behandlingsresultat behandlingsresultat) {
        return Optional.ofNullable(behandlingsresultat)
            .map(Behandlingsresultat::getVilkårResultat)
            .map(VilkårResultat::getVilkårene)
            .orElse(List.of())
            .stream()
            .map(Vilkår::getVilkårType)
            .toList();
    }

    static void setStandardfelter(Behandling behandling, BrevGrunnlagResponseDto dto) {
        dto.setUuid(behandling.getUuid());
        dto.setType(behandling.getType());
        dto.setOpprettet(behandling.getOpprettetDato());
        dto.setAvsluttet(behandling.getAvsluttetDato());
        dto.setStatus(behandling.getStatus());
        dto.setBehandlendeEnhetId(behandling.getBehandlendeOrganisasjonsEnhet().enhetId());
        dto.setToTrinnsBehandling(behandling.isToTrinnsBehandling());
        dto.setBehandlingÅrsaker(lagBehandlingÅrsakDto(behandling));
    }

    private static List<BehandlingÅrsakType> lagBehandlingÅrsakDto(Behandling behandling) {
        if (!behandling.getBehandlingÅrsaker().isEmpty()) {
            return behandling.getBehandlingÅrsaker().stream().map(BehandlingÅrsak::getBehandlingÅrsakType).toList();
        }
        return emptyList();
    }

    private static boolean erAktivPapirsøknad(Behandling behandling) {
        var kriterier = Arrays.asList(AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD,
            AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_FORELDREPENGER, AksjonspunktDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER,
            AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER);
        return !behandling.getÅpneAksjonspunkter(kriterier).isEmpty();
    }

    private void settFagsakInfoForBrev(Saksnummer saksnummer, BrevGrunnlagResponseDto dto) {
        var fagsak = fagsakTjeneste.hentFagsakDtoForSaksnummer(saksnummer).orElseThrow();
        var fagsakDto = new FagsakDto(fagsak.saksnummer(), fagsak.fagsakYtelseType(), fagsak.relasjonsRolleType(), fagsak.aktørId(),
            fagsak.dekningsgrad());
        dto.setFagsak(fagsakDto);
    }

    private Språkkode getSpråkkode(Behandling behandling) {
        if (!behandling.erYtelseBehandling()) {
            return hentSpråkkodeFraSøknadEllerFagsak(behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(behandling.getFagsakId()));
        }
        return hentSpråkkodeFraSøknadEllerFagsak(Optional.of(behandling));
    }

    private Språkkode hentSpråkkodeFraSøknadEllerFagsak(Optional<Behandling> behandlingOpt) {
        return behandlingOpt.flatMap(behandling -> søknadRepository.hentSøknadHvisEksisterer(behandling.getId()))
            .map(SøknadEntitet::getSpråkkode)
            .orElseGet(() -> behandlingOpt.map(Behandling::getFagsak).map(Fagsak::getNavBruker).map(NavBruker::getSpråkkode).orElse(null));
    }

    private void settVergeInfoForBrev(Behandling behandling, BrevGrunnlagResponseDto dto) {
        var verge = vergeTjeneste.hentVergeForBackend(behandling);
        if (verge != null) {
            var vergeV2Dto = new VergeDto(verge.getAktoerId(), verge.getNavn(), verge.getOrganisasjonsnummer(), verge.getGyldigFom(),
                verge.getGyldigTom());
            dto.setVerge(vergeV2Dto);
        }
    }

    private BrevGrunnlagResponseDto utvideBehandlingDtoKlage(Behandling behandling, BrevGrunnlagResponseDto dto) {
        var uuidDto = new UuidDto(behandling.getUuid());
        dto.leggTil(get(KlageRestTjeneste.KLAGE_V2_PATH, "klage-vurdering", uuidDto));
        dto.leggTil(get(KlageRestTjeneste.MOTTATT_KLAGEDOKUMENT_V2_PATH, "mottatt-klagedokument", uuidDto));
        return dto;
    }

    private BrevGrunnlagResponseDto utvideBehandlingDtoForInnsyn(Behandling behandling, BrevGrunnlagResponseDto dto) {
        var uuidDto = new UuidDto(behandling.getUuid());
        dto.leggTil(get(InnsynRestTjeneste.INNSYN_PATH, "innsyn", uuidDto));
        return dto;
    }

    private BrevGrunnlagResponseDto utvideBehandlingDto(Behandling behandling, BrevGrunnlagResponseDto dto) {
        var uuidDto = new UuidDto(behandling.getUuid());
        // mapping ved hjelp av tjenester
        dto.leggTil(get(SøknadRestTjeneste.SOKNAD_BACKEND_PATH, "soknad-backend", uuidDto));
        dto.leggTil(get(DokumentRestTjeneste.MOTTATT_DOKUMENTER_PATH, "mottattdokument", uuidDto));

        if (erAktivPapirsøknad(behandling)) {
            return dto;
        }

        dto.leggTil(get(FamiliehendelseRestTjeneste.FAMILIEHENDELSE_V2_PATH, "familiehendelse-v2", uuidDto));

        if (BehandlingType.REVURDERING.equals(behandling.getType()) && BehandlingStatus.UTREDES.equals(behandling.getStatus())) {
            dto.leggTil(get(BrevRestTjeneste.VARSEL_REVURDERING_PATH, "sendt-varsel-om-revurdering", uuidDto));
        }

        dto.leggTil(get(InntektArbeidYtelseRestTjeneste.ALLE_INNTEKTSMELDINGER_PATH, "inntektsmeldinger", uuidDto));

        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            dto.leggTil(get(TilkjentYtelseFormidlingRestTjeneste.TILKJENT_YTELSE_ENGAGSSTØNAD_PATH, "tilkjentytelse-engangsstonad", uuidDto));
            dto.leggTil(get(BeregningsresultatRestTjeneste.ENGANGSTONAD_PATH, "beregningsresultat-engangsstonad", uuidDto));
            dto.setMedlemskapFom(medlemTjeneste.hentMedlemFomDato(behandling.getId()).orElse(null));
        } else {
            var beregningsgrunnlag = beregningTjeneste.hent(BehandlingReferanse.fra(behandling))
                .flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);
            if (beregningsgrunnlag.isPresent()) {
                dto.leggTil(get(BeregningsgrunnlagFormidlingRestTjeneste.BEREGNINGSGRUNNLAG_PATH, "beregningsgrunnlag-formidling", uuidDto));
            }
            dto.leggTil(get(ArbeidsforholdInntektsmeldingFormidlingRestTjeneste.INNTEKTSMELDING_STATUS_PATH, "inntektsmelding-status", uuidDto));
            if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType())) {
                var uttak = uttakTjeneste.hentHvisEksisterer(behandling.getId());

                if (uttak.isPresent()) {
                    dto.leggTil(get(UttakRestTjeneste.RESULTAT_SVANGERSKAPSPENGER_PATH, "uttaksresultat-svangerskapspenger", uuidDto));
                    dto.leggTil(get(TilkjentYtelseFormidlingRestTjeneste.TILKJENT_YTELSE_DAGYTELSE_PATH, "tilkjentytelse-dagytelse", uuidDto));
                    dto.leggTil(get(FormidlingRestTjeneste.MOTTATT_DATO_SØKNADSFRIST_PATH, "motattdato-søknad", uuidDto));
                }
            } else {
                var harAvklartAnnenForelderRett = behandling.getAksjonspunktMedDefinisjonOptional(
                        AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT)
                    .filter(ap -> AksjonspunktStatus.UTFØRT.equals(ap.getStatus()))
                    .isPresent();
                dto.setHarAvklartAnnenForelderRett(harAvklartAnnenForelderRett);

                var uttak = uttakTjeneste.hentHvisEksisterer(behandling.getId());

                var stønadskontoberegning = utregnetStønadskontoTjeneste.gjeldendeKontoutregning(BehandlingReferanse.fra(behandling));
                if (!stønadskontoberegning.isEmpty() && uttak.isPresent()) {
                    dto.leggTil(get(UttakRestTjeneste.STONADSKONTOER_PATH, "uttak-stonadskontoer", uuidDto));
                }

                var uttakResultatAnnenPart = hentUttakAnnenpartForeldrepengerHvisEksisterer(behandling);
                if (uttak.isPresent() || uttakResultatAnnenPart.isPresent()) {
                    // Fpformidling trenger også å få fatt på uttaksresultatet når bare annen part har det
                    dto.leggTil(get(UttakRestTjeneste.RESULTAT_PERIODER_PATH, "uttaksresultat-perioder-formidling", uuidDto));
                }
                if (uttak.isPresent()) {
                    dto.leggTil(get(TilkjentYtelseFormidlingRestTjeneste.TILKJENT_YTELSE_DAGYTELSE_PATH, "tilkjentytelse-dagytelse", uuidDto));
                    uttak.filter(Uttak::harAvslagPgaMedlemskap).ifPresent(u -> {
                        var avslagsårsak = medlemTjeneste.hentAvslagsårsak(behandling.getId());
                        dto.setMedlemskapOpphørsårsak(avslagsårsak.orElse(null));
                    });
                }

                dto.leggTil(get(FormidlingRestTjeneste.UTSATT_START_PATH, "utsatt-oppstart", uuidDto));
                dto.leggTil(get(YtelsefordelingRestTjeneste.YTELSESFORDELING_PATH, "ytelsefordeling", uuidDto));
            }
        }

        behandling.getOriginalBehandlingId()
            .map(behandlingRepository::hentBehandling)
            .map(Behandling::getUuid)
            .ifPresent(dto::setOriginalBehandlingUuid);

        return dto;
    }

    private Optional<Uttak> hentUttakAnnenpartForeldrepengerHvisEksisterer(Behandling søkersBehandling) {
        var annenpartBehandling = relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattBehandling(søkersBehandling.getSaksnummer());
        return annenpartBehandling.flatMap(ab -> uttakTjeneste.hentHvisEksisterer(ab.getId()));
    }

    private Optional<BehandlingsresultatDto> lagBehandlingsresultatDto(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        if (behandlingsresultat == null) {
            return Optional.empty();
        }
        var dto = new BehandlingsresultatDto();
        dto.setType(behandlingsresultat.getBehandlingResultatType());
        dto.setAvslagsarsak(behandlingsresultat.getAvslagsårsak());
        dto.setKonsekvenserForYtelsen(mapKonsekvensForYtelsen(behandlingsresultat.getKonsekvenserForYtelsen()));
        dto.setSkjæringstidspunkt(finnSkjæringstidspunktForBehandling(behandling, behandlingsresultat).orElse(null));
        dto.setEndretDekningsgrad(dekningsgradTjeneste.behandlingHarEndretDekningsgrad(BehandlingReferanse.fra(behandling)));
        if (!FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            var opphørsdato = uttakTjeneste.hentHvisEksisterer(behandling.getId()).flatMap(Uttak::opphørsdato).orElse(null);
            dto.setOpphørsdato(opphørsdato);
        }

        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        if (behandlingDokument.isPresent()) {
            dto.setAvslagsarsakFritekst(behandlingDokument.get().getVedtakFritekst());
            dto.setOverskrift(behandlingDokument.get().getOverstyrtBrevOverskrift());
            dto.setFritekstbrev(behandlingDokument.get().getOverstyrtBrevFritekst());
        }
        return Optional.of(dto);
    }

    private List<KonsekvensForYtelsen> mapKonsekvensForYtelsen(List<no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen> konsekvenserForYtelsen) {
        return konsekvenserForYtelsen.stream().map(k -> KonsekvensForYtelsen.valueOf(k.name())).toList();
    }

    private Optional<SkjæringstidspunktDto> finnSkjæringstidspunktForBehandling(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        if (!behandling.erYtelseBehandling() || behandlingsresultat.isBehandlingHenlagt()) {
            return Optional.empty();
        }
        try {
            return SkjæringstidspunktDto.fraSkjæringstidspunkt(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
    }
}
