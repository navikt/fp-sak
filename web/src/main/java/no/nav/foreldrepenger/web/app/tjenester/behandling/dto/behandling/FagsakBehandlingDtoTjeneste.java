package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import static no.nav.foreldrepenger.web.app.rest.ResourceLinks.get;
import static no.nav.foreldrepenger.web.app.rest.ResourceLinks.post;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoUtil.erAktivPapirsøknad;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoUtil.getBehandlingsResultatType;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoUtil.getFristDatoBehandlingPåVent;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoUtil.lagBehandlingÅrsakDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
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
import no.nav.foreldrepenger.domene.uttak.Uttak;
import no.nav.foreldrepenger.domene.uttak.UttakTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktDtoMapper;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.KlageRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.app.KontrollDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app.TotrinnskontrollAksjonspunkterTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.TotrinnskontrollSkjermlenkeContextDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.VilkårDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.VilkårDtoMapper;
import no.nav.foreldrepenger.web.app.tjenester.brev.BrevRestTjeneste;

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
    private TotrinnskontrollAksjonspunkterTjeneste totrinnskontrollTjeneste;
    private KontrollDtoTjeneste kontrollDtoTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;
    private UttakTjeneste uttakTjeneste;
    private FagsakBehandlingOperasjonerDtoTjeneste behandlingOperasjonerDtoTjeneste;

    @Inject
    public FagsakBehandlingDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                       SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                       BehandlingDokumentRepository behandlingDokumentRepository,
                                       BrevmalTjeneste brevmalTjeneste,
                                       TotrinnskontrollAksjonspunkterTjeneste totrinnskontrollTjeneste,
                                       KontrollDtoTjeneste kontrollDtoTjeneste,
                                       DekningsgradTjeneste dekningsgradTjeneste,
                                       UttakTjeneste uttakTjeneste,
                                       FagsakBehandlingOperasjonerDtoTjeneste behandlingOperasjonerDtoTjeneste) {

        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.brevmalTjeneste = brevmalTjeneste;
        this.totrinnskontrollTjeneste = totrinnskontrollTjeneste;
        this.kontrollDtoTjeneste = kontrollDtoTjeneste;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.behandlingOperasjonerDtoTjeneste = behandlingOperasjonerDtoTjeneste;
    }

    FagsakBehandlingDtoTjeneste() {
        // for CDI proxy
    }

    public List<FagsakBehandlingDto> lagBehandlingDtoer(List<Behandling> behandlinger) {
        if (behandlinger.isEmpty()) {
            return Collections.emptyList();
        }
        var gjeldendeVedtak = behandlingVedtakRepository.hentGjeldendeVedtak(behandlinger.getFirst().getFagsak());
        var behandlingMedGjeldendeVedtak = gjeldendeVedtak.map(BehandlingVedtak::getBehandlingsresultat).map(Behandlingsresultat::getBehandlingId).map(behandlingRepository::hentBehandling);
        return behandlinger.stream().map(behandling -> {
            var erBehandlingMedGjeldendeVedtak = erBehandlingMedGjeldendeVedtak(behandling, behandlingMedGjeldendeVedtak);
            var behandlingsresultat = getBehandlingsresultat(behandling.getId());
            return lagBehandlingDto(behandling, behandlingsresultat, erBehandlingMedGjeldendeVedtak);
        }).toList();
    }

    private FagsakBehandlingDto lagBehandlingDto(Behandling behandling,
                                                 Behandlingsresultat behandlingsresultat,
                                                 boolean erBehandlingMedGjeldendeVedtak) {
        var uuid = behandling.getUuid();
        var uuidDto = new UuidDto(uuid);
        var versjon = behandling.getVersjon();
        var type = behandling.getType();
        var status = behandling.getStatus();
        var behandlendeEnhetId = behandling.getBehandlendeOrganisasjonsEnhet().enhetId();
        var behandlendeEnhetNavn = behandling.getBehandlendeOrganisasjonsEnhet().enhetNavn();
        var erAktivPapirsøknad = erAktivPapirsøknad(behandling);
        var behandlingPåVent = behandling.isBehandlingPåVent();
        var behandlingHenlagt = getBehandlingsResultatType(behandlingsresultat).erHenlagt();
        var fristDatoBehandlingPåVent = getFristDatoBehandlingPåVent(behandling).orElse(null);
        var behandlingÅrsaker = lagBehandlingÅrsakDto(behandling);
        List<VilkårDto> vilkår = !erAktivPapirsøknad ? VilkårDtoMapper.lagVilkarDto(behandling, behandlingsresultat) : List.of();
        var ansvarligSaksbehandler = behandling.getAnsvarligSaksbehandler();
        var brDto = lagBehandlingsresultatDto(behandling, behandlingsresultat).orElse(null);
        var førsteÅrsak = førsteÅrsak(behandling).orElse(null);
        var toTrinnsBehandling = behandling.isToTrinnsBehandling();
        var behandlingTillatteOperasjoner = behandlingOperasjonerDtoTjeneste.lovligeOperasjoner(behandling);
        var kontrollresultatDto = kontrollDtoTjeneste.lagKontrollresultatForBehandling(BehandlingReferanse.fra(behandling)).orElse(null);
        var ugunstAksjonspunkt = BehandlingType.REVURDERING.equals(type) && behandling.harÅpentAksjonspunktMedType(
            AksjonspunktDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST);
        var aksjonspunktDto = BehandlingType.FØRSTEGANGSSØKNAD.equals(type) ? AksjonspunktDtoMapper.lagAksjonspunktDtoFor(behandling, behandlingsresultat,
            AksjonspunktDefinisjon.VURDER_FARESIGNALER).orElse(null) : null;
        var brevmaler = brevmalTjeneste.hentBrevmalerFor(behandling);
        var totrinnskontrollÅrsaker = finnTotrinnDto(behandling, behandlingsresultat, type, status);
        var links = lagLinks(behandling, uuidDto);
        var opprettet = behandling.getOpprettetTidspunkt();
        var språkkode = getSpråkkode(behandling);

        return new FagsakBehandlingDto(uuid, behandlingTillatteOperasjoner, brevmaler, totrinnskontrollÅrsaker, aksjonspunktDto, kontrollresultatDto,
            ugunstAksjonspunkt, ansvarligSaksbehandler, behandling.getAvsluttetDato(), førsteÅrsak, erBehandlingMedGjeldendeVedtak, opprettet,
            toTrinnsBehandling, versjon, type, status, behandlendeEnhetId, erAktivPapirsøknad, behandlendeEnhetNavn,
            behandlingHenlagt, språkkode, behandlingPåVent, brDto, behandlingÅrsaker, vilkår, fristDatoBehandlingPåVent, links);
    }

    private static List<ResourceLink> lagLinks(Behandling behandling, UuidDto uuidDto) {
        var links = new ArrayList<ResourceLink>();
        if (behandling.erYtelseBehandling()) {
            links.add(get(PersonRestTjeneste.PERSONOVERSIKT_PATH, "behandling-personoversikt", uuidDto));
        }
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            links.add(get(KlageRestTjeneste.KLAGE_V2_PATH, "klage-vurdering", uuidDto));
        }
        if (!BehandlingType.INNSYN.equals(behandling.getType()) && BehandlingStatus.FATTER_VEDTAK.equals(behandling.getStatus())) {
            links.add(post(AksjonspunktRestTjeneste.AKSJONSPUNKT_BESLUTT_PATH, "bekreft-totrinnsaksjonspunkt", uuidDto));
        }
        links.add(post(BrevRestTjeneste.BREV_BESTILL_PATH, "brev-bestill"));
        links.add(post(BrevRestTjeneste.BREV_VIS_PATH, "brev-vis"));
        return links;
    }

    private List<TotrinnskontrollSkjermlenkeContextDto> finnTotrinnDto(Behandling behandling,
                                                                       Behandlingsresultat behandlingsresultat,
                                                                       BehandlingType type,
                                                                       BehandlingStatus status) {
        if (!BehandlingType.INNSYN.equals(type)) {
            // Totrinnsbehandling
            if (BehandlingStatus.FATTER_VEDTAK.equals(status)) {
                return totrinnskontrollTjeneste.hentTotrinnsSkjermlenkeContext(behandling, behandlingsresultat);
            } else if (BehandlingStatus.UTREDES.equals(status)) {
                return totrinnskontrollTjeneste.hentTotrinnsvurderingSkjermlenkeContext(behandling, behandlingsresultat);
            }
        }
        return List.of();
    }

    private static Optional<BehandlingÅrsakDto> førsteÅrsak(Behandling behandling) {
        return behandling.getBehandlingÅrsaker().stream()
            .sorted(Comparator.comparing(BaseEntitet::getOpprettetTidspunkt))
            .map(BehandlingDtoUtil::map)
            .findFirst();
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

    private boolean erBehandlingMedGjeldendeVedtak(Behandling behandling, Optional<Behandling> behandlingMedGjeldendeVedtak) {
        return behandlingMedGjeldendeVedtak.filter(b -> b.getId().equals(behandling.getId())).isPresent();
    }

    private Optional<BehandlingsresultatDto> lagBehandlingsresultatDto(Behandling behandling, Behandlingsresultat behandlingsresultat) {
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
        dto.setEndretDekningsgrad(dekningsgradTjeneste.behandlingHarEndretDekningsgrad(BehandlingReferanse.fra(behandling)));
        if (!FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            var opphørsdato = uttakTjeneste.hentHvisEksisterer(behandling.getId()).flatMap(Uttak::opphørsdato).orElse(null);
            dto.setOpphørsdato(opphørsdato);
        }
        dto.setErRevurderingMedUendretUtfall(erRevurderingMedUendretUtfall(behandling));

        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        if (behandlingDokument.isPresent()) {
            var behandlingDokumentEntitet = behandlingDokument.get();
            dto.setAvslagsarsakFritekst(behandlingDokumentEntitet.getVedtakFritekst());
            dto.setOverskrift(behandlingDokumentEntitet.getOverstyrtBrevOverskrift());
            dto.setFritekstbrev(behandlingDokumentEntitet.getOverstyrtBrevFritekst());
            dto.setHarRedigertVedtaksbrev(behandlingDokumentEntitet.getOverstyrtBrevFritekstHtml() != null);
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

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
    }

}
