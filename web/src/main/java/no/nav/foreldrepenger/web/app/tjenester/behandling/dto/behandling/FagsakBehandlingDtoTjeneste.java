package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import static no.nav.foreldrepenger.web.app.rest.ResourceLinks.get;
import static no.nav.foreldrepenger.web.app.rest.ResourceLinks.post;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
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
import no.nav.foreldrepenger.domene.uttak.Uttak;
import no.nav.foreldrepenger.domene.uttak.UttakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.TotrinnTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktDtoMapper;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingOperasjonerDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.KlageRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.app.KontrollDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app.TotrinnskontrollAksjonspunkterTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.verge.VergeTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.brev.BrevRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.familiehendelse.FamiliehendelseRestTjeneste;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

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
    private DekningsgradTjeneste dekningsgradTjeneste;
    private UttakTjeneste uttakTjeneste;

    @Inject
    public FagsakBehandlingDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                       SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                       BehandlingDokumentRepository behandlingDokumentRepository,
                                       BrevmalTjeneste brevmalTjeneste,
                                       TotrinnTjeneste totrinnTjeneste,
                                       TotrinnskontrollAksjonspunkterTjeneste totrinnskontrollTjeneste,
                                       VergeTjeneste vergeTjeneste,
                                       KontrollDtoTjeneste kontrollDtoTjeneste,
                                       DekningsgradTjeneste dekningsgradTjeneste,
                                       UttakTjeneste uttakTjeneste) {

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
        this.dekningsgradTjeneste = dekningsgradTjeneste;
        this.uttakTjeneste = uttakTjeneste;
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
            var behandlingsresultat = getBehandlingsresultat(behandling.getId());
            var vedtaksdato = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId())
                .map(BehandlingVedtak::getVedtaksdato).orElse(null);
            return lagBehandlingDto(behandling, behandlingsresultat, erBehandlingMedGjeldendeVedtak, vedtaksdato);
        }).toList();
    }

    private FagsakBehandlingDto lagBehandlingDto(Behandling behandling,
                                           Behandlingsresultat behandlingsresultat,
                                           boolean erBehandlingMedGjeldendeVedtak, LocalDate vedtaksdato) {
        var dto = new FagsakBehandlingDto();
        var uuidDto = new UuidDto(behandling.getUuid());

        BehandlingDtoUtil.setStandardfelterMedGjeldendeVedtak(behandling, behandlingsresultat, dto, erBehandlingMedGjeldendeVedtak, vedtaksdato);
        dto.setSpråkkode(getSpråkkode(behandling));
        dto.setBehandlingsresultat(lagBehandlingsresultatDto(behandling, behandlingsresultat).orElse(null));

        // Felles for alle behandlingstyper
        dto.setBehandlingTillatteOperasjoner(lovligeOperasjoner(behandling));

        if (behandling.erYtelseBehandling()) {
            dto.leggTil(get(PersonRestTjeneste.PERSONOVERSIKT_PATH, "behandling-personoversikt", uuidDto));
            dto.leggTil(get(FamiliehendelseRestTjeneste.FAMILIEHENDELSE_V2_PATH, "familiehendelse-v2", uuidDto));
        }

        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            kontrollDtoTjeneste.lagKontrollresultatForBehandling(BehandlingReferanse.fra(behandling)).ifPresent(dto::setKontrollResultat);
            AksjonspunktDtoMapper.lagAksjonspunktDtoFor(behandling, behandlingsresultat, AksjonspunktDefinisjon.VURDER_FARESIGNALER).ifPresent(dto::setRisikoAksjonspunkt);
        }

        if (BehandlingType.REVURDERING.equals(behandling.getType())) {
            dto.setUgunstAksjonspunkt(behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST));
        }

        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            dto.leggTil(get(KlageRestTjeneste.KLAGE_V2_PATH, "klage-vurdering", uuidDto));
        }

        dto.setTotrinnskontrollReadonly(true);
        if (!BehandlingType.INNSYN.equals(behandling.getType())) {
            // Totrinnsbehandling
            if (BehandlingStatus.FATTER_VEDTAK.equals(behandling.getStatus())) {
                dto.setTotrinnskontrollÅrsaker(totrinnskontrollTjeneste.hentTotrinnsSkjermlenkeContext(behandling, behandlingsresultat));
                dto.setTotrinnskontrollReadonly(false);
                dto.leggTil(post(AksjonspunktRestTjeneste.AKSJONSPUNKT_BESLUTT_PATH, "bekreft-totrinnsaksjonspunkt", uuidDto));
            } else if (BehandlingStatus.UTREDES.equals(behandling.getStatus())) {
                dto.setTotrinnskontrollÅrsaker(totrinnskontrollTjeneste.hentTotrinnsvurderingSkjermlenkeContext(behandling, behandlingsresultat));
            }
        }

        // Brev
        dto.setBrevmaler(brevmalTjeneste.hentBrevmalerFor(behandling));
        dto.leggTil(post(BrevRestTjeneste.BREV_BESTILL_PATH, "brev-bestill"));
        dto.leggTil(post(BrevRestTjeneste.BREV_VIS_PATH, "brev-vis"));

        return dto;
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

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
    }

    private BehandlingOperasjonerDto lovligeOperasjoner(Behandling b) {
        if (b.erSaksbehandlingAvsluttet()) {
            return new BehandlingOperasjonerDto(b.getUuid()); // Skal ikke foreta menyvalg lenger
        }
        if (BehandlingStatus.FATTER_VEDTAK.equals(b.getStatus())) {
            var tilgokjenning = b.getAnsvarligSaksbehandler() != null && !b.getAnsvarligSaksbehandler().equalsIgnoreCase(
                KontekstHolder.getKontekst().getUid());
            return new BehandlingOperasjonerDto(b.getUuid(), tilgokjenning);
        }
        var kanÅpnesForEndring = b.erRevurdering() && !b.isBehandlingPåVent() &&
            SpesialBehandling.erIkkeSpesialBehandling(b) && !b.erKøet() &&
            !FagsakYtelseType.ENGANGSTØNAD.equals(b.getFagsakYtelseType());
        var totrinnRetur = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(b.getId()).stream()
            .anyMatch(tt -> !tt.isGodkjent());
        return new BehandlingOperasjonerDto(b.getUuid(),
            !b.erKøet(), // Bytte enhet
            SpesialBehandling.kanHenlegges(b), // Henlegges
            b.isBehandlingPåVent() && !b.erKøet(), // Gjenopptas
            kanÅpnesForEndring, // Åpnes for endring
            !b.isBehandlingPåVent(), // Settes på vent
            !b.isBehandlingPåVent(), // Sende melding
            !b.isBehandlingPåVent() && totrinnRetur, // Fra beslutter
            false, // Til godkjenning
            vergeTjeneste.utledBehandlingOperasjon(b.getId()));
    }

}
