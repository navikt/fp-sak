package no.nav.foreldrepenger.behandling.steg.anke;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.konfig.Environment;

@BehandlingStegRef(BehandlingStegType.ANKE)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class AnkeSteg implements BehandlingSteg {
    private static final boolean ER_PROD = Environment.current().isProd();

    private AnkeRepository ankeRepository;
    private BehandlingRepository behandlingRepository;

    public AnkeSteg() {
        // For CDI proxy
    }

    @Inject
    public AnkeSteg(AnkeRepository ankeRepository,
                    BehandlingRepository behandlingRepository) {
        this.ankeRepository = ankeRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        /**
         * TODO: Gå opp dynamikk rundt kabal. Må håndtere en del scenarier
         * - Anke opprettet i VL, uten kabalhendelse -> overfør til kabal manuelt/automatisk/begge - tenk steg + oppdaterer. Sett på vent
         * - Anke opprettet i KABAL og behandling i VL med referanse -> settes på vent til behandling i Kabal avsluttet
         * - Anke avsluttet / trukket -> henlegg
         * - Anke avsluttet / retur -> ukjent betydning
         * - Anke avsluttet / stadfest/avvist -> fortsett/avslutt. avklar oppførsel vs TR
         * - Anke avsluttet / opphev/endre -> fortsett/avslutt. avklar oppførsel vs TR
        */
        if (!ER_PROD) {
            // Ved første besøk kan anke være opprettet manuelt i VL eller pga opprettet-anke-hendelse fra KABAL med referanse
            // Ved senere besøk kan hoppet tilbake, ta av kabal-vent uten resultat, eller avsluttet-anke-hendelse fra KABAL med referanse
            var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            var kabalReferanse = ankeRepository.hentAnkeResultat(kontekst.getBehandlingId())
                .map(AnkeResultatEntitet::erBehandletAvKabal).orElse(false);
            var harVentKabal = behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_ANKE);
            if (kabalReferanse) {
                // Første gang med kabalRef -> reset kabalreferanse, vent på kabal
                // Tatt av vent med kabalref -> har mottatt resultat fra kabal. gå videre
                if (!harVentKabal || manglerAnkeVurdering(behandling.getId())) {
                    return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(ventPåKabal()));
                } else {
                    return BehandleStegResultat.utførtUtenAksjonspunkter();
                }
            }
            if (harVentKabal) {
                // Ingen vei tilbake ....
                return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(ventPåKabal()));
            }
            // Ellers default oppførsel
        }
        return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_ANKE));
    }

    private AksjonspunktResultat ventPåKabal() {
        return AksjonspunktResultat.opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_ANKE, Venteårsak.VENT_KABAL, null);
    }

    private boolean manglerAnkeVurdering(Long behandlingId) {
        var ankeVurdering = ankeRepository.hentAnkeVurderingResultat(behandlingId);
        return ankeVurdering.isEmpty() || ankeVurdering.map(AnkeVurderingResultatEntitet::getAnkeVurdering)
            .filter(AnkeVurdering.UDEFINERT::equals)
            .isPresent();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg,
            BehandlingStegType sisteSteg) {
        ankeRepository.settAnkeGodkjentHosMedunderskriver(kontekst.getBehandlingId(), false);
    }
}
