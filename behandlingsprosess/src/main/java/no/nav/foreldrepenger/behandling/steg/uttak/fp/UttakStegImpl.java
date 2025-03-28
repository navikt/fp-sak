package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.uttak.UttakSteg;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.domene.uttak.KopierForeldrepengerUttaktjeneste;
import no.nav.foreldrepenger.domene.uttak.SkalKopiereUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettUttakManueltAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderTjeneste;

@BehandlingStegRef(BehandlingStegType.VURDER_UTTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class UttakStegImpl implements UttakSteg {

    private final FastsettePerioderTjeneste fastsettePerioderTjeneste;
    private final FastsettUttakManueltAksjonspunktUtleder fastsettUttakManueltAksjonspunktUtleder;
    private final FpUttakRepository fpUttakRepository;
    private final UttakInputTjeneste uttakInputTjeneste;
    private final UttakStegBeregnStønadskontoTjeneste beregnStønadskontoTjeneste;
    private final SkalKopiereUttakTjeneste skalKopiereUttakTjeneste;
    private final KopierForeldrepengerUttaktjeneste kopierUttaktjeneste;

    @Inject
    public UttakStegImpl(BehandlingRepositoryProvider repositoryProvider,
                         FastsettePerioderTjeneste fastsettePerioderTjeneste,
                         FastsettUttakManueltAksjonspunktUtleder fastsettUttakManueltAksjonspunktUtleder,
                         UttakInputTjeneste uttakInputTjeneste,
                         UttakStegBeregnStønadskontoTjeneste beregnStønadskontoTjeneste,
                         SkalKopiereUttakTjeneste skalKopiereUttakTjeneste,
                         KopierForeldrepengerUttaktjeneste kopierUttaktjeneste) {
        this.fastsettUttakManueltAksjonspunktUtleder = fastsettUttakManueltAksjonspunktUtleder;
        this.fastsettePerioderTjeneste = fastsettePerioderTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
        this.beregnStønadskontoTjeneste = beregnStønadskontoTjeneste;
        this.skalKopiereUttakTjeneste = skalKopiereUttakTjeneste;
        this.kopierUttaktjeneste = kopierUttaktjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();

        var input = uttakInputTjeneste.lagInput(behandlingId);

        var kontoutregningForBehandling = beregnStønadskontoTjeneste.fastsettStønadskontoerForBehandling(input);

        fastsettePerioderTjeneste.fastsettePerioder(input, kontoutregningForBehandling);

        var aksjonspunkter = fastsettUttakManueltAksjonspunktUtleder.utledAksjonspunkterFor(input)
            .stream().map(AksjonspunktResultat::opprettForAksjonspunkt).toList();
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst,
                                   BehandlingStegModell modell,
                                   BehandlingStegType førsteSteg,
                                   BehandlingStegType sisteSteg) {
        if (!Objects.equals(BehandlingStegType.VURDER_UTTAK, førsteSteg)) {
            ryddUttak(kontekst.getBehandlingId());
        }
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
            kopierUttaktjeneste.kopierUttaksresultatFraOriginalBehandling(uttakInput.getBehandlingReferanse().getOriginalBehandlingId().orElseThrow(),
                uttakInput.getBehandlingReferanse().behandlingId());
        } else {
            ryddUttak(kontekst.getBehandlingId());
        }
    }

    private void ryddUttak(Long behandlingId) {
        fpUttakRepository.deaktivterAktivtResultat(behandlingId);
    }

}
