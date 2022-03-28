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
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegKoder;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;

@FagsakYtelseTypeRef
@BehandlingStegRef(kode = BehandlingStegKoder.FORESLÅ_BEREGNINGSGRUNNLAG_KODE)
@BehandlingTypeRef
@ApplicationScoped
public class ForeslåBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;

    protected ForeslåBeregningsgrunnlagSteg() {
        // for CDI proxy
    }

    @Inject
    public ForeslåBeregningsgrunnlagSteg(BehandlingRepository behandlingRepository,
            FamilieHendelseRepository familieHendelseRepository,
            BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
            BeregningsgrunnlagInputProvider inputTjenesteProvider) {
        this.behandlingRepository = behandlingRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.beregningsgrunnlagInputProvider = Objects.requireNonNull(inputTjenesteProvider, "inputTjenesteProvider");
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);
        var input = getInputTjeneste(ref.getFagsakYtelseType()).lagInput(ref.getBehandlingId());
        var resultat = beregningsgrunnlagKopierOgLagreTjeneste.foreslåBeregningsgrunnlag(input);

        var aksjonspunkter = resultat.getAksjonspunkter().stream().map(BeregningAksjonspunktResultatMapper::map)
                .collect(Collectors.toList());

        if (behandling.getFagsakYtelseType().equals(FagsakYtelseType.FORELDREPENGER)) {
            boolean skalHaAksjonspunktForVurderDekningsgrad = VurderDekningsgradVedDødsfallAksjonspunktUtleder.utled(input.getYtelsespesifiktGrunnlag().getDekningsgrad(input.getBeregningsgrunnlag(), null),
                getBarn(ref.getBehandlingId()));
            if (skalHaAksjonspunktForVurderDekningsgrad) {
                aksjonspunkter.add(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_DEKNINGSGRAD));
            }
        }
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        if (tilSteg.equals(BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG)) {
            beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddForeslåBeregningsgrunnlagVedTilbakeføring();
        }
    }

    private BeregningsgrunnlagInputFelles getInputTjeneste(FagsakYtelseType ytelseType) {
        return beregningsgrunnlagInputProvider.getTjeneste(ytelseType);
    }

    private List<UidentifisertBarn> getBarn(Long behandlingId) {
        Objects.requireNonNull(familieHendelseRepository, "familieHendelseRepository");
        var familiehendelse = familieHendelseRepository
                .hentAggregatHvisEksisterer(behandlingId)
                .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon);
        return familiehendelse.orElseThrow().getBarna();
    }
}
