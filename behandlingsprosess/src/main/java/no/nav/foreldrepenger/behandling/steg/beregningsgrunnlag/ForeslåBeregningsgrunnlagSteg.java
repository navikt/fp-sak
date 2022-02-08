package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp.VurderDekningsgradVedDødsfallAksjonspunktUtleder;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.prosess.KalkulusTjeneste;

@FagsakYtelseTypeRef
@BehandlingStegRef(kode = "FORS_BERGRUNN")
@BehandlingTypeRef
@ApplicationScoped
public class ForeslåBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private BeregningTjeneste beregningTjeneste;
    private FagsakRelasjonRepository fagsakRelasjonRepository;


    protected ForeslåBeregningsgrunnlagSteg() {
        // for CDI proxy
    }

    @Inject
    public ForeslåBeregningsgrunnlagSteg(BehandlingRepository behandlingRepository,
                                         FamilieHendelseRepository familieHendelseRepository, BeregningTjeneste beregningTjeneste,
                                         FagsakRelasjonRepository fagsakRelasjonRepository) {
        this.behandlingRepository = behandlingRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.beregningTjeneste = beregningTjeneste;
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);
        var resultat = beregningTjeneste.beregn(ref, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG);

        var aksjonspunkter = resultat.getAksjonspunkter().stream().map(BeregningAksjonspunktResultatMapper::map).collect(Collectors.toList());

        if (behandling.getFagsakYtelseType().equals(FagsakYtelseType.FORELDREPENGER)) {
            boolean skalHaAksjonspunktForVurderDekningsgrad = VurderDekningsgradVedDødsfallAksjonspunktUtleder.utled(finnDekningsgrad(ref),
                getBarn(ref.getBehandlingId()));
            if (skalHaAksjonspunktForVurderDekningsgrad) {
                aksjonspunkter.add(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_DEKNINGSGRAD));
            }
        }
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
    }

    private int finnDekningsgrad(BehandlingReferanse behandlingReferanse) {
        var fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonHvisEksisterer(behandlingReferanse.getSaksnummer());
        return fagsakRelasjon.map(FagsakRelasjon::getGjeldendeDekningsgrad)
            .map(Dekningsgrad::getVerdi)
            .orElseThrow(() -> new IllegalStateException("Mangler FagsakRelasjon#dekningsgrad for behandling: " + behandlingReferanse));
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst,
                                   BehandlingStegModell modell,
                                   BehandlingStegType tilSteg,
                                   BehandlingStegType fraSteg) {
        beregningTjeneste.rydd(kontekst, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, tilSteg);
    }

    private List<UidentifisertBarn> getBarn(Long behandlingId) {
        Objects.requireNonNull(familieHendelseRepository, "familieHendelseRepository");
        var familiehendelse = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon);
        return familiehendelse.orElseThrow().getBarna();
    }
}
