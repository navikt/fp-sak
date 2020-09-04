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
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

@BehandlingStegRef(kode = "ANKE_MERKNADER")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class AnkeMerknaderSteg implements BehandlingSteg {

    private static final AksjonspunktDefinisjon AUTO_VENT_ANKE_MERKNADER_FRA_BRUKER = AksjonspunktDefinisjon.AUTO_VENT_ANKE_MERKNADER_FRA_BRUKER;
    private static final long FRIST_VENT_PAA_MERKNADER_FRA_BRUKER = 3;

    private AnkeRepository ankeRepository;
    private BehandlingRepository behandlingRepository;

    public AnkeMerknaderSteg() {
        // For CDI proxy
    }

    @Inject
    public AnkeMerknaderSteg(BehandlingRepository behandlingRepository,
                             AnkeRepository ankeRepository) {
        this.ankeRepository = ankeRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Optional<AnkeVurderingResultatEntitet> ankeVurderingResultat = ankeRepository.hentAnkeVurderingResultat(kontekst.getBehandlingId());
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        if (skalAnkeMerknaderInnhentingUtføres(ankeVurderingResultat)) {
            if (skalBehandlingVentePåMerknaderFraBruker(behandling)) {
                return BehandleStegResultat.utførtMedAksjonspunktResultater(singletonList(settAnkebehandlingPåVentOgLagHistorikkInnslag(behandling)));
            }
            List<AksjonspunktDefinisjon> aksjonspunktDefinisjons = singletonList(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_ANKE_MERKNADER);
            return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjons);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean skalBehandlingVentePåMerknaderFraBruker(Behandling behandling) {
        return !behandling.harAksjonspunktMedType(AUTO_VENT_ANKE_MERKNADER_FRA_BRUKER);
    }

    private boolean skalAnkeMerknaderInnhentingUtføres(Optional<AnkeVurderingResultatEntitet> ankeVurderingResultat) {
        return ankeVurderingResultat.isPresent()
            && ankeVurderingResultat.get().godkjentAvMedunderskriver()
            && (AnkeVurdering.ANKE_AVVIS.equals(ankeVurderingResultat.get().getAnkeVurdering())
            || AnkeVurdering.ANKE_STADFESTE_YTELSESVEDTAK.equals(ankeVurderingResultat.get().getAnkeVurdering()));
    }

    private AksjonspunktResultat settAnkebehandlingPåVentOgLagHistorikkInnslag(Behandling behandling) {
        LocalDate settPåVentTom = LocalDate.now().plusWeeks(FRIST_VENT_PAA_MERKNADER_FRA_BRUKER);
        LocalDateTime frist = LocalDateTime.of(settPåVentTom, LocalTime.MIDNIGHT);
        return AksjonspunktResultat.opprettForAksjonspunktMedFrist(AUTO_VENT_ANKE_MERKNADER_FRA_BRUKER, Venteårsak.ANKE_VENTER_PAA_MERKNADER_FRA_BRUKER, frist);
    }

}
