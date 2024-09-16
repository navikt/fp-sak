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
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultatOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.AvklarMedlemskapUtleder;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAksjonspunktÅrsak;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAvvik;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.InngangsvilkårRegler;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@VilkårTypeRef(VilkårType.MEDLEMSKAPSVILKÅRET)
public class InngangsvilkårMedlemskap implements Inngangsvilkår {

    private static final Logger LOG = LoggerFactory.getLogger(InngangsvilkårMedlemskap.class);
    private static final Environment ENV = Environment.current();

    private MedlemsvilkårOversetter medlemsvilkårOversetter;
    private BehandlingRepository behandlingRepository;
    private AvklarMedlemskapUtleder avklarMedlemskapUtleder;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    InngangsvilkårMedlemskap() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårMedlemskap(MedlemsvilkårOversetter medlemsvilkårOversetter,
                                    BehandlingRepository behandlingRepository,
                                    AvklarMedlemskapUtleder avklarMedlemskapUtleder,
                                    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.medlemsvilkårOversetter = medlemsvilkårOversetter;
        this.behandlingRepository = behandlingRepository;
        this.avklarMedlemskapUtleder = avklarMedlemskapUtleder;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(ref.behandlingId());
        var grunnlag = medlemsvilkårOversetter.oversettTilRegelModellMedlemskap(ref, skjæringstidspunkt);

        var resultat = InngangsvilkårRegler.medlemskap(grunnlag);

        if (ENV.isProd()) {
            var vilkårData = RegelResultatOversetter.oversett(VilkårType.MEDLEMSKAPSVILKÅRET, resultat);

            try {
                var behandling = behandlingRepository.hentBehandling(ref.behandlingId());

                var nyUtledingResultat = utledNyttMedlemskapAksjonspunkt(ref);
                var gammelUtledetÅrsaker = behandling.getAksjonspunkter()
                    .stream()
                    .map(Aksjonspunkt::getAksjonspunktDefinisjon)
                    .filter(
                        ap -> Set.of(AVKLAR_OM_ER_BOSATT, AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE, AVKLAR_LOVLIG_OPPHOLD, AVKLAR_OPPHOLDSRETT).contains(ap))
                    .map(InngangsvilkårMedlemskap::map)
                    .collect(Collectors.toSet());
                logDiff(nyUtledingResultat, gammelUtledetÅrsaker, behandling.getFagsakYtelseType(), "inngangsvilkårv2");
            } catch (Exception e) {
                LOG.info("Medlemskap sammenligning feilet", e);
            }
            return vilkårData;
        } else {
            return avklarMedlemskapUtleder.utledFor(ref);
        }
    }

    public static void logDiff(Set<MedlemskapAksjonspunktÅrsak> nyUtledetÅrsaker,
                               Set<MedlemskapAksjonspunktÅrsak> gammelUtledetÅrsaker,
                               FagsakYtelseType ytelse,
                               String kontekst) {

        if (gammelUtledetÅrsaker.isEmpty() && nyUtledetÅrsaker.isEmpty()) {
            LOG.info("Medlemskap for ytelse {} {} like, ingen avklaring", ytelse, kontekst);
        } else if (gammelUtledetÅrsaker.equals(nyUtledetÅrsaker)) {
            LOG.info("Medlemskap for ytelse {} {} like, samme avklaring: {}", ytelse, kontekst, nyUtledetÅrsaker);
        } else {


            nyUtledetÅrsaker.stream()
                .filter(nyÅrsak -> !gammelUtledetÅrsaker.contains(nyÅrsak))
                .forEach(nyÅrsak -> LOG.info("Medlemskap for ytelse {} {} diff {} {} - {}", ytelse, kontekst,
                    gammelUtledetÅrsaker.isEmpty() ? "Ny årsak hadde ikke noen gamle" : "Ny årsak som ikke var blant de gamle", nyÅrsak,
                    gammelUtledetÅrsaker));

            gammelUtledetÅrsaker.stream()
                .filter(gammelÅrsak -> !nyUtledetÅrsaker.contains(gammelÅrsak))
                .forEach(gammelÅrsak -> LOG.info("Medlemskap for ytelse {} {} diff {} {} - {}", ytelse, kontekst,
                    nyUtledetÅrsaker.isEmpty() ? "Gammel årsak hadde ikke noen nye" : "Gammel årsak som ikke var blant de nye", gammelÅrsak,
                    nyUtledetÅrsaker));
        }
    }

    private Set<MedlemskapAksjonspunktÅrsak> utledNyttMedlemskapAksjonspunkt(BehandlingReferanse behandlingRef) {
        return avklarMedlemskapUtleder.utledAvvik(behandlingRef).stream().map(this::map).collect(Collectors.toSet());
    }

    private MedlemskapAksjonspunktÅrsak map(MedlemskapAvvik avvik) {
        return switch (avvik) {
            case BOSATT_UTENLANDSOPPHOLD, BOSATT_MANGLENDE_BOSTEDSADRESSE, BOSATT_UTENLANDSADRESSE, BOSATT_UGYLDIG_PERSONSTATUS ->
                MedlemskapAksjonspunktÅrsak.BOSATT;
            case TREDJELAND_MANGLENDE_LOVLIG_OPPHOLD -> MedlemskapAksjonspunktÅrsak.OPPHOLD;
            case EØS_MANGLENDE_ANSETTELSE_MED_INNTEKT -> MedlemskapAksjonspunktÅrsak.OPPHOLDSRETT;
            case MEDL_PERIODER -> MedlemskapAksjonspunktÅrsak.MEDLEMSKAPSPERIODER_FRA_REGISTER;
        };
    }

    public static MedlemskapAksjonspunktÅrsak map(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return switch (aksjonspunktDefinisjon) {
            case AVKLAR_OM_ER_BOSATT -> MedlemskapAksjonspunktÅrsak.BOSATT;
            case AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE -> MedlemskapAksjonspunktÅrsak.MEDLEMSKAPSPERIODER_FRA_REGISTER;
            case AVKLAR_LOVLIG_OPPHOLD -> MedlemskapAksjonspunktÅrsak.OPPHOLD;
            case AVKLAR_OPPHOLDSRETT -> MedlemskapAksjonspunktÅrsak.OPPHOLDSRETT;
            default -> throw new IllegalStateException("Unexpected value: " + aksjonspunktDefinisjon);
        };
    }
}
