package no.nav.foreldrepenger.behandling.steg.medlemskap.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.BehandlingFlytkontroll;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.medlemskap.KontrollerFaktaLøpendeMedlemskapSteg;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.medlem.UtledVurderingsdatoerForMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.medlem.VurderMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.uttak.SkalKopiereUttakTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING;

@BehandlingStegRef(BehandlingStegType.KONTROLLER_LØPENDE_MEDLEMSKAP)
@BehandlingTypeRef(BehandlingType.REVURDERING) // Revurdering
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) // Foreldrepenger
@ApplicationScoped
public class KontrollerFaktaLøpendeMedlemskapStegRevurdering implements KontrollerFaktaLøpendeMedlemskapSteg {

    private static final Logger LOG = LoggerFactory.getLogger(KontrollerFaktaLøpendeMedlemskapStegRevurdering.class);

    private BehandlingsresultatRepository behandlingsresultatRepository;
    private UtledVurderingsdatoerForMedlemskapTjeneste tjeneste;
    private BehandlingRepository behandlingRepository;
    private VurderMedlemskapTjeneste vurderMedlemskapTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingFlytkontroll flytkontroll;
    private SkalKopiereUttakTjeneste skalKopiereUttakTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;

    @Inject
    public KontrollerFaktaLøpendeMedlemskapStegRevurdering(UtledVurderingsdatoerForMedlemskapTjeneste vurderingsdatoer,
                                                           BehandlingRepositoryProvider provider,
                                                           VurderMedlemskapTjeneste vurderMedlemskapTjeneste,
                                                           SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                           BehandlingFlytkontroll flytkontroll,
                                                           SkalKopiereUttakTjeneste skalKopiereUttakTjeneste,
                                                           UttakInputTjeneste uttakInputTjeneste) {
        this.tjeneste = vurderingsdatoer;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.vurderMedlemskapTjeneste = vurderMedlemskapTjeneste;
        this.behandlingsresultatRepository = provider.getBehandlingsresultatRepository();
        this.flytkontroll = flytkontroll;
        this.skalKopiereUttakTjeneste = skalKopiereUttakTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
    }

    KontrollerFaktaLøpendeMedlemskapStegRevurdering() {
        // CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        List<AksjonspunktResultat> aksjonspunkter = new ArrayList<>();
        if (flytkontroll.uttaksProsessenSkalVente(kontekst.getBehandlingId())) {
            LOG.info("Flytkontroll UTTAK: Setter behandling {} revurdering på vent grunnet berørt eller annen part", kontekst.getBehandlingId());
            aksjonspunkter.add(AksjonspunktResultat.opprettForAksjonspunktMedFrist(AUTO_KØET_BEHANDLING, Venteårsak.VENT_ÅPEN_BEHANDLING, null));
        }

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (skalVurdereLøpendeMedlemskap(kontekst.getBehandlingId())) {
            if (!(behandling.erRevurdering() && FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType()))) {
                throw new IllegalStateException("Utvikler-feil: Behandler bare revudering i foreldrepengerkontekst!.");
            }
            var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
            var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
            var finnVurderingsdatoer = tjeneste.finnVurderingsdatoer(ref);
            var resultat = new HashSet<>();
            if (!finnVurderingsdatoer.isEmpty()) {
                finnVurderingsdatoer.forEach(dato -> resultat.addAll(vurderMedlemskapTjeneste.vurderMedlemskap(ref, dato)));
            }
            if (!resultat.isEmpty()) {
                aksjonspunkter.add(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP));
            }
        }
        var uttakInput = uttakInputTjeneste.lagInput(behandlingId);
        if (skalKopiereUttakTjeneste.skalKopiereStegResultat(uttakInput)) {
            return BehandleStegResultat.fremoverførtMedAksjonspunktResultater(FellesTransisjoner.FREMHOPP_TIL_BEREGN_YTELSE, aksjonspunkter);
        }
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
    }

    private boolean skalVurdereLøpendeMedlemskap(Long behandlingId) {
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        return behandlingsresultat.map(b -> b.getVilkårResultat().getVilkårene()).orElse(Collections.emptyList())
                .stream()
                .anyMatch(v -> v.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET)
                        && v.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.OPPFYLT));
    }
}
