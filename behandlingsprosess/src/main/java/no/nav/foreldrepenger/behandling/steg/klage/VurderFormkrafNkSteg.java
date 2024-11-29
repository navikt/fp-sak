package no.nav.foreldrepenger.behandling.steg.klage;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_KLAGE;

import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.kabal.SendTilKabalTask;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@BehandlingStegRef(BehandlingStegType.KLAGE_VURDER_FORMKRAV_NK)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderFormkrafNkSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private KlageRepository klageRepository;
    private ProsessTaskTjeneste taskTjeneste;

    public VurderFormkrafNkSteg() {
        // For CDI proxy
    }

    @Inject
    public VurderFormkrafNkSteg(BehandlingRepository behandlingRepository,
                                KlageRepository klageRepository,
                                ProsessTaskTjeneste taskTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.klageRepository = klageRepository;
        this.taskTjeneste = taskTjeneste;
    }

    /**
     * Kompleks håndtering:
     * - Første gang: Hvis NFP Stadfest -> Kabel, ellers passere i stillhet
     * - Retur 1: Kabalutfall utenom RETUR foreligger og tatt av vent (på kabal) - gå videre
     * - Retur 2: Kabalutfall RETUR foreligger og fortsatt stadfestet - send til Kabal på nytt. Utfall RETUR avbryter autopunkt.
     * - Retur 3: Manuelt tatt av vent (på kabal) - gå tilbake på vent og ikke send på nytt. Ta av vent setter autopunkt utført.
     * - Ikke håndtert: Henlagt i VL, fulgt av utfall RETUR el fra Kabal.
     */
    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var klageVurderingResultat = klageRepository.hentKlageVurderingResultat(kontekst.getBehandlingId(), KlageVurdertAv.NFP)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Skal alltid ha klagevurdering fra NFP "));
        if (KlageVurderingTjeneste.skalBehandlesAvKlageInstans(KlageVurdertAv.NFP, klageVurderingResultat.getKlageVurdering())) {
            var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            var kabalFerdig = klageVurderingResultat.getKlageResultat().erBehandletAvKabal();
            if (kabalFerdig) {
                return BehandleStegResultat.utførtUtenAksjonspunkter();
            } else if (erAlleredeSendtTilKabal(behandling)) {
                var apResultat = AksjonspunktResultat.opprettForAksjonspunktMedFrist(AUTO_VENT_PÅ_KABAL_KLAGE, Venteårsak.VENT_KABAL, null);
                return BehandleStegResultat.utførtMedAksjonspunktResultat(apResultat);
            } else {
                var klageHjemmel = klageVurderingResultat.getKlageHjemmel() == null || KlageHjemmel.UDEFINERT.equals(klageVurderingResultat.getKlageHjemmel()) ?
                    KlageHjemmel.standardHjemmelForYtelse(behandling.getFagsakYtelseType()) : klageVurderingResultat.getKlageHjemmel();

                var tilKabalTask = ProsessTaskData.forProsessTask(SendTilKabalTask.class);
                tilKabalTask.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
                tilKabalTask.setCallIdFraEksisterende();
                tilKabalTask.setProperty(SendTilKabalTask.HJEMMEL_KEY, klageHjemmel.getKode());
                taskTjeneste.lagre(tilKabalTask);
                var frist = LocalDateTime.now().plusYears(3);
                var apVent = AksjonspunktResultat.opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_KLAGE, Venteårsak.VENT_KABAL, frist);
                return BehandleStegResultat.utførtMedAksjonspunktResultat(apVent);
            }
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean erAlleredeSendtTilKabal(Behandling behandling) {
        return behandling.getAksjonspunktMedDefinisjonOptional(AUTO_VENT_PÅ_KABAL_KLAGE)
            .filter(a -> !AksjonspunktStatus.AVBRUTT.equals(a.getStatus()))
            .isPresent();
    }
}
