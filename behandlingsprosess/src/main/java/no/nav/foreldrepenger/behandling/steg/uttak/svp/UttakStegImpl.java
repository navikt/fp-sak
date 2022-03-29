package no.nav.foreldrepenger.behandling.steg.uttak.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.uttak.UttakSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegKoder;
import no.nav.foreldrepenger.domene.uttak.svp.FastsettUttaksresultatTjeneste;

@BehandlingStegRef(kode = BehandlingStegKoder.VURDER_UTTAK_KODE)
@BehandlingTypeRef
@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class UttakStegImpl implements UttakSteg {

    private FastsettUttaksresultatTjeneste fastsettUttaksresultatTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;

    @Inject
    public UttakStegImpl(UttakInputTjeneste uttakInputTjeneste,
            FastsettUttaksresultatTjeneste fastsettUttaksresultatTjeneste) {
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.fastsettUttaksresultatTjeneste = fastsettUttaksresultatTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        var behandlingId = kontekst.getBehandlingId();

        var input = uttakInputTjeneste.lagInput(behandlingId);

        fastsettUttaksresultatTjeneste.fastsettUttaksresultat(input);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
