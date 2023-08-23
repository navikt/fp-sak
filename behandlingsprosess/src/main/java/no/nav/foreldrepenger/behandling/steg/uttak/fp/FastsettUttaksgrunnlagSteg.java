package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.KopierForeldrepengerUttaktjeneste;
import no.nav.foreldrepenger.domene.uttak.SkalKopiereUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.FastsettUttaksgrunnlagTjeneste;

@BehandlingStegRef(BehandlingStegType.GRUNNLAG_UTTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class FastsettUttaksgrunnlagSteg implements BehandlingSteg {

    private final FastsettUttaksgrunnlagTjeneste fastsettUttaksgrunnlagTjeneste;
    private final UttakInputTjeneste uttakInputTjeneste;
    private final SkalKopiereUttakTjeneste skalKopiereUttakTjeneste;
    private final KopierForeldrepengerUttaktjeneste kopierForeldrepengerUttaktjeneste;

    @Inject
    public FastsettUttaksgrunnlagSteg(UttakInputTjeneste uttakInputTjeneste,
                                      FastsettUttaksgrunnlagTjeneste fastsettUttaksgrunnlagTjeneste,
                                      SkalKopiereUttakTjeneste skalKopiereUttakTjeneste,
                                      KopierForeldrepengerUttaktjeneste kopierForeldrepengerUttaktjeneste) {
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.fastsettUttaksgrunnlagTjeneste = fastsettUttaksgrunnlagTjeneste;
        this.skalKopiereUttakTjeneste = skalKopiereUttakTjeneste;
        this.kopierForeldrepengerUttaktjeneste = kopierForeldrepengerUttaktjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var input = uttakInputTjeneste.lagInput(behandlingId);
        fastsettUttaksgrunnlagTjeneste.fastsettUttaksgrunnlag(input);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    @Override
    public void vedHoppOverFramover(BehandlingskontrollKontekst kontekst,
                                    BehandlingStegModell modell,
                                    BehandlingStegType førsteSteg,
                                    BehandlingStegType sisteSteg) {
        // TODO(jol) bedre grensesnitt for henleggelser TFP-3721
        if (BehandlingStegType.IVERKSETT_VEDTAK.equals(sisteSteg)) {
            return;
        }
        var uttakInput = uttakInputTjeneste.lagInput(kontekst.getBehandlingId());
        if (skalKopiereUttakTjeneste.skalKopiereStegResultat(uttakInput)) {
            var ref = uttakInput.getBehandlingReferanse();
            kopierForeldrepengerUttaktjeneste.kopierUttaksgrunnlagFraOriginalBehandling(ref.getOriginalBehandlingId().orElseThrow(), ref.behandlingId());
        }
    }
}
