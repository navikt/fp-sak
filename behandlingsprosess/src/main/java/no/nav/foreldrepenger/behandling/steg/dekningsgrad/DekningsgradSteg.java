package no.nav.foreldrepenger.behandling.steg.dekningsgrad;

import java.util.Optional;

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
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

@BehandlingStegRef(BehandlingStegType.DEKNINGSGRAD)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class DekningsgradSteg implements BehandlingSteg {

    private final FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private final FamilieHendelseTjeneste familieHendelseTjeneste;
    private final EndreDekningsgradVedDødTjeneste endreDekningsgradVedDødTjeneste;
    private final YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    public DekningsgradSteg(FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                            FamilieHendelseTjeneste familieHendelseTjeneste,
                            EndreDekningsgradVedDødTjeneste endreDekningsgradVedDødTjeneste,
                            YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.endreDekningsgradVedDødTjeneste = endreDekningsgradVedDødTjeneste;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        if (erBarnDødsfall(kontekst)) {
            endreDekningsgradVedDødTjeneste.endreDekningsgradTil100(behandlingId);
        }
        var fagsakRelasjonDekningsgrad = hentFagsakRelasjon(kontekst).getGjeldendeDekningsgrad();
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandlingId);
        var eksisterendeSakskompleksDekningsgrad = ytelseFordelingAggregat.getSakskompleksDekningsgrad();
        var annenPartsOppgittDekningsgrad = finnAnnenPartsOppgittDekningsgrad(kontekst.getFagsakId()).orElse(null);
        var fh = familieHendelseTjeneste.hentAggregat(behandlingId).getGjeldendeVersjon();
        return SakskompleksDekningsgradUtleder.utledFor(fagsakRelasjonDekningsgrad, eksisterendeSakskompleksDekningsgrad,
            ytelseFordelingAggregat.getOppgittDekningsgrad(), annenPartsOppgittDekningsgrad, fh).map(d -> {
            ytelseFordelingTjeneste.lagreSakskompleksDekningsgrad(behandlingId, d);
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }).orElseGet(() -> {
            //TODO TFP-5702 aksjonspunkt
            return BehandleStegResultat.utførtUtenAksjonspunkter();
            });
    }

    private Optional<Dekningsgrad> finnAnnenPartsOppgittDekningsgrad(Long fagsakId) {
        //TODO TFP-5702 Bare åpne behandlinger før vedtak?
        return Optional.empty();
    }

    private boolean erBarnDødsfall(BehandlingskontrollKontekst kontekst) {
        var fagsakRelasjon = hentFagsakRelasjon(kontekst);
        var fh = familieHendelseTjeneste.hentAggregat(kontekst.getBehandlingId());
        var barna = fh.getGjeldendeVersjon().getBarna();
        return VurderDekningsgradVedDødsfall.skalEndreDekningsgrad(fagsakRelasjon.getGjeldendeDekningsgrad(), barna);
    }

    private FagsakRelasjon hentFagsakRelasjon(BehandlingskontrollKontekst kontekst) {
        return fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(kontekst.getFagsakId()).orElseThrow();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst,
                                   BehandlingStegModell modell,
                                   BehandlingStegType førsteSteg,
                                   BehandlingStegType sisteSteg) {
        //TODO TFP-5702 rydd sakskompleksDG??
    }
}
