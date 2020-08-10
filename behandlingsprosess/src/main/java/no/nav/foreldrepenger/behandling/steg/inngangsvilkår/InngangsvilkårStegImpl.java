package no.nav.foreldrepenger.behandling.steg.inngangsvilkår;

import static no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT;
import static no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner.FREMHOPP_TIL_UTTAKSPLAN;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;

public abstract class InngangsvilkårStegImpl implements InngangsvilkårSteg {
    private BehandlingRepository behandlingRepository;
    private InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingStegType behandlingStegType;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    public InngangsvilkårStegImpl(BehandlingRepositoryProvider repositoryProvider, InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste, BehandlingStegType behandlingStegType) {
        this.repositoryProvider = repositoryProvider;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.inngangsvilkårFellesTjeneste = inngangsvilkårFellesTjeneste;
        this.behandlingStegType = behandlingStegType;
    }

    protected InngangsvilkårStegImpl() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        // Hent behandlingsgrunnlag og vilkårtyper
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        List<VilkårType> vilkårHåndtertAvSteg = vilkårHåndtertAvSteg();
        Set<VilkårType> vilkårTyper = getBehandlingsresultat(behandling).getVilkårResultat().getVilkårene().stream()
            .map(Vilkår::getVilkårType)
            .filter(vilkårHåndtertAvSteg::contains)
            .collect(Collectors.toSet());

        if (!(vilkårHåndtertAvSteg.isEmpty() || !vilkårTyper.isEmpty())) {
            throw new IllegalArgumentException(String.format("Utviklerfeil: Steg[%s] håndterer ikke angitte vilkår %s", this.getClass(), vilkårTyper)); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (skipSteget(kontekst)) {
            // Vil ikke re-evaluere hvis allerede overstyrt til avslag/oppfylt. Vil man endre overstyring gjøres det ved FORVED
            VilkårUtfallType overstyrtUtfall = overstyrtVilkårUtfall(kontekst.getBehandlingId()).orElse(VilkårUtfallType.UDEFINERT);
            if (VilkårUtfallType.IKKE_OPPFYLT.equals(overstyrtUtfall)) {
                return getBehandleStegResultatVedAvslag(behandling, Collections.emptyList());
            }
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }


        // vilkårTyper kan her vær tom, men vi går videre med et likevel (VURDERSAMLET steget skriver ned vilkåresultat)

        // Kall regelmotor
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, inngangsvilkårFellesTjeneste.getSkjæringstidspunkter(behandling.getId()));
        RegelResultat regelResultat = inngangsvilkårFellesTjeneste.vurderInngangsvilkår(vilkårTyper, behandling, ref);

        // Oppdater behandling
        behandlingRepository.lagre(regelResultat.getVilkårResultat(), kontekst.getSkriveLås());

        utførtRegler(kontekst, behandling, regelResultat);

        // Returner behandlingsresultat
        if (erNoenVilkårIkkeOppfylt(regelResultat) && !harÅpentOverstyringspunktForInneværendeSteg(behandling)) {
            return stegResultatVilkårIkkeOppfylt(regelResultat, behandling);
        } else {
            return stegResultat(regelResultat);
        }
    }

    protected boolean skipSteget(BehandlingskontrollKontekst kontekst) {
        // Hvis vilkåret er overstyrt skal vi ikke kjøre forrettningslogikken
        return erVilkårOverstyrt(kontekst.getBehandlingId());
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }

    private boolean harAvslåttForrigeBehandling(Behandling revurdering) {
        Optional<Behandling> originalBehandling = revurdering.getOriginalBehandlingId().map(behandlingRepository::hentBehandling);
        if (originalBehandling.isPresent()) {
            Behandlingsresultat behandlingsresultat = getBehandlingsresultat(originalBehandling.get());
            // Dersom originalBehandling er et beslutningsvedtak må vi lete videre etter det faktiske resultatet for å kunne vurdere om forrige behandling var avslått
            if (BehandlingResultatType.INGEN_ENDRING.equals(behandlingsresultat.getBehandlingResultatType())) {
                return harAvslåttForrigeBehandling(originalBehandling.get());
            } else {
                return behandlingsresultat.isBehandlingsresultatAvslått();
            }
        }
        return false;
    }

    private boolean harÅpentOverstyringspunktForInneværendeSteg(Behandling behandling) {
        return behandling.getÅpneAksjonspunkter().stream()
            .filter(aksjonspunkt -> aksjonspunkt.getAksjonspunktDefinisjon().getAksjonspunktType().equals(AksjonspunktType.OVERSTYRING))
            .anyMatch(aksjonspunkt ->
                aksjonspunkt.getAksjonspunktDefinisjon().getBehandlingSteg().equals(behandlingStegType));
    }

    protected BehandleStegResultat stegResultat(RegelResultat regelResultat) {
        return BehandleStegResultat.utførtMedAksjonspunkter(regelResultat.getAksjonspunktDefinisjoner());
    }

    protected BehandleStegResultat stegResultatVilkårIkkeOppfylt(RegelResultat regelResultat, Behandling behandling) {
        // Forbedring: InngangsvilkårStegImpl som annoterbar med FagsakYtelseType og BehandlingType
        // Her hardkodes disse parameterne
        return getBehandleStegResultatVedAvslag(behandling, regelResultat.getAksjonspunktDefinisjoner());
    }

    private BehandleStegResultat getBehandleStegResultatVedAvslag(Behandling behandling, List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner) {
        if (behandling.erRevurdering() && behandling.getFagsak().getYtelseType().equals(FagsakYtelseType.FORELDREPENGER)
            && !harAvslåttForrigeBehandling(behandling)) {
            return BehandleStegResultat.fremoverførtMedAksjonspunkter(FREMHOPP_TIL_UTTAKSPLAN, aksjonspunktDefinisjoner);
        }
        return BehandleStegResultat.fremoverførtMedAksjonspunkter(FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT, aksjonspunktDefinisjoner);
    }

    @SuppressWarnings("unused")
    protected void utførtRegler(BehandlingskontrollKontekst kontekst, Behandling behandling, RegelResultat regelResultat) {
        // template method
    }

    // Vennligst ikke override - det er forbeholdt vurdersamlet ....
    protected boolean erNoenVilkårIkkeOppfylt(RegelResultat regelResultat) {
        return regelResultat.getVilkårResultat().getVilkårene().stream()
            .filter(vilkår -> vilkårHåndtertAvSteg().contains(vilkår.getVilkårType()))
            .anyMatch(v -> v.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.IKKE_OPPFYLT));
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType hoppesTilSteg,
                                   BehandlingStegType hoppesFraSteg) {
        if (!erVilkårOverstyrt(kontekst.getBehandlingId())) {
            Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            RyddVilkårTyper ryddVilkårTyper = new RyddVilkårTyper(repositoryProvider, behandling, kontekst);
            ryddVilkårTyper.ryddVedTilbakeføring(vilkårHåndtertAvSteg());
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        }
    }

    @Override
    public void vedHoppOverFramover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType hoppesFraSteg,
                                    BehandlingStegType hoppesTilSteg) {
        //TODO skal man rydde opp ved framoverhopp?
        if (!erVilkårOverstyrt(kontekst.getBehandlingId())) {
            Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            RyddVilkårTyper ryddVilkårTyper = new RyddVilkårTyper(repositoryProvider, behandling, kontekst);
            ryddVilkårTyper.ryddVedOverhoppFramover(vilkårHåndtertAvSteg());
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        }
    }

    protected boolean erVilkårOverstyrt(Long behandlingId) {
        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        return behandlingsresultat.map(Behandlingsresultat::getVilkårResultat).map(VilkårResultat::getVilkårene).orElse(Collections.emptyList()).stream()
            .filter(vilkår -> vilkårHåndtertAvSteg().contains(vilkår.getVilkårType()))
            .anyMatch(Vilkår::erOverstyrt);
    }

    private Optional<VilkårUtfallType> overstyrtVilkårUtfall(Long behandlingId) {
        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        return behandlingsresultat.map(Behandlingsresultat::getVilkårResultat).map(VilkårResultat::getVilkårene).orElse(Collections.emptyList()).stream()
            .filter(vilkår -> vilkårHåndtertAvSteg().contains(vilkår.getVilkårType()))
            .filter(Vilkår::erOverstyrt)
            .map(Vilkår::getGjeldendeVilkårUtfall)
            .findFirst();
    }
}
