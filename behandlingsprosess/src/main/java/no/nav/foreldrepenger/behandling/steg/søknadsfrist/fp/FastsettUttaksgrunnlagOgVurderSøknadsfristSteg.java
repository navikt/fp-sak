package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import static java.util.Collections.singletonList;

import java.util.Objects;
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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.KopierForeldrepengerUttaktjeneste;
import no.nav.foreldrepenger.domene.uttak.SkalKopiereUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.FastsettUttaksgrunnlagTjeneste;

@BehandlingStegRef(BehandlingStegType.SØKNADSFRIST_FORELDREPENGER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class FastsettUttaksgrunnlagOgVurderSøknadsfristSteg implements BehandlingSteg {

    private static final Logger LOG = LoggerFactory.getLogger(FastsettUttaksgrunnlagOgVurderSøknadsfristSteg.class);

    private final YtelsesFordelingRepository ytelsesFordelingRepository;
    private final VurderSøknadsfristTjeneste vurderSøknadsfristTjeneste;
    private final FastsettUttaksgrunnlagTjeneste fastsettUttaksgrunnlagTjeneste;
    private final UttakInputTjeneste uttakInputTjeneste;
    private final BehandlingRepository behandlingRepository;
    private final SkalKopiereUttakTjeneste skalKopiereUttakTjeneste;
    private final KopierForeldrepengerUttaktjeneste kopierForeldrepengerUttaktjeneste;

    @Inject
    public FastsettUttaksgrunnlagOgVurderSøknadsfristSteg(UttakInputTjeneste uttakInputTjeneste,
                                                          YtelsesFordelingRepository ytelsesFordelingRepository,
                                                          @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) VurderSøknadsfristTjeneste vurderSøknadsfristTjeneste,
                                                          FastsettUttaksgrunnlagTjeneste fastsettUttaksgrunnlagTjeneste,
                                                          BehandlingRepository behandlingRepository,
                                                          SkalKopiereUttakTjeneste skalKopiereUttakTjeneste,
                                                          KopierForeldrepengerUttaktjeneste kopierForeldrepengerUttaktjeneste) {
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.vurderSøknadsfristTjeneste = vurderSøknadsfristTjeneste;
        this.fastsettUttaksgrunnlagTjeneste = fastsettUttaksgrunnlagTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.skalKopiereUttakTjeneste = skalKopiereUttakTjeneste;
        this.kopierForeldrepengerUttaktjeneste = kopierForeldrepengerUttaktjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();

        // Sjekk søknadsfrist for søknadsperioder
        var søknadfristAksjonspunktDefinisjon = vurderSøknadsfristTjeneste.vurder(kontekst.getBehandlingId());

        // Fastsett uttaksgrunnlag
        var input = uttakInputTjeneste.lagInput(behandlingId);
        fastsettUttaksgrunnlagTjeneste.fastsettUttaksgrunnlag(input);

        // Returner eventuelt aksjonspunkt ifm søknadsfrist
        if (søknadfristAksjonspunktDefinisjon.isPresent()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(singletonList(søknadfristAksjonspunktDefinisjon.get()));
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst,
                                   BehandlingStegModell modell,
                                   BehandlingStegType førsteSteg,
                                   BehandlingStegType sisteSteg) {
        if (!Objects.equals(BehandlingStegType.SØKNADSFRIST_FORELDREPENGER, førsteSteg)) {
            var opprinnelig = ytelsesFordelingRepository.hentAggregatHvisEksisterer(
                kontekst.getBehandlingId());
            if (opprinnelig.isPresent()) {
                rydd(kontekst.getBehandlingId(), opprinnelig.get());
            }
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
            var ref = uttakInput.getBehandlingReferanse();
            kopierForeldrepengerUttaktjeneste.kopierUttaksgrunnlagSøknadsfristResultatFraOriginalBehandling(ref.getOriginalBehandlingId().orElseThrow(), ref.getBehandlingId());
        }
    }

    private void rydd(Long behandlingId, YtelseFordelingAggregat ytelseFordelingAggregat) {
        var builder = YtelseFordelingAggregat.Builder.oppdatere(
            Optional.of(ytelseFordelingAggregat));
        var ytelseFordeling = builder.medJustertFordeling(null)
            .medAvklarteDatoer(new AvklarteUttakDatoerEntitet.Builder(
                ytelseFordelingAggregat.getAvklarteDatoer()).medOpprinneligEndringsdato(null).build())
            .build();
        ytelsesFordelingRepository.lagre(behandlingId, ytelseFordeling);
    }
}
