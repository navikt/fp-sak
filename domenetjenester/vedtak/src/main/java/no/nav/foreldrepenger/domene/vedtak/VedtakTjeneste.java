package no.nav.foreldrepenger.domene.vedtak;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.UtledVedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtak;
import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtakRepository;

@ApplicationScoped
public class VedtakTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private Historikkinnslag2Repository historikkRepository;
    private LagretVedtakRepository lagretVedtakRepository;
    private TotrinnTjeneste totrinnTjeneste;

    VedtakTjeneste() {
        // CDI
    }

    @Inject
    public VedtakTjeneste(LagretVedtakRepository lagretVedtakRepository,
                          BehandlingRepositoryProvider repositoryProvider, TotrinnTjeneste totrinnTjeneste) {
        this(repositoryProvider.getBehandlingRepository(), repositoryProvider.getBehandlingsresultatRepository(),
            repositoryProvider.getHistorikkinnslag2Repository(), lagretVedtakRepository, totrinnTjeneste);

    }

    public VedtakTjeneste(BehandlingRepository behandlingRepository,
                          BehandlingsresultatRepository behandlingsresultatRepository,
                          Historikkinnslag2Repository historikkRepository,
                          LagretVedtakRepository lagretVedtakRepository,
                          TotrinnTjeneste totrinnTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.historikkRepository = historikkRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.lagretVedtakRepository = lagretVedtakRepository;
        this.totrinnTjeneste = totrinnTjeneste;
    }

    public List<LagretVedtak> hentLagreteVedtakPåFagsak(Long fagsakId) {
        return lagretVedtakRepository.hentLagreteVedtakPåFagsak(fagsakId);
    }

    public LagretVedtak hentLagreteVedtak(Long behandlingId) {
        return lagretVedtakRepository.hentLagretVedtakForBehandlingForOppdatering(behandlingId);
    }

    public List<Long> hentLagreteVedtakBehandlingId(LocalDateTime fom, LocalDateTime tom) {
        return lagretVedtakRepository.hentLagreteVedtakBehandlingId(fom, tom);
    }

    public void lagHistorikkinnslagFattVedtak(Behandling behandling) {
        if (behandling.isToTrinnsBehandling()) {
            var totrinnsvurderings = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling.getId());
            if (sendesTilbakeTilSaksbehandler(totrinnsvurderings)) {
                lagHistorikkInnslagVurderPåNytt(behandling, totrinnsvurderings);
                return;
            }
        }
        lagHistorikkInnslagVedtakFattet(behandling);
    }

    private boolean sendesTilbakeTilSaksbehandler(Collection<Totrinnsvurdering> medTotrinnskontroll) {
        return medTotrinnskontroll.stream().anyMatch(a -> !Boolean.TRUE.equals(a.isGodkjent()));
    }

    private void lagHistorikkInnslagVedtakFattet(Behandling behandling) {
        var erUendretUtfall = getRevurderingTjeneste(behandling).erRevurderingMedUendretUtfall(behandling);
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medAktør(behandling.isToTrinnsBehandling() ? HistorikkAktør.BESLUTTER : HistorikkAktør.VEDTAKSLØSNINGEN)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel(SkjermlenkeType.VEDTAK)
            .addLinje(erUendretUtfall ? "Uendret utfall" : String.format("Vedtak fattet: %s", utledVedtakResultatType(behandling).getNavn()))
            .build();
        historikkRepository.lagre(historikkinnslag);
    }

    private static RevurderingTjeneste getRevurderingTjeneste(Behandling behandling) {
        return FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, behandling.getFagsakYtelseType()).orElseThrow();
    }

    private void lagHistorikkInnslagVurderPåNytt(Behandling behandling, Collection<Totrinnsvurdering> medTotrinnskontroll) {
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.BESLUTTER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel("Sak retur")
            .medTekstlinjer(lagTekstForHverTotrinnkontroll(medTotrinnskontroll))
            .build();
        historikkRepository.lagre(historikkinnslag);
    }

    private static List<String> lagTekstForHverTotrinnkontroll(Collection<Totrinnsvurdering> medTotrinnskontroll) {
        return medTotrinnskontroll.stream()
            .sorted(Comparator.comparing(ttv -> ttv.getEndretTidspunkt() != null ? ttv.getEndretTidspunkt() : ttv.getOpprettetTidspunkt()))
            .map(VedtakTjeneste::tilHistorikkinnslagTekst)
            .map(VedtakTjeneste::leggTilLinjeskift) // TODO TFP-5554: Blir trailing linjeskift. Håndtere her eller i frontend?
            .flatMap(Collection::stream)
            .toList();
    }

    private static List<String> tilHistorikkinnslagTekst(Totrinnsvurdering ttv) {
        var aksjonspunktNavn = ttv.getAksjonspunktDefinisjon().getNavn();
        return Boolean.TRUE.equals(ttv.isGodkjent())
            ? List.of(String.format("__%s er godkjent__", aksjonspunktNavn))
            : List.of(String.format("__%s må vurderes på nytt__", aksjonspunktNavn), String.format("Kommentar: %s", ttv.getBegrunnelse()));
    }

    private static List<String> leggTilLinjeskift(List<String> eksistrendeLinjer) {
        var linjer = new ArrayList<>(eksistrendeLinjer);
        linjer.add(HistorikkinnslagLinjeBuilder.LINJESKIFT.build());
        return linjer;
    }

    public VedtakResultatType utledVedtakResultatType(Behandling behandling) {
        Long behandlingId = behandling.getId();
        var behandlingResultatType = behandlingsresultatRepository.hent(behandlingId).getBehandlingResultatType();
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            return UtledVedtakResultatType.utled(behandling.getType(), behandlingResultatType);
        }
        if (BehandlingResultatType.INGEN_ENDRING.equals(behandlingResultatType)) {
            var original = behandling.getOriginalBehandlingId()
                .map(behandlingRepository::hentBehandling)
                .orElseThrow(() -> new IllegalStateException("INGEN ENDRING uten original behandling"));
            return utledVedtakResultatType(original);
        }
        return UtledVedtakResultatType.utled(behandling.getType(), behandlingResultatType);
    }

}
