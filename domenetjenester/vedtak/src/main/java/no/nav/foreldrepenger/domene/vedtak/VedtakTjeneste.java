package no.nav.foreldrepenger.domene.vedtak;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.es.UtledVedtakResultatTypeES;
import no.nav.foreldrepenger.behandling.fp.UtledVedtakResultatType;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTotrinnsvurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtak;
import no.nav.foreldrepenger.domene.vedtak.repo.LagretVedtakRepository;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;

@ApplicationScoped
public class VedtakTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private HistorikkRepository historikkRepository;
    private LagretVedtakRepository lagretVedtakRepository;
    private TotrinnTjeneste totrinnTjeneste;

    VedtakTjeneste() {
        // CDI
    }

    @Inject
    public VedtakTjeneste(LagretVedtakRepository lagretVedtakRepository,
                          BehandlingRepositoryProvider repositoryProvider, TotrinnTjeneste totrinnTjeneste) {
        this(repositoryProvider.getBehandlingRepository(), repositoryProvider.getBehandlingsresultatRepository(),
            repositoryProvider.getHistorikkRepository(), lagretVedtakRepository, totrinnTjeneste);

    }

    public VedtakTjeneste(BehandlingRepository behandlingRepository,
                          BehandlingsresultatRepository behandlingsresultatRepository,
                          HistorikkRepository historikkRepository,
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
        var historikkinnslagType = erUendretUtfall ? HistorikkinnslagType.UENDRET_UTFALL : HistorikkinnslagType.VEDTAK_FATTET;
        var tekstBuilder = new HistorikkInnslagTekstBuilder().medHendelse(historikkinnslagType).medSkjermlenke(SkjermlenkeType.VEDTAK);
        if (!erUendretUtfall) {
            tekstBuilder.medResultat(utledVedtakResultatType(behandling));
        }
        var innslag = new Historikkinnslag();
        innslag.setAktør(behandling.isToTrinnsBehandling() ? HistorikkAktør.BESLUTTER : HistorikkAktør.VEDTAKSLØSNINGEN);
        innslag.setType(historikkinnslagType);
        innslag.setBehandling(behandling);
        tekstBuilder.build(innslag);

        historikkRepository.lagre(innslag);
    }

    private RevurderingTjeneste getRevurderingTjeneste(Behandling behandling) {
        return FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, behandling.getFagsakYtelseType()).orElseThrow();
    }

    private void lagHistorikkInnslagVurderPåNytt(Behandling behandling, Collection<Totrinnsvurdering> medTotrinnskontroll) {
        lagHistorikkInnslagVedtakReturEllerNK(HistorikkinnslagType.SAK_RETUR, behandling, medTotrinnskontroll);
    }

    private void lagHistorikkInnslagVedtakReturEllerNK(HistorikkinnslagType hendelse,
                                                       Behandling behandling,
                                                       Collection<Totrinnsvurdering> medTotrinnskontroll) {
        Map<SkjermlenkeType, List<HistorikkinnslagTotrinnsvurdering>> vurdering = new HashMap<>();
        List<HistorikkinnslagTotrinnsvurdering> vurderingUtenLenke = new ArrayList<>();

        var delBuilder = new HistorikkInnslagTekstBuilder().medHendelse(hendelse);

        for (var ttv : medTotrinnskontroll) {
            var totrinnsVurdering = lagHistorikkinnslagTotrinnsvurdering(ttv);
            var sistEndret = ttv.getEndretTidspunkt() != null ? ttv.getEndretTidspunkt() : ttv.getOpprettetTidspunkt();
            totrinnsVurdering.setAksjonspunktSistEndret(sistEndret);
            var skjermlenkeType = SkjermlenkeType.finnSkjermlenkeType(ttv.getAksjonspunktDefinisjon(), behandling,
                behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null));
            if (SkjermlenkeType.totrinnsSkjermlenke(skjermlenkeType)) {
                vurdering.computeIfAbsent(skjermlenkeType, k -> new ArrayList<>()).add(totrinnsVurdering);
            } else {
                vurderingUtenLenke.add(totrinnsVurdering);
            }
        }
        delBuilder.medTotrinnsvurdering(vurdering, vurderingUtenLenke);

        historikkRepository.lagre(lagHistorikkinnslag(behandling, hendelse, delBuilder));

    }

    private Historikkinnslag lagHistorikkinnslag(Behandling behandling,
                                                 HistorikkinnslagType historikkinnslagType,
                                                 HistorikkInnslagTekstBuilder builder) {
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setBehandling(behandling);
        historikkinnslag.setAktør(HistorikkAktør.BESLUTTER);
        historikkinnslag.setType(historikkinnslagType);
        builder.build(historikkinnslag);

        return historikkinnslag;
    }

    private HistorikkinnslagTotrinnsvurdering lagHistorikkinnslagTotrinnsvurdering(Totrinnsvurdering ttv) {
        var totrinnsVurdering = new HistorikkinnslagTotrinnsvurdering();
        totrinnsVurdering.setAksjonspunktDefinisjon(ttv.getAksjonspunktDefinisjon());
        totrinnsVurdering.setBegrunnelse(ttv.getBegrunnelse());
        totrinnsVurdering.setGodkjent(Boolean.TRUE.equals(ttv.isGodkjent()));
        return totrinnsVurdering;
    }

    public VedtakResultatType utledVedtakResultatType(Behandling behandling) {
        var behandlingResultatType = getBehandlingsresultat(behandling.getId()).getBehandlingResultatType();
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            return UtledVedtakResultatTypeES.utled(behandling.getType(), behandlingResultatType);
        }
        if (BehandlingResultatType.INGEN_ENDRING.equals(behandlingResultatType)) {
            var original = behandling.getOriginalBehandlingId()
                .map(behandlingRepository::hentBehandling)
                .orElseThrow(() -> new IllegalStateException("INGEN ENDRING uten original behandling"));
            return utledVedtakResultatType(original);
        }
        return UtledVedtakResultatType.utled(behandling.getType(), behandlingResultatType);
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
    }

}
