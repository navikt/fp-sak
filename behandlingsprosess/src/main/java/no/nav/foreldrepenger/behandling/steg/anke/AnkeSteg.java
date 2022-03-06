package no.nav.foreldrepenger.behandling.steg.anke;

import static java.util.Collections.singletonList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;

@BehandlingStegRef(kode = "ANKE")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class AnkeSteg implements BehandlingSteg {
    private AnkeRepository ankeRepository;

    public AnkeSteg() {
        // For CDI proxy
    }

    @Inject
    public AnkeSteg(AnkeRepository ankeRepository) {
        this.ankeRepository = ankeRepository;
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
        var aksjonspunktDefinisjons = singletonList(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_ANKE);
        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjons);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg,
            BehandlingStegType sisteSteg) {
        ankeRepository.settAnkeGodkjentHosMedunderskriver(kontekst.getBehandlingId(), false);
    }
}
