package no.nav.foreldrepenger.behandling.steg.medlemskap.fp;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.steg.medlemskap.KontrollerFaktaLøpendeMedlemskapSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.domene.medlem.UtledVurderingsdatoerForMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.medlem.VurderMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.medlem.impl.MedlemResultat;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "KOFAK_LOP_MEDL")
@BehandlingTypeRef("BT-004") //Revurdering
@FagsakYtelseTypeRef("FP")  //Foreldrepenger
@ApplicationScoped
public class KontrollerFaktaLøpendeMedlemskapStegRevurdering implements KontrollerFaktaLøpendeMedlemskapSteg {

    private BehandlingsresultatRepository behandlingsresultatRepository;
    private UtledVurderingsdatoerForMedlemskapTjeneste tjeneste;

    private BehandlingRepository behandlingRepository;
    private VurderMedlemskapTjeneste vurderMedlemskapTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    KontrollerFaktaLøpendeMedlemskapStegRevurdering() {
        //CDI
    }

    @Inject
    public KontrollerFaktaLøpendeMedlemskapStegRevurdering(UtledVurderingsdatoerForMedlemskapTjeneste vurderingsdatoer,
                                                           BehandlingRepositoryProvider provider,
                                                           VurderMedlemskapTjeneste vurderMedlemskapTjeneste,
                                                           SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.tjeneste = vurderingsdatoer;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.vurderMedlemskapTjeneste = vurderMedlemskapTjeneste;
        this.behandlingsresultatRepository = provider.getBehandlingsresultatRepository();
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        if (skalVurdereLøpendeMedlemskap(kontekst.getBehandlingId())) {
            Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
            if (!(behandling.erRevurdering() && behandling.getFagsakYtelseType().gjelderForeldrepenger())) {
                throw new IllegalStateException("Utvikler-feil: Behandler bare revudering i foreldrepengerkontekst!.");
            }
            Set<LocalDate> finnVurderingsdatoer = tjeneste.finnVurderingsdatoer(behandlingId);
            Set<MedlemResultat> resultat = new HashSet<>();
            if (!finnVurderingsdatoer.isEmpty()) {
                Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
                BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
                finnVurderingsdatoer.forEach(dato -> resultat.addAll(vurderMedlemskapTjeneste.vurderMedlemskap(ref, dato)));
            }
            if (!resultat.isEmpty()) {
                return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP));
            }
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean skalVurdereLøpendeMedlemskap(Long behandlingId) {
        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        return behandlingsresultat.map(b -> b.getVilkårResultat().getVilkårene()).orElse(Collections.emptyList())
            .stream()
            .anyMatch(v -> v.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET) && v.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.OPPFYLT));
    }
}
