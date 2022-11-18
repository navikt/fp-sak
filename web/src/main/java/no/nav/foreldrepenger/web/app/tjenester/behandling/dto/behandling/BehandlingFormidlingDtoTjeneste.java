package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import static no.nav.foreldrepenger.web.app.rest.ResourceLinks.get;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidsforhold.InntektArbeidYtelseRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.BeregningsresultatRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn.InnsynRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.KlageRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.SøknadRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.UttakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.VilkårRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling.YtelsefordelingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.brev.BrevRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.dokument.DokumentRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.familiehendelse.FamiliehendelseRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.formidling.FormidlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag.BeregningsgrunnlagFormidlingRestTjeneste;

/**
 * Bygger en BehandlingBrevDto som skal brukes til å populere brev for behandlinger behandlet i fpsak.
 */

@ApplicationScoped
public class BehandlingFormidlingDtoTjeneste {

    private BeregningTjeneste beregningTjeneste;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private SøknadRepository søknadRepository;
    private SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;

    @Inject
    public BehandlingFormidlingDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                           BeregningTjeneste beregningTjeneste,
                                           SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                           BehandlingDokumentRepository behandlingDokumentRepository,
                                           RelatertBehandlingTjeneste relatertBehandlingTjeneste,
                                           ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste) {
        this.beregningTjeneste = beregningTjeneste;
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.svangerskapspengerUttakResultatRepository = repositoryProvider.getSvangerskapspengerUttakResultatRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
    }

    BehandlingFormidlingDtoTjeneste() {
        // for CDI proxy
    }

    public BehandlingFormidlingDto lagDtoForFormidling(Behandling behandling) {
        return lagDto(behandling);
    }

    private static Språkkode getSpråkkode(Behandling behandling, SøknadRepository søknadRepository, BehandlingRepository behandlingRepository) {
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
        dto.setSpråkkode(getSpråkkode(behandling, søknadRepository, behandlingRepository));
        var behandlingsresultatDto = lagBehandlingsresultatDto(behandling);
        dto.setBehandlingsresultat(behandlingsresultatDto.orElse(null));
    }

    private BehandlingFormidlingDto lagDto(Behandling behandling) {
        var dto = new BehandlingFormidlingDto();
        settStandardfelterForBrev(behandling, dto);

        var saksnummerDto = new SaksnummerDto(behandling.getFagsak().getSaksnummer());
        dto.leggTil(get(FagsakRestTjeneste.FAGSAK_PATH, "fagsak", saksnummerDto));

        var uuidDto = new UuidDto(behandling.getUuid());
        dto.leggTil(get(AksjonspunktRestTjeneste.AKSJONSPUNKT_V2_PATH, "aksjonspunkter", uuidDto));

        if (!dto.isErAktivPapirsoknad()) {
            dto.leggTil(get(VilkårRestTjeneste.VILKÅR_V2_PATH, "vilkar", uuidDto));
        }

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
            dto.leggTil(get(BeregningsresultatRestTjeneste.ENGANGSTONAD_PATH, "beregningsresultat-engangsstonad", uuidDto));
        } else {
            var beregningsgrunnlag = beregningTjeneste.hent(behandling.getId())
                    .flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);
            if (beregningsgrunnlag.isPresent()) {
                dto.leggTilFormidlingRessurs(get(BeregningsgrunnlagFormidlingRestTjeneste.BEREGNINGSGRUNNLAG_PATH, "beregningsgrunnlag-formidling", uuidDto));
            }

            if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType())) {
                var svangerskapspengerUttakResultatEntitet = svangerskapspengerUttakResultatRepository.hentHvisEksisterer(behandling.getId());
                var stønadskontoberegning = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak())
                    .flatMap(FagsakRelasjon::getGjeldendeStønadskontoberegning);
                if (stønadskontoberegning.isPresent() && svangerskapspengerUttakResultatEntitet.isPresent()) {
                    dto.leggTil(get(UttakRestTjeneste.STONADSKONTOER_PATH, "uttak-stonadskontoer", uuidDto));
                }

                if (svangerskapspengerUttakResultatEntitet.isPresent()) {
                    dto.leggTil(get(UttakRestTjeneste.RESULTAT_SVANGERSKAPSPENGER_PATH, "uttaksresultat-svangerskapspenger", uuidDto));
                    dto.leggTil(get(BeregningsresultatRestTjeneste.FORELDREPENGER_PATH, "beregningsresultat-foreldrepenger", uuidDto));
                }
            } else {
                var harAvklartAnnenForelderRett = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT)
                    .filter(ap -> AksjonspunktStatus.UTFØRT.equals(ap.getStatus()))
                    .isPresent();
                dto.setHarAvklartAnnenForelderRett(harAvklartAnnenForelderRett);

                var uttakResultat = foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(behandling.getId());
                var stønadskontoberegning = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak())
                    .flatMap(FagsakRelasjon::getGjeldendeStønadskontoberegning);
                if (stønadskontoberegning.isPresent() && uttakResultat.isPresent()) {
                    dto.leggTil(get(UttakRestTjeneste.STONADSKONTOER_PATH, "uttak-stonadskontoer", uuidDto));
                }

                var uttakResultatAnnenPart = hentUttakAnnenpartHvisEksisterer(behandling);
                if (uttakResultat.isPresent() || uttakResultatAnnenPart.isPresent()) {
                    // Fpformidling trenger også å få fatt på uttaksresultatet når bare annen part har det
                    dto.leggTil(get(UttakRestTjeneste.RESULTAT_PERIODER_PATH, "uttaksresultat-perioder-formidling", uuidDto));
                }
                if (uttakResultat.isPresent()) {
                    // FIXME: Bør ikke ha ytelsesspesifikk url her. Bør kun være beregningsresultat
                    dto.leggTil(get(BeregningsresultatRestTjeneste.FORELDREPENGER_PATH, "beregningsresultat-foreldrepenger", uuidDto));
                }

                dto.leggTil(get(FormidlingRestTjeneste.UTSATT_START_PATH, "utsatt-oppstart", uuidDto));
                dto.leggTil(get(UttakRestTjeneste.SAMMENHENGENDE_UTTAK_PATH, "krever-sammenhengende-uttak", uuidDto));
                dto.leggTil(get(UttakRestTjeneste.UTEN_MINSTERETT_PATH, "uten-minsterett", uuidDto));
                dto.leggTil(get(YtelsefordelingRestTjeneste.YTELSESFORDELING_PATH, "ytelsefordeling", uuidDto));
            }
        }

        behandling.getOriginalBehandlingId().ifPresent(originalBehandlingId -> {
            var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId);
            var originalUuidDto = new BehandlingIdDto(originalBehandling.getUuid());

            // Denne brukes kun av FPFORMIDLING
            dto.leggTil(get(FormidlingRestTjeneste.RESSURSER_PATH, "original-behandling", originalUuidDto));
            dto.setOriginalBehandlingUuid(originalBehandling.getUuid());
        });

        return dto;
    }

    private Optional<ForeldrepengerUttak> hentUttakAnnenpartHvisEksisterer(Behandling søkersBehandling) {
        var annenpartBehandling = relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattBehandling(søkersBehandling.getFagsak().getSaksnummer());
        return annenpartBehandling.flatMap(ab -> foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(ab.getId()));
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
        return SkjæringstidspunktDto.fraSkjæringstidspunkt(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
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
