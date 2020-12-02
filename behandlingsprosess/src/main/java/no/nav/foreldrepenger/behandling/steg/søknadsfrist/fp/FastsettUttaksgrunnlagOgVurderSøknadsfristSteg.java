package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import static java.util.Collections.singletonList;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.uttak.SkalKopiereUttaksstegTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.fp.FastsettUttaksgrunnlagTjeneste;

@BehandlingStegRef(kode = "SØKNADSFRIST_FP")
@BehandlingTypeRef
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class FastsettUttaksgrunnlagOgVurderSøknadsfristSteg implements BehandlingSteg {

    private static final Logger LOG = LoggerFactory.getLogger(FastsettUttaksgrunnlagOgVurderSøknadsfristSteg.class);

    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private VurderSøknadsfristTjeneste vurderSøknadsfristTjeneste;
    private FastsettUttaksgrunnlagTjeneste fastsettUttaksgrunnlagTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public FastsettUttaksgrunnlagOgVurderSøknadsfristSteg(UttakInputTjeneste uttakInputTjeneste,
            YtelsesFordelingRepository ytelsesFordelingRepository,
            @FagsakYtelseTypeRef("FP") VurderSøknadsfristTjeneste vurderSøknadsfristTjeneste,
            FastsettUttaksgrunnlagTjeneste fastsettUttaksgrunnlagTjeneste,
            BehandlingRepository behandlingRepository) {
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.vurderSøknadsfristTjeneste = vurderSøknadsfristTjeneste;
        this.fastsettUttaksgrunnlagTjeneste = fastsettUttaksgrunnlagTjeneste;
        this.behandlingRepository = behandlingRepository;
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
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg,
            BehandlingStegType sisteSteg) {
        if (!Objects.equals(BehandlingStegType.SØKNADSFRIST_FORELDREPENGER, førsteSteg)) {
            Optional<YtelseFordelingAggregat> opprinnelig = ytelsesFordelingRepository.hentAggregatHvisEksisterer(kontekst.getBehandlingId());
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
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        if (SkalKopiereUttaksstegTjeneste.skalKopiereStegResultat(behandlingsårsaker(behandling))) {
            var originalBehandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId()).getOriginalBehandlingId().orElseThrow();
            LOG.info("Kopierer yfgrunnlag fra behandling {}, til behandling {}", originalBehandling, kontekst.getBehandlingId());
            ytelsesFordelingRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandling, kontekst.getBehandlingId());
        }
    }

    private List<BehandlingÅrsakType> behandlingsårsaker(Behandling behandling) {
        return behandling.getBehandlingÅrsaker()
                .stream()
                .map(behandlingÅrsak -> behandlingÅrsak.getBehandlingÅrsakType())
                .collect(Collectors.toList());
    }

    private void rydd(Long behandlingId, YtelseFordelingAggregat ytelseFordelingAggregat) {
        YtelseFordelingAggregat.Builder builder = YtelseFordelingAggregat.Builder.oppdatere(Optional.of(ytelseFordelingAggregat));
        YtelseFordelingAggregat ytelseFordeling = builder
                .medJustertFordeling(null)
                .medAvklarteDatoer(new AvklarteUttakDatoerEntitet.Builder(ytelseFordelingAggregat.getAvklarteDatoer())
                        .medOpprinneligEndringsdato(null)
                        .build())
                .build();
        ytelsesFordelingRepository.lagre(behandlingId, ytelseFordeling);
    }
}
