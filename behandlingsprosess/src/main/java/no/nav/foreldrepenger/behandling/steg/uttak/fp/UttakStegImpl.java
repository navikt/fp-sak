package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.domene.uttak.KopierForeldrepengerUttaktjeneste;
import no.nav.foreldrepenger.behandling.steg.uttak.UttakSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.domene.uttak.SkalKopiereUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettUttakManueltAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderTjeneste;

@BehandlingStegRef(kode = "VURDER_UTTAK")
@BehandlingTypeRef
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class UttakStegImpl implements UttakSteg {

    private static final Logger LOG = LoggerFactory.getLogger(UttakStegImpl.class);

    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final FastsettePerioderTjeneste fastsettePerioderTjeneste;
    private final FastsettUttakManueltAksjonspunktUtleder fastsettUttakManueltAksjonspunktUtleder;
    private final FagsakRelasjonRepository fagsakRelasjonRepository;
    private final FpUttakRepository fpUttakRepository;
    private final UttakInputTjeneste uttakInputTjeneste;
    private final UttakStegBeregnStønadskontoTjeneste beregnStønadskontoTjeneste;
    private final FagsakRepository fagsakRepository;
    private final BehandlingRepository behandlingRepository;
    private final SkalKopiereUttakTjeneste skalKopiereUttakTjeneste;
    private final KopierForeldrepengerUttaktjeneste kopierUttaktjeneste;

    @Inject
    public UttakStegImpl(BehandlingRepositoryProvider repositoryProvider,
                         FastsettePerioderTjeneste fastsettePerioderTjeneste,
                         FastsettUttakManueltAksjonspunktUtleder fastsettUttakManueltAksjonspunktUtleder,
                         UttakInputTjeneste uttakInputTjeneste,
                         UttakStegBeregnStønadskontoTjeneste beregnStønadskontoTjeneste,
                         BehandlingRepository behandlingRepository,
                         SkalKopiereUttakTjeneste skalKopiereUttakTjeneste,
                         KopierForeldrepengerUttaktjeneste kopierUttaktjeneste) {
        this.fastsettUttakManueltAksjonspunktUtleder = fastsettUttakManueltAksjonspunktUtleder;
        this.fastsettePerioderTjeneste = fastsettePerioderTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
        this.beregnStønadskontoTjeneste = beregnStønadskontoTjeneste;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = behandlingRepository;
        this.skalKopiereUttakTjeneste = skalKopiereUttakTjeneste;
        this.kopierUttaktjeneste = kopierUttaktjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();

        var input = uttakInputTjeneste.lagInput(behandlingId);

        beregnStønadskontoTjeneste.beregnStønadskontoer(input);

        fastsettePerioderTjeneste.fastsettePerioder(input);

        var aksjonspunkter = fastsettUttakManueltAksjonspunktUtleder.utledAksjonspunkterFor(input);
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst,
                                   BehandlingStegModell modell,
                                   BehandlingStegType førsteSteg,
                                   BehandlingStegType sisteSteg) {
        if (!Objects.equals(BehandlingStegType.VURDER_UTTAK, førsteSteg)) {
            ryddUttak(kontekst.getBehandlingId());
            ryddStønadskontoberegning(kontekst.getBehandlingId(), kontekst.getFagsakId());
        }
    }

    @Override
    public void vedHoppOverFramover(BehandlingskontrollKontekst kontekst,
                                    BehandlingStegModell modell,
                                    BehandlingStegType førsteSteg,
                                    BehandlingStegType sisteSteg) {
        var uttakInput = uttakInputTjeneste.lagInput(kontekst.getBehandlingId());
        if (skalKopiereUttakTjeneste.skalKopiereStegResultat(uttakInput)) {
            kopierUttaktjeneste.kopierUttaksresultatFraOriginalBehandling(uttakInput.getBehandlingReferanse());
        } else {
            ryddUttak(kontekst.getBehandlingId());
            ryddStønadskontoberegning(kontekst.getBehandlingId(), kontekst.getFagsakId());
        }
    }

    private void ryddUttak(Long behandlingId) {
        fpUttakRepository.deaktivterAktivtResultat(behandlingId);
    }

    private void ryddStønadskontoberegning(Long behandlingId, Long fagsakId) {
        var behandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
        if (behandlingsresultat.isEndretStønadskonto()) {
            var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
            fagsakRelasjonRepository.nullstillOverstyrtStønadskontoberegning(fagsak);
            var nyttBehandlingsresultat = Behandlingsresultat.builderEndreEksisterende(behandlingsresultat)
                .medEndretStønadskonto(false)
                .build();
            behandlingsresultatRepository.lagre(behandlingId, nyttBehandlingsresultat);
        }
    }
}
