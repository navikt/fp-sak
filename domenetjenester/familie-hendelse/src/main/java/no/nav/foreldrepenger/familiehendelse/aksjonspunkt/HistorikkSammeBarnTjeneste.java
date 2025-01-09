package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AvslagbartAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import org.jboss.weld.exceptions.UnsupportedOperationException;

import java.util.Objects;
import java.util.Optional;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

@ApplicationScoped
class HistorikkSammeBarnTjeneste {
    private HistorikkinnslagRepository historikkinnslagRepository;

    protected HistorikkSammeBarnTjeneste() {
        // CDI
    }

    @Inject
    public HistorikkSammeBarnTjeneste(HistorikkinnslagRepository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    public void lagHistorikkinnslagForAksjonspunkt(BehandlingReferanse ref,
                                                   Behandlingsresultat behandlingsresultat,
                                                   AvslagbartAksjonspunktDto dto,
                                                   Vilkår relevantVilkårPåBehandling) {
        var vilkårType = Optional.ofNullable(behandlingsresultat)
            .map(Behandlingsresultat::getVilkårResultat)
            .flatMap(VilkårResultat::getVilkårForRelasjonTilBarn)
            .orElse(null);
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(getSkjermlenkeType(vilkårType, dto.getAksjonspunktDefinisjon()))
            .addLinje(vilkårResultatTekst(relevantVilkårPåBehandling, dto))
            .addLinje(dto.getBegrunnelse())
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private static HistorikkinnslagLinjeBuilder vilkårResultatTekst(Vilkår relevantVilkårPåBehandling, AvslagbartAksjonspunktDto dto) {
        var navn = finnTekstForFelt(relevantVilkårPåBehandling);
        var gjeldendeVilkårUtfall = relevantVilkårPåBehandling.getGjeldendeVilkårUtfall();
        var nyttVilkårUtfall = vilkårUtfallTypeFra(dto);
        var avslagsårsak = Avslagsårsak.fraDefinertKode(dto.getAvslagskode()).orElse(null);
        return VilkårUtfallType.IKKE_VURDERT.equals(gjeldendeVilkårUtfall) || Objects.equals(gjeldendeVilkårUtfall, nyttVilkårUtfall)
            ? fraTilEquals(navn, null, vilkårTekst(nyttVilkårUtfall, avslagsårsak))
            : fraTilEquals(navn, vilkårTekst(gjeldendeVilkårUtfall, relevantVilkårPåBehandling.getAvslagsårsak()), vilkårTekst(nyttVilkårUtfall, avslagsårsak));
    }

    private static String vilkårTekst(VilkårUtfallType vilkårUtfall, Avslagsårsak avslagsårsak) {
        return avslagsårsak != null
            ? String.format("%s med avslagsårsak %s", vilkårUtfall.getNavn(), avslagsårsak.getNavn())
            : vilkårUtfall.getNavn();
    }

    private static VilkårUtfallType vilkårUtfallTypeFra(AvslagbartAksjonspunktDto dto) {
        return Boolean.TRUE.equals(dto.getErVilkarOk()) ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;
    }

    private static String finnTekstForFelt(Vilkår vilkår) {
        return switch (vilkår.getVilkårType()) {
            case FØDSELSVILKÅRET_MOR, FØDSELSVILKÅRET_FAR_MEDMOR -> "Fødselsvilkåret";
            case ADOPSJONSVILKÅRET_ENGANGSSTØNAD -> "Adopsjonsvilkåret";
            default -> throw new IllegalStateException("Vilkår ikke støttet for dette historikkinnslaget: " + vilkår.getVilkårType());
        };
    }

    static SkjermlenkeType getSkjermlenkeType(VilkårType vilkårType, AksjonspunktDefinisjon aksjonspunktKode) {
        return switch (aksjonspunktKode) {
            case MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET -> SkjermlenkeType.SOEKNADSFRIST;
            case MANUELL_VURDERING_AV_OMSORGSVILKÅRET -> SkjermlenkeType.PUNKT_FOR_OMSORG;
            case MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_2_LEDD -> SkjermlenkeType.PUNKT_FOR_FORELDREANSVAR;
            case MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_4_LEDD -> SkjermlenkeType.PUNKT_FOR_FORELDREANSVAR;
            case SØKERS_OPPLYSNINGSPLIKT_MANU -> SkjermlenkeType.OPPLYSNINGSPLIKT;
            case AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE -> SkjermlenkeType.getSkjermlenkeTypeForMottattStotte(vilkårType);  // avklar om søker har mottatt støte
            case AVKLAR_OM_ANNEN_FORELDRE_HAR_MOTTATT_STØTTE -> SkjermlenkeType.getSkjermlenkeTypeForMottattStotte(vilkårType);  // avklar om annen forelder har mottatt støtte
            default -> throw new UnsupportedOperationException("Støtter ikke aksjonspunktKode=" + aksjonspunktKode);
        };
    }
}
