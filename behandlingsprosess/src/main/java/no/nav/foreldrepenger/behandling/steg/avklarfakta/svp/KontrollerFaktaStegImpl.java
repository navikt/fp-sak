package no.nav.foreldrepenger.behandling.steg.avklarfakta.svp;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerFaktaSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.inngangsvilkaar.utleder.SvangerskapspengerVilkårUtleder;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(BehandlingStegType.KONTROLLER_FAKTA)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
class KontrollerFaktaStegImpl implements KontrollerFaktaSteg {

    private KontrollerFaktaTjeneste tjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    KontrollerFaktaStegImpl() {
        // for CDI proxy
    }

    @Inject
    KontrollerFaktaStegImpl(BehandlingRepositoryProvider repositoryProvider,
            SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
            @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER) KontrollerFaktaTjeneste tjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.tjeneste = tjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);
        var aksjonspunktResultater = tjeneste.utledAksjonspunkter(ref, skjæringstidspunkter);
        utledVilkår(kontekst);
        behandling.setStartpunkt(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT); // Settes til første steg i Inngangsvilkår.
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultater);
    }

    private void utledVilkår(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var utledeteVilkår = SvangerskapspengerVilkårUtleder.utledVilkårFor(behandling);
        opprettVilkår(utledeteVilkår, behandling, kontekst.getSkriveLås());
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        if (!BehandlingStegType.KONTROLLER_FAKTA.equals(fraSteg) || BehandlingStegType.KONTROLLER_FAKTA.equals(tilSteg)) {
            var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            behandling.nullstillToTrinnsBehandling();
        }
    }

    private void opprettVilkår(Set<VilkårType> utledeteVilkår, Behandling behandling, BehandlingLås skriveLås) {
        // Opprett Vilkårsresultat med vilkårne som skal vurderes, og sett dem som ikke
        // vurdert
        var behandlingsresultat = getBehandlingsresultat(behandling);
        var vilkårBuilder = behandlingsresultat != null
                ? VilkårResultat.builderFraEksisterende(behandlingsresultat.getVilkårResultat())
                : VilkårResultat.builder();
        utledeteVilkår.forEach(vilkårBuilder::leggTilVilkårIkkeVurdert);
        var vilkårResultat = vilkårBuilder.buildFor(behandling);
        behandlingRepository.lagre(vilkårResultat, skriveLås);
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
    }

}
