package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import static no.nav.foreldrepenger.web.app.rest.ResourceLinks.get;
import static no.nav.foreldrepenger.web.app.rest.ResourceLinks.post;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dokumentbestiller.brevmal.BrevmalTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktDtoMapper;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.BeregningsresultatRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingOperasjonerDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.KlageRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.KontrollRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.app.KontrollDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.TotrinnskontrollRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app.TotrinnskontrollAksjonspunkterTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.verge.VergeTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.brev.BrevRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.familiehendelse.FamiliehendelseRestTjeneste;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

/**
 * Bygger et sammensatt resultat av FagsakBehandlingDto ved å samle data fra ulike tjenester, for å kunne levere dette ut på en REST tjeneste.
 */
@ApplicationScoped
public class FagsakBehandlingDtoTjeneste {

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private SøknadRepository søknadRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private BrevmalTjeneste brevmalTjeneste;
    private TotrinnTjeneste totrinnTjeneste;
    private VergeTjeneste vergeTjeneste;
    private TotrinnskontrollAksjonspunkterTjeneste totrinnskontrollTjeneste;
    private KontrollDtoTjeneste kontrollDtoTjeneste;

    @Inject
    public FagsakBehandlingDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                       SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                       BehandlingDokumentRepository behandlingDokumentRepository,
                                       BrevmalTjeneste brevmalTjeneste,
                                       TotrinnTjeneste totrinnTjeneste,
                                       TotrinnskontrollAksjonspunkterTjeneste totrinnskontrollTjeneste,
                                       VergeTjeneste vergeTjeneste,
                                       KontrollDtoTjeneste kontrollDtoTjeneste) {

        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.brevmalTjeneste = brevmalTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
        this.totrinnskontrollTjeneste = totrinnskontrollTjeneste;
        this.vergeTjeneste = vergeTjeneste;
        this.kontrollDtoTjeneste = kontrollDtoTjeneste;
    }

    FagsakBehandlingDtoTjeneste() {
        // for CDI proxy
    }

    public List<FagsakBehandlingDto> lagBehandlingDtoer(List<Behandling> behandlinger) {
        if (behandlinger.isEmpty()) {
            return Collections.emptyList();
        }
        var gjeldendeVedtak = behandlingVedtakRepository.hentGjeldendeVedtak(behandlinger.get(0).getFagsak());
        var behandlingMedGjeldendeVedtak = gjeldendeVedtak.map(BehandlingVedtak::getBehandlingsresultat).map(Behandlingsresultat::getBehandlingId).map(behandlingRepository::hentBehandling);
        return behandlinger.stream().map(behandling -> {
            var erBehandlingMedGjeldendeVedtak = erBehandlingMedGjeldendeVedtak(behandling, behandlingMedGjeldendeVedtak);
            var behandlingsresultatDto = lagBehandlingsresultatDto(behandling);
            var vedtaksdato = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId())
                .map(BehandlingVedtak::getVedtaksdato).orElse(null);
            return lagBehandlingDto(behandling, behandlingsresultatDto, erBehandlingMedGjeldendeVedtak,
                søknadRepository, vedtaksdato, behandlingRepository);
        }).collect(Collectors.toList());
    }



    private FagsakBehandlingDto lagBehandlingDto(Behandling behandling,
                                           Optional<BehandlingsresultatDto> behandlingsresultatDto,
                                           boolean erBehandlingMedGjeldendeVedtak,
                                           SøknadRepository søknadRepository,
                                           LocalDate vedtaksdato,
                                           BehandlingRepository behandlingRepository) {
        var dto = new FagsakBehandlingDto();
        var uuidDto = new UuidDto(behandling.getUuid());
        var behandlingsresultat = Optional.ofNullable(getBehandlingsresultat(behandling.getId()))
            .map(Behandlingsresultat::getBehandlingResultatType).orElse(BehandlingResultatType.IKKE_FASTSATT);
        BehandlingDtoUtil.setStandardfelterMedGjeldendeVedtak(behandling, behandlingsresultat, dto, erBehandlingMedGjeldendeVedtak, vedtaksdato);
        dto.setSpråkkode(getSpråkkode(behandling, søknadRepository, behandlingRepository));
        dto.setBehandlingsresultat(behandlingsresultatDto.orElse(null));

        // Felles for alle behandlingstyper
        dto.leggTil(get(BehandlingRestTjeneste.RETTIGHETER_PATH, "behandling-rettigheter", uuidDto));
        dto.setBehandlingTillatteOperasjoner(lovligeOperasjoner(behandling));

        if (behandling.erYtelseBehandling()) {
            dto.leggTil(get(PersonRestTjeneste.PERSONOVERSIKT_PATH, "behandling-personoversikt", uuidDto));
            dto.leggTil(get(FamiliehendelseRestTjeneste.FAMILIEHENDELSE_V2_PATH, "familiehendelse-v2", uuidDto));
        }

        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            dto.leggTil(get(KontrollRestTjeneste.KONTROLLRESULTAT_V2_PATH, "kontrollresultat", uuidDto));
            dto.leggTil(get(AksjonspunktRestTjeneste.AKSJONSPUNKT_RISIKO_PATH, "risikoklassifisering-aksjonspunkt", uuidDto));
            AksjonspunktDtoMapper.lagAksjonspunktDtoFor(behandling, AksjonspunktDefinisjon.VURDER_FARESIGNALER).ifPresent(dto::setRisikoAksjonspunkt);
            kontrollDtoTjeneste.lagKontrollresultatForBehandling(BehandlingReferanse.fra(behandling)).ifPresent(dto::setKontrollResultat);
        }

        if (BehandlingType.REVURDERING.equals(behandling.getType())) {
            dto.leggTil(get(AksjonspunktRestTjeneste.AKSJONSPUNKT_KONTROLLER_REVURDERING_PATH, "har-apent-kontroller-revurdering-aksjonspunkt", uuidDto));
            dto.leggTil(get(BeregningsresultatRestTjeneste.HAR_SAMME_RESULTAT_PATH, "har-samme-resultat", uuidDto));
            dto.setUgunstAksjonspunkt(behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST));
        }

        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            dto.leggTil(get(KlageRestTjeneste.KLAGE_V2_PATH, "klage-vurdering", uuidDto));
        }

        dto.setTotrinnskontrollReadonly(true);
        if (!BehandlingType.INNSYN.equals(behandling.getType())) {
            // Totrinnsbehandling
            if (BehandlingStatus.FATTER_VEDTAK.equals(behandling.getStatus())) {
                dto.leggTil(get(TotrinnskontrollRestTjeneste.ARSAKER_PATH, "totrinnskontroll-arsaker", uuidDto));
                dto.setTotrinnskontrollÅrsaker(totrinnskontrollTjeneste.hentTotrinnsSkjermlenkeContext(behandling));
                dto.setTotrinnskontrollReadonly(false);
                dto.leggTil(post(AksjonspunktRestTjeneste.AKSJONSPUNKT_PATH, "bekreft-totrinnsaksjonspunkt", uuidDto));
            } else if (BehandlingStatus.UTREDES.equals(behandling.getStatus())) {
                dto.leggTil(get(TotrinnskontrollRestTjeneste.ARSAKER_READ_ONLY_PATH, "totrinnskontroll-arsaker-readOnly", uuidDto));
                dto.setTotrinnskontrollÅrsaker(totrinnskontrollTjeneste.hentTotrinnsvurderingSkjermlenkeContext(behandling));
            }
        }

        // Brev
        dto.leggTil(get(BrevRestTjeneste.BREV_MALER_PATH, "fpsak-brev-maler", uuidDto));
        dto.setBrevmaler(brevmalTjeneste.hentBrevmalerFor(behandling));
        dto.leggTil(post(BrevRestTjeneste.BREV_BESTILL_PATH, "brev-bestill", new BestillBrevDto()));
        dto.leggTil(ResourceLink.get("/fpformidling/api/brev/maler", "brev-maler", uuidDto));

        return dto;
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

    private boolean erBehandlingMedGjeldendeVedtak(Behandling behandling, Optional<Behandling> behandlingMedGjeldendeVedtak) {
        return behandlingMedGjeldendeVedtak.filter(b -> b.getId().equals(behandling.getId())).isPresent();
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
        var skjæringstidspunktHvisUtledet = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getSkjæringstidspunktHvisUtledet();
        if (skjæringstidspunktHvisUtledet.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SkjæringstidspunktDto(skjæringstidspunktHvisUtledet.get()));
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
    }

    private BehandlingOperasjonerDto lovligeOperasjoner(Behandling b) {
        if (b.erSaksbehandlingAvsluttet()) {
            return BehandlingOperasjonerDto.builder(b.getUuid()).build(); // Skal ikke foreta menyvalg lenger
        }
        if (BehandlingStatus.FATTER_VEDTAK.equals(b.getStatus())) {
            var tilgokjenning = b.getAnsvarligSaksbehandler() != null && !b.getAnsvarligSaksbehandler().equalsIgnoreCase(
                SubjectHandler.getSubjectHandler().getUid());
            return BehandlingOperasjonerDto.builder(b.getUuid()).medTilGodkjenning(tilgokjenning).build();
        }
        var kanÅpnesForEndring = b.erRevurdering() && !b.isBehandlingPåVent() &&
            SpesialBehandling.erIkkeSpesialBehandling(b) && !b.erKøet() &&
            !FagsakYtelseType.ENGANGSTØNAD.equals(b.getFagsakYtelseType());
        var totrinnRetur = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(b).stream()
            .anyMatch(tt -> !tt.isGodkjent());
        return BehandlingOperasjonerDto.builder(b.getUuid())
            .medTilGodkjenning(false)
            .medFraBeslutter(!b.isBehandlingPåVent() && totrinnRetur)
            .medKanBytteEnhet(!b.erKøet())
            .medKanHenlegges(SpesialBehandling.kanHenlegges(b))
            .medKanSettesPaVent(!b.isBehandlingPåVent())
            .medKanGjenopptas(b.isBehandlingPåVent() && !b.erKøet())
            .medKanOpnesForEndringer(kanÅpnesForEndring)
            .medKanSendeMelding(!b.isBehandlingPåVent())
            .medVergemeny(vergeTjeneste.utledBehandlingsmeny(b.getId()).getVergeBehandlingsmeny())
            .build();
    }

}
