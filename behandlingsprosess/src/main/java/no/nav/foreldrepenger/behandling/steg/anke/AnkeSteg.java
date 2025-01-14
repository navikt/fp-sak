package no.nav.foreldrepenger.behandling.steg.anke;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.anke.AnkeVurderingTjeneste;
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
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

@BehandlingStegRef(BehandlingStegType.ANKE)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class AnkeSteg implements BehandlingSteg {

    private static final Logger LOG = LoggerFactory.getLogger(AnkeSteg.class);

    private BehandlingRepository behandlingRepository;
    private AnkeVurderingTjeneste ankeVurderingTjeneste;

    public AnkeSteg() {
        // For CDI proxy
    }

    @Inject
    public AnkeSteg(AnkeVurderingTjeneste ankeVurderingTjeneste, BehandlingRepositoryProvider behandlingRepositoryProvider ) {
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.ankeVurderingTjeneste = ankeVurderingTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        /*
         * Ved første besøk kan anke være opprettet manuelt i VL eller pga opprettet-anke-hendelse fra KABAL med referanse
         * Ved senere besøk kan hoppet tilbake, ta av kabal-vent uten resultat, eller avsluttet-anke-hendelse fra KABAL med referanse
         * - Anke opprettet i KABAL og behandling i VL med referanse -> settes på vent til behandling i Kabal avsluttet
         * - Anke avsluttet / trukket -> henlegges utenfor steg
         * - Anke avsluttet / retur -> ukjent betydning, exception utenfor steg
         * - Anke avsluttet / andre utfall -> fortsett/avslutt uten flere AP.
        */
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var kabalReferanse = ankeVurderingTjeneste.hentAnkeResultatHvisEksisterer(behandling)
            .map(AnkeResultatEntitet::erBehandletAvKabal).orElse(false);
        var harVentKabal = behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_ANKE);

        if (kabalReferanse) { // Skal ikke oversendes
            // Første gang med kabalRef -> vent på kabal
            // Tatt av vent med kabalref -> har mottatt resultat fra kabal. gå videre
            if (!harVentKabal || manglerAnkeVurdering(behandling)) {
                return BehandleStegResultat.utførtMedAksjonspunktResultat(ventPåKabal());
            } else {
                return BehandleStegResultat.utførtUtenAksjonspunkter();
            }
        }

        return BehandleStegResultat.utførtMedAksjonspunktResultat(ventPåKabal());
    }

    private AksjonspunktResultat ventPåKabal() {
        return AksjonspunktResultat.opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.AUTO_VENT_PÅ_KABAL_ANKE, Venteårsak.VENT_KABAL, null);
    }

    private boolean manglerAnkeVurdering(Behandling anke) {
        var ankeVurdering = ankeVurderingTjeneste.hentAnkeVurderingResultat(anke);
        return ankeVurdering.isEmpty() || ankeVurdering.map(AnkeVurderingResultatEntitet::getAnkeVurdering)
            .filter(AnkeVurdering.UDEFINERT::equals)
            .isPresent();
    }

    private Long lagrePåanketBehandling(Behandling anke, Behandling klage) {
        ankeVurderingTjeneste.oppdaterAnkeMedPåanketKlage(anke, klage.getId());
        return klage.getId();
    }
}
