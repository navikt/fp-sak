package no.nav.foreldrepenger.behandling.steg.medlemskap.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.BehandlingFlytkontroll;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.medlemskap.KontrollerFaktaLøpendeMedlemskapSteg;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
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
import no.nav.foreldrepenger.domene.medlem.impl.MedlemResultat;
import no.nav.foreldrepenger.domene.uttak.SkalKopiereUttakTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.AvklarMedlemskapUtleder;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(BehandlingStegType.KONTROLLER_LØPENDE_MEDLEMSKAP)
@BehandlingTypeRef(BehandlingType.REVURDERING) // Revurdering
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) // Foreldrepenger
@ApplicationScoped
public class KontrollerFaktaLøpendeMedlemskapStegRevurdering implements KontrollerFaktaLøpendeMedlemskapSteg {

    private static final Environment ENV = Environment.current(); // TODO medlemskap2 sanere etter omlegging
    private static final Logger LOG = LoggerFactory.getLogger(KontrollerFaktaLøpendeMedlemskapStegRevurdering.class);

    private BehandlingsresultatRepository behandlingsresultatRepository;
    private UtledVurderingsdatoerForMedlemskapTjeneste tjeneste;
    private BehandlingRepository behandlingRepository;
    private VurderMedlemskapTjeneste vurderMedlemskapTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingFlytkontroll flytkontroll;
    private SkalKopiereUttakTjeneste skalKopiereUttakTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private AvklarMedlemskapUtleder avklarMedlemskapUtleder;

    @Inject
    public KontrollerFaktaLøpendeMedlemskapStegRevurdering(UtledVurderingsdatoerForMedlemskapTjeneste vurderingsdatoer,
                                                           BehandlingRepositoryProvider provider,
                                                           VurderMedlemskapTjeneste vurderMedlemskapTjeneste,
                                                           SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                           BehandlingFlytkontroll flytkontroll,
                                                           SkalKopiereUttakTjeneste skalKopiereUttakTjeneste,
                                                           UttakInputTjeneste uttakInputTjeneste,
                                                           AvklarMedlemskapUtleder avklarMedlemskapUtleder) {
        this.tjeneste = vurderingsdatoer;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.vurderMedlemskapTjeneste = vurderMedlemskapTjeneste;
        this.behandlingsresultatRepository = provider.getBehandlingsresultatRepository();
        this.flytkontroll = flytkontroll;
        this.skalKopiereUttakTjeneste = skalKopiereUttakTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.avklarMedlemskapUtleder = avklarMedlemskapUtleder;
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

        if (!ENV.isProd()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (skalVurdereLøpendeMedlemskap(kontekst.getBehandlingId())) {
            if (!(behandling.erRevurdering() && FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType()))) {
                throw new IllegalStateException("Utvikler-feil: Behandler bare revudering i foreldrepengerkontekst!.");
            }
            var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
            var ref = BehandlingReferanse.fra(behandling);
            var finnVurderingsdatoer = tjeneste.finnVurderingsdatoer(ref, skjæringstidspunkter);
            var resultat = new HashSet<MedlemResultat>();
            if (!finnVurderingsdatoer.isEmpty()) {
                finnVurderingsdatoer.forEach(dato -> resultat.addAll(vurderMedlemskapTjeneste.vurderMedlemskap(ref, skjæringstidspunkter, dato)));
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
