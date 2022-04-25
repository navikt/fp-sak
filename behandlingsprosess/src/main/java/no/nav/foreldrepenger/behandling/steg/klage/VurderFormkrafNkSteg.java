package no.nav.foreldrepenger.behandling.steg.klage;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_KLAGE;

import java.time.LocalDateTime;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.kabal.SendTilKabalTask;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
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

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var klageVurderingResultat = klageRepository.hentKlageVurderingResultat(kontekst.getBehandlingId(), KlageVurdertAv.NFP)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Skal alltid ha klagevurdering fra NFP "));
        if (KlageVurderingTjeneste.skalBehandlesAvKlageInstans(KlageVurdertAv.NFP, klageVurderingResultat.getKlageVurdering())) {
            var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            var kabalFerdig = klageVurderingResultat.getKlageResultat().erBehandletAvKabal();
            if (kabalFerdig) {
                return BehandleStegResultat.utførtUtenAksjonspunkter();
            } else if (behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_KLAGE)) {
                var apResultat = AksjonspunktResultat.opprettForAksjonspunktMedFrist(AUTO_VENT_PÅ_KABAL_KLAGE, Venteårsak.VENT_KABAL, null);
                return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(apResultat));
            } else {
                var klageHjemmel = klageVurderingResultat.getKlageHjemmel() == null || KlageHjemmel.UDEFINERT.equals(klageVurderingResultat.getKlageHjemmel()) ?
                    KlageHjemmel.standardHjemmelForYtelse(behandling.getFagsakYtelseType()) : klageVurderingResultat.getKlageHjemmel();

                var tilKabalTask = ProsessTaskData.forProsessTask(SendTilKabalTask.class);
                tilKabalTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
                tilKabalTask.setCallIdFraEksisterende();
                tilKabalTask.setProperty(SendTilKabalTask.HJEMMEL_KEY, klageHjemmel.getKode());
                taskTjeneste.lagre(tilKabalTask);
                var frist = LocalDateTime.now().plusYears(3);
                var apVent = AksjonspunktResultat.opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_KLAGE, Venteårsak.VENT_KABAL, frist);
                return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(apVent));
            }
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
