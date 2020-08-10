package no.nav.foreldrepenger.domene.vedtak;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.es.UtledVedtakResultatTypeES;
import no.nav.foreldrepenger.behandling.fp.UtledVedtakResultatType;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTotrinnsvurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtak;
import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtakMedBehandlingType;
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
    private KlageRepository klageRepository;
    private AnkeRepository ankeRepository;

    VedtakTjeneste() {
        // CDI
    }

    @Inject
    public VedtakTjeneste(LagretVedtakRepository lagretVedtakRepository,
                          BehandlingRepositoryProvider repositoryProvider,
                          KlageRepository klageRepository,
                          TotrinnTjeneste totrinnTjeneste,
                          InnsynRepository innsynRepository,
                          AnkeRepository ankeRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.historikkRepository = repositoryProvider.getHistorikkRepository();
        this.lagretVedtakRepository = lagretVedtakRepository;
        this.totrinnTjeneste = totrinnTjeneste;
        this.klageRepository = klageRepository;
        this.ankeRepository = ankeRepository;
    }

    public List<LagretVedtakMedBehandlingType> hentLagreteVedtakPåFagsak(Long fagsakId) {
        return lagretVedtakRepository.hentLagreteVedtakPåFagsak(fagsakId);
    }

    public LagretVedtak hentLagreteVedtak(Long behandlingId) {
        return lagretVedtakRepository.hentLagretVedtakForBehandlingForOppdatering(behandlingId);
    }

    public List<Long> hentLagreteVedtakBehandlingId(LocalDateTime fom, LocalDateTime tom){
        return lagretVedtakRepository.hentLagreteVedtakBehandlingId(fom,tom);
    }

    public void lagHistorikkinnslagFattVedtak(Behandling behandling) {
        if (behandling.isToTrinnsBehandling()) {
            Collection<Totrinnsvurdering> totrinnsvurderings = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling);
            if (sendesTilbakeTilSaksbehandler(totrinnsvurderings)) {
                lagHistorikkInnslagVurderPåNytt(behandling, totrinnsvurderings);
                return;
            }
            if (behandlingErKlageEllerAnke(behandling) && erGodkjentHosMedunderskriver(behandling)) {
                lagHistorikkInnslagGodkjentAvMedunderskriver(behandling, totrinnsvurderings);
                return;
            }
        }
        lagHistorikkInnslagVedtakFattet(behandling);
    }

    private boolean behandlingErKlageEllerAnke(Behandling behandling) {
        return BehandlingType.ANKE.equals(behandling.getType()) || BehandlingType.KLAGE.equals(behandling.getType());
    }

    private boolean erGodkjentHosMedunderskriver(Behandling behandling) {
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            Optional<KlageVurderingResultat> klageVurderingResultat = klageRepository.hentGjeldendeKlageVurderingResultat(behandling);
            return klageVurderingResultat.isPresent() && klageVurderingResultat.get().getKlageVurdertAv().equals(KlageVurdertAv.NK)
                && klageVurderingResultat.get().isGodkjentAvMedunderskriver();
        } else if (BehandlingType.ANKE.equals(behandling.getType())) {
            Optional<AnkeVurderingResultatEntitet> ankeVurderingResultat = ankeRepository.hentAnkeVurderingResultat(behandling.getId());
            return ankeVurderingResultat.isPresent() && ankeVurderingResultat.get().godkjentAvMedunderskriver();
        }
        return false;
    }

    private boolean sendesTilbakeTilSaksbehandler(Collection<Totrinnsvurdering> medTotrinnskontroll) {
        return medTotrinnskontroll.stream()
            .anyMatch(a -> !Boolean.TRUE.equals(a.isGodkjent()));
    }

    private void lagHistorikkInnslagVedtakFattet(Behandling behandling) {
        boolean erUendretUtfall = getRevurderingTjeneste(behandling).erRevurderingMedUendretUtfall(behandling);
        HistorikkinnslagType historikkinnslagType = erUendretUtfall ? HistorikkinnslagType.UENDRET_UTFALL : HistorikkinnslagType.VEDTAK_FATTET;
        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(historikkinnslagType)
            .medSkjermlenke(SkjermlenkeType.VEDTAK);
        if (!erUendretUtfall) {
            tekstBuilder.medResultat(utledVedtakResultatType(behandling));
        }
        Historikkinnslag innslag = new Historikkinnslag();
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
        Map<SkjermlenkeType, List<HistorikkinnslagTotrinnsvurdering>> vurdering = new HashMap<>();
        List<HistorikkinnslagTotrinnsvurdering> vurderingUtenLenke = new ArrayList<>();

        HistorikkInnslagTekstBuilder delBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.SAK_RETUR);

        for (Totrinnsvurdering ttv : medTotrinnskontroll) {
            HistorikkinnslagTotrinnsvurdering totrinnsVurdering = lagHistorikkinnslagTotrinnsvurdering(ttv);
            LocalDateTime sistEndret = ttv.getEndretTidspunkt() != null ? ttv.getEndretTidspunkt() : ttv.getOpprettetTidspunkt();
            totrinnsVurdering.setAksjonspunktSistEndret(sistEndret);
            SkjermlenkeType skjermlenkeType = SkjermlenkeType.finnSkjermlenkeType(ttv.getAksjonspunktDefinisjon(), behandling);
            if (skjermlenkeType != null && !SkjermlenkeType.UDEFINERT.equals(skjermlenkeType)) {
                vurdering.computeIfAbsent(skjermlenkeType, k -> new ArrayList<>()).add(totrinnsVurdering);
            } else {
                vurderingUtenLenke.add(totrinnsVurdering);
            }
        }
        delBuilder.medTotrinnsvurdering(vurdering, vurderingUtenLenke);

        historikkRepository.lagre(lagHistorikkinnslag(behandling, HistorikkinnslagType.SAK_RETUR, delBuilder));
    }

    private void lagHistorikkInnslagGodkjentAvMedunderskriver(Behandling behandling, Collection<Totrinnsvurdering> medTotrinnskontroll) {
        HistorikkInnslagTekstBuilder delBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.SAK_GODKJENT);

        for (Totrinnsvurdering ttv : medTotrinnskontroll) {
            SkjermlenkeType skjermlenkeType = SkjermlenkeType.finnSkjermlenkeType(ttv.getAksjonspunktDefinisjon(), behandling);
            if (skjermlenkeType != null && !SkjermlenkeType.UDEFINERT.equals(skjermlenkeType)) {
                delBuilder.medSkjermlenke(skjermlenkeType);
            }
            delBuilder.medBegrunnelse(ttv.getBegrunnelse());
        }

        historikkRepository.lagre(lagHistorikkinnslag(behandling, HistorikkinnslagType.SAK_GODKJENT, delBuilder));
    }

    private Historikkinnslag lagHistorikkinnslag(Behandling behandling, HistorikkinnslagType historikkinnslagType,
                                                 HistorikkInnslagTekstBuilder builder) {
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setBehandling(behandling);
        historikkinnslag.setAktør(HistorikkAktør.BESLUTTER);
        historikkinnslag.setType(historikkinnslagType);
        builder.build(historikkinnslag);

        return historikkinnslag;
    }

    private HistorikkinnslagTotrinnsvurdering lagHistorikkinnslagTotrinnsvurdering(Totrinnsvurdering ttv) {
        HistorikkinnslagTotrinnsvurdering totrinnsVurdering = new HistorikkinnslagTotrinnsvurdering();
        totrinnsVurdering.setAksjonspunktDefinisjon(ttv.getAksjonspunktDefinisjon());
        totrinnsVurdering.setBegrunnelse(ttv.getBegrunnelse());
        totrinnsVurdering.setGodkjent(Boolean.TRUE.equals(ttv.isGodkjent()));
        return totrinnsVurdering;
    }

    public VedtakResultatType utledVedtakResultatType(Behandling behandling) {
        var behandlingResultatType = getBehandlingsresultat(behandling.getId()).getBehandlingResultatType();
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            return UtledVedtakResultatTypeES.utled(behandling.getType(), behandlingResultatType);
        } else {
            if (BehandlingResultatType.INGEN_ENDRING.equals(behandlingResultatType)) {
                var original = behandling.getOriginalBehandlingId().map(behandlingRepository::hentBehandling)
                    .orElseThrow(() -> new IllegalStateException("INGEN ENDRING uten original behandling"));
                return utledVedtakResultatType(original);
            }
            return UtledVedtakResultatType.utled(behandling.getType(), behandlingResultatType);
        }
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
    }

}
