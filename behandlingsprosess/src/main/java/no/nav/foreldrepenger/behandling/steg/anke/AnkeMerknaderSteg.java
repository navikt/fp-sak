package no.nav.foreldrepenger.behandling.steg.anke;

import static java.util.Collections.singletonList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@BehandlingStegRef(kode = "ANKE_MERKNADER")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class AnkeMerknaderSteg implements BehandlingSteg {

    private static final AksjonspunktDefinisjon AUTO_VENT_ANKE_MERKNADER_FRA_BRUKER = AksjonspunktDefinisjon.AUTO_VENT_ANKE_MERKNADER_FRA_BRUKER;
    private static final long FRIST_VENT_PAA_MERKNADER_FRA_BRUKER = 3;

    private AnkeRepository ankeRepository;
    private BehandlingRepository behandlingRepository;
    private HistorikkRepository historikkRepository;

    public AnkeMerknaderSteg() {
        // For CDI proxy
    }

    @Inject
    public AnkeMerknaderSteg(BehandlingRepository behandlingRepository,
                             AnkeRepository ankeRepository,
                             HistorikkRepository historikkRepository) {
        this.ankeRepository = ankeRepository;
        this.behandlingRepository = behandlingRepository;
        this.historikkRepository = historikkRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Optional<AnkeVurderingResultatEntitet> ankeVurderingResultat = ankeRepository.hentAnkeVurderingResultat(kontekst.getBehandlingId());
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        if (kanAnkeMerknaderUtføres(ankeVurderingResultat)) {
            if (kanBehandlingSettesPåVent(behandling)) {
                return BehandleStegResultat.utførtMedAksjonspunktResultater(singletonList(settAnkebehandlingPåVentOgLagHistorikkInnslag(behandling)));
            }
            List<AksjonspunktDefinisjon> aksjonspunktDefinisjons = singletonList(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_ANKE_MERKNADER);
            return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjons);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean kanBehandlingSettesPåVent(Behandling behandling) {
        List<Aksjonspunkt> behandledeAksjonspunkter = behandling.getBehandledeAksjonspunkter();
        Optional<Aksjonspunkt> autoVentMerknaderFraBruker = behandledeAksjonspunkter.stream()
            .filter(a -> a.getAksjonspunktDefinisjon().equals(AUTO_VENT_ANKE_MERKNADER_FRA_BRUKER))
            .findFirst();

        return autoVentMerknaderFraBruker.isEmpty();
    }

    private boolean kanAnkeMerknaderUtføres(Optional<AnkeVurderingResultatEntitet> ankeVurderingResultat) {
        return ankeVurderingResultat.isPresent()
            && ankeVurderingResultat.get().godkjentAvMedunderskriver()
            && (AnkeVurdering.ANKE_AVVIS.equals(ankeVurderingResultat.get().getAnkeVurdering())
            || AnkeVurdering.ANKE_STADFESTE_YTELSESVEDTAK.equals(ankeVurderingResultat.get().getAnkeVurdering()));
    }

    private AksjonspunktResultat settAnkebehandlingPåVentOgLagHistorikkInnslag(Behandling behandling) {
        LocalDate settPåVentTom = LocalDate.now().plusWeeks(FRIST_VENT_PAA_MERKNADER_FRA_BRUKER);
        LocalDateTime frist = LocalDateTime.of(settPåVentTom, LocalTime.MIDNIGHT);

        lagHistorikkInnslagSattPåVent(behandling, frist);
        return AksjonspunktResultat.opprettForAksjonspunktMedFrist(AUTO_VENT_ANKE_MERKNADER_FRA_BRUKER, Venteårsak.ANKE_VENTER_PAA_MERKNADER_FRA_BRUKER, frist);
    }

    private void lagHistorikkInnslagSattPåVent(Behandling behandling, LocalDateTime frist) {
        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder();
        builder.medHendelse(HistorikkinnslagType.BEH_VENT, frist.toLocalDate());
        builder.medÅrsak(Venteårsak.ANKE_VENTER_PAA_MERKNADER_FRA_BRUKER);

        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(HistorikkinnslagType.BEH_VENT);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());
        builder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }
}
