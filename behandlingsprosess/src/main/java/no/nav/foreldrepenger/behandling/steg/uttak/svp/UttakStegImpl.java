package no.nav.foreldrepenger.behandling.steg.uttak.svp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.uttak.UttakSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.svp.FastsettUttaksresultatTjeneste;

@BehandlingStegRef(BehandlingStegType.VURDER_UTTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class UttakStegImpl implements UttakSteg {

    private final FastsettUttaksresultatTjeneste fastsettUttaksresultatTjeneste;
    private final UttakInputTjeneste uttakInputTjeneste;

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
