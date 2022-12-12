package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.KopierForeldrepengerUttaktjeneste;
import no.nav.foreldrepenger.domene.uttak.SkalKopiereUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.FastsettUttaksgrunnlagTjeneste;

@BehandlingStegRef(BehandlingStegType.GRUNNLAG_UTTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class FastsettUttaksgrunnlagSteg implements BehandlingSteg {

    private static final Logger LOG = LoggerFactory.getLogger(FastsettUttaksgrunnlagSteg.class);

    private final YtelsesFordelingRepository ytelsesFordelingRepository;
    private final FastsettUttaksgrunnlagTjeneste fastsettUttaksgrunnlagTjeneste;
    private final UttakInputTjeneste uttakInputTjeneste;
    private final SkalKopiereUttakTjeneste skalKopiereUttakTjeneste;
    private final KopierForeldrepengerUttaktjeneste kopierForeldrepengerUttaktjeneste;

    @Inject
    public FastsettUttaksgrunnlagSteg(UttakInputTjeneste uttakInputTjeneste,
                                      YtelsesFordelingRepository ytelsesFordelingRepository,
                                      FastsettUttaksgrunnlagTjeneste fastsettUttaksgrunnlagTjeneste,
                                      SkalKopiereUttakTjeneste skalKopiereUttakTjeneste,
                                      KopierForeldrepengerUttaktjeneste kopierForeldrepengerUttaktjeneste) {
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.fastsettUttaksgrunnlagTjeneste = fastsettUttaksgrunnlagTjeneste;
        this.skalKopiereUttakTjeneste = skalKopiereUttakTjeneste;
        this.kopierForeldrepengerUttaktjeneste = kopierForeldrepengerUttaktjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();

        // Fastsett uttaksgrunnlag - vurder å lage input på nytt (potensiell sideeffekt fra frist)
        var input = uttakInputTjeneste.lagInput(behandlingId);
        fastsettUttaksgrunnlagTjeneste.fastsettUttaksgrunnlag(input);

        // Returner eventuelt aksjonspunkt ifm søknadsfrist
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst,
                                   BehandlingStegModell modell,
                                   BehandlingStegType førsteSteg,
                                   BehandlingStegType sisteSteg) {
        ytelsesFordelingRepository.hentAggregatHvisEksisterer(kontekst.getBehandlingId())
            .ifPresent(a -> {
                var ytelseFordelingBuilder = YtelseFordelingAggregat.Builder.oppdatere(Optional.of(a))
                    .medJustertFordeling(null)
                    .medOverstyrtFordeling(null)
                    .medAvklarteDatoer(new AvklarteUttakDatoerEntitet.Builder(a.getAvklarteDatoer())
                        .medOpprinneligEndringsdato(null)
                        .medJustertEndringsdato(null)
                        .build());
                ytelsesFordelingRepository.lagre(kontekst.getBehandlingId(), ytelseFordelingBuilder.build());
            });
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
