package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import java.util.List;

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
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.FaktaUttakAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@BehandlingStegRef(BehandlingStegType.FAKTA_UTTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class FaktaUttakSteg implements UttakSteg {

    private FaktaUttakAksjonspunktUtleder faktaUttakAksjonspunktUtleder;
    private UttakInputTjeneste uttakInputTjeneste;
    private BehandlingRepository behandlingRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private EøsUttakRepository eøsUttakRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;

    @Inject
    public FaktaUttakSteg(FaktaUttakAksjonspunktUtleder faktaUttakAksjonspunktUtleder,
                          UttakInputTjeneste uttakInputTjeneste,
                          BehandlingRepository behandlingRepository,
                          YtelseFordelingTjeneste ytelseFordelingTjeneste,
                          EøsUttakRepository eøsUttakRepository,
                          HistorikkinnslagRepository historikkinnslagRepository) {
        this.faktaUttakAksjonspunktUtleder = faktaUttakAksjonspunktUtleder;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.eøsUttakRepository = eøsUttakRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    FaktaUttakSteg() {
        //CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var uttakInput = uttakInputTjeneste.lagInput(kontekst.getBehandlingId());
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(kontekst.getBehandlingId());
        if (!ytelseFordelingAggregat.avklartAnnenForelderHarRettEØS()) {
            fjernEøsUttak(kontekst);
        }
        var faktaUttakAP = utledAp(uttakInput);
        return BehandleStegResultat.utførtMedAksjonspunkter(faktaUttakAP);
    }

    private void fjernEøsUttak(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        eøsUttakRepository.hentGrunnlag(behandlingId).ifPresent(grunnlag -> {
            historikkinnslagRepository.lagre(historikkinnslagForFjerningAvEøsUttak(kontekst, behandlingId));
            eøsUttakRepository.deaktiverAktivtGrunnlagHvisFinnes(behandlingId);
        });
    }

    private static Historikkinnslag historikkinnslagForFjerningAvEøsUttak(BehandlingskontrollKontekst kontekst, Long behandlingId) {
        return new Historikkinnslag.Builder().medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medBehandlingId(behandlingId)
            .medFagsakId(kontekst.getFagsakId())
            .medTittel("Ryddet bort annen forelders uttak i eøs ettersom avklart rettighetstype tilsier at dette ikke lenger er en EØS-sak")
            .build();
    }

    private List<AksjonspunktDefinisjon> utledAp(UttakInput uttakInput) {
        var behandlingId = uttakInput.getBehandlingReferanse().behandlingId();
        if (harÅpentOverstyringAp(behandlingId)) {
            return List.of();
        }
        //Bruker justert her for å reutlede avbrutt AP ved tilbakehopp
        return faktaUttakAksjonspunktUtleder.utledAksjonspunkterFor(uttakInput);
    }

    private boolean harÅpentOverstyringAp(Long behandlingId) {
        return behandlingRepository.hentBehandling(behandlingId)
            .harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.OVERSTYRING_FAKTA_UTTAK);
    }
}
