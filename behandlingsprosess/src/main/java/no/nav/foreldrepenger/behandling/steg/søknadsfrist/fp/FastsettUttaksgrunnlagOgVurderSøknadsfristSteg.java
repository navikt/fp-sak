package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import static java.util.Collections.singletonList;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.fp.FastsettUttaksgrunnlagTjeneste;

@BehandlingStegRef(kode = "SØKNADSFRIST_FP")
@BehandlingTypeRef
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class FastsettUttaksgrunnlagOgVurderSøknadsfristSteg implements BehandlingSteg {

    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private SøknadsfristTjeneste søknadsfristForeldrepengerTjeneste;
    private FastsettUttaksgrunnlagTjeneste fastsettUttaksgrunnlagTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;

    @Inject
    public FastsettUttaksgrunnlagOgVurderSøknadsfristSteg(UttakInputTjeneste uttakInputTjeneste,
                                                          YtelsesFordelingRepository ytelsesFordelingRepository,
                                                          SøknadsfristTjeneste søknadsfristForeldrepengerTjeneste,
                                                          FastsettUttaksgrunnlagTjeneste fastsettUttaksgrunnlagTjeneste) {
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.søknadsfristForeldrepengerTjeneste = søknadsfristForeldrepengerTjeneste;
        this.fastsettUttaksgrunnlagTjeneste = fastsettUttaksgrunnlagTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();

        var input = uttakInputTjeneste.lagInput(behandlingId);

        //Sjekk søknadsfrist for søknadsperioder
        Optional<AksjonspunktDefinisjon> søknadfristAksjonspunktDefinisjon = søknadsfristForeldrepengerTjeneste.vurderSøknadsfristForForeldrepenger(kontekst);

        //Fastsett uttaksgrunnlag
        fastsettUttaksgrunnlagTjeneste.fastsettUttaksgrunnlag(input);

        //Returner eventuelt aksjonspunkt ifm søknadsfrist
        if (søknadfristAksjonspunktDefinisjon.isPresent()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(singletonList(søknadfristAksjonspunktDefinisjon.get()));
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg, BehandlingStegType sisteSteg) {
        if (!Objects.equals(BehandlingStegType.SØKNADSFRIST_FORELDREPENGER, førsteSteg)) {
            Optional<YtelseFordelingAggregat> opprinnelig = ytelsesFordelingRepository.hentAggregatHvisEksisterer(kontekst.getBehandlingId());
            if (opprinnelig.isPresent()) {
                rydd(kontekst.getBehandlingId(), opprinnelig.get());
            }
        }
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
