package no.nav.foreldrepenger.behandling.steg.dekningsgrad;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.EndreDekningsgradVedDødTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

@BehandlingStegRef(BehandlingStegType.DEKNINGSGRAD)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class DekningsgradSteg implements BehandlingSteg {

    private final FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private final FamilieHendelseTjeneste familieHendelseTjeneste;
    private final EndreDekningsgradVedDødTjeneste endreDekningsgradVedDødTjeneste;

    @Inject
    public DekningsgradSteg(FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                            FamilieHendelseTjeneste familieHendelseTjeneste,
                            EndreDekningsgradVedDødTjeneste endreDekningsgradVedDødTjeneste) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.endreDekningsgradVedDødTjeneste = endreDekningsgradVedDødTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        //TODO TFP-5702: sett sakskompleksDG i YF. Se oppgitt vs fagsakrel vs annen parts åpen behandling vs dødsfall. Opprette AP hvis uavklart
        if (erBarnDødsfall(kontekst)) {
            endreDekningsgradVedDødTjeneste.endreDekningsgradTil100(kontekst.getBehandlingId());
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean erBarnDødsfall(BehandlingskontrollKontekst kontekst) {
        var fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(kontekst.getFagsakId()).orElseThrow();
        var fh = familieHendelseTjeneste.hentAggregat(kontekst.getBehandlingId());
        var barna = fh.getGjeldendeVersjon().getBarna();
        return VurderDekningsgradVedDødsfall.skalEndreDekningsgrad(fagsakRelasjon.getGjeldendeDekningsgrad(), barna);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst,
                                   BehandlingStegModell modell,
                                   BehandlingStegType førsteSteg,
                                   BehandlingStegType sisteSteg) {
        //TODO TFP-5702 rydd sakskompleksDG??
    }
}
