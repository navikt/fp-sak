package no.nav.foreldrepenger.inngangsvilkaar.medlemskap;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_OPPHOLDSRETT;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultatOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.AvklarMedlemskapUtleder;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.InngangsvilkårRegler;

@ApplicationScoped
@VilkårTypeRef(VilkårType.MEDLEMSKAPSVILKÅRET)
public class InngangsvilkårMedlemskap implements Inngangsvilkår {

    private static final Logger LOG = LoggerFactory.getLogger(InngangsvilkårMedlemskap.class);

    private MedlemsvilkårOversetter medlemsvilkårOversetter;
    private BehandlingRepository behandlingRepository;
    private AvklarMedlemskapUtleder avklarMedlemskapUtleder;

    InngangsvilkårMedlemskap() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårMedlemskap(MedlemsvilkårOversetter medlemsvilkårOversetter,
                                    BehandlingRepository behandlingRepository,
                                    AvklarMedlemskapUtleder avklarMedlemskapUtleder) {
        this.medlemsvilkårOversetter = medlemsvilkårOversetter;
        this.behandlingRepository = behandlingRepository;
        this.avklarMedlemskapUtleder = avklarMedlemskapUtleder;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        var grunnlag = medlemsvilkårOversetter.oversettTilRegelModellMedlemskap(ref);

        var resultat = InngangsvilkårRegler.medlemskap(grunnlag);

        var vilkårData = RegelResultatOversetter.oversett(VilkårType.MEDLEMSKAPSVILKÅRET, resultat);

        try {
            var behandling = behandlingRepository.hentBehandling(ref.behandlingId());

            var nyUtledingResultat = utledNyttMedlemskapAksjonspunkt(ref);
            logDiff(behandling, nyUtledingResultat);
        } catch (Exception e) {
            LOG.info("Medlemskap sammenligning feilet", e);
        }

        return vilkårData;

    }

    private static void logDiff(Behandling behandling, Set<MedlemskapAksjonspunktÅrsak> nyUtledetÅrsaker) {
        var gammelUtledetÅrsaker = behandling.getAksjonspunkter()
            .stream()
            .map(Aksjonspunkt::getAksjonspunktDefinisjon)
            .filter(ap -> Set.of(AVKLAR_OM_ER_BOSATT, AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE, AVKLAR_LOVLIG_OPPHOLD, AVKLAR_OPPHOLDSRETT).contains(ap))
            .map(InngangsvilkårMedlemskap::map)
            .collect(Collectors.toSet());

        var ytelse = behandling.getFagsakYtelseType();
        if (gammelUtledetÅrsaker.isEmpty() && nyUtledetÅrsaker.isEmpty()) {
            LOG.info("Medlemskap for ytelse {} like, ingen avklaring", ytelse);
        } else if (gammelUtledetÅrsaker.equals(nyUtledetÅrsaker)) {
            LOG.info("Medlemskap for ytelse {} like, samme avklaring: {}", ytelse, nyUtledetÅrsaker);
        } else {
            nyUtledetÅrsaker.stream()
                .filter(nyÅrsak -> !gammelUtledetÅrsaker.contains(nyÅrsak))
                .forEach(nyÅrsak -> LOG.info("Medlemskap for ytelse {} diff nyÅrsakManglerGammelÅrsak {} - {}", ytelse, nyÅrsak, gammelUtledetÅrsaker));

            gammelUtledetÅrsaker.stream()
                .filter(gammelÅrsak -> !nyUtledetÅrsaker.contains(gammelÅrsak))
                .forEach(gammelÅrsak -> LOG.info("Medlemskap for ytelse {} diff gammelÅrsakManglerNyÅrsak {} - {}", ytelse, gammelÅrsak, nyUtledetÅrsaker));
        }
    }

    private Set<MedlemskapAksjonspunktÅrsak> utledNyttMedlemskapAksjonspunkt(BehandlingReferanse behandlingRef) {
        return avklarMedlemskapUtleder.utledFor(behandlingRef);
    }

    private static MedlemskapAksjonspunktÅrsak map(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return switch (aksjonspunktDefinisjon) {
            case AVKLAR_OM_ER_BOSATT -> MedlemskapAksjonspunktÅrsak.BOSATT;
            case AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE -> MedlemskapAksjonspunktÅrsak.MEDLEMSKAPSPERIODER_FRA_REGISTER;
            case AVKLAR_LOVLIG_OPPHOLD -> MedlemskapAksjonspunktÅrsak.OPPHOLD;
            case AVKLAR_OPPHOLDSRETT -> MedlemskapAksjonspunktÅrsak.OPPHOLDSRETT;
            default -> throw new IllegalStateException("Unexpected value: " + aksjonspunktDefinisjon);
        };
    }
}
