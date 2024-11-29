package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import static no.nav.foreldrepenger.web.app.rest.ResourceLinks.get;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.uttak.Uttak;
import no.nav.foreldrepenger.domene.uttak.UttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.UtregnetStønadskontoTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidsforhold.InntektArbeidYtelseRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.BeregningsresultatRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn.InnsynRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.KlageRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.SøknadRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.UttakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling.YtelsefordelingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.brev.BrevRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.dokument.DokumentRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.familiehendelse.FamiliehendelseRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.formidling.FormidlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.formidling.arbeidsforholdInntektsmelding.ArbeidsforholdInntektsmeldingFormidlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag.BeregningsgrunnlagFormidlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.formidling.tilkjentytelse.TilkjentYtelseFormidlingRestTjeneste;

/**
 * Bygger en BehandlingBrevDto som skal brukes til å populere brev for behandlinger behandlet i fpsak.
 */

@ApplicationScoped
public class BehandlingFormidlingDtoTjeneste {

    private BeregningTjeneste beregningTjeneste;
    private UttakTjeneste uttakTjeneste;
    private UtregnetStønadskontoTjeneste utregnetStønadskontoTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private SøknadRepository søknadRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;
    private MedlemTjeneste medlemTjeneste;

    @Inject
    public BehandlingFormidlingDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                           BeregningTjeneste beregningTjeneste,
                                           SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                           BehandlingDokumentRepository behandlingDokumentRepository,
                                           RelatertBehandlingTjeneste relatertBehandlingTjeneste,
                                           UttakTjeneste uttakTjeneste,
                                           DekningsgradTjeneste dekningsgradTjeneste,
                                           UtregnetStønadskontoTjeneste utregnetStønadskontoTjeneste,
                                           MedlemTjeneste medlemTjeneste) {
        this.beregningTjeneste = beregningTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.utregnetStønadskontoTjeneste = utregnetStønadskontoTjeneste;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
        this.medlemTjeneste = medlemTjeneste;
    }

    BehandlingFormidlingDtoTjeneste() {
        // for CDI proxy
    }

    public BehandlingFormidlingDto lagDtoForFormidling(Behandling behandling) {
        return lagDto(behandling);
    }

    private Språkkode getSpråkkode(Behandling behandling) {
        if (!behandling.erYtelseBehandling()) {
            return behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(behandling.getFagsakId())
                .flatMap(s -> søknadRepository.hentSøknadHvisEksisterer(s.getId()))
                .map(SøknadEntitet::getSpråkkode)
                .orElseGet(()-> behandling.getFagsak().getNavBruker().getSpråkkode());
        }
        return søknadRepository.hentSøknadHvisEksisterer(behandling.getId()).map(SøknadEntitet::getSpråkkode).orElseGet(() -> behandling.getFagsak().getNavBruker().getSpråkkode());
    }

    private void settStandardfelterForBrev(Behandling behandling, BehandlingFormidlingDto dto) {
        var vedtaksDato = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId())
            .map(BehandlingVedtak::getVedtaksdato)
            .orElse(null);
        BehandlingDtoUtil.setStandardfelter(behandling, getBehandlingsresultat(behandling.getId()), dto, vedtaksDato);
        dto.setSpråkkode(getSpråkkode(behandling));
        var behandlingsresultatDto = lagBehandlingsresultatDto(behandling);
        dto.setBehandlingsresultat(behandlingsresultatDto.orElse(null));
    }

    private BehandlingFormidlingDto lagDto(Behandling behandling) {
        var dto = new BehandlingFormidlingDto();
        settStandardfelterForBrev(behandling, dto);

        var saksnummerDto = new SaksnummerDto(behandling.getSaksnummer());
        dto.leggTil(get(FagsakRestTjeneste.FAGSAK_PATH, "fagsak", saksnummerDto));

        var uuidDto = new UuidDto(behandling.getUuid());
        dto.leggTil(get(AksjonspunktRestTjeneste.AKSJONSPUNKT_V2_PATH, "aksjonspunkter", uuidDto));

        if (behandlingHarVergeAksjonspunkt(behandling)) {
            dto.leggTil(get(PersonRestTjeneste.VERGE_BACKEND_PATH, "verge-backend", uuidDto));
        }

        if (BehandlingType.INNSYN.equals(behandling.getType())) {
            return utvideBehandlingDtoForInnsyn(behandling, dto);
        }
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            return utvideBehandlingDtoKlage(behandling, dto);
        }
        return utvideBehandlingDto(behandling, dto);
    }

    private BehandlingFormidlingDto utvideBehandlingDtoKlage(Behandling behandling, BehandlingFormidlingDto dto) {
        var uuidDto = new UuidDto(behandling.getUuid());
        dto.leggTil(get(KlageRestTjeneste.KLAGE_V2_PATH, "klage-vurdering", uuidDto));
        dto.leggTil(get(KlageRestTjeneste.MOTTATT_KLAGEDOKUMENT_V2_PATH, "mottatt-klagedokument", uuidDto));
        return dto;
    }

    private BehandlingFormidlingDto utvideBehandlingDtoForInnsyn(Behandling behandling, BehandlingFormidlingDto dto) {
        var uuidDto = new UuidDto(behandling.getUuid());
        dto.leggTil(get(InnsynRestTjeneste.INNSYN_PATH, "innsyn", uuidDto));
        return dto;
    }

    private BehandlingFormidlingDto utvideBehandlingDto(Behandling behandling, BehandlingFormidlingDto dto) {
        var uuidDto = new UuidDto(behandling.getUuid());
        // mapping ved hjelp av tjenester
        dto.leggTil(get(SøknadRestTjeneste.SOKNAD_BACKEND_PATH, "soknad-backend", uuidDto));
        dto.leggTil(get(DokumentRestTjeneste.MOTTATT_DOKUMENTER_PATH, "mottattdokument", uuidDto));

        if (dto.isErAktivPapirsoknad()) {
            return dto;
        }

        dto.leggTil(get(FamiliehendelseRestTjeneste.FAMILIEHENDELSE_V2_PATH, "familiehendelse-v2", uuidDto));

        if (BehandlingType.REVURDERING.equals(behandling.getType()) && BehandlingStatus.UTREDES.equals(behandling.getStatus())) {
            dto.leggTil(get(BrevRestTjeneste.VARSEL_REVURDERING_PATH, "sendt-varsel-om-revurdering", uuidDto));
        }

        dto.leggTil(get(InntektArbeidYtelseRestTjeneste.ALLE_INNTEKTSMELDINGER_PATH, "inntektsmeldinger", uuidDto));

        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            dto.leggTilFormidlingRessurs(get(TilkjentYtelseFormidlingRestTjeneste.TILKJENT_YTELSE_ENGAGSSTØNAD_PATH, "tilkjentytelse-engangsstonad", uuidDto));
            dto.leggTil(get(BeregningsresultatRestTjeneste.ENGANGSTONAD_PATH, "beregningsresultat-engangsstonad", uuidDto));
            dto.setMedlemskapFom(medlemTjeneste.hentMedlemFomDato(behandling.getId()).orElse(null));
        } else {
            var beregningsgrunnlag = beregningTjeneste.hent(BehandlingReferanse.fra(behandling))
                    .flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);
            if (beregningsgrunnlag.isPresent()) {
                dto.leggTilFormidlingRessurs(get(BeregningsgrunnlagFormidlingRestTjeneste.BEREGNINGSGRUNNLAG_PATH, "beregningsgrunnlag-formidling", uuidDto));
            }
            dto.leggTilFormidlingRessurs(get(ArbeidsforholdInntektsmeldingFormidlingRestTjeneste.INNTEKTSMELDING_STATUS_PATH, "inntektsmelding-status", uuidDto));
            if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType())) {
                var uttak = uttakTjeneste.hentHvisEksisterer(behandling.getId());

                if (uttak.isPresent()) {
                    dto.leggTil(get(UttakRestTjeneste.RESULTAT_SVANGERSKAPSPENGER_PATH, "uttaksresultat-svangerskapspenger", uuidDto));
                    dto.leggTilFormidlingRessurs(get(TilkjentYtelseFormidlingRestTjeneste.TILKJENT_YTELSE_DAGYTELSE_PATH, "tilkjentytelse-dagytelse", uuidDto));
                    dto.leggTil(get(FormidlingRestTjeneste.MOTTATT_DATO_SØKNADSFRIST_PATH, "motattdato-søknad", uuidDto));
                }
            } else {
                var harAvklartAnnenForelderRett = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT)
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
                    dto.leggTilFormidlingRessurs(get(TilkjentYtelseFormidlingRestTjeneste.TILKJENT_YTELSE_DAGYTELSE_PATH, "tilkjentytelse-dagytelse", uuidDto));
                    uttak.filter(Uttak::harAvslagPgaMedlemskap).ifPresent(u -> {
                        var avslagsårsak = medlemTjeneste.hentAvslagsårsak(behandling.getId());
                        dto.setMedlemskapOpphørsårsak(avslagsårsak.orElse(null));
                    });
                }

                dto.leggTil(get(FormidlingRestTjeneste.UTSATT_START_PATH, "utsatt-oppstart", uuidDto));
                dto.leggTil(get(YtelsefordelingRestTjeneste.YTELSESFORDELING_PATH, "ytelsefordeling", uuidDto));
            }
        }

        behandling.getOriginalBehandlingId().map(behandlingRepository::hentBehandling).map(Behandling::getUuid)
            .ifPresent(dto::setOriginalBehandlingUuid);

        return dto;
    }

    private Optional<Uttak> hentUttakAnnenpartForeldrepengerHvisEksisterer(Behandling søkersBehandling) {
        var annenpartBehandling = relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattBehandling(søkersBehandling.getSaksnummer());
        return annenpartBehandling.flatMap(ab -> uttakTjeneste.hentHvisEksisterer(ab.getId()));
    }

    private Optional<BehandlingsresultatDto> lagBehandlingsresultatDto(Behandling behandling) {
        var behandlingsresultat = getBehandlingsresultat(behandling.getId());
        if (behandlingsresultat == null) {
            return Optional.empty();
        }
        var dto = new BehandlingsresultatDto();
        dto.setId(behandlingsresultat.getId());
        dto.setType(behandlingsresultat.getBehandlingResultatType());
        dto.setAvslagsarsak(behandlingsresultat.getAvslagsårsak());
        dto.setKonsekvenserForYtelsen(behandlingsresultat.getKonsekvenserForYtelsen());
        dto.setRettenTil(behandlingsresultat.getRettenTil());
        dto.setSkjæringstidspunkt(finnSkjæringstidspunktForBehandling(behandling, behandlingsresultat).orElse(null));
        dto.setErRevurderingMedUendretUtfall(erRevurderingMedUendretUtfall(behandling));
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

        dto.setVedtaksbrev(behandlingsresultat.getVedtaksbrev());
        return Optional.of(dto);
    }

    private boolean erRevurderingMedUendretUtfall(Behandling behandling) {
        return FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, behandling.getFagsakYtelseType()).orElseThrow().erRevurderingMedUendretUtfall(behandling);
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

    private boolean behandlingHarVergeAksjonspunkt(Behandling behandling) {
        return behandling.getAksjonspunkter().stream()
            .map(Aksjonspunkt::getAksjonspunktDefinisjon)
            .anyMatch(AksjonspunktDefinisjon.AVKLAR_VERGE::equals);
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
    }
}
