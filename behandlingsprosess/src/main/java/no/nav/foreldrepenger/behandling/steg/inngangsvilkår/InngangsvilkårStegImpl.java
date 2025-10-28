package no.nav.foreldrepenger.behandling.steg.inngangsvilkår;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;

public abstract class InngangsvilkårStegImpl implements InngangsvilkårSteg {

    private BehandlingRepository behandlingRepository;
    private InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingStegType behandlingStegType;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    public InngangsvilkårStegImpl(BehandlingRepositoryProvider repositoryProvider, InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste,
            BehandlingStegType behandlingStegType) {
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
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        // Ett tilfelle må velge vilkår etter at tidligere vilkår er vurdert (termindato)
        opprettDynamiskeVilkårForBehandling(kontekst, behandling);

        var vilkårHåndtertAvSteg = vilkårHåndtertAvSteg();
        var vilkårTyper = getBehandlingsresultat(behandling).getVilkårResultat().getVilkårene().stream()
                .map(Vilkår::getVilkårType)
                .filter(vilkårHåndtertAvSteg::contains)
                .collect(Collectors.toSet());

        if (!(vilkårHåndtertAvSteg.isEmpty() || !vilkårTyper.isEmpty())) {
            throw new IllegalArgumentException(
                    String.format("Utviklerfeil: Steg[%s] håndterer ikke angitte vilkår %s", this.getClass(), vilkårTyper));
        }

        if (skipSteget(kontekst)) {
            // Vil ikke re-evaluere hvis allerede overstyrt til avslag/oppfylt. Vil man
            // endre overstyring gjøres det ved FORVED
            var overstyrtUtfall = overstyrtVilkårUtfall(kontekst.getBehandlingId()).orElse(VilkårUtfallType.UDEFINERT);
            if (VilkårUtfallType.IKKE_OPPFYLT.equals(overstyrtUtfall)) {
                return getBehandleStegResultatVedAvslag(behandling, Collections.emptyList());
            }
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        // vilkårTyper kan her vær tom, men vi går videre med et likevel (VURDERSAMLET
        // steget skriver ned vilkåresultat)

        // Kall regelmotor
        var ref = BehandlingReferanse.fra(behandling);
        var regelResultat = inngangsvilkårFellesTjeneste.vurderInngangsvilkår(vilkårTyper, behandling, ref);

        // Oppdater behandling
        behandlingRepository.lagre(regelResultat.vilkårResultat(), kontekst.getSkriveLås());

        utførtRegler(kontekst, behandling, regelResultat);

        // Returner behandlingsresultat
        if (erNoenVilkårIkkeOppfylt(regelResultat) && !harÅpentOverstyringspunktForInneværendeSteg(behandling)) {
            return stegResultatVilkårIkkeOppfylt(regelResultat, behandling);
        }
        valideringOmsorgsovertakelseSammeBarn(regelResultat.aksjonspunktDefinisjoner(), behandling);
        return stegResultat(regelResultat);
    }

    protected boolean skipSteget(BehandlingskontrollKontekst kontekst) {
        // Hvis vilkåret er overstyrt skal vi ikke kjøre forrettningslogikken
        return erVilkårOverstyrt(kontekst.getBehandlingId());
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandlingsresultatRepository.hent(behandling.getId());
    }

    private boolean harAvslåttForrigeBehandling(Behandling revurdering) {
        var originalBehandling = revurdering.getOriginalBehandlingId().map(behandlingRepository::hentBehandling);
        if (originalBehandling.isPresent()) {
            var behandlingsresultat = getBehandlingsresultat(originalBehandling.get());
            // Dersom originalBehandling er et beslutningsvedtak må vi lete videre etter det
            // faktiske resultatet for å kunne vurdere om forrige behandling var avslått
            if (BehandlingResultatType.INGEN_ENDRING.equals(behandlingsresultat.getBehandlingResultatType())) {
                return harAvslåttForrigeBehandling(originalBehandling.get());
            }
            return behandlingsresultat.isBehandlingsresultatAvslått();
        }
        return false;
    }

    private boolean harÅpentOverstyringspunktForInneværendeSteg(Behandling behandling) {
        return behandling.getÅpneAksjonspunkter().stream()
                .filter(aksjonspunkt -> AksjonspunktType.OVERSTYRING.equals(aksjonspunkt.getAksjonspunktDefinisjon().getAksjonspunktType()))
                .anyMatch(aksjonspunkt -> aksjonspunkt.getAksjonspunktDefinisjon().getBehandlingSteg().equals(behandlingStegType));
    }

    protected BehandleStegResultat stegResultat(RegelResultat regelResultat) {
        return BehandleStegResultat.utførtMedAksjonspunkter(regelResultat.aksjonspunktDefinisjoner());
    }

    private void valideringOmsorgsovertakelseSammeBarn(Collection<AksjonspunktDefinisjon> apDefs,  Behandling behandling) {
        if (apDefs.contains(AksjonspunktDefinisjon.VURDER_OMSORGSOVERTAKELSEVILKÅRET)
            && behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE).isPresent()) {
            throw new IllegalStateException("Omsorgsovertakelse og samme barn ikke støttet ennå");
        }
    }



    protected BehandleStegResultat stegResultatVilkårIkkeOppfylt(RegelResultat regelResultat, Behandling behandling) {
        // Forbedring: InngangsvilkårStegImpl som annoterbar med FagsakYtelseType og
        // BehandlingType
        // Her hardkodes disse parameterne
        return getBehandleStegResultatVedAvslag(behandling, regelResultat.aksjonspunktDefinisjoner());
    }

    private BehandleStegResultat getBehandleStegResultatVedAvslag(Behandling behandling, List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner) {
        if (behandling.erRevurdering() && !FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType()) && !harAvslåttForrigeBehandling(
            behandling)) {
            return BehandleStegResultat.fremoverførtMedAksjonspunkter(BehandlingStegType.INNGANG_UTTAK, aksjonspunktDefinisjoner);
        }
        return BehandleStegResultat.fremoverførtMedAksjonspunkter(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT, aksjonspunktDefinisjoner);
    }

    protected void opprettDynamiskeVilkårForBehandling(BehandlingskontrollKontekst kontekst, Behandling behandling) {
    }

    protected void utførtRegler(BehandlingskontrollKontekst kontekst, Behandling behandling, RegelResultat regelResultat) {
    }

    // Vennligst ikke override - det er forbeholdt vurdersamlet ....
    protected boolean erNoenVilkårIkkeOppfylt(RegelResultat regelResultat) {
        // Sjekk om gjeldende ikke oppfylt (kan være manuell satt fra før), eller om siste regelkjøring gir merknad (ikke oppfylt)
        return regelResultat.vilkårResultat().getVilkårene().stream()
                .filter(vilkår -> vilkårHåndtertAvSteg().contains(vilkår.getVilkårType()))
                .anyMatch(v -> v.erIkkeOppfylt() || (!v.erOverstyrt() && manuellVurderingNårRegelVilkårIkkeOppfylt() && v.getVilkårUtfallMerknad() != null));
    }

    protected boolean manuellVurderingNårRegelVilkårIkkeOppfylt() {
        return false;
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType hoppesTilSteg,
            BehandlingStegType hoppesFraSteg) {
        ryddVilkårTilbakeHopp(kontekst, b -> BehandlingType.FØRSTEGANGSSØKNAD.equals(b.getType()));
    }

    @Override
    public void vedHoppOverFramover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType hoppesFraSteg,
            BehandlingStegType hoppesTilSteg) {
        // TODO skal man rydde opp ved framoverhopp?
        if (!erVilkårOverstyrt(kontekst.getBehandlingId())) {
            var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            var ryddVilkårTyper = new RyddVilkårTyper(repositoryProvider, behandling, kontekst);
            ryddVilkårTyper.ryddVedOverhoppFramover(vilkårHåndtertAvSteg());
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        }
    }

    protected void ryddVilkårTilbakeHopp(BehandlingskontrollKontekst kontekst, Predicate<Behandling> filter) {
        if (!erVilkårOverstyrt(kontekst.getBehandlingId())) {
            var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            var ryddVilkårTyper = new RyddVilkårTyper(repositoryProvider, behandling, kontekst);
            ryddVilkårTyper.ryddVedTilbakeføring(vilkårHåndtertAvSteg(), filter.test(behandling));
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        }
    }

    protected boolean erVilkårOverstyrt(Long behandlingId) {
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        return behandlingsresultat.map(Behandlingsresultat::getVilkårResultat).map(VilkårResultat::getVilkårene).orElse(Collections.emptyList())
                .stream()
                .filter(vilkår -> vilkårHåndtertAvSteg().contains(vilkår.getVilkårType()))
                .anyMatch(Vilkår::erOverstyrt);
    }

    private Optional<VilkårUtfallType> overstyrtVilkårUtfall(Long behandlingId) {
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        return behandlingsresultat.map(Behandlingsresultat::getVilkårResultat).map(VilkårResultat::getVilkårene).orElse(Collections.emptyList())
                .stream()
                .filter(vilkår -> vilkårHåndtertAvSteg().contains(vilkår.getVilkårType()))
                .filter(Vilkår::erOverstyrt)
                .map(Vilkår::getGjeldendeVilkårUtfall)
                .findFirst();
    }
}
